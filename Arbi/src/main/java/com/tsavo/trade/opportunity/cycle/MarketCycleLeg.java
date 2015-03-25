package com.tsavo.trade.opportunity.cycle;

import java.math.BigDecimal;

public class MarketCycleLeg {


	public MarketCycleLeg(String x) {
		marketName = x;
	}
	
	@Override
	public String toString() {
		return marketName + (tradeAmount != null ? " (" + tradeAmount + ")" : "");
	}
	
	public String marketName;
	public BigDecimal tradeAmount;
}
