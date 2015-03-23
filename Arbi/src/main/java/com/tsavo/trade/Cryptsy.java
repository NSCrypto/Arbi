package com.tsavo.trade;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.cryptsy.CryptsyExchange;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicOrderbook;
import com.xeiam.xchange.cryptsy.service.polling.CryptsyPublicMarketDataService;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.exceptions.ExchangeException;

public class Cryptsy {

	public List<CurrencyPair> currencyPairs;
	public CryptsyPublicMarketDataService marketDataService;

	public Cryptsy() throws IOException {
		CryptsyExchange exchange = (CryptsyExchange) ExchangeFactory.INSTANCE
				.createExchange(CryptsyExchange.class.getName());

		marketDataService = (CryptsyPublicMarketDataService) exchange
				.getPollingPublicMarketDataService();

		currencyPairs = marketDataService.getExchangeSymbols();
	}

	public Map<Integer, CryptsyPublicOrderbook> GetOrders()
			throws ExchangeException, IOException {
		return marketDataService.getAllCryptsyOrderBooks();
	}
	
}
