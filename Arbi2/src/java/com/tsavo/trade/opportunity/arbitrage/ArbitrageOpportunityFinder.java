package com.tsavo.trade.opportunity.arbitrage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.OrderBook;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.service.polling.PollingMarketDataService;

public class ArbitrageOpportunityFinder implements OpportunityFinder {

	private List<Exchange> exchanges;
	private Portfolio portfolio;

	public ArbitrageOpportunityFinder(List<Exchange> exchanges, Portfolio aPortfolio) {
		super();
		this.exchanges = exchanges;
		this.portfolio = aPortfolio;
	}

	public List<Opportunity> findOpportunities() {
		List<Opportunity> opportunities = new ArrayList<Opportunity>();

		for (CurrencyPair pair : portfolio.getCurrencyPairsInPortfolio()) {
			Map<List<LimitOrder>, Exchange> asks = new HashMap<List<LimitOrder>, Exchange>();
			Map<List<LimitOrder>, Exchange> bids = new HashMap<List<LimitOrder>, Exchange>();
			for (Exchange exchange : exchanges) {
				if (!exchange.isSupportedCurrencyPair(pair)) {
					continue;
				}
				try {
					PollingMarketDataService service = exchange.getPollingMarketDataService();

					OrderBook book = service.getFullOrderBook(pair.baseCurrency, pair.counterCurrency);
					asks.put(book.getAsks(), exchange);
					bids.put(book.getBids(), exchange);
				} catch (Exception e) {
					System.out.println(exchange.getExchangeSpecification().getExchangeName() + " isn't working. Skipping.");
					e.printStackTrace();
					continue;
				}
			}
			for (Map.Entry<List<LimitOrder>, Exchange> askList : asks.entrySet()) {
				for (LimitOrder ask : askList.getKey()) {
					outer: for (Map.Entry<List<LimitOrder>, Exchange> bidList : bids.entrySet()) {
						for (LimitOrder bid : bidList.getKey()) {
							if (askList.getValue().equals(bidList.getValue())) {
								continue outer;
							}
							if (ask.getLimitPrice().isLessThan(bid.getLimitPrice())) {
								float difference = bid.getLimitPrice().getAmount().floatValue() - ask.getLimitPrice().getAmount().floatValue();
								// BigMoney average =
								// bid.getLimitPrice().toBigMoney().plus(ask.getLimitPrice().toBigMoney()).dividedBy(2l,
								// RoundingMode.HALF_EVEN);
								float percent = (difference / Math.max(bid.getLimitPrice().getAmount().floatValue(), ask.getLimitPrice().getAmount().floatValue())) * 100;
								if (percent >= 1.1 && ask.getTradableAmount().floatValue() >= 1 && bid.getTradableAmount().floatValue() >= 1) {
									System.out.println("Arbitrage opportunity found: " + bid.getTradableIdentifier() + "->" + bid.getTransactionCurrency() + " Bid: " + bid.getLimitPrice().getAmount().toPlainString() + " @ " + bidList.getValue().getExchangeSpecification().getExchangeName() + "("
											+ bid.getTradableAmount().floatValue() + "), Ask: " + ask.getLimitPrice().getAmount().toPlainString() + " @ " + askList.getValue().getExchangeSpecification().getExchangeName() + "(" + ask.getTradableAmount().floatValue() + ") (" + percent + "% gain)");
									ArbitrageOpportunity opportunity = new ArbitrageOpportunity(pair, askList.getValue(), ask, bidList.getValue(), bid, percent);
									opportunity.setCurrencyPair(pair);
									opportunities.add(opportunity);

								}
							}
						}
					}
				}
			}
		}
		return opportunities;
	}

}
