package org.itadaki.bobbin.connectionmanager;


/**
 * An interface to receive notifications for new inbound connections
 */
public interface InboundConnectionListener {

	/**
	 * Indicates that an inbound connection has been accepted and registered. The connection's
	 * listener should be set using {@link Connection#setListener(ConnectionReadyListener)} within
	 * this method.
	 *
	 * @param connection The newly registered Connection
	 */
	public void accepted (Connection connection);


}
