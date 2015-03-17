__author__ = 'BLN'

from postagging import MostFrequentTagger, HMMTagger
from postaggingutils import universal_treebank_pos_tags, load_corpus

#corpusPath = "E:\\Users\\bln\\Downloads\\universal_treebanks_v2.0\\universal_treebanks_v2.0\\std\\it\\it-universal-dev.conll"
corpus_path = "E:\\DATI\\UTENTI\\BLN-MAIN\\Downloads\\universal_treebanks_v2.0\\universal_treebanks_v2.0\\std\\it\\it-universal-train.conll"
corpus, corpus_digest = load_corpus(corpus_path)

sentence = "Lo Stato dovrebbe prendere a schiaffi i corrotti"

hmm_tagger = HMMTagger(corpus, universal_treebank_pos_tags, corpus_digest)
(words, tags_index, tags)=hmm_tagger.get_sentence_tags(sentence)
print("HMMTagger:")
print(tags)

mf_tagger = MostFrequentTagger(corpus, universal_treebank_pos_tags)
(words, tags_index, tags)=mf_tagger.get_sentence_tags(sentence)
print("MostFreguentTagger:")
print(tags)