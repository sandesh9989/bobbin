package org.itadaki.bobbin.peer.protocol;

import java.io.IOException;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.ViewSignature;



/**
 * An interface for a consumer of BitTorrent peer protocol messages
 */
public interface PeerProtocolConsumer {

	/**
	 * Indicates that the basic extension part of the handshake has been received
	 *
	 * @param fastExtensionEnabled If {@code true}, the remote peer supports the Fast extension
	 * @param extensionProtocolEnabled If {@code true}, the remote peer supports the extension protocol
	 * @throws IOException On any validation error
	 */
	public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) throws IOException;

	/**
	 * Indicates that the info hash part of the handshake has been received
	 *
	 * @param infoHash The 20 byte info hash received
	 * @throws IOException On any validation error
	 */
	public void handshakeInfoHash (InfoHash infoHash) throws IOException;

	/**
	 * Indicates that the peer ID part of the handshake has been received
	 *
	 * @param peerID The peer ID received
	 * @throws IOException On any validation error
	 */
	public void handshakePeerID (PeerID peerID) throws IOException;

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
	 * Indicates that a "have" message has been received (basic protocol message ID 4)
	 * 
	 * @param pieceIndex The index of the piece that has been announced
	 * @throws IOException On any validation error
	 */
	public void haveMessage (int pieceIndex) throws IOException;

	/**
	 * Indicates that a "bitfield" message has been received (basic protocol message ID 5)
	 * 
	 * @param bitField The bits of the received bitfield
	 * @throws IOException On any validation error
	 */
	public void bitfieldMessage (byte[] bitField) throws IOException;

	/**
	 * Indicates that a "request" message has been received (basic protocol message ID 6)
	 * 
	 * @param descriptor The received request
	 * @throws IOException On any validation error
	 */
	public void requestMessage (BlockDescriptor descriptor) throws IOException;

	/**
	 * Indicates that a "piece" message has been received (basic protocol message ID 7)
	 * 
	 * @param descriptor The descriptor of the block received
	 * @param block The contents of the block received
	 * @throws IOException On any validation error
	 */
	public void pieceMessage (BlockDescriptor descriptor, byte[] block) throws IOException;

	/**
	 * Indicates that a "cancel" message has been received (basic protocol message ID 8)
	 * 
	 * @param descriptor The request to cancel
	 * @throws IOException On any validation error
	 */
	public void cancelMessage (BlockDescriptor descriptor) throws IOException;

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
	 * Indicates that a "reject request" message has been received (basic protocol message ID 16
	 * when the Fast extension is enabled)
	 *
	 * @param descriptor The descriptor of the request that has been rejected
	 * @throws IOException On any validation error
	 */
	public void rejectRequestMessage (BlockDescriptor descriptor) throws IOException;

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
	 * Indicates that a "Merkle piece" message has been received (extension protocol identifier
	 * "Tr_hashpiece")
	 * 
	 * @param descriptor The descriptor of the block received
	 * @param hashChain The sibling hash chain received
	 * @param block The contents of the block received
	 * @throws IOException On any validation error
	 */
	public void merklePieceMessage (BlockDescriptor descriptor, byte[] hashChain, byte[] block) throws IOException;


	/**
	 * Indicates that a "elastic signature" message has been received (extension protocol identifier
	 * "bo_elastic", sub type 0)
	 *
	 * @param signature The view signature
	 * 
	 * @throws IOException On any validation error
	 */
	public void elasticSignatureMessage (ViewSignature signature) throws IOException;


	/**
	 * Indicates that a "elastic piece" message has been received (extension protocol identifier
	 * "bo_elastic", sub type 1)
	 * @param descriptor The descriptor of the block received
	 * @param viewLength The view length to which the hash chain applies
	 * @param hashChain The sibling hash chain received
	 * @param block The contents of the block received
	 * 
	 * @throws IOException On any validation error
	 */
	public void elasticPieceMessage (BlockDescriptor descriptor, Long viewLength, byte[] hashChain, byte[] block) throws IOException;


	/**
	 * Indicates that a "elastic bitfield" message has been received (extension protocol identifier
	 * "bo_elastic", sub type 2)
	 * 
	 * @param bitField The bits of the received bitfield
	 * @throws IOException On any validation error
	 */
	public void elasticBitfieldMessage (byte[] bitField) throws IOException;


	/**
	 * Indicates that an unknown message has been received
	 * 
	 * @param messageID The type ID of the received message
	 * @param messageBytes The content of the received message
	 * @throws IOException On any validation error
	 */
	public void unknownMessage (int messageID, byte[] messageBytes) throws IOException;

}
