package it.unito.nlplap.semantics.rocchio.utils;


public class MutableInt implements Cloneable, Comparable<MutableInt> {
	private int value = 1;

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

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
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
		if (o.getValue() > value)
			return -1;
		else if (o.getValue() < value)
			return 1;
		return 0;
	}
}