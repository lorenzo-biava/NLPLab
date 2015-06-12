__author__ = 'BLN'

import hashlib

universal_treebank_pos_tags = (
    'ADJ', 'ADP', 'ADV', 'AUX', 'CONJ', 'DET', 'INTJ', 'NOUN', 'NUM', 'PART', 'PRON', 'PROPN', '.', 'SCONJ', 'SYM',
    'VERB',
    'X')
# universal_treebank_pos_tags = ('ADJ', 'ADP', 'ADV', 'CONJ', 'DET', 'NOUN', 'NUM', 'PRON', '.', 'VERB', 'X')


def load_corpus(path, word_field_index=1, tag_field_index=3):
    """
    Loads the corpus in form of a list of sentences (list of (Word, Tag)),
    also calculating the check-sum for caching purpose
    :param path: the path to the corpus
    :param word_field_index: the index of the column to use to extract the Word
    :param tag_field_index: the index of the column to use to extract the Tag
    :return:
    """
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
        if not line.strip():
            sentences.append(sentence)
            sentence = []
            continue

        # print(line)
        fields = line.split("\t")
        sentence.append((fields[word_field_index], fields[tag_field_index]))

    return sentences, hasher.hexdigest()


def get_corpus_tags(corpus):
    """
    Extracts the tags list from the corpus
    :param corpus: the corpus in the form of list of sentences (list of (Word, Tag))
    :return:
    """
    tags = dict()

    for sentence in corpus:
        for line in sentence:
            if line[1] not in tags:
                tags[line[1]] = 0

    return list([i for i in tags])