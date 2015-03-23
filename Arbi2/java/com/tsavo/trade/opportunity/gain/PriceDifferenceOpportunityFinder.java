package com.tsavo.trade.opportunity.gain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.tsavo.trade.opportunity.BuyOpportunity;
import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.tsavo.trade.opportunity.SellOpportunity;
import com.tsavo.trade.portfolio.Portfolio;
import com.tsavo.trade.portfolio.TickerData;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.marketdata.OrderBook;
import com.xeiam.xchange.dto.trade.LimitOrder;

public class PriceDifferenceOpportunityFinder implements OpportunityFinder {
	private static final float REFERENCE_PRICE = 0.0009f;
	public static final float WALLET_LOW_MIN = 0.1f;
	public static final float WALLET_HIGH_MIN = 0.1f;
	public static final int WALLET_MAX = 1000;
	List<Exchange> exchanges;
	Portfolio portfolio;
	Set<String> suggestions;

	public PriceDifferenceOpportunityFinder(List<Exchange> someExchanges, Portfolio aPortfolio) {
		exchanges = someExchanges;
		portfolio = aPortfolio;
	}

	public List<Opportunity> findOpportunities() {
		List<Opportunity> opportunities = new ArrayList<Opportunity>();
		for (Exchange exchange : exchanges) {
			for (CurrencyPair pair : exchange.getPollingMarketDataService().getExchangeSymbols()) {
				try {
					if (!exchange.isSupportedCurrencyPair(pair)) {
						continue;
					}
					BigMoney balance = exchange.getPollingAccountService().getAccountInfo().getBalance(CurrencyUnit.of(pair.baseCurrency));
					float average = portfolio.getAverageValue(pair);
					if (average == 0) {
						// we don't have any to set the price. Time to go to the ticker!!!
						Map<CurrencyPair, TickerData> map = portfolio.tickers.get(exchange.getExchangeSpecification().getExchangeName());
						if (map == null) {
							continue;
						}
						TickerData data = map.get(pair);
						if (data == null) {
							continue;
						}

						average = (data.getAverage() + data.getLow()) / 2;
					}
					OrderBook book = exchange.getPollingMarketDataService().getFullOrderBook(pair.baseCurrency, pair.counterCurrency);
					if (book == null) {
						System.out.println("Couldn't get the market data for " + pair + " on " + exchange.getExchangeSpecification().getExchangeName() + ", skipping.");
						continue;
					}
					LimitOrder bidOrder = book.getHighestBid();
					LimitOrder askOrder = book.getLowestAsk();
					if (bidOrder == null || askOrder == null) {
						System.out.println("Couldn't get the market data for " + pair + " on " + exchange.getExchangeSpecification().getExchangeName() + ", skipping.");
						continue;
					}
					float adder = (REFERENCE_PRICE - average) * 100000;
					adder = Math.max(adder, -(WALLET_MAX - WALLET_LOW_MIN));
					if (balance.getAmount().floatValue() < Math.min(Math.max(WALLET_MAX, WALLET_MAX + adder), WALLET_MAX + adder)) {
						if (average * portfolio.buyMultiplier > askOrder.getLimitPrice().getAmount().floatValue()) {
							BigDecimal amount = new BigDecimal(Math.min(WALLET_MAX + adder, Math.min(bidOrder.getTradableAmount().floatValue(), (WALLET_MAX + adder - balance.getAmount().floatValue()))));
							bidOrder = new LimitOrder(OrderType.BID, amount, pair.baseCurrency, pair.counterCurrency, askOrder.getLimitPrice());
							opportunities.add(new BuyOpportunity(exchange, 1, pair, amount, bidOrder));
							continue;
						}
					}
					if (balance.getAmount().floatValue() < Math.max(WALLET_LOW_MIN, Math.min(WALLET_HIGH_MIN, WALLET_MAX + adder)) || (average * portfolio.sellMultiplier) >= bidOrder.getLimitPrice().getAmount().floatValue()) {
						continue;
					}
					BigDecimal amount = new BigDecimal(Math.min(Math.min(Math.min(Math.max(WALLET_HIGH_MIN, WALLET_MAX + adder), WALLET_MAX + adder), balance.getAmount().floatValue() - WALLET_HIGH_MIN), bidOrder.getTradableAmount().floatValue()));
					askOrder = new LimitOrder(OrderType.ASK, amount, pair.baseCurrency, pair.counterCurrency, bidOrder.getLimitPrice());
					opportunities.add(new SellOpportunity(exchange, 1, pair, amount, askOrder));
				} catch (Exception e) {
					System.out.println(exchange.getExchangeSpecification().getExchangeName() + " isn't working. Skipping for now.");
					e.printStackTrace();
					continue;
				}
			}
		}
		return opportunities;
	}
}
