package com.tsavo.trade.signal;

import java.util.Random;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class IntegerOptimizableParameter implements OptimizableParameter<Integer> {
	int value, lowerRange, upperRange, range;
	int step;

	public IntegerOptimizableParameter(int value, int lowerRange, int upperRange, int range, int step) {
		super();
		this.value = value;
		this.lowerRange = lowerRange;
		this.upperRange = upperRange;
		this.range = range;
		this.step = step;
	}

	@Override
	public void mutate(Random rng) {
		if (rng.nextBoolean()) {
			value += step * rng.nextInt(range);
		} else {
			value -= step * rng.nextInt(range);
		}
		value = Math.min(Math.max(value, lowerRange), upperRange);
	}

	@Override
	public void set(Integer aT) {
		value = aT;
	}

	@Override
	public Integer get() {
		return value;
	}
	@Override
	public String toString() {
		return new Integer(value).toString();
	}
	
	
	@Override
	public boolean equals(Object obj) {

		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		IntegerOptimizableParameter rhs = (IntegerOptimizableParameter) obj;
		return new EqualsBuilder().append(value, rhs.value).isEquals();
	}
	
	@Override
	public int hashCode() {
		return new Integer(value).hashCode();
	}
}
