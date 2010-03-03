/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.ExtensiblePeer;
import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.PeerHandler;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.PeerServices;
import org.itadaki.bobbin.peer.PeerServicesProvider;
import org.itadaki.bobbin.peer.PeerStatistics;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConstants;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.Info;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.MemoryStorage;
import org.itadaki.bobbin.torrentdb.Piece;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.Storage;
import org.itadaki.bobbin.torrentdb.StorageDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.elastictree.ElasticTree;
import org.itadaki.bobbin.util.elastictree.HashChain;
import org.junit.Test;

import test.Util;
import test.torrentdb.MockPieceDatabase;


/**
 * Tests PeerHandler
 */
public class TestPeerHandler {

	/**
	 * Test initial state
	 * @throws Exception 
	 */
	@Test
	public void testInitialState() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase);

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		assertFalse (handler.getWeAreInterested());
		assertTrue (handler.getWeAreChoking());
		assertFalse (handler.getTheyAreInterested());
		assertTrue (handler.getTheyAreChoking());
		assertEquals (0, handler.getReadableStatistics().getReadableCounter (PeerStatistics.Type.BLOCK_BYTES_RECEIVED_RAW).getTotal());
		assertEquals (0, handler.getReadableStatistics().getReadableCounter (PeerStatistics.Type.BLOCK_BYTES_SENT).getTotal());
		assertEquals (0, handler.getReadableStatistics().getReadableCounter (PeerStatistics.Type.PROTOCOL_BYTES_RECEIVED).getTotal());
		assertEquals (0, handler.getReadableStatistics().getReadableCounter (PeerStatistics.Type.PROTOCOL_BYTES_SENT).getTotal());
		assertEquals (false, handler.getTheyHaveOutstandingRequests());
		assertEquals (new BitField (1), handler.getRemoteBitField());

	}


	/**
	 * Test getTheyAreChoking()
	 * @throws Exception 
	 */
	@Test
	public void testGetTheyAreChoking1() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// Remote peer sends us an unchoke
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, false);

		assertFalse (handler.getTheyAreChoking());

	}


	/**
	 * Test getTheyAreChoking()
	 * @throws Exception 
	 */
	@Test
	public void testGetTheyAreChoking2() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// Remote peer sends us an unchoke then a choke
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		mockConnection.mockInput (PeerProtocolBuilder.chokeMessage());
		handler.connectionReady (mockConnection, true, false);

		assertTrue (handler.getTheyAreChoking());

	}


	/**
	 * Test getTheyAreInterested()
	 * @throws Exception
	 */
	@Test
	public void testGetTheyAreInterested1() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public void adjustChoking (boolean opportunistic) {
				assertTrue (opportunistic);
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// Remote peer sends us an interested message
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.interestedMessage());
		handler.connectionReady (mockConnection, true, false);

		assertTrue (handler.getTheyAreInterested());

	}


	/**
	 * Test getTheyAreInterested()
	 * @throws Exception
	 */
	@Test
	public void testGetTheyAreInterested2() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public void adjustChoking (boolean opportunistic) {
				assertTrue (opportunistic);
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// Remote peer sends us an interested message then a not interested message
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockInput (PeerProtocolBuilder.notInterestedMessage());
		handler.connectionReady (mockConnection, true, false);

		assertFalse (handler.getTheyAreInterested());

	}


	/**
	 * Test getBlockBytesReceived()
	 * @throws Exception 
	 */
	@Test
	public void testGetBlockBytesReceived() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return true;
			}
			@Override
			public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
				return Arrays.asList (new BlockDescriptor[] {
						new BlockDescriptor (0, 0, 16384)
				});
			}
			@Override
			public void handleBlock (ManageablePeer peer, BlockDescriptor request, ViewSignature viewSignature, HashChain hashChain, byte[] block) {
				// Expected
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// We send remote peer a request
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (new BlockDescriptor (0, 0, 16384)));
		mockConnection.mockExpectNoMoreOutput();

		// Remote peer sends us a block
		mockConnection.mockInput (PeerProtocolBuilder.pieceMessage (new BlockDescriptor (0, 0, 16384), ByteBuffer.allocate (16384)));
		handler.connectionReady (mockConnection, true, false);

		mockConnection.mockExpectNoMoreOutput();
		assertEquals (16384, handler.getReadableStatistics().getTotal (PeerStatistics.Type.BLOCK_BYTES_RECEIVED_RAW));


		pieceDatabase.terminate (true);

	}


	/**
	 * Test getBlockBytesSent()
	 * @throws Exception 
	 */
	@Test
	public void testGetBlockBytesSent() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		BlockDescriptor request = new BlockDescriptor (0, 0, 16384);

		// Initial handshake
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.bitfieldMessage (pieceDatabase.getPresentPieces()));
		mockConnection.mockExpectNoMoreOutput();

		// Remote peer sends us a request, we send remote peer a block
		handler.setWeAreChoking (false);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (request));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.unchokeMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.pieceMessage (
				request,
				pieceDatabase.readPiece (request.getPieceNumber()).getBlock (request)
		));
		mockConnection.mockExpectNoMoreOutput();
		assertEquals (16384, handler.getReadableStatistics().getReadableCounter (PeerStatistics.Type.BLOCK_BYTES_SENT).getTotal());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test getProtocolBytesReceived()
	 * @throws Exception 
	 */
	@Test
	public void testGetProtocolBytesReceived() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return true;
			}
			@Override
			public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
				return Arrays.asList (new BlockDescriptor[] {
						new BlockDescriptor (0, 0, 16384)
				});
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// Remote peer sends us a block; protocol counter measures total bytes including the block
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, false);
		mockConnection.mockInput (PeerProtocolBuilder.pieceMessage (new BlockDescriptor (0, 0, 16384), ByteBuffer.allocate (16384)));
		handler.connectionReady (mockConnection, true, false);

		assertEquals (16476, handler.getReadableStatistics().getReadableCounter (PeerStatistics.Type.PROTOCOL_BYTES_RECEIVED).getTotal());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test getProtocolBytesSent()
	 * @throws Exception 
	 */
	@Test
	public void testGetProtocolBytesSent() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// We send the remote peer a block; protocol counter measures total bytes including the block
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);
		handler.setWeAreChoking (false);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (new BlockDescriptor (0, 0, 16384)));
		handler.connectionReady (mockConnection, true, true);

		assertEquals (16476, handler.getReadableStatistics().getReadableCounter (PeerStatistics.Type.PROTOCOL_BYTES_SENT).getTotal());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test getTheyHaveOutstandingRequests
	 * @throws Exception 
	 */
	@Test
	public void testGetTheyHaveOutstandingRequests() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// The remote peer requests a block but we don't send it
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);
		handler.setWeAreChoking (false);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (new BlockDescriptor (0, 0, 16384)));
		handler.connectionReady (mockConnection, true, false);

		assertTrue (handler.getTheyHaveOutstandingRequests());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test setWeAreChoking
	 * @throws Exception 
	 */
	@Test
	public void testSetWeAreChoking() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// The remote peer requests a block but we discard the request by choking them
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);
		handler.setWeAreChoking (false);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (new BlockDescriptor (0, 0, 16384)));
		handler.connectionReady (mockConnection, true, false);

		assertFalse (handler.getWeAreChoking());

		handler.setWeAreChoking (true);

		assertTrue (handler.getWeAreChoking());
		assertFalse (handler.getTheyHaveOutstandingRequests());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test setWeAreInterested
	 * @throws Exception 
	 */
	@Test
	public void testSetWeAreInterested() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// Set up basic connection and test interested false -> true
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		assertFalse (handler.getWeAreInterested());

		handler.setWeAreInterested (true);

		assertTrue (handler.getWeAreInterested());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test cancelRequests
	 * @throws Exception 
	 */
	@Test
	public void testCancelRequests1() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			private int sequence = 0;
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return true;
			}
			@Override
			public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
				if (this.sequence++ == 0) {
					return Arrays.asList (new BlockDescriptor[] {
							new BlockDescriptor (0, 0, 16384)
					});
				}
				return new ArrayList<BlockDescriptor>();
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// We make a request but cancel it before it is sent
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, false);
		handler.cancelRequests (Arrays.asList (new BlockDescriptor[] {new BlockDescriptor (0, 0, 16384)}));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Test cancelRequests
	 * @throws Exception 
	 */
	@Test
	public void testCancelRequests2() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			private int sequence = 0;
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return true;
			}
			@Override
			public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
				if (this.sequence++ == 0) {
					return Arrays.asList (new BlockDescriptor[] {
							new BlockDescriptor (0, 0, 16384)
					});
				}
				return new ArrayList<BlockDescriptor>();
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		BlockDescriptor request = new BlockDescriptor (0, 0, 16384);

		// We make a request and cancel it after it has been sent
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (request));
		mockConnection.mockExpectNoMoreOutput();

		// Request has already been sent, so a cancel message will result
		handler.cancelRequests (Arrays.asList (new BlockDescriptor[] {request}));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.notInterestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.cancelMessage (request));
		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Test sendHavePiece
	 * @throws Exception 
	 */
	@Test
	public void testSendHavePiece() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return true;
			}
			@Override
			public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
				return new ArrayList<BlockDescriptor>();
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();

		// Send a have message
		handler.sendHavePiece (0);
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveMessage (0));
		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Test getRemoteBitField
	 * @throws Exception 
	 */
	@Test
	public void testGetRemoteBitField1() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		handler.connectionReady (mockConnection, true, false);

		// Remote bitfield is all zero until set
		assertEquals (new BitField (1), handler.getRemoteBitField());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test getRemoteBitField
	 * @throws Exception 
	 */
	@Test
	public void testGetRemoteBitField2() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return true;
			}
			@Override
			public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
				return new ArrayList<BlockDescriptor>();
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, false);

		// Remote bitfield has sent value after bitfield message received
		assertEquals (new BitField (1).not(), handler.getRemoteBitField());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test handshakeInfoHash on an outbound connection where the remote peer responds with the
	 * wrong hash
	 * @throws Exception 
	 */
	@Test
	public void testHandshakeInfoHashOutboundWrongHash() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		final boolean[] peerDisconnectedCalled = new boolean[1];
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public void peerDisconnected (ManageablePeer peer) {
				peerDisconnectedCalled[0] = true;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		handler.connectionReady (mockConnection, false, true);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, new InfoHash (new byte[20]), new PeerID()));

		assertFalse (peerDisconnectedCalled[0]);

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (peerDisconnectedCalled[0]);


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests behaviour in response to an unknown info hash passed to a PeerHandler constructed for
	 * inbound use
	 * @throws Exception
	 */
	@Test
	public void testHandshakeInfoHashInboundUnknownHash() throws Exception {

		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return null;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, new InfoHash (new byte[20]), new PeerID()));

		assertTrue (mockConnection.isOpen());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectNoMoreOutput();
		assertFalse (mockConnection.isOpen());

	}


	/**
	 * Tests behaviour in response to a known info hash passed to a PeerHandler constructed for
	 * inbound use
	 * @throws Exception
	 */
	@Test
	public void testHandshakeInfoHashInboundKnownHash() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));

		assertTrue (mockConnection.isOpen());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests getRemoteSocketAddress
	 * @throws Exception
	 */
	@Test
	public void testGetRemoteSocketAddress() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase);

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		assertEquals ("1.2.3.4", handler.getRemoteSocketAddress().getAddress().getHostAddress());
		assertEquals (5678, handler.getRemoteSocketAddress().getPort());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests getRemotePeerID
	 * @throws Exception
	 */
	@Test
	public void testGetRemotePeerID() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected(ManageablePeer peer) { return true; }
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		PeerID remotePeerID = new PeerID();
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), remotePeerID));

		handler.connectionReady (mockConnection, true, false);

		assertEquals (remotePeerID, handler.getRemotePeerID());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests getBlockBytesReceivedCounter in its initial state
	 * @throws Exception
	 */
	@Test
	public void testGetBlockBytesReceivedCounter() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected(ManageablePeer peer) { return true; }
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		PeerID remotePeerID = new PeerID();
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), remotePeerID));

		handler.connectionReady (mockConnection, true, false);

		assertEquals (0, handler.getStatistics().getCounter(PeerStatistics.Type.BLOCK_BYTES_RECEIVED_RAW).getTotal());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests getBlockBytesSentCounter in its initial state
	 * @throws Exception
	 */
	@Test
	public void testGetBlockBytesSentCounter() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected(ManageablePeer peer) { return true; }
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		PeerID remotePeerID = new PeerID();
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), remotePeerID));

		handler.connectionReady (mockConnection, true, false);

		assertEquals (0, handler.getStatistics().getCounter(PeerStatistics.Type.BLOCK_BYTES_SENT).getTotal());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests that an explicit rejection is sent when we choke with the Fast extension enabled
	 * @throws Exception
	 */
	@Test
	public void testFastSetWeAreChoking() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("111111111111111", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		// The remote peer requests a block but we discard the request by choking them
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (1));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (2));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (3));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (4));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (6));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (9));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (10));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (12));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (13));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (14));
		mockConnection.mockExpectNoMoreOutput();

		handler.setWeAreChoking (false);
		BlockDescriptor sentRequest = new BlockDescriptor (0, 0, 16384); // Not an Allowed Fast piece
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (sentRequest));

		handler.connectionReady (mockConnection, true, false);

		assertFalse (handler.getWeAreChoking());

		handler.setWeAreChoking (true);
		handler.connectionReady (mockConnection, false, true);

		// Because the Fast extension is enabled, we must send an explicit reject for their request
		mockConnection.mockExpectOutput (PeerProtocolBuilder.unchokeMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.chokeMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.rejectRequestMessage (sentRequest));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (handler.getWeAreChoking());
		assertFalse (handler.getTheyHaveOutstandingRequests());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test that a Have None message is sent when appropriate, when the Fast extension is enabled
	 * @throws Exception
	 */
	@Test
	public void testFastHandshakePeerIDHaveNone() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (0));
		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Test that a Have All message is sent when appropriate, when the Fast extension is enabled
	 * @throws Exception
	 */
	@Test
	public void testFastHandshakePeerIDHaveAll() throws Exception {


		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (0));
		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Test that a Bitfield message is sent when appropriate, when the Fast extension is enabled
	 * @throws Exception
	 */
	@Test
	public void testFastHandshakePeerIDBitfield() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("10", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.bitfieldMessage (pieceDatabase.getPresentPieces()));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (0));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (1));
		mockConnection.mockExpectNoMoreOutput();



		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a Bitfield message with the Fast extension enabled
	 * @throws Exception
	 */
	@Test
	public void testFastBitfieldMessage() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("00000", 16384);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		pieceDatabase.start (true);

		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
					@Override
					public boolean addAvailablePieces (ManageablePeer peer) {
						return true;
					}
					@Override
					public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
						return new ArrayList<BlockDescriptor>();
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		// Send Have All message
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (0));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (1));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (2));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (3));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (4));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a request for a non Allowed Fast block while choked, with the Fast
	 * extension enabled
	 * @throws Exception
	 */
	@Test
	public void testFastRequestMessageChokedNotAllowedFast() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("111111111111111", 16384);
		pieceDatabase.start (true);

		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);

		// Handshake
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (1));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (2));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (3));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (4));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (6));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (9));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (10));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (12));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (13));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (14));
		mockConnection.mockExpectNoMoreOutput();

		// Request a non Allowed Fast piece while choked
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (requestDescriptor));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.rejectRequestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a request for an Allowed Fast block while choked, with the Fast
	 * extension enabled
	 * @throws Exception
	 */
	@Test
	public void testFastRequestMessageChokedAllowedFast() throws Exception {


		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);

		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		BlockDescriptor request = new BlockDescriptor (0, 0, 16384);

		// Handshake
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (0));
		mockConnection.mockExpectNoMoreOutput();

		// Request an Allowed Fast piece while choked
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (request));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.pieceMessage (
				request,
				pieceDatabase.readPiece (request.getPieceNumber()).getBlock (request)
		));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a request message that is Allowed Fast but over the threshold for the
	 * peer, while choked, with the Fast extension enabled
	 * @throws Exception
	 */
	@Test
	public void testFastRequestMessageChokedAllowedFastOverLimit() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("111111111111111", 16384);
		pieceDatabase.start (true);

		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
					@Override
					public boolean addAvailablePiece (ManageablePeer peer, int pieceNumber) {
						return false;
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		BlockDescriptor request = new BlockDescriptor (14, 0, 16384);

		// Handshake
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (1));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (2));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (3));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (4));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (6));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (9));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (10));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (12));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (13));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (14));
		mockConnection.mockExpectNoMoreOutput();

		// Request an Allowed Fast piece while choked but over the allowed piece count
		mockConnection.mockInput (PeerProtocolBuilder.haveMessage (0));
		mockConnection.mockInput (PeerProtocolBuilder.haveMessage (1));
		mockConnection.mockInput (PeerProtocolBuilder.haveMessage (2));
		mockConnection.mockInput (PeerProtocolBuilder.haveMessage (3));
		mockConnection.mockInput (PeerProtocolBuilder.haveMessage (4));
		mockConnection.mockInput (PeerProtocolBuilder.haveMessage (5));
		mockConnection.mockInput (PeerProtocolBuilder.haveMessage (6));
		mockConnection.mockInput (PeerProtocolBuilder.haveMessage (7));
		mockConnection.mockInput (PeerProtocolBuilder.haveMessage (8));
		mockConnection.mockInput (PeerProtocolBuilder.haveMessage (9));
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (request));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.rejectRequestMessage (request));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to an unrequested piece message with the Fast extension enabled
	 * @throws Exception
	 */
	@Test
	public void testFastPieceMessageUnrequested() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		final boolean[] peerDisconnectedCalled = new boolean[1];
		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
					@Override
					public boolean addAvailablePieces (ManageablePeer peer) {
						return true;
					}
					@Override
					public void peerDisconnected (ManageablePeer peer) {
						peerDisconnectedCalled[0] = true;
					}
					@Override
					public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
						return new ArrayList<BlockDescriptor>();
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());

		assertTrue (mockConnection.isOpen());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockInput (PeerProtocolBuilder.pieceMessage (requestDescriptor, ByteBuffer.allocate (16384)));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (peerDisconnectedCalled[0]);
		assertFalse (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a cancel message with the Fast extension enabled
	 * @throws Exception
	 */
	@Test
	public void testFastCancelMessage() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);

		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());

		assertTrue (mockConnection.isOpen());

		handler.connectionReady (mockConnection, true, false);

		handler.setWeAreChoking (false);

		handler.connectionReady (mockConnection, false, true);

		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockInput (PeerProtocolBuilder.cancelMessage (requestDescriptor));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (0));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.unchokeMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.rejectRequestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a have all message
	 * @throws Exception
	 */
	@Test
	public void testFastHaveAllMessage() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("00000", 16384);
		pieceDatabase.start (true);

		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
					@Override
					public boolean addAvailablePieces (ManageablePeer peer) {
						return true;
					}
					@Override
					public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
						return new ArrayList<BlockDescriptor>();
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		// Send Have All message
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());
		assertEquals (5, handler.getRemoteBitField().cardinality());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a have none message
	 * @throws Exception
	 */
	@Test
	public void testFastHaveNoneMessage() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("00000", 16384);
		pieceDatabase.start (true);

		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
					@Override
					public boolean addAvailablePieces (ManageablePeer peer) {
						return true;
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		// Send Have All message
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (0));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (1));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (2));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (3));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (4));
		mockConnection.mockExpectNoMoreOutput();

		assertTrue (mockConnection.isOpen());
		assertEquals (0, handler.getRemoteBitField().cardinality());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a reject request message
	 * @throws Exception
	 */
	@Test
	public void testFastRejectRequestMessage() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		final BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);

		final int[] getRequestSequence = new int[] { 0 };
		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
					@Override
					public boolean addAvailablePieces (ManageablePeer peer) {
						return true;
					}
					@Override
					public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
						if (getRequestSequence[0]++ == 0) {
							return new ArrayList<BlockDescriptor> (Arrays.asList (new BlockDescriptor[] {requestDescriptor}));
						}
						return new ArrayList<BlockDescriptor>();
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		// Allow PeerHandler to make a request
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());

		// Choke and reject the request
		mockConnection.mockInput (PeerProtocolBuilder.chokeMessage());
		mockConnection.mockInput (PeerProtocolBuilder.rejectRequestMessage (requestDescriptor));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());

		// Unchoke - request should be resent
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a reject request message for a request that was never issued
	 * @throws Exception
	 */
	@Test
	public void testFastRejectRequestMessageInvalid1() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		final BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);

		final int[] getRequestSequence = new int[] { 0 };
		final boolean[] peerDisconnectedCalled = new boolean[1];
		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
					@Override
					public boolean addAvailablePieces (ManageablePeer peer) {
						return true;
					}
					@Override
					public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
						if (getRequestSequence[0]++ == 0) {
							return new ArrayList<BlockDescriptor> (Arrays.asList (new BlockDescriptor[] {requestDescriptor}));
						}
						return new ArrayList<BlockDescriptor>();
					}
					@Override
					public void peerDisconnected (ManageablePeer peer) {
						peerDisconnectedCalled[0] = true;
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		// Allow PeerHandler to make a request
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());

		// Choke and reject an invalid request
		mockConnection.mockInput (PeerProtocolBuilder.chokeMessage());
		mockConnection.mockInput (PeerProtocolBuilder.rejectRequestMessage (new BlockDescriptor (0, 16384, 16384)));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectNoMoreOutput();
		assertTrue (peerDisconnectedCalled[0]);
		assertFalse (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a reject request message for an already rejected request
	 * @throws Exception
	 */
	@Test
	public void testFastRejectRequestMessageInvalid2() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		final BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);

		final int[] getRequestSequence = new int[] { 0 };
		final boolean[] peerDisconnectedCalled = new boolean[1];
		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
					@Override
					public boolean addAvailablePieces (ManageablePeer peer) {
						return true;
					}
					@Override
					public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
						if (getRequestSequence[0]++ == 0) {
							return new ArrayList<BlockDescriptor> (Arrays.asList (new BlockDescriptor[] {requestDescriptor}));
						}
						return new ArrayList<BlockDescriptor>();
					}
					@Override
					public void peerDisconnected (ManageablePeer peer) {
						peerDisconnectedCalled[0] = true;
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		// Allow PeerHandler to make a request
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());

		// Choke and reject the request
		mockConnection.mockInput (PeerProtocolBuilder.chokeMessage());
		mockConnection.mockInput (PeerProtocolBuilder.rejectRequestMessage (requestDescriptor));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());

		// Re-reject the request
		mockConnection.mockInput (PeerProtocolBuilder.rejectRequestMessage (requestDescriptor));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectNoMoreOutput();
		assertTrue (peerDisconnectedCalled[0]);
		assertFalse (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}



	/**
	 * Test that an Allowed Fast message while choked leads to a request
	 * @throws Exception
	 */
	@Test
	public void testFastAllowedFastMessage() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("00000", 16384);
		pieceDatabase.start (true);

		final BlockDescriptor requestDescriptor = new BlockDescriptor (2, 0, 16384);
		final boolean[] setPieceAllowedFastCalled = new boolean[1];
		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
					@Override
					public boolean addAvailablePieces (ManageablePeer peer) {
						return true;
					}
					@Override
					public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
						if (!allowedFastOnly) {
							fail();
						}
						return new ArrayList<BlockDescriptor> (Arrays.asList (new BlockDescriptor[] {requestDescriptor}));
					}
					@Override
					public void setPieceAllowedFast (ManageablePeer peer, int pieceNumber) {
						setPieceAllowedFastCalled[0] = true;
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		// Allow PeerHandler to make a request for an Allowed Fast piece while choked
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.allowedFastMessage (2));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (setPieceAllowedFastCalled[0]);
		assertTrue (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test that an Allowed Fast message while choked leads to a request
	 * @throws Exception
	 */
	@Test
	public void testFastSuggestPieceMessage() throws Exception {

		final PeerID localPeerID = new PeerID();
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("00000", 16384);
		pieceDatabase.start (true);

		final BlockDescriptor requestDescriptor = new BlockDescriptor (2, 0, 16384);
		final boolean[] setPieceSuggested = new boolean[1];
		PeerServicesProvider provider = new PeerServicesProvider() {
			public PeerServices getPeerServices (InfoHash infoHash) {
				return new MockPeerServices (localPeerID, pieceDatabase) {
					@Override
					public boolean peerConnected (ManageablePeer peer) {
						return true;
					}
					@Override
					public boolean addAvailablePieces (ManageablePeer peer) {
						return true;
					}
					@Override
					public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
						if (allowedFastOnly) {
							return new ArrayList<BlockDescriptor>();
						}
						return new ArrayList<BlockDescriptor> (Arrays.asList (new BlockDescriptor[] {requestDescriptor}));
					}
					@Override
					public void setPieceSuggested (ManageablePeer peer, int pieceNumber) {
						setPieceSuggested[0] = true;
					}
				};
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (provider, mockConnection);

		// Allow PeerHandler to make a request for an Allowed Fast piece while choked
		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.suggestPieceMessage (2));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (setPieceSuggested[0]);
		assertTrue (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test that the extension protocol handshake is negotiated correctly
	 * @throws Exception
	 */
	@Test
	public void testExtensionHandshake() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);

		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		final boolean[] configureExtensionsCalled = new boolean[1];
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
			@Override
			public void offerExtensionsToPeer (ExtensiblePeer peerHandler) {
				configureExtensionsCalled[0] = true;
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, true, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (configureExtensionsCalled[0]);


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests behaviour during negotiation of an extension
	 * @throws Exception
	 */
	@Test
	public void testExtensionNegotiation() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put ("bl_ah", 1);

		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		final boolean[] configureExtensionsCalled = new boolean[1];
		final boolean[] enableDisableExtensionsCalled = new boolean[1];
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
			@Override
			public void offerExtensionsToPeer (ExtensiblePeer peerHandler) {
				configureExtensionsCalled[0] = true;
			}
			@Override
			public void enableDisablePeerExtensions (ExtensiblePeer peer, Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra) {
				enableDisableExtensionsCalled[0] = true;
				assertEquals (1, extensionsEnabled.size());
				assertEquals ("bl_ah", extensionsEnabled.iterator().next());
				assertEquals (0, extensionsDisabled.size());
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, true, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (configureExtensionsCalled[0]);
		assertTrue (enableDisableExtensionsCalled[0]);


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests Merkle extension sending pieces
	 * @throws Exception
	 */
	@Test
	public void testMerklePieceSent() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384;

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.createMerkle ("1", pieceSize);
		pieceDatabase.start (true);
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_MERKLE, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE);

		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		final boolean[] configureExtensionsCalled = new boolean[1];
		final boolean[] enableDisableExtensionsCalled = new boolean[1];
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return false;
			}
			@Override
			public void offerExtensionsToPeer (ExtensiblePeer peerHandler) {
				configureExtensionsCalled[0] = true;
			}
			@Override
			public void enableDisablePeerExtensions (ExtensiblePeer peer, Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra) {
				enableDisableExtensionsCalled[0] = true;
				assertEquals (1, extensionsEnabled.size());
				assertEquals (PeerProtocolConstants.EXTENSION_MERKLE, extensionsEnabled.iterator().next());
				assertEquals (0, extensionsDisabled.size());
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);
		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (0));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (configureExtensionsCalled[0]);
		assertTrue (enableDisableExtensionsCalled[0]);

		BlockDescriptor descriptor1 = new BlockDescriptor (0, 0, 8192);
		BlockDescriptor descriptor2 = new BlockDescriptor (0, 8192, 8192);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (descriptor1));
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (descriptor2));
		handler.connectionReady (mockConnection, true, true);

		ByteBuffer expectedBlock1 = ByteBuffer.wrap (Util.pseudoRandomBlock (0, 8192, 8192));
		ByteBuffer expectedBlock2 = ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384), 8192, 8192);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.merklePieceMessage (descriptor1, tree.getHashChain(0, pieceSize).getHashes(), expectedBlock1));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.merklePieceMessage (descriptor2, null, expectedBlock2));
		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests Merkle extension receiving pieces
	 * @throws Exception
	 */
	@Test
	public void testMerklePieceReceived() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384;

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.createMerkle ("0", pieceSize);
		pieceDatabase.start (true);
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_MERKLE, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE);

		final boolean[] configureExtensionsCalled = new boolean[1];
		final boolean[] enableDisableExtensionsCalled = new boolean[1];
		final boolean[] requestsSent = new boolean[1];
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return true;
			}
			@Override
			public void offerExtensionsToPeer (ExtensiblePeer peerHandler) {
				configureExtensionsCalled[0] = true;
			}
			@Override
			public void enableDisablePeerExtensions (ExtensiblePeer peer, Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra) {
				enableDisableExtensionsCalled[0] = true;
				assertEquals (1, extensionsEnabled.size());
				assertEquals (PeerProtocolConstants.EXTENSION_MERKLE, extensionsEnabled.iterator().next());
				assertEquals (0, extensionsDisabled.size());
			}
			@Override
			public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
				if (requestsSent[0]) {
					return new ArrayList<BlockDescriptor>();
				}
				requestsSent[0] = true;
				return Arrays.asList (new BlockDescriptor[] {
						new BlockDescriptor (0, 0, 16384)
				});
			}
			@Override
			public void handleBlock (ManageablePeer peer, BlockDescriptor request, ViewSignature viewSignature, HashChain hashChain, byte[] block) {
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);
		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (configureExtensionsCalled[0]);
		assertTrue (enableDisableExtensionsCalled[0]);

		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		BlockDescriptor expectedDescriptor = new BlockDescriptor (0, 0, 16384);
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (expectedDescriptor));
		mockConnection.mockExpectNoMoreOutput();

		ByteBuffer block = ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384));
		mockConnection.mockInput (PeerProtocolBuilder.merklePieceMessage (expectedDescriptor, tree.getHashChain(0, pieceSize).getHashes(), block));

		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.notInterestedMessage());
		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the Elastic extension
	 *   Local database: Initial size (4)
	 *   Remote database: Initial size (4)
	 * @throws Exception
	 */
	@Test
	public void testElasticInitialSize() throws Exception {

		int pieceSize = 16384;

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.createElastic ("1111", pieceSize);
		pieceDatabase.start (true);
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC);

		final boolean[] configureExtensionsCalled = new boolean[1];
		final boolean[] enableDisableExtensionsCalled = new boolean[1];
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return true;
			}
			@Override
			public void offerExtensionsToPeer (ExtensiblePeer peerHandler) {
				configureExtensionsCalled[0] = true;
			}
			@Override
			public void enableDisablePeerExtensions (ExtensiblePeer peer, Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra) {
				enableDisableExtensionsCalled[0] = true;
				assertEquals (1, extensionsEnabled.size());
				assertEquals (PeerProtocolConstants.EXTENSION_ELASTIC, extensionsEnabled.iterator().next());
				assertEquals (0, extensionsDisabled.size());
			}
			@Override
			public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
				return new ArrayList<BlockDescriptor>();
			}
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.elasticBitfieldMessage (new BitField(4).not()));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (configureExtensionsCalled[0]);
		assertTrue (enableDisableExtensionsCalled[0]);

		assertEquals (4, handler.getRemoteBitField().length());

		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the Elastic extension
	 *   Local database: Extended (5)
	 *   Remote database: Initial size (4)
	 * @throws Exception
	 */
	@Test
	public void testElasticConnectionToExpandedDatabase() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384 * 4;

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.createElastic ("1111", pieceSize);
		pieceDatabase.start (true);
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC);

		final boolean[] configureExtensionsCalled = new boolean[1];
		final boolean[] enableDisableExtensionsCalled = new boolean[1];
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return true;
			}
			@Override
			public void offerExtensionsToPeer (ExtensiblePeer peerHandler) {
				configureExtensionsCalled[0] = true;
			}
			@Override
			public void enableDisablePeerExtensions (ExtensiblePeer peer, Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra) {
				enableDisableExtensionsCalled[0] = true;
				assertEquals (1, extensionsEnabled.size());
				assertEquals (PeerProtocolConstants.EXTENSION_ELASTIC, extensionsEnabled.iterator().next());
				assertEquals (0, extensionsDisabled.size());
			}
			@Override
			public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
				return new ArrayList<BlockDescriptor>();
			}
		};

		// Extend the database. The info hash will not change
		totalLength += pieceSize;
		pieceDatabase.extendData (MockPieceDatabase.mockPrivateKey, ByteBuffer.wrap (Util.pseudoRandomBlock (4, pieceSize, pieceSize)));

		// Remote peer connects on the original info hash
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.elasticSignatureMessage (pieceDatabase.getViewSignature (totalLength)));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.elasticBitfieldMessage (new BitField(5).not()));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (configureExtensionsCalled[0]);
		assertTrue (enableDisableExtensionsCalled[0]);

		assertEquals (4, handler.getRemoteBitField().length());

		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests Elastic extension behaviour on a request for an irregular final piece that has been
	 * obsoleted by a new view
	 * @throws Exception
	 */
	@Test
	public void testElasticRequestExpandedIrregularPiece() throws Exception {

		PeerID localPeerID = new PeerID();

		int pieceSize = 16384;
		int totalLength = 16384 + 8192;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		byte[] originalSignature = Util.dsaSign (MockPieceDatabase.mockPrivateKey, tree.getView(totalLength).getRootHash());
		Info info = Info.createSingleFileElastic ("blah", totalLength, pieceSize, tree.getView(totalLength).getRootHash(), originalSignature);
		Storage storage = new MemoryStorage (new StorageDescriptor (pieceSize, totalLength));
		PieceDatabase pieceDatabase = new PieceDatabase (info, MockPieceDatabase.mockPublicKey, storage, null);
		pieceDatabase.start (true);
		pieceDatabase.writePiece (new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)), tree.getHashChain (0, 16384)));
		pieceDatabase.writePiece (new Piece (1, ByteBuffer.wrap (Util.pseudoRandomBlock (1, 8192, 8192)), tree.getHashChain (0, 8192)));

		assertEquals (2, pieceDatabase.getVerifiedPieceCount());
		assertEquals (2, pieceDatabase.getPresentPieces().cardinality());


		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC);

		final boolean[] configureExtensionsCalled = new boolean[1];
		final boolean[] enableDisableExtensionsCalled = new boolean[1];
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public boolean addAvailablePieces (ManageablePeer peer) {
				return true;
			}
			@Override
			public void offerExtensionsToPeer (ExtensiblePeer peerHandler) {
				configureExtensionsCalled[0] = true;
			}
			@Override
			public void enableDisablePeerExtensions (ExtensiblePeer peer, Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra) {
				enableDisableExtensionsCalled[0] = true;
				assertEquals (1, extensionsEnabled.size());
				assertEquals (PeerProtocolConstants.EXTENSION_ELASTIC, extensionsEnabled.iterator().next());
				assertEquals (0, extensionsDisabled.size());
			}
			@Override
			public void adjustChoking (boolean opportunistic) {
			}
			@Override
			public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {
				return new ArrayList<BlockDescriptor>();
			}
		};


		// Remote peer connect
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		mockConnection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, pieceDatabase.getInfo().getHash(), localPeerID));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.elasticBitfieldMessage (new BitField(2).not()));
		mockConnection.mockExpectNoMoreOutput();

		assertEquals (2, handler.getRemoteBitField().length());

		mockConnection.mockInput (PeerProtocolBuilder.interestedMessage());
		handler.connectionReady (mockConnection, true, true);
		handler.setWeAreChoking (false);
		handler.connectionReady (mockConnection, true, true);


		// Deliver a request but don't allow the response out
		BlockDescriptor request = new BlockDescriptor (1, 0, 8192);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (request));
		handler.connectionReady (mockConnection, true, false);

		// Extend the database with a signature
		// TODO Test - not good practice (pretending to be PeerCoordinator inline)
		totalLength += pieceSize;
		tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		byte[] extendedSignature = Util.dsaSign (MockPieceDatabase.mockPrivateKey, tree.getView(totalLength).getRootHash());
		pieceDatabase.extend (
				new ViewSignature (
						totalLength,
						ByteBuffer.wrap (tree.getView(totalLength).getRootHash()),
						ByteBuffer.wrap (extendedSignature)
				)
		);
		handler.rejectPiece (1);

		assertEquals (3, pieceDatabase.getVerifiedPieceCount());
		assertEquals (1, pieceDatabase.getPresentPieces().cardinality());

		// Let the response out
		handler.connectionReady (mockConnection, false, true);
		mockConnection.mockExpectOutput (PeerProtocolBuilder.unchokeMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.rejectRequestMessage (request));
		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Test close
	 * @throws Exception 
	 */
	@Test
	public void testClose() throws Exception {

		PeerID localPeerID = new PeerID();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		PeerServices peerServices = new MockPeerServices (localPeerID, pieceDatabase) {
			@Override
			public boolean peerConnected (ManageablePeer peer) {
				return true;
			}
			@Override
			public void peerDisconnected (ManageablePeer peer) { }
		};

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (peerServices, mockConnection);

		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		handler.connectionReady (mockConnection, true, false);

		handler.close();

		assertFalse (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


}
