package com.tsavo.trade.portfolio;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthResultHandler;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.tsavo.trade.PriceIndex;
import com.tsavo.trade.Wallet;
import com.tsavo.trade.portfolio.Position.Disposition;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class Portfolio {

	public static Firebase db = new Firebase("https://t3-portfolio.firebaseio.com/Portfolio");
	public transient PriceIndex priceIndex;
	public transient Wallet wallet;

	public static class PositionListener implements ValueEventListener {
		Semaphore semaphore = new Semaphore(0);
		List<Position> positions = new ArrayList<>();

		@Override
		public void onDataChange(DataSnapshot arg0) {
			for (DataSnapshot data : arg0.getChildren()) {
				positions.add(new Position(data.getKey(), data.child("Disposition").getValue().equals(Disposition.LONG.toString()) ? Disposition.LONG : Disposition.SHORT,
						new CurrencyPair(data.child("CurrencyPair").getValue().toString()), (float) data.child("EntryPosition").getValue(), (float) data.child("ExitPosition")
								.getValue(), (float) data.child("StopLoss").getValue(), (float) data.child("Size").getValue(), data.child("ExchangeName").getValue().toString(),
						(boolean) data.child("Armed").getValue(), (long) data.child("EntryTimestamp").getValue(), (long) data.child("ExitTimestamp").getValue()));
			}
			semaphore.release(Integer.MAX_VALUE);
		}

		@Override
		public void onCancelled(FirebaseError arg0) {
			System.out.println(arg0);
			semaphore.release(Integer.MAX_VALUE);
		}

		public List<Position> getPositions() {
			try {
				semaphore.acquire();
				semaphore.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return new ArrayList<Position>();
			}
			return positions;
		}
	}

	public static String persistPosition(Firebase aDb, Position aPosition) {
		Firebase position = aDb.push();
		position.child("ExchangeName").setValue(aPosition.exchangeName);
		position.child("CurrencyPair").setValue(aPosition.currencyPair.toString());
		position.child("Disposition").setValue(aPosition.disposition);
		position.child("EntryPosition").setValue(aPosition.entryPosition);
		position.child("ExitPosition").setValue(aPosition.exitPosition);
		position.child("Size").setValue(aPosition.size);
		position.child("StopLoss").setValue(aPosition.stopLoss);
		position.child("Armed").setValue(aPosition.armed);
		position.child("EntryTimestamp").setValue(aPosition.entryTimestamp);
		position.child("ExitTimestamp").setValue(aPosition.exitTimestamp);
		return position.getKey();
	}

	static {
		db.authWithPassword("evilgenius@nefariousplan.com", "Zabbas4242!", new AuthResultHandler() {

			@Override
			public void onAuthenticationError(FirebaseError arg0) {
				throw new RuntimeException(arg0.toException());
			}

			@Override
			public void onAuthenticated(AuthData arg0) {
				System.out.println(arg0);
			}
		});
	}

	public List<Position> getOpenPositions() {
		PositionListener listener = new PositionListener();
		db.child("Positions").child("Open").addListenerForSingleValueEvent(listener);
		return listener.getPositions();
	}

	public List<Position> getClosedPositions() {
		PositionListener listener = new PositionListener();
		db.child("Positions").child("Closed").addListenerForSingleValueEvent(listener);
		return listener.getPositions();
	}

	public void removeOpenPosition(Position aPosition) {
		db.child("Positions").child("Open").child(aPosition.id).removeValue();
	}

	public String addOpenPosition(Position aPosition) {
		return persistPosition(db.child("Positions").child("Open"), aPosition);
	}

	public String addClosedPosition(Position aPosition) {
		return persistPosition(db.child("Positions").child("Closed"), aPosition);
	}

	public Portfolio() {
	}

	public Portfolio(PriceIndex anIndex, Wallet aWallet) {
		priceIndex = anIndex;
		wallet = aWallet;
	}

	@JsonIgnore
	public String getReport() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		StringBuffer buffer = new StringBuffer();
		DecimalFormat format = new DecimalFormat("###,###.########");
		Map<String, BigDecimal> totals = new HashMap<>();
		
		Firebase walletDb = db.child("Wallet").child(new Date().getTime() + "");
		
		for (Exchange exchange : wallet.exchanges) {
			buffer.append(exchange.getExchangeSpecification().getExchangeName() + ":\n");
			AccountInfo info = exchange.getPollingAccountService().getAccountInfo();
			Firebase exchangeDb = walletDb.child("ExchangeWallets").child(exchange.getExchangeSpecification().getExchangeName());
			for (com.xeiam.xchange.dto.trade.Wallet wallet : info.getWallets()) {
				if (wallet.getBalance().floatValue() < .001) {
					continue;
				}
				exchangeDb.child(wallet.getCurrency()).setValue(wallet.getBalance().setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros().floatValue());
				buffer.append(wallet.getBalance().setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros() + " " + wallet.getCurrency() + "\n");
				if (!totals.containsKey(wallet.getCurrency())) {
					totals.put(wallet.getCurrency(), wallet.getBalance());
				} else {
					totals.put(wallet.getCurrency(), totals.get(wallet.getCurrency()).add(wallet.getBalance()).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros());
				}
			}
		}
		buffer.append("----------------\n");
		buffer.append("Totals:\n");
		Firebase totalsDb = walletDb.child("Totals");
		for (String k : totals.keySet()) {
			buffer.append("Total " + k + ": " + format.format(totals.get(k)) + "\n");
			totalsDb.child(k).setValue(totals.get(k));
		}
		buffer.append("----------------\n");
		buffer.append("Liquidated totals:\n");
		Firebase liquidatedTotalsDb = walletDb.child("LiquidatedTotals");
		BigDecimal usd = BigDecimal.ZERO;
		for (String k : totals.keySet()) {
			if (k.equals("USD")) {
				usd = usd.add(totals.get(k));
			} else {
				usd = usd.add(totals.get(k).multiply(priceIndex.getLowestSellPrice(new CurrencyPair(k, "USD")).limitOrder.getLimitPrice()));
			}
		}
		buffer.append("USD: " + format.format(usd.setScale(2, RoundingMode.HALF_DOWN).stripTrailingZeros()) + "\n");
		liquidatedTotalsDb.child("USD").setValue(usd.setScale(2, RoundingMode.HALF_DOWN).stripTrailingZeros().floatValue());
		
		BigDecimal btc = BigDecimal.ZERO;
		for (String k : totals.keySet()) {
			if (k.equals("BTC")) {
				btc = btc.add(totals.get(k));
			} else if (k.equals("USD")) {
				btc = btc.add(totals.get(k).divide(priceIndex.getLowestSellPrice(new CurrencyPair("BTC", k)).limitOrder.getLimitPrice(), RoundingMode.HALF_DOWN));
			} else {
				btc = btc.add(totals.get(k).multiply(priceIndex.getHighestBuyPrice(new CurrencyPair(k, "BTC")).limitOrder.getLimitPrice()));
			}
		}
		buffer.append("BTC: " + format.format(btc.setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros()) + "\n");
		liquidatedTotalsDb.child("BTC").setValue(btc.setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros().floatValue());

		return buffer.toString();

	}

	public void enterPosition(Position aPosition) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		aPosition.enter(priceIndex, wallet);
		addOpenPosition(aPosition);
	}

	public void exitCompleteOpenPositions() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		getOpenPositions().stream().filter(x -> x.isAtExit(priceIndex)).forEach(x -> {
			try {
				x.exit(priceIndex, wallet);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			removeOpenPosition(x);
			addClosedPosition(x);
		});

	}

	public void clearCache() {
		wallet.clearCache();
		priceIndex.clearCache();
	}
}
