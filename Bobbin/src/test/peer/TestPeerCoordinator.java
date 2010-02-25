/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;

import static org.junit.Assert.*;

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
import org.itadaki.bobbin.peer.PeerCoordinatorListener;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.peer.protocol.PeerProtocolParser;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.trackerclient.PeerIdentifier;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.DSAUtil;
import org.itadaki.bobbin.util.counter.StatisticCounter;
import org.itadaki.bobbin.util.elastictree.ElasticTree;
import org.junit.Test;

import test.Util;
import test.peer.protocol.MockProtocolConsumer;
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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = null;
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		peerCoordinator.peersDiscovered (new ArrayList<PeerIdentifier>());

		pieceDatabase.terminate (true);

	}


	/**
	 * Test peersDiscovered on an list of one peer that cannot be connected to
	 * @throws Exception
	 */
	@Test
	public void testPeersDiscoveredFail() throws Exception {

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

		peerCoordinator.peersDiscovered (Arrays.asList (new PeerIdentifier[] {
			new PeerIdentifier (new byte[20], "localhost", port)
		}));

		Thread.sleep (100); // Hack

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		// Open a port
		ServerSocket serverSocket = new ServerSocket (0);
		int port = serverSocket.getLocalPort();

		peerCoordinator.peersDiscovered (Arrays.asList (new PeerIdentifier[] {
			new PeerIdentifier (new byte[20], "localhost", port)
		}));

		serverSocket.accept();

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		// Open a port
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind (null);

		// Trigger the coordinator to connect
		peerCoordinator.peersDiscovered (Arrays.asList (new PeerIdentifier[] {
			new PeerIdentifier (new byte[20], "localhost", serverSocketChannel.socket().getLocalPort())
		}));

		assertEquals (0, peerCoordinator.getConnectedPeers().size());

		// Handshake through the port and wait to hear that the coordinator has replied
		final SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.write (PeerProtocolBuilder.handshake (false, false, pieceDatabase.getInfo().getHash(), new PeerID()));
		socketChannel.configureBlocking (false);
		final boolean[] bitfieldReceived = new boolean[1];
		PeerProtocolParser parser = new PeerProtocolParser (new MockProtocolConsumer() {
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) { }
			@Override
			public void handshakeInfoHash (InfoHash infoHash) { }
			@Override
			public void handshakePeerID (PeerID peerID) { }
			@Override
			public void bitfieldMessage (byte[] bitField) {
				bitfieldReceived[0] = true;
			}
		}, false, false);
		while (!bitfieldReceived[0]) {
			parser.parseBytes (socketChannel);
		}

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);

		boolean registered = peerCoordinator.peerConnected (new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return new PeerID();
			}
		});

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

		final PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		boolean registered = peerCoordinator.peerConnected (new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return localPeerID;
			}
		});

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID = new PeerID();
		boolean registered1 = peerCoordinator.peerConnected (new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public void close() { }
		});
		boolean registered2 = peerCoordinator.peerConnected (new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID;
			}
		});

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		peerCoordinator.setDesiredPeerConnections (20);
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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		try {
			peerCoordinator.setDesiredPeerConnections (-1);
		} finally {
			peerCoordinator.terminate();
			pieceDatabase.terminate (true);
		}

	}


	/**
	 * Tests setMaximumPeerConnections
	 * @throws Exception
	 */
	@Test
	public void testSetMaximumPeerConnections() throws Exception {

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		peerCoordinator.setMaximumPeerConnections (30);
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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		final PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID1 = new PeerID();
		peerCoordinator.peerConnected (new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID1;
			}
			@Override
			public BitField getRemoteBitField() {
				return new BitField (1);
			}
			@Override
			public boolean getWeAreChoking() {
				return true;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public long getBlockBytesSent() {
				return 10;
			}
			@Override
			public void close() {
				peerCoordinator.peerDisconnected (this);
			}
		});

		final PeerID peerID2 = new PeerID();
		peerCoordinator.peerConnected (new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID2;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public long getBlockBytesSent() {
				return 20;
			}
			@Override
			public void close() {
				peerCoordinator.peerDisconnected (this);
			}
		});

		assertEquals (2, peerCoordinator.getConnectedPeers().size());

		peerCoordinator.setMaximumPeerConnections (1);

		assertEquals (1, peerCoordinator.getConnectedPeers().size());
		assertEquals (peerID2, peerCoordinator.getConnectedPeers().iterator().next().getRemotePeerID());

		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests setMaximumPeerConnections shedding peers over the limit while downloading
	 * @throws Exception
	 */
	@Test
	public void testSetMaximumPeerConnectionsShedPeersDownloading() throws Exception {

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		final PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID1 = new PeerID();
		peerCoordinator.peerConnected (new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID1;
			}
			@Override
			public BitField getRemoteBitField() {
				return new BitField (1);
			}
			@Override
			public boolean getWeAreChoking() {
				return true;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public long getBlockBytesReceived() {
				return 10;
			}
			@Override
			public void close() {
				peerCoordinator.peerDisconnected (this);
			}
		});

		final PeerID peerID2 = new PeerID();
		peerCoordinator.peerConnected (new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID2;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public long getBlockBytesReceived() {
				return 20;
			}
			@Override
			public void close() {
				peerCoordinator.peerDisconnected (this);
			}
		});

		assertEquals (2, peerCoordinator.getConnectedPeers().size());

		peerCoordinator.setMaximumPeerConnections (1);

		assertEquals (1, peerCoordinator.getConnectedPeers().size());
		assertEquals (peerID2, peerCoordinator.getConnectedPeers().iterator().next().getRemotePeerID());

		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests setMaximumPeerConnections with an invalid value
	 * @throws Exception
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testSetMaximumPeerConnectionsInvalid() throws Exception {

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		try {
			peerCoordinator.setMaximumPeerConnections (-1);
		} finally {
			peerCoordinator.terminate();
			pieceDatabase.terminate (true);
		}

	}


	/**
	 * Tests setWantedPeers
	 * @throws Exception
	 */
	@Test
	public void testSetWantedPeers() throws Exception {

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		assertEquals (1, peerCoordinator.getWantedPieces().cardinality());

		peerCoordinator.setWantedPieces (new BitField (1));

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID = new PeerID();
		ManageablePeer peer = new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public void close() { }
		};
		peerCoordinator.peerConnected (peer);

		peerCoordinator.handleBlock (peer, new BlockDescriptor (0, 0, 16384), null, null, Util.pseudoRandomBlock (0, 16384, 16384));

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinatorListener listener = new PeerCoordinatorListener() {
			public void peerCoordinatorCompleted() { }
			public void peerRegistered (ManageablePeer peer) { }
			public void peerDeregistered (ManageablePeer peer) { }
		};
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.addListener (listener);
		peerCoordinator.start();

		final PeerID peerID = new PeerID();
		ManageablePeer peer = new MockManageablePeer() {
			@Override
			public void setWeAreInterested (boolean weAreInterested) { }
			@Override
			public PeerID getRemotePeerID() {
				return peerID;
			}
			@Override
			public BitField getRemoteBitField() {
				return new BitField(1).not();
			}
			@Override
			public long getRemoteViewLength() {
				return 16384;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public void close() { }
		};
		peerCoordinator.peerConnected (peer);

		assertEquals (0, pieceDatabase.getPresentPieces().cardinality());

		List<BlockDescriptor> descriptors = peerCoordinator.getRequests (peer, 1, false);
		assertEquals (1, descriptors.size());
		assertEquals (new BlockDescriptor (0, 0, 16384), descriptors.get (0));
		peerCoordinator.handleBlock (peer, descriptors.get (0), null, null, Util.pseudoRandomBlock (0, 16384, 16384));

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("00", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID = new PeerID();
		ManageablePeer peer = new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID;
			}
			@Override
			public BitField getRemoteBitField() {
				return new BitField(2).not();
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public void close() { }
		};
		peerCoordinator.peerConnected (peer);

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("11", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID = new PeerID();
		ManageablePeer peer = new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID;
			}
			@Override
			public BitField getRemoteBitField() {
				return new BitField(2).not();
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public void close() { }
		};
		peerCoordinator.peerConnected (peer);

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("00", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID = new PeerID();
		ManageablePeer peer = new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public void close() { }
		};
		peerCoordinator.peerConnected (peer);

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("11", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID = new PeerID();
		ManageablePeer peer = new MockManageablePeer() {
			@Override
			public PeerID getRemotePeerID() {
				return peerID;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return new StatisticCounter();
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return new StatisticCounter();
			}
			@Override
			public void close() { }
		};
		peerCoordinator.peerConnected (peer);

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

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID = new PeerID();
		final Boolean[] chokeSet = new Boolean[1];
		ManageablePeer peer = new MockManageablePeer() {
			private StatisticCounter blockBytesReceivedCounter = new StatisticCounter();
			private StatisticCounter blockBytesSentCounter = new StatisticCounter();
			@Override
			public PeerID getRemotePeerID() {
				return peerID;
			}
			@Override
			public BitField getRemoteBitField() {
				return new BitField(1).not();
			}
			@Override
			public boolean getTheyAreInterested() {
				return false;
			}
			@Override
			public boolean getWeAreChoking() {
				return true;
			}
			@Override
			public boolean setWeAreChoking (boolean weAreChokingThem) {
				chokeSet[0] = weAreChokingThem;
				return true;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return this.blockBytesReceivedCounter;
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return this.blockBytesSentCounter;
			}
			@Override
			public void close() { }
		};
		peerCoordinator.peerConnected (peer);

		assertNull (chokeSet[0]);

		peerCoordinator.addAvailablePieces (peer);
		peerCoordinator.adjustChoking(false);

		assertFalse (chokeSet[0]);

		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


	/**
	 * Tests signature handling
	 * @throws Exception
	 */
	@Test
	public void testSignatureExtends() throws Exception {

		int pieceSize = 16384;
		int totalLength = pieceSize;

		PeerID localPeerID = new PeerID();
		ConnectionManager connectionManager = new ConnectionManager();
		PieceDatabase pieceDatabase = MockPieceDatabase.createElastic ("0", pieceSize);
		pieceDatabase.start (true);
		BitField wantedPieces = pieceDatabase.getPresentPieces().not();
		PeerCoordinator peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		peerCoordinator.start();

		final PeerID peerID = new PeerID();
		final ViewSignature[] sentViewSignature = new ViewSignature[1];
		ManageablePeer peer = new MockManageablePeer() {
			private StatisticCounter blockBytesReceivedCounter = new StatisticCounter();
			private StatisticCounter blockBytesSentCounter = new StatisticCounter();
			@Override
			public PeerID getRemotePeerID() {
				return peerID;
			}
			@Override
			public BitField getRemoteBitField() {
				return new BitField(1).not();
			}
			@Override
			public boolean getTheyAreInterested() {
				return false;
			}
			@Override
			public boolean getWeAreChoking() {
				return true;
			}
			@Override
			public boolean setWeAreChoking (boolean weAreChokingThem) {
				return true;
			}
			@Override
			public StatisticCounter getBlockBytesReceivedCounter() {
				return this.blockBytesReceivedCounter;
			}
			@Override
			public StatisticCounter getBlockBytesSentCounter() {
				return this.blockBytesSentCounter;
			}
			@Override
			public void setWeAreInterested (boolean weAreInterested) {
			}
			@Override
			public void sendViewSignature (ViewSignature viewSignature) {
				sentViewSignature[0] = viewSignature;
			}
			@Override
			public void close() { }
		};
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
		peerCoordinator.handleViewSignature (receivedViewSignature);
		assertEquals (totalLength, pieceDatabase.getStorageDescriptor().getLength());
		assertEquals (2, peerCoordinator.getWantedPieces().cardinality());
		assertEquals (receivedViewSignature, sentViewSignature[0]);

		peerCoordinator.terminate();
		pieceDatabase.terminate (true);

	}


}
