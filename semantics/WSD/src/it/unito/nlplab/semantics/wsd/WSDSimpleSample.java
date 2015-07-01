package it.unito.nlplab.semantics.wsd;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

		WSD wsd = new WSD(language);
		
		// TextCleaner cleaner = new DummyTextCleaner();
		//TextCleaner cleaner = new StopWordOnlyTextCleaner(language);
		//TextCleaner cleaner = new StemmingTextCleaner(language);
		// TextCleaner cleaner = new LemmatizingTextCleaner(language);

		// Get words' senses
		for (String context : contexts) {

			Sense bestSense = wsd.getBestSense(searchWord, context);

			System.out
					.println(String
							.format("Word '%s' has the following sense in context '%s':\n '%s'\n",
									searchWord, context,
									bestSense));

			System.out.println("----\t\t----\n");
		}

	}



}
