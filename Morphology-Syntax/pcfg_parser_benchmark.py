from nltk import Tree
from pcfg_parser import PCFGViterbiParser

def load_corpus(path):
    sentences = [{}]
    BLOCKSIZE = 65536

    with open(path, 'rb') as afile:
        buf = afile.read(BLOCKSIZE)
        while len(buf) > 0:
            buf = afile.read(BLOCKSIZE)

    lines = [line.strip() for line in open(path, encoding="utf-8")]

    return lines

pcfg_training_set_path = "E:\\PROGETTI\\Dropbox\\UNIVERSITA'\\SisCog\\LAB\\Es2\\tut-clean-simple.penn.txt"
test_corpus_path = "E:\\PROGETTI\\Dropbox\\UNIVERSITA'\\SisCog\\LAB\\Es2\\tut-clean-simple.penn.txt"

#Train parser
#viterbi_parser = PCFGViterbiParser.train(open(pcfg_training_set_path, 'r'), root='S')
#with open('tmp/benchmark.it.pcfg', 'w') as outfile:
#    print(viterbi_parser.grammar().productions(), file=outfile)

test_corpus=load_corpus(test_corpus_path)
for sentence_tree in test_corpus:

    # Extract sentence from tree notation
    t = Tree.fromstring(sentence_tree)
    sentence = t.leaves()
    print(t)
    #trees = viterbi_parser.parse(sentence)
    #nltk.word_tokenize(sentence))