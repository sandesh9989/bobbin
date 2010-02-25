/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;

import java.net.InetSocketAddress;

/**
 * Interface representing public information about a peer
 */
public interface Peer {

	/**
	 * @return The remote peer's ID
	 */
	public PeerID getRemotePeerID();

	/**
	 * @return The peer's address
	 */
	public InetSocketAddress getRemoteSocketAddress();

	/**
	 * @return The peer's view length
	 */
	public long getRemoteViewLength();

	/**
	 * @return {@code true} if the Fast extension is enabled, otherwise {@code false}
	 */
	public boolean isFastExtensionEnabled();

	/**
	 * @return {@code true} if the extension protocol is enabled, otherwise {@code false}
	 */
	public boolean isExtensionProtocolEnabled();

	/**
	 * @return {@code true} if the peer is choking us, otherwise {@code false}
	 */
	public boolean getTheyAreChoking();

	/**
	 * @return {@code true} if the peer is interested in us, otherwise {@code false}
	 */
	public boolean getTheyAreInterested();

	/**
	 * @return {@code true} if we are choking the peer, otherwise {@code false}
	 */
	public boolean getWeAreChoking();

	/**
	 * @return {@code true} if we are interested in the peer, otherwise {@code false}
	 */
	public boolean getWeAreInterested();

	/**
	 * @return The number of piece block bytes sent to the remote peer
	 */
	public long getBlockBytesSent();

	/**
	 * @return The number of piece block bytes received from the remote peer
	 */
	public long getBlockBytesReceived();

	/**
	 * @return The number of protocol bytes sent to the remote peer
	 */
	public long getProtocolBytesSent();

	/**
	 * @return The number of protocol bytes received from the remote peer
	 */
	public long getProtocolBytesReceived();

	/**
	 * @return A moving one second average of he number of protocol bytes sent to the remote peer
	 */
	public int getProtocolBytesSentPerSecond();

	/**
	 * @return A moving one second average of he number of protocol bytes received from the remote peer
	 */
	public int getProtocolBytesReceivedPerSecond();

}