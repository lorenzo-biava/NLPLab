__author__ = 'BLN'

import hashlib

universal_treebank_pos_tags = ('ADJ', 'ADP', 'ADV', 'AUX', 'CONJ', 'DET', 'INTJ', 'NOUN', 'NUM', 'PART', 'PRON', 'PROPN', 'PUNCT', 'SCONJ', 'SYM', 'VERB', 'X')

def load_corpus(path):
    sentences = [{}]
    hasher = hashlib.md5()
    BLOCKSIZE = 65536

    with open(path, 'rb') as afile:
        buf = afile.read(BLOCKSIZE)
        while len(buf) > 0:
            hasher.update(buf)
            buf = afile.read(BLOCKSIZE)

    lines = [line.strip() for line in open(path, encoding="utf-8")]
    sentence = []
    for line in lines:
        #hasher.update(line)
        if not line.strip():
            sentences.append(sentence)
            sentence = []
            continue

        #print(line)
        fields = line.split("\t")
        sentence.append((fields[1], fields[3]))

    return (sentences, hasher.hexdigest())