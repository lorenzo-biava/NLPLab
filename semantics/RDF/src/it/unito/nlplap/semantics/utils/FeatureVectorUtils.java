package it.unito.nlplap.semantics.utils;

import java.util.List;

public class FeatureVectorUtils {

	public static List<String> getFeatureVector(String text) throws Exception {
		List<String> words = StopWordsTrimmer.trim(StopWordsTrimmer
				.tokenize(StopWordsTrimmer.normalize(text)));
		return words;
	}

}
