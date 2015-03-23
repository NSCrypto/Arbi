package com.tsavo.trade.opportunity;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.tsavo.trade.portfolio.Portfolio;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public interface Opportunity {

	public float getSize();

	public BigDecimal getAmountToTrade() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException;

	public void trade(BigDecimal anAmmount, Portfolio aPortfolio) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException;

	public Set<String> getSuggestions(List<Exchange> exchanges) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException;

}
