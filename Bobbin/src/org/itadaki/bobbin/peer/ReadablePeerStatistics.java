package org.itadaki.bobbin.peer;

import org.itadaki.bobbin.peer.PeerStatistics.Type;
import org.itadaki.bobbin.util.counter.ReadableStatisticCounter;

/**
 * The read-only interface to PeerStatistics
 */
public interface ReadablePeerStatistics {

	/**
	 * @param type The identifier of the counter to return
	 * @return The read-only counter
	 */
	public ReadableStatisticCounter getReadableCounter (Type type);

	/**
	 * @param type The identifier of the counter to return the statistic for
	 * @return The statistic total
	 */
	public long getTotal (Type type);

	/**
	 * @param type The identifier of the counter to return the statistic for
	 * @return The statistic per second
	 */
	public long getPerSecond (Type type);

}