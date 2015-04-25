package it.unito.nlplap.semantics.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class StopWordsTrimmer {

	public static final String DEFAULT_DATASET_FILE = "data/stop_words_FULL.txt";

	/**
	 * Remove all chars except letters (replace with space).
	 * @param text
	 * @return
	 */
	public static String normalize(String text) {
		//return text.replaceAll("[^A-Za-z0-9 ]", " ");
		return text.replaceAll("[^A-Za-z ]", " ").toLowerCase();
	}

	/**
	 * Split a sentence in words by spaces.
	 * @param text
	 * @return
	 */
	public static List<String> tokenize(String text) {
		return Arrays.asList(text.split(" "));
	}

	/**
	 * Remove stopwords found in the given dataset.
	 * @param words
	 * @param datasetPath
	 * @return
	 * @throws FileNotFoundException
	 */
	public static List<String> trim(List<String> words, String datasetPath)
			throws FileNotFoundException {
		Map<String, String> stopWords = loadStopWords(datasetPath);
		List<String> okWords = new ArrayList<String>();

		for (String word : words)
			if (!stopWords.containsKey(word))
				okWords.add(word);

		return okWords;
	}

	/**
	 * Remove stopwords using the default dataset ({@link StopWordsTrimmer#DEFAULT_DATASET_FILE});
	 * @param words
	 * @return
	 * @throws FileNotFoundException
	 */
	public static List<String> trim(List<String> words)
			throws FileNotFoundException {
		return trim(words, DEFAULT_DATASET_FILE);
	}

	private static Map<String, String> loadStopWords(String filePath)
			throws FileNotFoundException {
		Map<String, String> stopWords = new HashMap<String, String>();

		Scanner sc = new Scanner(new File(filePath));
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
