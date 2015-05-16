package it.unito.nlplap.syntax.yoddish;

import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
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

		LOG.info(String.format("Sentence '%s', in Yoddish is:\n\t%s", text,
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
			yoddish += StringUtils.join(tree.yieldWords(), " ") + "\n";
		}

		return yoddish;
	}

	protected static Tree yoddishVisitTree(Tree t, int depth) {
		if (t.isEmpty())
			return t;

		String tabs = "";
		for (int i = 0; i < depth; i++)
			tabs += "\t";

		if (t.isLeaf()) {
			// System.out.println(tabs + t.nodeString());
		} else {
			// System.out.println(tabs + t.label());

			try {
				Tree[] children = t.children();
				// if (isTag(children[0], "NP"))
				for (int i = 0; i < children.length; i++)
					if (isTag(children[i], "VP")) {
						Tree op = findObjectPhraseAndRemove(children[1]);
						if (op != null)
							t.addChild(0, op);
					}
			} catch (Exception e) {
				e = e;
			}

			for (Tree c : t.children())
				yoddishVisitTree(c, depth + 1);
		}

		return t;
	}

	protected static Tree findObjectPhraseAndRemove(Tree t) {
		if (t.isLeaf())
			return null;

		if (!isTag(t, "VP"))
			return null;

		int i = 0;
		for (Tree c : t.children()) {
			if (isTag(c, "NP")) {
				t.removeChild(i);
				return c;
			}
			i++;
		}

		return t;
	}

	protected static boolean isTag(Tree t, String pos) {
		if (t.isLeaf())
			return false;

		return t.label().value().equals(pos);
	}
}
