__author__ = 'BLN'

import abc
import pickle
import re
import os.path
import pos_tagging_utils
import collections
from enum import Enum

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


class MostFrequentTagger(PoSTagger):
    """
    A PoS tagger based on the most frequent tag of a word.
    """

    def __init__(self, corpus, pos_tags, special_words=None):

        self._opt_words_ignore_case = 0
        self._opt_unknown_words_strategy = 1
        self._special_words = special_words

        if type(pos_tags) is tuple:
            pos_tags = [item for item in pos_tags]
        self.pos_tags = pos_tags

        if special_words is not None:
            self._special_words = special_words
            # Add special words tags if not present
            for word, tag in special_words.items():
                if tag not in self.pos_tags:
                    self.pos_tags.append(tag)

        # WARNING: Force PROPN for Proper Nouns rule
        if 'PROPN' not in self.pos_tags:
            self.pos_tags.append('PROPN')
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

    @property
    def opt_words_ignore_case(self):
        return self._opt_words_ignore_case

    @opt_words_ignore_case.setter
    def opt_words_ignore_case(self, x):
        self._opt_words_ignore_case = x

    @property
    def opt_unknown_words_strategy(self):
        return self._opt_unknown_words_strategy

    @opt_unknown_words_strategy.setter
    def opt_unknown_words_strategy(self, x):
        self._opt_unknown_words_strategy = x

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
                        tags[i] = 'PROPN'
                    else:
                        tags[i] = 'NOUN'
                elif self._opt_unknown_words_strategy == UnknownWordsStrategy.noun:
                    tags[i] = 'NOUN'
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


class HMMTagger(PoSTagger):
    """
    A stochastic tagger based on Hidden Markov Model.
    """

    def __init__(self, corpus, pos_tags, corpus_digest=None):
        self._opt_words_smoothing = 1
        self._opt_words_ignore_case = 0
        self.corpus = corpus

        if corpus_digest is not None:
            corpus_cache_file = "tmp_corpus_" + corpus_digest

        # TODO: Restore once fixed order change during dump/load
        # Load cached corpus probabilities if existent
        # if os.path.isfile(corpus_cache_file):
        # with open(corpus_cache_file, 'rb') as f:
        # self.pos_tags = self._from_pickle_order_list(pickle.load(f))
        # self.pos_tags_dict = dict([(v, i) for i, v in enumerate(pos_tags)])
        # self.countTag = self._from_pickle_order_list(pickle.load(f))
        # self.probTagStart = self._from_pickle_order_list(pickle.load(f))
        # self.probTagCons = self._from_pickle_order_list(pickle.load(f))
        # else:
        # Calculate and then cache corpus probabilities for future reuse
        # self.pos_tags = postaggingutils.get_corpus_tags(corpus)
        if type(pos_tags) is tuple:
            pos_tags = [item for item in pos_tags]
        self.pos_tags = pos_tags

        # WARNING: Force PROPN for Proper Nouns rule
        if 'PROPN' not in self.pos_tags:
            self.pos_tags.append('PROPN')
        # WARNING: Force . for .uation
        if '.' not in self.pos_tags:
            self.pos_tags.append('.')

        self.pos_tags_dict = collections.OrderedDict([(v, i) for i, v in enumerate(pos_tags)])
        self.countTag, self.probTagStart, self.probTagCons = self.get_corpus_transition_probability()
        #
        # with open(corpus_cache_file, 'wb') as f:
        #         pickle.dump(self._to_pickle_order_list(self.pos_tags), f)
        #         pickle.dump(self._to_pickle_order_list(self.countTag), f)
        #         pickle.dump(self._to_pickle_order_list(self.probTagStart), f)
        #         pickle.dump(self._to_pickle_order_list(self.probTagCons), f)

    @staticmethod
    def from_file(path):
        corpus, _ = pos_tagging_utils.load_corpus(path)
        corpus_tags = pos_tagging_utils.get_corpus_tags(corpus)
        return HMMTagger(corpus, corpus_tags)

    @staticmethod
    def _to_pickle_order_list(obj):
        return dict([(i, v) for i, v in enumerate(obj)])

    @staticmethod
    def _from_pickle_order_list(obj):
        return list([v for k, v in obj.items()])

    @property
    def opt_words_smoothing(self):
        return self._opt_words_smoothing

    @opt_words_smoothing.setter
    def opt_words_smoothing(self, x):
        self._opt_words_smoothing = x

    @property
    def opt_words_ignore_case(self):
        return self._opt_words_ignore_case

    @opt_words_ignore_case.setter
    def opt_words_ignore_case(self, x):
        self._opt_words_ignore_case = x

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

        sentences = 0

        for sentence in self.corpus:
            # Starting new sentence -> reset previous tag
            prevTag = None
            sentences += 1

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

        # return count_tag, count_tag_start, count_tag_cons
        prob_tag_start = count_tag_start[:]
        """The probability of a starting tag (i.e. P(t_i|start))"""
        prob_tag_start[:] = [x / sentences for x in prob_tag_start]
        # for item in prob_tag_start:
        # item=item/sentences

        prob_tag_cons = count_tag_cons[:]
        """The probability of tag i follows tag i-1 (i.e. P(t_i+1|t_i))"""
        prob_tag_cons[:] = [[self.div(x, count_tag[i]) for (i, x) in enumerate(r)] for (c, r) in
                            enumerate(prob_tag_cons)]
        # for row in prob_tag_cons:
        # i = 0
        # for item in count_tag_cons:
        # item/=count_tag[i]
        # i+=1

        # smooth tag probabilities (USELESS ?)
        # s = (1 - sum(prob_tag_start)) / sum([(x == 0) for x in prob_tag_start])
        # prob_tag_start = [(x if x != 0 else s) for x in prob_tag_start]

        return count_tag, prob_tag_start, prob_tag_cons

    @staticmethod
    def div(x, c):
        # P(A|B) and P(B) = 0 makes no sense
        # TODO: Check that out
        if c == 0:
            return 0

        return x / c

    # def tag_index(self, tag):
    # if tag in self.pos_tags:
    # return self.pos_tags.index(tag)
    # else:
    # if tag in ('.', ','):
    # return self.pos_tags.index('.')


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
        Return the corpus emission probability (i.e. P(w_i|t_j))
        :param words: a list of words
        :returns:
        """

        if self._opt_words_ignore_case:
            words = [x.lower() for x in words]

        # words_dict = dict([(v, i) for i, v in enumerate(words)])

        words_freq_dict, words_tag_freq_dict = self._get_word_with_tag_count(words)

        # |words| rows x |tags| cols
        # countWordWithTag = [[0 for i in range(len(self.countTag))] for i in range(len(words))]
        # words_count = [0 for i in range(len(words))]
        # for sentence in self.corpus:
        # for tag in sentence:
        # # Word in corpus is in words
        # if tag[0] in words_dict:
        # if self._opt_words_ignore_case:
        # word_index = words_dict[tag[0].lower()]
        # else:
        # word_index = words_dict[tag[0]]
        #
        # words_count[word_index] += 1
        # countWordWithTag[word_index][self.tag_index(tag[1], self.pos_tags_dict)] += 1

        # prob_word_with_tag = [words_freq_dict[w] for w in words]
        #prob_word_with_tag[:] = [[self.div_smooth(x, self.countTag[i], len(self.pos_tags)) for (i, x) in enumerate(r)] for
        #                      (c, r) in
        #                      enumerate(prob_word_with_tag)]

        # Copy dictionary for (Word -> (Tag -> Word_Tag_Probability))
        # and convert to probability (Word_Tag_Probability = Word_Tag_Count/Tag_Count)
        prob_word_with_tag = list([
            collections.OrderedDict(
                [(tag, self.div(w_t_freq, self.countTag[self.pos_tags_dict[tag]]))
                 for tag, w_t_freq in words_tag_freq_dict[word].items()]
            )
            for word in words])

        # prob_word_with_tag = dict([(word,
        #                         dict(
        #                             [(tag, self.div(w_t_freq, self.countTag[self.pos_tags_dict[tag]]))
        #                              for tag, w_t_freq in w_t_f_dict.items()]
        #                         )
        #                         ) for word, w_t_f_dict in words_tag_freq_dict.items()])

        # for word, word_tag_freq_in_dict in words_tag_freq_dict:
        #     for tag, word_tag_freq in word_tag_freq_in_dict:
        #         word_tag_freq = self.div(word_tag_freq/self.countTag[self.pos_tags_dict[tag]])

        # VERY IMPORTANT !! Smooth unknown words
        # Current smoothing function -> uniform distribution over tags
        if self._opt_words_smoothing:
            i = 0
            for word in words:
                if words_freq_dict[word] == 0:
                    prob_word_with_tag[i] = collections.OrderedDict(
                        [(tag, 1 / len(self.pos_tags)) for tag in prob_word_with_tag[i].keys()])
                i += 1

        return prob_word_with_tag

    def get_sentence_tags(self, sentence=None, words=None):
        # Compute tags using Hidden Markov Model

        if words is None:
            words = self.tokenize_sentence(sentence)

        # Array_Word( Tag -> Word_Tag_Probability )
        prob_emission = self.get_corpus_emission_probability(words)
        # Adapt for input to Viterbi algorithm
        prob_emission_for_viterbi = [list(
            [f for t, f in
             sorted([(self.pos_tags_dict[k], v) for k, v in tags_dict.items()])
             ]
        ) for tags_dict in prob_emission]

        w = [i for i in range(len(words))]
        t = [i for i in range(len(self.pos_tags))]

        (prob, tags) = viterbi(w, t, self.probTagStart, [list(x) for x in zip(*self.probTagCons)],
                               [list(x) for x in zip(*prob_emission_for_viterbi)])

        tag_values = [self.pos_tags[t] for t in tags]

        return words, tags, [list(a) for a in zip(words, tag_values)]

    @staticmethod
    def from_file(path, **kwargs):
        """
        Utility method to automatically build the tagger from a given corpus file.
        """
        corpus, _ = pos_tagging_utils.load_corpus(path)
        corpus_tags = pos_tagging_utils.get_corpus_tags(corpus)
        return HMMTagger(corpus, corpus_tags, **kwargs)

def viterbi(obs, states, start_p, trans_p, emit_p):
    """
    Returns the most probable path into the given graph, with the related probability, using Viterbi algorithm
    :param obs:
    :param states:
    :param start_p:
    :param trans_p:
    :param emit_p:
    :return:
    """
    V = [{}]
    path = {}

    # Initialize base cases (t == 0)
    for y in states:
        V[0][y] = start_p[y] * emit_p[y][obs[0]]
        path[y] = [y]

    # Run Viterbi for t > 0
    for t in range(1, len(obs)):
        V.append({})
        newpath = {}

        for y in states:
            (prob, state) = max((V[t - 1][y0] * trans_p[y0][y] * emit_p[y][obs[t]], y0) for y0 in states)
            V[t][y] = prob
            newpath[y] = path[state] + [y]

        # Don't need to remember the old paths
        path = newpath

    n = 0  # if only one element is observed max is sought in the initialization values
    if len(obs) != 1:
        n = t

    (prob, state) = max((V[n][y], y) for y in states)
    return prob, path[state]