package it.unito.nlplab.semantics.wsd;

import it.unito.nlplab.semantics.textcleaner.LemmatizingTextCleaner;
import it.unito.nlplab.semantics.textcleaner.TextCleaner;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WSDExtendedGlossSample {

	private static final Logger LOG = LogManager
			.getLogger(WSDExtendedGlossSample.class);

	public static void main(String[] args) throws Exception {

		Locale language = Locale.ITALIAN;

		String wordA = "pianta";
		List<String> contextsA = Arrays
				.asList(new String[] {
						"La pianta dell'alloggio è disponibile in ufficio, accanto all'appartamento; dal disegno è possibile cogliere i dettagli dell'architettura dello stabile, sulla distribuzione dei vani e la disposizione di porte e finestre.",
						"I platani sono piante ad alto fusto, organismi viventi: non ha senso toglierli per fare posto a un parcheggio.",
						"Non riesce ad appoggiare pianta del piede perché ha un profondo taglio vicino all'alluce." });

		String wordB = "testa";
		List<String> contextsB = Arrays
				.asList(new String[] {
						"Si tratta di un uomo facilmente riconoscibile: ha una testa piccola, gli occhi sporgenti, naso adunco e piccole orecchie a sventola.",
						"Come per tutte le cose, ci vorrebbe un po' di testa, una punta di cervello, per non prendere decisioni fuori dal senso dell’intelletto.",
						"La testa della struttura di parsing è l’elemento che corrisponde al sintagma più alto dell’albero." });

		String word = wordA;
		List<String> contexts = contextsA;

		// TextCleaner cleaner = new DummyTextCleaner();
		// TextCleaner cleaner = new StopWordOnlyTextCleaner(language);
		// TextCleaner cleaner = new StemmingTextCleaner(language);
		TextCleaner cleaner = new LemmatizingTextCleaner(language);

		int relatedSensesLimit = 3;

		ExtendedWSD ewsd = new ExtendedWSD(language, cleaner);

		for (String context : contexts) {

			// Process input
			LOG.info(String
					.format("\n\nBest sense for word '%s' in context '%s':\n\t%s\n\n",
							word, context, ewsd.getBestSense(
									word, context, relatedSensesLimit)));
		}
	}

}
