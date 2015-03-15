__author__ = 'BLN'

from postagging import MostFrequentTagger, HMMTagger
from postaggingutils import universal_treebank_pos_tags, load_corpus


def get_word_tag_list(sentence):
    tags = []
    words = []
    for line in sentence:
        tags.append((line[0], line[1]))
        words.append(line[0])

    return tags, words


def compare_sentence(tags, test_tags, tags_count=0, correct_tags_count=0):
    for tag in zip(tags, test_tags):
        tags_count += 1
        # Correct tags in tag[0], test_tags in tag[1]
        if tag[0][1] == tag[1][1]:
            correct_tags_count += 1

    return tags_count, correct_tags_count


corpus_path = "E:\\DATI\\UTENTI\\BLN-MAIN\\Downloads\\universal_treebanks_v2.0\\universal_treebanks_v2.0\\std\\it\\it-universal-train.conll"
test_corpus_path = "E:\\DATI\\UTENTI\\BLN-MAIN\\Downloads\\universal_treebanks_v2.0\\universal_treebanks_v2.0\\std\\it\\it-universal-test.conll"
corpus, corpus_digest = load_corpus(corpus_path)

test_corpus, _ = load_corpus(test_corpus_path)

hmm_tagger = HMMTagger(corpus, universal_treebank_pos_tags, corpus_digest)
mf_tagger = MostFrequentTagger(corpus, universal_treebank_pos_tags)
hmm_tags_count=0
hmm_correct_tags_count=0
mf_tags_count=0
mf_correct_tags_count=0

for sentence in test_corpus:
    if len(sentence)>0:
        corpus_tags, words = get_word_tag_list(sentence)

        (words, tags_index, hmm_tags) = hmm_tagger.get_sentence_tags(words=words)
        hmm_tags_count, hmm_correct_tags_count = compare_sentence(hmm_tags, corpus_tags, hmm_tags_count,
                                                                  hmm_correct_tags_count)

        (words, tags_index, mf_tags) = mf_tagger.get_sentence_tags(words=words)
        mf_tags_count, mf_correct_tags_count = compare_sentence(mf_tags, corpus_tags, mf_tags_count, mf_correct_tags_count)

        print("HMMTagger:")
        print(hmm_tags)
        print("MostFreguentTagger:")
        print(mf_tags)


print("HMMTagger:")
print(hmm_correct_tags_count/hmm_tags_count)

print("MostFreguentTagger:")
print(mf_correct_tags_count/mf_tags_count)

print("Total tags:")
print(mf_tags_count)