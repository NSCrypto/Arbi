package com.tsavo.trade;

import java.io.IOException;
import java.util.concurrent.PriorityBlockingQueue;

import com.tsavo.trade.opportunity.Opportunity;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class OpportunityExecutor {

	private PriorityBlockingQueue<Opportunity> opportunityQueue = new PriorityBlockingQueue<>(100);

	public OpportunityExecutor(final PriceIndex anIndex, final Wallet aWallet) {
		final OpportunityExecutor me = this;
		Thread t = new Thread("Opportunity Executor") {
			@Override
			public void run() {
				while (true) {
					Opportunity opp;
					try {
						opp = opportunityQueue.take();
					} catch (InterruptedException e) {
						return;
					}
					if (!opp.canTrade(anIndex, aWallet)) {
						opp.getSuggestions(aWallet).forEach(System.out::println);
						continue;
					}
					try {
						System.out.println("Executing trade: " + opp);
						opp.trade(me);
					} catch (ExchangeException | NotAvailableFromExchangeException | NotYetImplementedForExchangeException | IOException e) {
						e.printStackTrace();
					}

				}
			}
		};
		t.setDaemon(true);
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
	}

	public void executeOpportunity(Opportunity anOpportunity) {
		opportunityQueue.add(anOpportunity);
	}
}
