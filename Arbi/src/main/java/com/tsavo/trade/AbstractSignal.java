package com.tsavo.trade;

import java.math.BigDecimal;
import java.util.List;

public abstract class AbstractSignal implements Signal {

	public BigDecimal test(List<BigDecimal> signalData, List<BigDecimal> tradePrices, BigDecimal aTarget, BigDecimal aStopLoss) {
		BigDecimal performance = BigDecimal.ZERO;

		BigDecimal entryPrice = BigDecimal.ZERO;
		boolean shortState = false, longState = false;

		for (int x = 0; x < signalData.size(); x++) {
			BigDecimal signalItem = signalData.get(x);
			BigDecimal tradePrice = tradePrices.get(x);

			if (longState) {
				double diff = tradePrice.doubleValue() - entryPrice.doubleValue();
				if (diff > aTarget.doubleValue() || diff < aStopLoss.doubleValue()) {
					longState = false;
					performance = performance.add(new BigDecimal(diff).multiply(new BigDecimal(0.95)));
				}
			}

			if (shortState) {
				double diff = (tradePrice.doubleValue() - entryPrice.doubleValue()) * -1;
				if (diff > aTarget.doubleValue() || diff < aStopLoss.doubleValue()) {
					shortState = false;
					performance = performance.add(new BigDecimal(diff).multiply(new BigDecimal(0.95)));
				}
			}

			addSample(signalItem);

			if (longState && isShort()) {
				longState = false;
				performance = performance.add(tradePrice.subtract(entryPrice).multiply(new BigDecimal(1.05)));
			}

			if (shortState && isLong()) {
				shortState = false;
				performance = performance.subtract(tradePrice.subtract(entryPrice).multiply(new BigDecimal(1.05)));
			}

			if (!longState && isLong()) {
				entryPrice = tradePrice;
				longState = true;
			}
			if (!shortState && isShort()) {
				entryPrice = tradePrice;
				shortState = true;
			}
		}
		return performance;
	}
}
