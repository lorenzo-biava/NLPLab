__author__ = 'BLN'

import abc


class PoSTagger:
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def get_sentence_tags(self, sentence=None, words=None):
        """Returns the tags associated with the words in the sentence"""
        pass

    @staticmethod
    def tokenize_sentence(sentence):
        """

        :rtype : list(str)
        """
        return sentence.split()
        # Improve
        # return re.split(';|.|,|',sentence)

    @staticmethod
    def tag_index(tag, tags):
        if tag in tags:
            return tags[tag]
        else:  # workaround for PUNCT character
            # if tag in ('.', ','):
            return tags['PUNCT']


class MostFrequentTagger(PoSTagger):
    _opt_words_ignore_case = 0
    _special_words = dict()

    def __init__(self, corpus, pos_tags, special_words=None):
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

        self.pos_tags_dict = dict([(v, i) for i, v in enumerate(pos_tags)])
        self.corpus = corpus

    @property
    def opt_words_ignore_case(self):
        return self._opt_words_ignore_case

    @opt_words_ignore_case.setter
    def opt_words_ignore_case(self, x):
        self._opt_words_ignore_case = x

    def get_sentence_tags(self, sentence=None, words=None):
        # The strategy is to find the most frequent Tag associated
        # to each given Word, looking into the corpus.
        # If a Word is unknown a default one is assigned

        if words is None:
            words = self.tokenize_sentence(sentence)

        if self._opt_words_ignore_case:
            words = [x.lower() for x in words]

        # Dictionary indexed by Words, containing dictionaries indexed by Tags
        words_freq_dict = dict([(w, dict((t, 0) for i, t in enumerate(self.pos_tags))) for i, w in enumerate(words)])

        tags = [None for i in range(len(words))]

        for sentence in self.corpus:
            for tag in sentence:
                if tag[0] in words_freq_dict:
                    # Ignore case option ?
                    if self._opt_words_ignore_case:
                        word_entry = words_freq_dict[tag[0].lower()]
                    else:
                        word_entry = words_freq_dict[tag[0]]
                    word_entry[tag[1]] += 1

        i = 0
        for word in words:
            # Ignore case option ?
            if self._opt_words_ignore_case:
                word_entry = words_freq_dict[word.lower()]
            else:
                word_entry = words_freq_dict[word]

            # Get most frequent tag
            tags[i] = [k for k, v in word_entry.items() if v == max(word_entry.values())][0]

            # Unknown words
            if word_entry[tags[i]] == 0:
                # Check if it's a special word
                if word in self._special_words:
                    tags[i] = self._special_words[word]

                # Unknown words are defaulted to NOUN or PROPN
                # WARNING: PROPN tag is forced during loading
                elif word.isupper():
                    tags[i] = 'PROPN'
                else:
                    tags[i] = 'NOUN'

            i += 1

        # Transform Tags in a list of indexes based on the given Tag list
        tags_indexes = [self.pos_tags.index(t) for t in tags]

        # Return the list of Words, the list of Tag indexes and a dictionary with (Word -> Tag)
        return words, tags_indexes, [list(a) for a in zip(words, tags)]

    @staticmethod
    def fromFile(path, **kwargs):
        corpus, _ = pos_tagging_utils.load_corpus(path)
        corpus_tags = pos_tagging_utils.get_corpus_tags(corpus)
        return MostFrequentTagger(corpus, corpus_tags, **kwargs)


import pickle
import os.path
import pos_tagging_utils


class HMMTagger(PoSTagger):
    _opt_words_smoothing = 1
    _opt_words_ignore_case = 0

    def __init__(self, corpus, pos_tags, corpus_digest=None):
        self.corpus = corpus

        if corpus_digest is not None:
            corpus_cache_file = "tmp_corpus_" + corpus_digest

        # TODO: Restore once fixed order change during dump/load
        # Load cached corpus probabilities if existent
        # if os.path.isfile(corpus_cache_file):
        # with open(corpus_cache_file, 'rb') as f:
        # self.pos_tags = self._from_pickle_order_list(pickle.load(f))
        #         self.pos_tags_dict = dict([(v, i) for i, v in enumerate(pos_tags)])
        #         self.countTag = self._from_pickle_order_list(pickle.load(f))
        #         self.probTagStart = self._from_pickle_order_list(pickle.load(f))
        #         self.probTagCons = self._from_pickle_order_list(pickle.load(f))
        # else:
        # Calculate and then cache corpus probabilities for future reuse
        #         self.pos_tags = postaggingutils.get_corpus_tags(corpus)
        self.pos_tags = pos_tags
        self.pos_tags_dict = dict([(v, i) for i, v in enumerate(pos_tags)])
        self.countTag, self.probTagStart, self.probTagCons = self.getCorpusTransitionProbability()
        #
        #     with open(corpus_cache_file, 'wb') as f:
        #         pickle.dump(self._to_pickle_order_list(self.pos_tags), f)
        #         pickle.dump(self._to_pickle_order_list(self.countTag), f)
        #         pickle.dump(self._to_pickle_order_list(self.probTagStart), f)
        #         pickle.dump(self._to_pickle_order_list(self.probTagCons), f)

    @staticmethod
    def fromFile(path):
        corpus, _ = pos_tagging_utils.load_corpus(path)
        corpus_tags = pos_tagging_utils.get_corpus_tags(corpus)
        if 'PROPN' not in corpus_tags:
            corpus_tags.append('PROPN')
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

    def getCorpusTransitionProbability(self):

        pos_tags_len = len(self.pos_tags)

        # Occurrences of a tag
        countTag = [0 for i in range(pos_tags_len)]

        # Occurrences of tag i after i-1: countTagCons[i][i-1]
        countTagCons = [[0 for i in range(pos_tags_len)] for i in range(pos_tags_len)]
        countTagStart = [0 for i in range(pos_tags_len)]

        sentences = 0

        for sentence in self.corpus:
            # Starting new sentence -> reset previous tag
            prevTag = None
            sentences += 1

            for tag in sentence:
                # Add a tag occurrence
                countTag[self.tag_index(tag[1], self.pos_tags_dict)] += 1

                if prevTag is None:
                    # Starting new sentence -> update starting tag occurrence
                    countTagStart[self.tag_index(tag[1], self.pos_tags_dict)] += 1
                else:
                    # Update count of tag t(i-1),t(i)
                    countTagCons[self.tag_index(tag[1], self.pos_tags_dict)][
                        self.tag_index(prevTag[1], self.pos_tags_dict)] += 1
                prevTag = tag

        # return countTag, countTagStart, countTagCons
        probTagStart = countTagStart[:]
        probTagStart[:] = [x / sentences for x in probTagStart]
        # for item in probTagStart:
        # item=item/sentences

        probTagCons = countTagCons[:]
        probTagCons[:] = [[self.div(x, countTag[i]) for (i, x) in enumerate(r)] for (c, r) in enumerate(probTagCons)]
        # for row in probTagCons:
        # i = 0
        # for item in countTagCons:
        # item/=countTag[i]
        # i+=1

        # smooth tag probabilities (USELESS ?)
        # s = (1 - sum(probTagStart)) / sum([(x == 0) for x in probTagStart])
        # probTagStart = [(x if x != 0 else s) for x in probTagStart]

        return countTag, probTagStart, probTagCons

    @staticmethod
    def div(x, c):
        # P(A|B) and P(B) = 0 makes no sense
        # TODO: Check that out
        if c == 0:
            return 0

        return x / c

    @staticmethod
    def div_smooth(x, c, pos_tags_len):
        # P(A|B) and P(B) = 0 makes no sense
        # TODO: Check that out
        if c == 0:
            return 0

        # if x == 0:
        # return 1 / pos_tags_len

        return x / c

    # def tag_index(self, tag):
    # if tag in self.pos_tags:
    # return self.pos_tags.index(tag)
    # else:
    # if tag in ('.', ','):
    # return self.pos_tags.index('PUNCT')

    def _get_word_with_tag_count(self, words):
        # Dictionary indexed by Words, containing dictionaries indexed by Tags
        words_freq_dict = dict([(w, 0) for i, w in enumerate(words)])
        words_tag_freq_dict = dict(
            [(w, dict((t, 0) for i, t in enumerate(self.pos_tags))) for i, w in enumerate(words)])

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

    def getCorpusEmissionProbability(self, words):

        if self._opt_words_ignore_case:
            words = [x.lower() for x in words]

        # words_dict = dict([(v, i) for i, v in enumerate(words)])

        words_freq_dict, words_tag_freq_dict = self._get_word_with_tag_count(words)

        # |words| rows x |tags| cols
        # countWordWithTag = [[0 for i in range(len(self.countTag))] for i in range(len(words))]
        words_count = [0 for i in range(len(words))]
        # for sentence in self.corpus:
        # for tag in sentence:
        # # Word in corpus is in words
        # if tag[0] in words_dict:
        # if self._opt_words_ignore_case:
        #                 word_index = words_dict[tag[0].lower()]
        #             else:
        #                 word_index = words_dict[tag[0]]
        #
        #             words_count[word_index] += 1
        #             countWordWithTag[word_index][self.tag_index(tag[1], self.pos_tags_dict)] += 1

        #probWordWithTag = [words_freq_dict[w] for w in words]
        #probWordWithTag[:] = [[self.div_smooth(x, self.countTag[i], len(self.pos_tags)) for (i, x) in enumerate(r)] for
        #                      (c, r) in
        #                      enumerate(probWordWithTag)]

        # Copy dictionary for (Word -> (Tag -> Word_Tag_Probability))
        # and convert to probability (Word_Tag_Probability = Word_Tag_Count/Tag_Count)
        probWordWithTag = list([
            dict(
                [(tag, self.div(w_t_freq, self.countTag[self.pos_tags_dict[tag]]))
                 for tag, w_t_freq in words_tag_freq_dict[word].items()]
            )
            for word in words])

        # probWordWithTag = dict([(word,
        #                         dict(
        #                             [(tag, self.div(w_t_freq, self.countTag[self.pos_tags_dict[tag]]))
        #                              for tag, w_t_freq in w_t_f_dict.items()]
        #                         )
        #                         ) for word, w_t_f_dict in words_tag_freq_dict.items()])

        # for word, word_tag_freq_in_dict in words_tag_freq_dict:
        #     for tag, word_tag_freq in word_tag_freq_in_dict:
        #         word_tag_freq = self.div(word_tag_freq/self.countTag[self.pos_tags_dict[tag]])

        # smooth unknown words (VERY IMPORTANT !!)
        if self._opt_words_smoothing:
            i = 0
            for word in words:
                if words_freq_dict[word] == 0:
                    probWordWithTag[i] = dict([(tag, 1 / len(self.pos_tags)) for tag in probWordWithTag[i].keys()])
                i += 1

        return probWordWithTag

    def get_sentence_tags(self, sentence=None, words=None):
        if words is None:
            words = self.tokenize_sentence(sentence)

        # Array_Word( Tag -> Word_Tag_Probability )
        probEmission = self.getCorpusEmissionProbability(words)
        probEmissionForViterbi = [list(
            [f for t, f in
             sorted([(self.pos_tags_dict[k], v) for k, v in tags_dict.items()])
             ]
        ) for tags_dict in probEmission]

        w = [i for i in range(len(words))]
        t = [i for i in range(len(self.pos_tags))]

        (prob, tags) = viterbi(w, t, self.probTagStart, [list(x) for x in zip(*self.probTagCons)],
                               [list(x) for x in zip(*probEmissionForViterbi)])

        tag_values = [self.pos_tags[t] for t in tags]

        return words, tags, [list(a) for a in zip(words, tag_values)]


def viterbi(obs, states, start_p, trans_p, emit_p):
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
    # print_dptable(V)
    (prob, state) = max((V[n][y], y) for y in states)
    return (prob, path[state])


# Don't study this, it just prints a table of the steps.
def print_dptable(V):
    s = "    " + " ".join(("%7d" % i) for i in range(len(V))) + "\n"
    for y in V[0]:
        s += "%.5s: " % y
        s += " ".join("%.7s" % ("%e\t" % v[y]) for v in V)
        s += "\n"
    print(s)