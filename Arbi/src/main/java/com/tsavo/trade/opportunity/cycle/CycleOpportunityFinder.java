package com.tsavo.trade.opportunity.cycle;

import java.io.IOException;
import java.util.List;

import com.tsavo.trade.Cryptsy;
import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class CycleOpportunityFinder implements OpportunityFinder {

	Cryptsy cryptsy;
	List<MarketCycle> marketCycles;

	public CycleOpportunityFinder() throws IOException {
		cryptsy = new Cryptsy();
		//marketCycles = CycleFinder.findCycles("BTC", cryptsy.currencyPairs);
	}

	@Override
	public List<Opportunity> findOpportunities() throws ExchangeException,
			NotAvailableFromExchangeException,
			NotYetImplementedForExchangeException, IOException {
		return null;
	}
}
