import os
import logging
import nltk
from pcfg_parser import PCFGViterbiParser

logging.basicConfig(level=logging.DEBUG)
# sentence = 'Le famiglie delle vittime hanno incontrato la famiglia del pilota.'
sentence = 'Mario, lascia andare quella torta !'
pcfg_training_set_path = "E:\\PROGETTI\\Dropbox\\UNIVERSITA'\\SisCog\\LAB\\Es2\\tut-clean-simple.penn.txt"

viterbi_parser = PCFGViterbiParser.train(open(pcfg_training_set_path, 'r'), root='S')
if not os.path.exists("tmp"):
    os.makedirs("tmp")

with open('tmp/it.pcfg', 'w') as outfile:
    print(viterbi_parser.grammar().productions(), file=outfile)

trees = viterbi_parser.parse(
    nltk.word_tokenize(sentence))

trees = [tree for tree in trees]
if len(trees) < 1:
    print("FALLITO !")

for tree in trees:
    print(tree)
    tree.draw()