/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.trackerclient;


/**
 * An identifier consisting of the peer ID, host name or IP and port number of a peer
 */
public final class PeerIdentifier {

	/**
	 * The peer's peer ID
	 */
	public final byte[] peerID;

	/**
	 * The peer's host name or IP
	 */
	public final String host;

	/**
	 * The peer's port
	 */
	public final int port;


	/**
	 * @param peerID The peer's peer ID
	 * @param host The peer's host name or IP
	 * @param port The peer's port number
	 */
	public PeerIdentifier (byte[] peerID, String host, int port) {

		this.peerID = peerID;
		this.host = host;
		this.port = port;

	}

}
