package com.tsavo.trade.opportunity;

import java.io.IOException;
import java.math.BigDecimal;

import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public abstract class AbstractOpportunity implements Opportunity {

	public float size;
	CurrencyPair currencyPair;
	BigDecimal amountToTrade;

	public AbstractOpportunity(CurrencyPair aPair, float aSize, BigDecimal anAmountToTrade){
		size = aSize;
		currencyPair = aPair;
		amountToTrade = anAmountToTrade;
	}
	@Override
	public int compareTo(Opportunity anOpportunity) {
		return (int) ((getSize() - anOpportunity.getSize()) * 100000000f);
	}

	@Override
	public float getSize() {
		return size;
	}

	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	public BigDecimal getAmountToTrade() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		return amountToTrade;
	}
}
