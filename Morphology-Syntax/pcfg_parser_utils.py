__author__ = 'bln'

"""
Module containing utility methods for CFG grammar parsing
"""

import logging
import nltk

def load_corpus(path):
    """
    Read the corpus, line by line
    :param path:
    :return: a list of lines in the corpus
    """
    sentences = [line.rstrip('\n') for line in open(path, 'r', encoding='utf-8')]

    return sentences

def to_universal_tags(text):
    #universal_treebank_pos_tags = ('ADJ', 'ADP', 'ADV', 'CONJ', 'DET', 'NOUN', 'NUM', 'PRON', '.', 'VERB', 'X')
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


def prune_tree(tree):
    """
    Removes useless nodes from the tree (currently only -NONE- nodes and parent without any other children)
    :param tree:
    :return:
    """
    if tree.height() < 3:
        return tree.label() == '-NONE-'

    to_remove = list()
    for t in tree:
        if prune_tree(t):
            to_remove.append(t)

    [tree.remove(t) for t in to_remove]

    if len(tree) < 1:
        return True


def corpus2trees(content, terminal_are_tags=False, convert_to_universal_tags=False):
    """
    Parse the corpus and return a list of Trees
    :param content:
    :param terminal_are_tags: Do not use !
    :param convert_to_universal_tags:
    :return:
    """

    if not isinstance(content, list):
        if convert_to_universal_tags:
            content = to_universal_tags(content)
        raw_parses = content.split("\n")
    else:
        if (convert_to_universal_tags):
            tmp_content = list()
            for entry in content:
                if convert_to_universal_tags:
                    entry = to_universal_tags(entry)
                tmp_content.append(entry)
            content = tmp_content
        raw_parses = content

    trees = []

    for rp in raw_parses:
        if not rp.strip():
            continue

        try:
            t = nltk.Tree.fromstring(rp)
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
    """
    Transform list of Trees to a list of productions
    :param trees:
    :return:
    """
    productions = []
    for t in trees:
        productions += t.productions()
    return productions


def extract_pcfg(content, root, terminal_are_tags=False, convert_to_universal_tags=False):
    """
    Extract a Probabilistic Context-Free Grammar from the corpus
    :param content: either a file or a list of strings representing the sentences in the corpus
    :param root: the productions root Non-terminal
    :param terminal_are_tags:
    :param convert_to_universal_tags:
    :return:
    """
    if not isinstance(content, list):
        if not isinstance(content, str):
            content = content.read()

    trees = corpus2trees(content, terminal_are_tags, convert_to_universal_tags)
    productions = trees2productions(trees)
    return nltk.grammar.induce_pcfg(nltk.grammar.Nonterminal(root), productions)


def nltk_tree_flat_pprint(tree):
    """
    :param tree: nltk.Tree
    :return: the flat (inline) tree pprint
    """
    return ' '.join(tree.pformat().split())


def clean_dataset(dataset, enable_prune_tree=False):
    """
    Clean dataset, converting tags to Universal PoS tags and optionally pruning useless nodes
    :param dataset:
    :param enable_prune_tree: [optional] enable the pruning of useless nodes (for example -NONE- nodes)
    :return:
    """
    tmp_dataset = list()
    for entry in dataset:
        entry = to_universal_tags(entry)
        # WARNING !! Labeling tree ROOT (because input treebank has no label for the root, but PCKY needs one)
        entry = "(ROOT%s" % entry[1:]
        t = nltk.Tree.fromstring(entry)

        if prune_tree:
            # Prune tree (remove -NONE- subtrees)
            prune_tree(t)

        tmp_dataset.append(nltk_tree_flat_pprint(t))
    return tmp_dataset


def from_chomsky_normal_form(dataset):
    """
        De-Convert dataset from CNF
    :param dataset:
    :return:
    """
    tmp_dataset = list()
    for entry in dataset:
        t = nltk.Tree.fromstring(entry)
        t.un_chomsky_normal_form()
        tmp_dataset.append(nltk_tree_flat_pprint(t))
    return tmp_dataset


def to_chomsky_normal_form(dataset):
    """
        Convert dataset to CNF
    :param dataset:
    :return:
    """
    tmp_dataset = list()
    for entry in dataset:
        t = nltk.Tree.fromstring(entry)
        t.chomsky_normal_form()
        tmp_dataset.append(nltk_tree_flat_pprint(t))
    return tmp_dataset


def get_tagger_corpus_from_treebank(dataset):
    """
    :param dataset: a list of sentences
    :return: a list of sentences, each one is a list of tuples (token, PoS tag)
    """
    corpus = []
    for entry in dataset:
        t = nltk.Tree.fromstring(entry)
        sentence = []
        for tag in t.pos():
            sentence.append(tag)
        corpus.append(sentence)
    return corpus