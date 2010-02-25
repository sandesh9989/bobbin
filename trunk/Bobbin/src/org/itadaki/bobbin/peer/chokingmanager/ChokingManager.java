/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer.chokingmanager;

import org.itadaki.bobbin.peer.PeerCoordinatorListener;

/**
 * A manager to control the choking and unchoking of peers
 */
public interface ChokingManager extends PeerCoordinatorListener {

	/**
	 * Adjusts the choking of the registered peer set. Peers that are selected by the algorithm will
	 * be unchoked; all other peers will be choked
	 *
	 * @param seeding If {@code true}, the seed choking algorithm will be applied; if {@code false},
	 *        the download choking algorithm will be applied
	 */
	public void chokePeers (boolean seeding);

}