package it.unito.nlplap.semantics.rdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.DC;

public class SPARQLQuerySample {

	public static void main(String[] args) throws FileNotFoundException {
		start();
	}

	public static void start() throws FileNotFoundException {
		Model model = ModelFactory.createDefaultModel();

		// read the RDF/XML file
		model.read(new FileInputStream(new File(Main.RDF_FILE)), null);

		String author = "Anakin Skywalker";
		searchByAuthor(model, author);

		searchByTitle(model, "Unavailable Title");

		searchByTitle(model, "Avatar breaks US DVD sales record");
	}

	public static void searchByAuthor(Model model, String author) {
		String queryString = String
				.format("PREFIX dc:  <%s> SELECT ?title WHERE {?d dc:title ?title . ?d dc:creator '%s'}",
						DC.NAMESPACE, author);
		Query query = QueryFactory.create(queryString);
		System.out.println("Documents created by: " + author);

		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ResultSet results = qexec.execSelect();

		int i = 0;
		for (; results.hasNext();) {
			QuerySolution soln = results.nextSolution();
			RDFNode title = soln.get("title"); // Get a result variable by name.
			System.out.println(String.format("(%d) \t Title: %s", ++i, title));
		}

		System.out.println(String.format("\n----\t Total: %d \t----\n", i));
	}

	public static void searchByTitle(Model model, String title) {
		String queryString = String
				.format("PREFIX dc:  <%s> SELECT ?description WHERE {?d dc:description ?description . ?d dc:title '%s'}",
						DC.NAMESPACE, title);
		Query query = QueryFactory.create(queryString);
		System.out.println("Documents with title: " + title);

		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ResultSet results = qexec.execSelect();

		int i = 0;
		for (; results.hasNext();) {
			QuerySolution soln = results.nextSolution();
			RDFNode description = soln.get("description"); // Get a result
															// variable by name.
			System.out.println(String.format("(%d) \t Description: %s", ++i,
					description));
		}

		System.out.println(String.format("\n----\t Total: %d \t----\n", i));
	}

}
