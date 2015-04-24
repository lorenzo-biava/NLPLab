package it.unito.nlplap.semantics.rdf;

import it.unito.nlplap.semantics.utils.FeatureVectorUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Scanner;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.DC;

import edu.stanford.nlp.util.StringUtils;

public class Main {

	public static final String DOCS_DIR = "data/news_collection";
	public static final String RDF_FILE = "data/news_collection.rdf";

	public static class DocFeatures {
		private String uri, title, subject, description, date, creator,
				publisher;

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getSubject() {
			return subject;
		}

		public void setSubject(String subject) {
			this.subject = subject;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getDate() {
			return date;
		}

		public void setDate(String date) {
			this.date = date;
		}

		public String getCreator() {
			return creator;
		}

		public void setCreator(String creator) {
			this.creator = creator;
		}

		public String getPublisher() {
			return publisher;
		}

		public void setPublisher(String publisher) {
			this.publisher = publisher;
		}

	}

	public static void main(String[] args) throws Exception {
		start();
	}

	public static void start() throws Exception {

		// create an empty Model
		Model model = ModelFactory.createDefaultModel();

		File[] docs = new File(DOCS_DIR).listFiles();

		int anakinCreator = 2;
		for (File doc : docs) {
			// Extracting features
			DocFeatures docFeatures = extractDocFeatures(doc);

			if (anakinCreator > 0) {
				docFeatures.setCreator("Anakin Skywalker");
				anakinCreator--;
			}

			// create the resource
			// and add the properties cascading style

			/* Resource newDoc = */
			model.createResource(docFeatures.getUri())
					.addProperty(DC.title, docFeatures.getTitle())
					.addProperty(DC.subject, docFeatures.getSubject())
					.addProperty(DC.description, docFeatures.getDescription())
					.addProperty(DC.date, docFeatures.getDate())
					.addProperty(DC.creator, docFeatures.getCreator())
					.addProperty(DC.publisher, docFeatures.getPublisher());
		}

		model.write(System.out);
		model.write(new FileOutputStream(new File(RDF_FILE)));
	}

	public static DocFeatures extractDocFeatures(File doc) throws Exception {
		DocFeatures docFeatures = new DocFeatures();

		Scanner sc = new Scanner(doc);
		int lineN = 0;
		StringBuilder contentBuilder = new StringBuilder();

		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			lineN++;

			if (lineN == 2)
				docFeatures.setUri(line.substring(2));

			if (lineN == 5)
				docFeatures.setTitle(line);

			if (lineN == 7)
				docFeatures.setDescription(line);

			if (lineN >= 7 && !line.contains("Page last updated"))
				contentBuilder.append(line);

			if (line.contains("Page last updated"))
				docFeatures.setDate(line);
		}

		List<String> feat = FeatureVectorUtils.getFeatureVector(contentBuilder
				.toString());
		docFeatures.setSubject(StringUtils.join(
				feat.subList(0, Math.min(3, feat.size())), ","));
		docFeatures.setPublisher("BBC");
		docFeatures.setCreator("Lorenzo Biava");

		sc.close();

		return docFeatures;
	}
}
