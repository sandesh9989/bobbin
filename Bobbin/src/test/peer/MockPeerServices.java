/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.ExtensiblePeer;
import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.PeerServices;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.counter.StatisticCounter;
import org.itadaki.bobbin.util.elastictree.HashChain;


/**
 * A mock PeerServices
 */
public class MockPeerServices implements PeerServices {

	/**
	 * The local PeerID
	 */
	private PeerID localPeerID;

	/**
	 * The PieceDatabase
	 */
	private PieceDatabase pieceDatabase;


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#addAvailablePiece(org.itadaki.bobbin.peer.PeerHandler, int)
	 */
	public boolean addAvailablePiece (ManageablePeer peer, int pieceNumber) {
		fail();
		return false;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#addAvailablePieces(org.itadaki.bobbin.peer.PeerHandler)
	 */
	public boolean addAvailablePieces (ManageablePeer peer) {
		fail();
		return false;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#adjustChoking()
	 */
	public void adjustChoking (boolean opportunistic) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#configureExtensions(org.itadaki.bobbin.peer.PeerHandler)
	 */
	public void offerExtensionsToPeer (ExtensiblePeer peerHandler) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#enabledDisableExtensions(org.itadaki.bobbin.peer.PeerHandler, java.util.Set, java.util.Set)
	 */
	public void enableDisablePeerExtensions (ExtensiblePeer peer, Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#getBlockBytesReceivedCounter()
	 */
	public StatisticCounter getBlockBytesReceivedCounter() {
		return null;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#getBlockBytesSentCounter()
	 */
	public StatisticCounter getBlockBytesSentCounter() {
		return null;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#getPieceDatabase()
	 */
	public PieceDatabase getPieceDatabase() {
		return this.pieceDatabase;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#getLocalPeerID()
	 */
	public PeerID getLocalPeerID() {
		return this.localPeerID;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#getProtocolBytesReceivedCounter()
	 */
	public StatisticCounter getProtocolBytesReceivedCounter() {
		return null;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#getProtocolBytesSentCounter()
	 */
	public StatisticCounter getProtocolBytesSentCounter() {
		return null;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.ManageablePeer#getRequests(org.itadaki.bobbin.peer.PeerHandler, int)
	 */
	public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
		fail();
		return null;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#handleBlock(org.itadaki.bobbin.peer.ManageablePeer, org.itadaki.bobbin.peer.BlockDescriptor, byte[])
	 */
	public void handleBlock (ManageablePeer peer, BlockDescriptor request, ViewSignature viewSignature, HashChain hashChain, byte[] block) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#lock()
	 */
	public void lock() { }


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#peerConnected(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public boolean peerConnected (ManageablePeer peer) {
		fail();
		return false;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#peerDisconnected(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public void peerDisconnected (ManageablePeer peer) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#processExtensionMessage(org.itadaki.bobbin.peer.PeerHandler, java.lang.String, byte[])
	 */
	public void processExtensionMessage (ExtensiblePeer peer, String identifier, byte[] data) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#pieceSuggested(org.itadaki.bobbin.peer.ManageablePeer, int)
	 */
	public void setPieceAllowedFast (ManageablePeer peer, int pieceNumber) {
		fail();
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#setPieceSuggested(org.itadaki.bobbin.peer.ManageablePeer, int)
	 */
	public void setPieceSuggested (ManageablePeer peer, int pieceNumber) {
		fail();
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#handleViewSignature(org.itadaki.bobbin.peer.ViewSignature)
	 */
	public boolean handleViewSignature (ViewSignature viewSignature) {
		fail();
		return false;
	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#unlock()
	 */
	public void unlock() { }


	/**
	 * @param localPeerID The local PeerID
	 * @param pieceDatabase The PieceDatabase
	 */
	public MockPeerServices (PeerID localPeerID, PieceDatabase pieceDatabase) {

		this.localPeerID = localPeerID;
		this.pieceDatabase = pieceDatabase;

	}


}