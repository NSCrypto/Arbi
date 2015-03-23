package com.tsavo.trade.portfolio;

public interface TickerListener {

	public void sampleAdded(float aSample, long aTimeStamp, float aVolume);
}
