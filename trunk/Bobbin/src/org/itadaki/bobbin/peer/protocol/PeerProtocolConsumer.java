/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.PieceStyle;
import org.itadaki.bobbin.torrentdb.ResourceType;
import org.itadaki.bobbin.torrentdb.ViewSignature;


/**
 * An interface for a consumer of BitTorrent peer protocol messages
 */
public interface PeerProtocolConsumer {

	/**
	 * Indicates that a "keep-alive" message has been received (a blank message)
	 *
	 * @throws IOException On any validation error
	 */
	public void keepAliveMessage() throws IOException;

	/**
	 * Indicates that a "choke" or "unchoke" message has been received (basic protocol message IDs 0
	 * and 1)
	 *
	 * @param choked {@code true} if choked, otherwise {@code false}
	 * @throws IOException On any validation error

	 */
	public void chokeMessage (boolean choked) throws IOException;

	/**
	 * Indicates that an "interested" or "not interested" message has been received (basic protocol
	 * message IDs 2 and 3)
	 *
	 * @param interested {@code true} if interested, otherwise {@code false}
	 * @throws IOException On any validation error
	 */
	public void interestedMessage (boolean interested) throws IOException;

	/**
	 * Indicates that a "have" or "resource have" message has been received (basic protocol message
	 * ID 4)
	 *
	 * @param resource The resource that is the subject of the message, or {@code null}
	 * @param pieceIndex The index of the piece that has been announced
	 * @throws IOException On any validation error
	 */
	public void haveMessage (ResourceType resource, int pieceIndex) throws IOException;

	/**
	 * Indicates that a "bitfield" or "resource bitfield" message has been received (basic protocol
	 * message ID 5)
	 *
	 * @param resource The resource that is the subject of the message, or {@code null}
	 * @param bitField The bits of the received bitfield
	 * @throws IOException On any validation error
	 */
	public void bitfieldMessage (ResourceType resource, byte[] bitField) throws IOException;

	/**
	 * Indicates that a "request" or "resource request" message has been received (basic protocol
	 * message ID 6)
	 *
	 * @param resource The resource that is the subject of the message, or {@code null}
	 * @param descriptor The received request
	 * @throws IOException On any validation error
	 */
	public void requestMessage (ResourceType resource, BlockDescriptor descriptor) throws IOException;

	/**
	 * Indicates that a "piece", "Merkle piece", "Elastic piece" or "resource piece" message has
	 * been received (basic protocol message ID 7)
	 * @param pieceStyle The style of the block received
	 * @param resource The resource that is the subject of the message, or {@code null}
	 * @param descriptor The descriptor of the block received
	 * @param viewLength For an elastic block, the view length to which the hash chain applies
	 * @param hashes For a Merkle or elastic block, the sibling hash chain received
	 * @param block The contents of the block received
	 *
	 * @throws IOException On any validation error
	 */
	public void pieceMessage (PieceStyle pieceStyle, ResourceType resource, BlockDescriptor descriptor, Long viewLength, ByteBuffer hashes, ByteBuffer block)
			throws IOException;

	/**
	 * Indicates that a "cancel" or "resource cancel" message has been received (basic protocol
	 * message ID 8)
	 *
	 * @param resource The resource that is the subject of the message, or {@code null}
	 * @param descriptor The request to cancel
	 * @throws IOException On any validation error
	 */
	public void cancelMessage (ResourceType resource, BlockDescriptor descriptor) throws IOException;

	/**
	 * Indicates that a "suggest piece" message has been received (basic protocol message ID 13 when
	 * the Fast extension is enabled)
	 *
	 * @param pieceNumber The piece number that is suggested
	 * @throws IOException On any validation error
	 */
	public void suggestPieceMessage (int pieceNumber) throws IOException;

	/**
	 * Indicates that a "have all" message has been received (basic protocol message ID 14 when the
	 * Fast extension is enabled)
	 *
	 * @throws IOException On any validation error
	 */
	public void haveAllMessage() throws IOException;

	/**
	 * Indicates that a "have none" message has been received (basic protocol message ID 15 when the
	 * Fast extension is enabled)
	 *
	 * @throws IOException On any validation error
	 */
	public void haveNoneMessage() throws IOException;

	/**
	 * Indicates that a "reject request" or "resource reject request" message has been received
	 * (basic protocol message ID 16 when the Fast extension is enabled)
	 *
	 * @param resource The resource that is the subject of the message, or {@code null}
	 * @param descriptor The descriptor of the request that has been rejected
	 * @throws IOException On any validation error
	 */
	public void rejectRequestMessage (ResourceType resource, BlockDescriptor descriptor) throws IOException;

	/**
	 * Indicates that an "allowed fast" message has been received (basic protocol message ID 17 when
	 * the Fast extension is enabled)
	 *
	 * @param pieceNumber The piece number that is allowed fast
	 * @throws IOException On any validation error
	 */
	public void allowedFastMessage (int pieceNumber) throws IOException;

	/**
	 * Indicates that an "extension handshake" message has been received (basic protocol message ID
	 * 20, extension ID 0 when the extension protocol is enabled)
	 *
	 * @param extensionsEnabled A set of extension identifier strings enabled in this handshake
	 * @param extensionsDisabled A set of extension identifier strings disabled in this handshake
	 * @param extra A set of key/value pairs included in the handshake
	 * @throws IOException On any validation error
	 */
	public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra) throws IOException;

	/**
	 * Indicates that an "extension" message has been received (basic protocol message ID 20,
	 * extension ID >0 when the extension protocol is enabled)
	 *
	 * @param identifier The string identifier of the extension, as negotiated in a previous
	 * extension handshake
	 * @param data The payload of the message
	 * @throws IOException On any validation error
	 */
	public void extensionMessage (String identifier, byte[] data) throws IOException;

	/**
	 * Indicates that a "elastic signature" message has been received (extension protocol identifier
	 * "bo_elastic", sub type 0)
	 *
	 * @param signature The view signature
	 * @throws IOException On any validation error
	 */
	public void elasticSignatureMessage (ViewSignature signature) throws IOException;

	/**
	 * Indicates that a "elastic bitfield" message has been received (extension protocol identifier
	 * "bo_elastic", sub type 2)
	 *
	 * @param bitField The bits of the received bitfield
	 * @throws IOException On any validation error
	 */
	public void elasticBitfieldMessage (byte[] bitField) throws IOException;

	/**
	 * Indicates that a "resource directory" message has been received (extension protocol
	 * identifier "bo_resource", sub type 1)
	 *
	 * @param resources The resources advertised
	 * @param lengths The lengths of the advertised resources
	 * @throws IOException On any validation error
	 */
	public void resourceDirectoryMessage (List<ResourceType> resources, List<Integer> lengths) throws IOException;

	/**
	 * Indicates that a "resource subscribe" message has been received (extension protocol
	 * identifier "bo_resource", sub type 2)
	 *
	 * @param resource The resource to subscribe to
	 * @throws IOException On any validation error
	 */
	public void resourceSubscribeMessage (ResourceType resource) throws IOException;

	/**
	 * Indicates that an unknown message has been received
	 *
	 * @param messageID The type ID of the received message
	 * @param messageBytes The content of the received message
	 * @throws IOException On any validation error
	 */
	public void unknownMessage (int messageID, byte[] messageBytes) throws IOException;

}
