package it.unito.nlplab.semantics.rocchio.utils;

/**
 * A class to conveniently store a classification result (in terms of best class
 * name and related score).
 *
 */
public class ClassificationResult {
	private String bestClass;
	private double bestScore;

	public ClassificationResult(String bestClass, double bestScore) {
		super();
		this.bestClass = bestClass;
		this.bestScore = bestScore;
	}

	public String getBestClass() {
		return bestClass;
	}

	public void setBestClass(String bestClass) {
		this.bestClass = bestClass;
	}

	public double getBestScore() {
		return bestScore;
	}

	public void setBestScore(double bestScore) {
		this.bestScore = bestScore;
	}
}