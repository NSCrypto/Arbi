package com.tsavo.trade.opportunity.cycle;

import java.util.Collection;

import com.xeiam.xchange.currency.CurrencyPair;

public class CycleFinder {
	public static CurrencyCycle findCurrencyCycle(String baseCurrency, Collection<CurrencyPair> currencies) {

		CurrencyCycle cycle = new CurrencyCycle(baseCurrency);
		cycle.populateCycles(currencies, baseCurrency);

		return cycle;
	}

	public static CurrencyCycle findCycle(String baseCurrency, String counterCurrency, Collection<CurrencyPair> currencies) {
		CurrencyCycle cycle = new CurrencyCycle(baseCurrency);
		cycle.populateCycles(currencies, counterCurrency);

		return cycle;
	}

}