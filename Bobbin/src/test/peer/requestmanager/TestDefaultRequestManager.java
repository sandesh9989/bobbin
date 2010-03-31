/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer.requestmanager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.util.List;

import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.PeerState;
import org.itadaki.bobbin.peer.requestmanager.DefaultRequestManager;
import org.itadaki.bobbin.peer.requestmanager.RequestManager;
import org.itadaki.bobbin.peer.requestmanager.RequestManagerListener;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.StorageDescriptor;
import org.itadaki.bobbin.util.BitField;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import test.Util;


/**
 * Tests DefaultRequestManager
 */
public class TestDefaultRequestManager {

	/**
	 * @param descriptor The remote peer's view
	 * @param peerBitField The remote peer's bitfield
	 * @return A standard mock peer
	 */
	private static ManageablePeer mockManageablePeer (StorageDescriptor descriptor, BitField peerBitField) {

		PeerState mockPeerState = mock (PeerState.class);
		when (mockPeerState.getRemoteView()).thenReturn (descriptor);
		ManageablePeer mockPeer = mock (ManageablePeer.class);
		when (mockPeer.getPeerState()).thenReturn (mockPeerState);
		when (mockPeer.getRemoteBitField()).thenReturn (peerBitField);
		return mockPeer;

	}


	// Test allocation

	/**
	 * Test piece allocation : We have 1/1 pieces, peer has 0; no allocation
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateHave1Peer0() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;
		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField (1);
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);
		BitField peerBitField = new BitField (1);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		requestManager.peerRegistered (peer);

		// When
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 1, false);

		// Then
		assertEquals (0, blocks.size());

	}


	/**
	 * Test piece allocation : We have 1/1 pieces, peer has 1; no allocation
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateHave1Peer1() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;
		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField (1);
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);
		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		requestManager.peerRegistered (peer);

		// When
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 1, false);

		// Then
		assertEquals (0, blocks.size());

	}


	/**
	 * Test piece allocation : We have 0 /1 pieces, peer has 0; no allocation
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateHave0Peer0() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;
		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);
		BitField peerBitField = new BitField (1);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		requestManager.peerRegistered (peer);

		// When
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 1, false);

		// Then
		assertEquals (0, blocks.size());

	}


	/**
	 * Test piece allocation : We have 0/1 pieces, peer has 1; allocation
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateHave0Peer1() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;
		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);
		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		requestManager.peerRegistered (peer);

		// When
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 1, false);

		// Then
		assertEquals (1, blocks.size());

	}


	/**
	 * Test piece allocation : We have 0/1 pieces, peer A has 1, peer B has 1; double allocation
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateHave0PeerA1PeerB1() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);

		BitField peerABitField = new BitField (1);
		peerABitField.set (0);
		ManageablePeer peerA = mockManageablePeer (descriptor, peerABitField);
		requestManager.peerRegistered (peerA);

		BitField peerBBitField = new BitField (1);
		peerBBitField.set (0);
		ManageablePeer peerB = mockManageablePeer (descriptor, peerBBitField);
		requestManager.peerRegistered (peerB);

		// When
		List<BlockDescriptor> blocksA1 = requestManager.allocateRequests (peerA, 16, false);
		List<BlockDescriptor> blocksA2 = requestManager.allocateRequests (peerA, 16, false);
		List<BlockDescriptor> blocksB1 = requestManager.allocateRequests (peerB, 16, false);
		List<BlockDescriptor> blocksB2 = requestManager.allocateRequests (peerB, 16, false);

		// Then
		assertEquals (16, blocksA1.size());
		assertEquals (0, blocksA2.size());
		assertEquals (16, blocksB1.size());
		assertEquals (0, blocksB2.size());
		assertArrayEquals (blocksA1.toArray(), blocksB1.toArray());

	}


	/**
	 * Test piece allocation : We have 0/2 pieces, peer A has 2, peer B has 2; double allocation, different first choice
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateSize2Have0PeerA1PeerB1() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize * 2;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(2).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);

		BitField peerABitField = new BitField (2);
		peerABitField.not();
		ManageablePeer peerA = mockManageablePeer (descriptor, peerABitField);
		requestManager.peerRegistered (peerA);

		BitField peerBBitField = new BitField (2);
		peerBBitField.not();
		ManageablePeer peerB = mockManageablePeer (descriptor, peerBBitField);
		requestManager.peerRegistered (peerB);

		// When
		List<BlockDescriptor> blocksA1 = requestManager.allocateRequests (peerA, 16, false);
		List<BlockDescriptor> blocksB1 = requestManager.allocateRequests (peerB, 16, false);
		List<BlockDescriptor> blocksA2 = requestManager.allocateRequests (peerA, 16, false);
		List<BlockDescriptor> blocksB2 = requestManager.allocateRequests (peerB, 16, false);

		// Then
		assertEquals (16, blocksA1.size());
		assertEquals (16, blocksB1.size());
		assertEquals (16, blocksA2.size());
		assertEquals (16, blocksB2.size());
		assertArrayEquals (blocksA1.toArray(), blocksB2.toArray());
		assertArrayEquals (blocksB1.toArray(), blocksA2.toArray());

	}


	/**
	 * Test piece allocation : We have 0/16 pieces, peer A has 16, peer B has 16; double allocation, different first choices
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateSize16Have0PeerA8PeerB8() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize * 16;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(16).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);

		BitField peerABitField = new BitField (16);
		peerABitField.not();
		ManageablePeer peerA = mockManageablePeer (descriptor, peerABitField);
		requestManager.peerRegistered (peerA);

		BitField peerBBitField = new BitField (16);
		peerBBitField.not();
		ManageablePeer peerB = mockManageablePeer (descriptor, peerBBitField);
		requestManager.peerRegistered (peerB);

		// When
		List<BlockDescriptor> blocksA1 = requestManager.allocateRequests (peerA, 16 * 8, false);
		List<BlockDescriptor> blocksB1 = requestManager.allocateRequests (peerB, 16 * 8, false);
		List<BlockDescriptor> blocksA2 = requestManager.allocateRequests (peerA, 16 * 8, false);
		List<BlockDescriptor> blocksB2 = requestManager.allocateRequests (peerB, 16 * 8, false);

		// Then
		assertEquals (16 * 8, blocksA1.size());
		assertEquals (16 * 8, blocksB1.size());
		assertEquals (16 * 8, blocksA2.size());
		assertEquals (16 * 8, blocksB2.size());
		assertArrayEquals (blocksA1.toArray(), blocksB2.toArray());
		assertArrayEquals (blocksB1.toArray(), blocksA2.toArray());

	}


	// Test piece handling

	/**
	 * After setting the last piece not needed, no more allocations are given
	 *
	 * @throws Exception
	 */
	@Test
	public void testPieceHandlingLastPiece() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManagerListener listener = mock (RequestManagerListener.class);
		RequestManager requestManager = new DefaultRequestManager (descriptor, listener);

		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		requestManager.peerRegistered (peer);

		// When
		requestManager.setPieceNotNeeded (0);
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);

		// Then
		assertEquals (0, blocks.size());

	}


	/**
	 * On a double allocation, the second peer is sent a cancel on receipt of the piece
	 *
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDoubleAllocationCancel() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManagerListener listener = mock (RequestManagerListener.class);
		RequestManager requestManager = new DefaultRequestManager (descriptor, listener);
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		BitField peer2BitField = new BitField (1);

		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		ManageablePeer peer2 = mockManageablePeer (descriptor, peer2BitField);
		ArgumentCaptor<List> peer2captor = ArgumentCaptor.forClass (List.class);
		peerBitField.set (0);
		requestManager.peerRegistered (peer);
		peer2BitField.set (0);
		requestManager.peerRegistered (peer2);

		// When
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);
		List<BlockDescriptor> blocks2 = requestManager.allocateRequests (peer2, 16, false);

		byte[] piece = Util.pseudoRandomBlock (0, 262144, 262144);
		int position = 0;
		for (int i = 0; i < 16; i++) {
			BlockDescriptor block = blocks.get (i);
			byte[] blockData = new byte[16384];
			System.arraycopy (piece, position, blockData, 0, 16384);
			position += 16384;
			requestManager.handleBlock (peer, block, null, null, ByteBuffer.wrap (blockData));
		}
		requestManager.setPieceNotNeeded (0);

		assertEquals (16, blocks.size());
		assertArrayEquals (blocks.toArray(), blocks2.toArray());
		verify (peer, never()).cancelRequests (any (List.class));
		verify (peer2).cancelRequests (peer2captor.capture());
		assertEquals (16, peer2captor.getValue().size());

	}


	/**
	 * On abort : One peer gets an allocation and partially completes it then aborts ; A second peer
	 * gets the partially completes allocation as its first request
	 *
	 * @throws Exception
	 */
	@Test
	public void testPieceHandlingAbort() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManagerListener listener = mock (RequestManagerListener.class);
		RequestManager requestManager = new DefaultRequestManager (descriptor, listener);
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);

		BitField peer2BitField = new BitField (1);
		peer2BitField.set (0);
		ManageablePeer peer2 = mockManageablePeer (descriptor, peer2BitField);

		// When
		requestManager.peerRegistered (peer);
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);

		byte[] piece = Util.pseudoRandomBlock (0, 262144, 262144);
		int position = 0;
		for (int i = 0; i < 8; i++) {
			BlockDescriptor block = blocks.get (i);
			byte[] blockData = new byte[16384];
			System.arraycopy (piece, position, blockData, 0, 16384);
			position += 16384;
			requestManager.handleBlock (peer, block, null, null, ByteBuffer.wrap (blockData));
		}

		requestManager.peerDeregistered (peer);

		requestManager.peerRegistered (peer2);

		List<BlockDescriptor> blocks2 = requestManager.allocateRequests (peer2, 16, false);
		position = 131072;
		for (int i = 0; i < 8; i++) {
			BlockDescriptor block = blocks2.get (i);
			byte[] blockData = new byte[16384];
			System.arraycopy (piece, position, blockData, 0, 16384);
			position += 16384;
			requestManager.handleBlock (peer2, block, null, null, ByteBuffer.wrap (blockData));
		}

		// Then
		assertEquals (16, blocks.size());
		assertEquals (8, blocks2.size());

	}


	/**
	 * Tests that after a piece is completed, no orphaned allocation is reused
	 * 
	 * Initial state:
	 *   A 1 piece database with 0 pieces present
	 * Sequence:
	 *   Peer 1 allocates - receives piece 1
	 *   Peer 2 allocates - receives piece 1
	 *   Peer 1 aborts (leaving an orphaned allocation)
	 *   Peer 2 completes piece 1
	 *   Peer 3 allocates - must not receive piece 1
	 *
	 * @throws Exception
	 */
	@Test
	public void testPieceHandlingAbortDestroysOrphan() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManagerListener listener = mock (RequestManagerListener.class);
		RequestManager requestManager = new DefaultRequestManager (descriptor, listener);
		requestManager.setNeededPieces (neededBitField);

		// Register 3 peers
		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		BitField peer2BitField = new BitField (1);
		peer2BitField.set (0);
		ManageablePeer peer2 = mockManageablePeer (descriptor, peer2BitField);
		BitField peer3BitField = new BitField (1);
		peer3BitField.set (0);
		ManageablePeer peer3 = mockManageablePeer (descriptor, peer3BitField);

		// When
		requestManager.peerRegistered (peer);
		requestManager.peerRegistered (peer2);
		requestManager.peerRegistered (peer3);

		// Allocation for peer 1 - piece 1
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);

		// Allocation for peer 2 - piece 1
		List<BlockDescriptor> blocks2 = requestManager.allocateRequests (peer2, 16, false);

		// Peer 1 aborts
		requestManager.peerDeregistered (peer);

		// Peer 2 completes piece 1
		byte[] piece = Util.pseudoRandomBlock (0, 262144, 262144);
		int position = 0;
		for (int i = 0; i < 16; i++) {
			BlockDescriptor block = blocks2.get (i);
			byte[] blockData = new byte[16384];
			System.arraycopy (piece, position, blockData, 0, 16384);
			position += 16384;
			requestManager.handleBlock (peer2, block, null, null, ByteBuffer.wrap (blockData));
		}
		requestManager.setPieceNotNeeded (0);

		// Allocation for peer 3
		List<BlockDescriptor> blocks3 = requestManager.allocateRequests (peer3, 16, false);

		// Then
		assertEquals (16, blocks.size());
		assertEquals (16, blocks2.size());
		assertEquals (0, blocks3.size());

	}


	// Needed pieces

	/**
	 * Tests setNeededPieces on a single peer
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSetNeededPieces() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		requestManager.peerRegistered (peer);

		// When
		// Allocation for peer 1 - piece 1
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);
		assertEquals (16, blocks.size());

		// Then
		verify(peer, never()).setWeAreInterested (anyBoolean());
		verify(peer, never()).cancelRequests (any (List.class));

		// When
		// Set the only piece not needed
		requestManager.setNeededPieces (new BitField (1));

		// Then
		verify(peer).setWeAreInterested (anyBoolean());
		verify(peer).cancelRequests(any(List.class));

	}


	/**
	 * Tests setNeededPieces on two peers
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSetNeededPiecesTwoPeers() throws Exception {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize * 2;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(2).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (2);
		peerBitField.set (0);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);

		BitField peer2BitField = new BitField (2);
		peer2BitField.set (1);
		ManageablePeer peer2 = mockManageablePeer (descriptor, peer2BitField);

		requestManager.peerRegistered (peer);
		requestManager.peerRegistered (peer2);

		// When
		// Allocation for peer 1 - piece 1
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);
		// Allocation for peer 2 - piece 2
		List<BlockDescriptor> blocks2 = requestManager.allocateRequests (peer2, 16, false);

		// Then
		assertEquals (16, blocks.size());
		assertEquals (16, blocks2.size());
		verify(peer, never()).setWeAreInterested (anyBoolean());
		verify(peer, never()).cancelRequests (any (List.class));
		verify(peer2, never()).setWeAreInterested (anyBoolean());

		// When
		// Set the only piece not needed
		BitField newNeededPieces = new BitField (2);
		newNeededPieces.set (1);
		requestManager.setNeededPieces (newNeededPieces);

		// Then
		verify(peer).setWeAreInterested (false);
		verify(peer).cancelRequests (any (List.class));
		verify(peer2).setWeAreInterested (true);

	}


	/**
	 * Test allocateRequests(,,true) on a peer with no Allowed Fast pieces
	 */
	@Test
	public void testAllocateAllowedFastRequests1() {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;
		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);
		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		requestManager.peerRegistered (peer);

		// When
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, true);

		// Then
		// Should not allocate
		assertEquals (0, blocks.size());

	}


	/**
	 * Test allocateRequests(,,true) on a peer with one Allowed Fast piece
	 */
	@Test
	public void testAllocateAllowedFastRequests2() {

		// Given
		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		requestManager.peerRegistered (peer);
		requestManager.setPieceAllowedFast (peer, 0);

		// When
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, true);

		// Then
		assertEquals (16, blocks.size());

	}


	/**
	 * Test allocateRequests on a peer with one suggested piece
	 */
	@Test
	public void testAllocateSuggestedPieces() {

		// Given
		int pieceSize = 16384;
		long totalLength = pieceSize * 5;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(5).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (5);
		peerBitField.not();
		ManageablePeer peer = mockManageablePeer (descriptor, peerBitField);
		requestManager.peerRegistered (peer);
		requestManager.setPieceSuggested (peer, 3);

		// When
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 1, false);

		// Then
		assertEquals (1, blocks.size());
		assertEquals (3, blocks.get(0).getPieceNumber());

	}


	/**
	 * Test extend
	 */
	@Test
	public void testExtend() {

		// Given
		int pieceSize = 16384;
		long totalLength = pieceSize * 5;
		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(5).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);

		// When
		requestManager.extend (new StorageDescriptor (pieceSize, pieceSize * 6));
		requestManager.setNeededPieces (new BitField(6).not());

		// Then
		assertEquals (6, requestManager.getNeededPieceCount());

	}


	/**
	 * Test piece allocation with differing remote view : Us:1+768 Them:1+512
	 * @throws Exception
	 */
	@Test
	public void testDifferentRemoteView1p768x1p512() throws Exception {

		// Given
		int pieceSize = 1024;
		long ourTotalLength = pieceSize + 768;
		long theirTotalLength = pieceSize + 512;
		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, ourTotalLength);
		BitField neededBitField = new BitField(2).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);
		BitField peerBitField = new BitField(2).not();
		ManageablePeer peer = mockManageablePeer (new StorageDescriptor (pieceSize, theirTotalLength), peerBitField);
		requestManager.peerRegistered (peer);

		// When
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 2, false);

		// Then
		assertEquals (1, blocks.size());

	}


	/**
	 * Test piece allocation with differing remote view : Us:2+512 Them:2+0
	 * @throws Exception
	 */
	@Test
	public void testDifferentRemoteView2p512x2p0() throws Exception {

		// Given
		int pieceSize = 1024;
		long ourTotalLength = (2 * pieceSize) + 512;
		long theirTotalLength = (2 * pieceSize);
		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, ourTotalLength);
		BitField neededBitField = new BitField(3).not();
		RequestManager requestManager = new DefaultRequestManager (descriptor, mock (RequestManagerListener.class));
		requestManager.setNeededPieces (neededBitField);
		BitField peerBitField = new BitField(2).not();
		ManageablePeer peer = mockManageablePeer (new StorageDescriptor (pieceSize, theirTotalLength), peerBitField);
		requestManager.peerRegistered (peer);

		// When
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 3, false);

		// Then
		assertEquals (2, blocks.size());

	}


}
