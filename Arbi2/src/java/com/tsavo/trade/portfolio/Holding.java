package com.tsavo.trade.portfolio;

public class Holding implements Comparable<Holding> {
	float amount;
	float value;

	public Holding() {
	}

	public int compareTo(Holding o) {
		return new Float((getValue() * 100000000f) - (o.getValue() * 100000000f)).intValue();
	}

	public Holding(float anAmount, float aValue) {
		amount = anAmount;
		value = aValue;
	}

	public float getValue() {
		return value;
	}

	public float getAmount() {
		return amount;
	}

	public void setAmount(float amount) {
		this.amount = amount;
	}

	public void setValue(float value) {
		this.value = value;
	}
}