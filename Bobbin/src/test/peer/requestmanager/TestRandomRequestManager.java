/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer.requestmanager;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.requestmanager.RandomRequestManager;
import org.itadaki.bobbin.peer.requestmanager.RequestManager;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.StorageDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.counter.StatisticCounter;
import org.junit.Test;

import test.Util;


/**
 * Tests RandomRequestManager
 */
public class TestRandomRequestManager {

	/**
	 * A mock ManageablePeer object for use in testing RandomRequestManager
	 */
	private static class MockManageablePeer implements ManageablePeer {

		/**
		 * The peer's view length
		 */
		private long peerViewLength;

		/**
		 * The peer's available piece bitfield
		 */
		private BitField peerBitField;

		public void cancelRequests (List<BlockDescriptor> requests) {
			fail();
		}

		public boolean getTheyHaveOutstandingRequests() {
			fail();
			return false;
		}

		public void rejectPiece (int pieceNumber) {
			fail();
		}

		public void sendExtensionHandshake (Set<String> extensionsAdded, Set<String> extensionsRemoved, BDictionary extra) {
			fail();
		}

		public void sendExtensionMessage (String identifier, ByteBuffer data) {
			fail();
		};

		public void sendHavePiece (int pieceNumber) {
			fail();
		}

		public void sendKeepaliveOrClose() {
			fail();
		}

		public void sendViewSignature (ViewSignature viewSignature) {
			fail();
		}

		public boolean setWeAreChoking (boolean weAreChokingThem) {
			fail();
			return false;
		}

		public void setWeAreInterested (boolean weAreInterested) {
			fail();
		}

		public PeerID getRemotePeerID() {
			fail();
			return null;
		}

		public InetSocketAddress getRemoteSocketAddress() {
			fail();
			return null;
		}

		public long getRemoteViewLength() {
			return this.peerViewLength;
		}

		public boolean isFastExtensionEnabled() {
			fail();
			return false;
		}

		public boolean isExtensionProtocolEnabled() {
			fail();
			return false;
		}

		public boolean getTheyAreChoking() {
			fail();
			return false;
		}

		public boolean getTheyAreInterested() {
			fail();
			return false;
		}

		public boolean getWeAreChoking() {
			fail();
			return false;
		}

		public boolean getWeAreInterested() {
			fail();
			return false;
		}

		public long getBlockBytesReceived() {
			return 0;
		}

		public long getBlockBytesSent() {
			return 0;
		}

		public StatisticCounter getBlockBytesReceivedCounter() {
			return null;
		}

		public StatisticCounter getBlockBytesSentCounter() {
			return null;
		}

		public long getProtocolBytesReceived() {
			return 0;
		}

		public long getProtocolBytesSent() {
			return 0;
		}

		public int getProtocolBytesReceivedPerSecond() {
			return 0;
		}

		public int getProtocolBytesSentPerSecond() {
			return 0;
		}

		public BitField getRemoteBitField() {
			return this.peerBitField;
		}

		public void close() {
			fail();
		}

		/**
		 * @param peerViewLength The peer's view length
		 * @param peerBitField The peer's available piece bitfield
		 */
		public MockManageablePeer (long peerViewLength, BitField peerBitField) {
			this.peerViewLength = peerViewLength;
			this.peerBitField = peerBitField;
		}

	}


	// Test allocation

	/**
	 * Test piece allocation : We have 1/1 pieces, peer has 0; no allocation
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateHave1Peer0() throws Exception {

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField (1);
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField);
		requestManager.peerRegistered (peer);

		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 1, false);

		assertEquals (0, blocks.size());

	}

	
	/**
	 * Test piece allocation : We have 1/1 pieces, peer has 1; no allocation
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateHave1Peer1() throws Exception {

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField (1);
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField);
		requestManager.peerRegistered (peer);

		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 1, false);

		assertEquals (0, blocks.size());

	}


	/**
	 * Test piece allocation : We have 0 /1 pieces, peer has 0; no allocation
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateHave0Peer0() throws Exception {

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField);
		requestManager.peerRegistered (peer);

		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 1, false);

		assertEquals (0, blocks.size());

	}


	/**
	 * Test piece allocation : We have 0/1 pieces, peer has 1; allocation
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateHave0Peer1() throws Exception {

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField);
		requestManager.peerRegistered (peer);

		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 1, false);

		assertEquals (1, blocks.size());

	}


	/**
	 * Test piece allocation : We have 0/1 pieces, peer A has 1, peer B has 1; double allocation
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllocateHave0PeerA1PeerB1() throws Exception {

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerABitField = new BitField (1);
		peerABitField.set (0);
		ManageablePeer peerA = new MockManageablePeer (totalLength, peerABitField);
		requestManager.peerRegistered (peerA);

		BitField peerBBitField = new BitField (1);
		peerBBitField.set (0);
		ManageablePeer peerB = new MockManageablePeer (totalLength, peerBBitField);
		requestManager.peerRegistered (peerB);

		List<BlockDescriptor> blocksA1 = requestManager.allocateRequests (peerA, 16, false);
		List<BlockDescriptor> blocksA2 = requestManager.allocateRequests (peerA, 16, false);
		List<BlockDescriptor> blocksB1 = requestManager.allocateRequests (peerB, 16, false);
		List<BlockDescriptor> blocksB2 = requestManager.allocateRequests (peerB, 16, false);

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

		int pieceSize = 262144;
		long totalLength = pieceSize * 2;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(2).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerABitField = new BitField (2);
		peerABitField.not();
		ManageablePeer peerA = new MockManageablePeer (totalLength, peerABitField);
		requestManager.peerRegistered (peerA);

		BitField peerBBitField = new BitField (2);
		peerBBitField.not();
		ManageablePeer peerB = new MockManageablePeer (totalLength, peerBBitField);
		requestManager.peerRegistered (peerB);

		List<BlockDescriptor> blocksA1 = requestManager.allocateRequests (peerA, 16, false);
		List<BlockDescriptor> blocksB1 = requestManager.allocateRequests (peerB, 16, false);
		Set<BlockDescriptor> allBlocks = new HashSet<BlockDescriptor>();
		allBlocks.addAll (blocksA1);
		allBlocks.addAll (blocksB1);

		assertEquals (16, blocksA1.size());
		assertEquals (16, blocksB1.size());
		assertEquals (32, allBlocks.size());

		List<BlockDescriptor> blocksA2 = requestManager.allocateRequests (peerA, 16, false);
		List<BlockDescriptor> blocksB2 = requestManager.allocateRequests (peerB, 16, false);

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

		int pieceSize = 262144;
		long totalLength = pieceSize * 16;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(16).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerABitField = new BitField (16);
		peerABitField.not();
		ManageablePeer peerA = new MockManageablePeer (totalLength, peerABitField);
		requestManager.peerRegistered (peerA);

		BitField peerBBitField = new BitField (16);
		peerBBitField.not();
		ManageablePeer peerB = new MockManageablePeer (totalLength, peerBBitField);
		requestManager.peerRegistered (peerB);


		List<BlockDescriptor> blocksA1 = requestManager.allocateRequests (peerA, 16 * 8, false);
		List<BlockDescriptor> blocksB1 = requestManager.allocateRequests (peerB, 16 * 8, false);
		Set<BlockDescriptor> allBlocks = new HashSet<BlockDescriptor>();
		allBlocks.addAll (blocksA1);
		allBlocks.addAll (blocksB1);

		assertEquals (16 * 8, blocksA1.size());
		assertEquals (16 * 8, blocksB1.size());
		assertEquals (32 * 8, allBlocks.size());

		List<BlockDescriptor> blocksA2 = requestManager.allocateRequests (peerA, 16 * 8, false);
		List<BlockDescriptor> blocksB2 = requestManager.allocateRequests (peerB, 16 * 8, false);

		assertEquals (16 * 8, blocksA2.size());
		assertEquals (16 * 8, blocksB2.size());
		assertArrayEquals (blocksA1.toArray(), blocksB2.toArray());
		assertArrayEquals (blocksB1.toArray(), blocksA2.toArray());

	}


	// Test piece handling

	/**
	 * After receiving 1 of 1 pieces, PieceDatabase contains the piece and no more allocations are given
	 *
	 * @throws Exception
	 */
	@Test
	public void testPieceHandlingLastPiece() throws Exception {

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);

		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField) {
			@Override
			public void setWeAreInterested (boolean interested) { }
		};
		requestManager.peerRegistered (peer);

		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);

		assertEquals (16, blocks.size());

		byte[] piece = Util.pseudoRandomBlock (0, 262144, 262144);
		int position = 0;
		for (BlockDescriptor block : blocks) {
			byte[] blockData = new byte[16384];
			System.arraycopy (piece, position, blockData, 0, 16384);
			position += 16384;
			if (requestManager.handleBlock (peer, block, null, null, blockData) != null) {
				requestManager.setPieceNotNeeded (block.getPieceNumber());
			}
		}

		List<BlockDescriptor> blocks2 = requestManager.allocateRequests (peer, 16, false);

		assertEquals (0, blocks2.size());

	}


	/**
	 * On a double allocation, the second peer is sent a cancel on receipt of the piece
	 *
	 * @throws Exception
	 */
	@Test
	public void testDoubleAllocationCancel() throws Exception {

		final List<BlockDescriptor> peer2CancelledRequests = new ArrayList<BlockDescriptor>();

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		BitField peer2BitField = new BitField (1);

		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField) {
			@Override
			public void cancelRequests (List<BlockDescriptor> requests) {
				fail();
			}
			@Override
			public void setWeAreInterested (boolean weAreInterested) {
			}
		};
		ManageablePeer peer2 = new MockManageablePeer (totalLength, peer2BitField) {
			@Override
			public void cancelRequests (List<BlockDescriptor> requests) {
				peer2CancelledRequests.addAll (requests);
			}

			@Override
			public void setWeAreInterested (boolean weAreInterested) {
			}
		};

		peerBitField.set (0);
		requestManager.peerRegistered (peer);

		peer2BitField.set (0);
		requestManager.peerRegistered (peer2);

		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);
		List<BlockDescriptor> blocks2 = requestManager.allocateRequests (peer2, 16, false);

		assertEquals (16, blocks.size());
		assertArrayEquals (blocks.toArray(), blocks2.toArray());

		byte[] piece = Util.pseudoRandomBlock (0, 262144, 262144);
		int position = 0;
		for (int i = 0; i < 16; i++) {
			BlockDescriptor block = blocks.get (i);
			byte[] blockData = new byte[16384];
			System.arraycopy (piece, position, blockData, 0, 16384);
			position += 16384;
			if (requestManager.handleBlock (peer, block, null, null, blockData) != null) {
				requestManager.setPieceNotNeeded (block.getPieceNumber());
			}
		}

		assertTrue (peer2CancelledRequests.size() > 0);

	}


	/**
	 * On abort : One peer gets an allocation and partially completes it then aborts ; A second peer
	 * gets the partially completes allocation as its first request
	 *
	 * @throws Exception
	 */
	@Test
	public void testPieceHandlingAbort() throws Exception {

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField);
		requestManager.peerRegistered (peer);

		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);

		assertEquals (16, blocks.size());

		byte[] piece = Util.pseudoRandomBlock (0, 262144, 262144);
		int position = 0;
		for (int i = 0; i < 8; i++) {
			BlockDescriptor block = blocks.get (i);
			byte[] blockData = new byte[16384];
			System.arraycopy (piece, position, blockData, 0, 16384);
			position += 16384;
			requestManager.handleBlock (peer, block, null, null, blockData);
		}

		requestManager.peerDeregistered (peer);

		BitField peer2BitField = new BitField (1);
		peer2BitField.set (0);
		ManageablePeer peer2 = new MockManageablePeer (totalLength, peer2BitField) {
			@Override
			public void setWeAreInterested (boolean weAreInterested) { }
		};
		requestManager.peerRegistered (peer2);

		List<BlockDescriptor> blocks2 = requestManager.allocateRequests (peer2, 16, false);
		position = 131072;
		for (int i = 0; i < 8; i++) {
			BlockDescriptor block = blocks2.get (i);
			byte[] blockData = new byte[16384];
			System.arraycopy (piece, position, blockData, 0, 16384);
			position += 16384;
			if (requestManager.handleBlock (peer2, block, null, null, blockData) != null) {
				requestManager.setPieceNotNeeded (block.getPieceNumber());
			}

		}

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

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		// Register 3 peers
		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField);
		requestManager.peerRegistered (peer);

		BitField peer2BitField = new BitField (1);
		peer2BitField.set (0);
		ManageablePeer peer2 = new MockManageablePeer (totalLength, peer2BitField) {
			@Override
			public void setWeAreInterested (boolean weAreInterested) {
			}
		};
		requestManager.peerRegistered (peer2);

		BitField peer3BitField = new BitField (1);
		peer3BitField.set (0);
		ManageablePeer peer3 = new MockManageablePeer (totalLength, peer3BitField) {
			@Override
			public void setWeAreInterested (boolean weAreInterested) {
			}
		};
		requestManager.peerRegistered (peer3);

		// Allocation for peer 1 - piece 1
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);
		assertEquals (16, blocks.size());

		// Allocation for peer 2 - piece 1
		List<BlockDescriptor> blocks2 = requestManager.allocateRequests (peer2, 16, false);
		assertEquals (16, blocks2.size());

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
			if (requestManager.handleBlock (peer2, block, null, null, blockData) != null) {
				requestManager.setPieceNotNeeded (block.getPieceNumber());
			}
		}

		// Allocation for peer 3
		List<BlockDescriptor> blocks3 = requestManager.allocateRequests (peer3, 16, false);

		assertEquals (0, blocks3.size());

	}


	// Needed pieces

	/**
	 * Tests setNeededPieces on a single peer
	 * @throws Exception 
	 */
	@Test
	public void testSetNeededPieces() throws Exception {

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		final Boolean[] interestSet = new Boolean[1];
		final boolean[] cancelRequestsCalled = new boolean[1];

		// Register a peer
		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField) {
			@Override
			public void setWeAreInterested (boolean weAreInterested) {
				interestSet[0] = weAreInterested;
			}
			@Override
			public void cancelRequests(List<BlockDescriptor> requests) {
				cancelRequestsCalled[0] = true;
			}

		};
		requestManager.peerRegistered (peer);

		// Allocation for peer 1 - piece 1
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);
		assertEquals (16, blocks.size());

		assertNull (interestSet[0]);
		assertFalse (cancelRequestsCalled[0]);

		// Set the only piece not needed
		requestManager.setNeededPieces (new BitField (1));

		assertFalse (interestSet[0]);
		assertTrue (cancelRequestsCalled[0]);

	}


	/**
	 * Tests setNeededPieces on two peers
	 * @throws Exception 
	 */
	@Test
	public void testSetNeededPiecesTwoPeers() throws Exception {

		int pieceSize = 262144;
		long totalLength = pieceSize * 2;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(2).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		final Boolean[] peer1InterestSet = new Boolean[1];
		final boolean[] peer1CancelRequestsCalled = new boolean[1];
		final Boolean[] peer2InterestSet = new Boolean[1];

		// Register two peers
		BitField peerBitField = new BitField (2);
		peerBitField.set (0);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField) {
			@Override
			public void setWeAreInterested (boolean weAreInterested) {
				peer1InterestSet[0] = weAreInterested;
			}
			@Override
			public void cancelRequests(List<BlockDescriptor> requests) {
				peer1CancelRequestsCalled[0] = true;
			}

		};
		requestManager.peerRegistered (peer);

		BitField peer2BitField = new BitField (2);
		peer2BitField.set (1);
		ManageablePeer peer2 = new MockManageablePeer (totalLength, peer2BitField) {
			@Override
			public void setWeAreInterested (boolean weAreInterested) {
				peer2InterestSet[0] = weAreInterested;
			}
		};
		requestManager.peerRegistered (peer2);

		// Allocation for peer 1 - piece 1
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, false);
		assertEquals (16, blocks.size());

		// Allocation for peer 2 - piece 2
		List<BlockDescriptor> blocks2 = requestManager.allocateRequests (peer2, 16, false);
		assertEquals (16, blocks2.size());

		assertNull (peer1InterestSet[0]);
		assertFalse (peer1CancelRequestsCalled[0]);
		assertNull (peer2InterestSet[0]);

		// Set the only piece not needed
		BitField newNeededPieces = new BitField (2);
		newNeededPieces.set (1);
		requestManager.setNeededPieces (newNeededPieces);

		assertFalse (peer1InterestSet[0]);
		assertTrue (peer1CancelRequestsCalled[0]);
		assertTrue (peer2InterestSet[0]);

	}


	/**
	 * Test allocateRequests(,,true) on a peer with no Allowed Fast pieces
	 */
	@Test
	public void testAllocateAllowedFastRequests1() {

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		// Register a peer
		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField);
		requestManager.peerRegistered (peer);

		// Should not allocate
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, true);
		assertEquals (0, blocks.size());

	}


	/**
	 * Test allocateRequests(,,true) on a peer with one Allowed Fast piece
	 */
	@Test
	public void testAllocateAllowedFastRequests2() {

		int pieceSize = 262144;
		long totalLength = pieceSize;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(1).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		// Register a peer
		BitField peerBitField = new BitField (1);
		peerBitField.set (0);
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField);
		requestManager.peerRegistered (peer);
		requestManager.setPieceAllowedFast (peer, 0);

		// Allocation for peer 1 - piece 1
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 16, true);
		assertEquals (16, blocks.size());

	}


	/**
	 * Test allocateRequests on a peer with one suggested piece
	 */
	@Test
	public void testAllocateSuggestedPieces() {

		int pieceSize = 16384;
		long totalLength = pieceSize * 5;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(5).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		// Register a peer
		BitField peerBitField = new BitField (5);
		peerBitField.not();
		ManageablePeer peer = new MockManageablePeer (totalLength, peerBitField);
		requestManager.peerRegistered (peer);
		requestManager.setPieceSuggested (peer, 3);

		// Should allocate piece 3
		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 1, false);
		assertEquals (1, blocks.size());
		assertEquals (3, blocks.get(0).getPieceNumber());

	}


	/**
	 * Test extend
	 */
	@Test
	public void testExtend() {

		int pieceSize = 16384;
		long totalLength = pieceSize * 5;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, totalLength);
		BitField neededBitField = new BitField(5).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		requestManager.extend (new StorageDescriptor (pieceSize, pieceSize * 6));
		requestManager.setNeededPieces (new BitField(6).not());

		assertEquals (6, requestManager.getNeededPieceCount());

	}


	/**
	 * Test piece allocation with differing remote view : Us:1+768 Them:1+512
	 * @throws Exception
	 */
	@Test
	public void testDifferentRemoteView1p768x1p512() throws Exception {

		int pieceSize = 1024;
		long ourTotalLength = pieceSize + 768;
		long theirTotalLength = pieceSize + 512;

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, ourTotalLength);
		BitField neededBitField = new BitField(2).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField(2).not();
		ManageablePeer peer = new MockManageablePeer (theirTotalLength, peerBitField);
		requestManager.peerRegistered (peer);

		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 2, false);

		assertEquals (1, blocks.size());

	}


	/**
	 * Test piece allocation with differing remote view : Us:2+512 Them:2+0
	 * @throws Exception
	 */
	@Test
	public void testDifferentRemoteView2p512x2p0() throws Exception {

		int pieceSize = 1024;
		long ourTotalLength = (2 * pieceSize) + 512;
		long theirTotalLength = (2 * pieceSize);

		StorageDescriptor descriptor = new StorageDescriptor (pieceSize, ourTotalLength);
		BitField neededBitField = new BitField(3).not();
		RequestManager requestManager = new RandomRequestManager (descriptor);
		requestManager.setNeededPieces (neededBitField);

		BitField peerBitField = new BitField(2).not();
		ManageablePeer peer = new MockManageablePeer (theirTotalLength, peerBitField);
		requestManager.peerRegistered (peer);

		List<BlockDescriptor> blocks = requestManager.allocateRequests (peer, 3, false);

		assertEquals (2, blocks.size());

	}


}
