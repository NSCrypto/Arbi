package com.tsavo.trade.opportunity;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class BuyOpportunity implements Opportunity {

	Exchange exchange;
	float size;
	CurrencyPair currencyPair;
	BigDecimal amountToTrade;

	LimitOrder bid;

	public BuyOpportunity(Exchange exchange, float size, CurrencyPair currencyPair, BigDecimal amountToTrade, LimitOrder bid) {
		super();
		this.exchange = exchange;
		this.size = size;
		this.currencyPair = currencyPair;
		this.amountToTrade = amountToTrade;

		this.bid = bid;
	}

	public float getSize() {
		return size;
	}

	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	public BigDecimal getAmountToTrade() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		BigDecimal balance = exchange.getPollingAccountService().getAccountInfo().getBalance(currencyPair.counterSymbol);
		float balanceNeeded = amountToTrade.floatValue() * bid.getLimitPrice().floatValue();
		if (balanceNeeded > balance.floatValue()) {
			return BigDecimal.ZERO;
		}

		return amountToTrade;
	}

	public void trade(BigDecimal anAmmount, Portfolio aPortfolio) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		System.out.println("We're buying " + anAmmount + " " + currencyPair.baseSymbol + " @ " + exchange.getExchangeSpecification().getExchangeName() + " for " + bid.getLimitPrice());
		exchange.getPollingTradeService().placeLimitOrder(bid);
		aPortfolio.addSample(currencyPair, bid.getLimitPrice().floatValue(), anAmmount.floatValue());
		return;
	}

	public Set<String> getSuggestions(List<Exchange> exchanges) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		Set<String> suggestions = new HashSet<String>();
		BigDecimal balance = exchange.getPollingAccountService().getAccountInfo().getBalance(currencyPair.counterSymbol);
		float balanceNeeded = amountToTrade.floatValue() * bid.getLimitPrice().floatValue();
		if (balanceNeeded > balance.floatValue() && amountToTrade.floatValue() >= 1) {
			DecimalFormat format = new DecimalFormat("#.########");
			suggestions.add("We're low on " + currencyPair.counterSymbol + " @ " + exchange.getExchangeSpecification().getExchangeName() + " or else we would buy " + format.format(amountToTrade.floatValue()) + " " + currencyPair.baseSymbol + " @ "
					+ format.format(bid.getLimitPrice().floatValue()));
		}
		return suggestions;
	}

}
