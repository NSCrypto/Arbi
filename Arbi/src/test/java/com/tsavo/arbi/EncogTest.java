package com.tsavo.arbi;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.market.MarketDataDescription;
import org.encog.ml.data.market.MarketDataType;
import org.encog.ml.data.market.MarketMLDataSet;
import org.encog.ml.data.market.loader.MarketLoader;
import org.encog.neural.networks.BasicNetwork;
import org.encog.util.simple.EncogUtility;
import org.encog.util.time.TimeUnit;
import org.junit.Test;

import com.tsavo.trade.LiveTickerReader;
import com.tsavo.trade.nn.Config;
import com.tsavo.trade.nn.ExchangeLoader;

public class EncogTest {

	enum Direction {
		up, down
	};

	public static Direction determineDirection(double d) {
		if (d < 0)
			return Direction.down;
		else
			return Direction.up;
	}

	@Test
	public void test() {

		LiveTickerReader ticker = new LiveTickerReader("BTC-e");
		final MarketLoader loader = new ExchangeLoader(ticker, 1000 * 60 * 10);
		final MarketMLDataSet market = new MarketMLDataSet(loader, Config.INPUT_WINDOW, Config.PREDICT_WINDOW);
		final MarketDataDescription desc = new MarketDataDescription(Config.TICKER, MarketDataType.ADJUSTED_CLOSE, true, true);
		market.addDescription(desc);
		market.setSequenceGrandularity(TimeUnit.MINUTES);

		Calendar end = new GregorianCalendar();// end today
		Calendar begin = (Calendar) end.clone();// begin 30 days ago

		begin.add(Calendar.DATE, -3);
		begin.set(Calendar.HOUR, 0);
		begin.set(Calendar.MINUTE, 0);
		begin.set(Calendar.SECOND, 0);

		end.add(Calendar.DATE, 1);

		market.load(begin.getTime(), end.getTime());
		market.generate();

		final BasicNetwork network = EncogUtility.simpleFeedForward(market.getInputSize(), Config.HIDDEN1_COUNT, Config.HIDDEN2_COUNT, market.getIdealSize(), true);
		EncogUtility.trainConsole(network, market, Config.TRAINING_MINUTES);

		DecimalFormat format = new DecimalFormat("#0.0000");

		int count = 0;
		int correct = 0;
		for (MLDataPair pair : market) {
			MLData input = pair.getInput();
			MLData actualData = pair.getIdeal();
			MLData predictData = network.compute(input);

			double actual = actualData.getData(0);
			double predict = predictData.getData(0);
			double diff = Math.abs(predict - actual);

			Direction actualDirection = determineDirection(actual);
			Direction predictDirection = determineDirection(predict);

			if (actualDirection == predictDirection)
				correct++;

			count++;

			System.out.println("Period: " + count + ":actual=" + format.format(actual) + "(" + actualDirection + ")" + ",predict=" + format.format(predict) + "("
					+ predictDirection + ")" + ",diff=" + format.format(diff));

		}
		double percent = (double) correct / (double) count;
		System.out.println("Direction correct:" + correct + "/" + count);
		System.out.println("Directional Accuracy:" + format.format(percent * 100) + "%");

	}

}
