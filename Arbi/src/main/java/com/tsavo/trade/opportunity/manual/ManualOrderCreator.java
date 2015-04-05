package com.tsavo.trade.opportunity.manual;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import com.tsavo.trade.OpportunityExecutor;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class ManualOrderCreator implements OpportunityFinder {

	Portfolio portfolio;

	public ManualOrderCreator(Portfolio aPortfolio) {
		portfolio = aPortfolio;

	}

	@Override
	public void findOpportunities(OpportunityExecutor anExecutor) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		Thread t = new Thread() {
			@Override
			public void run() {
				DecimalFormat format = new DecimalFormat("#.########");

				while (true) {
					String myOrder = null;
					try {
						myOrder = br.readLine();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String[] parts = myOrder.split(" ");

					if (parts[0].equals("portfolio")) {
						try {
							System.out.println(portfolio.getReport());
						} catch (ExchangeException | NotAvailableFromExchangeException | NotYetImplementedForExchangeException | IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						continue;

					}
				}
			}

		};
		t.setDaemon(true);
		t.start();

	}

}
