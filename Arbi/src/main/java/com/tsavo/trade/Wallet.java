package com.tsavo.trade;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xeiam.xchange.Exchange;

public class Wallet {

	private LoadingCache<Exchange, LoadingCache<String, BigDecimal>> wallets;
	public List<Exchange> exchanges;

	public Wallet(List<Exchange> someExchanges) {
		wallets = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(1000, TimeUnit.SECONDS)
				.build(new CacheLoader<Exchange, LoadingCache<String, BigDecimal>>() {
					@Override
					public LoadingCache<String, BigDecimal> load(Exchange anExchange) throws Exception {
						// TODO Auto-generated method stub
						return CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(4, TimeUnit.SECONDS).build(new CacheLoader<String, BigDecimal>() {
							@Override
							public BigDecimal load(String aCurrency) throws Exception {
								try {
									if (anExchange.getExchangeSpecification().getExchangeName().equals("Cryptsy") && aCurrency.equals("DRK")) {
										return anExchange.getPollingAccountService().getAccountInfo().getBalance("DASH");
									}
									return anExchange.getPollingAccountService().getAccountInfo().getBalance(aCurrency);
								} catch (Exception e) {
									return BigDecimal.ZERO;
								}
							}
						});
					}
				});
		exchanges = someExchanges;
	}

	public BigDecimal getTotalBalance(String aCurrency) {
		return exchanges.stream().map(exchange -> getBalance(exchange, aCurrency)).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getBalance(Exchange anExchange, String aCurrency) {
		try {
			return wallets.get(anExchange).get(aCurrency);
		} catch (ExecutionException e) {
			return BigDecimal.ZERO;
		}
	}
	public void clearCache(){
		wallets.invalidateAll();
	}
}
