package it.unito.nlplap.semantics.rocchio.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Document {
	private String name, path, text, category;
	private List<String> terms = new ArrayList<String>();
	private Map<String, MutableInt> collectionTermCount = new HashMap<String, MutableInt>();
	private Map<String, MutableDouble> collectionTermFrequency = new HashMap<String, MutableDouble>();

	public Document(String name, String path, String text,
			List<String> terms, String category) {
		super();
		this.name = name;
		this.path = path;
		this.text = text;
		this.terms = terms;
		this.category = category;
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

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Map<String, MutableInt> getColletionTermCount() {
		return collectionTermCount;
	}

	public void setCollectionTermCount(Map<String, MutableInt> termCount) {
		this.collectionTermCount = termCount;
	}

	public Map<String, MutableDouble> getCollectionTermFrequency() {
		return collectionTermFrequency;
	}

	public void setCollectionTermFrequency(
			Map<String, MutableDouble> collectionTermFrequency) {
		this.collectionTermFrequency = collectionTermFrequency;
	}

	public Map<String, MutableDouble> getCollectionTermWeight() {
		return collectionTermFrequency;
	}

	public List<String> getTerms() {
		return terms;
	}

	public void setTerms(List<String> terms) {
		this.terms = terms;
	}

}