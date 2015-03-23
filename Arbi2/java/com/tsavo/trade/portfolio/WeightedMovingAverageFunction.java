package com.tsavo.trade.portfolio;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public class WeightedMovingAverageFunction implements AverageFunction<WeightedSample> {

	public float getAverage(List<WeightedSample> someSamples) {
		float sampleTotal = 0;
		float weightTotal = 0;

		for (WeightedSample sample : someSamples) {
			sampleTotal += sample.getSample() * sample.getWeight();
			weightTotal += sample.getWeight();
		}
		return (sampleTotal / weightTotal);
	}

	public void preSampleAdded(List<WeightedSample> someSamples) {
	}
}
