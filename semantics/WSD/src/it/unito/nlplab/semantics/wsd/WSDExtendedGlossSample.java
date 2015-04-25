package it.unito.nlplab.semantics.wsd;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.data.BabelExample;
import it.uniroma1.lcl.babelnet.data.BabelGloss;
import it.uniroma1.lcl.babelnet.data.BabelPOS;
import it.uniroma1.lcl.jlt.util.Language;
import it.unito.nlplap.semantics.utils.FeatureVectorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rita.RiTa;

public class WSDExtendedGlossSample {

	private static final Logger LOG = LogManager
			.getLogger(WSDExtendedGlossSample.class);

	public static void main(String[] args) throws Exception {

		String word = "pianta";
		String context = "La pianta dell'alloggio è disponibile in ufficio, accanto all'appartamento; dal disegno è possibile cogliere i dettagli dell'architettura dello stabile, sulla distribuzione dei vani e la disposizione di porte e finestre.";

		String pos = RiTa.getPosTags(word, true)[0];
		
		System.out.println(String.format(
				"Best sense for word '%s' in context '%s':\n\t%s", word,
				context,
				getBestSenseWithExtendedLeskAlgorithm(word, context, pos)));
	}

	private static ExtendedSense getBestSenseWithExtendedLeskAlgorithm(
			String searchWord, String context, String pos) throws Exception {
		List<ExtendedSense> senses = getExtendedSenses(searchWord, pos);

		int maxOverlap = 0;
		ExtendedSense bestSense = null;
		for (ExtendedSense sense : senses) {
			int overlap = getOverlap(sense,
					FeatureVectorUtils.getLemmas(context));

			LOG.info(String
					.format("[getBestSenseWithExtendedLeskAlgorithm] - word=%s, pos=%s, overlap=%d, sense=%s",
							searchWord, pos, overlap, sense));

			if (overlap > maxOverlap) {
				maxOverlap = overlap;
				bestSense = sense;
			}
		}

		return bestSense;
	}

	private static int getOverlap(ExtendedSense sense,
			List<String> contextLemmas) throws Exception {
		int overlap = 0;

		Map<String, String> senseWords = new HashMap<String, String>();

		for (String gloss : sense.getGlosses()) {
			senseWords.clear();
			for (String senseWord : FeatureVectorUtils.getLemmas(gloss)) {
				senseWords.put(senseWord, null);
			}
			overlap += getOverlap(contextLemmas, senseWords);
		}

		senseWords.clear();
		for (String example : sense.getExamples()) {
			for (String exampleWord : FeatureVectorUtils.getLemmas(example)) {
				senseWords.put(exampleWord, null);
			}
		}
		overlap += getOverlap(contextLemmas, senseWords);

		for (Sense rs : sense.getRelatedSenses()) {
			for (String example : rs.getExamples()) {
				senseWords.clear();
				for (String exampleWord : FeatureVectorUtils.getLemmas(example)) {
					senseWords.put(exampleWord, null);
				}
				overlap += getOverlap(contextLemmas, senseWords);
			}
		}

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
			sense.setGlosses(extractGlossesFromBabelSynset(bsynset));

			sense.setExamples(extractExamplesFromBabelSynset(bsynset));

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
