package it.unito.nlplab.semantics.wsd;

import it.unito.nlplab.semantics.textcleaner.LemmatizingTextCleaner;
import it.unito.nlplab.semantics.textcleaner.TextCleaner;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WSDSimpleSample {

	// private static final Logger LOG = LogManager
	// .getLogger(WSDExtendedGlossSample.class);

	public static void main(String[] args) throws Exception {

		Locale language = Locale.ENGLISH;

		List<String> contexts = Arrays.asList(new String[] {
				"The house was burnt to ashes while the owner returned.",
				"This table is made of ash wood." });
		String searchWord = "ash";

		// TextCleaner cleaner = new DummyTextCleaner();
		// TextCleaner cleaner = new StopWordOnlyTextCleaner(language);
		// TextCleaner cleaner = new StemmingTextCleaner(language);
		TextCleaner cleaner = new LemmatizingTextCleaner(language);

		WSD wsd = new WSD(language, cleaner);

		// Get words' senses
		for (String context : contexts) {

			Sense bestSense = wsd.getBestSense(searchWord, context);

			System.out
					.println(String
							.format("Word '%s' has the following sense in context '%s':\n '%s'\n",
									searchWord, context, bestSense));

			System.out.println("----\t\t----\n");
		}

	}

}
