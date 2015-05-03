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
		private Map<String, MutableInt> termFrequency = new HashMap<String, Main.MutableInt>();

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

		public Map<String, MutableInt> getTermFrequency() {
			return termFrequency;
		}

		public void setTermFrequency(Map<String, MutableInt> termFrequency) {
			this.termFrequency = termFrequency;
		}

		public List<String> getTerms() {
			return terms;
		}

		public void setTerms(List<String> terms) {
			this.terms = terms;
		}

	}

	public static class MutableInt implements Cloneable, Comparable<MutableInt> {
		int value = 1;

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

		public int get() {
			return value;
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
			if(o.get()>value)
				return -1;
			else if(o.get()<value)
					return 1;
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {

		List<Document> documents = new ArrayList<Document>();

		File docDir = new File(DOCUMENT_DIR_PATH);
		int limit = -999;
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
						.getAbsolutePath(), text, FeatureVectorUtils
						.getLemmas(text, Locale.ITALIAN), category));
			}
		}

		Map<String, MutableInt> terms = new HashMap<String, MutableInt>();
		for (Document doc : documents) {
			for (String term : doc.getTerms())
				terms.put(term, new MutableInt(0));
		}
		
		LOG.info(String.format(
				"Total terms '%d', terms=[%s]", terms.size(), terms));


		for (Document doc : documents) {
			doc.setTermFrequency(clone(terms));

			for (String term : doc.getTerms())
				doc.getTermFrequency().get(term).increment();

			LOG.debug(String.format(
					"Document '%s' termFrequency=[%s]", doc.getName(), sortByComparator(doc.getTermFrequency(),true)));

			
		}
		LOG.info("ENDED");
	}

	private static <A extends Comparable<A>> Map<String, A> sortByComparator(
			Map<String, A> unsortMap, boolean reverse) {

		final boolean rev=reverse;
		
		// Convert Map to List
		List<Map.Entry<String, A>> list = new LinkedList<Map.Entry<String, A>>(
				unsortMap.entrySet());

		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, A>>() {
			public int compare(Map.Entry<String, A> o1, Map.Entry<String, A> o2) {
				return (o1.getValue()).compareTo(o2.getValue())*(rev?-1:1);
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
}
