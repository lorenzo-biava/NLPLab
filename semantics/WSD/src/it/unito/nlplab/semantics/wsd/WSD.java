package it.unito.nlplab.semantics.wsd;

import it.unito.nlplab.semantics.textcleaner.LemmatizingTextCleaner;
import it.unito.nlplab.semantics.textcleaner.TextCleaner;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rita.RiTa;
import rita.RiWordNet;

/**
 * Word Sense Disambiguation utility, implementing sense caching and Lesk
 * algorithm.
 * 
 * @author Lorenzo Biava
 *
 */
public class WSD {

	/**
	 * Exception to signal a that a given word to WSD has been skipped by the
	 * stop words filter.
	 *
	 */
	public static class StopWordException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public StopWordException() {
			super();
		}

		public StopWordException(String message, Throwable cause,
				boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public StopWordException(String message, Throwable cause) {
			super(message, cause);
		}

		public StopWordException(String message) {
			super(message);
		}

		public StopWordException(Throwable cause) {
			super(cause);
		}

	}

	private static final Logger LOG = LogManager.getLogger(WSD.class);

	public static final String WORDNET_DEFAULT_DIR = "/usr/local/WordNet-3.0";

	// private Locale language;
	private RiWordNet wn;
	private TextCleaner cleaner;

	/**
	 * Holds already discovered senses with cleaned contexts (i.e. lemmatized or
	 * something) for Word-PoS
	 */
	private ConcurrentMap<String, ConcurrentMap<String, List<Sense>>> senseCache = new ConcurrentHashMap<String, ConcurrentMap<String, List<Sense>>>();

	/**
	 * 
	 * @param language
	 * @param cleaner
	 *            if null, defaulting to {@link LemmatizingTextCleaner}
	 * @throws FileNotFoundException
	 */
	public WSD(Locale language, TextCleaner cleaner)
			throws FileNotFoundException {
		if (language != Locale.ENGLISH)
			throw new IllegalArgumentException(
					"Currently only English is supported.");

		String wordNetDir = System.getenv("WNHOME");
		if (wordNetDir == null)
			wordNetDir = WORDNET_DEFAULT_DIR;
		this.wn = new RiWordNet(wordNetDir);

		// this.language = language;

		if (cleaner == null) {
			cleaner = new LemmatizingTextCleaner(language);
		}
		this.cleaner = cleaner;

	}

	public WSD(Locale language) throws FileNotFoundException {
		this(language, null);
	}

	public Sense getBestSense(String word, String context) throws Exception {
		return getBestSense(word, context, null);
	}

	/**
	 * Returns the best sense for the given word and context (sentence in which
	 * the word is in) or null. Both word and context will be cleaned
	 * appropriately (i.e. tokenized, stopwords filtered and lemmatized).
	 * 
	 * @param word
	 * @param context
	 * @return
	 * @throws Exception
	 * @throws StopWordException
	 *             if the clean word becomes null (it might be a stop word)
	 */
	public Sense getBestSense(String word, String context, String pos)
			throws StopWordException, Exception {

		// Clean original word
		String cleanWord = cleaner.cleanText(word);
		if (cleanWord.length() < 1)
			throw new StopWordException(word);

		// Clean original word's context
		HashSet<String> cleanContext = new HashSet<String>();
		for (String contextWord : RiTa.tokenize(cleaner.cleanText(context))) {
			cleanContext.add(contextWord);
		}

		return getBestSense(cleanWord, cleanContext, pos);
	}

	/**
	 * Returns the best sense for the given word and context (sentence in which
	 * the word is in) or null. Both word and context MUST be already cleaned
	 * appropriately (i.e. tokenized, stopwords filtered and lemmatized).
	 * 
	 * @param cleanWord
	 * @param cleanContext
	 * @param pos
	 * @return
	 * @throws Exception
	 */
	public Sense getBestSense(String cleanWord, HashSet<String> cleanContext,
			String pos) throws Exception {

		// Get word's best PoS tag
		if (pos == null) {
			pos = "n";
			try {
				pos = RiTa.getPosTags(cleanWord, true)[0];
				if (pos.equals("-"))
					pos = "n";
			} catch (Exception e) {
			}
		}

		if (LOG.isDebugEnabled())
			LOG.debug(String
					.format("Calculating best sense for word '%s', PoS '%s' and context '%s'",
							cleanWord, pos, cleanContext));

		// Compute best sense
		Sense s = getBestSenseWithLeskAlgorithm(cleanWord, cleanContext, pos);

		// Associate sense with clean word
		if (s != null)
			s.setLemma(cleanWord);

		return s;
	}

	/**
	 * Returns the best sense (or null) for the given word, context (sentence in
	 * which the word is in) and word's PoS tag, calculated using the Lest
	 * algorithm.
	 * 
	 * @param word
	 * @param context
	 * @param pos
	 * @return
	 * @throws Exception
	 */
	public Sense getBestSenseWithLeskAlgorithm(String word,
			HashSet<String> context, String pos) throws Exception {

		// Retrieve senses for current word
		List<Sense> senses = getSenses(word, pos);

		// Find best sense (max overlap score)
		int maxOverlap = -1;
		Sense bestSense = null;
		for (Sense sense : senses) {
			int overlap = getOverlap(sense, context);

			if (LOG.isDebugEnabled())
				LOG.debug(String
						.format("[getBestSenseWithLeskAlgorithm] - word=%s, overlap=%d, sense=%s",
								word, overlap, sense));

			if (overlap > maxOverlap) {
				maxOverlap = overlap;
				bestSense = sense;
			}

		}

		return bestSense;
	}

	/**
	 * Returns all the senses related to the given (clean) word and PoS tag,
	 * after cleaning senses' context. <br/>
	 * <b>Note</b> that results are cached automatically.
	 * 
	 * @param word
	 * @param pos
	 *            RiTa pos tag (n,r,a,v)
	 * @return
	 * @throws Exception
	 */
	private List<Sense> getSenses(String word, String pos) throws Exception {

		// Check if senses are already cached
		ConcurrentMap<String, List<Sense>> wordCache = null;
		List<Sense> senses = null;
		if ((wordCache = senseCache.get(word)) != null) {
			if ((senses = wordCache.get(pos)) != null)
				return senses;
			else {
				senses = new ArrayList<Sense>();
				// Create cache PoS entry
				wordCache.put(pos, senses);
			}
		} else {
			// Create cache Word-Pos entry
			wordCache = new ConcurrentHashMap<String, List<Sense>>();
			senses = new ArrayList<Sense>();
			wordCache.put(pos, senses);
			senseCache.put(word, wordCache);
		}

		// Retrieve senses
		int[] senseIds = wn.getSenseIds(word, pos);
		for (int id : senseIds) {
			Sense sense = new Sense();
			sense.setId("" + id);
			sense.setName(wn.getDescription(id));
			sense.setGloss(wn.getGloss(id));
			sense.setExamples(Arrays.asList(wn.getExamples(id)));

			// Clean sense contexts
			for (String senseWord : RiTa.tokenize(cleaner.cleanText(sense
					.getGloss()))) {
				sense.getContext().add(senseWord);
			}
			for (String example : sense.getExamples()) {
				for (String exampleWord : RiTa.tokenize(cleaner
						.cleanText(example))) {
					sense.getContext().add(exampleWord);
				}
			}

			// Add discovered senses to cache
			senses.add(sense);
		}

		return senses;
	}

	/**
	 * Return the overlap score (i.e. number of word of the sense that are also
	 * in the context) for the given Sense and context
	 * 
	 * @param sense
	 * @param context
	 * @param cleaner
	 * @return
	 * @throws Exception
	 */
	public static int getOverlap(Sense sense, HashSet<String> context)
			throws Exception {
		int overlap = 0;

		List<String> commons = new ArrayList<String>();
		// Count words in common
		for (String contextWord : context) {
			if (sense.getContext().contains(contextWord)) {
				overlap++;
				commons.add(contextWord);
			}
		}

		if (LOG.isDebugEnabled())
			LOG.debug(String
					.format("Calculating Overlap: value=%d, commonWords=[%s], context=[%s], sense=[%s]",
							overlap, StringUtils.join(commons, ", "),
							StringUtils.join(context, ", "),
							StringUtils.join(sense.getContext(), ", ")));

		return overlap;
	}
}
