package com.tsavo.trade.portfolio;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EarningsReport implements Serializable {

	
	private static final long serialVersionUID = -134463260612228231L;
	List<Float> earnings = new ArrayList<Float>();

	@JsonIgnore
	public float getTotalEarnings() {
		float total = 0;
		for (Float earning : getEarnings()) {
			total += earning;
		}
		return total;
	}

	public List<Float> getEarnings() {
		return earnings;
	}

	public void setEarnings(List<Float> earnings) {
		this.earnings = earnings;
	}

}
