__author__ = 'BLN'

import abc
import pickle
import re
import os.path
import pos_tagging_utils
import collections
import morphit_reader
from enum import Enum
from decimal import Decimal

"""
This module contains implementation of some PoS taggers
"""


class PoSTagger:
    """
    This is a template class for PoS taggers
    """
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def get_sentence_tags(self, sentence=None, words=None):
        """
        Returns the tags associated with the words in the sentence
        :param sentence: a sentence in form of a string
        :param words: a sentence already tokenized in words
        :return:
        """
        pass

    @staticmethod
    def tokenize_sentence(sentence):
        """
        A tokenizer utility, just splitting the sentence with spaces and punctuation symbols (i.e. .,!?;:_-)
        :param sentence: a sentence in form of a string
        :return:
        """

        return re.findall(r"[\w']+|[.,!?;:_-]", sentence)

    @staticmethod
    def tag_index(tag, tags):
        if tag in tags:
            return tags[tag]
        else:  # workaround for punctuation character
            # if tag in ('.', ','):
            return tags['.']

    default_special_words = {'-LRB-': '-LRB-', '-RRB-': '-RRB-',
                             '-LSB-': '-LRB-', '-RSB-': '-RRB-',
                             '-': '.', ':': '.', ';': '.', '!': '.',
                             '?': '.'}
    """
    Some default special words to tag with the specified tag (mostly punctuation symbols)
    """


class UnknownWordsStrategy(Enum):
    """
    An enum for MostFrequentTagger unknown words tagging strategy
    """
    disabled = 0
    noun = 1
    noun_or_pnoun = 2
    morphit = 3


class MostFrequentTagger(PoSTagger):
    """
    A PoS tagger based on the most frequent tag of a word.
    """

    def __init__(self, corpus, pos_tags, special_words=None, noun_tag='NOUN', propn_tag='PROPN',
                 opt_words_ignore_case=False, opt_unknown_words_strategy=UnknownWordsStrategy.noun):
        print('Initiliazing MostFrequentTagger (opt_words_ignore_case=%s, opt_unknown_words_strategy=%s)' % (
            opt_words_ignore_case, opt_unknown_words_strategy))

        self._special_words = special_words
        self._noun_tag = noun_tag
        self._propn_tag = propn_tag
        self._opt_unknown_words_strategy = opt_unknown_words_strategy
        self._opt_words_ignore_case = opt_words_ignore_case

        if opt_unknown_words_strategy == UnknownWordsStrategy.morphit:
            self.unknown_words_tags = morphit_reader.load_morphit("data\\it\\morph-it.txt",
                                                                  morphit_reader.convert_tag_morphit_universal)

        if type(pos_tags) is tuple:
            pos_tags = [item for item in pos_tags]
        self.pos_tags = list([(tag) for tag in pos_tags])

        if special_words is not None:
            self._special_words = special_words
            # Add special words tags if not present
            for word, tag in special_words.items():
                if tag not in self.pos_tags:
                    self.pos_tags.append(tag)

        # WARNING: Force PROPN for Proper Nouns rule
        if self._propn_tag not in self.pos_tags:
            self.pos_tags.append(self._propn_tag)
        # WARNING: Force . for punctuation
        if '.' not in self.pos_tags:
            self.pos_tags.append('.')

        self.pos_tags_dict = collections.OrderedDict([(v, i) for i, v in enumerate(pos_tags)])
        self.corpus = corpus

        # Compute Word-Tag frequency
        self._words_tag_freq_dict = collections.OrderedDict()
        """Dictionary indexed by Words, containing dictionaries indexed by Tags with frequency of Word-Tag combination"""

        for sentence in self.corpus:
            for entry in sentence:
                word = entry[0]
                tag = entry[1]
                if self._opt_words_ignore_case:
                    word = word.lower()

                if word not in self._words_tag_freq_dict:
                    # Tags sub-dict must be created
                    self._words_tag_freq_dict[word] = collections.OrderedDict([(tag, 0) for tag in self.pos_tags])

                self._words_tag_freq_dict[word][tag] += 1

    def get_sentence_tags(self, sentence=None, words=None):
        # The strategy is to find the most frequent Tag associated
        # to each given Word, looking into the corpus.
        # If a Word is unknown the tag is assigned depending on the specified strategy

        # Tokenize sentence if needed
        if words is None:
            words = self.tokenize_sentence(sentence)

        if self._opt_words_ignore_case:
            words = [x.lower() for x in words]

        # Initialize empty tag for each word
        tags = [None for i in range(len(words))]

        i = 0
        # Tag each word
        for word in words:
            # Ignore case option ?
            if self._opt_words_ignore_case:
                word = word.lower()

            if word in self._words_tag_freq_dict:
                word_entry = self._words_tag_freq_dict[word]

                # Get most frequent tag
                tags[i] = [k for k, v in word_entry.items() if v == max(word_entry.values())][0]
            else:
                # Unknown words

                # Check if it's a special word
                if self._special_words is not None and word in self._special_words:
                    tags[i] = self._special_words[word]
                # Otherwise, apply UnknownWordsStrategy
                elif self._opt_unknown_words_strategy == UnknownWordsStrategy.noun_or_pnoun:
                    # Unknown words are defaulted to NOUN or PROPN
                    # WARNING: PROPN tag is forced during loading
                    if word.isupper():
                        tags[i] = self._propn_tag
                    else:
                        tags[i] = self._noun_tag
                elif self._opt_unknown_words_strategy == UnknownWordsStrategy.noun:
                    tags[i] = self._noun_tag
                elif self._opt_unknown_words_strategy == UnknownWordsStrategy.morphit:
                    # MorphIt smoothing
                    if word not in self.unknown_words_tags and word.lower() not in self.unknown_words_tags:
                        tags[i] = self._noun_tag
                    else:
                        if word not in self.unknown_words_tags:
                            word = word.lower()

                        word_tags = list(self.unknown_words_tags[word]['tags'].keys())
                        # Default to noun if present, or no MorphIt tags are available in current tag set
                        if self._noun_tag in word_tags or sum(
                                [(1 if t in self.pos_tags else 0) for t in word_tags]) == 0:
                            tags[i] = self._noun_tag
                        else:
                            # TODO: What tag to return ?
                            tags[i] = word_tags[0]
                else:
                    tags[i] = self.pos_tags[0]
            i += 1

        # Transform Tags into a list of indexes based on the given Tag list
        tags_indexes = [self.pos_tags.index(t) for t in tags]

        # Return the list of Words, the list of Tag indexes and a dictionary with (Word -> Tag)
        return words, tags_indexes, [list(a) for a in zip(words, tags)]

    @staticmethod
    def from_file(path, **kwargs):
        """
        Utility method to automatically build the tagger from a given corpus file.
        """
        corpus, _ = pos_tagging_utils.load_corpus(path)
        corpus_tags = pos_tagging_utils.get_corpus_tags(corpus)
        return MostFrequentTagger(corpus, corpus_tags, **kwargs)


class SmoothingStrategy(Enum):
    disabled = 0
    uniform_tags = 1
    morphit = 2


class HMMTagger(PoSTagger):
    """
    A stochastic tagger based on Hidden Markov Model.
    """


    def __init__(self, corpus, pos_tags, corpus_digest=None, opt_words_ignore_case=False,
                 opt_smoothing_strategy=SmoothingStrategy.uniform_tags):
        print('Initiliazing HMMTagger (opt_words_ignore_case=%s, opt_smoothing_strategy=%s)' % (
            opt_words_ignore_case, opt_smoothing_strategy))

        self._opt_smoothing_strategy = opt_smoothing_strategy
        self._opt_words_ignore_case = opt_words_ignore_case
        self.corpus = corpus

        if opt_smoothing_strategy == SmoothingStrategy.morphit:
            self.unknown_words_tags = morphit_reader.load_morphit("data\\it\\morph-it.txt",
                                                                  morphit_reader.convert_tag_morphit_universal)

        if type(pos_tags) is tuple:
            pos_tags = [item for item in pos_tags]
        self.pos_tags = pos_tags

        self.pos_tags_dict = collections.OrderedDict([(v, i) for i, v in enumerate(pos_tags)])
        # Train the HMM
        self.count_tag, self.prob_tag_start, self.prob_tag_cons = self.get_corpus_transition_probability()

    @staticmethod
    def from_file(path, **kwargs):
        """
        Utility method to automatically build the tagger from a given corpus file.
        """
        corpus, _ = pos_tagging_utils.load_corpus(path)
        corpus_tags = pos_tagging_utils.get_corpus_tags(corpus)
        return HMMTagger(corpus, corpus_tags, **kwargs)

    def get_corpus_transition_probability(self):
        """
        Computes the corpus transition probability (i.e. P(t_i+1|t_i))
        :param count_tag: the number of occurrences of each tag
        :param prob_tag_start:
        :param prob_tag_start:
        """

        pos_tags_len = len(self.pos_tags)

        # Occurrences of a tag
        count_tag = [0 for i in range(pos_tags_len)]

        # Occurrences of tag i after i-1: count_tag_cons[i][i-1]
        count_tag_cons = [[0 for i in range(pos_tags_len)] for i in range(pos_tags_len)]
        """The number of occurrences of a tag following another"""
        count_tag_start = [0 for i in range(pos_tags_len)]
        """The number of occurrences of a tag starting the sentence"""

        count_sentence = 0

        # Compute Sentence Count, Tag Count, Start Tag Count and Consecutive Tags Count
        for sentence in self.corpus:
            # Starting new sentence -> reset previous tag
            prevTag = None
            count_sentence += 1

            for tag in sentence:
                # Add a tag occurrence
                count_tag[self.tag_index(tag[1], self.pos_tags_dict)] += 1

                if prevTag is None:
                    # Starting new sentence -> update starting tag occurrence
                    count_tag_start[self.tag_index(tag[1], self.pos_tags_dict)] += 1
                else:
                    # Update count of tag t(i-1),t(i)
                    count_tag_cons[self.tag_index(tag[1], self.pos_tags_dict)][
                        self.tag_index(prevTag[1], self.pos_tags_dict)] += 1
                prevTag = tag

        prob_tag_start = count_tag_start[:]
        """The probability of a starting tag (i.e. P(t_i|start) = C(start, t_i)/# sentence)"""
        prob_tag_start[:] = [x / count_sentence for x in prob_tag_start]
        # Build dictionary Tag -> Start Prob
        prob_tag_start = dict([(self.pos_tags[i], prob_tag_start[i]) for i in range(0, len(self.pos_tags))])

        prob_tag_cons = count_tag_cons[:]
        """The probability of tag i following tag i-1 (i.e. P(t_i|t_i-1) = C(t_i-1,t_i)/C(t_i-1))"""
        # WARNING: Using utility method div to avoid /0 in case of tag not in corpus
        prob_tag_cons[:] = [[self.div(x, count_tag[i]) for (i, x) in enumerate(r)] for (c, r) in
                            enumerate(prob_tag_cons)]
        # Build dictionary Tag_from -> Tag_to Transition Prob
        prob_tag_cons = dict(
            [(self.pos_tags[i],
              dict([(self.pos_tags[c], prob_tag_cons[c][i]) for c in range(0, len(self.pos_tags))])) for i in
             range(0, len(self.pos_tags))]
        )

        return count_tag, prob_tag_start, prob_tag_cons

    @staticmethod
    def div(x, c):
        # P(A|B) and P(B) = 0 makes no sense -> return 0
        if c == 0:
            return 0

        return x / c

    def _get_word_with_tag_count(self, words):
        """
        Returns Words frequency and Word-Tag frequency in form of dictionary
        :param words: a list of words
        :returns:
        """

        # Dictionary indexed by Words, containing dictionaries indexed by Tags
        words_freq_dict = collections.OrderedDict([(w, 0) for i, w in enumerate(words)])
        words_tag_freq_dict = collections.OrderedDict(
            [(w, collections.OrderedDict((t, 0) for i, t in enumerate(self.pos_tags))) for i, w in enumerate(words)])

        # Compute Word Count and Word-Tag Count
        for sentence in self.corpus:
            for tag in sentence:
                if tag[0] in words_freq_dict:
                    # Ignore case option ?
                    if self._opt_words_ignore_case:
                        word = tag[0].lower()
                    else:
                        word = tag[0]

                    # Increment Word count
                    words_freq_dict[word] += 1
                    # Increment (Word -> Tag) count
                    words_tag_freq_dict[word][tag[1]] += 1

        # Return dictionaries (Word -> count), (Word -> (Tag -> count))
        return words_freq_dict, words_tag_freq_dict

    def get_corpus_emission_probability(self, words):
        """
        Return the corpus emission probability (i.e. P(w_i|t_j)) for the given sentence
        :param words: sentence as a list of words
        :returns:
        """

        if self._opt_words_ignore_case:
            words = [x.lower() for x in words]

        # Get Word Count and Word-Tag Count
        words_freq_dict, words_tag_freq_dict = self._get_word_with_tag_count(words)

        # Compute sentence's Word-Tag Probability (i.e. P(w_i|t_i) = C(t_i, w_i)/C(t_i))
        # Copy dictionary for (Word -> (Tag -> Word_Tag_Probability))
        # and convert to probability (Word_Tag_Probability = Word_Tag_Count/Tag_Count)
        prob_word_with_tag = list([
            collections.OrderedDict(
                [(tag, self.div(w_t_freq, self.count_tag[self.pos_tags_dict[tag]]))
                 for tag, w_t_freq in words_tag_freq_dict[word].items()]
            )
            for word in words])
        """A list of Word-Tag Probability, ordered for words in current sentence"""

        # VERY IMPORTANT !! Smooth unknown words
        # Current smoothing function -> uniform distribution over tags
        if self._opt_smoothing_strategy != SmoothingStrategy.disabled:
            i = 0
            for word in words:
                if words_freq_dict[word] == 0:
                    # Smoothing strategy
                    if self._opt_smoothing_strategy == SmoothingStrategy.morphit:
                        # MorphIt smoothing
                        if word not in self.unknown_words_tags and word.lower() not in self.unknown_words_tags:
                            prob_word_with_tag[i] = collections.OrderedDict(
                                [(tag, 0.1 if tag == 'NOUN' else float(0)) for tag in prob_word_with_tag[i].keys()])
                        else:
                            if word not in self.unknown_words_tags:
                                word = word.lower()

                            div = max(1, len(self.unknown_words_tags[word]['tags']))
                            prob_word_with_tag[i] = collections.OrderedDict(
                                [(tag,
                                  1 / div if tag in self.unknown_words_tags[word]['tags'] else float(0)) for tag in
                                 prob_word_with_tag[i].keys()])

                            # If all tags are null -> revert to Uniform tags smoothing
                            if sum([v for v in prob_word_with_tag[i].values()]):
                                prob_word_with_tag[i] = collections.OrderedDict(
                                    [(tag, 1.0 / len(self.pos_tags)) for tag in prob_word_with_tag[i].keys()])
                    else:
                        # Uniform tags smoothing
                        prob_word_with_tag[i] = collections.OrderedDict(
                            [(tag, 1.0 / len(self.pos_tags)) for tag in prob_word_with_tag[i].keys()])
                i += 1

        return prob_word_with_tag


    def get_sentence_tags(self, sentence=None, words=None):
        # Compute tags using Hidden Markov Model

        if words is None:
            words = self.tokenize_sentence(sentence)

        # Compute corpus Emission Probability for the current sentence
        # List of Word( Tag -> Word_Tag_Probability )
        prob_emission = self.get_corpus_emission_probability(words)

        # Adapt for input to Viterbi algorithm
        # Translate into dictionary Tag -> (Words -> Emission Prob)
        prob_emission_for_viterbi = dict(
            [(pos_tag, dict([(words[c], prob_emission[c][pos_tag]) for c in range(0, len(words))])) for pos_tag in
             self.pos_tags]
        )

        # Compute best tag sequence with Viterbi algorithm
        (prob, tags) = viterbi(words, self.pos_tags, self.prob_tag_start, self.prob_tag_cons,
                               prob_emission_for_viterbi)

        return words, tags, [list(a) for a in zip(words, tags)]


def viterbi(obs, states, start_p, trans_p, emit_p):
    """
    Returns the most probable path into the given graph, with the related probability, using Viterbi algorithm
    :param obs: a list of observations
    :param states: a list of possible states
    :param start_p: starting state probability for each state (in form of a dictionary State -> Prob)
    :param trans_p: transition probability for each state (in form of a dictionary State_from -> (State_to -> Prob))
    :param emit_p: emission probability for each (obs, state) pair (in form of a dictionary State-> (Observation -> Prob))
    :return:
    """
    V = [{}]
    path = {}
    # Initialize base cases (t == 0)
    for y in states:
        # IMPORTANT: need more decimal precision than floats !
        V[0][y] = Decimal(start_p[y]) * Decimal(emit_p[y][obs[0]])
        path[y] = [y]


    # Run Viterbi for t > 0
    for t in range(1, len(obs)):
        V.append({})
        newpath = {}

        for y in states:
            (prob, state) = max(
                (V[t - 1][y0] * Decimal(trans_p[y0][y]) * Decimal(emit_p[y][obs[t]]), y0) for y0 in states)
            V[t][y] = prob
            newpath[y] = path[state] + [y]

        # Don't need to remember the old paths
        path = newpath

    n = 0  # if only one element is observed max is sought in the initialization values
    if len(obs) != 1:
        n = t

    (prob, state) = max((V[n][y], y) for y in states)
    return prob, path[state]