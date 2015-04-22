package com.tsavo.arbi;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;

import com.tsavo.trade.CachingOrderBook;
import com.tsavo.trade.Cryptsy;
import com.tsavo.trade.ExchangeLimitOrder;
import com.tsavo.trade.opportunity.cycle.CurrencyCycle;
import com.tsavo.trade.opportunity.cycle.CycleFinder;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.bitfinex.v1.BitfinexExchange;
import com.xeiam.xchange.btce.v3.BTCEExchange;
import com.xeiam.xchange.cryptsy.CryptsyCurrencyUtils;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.okcoin.OkCoinExchange;

public class CycleFinderTest {
	List<Exchange> exchanges = new ArrayList<Exchange>();

	@Test
	public void testFindCycles() throws IOException {

		Map<String, Float> quantityLimits = new HashMap<>();

		quantityLimits.put("BTC", 0.000001f);
		quantityLimits.put("USD", .60f);
		quantityLimits.put("XRP", 1200f);
		quantityLimits.put("LTC", 0.1f);
		ConsoleAppender console = new ConsoleAppender(); // create appender
		// configure the appender
		String PATTERN = "%d [%p|%c|%C{1}] %m%n";
		console.setLayout(new PatternLayout(PATTERN));
		console.setThreshold(Level.WARN);
		console.activateOptions();
		// add appender to any Logger (here is root)
		Logger.getRootLogger().addAppender(console);
		Cryptsy cryptsy = new Cryptsy();

		System.out.println("Exchange loaded, " + cryptsy.currencyPairs.size()
				+ " currency pairs found.");
		CurrencyCycle cycles = CycleFinder.findCurrencyCycle("BTC", Arrays
				.asList(new CurrencyPair("BTC", "USD"), new CurrencyPair("LTC",
						"BTC"), new CurrencyPair("LTC", "USD"),
						new CurrencyPair("XRP", "USD"), new CurrencyPair("XRP",
								"BTC"), new CurrencyPair("DRK", "USD"),
						new CurrencyPair("DRK", "BTC"), new CurrencyPair("DRK",
								"LTC")));
		CurrencyCycle usdcycles = CycleFinder.findCurrencyCycle("USD", Arrays
				.asList(new CurrencyPair("BTC", "USD"), new CurrencyPair("LTC",
						"BTC"), new CurrencyPair("LTC", "USD"),
						new CurrencyPair("XRP", "USD"), new CurrencyPair("XRP",
								"BTC"), new CurrencyPair("DRK", "USD"),
						new CurrencyPair("DRK", "BTC"), new CurrencyPair("DRK",
								"LTC")));

		System.out.println((cycles.GetAllLeaves().size() + usdcycles
				.GetAllLeaves().size()) + " market cycles detected.");
	
		List<CachingOrderBook> orderBooks = exchanges.stream()
				.map(exchange -> new CachingOrderBook(exchange))
				.collect(Collectors.<CachingOrderBook> toList());
		while (true) {

			cycles.balance = BigDecimal.ONE;
			usdcycles.balance = BigDecimal.ONE;
			// cycles.actual = BigDecimal.ONE;
			// cycles.previousActual = BigDecimal.ONE;
			putBalanceOnNext(cycles, orderBooks);
			putBalanceOnNext(usdcycles, orderBooks);

			List<CurrencyCycle> viableCycles = cycles
					.GetAllLeaves()
					.stream()
					.filter(x -> x.balance != null
							&& x.balance.floatValue() > 1)
					.collect(Collectors.<CurrencyCycle> toList());
			viableCycles.addAll(usdcycles
					.GetAllLeaves()
					.stream()
					.filter(x -> x.balance != null
							&& x.balance.floatValue() > 1)
					.collect(Collectors.<CurrencyCycle> toList()));

			System.out
					.println(viableCycles.size() + " opportunities detected.");

			viableCycles.forEach(x -> System.out.println(x + " " + x.getSize()
					+ "%"));

		}
	}

	public void putBalanceOnNext(CurrencyCycle aCycle,
			List<CachingOrderBook> orderBooks) {

		Map<Integer, Integer> marketSubs = new HashMap<>();
		marketSubs.put(464, 445);
		marketSubs.put(441, 454);

		String baseCurrency = aCycle.baseSymbol;
		for (CurrencyCycle cycle : aCycle.counterSymbols) {
			String counterCurrency = cycle.baseSymbol;

			int market = CryptsyCurrencyUtils
					.convertToMarketId(new CurrencyPair(baseCurrency,
							counterCurrency));
			if (marketSubs.containsKey(market)) {
				market = marketSubs.get(market);
			}
			CurrencyPair realPair = CryptsyCurrencyUtils
					.convertToCurrencyPair(market);
			if (realPair.baseSymbol.equals("DASH")) {
				realPair = new CurrencyPair("DRK", realPair.counterSymbol);
			}
			if (realPair.counterSymbol.equals("DASH")) {
				realPair = new CurrencyPair(realPair.baseSymbol, "DRK");
			}
			final CurrencyPair finalPair = realPair;
			if (realPair.baseSymbol.equals(counterCurrency)) {

				Optional<ExchangeLimitOrder> order = orderBooks
						.stream()
						.flatMap(x -> x.getSellOrders(finalPair).stream())
						.collect(
								Collectors.<ExchangeLimitOrder> minBy((x, y) -> {
									return x.limitOrder.getLimitPrice()
											.compareTo(
													y.limitOrder
															.getLimitPrice());
								}));

				if (!order.isPresent()) {
					continue;
				}

				cycle.balance = aCycle.balance.divide(
						order.get().limitOrder.getLimitPrice(), 8,
						RoundingMode.HALF_DOWN).multiply(
						new BigDecimal(0.99765));
				cycle.exchangeLimitOrder = order.get();
				// cycle.actual = aCycle.actual.divide(order.get().getPrice(),
				// 8, RoundingMode.HALF_DOWN).multiply(new
				// BigDecimal(0.99765)).min(order.get().getQuantity());
				// cycle.previousActual =
				// cycle.actual.multiply(order.get().getPrice());
				// limitOrders.add(new LimitOrder(OrderType.BID, actual
				// .divide(order.get().getPrice(), 8,
				// RoundingMode.HALF_DOWN).min(
				// order.get().getQuantity()), realPair,
				// null, new Date(), order.get().getPrice()));

			} else {
				Optional<ExchangeLimitOrder> order = orderBooks
						.stream()
						.flatMap(x -> x.getBuyOrders(finalPair).stream())
						.collect(
								Collectors.<ExchangeLimitOrder> maxBy((x, y) -> {
									return x.limitOrder.getLimitPrice()
											.compareTo(
													y.limitOrder
															.getLimitPrice());
								}));
				if (!order.isPresent()) {
					continue;
				}
				cycle.balance = aCycle.balance.multiply(
						order.get().limitOrder.getLimitPrice()).multiply(
						new BigDecimal(0.99765));
				cycle.exchangeLimitOrder = order.get();
				// cycle.actual =
				// aCycle.actual.multiply(order.get().getPrice()).multiply(new
				// BigDecimal(0.99765));

				// cycle.actual =
				// cycle.actual.min(order.get().getQuantity().divide(order.get().getPrice(),
				// 8, RoundingMode.HALF_DOWN));
				// cycle.previousActual =
				// cycle.actual.divide(order.get().getPrice(),8,RoundingMode.HALF_DOWN);
				// limitOrders.add(new LimitOrder(OrderType.ASK, actual
				// .min(order.get().getQuantity()), realPair,
				// null, new Date(), order.get().getPrice()));
			}
			putBalanceOnNext(cycle, orderBooks);
		}

	}

}
