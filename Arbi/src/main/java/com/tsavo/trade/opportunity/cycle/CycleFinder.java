package com.tsavo.trade.opportunity.cycle;

import java.util.Set;
import java.util.stream.Collectors;

import com.xeiam.xchange.currency.CurrencyPair;

public class CycleFinder {
	public Set<MarketCycle> findCycles(Set<CurrencyPair> currencies) {
		return currencies
				.stream()
				.map(x -> {
					CurrencyCycle cycle = new CurrencyCycle(x.baseSymbol);
					cycle.populateCycles(currencies, x.baseSymbol);
					return cycle;
				})
				.flatMap(
						cycle -> cycle.GetAllLeaves().stream()
								.map(y -> y.GetCurrencyCycle())
								.filter(y -> y.size() > 3))
								.map(y -> new MarketCycle(y))
				.collect(Collectors.<MarketCycle> toSet());
	}
}