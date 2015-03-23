package com.tsavo.trade.portfolio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.CLASS)
public class WeightedSample implements Sample {

	float sample;

	float weight;

	public WeightedSample() {
	}

	public WeightedSample(@JsonProperty("sample") float aSample, @JsonProperty("weight") float aWeight) {
		sample = aSample;
		weight = aWeight;
	}

	public float getSample() {
		return sample;
	}

	public void setSample(float sample) {
		this.sample = sample;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

}
