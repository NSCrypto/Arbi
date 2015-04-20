package com.tsavo.trade.opportunity.cycle;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.time.StopWatch;

import com.tsavo.trade.OpportunityExecutor;
import com.tsavo.trade.SpeechSynthesizer;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.currency.CurrencyPair;

public class CycleOpportunityFinder implements OpportunityFinder {

	CurrencyCycle cycles;
	Portfolio portfolio;

	public CycleOpportunityFinder(String aBaseCurrency, List<CurrencyPair> somePairs, Portfolio aPortfolio) {
		portfolio = aPortfolio;
		cycles = CycleFinder.findCurrencyCycle(aBaseCurrency, somePairs);
	}

	@Override
	public void findOpportunities(OpportunityExecutor anExecutor) {
		Thread t = new Thread("Cycle Opportunity finder for " + cycles.baseSymbol) {
			@Override
			public void run() {
				SpeechSynthesizer speech = new SpeechSynthesizer();
				while (true) {
					StopWatch watch = new StopWatch();
					watch.start();
					cycles.balance = BigDecimal.ONE;
					cycles.populateCycle(portfolio, anExecutor);
					watch.split();
					System.out.println("Opportunity cycle " + cycles.baseSymbol + " completed in " + watch.toSplitString() + ".");
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		};
		t.start();

	}

}
