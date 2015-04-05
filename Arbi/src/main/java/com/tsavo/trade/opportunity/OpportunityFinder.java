package com.tsavo.trade.opportunity;

import java.io.IOException;

import com.tsavo.trade.OpportunityExecutor;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public interface OpportunityFinder {
	public void findOpportunities(OpportunityExecutor anExecutor) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException;

}
