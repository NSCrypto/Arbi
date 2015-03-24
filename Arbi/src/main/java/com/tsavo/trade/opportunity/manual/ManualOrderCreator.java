package com.tsavo.trade.opportunity.manual;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.tsavo.trade.opportunity.BuyOpportunity;
import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.tsavo.trade.opportunity.SellOpportunity;
import com.tsavo.trade.opportunity.rsi.BasicChart;
import com.tsavo.trade.portfolio.Portfolio;
import com.tsavo.trade.portfolio.TickerData;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.marketdata.OrderBook;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.dto.trade.Wallet;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class ManualOrderCreator implements OpportunityFinder {

	List<Exchange> exchanges;
	Portfolio portfolio;
	List<Opportunity> opportunities = new ArrayList<Opportunity>();

	public ManualOrderCreator(List<Exchange> someExchanges, Portfolio aPortfolio) {
		exchanges = someExchanges;
		portfolio = aPortfolio;
		final BufferedReader br = new BufferedReader(new InputStreamReader(
				System.in));
		Thread t = new Thread() {
			@Override
			public void run() {
				DecimalFormat format = new DecimalFormat("#.########");

				while (true) {
					try {
						String myOrder = br.readLine();
						String[] parts = myOrder.split(" ");
						if (parts[0].equals("hold")) {
							portfolio.addSample(
									new CurrencyPair(parts[1],
											"BTC"), Float.parseFloat(parts[2]),
									Float.parseFloat(parts[3]));
							System.out.println(portfolio.getReport());
							portfolio.save();
							continue;
						}
						if (parts[0].equals("marketweight")) {
							BestExchangePrice exchange = getBestBuyExchange(
									parts[1], "BTC");
							CurrencyPair pair = new CurrencyPair(
									parts[1], "BTC");
							portfolio.addSample(pair,
									exchange.bestPrice.floatValue(), 100);
							System.out.println("Reweighted "
									+ parts[1]
									+ ", now at "
									+ format.format(portfolio
											.getAverageValue(pair)) + ".");
							continue;
						}
						if (parts[0].equals("average")) {
							CurrencyPair pair = new CurrencyPair(
									parts[1], "BTC");
							float average = portfolio.getAverageValue(pair);
							System.out.println("Average cost of "
									+ parts[1] + " is "
									+ format.format(average) + " (buy at "
									+ format.format((average * 0.96))
									+ ", sell at "
									+ format.format(average * 1.04) + ")");
							continue;
						}
						if (parts[0].equals("total")) {
							float total = 0;
							for (Exchange exchange : exchanges) {
								total += exchange.getPollingAccountService()
										.getAccountInfo()
										.getBalance(parts[1])
										.floatValue();
							}
							System.out.println("Total holdings of "
									+ parts[1] + ": "
									+ format.format(total));
							continue;
						}
						if (parts[0].equals("balance")) {
							Exchange exchange = null;
							String exchangeName = parts.length == 2 ? parts[1]
									: parts[2];
							for (Exchange ex : exchanges) {
								if (ex.getExchangeSpecification()
										.getExchangeName().toLowerCase()
										.contains(exchangeName)) {
									exchange = ex;
									break;
								}
							}
							if (exchange == null) {
								System.out.println("No exchange named "
										+ exchangeName);
								continue;
							}
							if (parts.length == 2) {
								AccountInfo info = exchange
										.getPollingAccountService()
										.getAccountInfo();
								for (Wallet w : info.getWallets()) {
									if (w.getBalance().floatValue() < 0.0001) {
										continue;
									}
									System.out.println("Our balance at "
											+ exchange
													.getExchangeSpecification()
													.getExchangeName()
											+ " is "
											+ w.getCurrency()
											+ " "
											+ format.format(w.getBalance()
													.floatValue()));
								}
							} else {
								BigDecimal balance = exchange
										.getPollingAccountService()
										.getAccountInfo()
										.getBalance(parts[1]);
								System.out.println("Our balance at "
										+ exchange.getExchangeSpecification()
												.getExchangeName() + " is "
										+ parts[1] + " "
										+ format.format(balance.floatValue()));
							}
							continue;
						}
						if (parts[0].equals("portfolio")) {
							System.out.println(portfolio.getReport());
							continue;
						}
						if (parts[0].equals("holdings")) {
							Map<String, Wallet> map = portfolio
									.getTotalHoldings(exchanges);
							for (Map.Entry<String, Wallet> entry : map
									.entrySet()) {
								System.out.println(entry.getKey()
										+ " -> "
										+ entry.getValue().getBalance()
												.floatValue());
							}
							continue;
						}
						if (parts[0].equals("chart")) {
							TickerData data = portfolio.tickers.get("Cryptsy")
									.get(new CurrencyPair(parts[1], parts[2]));
							new BasicChart(data, parts[1] + "/" + parts[2]);
							continue;
						}
						if (parts[0].equals("best")) {
							if (!parts[1].toLowerCase().equals("buy")
									&& !parts[1].toLowerCase().equals("sell")) {
								System.out.println("Unrecognized order type: "
										+ myOrder);
								continue;
							}
							OrderType orderType = parts[1].toLowerCase()
									.equals("buy") ? OrderType.BID
									: OrderType.ASK;

							String currencyUnit = parts[2];
							String transactionCurrency = "BTC";
							if (currencyUnit.contains("/")) {
								String[] s = currencyUnit.split("/");
								currencyUnit = s[0];
								transactionCurrency = s[1];
							}
							BestExchangePrice bestExchangePrice;

							if (orderType.equals(OrderType.BID)) {
								bestExchangePrice = getBestBuyExchange(
										currencyUnit, transactionCurrency);
							} else {
								bestExchangePrice = getBestSellExchange(
										currencyUnit, transactionCurrency);
							}
							System.out
									.println("The best place to "
											+ (orderType.equals(OrderType.BID) ? "buy "
													: "sell ")
											+ currencyUnit
											+ " is at "
											+ bestExchangePrice.exchange
													.getExchangeSpecification()
													.getExchangeName()
											+ " for "
											+ format.format(bestExchangePrice.bestPrice));
							continue;
						}
						if (!parts[0].toLowerCase().equals("buy")
								&& !parts[0].toLowerCase().equals("sell")) {
							System.out.println("Unrecognized order type: "
									+ myOrder);
							continue;
						}
						OrderType orderType = parts[0].toLowerCase().equals(
								"buy") ? OrderType.BID : OrderType.ASK;
						BigDecimal amount = new BigDecimal(parts[1]);
						String currencyUnit = parts[2];
						String transactionCurrency = "BTC";
						if (currencyUnit.contains("/")) {
							String[] s = currencyUnit.split("/");
							currencyUnit = s[0];
							transactionCurrency = s[1];
						}
						Exchange exchange = null;
						BigDecimal price = null;
						BestExchangePrice bestExchangePrice;
						if (orderType.equals(OrderType.BID)) {
							bestExchangePrice = getBestBuyExchange(
									currencyUnit, transactionCurrency);
						} else {
							bestExchangePrice = getBestSellExchange(
									currencyUnit, transactionCurrency);
						}
						if (parts.length > 3) {
							if (orderType.equals(OrderType.BID)) {
								price = new BigDecimal(Math.min(Float
										.parseFloat(parts[3]),
										bestExchangePrice.bestPrice
												.floatValue()));
							} else {
								price = new BigDecimal(Math.max(Float
										.parseFloat(parts[3]),
										bestExchangePrice.bestPrice
												.floatValue()));
							}
						} else {
							price = bestExchangePrice.bestPrice;
						}
						if (parts.length > 4) {
							for (Exchange exchangeLoop : exchanges) {
								if (exchangeLoop.getExchangeSpecification()
										.getExchangeName().toLowerCase()
										.contains(parts[4])) {
									exchange = exchangeLoop;
									break;
								}
							}
						} else {
							exchange = bestExchangePrice.exchange;
						}

						if (orderType.equals(OrderType.BID)) {
							BigDecimal balance = exchange
									.getPollingAccountService()
									.getAccountInfo()
									.getBalance(transactionCurrency);
							if (amount.floatValue() * price.floatValue() > balance
									.floatValue()) {
								System.out.println("We don't have enough "
										+ transactionCurrency
										+ " at "
										+ exchange.getExchangeSpecification()
												.getExchangeName()
										+ ". Aborting.");
								continue;
							}
						} else {
							BigDecimal balance = exchange
									.getPollingAccountService()
									.getAccountInfo().getBalance(currencyUnit);
							if (amount.floatValue() > balance.floatValue()) {
								System.out.println("We don't have enough "
										+ currencyUnit
										+ " at "
										+ exchange.getExchangeSpecification()
												.getExchangeName()
										+ ". Aborting.");
								continue;
							}
						}
						LimitOrder order = new LimitOrder(orderType, amount,
								new CurrencyPair(currencyUnit,
										transactionCurrency), null, new Date(),
								price);
						Opportunity opportunity;
						if (orderType.equals(OrderType.BID)) {
							opportunity = new BuyOpportunity(exchange, 1,
									new CurrencyPair(currencyUnit,
											transactionCurrency), amount, order);
						} else {
							opportunity = new SellOpportunity(exchange, 1,
									new CurrencyPair(currencyUnit,
											transactionCurrency), amount, order);
						}
						opportunities.add(opportunity);
						// exchange.getPollingTradeService().placeLimitOrder(order);
						// System.out.println("We " +
						// (orderType.equals(OrderType.BID) ?
						// "bought " : "sold ") + amount.floatValue() + " " +
						// currencyUnit + " for " +
						// format.format(price.floatValue()) + " @ " +
						// exchange.getExchangeSpecification().getExchangeName());
						// if (orderType.equals(OrderType.BID)) {
						// portfolio.addSample(new CurrencyPair(currencyUnit,
						// "BTC"),
						// price.floatValue(), amount.floatValue());
						// }
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}
			}

		};
		t.setDaemon(true);
		t.start();

	}

	private static class BestExchangePrice {
		Exchange exchange;
		BigDecimal bestPrice;
	}

	public BestExchangePrice getBestSellExchange(String aCurrency,
			String aTransactionCurrency) {
		Exchange bestExchange = null;
		float highestPrice = Float.MIN_VALUE;
		for (Exchange exchange : exchanges) {

			try {
				OrderBook book = exchange.getPollingMarketDataService()
						.getOrderBook(
								new CurrencyPair(aCurrency,
										aTransactionCurrency));
				for (LimitOrder order : book.getBids()) {
					if (order.getLimitPrice().floatValue() > highestPrice) {
						bestExchange = exchange;
						highestPrice = order.getLimitPrice().floatValue();
					}
				}
			} catch (Exception e) {
				System.out.println("Exchange "
						+ exchange.getExchangeSpecification().getExchangeName()
						+ " isn't working. Skipping.");
			}
		}
		BestExchangePrice price = new BestExchangePrice();
		price.exchange = bestExchange;
		price.bestPrice = new BigDecimal(highestPrice);
		return price;

	}

	public BestExchangePrice getBestBuyExchange(String aCurrency,
			String aTransactionCurrency) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		CurrencyPair pair = new CurrencyPair(aCurrency,
				aTransactionCurrency);
		Exchange bestExchange = null;
		float lowestPrice = Float.MAX_VALUE;
		for (Exchange exchange : exchanges) {

			OrderBook book = exchange
					.getPollingMarketDataService()
					.getOrderBook(pair);
			for (LimitOrder order : book.getAsks()) {
				if (order.getLimitPrice().floatValue() < lowestPrice) {
					bestExchange = exchange;
					lowestPrice = order.getLimitPrice().floatValue();
				}
			}
		}
		BestExchangePrice price = new BestExchangePrice();
		price.exchange = bestExchange;
		price.bestPrice = new BigDecimal(lowestPrice);
		return price;
	}

	public List<Opportunity> findOpportunities() {
		List<Opportunity> list = new ArrayList<Opportunity>();
		list.addAll(opportunities);
		opportunities.clear();
		return list;
	}

}
