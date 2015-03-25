package com.tsavo.trade;

import java.util.Optional;
import java.util.stream.Collectors;

import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicOrder;
import com.xeiam.xchange.cryptsy.dto.marketdata.CryptsyPublicOrderbook;

public class CachingCryptsyOrderBook {

	public Lazy<Optional<CryptsyPublicOrder>> lowestSell;
	public Lazy<Optional<CryptsyPublicOrder>> highestBuy;
	
	public CachingCryptsyOrderBook(CryptsyPublicOrderbook orderBook, float limit) {
		lowestSell = new Lazy<Optional<CryptsyPublicOrder>>(() -> {
			return  orderBook
				.getSellOrders()
				.stream()
				.filter(x -> x.getTotal().floatValue() > limit)
				.collect(
						Collectors
								.<CryptsyPublicOrder> minBy((x, y) -> x
										.getPrice().compareTo(
												y.getPrice())));
		});
		highestBuy = new Lazy<Optional<CryptsyPublicOrder>>(() -> {
			return  orderBook
					.getBuyOrders()
					.stream()
					.filter(x -> x.getTotal().floatValue() > limit)
					.collect(
							Collectors
									.<CryptsyPublicOrder> maxBy((x, y) -> x
											.getPrice().compareTo(
													y.getPrice())));
		});
	}
}
