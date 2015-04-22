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
import org.apache.commons.math3.genetics.TournamentSelection;
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
import org.uncommons.watchmaker.framework.selection.SigmaScaling;
import org.uncommons.watchmaker.framework.selection.StochasticUniversalSampling;
import org.uncommons.watchmaker.framework.termination.TargetFitness;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tsavo.hippo.ExponentialWeightedMovingAverageFunction;
import com.tsavo.hippo.LiveTickerReader;
import com.tsavo.hippo.OHLCVDataSet;
import com.tsavo.trade.AbstractSignal.SignalTestResults;
import com.tsavo.trade.ThresholdSignal;
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

							data = dataCache.get(candidate.time);

						} catch (ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						List<BigDecimal> priceCloses = data.stream().map(x -> x.close).collect(Collectors.<BigDecimal> toList());
						OHLCVDataSet average = null;
						average = data.difference().average(() -> new ExponentialWeightedMovingAverageFunction(candidate.window));
						List<BigDecimal> averageCloses = average.stream().map(x -> x.close).collect(Collectors.<BigDecimal> toList());
						ThresholdSignal signal = new ThresholdSignal("MomentumTrader", candidate.crossAbove, candidate.counterCrossAbove, candidate.crossBelow,
								candidate.counterCrossBelow);
						SignalTestResults test = signal.test(averageCloses, priceCloses, candidate.target, candidate.stop);
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

			if (mutationChance.nextEvent(rng)) {
				if (rng.nextBoolean()) {
					aParameter.time += 10000 * rng.nextInt(20);
				} else {
					aParameter.time -= 10000 * rng.nextInt(20);
				}
				if (aParameter.time < 150000) {
					aParameter.time = 0;
				}
				if (aParameter.time > 1000 * 10 * 40) {
					aParameter.time = 1000 * 10 * 40;
				}
			}
			if (mutationChance.nextEvent(rng)) {
				if (rng.nextBoolean()) {
					aParameter.window += rng.nextInt(10);
				} else {
					aParameter.window -= rng.nextInt(10);
				}
				if (aParameter.window < 1) {
					aParameter.window = 1;
				}
				if (aParameter.window > 20) {
					aParameter.window = 20;
				}
			}
			if (mutationChance.nextEvent(rng)) {
				if (rng.nextBoolean()) {
					aParameter.crossAbove = aParameter.crossAbove.add(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
				} else {
					aParameter.crossAbove = aParameter.crossAbove.subtract(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
				}
				if (aParameter.crossBelow.floatValue() < 0) {
					aParameter.crossBelow = BigDecimal.ZERO;
				}
				if (aParameter.crossAbove.floatValue() > 50) {
					aParameter.crossAbove = new BigDecimal(50);
				}
			}
			if (mutationChance.nextEvent(rng)) {
				if (rng.nextBoolean()) {
					aParameter.counterCrossAbove = aParameter.counterCrossAbove.add(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
				} else {
					aParameter.counterCrossAbove = aParameter.counterCrossAbove.subtract(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN)
							.stripTrailingZeros();
				}
				if (aParameter.counterCrossAbove.floatValue() > aParameter.crossAbove.floatValue()) {
					aParameter.counterCrossAbove = aParameter.crossAbove;
				}
				if (aParameter.counterCrossAbove.floatValue() < 0) {
					aParameter.counterCrossAbove = BigDecimal.ZERO;
				}
			}
			if (mutationChance.nextEvent(rng)) {
				if (rng.nextBoolean()) {
					aParameter.crossBelow = aParameter.crossBelow.add(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
				} else {
					aParameter.crossBelow = aParameter.crossBelow.subtract(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
				}
				if (aParameter.crossBelow.floatValue() > 0) {
					aParameter.crossBelow = BigDecimal.ZERO;
				}
				if (aParameter.crossBelow.floatValue() < -50) {
					aParameter.crossBelow = new BigDecimal(-50);
				}
			}
			if (mutationChance.nextEvent(rng)) {
				if (rng.nextBoolean()) {
					aParameter.counterCrossBelow = aParameter.counterCrossBelow.add(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
				} else {
					aParameter.counterCrossBelow = aParameter.counterCrossBelow.subtract(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN)
							.stripTrailingZeros();
				}
				if (aParameter.counterCrossBelow.floatValue() < aParameter.crossBelow.floatValue()) {
					aParameter.counterCrossBelow = aParameter.crossBelow;
				}
				if (aParameter.counterCrossBelow.floatValue() > 0) {
					aParameter.counterCrossBelow = BigDecimal.ZERO;
				}
			}

			if (mutationChance.nextEvent(rng)) {
				if (rng.nextBoolean()) {
					aParameter.target = aParameter.target.add(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
				} else {
					aParameter.target = aParameter.target.subtract(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
				}
				if (aParameter.target.floatValue() < 0.5) {
					aParameter.target = new BigDecimal(0.5);
				}
				if (aParameter.target.floatValue() > 5) {
					aParameter.target = new BigDecimal(5);
				}
			}

			if (mutationChance.nextEvent(rng)) {
				if (rng.nextBoolean()) {
					aParameter.stop = aParameter.stop.add(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
				} else {
					aParameter.stop = aParameter.stop.subtract(new BigDecimal(rng.nextInt(3) * 0.1)).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
				}
				if (aParameter.stop.floatValue() > -0.5) {
					aParameter.stop = new BigDecimal(-0.5);
				}
				if (aParameter.stop.floatValue() < -5) {
					aParameter.stop = new BigDecimal(-5);
				}

			}
			return aParameter;

		}
	}

	public static class SignalParameterCrossover extends AbstractCrossover<SignalParameter> {

		protected SignalParameterCrossover(int crossoverPoints, Probability crossoverProbability) {
			super(crossoverPoints, crossoverProbability);
		}

		@Override
		protected List<SignalParameter> mate(SignalParameter parent1, SignalParameter parent2, int numberOfCrossoverPoints, Random rng) {
			List<SignalParameter> out = new ArrayList<SignalParameter>();
			SignalParameter cross = new SignalParameter();
			if (rng.nextBoolean()) {
				cross.time = parent1.time;
			} else {
				cross.time = parent2.time;
			}
			if (rng.nextBoolean()) {
				cross.window = parent1.window;
			} else {
				cross.window = parent2.window;
			}
			if (rng.nextBoolean()) {
				cross.crossAbove = parent1.crossAbove;
			} else {
				cross.crossAbove = parent2.crossAbove;
			}
			if (rng.nextBoolean()) {
				cross.crossBelow = parent1.crossBelow;
			} else {
				cross.crossBelow = parent2.crossBelow;
			}
			if (rng.nextBoolean()) {
				cross.counterCrossAbove = parent1.counterCrossAbove;
			} else {
				cross.counterCrossAbove = parent2.counterCrossAbove;
			}
			if (rng.nextBoolean()) {
				cross.counterCrossBelow = parent1.counterCrossBelow;
			} else {
				cross.counterCrossBelow = parent2.counterCrossBelow;
			}
			if (rng.nextBoolean()) {
				cross.target = parent1.target;
			} else {
				cross.target = parent2.target;
			}
			if (rng.nextBoolean()) {
				cross.stop = parent1.stop;
			} else {
				cross.stop = parent2.stop;
			}
			if (cross.counterCrossAbove.floatValue() > cross.crossAbove.floatValue()) {
				cross.counterCrossAbove = cross.crossAbove;
			}
			if (cross.counterCrossBelow.floatValue() < cross.crossBelow.floatValue()) {
				cross.counterCrossBelow = cross.crossBelow;
			}

			out.add(cross);
			return out;

		}

	}

	public static class SignalParametersCandidateFactory extends AbstractCandidateFactory<SignalParameter> {

		@Override
		public SignalParameter generateRandomCandidate(Random rng) {
			SignalParameter out = new SignalParameter();
			out.time = 260000;
			out.window = rng.nextInt(20) + 1;
			out.crossAbove = new BigDecimal(rng.nextInt(50) * 0.1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
			out.crossBelow = new BigDecimal((rng.nextInt(50) * 0.1) * -1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
			out.counterCrossAbove = new BigDecimal(rng.nextInt(50) * 0.1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
			out.counterCrossBelow = new BigDecimal((rng.nextInt(50) * 0.1) * -1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
			out.target = new BigDecimal(rng.nextInt(50) * 0.1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
			out.stop = new BigDecimal((rng.nextInt(50) * 0.1) * -1).setScale(1, RoundingMode.HALF_EVEN).stripTrailingZeros();
			if (out.counterCrossBelow.floatValue() < out.crossBelow.floatValue()) {
				out.counterCrossBelow = out.crossBelow;
			}

			if (out.counterCrossAbove.floatValue() > out.crossAbove.floatValue()) {
				out.counterCrossAbove = out.crossAbove;
			}
			return out;
		}

	}

	public static class SignalParameter {
		public long time;
		public int window;
		public BigDecimal crossAbove;
		public BigDecimal counterCrossAbove;
		public BigDecimal crossBelow;
		public BigDecimal counterCrossBelow;
		public BigDecimal target;
		public BigDecimal stop;

		@Override
		public String toString() {
			return new ToStringBuilder(this).append("Time", time).append("Window", window).append("Cross Above", crossAbove).append("Counter Cross Above", counterCrossAbove)
					.append("Cross Below", crossBelow).append("Counter Cross Below", counterCrossBelow).append("Target", target).append("Stop", stop).toString();
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
			return new EqualsBuilder().appendSuper(super.equals(obj)).append(time, rhs.time).append(window, rhs.window).append(crossAbove, rhs.crossAbove)
					.append(counterCrossAbove, rhs.counterCrossAbove).append(crossBelow, rhs.crossBelow).append(counterCrossBelow, rhs.counterCrossBelow)
					.append(target, rhs.target).append(stop, rhs.stop).isEquals();
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
		operators.add(new SplitEvolution<SignalParameter>(new IdentityOperator<SignalParameter>(), new SplitEvolution<SignalParameter>(new SignalParameterMutator(new Probability(
				0.01)), new SignalParameterCrossover(1, new Probability(0.01)), 0.5), 0.95));
		EvolutionaryOperator<SignalParameter> pipeline = new EvolutionPipeline<SignalParameter>(operators);

		LiveTickerReader reader = new LiveTickerReader("BitFinex");

		FitnessEvaluator<SignalParameter> fitnessEvaluator = new SignalEvaluator(reader.getDataForTimeframe(new CurrencyPair("BTC", "USD")));
		SelectionStrategy<Object> selection = new RankSelection();
		Random rng = new MersenneTwisterRNG();

		// EvolutionEngine<SignalParameter> engine = new
		// GenerationalEvolutionEngine<SignalParameter>(factory, pipeline,
		// fitnessEvaluator, selection, rng);
		IslandEvolution<SignalParameter> engine = new IslandEvolution<SignalParameter>(8, new RingMigration(), factory, pipeline, fitnessEvaluator, new RouletteWheelSelection(),
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
		SignalParameter result = engine.evolve(500, 20, 10, 10, new TargetFitness(60, true));
		System.out.println(result);
	}
}
