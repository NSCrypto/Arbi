package com.tsavo.trade.portfolio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsavo.trade.PriceIndex;
import com.tsavo.trade.Wallet;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class Portfolio {


	public SortedMap<CurrencyPair, EarningsReport> earningsReport = Collections.synchronizedSortedMap(new TreeMap<CurrencyPair, EarningsReport>());
	public SortedMap<String, SortedMap<CurrencyPair, TickerData>> tickers = Collections.synchronizedSortedMap(new TreeMap<String, SortedMap<CurrencyPair, TickerData>>());

	public List<ExchangeSpecification> exchangeSpecifications = new ArrayList<>();

	public ExchangeSpecification referenceExchangeSpecification;
	// public Map<String, MovingAverage<WeightedSample>> tradedValue = new
	// HashMap<>();
	public double buyMultiplier = 1;
	public double sellMultiplier = 1.0;

	public PriceIndex priceIndex;
	public Wallet wallet;

	public Portfolio(PriceIndex anIndex, Wallet aWallet) {
		priceIndex = anIndex;
		wallet = aWallet;
	}

	@JsonIgnore
	public void save() {
		try {
			// use buffering
			File dest = new File("portfolio.json");
			if (dest.exists()) {
				dest.delete();
			}
			FileOutputStream file = new FileOutputStream(dest, false);
			OutputStream buffer = new BufferedOutputStream(file);
			try {
				ObjectMapper mapper = new ObjectMapper();
				mapper.writerWithDefaultPrettyPrinter().writeValue(buffer, this);
			} finally {
				buffer.flush();
				file.flush();
				file.close();
				// file.getFD().sync();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@JsonIgnore
	public String getReport() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		StringBuffer buffer = new StringBuffer();
		DecimalFormat format = new DecimalFormat("#.########");
		Map<String, BigDecimal> totals = new HashMap<>();
		for (Exchange exchange : wallet.exchanges) {
			buffer.append(exchange.getExchangeSpecification().getExchangeName() + ":\n");
			AccountInfo info = exchange.getPollingAccountService().getAccountInfo();
			for (com.xeiam.xchange.dto.trade.Wallet wallet : info.getWallets()) {
				if (wallet.getBalance().floatValue() < .001) {
					continue;
				}
				buffer.append(wallet.getBalance().setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros() + " " + wallet.getCurrency() + "\n");
				if (!totals.containsKey(wallet.getCurrency())) {
					totals.put(wallet.getCurrency(), wallet.getBalance());
				} else {
					totals.put(wallet.getCurrency(), totals.get(wallet.getCurrency()).add(wallet.getBalance()).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros());
				}
			}
		}
		buffer.append("----------------\n");
		for (String k : totals.keySet()) {
			buffer.append("Total " + k + ": " + format.format(totals.get(k)) + "\n");
		}
		return buffer.toString();

	}

	
	public SortedMap<String, SortedMap<CurrencyPair, TickerData>> getTickers() {
		return tickers;
	}

	public void setTickers(SortedMap<String, SortedMap<CurrencyPair, TickerData>> tickers) {
		this.tickers = tickers;
	}

	public List<ExchangeSpecification> getExchangeSpecifications() {
		return exchangeSpecifications;
	}

	public void setExchangeSpecifications(List<ExchangeSpecification> exchangeSpecifications) {
		this.exchangeSpecifications = exchangeSpecifications;
	}
}
