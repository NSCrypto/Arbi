package com.tsavo.trade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;

public class PriceIndex {

	public Map<Exchange, CachingOrderBook> orderBooks = new HashMap<>();
	public Map<String, Exchange> exchangeByName = new HashMap<>();

	public PriceIndex(List<Exchange> someOrderBooks) {
		someOrderBooks.forEach(x -> orderBooks.put(x, new CachingOrderBook(x)));
		someOrderBooks.forEach(x -> exchangeByName.put(x.getExchangeSpecification().getExchangeName(), x));

	}

	public ExchangeLimitOrder getLowestSellPrice(Exchange anExchange, CurrencyPair aPair) {
		return orderBooks.get(anExchange).getLowestSellPrice(aPair);
	}

	public ExchangeLimitOrder getHighestBuyPrice(Exchange anExchange, CurrencyPair aPair) {
		return orderBooks.get(anExchange).getHighestBuyPrice(aPair);
	}

	public ExchangeLimitOrder getLowestSellPrice(String anExchange, CurrencyPair aPair) {
		return orderBooks.get(exchangeByName.get(anExchange)).getLowestSellPrice(aPair);
	}

	public ExchangeLimitOrder getHighestBuyPrice(String anExchange, CurrencyPair aPair) {
		return orderBooks.get(exchangeByName.get(anExchange)).getHighestBuyPrice(aPair);
	}

	
	public ExchangeLimitOrder getLowestSellPrice(CurrencyPair aPair) {
		return orderBooks.values().stream().map(x -> x.getLowestSellPrice(aPair))
				.collect(Collectors.<ExchangeLimitOrder> minBy((x, y) -> x.limitOrder.getLimitPrice().compareTo(y.limitOrder.getLimitPrice()))).get();

	}

	public ExchangeLimitOrder getHighestBuyPrice(CurrencyPair aPair) {
		return orderBooks.values().stream().map(x -> x.getHighestBuyPrice(aPair))
				.collect(Collectors.<ExchangeLimitOrder> maxBy((x, y) -> x.limitOrder.getLimitPrice().compareTo(y.limitOrder.getLimitPrice()))).get();

	}

	public void clearCache() {
		orderBooks.values().forEach(CachingOrderBook::clearCache);
	}

}
