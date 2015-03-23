package com.tsavo.trade.portfolio;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.CLASS)
public interface Sample {

	public float getSample();

	public float getWeight();
}
