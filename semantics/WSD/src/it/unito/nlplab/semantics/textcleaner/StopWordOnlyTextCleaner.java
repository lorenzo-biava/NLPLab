package it.unito.nlplab.semantics.textcleaner;

import it.unito.nlplap.semantics.utils.StopWordsTrimmer;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

public class StopWordOnlyTextCleaner implements TextCleaner {

	// private Locale language;
	private StopWordsTrimmer swt;

	public StopWordOnlyTextCleaner(Locale language)
			throws FileNotFoundException {
		// this.language = language;
		this.swt = new StopWordsTrimmer(language);
	}

	public String cleanText(String text) throws Exception {
		// Trim stopwords
		List<String> terms = swt.trim(StopWordsTrimmer.tokenize(swt
				.normalize(text.toLowerCase())));

		return StringUtils.join(terms, " ");
	}

}
