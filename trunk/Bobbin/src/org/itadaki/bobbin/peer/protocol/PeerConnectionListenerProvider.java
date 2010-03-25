package org.itadaki.bobbin.peer.protocol;

import org.itadaki.bobbin.torrentdb.InfoHash;

/**
 * A provider interface to allow connecting the PeerNegotiator of an incoming connection to a
 * suitable PeerConnectionListener, once the info hash of the torrent the remote peer wants is known
 */
public interface PeerConnectionListenerProvider {

	/**
	 * @param infoHash The InfoHash to get a PeerConnectionListener for
	 * @return A suitable PeerConnectionListener for the info hash, if one is available, or
	 *         {@code null}
	 */
	public PeerConnectionListener getPeerConnectionListener (InfoHash infoHash);

}
