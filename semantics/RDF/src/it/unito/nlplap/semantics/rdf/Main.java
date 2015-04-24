package it.unito.nlplap.semantics.rdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.DC;

public class Main {

	public static final String DOCS_DIR = "data/news_collection";

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

	public static void main(String[] args) throws FileNotFoundException {
		start();
	}

	public static void start() throws FileNotFoundException {

		// create an empty Model
		Model model = ModelFactory.createDefaultModel();

		File[] docs = new File(DOCS_DIR).listFiles();
		for (File doc : docs) {
			// Extracting features
			DocFeatures docFeatures = extractDocFeatures(doc);

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
	}

	public static DocFeatures extractDocFeatures(File doc)
			throws FileNotFoundException {
		DocFeatures docFeatures = new DocFeatures();

		Scanner sc = new Scanner(doc);
		int lineN = 0;

		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			lineN++;

			if (lineN == 2)
				docFeatures.setUri(line.substring(2));

			if (lineN == 5)
				docFeatures.setTitle(line);

			if (lineN == 7)
				docFeatures.setDescription(line);

			if (line.contains("Page last updated"))
				docFeatures.setDate(line);
		}

		docFeatures.setSubject("");
		docFeatures.setPublisher("BBC");
		
		if (new Random().nextInt(100) < 20)
			docFeatures.setCreator("Anakin Skywalker");
		else
			docFeatures.setCreator("Lorenzo Biava");

		sc.close();

		return docFeatures;
	}
}
