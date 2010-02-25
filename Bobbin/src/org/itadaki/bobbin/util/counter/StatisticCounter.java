/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util.counter;

import java.util.HashMap;
import java.util.Map;


/**
 * A counter that provides the sum of supplied data, both in total and across user specified periods
 */
public class StatisticCounter {

	/**
	 * The base time in system milliseconds against which the periods are measured
	 */
	private long initialTime = System.currentTimeMillis();

	/**
	 * The grand total of all supplied data
	 */
	private long total = 0;

	/**
	 * The individual counters for the specified periods
	 */
	private Map<Period,TemporalCounter> counters = new HashMap<Period,TemporalCounter>();

	/**
	 * A counter that is hierarchically the parent of this counter. All additions to this counter
	 * will be added to the parent counter as well.
	 */
	private StatisticCounter parent = null;


	/**
	 * Instructs the counter to begin accounting for the given period. Adding a second period with
	 * the same characteristics has no effect
	 *
	 * @param period The period to account
	 */
	public synchronized void addCountedPeriod (Period period) {

		if (this.counters.containsKey (period))
			return;

		this.counters.put (period, new TemporalCounter (period, this.initialTime));

	}


	/**
	 * Gets the current total for a previously registered period
	 * @param period The period to get the current total for
	 *
	 * @return The current total for the given period
	 * @throws IllegalArgumentException if the period supplied was not previously registered
	 */
	public synchronized long getPeriodTotal (Period period) {

		if (!this.counters.containsKey (period)) {
			throw new IllegalArgumentException();
		}

		TemporalCounter counter = this.counters.get (period);
	
		return counter.getPeriodTotal();

	}


	/**
	 * Gets the grand total of the supplied data since the counter was created
	 *
	 * @return The grand total of the supplied data since the counter was created
	 */
	public synchronized long getTotal() {

		return this.total;

	}


	/**
	 * Adds a value to the counter
	 *
	 * @param value The value to add
	 */
	public synchronized void add (long value) {

		if (this.parent != null) {
			this.parent.add (value);
		}

		this.total += value;

		for (TemporalCounter counter : this.counters.values()) {
			counter.add (value);
		}

	}


	/**
	 * Sets a counter that is hierarchically the parent of this counter. All additions to this
	 * counter will be added to the parent counter as well.
	 *
	 * @param parent The parent counter to set
	 */
	public synchronized void setParent (StatisticCounter parent) {

		this.parent = parent;

	}


}
