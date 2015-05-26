import math
from random import randint
import logging
import sys
import time
import nltk
import os
import pcfg_parser
import pcky_parser
import pos_tagging
import parallel_task
import pos_tagging_utils


def load_corpus(path):
    sentences = [{}]
    BLOCKSIZE = 65536

    with open(path, 'rb') as afile:
        buf = afile.read(BLOCKSIZE)
        while len(buf) > 0:
            buf = afile.read(BLOCKSIZE)

    lines = [line.strip() for line in open(path, encoding="utf-8")]

    return lines


def split_dataset(dataset, testset_ratio):
    training_set = dataset[:]
    test_set = list()
    test_size = math.floor(len(dataset) * testset_ratio)
    for i in range(1, test_size):
        c = randint(0, len(training_set) - 1)
        item = training_set[c]
        test_set.append(item)
        training_set.remove(item)

    return training_set, test_set


def test_sentence(i, entry, kwargs):
    # print("PCFG Sentence Test %d" % i)
    # logger = logging.getLogger('PCFG-Parser-Benchmark')
    # Extract sentence from tree notation
    t = nltk.Tree.fromstring(entry)
    tokens = t.leaves()
    _, _, tagged = kwargs['pos_tagger'].get_sentence_tags(words=tokens)
    # if not logger.isEnabledFor(logging.DEBUG):
    # if i % 10 == 0:
    # logger.info("TEST\t%d%%\t%d/%d\t-\tFound: %d\tCorrect: %d" % (
    # math.floor((i / test_set_size) * 100), i, test_set_size, found, correct))

    # logger.debug("\nTEST\t%d" % i)
    # logger.debug("Testing sentence: %s" % tokens)
    # logger.debug("Tagged: %s" % tagged)
    # logger.debug("Original tree: ")
    # if logger.isEnabledFor(logging.DEBUG):
    # logger.debug(t)

    tagged = [tuple(row) for row in tagged]

    parses = kwargs['parser'].nbest_parse(tokens, tagged, tree_head='ROOT', debug=False)

    if (len(parses) < 1):
        # logger.debug("WARNING: No parsing tree found !")
        parse = ('')
    else:
        # found += 1
        for parse in parses:
            # logger.debug("Parsing tree: ")
            # if logger.isEnabledFor(logging.DEBUG):
            # logger.debug(parse)
            # logger.debug("Probability: %g" % parse.prob())
            # logger.debug("Same tree: %s" % parse == t)
            parse = (' '.join(parse.pformat().split()))
            break
            # if (parse == t):
            # correct += 1

    return {"index": i, "value": parse}


def nltk_tree_flat_pprint(tree):
    return ' '.join(tree.pformat().split())


def clean_dataset(dataset, prune_tree=False):
    """
        Convert dataset tags
    :param dataset:
    :return:
    """
    tmp_dataset = list()
    for entry in dataset:
        entry = pcfg_parser.convert_to_universal_tags(entry)
        # WARNING !! Labeling tree ROOT (because input treebank has no label for the root, but PCKY needs one)
        entry = "(ROOT%s" % entry[1:]
        t = nltk.Tree.fromstring(entry)

        if prune_tree:
            # Prune tree (remove -NONE- subtrees)
            pcfg_parser.prune_tree(t)

        tmp_dataset.append(nltk_tree_flat_pprint(t))
    return tmp_dataset


def from_chomsky_normal_form(dataset):
    """
        De-Convert dataset from CNF !
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
        Convert dataset from CNF !
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
    corpus = []
    for entry in dataset:
        t = nltk.Tree.fromstring(entry)
        sentence = []
        for tag in t.pos():
            sentence.append(tag)
        corpus.append(sentence)
    return corpus


if __name__ == '__main__':

    # Execution options
    use_treebank_as_tagging_corpus = True
    use_training_set_as_test_set = True
    parallel = True

    logging.basicConfig(stream=sys.stdout, level=logging.INFO)
    logger = logging.getLogger('PCFG-Parser-Benchmark')

    if not os.path.exists("tmp"):
        os.makedirs("tmp")

    # Load dataset
    pcfg_dataset_path = "data\\it\\tut-clean-simple.penn.txt"
    logger.info("Loading dataset")
    dataset = load_corpus(pcfg_dataset_path)
    # Convert dataset tags
    dataset = clean_dataset(dataset, prune_tree=True)

    # Split dataset
    logger.info("Splitting dataset")
    testset_ratio = 0.1
    training_set, test_set = split_dataset(dataset, testset_ratio)
    if use_training_set_as_test_set:
        test_set = training_set

    logger.info("Dataset size: %d" % len(dataset))
    logger.info("Training set size: %d" % len(training_set))
    logger.info("Test set ratio: %d%%, size: %d" % (math.floor(testset_ratio * 100), len(test_set)))

    special_words = {'-LRB-': '-LRB-', '-RRB-': '-RRB-',
                     '-LSB-': '-LRB-', '-RSB-': '-RRB-',
                     '-': '.', ':': '.', ';': '.', '!': '.',
                     '?': '.'}

    # Extract PCFG from Training set
    logger.info("Extracting PCFG")
    pcfg = pcfg_parser.extract_pcfg(training_set, 'ROOT')
    logger.debug("PCFG: %s" % pcfg.productions())
    with open('tmp\\it.pcfg', 'w') as outfile:
        print(pcfg.productions(), file=outfile)

    # Create parser
    parser = pcky_parser.PCKYParser(pcfg, None)

    logger.info("Loading PoS Tagger")

    if use_treebank_as_tagging_corpus == True:
        tagger_corpus = get_tagger_corpus_from_treebank(dataset)
        corpus_tags = pos_tagging_utils.get_corpus_tags(tagger_corpus)
        pos_tagger = pos_tagging.MostFrequentTagger(tagger_corpus, corpus_tags, special_words=pos_tagging.PoSTagger.default_special_words)
    else:
        pos_tagger = pos_tagging.MostFrequentTagger.fromFile("data\\it\\it-universal-train.conll", special_words=pos_tagging.PoSTagger.default_special_words)
        # pos_tagger = pos_tagging.HMMTagger.fromFile("data\\it\\it-universal-train.conll")

    # Test entries in Test set
    logger.info("Testing sentences")

    found = 0
    correct = 0
    test_set_size = len(test_set)

    started = time.time()

    result_set = list()
    # Multiproc
    if parallel:
        # logger = multiprocessing.log_to_stderr()
        # logger.setLevel(multiprocessing.SUBDEBUG)
        p_tasks = parallel_task.ParallelTasks(label="PCFG", progress_interval=2)  # , processes=4)
        p_tasks.log_level(logging.INFO)
        result_set = p_tasks.apply_async_with_callback(test_set, test_sentence, chunks=10, pos_tagger=pos_tagger,
                                                       parser=parser)

    # Old
    else:
        limit = 999
        i = 0
        for entry in test_set:
            limit -= 1
            i += 1
            if (limit < 0):
                exit()

            # Extract sentence from tree notation
            t = nltk.Tree.fromstring(entry)
            tokens = t.leaves()
            _, _, tagged = pos_tagger.get_sentence_tags(words=tokens)
            if not logger.isEnabledFor(logging.DEBUG):
                if i % 10 == 0:
                    logger.info("TEST\t%d%%\t%d/%d\t-\tFound: %d\tCorrect: %d" % (
                        math.floor((i / test_set_size) * 100), i, test_set_size, found, correct))

            logger.debug("\nTEST\t%d" % i)
            logger.debug("Testing sentence: %s" % tokens)
            logger.debug("Tagged: %s" % tagged)
            logger.debug("Original tree: ")
            if logger.isEnabledFor(logging.DEBUG):
                logger.debug(t)

            tagged = [tuple(row) for row in tagged]

            parses = parser.nbest_parse(tokens, tagged, tree_head='ROOT', debug=False)

            if (len(parses) < 1):
                logger.debug("WARNING: No parsing tree found !")
                result_set.append('')
            else:
                found += 1
                for parse in parses:
                    logger.debug("Parsing tree: ")
                    if logger.isEnabledFor(logging.DEBUG):
                        logger.debug(parse)
                    logger.debug("Probability: %g" % parse.prob())
                    logger.debug("Same tree: %s" % parse == t)
                    result_set.append(' '.join(parse.pformat().split()))
                    if (parse == t):
                        correct += 1

    found = 0
    for entry in result_set:
        if entry != '':
            found += 1

    logger.info("Parse found: %d/%d (%d%%)" % (found, test_set_size, math.floor(found / test_set_size * 100)))
    logger.info("Parse correct: %d/%d (%d%%)" % (correct, test_set_size, math.floor(correct / test_set_size * 100)))

    logger.info("Work complete ! (%f s)" % (time.time() - started))

    from subprocess import call
    # Output test set & result set for scoring (Evalb)
    with open('tmp\\parse.tst', 'w', encoding='utf-8') as f:
        for entry in result_set:
            f.write(entry)
            f.write('\n')

    with open('tmp\\parse.gld', 'w', encoding='utf-8') as f:
        for entry in test_set:
            f.write(entry)
            f.write('\n')

    # Invoke Evalb
    print(call(["tools\\evalb", "-p", "tools\\default.prm", "tmp\\parse.gld", "tmp\\parse.tst"]))