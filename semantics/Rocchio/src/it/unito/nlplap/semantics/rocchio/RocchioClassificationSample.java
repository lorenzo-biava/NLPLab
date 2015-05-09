package it.unito.nlplap.semantics.rocchio;

import it.unito.nlplap.semantics.rocchio.utils.Document;
import it.unito.nlplap.semantics.utils.FeatureVectorUtils;
import it.unito.nlplap.semantics.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RocchioClassificationSample {

	private static final Logger LOG = LogManager
			.getLogger(RocchioClassificationSample.class);

	private static final String DOCUMENT_DIR_PATH = "data/docs_200";

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

		RocchioClassifier rocchio = new RocchioClassifier(documents);

		// Classify docs
		limit = -200;
		int correctCount = 0;
		int wrongCount = 0;
		for (Document doc : documents) {
			if (limit > 0)
				break;

			ClassificationResult cr = rocchio.classify(doc);

			LOG.info(String.format(
					"Document '%s', correctClass=%s, bestClass=%s, score=%s",
					doc.getName(), doc.getCategory(), cr.getBestClass(),
					cr.getBestScore()));
			if (doc.getCategory().equals(cr.getBestClass()))
				correctCount++;
			else
				wrongCount++;

			limit++;
		}

		LOG.info(String.format(
				"Total docs=%d, Correctly classified=%d, Badly classified=%d",
				documents.size(), correctCount, wrongCount));
	}
}
