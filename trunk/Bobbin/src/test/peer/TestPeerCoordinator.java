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
import java.util.List;

import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.PeerCoordinator;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.PeerState;
import org.itadaki.bobbin.peer.PeerStatistics;
import org.itadaki.bobbin.peer.PeerStatistics.Type;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer;
import org.itadaki.bobbin.peer.protocol.PeerProtocolParser;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.trackerclient.PeerIdentifier;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.DSAUtil;
import org.itadaki.bobbin.util.elastictree.ElasticTree;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import test.Util;
import test.torrentdb.MockPieceDatabase;


/**
 * Tests PeerCoordinator
 */
public class TestPeerCoordinator {

	/**
	 * @param peerID The peer ID
	 * @return A mock ManageablePeer
	 */
	private ManageablePeer mockManageablePeer (PeerID peerID) {

		PeerState mockPeerState = mock (PeerState.class);
		when (mockPeerState.getRemotePeerID()).thenReturn (peerID);
		ManageablePeer mockPeer = mock (ManageablePeer.class);
		when (mockPeer.getPeerState()).thenReturn (mockPeerState);

		return mockPeer;

	}


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
		socketChannel.configureBlocking (false);
		final boolean[] bitfieldReceived = new boolean[1];
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		doAnswer (new Answer<Object>() {
			public Object answer (InvocationOnMock invocation) throws Throwable {
				bitfieldReceived[0] = true;
				return null;
			}
		}).when (mockConsumer).bitfieldMessage(any (byte[].class));
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);
		while (!bitfieldReceived[0]) {
			parser.parseBytes (socketChannel);
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
		boolean registered = peerCoordinator.peerConnected (mockManageablePeer (new PeerID()));

		// Then
		assertFalse (registered);


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
		boolean registered = peerCoordinator.peerConnected (mockManageablePeer (localPeerID));

		// Then
		assertFalse (registered);


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
		ManageablePeer peer1 = mockManageablePeer (peerID);
		when(peer1.getStatistics()).thenReturn (new PeerStatistics());
		ManageablePeer peer2 = mockManageablePeer (peerID);

		// When
		boolean registered1 = peerCoordinator.peerConnected (peer1);
		boolean registered2 = peerCoordinator.peerConnected (peer2);

		// Then
		assertTrue (registered1);
		assertFalse (registered2);


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
		final ManageablePeer peer1 = mock (ManageablePeer.class);
		PeerState peer1State = mock (PeerState.class);
		when (peer1State.getRemotePeerID()).thenReturn (peerID1);
		when (peer1State.getWeAreChoking()).thenReturn (true);
		when (peer1State.getRemoteBitField()).thenReturn (new BitField (1));
		PeerStatistics peer1Statistics = mock (PeerStatistics.class);
		when (peer1Statistics.getTotal (Type.BLOCK_BYTES_SENT)).thenReturn (10L);
		when (peer1.getStatistics()).thenReturn (new PeerStatistics());
		when (peer1.getReadableStatistics()).thenReturn (peer1Statistics);
		when (peer1.getPeerState()).thenReturn (peer1State);
		when (peer1.getRemoteBitField()).thenReturn (new BitField (1));
		doAnswer(new Answer<Object>() {
			public Object answer (InvocationOnMock invocation) throws Throwable {
				peerCoordinator.peerDisconnected (peer1);
				return null;
			};
		}).when(peer1).close();

		PeerID peerID2 = new PeerID();
		final ManageablePeer peer2 = mock (ManageablePeer.class);
		PeerState peer2State = mock (PeerState.class);
		when (peer2State.getRemotePeerID()).thenReturn (peerID2);
		when (peer2State.getRemoteBitField()).thenReturn (new BitField (1));
		PeerStatistics peer2Statistics = mock (PeerStatistics.class);
		when (peer2Statistics.getTotal (Type.BLOCK_BYTES_SENT)).thenReturn (20L);
		when (peer2.getStatistics()).thenReturn (new PeerStatistics());
		when (peer2.getReadableStatistics()).thenReturn (peer2Statistics);
		when (peer2.getPeerState()).thenReturn (peer2State);
		when (peer2.getRemoteBitField()).thenReturn (new BitField (1));
		doAnswer(new Answer<Object>() {
			public Object answer (InvocationOnMock invocation) throws Throwable {
				peerCoordinator.peerDisconnected (peer2);
				return null;
			};
		}).when(peer2).close();

		// When
		peerCoordinator.peerConnected (peer1);
		peerCoordinator.peerConnected (peer2);

		// Then
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
		final ManageablePeer peer1 = mock (ManageablePeer.class);
		PeerState peer1State = mock (PeerState.class);
		when (peer1State.getRemotePeerID()).thenReturn (peerID1);
		when (peer1State.getWeAreChoking()).thenReturn (true);
		when (peer1State.getRemoteBitField()).thenReturn (new BitField (1));
		PeerStatistics peer1Statistics = mock (PeerStatistics.class);
		when (peer1Statistics.getTotal (Type.BLOCK_BYTES_RECEIVED_RAW)).thenReturn (10L);
		when (peer1.getStatistics()).thenReturn (new PeerStatistics());
		when (peer1.getReadableStatistics()).thenReturn (peer1Statistics);
		when (peer1.getPeerState()).thenReturn (peer1State);
		when (peer1.getRemoteBitField()).thenReturn (new BitField (1));
		doAnswer(new Answer<Object>() {
			public Object answer (InvocationOnMock invocation) throws Throwable {
				peerCoordinator.peerDisconnected (peer1);
				return null;
			};
		}).when(peer1).close();

		PeerID peerID2 = new PeerID();
		final ManageablePeer peer2 = mock (ManageablePeer.class);
		PeerState peer2State = mock (PeerState.class);
		when (peer2State.getRemotePeerID()).thenReturn (peerID2);
		when (peer2State.getRemoteBitField()).thenReturn (new BitField (1));
		PeerStatistics peer2Statistics = mock (PeerStatistics.class);
		when (peer2Statistics.getTotal (Type.BLOCK_BYTES_RECEIVED_RAW)).thenReturn (20L);
		when (peer2.getStatistics()).thenReturn (new PeerStatistics());
		when (peer2.getReadableStatistics()).thenReturn (peer2Statistics);
		when (peer2.getPeerState()).thenReturn (peer2State);
		when (peer2.getRemoteBitField()).thenReturn (new BitField (1));
		doAnswer(new Answer<Object>() {
			public Object answer (InvocationOnMock invocation) throws Throwable {
				peerCoordinator.peerDisconnected (peer2);
				return null;
			};
		}).when(peer2).close();


		// When
		peerCoordinator.peerConnected (peer1);
		peerCoordinator.peerConnected (peer2);

		// Then
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
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();
		ManageablePeer peer = mockManageablePeer (new PeerID());
		when (peer.getStatistics()).thenReturn (new PeerStatistics());

		// When
		peerCoordinator.peerConnected (peer);
		peerCoordinator.handleBlock (peer, new BlockDescriptor (0, 0, 16384), null, null, Util.pseudoRandomBlock (0, 16384, 16384));

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

		PeerID peerID = new PeerID();
		PeerState peerState = mock (PeerState.class);
		when(peerState.getRemotePeerID()).thenReturn (peerID);
		when(peerState.getRemoteView()).thenReturn (pieceDatabase.getStorageDescriptor());
		ManageablePeer peer = mock (ManageablePeer.class);
		when(peer.getPeerState()).thenReturn (peerState);
		when(peer.getRemoteBitField()).thenReturn (new BitField(1).not());
		when(peer.getStatistics()).thenReturn (new PeerStatistics());
		peerCoordinator.peerConnected (peer);

		// Then
		assertEquals (0, pieceDatabase.getPresentPieces().cardinality());

		// When
		List<BlockDescriptor> descriptors = peerCoordinator.getRequests (peer, 1, false);
		assertEquals (1, descriptors.size());
		assertEquals (new BlockDescriptor (0, 0, 16384), descriptors.get (0));
		peerCoordinator.handleBlock (peer, descriptors.get (0), null, null, Util.pseudoRandomBlock (0, 16384, 16384));

		// Then
		assertEquals (1, pieceDatabase.getPresentPieces().cardinality());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests addAvailablePieces with wanted pieces
	 * @throws Exception
	 */
	@Test
	public void testAddAvailablePiecesWanted() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("00", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		PeerID peerID = new PeerID();
		PeerState peerState = mock (PeerState.class);
		when(peerState.getRemotePeerID()).thenReturn (peerID);
		ManageablePeer peer = mock (ManageablePeer.class);
		when(peer.getPeerState()).thenReturn (peerState);
		when(peer.getRemoteBitField()).thenReturn (new BitField(2).not());
		when(peer.getStatistics()).thenReturn (new PeerStatistics());
		peerCoordinator.peerConnected (peer);

		// Then
		assertTrue (peerCoordinator.addAvailablePieces (peer));


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

		PeerID peerID = new PeerID();
		PeerState peerState = mock (PeerState.class);
		when(peerState.getRemotePeerID()).thenReturn (peerID);
		ManageablePeer peer = mock (ManageablePeer.class);
		when(peer.getPeerState()).thenReturn (peerState);
		when(peer.getRemoteBitField()).thenReturn (new BitField(2).not());
		when(peer.getStatistics()).thenReturn (new PeerStatistics());
		peerCoordinator.peerConnected (peer);

		// Then
		assertFalse (peerCoordinator.addAvailablePieces (peer));


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

		PeerID peerID = new PeerID();
		PeerState peerState = mock (PeerState.class);
		when(peerState.getRemotePeerID()).thenReturn (peerID);
		ManageablePeer peer = mock (ManageablePeer.class);
		when(peer.getPeerState()).thenReturn (peerState);
		when(peer.getStatistics()).thenReturn (new PeerStatistics());
		peerCoordinator.peerConnected (peer);

		// Then
		assertTrue (peerCoordinator.addAvailablePiece (peer, 0));


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
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("11", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID = new PeerID();
		PeerState peerState = mock (PeerState.class);
		when(peerState.getRemotePeerID()).thenReturn (peerID);
		ManageablePeer peer = mock (ManageablePeer.class);
		when(peer.getPeerState()).thenReturn (peerState);
		when(peer.getStatistics()).thenReturn (new PeerStatistics());
		peerCoordinator.peerConnected (peer);

		// Then
		assertFalse (peerCoordinator.addAvailablePiece (peer, 0));


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests addAvailablePieces with wanted pieces
	 * @throws Exception
	 */
	@Test
	public void testAdjustChoking() throws Exception {

		// Given
		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		PeerID peerID = new PeerID();
		PeerState peerState = mock (PeerState.class);
		when(peerState.getRemotePeerID()).thenReturn (peerID);
		when(peerState.getTheyAreInterested()).thenReturn (false);
		when(peerState.getWeAreChoking()).thenReturn (true);
		ManageablePeer peer = mock (ManageablePeer.class);
		when(peer.getPeerState()).thenReturn (peerState);
		when(peer.getRemoteBitField()).thenReturn (new BitField(1).not());
		when(peer.getStatistics()).thenReturn (new PeerStatistics());

		// When
		peerCoordinator.peerConnected (peer);

		// Then
		verify(peer, never()).setWeAreChoking (anyBoolean());

		// When
		peerCoordinator.addAvailablePieces (peer);
		peerCoordinator.adjustChoking(false);

		// Then
		verify(peer).setWeAreChoking (false);


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

		PeerID peerID = new PeerID();
		PeerState peerState = mock (PeerState.class);
		when(peerState.getRemotePeerID()).thenReturn (peerID);
		when(peerState.getTheyAreInterested()).thenReturn (false);
		when(peerState.getWeAreChoking()).thenReturn (true);
		ManageablePeer peer = mock (ManageablePeer.class);
		when(peer.getPeerState()).thenReturn (peerState);
		when(peer.getRemoteBitField()).thenReturn (new BitField(1).not());
		when(peer.setWeAreChoking(false)).thenReturn (true);
		when(peer.getStatistics()).thenReturn (new PeerStatistics());
		peerCoordinator.peerConnected (peer);
		peerCoordinator.addAvailablePieces (peer);

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

		// Then
		assertEquals (totalLength, pieceDatabase.getStorageDescriptor().getLength());
		assertEquals (2, peerCoordinator.getWantedPieces().cardinality());
		ArgumentCaptor<ViewSignature> captor = ArgumentCaptor.forClass (ViewSignature.class);
		verify(peer).sendViewSignature (captor.capture());
		assertEquals (receivedViewSignature, captor.getValue());


		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


}
