__author__ = 'BLN'

from pos_tagging import MostFrequentTagger, HMMTagger
from pos_tagging_utils import universal_treebank_pos_tags, load_corpus

corpus_path = "data\\it\\it-universal-dev.conll"
corpus, corpus_digest = load_corpus(corpus_path)

sentence = "Lo Stato dovrebbe prendere a schiaffi i corrotti, come anche i politici !"

hmm_tagger = HMMTagger(corpus, universal_treebank_pos_tags, corpus_digest)
(words, tags_index, tags)=hmm_tagger.get_sentence_tags(sentence)
print("HMMTagger:")
print(tags)

mf_tagger = MostFrequentTagger(corpus, universal_treebank_pos_tags)
(words, tags_index, tags)=mf_tagger.get_sentence_tags(sentence)
print("MostFreguentTagger:")
print(tags)