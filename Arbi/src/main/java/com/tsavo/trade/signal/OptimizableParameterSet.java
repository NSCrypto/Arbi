package com.tsavo.trade.signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.uncommons.maths.random.Probability;

public class OptimizableParameterSet {

	public List<OptimizableParameter<?>> parameters = new ArrayList<>();
	private Probability chances;

	public OptimizableParameterSet(Probability aChance) {
		chances = aChance;
	}

	public void mutate(Random rng) {
		parameters.stream().filter(x -> chances.nextEvent(rng)).forEach(x -> x.mutate(rng));
	}
	

}
