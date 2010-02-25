/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;

import java.util.List;

import org.itadaki.bobbin.trackerclient.PeerIdentifier;



/**
 * A listener interface to receive notification of potential new peers
 */
public interface PeerSourceListener {

	/**
	 * Indicates that potential new peers have been discovered
	 *
	 * @param identifiers The identifiers of the potential new peers
	 */
	public void peersDiscovered (List<PeerIdentifier> identifiers);

}
