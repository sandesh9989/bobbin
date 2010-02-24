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
