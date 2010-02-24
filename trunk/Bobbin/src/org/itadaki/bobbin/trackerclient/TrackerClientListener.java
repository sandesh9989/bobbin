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
