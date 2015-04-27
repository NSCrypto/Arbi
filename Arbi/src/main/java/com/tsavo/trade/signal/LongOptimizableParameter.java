package com.tsavo.trade.signal;

import java.util.Random;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class LongOptimizableParameter implements OptimizableParameter<Long> {
	long value, lowerRange, upperRange, step;
	int range;

	public LongOptimizableParameter(long value, long lowerRange, long upperRange, int range, long step) {
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
	public void set(Long aT) {
		value = aT;
	}

	@Override
	public Long get() {
		return value;
	}

	@Override
	public String toString() {
		return new Long(value).toString();
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
		LongOptimizableParameter rhs = (LongOptimizableParameter) obj;
		return new EqualsBuilder().append(value, rhs.value).isEquals();
	}

	@Override
	public int hashCode() {
		return new Long(value).hashCode();
	}
}
