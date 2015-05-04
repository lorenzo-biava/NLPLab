package it.unito.nlplap.semantics.utils.lemmatizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Scanner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class MorphItLemmatizer {

	public static final String MORPHIT_PATH = "morph-it_048_UTF8.txt";

	Multimap<String, MorphItData> lemmas = HashMultimap.create();

	public MorphItLemmatizer() throws FileNotFoundException {
		this.lemmas = loadMorphItData(MORPHIT_PATH);
	}

	public enum PoSType {
		NOUN, ADV, ADJ, VERB, DEFAULT, ALL("");

		String tag;

		private PoSType() {
			this.tag = this.name();
		}

		private PoSType(String tag) {
			this.tag = tag;
		}

		public String getTag() {
			return tag;
		};
	}

	public MorphItData findLemmaByPoS(Collection<MorphItData> lemmas,
			PoSType pos) {
		for (MorphItData lemma : lemmas) {
			if (lemma.getPos().contains(pos.getTag()))
				return lemma;
		}
		return null;
	}

	public MorphItData getLemma(String word, PoSType pos) {
		Collection<MorphItData> lemmaList = lemmas.get(word.toLowerCase());
		MorphItData lemma = null;
		if (pos == PoSType.DEFAULT) {
			lemma = findLemmaByPoS(lemmaList, PoSType.NOUN);
			if (lemma == null)
				lemma = findLemmaByPoS(lemmaList, PoSType.ALL);
		} else
			lemma = findLemmaByPoS(lemmaList, pos);

		return lemma;
	}

	public MorphItData getLemma(String word) {
		return getLemma(word, PoSType.DEFAULT);
	}

	public String getLemmaString(String word, PoSType pos) {
		MorphItData data = getLemma(word, pos);
		return data != null ? data.getLemma() : word;
	}

	public String getLemmaString(String word) {
		return getLemmaString(word, PoSType.DEFAULT);
	}

	public static Multimap<String, MorphItData> loadMorphItData(String path)
			throws FileNotFoundException {
		Multimap<String, MorphItData> lemmas = HashMultimap.create();

		Scanner sc = null;

		try {
			try {
				sc = new Scanner(
						MorphItLemmatizer.class.getResourceAsStream(path));
			} catch (Exception ex) {
			}
			if (sc == null)
				sc = new Scanner(new File(path));
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				String lineSplit[] = line.split("\t");

				String extra = null;
				String pos = "";
				if (lineSplit.length > 2) {
					extra = lineSplit[2];
					int i = extra.indexOf(":");
					pos = (i > 0 ? lineSplit[2].substring(0, i) : lineSplit[2]);
				}
				if (lineSplit.length < 2)
					System.out.println(line);
				lemmas.put(lineSplit[0], new MorphItData(lineSplit[1],
						lineSplit[0], pos, extra));
			}
		} finally {
			if (sc != null)
				sc.close();
		}

		return lemmas;
	}
}
