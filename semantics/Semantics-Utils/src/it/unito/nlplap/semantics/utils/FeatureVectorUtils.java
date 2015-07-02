package it.unito.nlplap.semantics.utils;

import it.unito.nlplap.semantics.utils.lemmatizer.MorphItLemmatizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class FeatureVectorUtils {

	public static StanfordCoreNLP pipeline;
	public static MorphItLemmatizer morphitLemmatizer;

	/**
	 * Return a feature vector for the given text.<br/>
	 * Features are the lemmas found in the text, cleaned of the stop-words.
	 * 
	 * @param text
	 * @param language
	 * @param acceptedPoS
	 *            an optional list of PoS to filter words with. If null all
	 *            words will be used. <br/>
	 *            The format depends on the language:
	 *            <ul>
	 *            <li>English: Penn TreeBank Project</li>
	 *            <li>Others: not supported !</li>
	 *            </ul>
	 * @return a Map of Lemmas, Count
	 * @throws Exception
	 */
	public static Map<String, Integer> getFeatureVector(String text,
			Locale language, List<String> acceptedPoS) throws Exception {

		StopWordsTrimmer swt = new StopWordsTrimmer(language);

		// IMPORTANT: Using LinkedHashMap to preserve order !
		Map<String, Integer> lemmaCount = new LinkedHashMap<String, Integer>();

		// Extract lemmas with count
		List<String> lemmas = getLemmas(text, language, lemmaCount,
				acceptedPoS, false, true);

		// Trim stop-words
		List<String> terms = swt.trim(lemmas);

		// IMPORTANT: Using LinkedHashMap to preserve order !
		Map<String, Integer> lemmaCountFinal = new LinkedHashMap<String, Integer>();
		for (String term : terms)
			lemmaCountFinal.put(term, lemmaCount.get(term));

		return lemmaCountFinal;
	}

	/**
	 * Return normalized, tokenized and lemmatized words-
	 * 
	 * @param text
	 * @param language
	 * @param lemmaCount
	 *            an optional map in with set the count of lemmas in the text
	 * @param acceptedPoS
	 *            an optional list of PoS to filter words with. If null all
	 *            words will be used. <br/>
	 *            The format depends on the language:
	 *            <ul>
	 *            <li>English: Penn TreeBank Project</li>
	 *            <li>Others: not supported !</li>
	 *            </ul>
	 * @return
	 * @throws Exception
	 */
	public static List<String> getLemmas(String text, Locale language,
			Map<String, Integer> lemmaCount, List<String> acceptedPoS,
			boolean removeStopWords, boolean preserveProperNounCase) throws Exception {

		Map<String, Object> goodPoS = new HashMap<String, Object>();
		if (acceptedPoS != null)
			for (String pos : acceptedPoS)
				goodPoS.put(pos, null);

		StopWordsTrimmer swt = new StopWordsTrimmer(language);
		List<String> words = StopWordsTrimmer.tokenize(swt.normalize(text));

		if (removeStopWords)
			words = swt.trim(words);

		List<String> lemmas = new ArrayList<String>();

		if (language == Locale.ITALIAN) {
			if (morphitLemmatizer == null)
				morphitLemmatizer = new MorphItLemmatizer();

			for (String string : words) {
				String lemma = morphitLemmatizer.getLemmaString(string);
				lemmas.add(lemma);
				addToLemmaCount(lemmaCount, lemma);
			}

		} else {
			// Init Stanford Pipeline if needed
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
			if (pipeline == null)
				pipeline = new StanfordCoreNLP(props);

			text = StringUtils.join(words, " ");

			// Run the pipeline on the text 
			Annotation document = new Annotation(text);
			
			pipeline.annotate(document);

			
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);

			// For each Sentence
			for (CoreMap sentence : sentences) {
				
				// For each Word
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					
					String word = token.get(TextAnnotation.class);					
					String pos = token.get(PartOfSpeechAnnotation.class);
					
					// Skip unwanted PoS
					if (acceptedPoS != null && !goodPoS.containsKey(pos))
						continue;
										
					String lemma = token.get(LemmaAnnotation.class);
					
					if (preserveProperNounCase && isProperNoun(word, pos, "Stanford"))						
						lemma = word;

					lemmas.add(lemma);
					addToLemmaCount(lemmaCount, lemma);
				}
			}
		}

		lemmas = swt.trim(lemmas);
		return lemmas;
	}

	/**
	 * Return normalized, tokenized, stop-words removed and lemmatized words.
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static List<String> getLemmas(String text, Locale language)
			throws Exception {
		return getLemmas(text, language, null, null, true, false);
	}

	/**
	 * Return normalized, tokenized, stop-words removed and lemmatized words-<br/>
	 * Defaulted to English language.
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static List<String> getLemmas(String text) throws Exception {
		return getLemmas(text, Locale.ENGLISH);
	}
	
	/* Utilities */

	protected static boolean isProperNoun(String word, String pos, String posType) {
		if (posType.equals("Stanford")) {
			boolean is=false;
			is |= pos.equals("NNP");
			is |= pos.equals("NNPS");
			//is |= pos.equals("NN") && Character.isUpperCase(word.charAt(0));
			//is |= pos.equals("NNS") && Character.isUpperCase(word.charAt(0));
			return is;
		}

		throw new IllegalArgumentException(String.format(
				"Undefined pos type %s", posType));
	}

	protected static void addToLemmaCount(Map<String, Integer> lemmaCount,
			String lemma) {
		if (lemmaCount == null)
			return;

		if (lemmaCount.containsKey(lemma))
			lemmaCount.put(lemma, lemmaCount.get(lemma) + 1);
		else
			lemmaCount.put(lemma, 1);
	}
}
