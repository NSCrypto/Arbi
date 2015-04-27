package com.tsavo.arbi;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.CalculateScore;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.market.MarketDataDescription;
import org.encog.ml.data.market.MarketDataType;
import org.encog.ml.data.market.MarketMLDataSet;
import org.encog.ml.data.market.loader.MarketLoader;
import org.encog.ml.ea.opp.CompoundOperator;
import org.encog.ml.ea.opp.selection.TruncationSelection;
import org.encog.ml.ea.train.basic.TrainEA;
import org.encog.neural.hyperneat.HyperNEATCODEC;
import org.encog.neural.neat.NEATCODEC;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.neat.training.opp.NEATCrossover;
import org.encog.neural.neat.training.opp.NEATMutateAddLink;
import org.encog.neural.neat.training.opp.NEATMutateAddNode;
import org.encog.neural.neat.training.opp.NEATMutateRemoveLink;
import org.encog.neural.neat.training.opp.NEATMutateWeights;
import org.encog.neural.neat.training.opp.links.MutatePerturbLinkWeight;
import org.encog.neural.neat.training.opp.links.MutateResetLinkWeight;
import org.encog.neural.neat.training.opp.links.SelectFixed;
import org.encog.neural.neat.training.opp.links.SelectProportion;
import org.encog.neural.neat.training.species.OriginalNEATSpeciation;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.TrainingSetScore;
import org.encog.neural.pattern.FeedForwardPattern;
import org.encog.util.simple.EncogUtility;
import org.encog.util.time.TimeUnit;
import org.junit.Test;

import com.tsavo.hippo.TickerDatabase;
import com.tsavo.trade.nn.Config;

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

		TickerDatabase ticker = new TickerDatabase("BitFinex");
		MarketLoader loader = null;//new ExchangeLoader(ticker, 1000 * 60 * 60);
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

		NEATPopulation pop = new NEATPopulation(market.getInputSize(), market.getIdealSize(), 100);
		pop.setNEATActivationFunction(new ActivationTANH());
		pop.setInitialConnectionDensity(1.0);
		pop.reset();
		CalculateScore score = new TrainingSetScore(market);

		
		final TrainEA result = new TrainEA(pop, score);
		result.setSpeciation(new OriginalNEATSpeciation());

		result.setSelection(new TruncationSelection(result, 0.3));
		final CompoundOperator weightMutation = new CompoundOperator();
		weightMutation.getComponents().add(
				0.1125,
				new NEATMutateWeights(new SelectFixed(1),
						new MutatePerturbLinkWeight(0.02)));
		weightMutation.getComponents().add(
				0.1125,
				new NEATMutateWeights(new SelectFixed(2),
						new MutatePerturbLinkWeight(0.02)));
		weightMutation.getComponents().add(
				0.1125,
				new NEATMutateWeights(new SelectFixed(3),
						new MutatePerturbLinkWeight(0.02)));
		weightMutation.getComponents().add(
				0.1125,
				new NEATMutateWeights(new SelectProportion(0.02),
						new MutatePerturbLinkWeight(0.02)));
		weightMutation.getComponents().add(
				0.1125,
				new NEATMutateWeights(new SelectFixed(1),
						new MutatePerturbLinkWeight(1)));
		weightMutation.getComponents().add(
				0.1125,
				new NEATMutateWeights(new SelectFixed(2),
						new MutatePerturbLinkWeight(1)));
		weightMutation.getComponents().add(
				0.1125,
				new NEATMutateWeights(new SelectFixed(3),
						new MutatePerturbLinkWeight(1)));
		weightMutation.getComponents().add(
				0.1125,
				new NEATMutateWeights(new SelectProportion(0.02),
						new MutatePerturbLinkWeight(1)));
		weightMutation.getComponents().add(
				0.03,
				new NEATMutateWeights(new SelectFixed(1),
						new MutateResetLinkWeight()));
		weightMutation.getComponents().add(
				0.03,
				new NEATMutateWeights(new SelectFixed(2),
						new MutateResetLinkWeight()));
		weightMutation.getComponents().add(
				0.03,
				new NEATMutateWeights(new SelectFixed(3),
						new MutateResetLinkWeight()));
		weightMutation.getComponents().add(
				0.01,
				new NEATMutateWeights(new SelectProportion(0.02),
						new MutateResetLinkWeight()));
		weightMutation.getComponents().finalizeStructure();

		result.setChampMutation(weightMutation);
		result.addOperation(0.5, new NEATCrossover());
		result.addOperation(0.494, weightMutation);
		result.addOperation(0.05, new NEATMutateAddNode());
		result.addOperation(0.05, new NEATMutateAddLink());
		result.addOperation(0.05, new NEATMutateRemoveLink());
		result.getOperators().finalizeStructure();

		if (pop.isHyperNEAT()) {
			result.setCODEC(new HyperNEATCODEC());
		} else {
			result.setCODEC(new NEATCODEC());
		}
		EncogUtility.trainToError(result, 0.0000122);
		NEATPopulation n = (NEATPopulation) result.getMethod();
		

		long start = System.currentTimeMillis();
		

		
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
