package org.itadaki.bobbin.peer.protocol;

import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.peer.PeerID;

/**
 * A listener for peer connection events
 */
public interface PeerConnectionListener {

	/**
	 * Indicates that a new peer's connection has completed its initial handshake. A new listener
	 * must be synchronously connected to the connection during this call
	 *
	 * @param connection The peer's connection, positioned after the last byte of the base protocol
	 *        handshake
	 * @param remotePeerID The peer's ID
	 * @param fastExtensionEnabled {@code true} if the Fast extension was negotiated, otherwise
	 *        {@code false}
	 * @param extensionProtocolEnabled {@code true} if the extension protocol was negotiated,
	 *        otherwise {@code false}
	 */
	public void peerConnectionComplete (Connection connection, PeerID remotePeerID, boolean fastExtensionEnabled, boolean extensionProtocolEnabled);

	/**
	 * Indicates that an outbound connection has failed to complete negotiation
	 *
	 * @param connection The peer's connection
	 */
	public void peerConnectionFailed (Connection connection);

}
