package com.tsavo.trade;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class AbstractSignal implements Signal {

	public static class SignalTestResults {
		public BigDecimal performance = BigDecimal.ZERO;
		public BigDecimal runUp = BigDecimal.ZERO;
		public BigDecimal runDown = BigDecimal.ZERO;
		public int trades = 0;
		public int right = 0, wrong = 0, shorts = 0, longs = 0;

		@Override
		public String toString() {
			return new ToStringBuilder(this).append("Performance", performance).append("Run Up", runUp).append("Run Down", runDown).append("Trades", trades).append("Rights", right)
					.append("Wrongs", wrong).append("Shorts", shorts).append("Longs", longs).toString();
		}
	}

	public SignalTestResults test(List<BigDecimal> signalData, List<BigDecimal> tradePrices, BigDecimal aTarget, BigDecimal aStopLoss) {
		SignalTestResults results = new SignalTestResults();
		BigDecimal entryPrice = BigDecimal.ZERO;
		boolean shortState = false, longState = false;

		for (int x = 0; x < signalData.size(); x++) {
			BigDecimal signalItem = signalData.get(x);
			BigDecimal tradePrice = tradePrices.get(x);

			if (longState) {
				double diff = tradePrice.doubleValue() - entryPrice.doubleValue();
				if (diff > aTarget.doubleValue() || diff < aStopLoss.doubleValue()) {
					longState = false;
					results.performance = results.performance.add(new BigDecimal(diff).multiply(new BigDecimal(0.95)));
					if (diff > 0) {
						results.runUp = results.runUp.add(new BigDecimal(diff).multiply(new BigDecimal(0.95)));
						results.right++;
					} else {
						results.runDown = results.runDown.add(new BigDecimal(diff).multiply(new BigDecimal(0.95)));
						results.wrong++;
					}
					results.trades++;
				}
			}

			if (shortState) {
				double diff = (tradePrice.doubleValue() - entryPrice.doubleValue()) * -1;
				if (diff > aTarget.doubleValue() || diff < aStopLoss.doubleValue()) {
					shortState = false;
					results.performance = results.performance.add(new BigDecimal(diff).multiply(new BigDecimal(0.95)));
					if (diff > 0) {
						results.runUp = results.runUp.add(new BigDecimal(diff).multiply(new BigDecimal(0.95)));
						results.right++;
					} else {
						results.runDown = results.runDown.add(new BigDecimal(diff).multiply(new BigDecimal(0.95)));
						results.wrong++;
					}
					results.trades++;
				}
			}

			addSample(signalItem);

			if (longState && isShort()) {
				longState = false;
				results.performance = results.performance.add(tradePrice.subtract(entryPrice).multiply(new BigDecimal(1.05)));
				double diff = tradePrice.doubleValue() - entryPrice.doubleValue();
				if (diff > 0) {
					results.runUp = results.runUp.add(new BigDecimal(diff).multiply(new BigDecimal(1.05)));
					results.right++;
				} else {
					results.runDown = results.runDown.add(new BigDecimal(diff).multiply(new BigDecimal(1.05)));
					results.wrong++;
				}
				results.trades++;

			}

			if (shortState && isLong()) {
				shortState = false;
				results.performance = results.performance.subtract(tradePrice.subtract(entryPrice).multiply(new BigDecimal(1.05)));
				double diff = (tradePrice.doubleValue() - entryPrice.doubleValue()) * -1;
				if (diff > 0) {
					results.runUp = results.runUp.add(new BigDecimal(diff).multiply(new BigDecimal(1.05)));
					results.right++;
				} else {
					results.runDown = results.runDown.add(new BigDecimal(diff).multiply(new BigDecimal(1.05)));
					results.wrong++;
				}
				results.trades++;
			}

			if (!longState && isLong()) {
				entryPrice = tradePrice;
				longState = true;
				results.longs++;
			}
			if (!shortState && isShort()) {
				entryPrice = tradePrice;
				shortState = true;
				results.shorts++;
			}
		}
		reset();
		return results;
	}
}
