package com.tsavo.trade;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.trade.LimitOrder;

public class CachingOrderBook {

	public Exchange exchange;

	private LoadingCache<CurrencyPair, List<LimitOrder>> buyOrders = CacheBuilder
			.newBuilder().concurrencyLevel(4).weakKeys()
			.expireAfterWrite(2, TimeUnit.MINUTES)
			.build(new CacheLoader<CurrencyPair, List<LimitOrder>>() {
				@Override
				public List<LimitOrder> load(CurrencyPair key) throws Exception {
					// TODO Auto-generated method stub
					try {
						return exchange
								.getPollingMarketDataService()
								.getOrderBook(key)
								.getBids()
								.stream()
								.filter(x -> x.getTradableAmount().floatValue() > 0.0099)
								.sorted((x, y) -> y.getLimitPrice().compareTo(
										x.getLimitPrice())).limit(5)
								.collect(Collectors.<LimitOrder> toList());
					} catch (Exception e) {
						return Collections.emptyList();
					}
				}
			});

	private LoadingCache<CurrencyPair, List<LimitOrder>> sellOrders = CacheBuilder
			.newBuilder().concurrencyLevel(4).weakKeys()
			.expireAfterWrite(2, TimeUnit.MINUTES)
			.build(new CacheLoader<CurrencyPair, List<LimitOrder>>() {
				@Override
				public List<LimitOrder> load(CurrencyPair key) throws Exception {
					try {
						return exchange
								.getPollingMarketDataService()
								.getOrderBook(key)
								.getAsks()
								.stream()
								.filter(x -> x.getTradableAmount().floatValue() > 0.0099)
								.sorted((x, y) -> x.getLimitPrice().compareTo(
										y.getLimitPrice())).limit(5)
								.collect(Collectors.<LimitOrder> toList());
					} catch (Exception e) {
						return Collections.emptyList();
					}
				}
			});

	public CachingOrderBook(Exchange exchange) {
		this.exchange = exchange;
	}

	public List<ExchangeLimitOrder> getBuyOrders(CurrencyPair aPair) {
		try {
			return buyOrders.get(aPair).stream()
					.map(x -> new ExchangeLimitOrder(exchange, x))
					.collect(Collectors.<ExchangeLimitOrder> toList());
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public List<ExchangeLimitOrder> getSellOrders(CurrencyPair aPair) {
		try {
			return sellOrders.get(aPair).stream()
					.map(x -> new ExchangeLimitOrder(exchange, x))
					.collect(Collectors.<ExchangeLimitOrder> toList());
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

}
