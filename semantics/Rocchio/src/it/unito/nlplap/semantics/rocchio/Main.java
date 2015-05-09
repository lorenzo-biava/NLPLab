package it.unito.nlplap.semantics.rocchio;

import it.unito.nlplap.semantics.utils.FeatureVectorUtils;
import it.unito.nlplap.semantics.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

	private static final Logger LOG = LogManager.getLogger(Main.class);

	private static final String DOCUMENT_DIR_PATH = "data/docs_200";

	public static class Document {
		private String name, path, text, category;
		private List<String> terms = new ArrayList<String>();
		private Map<String, MutableInt> collectionTermCount = new HashMap<String, Main.MutableInt>();
		private Map<String, MutableDouble> collectionTermFrequency = new HashMap<String, Main.MutableDouble>();

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

	public static class MutableInt implements Cloneable, Comparable<MutableInt> {
		private int value = 1;

		/**
		 * Default value = 1;
		 */
		public MutableInt() {

		}

		/**
		 * Decide initial value
		 * 
		 * @param value
		 */
		public MutableInt(int value) {
			this.value = value;
		}

		public void increment() {
			++value;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public Object clone() {
			return new MutableInt(this.value);
		}

		@Override
		public String toString() {
			return value + "";
		}

		@Override
		public int compareTo(MutableInt o) {
			if (o.getValue() > value)
				return -1;
			else if (o.getValue() < value)
				return 1;
			return 0;
		}
	}

	public static class MutableDouble implements Cloneable,
			Comparable<MutableDouble> {
		private double value = 1;

		/**
		 * Default value = 1;
		 */
		public MutableDouble() {

		}

		/**
		 * Decide initial value
		 * 
		 * @param value
		 */
		public MutableDouble(double value) {
			this.value = value;
		}

		public void increment() {
			++value;
		}

		public double getValue() {
			return value;
		}

		public void setValue(double value) {
			this.value = value;
		}

		public Object clone() {
			return new MutableDouble(this.value);
		}

		@Override
		public String toString() {
			return value + "";
		}

		@Override
		public int compareTo(MutableDouble o) {
			if (o.getValue() > value)
				return -1;
			else if (o.getValue() < value)
				return 1;
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		List<Document> documents = new ArrayList<Document>();

		File docDir = new File(DOCUMENT_DIR_PATH);
		int limit = -200;
		for (File file : docDir.listFiles()) {
			if (limit > 10)
				break;

			if (file.isFile() && file.getName().indexOf(".") != 0) {
				limit++;
				String category = null;
				try {
					category = file.getName().split("_")[0];
				} catch (Exception ex) {
				}
				String text = Utils.fileToText(file);
				documents.add(new Document(file.getName(), file
						.getAbsolutePath(), text, FeatureVectorUtils.getLemmas(
						text, Locale.ITALIAN), category));
			}
		}

		Map<String, MutableInt> terms = new HashMap<String, MutableInt>();
		for (Document doc : documents) {
			for (String term : doc.getTerms())
				terms.put(term, new MutableInt(0));
		}

		LOG.info(String.format("Total terms '%d', terms=[%s]", terms.size(),
				terms));

		// For each document, extract term frequency
		for (Document doc : documents) {
			doc.setCollectionTermCount(clone(terms));

			// Term count
			for (String term : doc.getTerms())
				doc.getColletionTermCount().get(term).increment();

			// Term frequency
			for (Map.Entry<String, MutableInt> term : terms.entrySet())
				doc.getCollectionTermFrequency().put(
						term.getKey(),
						new MutableDouble((double) doc.getColletionTermCount()
								.get(term.getKey()).getValue()
								/ doc.getTerms().size()));

			// LOG.info(String.format("Document '%s' termCount=[%s]",
			// doc.getName(),
			// sortByComparator(doc.getColletionTermCount(), true)));
			// LOG.info(String.format("Document '%s' termFrequency=[%s]",
			// doc.getName(),
			// sortByComparator(doc.getCollectionTermFrequency(), true)));
			LOG.info(String.format("Document '%s'", doc.getName()));

		}
		LOG.info("ENDED");

		// Document d = documents.get(0);
		// for (Document q : documents)
		// LOG.debug(String.format(
		// "SIMILARITY: %f",
		// angleCosineSimilarity(d.getColletionTermFrequency(),
		// q.getColletionTermFrequency())));

		Map<String, Map<String, MutableDouble>> rocchioClasses = extractRocchioClasses(
				documents, terms);
		for (Map.Entry<String, Map<String, MutableDouble>> clazz : rocchioClasses
				.entrySet()) {
			// LOG.info(String.format("Rocchio Class '%s' features=[%s]",
			// clazz.getKey(), sortByComparator(clazz.getValue(), true)));
			LOG.info(String.format("Rocchio Class '%s'", clazz.getKey()));
		}

		// Classify docs
		limit = -200;
		int correctCount = 0;
		int wrongCount = 0;
		for (Document doc : documents) {
			if (limit > 0)
				break;

			// Test each Rocchio class
			double bestScore = 0;
			String bestClass = "";
			for (Map.Entry<String, Map<String, MutableDouble>> clazz : rocchioClasses
					.entrySet()) {
				double score = angleCosineSimilarity(
						doc.getCollectionTermWeight(), clazz.getValue());
				if (score > bestScore) {
					bestScore = score;
					bestClass = clazz.getKey();
				}
			}

			LOG.info(String.format(
					"Document '%s', correctClass=%s, bestClass=%s, score=%s",
					doc.getName(), doc.getCategory(), bestClass, bestScore));
			if (doc.getCategory().equals(bestClass))
				correctCount++;
			else
				wrongCount++;

			limit++;
		}

		LOG.info(String.format(
				"Total docs=%d, Correctly classified=%d, Badly classified=%d",
				documents.size(), correctCount, wrongCount));
	}

	private static <A extends Comparable<A>> Map<String, A> sortByComparator(
			Map<String, A> unsortMap, boolean reverse) {

		final boolean rev = reverse;

		// Convert Map to List
		List<Map.Entry<String, A>> list = new LinkedList<Map.Entry<String, A>>(
				unsortMap.entrySet());

		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, A>>() {
			public int compare(Map.Entry<String, A> o1, Map.Entry<String, A> o2) {
				return (o1.getValue()).compareTo(o2.getValue())
						* (rev ? -1 : 1);
			}
		});

		// Convert sorted map back to a Map
		Map<String, A> sortedMap = new LinkedHashMap<String, A>();
		for (Iterator<Map.Entry<String, A>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, A> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	public interface Cloneable {
		public Object clone();
	}

	private static <A, B> Map<A, B> clone(Map<A, B> map) {
		Map<A, B> clone = new HashMap<A, B>();
		for (Map.Entry<A, B> entry : map.entrySet()) {
			clone.put(entry.getKey(),
					(B) ((Cloneable) entry.getValue()).clone());
		}
		return clone;
	}

	public static double angleCosineSimilarity(Map<String, MutableDouble> wd,
			Map<String, MutableDouble> wq) {
		if (wd.size() != wq.size())
			throw new IllegalArgumentException(
					"Vectors must contain the same elements.");

		double sum = 0;
		for (Map.Entry<String, MutableDouble> d : wd.entrySet()) {
			sum += d.getValue().getValue() * wq.get(d.getKey()).getValue();
		}

		double wd2 = 0;
		for (Map.Entry<String, MutableDouble> d : wd.entrySet()) {
			wd2 += Math.pow(d.getValue().getValue(), 2);
		}
		wd2 = Math.sqrt(wd2);

		double wq2 = 0;
		for (Map.Entry<String, MutableDouble> q : wq.entrySet()) {
			wq2 += Math.pow(q.getValue().getValue(), 2);
		}
		wq2 = Math.sqrt(wq2);

		return sum / (wd2 * wq2);
	}

	public static Map<String, Map<String, MutableDouble>> extractRocchioClasses(
			List<Document> documents, Map<String, MutableInt> featuresInt) {
		Map<String, Map<String, MutableDouble>> classes = new HashMap<String, Map<String, MutableDouble>>();
		Map<String, List<Document>> classesPOS = new HashMap<String, List<Document>>();
		Map<String, List<Document>> classesNEG = new HashMap<String, List<Document>>();

		Map<String, MutableDouble> features = new HashMap<String, MutableDouble>();
		for (Map.Entry<String, MutableInt> feature : featuresInt.entrySet()) {
			features.put(feature.getKey(), new MutableDouble(feature.getValue()
					.getValue()));
		}

		// Extract empty classes from documents
		for (Document doc : documents) {
			if (classes.get(doc.getCategory()) == null) {
				// Initialize class
				classes.put(doc.getCategory(), clone(features));
				List<Document> pos = new ArrayList<Document>();
				pos.add(doc);
				classesPOS.put(doc.getCategory(), pos);

				classesNEG.put(doc.getCategory(), new ArrayList<Document>());
			} else {
				// Add document to class POSITIVES
				classesPOS.get(doc.getCategory()).add(doc);
			}
		}

		// For each class, extract class NEGATIVES
		for (Map.Entry<String, Map<String, MutableDouble>> clazz : classes
				.entrySet()) {
			List<Document> neg = classesNEG.get(clazz.getKey());

			for (Document doc : documents) {
				if (!doc.getCategory().equals(clazz.getKey()))
					neg.add(doc);
			}

		}

		double beta = 16;
		double gamma = 4;

		// For each class, extract features values
		for (Map.Entry<String, Map<String, MutableDouble>> clazz : classes
				.entrySet()) {

			// For each feature
			for (Map.Entry<String, MutableDouble> feature : clazz.getValue()
					.entrySet()) {

				// For each POS
				double pos = 0;
				int posSize = classesPOS.get(clazz.getKey()).size();
				for (Document doc : classesPOS.get(clazz.getKey())) {
					pos += doc.getCollectionTermWeight().get(feature.getKey())
							.getValue()
							/ posSize;
				}
				pos = pos * beta;

				// For each NEG
				double neg = 0;
				int negSize = classesNEG.get(clazz.getKey()).size();
				for (Document doc : classesNEG.get(clazz.getKey())) {
					pos += doc.getCollectionTermWeight().get(feature.getKey())
							.getValue()
							/ negSize;
				}
				neg = neg * gamma;

				feature.getValue().setValue(pos - neg);
			}

		}

		return classes;
	}
}
