package com.tsavo.arbi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.math3.util.Pair;
import org.joda.time.Duration;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.CandidateFactory;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;
import org.uncommons.watchmaker.framework.islands.IslandEvolution;
import org.uncommons.watchmaker.framework.islands.IslandEvolutionObserver;
import org.uncommons.watchmaker.framework.islands.RingMigration;
import org.uncommons.watchmaker.framework.operators.AbstractCrossover;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.operators.IdentityOperator;
import org.uncommons.watchmaker.framework.operators.SplitEvolution;
import org.uncommons.watchmaker.framework.selection.RankSelection;
import org.uncommons.watchmaker.framework.selection.RouletteWheelSelection;
import org.uncommons.watchmaker.framework.termination.TargetFitness;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tsavo.hippo.ExponentialWeightedMovingAverageFunction;
import com.tsavo.hippo.OHLCVDataSet;
import com.tsavo.hippo.TickerDatabase;
import com.tsavo.trade.AbstractSignal.SignalTestResults;
import com.tsavo.trade.ThresholdSignal;
import com.tsavo.trade.signal.BigDecimalOptimizableParameter;
import com.tsavo.trade.signal.IntegerOptimizableParameter;
import com.tsavo.trade.signal.LongOptimizableParameter;
import com.tsavo.trade.signal.OptimizableParameterSet;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Trade;

public class EvolutionTest {

	public static class SignalEvaluator implements FitnessEvaluator<SignalParameter> {

		public SortedSet<Trade> rawData;

		public SignalEvaluator(SortedSet<Trade> rawData) {
			super();
			this.rawData = rawData;
		}

		LoadingCache<org.apache.commons.math3.util.Pair<Long, Integer>, OHLCVDataSet> averageCache = CacheBuilder.newBuilder().maximumSize(1000000).concurrencyLevel(8)
				.expireAfterWrite(10000, TimeUnit.HOURS).build(new CacheLoader<Pair<Long, Integer>, OHLCVDataSet>() {
					@Override
					public OHLCVDataSet load(Pair<Long, Integer> key) throws Exception {
						// TODO Auto-generated method stub
						return dataCache.get(key.getKey()).difference().average(() -> new ExponentialWeightedMovingAverageFunction(key.getValue()));
					}
				});
		LoadingCache<Long, OHLCVDataSet> dataCache = CacheBuilder.newBuilder().concurrencyLevel(8).maximumSize(1000000).expireAfterWrite(10000, TimeUnit.HOURS)
				.build(new CacheLoader<Long, OHLCVDataSet>() {

					@Override
					public OHLCVDataSet load(Long key) throws Exception {
						return new OHLCVDataSet(rawData, new Duration(key));
					}

				});

		LoadingCache<SignalParameter, Double> fitnessCache = CacheBuilder.newBuilder().concurrencyLevel(8).maximumSize(1000000).expireAfterWrite(10000, TimeUnit.HOURS)
				.build(new CacheLoader<SignalParameter, Double>() {
					@Override
					public Double load(SignalParameter candidate) throws Exception {
						OHLCVDataSet data = null;
						try {

							data = dataCache.get(candidate.time.get());

						} catch (ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						List<BigDecimal> priceCloses = data.stream().map(x -> x.close).collect(Collectors.<BigDecimal> toList());
						OHLCVDataSet average = null;
						average = data.difference().average(() -> new ExponentialWeightedMovingAverageFunction(candidate.window.get()));
						List<BigDecimal> averageCloses = average.stream().map(x -> x.close).collect(Collectors.<BigDecimal> toList());
						ThresholdSignal signal = new ThresholdSignal("MomentumTrader", candidate.crossAbove.get(), candidate.counterCrossAbove.get(), candidate.crossBelow.get(),
								candidate.counterCrossBelow.get());
						SignalTestResults test = signal.test(averageCloses, priceCloses, candidate.target.get(), candidate.stop.get());
						candidate.results = test;
						return test.performance.max(BigDecimal.ZERO).doubleValue();
					}
				});

		/**
		 * Assigns one "fitness point" for every character in the candidate
		 * String that matches the corresponding position in the target string.
		 */
		public double getFitness(SignalParameter candidate, List<? extends SignalParameter> population) {
			try {
				return fitnessCache.get(candidate);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 0;
		}

		public boolean isNatural() {
			return true;
		}
	}

	private static class SignalParameterMutator implements EvolutionaryOperator<SignalParameter> {

		private Probability mutationChance;

		public SignalParameterMutator(Probability mutationChance) {
			super();
			this.mutationChance = mutationChance;
		}

		@Override
		public List<SignalParameter> apply(List<SignalParameter> selectedCandidates, Random rng) {
			return selectedCandidates.stream().map(x -> mutateParameter(x, rng)).collect(Collectors.toList());
		}

		private SignalParameter mutateParameter(SignalParameter aParameter, Random rng) {
			aParameter.mutate(rng);
			return aParameter;

		}
	}

	public static class SignalParametersCandidateFactory extends AbstractCandidateFactory<SignalParameter> {

		@Override
		public SignalParameter generateRandomCandidate(Random rng) {
			SignalParameter out = new SignalParameter(new Probability(0.2));
			out.time = new LongOptimizableParameter(100000 + (rng.nextInt(100) * 10000), 100000, 400000, 100, 10000);
			out.window = new IntegerOptimizableParameter(rng.nextInt(20) + 1, 1, 30, 5, 1);
			out.crossAbove = new BigDecimalOptimizableParameter(new BigDecimal(rng.nextInt(50) * 0.1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros(),
					new BigDecimal(0.1), new BigDecimal(20), 20, new BigDecimal(0.1));
			out.crossBelow = new BigDecimalOptimizableParameter(new BigDecimal((rng.nextInt(50) * 0.1) * -1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros(),
					new BigDecimal(-0.1), new BigDecimal(2 - 0), 20, new BigDecimal(0.1));
			out.counterCrossAbove = new BigDecimalOptimizableParameter(new BigDecimal(rng.nextInt(50) * 0.1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros(),
					new BigDecimal(0.1), new BigDecimal(20), 20, new BigDecimal(0.1));
			out.counterCrossBelow = new BigDecimalOptimizableParameter(new BigDecimal((rng.nextInt(50) * 0.1) * -1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros(),
					new BigDecimal(-0.1), new BigDecimal(-20), 20, new BigDecimal(0.1));
			out.target = new BigDecimalOptimizableParameter(new BigDecimal(rng.nextInt(50) * 0.1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros(), new BigDecimal(0.1),
					new BigDecimal(5), 20, new BigDecimal(0.1));
			out.stop = new BigDecimalOptimizableParameter(new BigDecimal((rng.nextInt(50) * 0.1) * -1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros(), new BigDecimal(
					-0.1), new BigDecimal(-5), 20, new BigDecimal(0.1));
			if (out.counterCrossBelow.get().floatValue() < out.crossBelow.get().floatValue()) {
				out.counterCrossBelow.set(out.crossBelow.get());
			}

			if (out.counterCrossAbove.get().floatValue() > out.crossAbove.get().floatValue()) {
				out.counterCrossAbove.set(out.crossAbove.get());
			}
			out.init();
			return out;
		}

	}

	public static class SignalParameter extends OptimizableParameterSet {
		public SignalParameter(Probability aChance) {
			super(aChance);
		}
		
		public void init(){
			parameters.add(time);
			parameters.add(window);
			parameters.add(crossAbove);
			parameters.add(crossBelow);
			parameters.add(counterCrossAbove);
			parameters.add(counterCrossBelow);
			parameters.add(target);
			parameters.add(stop);
		}

		
		public LongOptimizableParameter time;
		public IntegerOptimizableParameter window;
		public BigDecimalOptimizableParameter crossAbove;
		public BigDecimalOptimizableParameter counterCrossAbove;
		public BigDecimalOptimizableParameter crossBelow;
		public BigDecimalOptimizableParameter counterCrossBelow;
		public BigDecimalOptimizableParameter target;
		public BigDecimalOptimizableParameter stop;
		public SignalTestResults results;

		@Override
		public String toString() {
			return new ToStringBuilder(this).append("Time", time).append("Window", window).append("Cross Above", crossAbove).append("Counter Cross Above", counterCrossAbove)
					.append("Cross Below", crossBelow).append("Counter Cross Below", counterCrossBelow).append("Target", target).append("Stop", stop).append("Results", results)
					.toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			if (obj.getClass() != getClass()) {
				return false;
			}
			SignalParameter rhs = (SignalParameter) obj;
			return new EqualsBuilder().append(time, rhs.time).append(window, rhs.window).append(crossAbove, rhs.crossAbove).append(counterCrossAbove, rhs.counterCrossAbove)
					.append(crossBelow, rhs.crossBelow).append(counterCrossBelow, rhs.counterCrossBelow).append(target, rhs.target).append(stop, rhs.stop).isEquals();
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(time).append(window).append(crossAbove).append(crossBelow).append(counterCrossAbove).append(counterCrossBelow).append(target)
					.append(stop).toHashCode();
		}
	}

	public static void main(String[] args) {
		char[] chars = new char[27];
		for (char c = 'A'; c <= 'Z'; c++) {
			chars[c - 'A'] = c;
		}
		chars[26] = ' ';
		CandidateFactory<SignalParameter> factory = new SignalParametersCandidateFactory();

		// Create a pipeline that applies cross-over then mutation.
		List<EvolutionaryOperator<SignalParameter>> operators = new LinkedList<EvolutionaryOperator<SignalParameter>>();
		operators.add(new SplitEvolution<SignalParameter>(new IdentityOperator<SignalParameter>(), new SignalParameterMutator(new Probability(0.01)), 0.95));
		EvolutionaryOperator<SignalParameter> pipeline = new EvolutionPipeline<SignalParameter>(operators);

		TickerDatabase reader = new TickerDatabase("BitFinex");

		FitnessEvaluator<SignalParameter> fitnessEvaluator = new SignalEvaluator(reader.get(new CurrencyPair("BTC", "USD")));
		SelectionStrategy<Object> selection = new RankSelection();
		Random rng = new MersenneTwisterRNG();

		// EvolutionEngine<SignalParameter> engine = new
		// GenerationalEvolutionEngine<SignalParameter>(factory, pipeline,
		// fitnessEvaluator, selection, rng);
		IslandEvolution<SignalParameter> engine = new IslandEvolution<SignalParameter>(25, new RingMigration(), factory, pipeline, fitnessEvaluator, new RouletteWheelSelection(),
				rng);

		engine.addEvolutionObserver(new IslandEvolutionObserver<SignalParameter>() {
			double best = 0;

			@Override
			public void populationUpdate(PopulationData<? extends SignalParameter> arg0) {
				if (arg0.getMeanFitness() > best) {
					System.out.printf("Generation %d: %s %s\n", arg0.getGenerationNumber(), arg0.getMeanFitness(), arg0.getBestCandidate());
					best = arg0.getMeanFitness();
				}
			}

			@Override
			public void islandPopulationUpdate(int islandIndex, PopulationData<? extends SignalParameter> data) {
				if (data.getMeanFitness() > best) {
					System.out.printf("Island %d, Generation %d: %s %s\n", islandIndex, data.getGenerationNumber(), data.getMeanFitness(), data.getBestCandidate());
					best = data.getMeanFitness();
				}
			}
		});
		SignalParameter result = engine.evolve(200, 20, 10, 10, new TargetFitness(60, true));
		System.out.println(result);
	}
}
