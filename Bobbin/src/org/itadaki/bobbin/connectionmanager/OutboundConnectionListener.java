package org.itadaki.bobbin.connectionmanager;


/**
 * An interface to receive notifications for new outbound connections
 */
public interface OutboundConnectionListener {

	/**
	 * Indicates that an outgoing connection has been successfully made
	 *
	 * @param connection The newly registered Connection
	 */
	public void connected (Connection connection);


	/**
	 * Indicates that an outgoing connection could not be completed
	 *
	 * @param connection The failed connection
	 */
	public void rejected (Connection connection);


}
