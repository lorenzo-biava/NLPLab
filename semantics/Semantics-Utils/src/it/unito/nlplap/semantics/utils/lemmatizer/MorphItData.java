package it.unito.nlplap.semantics.utils.lemmatizer;

/**
 * Class to hold MorphIt data for each word.
 */
public class MorphItData {
	private String lemma, word, pos, extra;

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getPos() {
		return pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public MorphItData(String lemma, String word, String pos, String extra) {
		super();
		this.lemma = lemma;
		this.word = word;
		this.pos = pos;
		this.extra = extra;
	}

}
