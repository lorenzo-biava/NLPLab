package it.unito.nlplap.syntax.yoddish;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

/**
 * A Yoddish translator, currently working only for English->Yoddish.
 */
public class YoddishTranslator {

	private static final Logger LOG = LogManager
			.getLogger(YoddishTranslator.class);

	private StanfordCoreNLP pipeline;

	public YoddishTranslator() {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing
		Properties props = new Properties();
		props.setProperty("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse");

		pipeline = new StanfordCoreNLP(props);
	}

	/**
	 * Translates a sentence into Yoddish.
	 * 
	 * @param text
	 *            the original sentence.
	 * @param language
	 *            the language of the sentence (<b>WARNING</b>: currently only
	 *            English is supported)
	 * @return the translated sentence
	 * @throws Exception
	 */
	public String toYoddish(String text, Locale language) throws Exception {
		if (language != Locale.ENGLISH)
			throw new IllegalArgumentException("Unsupported language '"
					+ language + "'. Only english is currently supported.");

		String yoddish = "";

		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);

		// for each sentence: get parsing tree and apply translation rules
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			Tree tree = sentence.get(TreeAnnotation.class);

			LOG.debug("Original parse: " + tree.toString());
			yoddishVisitTree(tree, 0);
			LOG.debug("Yoddish parse: " + tree.toString());
			yoddish += rebuildSentence(tree.yieldWords()) + "\n";
		}

		return yoddish;
	}

	/**
	 * Rebuilds sentence case.
	 * 
	 * @param words
	 *            the sentence in the form of a list of words.
	 * @return the rebuilt sentence.
	 */
	protected static String rebuildSentence(List<Word> words) {
		StringBuilder sentence = new StringBuilder();
		int i = 0;
		for (Word word : words) {
			if (word.word().matches("[,.:;!?]"))
				sentence.append(word.word());
			else if (i == 0)
				sentence.append(StringUtils.capitalize(word.word()));
			else
				sentence.append(" " + word.word());
			i++;
		}

		return sentence.toString();
	}

	/**
	 * Visits the parsing tree recursively and applies rules to translate to
	 * Yoddish.
	 * 
	 * @param t
	 *            the parsing tree.
	 * @param depth
	 *            the current tree depth (invoke starting with 0).
	 * @return the translated tree.
	 */
	protected static Tree yoddishVisitTree(Tree t, int depth) {
		if (t.isEmpty())
			return t;

		if (!t.isLeaf()) {
			// for each sequence of NP [*] VP -> O NP [*] VP
			try {
				Tree[] children = t.children();
				int npIndex = -1;
				for (int i = 0; i < children.length; i++) {
					if (isTag(children[i], "NP")) {
						npIndex = i;
					}

					if (isTag(children[i], "VP")) {
						if (npIndex == -1)
							if ((i != 0 || depth > 1))
								continue;
							else
								// Exception for phrase starting with VP at top
								// level
								// (i.e. sentences like 'Do not verb object')
								npIndex = 0;

						// Find the object phrase
						Tree[] op = findObjectPhraseAndRemove(children[i], t);
						if (op != null && op.length > 0) {

							// Add comma after moved Object
							CoreLabel comma = new CoreLabel();
							comma.setValue(",");
							CoreLabel l = new CoreLabel();
							l.setValue(".");
							List<Tree> childComma = new ArrayList<Tree>();
							childComma.add(new LabeledScoredTreeNode(comma));
							t.addChild(npIndex, new LabeledScoredTreeNode(l, childComma));

							// Add object phrase to current level before current
							// NP children
							for (int c = op.length - 1; c >= 0; c--)
								t.addChild(npIndex, op[c]);
						}

						// Reset for other NP-VP sequence
						npIndex = -1;
					}
				}
			} catch (Exception e) {
			}

			// Visit children
			for (Tree c : t.children())
				yoddishVisitTree(c, depth + 1);
		}

		return t;
	}

	/**
	 * Looks for the subtree corresponding to the phrase containing the object
	 * and removes it.
	 * 
	 * @param t
	 * @param parent
	 * @return
	 */
	protected static Tree[] findObjectPhraseAndRemove(Tree t, Tree parent) {
		if (t.isLeaf())
			return null;

		if (!isTag(t, "VP"))
			return null;

		// VP -> [VBx - skip] (* - object)
		List<Tree> object = new ArrayList<Tree>();
		List<Integer> toRemove = new ArrayList<Integer>();
		int i = -1;
		for (Tree c : t.children()) {
			i++;

			// Skip Verb types, if they are not the only part of the phrase and
			// following another VBx
			if (isTag(c, "VB(\\w)+"))
				if (t.children().length > 1 || !hasVBxBefore(t, parent)) {
					continue;
				}

			// Nested VP
			if (isTag(c, "VP")) {
				return findObjectPhraseAndRemove(c, t);
			}

			// Stop sentence
			if (isTag(c, "[,.!?]"))
				break;

			// Hold the following children
			object.add(c);
			toRemove.add(i);
		}

		// If no nodes remain in the tree, just return the tree and remove from
		// father
		if (object.size() == t.children().length) {
			parent.removeChild(parent.objectIndexOf(t));
			return new Tree[] { t };
		}

		// Remove selected children from current tree
		int c = 0;
		for (Integer index : toRemove) {
			t.removeChild(index - c);
			c++;
		}

		Tree[] obj = new Tree[object.size()];
		return object.toArray(obj);
	}

	protected static boolean hasVBxBefore(Tree t, Tree parent) {
		int i = parent.objectIndexOf(t);
		if (i > 0)
			return parent.getChild(i - 1).value().matches("VB(\\w)+");

		return false;
	}

	/**
	 * 
	 * @param t
	 * @param pos
	 *            regex
	 * @return
	 */
	protected static boolean isTag(Tree t, String pos) {
		if (t.isLeaf())
			return false;

		return t.label().value().matches(pos);
	}
}
