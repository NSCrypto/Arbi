package com.tsavo.trade.opportunity.cycle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.xeiam.xchange.currency.CurrencyPair;

public class CurrencyCycle {

	public CurrencyCycle(String aBaseSymbol) {
		baseSymbol = aBaseSymbol;
	}
	
	public BigDecimal balance;

	public CurrencyCycle(String aBaseSymbol, CurrencyCycle aParentCycle) {
		this(aBaseSymbol);
		parentCycle = aParentCycle;
	}

	public List<String> GetCurrencyCycle() {
		List<String> all = new ArrayList<>();
		if (parentCycle != null) {
			all.addAll(parentCycle.GetCurrencyCycle());
		}
		all.add(baseSymbol);
		return all;
	}

	public CurrencyCycle populateCycles(List<CurrencyPair> someCurrencyPairs,
			String rootCurrency) {
		findCycle(someCurrencyPairs, baseSymbol, this, rootCurrency);
		return this;
	}

	private CurrencyCycle findCycle(List<CurrencyPair> someCurrencyPairs,
			String aCurrency, CurrencyCycle currentCycle, String rootCurrency) {


		List<String> nexts = findNextInCycle(aCurrency, someCurrencyPairs);

		for (String next : nexts) {
			if (currentCycle.GetCurrencyCycle().contains(next)
					&& !next.equals(rootCurrency)) {
				continue;
			}
			CurrencyCycle cycle;
			if (next.equals(rootCurrency)) {
				cycle = new CurrencyCycle(next, currentCycle);
				currentCycle.counterSymbols.add(cycle);
			} else {
				cycle = new CurrencyCycle(next, currentCycle);
				currentCycle.counterSymbols.add(cycle);
				cycle.populateCycles(someCurrencyPairs, rootCurrency);
				if (cycle.counterSymbols.size() == 0) {
					currentCycle.counterSymbols.remove(cycle);
				}
			}
		}
		return currentCycle;
	}

	private List<String> findNextInCycle(String aCurrency,
			List<CurrencyPair> somePairs) {
		return somePairs
				.stream()
				.filter(x -> hasCurrency(x, aCurrency))
				.map(x -> {
					return x.baseSymbol.equals(aCurrency) ? x.counterSymbol
							: x.baseSymbol;
				}).collect(Collectors.<String> toList());
	}

	private boolean hasCurrency(CurrencyPair aPair, String aCurrency) {
		return aPair.baseSymbol.equals(aCurrency)
				|| aPair.counterSymbol.equals(aCurrency);
	}

	public int GetCycleLength() {
		if (parentCycle == null) {
			return 1;
		}
		return 1 + parentCycle.GetCycleLength();
	}

	public List<CurrencyCycle> GetAllLeaves() {
		List<CurrencyCycle> leaves = new ArrayList<>();
		if (this.counterSymbols.size() == 0) {
			leaves.add(this);
		} else {
			leaves.addAll(counterSymbols.stream()
					.flatMap(x -> x.GetAllLeaves().stream())
					.collect(Collectors.<CurrencyCycle> toSet()));
		}
		return leaves;
	}

	CurrencyCycle parentCycle;
	public String baseSymbol;
	public List<CurrencyCycle> counterSymbols = new ArrayList<CurrencyCycle>();
}
