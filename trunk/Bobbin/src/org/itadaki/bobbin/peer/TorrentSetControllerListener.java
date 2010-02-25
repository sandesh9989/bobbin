/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;


/**
 * A listener for TorrentSetController events
 */
public interface TorrentSetControllerListener {

	/**
	 * Indicates that a TorrentSetController has terminated
	 *
	 * @param controller The TorrentSetController that has terminated
	 */
	public void torrentSetControllerTerminated (TorrentSetController controller);

}
