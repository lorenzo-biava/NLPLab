package it.unito.nlplap.semantics.rocchio;

import it.unito.nlplap.semantics.rocchio.utils.Document;
import it.unito.nlplap.semantics.utils.MutableDouble;
import it.unito.nlplap.semantics.utils.MutableInt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RocchioClassifier {

	private static final Logger LOG = LogManager
			.getLogger(RocchioClassifier.class);

	Map<String, MutableInt> terms = new HashMap<String, MutableInt>();
	Map<String, MutableInt> featureVector = new HashMap<String, MutableInt>();
	Map<String, MutableDouble> idf = new HashMap<String, MutableDouble>();
	Map<String, Map<String, MutableDouble>> rocchioClasses;

	/**
	 * 
	 * @param trainingDocuments
	 *            documents for training. It is required that they contain a
	 *            correct category and an already lemmatized term list. Further
	 *            processing will be performed to calculate Term-Frequency
	 *            related to the collection.
	 * @param pruningThreshold
	 *            a threshold to prune terms with IDF below. Set to 0 to
	 *            disable.
	 */
	public RocchioClassifier(List<Document> trainingDocuments,
			double pruningThreshold) {
		// Train the classificator
		train(trainingDocuments, pruningThreshold);
	}

	protected void train(List<Document> documents, double pruningThreshold) {

		LOG.info("Training in progress...");

		int documentCount = documents.size();

		LOG.info(String.format("Training: Total docs=%d", documentCount));
		// Extract collection terms (feature vector) - union
		// Calculate terms IDF
		for (Document doc : documents) {
			for (Map.Entry<String, MutableInt> term : doc.getTermCount()
					.entrySet()) {
				if (!idf.containsKey(term.getKey()))
					idf.put(term.getKey(), new MutableDouble(1));
				else
					idf.get(term.getKey()).increment();
			}
		}
		for (Map.Entry<String, MutableDouble> term : idf.entrySet()) {
			term.getValue().setValue(
					Math.log(documentCount / term.getValue().getValue()));
		}
		if (LOG.isDebugEnabled())
			LOG.debug(String.format("Training: Total IDF='%d', idf=[%s]", idf
					.size(), it.unito.nlplap.semantics.utils.Utils
					.sortByComparator(idf, true)));

		// Extract collection terms (feature vector)
		// Only relevant terms (i.e. idf > 0)
		for (Map.Entry<String, MutableDouble> term : idf.entrySet()) {
			if (term.getValue().getValue() >= pruningThreshold)
				terms.put(term.getKey(), new MutableInt(0));
		}
		featureVector = it.unito.nlplap.semantics.utils.Utils.clone(terms);

		LOG.debug(String.format("Training: Total terms='%d', terms=[%s]",
				terms.size(), terms));

		LOG.info("Training: Computing documents features...");
		// For each document, extract term frequency
		int i = 0;
		for (Document doc : documents) {
			i++;
			LOG.info(String
					.format("Training: Extracting features of document '%s' - %d/%d - %d %%",
							doc.getName(), i, documentCount,
							(int) (i * 100 / documentCount)));

			doc = computeDocumentFeatures(doc, idf);
		}

		// Extract Rocchio classes
		LOG.info("Training: Computing classes");

		rocchioClasses = RocchioClassifier.extractRocchioClasses(documents,
				terms);
		for (Map.Entry<String, Map<String, MutableDouble>> clazz : rocchioClasses
				.entrySet()) {
			if (LOG.isDebugEnabled())
				LOG.debug(String
						.format("Rocchio Class '%s' features=[%s]",
								clazz.getKey(),
								trimLog(it.unito.nlplap.semantics.utils.Utils
										.sortByComparator(clazz.getValue(),
												true), 256)));
			LOG.info(String.format("Training: extracted Rocchio class '%s'",
					clazz.getKey()));
		}
		LOG.info(String.format("Training: Total classes=%d",
				rocchioClasses.size()));

		LOG.info("Training complete");
	}

	public Document computeDocumentFeatures(Document doc) {
		return computeDocumentFeatures(doc, idf);
	}

	public Document computeDocumentFeatures(Document doc,
			Map<String, MutableDouble> idf) {
		doc.setCollectionTermCount(it.unito.nlplap.semantics.utils.Utils
				.clone(terms));

		// Term Count
		doc.getCollectionTermCount().putAll(doc.getTermCount());

		// Term frequency
		for (Map.Entry<String, MutableInt> term : terms.entrySet()) {
			double value = 0;
			if (doc.getTerms().size() > 0)
				value = ((double) doc.getCollectionTermCount().get(term.getKey())
						.getValue()) / doc.getTerms().size();
			doc.getCollectionTermFrequency().put(term.getKey(),
					new MutableDouble(value));
		}

		doc.setCollectionTermWeight(it.unito.nlplap.semantics.utils.Utils
				.clone(doc.getCollectionTermFrequency()));

		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format(
					"Document '%s' termCount=[%s]",
					doc.getName(),
					trimLog(it.unito.nlplap.semantics.utils.Utils
							.sortByComparator(doc.getTermCount(), true), 256)));
			LOG.debug(String.format(
					"Document '%s' termFrequency=[%s]",
					doc.getName(),
					trimLog(it.unito.nlplap.semantics.utils.Utils
							.sortByComparator(doc.getCollectionTermFrequency(),
									true), 256)));
		}

		if (idf != null) {
			for (Map.Entry<String, MutableDouble> term : doc
					.getCollectionTermWeight().entrySet())
				term.getValue().setValue(
						term.getValue().getValue()
								* idf.get(term.getKey()).getValue());
		}

		if (LOG.isDebugEnabled())
			LOG.debug(String.format(
					"Document '%s' termWeight=[%s]",
					doc.getName(),
					trimLog(it.unito.nlplap.semantics.utils.Utils
							.sortByComparator(doc.getCollectionTermWeight(),
									true), 256)));

		return doc;
	}

	public Map<String, MutableInt> getFeatureVector() {
		return featureVector;
	}

	public ClassificationResult classify(Document document) {
		// Test each Rocchio class
		double bestScore = 0;
		String bestClass = null;
		for (Map.Entry<String, Map<String, MutableDouble>> clazz : rocchioClasses
				.entrySet()) {
			double score = RocchioClassifier.cosineSimilarity(
					document.getCollectionTermWeight(), clazz.getValue());
			if (score > bestScore) {
				bestScore = score;
				bestClass = clazz.getKey();
			}
		}

		return new ClassificationResult(bestClass, bestScore);
	}

	public static double cosineSimilarity(Map<String, MutableDouble> wd,
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
				classes.put(doc.getCategory(),
						it.unito.nlplap.semantics.utils.Utils.clone(features));
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

	public static String trimLog(Object obj, int maxSize) {
		String text = obj.toString();
		return text.length() > maxSize ? text.substring(0, maxSize) + "..."
				: text;
	}
}
