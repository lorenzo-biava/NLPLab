import os
import logging
import nltk
from pcfg_parser import PCFGViterbiParser
import codecs
import pickle

logging.basicConfig(level=logging.DEBUG)
# sentence = 'Le famiglie delle vittime hanno incontrato la famiglia del pilota.'
sentence = 'Ciascun partecipante può disporre del suo diritto.'
pcfg_training_set_path = "E:\\PROGETTI\\Dropbox\\UNIVERSITA'\\SisCog\\LAB\\Es2\\tut-clean-simple.penn.txt"

file = codecs.open(pcfg_training_set_path, 'r', 'utf-8')
# file = (open(pcfg_training_set_path, 'r')

pcfg_cache_file = "tmp/it.pcfg.ser"
if (os.path.isfile(pcfg_cache_file)):
    with open(pcfg_cache_file, 'rb') as f:
        pcfg = pickle.load(f)
else:
    viterbi_parser = PCFGViterbiParser.train(file, root='S')
    pcfg = viterbi_parser.grammar()
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

parses = parser.nbest_parse(tokens)

for parse in parses:
    print(cky_parser.treeProb(parse))
    print(cky_parser.extractHeadTree(parse))
