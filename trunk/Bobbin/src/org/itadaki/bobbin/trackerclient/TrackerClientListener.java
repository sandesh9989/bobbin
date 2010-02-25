/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.trackerclient;

import org.itadaki.bobbin.peer.PeerSourceListener;


/**
 * A listener interface for TrackerClient events
 */
public interface TrackerClientListener extends PeerSourceListener {

	/**
	 * Indicates that the TrackerClient has terminated
	 */
	public void trackerClientTerminated();

}
