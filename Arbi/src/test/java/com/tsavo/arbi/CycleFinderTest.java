package com.tsavo.arbi;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
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
import com.tsavo.trade.opportunity.cycle.CycleFinder;
import com.tsavo.trade.opportunity.cycle.MarketCycle;
import com.xeiam.xchange.cryptsy.CryptsyCurrencyUtils;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicOrder;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicOrderbook;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;

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
		List<MarketCycle> cycles = CycleFinder.findCycles("BTC",
				cryptsy.currencyPairs);
		System.out.println(cycles.size() + " market cycles detected.");
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

			outer: for (MarketCycle cycle : cycles) {
				List<LimitOrder> limitOrders = new ArrayList<LimitOrder>();
				BigDecimal balance = BigDecimal.ONE;
				BigDecimal actual = BigDecimal.ONE;
				String last = null;
				for (String currency : cycle.markets) {
					if (last == null) {
						last = currency;
						continue;
					}
					int market = CryptsyCurrencyUtils
							.convertToMarketId(new CurrencyPair(last, currency));
					CryptsyPublicOrderbook orderbook = orders.get(market);
					if (orderbook == null || orderbook.getBuyOrders() == null
							|| orderbook.getSellOrders() == null) {
						continue outer;
					}
					CurrencyPair realPair = CryptsyCurrencyUtils
							.convertToCurrencyPair(market);
					if (realPair.baseSymbol.equals(currency)) {
						Optional<CryptsyPublicOrder> order = orderbook
								.getSellOrders()
								.stream()
								.filter(x -> x.getTotal().floatValue() > quantityLimits
										.get(realPair.counterSymbol))
								.collect(
										Collectors.<CryptsyPublicOrder> minBy((
												x, y) -> x.getPrice()
												.compareTo(y.getPrice())));
						if (!order.isPresent()) {
							continue outer;
						}
						limitOrders.add(new LimitOrder(OrderType.BID, actual
								.divide(order.get().getPrice(), 8,
										RoundingMode.HALF_DOWN).min(
										order.get().getQuantity()), realPair,
								null, new Date(), order.get().getPrice()));
						balance = balance.divide(order.get().getPrice(), 8,
								RoundingMode.HALF_DOWN).multiply(
								new BigDecimal(0.99765));
						actual = actual.divide(order.get().getPrice(), 8,
								RoundingMode.HALF_DOWN).min(
								order.get().getQuantity());
					} else {
						Optional<CryptsyPublicOrder> order = orderbook
								.getBuyOrders()
								.stream()
								.filter(x -> x.getTotal().floatValue() > quantityLimits
										.get(realPair.counterSymbol))
								.collect(
										Collectors.<CryptsyPublicOrder> maxBy((
												x, y) -> x.getPrice()
												.compareTo(y.getPrice())));
						if (!order.isPresent()) {
							continue outer;
						}
						limitOrders.add(new LimitOrder(OrderType.ASK, actual
								.min(order.get().getQuantity()), realPair,
								null, new Date(), order.get().getPrice()));
						balance = balance.multiply(order.get().getPrice());
						balance = balance.multiply(new BigDecimal(0.99765));
						actual = actual.multiply(order.get().getPrice()).min(
								order.get().getQuantity());
					}
					last = currency;
				}
				if (balance.floatValue() > 1) {
					System.out.println((balance.floatValue() - 1) * 100
							+ "% gain");
					System.out.println(cycle);
					System.out.println(limitOrders);
					System.out.println(actual);
				}

			}
			// cycles.stream().sorted((x, y) -> x.markets.size() -
			// y.markets.size())
			// .forEach(x -> System.out.println(x));
			// System.out.println(cycles.size());
		}
	}

}
