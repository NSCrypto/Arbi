package com.tsavo.trade.signal;

import java.util.Random;

public interface OptimizableParameter<T> {

	public void mutate(Random rng);
	public void set(T aT);
	public T get();
	
	
}
