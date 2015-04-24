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

	public static final String DEAFULT_DATASET_FILE = "data/stop_words_FULL.txt";

	public static String normalize(String text) {
		//return text.replaceAll("[^A-Za-z0-9 ]", " ");
		return text.replaceAll("[^A-Za-z ]", " ");
	}

	public static List<String> tokenize(String text) {
		return Arrays.asList(text.split(" "));
	}

	public static List<String> trim(List<String> words, String datasetPath)
			throws FileNotFoundException {
		Map<String, String> stopWords = loadStopWords(datasetPath);
		List<String> okWords = new ArrayList<String>();

		for (String word : words)
			if (!stopWords.containsKey(word))
				okWords.add(word);

		return okWords;
	}

	public static List<String> trim(List<String> words)
			throws FileNotFoundException {
		return trim(words, DEAFULT_DATASET_FILE);
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
