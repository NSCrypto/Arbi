package com.tsavo.trade.portfolio;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import com.tsavo.trade.ExchangeLimitOrder;
import com.tsavo.trade.PriceIndex;
import com.tsavo.trade.Wallet;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class Position {

	public enum Disposition {
		LONG, SHORT
	};

	public CurrencyPair currencyPair;
	public float entryPosition;
	public float exitPosition;
	public float stopLoss;
	public float size;
	public boolean armed = false;
	public String exchangeName;
	public float actualExitPosition;
	public Disposition disposition;

	public Position(Disposition aDisposition, CurrencyPair currencyPair, float entryPosition, float exitPosition, float stopLoss, float aSize, String anExchangeName) {
		super();
		disposition = aDisposition;
		this.currencyPair = currencyPair;
		this.entryPosition = entryPosition;
		this.exitPosition = exitPosition;
		this.stopLoss = stopLoss;
		this.size = aSize;
		this.exchangeName = anExchangeName;
	}

	public void enter(PriceIndex anIndex, Wallet aWallet) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		BigDecimal remainingAmount = new BigDecimal(size);
		while (remainingAmount.floatValue() > 0) {
			ExchangeLimitOrder order = disposition == Disposition.LONG ? anIndex.getLowestSellPrice(exchangeName, currencyPair) : anIndex.getHighestBuyPrice(exchangeName,
					currencyPair);
			BigDecimal tradeableAmount = order.limitOrder.getTradableAmount().min(remainingAmount).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
			String currencyNeeded = disposition == Disposition.LONG ? order.limitOrder.getCurrencyPair().counterSymbol : order.limitOrder.getCurrencyPair().baseSymbol;
			BigDecimal balance = aWallet.getBalance(order.exchange, currencyNeeded);
			if (balance.floatValue() < (disposition == Disposition.LONG ? tradeableAmount.divide(order.limitOrder.getLimitPrice(), 8, RoundingMode.HALF_DOWN).floatValue()
					: tradeableAmount.floatValue())) {
				setArmed(false);
				System.out.println("We would entering a " + disposition + " position of " + size + " " + order.limitOrder.getCurrencyPair() + " @ "
						+ order.limitOrder.getLimitPrice() + " but we only have " + balance + " " + currencyNeeded + " @ "
						+ order.exchange.getExchangeSpecification().getExchangeName() + ".");
				return;
			}
			remainingAmount = remainingAmount.subtract(tradeableAmount).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
			LimitOrder buyOrder = new LimitOrder(disposition == Disposition.LONG ? OrderType.BID : OrderType.ASK, tradeableAmount, currencyPair, "", new Date(), order.limitOrder
					.getLimitPrice().setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros());
			System.out.println("Entering " + disposition + " position of " + size + " " + order.limitOrder.getCurrencyPair() + " @ " + order.limitOrder.getLimitPrice() + " ("
					+ order.exchange.getExchangeSpecification().getExchangeName() + ")");
			entryPosition = order.limitOrder.getLimitPrice().floatValue();
			if (isArmed()) {
				order.exchange.getPollingTradeService().placeLimitOrder(buyOrder);
				anIndex.clearCache();
				aWallet.clearCache();
			}else{
				System.out.println("...but the order isn't armed so the trade won't actually go through.");
			}
		}
	}

	public void exit(PriceIndex anIndex, Wallet aWallet) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		BigDecimal remainingAmount = new BigDecimal(size);
		while (remainingAmount.floatValue() > 0) {
			ExchangeLimitOrder order = disposition == Disposition.LONG ? anIndex.getHighestBuyPrice(exchangeName, currencyPair) : anIndex.getLowestSellPrice(exchangeName,
					currencyPair);
			BigDecimal tradeableAmount = order.limitOrder.getTradableAmount().min(remainingAmount).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
			String currencyNeeded = disposition == Disposition.LONG ? order.limitOrder.getCurrencyPair().baseSymbol : order.limitOrder.getCurrencyPair().counterSymbol;
			BigDecimal balance = aWallet.getBalance(order.exchange, currencyNeeded);
			if (balance.floatValue() < (disposition == Disposition.LONG ? tradeableAmount.floatValue() : tradeableAmount.divide(order.limitOrder.getLimitPrice(), 8,
					RoundingMode.HALF_DOWN).floatValue())) {
				setArmed(false);
				System.out.println("We would entering a " + disposition + " position of " + size + " " + order.limitOrder.getCurrencyPair() + " @ "
						+ order.limitOrder.getLimitPrice() + " but we only have " + balance + " " + currencyNeeded + " @ "
						+ order.exchange.getExchangeSpecification().getExchangeName() + ".");
				return;
			}
			remainingAmount = remainingAmount.subtract(tradeableAmount).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
			LimitOrder buyOrder = new LimitOrder(disposition == Disposition.LONG ? OrderType.ASK : OrderType.BID, tradeableAmount, currencyPair, "", new Date(), order.limitOrder
					.getLimitPrice().setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros());
			System.out.println("Entering " + disposition + " position of " + size + " " + order.limitOrder.getCurrencyPair() + " @ " + order.limitOrder.getLimitPrice() + " ("
					+ order.exchange.getExchangeSpecification().getExchangeName() + ")");
			actualExitPosition = order.limitOrder.getLimitPrice().floatValue();
			if (isArmed()) {
				order.exchange.getPollingTradeService().placeLimitOrder(buyOrder);
				anIndex.clearCache();
				aWallet.clearCache();
			}else{
				System.out.println("...but the order isn't armed so the trade won't actually go through.");
			}
		}
	}

	public boolean isAtExit(PriceIndex anIndex) {

		return disposition == Disposition.LONG ? (anIndex.getHighestBuyPrice(exchangeName, currencyPair).limitOrder.getLimitPrice().floatValue() >= exitPosition || anIndex
				.getHighestBuyPrice(exchangeName, currencyPair).limitOrder.getLimitPrice().floatValue() <= stopLoss)
				: (anIndex.getHighestBuyPrice(exchangeName, currencyPair).limitOrder.getLimitPrice().floatValue() <= exitPosition || anIndex.getHighestBuyPrice(exchangeName,
						currencyPair).limitOrder.getLimitPrice().floatValue() >= stopLoss);
	}

	public boolean isArmed() {
		return armed;
	}

	public void setArmed(boolean armed) {
		this.armed = armed;
	}
}
