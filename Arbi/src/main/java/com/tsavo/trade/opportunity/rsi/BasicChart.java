package com.tsavo.trade.opportunity.rsi;

import java.text.SimpleDateFormat;
import java.util.Date;

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
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import com.tsavo.trade.portfolio.TickerData;
import com.tsavo.trade.portfolio.TickerDataPoint;
import com.tsavo.trade.portfolio.TickerListener;

public class BasicChart extends ApplicationFrame implements TickerListener {

	private static final long serialVersionUID = 5888182374281592510L;
	TimeSeries priceSeries;

	public BasicChart(TickerData aTicker, String aName) {
		super(aName + " Price Chart");

		final JFreeChart chart = createCombinedChart(aTicker, aName);
		final ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
		panel.setPreferredSize(new java.awt.Dimension(500, 270));
		setContentPane(panel);
		pack();
		RefineryUtilities.centerFrameOnScreen(this);
		setVisible(true);
		aTicker.addListener(this);
	}

	private JFreeChart createCombinedChart(TickerData aTicker, String aName) {

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
		priceSeries = new TimeSeries(aName + " Price");

		final TimeSeriesCollection collection = new TimeSeriesCollection();

		priceSeries.setMaximumItemCount(4500);

		for (TickerDataPoint item : aTicker.getData()) {
			priceSeries.addOrUpdate(new Minute(new Date(item.getTimestamp())), item.getPrice());
		}

		collection.addSeries(priceSeries);

		final XYPlot subplot1 = new XYPlot(collection, null, rangeAxis1, renderer1);

		final long ONE_DAY = 24 * 60 * 60 * 1000;
		final long ONE_HOUR = 60 * 60 * 1000;
		final long ONE_MINUTE = 60 * 1000;
		XYLineAndShapeRenderer maRenderer = new XYLineAndShapeRenderer(true, false);
		XYDataset maSataset = MovingAverage.createMovingAverage(collection, " MA", ONE_MINUTE*5, 0);
		subplot1.setRenderer(1, maRenderer);
		subplot1.setDataset(1, maSataset);

		subplot1.setRangeAxisLocation(AxisLocation.TOP_OR_RIGHT);
		combinedPlot.add(subplot1);

		// add the subplots...
		combinedPlot.setOrientation(PlotOrientation.VERTICAL);

		// return a new chart containing the overlaid plot...
		return new JFreeChart(aName + " Price Chart", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);
	}

	@Override
	public void sampleAdded(float aSample, long aTimeStamp, float aVolume) {
		priceSeries.addOrUpdate(new Minute(new Date(aTimeStamp)), aSample);
	}

}
