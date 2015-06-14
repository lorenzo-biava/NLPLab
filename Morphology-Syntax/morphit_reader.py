__author__ = 'BLN'

import re


def convert_tag_morphit_universal(tag):
    # 'ADJ', 'ADP', 'ADV', 'AUX', 'CONJ', 'DET', 'INTJ', 'NOUN', 'NUM', 'PART', 'PRON', 'PROPN', '.', 'SCONJ', 'SYM', 'VERB', 'X')
    replacements = {
        'ABL': '',
        # 'ADJ':'ADJ',
        'ART': 'DET',
        'ARTPRE': 'DET',
        #'AUX':'AUX',
        'CAU': 'VERB',
        'CE': 'PRON',
        'CI': 'PRON',
        'CON': 'CONJ',
        'ARTPRE': 'DET',
        #'DET':'DET',
        'INT': 'INTJ',
        'MOD': 'VERB',
        'NE': 'PRON',
        #'NOUN':'NOUN',
        'NPR': 'NOUN',  #TODO: or PROPN, depending on the dataset
        'PON': '.',
        'PRE': 'ADP',
        'PRO': 'PRON',
        'SENT': '.',
        'SI': 'PRON',
        'TALE': 'X',
        'VER': 'VERB',
        'WH': 'ADV',
    }

    for repl_old, rep_new in replacements.items():
        new_tag = tag.replace(repl_old, rep_new)

        # Apply only one substitution
        if new_tag != tag:
            tag = new_tag
            break

    return tag


def load_morphit(path, tag_converter=None):
    """
    Loads MorphIt dataset.
    :param path: the path to the MorphIt dataset.
    :return: a dictionary of Word -> ('tags' -> dict(Tags))
    """
    print('Loading Morph-it')

    lines = [line.strip() for line in open(path, encoding="utf-8")]
    words = dict()
    for line in lines:
        if not line.strip():
            continue

        fields = line.split("\t")
        word = fields[0]
        if word not in words:
            words[word] = {'tags': dict()}

        word = words[word]

        tag = fields[2]
        m = re.search('[\w]+', tag)
        tag = m.group()
        if tag_converter is not None:
            tag = tag_converter(tag)

        tags = word['tags']
        if tag not in tags:
            tags[tag] = True

    print('Loaded Morph-it')

    return words