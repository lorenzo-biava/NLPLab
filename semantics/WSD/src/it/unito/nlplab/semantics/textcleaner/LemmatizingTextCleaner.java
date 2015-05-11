package it.unito.nlplab.semantics.textcleaner;

import it.unito.nlplap.semantics.utils.StopWordsTrimmer;
import it.unito.nlplap.semantics.utils.lemmatizer.MorphItLemmatizer;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class LemmatizingTextCleaner implements TextCleaner {

	private Locale language;
	private StopWordsTrimmer swt;
	private StanfordCoreNLP pipeline;
	private MorphItLemmatizer morphitLemmatizer;

	public LemmatizingTextCleaner(Locale language) throws FileNotFoundException {
		this.language = language;
		this.swt = new StopWordsTrimmer(language);

		if (language == Locale.ITALIAN) {
			morphitLemmatizer = new MorphItLemmatizer();
		} else {
			// creates a StanfordCoreNLP object, with POS tagging,
			// lemmatization,
			// NER, parsing, and coreference resolution
			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
			this.pipeline = new StanfordCoreNLP(props);
		}

	}

	public String cleanText(String text) throws Exception {
		// Trim stopwords
		List<String> terms = swt.trim(StopWordsTrimmer.tokenize(swt
				.normalize(text.toLowerCase())));

		// Lemmatizing
		if (language == Locale.ITALIAN) {
			List<String> lemmas = new ArrayList<String>();
			for (String term : terms) {
				lemmas.add(morphitLemmatizer.getLemmaString(term));
			}
			terms = lemmas;
		} else
			terms = getLemmas(StringUtils.join(terms, " "));

		return StringUtils.join(terms, " ");
	}

	public List<String> getLemmas(String text) {

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
				// String pos = token.get(PartOfSpeechAnnotation.class);
				String lemma = token.get(LemmaAnnotation.class);
				lemmas.add(lemma);
			}
		}

		return lemmas;
	}

}
