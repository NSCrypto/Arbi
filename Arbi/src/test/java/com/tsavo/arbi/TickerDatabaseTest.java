package com.tsavo.arbi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.tsavo.hippo.TickerDatabase;
import com.tsavo.hippo.TradeListener;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.bitfinex.v1.BitfinexExchange;
import com.xeiam.xchange.btce.v3.BTCEExchange;
import com.xeiam.xchange.bter.BTERExchange;
import com.xeiam.xchange.cryptsy.CryptsyExchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Trade;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;
import com.xeiam.xchange.kraken.KrakenExchange;
import com.xeiam.xchange.okcoin.OkCoinExchange;
import com.xeiam.xchange.yacuna.YacunaExchange;

public class TickerDatabaseTest {

	@Test
	public void test() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException, InterruptedException {
		List<Exchange> exchanges = new ArrayList<>();

		ExchangeSpecification cryptsy = new ExchangeSpecification(CryptsyExchange.class);

		Exchange btce = ExchangeFactory.INSTANCE.createExchange(BTCEExchange.class.getName());
		ExchangeSpecification btceSpec = btce.getDefaultExchangeSpecification();

		Exchange okcoin = ExchangeFactory.INSTANCE.createExchange(OkCoinExchange.class.getName());
		ExchangeSpecification okcoinSpec = okcoin.getDefaultExchangeSpecification();

		exchanges.add(ExchangeFactory.INSTANCE.createExchange(okcoinSpec));

		Exchange bfx = ExchangeFactory.INSTANCE.createExchange(BitfinexExchange.class.getName());

		ExchangeSpecification bitfinexSpec = bfx.getDefaultExchangeSpecification();

		Exchange kraken = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());

		exchanges.add(kraken);
	
		Exchange yacuna = ExchangeFactory.INSTANCE.createExchange(YacunaExchange.class.getName());

		 exchanges.add(yacuna);

		Exchange bter = ExchangeFactory.INSTANCE.createExchange(BTERExchange.class.getName());

		exchanges.add(ExchangeFactory.INSTANCE.createExchange(bitfinexSpec));
		// exchanges.add(bter);
		exchanges.add(ExchangeFactory.INSTANCE.createExchange(btceSpec));
		Exchange referenceExchange = ExchangeFactory.INSTANCE.createExchange(cryptsy);
		exchanges.add(referenceExchange);
		List<TickerDatabase> tickers = new ArrayList<>();
		exchanges.forEach(exchange -> tickers.add(new TickerDatabase(exchange.getExchangeSpecification().getExchangeName())));

		tickers.forEach(ticker -> {
			ticker.addTradeListener(new CurrencyPair("BTC", "USD"), new TradeListener() {

				@Override
				public void handleTrade(Trade trade) {
					System.out.println(ticker.exchangeName + ": " + trade);
				}

			});
		});

		while (true) {

			Thread.sleep(1000);
		}

	}
}
