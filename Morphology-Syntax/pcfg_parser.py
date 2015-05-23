__author__ = 'bln'

from nltk import Tree, PCFG
import logging
from pos_tagging import MostFrequentTagger


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
        # NUMERI
        'NUMR': 'NUM',
        # PUNCT
        'PUNCT': '.'
    }

    for repl_old, rep_new in replacements.items():
        text = text.replace('(' + repl_old, '(' + rep_new)
    return text


def corpus2trees(content, terminal_are_tags=False):
    """ Parse the corpus and return a list of Trees """

    if not isinstance(content, list):
        content = convert_to_universal_tags(content)
        raw_parses = content.split("\n")
    else:
        for entry in content:
            entry = convert_to_universal_tags(entry)
        raw_parses = content

    trees = []

    for rp in raw_parses:
        if not rp.strip():
            continue

        try:
            t = Tree.fromstring(rp)
            if terminal_are_tags:
                remove_word_terminals(t)
            t.chomsky_normal_form()
            trees.append(t)
        except ValueError:
            logging.error('Malformed parse: "%s"' % rp)

    return trees


def remove_word_terminals(tree):
    i = -1
    for t in tree:
        i += 1
        if (t.height() >= 3):
            remove_word_terminals(t)
        else:
            t2 = t[0]  # t.label()
            t.clear()
            t.insert(0, t.label())


def trees2productions(trees):
    """ Transform list of Trees to a list of productions """
    productions = []
    for t in trees:
        productions += t.productions()
    return productions


def extract_pcfg(content, root, terminal_are_tags=False):
    if not isinstance(content, list):
        if not isinstance(content, str):
            content = content.read()

    trees = corpus2trees(content, terminal_are_tags)
    productions = trees2productions(trees)
    return nltk.grammar.induce_pcfg(nltk.grammar.Nonterminal(root), productions)


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
        # tagged = nltk.pos_tag(tokens)
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

