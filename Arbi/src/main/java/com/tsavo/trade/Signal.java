package com.tsavo.trade;

import java.math.BigDecimal;
import java.util.List;

import com.tsavo.trade.AbstractSignal.SignalTestResults;

public interface Signal {

	public boolean isLong();
	public boolean isShort();
	public String getName();
	public void reset();
	public void addSample(BigDecimal item);
	public SignalTestResults test(List<BigDecimal> signalData, List<BigDecimal> tradePrices, BigDecimal exitDifference, BigDecimal stopLoss);
	

}
