package com.tsavo.trade.opportunity;

import java.io.IOException;
import java.util.Set;

import com.tsavo.trade.OpportunityExecutor;
import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public interface Opportunity extends Comparable<Opportunity> {

	public float getSize();

	public boolean canTrade(Portfolio aPortfolio);

	public Set<String> getSuggestions(Portfolio aPortfolio);

	void trade(OpportunityExecutor anExecutor) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException;
}
