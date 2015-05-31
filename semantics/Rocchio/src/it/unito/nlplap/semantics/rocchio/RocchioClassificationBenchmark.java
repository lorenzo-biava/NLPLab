package it.unito.nlplap.semantics.rocchio;

import it.unito.nlplap.semantics.rocchio.utils.Document;
import it.unito.nlplap.semantics.utils.FeatureVectorUtils;
import it.unito.nlplap.semantics.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RocchioClassificationBenchmark {

	private static final Logger LOG = LogManager
			.getLogger(RocchioClassificationBenchmark.class);

	private static final String DOCUMENT_DIR_PATH_IT = "data/docs_200";
	private static final Locale DOCUMENTS_LANGUAGE_IT = Locale.ITALIAN;
	private static final String DOCUMENT_DIR_PATH_EN = "data/20_NGs_400";
	private static final Locale DOCUMENTS_LANGUAGE_EN = Locale.ENGLISH;

	public static void main(String[] args) throws Exception {

		boolean ita = false;

		String docDirPath;
		Locale docLang;
		File docDir;
		List<Document> dataSet;

		if (ita) {
			docDirPath = DOCUMENT_DIR_PATH_IT;
			docLang = DOCUMENTS_LANGUAGE_IT;

		} else {
			docDirPath = DOCUMENT_DIR_PATH_EN;
			docLang = DOCUMENTS_LANGUAGE_EN;
		}

		docDir = new File(docDirPath);
		dataSet = loadDocsInSubdirs(docDir, docLang);

		double testsetRatio = 0.10;

		List<Document> testSet = new ArrayList<Document>();
		List<Document> trainingSet = new ArrayList<Document>();

		Map<String, List<Document>> datasetInClasses = datasetSplitInClasses(dataSet);
		for (Map.Entry<String, List<Document>> dsClass : datasetInClasses
				.entrySet()) {
			datasetSplit(dsClass.getValue(), testsetRatio, trainingSet, testSet);
		}

		RocchioClassifier rc = new RocchioClassifier(trainingSet, 4);

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

		// testSet.clear();
		// trainingSet.clear();
		// trainingSet.addAll(dataSet);

		List<T> tmpTrainingSet = new ArrayList<T>();
		tmpTrainingSet.addAll(dataSet);

		Random r = new Random(System.currentTimeMillis());
		for (int i = testSetSize; i > 0; i--) {
			int index = r.nextInt(tmpTrainingSet.size());
			testSet.add(tmpTrainingSet.get(index));
			tmpTrainingSet.remove(index);
		}

		trainingSet.addAll(tmpTrainingSet);
	}

	/**
	 * A classification item aware of its class.
	 */
	public interface ClassificationClassAware<C> {
		public C getClassificationClass();
	}

	public static <C, T extends ClassificationClassAware<C>> Map<C, List<T>> datasetSplitInClasses(
			List<T> dataSet) {
		Map<C, List<T>> classes = new LinkedHashMap<C, List<T>>();

		for (T item : dataSet) {
			C clazz = item.getClassificationClass();
			if (!classes.containsKey(clazz))
				classes.put(clazz, new ArrayList<T>());

			classes.get(clazz).add(item);
		}

		return classes;
	}

	public static List<Document> loadDocs(File docDir, Locale locale)
			throws Exception {
		LOG.info(String.format(
				"Loading docs from folder '%s' and language '%s'",
				docDir.getAbsolutePath(), locale.getLanguage()));

		List<Document> documents = new ArrayList<Document>();

		int limit = -200;
		for (File file : docDir.listFiles()) {
			if (limit > 10)
				break;

			if (file.isFile() && file.getName().indexOf(".") != 0
					&& file.length() > 0) {
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

	public static List<Document> loadDocsInSubdirs(File docDir, Locale locale)
			throws Exception {
		LOG.info(String.format(
				"Loading docs from folder '%s' and language '%s'",
				docDir.getAbsolutePath(), locale.getLanguage()));

		List<Document> documents = new ArrayList<Document>();

		// int limit = -400;
		int i = 0;
		for (File dir : docDir.listFiles()) {
			// if (limit > 10)
			// break;

			if (dir.isDirectory()) {
				String category = dir.getName();

				for (File file : dir.listFiles()) {
					if (file.isFile() && file.getName().indexOf(".") != 0
							/*&& file.length() > 0*/) {
						i++;

						String text = Utils.fileToText(file);
						documents.add(new Document(file.getName(), file
								.getAbsolutePath(), text, FeatureVectorUtils
								.getLemmas(text, locale), category));

						LOG.debug(String.format("Loading doc %d, title '%s'",
								i, file.getName()));
					}
				}
			}
		}

		return documents;
	}
}