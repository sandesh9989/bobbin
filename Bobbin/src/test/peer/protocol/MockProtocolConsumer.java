/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer.protocol;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.ViewSignature;


/**
 * A mock PeerProtocolConsumer that fails by default on invocation of any method
 */
public abstract class MockProtocolConsumer implements PeerProtocolConsumer {

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#bitfieldMessage(byte[])
	 */
	public void bitfieldMessage (byte[] bitField) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#cancelMessage(org.itadaki.bobbin.peer.BlockDescriptor)
	 */
	public void cancelMessage (BlockDescriptor blockDescriptor) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#chokeMessage(boolean)
	 */
	public void chokeMessage (boolean choked) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#extensionHandshakeMessage(java.util.Set, java.util.Set, org.itadaki.bobbin.bencode.BDictionary)
	 */
	public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra) throws IOException
	{
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#extensionMessage(java.lang.String, byte[])
	 */
	public void extensionMessage (String identifier, byte[] data) throws IOException {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#handshakeExtensions(boolean)
	 */
	public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#handshakeInfoHash(byte[])
	 */
	public void handshakeInfoHash (InfoHash infoHash) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#handshakePeerID(PeerID)
	 */
	public void handshakePeerID (PeerID peerID) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#haveMessage(int)
	 */
	public void haveMessage (int pieceIndex) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#interestedMessage(boolean)
	 */
	public void interestedMessage (boolean interested) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#keepAliveMessage()
	 */
	public void keepAliveMessage() {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#pieceMessage(org.itadaki.bobbin.peer.BlockDescriptor, byte[])
	 */
	public void pieceMessage (BlockDescriptor blockDescriptor, byte[] data) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#requestMessage(org.itadaki.bobbin.peer.BlockDescriptor)
	 */
	public void requestMessage (BlockDescriptor blockDescriptor) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerProtocolConsumer#unknownMessage(int, byte[])
	 */
	public void unknownMessage (int messageID, byte[] messageData) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#allowedFastMessage(int)
	 */
	public void allowedFastMessage (int pieceNumber) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#haveAllMessage()
	 */
	public void haveAllMessage() {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#haveNoneMessage()
	 */
	public void haveNoneMessage() {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#rejectRequestMessage(org.itadaki.bobbin.peer.BlockDescriptor)
	 */
	public void rejectRequestMessage (BlockDescriptor descriptor) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#suggestPieceMessage(int)
	 */
	public void suggestPieceMessage (int pieceNumber) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#merklePieceMessage(org.itadaki.bobbin.torrentdb.BlockDescriptor, byte[], byte[])
	 */
	public void merklePieceMessage (BlockDescriptor descriptor, byte[] hashChain, byte[] block) throws IOException {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#elasticSignatureMessage(org.itadaki.bobbin.peer.ViewSignature)
	 */
	public void elasticSignatureMessage (ViewSignature viewSignature) throws IOException {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#elasticPieceMessage(long, org.itadaki.bobbin.torrentdb.BlockDescriptor, byte[], byte[])
	 */
	public void elasticPieceMessage (BlockDescriptor descriptor, Long viewLength, byte[] hashChain, byte[] block) throws IOException {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#elasticBitfieldMessage(byte[])
	 */
	public void elasticBitfieldMessage (byte[] bitField) throws IOException {
		fail();
	}


}