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
            return tags.index(tag)
        else:
            if tag in ('.', ','):
                return tags.index('PUNCT')


class MostFrequentTagger(PoSTagger):
    def __init__(self, corpus, pos_tags):
        self.pos_tags = pos_tags
        self.corpus = corpus

    def get_sentence_tags(self, sentence=None, words=None):
        if words is None:
            words = self.tokenize_sentence(sentence)

        # freq[words x tags]
        freq = [[0 for i in range(len(self.pos_tags))] for i in range(len(words))]
        tags = [None for i in range(len(words))]

        for sentence in self.corpus:
            for tag in sentence:
                if tag[0] in words:
                    freq[words.index(tag[0])][self.tag_index(tag[1], self.pos_tags)] += 1

        i = 0
        for word_row in freq:
            tags[i] = word_row.index(max(word_row))
            i += 1

        tag_values = [self.pos_tags[t] for t in tags]

        return words, tags, [list(a) for a in zip(words, tag_values)]


import pickle
import os.path
import base64
import hashlib

class HMMTagger(PoSTagger):
    def __init__(self, corpus, pos_tags, corpus_digest=None):
        self.pos_tags = pos_tags
        self.corpus = corpus
        if corpus_digest is not None:
            corpus_cache_file = "tmp_corpus_" + corpus_digest

        # Load cached corpus probabilities if existent
        if os.path.isfile(corpus_cache_file):
            with open(corpus_cache_file, 'rb') as f:
                self.countTag = pickle.load(f)
                self.probTagStart = pickle.load(f)
                self.probTagCons = pickle.load(f)
        else:
            # Calculate and then cache corpus probabilities for future reuse
            self.countTag, self.probTagStart, self.probTagCons = self.getCorpusTransitionProbability()
            with open(corpus_cache_file, 'wb') as f:
                pickle.dump(self.countTag, f)
                pickle.dump(self.probTagStart, f)
                pickle.dump(self.probTagCons, f)

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
                countTag[self.tag_index(tag[1])] += 1

                if prevTag is None:
                    # Starting new sentence -> update starting tag occurrence
                    countTagStart[self.tag_index(tag[1])] += 1
                else:
                    # Update count of tag t(i-1),t(i)
                    countTagCons[self.tag_index(tag[1])][self.tag_index(prevTag[1])] += 1
                prevTag = tag

        # return countTag, countTagStart, countTagCons
        probTagStart = countTagStart[:]
        probTagStart[:] = [x / sentences for x in probTagStart]
        # for item in probTagStart:
        # item=item/sentences

        probTagCons = countTagCons[:]
        probTagCons[:] = [[self.div(x, countTag[i]) for (i, x) in enumerate(r)] for (c, r) in enumerate(probTagCons)]
        # for row in probTagCons:
        #     i = 0
        #     for item in countTagCons:
        #         item/=countTag[i]
        #         i+=1

        return countTag, probTagStart, probTagCons

    @staticmethod
    def div(x, c):
        return x / c if c != 0 else x / 999999

    def tag_index(self, tag):
        if tag in self.pos_tags:
            return self.pos_tags.index(tag)
        else:
            if tag in ('.', ','):
                return self.pos_tags.index('PUNCT')


    def getCorpusEmissionProbability(self, words):

        # |words| rows x |tags| cols
        countWordWithTag = [[0 for i in range(len(self.countTag))] for i in range(len(words))]
        for sentence in self.corpus:
            for tag in sentence:
                # Word in corpus is in words
                if tag[0] in words:
                    countWordWithTag[words.index(tag[0])][self.tag_index(tag[1])] += 1

        probWordWithTag = countWordWithTag[:]
        probWordWithTag[:] = [[self.div(x, self.countTag[i]) for (i, x) in enumerate(r)] for (c, r) in
                              enumerate(probWordWithTag)]

        return probWordWithTag

    def get_sentence_tags(self, sentence=None, words=None):
        if words is None:
            words = self.tokenize_sentence(sentence)

        probEmission = self.getCorpusEmissionProbability(words)

        w=[i for i in range(len(words))]
        t=[i for i in range(len(self.pos_tags))]

        (prob, tags) = viterbi(w, t, self.probTagStart, [list(x) for x in zip(*self.probTagCons)], [list(x) for x in zip(*probEmission)])

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
    #print_dptable(V)
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