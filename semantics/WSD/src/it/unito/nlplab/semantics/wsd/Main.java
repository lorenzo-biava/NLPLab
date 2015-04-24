package it.unito.nlplab.semantics.wsd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import rita.RiTa;
import rita.RiWordNet;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

public class Main {

	public static class Sense {
		private String gloss;
		private List<String> examples;

		public String getGloss() {
			return gloss;
		}

		public void setGloss(String gloss) {
			this.gloss = gloss;
		}

		public List<String> getExamples() {
			return examples;
		}

		public void setExamples(List<String> examples) {
			this.examples = examples;
		}

		@Override
		public String toString() {
			return String.format("Sense[gloss=%s, examples=%s]", gloss,
					StringUtils.join(examples, ", "));
		}
	}

	public static final String WORDNET_DEFAULT_DIR = "/usr/local/WordNet-3.0";

	public static void main(String[] args) {

		String wordNetDir = System.getenv("WNHOME");
		if (wordNetDir == null)
			wordNetDir = WORDNET_DEFAULT_DIR;

		List<String> contexts = Arrays.asList(new String[] {
				"The house was burnt to ashes while the owner returned.",
				"This table is made of ash wood." });
		String searchWord = "ash";

		// Get word's senses
		RiWordNet wn = new RiWordNet(wordNetDir);

		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
//		Properties props = new Properties();
//		props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
//		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		for (String context : contexts) {

			// String pos = getPoSForLemma(searchWord, context, pipeline);
			// if (pos == null)
			// throw new RuntimeException("No pos for lemma " + searchWord);

			String pos = RiTa.getPosTags(searchWord, true)[0];

			Sense bestSense = getBestSenseWithLeskAlgorithm(searchWord, context, pos, wn);

			System.out
					.println(String
							.format("Word '%s' (with PoS %s) has the following sense in context '%s':\n '%s'\n",
									searchWord, pos, context, bestSense));

			System.out.println("----\t\t----\n");
		}

	}

	private static Sense getBestSenseWithLeskAlgorithm(String searchWord, String context, String pos,
			RiWordNet wn) {		
		List<Sense> senses = getSenses(searchWord, pos, wn);

		int maxOverlap = 0;
		Sense bestSense = null;
		for (Sense sense : senses) {
			int overlap = getOverlap(sense, context);

			if (overlap > maxOverlap) {
				maxOverlap = overlap;
				bestSense = sense;
			}
		}
		
		return bestSense;
	}
	
	private static int getOverlap(Sense sense, String context) {
		int overlap = 0;

		Map<String, String> senseWords = new HashMap<String, String>();
		for (String senseWord : RiTa.tokenize(sense.getGloss())) {
			senseWords.put(senseWord, null);
		}
		for (String example : sense.getExamples()) {
			for (String exampleWord : RiTa.tokenize(example)) {
				senseWords.put(exampleWord, null);
			}
		}

		for (String contextWord : RiTa.tokenize(context)) {
			if (senseWords.containsKey(contextWord))
				overlap++;
		}

		return overlap;
	}

	private static List<Sense> getSenses(String searchWord, String pos,
			RiWordNet wn) {
		List<Sense> senses = new ArrayList<Main.Sense>();

		int[] senseIds = wn.getSenseIds(searchWord, pos);
		for (int id : senseIds) {
			Sense sense = new Sense();
			sense.setGloss(wn.getGloss(id));
			sense.setExamples(Arrays.asList(wn.getExamples(id)));
			senses.add(sense);
		}
		return senses;
	}

	private static String getPoSForLemma(String word, String text,
			StanfordCoreNLP pipeline) {

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and
		// has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// // this is the text of the token
				// String word = token.get(TextAnnotation.class);
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				String lemma = token.get(LemmaAnnotation.class);
				if (lemma.equals(word))
					return pos;
			}
		}

		return null;
	}
}
