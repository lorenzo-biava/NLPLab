package it.unito.nlplab.semantics.wsd;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.util.StringUtils;

public class Sense {
	private String name;
	private String id;
	private List<String> glosses = new ArrayList<String>();
	private List<String> examples = new ArrayList<String>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getGloss() {
		if (this.glosses == null || this.glosses.size() < 1)
			return null;

		return glosses.get(0);
	}

	public void setGloss(String gloss) {
		this.glosses = new ArrayList<String>();
		this.glosses.add(gloss);
	}

	public List<String> getGlosses() {
		return glosses;
	}

	public void setGlosses(List<String> glosses) {
		this.glosses = glosses;
	}

	public List<String> getExamples() {
		return examples;
	}

	public void setExamples(List<String> examples) {
		this.examples = examples;
	}

	@Override
	public String toString() {
		return String.format("Sense[id=%s, name=%s, glosses=%s, examples=%s]",id, name,
				StringUtils.join(glosses, ", "),
				StringUtils.join(examples, ", "));
	}
	
	public String pprint(int startTabs) {
		StringBuilder pp = new StringBuilder();
		pp.append(String.format(getTabs(startTabs + 1) + "Id: %s", getId()));
		pp.append(String.format(getTabs(startTabs + 1) + "Name: %s", getName()));
		pp.append(String.format(getTabs(startTabs + 1) + "Glosses: %d",
				getGlosses().size()));
		for (String gloss : getGlosses())
			pp.append(String.format(getTabs(startTabs + 2) + "%s", gloss));
		pp.append(String.format(getTabs(startTabs + 1) + "Examples: %d",
				getExamples().size()));
		for (String example : getExamples())
			pp.append(String.format(getTabs(startTabs + 2) + "%s", example));
		
		return pp.toString();
	}
	
	protected String getTabs(int size) {
		String tabs = "\n";
		while (--size > 0)
			tabs += "\t";
		return tabs;

	}
}