package com.tsavo.arbi;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.encog.engine.network.activation.ActivationStep;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.CalculateScore;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.market.MarketDataDescription;
import org.encog.ml.data.market.MarketDataType;
import org.encog.ml.data.market.MarketMLDataSet;
import org.encog.ml.data.market.loader.MarketLoader;
import org.encog.ml.ea.train.basic.TrainEA;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.neat.NEATUtil;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.TrainingSetScore;
import org.encog.neural.networks.training.propagation.Propagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.neural.pattern.FeedForwardPattern;
import org.encog.util.Format;
import org.encog.util.simple.EncogUtility;
import org.encog.util.time.TimeUnit;
import org.junit.Test;

import com.tsavo.hippo.LiveTickerReader;
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

		LiveTickerReader ticker = new LiveTickerReader("BitFinex");
		MarketLoader loader = new ExchangeLoader(ticker, 1000 * 60 * 25);
		MarketMLDataSet market = new MarketMLDataSet(loader, Config.INPUT_WINDOW, Config.PREDICT_WINDOW);
		MarketDataDescription desc = new MarketDataDescription(Config.TICKER, MarketDataType.ADJUSTED_CLOSE, true, true);
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

		final FeedForwardPattern pattern = new FeedForwardPattern();
		pattern.setInputNeurons(market.getInputSize());
		pattern.setOutputNeurons(market.getIdealSize());
		pattern.setActivationFunction(new ActivationTANH());
		// pattern.setActivationFunction(new ActivationSigmoid());
		pattern.addHiddenLayer(100);
		pattern.addHiddenLayer(25);

		BasicNetwork network = (BasicNetwork) pattern.generate();
		network.reset();

		NEATPopulation pop = new NEATPopulation(market.getInputSize(), market.getIdealSize(), 250);
		pop.setNEATActivationFunction(new ActivationTANH());
		pop.setInitialConnectionDensity(1.0);
		pop.reset();
		CalculateScore score = new TrainingSetScore(market);

		
		TrainEA t = NEATUtil.constructNEATTrainer(pop, score);
		EncogUtility.trainToError(t, 0.0000001);
		NEATPopulation n = (NEATPopulation) t.getMethod();
		

		long start = System.currentTimeMillis();
		

		Propagation train = new ResilientPropagation(network, market);
		train.setThreadCount(0);

//		do {
//			train.iteration();
//
//			final long current = System.currentTimeMillis();
//			final long elapsed = (current - start) / 1000;// seconds
//
//			int iteration = train.getIteration();
//
//			System.out.println("Iteration #" + Format.formatInteger(iteration) + " Error:" + Format.formatPercent(train.getError()) + " elapsed time = "
//					+ Format.formatTimeSpan((int) elapsed));
//
//		} while (train.getError() > 0.0000001);
//
//		train.finishTraining();
		DecimalFormat format = new DecimalFormat("#0.0000");

		int count = 0;
		int correct = 0;
		for (MLDataPair pair : market) {
			MLData input = pair.getInput();
			MLData actualData = pair.getIdeal();
			MLData predictData = n.compute(input);

			double actual = actualData.getData(0);
			double predict = predictData.getData(0);
			double diff = Math.abs(predict - actual);

			Direction actualDirection = determineDirection(actual);
			Direction predictDirection = determineDirection(predict);

			if (actualDirection == predictDirection)
				correct++;

			count++;

			System.out.println("Period: " + count + ": actual=" + format.format(actual) + "(" + actualDirection + ")" + ", predict=" + format.format(predict) + "("
					+ predictDirection + ")" + ", diff=" + format.format(diff));

		}
		double percent = (double) correct / (double) count;
		System.out.println("Direction correct:" + correct + "/" + count);
		System.out.println("Directional Accuracy:" + format.format(percent * 100) + "%");

	}

}
