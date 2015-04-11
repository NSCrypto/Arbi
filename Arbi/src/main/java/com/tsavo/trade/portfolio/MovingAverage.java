package com.tsavo.trade.portfolio;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public class MovingAverage<T extends Sample> implements Sampler<T> {

	public List<T> window = new ArrayList<T>();
	public AverageFunction<T> averageFunction;

	public int windowSize = 30;
	public float total = 0;
	public int index = 0;

	public MovingAverage() {
	}

	public MovingAverage(int aWindowSize, AverageFunction<T> aFunction) {
		windowSize = aWindowSize;
		averageFunction = aFunction;
	}

	@Override
	public void addSample(T aSample) {
		while (window.size() > windowSize) {
			window.remove(window.size() - 1);
		}
		if (index > windowSize) {
			index = 0;
		}
		averageFunction.preSampleAdded(window);
		if (window.size() < windowSize) {
			window.add(aSample);
		} else {
			window.set(index, aSample);
		}
		++index;
		if (index == windowSize) {
			index = 0;
		}

	}

	@JsonIgnore
	public float getAverage() {
		return averageFunction.getAverage(window);
	}

	public List<T> getWindow() {
		return window;
	}

	public void setWindow(List<T> window) {
		this.window = window;
	}

	public AverageFunction<T> getAverageFunction() {
		return averageFunction;
	}

	public void setAverageFunction(AverageFunction<T> averageFunction) {
		this.averageFunction = averageFunction;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public float getTotal() {
		return total;
	}

	public void setTotal(float total) {
		this.total = total;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

}
