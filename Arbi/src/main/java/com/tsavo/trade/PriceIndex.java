package com.tsavo.trade;

import java.util.List;
import java.util.stream.Collectors;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;

public class PriceIndex {

	public List<CachingOrderBook> orderBooks;

	public PriceIndex(List<Exchange> someOrderBooks) {
		orderBooks = someOrderBooks.stream().map(x -> new CachingOrderBook(x)).collect(Collectors.<CachingOrderBook> toList());
	}

	public ExchangeLimitOrder getLowestSellPrice(CurrencyPair aPair) {
		return orderBooks.stream().map(x -> x.getLowestSellPrice(aPair))
				.collect(Collectors.<ExchangeLimitOrder> minBy((x, y) -> x.limitOrder.getLimitPrice().compareTo(y.limitOrder.getLimitPrice()))).get();

	}

	public ExchangeLimitOrder getHighestBuyPrice(CurrencyPair aPair) {
		return orderBooks.stream().map(x -> x.getHighestBuyPrice(aPair))
				.collect(Collectors.<ExchangeLimitOrder> maxBy((x, y) -> x.limitOrder.getLimitPrice().compareTo(y.limitOrder.getLimitPrice()))).get();

	}

}
