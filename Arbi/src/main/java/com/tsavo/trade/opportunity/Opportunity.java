package com.tsavo.trade.opportunity;

import java.io.IOException;
import java.util.Set;

import com.tsavo.trade.OpportunityExecutor;
import com.tsavo.trade.PriceIndex;
import com.tsavo.trade.Wallet;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public interface Opportunity extends Comparable<Opportunity> {

	public float getSize();

	public boolean canTrade(PriceIndex anIndex, Wallet aWallet);

	public Set<String> getSuggestions(Wallet aWallet);

	void trade(OpportunityExecutor anExecutor) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException;
}
