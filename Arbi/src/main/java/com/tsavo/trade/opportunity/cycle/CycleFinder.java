package com.tsavo.trade.opportunity.cycle;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.xeiam.xchange.currency.CurrencyPair;

public class CycleFinder {
	public static CurrencyCycle findCurrencyCycle(String baseCurrency,
			Collection<CurrencyPair> currencies) {

		CurrencyCycle cycle = new CurrencyCycle(baseCurrency);
		cycle.populateCycles(currencies, baseCurrency);

		return cycle;
	}

	public static List<MarketCycle> findCycle(String baseCurrency,
			String counterCurrency, Collection<CurrencyPair> currencies) {
		CurrencyCycle cycle = new CurrencyCycle(baseCurrency);
		cycle.populateCycles(currencies, counterCurrency);

		return cycle.GetAllLeaves().stream()
				.map(y -> new MarketCycle(y.GetCurrencyCycle()))
				.collect(Collectors.<MarketCycle> toList());
	}
	
	
}