__author__ = 'BLN'

import multiprocessing
import time

from pos_tagging import MostFrequentTagger, HMMTagger, UnknownWordsStrategy
import pos_tagging_utils


def get_word_tag_list(sentence):
    """
    Returns the list of words and the list of tags of the given sentence
    :param sentence: a sentence in form of a list of tuples (Word, Tag)
    :return:
    """
    tags = []
    words = []
    for line in sentence:
        tags.append((line[0], line[1]))
        words.append(line[0])

    return tags, words


def compare_sentence(tags, test_tags, bad_tags, tags_count=0, correct_tags_count=0):
    """
    Compares the sentence tagging output, computing tagging statistics and bad tags statistics
    :param tags: a list of gold tagged words in the form (Word, Tag)
    :param test_tags: a list of test tagged words in the form (Word, Tag)
    :param bad_tags: a list in which will be appended the test tag if incorrect
    :param tags_count: an incremental counter of tag already tested
    :param correct_tags_count: an incremental counter of tag already tested and correct
    :return:
    """
    for tag in zip(tags, test_tags):
        tags_count += 1
        # Correct tags in tag[0], test_tags in tag[1]
        if tag[0][1] == tag[1][1]:
            correct_tags_count += 1
        else:
            # if tag[0][1]==".":
            # print(".")
            bad_tags.append(tag)

    return tags_count, correct_tags_count


def tagger_benchmark(results, id, tagger_name, tagger, corpus):
    """
    Execute the benchmark process, tagging each sentence in the corpus and getting the statistics
    :param results: an array in which store the result for the current tagger id
    :param id: see results
    :param tagger_name: the name of the current tagger
    :param tagger: a tagger to execute the benchmark with
    :param corpus: a corpus in the form of a list of sentences (list of (Word, Tag))
    :return:
    """
    tags_count = 0
    bad_tags = []
    correct_tags_count = 0

    print('%s started tagging !' % tagger_name)

    start_time = time.time()
    progress_time = time.time()
    PROGRESS_INTERVAL = 5
    sentence_count = 0
    corpus_len = len(corpus)
    for sentence in corpus:
        sentence_count += 1
        if len(sentence) > 0:
            corpus_tags, words = get_word_tag_list(sentence)

            # Execute tagging
            (words, tags_index, tags) = tagger.get_sentence_tags(words=words)
            # Compare results
            tags_count, correct_tags_count = compare_sentence(corpus_tags, tags, bad_tags, tags_count,
                                                              correct_tags_count)

            # Log progress if needed
            if time.time() - progress_time >= PROGRESS_INTERVAL:
                perc = (sentence_count / corpus_len) * 100
                print('{:s}: {:d}/{:d} ({:.0f}%)'.format(tagger_name, sentence_count, corpus_len, perc))
                progress_time = time.time()
                # print(tagger_name+": "+sentence_count+"/"+corpus_len+" ("+strround((sentence_count/corpus_len)*100)+"%)")
                # print(tags)

    print('{:s}: ENDED ! Total time: {:.2f}s'.format(tagger_name, time.time() - start_time))

    # Store results for the current tagger
    results[id] = {'tags_count': tags_count, 'correct_tags_count': correct_tags_count, 'bad_tags': bad_tags}


def get_bad_tags_stats(bad_tags):
    """
    Return a frequency count for bad tags, ordered descending
    :param bad_tags: a list of tuples ((Word, CorrectTag), (Word, ComputedTag))
    :return:
    """
    stats = dict()
    for g, t in bad_tags:
        key = (str(g[1]), str(t[1]))
        if key in stats:
            stats[key] += 1
        else:
            stats[key] = 1
    return sorted(stats.items(), key=lambda e: e[1], reverse=True)


if __name__ == '__main__':

    corpus_path = "data\\it\\it-universal-train.conll"
    test_corpus_path = "data\\it\\it-universal-test.conll"

    # Execution Options
    opt_words_smoothing_enabled = True
    opt_words_ignore_case = False
    opt_unknown_words_strategy = UnknownWordsStrategy.noun
    opt_auto_tags_enabled = True
    opt_use_fine_grained_tags_enabled = False
    enable_bad_tags_stats_output = False

    if opt_use_fine_grained_tags_enabled:
        tag_field_index = 4
    else:
        tag_field_index = 3

    # Load training set
    corpus, corpus_digest = pos_tagging_utils.load_corpus(corpus_path, tag_field_index=tag_field_index)

    # Extract tags from corpus or use the complete Universal PoS tag set
    # IMPORTANT NOTE: Accuracy may vary depending by PoS tags order !
    if opt_auto_tags_enabled:
        corpus_tags = pos_tagging_utils.get_corpus_tags(corpus)
    else:
        corpus_tags = pos_tagging_utils.universal_treebank_pos_tags

    # Load test set
    test_corpus, _ = pos_tagging_utils.load_corpus(test_corpus_path, tag_field_index=tag_field_index)

    # Initialize taggers
    # HMM with options
    hmm_tagger = HMMTagger(corpus, corpus_tags, corpus_digest)
    hmm_tagger.opt_words_smoothing = opt_words_smoothing_enabled
    hmm_tagger.opt_words_ignore_case = opt_words_ignore_case
    # FM with options
    mf_tagger = MostFrequentTagger(corpus, corpus_tags)  # , noun_tag='NOUN', propn_tag='PNOUN')
    mf_tagger.opt_words_ignore_case = opt_words_ignore_case
    mf_tagger.opt_unknown_words_strategy = opt_unknown_words_strategy
    # Reset statistics
    hmm_tags_count = 0
    hmm_bad_tags = []
    hmm_correct_tags_count = 0
    mf_tags_count = 0
    mf_bad_tags = []
    mf_correct_tags_count = 0

    # Setup parallel benchmarks execution
    manager = multiprocessing.Manager()
    results = manager.dict()

    start_time = time.time()

    hmm_label = "HMMTagger"
    mf_label = "MFTagger"
    proc_hmm = multiprocessing.Process(target=tagger_benchmark, args=(results, 0, hmm_label, hmm_tagger, test_corpus))
    proc_mf = multiprocessing.Process(target=tagger_benchmark, args=(results, 1, mf_label, mf_tagger, test_corpus))

    proc_hmm.start()
    proc_mf.start()

    # Wait for processes completion
    proc_hmm.join()
    proc_mf.join()

    # Print results
    print("HMMTagger:")
    hmm_correct_tags_count = results[0]['correct_tags_count']
    hmm_tags_count = results[0]['tags_count']
    print('\tAccuracy: %.2f%%' % (hmm_correct_tags_count / hmm_tags_count * 100))
    print('\tGood tags: %d, Bad Tags: %d' % (hmm_correct_tags_count, len(results[0]['bad_tags'])))
    print('\tBad tags (correct, computed): %s' % results[0]['bad_tags'])
    bad_tags_stats = get_bad_tags_stats(results[0]['bad_tags'])
    if enable_bad_tags_stats_output:
        [print('%s|%s|%d' % (k[0], k[1], v)) for k, v in bad_tags_stats]
    else:
        print('\tBad tags stats (correct, computed): %s' % bad_tags_stats)

    print("MostFreguentTagger:")
    mf_correct_tags_count = results[1]['correct_tags_count']
    mf_tags_count = results[1]['tags_count']
    print('\tAccuracy: %.2f%%' % (mf_correct_tags_count / mf_tags_count * 100))
    print('\tGood tags: %d, Bad Tags: %d' % (mf_correct_tags_count, len(results[1]['bad_tags'])))
    print('\tBad tags (correct, computed): %s' % results[1]['bad_tags'])
    bad_tags_stats = get_bad_tags_stats(results[1]['bad_tags'])
    if enable_bad_tags_stats_output:
        [print('%s|%s|%d' % (k[0], k[1], v)) for k, v in bad_tags_stats]
    else:
        print('\tBad tags stats (correct, computed): %s' % bad_tags_stats)

    print("Total tags: %d" % mf_tags_count)

    # Print total time stats
    elapsed_time = time.time() - start_time
    print("Total time: %.2fs" % elapsed_time)