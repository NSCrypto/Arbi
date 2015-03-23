package com.tsavo.arbi;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;

import com.tsavo.trade.Cryptsy;
import com.tsavo.trade.opportunity.cycle.CycleFinder;
import com.tsavo.trade.opportunity.cycle.MarketCycle;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.cryptsy.CryptsyAdapters;
import com.xeiam.xchange.cryptsy.CryptsyCurrencyUtils;
import com.xeiam.xchange.cryptsy.CryptsyExchange;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicMarketData;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicOrder;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicOrderbook;
import com.xeiam.xchange.cryptsy.service.polling.CryptsyMarketDataService;
import com.xeiam.xchange.cryptsy.service.polling.CryptsyPublicMarketDataService;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.OrderBook;

public class CycleFinderTest {

	@Test
	public void testFindCycles() throws IOException {

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

		Map<Integer, CryptsyPublicOrderbook> orders = cryptsy.GetOrders();
		outer: for (MarketCycle cycle : cycles) {
			BigDecimal balance = BigDecimal.ONE;
			String last = null;
			for (String currency : cycle.markets) {
				if (last == null) {
					last = currency;
					continue;
				}
				int market = CryptsyCurrencyUtils
						.convertToMarketId(new CurrencyPair(last, currency));
				CryptsyPublicOrderbook orderbook = orders.get(market);
				if (orderbook == null || orderbook.getBuyOrders() == null || orderbook.getSellOrders() == null) {
					continue outer;
				}
				CurrencyPair realPair = CryptsyCurrencyUtils
						.convertToCurrencyPair(market);
				if (realPair.baseSymbol.equals(currency)) {
					balance = balance
							.divide(orderbook
									.getSellOrders()
									.stream()
									.collect(
											Collectors
													.<CryptsyPublicOrder> minBy((
															x, y) -> x
															.getPrice()
															.compareTo(
																	y.getPrice())))
									.get().getPrice(), 8,
									RoundingMode.HALF_DOWN);
				} else {
					balance = balance
							.multiply(orderbook
									.getBuyOrders()
									.stream()
									.collect(
											Collectors
													.<CryptsyPublicOrder> maxBy((
															x, y) -> x
															.getPrice()
															.compareTo(
																	y.getPrice())))
									.get().getPrice());
				}
				last = currency;
			}
			if(balance.floatValue() > 1){
				System.out.println(balance);
				System.out.println(cycle);
			}
			
		}
		// cycles.stream().sorted((x, y) -> x.markets.size() - y.markets.size())
		// .forEach(x -> System.out.println(x));
		// System.out.println(cycles.size());
	}

}
