package it.unito.nlplap.semantics.rocchio;

import it.unito.nlplap.semantics.rocchio.utils.Document;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RocchioClassificationSample {

	private static final Logger LOG = LogManager
			.getLogger(RocchioClassificationSample.class);

	private static final String DOCUMENT_DIR_PATH_IT = "data/docs_200";
	private static final Locale DOCUMENTS_LANGUAGE_IT = Locale.ITALIAN;
	private static final String DOCUMENT_DIR_PATH_EN = "data/20_NGs_400";
	private static final Locale DOCUMENTS_LANGUAGE_EN = Locale.ENGLISH;

	public static void main(String[] args) throws Exception {

		boolean ita = false;

		String docDirPath;
		Locale docLang;

		if (ita) {
			docDirPath = DOCUMENT_DIR_PATH_IT;
			docLang = DOCUMENTS_LANGUAGE_IT;

		} else {
			docDirPath = DOCUMENT_DIR_PATH_EN;
			docLang = DOCUMENTS_LANGUAGE_EN;
		}
		
		List<Document> documents = RocchioClassificationBenchmark
				.loadDocsInSubdirs(new File(docDirPath), docLang);

		RocchioClassifier rocchio = new RocchioClassifier(documents, 0);

		// Classify docs
		int limit = -200;
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
