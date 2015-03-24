package com.tsavo.trade.opportunity.cycle;

import java.util.List;
import java.util.stream.Collectors;

import com.xeiam.xchange.currency.CurrencyPair;

public class CycleFinder {
	public static CurrencyCycle findCurrencyCycle(String baseCurrency,
			List<CurrencyPair> currencies) {

		CurrencyCycle cycle = new CurrencyCycle(baseCurrency);
		cycle.populateCycles(currencies, baseCurrency);

		return cycle;
	}

	public static List<MarketCycle> findCycle(String baseCurrency,
			String counterCurrency, List<CurrencyPair> currencies) {
		CurrencyCycle cycle = new CurrencyCycle(baseCurrency);
		cycle.populateCycles(currencies, counterCurrency);

		return cycle.GetAllLeaves().stream()
				.map(y -> new MarketCycle(y.GetCurrencyCycle()))
				.collect(Collectors.<MarketCycle> toList());
	}
	
	
}