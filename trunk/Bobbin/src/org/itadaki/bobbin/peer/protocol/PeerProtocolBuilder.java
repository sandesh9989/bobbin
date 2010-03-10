/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer.protocol;

import java.nio.ByteBuffer;
import java.util.Map;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BEncoder;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.CharsetUtil;


/**
 * Constructs byte arrays containing BitTorrent protocol messages
 */
public class PeerProtocolBuilder {

	/**
	 * Constructs a ByteBuffer containing a complete handshake
	 * @param fastExtensionEnabled If {@code true}, advertise the Fast extension to the remote peer
	 * @param extensionProtocolEnabled If {@code true}, advertise the Extension protocol to the
	 *        remote peer
	 * @param infoHash The info hash to use
	 * @param peerID The peer ID to use
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer handshake (boolean fastExtensionEnabled, boolean extensionProtocolEnabled, InfoHash infoHash, PeerID peerID) {

		byte[] handshakeBytes = new byte[68];
		handshakeBytes[0] = 19;
		System.arraycopy ("BitTorrent protocol".getBytes (CharsetUtil.ASCII), 0, handshakeBytes, 1, 19);
		if (fastExtensionEnabled) {
			handshakeBytes[27] |= (byte)0x04;
		}
		if (extensionProtocolEnabled) {
			handshakeBytes[25] |= (byte)0x10;
		}
		System.arraycopy (infoHash.getBytes(), 0, handshakeBytes, 28, 20);
		System.arraycopy (peerID.getBytes(), 0, handshakeBytes, 48, 20);

		return ByteBuffer.wrap (handshakeBytes);

	}


	/**
	 * Constructs a byte array containing a "keep-alive" message
	 *
	 * @return A byte array containing the encoded message
	 */
	public static ByteBuffer keepaliveMessage() {

		return ByteBuffer.wrap (new byte[] { 0, 0, 0, 0 });

	}


	/**
	 * Constructs a ByteBuffer containing a "choke" message
	 *
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer chokeMessage() {

		return ByteBuffer.wrap (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_CHOKE });

	}


	/**
	 * Constructs a ByteBuffer containing an "unchoke" message
	 *
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer unchokeMessage() {

		return ByteBuffer.wrap (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_UNCHOKE });

	}


	/**
	 * Constructs a ByteBuffer containing an "interested" message
	 *
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer interestedMessage() {

		return ByteBuffer.wrap (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_INTERESTED });

	}


	/**
	 * Constructs a ByteBuffer containing a "not interested" message
	 *
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer notInterestedMessage() {

		return ByteBuffer.wrap (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_NOT_INTERESTED });

	}


	/**
	 * Constructs a ByteBuffer containing a "have" message
	 *
	 * @param pieceNumber The piece number to send
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer haveMessage (int pieceNumber) {

		byte[] messageBytes = new byte[] {
				0, 0, 0, 5,
				PeerProtocolConstants.MESSAGE_TYPE_HAVE,
				(byte)((pieceNumber >>> 24) & 0xff),
				(byte)((pieceNumber >>> 16) & 0xff),
				(byte)((pieceNumber >>> 8) & 0xff),
				(byte)(pieceNumber & 0xff)
		};

		return ByteBuffer.wrap (messageBytes);

	}


	/**
	 * Constructs a ByteBuffer containing a "bitfield" message
	 *
	 * @param bitField The bitfield to send
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer bitfieldMessage (BitField bitField) {

		int length = 1 + bitField.byteLength();

		byte[] messageBytes =  new byte[length + 4];
		messageBytes[0] = (byte)((length >>> 24) & 0xff);
		messageBytes[1] = (byte)((length >>> 16) & 0xff);
		messageBytes[2] = (byte)((length >>> 8) & 0xff);
		messageBytes[3] = (byte)(length & 0xff);
		messageBytes[4] = PeerProtocolConstants.MESSAGE_TYPE_BITFIELD;
		bitField.copyTo (messageBytes, 5);

		ByteBuffer messageBuffer = ByteBuffer.wrap (messageBytes);
		return messageBuffer;

	}


	/**
	 * Constructs a ByteBuffer containing a "request" message
	 *
	 * @param descriptor The block to request
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer requestMessage (BlockDescriptor descriptor) {

		int pieceNumber = descriptor.getPieceNumber();
		int offset = descriptor.getOffset();
		int length = descriptor.getLength();

		byte[] messageBytes = new byte[] {
				0, 0, 0, 13,
				PeerProtocolConstants.MESSAGE_TYPE_REQUEST,
				(byte)((pieceNumber >>> 24) & 0xff),
				(byte)((pieceNumber >>> 16) & 0xff),
				(byte)((pieceNumber >>> 8) & 0xff),
				(byte)(pieceNumber & 0xff),
				(byte)((offset >>> 24) & 0xff),
				(byte)((offset >>> 16) & 0xff),
				(byte)((offset >>> 8) & 0xff),
				(byte)(offset & 0xff),
				(byte)((length >>> 24) & 0xff),
				(byte)((length >>> 16) & 0xff),
				(byte)((length >>> 8) & 0xff),
				(byte)(length & 0xff)

		};

		return ByteBuffer.wrap (messageBytes);

	}


	/**
	 * Constructs a pair of ByteBuffers that together contain a "piece" message
	 *
	 * @param descriptor The descriptor of the block to send
	 * @param block The block to send
	 * @return A byte array containing the encoded message
	 * @throws IllegalArgumentException if descriptor.getLength() is not equal to data.length
	 */
	public static ByteBuffer[] pieceMessage (BlockDescriptor descriptor, ByteBuffer block) {

		if (descriptor.getLength() != block.remaining()) {
			throw new IllegalArgumentException ("Invalid block data length");
		}

		int pieceNumber = descriptor.getPieceNumber();
		int offset = descriptor.getOffset();
		int messageLength = 9 + block.remaining();

		byte[] headerBytes =  new byte[13];
		headerBytes[0] = (byte)((messageLength >>> 24) & 0xff);
		headerBytes[1] = (byte)((messageLength >>> 16) & 0xff);
		headerBytes[2] = (byte)((messageLength >>> 8) & 0xff);
		headerBytes[3] = (byte)(messageLength & 0xff);
		headerBytes[4] = PeerProtocolConstants.MESSAGE_TYPE_PIECE;
		headerBytes[5] = (byte)((pieceNumber >>> 24) & 0xff);
		headerBytes[6] = (byte)((pieceNumber >>> 16) & 0xff);
		headerBytes[7] = (byte)((pieceNumber >>> 8) & 0xff);
		headerBytes[8] = (byte)(pieceNumber & 0xff);
		headerBytes[9] = (byte)((offset >>> 24) & 0xff);
		headerBytes[10] = (byte)((offset >>> 16) & 0xff);
		headerBytes[11] = (byte)((offset >>> 8) & 0xff);
		headerBytes[12] = (byte)(offset & 0xff);

		return new ByteBuffer[] { ByteBuffer.wrap (headerBytes), block };

	}


	/**
	 * Constructs a ByteBuffer containing a "cancel" message
	 *
	 * @param descriptor The block to request
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer cancelMessage (BlockDescriptor descriptor) {

		int pieceNumber = descriptor.getPieceNumber();
		int offset = descriptor.getOffset();
		int length = descriptor.getLength();

		byte[] messageBytes = new byte[] {
				0, 0, 0, 13,
				PeerProtocolConstants.MESSAGE_TYPE_CANCEL,
				(byte)((pieceNumber >>> 24) & 0xff),
				(byte)((pieceNumber >>> 16) & 0xff),
				(byte)((pieceNumber >>> 8) & 0xff),
				(byte)(pieceNumber & 0xff),
				(byte)((offset >>> 24) & 0xff),
				(byte)((offset >>> 16) & 0xff),
				(byte)((offset >>> 8) & 0xff),
				(byte)(offset & 0xff),
				(byte)((length >>> 24) & 0xff),
				(byte)((length >>> 16) & 0xff),
				(byte)((length >>> 8) & 0xff),
				(byte)(length & 0xff)

		};

		return ByteBuffer.wrap (messageBytes);

	}


	/**
	 * Constructs a ByteBuffer containing a "suggest piece" message
	 *
	 * @param pieceNumber The number of the piece that is suggested
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer suggestPieceMessage (int pieceNumber) {

		byte[] messageBytes = new byte[] {
				0, 0, 0, 5,
				PeerProtocolConstants.MESSAGE_TYPE_SUGGEST_PIECE,
				(byte)((pieceNumber >>> 24) & 0xff),
				(byte)((pieceNumber >>> 16) & 0xff),
				(byte)((pieceNumber >>> 8) & 0xff),
				(byte)(pieceNumber & 0xff)
		};

		return ByteBuffer.wrap (messageBytes);

	}


	/**
	 * Constructs a ByteBuffer containing a "have all" message
	 *
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer haveAllMessage() {

		return ByteBuffer.wrap (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_HAVE_ALL });

	}


	/**
	 * Constructs a ByteBuffer containing a "have none" message
	 *
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer haveNoneMessage() {

		return ByteBuffer.wrap (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_HAVE_NONE });

	}


	/**
	 * Constructs a ByteBuffer containing a "reject request" message
	 *
	 * @param descriptor
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer rejectRequestMessage (BlockDescriptor descriptor) {

		int pieceNumber = descriptor.getPieceNumber();
		int offset = descriptor.getOffset();
		int length = descriptor.getLength();

		byte[] messageBytes = new byte[] {
				0, 0, 0, 13,
				PeerProtocolConstants.MESSAGE_TYPE_REJECT_REQUEST,
				(byte)((pieceNumber >>> 24) & 0xff),
				(byte)((pieceNumber >>> 16) & 0xff),
				(byte)((pieceNumber >>> 8) & 0xff),
				(byte)(pieceNumber & 0xff),
				(byte)((offset >>> 24) & 0xff),
				(byte)((offset >>> 16) & 0xff),
				(byte)((offset >>> 8) & 0xff),
				(byte)(offset & 0xff),
				(byte)((length >>> 24) & 0xff),
				(byte)((length >>> 16) & 0xff),
				(byte)((length >>> 8) & 0xff),
				(byte)(length & 0xff)

		};

		return ByteBuffer.wrap (messageBytes);

	}


	/**
	 * Constructs a ByteBuffer containing an "allowed fast" message
	 *
	 * @param pieceNumber The number of the piece that is allowed fast
	 * @return A ByteBuffer containing the encoded message
	 */
	public static ByteBuffer allowedFastMessage (int pieceNumber) {

		byte[] messageBytes = new byte[] {
				0, 0, 0, 5,
				PeerProtocolConstants.MESSAGE_TYPE_ALLOWED_FAST,
				(byte)((pieceNumber >>> 24) & 0xff),
				(byte)((pieceNumber >>> 16) & 0xff),
				(byte)((pieceNumber >>> 8) & 0xff),
				(byte)(pieceNumber & 0xff)
		};

		return ByteBuffer.wrap (messageBytes);

	}


	/**
	 * Constructs a ByteBuffer containing an extension handshake message
	 *
	 * @param extensions A map linking extension names to the identifiers that will be used in
	 *        subsequent extension messages
	 * @param extra A set of key/value pairs to include in the handshake message, or {@code null}
	 * @return A ByteBuffer containing the encoded handshake
	 */
	public static ByteBuffer[] extensionHandshakeMessage (Map<String,Integer> extensions, BDictionary extra) {

		BDictionary extensionIDDictionary = new BDictionary();

		for (String extensionKey : extensions.keySet ()) {
			extensionIDDictionary.put (extensionKey, extensions.get (extensionKey));
		}

		BDictionary handshakeDictionary = (extra == null) ? new BDictionary() : extra.clone();
		handshakeDictionary.put ("m", extensionIDDictionary);

		return extensionMessage (0, ByteBuffer.wrap (BEncoder.encode (handshakeDictionary)));

	}


	/**
	 * Constructs a ByteBuffer containing an extension message
	 *
	 * @param extendedMessageID An extension ID defined in a previously sent extension handshake
	 * @param data The payload of the extension message
	 * @return An array of ByteBuffers containing the encoded message
	 */
	public static ByteBuffer[] extensionMessage (int extendedMessageID, ByteBuffer... data) {

		int messageLength = 2;
		for (ByteBuffer buffer : data) {
			messageLength += buffer.remaining();
		}

		byte[] headerBytes =  new byte[6];
		headerBytes[0] = (byte)((messageLength >>> 24) & 0xff);
		headerBytes[1] = (byte)((messageLength >>> 16) & 0xff);
		headerBytes[2] = (byte)((messageLength >>> 8) & 0xff);
		headerBytes[3] = (byte)(messageLength & 0xff);
		headerBytes[4] = PeerProtocolConstants.MESSAGE_TYPE_EXTENDED;
		headerBytes[5] = (byte)(extendedMessageID & 0xff);

		ByteBuffer[] buffers = new ByteBuffer[data.length + 1];
		buffers[0] = ByteBuffer.wrap (headerBytes);
		System.arraycopy (data, 0, buffers, 1, data.length);

		return buffers;

	}


	/**
	 * Constructs a Merkle extension piece message
	 *
	 * @param descriptor The descriptor of the block to send
	 * @param hashChain The concatenated sibling hash chain of the block to send
	 * @param block The block to send
	 * @return An array of ByteBuffers containing the encoded message
	 */
	public static ByteBuffer[] merklePieceMessage (BlockDescriptor descriptor, ByteBuffer hashChain, ByteBuffer block) {

		if (descriptor.getLength() != block.remaining()) {
			throw new IllegalArgumentException ("Invalid block data length");
		}

		if ((hashChain != null) && ((hashChain.remaining() % 20) != 0)) {
			throw new IllegalArgumentException ("Invalid hash chain length");
		}

		int pieceNumber = descriptor.getPieceNumber();
		int offset = descriptor.getOffset();
		int hashChainLength = 0;
		ByteBuffer encodedHashChain = null;
		if (hashChain != null) {
			int treeHeight = (hashChain.remaining() / 20) - 1;
			int path = descriptor.getPieceNumber();
			int breadthFirstIndex = (1 << (treeHeight - 1)) - 1 + descriptor.getPieceNumber();
			BList outerList = new BList();
			byte[] hash = new byte[20];
			hashChain.get (hash);
			outerList.add (new BList (new BInteger (breadthFirstIndex), new BBinary (hash)));
			for (int i = 0; i < treeHeight - 1; i++) {
				int siblingIndex = breadthFirstIndex + (((path & 0x01) == 0) ? 1 : -1);
				breadthFirstIndex = (breadthFirstIndex - 1) >> 1;
				path >>>= 1;
				hash = new byte[20];
				hashChain.get (hash);
				outerList.add (new BList (new BInteger (siblingIndex), new BBinary (hash)));
			}
			if (treeHeight > 0) {
				hash = new byte[20];
				hashChain.get (hash);
				outerList.add (new BList (new BInteger (0), new BBinary (hash)));
			}
			encodedHashChain = ByteBuffer.wrap (BEncoder.encode (outerList));
			hashChainLength = encodedHashChain.remaining();
		}

		byte[] messageHeaderBytes =  new byte[12];
		messageHeaderBytes[0] = (byte)((pieceNumber >>> 24) & 0xff);
		messageHeaderBytes[1] = (byte)((pieceNumber >>> 16) & 0xff);
		messageHeaderBytes[2] = (byte)((pieceNumber >>> 8) & 0xff);
		messageHeaderBytes[3] = (byte)(pieceNumber & 0xff);
		messageHeaderBytes[4] = (byte)((offset >>> 24) & 0xff);
		messageHeaderBytes[5] = (byte)((offset >>> 16) & 0xff);
		messageHeaderBytes[6] = (byte)((offset >>> 8) & 0xff);
		messageHeaderBytes[7] = (byte)(offset & 0xff);
		messageHeaderBytes[8] = (byte)((hashChainLength >>> 24) & 0xff);
		messageHeaderBytes[9] = (byte)((hashChainLength >>> 16) & 0xff);
		messageHeaderBytes[10] = (byte)((hashChainLength >>> 8) & 0xff);
		messageHeaderBytes[11] = (byte)(hashChainLength & 0xff);

		if (hashChain == null) {
			return extensionMessage (PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, ByteBuffer.wrap (messageHeaderBytes), block);
		}

		return extensionMessage (PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, ByteBuffer.wrap (messageHeaderBytes), encodedHashChain, block);

	}


	/**
	 * Constructs an Elastic extension signature message
	 *
	 * @param viewSignature The view signature
	 * @return An array of ByteBuffers containing the encoded message
	 */
	public static ByteBuffer[] elasticSignatureMessage (ViewSignature viewSignature) {

		if (viewSignature.getViewRootHash().remaining() != 20) {
			throw new IllegalArgumentException ("Invalid view root hash length");
		}

		if (viewSignature.getSignature().remaining() != 40) {
			throw new IllegalArgumentException ("Invalid signature length");
		}

		long viewLength = viewSignature.getViewLength();
		byte[] messageHeaderBytes =  new byte[9];
		messageHeaderBytes[0] = PeerProtocolConstants.ELASTIC_MESSAGE_TYPE_SIGNATURE;
		messageHeaderBytes[1] = (byte)((viewLength >>> 56) & 0xff);
		messageHeaderBytes[2] = (byte)((viewLength >>> 48) & 0xff);
		messageHeaderBytes[3] = (byte)((viewLength >>> 40) & 0xff);
		messageHeaderBytes[4] = (byte)((viewLength >>> 32) & 0xff);
		messageHeaderBytes[5] = (byte)((viewLength >>> 24) & 0xff);
		messageHeaderBytes[6] = (byte)((viewLength >>> 16) & 0xff);
		messageHeaderBytes[7] = (byte)((viewLength >>> 8) & 0xff);
		messageHeaderBytes[8] = (byte)(viewLength & 0xff);

		return extensionMessage (PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC, ByteBuffer.wrap (messageHeaderBytes), viewSignature.getViewRootHash(), viewSignature.getSignature());

	}


	/**
	 * Constructs an Elastic extension piece message
	 * @param descriptor The descriptor of the block to send
	 * @param viewLength The view length to which the hash chain applies
	 * @param hashChain The concatenated sibling hash chain of the block to send
	 * @param block The block to send
	 *
	 * @return An array of ByteBuffers containing the encoded message
	 */
	public static ByteBuffer[] elasticPieceMessage (BlockDescriptor descriptor, Long viewLength, ByteBuffer hashChain, ByteBuffer block) {

		if (descriptor.getLength() != block.remaining()) {
			throw new IllegalArgumentException ("Invalid block data length");
		}

		if ((hashChain != null) && ((hashChain.remaining() % 20) != 0)) {
			throw new IllegalArgumentException ("Invalid hash chain length");
		}

		int pieceNumber = descriptor.getPieceNumber();
		int offset = descriptor.getOffset();
		byte hashCount = (hashChain == null) ? 0 : (byte)(hashChain.remaining() / 20);

		byte[] messageHeaderBytes =  new byte[10];
		messageHeaderBytes[0] = PeerProtocolConstants.ELASTIC_MESSAGE_TYPE_PIECE;
		messageHeaderBytes[1] = (byte)((pieceNumber >>> 24) & 0xff);
		messageHeaderBytes[2] = (byte)((pieceNumber >>> 16) & 0xff);
		messageHeaderBytes[3] = (byte)((pieceNumber >>> 8) & 0xff);
		messageHeaderBytes[4] = (byte)(pieceNumber & 0xff);
		messageHeaderBytes[5] = (byte)((offset >>> 24) & 0xff);
		messageHeaderBytes[6] = (byte)((offset >>> 16) & 0xff);
		messageHeaderBytes[7] = (byte)((offset >>> 8) & 0xff);
		messageHeaderBytes[8] = (byte)(offset & 0xff);
		messageHeaderBytes[9] = hashCount;

		if (hashChain == null) {
			return extensionMessage (PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC, ByteBuffer.wrap (messageHeaderBytes), block);
		}

		ByteBuffer viewLengthBuffer = ByteBuffer.allocate (8);
		viewLengthBuffer.asLongBuffer().put (viewLength);

		return extensionMessage (PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC, ByteBuffer.wrap (messageHeaderBytes), viewLengthBuffer, hashChain, block);

	}


	/**
	 * Constructs an Elastic extension bitfield message
	 *
	 * @param bitField The bitfield to send
	 * @return An array of ByteBuffers containing the encoded message
	 */
	public static ByteBuffer[] elasticBitfieldMessage (BitField bitField) {

		byte[] messageBytes =  new byte[bitField.byteLength() + 1];
		messageBytes[0] = PeerProtocolConstants.ELASTIC_MESSAGE_TYPE_BITFIELD;
		bitField.copyTo (messageBytes, 1);

		return extensionMessage (PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC, ByteBuffer.wrap (messageBytes));

	}


	/**
	 * Not instantiable
	 */
	private PeerProtocolBuilder() {

	}

}
