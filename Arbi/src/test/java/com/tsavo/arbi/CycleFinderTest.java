package com.tsavo.arbi;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

import com.tsavo.trade.CachingCryptsyOrderBook;
import com.tsavo.trade.Cryptsy;
import com.tsavo.trade.opportunity.cycle.CurrencyCycle;
import com.tsavo.trade.opportunity.cycle.CycleFinder;
import com.xeiam.xchange.cryptsy.CryptsyCurrencyUtils;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicOrder;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicOrderbook;
import com.xeiam.xchange.currency.CurrencyPair;

public class CycleFinderTest {

	@Test
	public void testFindCycles() throws IOException {

		Map<String, Float> quantityLimits = new HashMap<>();

		quantityLimits.put("BTC", 0.000001f);
		quantityLimits.put("USD", 0.8f);
		quantityLimits.put("XRP", 1100f);
		quantityLimits.put("LTC", 0.001f);
		ConsoleAppender console = new ConsoleAppender(); // create appender
		// configure the appender
		String PATTERN = "%d [%p|%c|%C{1}] %m%n";
		console.setLayout(new PatternLayout(PATTERN));
		console.setThreshold(Level.WARN);
		console.activateOptions();
		// add appender to any Logger (here is root)
		Logger.getRootLogger().addAppender(console);
		Cryptsy cryptsy = new Cryptsy();
		
		System.out.println("Exchange loaded, " + cryptsy.currencyPairs.size() + " currency pairs found.");
		CurrencyCycle cycles = CycleFinder.findCurrencyCycle(
				"BTC",
				cryptsy.currencyPairs);
		System.out.println(cycles.GetAllLeaves().size()
				+ " market cycles detected.");
		while (true) {
			Map<Integer, CryptsyPublicOrderbook> orders = cryptsy.GetOrders();
			orders = orders
					.entrySet()
					.stream()
					.filter(x -> x.getValue().getBuyOrders() != null
							&& x.getValue().getSellOrders() != null)
					.collect(
							Collectors.toMap(x -> x.getKey(), y -> y.getValue()));
			Map<Integer, CachingCryptsyOrderBook> cachingOrders = new HashMap<Integer, CachingCryptsyOrderBook>();
			orders.forEach((x, y) -> cachingOrders.put(x, new CachingCryptsyOrderBook(y, quantityLimits.get(CryptsyCurrencyUtils.convertToCurrencyPair(x).counterSymbol))));
			System.out.println(orders.values().stream()
					.flatMap(x -> x.getBuyOrders().stream()).count()
					+ orders.values().stream()
							.flatMap(x -> x.getSellOrders().stream()).count()
					+ " orders loaded.");
			cycles.balance = BigDecimal.ONE;
			//cycles.actual = BigDecimal.ONE;
			//cycles.previousActual = BigDecimal.ONE;
			putBalanceOnNext(cycles, cachingOrders);
			List<CurrencyCycle> viableCycles = cycles
					.GetAllLeaves()
					.stream()
					.filter(x -> x.balance != null
							&& x.balance.floatValue() > 1)
					.collect(Collectors.<CurrencyCycle> toList());
			System.out.println(viableCycles.size() + " opportunities detected.");
			
			viableCycles
					.forEach(x -> System.out.println(x));
			
		}

	}

	public void putBalanceOnNext(CurrencyCycle aCycle,
			Map<Integer, CachingCryptsyOrderBook> orders) {

		Map<Integer, Integer> marketSubs = new HashMap<>();
		marketSubs.put(464, 445);
		marketSubs.put(441, 454);

		Map<String, Float> quantityLimits = new HashMap<>();

		quantityLimits.put("BTC", 0.0001f);
		quantityLimits.put("USD", 1f);
		quantityLimits.put("XRP", 1100f);
		quantityLimits.put("LTC", 0.01f);
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
			CachingCryptsyOrderBook orderbook = orders.get(market);
			if (orderbook == null) {
				continue;
			}
			if (realPair.baseSymbol.equals(counterCurrency)) {
				Optional<CryptsyPublicOrder> order = orderbook.lowestSell.get();
				if (!order.isPresent()) {
					continue;
				}

				cycle.balance = aCycle.balance.divide(order.get().getPrice(),
						8, RoundingMode.HALF_DOWN).multiply(new	BigDecimal(0.99765));
				//cycle.actual = aCycle.actual.divide(order.get().getPrice(), 8, RoundingMode.HALF_DOWN).multiply(new BigDecimal(0.99765)).min(order.get().getQuantity());
				//cycle.previousActual = cycle.actual.multiply(order.get().getPrice());
						// limitOrders.add(new LimitOrder(OrderType.BID, actual
				// .divide(order.get().getPrice(), 8,
				// RoundingMode.HALF_DOWN).min(
				// order.get().getQuantity()), realPair,
				// null, new Date(), order.get().getPrice()));

			} else {
				Optional<CryptsyPublicOrder> order = orderbook.highestBuy.get();
				if (!order.isPresent()) {
					continue;
				}
				cycle.balance = aCycle.balance.multiply(order.get().getPrice());//.multiply(new BigDecimal(0.99765));
				//cycle.actual = aCycle.actual.multiply(order.get().getPrice()).multiply(new BigDecimal(0.99765));
				
				//cycle.actual = cycle.actual.min(order.get().getQuantity().divide(order.get().getPrice(), 8, RoundingMode.HALF_DOWN));
				//cycle.previousActual = cycle.actual.divide(order.get().getPrice(),8,RoundingMode.HALF_DOWN);
				// limitOrders.add(new LimitOrder(OrderType.ASK, actual
				// .min(order.get().getQuantity()), realPair,
				// null, new Date(), order.get().getPrice()));
			}
			putBalanceOnNext(cycle, orders);
		}

	}

}
