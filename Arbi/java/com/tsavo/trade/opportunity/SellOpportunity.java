package com.tsavo.trade.opportunity;

import java.io.IOException;
import java.math.BigDecimal;
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

public class SellOpportunity implements Opportunity {

	Exchange exchange;
	float size;
	CurrencyPair currencyPair;
	BigDecimal amountToTrade;
	LimitOrder ask;

	public SellOpportunity(Exchange exchange, float size, CurrencyPair currencyPair, BigDecimal amountToTrade, LimitOrder ask) {
		super();
		this.exchange = exchange;
		this.size = size;
		this.currencyPair = currencyPair;
		this.amountToTrade = amountToTrade;
		this.ask = ask;

	}

	public float getSize() {
		return size;
	}

	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	public BigDecimal getAmountToTrade() {
		return amountToTrade;
	}

	public void trade(BigDecimal anAmount, Portfolio aPortfolio) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		System.out.println("We are selling " + anAmount + " " + currencyPair.baseSymbol + " @ " + exchange.getExchangeSpecification().getExchangeName() + " for " + ask.getLimitPrice());
		exchange.getPollingTradeService().placeLimitOrder(ask);
		aPortfolio.addEarning(currencyPair, anAmount.floatValue(), ask.getLimitPrice().floatValue());
		return;
	}

	public Set<String> getSuggestions(List<Exchange> exchanges) {
		return new HashSet<String>();
	}

}
