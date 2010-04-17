/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.peer.PeerCoordinator;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConstants;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.trackerclient.PeerIdentifier;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.DSAUtil;
import org.itadaki.bobbin.util.elastictree.ElasticTree;
import org.junit.Test;

import test.Util;
import test.torrentdb.MockPieceDatabase;


/**
 * Tests PeerCoordinator
 */
public class TestPeerCoordinator {

	/**
	 * Test peersDiscovered on an empty list
	 * @throws Exception
	 */
	@Test
	public void testPeersDiscoveredEmpty() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = null;
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		// When
		peerCoordinator.peersDiscovered (new ArrayList<PeerIdentifier>());

		// Then
		// ... nothing


		pieceDatabase.terminate (true);

	}


	/**
	 * Test peersDiscovered on an list of one peer that cannot be connected to
	 * @throws Exception
	 */
	@Test
	public void testPeersDiscoveredFail() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		// Open then close a port so we are reasonably sure of having an uncontactable destination port
		ServerSocket serverSocket = new ServerSocket (0);
		int port = serverSocket.getLocalPort();
		serverSocket.close();

		// When
		peerCoordinator.peersDiscovered (Arrays.asList (new PeerIdentifier[] {
			new PeerIdentifier (new byte[20], "localhost", port)
		}));
		Thread.sleep (100); // Hack

		// Then
		assertEquals (peerCoordinator.getDesiredPeerConnections(), peerCoordinator.getPeersWanted());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);
		connectionManager.close();

	}


	/**
	 * Test peersDiscovered on an list of one peer that can be connected to
	 * @throws Exception
	 */
	@Test
	public void testPeersDiscoveredSuccess() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		ServerSocket serverSocket = new ServerSocket (0);
		int port = serverSocket.getLocalPort();

		// When
		peerCoordinator.peersDiscovered (Arrays.asList (new PeerIdentifier[] {
			new PeerIdentifier (new byte[20], "localhost", port)
		}));
		serverSocket.accept();

		// Then
		assertEquals (peerCoordinator.getDesiredPeerConnections() - 1, peerCoordinator.getPeersWanted());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);
		connectionManager.close();

	}


	/**
	 * Test peersDiscovered on an list of one peer that can be connected to, but which would be a
	 * peer too many when the connection is complete
	 * @throws Exception
	 */
	@Test
	public void testPeersDiscoveredSuccessOverLimit() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind (null);
		int port = serverSocketChannel.socket().getLocalPort();

		// When
		peerCoordinator.peersDiscovered (Arrays.asList (new PeerIdentifier[] {
			new PeerIdentifier (new byte[20], "localhost", port)
		}));
		peerCoordinator.setMaximumPeerConnections (0);
		SocketChannel channel = serverSocketChannel.accept();

		// Then
		assertEquals (-1, channel.read (ByteBuffer.allocate (1)));


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);
		connectionManager.close();

	}


	/**
	 * Test getConnectedPeers
	 * @throws Exception
	 */
	@Test
	public void testGetConnectedPeers() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind (null);

		// When
		// Trigger the coordinator to connect
		peerCoordinator.peersDiscovered (Arrays.asList (new PeerIdentifier[] {
			new PeerIdentifier (new byte[20], "localhost", serverSocketChannel.socket().getLocalPort())
		}));

		//Then
		assertEquals (0, peerCoordinator.getConnectedPeers().size());

		// When
		// Handshake through the port and wait to hear that the coordinator has replied
		final SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.write (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		int bytesRead = 0;
		while (bytesRead < 74) {
			bytesRead += socketChannel.read (ByteBuffer.allocate (68));
		}

		// Then
		assertEquals (1, peerCoordinator.getConnectedPeers().size());


		socketChannel.close();
		serverSocketChannel.close();
		peerCoordinator.terminate();
		pieceDatabase.terminate (true);
		connectionManager.close();

	}


	/**
	 * Tests connecting a peer while stopped
	 * @throws Exception
	 */
	@Test
	public void testPeerConnectedStopped() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);

		// When
		peerCoordinator.peerConnectionComplete (mock (Connection.class), new PeerID(), false, false);

		// Then
		assertEquals (0, peerCoordinator.getConnectedPeers().size());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);
		connectionManager.close();

	}


	/**
	 * Tests connecting a peer with the local peer's ID
	 * @throws Exception
	 */
	@Test
	public void testPeerConnectedSelf() throws Exception {

		// Given
		final PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		// When
		peerCoordinator.peerConnectionComplete (mock (Connection.class), localPeerID, false, false);

		// Then
		assertEquals (0, peerCoordinator.getConnectedPeers().size());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests connecting a duplicate peer
	 * @throws Exception
	 */
	@Test
	public void testPeerConnectedDuplicate() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		PeerID peerID = new PeerID();

		// When
		peerCoordinator.peerConnectionComplete (mock (Connection.class), peerID, false, false);
		peerCoordinator.peerConnectionComplete (mock (Connection.class), peerID, false, false);

		// Then
		assertEquals (1, peerCoordinator.getConnectedPeers().size());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests setDesiredPeerConnections
	 * @throws Exception
	 */
	@Test
	public void testSetDesiredPeerConnections() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		// When
		peerCoordinator.setDesiredPeerConnections (20);

		// Then
		assertEquals (20, peerCoordinator.getDesiredPeerConnections());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests setDesiredPeerConnections with an invalid value
	 * @throws Exception
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testSetDesiredPeerConnectionsInvalid() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		// When
		try {
			peerCoordinator.setDesiredPeerConnections (-1);
		} finally {
			peerCoordinator.terminate();
			pieceDatabase.terminate (true);
		}

		// Then
		// ... exception

	}


	/**
	 * Tests setMaximumPeerConnections
	 * @throws Exception
	 */
	@Test
	public void testSetMaximumPeerConnections() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		// When
		peerCoordinator.setMaximumPeerConnections (30);

		// Then
		assertEquals (30, peerCoordinator.getMaximumPeerConnections());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests setMaximumPeerConnections shedding peers over the limit while seeding
	 * @throws Exception
	 */
	@Test
	public void testSetMaximumPeerConnectionsShedPeersSeeding() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		final PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		PeerID peerID1 = new PeerID();
		MockConnection connection1 = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection1, peerID1, false, false);
		PeerID peerID2 = new PeerID();
		MockConnection connection2 = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection2, peerID2, false, false);
		BlockDescriptor descriptor = new BlockDescriptor (0, 0, 16384);

		// When
		connection1.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField (1)));
		connection1.mockInput (PeerProtocolBuilder.interestedMessage());
		connection1.mockTriggerIO (true, true);
		connection2.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField (1)));
		connection2.mockInput (PeerProtocolBuilder.interestedMessage());
		connection2.mockTriggerIO (true, true);

		// Then
		connection1.mockExpectOutput (PeerProtocolBuilder.bitfieldMessage (pieceDatabase.getPresentPieces()));
		connection1.mockExpectOutput (PeerProtocolBuilder.unchokeMessage());
		connection2.mockExpectOutput (PeerProtocolBuilder.bitfieldMessage (pieceDatabase.getPresentPieces()));
		connection2.mockExpectOutput (PeerProtocolBuilder.unchokeMessage());

		// When
		connection2.mockInput (PeerProtocolBuilder.requestMessage (descriptor));
		connection2.mockTriggerIO (true, true);

		// Then
		connection2.mockExpectOutput (PeerProtocolBuilder.pieceMessage (descriptor, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384))));
		assertEquals (2, peerCoordinator.getConnectedPeers().size());

		// When
		peerCoordinator.setMaximumPeerConnections (1);

		// Then
		assertEquals (1, peerCoordinator.getConnectedPeers().size());
		assertEquals (peerID2, peerCoordinator.getConnectedPeers().iterator().next().getPeerState().getRemotePeerID());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests setMaximumPeerConnections shedding peers over the limit while downloading
	 * @throws Exception
	 */
	@Test
	public void testSetMaximumPeerConnectionsShedPeersDownloading() throws Exception {


		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		final PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		PeerID peerID1 = new PeerID();
		MockConnection connection1 = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection1, peerID1, false, false);
		PeerID peerID2 = new PeerID();
		MockConnection connection2 = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection2, peerID2, false, false);
		BlockDescriptor descriptor = new BlockDescriptor (0, 0, 16384);

		// When
		connection1.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField(1).not()));
		connection1.mockInput (PeerProtocolBuilder.unchokeMessage());
		connection1.mockTriggerIO (true, true);
		connection2.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField(1).not()));
		connection2.mockInput (PeerProtocolBuilder.unchokeMessage());
		connection2.mockTriggerIO (true, true);

		// Then
		connection1.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		connection1.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection2.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		connection2.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));

		// When
		connection2.mockInput (PeerProtocolBuilder.pieceMessage (descriptor, ByteBuffer.allocate (16384)));
		connection2.mockTriggerIO (true, true);
		peerCoordinator.setMaximumPeerConnections (1);

		// Then
		assertEquals (1, peerCoordinator.getConnectedPeers().size());
		assertEquals (peerID2, peerCoordinator.getConnectedPeers().iterator().next().getPeerState().getRemotePeerID());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests setMaximumPeerConnections with an invalid value
	 * @throws Exception
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testSetMaximumPeerConnectionsInvalid() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		// When
		try {
			peerCoordinator.setMaximumPeerConnections (-1);
		} finally {
			peerCoordinator.terminate();
			pieceDatabase.terminate (true);
		}

		// Then
		// ... exception
	}


	/**
	 * Tests setWantedPeers
	 * @throws Exception
	 */
	@Test
	public void testSetWantedPeers() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);

		// When
		peerCoordinator.start();

		// Then
		assertEquals (1, peerCoordinator.getWantedPieces().cardinality());

		// When
		peerCoordinator.setWantedPieces (new BitField (1));

		// Then
		assertEquals (0, peerCoordinator.getWantedPieces().cardinality());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests handleBlock with no request
	 * @throws Exception
	 */
	@Test
	public void testHandleBlockNoRequest() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = mock (ConnectionManager.class);
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		MockConnection connection = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection, new PeerID(), false, false);
		BlockDescriptor descriptor = new BlockDescriptor (0, 0, 16384);

		// When
		connection.mockInput (PeerProtocolBuilder.pieceMessage (descriptor, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384))));
		connection.mockTriggerIO (true, true);

		// Then
		assertEquals (0, pieceDatabase.getPresentPieces().cardinality());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests handleBlock in response to a valid request
	 * @throws Exception
	 */
	@Test
	public void testHandleBlockValid() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		MockConnection connection = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection, new PeerID(), false, false);
		BlockDescriptor descriptor = new BlockDescriptor (0, 0, 16384);

		// When
		connection.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField(1).not()));
		connection.mockInput (PeerProtocolBuilder.unchokeMessage());
		connection.mockTriggerIO (true, true);

		// Then
		connection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));

		// When
		connection.mockInput (PeerProtocolBuilder.pieceMessage (descriptor, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384))));
		connection.mockTriggerIO (true, true);

		// Then
		assertEquals (1, pieceDatabase.getPresentPieces().cardinality());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests response to a peer with a bitfield containing pieces that we want
	 * @throws Exception
	 */
	@Test
	public void testAddAvailablePiecesWanted() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		MockConnection connection = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection, new PeerID(), false, false);

		// When
		connection.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField(1).not()));
		connection.mockInput (PeerProtocolBuilder.unchokeMessage());
		connection.mockTriggerIO (true, true);

		// Then
		connection.mockExpectOutput(PeerProtocolBuilder.interestedMessage());
		connection.mockExpectOutput(PeerProtocolBuilder.requestMessage (new BlockDescriptor (0, 0, 16384)));
		connection.mockExpectNoMoreOutput();


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}

	/**
	 * Tests addAvailablePieces with unwanted pieces
	 * @throws Exception
	 */
	@Test
	public void testAddAvailablePiecesUnwanted() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("11", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		MockConnection connection = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection, new PeerID(), false, false);

		// When
		connection.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField(2).not()));
		connection.mockInput (PeerProtocolBuilder.unchokeMessage());
		connection.mockTriggerIO (true, true);

		// Then
		connection.mockExpectOutput (PeerProtocolBuilder.bitfieldMessage (new BitField(2).not()));
		connection.mockExpectNoMoreOutput();

		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests addAvailablePiece with a wanted piece
	 * @throws Exception
	 */
	@Test
	public void testAddAvailablePieceWanted() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("00", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		MockConnection connection = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection, new PeerID(), false, false);

		// When
		connection.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField (2)));
		connection.mockInput (PeerProtocolBuilder.unchokeMessage());
		connection.mockTriggerIO (true, true);

		// Then
		connection.mockExpectNoMoreOutput();

		// When
		connection.mockInput (PeerProtocolBuilder.haveMessage (0));
		connection.mockTriggerIO (true, true);

		// Then
		connection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		connection.mockExpectOutput(PeerProtocolBuilder.requestMessage (new BlockDescriptor (0, 0, 16384)));
		connection.mockExpectNoMoreOutput();


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests addAvailablePiece with an unwanted piece
	 * @throws Exception
	 */
	@Test
	public void testAddAvailablePieceUnwanted() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("10", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		MockConnection connection = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection, new PeerID(), false, false);

		// When
		connection.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField (2)));
		connection.mockInput (PeerProtocolBuilder.unchokeMessage());
		connection.mockInput (PeerProtocolBuilder.haveMessage (0));
		connection.mockTriggerIO (true, true);

		// Then
		connection.mockExpectOutput (PeerProtocolBuilder.bitfieldMessage (pieceDatabase.getPresentPieces()));
		connection.mockExpectNoMoreOutput();


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests adjustChoking
	 * @throws Exception
	 */
	@Test
	public void testAdjustChoking() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("10", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		MockConnection connection = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection, new PeerID(), false, false);

		// When
		connection.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField (2)));
		connection.mockTriggerIO (true, true);

		// Then
		connection.mockExpectOutput (PeerProtocolBuilder.bitfieldMessage (pieceDatabase.getPresentPieces()));
		connection.mockExpectNoMoreOutput();

		// When
		peerCoordinator.adjustChoking (false);
		connection.mockTriggerIO (true, true);

		// Then
		connection.mockExpectOutput (PeerProtocolBuilder.unchokeMessage());
		connection.mockExpectNoMoreOutput();


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests signature handling
	 * @throws Exception
	 */
	@Test
	public void testSignatureExtends() throws Exception {

		// Given
		int pieceSize = 16384;
		int totalLength = pieceSize;
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.createElastic ("0", pieceSize);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		MockConnection connection = new MockConnection();
		peerCoordinator.peerConnectionComplete (connection, new PeerID(), true, true);

		connection.mockInput (PeerProtocolBuilder.bitfieldMessage (new BitField(1).not()));
		connection.mockTriggerIO (true, true);

		totalLength += pieceSize;
		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		byte[] extendedRootHash = tree.getView(totalLength).getRootHash();

		byte[] token = new byte[48];
		System.arraycopy (pieceDatabase.getInfo().getHash().getBytes(), 0, token, 0, 20);
		ByteBuffer viewLengthBuffer = ByteBuffer.allocate (8);
		viewLengthBuffer.asLongBuffer().put (totalLength);
		viewLengthBuffer.get (token, 20, 8);
		System.arraycopy (extendedRootHash, 0, token, 28, 20);
		Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
		dsa.initSign (MockPieceDatabase.mockPrivateKey);
		dsa.update (token);
		byte[] derSignature = dsa.sign();

		ViewSignature receivedViewSignature = new ViewSignature (totalLength, ByteBuffer.wrap (extendedRootHash),
				ByteBuffer.wrap (DSAUtil.derSignatureToP1363Signature(derSignature)));

		// When
		peerCoordinator.handleViewSignature (receivedViewSignature);
		connection.mockTriggerIO (true, true);

		// Then
		Map<String,Integer> extensionsMap = new HashMap<String,Integer>();
		extensionsMap.put (PeerProtocolConstants.EXTENSION_ELASTIC, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC);
		BDictionary extra = new BDictionary();
		extra.put ("reqq", 250);
		assertEquals (totalLength, pieceDatabase.getPiecesetDescriptor().getLength());
		assertEquals (2, peerCoordinator.getWantedPieces().cardinality());
		connection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		connection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (extensionsMap, new BDictionary()));
		connection.mockExpectOutput (PeerProtocolBuilder.elasticBitfieldMessage (new BitField (1)));
		connection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (new HashMap<String,Integer>(), extra));
		connection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		connection.mockExpectOutput (PeerProtocolBuilder.elasticSignatureMessage (receivedViewSignature));


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


}
