package com.tsavo.trade;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.cryptsy.CryptsyExchange;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicOrderbook;
import com.xeiam.xchange.cryptsy.service.polling.CryptsyPublicMarketDataService;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.exceptions.ExchangeException;

public class Cryptsy {

	public Set<CurrencyPair> currencyPairs;
	public CryptsyPublicMarketDataService marketDataService;
	public CryptsyExchange exchange;

	public Cryptsy() throws IOException {
		ExchangeSpecification cryptsy = new ExchangeSpecification(CryptsyExchange.class);
		cryptsy.setApiKey("1948367b66763024000812b257c1c5907e1e36fb");
		cryptsy.setSecretKey("9c5baae0e58978fd7daa317ce2418980aae3ee0ed2dc623dbd78dfdd5ef319ff78b0f80a5a1a9178");

		exchange = (CryptsyExchange) ExchangeFactory.INSTANCE.createExchange(cryptsy);

		marketDataService = (CryptsyPublicMarketDataService) exchange.getPollingPublicMarketDataService();

		currencyPairs = marketDataService.getExchangeSymbols().stream().collect(Collectors.<CurrencyPair> toSet());
		;
	}

	public Map<Integer, CryptsyPublicOrderbook> GetOrders() throws ExchangeException, IOException {
		return marketDataService.getAllCryptsyOrderBooks();
	}

}
