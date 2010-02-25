/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;

import org.itadaki.bobbin.torrentdb.InfoHash;


/**
 * A provider interface to allow connecting the PeerHandler of an incoming connection to a suitable
 * PeerServices, once the info hash of the torrent the remote peer wants is known
 */
public interface PeerServicesProvider {

	/**
	 * @param infoHash A 20 byte info hash
	 * @return A suitable PeerServices for the info hash, if one is available, or {@code null}
	 */
	public PeerServices getPeerServices (InfoHash infoHash);

}
