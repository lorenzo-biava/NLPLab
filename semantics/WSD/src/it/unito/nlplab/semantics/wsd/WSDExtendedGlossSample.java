package it.unito.nlplab.semantics.wsd;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.data.BabelCategory;
import it.uniroma1.lcl.babelnet.data.BabelExample;
import it.uniroma1.lcl.babelnet.data.BabelGloss;
import it.uniroma1.lcl.babelnet.data.BabelPOS;
import it.uniroma1.lcl.jlt.util.Language;
import it.unito.nlplap.semantics.utils.FeatureVectorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rita.RiTa;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.StringUtils;

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
		
		//TextCleaner cleaner = new StanfordCoreNLPTextCleaner(language);
		TextCleaner cleaner = new DummyTextCleaner();

		// Process input
		List<String> cleanContexts = new ArrayList<String>();
		for (String ctx : contexts) {
			cleanContexts.add(cleaner.cleanText(ctx));
		}

		String pos = RiTa.getPosTags(word, true)[0];

		List<ExtendedSense> senses = getExtendedSenses(word, pos);

		// Clean senses
		for (ExtendedSense sense : senses) {
			List<String> glosses = new ArrayList<String>();
			for (String gloss : sense.getGlosses())
				glosses.add(cleaner.cleanText(gloss));

			sense.setGlosses(glosses);

			List<String> examples = new ArrayList<String>();
			for (String example : sense.getExamples())
				examples.add(cleaner.cleanText(example));
			sense.setExamples(examples);
			
			//TODO: Related senses
		}

		
		for (String context : contexts) {

			System.out.println(String.format(
					"\nBest sense for word '%s' in context '%s':\n\t%s\n\n",
					word,
					context,
					getBestSenseWithExtendedLeskAlgorithm(word, context,
							senses, language)));
		}
	}

	private static ExtendedSense getBestSenseWithExtendedLeskAlgorithm(
			String searchWord, String context, List<ExtendedSense> senses,
			Locale language) throws Exception {

		int maxOverlap = 0;
		ExtendedSense bestSense = null;
		for (ExtendedSense sense : senses) {
			int overlap = getOverlap(sense,
					FeatureVectorUtils.getLemmas(context, language), language);

			LOG.info(String
					.format("[getBestSenseWithExtendedLeskAlgorithm] - word=%s, overlap=%d, sense=%s",
							searchWord, overlap, sense));

			if (overlap > maxOverlap) {
				maxOverlap = overlap;
				bestSense = sense;
			}
		}

		return bestSense;
	}

	private static int getOverlap(ExtendedSense sense,
			List<String> contextLemmas, Locale language) throws Exception {
		int overlap = 0;

		Map<String, String> senseWords = new HashMap<String, String>();

		for (String gloss : sense.getGlosses()) {
			// senseWords.clear();
			for (String senseWord : FeatureVectorUtils.getLemmas(gloss,
					language)) {
				senseWords.put(senseWord, null);
			}
			// overlap += getOverlap(contextLemmas, senseWords);
		}

		// senseWords.clear();
		for (String example : sense.getExamples()) {
			for (String exampleWord : FeatureVectorUtils.getLemmas(example,
					language)) {
				senseWords.put(exampleWord, null);
			}
		}
		// overlap += getOverlap(contextLemmas, senseWords);

		for (Sense rs : sense.getRelatedSenses()) {
			for (String example : rs.getExamples()) {
				// senseWords.clear();
				for (String exampleWord : FeatureVectorUtils.getLemmas(example,
						language)) {
					senseWords.put(exampleWord, null);
				}
				// overlap += getOverlap(contextLemmas, senseWords);
			}
		}

		overlap = getOverlap(contextLemmas, senseWords);
		LOG.info(String.format(
				"Calculating Overlap: value=%d, context=[%s], sense=[%s]",
				overlap, StringUtils.join(contextLemmas, ", "),
				StringUtils.join(senseWords.keySet(), ", ")));

		return overlap;
	}

	private static int getOverlap(List<String> context,
			Map<String, String> senseWords) {
		int overlap = 0;

		for (String contextWord : context) {
			if (senseWords.containsKey(contextWord))
				overlap++;
		}

		return overlap;
	}

	private static List<ExtendedSense> getExtendedSenses(String searchWord,
			String pos) {
		List<ExtendedSense> senses = new ArrayList<ExtendedSense>();

		BabelNet bn = BabelNet.getInstance();
		BabelPOS bpos = BabelPOS.NOUN;
		switch (pos) {
		case "n":
			bpos = BabelPOS.NOUN;
			break;
		case "v":
			bpos = BabelPOS.VERB;
			break;
		case "a":
			bpos = BabelPOS.ADJECTIVE;
			break;
		case "r":
			bpos = BabelPOS.ADVERB;
			break;
		default:
		}

		List<BabelSynset> bySynsets = bn.getSynsets(Language.IT, searchWord,
				bpos);

		for (BabelSynset bsynset : bySynsets) {
			ExtendedSense sense = new ExtendedSense();
			sense.setId(bsynset.getId().getID());
			sense.setName(bsynset.getMainSense());
			sense.setGlosses(extractGlossesFromBabelSynset(bsynset));

			sense.setExamples(extractExamplesFromBabelSynset(bsynset));

			sense.getExamples().add(
					"_: "
							+ StringUtils.join(
									extractCategoriesFromBabelSynset(bsynset),
									", "));
			// List<Sense> rss = new ArrayList<Sense>();
			// for (BabelSynsetIDRelation brel : bsynset.getEdges()) {
			// Sense rs = new Sense();
			// rs.setGlosses(extractGlossesFromBabelSynset(bsynset));
			//
			// rs.setExamples(extractExamplesFromBabelSynset(brel
			// .getBabelSynsetIDTarget().toBabelSynset()));
			//
			// rss.add(rs);
			// }
			// sense.setRelatedSenses(rss);

			senses.add(sense);
		}
		return senses;
	}

	private static List<String> extractCategoriesFromBabelSynset(
			BabelSynset synset) {
		List<String> examples = new ArrayList<String>();
		for (BabelCategory bexample : synset.getCategories(Language.IT))
			examples.add(bexample.getCategory());
		return examples;
	}

	private static List<String> extractExamplesFromBabelSynset(
			BabelSynset synset) {
		List<String> examples = new ArrayList<String>();
		for (BabelExample bexample : synset.getExamples(Language.IT))
			examples.add(bexample.getExample());
		return examples;
	}

	private static List<String> extractGlossesFromBabelSynset(BabelSynset synset) {
		List<String> examples = new ArrayList<String>();
		for (BabelGloss bgloss : synset.getGlosses(Language.IT))
			examples.add(bgloss.getGloss());
		return examples;
	}

}
