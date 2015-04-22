package com.tsavo.arbi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.tsavo.hippo.LiveTickerReader;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.bitfinex.v1.BitfinexExchange;
import com.xeiam.xchange.btce.v3.BTCEExchange;
import com.xeiam.xchange.bter.BTERExchange;
import com.xeiam.xchange.cryptsy.CryptsyExchange;
import com.xeiam.xchange.currency.CurrencyPair;
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
		cryptsy.setApiKey("1948367b66763024000812b257c1c5907e1e36fb");
		cryptsy.setSecretKey("9c5baae0e58978fd7daa317ce2418980aae3ee0ed2dc623dbd78dfdd5ef319ff78b0f80a5a1a9178");

		Exchange btce = ExchangeFactory.INSTANCE.createExchange(BTCEExchange.class.getName());
		ExchangeSpecification btceSpec = btce.getDefaultExchangeSpecification();
		btceSpec.setApiKey("OIW7IED5-XTPBARNQ-78G66VT3-NOX4NY4T-CBPV070X");
		btceSpec.setSecretKey("ccba84ce0908586fd2baa360e7da98d9cd5be4109a11cb8bab4269025652b834");

		Exchange okcoin = ExchangeFactory.INSTANCE.createExchange(OkCoinExchange.class.getName());
		ExchangeSpecification okcoinSpec = okcoin.getDefaultExchangeSpecification();
		okcoinSpec.setExchangeSpecificParametersItem("Use_Intl", true);
		okcoinSpec.setApiKey("65e10714-24a8-4587-a79e-ae9e4579fe29");
		okcoinSpec.setSecretKey("692338766D348C051F91B189674D6D3E");

		exchanges.add(ExchangeFactory.INSTANCE.createExchange(okcoinSpec));

		Exchange bfx = ExchangeFactory.INSTANCE.createExchange(BitfinexExchange.class.getName());

		ExchangeSpecification bitfinexSpec = bfx.getDefaultExchangeSpecification();

		bitfinexSpec.setApiKey("fqnmXJypz1WV5qGdxf9qPLEqYuhJ1l0BOVzJOBgz5y9");
		bitfinexSpec.setSecretKey("7LaUvSp90XOoYkDM9mOf3vr7iwwhFuTcqfQs0VQxDdm");

		Exchange kraken = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
		kraken.getExchangeSpecification().setApiKey("U65ezt/UHp1l61CNTRchqz9gP8ApTGBonTK53M7xBh5CEp2FrCLxsAWE");
		kraken.getExchangeSpecification().setSecretKey("y7Xv5rfE+bQ9hLy+Xd76sPIDibl38e4BYiC4iRRam+9aSr78CBAdToMgOPGukED0MJ3DleacCcmznVySGjvsPQ==");

		// exchanges.add(kraken);

		Exchange yacuna = ExchangeFactory.INSTANCE.createExchange(YacunaExchange.class.getName());
		yacuna.getExchangeSpecification().setApiKey("AAEAAAgfi0NutxlS0JuXBlez5MeK6PsB6b1-afrMV5iyAT6461D079U2");
		yacuna.getExchangeSpecification().setSecretKey("36422983b180fbc37095bd9530869bc9");

		// exchanges.add(yacuna);

		Exchange bter = ExchangeFactory.INSTANCE.createExchange(BTERExchange.class.getName());
		bter.getExchangeSpecification().setApiKey("6B8C5A04-2FBF-4643-AE67-616D0066A412");
		bter.getExchangeSpecification().setSecretKey("63d5c7892f0831d8d04a9af8ddaefeb498ec0822edcf8b47073689f3783d2ee8");

		exchanges.add(ExchangeFactory.INSTANCE.createExchange(bitfinexSpec));
		// exchanges.add(bter);
		exchanges.add(ExchangeFactory.INSTANCE.createExchange(btceSpec));
		Exchange referenceExchange = ExchangeFactory.INSTANCE.createExchange(cryptsy);
		exchanges.add(referenceExchange);
		List<LiveTickerReader> tickers = new ArrayList<>();
		exchanges.forEach(exchange -> tickers.add(new LiveTickerReader(exchange.getExchangeSpecification().getExchangeName())));
		while (true) {
			Date when = new Date(new Date().getTime() - 1000000000);
			when.setHours(0);
			when.setMinutes(0);
			when.setSeconds(0);
			tickers.forEach(ticker -> {
				System.out.println(ticker.db.exchangeName + ":");
				ticker.getDataForTimeframe(new CurrencyPair("BTC", "USD")).forEach(x -> System.out.println(x));
				;
			});

			Thread.sleep(1000);
		}

	}
}
