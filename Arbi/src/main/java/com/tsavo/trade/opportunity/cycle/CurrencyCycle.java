package com.tsavo.trade.opportunity.cycle;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.xeiam.xchange.currency.CurrencyPair;

public class CurrencyCycle {

	public CurrencyCycle(String aBaseSymbol) {
		baseSymbol = aBaseSymbol;
	}

	public BigDecimal balance;
	//public BigDecimal actual;
	//public BigDecimal previousActual;
	//public LimitOrder limitOrder;

	public CurrencyCycle(String aBaseSymbol, CurrencyCycle aParentCycle) {
		this(aBaseSymbol);
		parentCycle = aParentCycle;
	}

	public CurrencyCycle(CurrencyCycle cycle) {
		baseSymbol = cycle.baseSymbol;
		balance = cycle.balance;
		if (cycle.parentCycle != null) {
			parentCycle = new CurrencyCycle(cycle.parentCycle);
		}
	}

	public List<String> GetCurrencyCycle() {
		List<String> all = new ArrayList<>();
		if (parentCycle != null) {
			all.addAll(parentCycle.GetCurrencyCycle());
		}
		all.add(baseSymbol);
		return all;
	}

	public CurrencyCycle populateCycles(
			Collection<CurrencyPair> someCurrencyPairs, String rootCurrency) {
		findCycle(someCurrencyPairs, baseSymbol, this, rootCurrency);
		return this;
	}

	private CurrencyCycle findCycle(Collection<CurrencyPair> someCurrencyPairs,
			String aCurrency, CurrencyCycle currentCycle, String rootCurrency) {

		if (currentCycle.GetCycleLength() > 7) {
			return this;
		}
		Set<String> nexts = findNextInCycle(aCurrency, someCurrencyPairs);

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

	private Set<String> findNextInCycle(String aCurrency,
			Collection<CurrencyPair> somePairs) {
		return somePairs
				.stream()
				.filter(x -> hasCurrency(x, aCurrency))
				.map(x -> {
					return x.baseSymbol.equals(aCurrency) ? x.counterSymbol
							: x.baseSymbol;
				}).collect(Collectors.<String> toSet());
	}

	private boolean hasCurrency(CurrencyPair aPair, String aCurrency) {
		return aPair.baseSymbol.equals(aCurrency)
				|| aPair.counterSymbol.equals(aCurrency);
	}

	public CurrencyCycle isolateCycle() {
		return new CurrencyCycle(this);
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

	static DecimalFormat format = new DecimalFormat("###,###.########");

	@Override
	public String toString() {

		String str = baseSymbol + "(" + format.format(balance.floatValue())
				+ ")";
		// if (previousActual != BigDecimal.ONE && previousActual != null) {
		// str += format.format(previousActual);
		// }
		if (parentCycle != null) {
			return parentCycle.toString() + " -> " + str;
		}
		return str;
	}

	CurrencyCycle parentCycle;
	public String baseSymbol;
	public List<CurrencyCycle> counterSymbols = new ArrayList<CurrencyCycle>();
}
