__author__ = 'BLN'

from postagging import MostFrequentTagger, HMMTagger
import hashlib

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

pos_tags = ('ADJ', 'ADP', 'ADV', 'AUX', 'CONJ', 'DET', 'INTJ', 'NOUN', 'NUM', 'PART', 'PRON', 'PROPN', 'PUNCT', 'SCONJ', 'SYM', 'VERB', 'X')


#corpusPath = "E:\\Users\\bln\\Downloads\\universal_treebanks_v2.0\\universal_treebanks_v2.0\\std\\it\\it-universal-dev.conll"
corpus_path = "E:\\DATI\\UTENTI\\BLN-MAIN\\Downloads\\universal_treebanks_v2.0\\universal_treebanks_v2.0\\std\\it\\it-universal-train.conll"
corpus, corpus_digest = load_corpus(corpus_path)

sentence = "Paolo ama Francesca !"

hmm_tagger = HMMTagger(corpus, pos_tags, corpus_digest)
(words, tags_index, tags)=hmm_tagger.get_sentence_tags(sentence)
print("HMMTagger:")
print(tags)

mf_tagger = MostFrequentTagger(corpus, pos_tags)
(words, tags_index, tags)=mf_tagger.get_sentence_tags(sentence)
print("MostFreguentTagger:")
print(tags)