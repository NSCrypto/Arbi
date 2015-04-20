package com.tsavo.trade;

import java.math.BigDecimal;
import java.util.List;

public interface Signal {

	public boolean isLong();
	public boolean isShort();
	public void addSample(BigDecimal item);
	public BigDecimal test(List<BigDecimal> signalData, List<BigDecimal> tradePrices, BigDecimal exitDifference, BigDecimal stopLoss);
	

}
