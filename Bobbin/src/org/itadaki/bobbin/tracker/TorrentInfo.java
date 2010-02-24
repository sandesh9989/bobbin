package org.itadaki.bobbin.tracker;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.itadaki.bobbin.peer.PeerID;



/**
 * Information about a torrent tracked by the tracker
 */
public class TorrentInfo {

	/**
	 * The peers that have announced for this torrent
	 */
	private Map<PeerID,PeerInfo> peers = new HashMap<PeerID, PeerInfo>();

	/**
	 * Number of peers without a complete set of data
	 */
	private int incompletePeerCount = 0;

	/**
	 * Number of peers with a complete set of data
	 */
	private int completePeerCount = 0;

	/**
	 * Number of times a peer has reported a completed download
	 */
	private int downloadedPeerCount = 0;


	/**
	 * @return the peers
	 */
	public Map<PeerID,PeerInfo> getPeers() {

		return this.peers;

	}


	/**
	 * @return the incompletePeerCount
	 */
	public int getIncompletePeerCount() {

		return this.incompletePeerCount;

	}


	/**
	 * @return the completePeerCount
	 */
	public int getCompletePeerCount() {

		return this.completePeerCount;

	}


	/**
	 * @return the downloadedPeerCount
	 */
	public int getDownloadedPeerCount() {

		return this.downloadedPeerCount;

	}


	/**
	 * Updates tracked data based on a peer event
	 * 
	 * @param peerID The peer's ID
	 * @param peerKey The peer's key
	 * @param address The peer's address
	 * @param port The peer's port
	 * @param uploaded The peer's bytes uploaded
	 * @param downloaded The peer's bytes downloaded
	 * @param remaining The peer's bytes remaining
	 * @param event The event
	 * @return true if successfully processed, or false if the peer key was incorrect
	 *
	 */
	public boolean event (PeerID peerID, ByteBuffer peerKey, InetAddress address, int port,
			long uploaded, long downloaded, long remaining, TrackerEvent event)
	{

		PeerInfo peerInfo = this.peers.get (peerID);
		if ((peerInfo != null) && !(peerInfo.getKey().equals (peerKey))) {
			return false;
		}

		boolean peerExistedPreviously = (peerInfo != null);
		long previousRemaining = (peerInfo == null) ? -1 : peerInfo.getRemaining();

		if (peerInfo == null) {
			peerInfo = new PeerInfo (peerID, peerKey, address, port, uploaded, downloaded, remaining);
			this.peers.put (peerID, peerInfo);
		}

		switch (event) {

			case STARTED:
				break;

			case STOPPED:
				this.peers.remove (peerID);
				peerInfo = null;
				break;

			case COMPLETED:
				peerInfo.setUploaded (uploaded);
				peerInfo.setDownloaded (downloaded);
				peerInfo.setRemaining (remaining);
				peerInfo.setComplete (true);
				this.downloadedPeerCount++;
				break;

			case UPDATE:
				peerInfo.setUploaded (uploaded);
				peerInfo.setDownloaded (downloaded);
				peerInfo.setRemaining (remaining);
				peerInfo.setLastUpdated (System.currentTimeMillis());
				break;

		}

		// Adjust complete/incomplete tallies
		if (peerExistedPreviously) {

			if (peerInfo == null) {
				// Remove the peer from the tally if we deleted it
				if (previousRemaining > 0) {
					this.incompletePeerCount--;
				} else {
					this.completePeerCount--;
				}
			} else {
				if ((previousRemaining > 0) && (peerInfo.getRemaining() == 0)) {
					// incomplete -> complete
					this.incompletePeerCount--;
					this.completePeerCount++;
				} else if ((previousRemaining == 0) && (peerInfo.getRemaining () > 0)) {
					// complete -> incomplete (shouldn't happen but deal with it anyway)
					this.completePeerCount--;
					this.incompletePeerCount++;
				}

			}

		} else {

			if (peerInfo != null) {
				// New peer
				if (peerInfo.getRemaining() > 0) {
					this.incompletePeerCount++;
				} else {
					this.completePeerCount++;
				}
			}

		}

		return true;

	}


}
