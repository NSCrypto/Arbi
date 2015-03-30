package com.tsavo.trade.opportunity.arbitrage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;
import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class ArbitrageOpportunity implements Opportunity {
	Exchange askExchange;
	Exchange bidExchange;
	LimitOrder bid;
	LimitOrder ask;
	float size;
	CurrencyPair currencyPair;
	private static LoadingCache<Exchange, LoadingCache<String, BigDecimal>> balanceCache = CacheBuilder
			.newBuilder().concurrencyLevel(4).weakKeys()
			.expireAfterWrite(20, TimeUnit.MINUTES)
			.build(new CacheLoader<Exchange, LoadingCache<String, BigDecimal>>() {
				@Override
				public LoadingCache<String, BigDecimal> load(Exchange exchange)
						throws Exception {
					return CacheBuilder.newBuilder().concurrencyLevel(4)
							.weakKeys().expireAfterWrite(20, TimeUnit.SECONDS)
							.build(new CacheLoader<String, BigDecimal>() {
								@Override
								public BigDecimal load(String currency)
										throws Exception {
									try {
										BigDecimal amount = exchange
												.getPollingAccountService()
												.getAccountInfo()
												.getBalance(currency);
										if(currency.equals("USD") && amount.floatValue() < 1){
											return BigDecimal.ZERO;
										}
										if(amount.floatValue() < 0.0001){
											return BigDecimal.ZERO;
										}
										return amount;
									} catch (
											ExchangeException
											| NotAvailableFromExchangeException
											| NotYetImplementedForExchangeException
											| IOException e) {
										// TODO Auto-generated
										// catch block
										e.printStackTrace();
										return BigDecimal.ZERO;
									}
								}
							});
				}
			});

	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	public void setCurrencyPair(CurrencyPair currencyPair) {
		this.currencyPair = currencyPair;
	}

	public ArbitrageOpportunity(CurrencyPair aPair, Exchange anAskExchange,
			LimitOrder anAskOrder, Exchange aBidExchange, LimitOrder aBidOrder,
			float aSize) {
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

	public void trade(BigDecimal anAmount, Portfolio aPortfolio)
			throws ExchangeException, NotAvailableFromExchangeException,
			NotYetImplementedForExchangeException, IOException {
		if (getPlaceToBuy().getPollingTradeService() == null) {
			System.out.println("Can't trade at "
					+ getPlaceToBuy().getExchangeSpecification()
							.getExchangeName());
			return;
		}
		if (getPlaceToSell().getPollingTradeService() == null) {
			System.out.println("Can't trade at "
					+ getPlaceToSell().getExchangeSpecification()
							.getExchangeName());
			return;
		}
		String order = getPlaceToBuy().getPollingTradeService().placeLimitOrder(new
		LimitOrder(OrderType.BID, anAmount, getAsk().getCurrencyPair(), null,
		new Date(), getAsk().getLimitPrice()));
		try{getPlaceToSell().getPollingTradeService().placeLimitOrder(new
		LimitOrder(OrderType.ASK, anAmount, getBid().getCurrencyPair(), null,
		new Date(), getBid().getLimitPrice()));}
		catch(Exception e){
			getPlaceToBuy().getPollingTradeService().cancelOrder(order);
			throw e;
		}
		float profit = anAmount.floatValue()
				* (getBid().getLimitPrice().floatValue() - getAsk()
						.getLimitPrice().floatValue());
		DecimalFormat format = new DecimalFormat("#.########");
		//aPortfolio.addEarning(currencyPair, anAmount.floatValue(),
			//	getBid().getLimitPrice().add((getAsk().getLimitPrice()))
					//	.divide(new BigDecimal(2)).floatValue());
		System.out.println("We bought " + format.format(anAmount) + " "
				+ getAsk().getCurrencyPair().baseSymbol + " @ "
				+ getPlaceToBuy().getExchangeSpecification().getExchangeName()
				+ " for " + format.format(getAsk().getLimitPrice())
				+ " each, and sold them for "
				+ format.format(getBid().getLimitPrice()) + " @ "
				+ getPlaceToSell().getExchangeSpecification().getExchangeName()
				+ " and in doing so made " + format.format(profit) + " "
				+ getAsk().getCurrencyPair().counterSymbol + ".");
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
		// voice.speak("We bought " + anAmount + " " +
		// getAsk().getTradableIdentifier() + " at " +
		// getPlaceToBuy().getExchangeSpecification().getExchangeName() +
		// " for " +
		// opp.getAsk().getLimitPrice().toString() + " each, and sold them for "
		// +
		// opp.getBid().getLimitPrice().toString() + " at "
		// + placeToSell.getExchangeSpecification().getExchangeName() +
		// " and in doing so made " + profit + " " +
		// getAsk().getTransactionCurrency() + ".");

	}

	public BigDecimal getAmountToTrade() {
		if (getPlaceToBuy().getPollingAccountService() == null
				|| getPlaceToSell().getPollingAccountService() == null) {
			System.out
					.println("We can't check at "
							+ (getPlaceToBuy().getPollingAccountService() == null ? getPlaceToBuy()
									.getExchangeSpecification()
									.getExchangeName() : getPlaceToSell()
									.getExchangeSpecification()
									.getExchangeName()) + " so skipping.");
			return BigDecimal.ZERO;
		}
		BigDecimal buyingBalence;
		try {
			buyingBalence = balanceCache.get(getPlaceToBuy()).get(
					getAsk().getCurrencyPair().counterSymbol).divide(getBid().getLimitPrice(), 8, RoundingMode.HALF_DOWN);
		} catch (Exception e) {
			System.out.println("Couldn't check balance at "
					+ getPlaceToBuy().getExchangeSpecification()
							.getExchangeName()
					+ " so skipping the opportunity for now.");
			e.printStackTrace();
			return BigDecimal.ZERO;
		}
		BigDecimal sellingBalance;
		try {
			sellingBalance = balanceCache.get(getPlaceToSell()).get(
					getAsk().getCurrencyPair().baseSymbol);
		} catch (Exception e) {
			System.out.println("Couldn't check balance at "
					+ getPlaceToSell().getExchangeSpecification()
							.getExchangeName()
					+ " so skipping the opportunity for now.");
			e.printStackTrace();
			return BigDecimal.ZERO;
		}
		BigDecimal amountToTrade = new BigDecimal(Math.min(Math.min(
				buyingBalence.floatValue(),
				sellingBalance.floatValue()), Math.min(getAsk()
				.getTradableAmount().floatValue(), getBid().getTradableAmount()
				.floatValue())));


		return amountToTrade;
	}

	public Set<String> getSuggestions(List<Exchange> exchanges) {
		Set<String> suggestions = new HashSet<String>();
		BigDecimal buyingBalence = BigDecimal.ZERO;
		try {
			buyingBalence = balanceCache.get(getPlaceToBuy()).get(
					getAsk().getCurrencyPair().counterSymbol);
		} catch (Exception e) {
			System.out.println("Couldn't check balance at "
					+ getPlaceToBuy().getExchangeSpecification()
							.getExchangeName()
					+ " so skipping the opportunity for now.");
			e.printStackTrace();
		}

		if (buyingBalence.floatValue() < .1) {
			float biggestPlace = 0;
			Exchange bestExchangeSpecification = null;
			for (Exchange ex : exchanges) {
				try {

					float bal;
					if ((bal = balanceCache.get(ex)
							.get(getAsk().getCurrencyPair().counterSymbol)
							.floatValue()) > 0.1) {
						biggestPlace = Math.max(biggestPlace, bal);
						if (biggestPlace == bal) {
							bestExchangeSpecification = ex;
						}
					}

				} catch (Exception e) {
				}
			}
			if (bestExchangeSpecification != null && biggestPlace > 0) {
				suggestions.add("You should send some "
						+ getAsk().getCurrencyPair().counterSymbol
						+ " from "
						+ bestExchangeSpecification.getExchangeSpecification()
								.getExchangeName()
						+ " to "
						+ getAskExchange().getExchangeSpecification()
								.getExchangeName() + ".");
			}
		}
		BigDecimal sellingBalance = BigDecimal.ZERO;

		try {
			sellingBalance = balanceCache.get(getPlaceToSell()).get(
					getAsk().getCurrencyPair().baseSymbol);
		} catch (Exception e) {
			System.out.println("Couldn't check balance at "
					+ getPlaceToSell().getExchangeSpecification()
							.getExchangeName()
					+ " so skipping the opportunity for now.");
			e.printStackTrace();
		}
		if (sellingBalance.floatValue() < .1) {
			float biggestPlace = 0;
			Exchange bestExchangeSpecification = null;
			for (Exchange ex : exchanges) {
				try {
					float bal;
					if ((bal = balanceCache.get(ex)
							.get(getAsk().getCurrencyPair().baseSymbol)
							.floatValue()) > 0.1
							) {
						biggestPlace = Math.max(biggestPlace, bal);
						if (biggestPlace == bal) {
							bestExchangeSpecification = ex;
						}

					}

				} catch (Exception e) {

				}
			}
			if (bestExchangeSpecification != null) {
				suggestions.add("You should send some "
						+ getAsk().getCurrencyPair().baseSymbol
						+ " from "
						+ bestExchangeSpecification.getExchangeSpecification()
								.getExchangeName()
						+ " to "
						+ getBidExchange().getExchangeSpecification()
								.getExchangeName() + ".");
			}
		}
		return suggestions;
	}
}