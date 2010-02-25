/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;


import java.nio.ByteBuffer;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;


/**
 * Interface exposing the capability to send extension messages to a remote peer
 */
public interface ExtensiblePeer extends Peer {

	/**
	 * Sends an extension handshake message to the peer
	 *
	 * @param extensionsAdded A list of extensions that have been enabled since the last handshake
	 * @param extensionsRemoved A list of extensions that have been disabled since the last
	 *        handshake
	 * @param extra A dictionary of extra key/value pairs to insert into the extension handshake, or
	 *        {@code null}
	 */
	public void sendExtensionHandshake (Set<String> extensionsAdded, Set<String> extensionsRemoved, BDictionary extra);

	/**
	 * Sends an extension message to the peer
	 *
	 * @param identifier The extension identifier
	 * @param data The message payload
	 */
	public void sendExtensionMessage (String identifier, ByteBuffer data);

}