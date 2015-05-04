package it.unito.nlplap.semantics.utils;

import it.unito.nlplap.semantics.utils.lemmatizer.MorphItLemmatizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class FeatureVectorUtils {

	public static StanfordCoreNLP pipeline;
	public static MorphItLemmatizer morphitLemmatizer;

	/**
	 * Stub, currently returning only normalized and lemmatized words.
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static List<String> getFeatureVector(String text) throws Exception {

		// LEMMATIZE
		List<String> lemmas = getLemmas(text);

		return lemmas;
	}

	/**
	 * Return normalized, tokenized and lemmatized words-
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static List<String> getLemmas(String text, Locale language)
			throws Exception {
		List<String> words = StopWordsTrimmer.trim(StopWordsTrimmer
				.tokenize(StopWordsTrimmer.normalize(text)));

		List<String> lemmas = new ArrayList<String>();

		if (language == Locale.ITALIAN) {
			// SnowballStemmer stemmer = (SnowballStemmer) new italianStemmer();
			//
			// for (String string : words) {
			// stemmer.setCurrent(string);
			// stemmer.stem();
			// lemmas.add(stemmer.getCurrent());
			// }
			if (morphitLemmatizer == null)
				morphitLemmatizer = new MorphItLemmatizer();

			for (String string : words) {
				if(string.equalsIgnoreCase("può"))
					string=string;
				lemmas.add(morphitLemmatizer.getLemmaString(string));
			}

		} else {
			// creates a StanfordCoreNLP object, with POS tagging,
			// lemmatization,
			// NER, parsing, and coreference resolution
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
			if (pipeline == null)
				pipeline = new StanfordCoreNLP(props);

			// read some text in the text variable
			text = StringUtils.join(words, " ").toLowerCase();

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
				// traversing the words in the current sentence
				// a CoreLabel is a CoreMap with additional token-specific
				// methods
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					// // this is the text of the token
					// String word = token.get(TextAnnotation.class);
					// // this is the POS tag of the token
					// String pos = token.get(PartOfSpeechAnnotation.class);
					// // this is the NER label of the token
					String lemma = token.get(LemmaAnnotation.class);
					lemmas.add(lemma);
				}
			}
		}

		return lemmas;
	}

	/**
	 * Return normalized, tokenized and lemmatized words- Default English
	 * locale.
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static List<String> getLemmas(String text) throws Exception {
		List<String> words = StopWordsTrimmer.trim(StopWordsTrimmer
				.tokenize(StopWordsTrimmer.normalize(text)));

		return getLemmas(text, Locale.ENGLISH);
	}
}
