package com.tsavo.arbi;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
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

import com.tsavo.trade.Cryptsy;
import com.tsavo.trade.opportunity.cycle.CurrencyCycle;
import com.tsavo.trade.opportunity.cycle.CycleFinder;
import com.tsavo.trade.opportunity.cycle.MarketCycle;
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
		quantityLimits.put("XRP", 700f);
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
			System.out.println(orders.values().stream()
					.flatMap(x -> x.getBuyOrders().stream()).count()
					+ orders.values().stream()
							.flatMap(x -> x.getSellOrders().stream()).count()
					+ " orders loaded.");
			cycles.balance = BigDecimal.ONE;
			putBalanceOnNext(cycles, orders);
			List<CurrencyCycle> viableCycles = cycles
					.GetAllLeaves()
					.stream()
					.filter(x -> x.balance != null
							&& x.balance.floatValue() > 1)
					.collect(Collectors.<CurrencyCycle> toList());
			System.out.println(viableCycles.size());
			DecimalFormat format = new DecimalFormat("#.##");
			viableCycles
					.forEach(x -> System.out.println(new MarketCycle(x
							.GetCurrencyCycle()).toString()
							+ " @ "
							+ format.format((x.balance.subtract(new BigDecimal(
									1)).multiply(new BigDecimal(100)))
									.floatValue()) + "%"));
		}

	}

	public void putBalanceOnNext(CurrencyCycle aCycle,
			Map<Integer, CryptsyPublicOrderbook> orders) {

		Map<Integer, Integer> marketSubs = new HashMap();
		marketSubs.put(464, 445);
		marketSubs.put(441, 454);

		Map<String, Float> quantityLimits = new HashMap<>();

		quantityLimits.put("BTC", 0.000001f);
		quantityLimits.put("USD", 0.8f);
		quantityLimits.put("XRP", 1000f);
		quantityLimits.put("LTC", 0.001f);
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
			CryptsyPublicOrderbook orderbook = orders.get(market);
			if (orderbook == null) {
				System.out.println("Missing orders for market " + baseCurrency
						+ "/" + counterCurrency + "(" + market + ")");
				continue;
			}
			if (realPair.baseSymbol.equals(baseCurrency)) {
				Optional<CryptsyPublicOrder> order = orderbook
						.getSellOrders()
						.stream()
						.filter(x -> x.getTotal().floatValue() > quantityLimits
								.get(realPair.counterSymbol))
						.collect(
								Collectors
										.<CryptsyPublicOrder> minBy((x, y) -> x
												.getPrice().compareTo(
														y.getPrice())));
				if (!order.isPresent()) {
					continue;
				}

				cycle.balance = aCycle.balance.divide(order.get().getPrice(),
						8, RoundingMode.HALF_DOWN).multiply(new
													BigDecimal(0.99765));
				// limitOrders.add(new LimitOrder(OrderType.BID, actual
				// .divide(order.get().getPrice(), 8,
				// RoundingMode.HALF_DOWN).min(
				// order.get().getQuantity()), realPair,
				// null, new Date(), order.get().getPrice()));

			} else {
				Optional<CryptsyPublicOrder> order = orderbook
						.getBuyOrders()
						.stream()
						.filter(x -> x.getTotal().floatValue() > quantityLimits
								.get(realPair.counterSymbol))
						.collect(
								Collectors
										.<CryptsyPublicOrder> maxBy((x, y) -> x
												.getPrice().compareTo(
														y.getPrice())));
				if (!order.isPresent()) {
					continue;
				}
				cycle.balance = aCycle.balance.multiply(order.get().getPrice()).multiply(new
																				BigDecimal(0.99765));
				// limitOrders.add(new LimitOrder(OrderType.ASK, actual
				// .min(order.get().getQuantity()), realPair,
				// null, new Date(), order.get().getPrice()));
			}
			putBalanceOnNext(cycle, orders);
		}

	}

}
