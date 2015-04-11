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

	static DB db = DBMaker.newFileDB(new File("d:\\traderbot\\db.mapdb")).closeOnJvmShutdown().cacheLRUEnable().asyncWriteEnable().compressionEnable().make();
	static volatile boolean running = true;
	ConcurrentNavigableMap<CurrencyPair, SortedSet<Trade>> map;
	public String exchangeName;

	public TickerDatabase(String anExchangeName) {
		exchangeName = anExchangeName;
		map = db.getTreeMap(anExchangeName);
	}

	static {
		Thread cleaner = new Thread("Database compactor") {
			@Override
			public void run() {
				while (running) {
					try {
						Thread.sleep(1000 * 60);
					} catch (InterruptedException e) {
						return;
					}
					db.compact();
				}
			}
		};
		cleaner.setDaemon(true);
		cleaner.start();
	}

	public void put(CurrencyPair aPair, Trade anOrder) {
		SortedSet<Trade> orders = map.get(aPair);
		if (orders == null) {
			orders = new TreeSet<Trade>();
		}
		orders.add(anOrder);
		map.put(aPair, orders);
		db.commit();
	}

	public SortedSet<Trade> get(CurrencyPair aPair) {
		return map.get(aPair);
	}

	public void putAll(SortedSet<Trade> aList) {
		aList.forEach(trade -> {
			SortedSet<Trade> orders = map.get(trade.getCurrencyPair());
			if (orders == null) {
				orders = new TreeSet<Trade>();
			}
			orders.add(trade);
			map.put(trade.getCurrencyPair(), orders);
		});
		if (aList.size() > 0) {
			db.commit();
		}
	}

	public static void close() {
		running = false;
		db.close();
	}

}
