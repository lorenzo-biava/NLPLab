package it.unito.nlplab.semantics.textcleaner;

public interface TextCleaner {

	/**
	 * Should return a cleaned text (i.e. normalized, stemmed/lemmetized, etc).
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public String cleanText(String text) throws Exception;
}
