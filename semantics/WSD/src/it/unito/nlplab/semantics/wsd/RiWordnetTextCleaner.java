package it.unito.nlplab.semantics.wsd;

import it.unito.nlplap.semantics.utils.StopWordsTrimmer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

import rita.RiTa;
import rita.RiWordNet;

public class RiWordnetTextCleaner implements TextCleaner {

	private Locale language;
	private StopWordsTrimmer swt;
	private RiWordNet wn;

	public RiWordnetTextCleaner(Locale language, RiWordNet wn) {
		this.language = language;
		this.swt = new StopWordsTrimmer(language);
		this.wn = wn;
	}

	public String cleanText(String text) throws Exception {
		// Trim stopwords
		List<String> terms = swt.trim(swt.tokenize(swt.normalize(text.toLowerCase())));

		// Lemmatizing
		List<String> stems = new ArrayList<String>();
		for (String term : terms) {
			String pos = wn.getBestPos(term);
			stems.add(getBestStem(term,
					wn.getStems(term, (pos != null ? pos : RiWordNet.NOUN))));
		}

		return StringUtils.join(stems, " ");
	}

	private static String getBestStem(String word, String[] stems) {
		if (stems.length == 0)
			return word;
		else
			return stems[0];
	}

}
