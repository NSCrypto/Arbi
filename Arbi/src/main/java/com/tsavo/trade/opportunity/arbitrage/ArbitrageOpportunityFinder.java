package com.tsavo.trade.opportunity.arbitrage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.OrderBook;
import com.xeiam.xchange.dto.trade.LimitOrder;


public class ArbitrageOpportunityFinder implements OpportunityFinder {

	private List<Exchange> exchanges;
	private Portfolio portfolio;

	public ArbitrageOpportunityFinder(List<Exchange> exchanges,
			Portfolio aPortfolio) {
		super();
		this.exchanges = exchanges;
		this.portfolio = aPortfolio;
	}

	public List<Opportunity> findOpportunities() throws IOException {
		List<Opportunity> opportunities = new ArrayList<Opportunity>();

		List<String> interestingPairs = new ArrayList<>();
		interestingPairs.add("BTC");
		interestingPairs.add("LTC");
		interestingPairs.add("USD");
		interestingPairs.add("DRK");
		for (CurrencyPair pair : exchanges
				.stream()
				.flatMap(
						x -> {

							try {
								return x.getPollingMarketDataService()
										.getExchangeSymbols().stream().filter(y -> interestingPairs.contains(y.baseSymbol) && interestingPairs.contains(y.counterSymbol));
							} catch (Exception e) {
								return new ArrayList<CurrencyPair>().stream();
							}
						}).distinct().collect(Collectors.<CurrencyPair> toList())) {
			Map<List<LimitOrder>, Exchange> asks = new HashMap<List<LimitOrder>, Exchange>();
			Map<List<LimitOrder>, Exchange> bids = new HashMap<List<LimitOrder>, Exchange>();
			for (Exchange exchange : exchanges) {

				try {
					com.xeiam.xchange.service.polling.marketdata.PollingMarketDataService service = exchange
							.getPollingMarketDataService();

					OrderBook book = service.getOrderBook(pair);
					asks.put(book.getAsks(), exchange);
					bids.put(book.getBids(), exchange);
				} catch (Exception e) {
					System.out.println(exchange.getExchangeSpecification()
							.getExchangeName() + " isn't working for " + pair + ". Skipping.");
					//e.printStackTrace();
					continue;
				}
			}
			for (Map.Entry<List<LimitOrder>, Exchange> askList : asks
					.entrySet()) {
				for (LimitOrder ask : askList.getKey().stream().sorted((x,y) -> x.getLimitPrice().compareTo(y.getLimitPrice())).filter(x -> x.getTradableAmount().floatValue() > 0.0099).limit(5).collect(Collectors.toList())) {
					outer: for (Map.Entry<List<LimitOrder>, Exchange> bidList : bids
							.entrySet()) {
						for (LimitOrder bid : bidList.getKey().stream().sorted((x,y) -> y.getLimitPrice().compareTo(x.getLimitPrice())).filter(x-> x.getTradableAmount().floatValue() > 0.0099).limit(5).collect(Collectors.toList())) {
							if (ask.getLimitPrice().floatValue() < bid
									.getLimitPrice().floatValue()) {
								float difference = bid.getLimitPrice()
										.floatValue()
										- ask.getLimitPrice().floatValue();
								// BigMoney average =
								// bid.getLimitPrice().toBigMoney().plus(ask.getLimitPrice().toBigMoney()).dividedBy(2l,
								// RoundingMode.HALF_EVEN);
								float percent = (difference / Math.max(bid
										.getLimitPrice().floatValue(), ask
										.getLimitPrice().floatValue())) * 100;
								if (percent >= 1.1) {
									System.out
											.println("Arbitrage opportunity found: "
													+ pair.baseSymbol
													+ "->"
													+ pair.counterSymbol
													+ " Bid: "
													+ bid.getLimitPrice()
															.toPlainString()
													+ " @ "
													+ bidList
															.getValue()
															.getExchangeSpecification()
															.getExchangeName()
													+ "("
													+ bid.getTradableAmount()
															.floatValue()
													+ "), Ask: "
													+ ask.getLimitPrice()
															.toPlainString()
													+ " @ "
													+ askList
															.getValue()
															.getExchangeSpecification()
															.getExchangeName()
													+ "("
													+ ask.getTradableAmount()
															.floatValue()
													+ ") ("
													+ percent
													+ "% gain)");
									ArbitrageOpportunity opportunity = new ArbitrageOpportunity(
											pair, askList.getValue(), ask,
											bidList.getValue(), bid, percent);
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
