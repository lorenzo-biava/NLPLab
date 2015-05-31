package it.unito.nlplap.semantics.utils;



public class MutableDouble implements Cloneable,
		Comparable<MutableDouble> {
	private double value = 1;

	/**
	 * Default value = 1;
	 */
	public MutableDouble() {

	}

	/**
	 * Decide initial value
	 * 
	 * @param value
	 */
	public MutableDouble(double value) {
		this.value = value;
	}

	public void increment() {
		++value;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public Object clone() {
		return new MutableDouble(this.value);
	}

	@Override
	public String toString() {
		return value + "";
	}

	@Override
	public int compareTo(MutableDouble o) {
		if (o.getValue() > value)
			return -1;
		else if (o.getValue() < value)
			return 1;
		return 0;
	}
}