package com.tsavo.trade;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.trade.LimitOrder;

public class ExchangeLimitOrder {

	public Exchange exchange;
	public LimitOrder limitOrder;

	public ExchangeLimitOrder(Exchange anExchange, LimitOrder aLimitOrder) {
		exchange = anExchange;
		limitOrder = aLimitOrder;
	}
}
