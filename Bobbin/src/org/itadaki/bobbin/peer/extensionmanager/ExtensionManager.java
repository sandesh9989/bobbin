/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer.extensionmanager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.ExtensiblePeer;
import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.PeerCoordinatorListener;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConstants;



/**
 * A manager to process extension messages
 */
public class ExtensionManager implements PeerCoordinatorListener {

	/**
	 * The set of registered extension handlers
	 */
	private final Map<String,ExtensionHandler> handlers = new HashMap<String,ExtensionHandler>();


	/* PeerCoordinatorLister interface */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerCoordinatorListener#peerCoordinatorCompleted()
	 */
	public void peerCoordinatorCompleted() {

		for (ExtensionHandler handler : this.handlers.values()) {
			handler.peerCoordinatorCompleted();
		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerCoordinatorListener#peerRegistered(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public void peerRegistered (ManageablePeer peer) {

		for (ExtensionHandler handler : this.handlers.values()) {
			handler.peerRegistered (peer);
		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerCoordinatorListener#peerDeregistered(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public void peerDeregistered (ManageablePeer peer) {

		for (ExtensionHandler handler : this.handlers.values()) {
			handler.peerDeregistered (peer);
		}

	}


	/**
	 * Offers all available extensions to a peer
	 *
	 * @param peer The peer to offer extensions to
	 */
	public void offerExtensionsToPeer (ExtensiblePeer peer) {

		int extendedMessageID = PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_CUSTOM;
		Map<String,Integer> enabledExtensions = new HashMap<String,Integer>();
		for (String identifier : this.handlers.keySet()) {
			enabledExtensions.put (identifier, extendedMessageID++);
		}

		BDictionary extra = new BDictionary();
		extra.put ("reqq", PeerProtocolConstants.MAXIMUM_INBOUND_REQUESTS);

		peer.sendExtensionHandshake (enabledExtensions, new HashSet<String>(), extra);

	}


	/**
	 * Enables and disables extensions in response to a peer's extension handshake message
	 *
	 * @param peer The peer that has enabled and/or disabled extensions
	 * @param extensionsEnabled The extension identifiers that have been enabled, or {@code null}
	 * @param extensionsDisabled The extension identifiers that have been disabled, or {@code null}
	 * @param extra Additional key/value pairs 
	 */
	public void enableDisablePeerExtensions (ExtensiblePeer peer, Set<String> extensionsEnabled, Set<String> extensionsDisabled,
			BDictionary extra)
	{

		if (extensionsEnabled != null) {
			for (String identifier : extensionsEnabled) {
				ExtensionHandler handler = this.handlers.get (identifier);
				if (handler != null) {
					handler.peerEnabledExtension (peer, extra);
				}
			}
		}

		if (extensionsDisabled != null) {
			for (String identifier : extensionsDisabled) {
				ExtensionHandler handler = this.handlers.get (identifier);
				if (handler != null) {
					handler.peerDisabledExtension (peer);
				}
			}
		}

	}


	/**
	 * Processes a peer extension message through the appropriate handler
	 *
	 * @param peer The peer that sent the message
	 * @param identifier The extension's identifier
	 * @param messageData The message payload
	 */
	public void processExtensionMessage (ExtensiblePeer peer, String identifier, byte[] messageData) {

		ExtensionHandler handler = this.handlers.get (identifier);
		handler.extensionMessage (peer, messageData);

	}

	/**
	 * Registers a new extension handler
	 *
	 * @param handler
	 */
	public void registerExtension (ExtensionHandler handler) {

		this.handlers.put (handler.getIdentifier(), handler);

	}


}
