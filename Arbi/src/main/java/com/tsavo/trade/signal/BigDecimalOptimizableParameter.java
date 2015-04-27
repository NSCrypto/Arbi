package com.tsavo.trade.signal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class BigDecimalOptimizableParameter implements OptimizableParameter<BigDecimal> {
	BigDecimal value, lowerRange, upperRange, step;
	int range;

	public BigDecimalOptimizableParameter(BigDecimal aValue, BigDecimal aLowerRange, BigDecimal aUpperRange, int aRange, BigDecimal aStep) {
		super();
		this.value = aValue;
		this.lowerRange = aLowerRange;
		this.upperRange = aUpperRange;
		this.step = aStep;
		this.range = aRange;
	}

	@Override
	public void mutate(Random rng) {
		if (rng.nextBoolean()) {
			value = value.add(step.multiply(new BigDecimal(rng.nextInt(range)))).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
		} else {
			value = value.subtract(step.multiply(new BigDecimal(rng.nextInt(range)))).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
		}
		value = value.max(lowerRange).min(upperRange);
	}

	@Override
	public void set(BigDecimal aT) {
		this.value = aT;
	}

	@Override
	public BigDecimal get() {
		return value;
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
		BigDecimalOptimizableParameter rhs = (BigDecimalOptimizableParameter) obj;
		return new EqualsBuilder().append(value, rhs.value).isEquals();
	}
	
	@Override
	public String toString() {
		return value.toString();
	}

}
