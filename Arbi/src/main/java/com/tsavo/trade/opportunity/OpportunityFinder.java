package com.tsavo.trade.opportunity;

import java.io.IOException;
import java.util.List;

import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public interface OpportunityFinder {
	public List<Opportunity> findOpportunities() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException;

}
