__author__ = 'bln'

from nltk import Tree, PCFG
import logging
from postagging import MostFrequentTagger


def convert_to_universal_tags(text):
    universal_treebank_pos_tags = ('ADJ', 'ADP', 'ADV', 'CONJ', 'DET', 'NOUN', 'NUM', 'PRON', '.', 'VERB', 'X')
    replacements = {
        # ARTICOLI
        'ART': 'DET',
        # AVVERBI
        'ADVB': 'ADV',
        # VERBI
        'VAU': 'VERB', 'VCA': 'VERB', 'VMO': 'VERB', 'VMA': 'VERB',
        # NOMI
        'NOU': 'NOUN',
        # PREPOSIZIONI
        'PREP': 'ADP',
        # PRONOMI
        'PRO': 'PRON',
        # PUNCT
        'PUNCT': '.'
    }

    for repl_old, rep_new in replacements.items():
        text = text.replace('(' + repl_old, '(' + rep_new)
    return text


def corpus2trees(text):
    """ Parse the corpus and return a list of Trees """
    text = convert_to_universal_tags(text)

    rawparses = text.split("\n")
    trees = []

    for rp in rawparses:
        if not rp.strip():
            continue

        try:
            t = Tree.fromstring(rp)
            trees.append(t)
        except ValueError:
            logging.error('Malformed parse: "%s"' % rp)

    return trees


def trees2productions(trees):
    """ Transform list of Trees to a list of productions """
    productions = []
    for t in trees:
        productions += t.productions()
    return productions


import nltk

from nltk.grammar import ProbabilisticProduction, Nonterminal


class PCFGViterbiParser(nltk.ViterbiParser):
    def __init__(self, grammar, trace=0):
        super(PCFGViterbiParser, self).__init__(grammar, trace)

    @staticmethod
    def _preprocess(tokens):
        replacements = {
            "(": "-LBR-",
            ")": "-RBR-",
        }
        for idx, tok in enumerate(tokens):
            if tok in replacements:
                tokens[idx] = replacements[tok]

        return tokens

    @classmethod
    def train(cls, content, root):
        if not isinstance(content, str):
            content = content.read()

        trees = corpus2trees(content)
        productions = trees2productions(trees)
        pcfg = nltk.grammar.induce_pcfg(nltk.grammar.Nonterminal(root), productions)
        return cls(pcfg)

    def parse(self, tokens):
        tokens = self._preprocess(list(tokens))

        # MUST BE CHANGED !
        #tagged = nltk.pos_tag(tokens)
        _, _, tagged = MostFrequentTagger.fromFile("data\\it\\it-universal-train.conll").get_sentence_tags(words=tokens)
        tagged = [tuple(row) for row in tagged]
        logging.debug(tagged)

        missing = False
        for tok, pos in tagged:
            if not self._grammar._lexical_index.get(tok):
                missing = True
                self._grammar._productions.append(ProbabilisticProduction(Nonterminal(pos), [tok],
                                                                          prob=0.000001))
        if missing:
            self._grammar._calculate_indexes()

        return super(PCFGViterbiParser, self).parse(tokens)


logging.basicConfig(level=logging.DEBUG)
pcfg_training_set_path = "E:\\PROGETTI\\Dropbox\\UNIVERSITA'\\SisCog\\LAB\\Es2\\tut-clean-simple.penn.txt"

viterbi_parser = PCFGViterbiParser.train(open(pcfg_training_set_path, 'r'), root='S')
with open('it.pcfg', 'w') as outfile:
    print(viterbi_parser.grammar().productions(), file=outfile)

trees = viterbi_parser.parse(
    nltk.word_tokenize('Le famiglie delle vittime hanno incontrato la famiglia del pilota.'))

trees = [tree for tree in trees]
if len(trees) < 1:
    print("FALLITO !")

for tree in trees:
    print(tree)
    tree.draw()