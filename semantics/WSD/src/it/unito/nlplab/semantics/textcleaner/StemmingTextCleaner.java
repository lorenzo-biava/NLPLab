package it.unito.nlplab.semantics.textcleaner;

import it.unito.nlplap.semantics.utils.StopWordsTrimmer;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.tartarus.snowball.ext.EnglishStemmer;
import org.tartarus.snowball.ext.ItalianStemmer;

import rita.RiWordNet;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.morph.WordnetStemmer;

public class StemmingTextCleaner implements TextCleaner {

	public static final String WORDNET_DEFAULT_DIR = "/usr/local/WordNet-3.0";

	private Locale language;
	private StopWordsTrimmer swt;
	private RiWordNet wn;
	private ItalianStemmer itStemmer;
	private EnglishStemmer enStemmer;
	private String wordNetDir;

	public StemmingTextCleaner(Locale language) {
		this.language = language;
		this.swt = new StopWordsTrimmer(language);
		if (language == Locale.ITALIAN)
			itStemmer = new ItalianStemmer();
		else {
			enStemmer = new EnglishStemmer();
			wordNetDir = System.getenv("WNHOME");
			if (wordNetDir == null)
				wordNetDir = WORDNET_DEFAULT_DIR;
			this.wn = new RiWordNet(wordNetDir);
		}
	}

	public String cleanText(String text) throws Exception {
		// Trim stopwords
		List<String> terms = swt.trim(StopWordsTrimmer.tokenize(swt
				.normalize(text.toLowerCase())));

		// Stemming
		List<String> stems = new ArrayList<String>();
		for (String term : terms) {
			if (language == Locale.ITALIAN) {
				itStemmer.setCurrent(term);
				itStemmer.stem();
				stems.add(itStemmer.getCurrent());
			} else {
				enStemmer.setCurrent(term);
				enStemmer.stem();
				stems.add(enStemmer.getCurrent());

				// RiWordNet Stemmer -> Not Working
				// String pos = wn.getBestPos(term);
				// stems.add(getBestStem(term,
				// wn.getStems(term, (pos != null ? pos : RiWordNet.NOUN))));

				// WordNet Stemmer -> Not Working
				// Dictionary dict = new Dictionary(new URL("file://"+wordNetDir
				// + "/dict"));
				// dict.open();
				// WordnetStemmer wnStemmer = new WordnetStemmer(dict);
				//
				// // String pos = wn.getBestPos(term);
				// stems.add(getBestStem(term, (String[]) wnStemmer
				// .findStems(term).toArray()));
			}
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
