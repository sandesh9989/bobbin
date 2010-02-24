package org.itadaki.bobbin.util.counter;

/**
 * A counting class to gather the total of supplied figures on a periodic basis
 */
public class PeriodicCounter {

	/**
	 * The totals for each period unit
	 */
	private long[] totals;

	/**
	 * The sum total of all past units
	 */
	private long sumTotal = 0;

	/**
	 * The position of the current period unit within the totals array;
	 */
	private int currentPosition = 0;


	/**
	 * Returns the number of units that make up the period counted
	 *
	 * @return The number of units that make up the period counted
	 */
	public int getNumUnits() {

		return this.totals.length;

	}


	/**
	 * Gets the sum total of all values added up to the end of the most recent period unit
	 *
	 * @return The sum total of all values added up to the end of the most recent period unit
	 */
	public long getPeriodTotal() {

		return this.sumTotal;

	}


	/**
	 * Adds a value to the total for the current period
	 *
	 * @param value
	 */
	public void add (long value) {

		this.totals[this.currentPosition] += value;

	}


	/**
	 * Advances the counter the given number of periods. The totals from the oldest period unit(s)
	 * will be discarded.
	 * 
	 * @param numUnits The number of periods to advance
	 */
	public void advance (int numUnits) {

		int previousPosition = this.currentPosition;

		for (int i = 0; i < numUnits; i++) {
			int nextPosition = (this.currentPosition + 1) % this.totals.length;
			this.sumTotal += this.totals[this.currentPosition];
			this.sumTotal -= this.totals[nextPosition];
			this.totals[nextPosition] = 0;
			this.currentPosition = nextPosition;
			if (this.currentPosition == previousPosition)
				break;
		}

	}


	/**
	 * @param numUnits The total number of period units to measure
	 */
	public PeriodicCounter (int numUnits) {

		this.totals = new long[numUnits + 1];

	}


}
