package com.tsavo.trade;

import java.math.BigDecimal;

public class ThresholdSignal extends AbstractSignal implements Signal {

	BigDecimal aboveCrossing, aboveCounterCrossing, belowCrossing, belowCounterCrossing;
	boolean crossedAbove = false, crossedBelow = false;
	boolean shortState = false, longState = false;

	public ThresholdSignal(BigDecimal aboveCrossing, BigDecimal aboveCounterCrossing, BigDecimal belowCrossing, BigDecimal belowCounterCrossing) {
		super();
		this.aboveCrossing = aboveCrossing;
		this.aboveCounterCrossing = aboveCounterCrossing;
		this.belowCrossing = belowCrossing;
		this.belowCounterCrossing = belowCounterCrossing;
	}

	public void addSample(BigDecimal aSample) {
		shortState = false;
		longState = false;
		if (aSample.compareTo(aboveCrossing) > 0) {
			crossedAbove = true;
		}
		if (aSample.compareTo(belowCrossing) < 0) {
			crossedBelow = true;
		}
		if (crossedAbove && aSample.compareTo(aboveCounterCrossing) < 0) {
			crossedAbove = false;
			shortState = true;
		}
		if (crossedBelow && aSample.compareTo(belowCounterCrossing) > 0) {
			crossedBelow = false;
			longState = true;
		}
	}

	@Override
	public boolean isLong() {
		return longState;
	}

	@Override
	public boolean isShort() {
		return shortState;
	}
}
