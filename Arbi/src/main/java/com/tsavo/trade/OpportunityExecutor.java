package com.tsavo.trade;

import java.io.IOException;
import java.util.concurrent.PriorityBlockingQueue;

import javax.speech.AudioException;
import javax.speech.EngineException;

import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class OpportunityExecutor {

	private PriorityBlockingQueue<Opportunity> opportunityQueue = new PriorityBlockingQueue<>(100);

	public OpportunityExecutor(final Portfolio aPortfolio) {
		final OpportunityExecutor me = this;
		Thread t = new Thread("Opportunity Executor") {
			@Override
			public void run() {
				SpeechSynthesizer speech = new SpeechSynthesizer();
				while (true) {
					Opportunity opp;
					try {
						opp = opportunityQueue.take();
					} catch (InterruptedException e) {
						return;
					}
					if (!opp.canTrade(aPortfolio)) {
						System.out.println("Skipping opportunity: " + opp);
						opp.getSuggestions(aPortfolio).forEach(System.out::println);
						continue;
					}
					try {
						System.out.println("Executing trade: " + opp);
						opp.trade(me);
						speech.speak("Executing trade: " + opp);
						aPortfolio.clearCache();

					} catch (ExchangeException | NotAvailableFromExchangeException | NotYetImplementedForExchangeException | IOException e) {
						e.printStackTrace();
					} catch (EngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (AudioException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
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
