package com.tsavo.trade.opportunity.rsi;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

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
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.tsavo.hippo.ExponentialWeightedMovingAverageFunction;
import com.tsavo.hippo.LiveTickerReader;
import com.tsavo.hippo.OHLCVData;
import com.tsavo.hippo.OHLCVDataSet;
import com.tsavo.trade.OpportunityExecutor;
import com.tsavo.trade.opportunity.OpportunityFinder;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;

public class ChartingOpportunityFinder extends ApplicationFrame implements OpportunityFinder {

	private static final long serialVersionUID = 9080959986802470382L;

	long lastRun = 0;
	public CurrencyPair pair;

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

	public ChartingOpportunityFinder(LiveTickerReader aTicker, CurrencyPair aPair) throws IOException {
		super("Price Chart");
		pair = aPair;

		final JFreeChart chart = createCombinedChart(aTicker);
		final ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
		panel.setPreferredSize(new java.awt.Dimension(500, 270));
		setContentPane(panel);
		pack();
		RefineryUtilities.centerFrameOnScreen(this);
		setVisible(true);
	}

	private JFreeChart createCombinedChart(LiveTickerReader aTicker) throws IOException {

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
		rangeAxis1.setAutoRange(true);
		rangeAxis1.setAutoRangeIncludesZero(false);
		rangeAxis1.setAutoRangeStickyZero(false);
		final TimeSeriesCollection collection = new TimeSeriesCollection();
		Plot plot = new Plot(pair);
		plot.priceSeries.setMaximumItemCount(4500);
		DateTime now = new DateTime();
		DateTime start = now.minusDays(5);

		OHLCVDataSet data = new OHLCVDataSet(aTicker.getDataForTimeframe(pair), new Duration(360000));
		for (OHLCVData item : data.difference().average(() -> new ExponentialWeightedMovingAverageFunction(3))) {
			plot.priceSeries.addOrUpdate(new Minute(item.startDate.toDate()),
					item.open.add(item.close).divide(new BigDecimal(2), 8, RoundingMode.HALF_DOWN).setScale(8, RoundingMode.HALF_DOWN).stripTrailingZeros());
		}
		collection.addSeries(plot.priceSeries);
		final XYPlot subplot1 = new XYPlot(collection, null, rangeAxis1, renderer1);

		final long ONE_DAY = 12 * 60 * 60 * 1000;
		XYLineAndShapeRenderer maRenderer = new XYLineAndShapeRenderer(true, false);
		XYDataset maSataset = MovingAverage.createMovingAverage(collection, "MA", ONE_DAY, 0);
		subplot1.setRenderer(1, maRenderer);
		subplot1.setDataset(1, maSataset);

		subplot1.setRangeAxisLocation(AxisLocation.TOP_OR_RIGHT);
		combinedPlot.add(subplot1);

		// add the subplots...
		combinedPlot.setOrientation(PlotOrientation.VERTICAL);

		// return a new chart containing the overlaid plot...
		return new JFreeChart("Trade bot chart", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);

	}

	public void findOpportunities(OpportunityExecutor anExecutor) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

		// TickerData data = currencyTickers.get(currency);
		// if (data == null) {
		// data = new TickerData();
		// currencyTickers.put(currency, data);
		// }
		// if (data.getData().isEmpty() || current.getTime() >
		// data.getData().last().getTimestamp()) {
		//
		// if (ticker.getLast().floatValue() < 0.003f &&
		// ticker.getLast().floatValue() > 0.00004f) {
		// Plot plot = plotMap.get(currency);
		// // plot.fastMinusSlowSeries.add((System.currentTimeMillis()
		// // -
		// // startTime) / DELAY, fastEma.getAverage() -
		// // slowEma.getAverage());
		// // plot.macdSeries.add((System.currentTimeMillis() -
		// // startTime) /
		// // DELAY, macd.getAverage());
		// if (plot != null) {
		// plot.priceSeries.addOrUpdate(new Minute(ticker.getTimestamp()),
		// ticker.getLast());
		// }
		// }
		//
		// data.addSample(ticker.getLast().floatValue(),
		// ticker.getTimestamp().getTime(), ticker.getVolume().floatValue());
		// }// DecimalFormat format = new DecimalFormat("#.##########");
		// //
		// System.out.println(exchange.getExchangeSpecification().getExchangeName()
		// // + " " + currency + ": " +
		// // format.format(macd.getAverage()) + " " +
		// // format.format(fastEma.getAverage() -
		// // slowEma.getAverage()));
		return;
	}

}
