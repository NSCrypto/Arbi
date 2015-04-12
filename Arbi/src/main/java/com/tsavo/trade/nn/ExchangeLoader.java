package com.tsavo.trade.nn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.SortedSet;

import org.encog.ml.data.market.MarketDataType;
import org.encog.ml.data.market.TickerSymbol;
import org.encog.ml.data.market.loader.LoadedMarketData;
import org.encog.ml.data.market.loader.MarketLoader;

import com.tsavo.hippo.OHLCVData;
import com.tsavo.trade.LiveTickerReader;
import com.xeiam.xchange.currency.CurrencyPair;

public class ExchangeLoader implements MarketLoader {

	private int rollupPeriod;
	private LiveTickerReader ticker;

	public ExchangeLoader(LiveTickerReader aTicker, int aRollupPeriod) {
		rollupPeriod = aRollupPeriod;
		ticker = aTicker;
	}

	@Override
	public Collection<LoadedMarketData> load(TickerSymbol pair, Set<MarketDataType> dataNeeded, Date from, Date to) {
		final Collection<LoadedMarketData> result = new ArrayList<LoadedMarketData>();
		SortedSet<OHLCVData> data = ticker.getDataForTimeframe(new CurrencyPair(pair.getSymbol()), from, to, rollupPeriod);
		for (OHLCVData row : data) {
			final LoadedMarketData mlData = new LoadedMarketData(row.startDate, pair);
			mlData.setData(MarketDataType.ADJUSTED_CLOSE, row.close.doubleValue());
			mlData.setData(MarketDataType.OPEN, row.open.doubleValue());
			mlData.setData(MarketDataType.CLOSE, row.close.doubleValue());
			mlData.setData(MarketDataType.HIGH, row.high.doubleValue());
			mlData.setData(MarketDataType.LOW, row.low.doubleValue());
			mlData.setData(MarketDataType.VOLUME, row.volume.doubleValue());
			result.add(mlData);
		}
		return result;
	}
}
