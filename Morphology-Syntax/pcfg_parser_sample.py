import os
import logging
import nltk
import pcfg_parser_utils
import pos_tagging
import pickle
import pcky_parser

logging.basicConfig(level=logging.DEBUG)

enable_caching = False
pcfg_cache_file = "tmp/it.pcfg.ser"

sentence = 'Ciascun Pluto può disporre del suo diritto.'
#sentence = 'I rappresentanti della Grecia hanno trovato un accordo a livello tecnico.'

pcfg_training_set_path = "data\\it\\tut-clean-simple.penn.txt"
dataset = pcfg_parser_utils.load_corpus(pcfg_training_set_path)
dataset=pcfg_parser_utils.clean_dataset(dataset, enable_prune_tree=True)

if (enable_caching and os.path.isfile(pcfg_cache_file)):
    with open(pcfg_cache_file, 'rb') as f:
        pcfg = pickle.load(f)
else:
    pcfg = pcfg_parser_utils.extract_pcfg(dataset, root='ROOT')
    with open(pcfg_cache_file, 'wb') as f:
        pickle.dump(pcfg, f)

print(pcfg.productions())

if not os.path.exists("tmp"):
    os.makedirs("tmp")

with open('tmp/it.pcfg', 'w') as outfile:
    print(pcfg.productions(), file=outfile)


parser = pcky_parser.PCKYParser(pcfg, None)

tokens = nltk.tokenize.wordpunct_tokenize(sentence)

_, _, tagged = pos_tagging.MostFrequentTagger.from_file("data\\it\\it-universal-train.conll").get_sentence_tags(
    words=tokens)
tagged = [tuple(row) for row in tagged]

parsing_tree = parser.get_parsing_tree(tokens, tagged, tree_head='ROOT', debug=False)

if parsing_tree is not None:
    print("Input sentence: '%s'" % sentence)
    print("Tagged: %s" % tagged)
    print("Parsing tree:")
    print(parsing_tree.pprint())
    print("Parsing tree probability: %g" % parsing_tree.prob())
    parsing_tree.draw()
else:
    print("No parsing tree found !")