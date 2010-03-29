/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.connectionmanager.OutboundConnectionListener;
import org.itadaki.bobbin.peer.chokingmanager.ChokingManager;
import org.itadaki.bobbin.peer.chokingmanager.DefaultChokingManager;
import org.itadaki.bobbin.peer.extensionmanager.ExtensionManager;
import org.itadaki.bobbin.peer.protocol.PeerConnectionListener;
import org.itadaki.bobbin.peer.protocol.PeerProtocolNegotiator;
import org.itadaki.bobbin.peer.requestmanager.RandomRequestManager;
import org.itadaki.bobbin.peer.requestmanager.RequestManager;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.Piece;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.PieceStyle;
import org.itadaki.bobbin.torrentdb.StorageDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.trackerclient.PeerIdentifier;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.CharsetUtil;
import org.itadaki.bobbin.util.WorkQueue;
import org.itadaki.bobbin.util.counter.Period;
import org.itadaki.bobbin.util.elastictree.HashChain;


/**
 * Administrates the connected peer set of a torrent, coordinating choking, request allocation,
 * received piece assembly and protocol extensions for the connected peers
 */
public class PeerCoordinator implements PeerConnectionListener, PeerSourceListener, PeerServices {

	/**
	 * A period of two seconds measured in 500ms intervals. Used in network statistics gathering
	 */
	static final Period TWO_SECOND_PERIOD = new Period (500, 4);

	/**
	 * A lock used to serialise access to the state of the managed torrent and its peer set between
	 * the PeerHandlers (which execute serially on a {@link ConnectionManager} thread), the periodic
	 * management task, which executes on a timer asynchronously to the PeerHandlers, and external
	 * user access.
	 */
	private final ReentrantLock peerContextLock = new ReentrantLock();

	/**
	 * The listener to inform of PeerCoordinator events
	 */
	private final Set<PeerCoordinatorListener> listeners = new LinkedHashSet<PeerCoordinatorListener>();

	/**
	 * A WorkQueue to perform asynchronous tasks
	 */
	private final WorkQueue workQueue;

	/**
	 * The ConnectionManager for the managed torrent
	 */
	private final ConnectionManager connectionManager;

	/**
	 * The ChokingManager for the managed torrent
	 */
	private final ChokingManager chokingManager;

	/**
	 * The PieceDatabase for the managed torrent
	 */
	private final PieceDatabase pieceDatabase;

	/**
	 * The RequestManager for the managed torrent
	 */
	private final RequestManager requestManager;

	/**
	 * The ExtensionManager for the managed torrent
	 */
	private final ExtensionManager extensionManager;

	/**
	 * The local peer's ID
	 */
	private final PeerID localPeerID;

	/**
	 * The set of outbound connections that have not yet connected to their destination
	 */
	private final Set<Connection> pendingConnections = new HashSet<Connection>();

	/**
	 * The set of outbound peer connections that have not yet completed their base protocol handshake
	 */
	private final Set<Connection> unconnectedPeers = new HashSet<Connection>();

	/**
	 * The set of all fully connected peers
	 */
	private final Set<ManageablePeer> connectedPeers = Collections.newSetFromMap (new ConcurrentHashMap<ManageablePeer,Boolean>());

	/**
	 * The peer IDs of all fully connected peers
	 */
	private final Set<PeerID> connectedPeerIDs = new HashSet<PeerID>();

	/**
	 * Protocol statistics for the entire peer set
	 */
	private PeerStatistics peerSetStatistics = new PeerStatistics();

	/**
	 * if {@code true}, the PeerCoordinator is running; offered connections will be accepted and
	 * connected peers maintained. If {@code false}, the PeerCoordinator is stopped.
	 */
	private boolean running = false;

	/**
	 * The pieces of the torrent that we want
	 * <p>Note: This field is accessed through synchronisation on {@code this} in order to let it be
	 * read outside the peer context lock
	 */
	private BitField wantedPieces;

	/**
	 * The number of peer connections that will be proactively sought
	 */
	private int desiredPeerConnections = 50;

	/**
	 * The maximum number of peer connections that may be open at one time
	 */
	private int maximumPeerConnections = 80;

	/**
	 * A listener for completed outbound connections
	 */
	private final OutboundConnectionListener outboundListener = new OutboundConnectionListener() {

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.connectionmanager.OutboundConnectionListener#connected(org.itadaki.bobbin.connectionmanager.Connection)
		 */
		public void connected (Connection connection) {
			lock();
			PeerCoordinator.this.pendingConnections.remove (connection);

			// Connections may be closed by a state change between being accepted by the connection
			// manager (outside the peer context lock) and their receipt here (inside the lock) 
			if (connection.isOpen() && mayConnectToPeer()) {
				PeerCoordinator.this.unconnectedPeers.add (connection);
				new PeerProtocolNegotiator (connection, PeerCoordinator.this, PeerCoordinator.this.pieceDatabase.getInfo().getHash(), PeerCoordinator.this.localPeerID);
			} else {
				try {
					connection.close();
				} catch (IOException e) {
					// Can't do anything and don't much care
				}
			}
			unlock();
		}

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.connectionmanager.OutboundConnectionListener#rejected(org.itadaki.bobbin.connectionmanager.Connection)
		 */
		public void rejected (Connection connection) {
			lock();
			PeerCoordinator.this.pendingConnections.remove (connection);
			unlock();
		}

	};


	/**
	 * A task to perform regular maintenance on the peer set
	 */
	private final Runnable maintenanceRunnable = new Runnable() {
		public void run() {
			lock();
			try {
				if (PeerCoordinator.this.running) {
					// Perform the regular choke adjustment
					adjustChoking (false);

					// Send keepalives to any peers that need it
					for (ManageablePeer peer : getConnectedPeers()) {
						peer.sendKeepaliveOrClose();
					}

					// Clean unneeded views from the piece database
					PeerCoordinator.this.pieceDatabase.garbageCollectViews();
				}
			} finally {
				unlock();
			}
		}
	};


	/* PeerConnectionListener interface */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerConnectionListener#peerConnectionComplete(org.itadaki.bobbin.connectionmanager.Connection, org.itadaki.bobbin.peer.PeerID, boolean, boolean)
	 */
	public void peerConnectionComplete (Connection connection, PeerID remotePeerID, boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {

		this.unconnectedPeers.remove (connection);

		if ((this.pieceDatabase.getInfo().getPieceStyle() != PieceStyle.PLAIN) && !extensionProtocolEnabled) {

			// If the torrent is a Merkle or Elastic torrent, the extension protocol is mandatory
			try {
				connection.close();
			} catch (IOException e) {
				// Can't do anything and don't much care
			}

		} else {

			// Apply peer limit (outbound connections are limited at source, but inbound connections
			// are not)
			if (!this.mayConnectToPeer()) {
				return;
			}

			// Refuse to connect to ourselves
			if (this.localPeerID.equals (remotePeerID)) {
				return;
			}

			// Refuse to connect to an already connected peer
			if (this.connectedPeerIDs.contains (remotePeerID)) {
				return;
			}

			// Register the peer
			PeerHandler peer = new PeerHandler (this, connection, remotePeerID, this.peerSetStatistics, this.pieceDatabase, fastExtensionEnabled, extensionProtocolEnabled);
			this.connectedPeers.add (peer);
			this.connectedPeerIDs.add (remotePeerID);
			for (PeerCoordinatorListener listener : this.listeners) {
				listener.peerRegistered (peer);
			}

		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.protocol.PeerConnectionListener#peerConnectionFailed(org.itadaki.bobbin.connectionmanager.Connection)
	 */
	public void peerConnectionFailed (Connection connection) {

		this.unconnectedPeers.remove (connection);

	}


	/* PeerSourceListener interface */
	/* This interface is used by TrackerClient to report prospective new peers */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerSourceListener#peersDiscovered(java.util.List)
	 */
	public void peersDiscovered (List<PeerIdentifier> identifiers) {

		Iterator<PeerIdentifier> iterator = identifiers.iterator();

		while (iterator.hasNext()) {

			PeerIdentifier identifier = iterator.next();
			try {

				// Refuse to connect to ourselves
				if (PeerCoordinator.this.localPeerID.equals (identifier.peerID))
					continue;

				lock();
				try {
					// If an expected peer ID is provided, refuse to connect to a known peer
					if ((identifier.peerID != null) && PeerCoordinator.this.connectedPeerIDs.contains (identifier.peerID))
						continue;

					if (shouldConnectToPeer()) {
						Connection connection = PeerCoordinator.this.connectionManager.connect (
								InetAddress.getByName (identifier.host), identifier.port, PeerCoordinator.this.outboundListener, 30);
						PeerCoordinator.this.pendingConnections.add (connection);
					}
				} finally {
					unlock();
				}

			} catch (IOException e) {
				// Can't do anything and don't much care
			}

		}

	}


	/* PeerServices interface */
	/* This interface is used by PeerHandler to access the peer set management services provided by
	 * the PeerCoordinator */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#peerDisconnected(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public void peerDisconnected (ManageablePeer peer) {

		if (this.unconnectedPeers.contains (peer)) {
			this.unconnectedPeers.remove (peer);
		} else {
			if (this.connectedPeers.contains (peer)) {
				this.connectedPeers.remove (peer);
				this.connectedPeerIDs.remove (peer.getPeerState().getRemotePeerID());
				for (PeerCoordinatorListener listener : this.listeners) {
					listener.peerDeregistered (peer);
				}
				if (this.running && peer.getPeerState().getWeAreChoking() == false) {
					adjustChoking (false);
				}
			}
		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#configureExtensions(org.itadaki.bobbin.peer.PeerHandler)
	 */
	public void offerExtensionsToPeer (ExtensiblePeer peerHandler) {

		this.extensionManager.offerExtensionsToPeer (peerHandler);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#enableDisableExtensions(org.itadaki.bobbin.peer.ExtensiblePeer, java.util.Set, java.util.Set, org.itadaki.bobbin.bencode.BDictionary)
	 */
	public void enableDisablePeerExtensions (ExtensiblePeer peer, Set<String> extensionsEnabled, Set<String> extensionsDisabled,
			BDictionary extra) {

		this.extensionManager.enableDisablePeerExtensions (peer, extensionsEnabled, extensionsDisabled, null);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#processExtensionMessage(org.itadaki.bobbin.peer.PeerHandler, java.lang.String, byte[])
	 */
	public void processExtensionMessage (ExtensiblePeer peer, String identifier, byte[] data) {

		this.extensionManager.processExtensionMessage (peer, identifier, data);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#addAvailablePieces(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public boolean addAvailablePieces (ManageablePeer peer) {

		return this.requestManager.piecesAvailable (peer);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#addAvailablePiece(org.itadaki.bobbin.peer.ManageablePeer, int)
	 */
	public boolean addAvailablePiece (ManageablePeer peer, int pieceNumber) {

		return this.requestManager.pieceAvailable (peer, pieceNumber);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#getRequests(org.itadaki.bobbin.peer.ManageablePeer, int)
	 */
	public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {

		return this.requestManager.allocateRequests (peer, numRequests, allowedFastOnly);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#handleBlock(org.itadaki.bobbin.peer.ManageablePeer, org.itadaki.bobbin.torrentdb.BlockDescriptor, org.itadaki.bobbin.torrentdb.ViewSignature, org.itadaki.bobbin.util.elastictree.HashChain, java.nio.ByteBuffer)
	 */
	public void handleBlock (ManageablePeer peer, BlockDescriptor request, ViewSignature viewSignature, HashChain hashChain, ByteBuffer block) {

		Piece piece = this.requestManager.handleBlock (peer, request, viewSignature, hashChain, block);

		if (piece != null) {
			try {
				if (this.pieceDatabase.writePiece (piece)) {
					this.requestManager.setPieceNotNeeded (request.getPieceNumber());
				}
				if ((this.requestManager.getNeededPieceCount() == 0) && (this.pieceDatabase.getInfo().getPieceStyle() != PieceStyle.ELASTIC)) {
					for (PeerCoordinatorListener listener : this.listeners) {
						listener.peerCoordinatorCompleted();
					}
				}
			} catch (IOException e) {
				// PieceDatabase will signal the error shortly
			}
		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#pieceSuggested(org.itadaki.bobbin.peer.ManageablePeer, int)
	 */
	public void setPieceAllowedFast (ManageablePeer peer, int pieceNumber) {

		this.requestManager.setPieceAllowedFast (peer, pieceNumber);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#setPieceSuggested(org.itadaki.bobbin.peer.ManageablePeer, int)
	 */
	public void setPieceSuggested (ManageablePeer peer, int pieceNumber) {

		this.requestManager.setPieceSuggested (peer, pieceNumber);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#adjustChoking()
	 */
	public void adjustChoking (boolean opportunistic) {

		if (!opportunistic || (this.connectedPeers.size() <= 4)) {
			this.chokingManager.chokePeers (this.requestManager.getNeededPieceCount() == 0);
		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#handleViewSignature(org.itadaki.bobbin.peer.ViewSignature)
	 */
	public boolean handleViewSignature (ViewSignature viewSignature) {

		// Verify the signature
		if (!this.pieceDatabase.verifyViewSignature (viewSignature)) {
			return false;
		}

		// If the signed view is longer than our view, extend our view
		if (viewSignature.getViewLength() > this.pieceDatabase.getStorageDescriptor().getLength()) {

			// If the last piece is irregular, reject peers' requests for blocks within it
			if (!this.pieceDatabase.getStorageDescriptor().isRegular()) {
				int rejectPieceNumber = this.pieceDatabase.getStorageDescriptor().getNumberOfPieces() - 1;
				for (ManageablePeer peer : this.connectedPeers) {
					peer.rejectPiece (rejectPieceNumber);
				}
			}

			// Extend the database and services
			try {
				this.pieceDatabase.extend (viewSignature);
				extendServices (viewSignature);
			} catch (IOException e) {
				// PieceDatabase will signal the error shortly
			}

			// TODO Temporary - hack to want all newly extended pieces
			this.wantedPieces.clear();
			this.wantedPieces.not();
			BitField neededPieces = this.wantedPieces.clone();
			neededPieces.and (this.pieceDatabase.getPresentPieces().not());
			this.requestManager.setNeededPieces (neededPieces);

		}

		return true;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#lock()
	 */
	public void lock() {

		this.peerContextLock.lock();

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerServices#unlock()
	 */
	public void unlock() {

		this.peerContextLock.unlock();

	}


	/**
	 * Closes all peer connections
	 * 
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 */
	private void closeAllConnections() {

		// Close all pending connections
		for (Iterator<Connection> iterator = this.pendingConnections.iterator(); iterator.hasNext();) {
			Connection connection = iterator.next();
			iterator.remove();
			try {
				connection.close();
			} catch (IOException e) {
				// Shouldn't happen and don't care
			}
		}

		// Close all unconnected peers
		for (Iterator<Connection> iterator = this.unconnectedPeers.iterator(); iterator.hasNext();) {
			Connection connection = iterator.next();
			iterator.remove();
			try {
				connection.close();
			} catch (IOException e) {
				// Shouldn't happen and don't care
			}
		}

		// Close all connected peers
		for (Iterator<ManageablePeer> iterator = this.connectedPeers.iterator(); iterator.hasNext();) {
			ManageablePeer peer = iterator.next();
			iterator.remove();
			peer.close();
		}
		this.connectedPeerIDs.clear();

	}


	/**
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 * 
	 * @param viewSignature The
	 */
	private void extendServices (ViewSignature viewSignature) {

		this.requestManager.extend (this.pieceDatabase.getStorageDescriptor());
		this.wantedPieces.extend (this.pieceDatabase.getStorageDescriptor().getNumberOfPieces());

		for (ManageablePeer peer : this.connectedPeers) {
			peer.sendViewSignature (viewSignature);
		}

	}


	/**
	 * Checks if the torrent manager is already at its maximum number of connections
	 *
	 *<p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @return {@code true} if a peer may be connected to, otherwise {@code false} 
	 */
	private boolean mayConnectToPeer() {

		return (
				   this.running
				&& ((this.pendingConnections.size() + this.unconnectedPeers.size() + this.connectedPeers.size()) < this.maximumPeerConnections)
		       );

	}


	/**
	 * Checks if the torrent manager is already at its desired number of connections
	 *
	 *<p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @return {@code true} if a peer should be connected to, otherwise {@code false} 
	 */
	private boolean shouldConnectToPeer() {

		return (
				   this.running
				&& ((this.pendingConnections.size() + this.unconnectedPeers.size() + this.connectedPeers.size())
						< Math.min (this.maximumPeerConnections, this.desiredPeerConnections))
		       );

	}


	/**
	 * @return The aggregate peer set statistics
	 */
	public PeerStatistics getStatistics() {

		return this.peerSetStatistics;

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The set of connected peers
	 */
	public Set<ManageablePeer> getConnectedPeers() {

		// All other access to connectedPeers is synchronised through the peer context lock. This
		// method returns a weakly consistent snapshot of the peer set if called outside the lock
		return new HashSet<ManageablePeer> (this.connectedPeers);

	}


	/**
	 *<p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @return The number of additional peers that should be connected to
	 */
	public int getPeersWanted() {

		return Math.min (
				this.maximumPeerConnections,
				Math.max (0, this.desiredPeerConnections - (this.pendingConnections.size() + this.unconnectedPeers.size() + this.connectedPeers.size()))
		);

	}


	/**
	 * Gets the set of pieces that are wanted
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The set of pieces that are wanted
	 */
	public BitField getWantedPieces() {

		synchronized (this) {
			return this.wantedPieces.clone();
		}

	}


	/**
	 * Sets the set of pieces that are wanted
	 *
	 * <p><b>Thread safety:</b> This method implicitly acquires the peer context lock
	 *
	 * @param wantedPieces The set of pieces that are wanted
	 */
	public void setWantedPieces (BitField wantedPieces) {

		lock();
		try {
			synchronized (this) {
				this.wantedPieces = wantedPieces.clone();
			}

			if (this.running) {
				BitField neededPieces = wantedPieces.clone();
				neededPieces.and (this.pieceDatabase.getPresentPieces().not());
				this.requestManager.setNeededPieces (neededPieces);
			}
		} finally {
			unlock();
		}

	}


	/**
	 * Gets the desired number of peer connections that this TorrentManager should connect to.
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The maximum number of peer connections that this TorrentManager may be connected to
	 */
	public int getDesiredPeerConnections() {

		lock();
		try {
			return this.desiredPeerConnections;
		} finally {
			unlock();
		}

	}


	/**
	 * Sets the desired number of peer connections that this TorrentManager should connect to.
	 * Outbound connections will be proactively formed until this number of connections is
	 * reached.
	 *
	 * <p><b>Thread safety:</b> This method implicitly acquires the peer context lock
	 *
	 * @param desiredPeerConnections The desired number of peer connections.
	 */
	public void setDesiredPeerConnections (int desiredPeerConnections) {

		if (desiredPeerConnections < 0) {
			throw new IllegalArgumentException();
		}

		lock();
		this.desiredPeerConnections = desiredPeerConnections;
		unlock();

	}


	/**
	 * Gets the maximum number of peer connections that this TorrentManager may be connected to
	 *
	 * <p><b>Thread safety:</b> This method implicitly acquires the peer context lock
	 * 
	 * @return The maximum number of peer connections that this TorrentManager may be connected to
	 */
	public int getMaximumPeerConnections() {

		lock();
		try {
			return this.maximumPeerConnections;
		} finally {
			unlock();
		}

	}


	/**
	 * Sets the maximum number of peer connections that this TorrentManager may be connected to.
	 * If there are currently more connections than the maximum, currently open connections will be
	 * closed to comply with the limit
	 *
	 * <p><b>Thread safety:</b> This method implicitly acquires the peer context lock
	 *
	 * @param maximumPeerConnections The maximum number of peer connections
	 */
	public void setMaximumPeerConnections (int maximumPeerConnections) {

		if (maximumPeerConnections < 0) {
			throw new IllegalArgumentException();
		}

		lock();

		this.maximumPeerConnections = maximumPeerConnections;

		// Close some connections if needed
		if (this.connectedPeers.size() > maximumPeerConnections) {
			final boolean complete = (getNeededPieceCount() == 0);
			List<ManageablePeer> peers = new ArrayList<ManageablePeer> (this.connectedPeers);
			Collections.sort (peers, new Comparator<ManageablePeer>() {
				public int compare (ManageablePeer peer1, ManageablePeer peer2) {
					ReadablePeerStatistics peer1Statistics = peer1.getReadableStatistics();
					ReadablePeerStatistics peer2Statistics = peer2.getReadableStatistics();
					if (complete) {
						return (int) Math.signum (peer1Statistics.getTotal (PeerStatistics.Type.BLOCK_BYTES_SENT)
								- peer2Statistics.getTotal (PeerStatistics.Type.BLOCK_BYTES_SENT));
					}
					return (int) Math.signum (peer1Statistics.getTotal (PeerStatistics.Type.BLOCK_BYTES_RECEIVED_RAW)
							- peer2Statistics.getTotal (PeerStatistics.Type.BLOCK_BYTES_RECEIVED_RAW));
				}
			});
			Iterator<ManageablePeer> iterator = peers.iterator();
			int numPeers = peers.size();
			while ((numPeers > this.maximumPeerConnections) && iterator.hasNext()) {
				iterator.next().close();
				numPeers--;
			}
		}

		unlock();

	}


	/**
	 * Extends the piece database with additional bytes, notifying peers as appropriate
	 *
	 * <p><b>Thread safety:</b> This method implicitly acquires the peer context lock
	 *
	 * @param privateKey The torrent's private key to sign the additional data
	 * @param additionalData The data to append to the database
	 * @throws IOException on any I/O error extending the database
	 */
	public void extendData (PrivateKey privateKey, ByteBuffer additionalData) throws IOException {

		lock();

		try {

			StorageDescriptor oldDescriptor = this.pieceDatabase.getStorageDescriptor();
			ViewSignature viewSignature = this.pieceDatabase.extendData (privateKey, additionalData);
			extendServices (viewSignature);
			StorageDescriptor newDescriptor = this.pieceDatabase.getStorageDescriptor();

			int fromHavePiece = oldDescriptor.getNumberOfPieces() - (oldDescriptor.isRegular() ? 0 : 1);
			int toHavePiece = newDescriptor.getNumberOfPieces() - 1;

			for (ManageablePeer peer : this.connectedPeers) {
				for (int i = fromHavePiece; i <= toHavePiece; i++) {
					peer.sendHavePiece (i);
				}
			}

		} finally {
			unlock();
		}

	}


	/**
	 * Extends the piece database by growing it to cover more of an existing file
	 *
	 * <p><b>Thread safety:</b> This method implicitly acquires the peer context lock
	 *
	 * @param privateKey The torrent's private key to sign the additional data
	 * @param length The new total length of the torrent, which must be strictly greater than the
	 *        current length
	 * @throws IOException on any I/O error extending the database
	 */
	public void extendDataInPlace (PrivateKey privateKey, long length) throws IOException {


		lock();

		try {

			StorageDescriptor oldDescriptor = this.pieceDatabase.getStorageDescriptor();
			ViewSignature viewSignature = this.pieceDatabase.extendDataInPlace (privateKey, length);
			extendServices (viewSignature);
			StorageDescriptor newDescriptor = this.pieceDatabase.getStorageDescriptor();

			int fromHavePiece = oldDescriptor.getNumberOfPieces() - (oldDescriptor.isRegular() ? 0 : 1);
			int toHavePiece = newDescriptor.getNumberOfPieces() - 1;

			for (ManageablePeer peer : this.connectedPeers) {
				for (int i = fromHavePiece; i <= toHavePiece; i++) {
					peer.sendHavePiece (i);
				}
			}

		} finally {
			unlock();
		}


	}

	/**
	 * <p><b>Thread safety:</b> This method implicitly acquires the peer context lock
	 *
	 * @return The current number of pieces that are both wanted and not present in the file
	 *         database
	 */
	public int getNeededPieceCount() {

		lock();
		try {
			return this.requestManager.getNeededPieceCount ();
		} finally {
			unlock();
		}

	}


	/**
	 * Adds a listener for PeerCoordinator events
	 *
	 * @param listener The listener to add
	 */
	public void addListener (PeerCoordinatorListener listener) {

		lock();
		this.listeners.add (listener);
		unlock();

	}

	/**
	 * Starts the PeerCoordinator
	 *
	 * <p><b>Thread safety:</b> This method implicitly acquires the peer context lock
	 */
	public void start() {

		lock();
		this.running = true;
		synchronized (this) {
			BitField neededPieces = this.wantedPieces.clone();
			neededPieces.and (this.pieceDatabase.getPresentPieces().not());
			this.requestManager.setNeededPieces (neededPieces);
		}
		unlock();

	}


	/**
	 * Stops the PeerCoordinator
	 *
	 * <p><b>Thread safety:</b> This method implicitly acquires the peer context lock
	 */
	public void stop() {

		lock();
		this.running = false;
		closeAllConnections();
		unlock();

	}


	/**
	 * Terminates the PeerCoordinator
	 *
	 * <p><b>Thread safety:</b> This method implicitly acquires the peer context lock
	 */
	public void terminate() {

		lock();
		this.running = false;
		closeAllConnections();
		this.workQueue.shutdown();
		unlock();

	}


	/**
	 * @param localPeerID The local peer's ID
	 * @param connectionManager The ConnectionManager for the managed torrent
	 * @param pieceDatabase The PieceDatabase of the managed torrent
	 * @param wantedPieces The set of pieces that are wanted
	 */
	public PeerCoordinator (PeerID localPeerID, ConnectionManager connectionManager, PieceDatabase pieceDatabase, BitField wantedPieces) {

		this.workQueue = new WorkQueue ("PeerCoordinator WorkQueue - " + CharsetUtil.hexencode (pieceDatabase.getInfo().getHash().getBytes()));

		this.localPeerID = localPeerID;
		this.connectionManager = connectionManager;
		this.pieceDatabase = pieceDatabase;
		this.wantedPieces = wantedPieces.clone();
		this.chokingManager = new DefaultChokingManager();
		this.listeners.add (this.chokingManager);
		this.requestManager = new RandomRequestManager (pieceDatabase.getStorageDescriptor());
		this.listeners.add (this.requestManager);
		this.extensionManager = new ExtensionManager();
		this.listeners.add (this.extensionManager);

		this.workQueue.scheduleWithFixedDelay (this.maintenanceRunnable, 10, 10, TimeUnit.SECONDS);

	}


}
