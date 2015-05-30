import math
from random import randint
import logging
import sys
import time
import os
from subprocess import call
import nltk
import pcfg_parser_utils
import pcky_parser
import pos_tagging
import parallel_task
import pos_tagging_utils


def split_dataset(dataset, testset_ratio):
    """
    Split a dataset in training set and test set
    :param dataset:
    :param testset_ratio: between 0 and 1
    :return: the training set, the test set
    """
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
    """
    Tries to build a parsing tree for the test sentence.
    Note that this is called by the parallel_task module !
    :param i: the index of the current sentence in the task list
    :param entry: the sentence as a string
    :param kwargs: additional arguments, must contain a pos_tagger and the parser
    :return:
    """

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

    parsing_tree = kwargs['parser'].get_parsing_tree(tokens, tagged, tree_head='ROOT', debug=False)

    if parsing_tree is None:
        # logger.debug("WARNING: No parsing tree found !")
        parsing_tree = ('')
    else:
        # logger.debug("Parsing tree: ")
        # if logger.isEnabledFor(logging.DEBUG):
        # logger.debug(parse)
        # logger.debug("Probability: %g" % parse.prob())
        # logger.debug("Same tree: %s" % parse == t)
        parsing_tree = (' '.join(parsing_tree.pformat().split()))
        # if (parse == t):
        # correct += 1

    return {"index": i, "value": parsing_tree}

def create_training_test_set(dataset, testset_ratio):
    training_set, test_set = split_dataset(dataset, testset_ratio)

    # Output training set & test set
    with open('tmp\\tmp.train', 'w', encoding='utf-8') as f:
        for entry in training_set:
            f.write(entry)
            f.write('\n')

    with open('tmp\\tmp.test', 'w', encoding='utf-8') as f:
        for entry in test_set:
            f.write(entry)
            f.write('\n')

if __name__ == '__main__':

    # Execution options
    create_training_test_set_only = False
    testset_ratio = 0.1

    use_default_sets = True
    use_training_set_as_test_set = False

    use_treebank_as_tagging_corpus = True

    parallel = True
    # --- End Options ---

    logging.basicConfig(stream=sys.stdout, level=logging.INFO)
    logger = logging.getLogger('PCFG-Parser-Benchmark')

    if not os.path.exists("tmp"):
        os.makedirs("tmp")

    # Load dataset
    if use_default_sets:
        pcfg_training_set_path = "tmp\\tmp.train"
        pcfg_test_set_path = "tmp\\tmp.test"
        logger.info("Loading dataset")
        training_set = pcfg_parser_utils.load_corpus(pcfg_training_set_path)
        test_set = pcfg_parser_utils.load_corpus(pcfg_test_set_path)
        # Convert dataset tags
        # NOTE: Should NOT be already clean, otherwise it won't work !!
        training_set = pcfg_parser_utils.clean_dataset(training_set, enable_prune_tree=True,
                                                       enable_dash_rules_replace=True)
        test_set = pcfg_parser_utils.clean_dataset(test_set, enable_prune_tree=True, enable_dash_rules_replace=True)
        testset_ratio = len(test_set) / len(training_set)
        dataset = training_set + test_set
    else:
        pcfg_dataset_path = "data\\it\\tut-clean-simple.penn.txt"
        logger.info("Loading dataset")
        dataset = pcfg_parser_utils.load_corpus(pcfg_dataset_path)

        if create_training_test_set_only:
            logger.info("Creating only training & test set")
            create_training_test_set(dataset, testset_ratio)
            logger.info("Complete")
            exit()

        # Convert dataset tags
        dataset = pcfg_parser_utils.clean_dataset(dataset, enable_prune_tree=True, enable_dash_rules_replace=True)

        # Split dataset
        logger.info("Splitting dataset")
        if use_training_set_as_test_set:
            training_set = [item for item in dataset]
            test_set = [item for item in dataset]
            testset_ratio = 0
        else:
            training_set, test_set = split_dataset(dataset, testset_ratio)

    logger.info("Dataset size: %d" % len(dataset))
    logger.info("Training set size: %d" % len(training_set))
    logger.info("Test set ratio: %d%%, size: %d" % (math.floor(testset_ratio * 100), len(test_set)))

    # Extract PCFG from Training set
    logger.info("Extracting PCFG")
    pcfg = pcfg_parser_utils.extract_pcfg(training_set, 'ROOT')
    logger.debug("PCFG: %s" % pcfg.productions())
    with open('tmp\\it.pcfg', 'w') as outfile:
        print(pcfg.productions(), file=outfile)

    # Create parser
    parser = pcky_parser.PCKYParser(pcfg, None)

    logger.info("Loading PoS Tagger")

    if use_treebank_as_tagging_corpus == True:
        tagger_corpus = pcfg_parser_utils.get_tagger_corpus_from_treebank(dataset)
        corpus_tags = pos_tagging_utils.get_corpus_tags(tagger_corpus)
        pos_tagger = pos_tagging.MostFrequentTagger(tagger_corpus, corpus_tags,
                                                    special_words=pos_tagging.PoSTagger.default_special_words)
    else:
        pos_tagger = pos_tagging.MostFrequentTagger.from_file("data\\it\\it-universal-train.conll",
                                                              special_words=pos_tagging.PoSTagger.default_special_words)
        # pos_tagger = pos_tagging.HMMTagger.fromFile("data\\it\\it-universal-train.conll")

    # Test entries in Test set
    logger.info("Testing sentences")

    found = 0
    correct = 0
    test_set_size = len(test_set)

    started = time.time()

    result_set = list()
    # Parallel processing
    if parallel:
        # logger = multiprocessing.log_to_stderr()
        # logger.setLevel(multiprocessing.SUBDEBUG)
        p_tasks = parallel_task.ParallelTasks(label="PCFG", progress_interval=2)  # , processes=4)
        p_tasks.log_level(logging.INFO)
        result_set = p_tasks.apply_async_with_callback(test_set, test_sentence, chunks=10, pos_tagger=pos_tagger,
                                                       parser=parser)

    # Sequential processing
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

            parses = parser.get_parsing_tree(tokens, tagged, tree_head='ROOT', debug=False)

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

    # Output test set & result set for scoring (Evalb)
    with open('tmp\\parse.train', 'w', encoding='utf-8') as f:
        for entry in training_set:
            f.write(entry)
            f.write('\n')

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