package com.tsavo.trade;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.tsavo.hippo.OHLCVData;
import com.tsavo.hippo.TickerDatabase;
import com.tsavo.hippo.TradeListener;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Trade;


public class LiveTickerReader {

	public TickerDatabase db;

	public volatile boolean running = true;

	private List<TradeListener> listeners = new ArrayList<TradeListener>();

	public LiveTickerReader(final String anExchangeName) {
		db = new TickerDatabase(anExchangeName);
	}

	public SortedSet<OHLCVData> getDataForTimeframe(CurrencyPair aPair, Date aStartDate, Date anEndDate, long aRollUp) {
		SortedSet<OHLCVData> data = new TreeSet<>();
		Date nextDate = aStartDate;
		SortedSet<Trade> dbData = db.get(aPair);
		if (dbData == null) {
			return data;
		}
		while (nextDate.before(anEndDate)) {
			data.add(new OHLCVData(nextDate, aRollUp, dbData));
			nextDate = new Date(nextDate.getTime() + aRollUp);
			if (data.last().volume == null) {
				data.remove(data.last());
			}
		}
		return data;
	}
	
	

	public void stop() {
		running = false;
	}

	public void addListener(TradeListener aListener) {
		listeners.add(aListener);
	}

	@Override
	protected void finalize() throws Throwable {
		stop();
	}
}
