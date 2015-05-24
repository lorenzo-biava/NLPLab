import os
import logging
import nltk
from pcfg_parser import PCFGViterbiParser
import pcfg_parser
import pos_tagging
import codecs
import pickle
import pcky_parser

logging.basicConfig(level=logging.DEBUG)
#sentence = 'Una frase complessa è costituita da molte parole'
sentence = 'Ciascun Pluto può disporre del suo diritto.'
pcfg_training_set_path = "data\\it\\tut-clean-simple.penn.txt"

file = codecs.open(pcfg_training_set_path, 'r', 'utf-8')
# file = (open(pcfg_training_set_path, 'r')

pcfg_cache_file = "tmp/it.pcfg.ser"
enable_caching=True
if (enable_caching and os.path.isfile(pcfg_cache_file)):
    with open(pcfg_cache_file, 'rb') as f:
        pcfg = pickle.load(f)
else:
    pcfg = pcfg_parser.extract_pcfg(file, root='S', terminal_are_tags=False)
    with open(pcfg_cache_file, 'wb') as f:
        pickle.dump(pcfg, f)

print(pcfg.productions())

if not os.path.exists("tmp"):
    os.makedirs("tmp")

with open('tmp/it.pcfg', 'w') as outfile:
    print(pcfg.productions(), file=outfile)
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

parser = pcky_parser.PCKYParser(pcfg,None)

tokens = nltk.tokenize.wordpunct_tokenize(sentence)

_, _, tagged = pos_tagging.MostFrequentTagger.fromFile("data\\it\\it-universal-train.conll").get_sentence_tags(words=tokens)
tagged = [tuple(row) for row in tagged]
#tags = [row[1] for row in tagged]

parses = parser.nbest_parse(tokens, tagged, debug=True)

for parse in parses:
    print("Input sentence='%s'" % sentence)
    print("Tagged=%s" % tagged)
    print(parse.pprint())
    print(parse.prob())

# prob = 0
# for parse in parses:
#     tree_prob=cky_parser.treeProb(parse)
#     print("Tree probability: %g" % tree_prob)
#     print(cky_parser.extractHeadTree(parse))
#     print("")
#     if(tree_prob>prob):
#         prob=tree_prob
#         max_tree=parse

# print("")
# print("Best tree:")
# print("Tree probability: %g" % prob)
# print(cky_parser.extractHeadTree(max_tree))
# max_tree.draw()
# cky_parser.extractHeadTree(max_tree).draw()