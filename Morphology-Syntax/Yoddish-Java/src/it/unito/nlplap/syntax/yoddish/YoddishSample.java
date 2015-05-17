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

public class YoddishSample {

	private static final Logger LOG = LogManager.getLogger(YoddishSample.class);

	public static void main(String[] args) throws Exception {

		String text = "I eat an apple.";
		text += " " + "Young Skywalker has become twisted by the Dark Side.";
		text += " "
				+ "The boy you trained, he is gone, consumed by Darth Vader.";
		// gone è letto come verbo e non come aggettivo
		
		text += " " + "There are always two, no more.";
		text += " " + "The mind of a child is truly wonderful.";
		text += " " + "You still have much to learn.";
		text += " "
				+ "When you reach nine hundred years old, you will not look as good.";
		text += " " + "The council does agree with you.";
		text += " " + "Skywalker will be your apprentice.";
		text += " " + "Master Obi-Wan has lost a planet.";
		text += " " + "The Clone Wars has begun.";
		text += " " + "There is no time to question.";
		text += " " + "Do not mourn them. Do not miss them.";
		Locale language = Locale.ENGLISH;

		String yoddish = toYoddish(text, language);

		LOG.info(String.format("Sentence '%s', in Yoddish is:\n%s", text,
				yoddish));
	}

	public static String toYoddish(String text, Locale language)
			throws Exception {
		if (language != Locale.ENGLISH)
			throw new IllegalArgumentException("Unsupported language '"
					+ language + "'. Only english is currently supported.");

		String yoddish = "";

		// creates a StanfordCoreNLP object, with POS tagging,
		// lemmatization,
		// NER, parsing
		Properties props = new Properties();
		props.setProperty("annotators",
				"tokenize, ssplit, pos, lemma, ner, parse");

		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys
		// and
		// has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			Tree tree = sentence.get(TreeAnnotation.class);

			yoddishVisitTree(tree, 0);
			yoddish += rebuildSentence(tree.yieldWords()) + "\n";
		}

		return yoddish;
	}

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

	protected static Tree yoddishVisitTree(Tree t, int depth) {
		if (t.isEmpty())
			return t;

		if (!t.isLeaf()) {
			// NP [*] VP -> O NP [*] VP
			try {
				Tree[] children = t.children();
				int npIndex = -1;
				// if (isTag(children[0], "NP"))
				for (int i = 0; i < children.length; i++) {
					if (isTag(children[i], "NP")) {
						npIndex = i;
					}

					if (isTag(children[i], "VP")) {
						if (npIndex == -1)
							if ((i != 0 || depth > 1))
								continue;
							else
								npIndex = 0;

						Tree[] op = findObjectPhraseAndRemove(children[i], t);
						if (op != null && op.length > 0) {
							// TODO: Add comma after Object ?
							CoreLabel l = new CoreLabel();
							l.setValue(",");
							t.addChild(npIndex, new LabeledScoredTreeNode(l));

							for (int c = op.length - 1; c >= 0; c--)
								t.addChild(npIndex, op[c]);
						}

						// Reset for other NP-VP sequence
						npIndex = -1;
					}
				}
			} catch (Exception e) {
			}

			for (Tree c : t.children())
				yoddishVisitTree(c, depth + 1);
		}

		return t;
	}

	protected static Tree[] findObjectPhraseAndRemove(Tree t, Tree parent) {
		if (t.isLeaf())
			return null;

		if (!isTag(t, "VP"))
			return null;

		// VP -> [VB* - skip] (* - object)
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
		// for(Tree c: object)
		// t.removeChild(c.objectIndexOf(t));

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
