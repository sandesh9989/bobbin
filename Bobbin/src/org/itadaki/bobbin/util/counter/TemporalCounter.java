/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util.counter;


/**
 * A counter to gather the total of supplied figures on a periodic basis, automatically discarding
 * the oldest unit elements of the total as time progresses
 */
public class TemporalCounter {

	/**
	 * The period being measured
	 */
	private Period period;

	/**
	 * The time in system milliseconds of the most recently started unit
	 */
	private long startOfCurrentUnit;

	/**
	 * The underlying counter used to store the counted data
	 */
	private PeriodicCounter counter;


	/**
	 * Advances the counter to the present time, discarding any unit elements of the periodic total
	 * that occurred before the current period as measured relative to the present time
	 */
	private void advance() {

		long currentTime = System.currentTimeMillis();

		int periodsElapsed = (int)((currentTime - this.startOfCurrentUnit) / this.period.getUnitLength());

		this.counter.advance (periodsElapsed);
		this.startOfCurrentUnit += periodsElapsed * this.period.getUnitLength();

	}


	/**
	 * Gets the total value for the period
	 *
	 * @return The total value for the period, measured from the start of the oldest period unit
	 * recorded to the end of the most recently completed unit
	 */
	public long getPeriodTotal() {

		advance();
		return this.counter.getPeriodTotal();

	}


	/**
	 * Adds a value to the counter
	 *
	 * @param value The value to add
	 */
	public void add (long value) {

		advance();
		this.counter.add (value);

	}

	/**
	 * @param period The period to measure
	 * @param initialTime The time, in system milliseconds, to take as the start of the first unit
	 */
	public TemporalCounter (Period period, long initialTime) {

		this.startOfCurrentUnit = initialTime + ((System.currentTimeMillis() - initialTime) / period.getUnitLength());
		this.period = period;
		this.counter = new PeriodicCounter (period.getTotalUnits());

	}

}
