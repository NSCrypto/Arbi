package com.tsavo.trade.portfolio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.trade.Wallet;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class Portfolio {

	public SortedMap<CurrencyPair, HoldingsReport> holdingsReport = Collections.synchronizedSortedMap(new TreeMap<CurrencyPair, HoldingsReport>());
	// public Map<CurrencyPair, HoldingsReport> holdingsReport = new HashMap<>();
	public SortedMap<CurrencyPair, EarningsReport> earningsReport = Collections.synchronizedSortedMap(new TreeMap<CurrencyPair, EarningsReport>());
	public SortedMap<String, SortedMap<CurrencyPair, TickerData>> tickers = Collections.synchronizedSortedMap(new TreeMap<String, SortedMap<CurrencyPair, TickerData>>());

	public List<ExchangeSpecification> exchangeSpecifications = new ArrayList<>();

	public ExchangeSpecification referenceExchangeSpecification;
	// public Map<String, MovingAverage<WeightedSample>> tradedValue = new
	// HashMap<>();
	public double buyMultiplier = 1;
	public double sellMultiplier = 1.0;

	public Portfolio() {
	}

	@JsonIgnore
	public Map<String, Wallet> getTotalHoldings(List<Exchange> someExchanges) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		Map<String, Wallet> wallets = new HashMap<String, Wallet>();
		for (Exchange exchange : someExchanges) {
			AccountInfo info = exchange.getPollingAccountService().getAccountInfo();
			for (Wallet wallet : info.getWallets()) {
				if (wallet.getBalance().floatValue() < .001) {
					continue;
				}
				if (wallets.get(wallet.getCurrency()) == null) {
					wallets.put(wallet.getCurrency(), wallet);
				} else {
					wallets.put(wallet.getCurrency(), new Wallet(wallet.getCurrency(), wallets.get(wallet.getCurrency()).getBalance().add(wallet.getBalance())));
				}
			}
		}
		return wallets;
	}

	public void addEarning(CurrencyPair aCurrencyPair, float anAmount, float aValue) {
		EarningsReport report = earningsReport.get(aCurrencyPair);
		if (report == null) {
			report = new EarningsReport();
			earningsReport.put(aCurrencyPair, report);
		}
		HoldingsReport holdings = holdingsReport.get(aCurrencyPair);
		if (holdings == null) {
			holdings = new HoldingsReport(aCurrencyPair);
			holdingsReport.put(aCurrencyPair, holdings);
		}
		float profit = (aValue - holdings.remove(anAmount)) * anAmount;
		report.getEarnings().add(profit);
		save();
	}

	@JsonIgnore
	public void addSample(CurrencyPair aPair, float aPrice, float anAmount) {
		HoldingsReport average = holdingsReport.get(aPair);
		if (average == null) {
			average = new HoldingsReport(aPair);
			holdingsReport.put(aPair, average);
		}
		average.add(anAmount, aPrice);
		save();
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
	public String getReport() {
		StringBuffer buffer = new StringBuffer();
		DecimalFormat format = new DecimalFormat("#.########");
		Map<String, Float> totals = new HashMap<>();
		for (CurrencyPair pair : getCurrencyPairsInPortfolio()) {
			float average = getAverageValue(pair);
			float low = average;
			if (average == 0) {
				TickerData data = tickers.get("Cryptsy").get(pair);
				if (data != null) {
					average = data.getAverage();
					low = (average + tickers.get("Cryptsy").get(pair).getLow()) / 2;
				}
			}
			HoldingsReport holding = holdingsReport.get(pair);
			float held = 0;
			if (holding != null) {
				held = holding.getTotalAmount();
			}
			buffer.append(pair + " -> " + held + " +" + format.format(getTotalEarnings(pair)) + " " + pair.counterSymbol + " (" + format.format(average) + " average value, buy @ " + format.format(low * buyMultiplier) + ", sell @ " + format.format(low * sellMultiplier) + ")\n");
			Float f = totals.get(pair.counterSymbol);
			if (f == null) {
				f = 0f;
			}
			totals.put(pair.counterSymbol, f + getTotalEarnings(pair));
		}
		for (String k : totals.keySet()) {
			buffer.append("Total " + k + ": " + format.format(totals.get(k)) + "\n");
		}
		return buffer.toString();

	}

	@JsonIgnore
	public Set<CurrencyPair> getCurrencyPairsInPortfolio() {
		return holdingsReport.keySet();
	}

	@JsonIgnore
	public float getAverageValue(CurrencyPair aUnit) {
		HoldingsReport average = holdingsReport.get(aUnit);
		if (average == null) {
			return 0;
		}
		return average.getAverageValue();
	}

	public SortedMap<CurrencyPair, EarningsReport> getEarningsReport() {
		return earningsReport;
	}

	public void setEarningsReport(SortedMap<CurrencyPair, EarningsReport> earningsReport) {
		this.earningsReport = earningsReport;
	}

	public float getTotalEarnings(CurrencyPair pair) {
		EarningsReport report = earningsReport.get(pair);
		if (report == null) {
			return 0;
		}
		return report.getTotalEarnings();
	}

	public double getBuyMultiplier() {
		return buyMultiplier;
	}

	public void setBuyMultiplier(double buyMultiplier) {
		this.buyMultiplier = buyMultiplier;
	}

	public double getSellMultiplier() {
		return sellMultiplier;
	}

	public void setSellMultiplier(double sellMultiplier) {
		this.sellMultiplier = sellMultiplier;
	}

	public Map<CurrencyPair, HoldingsReport> getHoldingsReport() {
		return holdingsReport;
	}

	public void setHoldingsReport(SortedMap<CurrencyPair, HoldingsReport> holdingsReport) {
		this.holdingsReport = holdingsReport;
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

	public ExchangeSpecification getReferenceExchangeSpecification() {
		return referenceExchangeSpecification;
	}

	public void setReferenceExchangeSpecification(ExchangeSpecification referenceExchangeSpecification) {
		this.referenceExchangeSpecification = referenceExchangeSpecification;
	}

}
