package com.tsavo.trade;

import java.math.BigDecimal;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Firebase.AuthResultHandler;

public class ThresholdSignal extends AbstractSignal implements Signal {

	public static Firebase db = new Firebase("https://t3-portfolio.firebaseio.com/Signals");
	
	static {
		db.authWithPassword("evilgenius@nefariousplan.com", "Zabbas4242!", new AuthResultHandler() {

			@Override
			public void onAuthenticationError(FirebaseError arg0) {
				throw new RuntimeException(arg0.toException());
			}

			@Override
			public void onAuthenticated(AuthData arg0) {
				System.out.println(arg0);
			}
		});
	}

	
	String name;
	BigDecimal aboveCrossing, aboveCounterCrossing, belowCrossing, belowCounterCrossing;
	boolean crossedAbove = false, crossedBelow = false;
	boolean shortState = false, longState = false;
	Firebase state;
	public ThresholdSignal(String aName, BigDecimal aboveCrossing, BigDecimal aboveCounterCrossing, BigDecimal belowCrossing, BigDecimal belowCounterCrossing) {
		super();
		this.name = aName;
		this.aboveCrossing = aboveCrossing;
		this.aboveCounterCrossing = aboveCounterCrossing;
		this.belowCrossing = belowCrossing;
		this.belowCounterCrossing = belowCounterCrossing;
	//	state = db.child(name);
	}

	public void addSample(BigDecimal aSample) {
		shortState = false;
		longState = false;
		if (aSample.compareTo(aboveCrossing) > 0) {
			crossedAbove = true;
		}
		if (aSample.compareTo(belowCrossing) < 0) {
			crossedBelow = true;
		}
		if (crossedAbove && aSample.compareTo(aboveCounterCrossing) < 0) {
			crossedAbove = false;
			shortState = true;
		}
		if (crossedBelow && aSample.compareTo(belowCounterCrossing) > 0) {
			crossedBelow = false;
			longState = true;
		}
	}

	@Override
	public boolean isLong() {
		return longState;
	}

	@Override
	public boolean isShort() {
		return shortState;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void reset() {
		longState = false;
		shortState = false;
		crossedAbove = false;
		crossedBelow = false;
	}
}
