package com.tsavo.trade.opportunity.cycle;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.tsavo.trade.ExchangeLimitOrder;
import com.tsavo.trade.OpportunityExecutor;
import com.tsavo.trade.PriceIndex;
import com.tsavo.trade.Wallet;
import com.tsavo.trade.opportunity.Opportunity;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class CycleOpportunity implements Opportunity {

	public CurrencyCycle cycle;

	public CycleOpportunity(CurrencyCycle cycle) {
		this.cycle = cycle;
	}

	@Override
	public boolean canTrade(PriceIndex aPriceIndex, Wallet aWallet) {
		String resultingCurrency = null;
		BigDecimal tradeableAmount = null;
		for (ExchangeLimitOrder order : cycle.GetExchangeLimitOrders()) {
			if (order.limitOrder.getType().equals(OrderType.ASK)) {
				if(tradeableAmount == null){
					BigDecimal amount;
					switch(order.limitOrder.getCurrencyPair().baseSymbol){
						case "BTC": amount = new BigDecimal(0.02); break;
						case "LTC": amount = new BigDecimal(5); break;
						case "DRK": amount = new BigDecimal(5); break;
						default : amount = new BigDecimal(5);
					}
					tradeableAmount = order.limitOrder.getTradableAmount().min(amount).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
				}
				
				if(resultingCurrency == null){
					resultingCurrency = order.limitOrder.getCurrencyPair().baseSymbol;
				}
				
				if(resultingCurrency.equals(order.limitOrder.getCurrencyPair().counterSymbol)){
					tradeableAmount = tradeableAmount.divide(order.limitOrder.getLimitPrice(), 8, RoundingMode.HALF_DOWN).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
				}
				BigDecimal sellableAmount = tradeableAmount.multiply(order.limitOrder.getLimitPrice()).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
				
				
				
				resultingCurrency = order.limitOrder.getCurrencyPair().baseSymbol;
				if (aWallet.getBalance(order.exchange, order.limitOrder.getCurrencyPair().counterSymbol).floatValue() < sellableAmount.floatValue()) {
					System.out.println("Opportunity skipped because of lack of liquidity on " + order.exchange.getExchangeSpecification().getExchangeName() + ". We have "
							+ aWallet.getBalance(order.exchange, order.limitOrder.getCurrencyPair().counterSymbol) + " " + order.limitOrder.getCurrencyPair().counterSymbol
							+ " but we need " + sellableAmount + " to complete the trade.");
					return false;
				}
				if (order.limitOrder.getTradableAmount().floatValue() < tradeableAmount.floatValue()) {
					System.out.println("Opportunity skipped because volume insufficent.");
					return false;
				}
			} else {
				if(tradeableAmount == null){
					BigDecimal amount;
					switch(order.limitOrder.getCurrencyPair().baseSymbol){
						case "BTC": amount = new BigDecimal(0.02); break;
						case "LTC": amount = new BigDecimal(5); break;
						case "DRK": amount = new BigDecimal(5); break;
						default : amount = new BigDecimal(5);
					}
					tradeableAmount = order.limitOrder.getTradableAmount().min(amount).setScale(8, RoundingMode.HALF_DOWN);
				}
				
				if(resultingCurrency == null){
					resultingCurrency = order.limitOrder.getCurrencyPair().baseSymbol;
				}
				
				if(resultingCurrency.equals(order.limitOrder.getCurrencyPair().counterSymbol)){
					tradeableAmount = tradeableAmount.multiply(order.limitOrder.getLimitPrice()).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
				}
				
				resultingCurrency = order.limitOrder.getCurrencyPair().counterSymbol;
			
				if (order.limitOrder.getTradableAmount().floatValue() < tradeableAmount.floatValue()) {
					System.out.println("Opportunity skipped because volume insufficent.");
					return false;
				}
				BigDecimal balance = aWallet.getBalance(order.exchange, order.limitOrder.getCurrencyPair().baseSymbol);
				if (balance.floatValue() < tradeableAmount.floatValue()) {
					System.out.println("Opportunity skipped because of lack of liquidity on " + order.exchange.getExchangeSpecification().getExchangeName() + ". We have "
							+ balance + " " + order.limitOrder.getCurrencyPair().baseSymbol
							+ " but we need " + tradeableAmount + " to complete the trade.");
					return false;
				}
				tradeableAmount = tradeableAmount.multiply(order.limitOrder.getLimitPrice()).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
			}
			
		}
		return true;
	}

	@Override
	public float getSize() {
		return cycle.getSize();
	}

	@Override
	public String toString() {
		return cycle.toString() + " " + (cycle.balance.floatValue() - 1f) * 100 + "%.";
	}

	@Override
	public void trade(OpportunityExecutor anExecutor) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

		String resultingCurrency = null;
		BigDecimal tradeableAmount = null;
		
		for (ExchangeLimitOrder order : cycle.GetExchangeLimitOrders()) {

			if (order.limitOrder.getType().equals(OrderType.ASK)) {

				if(tradeableAmount == null){
					BigDecimal amount;
					switch(order.limitOrder.getCurrencyPair().baseSymbol){
						case "BTC": amount = new BigDecimal(0.02); break;
						case "LTC": amount = new BigDecimal(5); break;
						case "DRK": amount = new BigDecimal(5); break;
						default : amount = new BigDecimal(5);
					}
					tradeableAmount = order.limitOrder.getTradableAmount().min(amount).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
				}
				
				if(resultingCurrency == null){
					resultingCurrency = order.limitOrder.getCurrencyPair().baseSymbol;
				}
				
				if(resultingCurrency.equals(order.limitOrder.getCurrencyPair().counterSymbol)){
					tradeableAmount = tradeableAmount.divide(order.limitOrder.getLimitPrice(), 8, RoundingMode.HALF_DOWN).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
				}
				resultingCurrency = order.limitOrder.getCurrencyPair().baseSymbol;
				
				
				System.out.println("Buying " + tradeableAmount + " " + order.limitOrder.getCurrencyPair().baseSymbol + " on "
						+ order.exchange.getExchangeSpecification().getExchangeName() + " @ " + order.limitOrder.getLimitPrice() + " "
						+ order.limitOrder.getCurrencyPair().counterSymbol + " ("
						+ order.limitOrder.getLimitPrice().multiply(tradeableAmount).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros() + " "
						+ order.limitOrder.getCurrencyPair().counterSymbol + ") leaving us with " + tradeableAmount + " " + resultingCurrency + ".");
				LimitOrder outorder = new LimitOrder(OrderType.BID, tradeableAmount, order.limitOrder.getCurrencyPair(), null, new Date(), order.limitOrder.getLimitPrice());
				String id =	order.exchange.getPollingTradeService().placeLimitOrder(outorder);
			} else {
				if(tradeableAmount == null){
					BigDecimal amount;
					switch(order.limitOrder.getCurrencyPair().baseSymbol){
						case "BTC": amount = new BigDecimal(0.02); break;
						case "LTC": amount = new BigDecimal(5); break;
						case "DRK": amount = new BigDecimal(5); break;
						default : amount = new BigDecimal(5);
					}
					tradeableAmount = order.limitOrder.getTradableAmount().min(amount).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
				}
				
				if(resultingCurrency == null){
					resultingCurrency = order.limitOrder.getCurrencyPair().baseSymbol;
				}
				
				if(resultingCurrency.equals(order.limitOrder.getCurrencyPair().counterSymbol)){
					tradeableAmount = tradeableAmount.multiply(order.limitOrder.getLimitPrice()).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();
				}
				
				resultingCurrency = order.limitOrder.getCurrencyPair().counterSymbol;
				System.out.println("Selling " + tradeableAmount + " " + order.limitOrder.getCurrencyPair().baseSymbol + " on "
						+ order.exchange.getExchangeSpecification().getExchangeName() + " @ " + order.limitOrder.getLimitPrice() + " "
						+ order.limitOrder.getCurrencyPair().counterSymbol + " ("
						+ order.limitOrder.getLimitPrice().multiply(tradeableAmount).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros() + " "
						+ order.limitOrder.getCurrencyPair().counterSymbol + ") leaving us with " +  tradeableAmount.multiply(order.limitOrder.getLimitPrice()).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros() + " " + resultingCurrency + ".");
				LimitOrder outorder = new LimitOrder(OrderType.ASK, tradeableAmount, order.limitOrder.getCurrencyPair(), null, new Date(), order.limitOrder.getLimitPrice());
				String id =	order.exchange.getPollingTradeService().placeLimitOrder(outorder);
				tradeableAmount = tradeableAmount.multiply(order.limitOrder.getLimitPrice()).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros();

			}
		}

	}

	@Override
	public Set<String> getSuggestions(Wallet aWallet) {
		Set<String> suggestions = new HashSet<>();

		for (ExchangeLimitOrder order : cycle.GetExchangeLimitOrders()) {
			BigDecimal amount;
			try {
				amount = order.exchange.getPollingAccountService().getAccountInfo()
						.getBalance(order.limitOrder.getType() == OrderType.ASK ? order.limitOrder.getCurrencyPair().counterSymbol : order.limitOrder.getCurrencyPair().baseSymbol);
			} catch (ExchangeException | NotAvailableFromExchangeException | NotYetImplementedForExchangeException | IOException e1) {
				continue;
			}
			if (amount.floatValue() < 0.1) {
				float biggestPlace = 0;
				Exchange bestExchangeSpecification = null;
				for (Exchange ex : aWallet.exchanges) {

					float bal = aWallet.getBalance(ex,
							order.limitOrder.getType() == OrderType.ASK ? order.limitOrder.getCurrencyPair().counterSymbol : order.limitOrder.getCurrencyPair().baseSymbol)
							.floatValue();
					biggestPlace = Math.max(biggestPlace, bal);
					if (biggestPlace == bal) {
						bestExchangeSpecification = ex;
					}

				}
				if (bestExchangeSpecification != null && biggestPlace > 0) {
					suggestions.add("You should send some "
							+ (order.limitOrder.getType() == OrderType.ASK ? order.limitOrder.getCurrencyPair().counterSymbol : order.limitOrder.getCurrencyPair().baseSymbol)
							+ " from " + bestExchangeSpecification.getExchangeSpecification().getExchangeName() + " to "
							+ order.exchange.getExchangeSpecification().getExchangeName() + ".");
				}
			}
		}
		return suggestions;
	}

	@Override
	public int compareTo(Opportunity o) {
		return (int) (getSize() - o.getSize()) * 1000000000;
	}
}
