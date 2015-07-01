package it.unito.nlplap.semantics.rocchio.utils;

import it.unito.nlplap.semantics.rocchio.RocchioClassificationBenchmark.ClassificationClassAware;
import it.unito.nlplap.semantics.utils.MutableDouble;
import it.unito.nlplap.semantics.utils.MutableInt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Document implements ClassificationClassAware<String> {
	private static final Logger LOG = LogManager.getLogger(Document.class);

	private String name, path, text, category;
	private HashSet<String> terms = new HashSet<String>();
	private Map<String, MutableInt> termCount = new HashMap<String, MutableInt>();
	private Map<String, MutableInt> collectionTermCount = new HashMap<String, MutableInt>();
	private Map<String, MutableDouble> collectionTermFrequency = new HashMap<String, MutableDouble>();
	private Map<String, MutableDouble> collectionTermWeight = new HashMap<String, MutableDouble>();

	public Document(String name, String path, String text, HashSet<String> terms,
			String category) {
		super();
		this.name = name;
		this.path = path;
		this.text = text;
		this.terms = terms;
		this.category = category;

		setTerms(terms);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String getClassificationClass() {
		return category;
	}
	
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Map<String, MutableInt> getTermCount() {
		return termCount;
	}

	public void setTermCount(Map<String, MutableInt> termCount) {
		this.termCount = termCount;
	}

	public Map<String, MutableInt> getCollectionTermCount() {
		return collectionTermCount;
	}

	public void setCollectionTermCount(
			Map<String, MutableInt> collectionTermCount) {
		this.collectionTermCount = collectionTermCount;
	}

	public Map<String, MutableDouble> getCollectionTermFrequency() {
		return collectionTermFrequency;
	}

	public void setCollectionTermFrequency(
			Map<String, MutableDouble> collectionTermFrequency) {
		this.collectionTermFrequency = collectionTermFrequency;
	}

	public Map<String, MutableDouble> getCollectionTermWeight() {
		return collectionTermWeight;
	}

	public void setCollectionTermWeight(
			Map<String, MutableDouble> collectionTermWeight) {
		this.collectionTermWeight = collectionTermWeight;
	}

	public HashSet<String> getTerms() {
		return terms;
	}

	public void setTerms(HashSet<String> terms) {
		this.terms = terms;

		if (terms != null) {
			if (terms.size() == 0)
				LOG.warn(String.format("Document '%s' has no terms !", this.name));
			getTermCount().clear();
			// Term count
			for (String term : terms) {
				MutableInt t = getTermCount().get(term);
				if (t != null)
					t.increment();
				else
					getTermCount().put(term.intern(), new MutableInt(1));
			}
		}
	}

}