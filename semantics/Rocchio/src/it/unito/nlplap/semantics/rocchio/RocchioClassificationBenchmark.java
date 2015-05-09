package it.unito.nlplap.semantics.rocchio;

import it.unito.nlplap.semantics.rocchio.utils.Document;
import it.unito.nlplap.semantics.utils.FeatureVectorUtils;
import it.unito.nlplap.semantics.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RocchioClassificationBenchmark {

	private static final Logger LOG = LogManager
			.getLogger(RocchioClassificationBenchmark.class);

	private static final String DOCUMENT_DIR_PATH = "data/docs_200";

	public static void main(String[] args) throws Exception {
		File docDir = new File(DOCUMENT_DIR_PATH);

		double testsetRatio = 0.10;
		List<Document> dataSet = loadDocs(docDir, Locale.ITALIAN);
		List<Document> testSet = new ArrayList<Document>();
		List<Document> trainingSet = new ArrayList<Document>();

		datasetSplit(dataSet, testsetRatio, trainingSet, testSet);

		RocchioClassifier rc = new RocchioClassifier(trainingSet);

		// Classify docs
		int correctCount = 0;
		int wrongCount = 0;
		for (Document doc : testSet) {
			ClassificationResult cr = rc.classify(rc
					.computeDocumentFeatures(doc));

			LOG.info(String.format(
					"Document '%s', correctClass=%s, bestClass=%s, score=%s",
					doc.getName(), doc.getCategory(), cr.getBestClass(),
					cr.getBestScore()));
			if (doc.getCategory().equals(cr.getBestClass()))
				correctCount++;
			else
				wrongCount++;
		}

		LOG.info(String.format(
				"Total docs=%d, Correctly classified=%d, Badly classified=%d",
				testSet.size(), correctCount, wrongCount));
	}

	public static <T> void datasetSplit(List<T> dataSet, double testsetRatio,
			List<T> trainingSet, List<T> testSet) {
		int testSetSize = (int) Math.floor(dataSet.size() * testsetRatio);

		testSet.clear();
		trainingSet.clear();

		trainingSet.addAll(dataSet);

		Random r = new Random(System.currentTimeMillis());
		for (int i = testSetSize; i > 0; i--) {
			int index = r.nextInt(trainingSet.size());
			testSet.add(trainingSet.get(index));
			trainingSet.remove(index);
		}
	}

	public static List<Document> loadDocs(File docDir, Locale locale)
			throws Exception {
		List<Document> documents = new ArrayList<Document>();

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
						text, locale), category));
			}
		}

		return documents;
	}
}