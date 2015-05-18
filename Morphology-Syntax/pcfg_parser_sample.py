import os
import logging
import nltk
from pcfg_parser import PCFGViterbiParser
import pcfg_parser
import pos_tagging
import codecs
import pickle

logging.basicConfig(level=logging.DEBUG)
# sentence = 'Il diritto è sospeso.'
sentence = 'Ciascun panettiere può disporre del suo diritto.'
pcfg_training_set_path = "data\\it\\tut-clean-simple.penn.txt"

file = codecs.open(pcfg_training_set_path, 'r', 'utf-8')
# file = (open(pcfg_training_set_path, 'r')

pcfg_cache_file = "tmp/it.pcfg.ser"
enable_caching=True
if (enable_caching and os.path.isfile(pcfg_cache_file)):
    with open(pcfg_cache_file, 'rb') as f:
        pcfg = pickle.load(f)
else:
    pcfg = pcfg_parser.extract_pcfg(file, root='S', terminal_are_tags=True)
    with open(pcfg_cache_file, 'wb') as f:
        pickle.dump(pcfg, f)

print(pcfg.productions())

if not os.path.exists("tmp"):
    os.makedirs("tmp")

    with open('tmp/it.pcfg', 'w') as outfile:
        print(pcfg.production(), file=outfile)
#
# trees = viterbi_parser.parse(
# nltk.word_tokenize(sentence))
#
# trees = [tree for tree in trees]
# if len(trees) < 1:
#    print("FALLITO !")
#
# for tree in trees:
#    print(tree)
#    tree.draw()

import cky_parser

CNF = (
    ('S', ('NP', 'VP')),
    ('S', ('VP', 'NP')),
    ('VP', 'time'),
    ('VP', 'flies'),
    ('VP', 'like'),
    ('VP', 'arrow'),
    ('VP', ('Verb', 'NP')),
    ('VP', ('VP', 'PP')),
    ('NP', 'time'),
    ('NP', 'flies'),
    ('NP', 'arrow'),
    ('NP', ('Det', 'NP')),
    ('NP', ('Noun', 'NP')),
    ('NP', ('NP', 'PP')),
    ('PP', ('Preposition', 'NP')),
    ('Noun', 'time'),
    ('Noun', 'flies'),
    ('Noun', 'arrow'),
    ('Verb', 'time'),
    ('Verb', 'flies'),
    ('Verb', 'like'),
    ('Verb', 'arrow'),
    ('Preposition', 'like'),
    ('Det', 'an'),
)
# G = cky_parser.build(CNF)
# T = cky_parser.cky(G, ('time', 'flies', 'like', 'an', 'arrow'))
# print(T)

parser = cky_parser.CKYParser(pcfg)

tokens = nltk.tokenize.wordpunct_tokenize(sentence)

_, _, tagged = pos_tagging.MostFrequentTagger.fromFile("data\\it\\it-universal-train.conll").get_sentence_tags(words=tokens)
tokens = [tuple(row) for row in tagged]
#tokens = [row[1] for row in tagged]

parses = parser.nbest_parse(tokens)

prob = 0
for parse in parses:
    tree_prob=cky_parser.treeProb(parse)
    print("Tree probability: %g" % tree_prob)
    print(cky_parser.extractHeadTree(parse))
    print("")
    if(tree_prob>prob):
        prob=tree_prob
        max_tree=parse

print("")
print("Best tree:")
print("Tree probability: %g" % prob)
print(cky_parser.extractHeadTree(max_tree))
max_tree.draw()
cky_parser.extractHeadTree(max_tree).draw()
