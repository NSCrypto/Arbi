package com.tsavo.trade.portfolio;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public class ExpodentialMovingAverageFunction extends WeightedMovingAverageFunction {

	float weight = 0.8f;

	public ExpodentialMovingAverageFunction() {
		// TODO Auto-generated constructor stub
	}

	public ExpodentialMovingAverageFunction(int aWeight) {
		weight = 1f - (2.0f / (aWeight + 1.0f));
	}

	@Override
	public void preSampleAdded(List<WeightedSample> someSamples) {
		super.preSampleAdded(someSamples);
		for (WeightedSample sample : someSamples) {
			sample.setWeight(sample.getWeight() * weight);
		}
	}

}
