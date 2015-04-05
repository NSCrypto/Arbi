package com.tsavo.trade.opportunity.cycle;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.tsavo.trade.Cryptsy;
import com.tsavo.trade.OpportunityExecutor;
import com.tsavo.trade.PriceIndex;
import com.tsavo.trade.Wallet;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.xeiam.xchange.currency.CurrencyPair;

public class CycleOpportunityFinder implements OpportunityFinder {

	PriceIndex priceIndex;
	CurrencyCycle cycles;
	CurrencyCycle usdcycles;
	Wallet wallet;

	public CycleOpportunityFinder(String aBaseCurrency, List<CurrencyPair> somePairs, PriceIndex aPriceIndex, Wallet aWallet) throws IOException {
		new Cryptsy();
		priceIndex = aPriceIndex;
		wallet = aWallet;
		cycles = CycleFinder.findCurrencyCycle(aBaseCurrency, somePairs);
	}

	@Override
	public void findOpportunities(OpportunityExecutor anExecutor) {
		Thread t = new Thread("Cycle Opportunity finder for " + cycles.baseSymbol) {
			@Override
			public void run() {
				while (true) {
					cycles.balance = BigDecimal.ONE;
					cycles.populateCycle(priceIndex, anExecutor);
				}
			}
		};
		t.start();

	}

}
