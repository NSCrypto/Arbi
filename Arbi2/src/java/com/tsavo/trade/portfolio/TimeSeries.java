package com.tsavo.trade.portfolio;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.OHLCDataset;

public class TimeSeries {

	public String currency;
	public TickerData tickerData;
	public long timeRollup;

	public TimeSeries() {

	}

	public TimeSeries(String aCurrency, TickerData someData, long aTimeRollup) {
		currency = aCurrency;
		tickerData = someData;
		timeRollup = aTimeRollup;
	}

	public OHLCDataset getDataSet() {
		List<OHLCDataItem> list = new ArrayList<>();
		long startPeriod = 0;
		float open = 0;
		float high = Float.MIN_VALUE;
		float low = Float.MAX_VALUE;
		float close = 0;
		float volume = 0;
		for (TickerDataPoint item : tickerData.getData()) {
			if (startPeriod + timeRollup < item.getTimestamp()) {
				if (startPeriod != 0) {
					list.add(new OHLCDataItem(new Date(startPeriod), open, high, low, close, volume));
				}
				startPeriod = item.getTimestamp();
				open = item.getPrice();
				high = item.getPrice();
				low = item.getPrice();
				volume = 0;
			}
			high = Math.max(high, item.getPrice());
			low = Math.min(low, item.getPrice());
			volume += item.getVolume();
			close = item.getPrice();
		}
		return new DefaultOHLCDataset(currency, list.toArray(new OHLCDataItem[list.size()]));
	}
}
