
states = ('Healthy', 'Fever')

observations = ('normal', 'cold', 'dizzy')

start_probability = {'Healthy': 0.6, 'Fever': 0.4}

transition_probability = {
    'Healthy': {'Healthy': 0.7, 'Fever': 0.3},
    'Fever': {'Healthy': 0.4, 'Fever': 0.6}
}

emission_probability = {
    'Healthy': {'normal': 0.5, 'cold': 0.4, 'dizzy': 0.1},
    'Fever': {'normal': 0.1, 'cold': 0.3, 'dizzy': 0.6}
}


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
    print_dptable(V)
    (prob, state) = max((V[n][y], y) for y in states)
    return (prob, path[state])


# Don't study this, it just prints a table of the steps.
def print_dptable(V):
    s = "    " + " ".join(("%7d" % i) for i in range(len(V))) + "\n"
    for y in V[0]:
        s += "%.5s: " % y
        s += " ".join("%.7s" % ("%f" % v[y]) for v in V)
        s += "\n"
    print(s)


# (prob, path)=viterbi(observations, states, start_probability, transition_probability, emission_probability)
#print(path)

def loadCorpus(path):
    sentences = [{}]

    lines = [line.strip() for line in open(path)]
    sentence = []
    for line in lines:
        if not line.strip():
            sentences.append(sentence)
            sentence = []
            continue

        #print(line)
        fields = line.split("\t")
        sentence.append((fields[1], fields[3]))

    return (sentences)


def getCorpusTransitionProbability(corpus):
    # Occurrences of a tag
    countTag = [0 for i in range(PoSTagsSize)]

    # Occurrences of tag i after i-1: countTagCons[i][i-1]
    countTagCons = [[0 for i in range(PoSTagsSize)] for i in range(PoSTagsSize)]
    countTagStart = [0 for i in range(PoSTagsSize)]

    sentences=0

    for sentence in corpus:
        # Starting new sentence -> reset previous tag
        prevTag = None
        sentences+=1

        for tag in sentence:
            # Add a tag occurrence
            countTag[tagIndex(tag[1])] += 1

            if prevTag is None:
                # Starting new sentence -> update starting tag occurrence
                countTagStart[tagIndex(tag[1])] += 1
            else:
                # Update count of tag t(i-1),t(i)
                countTagCons[tagIndex(tag[1])][tagIndex(prevTag[1])] += 1
            prevTag = tag

    #return countTag, countTagStart, countTagCons
    return countTag, countTagStart/sentences, countTagCons/countTag


def tagIndex(tag):
    if tag == '.':
        return PoSTags.PUNCT.value - 1
    else:
        return PoSTags[tag].value - 1


def getCorpusEmissionProbability(corpus, words):
    return 0


from enum import Enum
#def enum(*sequential, **named):
#    enums = dict(zip(sequential, range(len(sequential))), **named)
#    reverse = dict((value, key) for key, value in enums.iteritems())
#    enums['reverse_mapping'] = reverse
#    return type('Enum', (), enums)
#PoSTags = Enum('PoSTags', 'ADP', 'ADV', 'AUX', 'CONJ', 'DET', 'INTJ', 'NOUN', 'NUM', 'PART', 'PRON', 'PROPN', 'PUNCT', 'SCONJ', 'SYM', 'VERB', 'X')
PoSTagsSize = 17


class PoSTags(Enum):
    ADJ = 1
    ADP = 2
    ADV = 3
    AUX = 4
    CONJ = 5
    DET = 6
    INTJ = 7
    NOUN = 8
    NUM = 9
    PART = 10
    PRON = 11
    PROPN = 12
    PUNCT = 13
    SCONJ = 14
    SYM = 15
    VERB = 16
    X = 17


corpusPath = "E:\\Users\\bln\\Downloads\\universal_treebanks_v2.0\\universal_treebanks_v2.0\\std\\it\\it-universal-dev.conll"
corpus = loadCorpus(corpusPath)
print(getCorpusTransitionProbability(corpus))