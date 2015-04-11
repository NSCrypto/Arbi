package com.tsavo.trade;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.speech.AudioException;
import javax.speech.EngineException;
import javax.speech.EngineStateError;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.tsavo.trade.opportunity.OpportunityFinder;
import com.tsavo.trade.opportunity.cycle.CycleOpportunityFinder;
import com.tsavo.trade.opportunity.manual.ManualOrderCreator;
import com.tsavo.trade.portfolio.Portfolio;
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

public class TradeBot {

	public static class Finder {
		List<OpportunityFinder> finders = new ArrayList<OpportunityFinder>();

		List<Exchange> exchanges = new ArrayList<Exchange>();

		public Finder() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

			ConsoleAppender console = new ConsoleAppender(); // create appender
			// configure the appender
			String PATTERN = "%d [%p|%c|%C{1}] %m%n";
			console.setLayout(new PatternLayout(PATTERN));
			console.setThreshold(Level.WARN);
			console.activateOptions();
			// add appender to any Logger (here is root)
			Logger.getRootLogger().addAppender(console);


			new Cryptsy();
			Wallet wallet = new Wallet(exchanges);
			PriceIndex index = new PriceIndex(exchanges);
			Portfolio portfolio = new Portfolio(index, wallet);
			OpportunityExecutor executor = new OpportunityExecutor(portfolio);

			new CycleOpportunityFinder("BTC", Arrays.asList(new CurrencyPair("BTC", "USD"), new CurrencyPair("LTC", "BTC"), new CurrencyPair("LTC", "USD"), new CurrencyPair("DRK",
					"USD"), new CurrencyPair("DRK", "BTC")), portfolio).findOpportunities(executor);
			new CycleOpportunityFinder("USD", Arrays.asList(new CurrencyPair("BTC", "USD"), new CurrencyPair("LTC", "BTC"), new CurrencyPair("LTC", "USD"), new CurrencyPair("DRK",
					"USD"), new CurrencyPair("DRK", "BTC")), portfolio).findOpportunities(executor);
			new CycleOpportunityFinder("LTC", Arrays.asList(new CurrencyPair("BTC", "USD"), new CurrencyPair("LTC", "BTC"), new CurrencyPair("LTC", "USD"), new CurrencyPair("DRK",
					"USD"), new CurrencyPair("DRK", "BTC")), portfolio).findOpportunities(executor);
			new CycleOpportunityFinder("DRK", Arrays.asList(new CurrencyPair("BTC", "USD"), new CurrencyPair("LTC", "BTC"), new CurrencyPair("LTC", "USD"), new CurrencyPair("DRK",
					"USD"), new CurrencyPair("DRK", "BTC")), portfolio).findOpportunities(executor);

			new ManualOrderCreator(portfolio).findOpportunities(executor);

		}

		static SpeechSynthesizer voice = new SpeechSynthesizer();

		public static void main(String[] args) throws InterruptedException, EngineException, AudioException, EngineStateError, PropertyVetoException, KeyManagementException,
				NoSuchAlgorithmException, ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
			// initSSL(); // Setup the SSL certificate to interact with mtgox
			// over
			// secure http.

			// System.setProperty("org.joda.money.CurrencyUnitDataProvider",
			// "org.joda.money.CryptsyCurrencyUnitDataProvider");
			// File dest = new File("portfolio.json");
			// if (dest.exists()) {
			// InputStream file;
			// try {
			// file = new FileInputStream(dest);
			// InputStream buffer = new BufferedInputStream(file);
			// // ObjectInput input;
			// // input = new ObjectInputStream(buffer);
			// ObjectMapper mapper = new ObjectMapper();
			// portfolio = mapper.readValue(buffer, Portfolio.class);
			// file.close();
			// // input.close();
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// }
			Finder finder = new Finder();
			System.out.println("System initialized successfully. Looking for opportunities...");
			while (true) {
				Thread.sleep(10000);
			}
		}

		public static void initSSL() throws KeyManagementException, NoSuchAlgorithmException {
			class MyManager implements X509TrustManager {

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

				}

			}

			TrustManager[] managers = new TrustManager[] { new MyManager() };
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, managers, new SecureRandom());

			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});

		}
	}
}
