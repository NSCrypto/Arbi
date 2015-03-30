package com.tsavo.trade.opportunity.cycle;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tsavo.trade.ExchangeLimitOrder;
import com.tsavo.trade.TradeBot;
import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class CycleOpportunity implements Opportunity {

	public CurrencyCycle cycle;

	public CycleOpportunity(CurrencyCycle cycle) {
		this.cycle = cycle;
	}

	@Override
	public BigDecimal getAmountToTrade() throws ExchangeException,
			NotAvailableFromExchangeException,
			NotYetImplementedForExchangeException, IOException {
		// TODO Auto-generated method stub

		BigDecimal amount = cycle.baseSymbol.equals("USD") ? new BigDecimal(1)
				: new BigDecimal(0.011);

		if (cycle.counterSymbols.equals("LTC")) {
			amount = new BigDecimal(0.1);
		}

		for (ExchangeLimitOrder order : cycle.GetExchangeLimitOrders()) {
			try {
				amount = amount
						.min(TradeBot.wallets
								.get(order.exchange)
								.get(order.limitOrder.getType() == OrderType.ASK ? order.limitOrder
										.getCurrencyPair().counterSymbol
										: order.limitOrder.getCurrencyPair().baseSymbol))
						.min(order.limitOrder.getTradableAmount());
			} catch (ExecutionException e) {
				amount = BigDecimal.ZERO;
			}
		}
		return amount.setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
	}

	@Override
	public float getSize() {
		return cycle.getSize();
	}

	@Override
	public void trade(final BigDecimal anAmmount, Portfolio aPortfolio)
			throws ExchangeException, NotAvailableFromExchangeException,
			NotYetImplementedForExchangeException, IOException {
		BigDecimal lastAmount = BigDecimal.TEN;
		cycle.GetExchangeLimitOrders()
				.forEach(
						x -> {

							BigDecimal tradeableAmount = null;

							try {
								tradeableAmount = x.limitOrder
										.getTradableAmount()
										.min(TradeBot.wallets
												.get(x.exchange)
												.get(x.limitOrder.getType() == OrderType.ASK ? x.limitOrder
														.getCurrencyPair().counterSymbol
														: x.limitOrder
																.getCurrencyPair().baseSymbol));
							} catch (Exception e) {
								tradeableAmount = BigDecimal.ZERO;
							}

							if (x.limitOrder.getType().equals(OrderType.ASK)) {
								System.out.println("Buying "
										+ tradeableAmount
										+ " "
										+ x.limitOrder.getCurrencyPair().baseSymbol
										+ " on "
										+ x.exchange.getExchangeSpecification()
												.getExchangeName()
										+ " @ "
										+ x.limitOrder.getLimitPrice()
										+ " "
										+ x.limitOrder.getCurrencyPair().counterSymbol
										+ " ("
										+ x.limitOrder
												.getLimitPrice()
												.multiply(tradeableAmount)
												.setScale(2,
														RoundingMode.HALF_DOWN)
												.stripTrailingZeros()
										+ " "
										+ x.limitOrder.getCurrencyPair().counterSymbol
										+ ")");
							} else {
								System.out.println("Selling "
										+ tradeableAmount
										+ " "
										+ x.limitOrder.getCurrencyPair().baseSymbol
										+ " on "
										+ x.exchange.getExchangeSpecification()
												.getExchangeName()
										+ " @ "
										+ x.limitOrder.getLimitPrice()
										+ " "
										+ x.limitOrder.getCurrencyPair().counterSymbol
										+ " ("
										+ x.limitOrder
												.getLimitPrice()
												.multiply(tradeableAmount)
												.setScale(2,
														RoundingMode.HALF_DOWN)
												.stripTrailingZeros()
										+ " "
										+ x.limitOrder.getCurrencyPair().counterSymbol
										+ ")");
							}

						});
	}

	@Override
	public Set<String> getSuggestions(List<Exchange> exchanges)
			throws ExchangeException, NotAvailableFromExchangeException,
			NotYetImplementedForExchangeException, IOException {
		// TODO Auto-generated method stub
		Set<String> suggestions = new HashSet<>();

		for (ExchangeLimitOrder order : cycle.GetExchangeLimitOrders()) {
			BigDecimal amount = order.exchange
					.getPollingAccountService()
					.getAccountInfo()
					.getBalance(
							order.limitOrder.getType() == OrderType.ASK ? order.limitOrder
									.getCurrencyPair().counterSymbol
									: order.limitOrder.getCurrencyPair().baseSymbol);
			if (amount.floatValue() < 0.1) {
				float biggestPlace = 0;
				Exchange bestExchangeSpecification = null;
				for (Exchange ex : exchanges) {
					try {

						float bal;
						if ((bal = TradeBot.wallets
								.get(ex)
								.get(order.limitOrder.getType() == OrderType.ASK ? order.limitOrder
										.getCurrencyPair().counterSymbol
										: order.limitOrder.getCurrencyPair().baseSymbol)
								.floatValue()) > 0.1) {
							biggestPlace = Math.max(biggestPlace, bal);
							if (biggestPlace == bal) {
								bestExchangeSpecification = ex;
							}
						}

					} catch (Exception e) {
					}
				}
				if (bestExchangeSpecification != null && biggestPlace > 0) {
					suggestions
							.add("You should send some "
									+ (order.limitOrder.getType() == OrderType.ASK ? order.limitOrder
											.getCurrencyPair().counterSymbol
											: order.limitOrder
													.getCurrencyPair().baseSymbol)
									+ " from "
									+ bestExchangeSpecification
											.getExchangeSpecification()
											.getExchangeName()
									+ " to "
									+ order.exchange.getExchangeSpecification()
											.getExchangeName() + ".");
				}
			}
		}
		return suggestions;
	}
}
