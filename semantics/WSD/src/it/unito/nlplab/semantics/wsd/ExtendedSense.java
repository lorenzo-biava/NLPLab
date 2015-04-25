package it.unito.nlplab.semantics.wsd;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.util.StringUtils;

public class ExtendedSense extends Sense {
	private List<Sense> relatedSenses = new ArrayList<Sense>();

	public List<Sense> getRelatedSenses() {
		return relatedSenses;
	}

	public void setRelatedSenses(List<Sense> relatedSenses) {
		this.relatedSenses = relatedSenses;
	}

	@Override
	public String toString() {
		return String
				.format("ExtendedSense[id=%s, name=%s, glosses=[%s], examples=[%s], relatedSenses=[%s]]",
						getId(), getName(), getGloss(),
						StringUtils.join(getExamples(), ", "),
						StringUtils.join(relatedSenses, ", "));
	}
}