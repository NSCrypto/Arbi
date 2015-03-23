package com.tsavo.trade.opportunity.cycle;

import java.util.List;
import java.util.stream.Collectors;

public class MarketCycle {

	public List<String> markets;

	public MarketCycle(List<String> markets) {
		super();
		this.markets = markets;
	}

	@Override
	public boolean equals(Object aThat) {
		if (this == aThat)
			return true;
		if (!(aThat instanceof MarketCycle))
			return false;
		MarketCycle that = (MarketCycle) aThat;
		if (this.markets.size() != that.markets.size()) {
			return false;
		}
		if (this.markets.size() == 0) {
			return true;
		}
		for (int i = 0; i < this.markets.size(); i++) {
			if (!this.markets.get(i).equals(that.markets.get(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return markets.stream().mapToInt(x -> x.hashCode() * 31).sum();
	}
	
	 @Override
	public String toString() {
		 return markets.stream().collect(Collectors.joining(" -> "));
	 }
}
