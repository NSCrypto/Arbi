package com.tsavo.trade.db;

import java.io.File;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Trade;

public class TickerDatabase {

	static DB db = DBMaker.newFileDB(new File("d:\\traderbot\\db.mapdb")).readOnly().closeOnJvmShutdown().cacheLRUEnable().compressionEnable().make();
	static volatile boolean running = true;
	ConcurrentNavigableMap<CurrencyPair, SortedSet<Trade>> map;
	public String exchangeName;

	public TickerDatabase(String anExchangeName) {
		exchangeName = anExchangeName;
		map = db.getTreeMap(anExchangeName);
	}

	public SortedSet<Trade> get(CurrencyPair aPair) {
		if (!map.containsKey(aPair)) {
			return new TreeSet<>();
		}
		return new TreeSet<>(map.get(aPair));

	}

	public static void close() {
		running = false;
		db.close();
	}

}
