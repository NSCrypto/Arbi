package com.tsavo.trade.opportunity.rsi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import com.tsavo.trade.opportunity.Opportunity;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.tsavo.trade.portfolio.Portfolio;
import com.tsavo.trade.portfolio.TickerData;
import com.tsavo.trade.portfolio.TickerDataPoint;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class ChartingOpportunityFinder extends ApplicationFrame implements OpportunityFinder {

	private static final long serialVersionUID = 9080959986802470382L;
	private static final int DELAY = 30000;
	List<Exchange> exchanges;
	Portfolio portfolio;
	long lastRun = 0;

	public static class Plot {
		int offset = 0;

		public Plot(CurrencyPair aCurrency) {
			priceSeries = new TimeSeries(aCurrency + " Price");
			// TODO Auto-generated constructor stub
		}

		TimeSeries priceSeries;
		XYSeries fastMinusSlowSeries;
		XYSeries macdSeries;

	}

	Map<CurrencyPair, Plot> plotMap = new HashMap<CurrencyPair, Plot>();

	final long startTime = System.currentTimeMillis();

	public ChartingOpportunityFinder(List<Exchange> someExchanges, Portfolio aPortfolio) throws IOException {
		super("Price Chart");
		exchanges = someExchanges;
		portfolio = aPortfolio;

		final JFreeChart chart = createCombinedChart();
		final ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
		panel.setPreferredSize(new java.awt.Dimension(500, 270));
		setContentPane(panel);
		pack();
		RefineryUtilities.centerFrameOnScreen(this);
		setVisible(true);
		// TODO Auto-generated constructor stub
	}

	private JFreeChart createCombinedChart() throws IOException {

		// create subplot 1...
		final CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(new DateAxis("Time"));
		((DateAxis) combinedPlot.getDomainAxis()).setDateFormatOverride(new SimpleDateFormat("M-d@h:mm aaa"));
		// ((DateAxis)
		// combinedPlot.getDomainAxis()).setTimeline(SegmentedTimeline.newMondayThroughFridayTimeline());
		combinedPlot.setGap(10.0);
		combinedPlot.setDomainCrosshairLockedOnData(true);
		combinedPlot.setDomainCrosshairVisible(true);
		combinedPlot.setRangeCrosshairLockedOnData(true);
		combinedPlot.setRangeCrosshairVisible(true);

		final XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
		renderer1.setBaseShapesVisible(true);
		renderer1.setBaseShapesFilled(true);
		renderer1.setDrawSeriesLineAsPath(true);
		// collection.addSeries(plot.fastMinusSlowSeries);
		// collection.addSeries(plot.macdSeries);
		final NumberAxis rangeAxis1 = new NumberAxis("Price");
		final TimeSeriesCollection collection = new TimeSeriesCollection();
		for (Exchange exchange : exchanges) {
			for (CurrencyPair currency : exchange.getPollingMarketDataService().getExchangeSymbols()) {
				if (plotMap.containsKey(currency)) {
					continue;
				}
				Plot plot = new Plot(currency);
				plot.priceSeries.setMaximumItemCount(4500);

				Map<CurrencyPair, TickerData> map = portfolio.getTickers().get(exchange.getExchangeSpecification().getExchangeName());
				int count = 0;
				if (map != null) {
					TickerData data = map.get(currency);
					if (data != null) {
						data.cleanUp();
						for (TickerDataPoint item : data.getData()) {
							if (item.getPrice() < 0.003f && item.getPrice() > 0.00004f) {
								plot.priceSeries.addOrUpdate(new Minute(new Date(item.getTimestamp())), item.getPrice());
								count++;
							}
						}
					}
				}
				if (count > 0) {
					collection.addSeries(plot.priceSeries);
					plotMap.put(currency, plot);
				}
			}
		}
		final XYPlot subplot1 = new XYPlot(collection, null, rangeAxis1, renderer1);

		final long ONE_DAY = 24 * 60 * 60 * 1000;
		XYLineAndShapeRenderer maRenderer = new XYLineAndShapeRenderer(true, false);
		XYDataset maSataset = MovingAverage.createMovingAverage(collection, "MA", 30 * ONE_DAY, 0);
		subplot1.setRenderer(1, maRenderer);
		subplot1.setDataset(1, maSataset);

		subplot1.setRangeAxisLocation(AxisLocation.TOP_OR_RIGHT);
		combinedPlot.add(subplot1);

		// add the subplots...
		combinedPlot.setOrientation(PlotOrientation.VERTICAL);

		// return a new chart containing the overlaid plot...
		return new JFreeChart("Trade bot chart", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);

	}

	public List<Opportunity> findOpportunities() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
		if (lastRun + DELAY > System.currentTimeMillis()) {
			return new ArrayList<Opportunity>();
		}
		lastRun = System.currentTimeMillis();
		List<Opportunity> opps = new ArrayList<Opportunity>();
		for (Exchange exchange : exchanges) {

			for (CurrencyPair currency : exchange.getPollingMarketDataService().getExchangeSymbols()) {
				
				Ticker ticker = exchange.getPollingMarketDataService().getTicker(currency);
				if (ticker == null) {
					continue;
				}
				Date current = ticker.getTimestamp();
				SortedMap<CurrencyPair, TickerData> currencyTickers = portfolio.tickers.get(exchange.getExchangeSpecification().getExchangeName());
				if (currencyTickers == null) {
					currencyTickers = Collections.synchronizedSortedMap(new TreeMap<CurrencyPair, TickerData>());
					portfolio.tickers.put(exchange.getExchangeSpecification().getExchangeName(), currencyTickers);
				}
				TickerData data = currencyTickers.get(currency);
				if (data == null) {
					data = new TickerData();
					currencyTickers.put(currency, data);
				}
				if (data.getData().isEmpty() || current.getTime() > data.getData().last().getTimestamp()) {

					if (ticker.getLast().floatValue() < 0.003f && ticker.getLast().floatValue() > 0.00004f) {
						Plot plot = plotMap.get(currency);
						// plot.fastMinusSlowSeries.add((System.currentTimeMillis() -
						// startTime) / DELAY, fastEma.getAverage() - slowEma.getAverage());
						// plot.macdSeries.add((System.currentTimeMillis() - startTime) /
						// DELAY, macd.getAverage());
						if (plot != null) {
							plot.priceSeries.addOrUpdate(new Minute(ticker.getTimestamp()), ticker.getLast());
						}
					}

					data.addSample(ticker.getLast().floatValue(), ticker.getTimestamp().getTime(), ticker.getVolume().floatValue());
				}// DecimalFormat format = new DecimalFormat("#.##########");
					// System.out.println(exchange.getExchangeSpecification().getExchangeName()
					// + " " + currency + ": " + format.format(macd.getAverage()) + " " +
					// format.format(fastEma.getAverage() - slowEma.getAverage()));
			}
		}
		portfolio.save();
		return opps;
	};

}
