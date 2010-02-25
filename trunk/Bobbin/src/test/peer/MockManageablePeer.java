/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.counter.StatisticCounter;



/**
 * A mock ManageablePeer
 */
public class MockManageablePeer implements ManageablePeer {

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#cancelRequests(java.util.List)
	 */
	public void cancelRequests (List<BlockDescriptor> requests) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#close()
	 */
	public void close() {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#getBlockBytesReceivedCounter()
	 */
	public StatisticCounter getBlockBytesReceivedCounter() {
		fail();
		return null;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#getBlockBytesSentCounter()
	 */
	public StatisticCounter getBlockBytesSentCounter() {
		fail();
		return null;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#getRemoteBitField()
	 */
	public BitField getRemoteBitField() {
		fail();
		return null;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getRemoteViewLength()
	 */
	public long getRemoteViewLength() {
		fail();
		return 0;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#getTheyHaveOutstandingRequests()
	 */
	public boolean getTheyHaveOutstandingRequests() {
		fail();
		return false;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#isExtensionProtocolEnabled()
	 */
	public boolean isExtensionProtocolEnabled() {
		fail();
		return false;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#isFastExtensionEnabled()
	 */
	public boolean isFastExtensionEnabled() {
		fail();
		return false;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#rejectPiece(int)
	 */
	public void rejectPiece (int pieceNumber) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ExtensiblePeer#sendExtensionHandshake(java.util.Set, java.util.Set, org.itadaki.bobbin.bencode.BDictionary)
	 */
	public void sendExtensionHandshake (Set<String> extensionsAdded, Set<String> extensionsRemoved, BDictionary extra) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#sendExtensionMessage(java.lang.String, java.nio.ByteBuffer)
	 */
	public void sendExtensionMessage (String identifier, ByteBuffer data) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#sendHavePiece(int)
	 */
	public void sendHavePiece (int pieceNumber) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#sendKeepaliveOrClose()
	 */
	public void sendKeepaliveOrClose() {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#sendViewSignature(org.itadaki.bobbin.peer.ViewSignature)
	 */
	public void sendViewSignature (ViewSignature viewSignature) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#setWeAreChoking(boolean)
	 */
	public boolean setWeAreChoking (boolean weAreChokingThem) {
		fail();
		return false;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#setWeAreInterested(boolean)
	 */
	public void setWeAreInterested (boolean weAreInterested) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getBlockBytesReceived()
	 */
	public long getBlockBytesReceived() {
		fail();
		return 0;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getBlockBytesSent()
	 */
	public long getBlockBytesSent() {
		fail();
		return 0;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getProtocolBytesReceived()
	 */
	public long getProtocolBytesReceived() {
		fail();
		return 0;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getProtocolBytesReceivedPerSecond()
	 */
	public int getProtocolBytesReceivedPerSecond() {
		fail();
		return 0;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getProtocolBytesSent()
	 */
	public long getProtocolBytesSent() {
		fail();
		return 0;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getProtocolBytesSentPerSecond()
	 */
	public int getProtocolBytesSentPerSecond() {
		fail();
		return 0;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getRemotePeerID()
	 */
	public PeerID getRemotePeerID() {
		fail();
		return null;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getRemoteSocketAddress()
	 */
	public InetSocketAddress getRemoteSocketAddress() {
		fail();
		return null;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getTheyAreChoking()
	 */
	public boolean getTheyAreChoking() {
		fail();
		return false;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getTheyAreInterested()
	 */
	public boolean getTheyAreInterested() {
		fail();
		return false;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getWeAreChoking()
	 */
	public boolean getWeAreChoking() {
		fail();
		return false;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.Peer#getWeAreInterested()
	 */
	public boolean getWeAreInterested() {
		fail();
		return false;
	}


}
