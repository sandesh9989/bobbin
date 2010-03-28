package org.itadaki.bobbin.peer;

import org.itadaki.bobbin.util.counter.ReadableStatisticCounter;
import org.itadaki.bobbin.util.counter.StatisticCounter;

/**
 * Protocol statistics about a peer connection
 */
public class PeerStatistics implements ReadablePeerStatistics {

	/**
	 * A type identifer for tracked statistics
	 */
	public static enum Type {

		/**
		 * Protocol bytes sent from the local peer to the remote peer
		 */
		PROTOCOL_BYTES_SENT,

		/**
		 * Protocol bytes received from the remote peer by the local peer
		 */
		PROTOCOL_BYTES_RECEIVED,

		/**
		 * Block bytes sent from the local peer to the remote peer
		 */
		BLOCK_BYTES_SENT,

		/**
		 * Unverified block bytes received from the remote peer by the local peer
		 */
		BLOCK_BYTES_RECEIVED_RAW;

	}


	/**
	 * Protocol bytes sent from the local peer to the remote peer
	 */
	final StatisticCounter protocolBytesSent = new StatisticCounter();

	/**
	 * Protocol bytes received from the remote peer by the local peer
	 */
	final StatisticCounter protocolBytesReceived = new StatisticCounter();

	/**
	 * Block bytes sent from the local peer to the remote peer
	 */
	final StatisticCounter blockBytesSent = new StatisticCounter();

	/**
	 * Unverified block bytes received from the remote peer by the local peer
	 */
	final StatisticCounter blockBytesReceivedRaw = new StatisticCounter();


	/* ReadablePeerStatistics interface */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ReadablePeerStatistics#getReadableCounter(org.itadaki.bobbin.peer.PeerStatistics.Type)
	 */
	public ReadableStatisticCounter getReadableCounter (Type type) {

		return getCounter (type);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ReadablePeerStatistics#getStatisticTotal(org.itadaki.bobbin.peer.PeerStatistics.Type)
	 */
	public long getTotal (Type type) {

		return getCounter(type).getTotal();

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ReadablePeerStatistics#getStatisticPerSecond(org.itadaki.bobbin.peer.PeerStatistics.Type)
	 */
	public long getPerSecond (Type type) {

		return getCounter(type).getPeriodTotal (PeerCoordinator.TWO_SECOND_PERIOD) / 2;

	}


	/**
	 * @param type The identifier of the counter to return
	 * @return The counter
	 */
	public StatisticCounter getCounter (Type type) {

		switch (type) {
			case PROTOCOL_BYTES_SENT:
				return this.protocolBytesSent;
			case PROTOCOL_BYTES_RECEIVED:
				return this.protocolBytesReceived;
			case BLOCK_BYTES_SENT:
				return this.blockBytesSent;
			case BLOCK_BYTES_RECEIVED_RAW:
				return this.blockBytesReceivedRaw;
		}

		return null;

	}


	/**
	 * Default constructor
	 */
	public PeerStatistics() {

		this.protocolBytesSent.addCountedPeriod (PeerCoordinator.TWO_SECOND_PERIOD);
		this.protocolBytesReceived.addCountedPeriod (PeerCoordinator.TWO_SECOND_PERIOD);
		this.blockBytesSent.addCountedPeriod (PeerCoordinator.TWO_SECOND_PERIOD);
		this.blockBytesReceivedRaw.addCountedPeriod (PeerCoordinator.TWO_SECOND_PERIOD);

	}


	/**
	 * Constructs a PeerStatistics that additionally passes through data to a parent aggregate
	 * PeerStatistics
	 *
	 * @param statistics The PeerStatistics to use as parent
	 */
	public PeerStatistics (PeerStatistics statistics) {

		this();

		this.protocolBytesSent.setParent (statistics.protocolBytesSent);
		this.protocolBytesReceived.setParent (statistics.protocolBytesReceived);
		this.blockBytesSent.setParent (statistics.blockBytesSent);
		this.blockBytesReceivedRaw.setParent (statistics.blockBytesReceivedRaw);

	}


}
