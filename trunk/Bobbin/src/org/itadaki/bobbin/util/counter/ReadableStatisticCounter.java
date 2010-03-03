package org.itadaki.bobbin.util.counter;

/**
 * The read-only interface to a StatisticCounter
 */
public interface ReadableStatisticCounter {

	/**
	 * Gets the current total for a previously registered period
	 * @param period The period to get the current total for
	 *
	 * @return The current total for the given period
	 * @throws IllegalArgumentException if the period supplied was not previously registered
	 */
	public long getPeriodTotal (Period period);

	/**
	 * Gets the grand total of the supplied data since the counter was created
	 *
	 * @return The grand total of the supplied data since the counter was created
	 */
	public long getTotal();

}