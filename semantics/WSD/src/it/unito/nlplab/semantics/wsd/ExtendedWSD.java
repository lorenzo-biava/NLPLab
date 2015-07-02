package it.unito.nlplab.semantics.wsd;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetIDRelation;
import it.uniroma1.lcl.babelnet.data.BabelCategory;
import it.uniroma1.lcl.babelnet.data.BabelExample;
import it.uniroma1.lcl.babelnet.data.BabelGloss;
import it.uniroma1.lcl.babelnet.data.BabelPOS;
import it.uniroma1.lcl.jlt.util.Language;
import it.unito.nlplab.semantics.textcleaner.LemmatizingTextCleaner;
import it.unito.nlplab.semantics.textcleaner.TextCleaner;
import it.unito.nlplap.semantics.utils.FeatureVectorUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rita.RiTa;
import edu.stanford.nlp.util.StringUtils;

/**
 * Word Sense Disambiguation utility, implementing Extended Lesk algorithm (i.e.
 * using also related senses as disambiguation context).
 * 
 * @author Lorenzo Biava
 *
 */
public class ExtendedWSD {

	private static final Logger LOG = LogManager.getLogger(ExtendedWSD.class);

	private Locale language;
	private Language babelLanguage;
	private TextCleaner cleaner;

	/**
	 * Holds already discovered senses (i.e. lemmatized or something) for
	 * Word-PoS
	 */
	private ConcurrentMap<String, ConcurrentMap<String, List<ExtendedSense>>> senseCache = new ConcurrentHashMap<String, ConcurrentMap<String, List<ExtendedSense>>>();

	/**
	 * 
	 * @param language
	 * @param cleaner
	 *            if null, defaulting to {@link LemmatizingTextCleaner}
	 * @throws FileNotFoundException
	 */
	public ExtendedWSD(Locale language, TextCleaner cleaner)
			throws FileNotFoundException {

		this.language = language;
		babelLanguage = Language.valueOf(language.getLanguage().toUpperCase());

		if (cleaner == null) {
			cleaner = new LemmatizingTextCleaner(language);
		}
		this.cleaner = cleaner;

	}

	/**
	 * Return the best sense for the given word and context, using Extended Lesk
	 * Algorithm. Both word and context will be cleaned appropriately (i.e.
	 * tokenized, stopwords filtered and lemmatized).
	 * 
	 * @param word
	 * @param context
	 * @param relatedSensesLimit
	 *            the maximum number of related senses to retrieve (0 =
	 *            unlimited)
	 * @return
	 * @throws Exception
	 */
	public ExtendedSense getBestSense(String word, String context,
			int relatedSensesLimit) throws Exception {

		// Get word's best PoS tag
		String pos = RiTa.getPosTags(word, true)[0];

		// Clean original word
		String cleanWord = cleaner.cleanText(word);

		// Clean original word's context
		HashSet<String> cleanContext = new HashSet<String>(
				FeatureVectorUtils.getLemmas(cleaner.cleanText(context),
						language));

		// Retrieve senses (and related) for current word
		List<ExtendedSense> senses = getExtendedSenses(cleanWord, pos,
				relatedSensesLimit);

		// Print sense for debug
		LOG.info(String.format("Found %d senses:", senses.size()));
		int senseCount = 0;
		for (ExtendedSense sense : senses) {
			LOG.info(String.format("\tSense %d:", ++senseCount));
			LOG.info(sense.pprint(1));
		}

		// Find best sense (max overlap score)
		int maxOverlap = -1;
		ExtendedSense bestSense = null;
		for (ExtendedSense sense : senses) {
			int overlap = getOverlap(sense, cleanContext);

			LOG.info(String
					.format("[getBestSenseWithExtendedLeskAlgorithm] - word=%s, overlap=%d, context=%s, sense=%s",
							cleanWord, overlap, cleanContext, sense));

			if (overlap > maxOverlap) {
				maxOverlap = overlap;
				bestSense = sense;
			}
		}

		// Associate sense with clean word
		if (bestSense != null)
			bestSense.setLemma(cleanWord);

		return bestSense;
	}

	/**
	 * Returns the overlap score (i.e. number of word of the sense that are also
	 * in the context) for the given Sense and context.
	 * 
	 * @param sense
	 * @param contextLemmas
	 * @return
	 * @throws Exception
	 */
	public int getOverlap(ExtendedSense sense, HashSet<String> contextLemmas)
			throws Exception {
		int overlap = 0;

		HashSet<String> senseWords = new HashSet<String>();

		// Extract lemmas from Sense's Glosses
		for (String gloss : sense.getGlosses()) {
			for (String senseWord : FeatureVectorUtils.getLemmas(
					cleaner.cleanText(gloss), language)) {
				senseWords.add(senseWord);
			}
		}

		// Extract lemmas from Sense's Examples
		for (String example : sense.getExamples()) {
			for (String exampleWord : FeatureVectorUtils.getLemmas(
					cleaner.cleanText(example), language)) {
				senseWords.add(exampleWord);
			}
		}

		// Extract lemmas from Related Senses
		for (Sense rs : sense.getRelatedSenses()) {
			for (String gloss : rs.getGlosses()) {
				for (String senseWord : FeatureVectorUtils.getLemmas(
						cleaner.cleanText(gloss), language)) {
					senseWords.add(senseWord);
				}
			}

			for (String example : rs.getExamples()) {
				for (String exampleWord : FeatureVectorUtils.getLemmas(
						cleaner.cleanText(example), language)) {
					senseWords.add(exampleWord);
				}
			}
		}

		Sense s = new Sense();
		s.setContext(senseWords);
		overlap = WSD.getOverlap(s, contextLemmas);
		LOG.info(String.format(
				"Calculating Overlap: value=%d, context=[%s], sense=[%s]",
				overlap, StringUtils.join(contextLemmas, ", "),
				StringUtils.join(senseWords, ", ")));

		return overlap;
	}

	public List<ExtendedSense> getExtendedSenses(String searchWord, String pos) {
		return getExtendedSenses(searchWord, pos, 0);
	}

	/**
	 * Returns all the senses related to the given (clean) word and PoS tag. <br/>
	 * <b>Note</b> that results are cached automatically.
	 * 
	 * @param word
	 * @param pos
	 *            RiTa pos tag (n,r,a,v)
	 * @param limit
	 *            the maximum number of related senses to retrieve (0 =
	 *            unlimited)
	 * @return
	 * @throws Exception
	 */
	public List<ExtendedSense> getExtendedSenses(String word, String pos,
			int limit) {

		LOG.info(String
				.format("Getting senses for word '%s', with PoS '%s', related senses limit %d",
						word, pos, limit));

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

		// Check if senses are already cached
		ConcurrentMap<String, List<ExtendedSense>> wordCache = null;
		List<ExtendedSense> senses = null;
		if ((wordCache = senseCache.get(word)) != null) {
			if ((senses = wordCache.get(pos)) != null)
				return senses;
			else {
				senses = new ArrayList<ExtendedSense>();
				// Create cache PoS entry
				wordCache.put(pos, senses);
			}
		} else {
			// Create cache Word-Pos entry
			wordCache = new ConcurrentHashMap<String, List<ExtendedSense>>();
			senses = new ArrayList<ExtendedSense>();
			wordCache.put(pos, senses);
			senseCache.put(word, wordCache);
		}

		List<BabelSynset> bySynsets = bn.getSynsets(babelLanguage, word, bpos);
		int senseCount = 0;

		for (BabelSynset bsynset : bySynsets) {

			LOG.debug(String.format("Retrieving sense # %d/%d", ++senseCount,
					bySynsets.size()));

			ExtendedSense sense = new ExtendedSense();
			sense.setId(bsynset.getId().getID());
			sense.setName(bsynset.getMainSense());			
			sense.setGlosses(extractGlossesFromBabelSynset(bsynset));

			sense.setExamples(extractExamplesFromBabelSynset(bsynset));

			// Add also Sense's categories
			sense.getExamples().add(
					"_: "
							+ StringUtils.join(
									extractCategoriesFromBabelSynset(bsynset),
									", "));

			// Get related senses (only directly connected, with limit)
			int relatedSensesLimit = (limit == 0 ? 999 : limit);

			List<Sense> rss = new ArrayList<Sense>();
			List<BabelSynsetIDRelation> relatedSenses = bsynset.getEdges();
			int relatedSenseCount = 0;
			for (BabelSynsetIDRelation brel : relatedSenses) {

				if (--relatedSensesLimit < 0)
					break;

				LOG.debug(String.format(
						"Retrieving related sense # %d/%d of sense %s",
						++relatedSenseCount, relatedSenses.size(),
						sense.getId()));

				Sense rs = new Sense();
				BabelSynset relatedSynset = brel.getBabelSynsetIDTarget()
						.toBabelSynset();

				rs.setId(relatedSynset.getId().getID());
				rs.setName(relatedSynset.getMainSense());
				rs.setGlosses(extractGlossesFromBabelSynset(relatedSynset));
				rs.setExamples(extractExamplesFromBabelSynset(relatedSynset));

				// Add also Sense's categories
				rs.getExamples().add(
						"_: "
								+ StringUtils.join(
										extractCategoriesFromBabelSynset(relatedSynset),
										", "));
				
				if (!(rs.getGlosses().isEmpty() && rs.getExamples().isEmpty()))
					rss.add(rs);
			}
			sense.setRelatedSenses(rss);

			senses.add(sense);
		}
		return senses;
	}

	/* Utilities */

	private List<String> extractCategoriesFromBabelSynset(BabelSynset synset) {
		List<String> examples = new ArrayList<String>();
		for (BabelCategory bexample : synset.getCategories(babelLanguage))
			examples.add(bexample.getCategory());
		return examples;
	}

	private List<String> extractExamplesFromBabelSynset(BabelSynset synset) {
		List<String> examples = new ArrayList<String>();
		for (BabelExample bexample : synset.getExamples(babelLanguage))
			examples.add(bexample.getExample());
		return examples;
	}

	private List<String> extractGlossesFromBabelSynset(BabelSynset synset) {
		List<String> examples = new ArrayList<String>();
		for (BabelGloss bgloss : synset.getGlosses(babelLanguage))
			examples.add(bgloss.getGloss());
		return examples;
	}

}