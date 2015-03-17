__author__ = 'BLN'

from postagging import MostFrequentTagger, HMMTagger
from postaggingutils import universal_treebank_pos_tags, load_corpus
import multiprocessing, time
import sys


def get_word_tag_list(sentence):
    tags = []
    words = []
    for line in sentence:
        tags.append((line[0], line[1]))
        words.append(line[0])

    return tags, words


def compare_sentence(tags, test_tags, bad_tags, tags_count=0, correct_tags_count=0):
    for tag in zip(tags, test_tags):
        tags_count += 1
        # Correct tags in tag[0], test_tags in tag[1]
        if tag[0][1] == tag[1][1]:
            correct_tags_count += 1
        else:
            bad_tags.append(tag)

    return tags_count, correct_tags_count


corpus_path = "E:\\DATI\\UTENTI\\BLN-MAIN\\Downloads\\universal_treebanks_v2.0\\universal_treebanks_v2.0\\std\\it\\it-universal-train.conll"
test_corpus_path = "E:\\DATI\\UTENTI\\BLN-MAIN\\Downloads\\universal_treebanks_v2.0\\universal_treebanks_v2.0\\std\\it\\it-universal-test.conll"
corpus, corpus_digest = load_corpus(corpus_path)

test_corpus, _ = load_corpus(test_corpus_path)

hmm_tagger = HMMTagger(corpus, universal_treebank_pos_tags, corpus_digest)
hmm_tagger.opt_words_smoothing = 1
hmm_tagger.opt_words_ignore_case = 0
mf_tagger = MostFrequentTagger(corpus, universal_treebank_pos_tags)
mf_tagger.opt_words_ignore_case = 0
hmm_tags_count = 0
hmm_bad_tags = []
hmm_correct_tags_count = 0
mf_tags_count = 0
mf_bad_tags = []
mf_correct_tags_count = 0

# max_sentences = sys.maxsize
# sentence_count = 0
# for sentence in test_corpus:
# sentence_count += 1
#     if sentence_count > max_sentences:
#         break
#
#     if len(sentence) > 0:
#         corpus_tags, words = get_word_tag_list(sentence)
#
#         (words, tags_index, hmm_tags) = hmm_tagger.get_sentence_tags(words=words)
#         hmm_tags_count, hmm_correct_tags_count = compare_sentence(hmm_tags, corpus_tags, hmm_bad_tags, hmm_tags_count,
#                                                                   hmm_correct_tags_count)
#
#         (words, tags_index, mf_tags) = mf_tagger.get_sentence_tags(words=words)
#         mf_tags_count, mf_correct_tags_count = compare_sentence(mf_tags, corpus_tags, mf_bad_tags, mf_tags_count,
#                                                                 mf_correct_tags_count)
#
#         print("HMMTagger:")
#         print(hmm_tags)
#         print("MostFreguentTagger:")
#         print(mf_tags)

def tagger_benchmark(results, id, tagger_name, tagger, corpus):
    tags_count = 0
    bad_tags = []
    correct_tags_count = 0

    start_time = time.time()
    PROGRESS_INTERVAL = 5
    sentence_count = 0
    corpus_len = len(corpus)
    for sentence in corpus:
        sentence_count += 1
        if len(sentence) > 0:
            corpus_tags, words = get_word_tag_list(sentence)

            (words, tags_index, tags) = tagger.get_sentence_tags(words=words)
            tags_count, correct_tags_count = compare_sentence(tags, corpus_tags, bad_tags, tags_count,
                                                              correct_tags_count)
            results[tagger_name] = (tags_count, correct_tags_count, bad_tags)
            if (time.time() - start_time >= PROGRESS_INTERVAL):
                perc = (sentence_count / corpus_len) * 100
                print('{:s}: {:d}/{:d} ({:.0f}%)'.format(tagger_name, sentence_count, corpus_len, perc))
                start_time = time.time()
                #print(tagger_name+": "+sentence_count+"/"+corpus_len+" ("+strround((sentence_count/corpus_len)*100)+"%)")
                #print(tags)

    print('{:s}: ENDED !'.format(tagger_name))
    results[id] = (tags_count, correct_tags_count, bad_tags)


if __name__ == '__main__':
    manager = multiprocessing.Manager()
    results = manager.dict()

    start_time = time.time()

    hmm_label = "HMMTagger"
    mf_label = "MFTagger"
    proc_hmm = multiprocessing.Process(target=tagger_benchmark, args=(results, 0, hmm_label, hmm_tagger, test_corpus))
    proc_mf = multiprocessing.Process(target=tagger_benchmark, args=(results, 1, mf_label, mf_tagger, test_corpus))

    proc_hmm.start()
    proc_mf.start()

    proc_hmm.join()
    proc_mf.join()

    print(results.items())
    print("HMMTagger:")
    hmm_correct_tags_count = results[0][1]
    hmm_tags_count = results[0][0]
    print(hmm_correct_tags_count / hmm_tags_count)

    print("MostFreguentTagger:")
    mf_correct_tags_count = results[1][1]
    mf_tags_count = results[1][0]
    print(mf_correct_tags_count / mf_tags_count)

    print("Total tags:")
    print(mf_tags_count)

    elapsed_time = time.time() - start_time
    print("Total time: " + str(elapsed_time)+"s")