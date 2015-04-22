package com.tsavo.arbi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.joda.time.Duration;
import org.junit.Test;

import com.tsavo.hippo.ExponentialWeightedMovingAverageFunction;
import com.tsavo.hippo.LiveTickerReader;
import com.tsavo.hippo.OHLCVDataSet;
import com.tsavo.trade.AbstractSignal.SignalTestResults;
import com.tsavo.trade.ThresholdSignal;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Trade;

public class ThresholdSignalTest {

	volatile SignalTestResults performance = new SignalTestResults();

	
	public static class SignalParameters{
		public long time;
		public int window;
		public float crossAbove;
		public float counterCrossAbove;
		public float crossBelow;
		public float counterCrossBelow;
		public float target;
		public float stop;
	}
	@Test
	public void testThresholdSignal() {
		LiveTickerReader ticker = new LiveTickerReader("BitFinex");
		SortedSet<Trade> rawPriceData = ticker.getDataForTimeframe(new CurrencyPair("BTC", "USD"));
		List<BigDecimal> outputs = new ArrayList<>();
		List<Long> times = new ArrayList<>();
		for (long time = 1000 * 10 * 10; time < 1000 * 30 * 10; time += 1000 * 5) {
			times.add(time);
		}
		List<Integer> windows = new ArrayList<>();
		for (int window = 5; window < 15; window++) {
			windows.add(window);
		}
		times.parallelStream().forEach(
				time -> {
					OHLCVDataSet data = new OHLCVDataSet(rawPriceData, new Duration(time));
					List<BigDecimal> priceCloses = data.stream().map(x -> x.close).collect(Collectors.<BigDecimal> toList());
					windows.parallelStream().forEach(
							window -> {
								OHLCVDataSet average = data.difference().average(() -> new ExponentialWeightedMovingAverageFunction(window));
								List<BigDecimal> averageCloses = average.stream().map(x -> x.close).collect(Collectors.<BigDecimal> toList());
								for (float cross = 1; cross < 6; cross += .1) {
									for (float counterCross = 0; counterCross < cross; counterCross += 0.1) {
										ThresholdSignal signal = new ThresholdSignal("MomentumTrader", new BigDecimal(cross), new BigDecimal(counterCross), new BigDecimal(cross
												* -1), new BigDecimal(counterCross * -1));
										for (float target = 2.1f; target < 5; target += 0.1) {
											BigDecimal bigTarget = new BigDecimal(target);
											for (float stop = -1f; stop > -2; stop -= 0.1) {
												SignalTestResults test = signal.test(averageCloses, priceCloses, bigTarget, new BigDecimal(stop));
												if (test.performance.compareTo(performance.performance) > 0) {
													performance = test;
													System.out.println("Time: " + time + " Window: " + window + " Above: " + cross + " CounterAbove: " + counterCross + " Target: "
															+ target + " StopLoss: " + stop);
													System.out.println("Total performance: " + performance.performance.setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros()
															+ " RunUp " + performance.runUp.setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros() + " RunDown "
															+ performance.runDown.setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros() + " Trades " + performance.trades
															+ " Right " + performance.right + " Wrong " + performance.wrong);
												}
											}
										}
									}
								}
							});

				});
		Collections.sort(outputs);
		Collections.reverse(outputs);
		System.out.println("----");
		System.out.println(outputs.iterator().next());

	}

}
