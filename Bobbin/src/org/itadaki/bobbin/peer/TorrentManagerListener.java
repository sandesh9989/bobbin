package org.itadaki.bobbin.peer;


/**
 * A listener for state changes in a TorrentManager
 */
public interface TorrentManagerListener {

	/**
	 * Indicates that a TorrentManager is running
	 *
	 * @param torrentManager The TorrentManager that is running
	 */
	public void torrentManagerRunning (TorrentManager torrentManager);

	/**
	 * Indicates that a TorrentManager is stopped
	 *
	 * @param torrentManager The TorrentManager that is stopped
	 */
	public void torrentManagerStopped (TorrentManager torrentManager);

	/**
	 * Indicates that a TorrentManager is stopped as a result of an error
	 *
	 * @param torrentManager The TorrentManager that is stopped
	 */
	public void torrentManagerError (TorrentManager torrentManager);

	/**
	 * Indicates that a TorrentManager has terminated
	 *
	 * @param torrentManager The TorrentManager that has terminated
	 */
	public void torrentManagerTerminated (TorrentManager torrentManager);

}
