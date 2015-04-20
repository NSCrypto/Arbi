package com.tsavo.arbi;

import java.math.BigDecimal;
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
import com.tsavo.trade.ThresholdSignal;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Trade;

public class ThresholdSignalTest {

	BigDecimal performance = BigDecimal.ZERO;


	@Test
	public void testThresholdSignal() {
		LiveTickerReader ticker = new LiveTickerReader("BitFinex");
		SortedSet<Trade> rawPriceData = ticker.getDataForTimeframe(new CurrencyPair("BTC", "USD"));
		List<BigDecimal> outputs = new ArrayList<>();
		List<Long> times = new ArrayList<>();
		for (long time = 1000 * 60; time < 1000 * 60 * 300; time += 1000 * 60 * 15) {
			times.add(time);
		}
		times.parallelStream().forEach(
				time -> {
					OHLCVDataSet data = new OHLCVDataSet(rawPriceData, new Duration(time));
					for (int window = 1; window < 10; window++) {
						final int finalWindow = window;
						OHLCVDataSet average = data.difference().average(() -> new ExponentialWeightedMovingAverageFunction(finalWindow));
						for (int cross = 1; cross < 30; cross += 1) {
							for (int counterCross = 1; counterCross < cross; counterCross += 1) {
								ThresholdSignal signal = new ThresholdSignal(new BigDecimal(cross), new BigDecimal(counterCross), new BigDecimal(cross * -1), new BigDecimal(
										counterCross * -1));
								for (float target = 0.5f; target < 5; target += 0.1) {
									for (float stop = -0.5f; stop > -5; stop -= 0.1) {
										BigDecimal test = signal.test(average.stream().map(x -> x.close).collect(Collectors.<BigDecimal> toList()), data.stream().map(x -> x.close)
												.collect(Collectors.<BigDecimal> toList()), new BigDecimal(target), new BigDecimal(stop));
										if (test.compareTo(performance) > 0) {
											performance = test;
											System.out.println("Time: " + time + " Window: " + window + " Above: " + cross + " CounterAbove: " + counterCross + " Target: "
													+ target + " StopLoss: " + stop);
											System.out.println(test);
										}
									}
								}
							}
						}
					}

				});
		Collections.sort(outputs);
		Collections.reverse(outputs);
		System.out.println("----");
		System.out.println(outputs.iterator().next());

	}

}
