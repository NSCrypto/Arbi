package com.tsavo.trade;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.speech.AudioException;
import javax.speech.EngineException;
import javax.speech.EngineStateError;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.tsavo.trade.opportunity.arbitrage.ArbitrageOpportunityFinder;
import com.tsavo.trade.opportunity.cycle.CycleOpportunityFinder;
import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.bitfinex.v1.BitfinexExchange;
import com.xeiam.xchange.btce.v3.BTCEExchange;
import com.xeiam.xchange.cryptsy.CryptsyExchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;
import com.xeiam.xchange.okcoin.OkCoinExchange;

public class TradeBot {

	public static LoadingCache<Exchange, LoadingCache<String, BigDecimal>> wallets;

	public static class Finder implements Runnable {
		public Portfolio portfolio = new Portfolio();
		List<OpportunityFinder> finders = new ArrayList<OpportunityFinder>();

		List<Exchange> exchanges = new ArrayList<Exchange>();

		public Finder(Portfolio aPortfolio) throws ExchangeException,
				NotAvailableFromExchangeException,
				NotYetImplementedForExchangeException, IOException {
			portfolio = aPortfolio;
			ExchangeSpecification cryptsy = new ExchangeSpecification(
					CryptsyExchange.class);
			cryptsy.setApiKey("1948367b66763024000812b257c1c5907e1e36fb");
			cryptsy.setSecretKey("9c5baae0e58978fd7daa317ce2418980aae3ee0ed2dc623dbd78dfdd5ef319ff78b0f80a5a1a9178");

			if (portfolio.getReferenceExchangeSpecification() == null) {
				portfolio.setReferenceExchangeSpecification(cryptsy);
			}

			Exchange btce = ExchangeFactory.INSTANCE
					.createExchange(BTCEExchange.class.getName());
			ExchangeSpecification btceSpec = btce
					.getDefaultExchangeSpecification();
			btceSpec.setApiKey("OIW7IED5-XTPBARNQ-78G66VT3-NOX4NY4T-CBPV070X");
			btceSpec.setSecretKey("ccba84ce0908586fd2baa360e7da98d9cd5be4109a11cb8bab4269025652b834");

			Exchange okcoin = ExchangeFactory.INSTANCE
					.createExchange(OkCoinExchange.class.getName());
			ExchangeSpecification okcoinSpec = okcoin
					.getDefaultExchangeSpecification();
			okcoinSpec.setExchangeSpecificParametersItem("Use_Intl", true);

			// exchanges.add(ExchangeFactory.INSTANCE.createExchange(okcoinSpec));

			Exchange bfx = ExchangeFactory.INSTANCE
					.createExchange(BitfinexExchange.class.getName());

			ExchangeSpecification bfxSpec = bfx
					.getDefaultExchangeSpecification();

			bfxSpec.setApiKey("fqnmXJypz1WV5qGdxf9qPLEqYuhJ1l0BOVzJOBgz5y9");
			bfxSpec.setSecretKey("7LaUvSp90XOoYkDM9mOf3vr7iwwhFuTcqfQs0VQxDdm");
			exchanges.add(ExchangeFactory.INSTANCE.createExchange(bfxSpec));
			// exchanges.add(ExchangeFactory.INSTANCE.createExchange(bter));
			exchanges.add(ExchangeFactory.INSTANCE.createExchange(btceSpec));
			Exchange referenceExchange = ExchangeFactory.INSTANCE
					.createExchange(portfolio.referenceExchangeSpecification);
			exchanges.add(referenceExchange);

			wallets = CacheBuilder
					.newBuilder()
					.concurrencyLevel(4)
					.weakKeys()
					.expireAfterWrite(30, TimeUnit.SECONDS)
					.build(new CacheLoader<Exchange, LoadingCache<String, BigDecimal>>() {
						@Override
						public LoadingCache<String, BigDecimal> load(
								Exchange anExchange) throws Exception {
							// TODO Auto-generated method stub
							return CacheBuilder
									.newBuilder()
									.concurrencyLevel(4)
									.weakKeys()
									.expireAfterWrite(30, TimeUnit.SECONDS)
									.build(new CacheLoader<String, BigDecimal>() {
										@Override
										public BigDecimal load(String aCurrency)
												throws Exception {
											return anExchange
													.getPollingAccountService()
													.getAccountInfo()
													.getBalance(aCurrency);
										}
									});
						}
					});

			// exchanges.add(ExchangeFactory.INSTANCE.createExchange(BitstampExchange.class.getName()));

			// for (ExchangeSpecification spec :
			// portfolio.getExchangeSpecifications()) {
			// exchanges.add(ExchangeFactory.INSTANCE.createExchange(spec));
			// }

			// for (String pair :
			// referenceExchange.getPollingMarketDataService().getExchangeSymbols().stream().map(x
			// ->
			// x.counterSymbol).distinct().collect(Collectors.<String>toList()))
			// {
			// float balance = 0;

			// TickerData data =
			// portfolio.tickers.get(referenceExchange.getExchangeSpecification().getExchangeName()).get(pair);
			// if (data == null) {
			// data = new TickerData();
			// portfolio.tickers.get(referenceExchange.getExchangeSpecification().getExchangeName()).put(pair,
			// data);
			// }
			// referenceExchange.getPollingMarketDataService();
			// CryptsyMarketDataService service = (CryptsyMarketDataService)
			// referenceExchange.getPollingMarketDataService();

			// for (Exchange exchange : exchanges) {
			// balance +=
			// exchange.getPollingAccountService().getAccountInfo().getBalance(pair).floatValue();

			// }

			// HoldingsReport report = portfolio.getHoldingsReport().get(pair);
			// if (report == null) {
			// continue;
			// }
			// float total = report.getTotalAmount();
			// if (balance < total) {
			// portfolio.getHoldingsReport().get(pair).remove(total - balance);
			// }
			// }

			// portfolio.save();
			finders.add(new ArbitrageOpportunityFinder(exchanges,
			 portfolio));
			//finders.add(new CycleOpportunityFinder(exchanges));
			// finders.add(new PriceDifferenceOpportunityFinder(exchanges,
			// portfolio));
			// finders.add(new ManualOrderCreator(exchanges, portfolio));
			// finders.add(new ChartingOpportunityFinder(exchanges, portfolio));

		}

		public void run() {

			Set<String> suggestions = new HashSet<String>();
			for (OpportunityFinder finder : finders) {
				try {
					List<Opportunity> opportunities = new ArrayList<Opportunity>();
					try {
						opportunities.addAll(finder.findOpportunities());
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}

					Collections.sort(opportunities,
							new Comparator<Opportunity>() {
								public int compare(Opportunity o1,
										Opportunity o2) {
									return new Float(o2.getSize()).compareTo(o1
											.getSize());
								}
							});

					for (Opportunity opp : opportunities) {
						try {
							BigDecimal amountToTrade = opp.getAmountToTrade();
							if (amountToTrade.floatValue() < 0.0099) {
								suggestions.addAll(opp
										.getSuggestions(exchanges));
								continue;
							}

							opp.trade(
									amountToTrade.setScale(6,
											RoundingMode.HALF_UP)
											.stripTrailingZeros(), portfolio);
							System.out.println("-------");
							//System.out.println(portfolio.getReport());
							//Thread.sleep(20000);
							//break;
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}
					}

				} finally {
					for (String suggestion : suggestions) {
						System.out.println(suggestion);
						// voice.speak(suggestion);
					}
					suggestions.clear();
				}
			}
		}
	}

	static SpeechSynthesizer voice = new SpeechSynthesizer();

	public static void main(String[] args) throws InterruptedException,
			EngineException, AudioException, EngineStateError,
			PropertyVetoException, KeyManagementException,
			NoSuchAlgorithmException, ExchangeException,
			NotAvailableFromExchangeException,
			NotYetImplementedForExchangeException, IOException {
		// initSSL(); // Setup the SSL certificate to interact with mtgox over
		// secure http.

		// System.setProperty("org.joda.money.CurrencyUnitDataProvider",
		// "org.joda.money.CryptsyCurrencyUnitDataProvider");
		Portfolio portfolio = new Portfolio();
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
		Finder finder = new Finder(portfolio);
		System.out
				.println("System initialized successfully. Looking for opportunities...");
		while (true) {
			finder.run();
			Thread.sleep(10000);
		}
	}

	public static void initSSL() throws KeyManagementException,
			NoSuchAlgorithmException {

		// SSL Certificates trustStore ----------------------------------------
		// Set the SSL certificate for mtgox - Read up on Java Trust store.
		// System.setProperty("javax.net.ssl.trustStore", "trader.jks");
		// System.setProperty("javax.net.ssl.trustStorePassword", "zabbas"); //
		// I

		class MyManager implements X509TrustManager {

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {

			}

		}

		TrustManager[] managers = new TrustManager[] { new MyManager() };
		final SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, managers, new SecureRandom());

		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext
				.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});

		// System.setProperty("javax.net.debug","ssl"); //Uncomment for
		// debugging SSL errors

	}

}
