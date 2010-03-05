package org.itadaki.bobbin.peer.protocol;

import java.io.IOException;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.ViewSignature;

/**
 * A default PeerProtocolConsumer that fails on any message
 */
public class DefaultPeerProtocolConsumer implements PeerProtocolConsumer {

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#allowedFastMessage(int)
	 */
	public void allowedFastMessage (int pieceNumber) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#bitfieldMessage(byte[])
	 */
	public void bitfieldMessage (byte[] bitField) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#cancelMessage(org.itadaki.bobbin.torrentdb.BlockDescriptor)
	 */
	public void cancelMessage (BlockDescriptor descriptor) throws IOException {

		throw new IOException ("Unexpected message");


	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#chokeMessage(boolean)
	 */
	public void chokeMessage (boolean choked) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#elasticBitfieldMessage(byte[])
	 */
	public void elasticBitfieldMessage (byte[] bitField) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#elasticPieceMessage(org.itadaki.bobbin.torrentdb.BlockDescriptor, java.lang.Long, byte[], byte[])
	 */
	public void elasticPieceMessage (BlockDescriptor descriptor, Long viewLength, byte[] hashChain, byte[] block) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#elasticSignatureMessage(org.itadaki.bobbin.torrentdb.ViewSignature)
	 */
	public void elasticSignatureMessage (ViewSignature signature) throws IOException {

		throw new IOException ("Unexpected message");


	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#extensionHandshakeMessage(java.util.Set, java.util.Set, org.itadaki.bobbin.bencode.BDictionary)
	 */
	public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#extensionMessage(java.lang.String, byte[])
	 */
	public void extensionMessage (String identifier, byte[] data) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#handshakeBasicExtensions(boolean, boolean)
	 */
	public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#handshakeInfoHash(org.itadaki.bobbin.torrentdb.InfoHash)
	 */
	public void handshakeInfoHash (InfoHash infoHash) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#handshakePeerID(org.itadaki.bobbin.peer.PeerID)
	 */
	public void handshakePeerID (PeerID peerID) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#haveAllMessage()
	 */
	public void haveAllMessage() throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#haveMessage(int)
	 */
	public void haveMessage (int pieceIndex) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#haveNoneMessage()
	 */
	public void haveNoneMessage() throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#interestedMessage(boolean)
	 */
	public void interestedMessage (boolean interested) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#keepAliveMessage()
	 */
	public void keepAliveMessage() throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#merklePieceMessage(org.itadaki.bobbin.torrentdb.BlockDescriptor, byte[], byte[])
	 */
	public void merklePieceMessage (BlockDescriptor descriptor, byte[] hashChain, byte[] block) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#pieceMessage(org.itadaki.bobbin.torrentdb.BlockDescriptor, byte[])
	 */
	public void pieceMessage (BlockDescriptor descriptor, byte[] block) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#rejectRequestMessage(org.itadaki.bobbin.torrentdb.BlockDescriptor)
	 */
	public void rejectRequestMessage (BlockDescriptor descriptor) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#requestMessage(org.itadaki.bobbin.torrentdb.BlockDescriptor)
	 */
	public void requestMessage (BlockDescriptor descriptor) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#suggestPieceMessage(int)
	 */
	public void suggestPieceMessage (int pieceNumber) throws IOException {

		throw new IOException ("Unexpected message");

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer#unknownMessage(int, byte[])
	 */
	public void unknownMessage (int messageID, byte[] messageBytes) throws IOException {

		throw new IOException ("Unexpected message");

	}

}
