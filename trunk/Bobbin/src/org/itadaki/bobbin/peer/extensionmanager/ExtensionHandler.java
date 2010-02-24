package org.itadaki.bobbin.peer.extensionmanager;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.ExtensiblePeer;
import org.itadaki.bobbin.peer.PeerCoordinatorListener;


/**
 * A handler for a single protocol extension
 */
public interface ExtensionHandler extends PeerCoordinatorListener {

	/**
	 * @return The extension's string identifier
	 */
	public String getIdentifier();

	/**
	 * Indicates that a peer has enabled this extension
	 *
	 * @param peer The peer that has enabled the extension
	 * @param extra Additional key/value pairs sent by the peer in the extension handshake
	 */
	public void peerEnabledExtension (ExtensiblePeer peer, BDictionary extra);

	/**
	 * Indicates that a peer has disabled this extension
	 *
	 * @param peer The peer that has disabled the extension
	 */
	public void peerDisabledExtension (ExtensiblePeer peer);

	/**
	 * Processes a message
	 * 
	 * @param peer @param peer The peer that has sent the message
	 * @param messageData The message payload
	 */
	public void extensionMessage (ExtensiblePeer peer, byte[] messageData);

}
