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
	 * @return The peer's socket address
	 */
	public InetSocketAddress getRemoteSocketAddress();

	/**
	 * @return The peer's protocol state
	 */
	public PeerState getPeerState();

	/**
	 * @return The peer's protocol statistics
	 */
	public ReadablePeerStatistics getReadableStatistics();

}