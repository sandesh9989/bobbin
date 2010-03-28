/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.PeerHandler;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.PeerServices;
import org.itadaki.bobbin.peer.PeerStatistics;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConstants;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.Info;
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
import org.mockito.ArgumentMatcher;

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

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// Then
		assertFalse (handler.getPeerState().getWeAreInterested());
		assertTrue (handler.getPeerState().getWeAreChoking());
		assertFalse (handler.getPeerState().getTheyAreInterested());
		assertTrue (handler.getPeerState().getTheyAreChoking());
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

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// Remote peer sends us an unchoke
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, false);

		// Then
		assertFalse (handler.getPeerState().getTheyAreChoking());

	}


	/**
	 * Test getTheyAreChoking()
	 * @throws Exception 
	 */
	@Test
	public void testGetTheyAreChoking2() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// Remote peer sends us an unchoke then a choke
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		mockConnection.mockInput (PeerProtocolBuilder.chokeMessage());
		handler.connectionReady (mockConnection, true, false);

		// Then
		assertTrue (handler.getPeerState().getTheyAreChoking());

	}


	/**
	 * Test getTheyAreInterested()
	 * @throws Exception
	 */
	@Test
	public void testGetTheyAreInterested1() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// Remote peer sends us an interested message
		mockConnection.mockInput (PeerProtocolBuilder.interestedMessage());
		handler.connectionReady (mockConnection, true, false);

		// Then
		verify(mockPeerServices).adjustChoking (true);
		assertTrue (handler.getPeerState().getTheyAreInterested());

	}


	/**
	 * Test getTheyAreInterested()
	 * @throws Exception
	 */
	@Test
	public void testGetTheyAreInterested2() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// Remote peer sends us an interested message then a not interested message
		mockConnection.mockInput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockInput (PeerProtocolBuilder.notInterestedMessage());
		handler.connectionReady (mockConnection, true, false);

		// Then
		verify(mockPeerServices, times(2)).adjustChoking (true);
		assertFalse (handler.getPeerState().getTheyAreInterested());

	}


	/**
	 * Test getBlockBytesReceived()
	 * @throws Exception 
	 */
	@Test
	public void testGetBlockBytesReceived() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		BlockDescriptor request = new BlockDescriptor (0, 0, 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false))).thenReturn (Arrays.asList (new BlockDescriptor[] { request }));

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// We send remote peer a request
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (request));
		mockConnection.mockExpectNoMoreOutput();

		// When
		// Remote peer sends us a block
		mockConnection.mockInput (PeerProtocolBuilder.pieceMessage (new BlockDescriptor (0, 0, 16384), ByteBuffer.allocate (16384)));
		handler.connectionReady (mockConnection, true, false);

		// Then
		mockConnection.mockExpectNoMoreOutput();
		verify (mockPeerServices).handleBlock (eq (handler), eq (request), eq ((ViewSignature)null), eq ((HashChain)null), any (byte[].class));
		assertEquals (16384, handler.getReadableStatistics().getTotal (PeerStatistics.Type.BLOCK_BYTES_RECEIVED_RAW));


		pieceDatabase.terminate (true);

	}


	/**
	 * Test getBlockBytesSent()
	 * @throws Exception 
	 */
	@Test
	public void testGetBlockBytesSent() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);
		BlockDescriptor request = new BlockDescriptor (0, 0, 16384);

		// When
		// Initial handshake
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.bitfieldMessage (pieceDatabase.getPresentPieces()));
		mockConnection.mockExpectNoMoreOutput();

		// When
		// Remote peer sends us a request, we send remote peer a block
		handler.setWeAreChoking (false);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (request));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		BlockDescriptor request = new BlockDescriptor (0, 0, 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false))).thenReturn (Arrays.asList (new BlockDescriptor[] { request }));
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// Remote peer sends us a block
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, false);
		mockConnection.mockInput (PeerProtocolBuilder.pieceMessage (request, ByteBuffer.allocate (16384)));
		handler.connectionReady (mockConnection, true, false);

		// Then
		// Protocol counter measures total bytes including the block
		assertEquals (16408, handler.getReadableStatistics().getReadableCounter (PeerStatistics.Type.PROTOCOL_BYTES_RECEIVED).getTotal());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test getProtocolBytesSent()
	 * @throws Exception 
	 */
	@Test
	public void testGetProtocolBytesSent() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// We send the remote peer a block
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);
		handler.setWeAreChoking (false);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (new BlockDescriptor (0, 0, 16384)));
		handler.connectionReady (mockConnection, true, true);

		// Then
		// Protocol counter measures total bytes including the block
		assertEquals (16408, handler.getReadableStatistics().getReadableCounter (PeerStatistics.Type.PROTOCOL_BYTES_SENT).getTotal());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test getTheyHaveOutstandingRequests
	 * @throws Exception 
	 */
	@Test
	public void testGetTheyHaveOutstandingRequests() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// The remote peer requests a block but we don't send it
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);
		handler.setWeAreChoking (false);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (new BlockDescriptor (0, 0, 16384)));
		handler.connectionReady (mockConnection, true, false);

		// Then
		assertTrue (handler.getTheyHaveOutstandingRequests());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test setWeAreChoking
	 * @throws Exception 
	 */
	@Test
	public void testSetWeAreChoking() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// The remote peer requests a block
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);
		handler.setWeAreChoking (false);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (new BlockDescriptor (0, 0, 16384)));
		handler.connectionReady (mockConnection, true, false);

		// Then
		assertFalse (handler.getPeerState().getWeAreChoking());

		// When
		// We discard the request by choking them
		handler.setWeAreChoking (true);

		// Then
		assertTrue (handler.getPeerState().getWeAreChoking());
		assertFalse (handler.getTheyHaveOutstandingRequests());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test setWeAreInterested
	 * @throws Exception 
	 */
	@Test
	public void testSetWeAreInterested() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		// Then
		assertFalse (handler.getPeerState().getWeAreInterested());

		// When
		handler.setWeAreInterested (true);

		// Then
		assertTrue (handler.getPeerState().getWeAreInterested());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test cancelRequests
	 * @throws Exception 
	 */
	@Test
	public void testCancelRequests1() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		BlockDescriptor request = new BlockDescriptor (0, 0, 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false)))
				.thenReturn (Arrays.asList (new BlockDescriptor[] { request }))
				.thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// We make a request but cancel it before it is sent
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, false);
		handler.cancelRequests (Arrays.asList (new BlockDescriptor[] { request }));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectNoMoreOutput();


		pieceDatabase.terminate (true);

	}


	/**
	 * Test cancelRequests
	 * @throws Exception 
	 */
	@Test
	public void testCancelRequests2() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		BlockDescriptor request = new BlockDescriptor (0, 0, 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false)))
				.thenReturn (Arrays.asList (new BlockDescriptor[] { request }))
				.thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		// We make a request and cancel it after it has been sent
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (request));
		mockConnection.mockExpectNoMoreOutput();

		// When
		handler.cancelRequests (Arrays.asList (new BlockDescriptor[] {request}));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false))).thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();

		// When
		// We send a have message
		handler.sendHavePiece (0);
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		handler.connectionReady (mockConnection, true, false);

		// Then
		// Remote bitfield is all zero
		assertEquals (new BitField (1), handler.getRemoteBitField());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test getRemoteBitField
	 * @throws Exception 
	 */
	@Test
	public void testGetRemoteBitField2() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false))).thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, false);

		// Then
		// Remote bitfield has sent value after bitfield message received
		assertEquals (new BitField (1).not(), handler.getRemoteBitField());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests getRemoteSocketAddress
	 * @throws Exception
	 */
	@Test
	public void testGetRemoteSocketAddress() throws Exception {

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);

		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, null, new PeerStatistics(), pieceDatabase, false, false);

		// Then
		assertEquals ("1.2.3.4", handler.getRemoteSocketAddress().getAddress().getHostAddress());
		assertEquals (5678, handler.getRemoteSocketAddress().getPort());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests getBlockBytesReceivedCounter in its initial state
	 * @throws Exception
	 */
	@Test
	public void testGetBlockBytesReceivedCounter() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, false, false);

		// When
		PeerID remotePeerID = new PeerID();
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), remotePeerID));
		handler.connectionReady (mockConnection, true, false);

		// Then
		assertEquals (0, handler.getStatistics().getCounter(PeerStatistics.Type.BLOCK_BYTES_RECEIVED_RAW).getTotal());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests getBlockBytesSentCounter in its initial state
	 * @throws Exception
	 */
	@Test
	public void testGetBlockBytesSentCounter() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, false, false);

		// When
		PeerID remotePeerID = new PeerID();
		mockConnection.mockInput (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), remotePeerID));
		handler.connectionReady (mockConnection, true, false);

		// Then
		assertEquals (0, handler.getStatistics().getCounter(PeerStatistics.Type.BLOCK_BYTES_SENT).getTotal());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests that an explicit rejection is sent when we choke with the Fast extension enabled
	 * @throws Exception
	 */
	@Test
	public void testFastSetWeAreChoking() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("111111111111111", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// When
		// The remote peer requests a block
		handler.setWeAreChoking (false);
		BlockDescriptor sentRequest = new BlockDescriptor (0, 0, 16384); // Not an Allowed Fast piece
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (sentRequest));
		handler.connectionReady (mockConnection, true, false);

		// Then
		assertFalse (handler.getPeerState().getWeAreChoking());

		// When
		// We discard the request by choking them
		handler.setWeAreChoking (true);
		handler.connectionReady (mockConnection, false, true);

		// Then
		// Because the Fast extension is enabled, we must send an explicit reject for their request
		mockConnection.mockExpectOutput (PeerProtocolBuilder.unchokeMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.chokeMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.rejectRequestMessage (sentRequest));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (handler.getPeerState().getWeAreChoking());
		assertFalse (handler.getTheyHaveOutstandingRequests());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test that a Have None message is sent when appropriate, when the Fast extension is enabled
	 * @throws Exception
	 */
	@Test
	public void testFastHandshakePeerIDHaveNone() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("10", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("00000", 16384);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false))).thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("111111111111111", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);
		BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);

		// WHen
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// When
		// They request a non Allowed Fast piece while choked
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);
		BlockDescriptor request = new BlockDescriptor (0, 0, 16384);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (0));
		mockConnection.mockExpectNoMoreOutput();

		// When
		// They request an Allowed Fast piece while choked
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (request));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("111111111111111", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);
		BlockDescriptor request = new BlockDescriptor (14, 0, 16384);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// When
		// They request an Allowed Fast piece while choked but over the allowed piece count
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

		// Then
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

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false))).thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);
		BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		handler.connectionReady (mockConnection, true, true);
		mockConnection.mockInput (PeerProtocolBuilder.pieceMessage (requestDescriptor, ByteBuffer.allocate (16384)));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).peerDisconnected (any (ManageablePeer.class));
		assertFalse (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a cancel message with the Fast extension enabled
	 * @throws Exception
	 */
	@Test
	public void testFastCancelMessage() throws Exception {

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);
		BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());
		handler.connectionReady (mockConnection, true, false);
		handler.setWeAreChoking (false);
		handler.connectionReady (mockConnection, false, true);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockInput (PeerProtocolBuilder.cancelMessage (requestDescriptor));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("00000", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false))).thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("00000", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		final BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false)))
				.thenReturn (Arrays.asList (new BlockDescriptor[] { requestDescriptor }))
				.thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		// They allow us to make a request
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());

		// When
		// They choke and reject the request
		mockConnection.mockInput (PeerProtocolBuilder.chokeMessage());
		mockConnection.mockInput (PeerProtocolBuilder.rejectRequestMessage (requestDescriptor));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());

		// When
		// They unchoke - request should be resent
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		final BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false)))
				.thenReturn (Arrays.asList (new BlockDescriptor[] { requestDescriptor }))
				.thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		// They allow us to make a request
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());

		// When
		// They choke and reject an invalid request
		mockConnection.mockInput (PeerProtocolBuilder.chokeMessage());
		mockConnection.mockInput (PeerProtocolBuilder.rejectRequestMessage (new BlockDescriptor (0, 16384, 16384)));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).peerDisconnected (handler);
		assertFalse (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests the response to a reject request message for an already rejected request
	 * @throws Exception
	 */
	@Test
	public void testFastRejectRequestMessageInvalid2() throws Exception {

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		final BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false)))
				.thenReturn (Arrays.asList (new BlockDescriptor[] { requestDescriptor }))
				.thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		// They allow us to make a request
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());

		// When
		// We choke and reject the request
		mockConnection.mockInput (PeerProtocolBuilder.chokeMessage());
		mockConnection.mockInput (PeerProtocolBuilder.rejectRequestMessage (requestDescriptor));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectNoMoreOutput();
		assertTrue (mockConnection.isOpen());

		// When
		// We re-reject the request
		mockConnection.mockInput (PeerProtocolBuilder.rejectRequestMessage (requestDescriptor));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).peerDisconnected (handler);
		assertFalse (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}



	/**
	 * Test that an Allowed Fast message while choked leads to a request
	 * @throws Exception
	 */
	@Test
	public void testFastAllowedFastMessage() throws Exception {

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("00000", 16384);
		pieceDatabase.start (true);
		final BlockDescriptor requestDescriptor = new BlockDescriptor (2, 0, 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (true)))
				.thenReturn (Arrays.asList (new BlockDescriptor[] { requestDescriptor }))
				.thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		// They send an Allowed Fast message, allowing us to make a request while choked
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.allowedFastMessage (2));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).setPieceAllowedFast (handler, 2);
		assertTrue (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test that an Allowed Fast message while choked leads to a request
	 * @throws Exception
	 */
	@Test
	public void testFastSuggestPieceMessage() throws Exception {

		// Given
		final PieceDatabase pieceDatabase = MockPieceDatabase.create ("00000", 16384);
		pieceDatabase.start (true);
		final BlockDescriptor requestDescriptor = new BlockDescriptor (2, 0, 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (true))).thenReturn (new ArrayList<BlockDescriptor>());
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false))).thenReturn (Arrays.asList (new BlockDescriptor[] { requestDescriptor }));
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, false);

		// When
		// They suggest a piece and unchoke
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.suggestPieceMessage (2));
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (requestDescriptor));
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).setPieceSuggested (handler, 2);
		assertTrue (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


	/**
	 * Test that the extension protocol handshake is negotiated correctly
	 * @throws Exception
	 */
	@Test
	public void testExtensionHandshake() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, false, true);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).offerExtensionsToPeer(handler);


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests behaviour during negotiation of an extension
	 * @throws Exception
	 */
	@Test
	public void testExtensionNegotiation() throws Exception {

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put ("bl_ah", 1);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, false, true);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).offerExtensionsToPeer (handler);
		verify(mockPeerServices).enableDisablePeerExtensions (
				eq (handler),
				eq (new HashSet<String> (Arrays.asList ("bl_ah"))),
				eq (new HashSet<String>()),
				argThat (new ArgumentMatcher<BDictionary>() {
					@Override
					public boolean matches (Object argument) {
						return ((BDictionary)argument).size() == 0;
					}
				})
		);


		pieceDatabase.terminate (true);

	}


	/**
	 * Tests Merkle extension sending pieces
	 * @throws Exception
	 */
	@Test
	public void testMerklePieceSent() throws Exception {

		// Given
		int pieceSize = 16384;
		int totalLength = 16384;
		PieceDatabase pieceDatabase = MockPieceDatabase.createMerkle ("1", pieceSize);
		pieceDatabase.start (true);
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_MERKLE, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, true);
		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		// When
		mockConnection.mockInput (PeerProtocolBuilder.bitfieldMessage (wantedPieces));
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.allowedFastMessage (0));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).offerExtensionsToPeer (handler);
		verify(mockPeerServices).enableDisablePeerExtensions (
				eq (handler),
				eq (new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_MERKLE))),
				eq (new HashSet<String>()),
				argThat (new ArgumentMatcher<BDictionary>() {
					@Override
					public boolean matches (Object argument) {
						return ((BDictionary)argument).size() == 0;
					}
				})
		);

		// When
		BlockDescriptor descriptor1 = new BlockDescriptor (0, 0, 8192);
		BlockDescriptor descriptor2 = new BlockDescriptor (0, 8192, 8192);
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (descriptor1));
		mockConnection.mockInput (PeerProtocolBuilder.requestMessage (descriptor2));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		int pieceSize = 16384;
		int totalLength = 16384;
		PieceDatabase pieceDatabase = MockPieceDatabase.createMerkle ("0", pieceSize);
		pieceDatabase.start (true);
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_MERKLE, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE);
		final BlockDescriptor requestDescriptor = new BlockDescriptor (0, 0, 16384);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (true))).thenReturn (new ArrayList<BlockDescriptor>());
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), eq (false)))
				.thenReturn (Arrays.asList (new BlockDescriptor[] { requestDescriptor }))
				.thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, true);
		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		// When
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).offerExtensionsToPeer (handler);
		verify(mockPeerServices).enableDisablePeerExtensions (
				eq (handler),
				eq (new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_MERKLE))),
				eq (new HashSet<String>()),
				argThat (new ArgumentMatcher<BDictionary>() {
					@Override
					public boolean matches (Object argument) {
						return ((BDictionary)argument).size() == 0;
					}
				})
		);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.unchokeMessage());
		handler.connectionReady (mockConnection, true, true);

		// Then
		BlockDescriptor expectedDescriptor = new BlockDescriptor (0, 0, 16384);
		mockConnection.mockExpectOutput (PeerProtocolBuilder.requestMessage (expectedDescriptor));
		mockConnection.mockExpectNoMoreOutput();

		// When
		ByteBuffer block = ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384));
		mockConnection.mockInput (PeerProtocolBuilder.merklePieceMessage (expectedDescriptor, tree.getHashChain(0, pieceSize).getHashes(), block));
		handler.connectionReady (mockConnection, true, true);

		// Then
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

		// Given
		int pieceSize = 16384;
		PieceDatabase pieceDatabase = MockPieceDatabase.createElastic ("1111", pieceSize);
		pieceDatabase.start (true);
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), anyBoolean())).thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, true);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.elasticBitfieldMessage (new BitField(4).not()));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).offerExtensionsToPeer (handler);
		verify(mockPeerServices).enableDisablePeerExtensions (
				eq (handler),
				eq (new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_ELASTIC))),
				eq (new HashSet<String>()),
				argThat (new ArgumentMatcher<BDictionary>() {
					@Override
					public boolean matches (Object argument) {
						return ((BDictionary)argument).size() == 0;
					}
				})
		);
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

		// Given
		int pieceSize = 16384;
		int totalLength = 16384 * 4;
		PieceDatabase pieceDatabase = MockPieceDatabase.createElastic ("1111", pieceSize);
		pieceDatabase.start (true);
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (true);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), anyBoolean())).thenReturn (new ArrayList<BlockDescriptor>());
		// Extend the database. The info hash will not change
		totalLength += pieceSize;
		pieceDatabase.extendData (MockPieceDatabase.mockPrivateKey, ByteBuffer.wrap (Util.pseudoRandomBlock (4, pieceSize, pieceSize)));
		// Remote peer connects on the original info hash
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, true);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.haveAllMessage());
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.elasticSignatureMessage (pieceDatabase.getViewSignature (totalLength)));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.elasticBitfieldMessage (new BitField(5).not()));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		mockConnection.mockExpectNoMoreOutput();
		verify(mockPeerServices).offerExtensionsToPeer (handler);
		verify(mockPeerServices).enableDisablePeerExtensions (
				eq (handler),
				eq (new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_ELASTIC))),
				eq (new HashSet<String>()),
				argThat (new ArgumentMatcher<BDictionary>() {
					@Override
					public boolean matches (Object argument) {
						return ((BDictionary)argument).size() == 0;
					}
				})
		);
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

		// Given
		int pieceSize = 16384;
		int totalLength = 16384 + 8192;
		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		byte[] originalSignature = Util.dsaSign (MockPieceDatabase.mockPrivateKey, tree.getView(totalLength).getRootHash());
		Info info = Info.createSingleFileElastic ("pieceDatabase", totalLength, pieceSize, tree.getView(totalLength).getRootHash(), originalSignature);
		Storage storage = new MemoryStorage (new StorageDescriptor (pieceSize, totalLength));
		PieceDatabase pieceDatabase = new PieceDatabase (info, MockPieceDatabase.mockPublicKey, storage, null);
		pieceDatabase.start (true);
		pieceDatabase.writePiece (new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)), tree.getHashChain (0, 16384)));
		pieceDatabase.writePiece (new Piece (1, ByteBuffer.wrap (Util.pseudoRandomBlock (1, 8192, 8192)), tree.getHashChain (1, 8192)));
		Map<String,Integer> extensions = new HashMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC);
		PeerServices mockPeerServices = mock (PeerServices.class);
		when(mockPeerServices.addAvailablePieces (any (ManageablePeer.class))).thenReturn (false);
		when(mockPeerServices.getRequests (any (ManageablePeer.class), anyInt(), anyBoolean())).thenReturn (new ArrayList<BlockDescriptor>());
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, true, true);

		// When
		mockConnection.mockInput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockInput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockInput (PeerProtocolBuilder.elasticBitfieldMessage (new BitField (2)));
		handler.connectionReady (mockConnection, true, true);

		// Then
		mockConnection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		mockConnection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null));
		mockConnection.mockExpectOutput (PeerProtocolBuilder.elasticBitfieldMessage (new BitField(2).not()));
		mockConnection.mockExpectNoMoreOutput();
		assertEquals (2, handler.getRemoteBitField().length());

		// When
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

		// Given
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		PeerServices mockPeerServices = mock (PeerServices.class);
		MockConnection mockConnection = new MockConnection();
		PeerHandler handler = new PeerHandler (mockPeerServices, mockConnection, new PeerID(), new PeerStatistics(), pieceDatabase, false, false);

		// When
		handler.connectionReady (mockConnection, true, false);
		handler.close();

		// Then
		verify(mockPeerServices).peerDisconnected (handler);
		assertFalse (mockConnection.isOpen());


		pieceDatabase.terminate (true);

	}


}
