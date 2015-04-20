package com.tsavo.trade;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;

public class CachingOrderBook {

	public Exchange exchange;
	public Map<String, Float> lowerLimits = new HashMap<>();

	private LoadingCache<CurrencyPair, List<LimitOrder>> buyOrders = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(10, TimeUnit.SECONDS)
			.build(new CacheLoader<CurrencyPair, List<LimitOrder>>() {
				@Override
				public List<LimitOrder> load(CurrencyPair key) throws Exception {
					// TODO Auto-generated method stub
					try {
						return exchange.getPollingMarketDataService().getOrderBook(key).getBids().stream()
								.filter(x -> x.getTradableAmount().floatValue() > getLowerLimit(key.counterSymbol))
								.sorted((x, y) -> y.getLimitPrice().compareTo(x.getLimitPrice())).limit(5).collect(Collectors.<LimitOrder> toList());
					} catch (Exception e) {
						return Collections.emptyList();
					}
				}
			});

	private LoadingCache<CurrencyPair, List<LimitOrder>> sellOrders = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(10, TimeUnit.SECONDS)
			.build(new CacheLoader<CurrencyPair, List<LimitOrder>>() {
				@Override
				public List<LimitOrder> load(CurrencyPair key) throws Exception {
					try {
						return exchange.getPollingMarketDataService().getOrderBook(key).getAsks().stream()
								.filter(x -> x.getTradableAmount().floatValue() > getLowerLimit(key.counterSymbol))
								.sorted((x, y) -> x.getLimitPrice().compareTo(y.getLimitPrice())).limit(5).collect(Collectors.<LimitOrder> toList());
					} catch (Exception e) {
						return Collections.emptyList();
					}
				}
			});

	public CachingOrderBook(Exchange exchange) {
		this.exchange = exchange;
		lowerLimits.put("LTC", 3f);
		lowerLimits.put("BTC", 10f);
		lowerLimits.put("DRK", 1f);
	}
	
	public void clearCache(){
		buyOrders.invalidateAll();
		sellOrders.invalidateAll();
	}

	public float getLowerLimit(String aCurrency) {
		if (lowerLimits.containsKey(aCurrency)) {
			return lowerLimits.get(aCurrency);
		}
		return 1f;
	}

	public List<ExchangeLimitOrder> getBuyOrders(CurrencyPair aPair) {
		try {
			return buyOrders.get(aPair).stream().map(x -> new ExchangeLimitOrder(exchange, x)).collect(Collectors.<ExchangeLimitOrder> toList());
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public List<ExchangeLimitOrder> getSellOrders(CurrencyPair aPair) {
		try {
			return sellOrders.get(aPair).stream().map(x -> new ExchangeLimitOrder(exchange, x)).collect(Collectors.<ExchangeLimitOrder> toList());
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public ExchangeLimitOrder getLowestSellPrice(CurrencyPair aPair) {
		return getSellOrders(aPair).stream().collect(Collectors.<ExchangeLimitOrder> minBy((x, y) -> x.limitOrder.getLimitPrice().compareTo(y.limitOrder.getLimitPrice())))
				.orElse(new ExchangeLimitOrder(exchange, new LimitOrder(OrderType.ASK, BigDecimal.ZERO, aPair, "", new Date(), new BigDecimal(Integer.MAX_VALUE))));
	}

	public ExchangeLimitOrder getHighestBuyPrice(CurrencyPair aPair) {
		return getBuyOrders(aPair).stream().collect(Collectors.<ExchangeLimitOrder> maxBy((x, y) -> x.limitOrder.getLimitPrice().compareTo(y.limitOrder.getLimitPrice())))
				.orElse(new ExchangeLimitOrder(exchange, new LimitOrder(OrderType.BID, BigDecimal.ZERO, aPair, "", new Date(), new BigDecimal(Integer.MIN_VALUE))));
	}

}
