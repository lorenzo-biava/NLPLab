package it.unito.nlplab.semantics.textcleaner;

/**
 * 
 * @author user
 *
 */
public class DummyTextCleaner implements TextCleaner {

	/**
	 * Returns original text.
	 * @inheritDoc
	 */
	public String cleanText(String text) throws Exception {
		return text;
	}

}
