package com.tsavo.trade.opportunity.cycle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.tsavo.trade.ExchangeLimitOrder;
import com.tsavo.trade.OpportunityExecutor;
import com.tsavo.trade.PriceIndex;
import com.xeiam.xchange.cryptsy.CryptsyCurrencyUtils;
import com.xeiam.xchange.currency.CurrencyPair;

public class CurrencyCycle {

	CurrencyCycle parentCycle;
	public String baseSymbol;
	public List<CurrencyCycle> counterSymbols = new ArrayList<CurrencyCycle>();
	public ExchangeLimitOrder exchangeLimitOrder;
	public BigDecimal balance;
	static Map<Integer, Integer> marketSubs = new HashMap<>();
	static {
		marketSubs.put(464, 445);
		marketSubs.put(441, 454);
	}

	// public BigDecimal actual;
	// public BigDecimal previousActual;
	// public LimitOrder limitOrder;

	public CurrencyCycle(String aBaseSymbol, CurrencyCycle aParentCycle) {
		this(aBaseSymbol);
		parentCycle = aParentCycle;

	}

	public CurrencyCycle(String aBaseSymbol) {
		baseSymbol = aBaseSymbol;
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

	public boolean isLeaf() {
		return counterSymbols.size() == 0;
	}

	public CurrencyCycle populateCycles(Collection<CurrencyPair> someCurrencyPairs, String rootCurrency) {
		findCycle(someCurrencyPairs, baseSymbol, this, rootCurrency);
		return this;
	}

	private CurrencyCycle findCycle(Collection<CurrencyPair> someCurrencyPairs, String aCurrency, CurrencyCycle currentCycle, String rootCurrency) {

		if (currentCycle.GetCycleLength() > 5) {
			return this;
		}
		Set<String> nexts = findNextInCycle(aCurrency, someCurrencyPairs);

		for (String next : nexts) {
			if (currentCycle.GetCurrencyCycle().contains(next) && !next.equals(rootCurrency)) {
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

	private Set<String> findNextInCycle(String aCurrency, Collection<CurrencyPair> somePairs) {
		return somePairs.stream().filter(x -> hasCurrency(x, aCurrency)).map(x -> {
			return x.baseSymbol.equals(aCurrency) ? x.counterSymbol : x.baseSymbol;
		}).collect(Collectors.<String> toSet());
	}

	private boolean hasCurrency(CurrencyPair aPair, String aCurrency) {
		return aPair.baseSymbol.equals(aCurrency) || aPair.counterSymbol.equals(aCurrency);
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
			leaves.addAll(counterSymbols.stream().flatMap(x -> x.GetAllLeaves().stream()).collect(Collectors.<CurrencyCycle> toSet()));
		}
		return leaves;
	}

	public List<ExchangeLimitOrder> GetExchangeLimitOrders() {
		List<ExchangeLimitOrder> orders = new ArrayList<>();
		if (this.parentCycle != null) {
			orders.addAll(parentCycle.GetExchangeLimitOrders());
		}
		if (exchangeLimitOrder != null) {
			orders.add(exchangeLimitOrder);
		}
		return orders;
	}

	static DecimalFormat format = new DecimalFormat("###,###.########");

	@Override
	public String toString() {

		String str = baseSymbol + "(" + format.format(balance.floatValue()) + ")";
		// if (previousActual != BigDecimal.ONE && previousActual != null) {
		// str += format.format(previousActual);
		// }
		if (parentCycle != null) {
			return parentCycle.toString() + " " + exchangeLimitOrder.exchange.getExchangeSpecification().getExchangeName() + "-> " + str;
		}
		return str;
	}

	public float getSize() {
		return balance.subtract(BigDecimal.ONE).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_DOWN).floatValue();
	}

	public void populateCycle(PriceIndex aPriceIndex, OpportunityExecutor anExecutor) {

		for (CurrencyCycle cycle : counterSymbols) {
			String counterCurrency = cycle.baseSymbol;

			int market = CryptsyCurrencyUtils.convertToMarketId(new CurrencyPair(baseSymbol, counterCurrency));
			if (marketSubs.containsKey(market)) {
				market = marketSubs.get(market);
			}
			CurrencyPair realPair = CryptsyCurrencyUtils.convertToCurrencyPair(market);
			if (realPair.baseSymbol.equals("DASH")) {
				realPair = new CurrencyPair("DRK", realPair.counterSymbol);
			}
			if (realPair.counterSymbol.equals("DASH")) {
				realPair = new CurrencyPair(realPair.baseSymbol, "DRK");
			}
			if (realPair.baseSymbol.equals(counterCurrency)) {
				ExchangeLimitOrder order = aPriceIndex.getLowestSellPrice(realPair);
				cycle.balance = balance.divide(order.limitOrder.getLimitPrice(), 8, RoundingMode.HALF_DOWN).multiply(new BigDecimal(0.998));
				cycle.exchangeLimitOrder = order;

			} else {
				ExchangeLimitOrder order = aPriceIndex.getHighestBuyPrice(realPair);
				cycle.balance = balance.multiply(order.limitOrder.getLimitPrice()).multiply(new BigDecimal(0.998));
				cycle.exchangeLimitOrder = order;

			}
			cycle.populateCycle(aPriceIndex, anExecutor);
		}
		if(isLeaf() && balance != null && balance.floatValue() > 1.001){
			anExecutor.executeOpportunity(new CycleOpportunity(this));
		}
	}
}
