package com.tsavo.trade.portfolio;

import java.util.Iterator;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xeiam.xchange.currency.CurrencyPair;

public class HoldingsReport {

	protected static final float COIN = 10000000;
	private CurrencyPair currencyPair;

	private TreeSet<Holding> holdings = new TreeSet<>();

	public HoldingsReport() {
	}

	public HoldingsReport(CurrencyPair aPair) {
		currencyPair = aPair;
	}

	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	public void setCurrencyPair(CurrencyPair currencyPair) {
		this.currencyPair = currencyPair;
	}

	public void add(float anAmount, float aValue) {
		for (Holding holding : holdings) {
			if (holding.getValue() == aValue) {
				holding.setAmount(holding.getAmount() + anAmount);
				return;
			}
		}
		holdings.add(new Holding(anAmount, aValue));
		Iterator<Holding> hi = holdings.iterator();
		while (hi.hasNext()) {
			Holding holding = hi.next();
			if (holding.amount < 0.1) {
				hi.remove();
				continue;
			}
		}
	}

	public float remove(float anAmount) {
		Iterator<Holding> hi = holdings.iterator();
		float burnedAmount = 0;
		float burnedWeight = 0;
		while (hi.hasNext()) {
			if (anAmount < 0.00000001) {
				return burnedAmount / burnedWeight;
			}
			Holding holding = hi.next();
			if (anAmount >= holding.amount || holding.amount < 0.1) {
				anAmount -= holding.amount;
				burnedAmount += holding.value * holding.amount;
				burnedWeight += holding.amount;
				hi.remove();
				continue;
			}
			holding.setAmount(holding.getAmount() - anAmount);
			burnedAmount += holding.value * holding.amount;
			burnedWeight += holding.amount;
			return burnedAmount / burnedWeight;
		}
		if (burnedWeight == 0) {
			return 0;
		}
		return burnedAmount / burnedWeight;
	}

	@JsonIgnore
	public float getAverageValue() {
		Iterator<Holding> hi = holdings.iterator();
		while (hi.hasNext()) {
			Holding holding = hi.next();
			if (holding.amount < 0.1) {
				hi.remove();
				continue;
			}
		}
		float value = 0;
		float weight = 0;
		if (holdings.size() == 0) {
			return 0;
		}
		for (Holding holding : holdings) {
			value += holding.getValue() * holding.getAmount();
			weight += holding.getAmount();
		}
		return value / weight;
	}

	@JsonIgnore
	public float getTotalAmount() {
		float total = 0;
		for (Holding holding : holdings) {
			total += holding.amount;
		}
		return total;
	}

	public TreeSet<Holding> getHoldings() {
		return holdings;
	}

	public void setHoldings(TreeSet<Holding> holdings) {
		this.holdings = holdings;
	}
}
