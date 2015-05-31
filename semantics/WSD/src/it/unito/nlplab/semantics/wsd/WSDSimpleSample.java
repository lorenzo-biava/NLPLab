package it.unito.nlplab.semantics.wsd;

import it.unito.nlplab.semantics.textcleaner.LemmatizingTextCleaner;
import it.unito.nlplab.semantics.textcleaner.StemmingTextCleaner;
import it.unito.nlplab.semantics.textcleaner.StopWordOnlyTextCleaner;
import it.unito.nlplab.semantics.textcleaner.TextCleaner;
import it.unito.nlplap.semantics.utils.StopWordsTrimmer;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class WSDSimpleSample {

	private static final Logger LOG = LogManager
			.getLogger(WSDExtendedGlossSample.class);

	public static final String WORDNET_DEFAULT_DIR = "/usr/local/WordNet-3.0";

	public static void main(String[] args) throws Exception {

		String wordNetDir = System.getenv("WNHOME");
		if (wordNetDir == null)
			wordNetDir = WORDNET_DEFAULT_DIR;

		Locale language = Locale.ENGLISH;

		List<String> contexts = Arrays.asList(new String[] {
				"The house was burnt to ashes while the owner returned.",
				"This table is made of ash wood." });
		String searchWord = "ash";

		RiWordNet wn = new RiWordNet(wordNetDir);

		// TextCleaner cleaner = new DummyTextCleaner();
		TextCleaner cleaner = new StopWordOnlyTextCleaner(language);
		// TextCleaner cleaner = new StemmingTextCleaner(language);
		// TextCleaner cleaner = new LemmatizingTextCleaner(language);

		String cleanWord = cleaner.cleanText(searchWord);

		// Get words' senses

		for (String context : contexts) {

			// String pos = getPoSForLemma(searchWord, context, pipeline);
			// if (pos == null)
			// throw new RuntimeException("No pos for lemma " + searchWord);

			String pos = RiTa.getPosTags(cleanWord, true)[0];

			Sense bestSense = getBestSenseWithLeskAlgorithm(cleanWord, context,
					pos, language, wn, cleaner);

			System.out
					.println(String
							.format("Word '%s' (was '%s') (with PoS %s) has the following sense in context '%s':\n '%s'\n",
									cleanWord, searchWord, pos, context,
									bestSense));

			System.out.println("----\t\t----\n");
		}

	}

	private static Sense getBestSenseWithLeskAlgorithm(String searchWord,
			String context, String pos, Locale language, RiWordNet wn,
			TextCleaner cleaner) throws Exception {
		List<Sense> senses = getSenses(searchWord, pos, wn);

		int maxOverlap = -1;
		Sense bestSense = null;
		for (Sense sense : senses) {
			int overlap = getOverlap(sense, context, cleaner);

			LOG.info(String
					.format("[getBestSenseWithLeskAlgorithm] - word=%s, overlap=%d, sense=%s",
							searchWord, overlap, sense));

			if (overlap > maxOverlap) {
				maxOverlap = overlap;
				bestSense = sense;
			}

		}

		return bestSense;
	}

	// private static String cleanText(String text, Locale language,
	// StanfordCoreNLP pipeline) throws Exception {
	// StopWordsTrimmer swt = new StopWordsTrimmer(language);
	//
	// // Trim stopwords
	// List<String> terms = swt.trim(swt.tokenize(swt.normalize(text)));
	//
	// // Stemming
	// // for (String term : terms)
	// // term = wn.getStems(term, wn.getBestPos(term))[0];
	//
	// // Lemmatizing
	// terms = getLemmas(StringUtils.join(terms, " "), pipeline);
	//
	// return StringUtils.join(terms, " ");
	//
	// }

	private static int getOverlap(Sense sense, String context,
			TextCleaner cleaner) throws Exception {
		int overlap = 0;

		Map<String, String> senseWords = new HashMap<String, String>();
		for (String senseWord : RiTa.tokenize(cleaner.cleanText(sense
				.getGloss()))) {
			senseWords.put(senseWord, null);
		}
		for (String example : sense.getExamples()) {
			for (String exampleWord : RiTa.tokenize(cleaner.cleanText(example))) {
				senseWords.put(exampleWord, null);
			}
		}

		String cleanContext = cleaner.cleanText(context);
		for (String contextWord : RiTa.tokenize(cleanContext)) {
			if (senseWords.containsKey(contextWord))
				overlap++;
		}

		LOG.info(String.format(
				"Calculating Overlap: value=%d, context=[%s], sense=[%s]",
				overlap, cleanContext,
				StringUtils.join(senseWords.keySet(), ", ")));

		return overlap;
	}

	private static List<Sense> getSenses(String searchWord, String pos,
			RiWordNet wn) {
		List<Sense> senses = new ArrayList<Sense>();

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

	private static List<String> getLemmas(String text, StanfordCoreNLP pipeline) {

		List<String> lemmas = new ArrayList<String>();

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
				lemmas.add(lemma);
			}
		}

		return lemmas;
	}
}
