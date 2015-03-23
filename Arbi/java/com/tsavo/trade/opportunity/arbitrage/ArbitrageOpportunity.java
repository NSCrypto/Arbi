package com.tsavo.trade.opportunity.arbitrage;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;

public class ArbitrageOpportunity implements Opportunity {
	Exchange askExchange;
	Exchange bidExchange;
	LimitOrder bid;
	LimitOrder ask;
	float size;
	CurrencyPair currencyPair;

	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	public void setCurrencyPair(CurrencyPair currencyPair) {
		this.currencyPair = currencyPair;
	}

	public ArbitrageOpportunity(CurrencyPair aPair, Exchange anAskExchange, LimitOrder anAskOrder, Exchange aBidExchange, LimitOrder aBidOrder, float aSize) {
		currencyPair = aPair;
		askExchange = anAskExchange;
		bidExchange = aBidExchange;
		ask = anAskOrder;
		bid = aBidOrder;
		size = aSize;
	}

	public Exchange getPlaceToBuy() {
		return askExchange;
	}

	public Exchange getPlaceToSell() {
		return bidExchange;
	}

	public Exchange getAskExchange() {
		return askExchange;
	}

	public void setAskExchange(Exchange askExchange) {
		this.askExchange = askExchange;
	}

	public Exchange getBidExchange() {
		return bidExchange;
	}

	public void setBidExchange(Exchange bidExchange) {
		this.bidExchange = bidExchange;
	}

	public LimitOrder getBid() {
		return bid;
	}

	public void setBid(LimitOrder bid) {
		this.bid = bid;
	}

	public LimitOrder getAsk() {
		return ask;
	}

	public void setAsk(LimitOrder ask) {
		this.ask = ask;
	}

	public float getSize() {
		return size;
	}

	public void setSize(float size) {
		this.size = size;
	}

	public void trade(BigDecimal anAmount, Portfolio aPortfolio) {
		if (getPlaceToBuy().getPollingTradeService() == null) {
			System.out.println("Can't trade at " + getPlaceToBuy().getExchangeSpecification().getExchangeName());
			return;
		}
		if (getPlaceToSell().getPollingTradeService() == null) {
			System.out.println("Can't trade at " + getPlaceToSell().getExchangeSpecification().getExchangeName());
			return;
		}
		getPlaceToBuy().getPollingTradeService().placeLimitOrder(new LimitOrder(OrderType.BID, anAmount, getAsk().getTradableIdentifier(), getAsk().getTransactionCurrency(), getAsk().getLimitPrice()));
		getPlaceToSell().getPollingTradeService().placeLimitOrder(new LimitOrder(OrderType.ASK, anAmount, getBid().getTradableIdentifier(), getBid().getTransactionCurrency(), getBid().getLimitPrice()));
		float profit = anAmount.floatValue() * (getBid().getLimitPrice().getAmount().floatValue() - getAsk().getLimitPrice().getAmount().floatValue());
		DecimalFormat format = new DecimalFormat("#.########");
		aPortfolio.addEarning(currencyPair, anAmount.floatValue(), getBid().getLimitPrice().getAmount().add((getAsk().getLimitPrice().getAmount())).divide(new BigDecimal(2)).floatValue());
		System.out.println("We bought " + anAmount.floatValue() + " " + getAsk().getTradableIdentifier() + " @ " + getPlaceToBuy().getExchangeSpecification().getExchangeName() + " for " + getAsk().getLimitPrice().toString() + " each, and sold them for " + getBid().getLimitPrice().toString() + " @ "
				+ getPlaceToSell().getExchangeSpecification().getExchangeName() + " and in doing so made " + format.format(profit) + " " + getAsk().getTransactionCurrency() + ".");
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
		// voice.speak("We bought " + anAmount + " " +
		// getAsk().getTradableIdentifier() + " at " +
		// getPlaceToBuy().getExchangeSpecification().getExchangeName() + " for " +
		// opp.getAsk().getLimitPrice().toString() + " each, and sold them for " +
		// opp.getBid().getLimitPrice().toString() + " at "
		// + placeToSell.getExchangeSpecification().getExchangeName() +
		// " and in doing so made " + profit + " " +
		// getAsk().getTransactionCurrency() + ".");

	}

	public BigDecimal getAmountToTrade() {
		if (getPlaceToBuy().getPollingAccountService() == null || getPlaceToSell().getPollingAccountService() == null) {
			System.out.println("We can't check at " + (getPlaceToBuy().getPollingAccountService() == null ? getPlaceToBuy().getExchangeSpecification().getExchangeName() : getPlaceToSell().getExchangeSpecification().getExchangeName()) + " so skipping.");
			return BigDecimal.ZERO;
		}
		BigMoney buyingBalence;
		try {
			buyingBalence = getPlaceToBuy().getPollingAccountService().getAccountInfo().getBalance(CurrencyUnit.of(getAsk().getTransactionCurrency()));
		} catch (Exception e) {
			System.out.println("Couldn't check balance at " + getPlaceToBuy().getExchangeSpecification().getExchangeName() + " so skipping the opportunity for now.");
			e.printStackTrace();
			return BigDecimal.ZERO;
		}
		BigMoney sellingBalance;
		try {
			sellingBalance = getPlaceToSell().getPollingAccountService().getAccountInfo().getBalance(CurrencyUnit.of(getAsk().getTradableIdentifier()));
		} catch (Exception e) {
			System.out.println("Couldn't check balance at " + getPlaceToSell().getExchangeSpecification().getExchangeName() + " so skipping the opportunity for now.");
			e.printStackTrace();
			return BigDecimal.ZERO;
		}
		BigDecimal amountToTrade = new BigDecimal(Math.min(Math.min(buyingBalence.getAmount().floatValue() / getAsk().getLimitPrice().getAmount().floatValue(), sellingBalance.getAmount().floatValue()), Math.min(getAsk().getTradableAmount().floatValue(), getBid().getTradableAmount().floatValue())));

		amountToTrade = new BigDecimal(Math.min(100, (int) amountToTrade.floatValue()));
		return amountToTrade;
	}

	public Set<String> getSuggestions(List<Exchange> exchanges) {
		Set<String> suggestions = new HashSet<String>();
		BigMoney buyingBalence = BigMoney.of(CurrencyUnit.of(getAsk().getTransactionCurrency()), BigDecimal.ZERO);
		try {
			buyingBalence = getPlaceToBuy().getPollingAccountService().getAccountInfo().getBalance(CurrencyUnit.of(getAsk().getTransactionCurrency()));
		} catch (Exception e) {
			System.out.println("Couldn't check balance at " + getPlaceToBuy().getExchangeSpecification().getExchangeName() + " so skipping the opportunity for now.");
			e.printStackTrace();
		}

		if (buyingBalence.getAmount().floatValue() < .1 && !getCurrencyPair().counterCurrency.equals("USD")) {
			float biggestPlace = 0;
			Exchange bestExchangeSpecification = null;
			for (Exchange ex : exchanges) {
				try {

					if (ex.isSupportedCurrencyPair(getCurrencyPair())) {
						float bal;
						if ((bal = ex.getPollingAccountService().getAccountInfo().getBalance(CurrencyUnit.of(getAsk().getTransactionCurrency())).getAmount().floatValue()) > 0.1) {
							biggestPlace = Math.max(biggestPlace, bal);
							if (biggestPlace == bal) {
								bestExchangeSpecification = ex;
							}
						}
					}
				} catch (Exception e) {
				}
			}
			if (bestExchangeSpecification != null && biggestPlace > 0) {
				suggestions.add("You should send some " + getAsk().getTransactionCurrency() + " from " + bestExchangeSpecification.getExchangeSpecification().getExchangeName() + " to " + getAskExchange().getExchangeSpecification().getExchangeName() + ".");
			}
		}
		BigMoney sellingBalance = BigMoney.of(CurrencyUnit.of(getAsk().getTradableIdentifier()), BigDecimal.ZERO);

		try {
			sellingBalance = getPlaceToSell().getPollingAccountService().getAccountInfo().getBalance(CurrencyUnit.of(getAsk().getTradableIdentifier()));
		} catch (Exception e) {
			System.out.println("Couldn't check balance at " + getPlaceToSell().getExchangeSpecification().getExchangeName() + " so skipping the opportunity for now.");
			e.printStackTrace();
		}
		if (sellingBalance.getAmount().floatValue() < 1) {
			float biggestPlace = 0;
			Exchange bestExchangeSpecification = null;
			for (Exchange ex : exchanges) {
				try {
					if (ex.isSupportedCurrencyPair(getCurrencyPair())) {
						float bal;
						if ((bal = ex.getPollingAccountService().getAccountInfo().getBalance(CurrencyUnit.of(getAsk().getTradableIdentifier())).getAmount().floatValue()) > 0.1 && bal >= 1) {
							biggestPlace = Math.max(biggestPlace, bal);
							if (biggestPlace == bal) {
								bestExchangeSpecification = ex;
							}

						}
					}
				} catch (Exception e) {

				}
			}
			if (bestExchangeSpecification != null && biggestPlace > 4) {
				suggestions.add("You should send some " + getAsk().getTradableIdentifier() + " from " + bestExchangeSpecification.getExchangeSpecification().getExchangeName() + " to " + getBidExchange().getExchangeSpecification().getExchangeName() + ".");
			}
		}
		return suggestions;
	}
}