package com.tsavo.trade.portfolio;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public interface AverageFunction<T> {

	public float getAverage(List<T> someSamples);

	public void preSampleAdded(List<T> someSamples);
}
