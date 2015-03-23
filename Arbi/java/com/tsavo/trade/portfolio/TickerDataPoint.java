package com.tsavo.trade.portfolio;

public class TickerDataPoint implements Comparable<TickerDataPoint> {
	private float price;
	private long timestamp;
	private float volume;

	public TickerDataPoint() {

	}

	public TickerDataPoint(float aPrice, long aTimeStamp, float aVolume) {
		price = aPrice;
		timestamp = aTimeStamp;
		setVolume(aVolume);
	}

	@Override
	public int compareTo(TickerDataPoint o) {
		return Long.compare(getTimestamp(), o.getTimestamp());
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
	}

}
