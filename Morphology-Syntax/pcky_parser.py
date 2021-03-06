import re
import nltk
import nltk.treetransforms

"""
Class implementing a Probabilistic CKY parsing algorithm
"""


class PCKYParser:
    """
    Probabilistic CKY parser implementation
    """

    def __init__(self, cnf_grammar, tagger):
        """
        Default constructor
        :param cnf_grammar: Chomsky normalized grammar
        :param tagger: a PoS Tagger (must provide method tags(tokens))
        :return:
        """
        self._grammar = cnf_grammar
        self._tagger = tagger

        self._productionTable = {}

        # Initialize Look-up Table of RHS non-terminal pairs
        for production in cnf_grammar.productions():
            rhs = production.rhs()
            rhs_len = len(rhs)

            # Check CNF
            if rhs_len == 2:
                key = (rhs[0].symbol(), rhs[1].symbol())

                if key not in self._productionTable:
                    self._productionTable[key] = []

                self._productionTable[key].append(production)

            # Non CNF production detected
            elif rhs_len > 2:
                raise ValueError('FATAL: non-CNF grammar detected, rule: %s' % production)

    def get_parsing_trees(self, tokens, tagged=None, tree_head='TOP', pruning_enabled=True, debug=False):
        """
        Returns a list of possible parsing trees of the given sentence, ordered with highest probability first
        :param tokens: a list of tokens representing the sentence to parse
        :param tagged: [optional] list of (token, PoS tag) - not using the default PoS tagger
        :param tree_head:  [optional] the Non-terminal for the head of the tree
        :param debug: [optional] whether to enable debug logs
        :return:
        """

        tokens_count = len(tokens)

        # Get tags from tagger if not provided
        if tagged is None:
            tagged = self._tagger.tags(tokens)

        # Setup the table
        table = [[None for end_index in range(tokens_count + 1)] for start_index in range(tokens_count)]

        # Look left-to-right
        for end_index in range(1, tokens_count + 1):
            # Will hold found parsing trees for the current token
            trees = []

            # Match current terminal using its PoS tag (=> PoS Tag -> Token)
            token, tag = tagged[end_index - 1]
            # Build base tree: PoS Tag -> Token (with prob == 1)
            trees.append(nltk.ProbabilisticTree(tag, [token], prob=1.0))

            # Match current terminal using the grammar (=> Production.LHS -> Token)
            productions = self._grammar.productions(rhs=tokens[end_index - 1])

            # Add to current trees, the ones representing the production for the Token
            # i.e. Production.LHS -> Token (with their prob <  1)
            for production in productions:
                if str(production.lhs()) != tag:
                    trees.append(
                        nltk.ProbabilisticTree(production.lhs().symbol(), [production.rhs()], prob=production.prob()))

            # Keep current token trees into bottom-right cell (relative to current token position)
            table[end_index - 1][end_index] = trees

            if debug:
                # Print matched POS
                print(str(end_index - 1) + " " + str(end_index) + ": " + token + " ")
                for tree in trees:
                    print(tree.label() + " ")
                print()

            # Look bottom-to-top
            for start_index in range(end_index - 2, -1, -1):

                # Will hold trees with highest probability (similar to A*)
                most_likely_trees = {}

                # Iterate over possible split positions
                for split in range(start_index + 1, end_index):
                    left_trees = table[start_index][split]
                    right_trees = table[split][end_index]

                    # Iterate through all possible combination of trees to find if there is a rule in the grammar
                    # to match them (i.e. A | A -> left_tree.lhs right_tree.rhs)
                    for left_tree in left_trees:
                        for right_tree in right_trees:
                            key = (str(left_tree.label()), str(right_tree.label()))

                            if key in self._productionTable:

                                # Iterate over possible productions
                                for production in self._productionTable[key]:
                                    # Compute (A->L.lhs R.lhs) tree probability
                                    prob = left_tree.prob() * right_tree.prob() * production.prob()

                                    # Add tree to most likely pool, only if new or replace existing if higher probability
                                    existing_tree = None
                                    if production.lhs().symbol() in most_likely_trees:
                                        existing_tree = most_likely_trees[production.lhs().symbol()]

                                    if existing_tree is None or prob > existing_tree.prob():
                                        most_likely_trees[production.lhs().symbol()] = nltk.ProbabilisticTree(
                                            production.lhs().symbol(), [left_tree, right_tree],
                                            prob=prob)

                trees_to_keep = most_likely_trees.values()
                # Sort by highest probability
                trees_to_keep = sorted(trees_to_keep, key=lambda t: t.prob(), reverse=True)

                # Prune all trees except the end node
                # (we want to maximize our chances of finding the top node)
                if pruning_enabled:
                    if not (start_index == 0 and end_index == tokens_count):
                        trees_to_keep = trees_to_keep[0:20]

                # Keep the trees in the previous row
                table[start_index][end_index] = trees_to_keep

                if debug:
                    # Print the number of trees found for the given i & j
                    print(str(start_index) + " " + str(end_index) + ": " + str(len(most_likely_trees)))

        # Extract the final parsing trees from the top-right corner of the table
        final_trees = table[0][len(tokens)]

        # Keep only trees belonging to the grammar (i.e. matching the root non-terminal)

        # Create lookup table of root productions
        root_productions = {}
        for prod in self._grammar.productions():
            if prod.lhs().symbol() == tree_head:
                root_productions[prod.rhs()[0].symbol()] = prod

        trees = []
        # Match against root productions
        # (remember that trees are already sorted with highest probability first)
        for tree in final_trees:
            if tree.label() in root_productions:
                if debug:
                    # Print the trees we found
                    print("Matched root production with tree: %s" % tree)
                trees.append(tree)

        if debug:
            # Print the total number of found trees
            print("Total trees: %d" % len(trees))
        return trees

    def get_parsing_tree(self, tokens, tagged=None, tree_head='TOP', pruning_enabled=True, debug=False):
        """
        Returns the parsing tree, of the given sentence, with the highest probability
        :param tokens: a list of tokens representing the sentence to parse
        :param tagged: [optional] list of (token, PoS tag) - not using the default PoS tagger
        :param tree_head:  [optional] the Non-terminal for the head of the tree
        :param debug: [optional] whether to enable debug logs
        :return:
        """

        trees = self.get_parsing_trees(tokens, tagged, tree_head, pruning_enabled, debug)

        if len(trees) > 0:
            # Need to extract the most probable (but trees are already sorted with highest probability)
            tree = trees[0]
            # Create the tree with given root non-terminal
            most_likely_tree = nltk.tree.ProbabilisticTree(tree_head, [tree], prob=tree.prob())
            # Un-CNF the tree
            most_likely_tree.un_chomsky_normal_form()
            return most_likely_tree
        else:
            # No parsing tree found !
            return None