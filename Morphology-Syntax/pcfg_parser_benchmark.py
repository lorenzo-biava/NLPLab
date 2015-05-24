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
    #     logger.debug(t)

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
            #     logger.debug(parse)
            # logger.debug("Probability: %g" % parse.prob())
            # logger.debug("Same tree: %s" % parse == t)
            parse = (' '.join(parse.pformat().split()))
            break
            # if (parse == t):
            #     correct += 1

    return {"index": i, "value": parse}


def nltk_tree_flat_pprint(tree):
    return ' '.join(tree.pformat().split())


if __name__ == '__main__':
    logging.basicConfig(stream=sys.stdout, level=logging.INFO)
    logger = logging.getLogger('PCFG-Parser-Benchmark')

    if not os.path.exists("tmp"):
        os.makedirs("tmp")

    # Load dataset
    pcfg_dataset_path = "data\\it\\tut-clean-simple.penn.txt"
    logger.info("Loading dataset")
    dataset = load_corpus(pcfg_dataset_path)
    # Convert dataset tags
    tmp_dataset = list()
    for entry in dataset:
        entry = pcfg_parser.convert_to_universal_tags(entry)
        # WARNING !! Labeling tree ROOT (because input treebank has no label for the root, but PCKY needs one)
        entry = "(ROOT%s" % entry[1:]
        tmp_dataset.append(entry)
    dataset = tmp_dataset
    result_set = list()

    # Split dataset
    logger.info("Splitting dataset")
    testset_ratio = 0.1
    training_set, test_set = split_dataset(dataset, testset_ratio)

    # Convert test set to CNF !
    # tmp_dataset = list()
    # for entry in test_set:
    # t = nltk.Tree.fromstring(entry)
    # t.chomsky_normal_form()
    #     tmp_dataset.append(nltk_tree_flat_pprint(t))
    # test_set = tmp_dataset

    # test_set = training_set
    logger.info("Dataset size: %d" % len(dataset))
    logger.info("Training set size: %d" % len(training_set))
    logger.info("Test set ratio: %d%%, size: %d" % (math.floor(testset_ratio * 100), len(test_set)))

    # Extract PCFG from Training set
    logger.info("Extracting PCFG")
    pcfg = pcfg_parser.extract_pcfg(training_set, 'ROOT')
    logger.debug("PCFG: %s" % pcfg.productions())
    with open('tmp\\it.pcfg', 'w') as outfile:
        print(pcfg.productions(), file=outfile)

    # Create parser
    parser = pcky_parser.PCKYParser(pcfg, None)

    logger.info("Loading PoS Tagger")
    pos_tagger = pos_tagging.MostFrequentTagger.fromFile("data\\it\\it-universal-train.conll")
    #pos_tagger = pos_tagging.HMMTagger.fromFile("data\\it\\it-universal-train.conll")

    # Test entries in Test set
    logger.info("Testing sentences")

    found = 0
    correct = 0
    test_set_size = len(test_set)

    parallel = True

    started = time.time()

    # Multiproc
    if parallel:
        # logger = multiprocessing.log_to_stderr()
        # logger.setLevel(multiprocessing.SUBDEBUG)
        p_tasks = parallel_task.ParallelTasks(label="PCFG", progress_interval=2)  # , processes=4)
        p_tasks.log_level(logging.INFO)
        result_set = p_tasks.apply_async_with_callback(test_set, test_sentence, pos_tagger=pos_tagger, parser=parser)

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
    tmp = list()
    for entry in result_set:
        if entry != '':
            found += 1
            # Restore from CNF !
            t = nltk.Tree.fromstring(entry)
            t.un_chomsky_normal_form()
            tmp.append(nltk_tree_flat_pprint(t))
        else:
            tmp.append(entry)
    #result_set=tmp

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

