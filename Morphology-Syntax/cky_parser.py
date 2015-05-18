import collections
import nltk


def build(CNF):
    G = collections.defaultdict(list)
    for left, right in CNF:
        G[right].append(left)
    return G


def cky(G, W):
    T = [[[] for j in range(len(W) + 1)] for i in range(len(W))]
    for j in range(1, len(W) + 1):
        T[j - 1][j] += G.get(W[j - 1], [])
        print("[%d,%d]: %r" % (j - 1, j, G.get(W[j - 1])))
        for i in range(j - 2, -1, -1):
            for k in range(i + 1, j):
                for x in T[i][k]:
                    for y in T[k][j]:
                        T[i][j] += G.get((x, y), [])
                        print("[%d,%d]: %r (%s [%d,%d] and %s [%d,%d])" % (i, j, G.get((x, y)), x, i, k, y, k, j))
    return T


class CKYParser:
    # Default constructor, takes in Chomsky normalized grammar
    #
    def __init__(self, cnfGrammar):
        self._grammar = cnfGrammar

        self._productionTable = {}

        # Initialize LUT of LHS non-terminal pairs
        for production in cnfGrammar.productions():
            rhs = production.rhs()
            rhsLen = len(rhs)

            # RHS Non-production pair detected
            if rhsLen == 2:
                key = (rhs[0].symbol(), rhs[1].symbol())

                if key not in self._productionTable:
                    self._productionTable[key] = []

                self._productionTable[key].append(production)
            elif rhsLen > 2:
                print(production)
                print("ERROR: non-CNF grammar with multiple rules")

    # Returns a list of possible parses
    # Uses CKY Algorithm (see Jurasky and Martin Chapter 13.4, 2nd edition)
    #
    def nbest_parse(self, tokens):
        self._grammar.check_coverage(tokens)

        tokensCount = len(tokens);

        table = [[0 for endIndex in range(tokensCount + 1)] for startIndex in range(tokensCount)]

        # Look left-to-right
        for endIndex in range(1, tokensCount + 1):

            # Match terminals
            #
            productions = self._grammar.productions(rhs=tokens[endIndex - 1])
            trees = []
            for production in productions:
                trees.append(nltk.Tree(production, [production.rhs()]))

            table[endIndex - 1][endIndex] = trees;

            # Look bottom-to-top
            for startIndex in range(endIndex - 2, -1, -1):

                # Match productions
                #

                trees = []
                # Iterate over possible split positions
                for split in range(startIndex + 1, endIndex):
                    leftTrees = table[startIndex][split]
                    rightTrees = table[split][endIndex]

                    # Iterate through all posibile combination of trees
                    for leftTree in leftTrees:
                        for rightTree in rightTrees:
                            key = (str(leftTree.label().lhs()), str(rightTree.label().lhs()))

                            if key in self._productionTable:
                                # Iterate over possible productions
                                for production in self._productionTable[key]:
                                    trees.append(nltk.Tree(production, [leftTree, rightTree]))

                table[startIndex][endIndex] = trees;

        treesFound = []
        # Look for trees beginning with S
        for tree in table[0][len(tokens)]:
            if str(tree.label().lhs()) == "S":
                treesFound.append(tree)

        return treesFound


def treeProb(tree):
    if tree.height() <= 2:
        return tree.label().prob()

    prob = 1
    for t in tree:
        prob = prob * treeProb(t)
    return prob


def extractHeadTree(tree):
    if tree.height() <= 2:
        return nltk.Tree(tree.label(), [])

    children = list()
    for t in tree:
        children.append(extractHeadTree(t))

    return nltk.Tree(tree.label().lhs(), children)