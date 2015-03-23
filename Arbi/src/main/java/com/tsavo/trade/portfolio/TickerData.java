package com.tsavo.trade.portfolio;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TickerData {
	private TreeSet<TickerDataPoint> data = new TreeSet<>();
	private long windowSize = (1000 * 60 * 60 * 24 * 3);

	private transient List<TickerListener> listeners = new ArrayList<>();

	public TickerData() {
	}

	public TickerData(long aWindowSize) {
		windowSize = aWindowSize;
	}

	public void addSample(float aSample, long aTimeStamp, float aVolume) {

		data.add(new TickerDataPoint(aSample, aTimeStamp, aVolume));
		for (TickerListener l : listeners) {
			l.sampleAdded(aSample, aTimeStamp, aVolume);
		}
		cleanUp();
	}

	public void addListener(TickerListener aListener) {
		listeners.add(aListener);
	}

	@JsonIgnore
	public float getHigh() {
		cleanUp();
		float high = 0;
		for (TickerDataPoint d : data) {
			high = Math.max(high, d.getPrice());
		}
		return high;
	}

	@JsonIgnore
	public float getLow() {
		cleanUp();
		float low = Float.MAX_VALUE;
		for (TickerDataPoint d : data) {
			low = Math.min(low, d.getPrice());
		}
		return low;

	}

	public void cleanUp() {
		Iterator<TickerDataPoint> i = data.iterator();
		while (i.hasNext()) {
			TickerDataPoint f = i.next();
			if (f.getTimestamp() < System.currentTimeMillis() - windowSize) {
				i.remove();
			}
		}

	}

	@JsonIgnore
	public float getAverage() {
		cleanUp();
		float average = 0;
		float weight = 0;
		for (TickerDataPoint f : data) {
			average += f.getPrice() * f.getVolume();
			weight += f.getVolume();
		}
		return average / weight;
	}

	public long getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(long windowSize) {
		this.windowSize = windowSize;
	}

	public void setIndex(int index) {
	}

	public int size() {
		return data.size();
	}

	public TreeSet<TickerDataPoint> getData() {
		return data;
	}

	public void setData(TreeSet<TickerDataPoint> aData) {
		this.data = aData;
	}

}
