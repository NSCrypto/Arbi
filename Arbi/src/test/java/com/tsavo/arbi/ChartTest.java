package com.tsavo.arbi;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import com.tsavo.trade.opportunity.rsi.BasicChart;
import com.tsavo.trade.portfolio.TickerData;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.bitfinex.v1.BitfinexExchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class ChartTest {

	@Test
	public void test() throws InterruptedException, ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		final Exchange exchange = ExchangeFactory.INSTANCE.createExchange(BitfinexExchange.class.getName());
		final TickerData data = new TickerData();
		BasicChart chart = new BasicChart(data, "Test Chart");
		while (true) {
			Ticker ticker;
			try {
				ticker = exchange.getPollingMarketDataService().getTicker(new CurrencyPair("BTC", "USD"));
				data.addSample(ticker.getLast().floatValue(), new Date().getTime(), data.size());
				Thread.sleep(10000);
			} catch (ExchangeException | NotAvailableFromExchangeException | NotYetImplementedForExchangeException | IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
