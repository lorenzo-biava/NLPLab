package it.unito.nlplap.semantics.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class StopWordsTrimmer {

	public static final String DEFAULT_DATASET_FILE = "stop_words_EN.txt";

	private Locale language;
	private String datasetPath;
	private Map<String, String> stopwords;

	public StopWordsTrimmer() {
		this.language = Locale.ENGLISH;
		datasetPath = DEFAULT_DATASET_FILE;
	}

	public StopWordsTrimmer(Locale language) {
		this.language = language;
		if (language == Locale.ITALIAN)
			datasetPath = "stop_words_IT.txt";
		else
			datasetPath = DEFAULT_DATASET_FILE;
	}

	/**
	 * Remove all chars except letters (replace with space) <b>in a text</b>.
	 * 
	 * @param text
	 * @return
	 */
	public String normalize(String text) {
		if (language == Locale.ITALIAN)
			return text.replaceAll("[^A-Za-zàèéìòù ]", " ");
		else
			return text.replaceAll("[^A-Za-z' ]", " ");
	}

	/**
	 * Remove all chars except letters <b>in a word</b>.
	 * 
	 * @param text
	 * @return
	 */
	public String normalizeWord(String text) {
		if (language == Locale.ITALIAN)
			return text.replaceAll("[^A-Za-zàèéìòù]", "");
		else
			return text.replaceAll("[^A-Za-z'\\-@.]", "");
	}

	/**
	 * Split a sentence in words by spaces.
	 * 
	 * @param text
	 * @return
	 */
	public static List<String> tokenize(String text) {
		return Arrays.asList(text.split(" "));
	}

	/**
	 * Split a sentence in words by spaces, commas, apex.
	 * 
	 * @param text
	 * @return
	 */
	public static List<String> tokenizeFull(String text) {
		return Arrays.asList(text.split("\\s|,|'"));
	}

	/**
	 * Remove stopwords found in the given dataset.
	 * 
	 * @param words
	 * @param stopwords
	 * @return
	 */
	public static List<String> trim(Collection<String> words,
			Map<String, String> stopWords) {
		List<String> okWords = new ArrayList<String>();

		for (String word : words)
			if (!word.equals(""))
				if (!stopWords.containsKey(word.toLowerCase()))
					okWords.add(word);

		return okWords;
	}

	/**
	 * Remove stopwords found in the given dataset file.
	 * 
	 * @param words
	 * @param datasetPath
	 * @return
	 * @throws FileNotFoundException
	 */
	public static List<String> trim(Collection<String> words, String datasetPath)
			throws FileNotFoundException {
		Map<String, String> stopWords = loadStopWords(datasetPath);
		return trim(words, stopWords);
	}

	/**
	 * Remove stopwords using the default dataset (
	 * {@link StopWordsTrimmer#DEFAULT_DATASET_FILE});
	 * 
	 * @param words
	 * @return
	 * @throws FileNotFoundException
	 */
	public List<String> trim(Collection<String> words)
			throws FileNotFoundException {
		if (stopwords == null)
			stopwords = loadStopWords(datasetPath);

		return trim(words, stopwords);
	}

	private static Map<String, String> loadStopWords(String filePath)
			throws FileNotFoundException {
		Map<String, String> stopWords = new HashMap<String, String>();

		Scanner sc;
		try {
			sc = new Scanner(
					StopWordsTrimmer.class.getResourceAsStream(filePath));
		} catch (Exception ex) {
			sc = new Scanner(new File(filePath));
		}

		try {
			while (sc.hasNextLine()) {
				stopWords.put(sc.nextLine(), null);
			}
		} finally {
			sc.close();
		}

		return stopWords;
	}
}
