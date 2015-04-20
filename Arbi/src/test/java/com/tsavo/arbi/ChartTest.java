package com.tsavo.arbi;

import java.io.IOException;

import org.junit.Test;

import com.tsavo.hippo.LiveTickerReader;
import com.tsavo.trade.opportunity.rsi.ChartingOpportunityFinder;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class ChartTest {

	@Test
	public void test() throws InterruptedException, ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		LiveTickerReader reader = new LiveTickerReader("BitFinex");
		ChartingOpportunityFinder finder = new ChartingOpportunityFinder(reader, new CurrencyPair("BTC", "USD"));
		
		while (true) {
			Thread.sleep(1000);
		}

	}

}
