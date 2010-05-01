/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.itadaki.bobbin.bencode.BDecoder;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.InvalidEncodingException;
import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.connectionmanager.InboundConnectionListener;
import org.itadaki.bobbin.peer.protocol.PeerConnectionListener;
import org.itadaki.bobbin.peer.protocol.PeerConnectionListenerProvider;
import org.itadaki.bobbin.peer.protocol.PeerProtocolNegotiator;
import org.itadaki.bobbin.torrentdb.FileMetadataProvider;
import org.itadaki.bobbin.torrentdb.FileStorage;
import org.itadaki.bobbin.torrentdb.IncompatibleLocationException;
import org.itadaki.bobbin.torrentdb.Info;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.MetaInfo;
import org.itadaki.bobbin.torrentdb.Metadata;
import org.itadaki.bobbin.torrentdb.MetadataProvider;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.CharsetUtil;
import org.itadaki.bobbin.util.WorkQueue;
import org.itadaki.bobbin.util.statemachine.Ordinal;
import org.itadaki.bobbin.util.statemachine.StateMachine;
import org.itadaki.bobbin.util.statemachine.StateMachineUtil;
import org.itadaki.bobbin.util.statemachine.TargetedAction;
import org.itadaki.bobbin.util.statemachine.TransitionTable;



/**
 * A controller for a set of {@link TorrentManager}s
 */
public class TorrentSetController {

	/**
	 * The identifier prefix to use for our local peer ID
	 */
	private static final byte[] localPeerIDPrefix = "-AN0001-".getBytes (CharsetUtil.ASCII);

	/**
	 * The transition table for a {@code TorrentSetController}'s state machine
	 */
	private static final TransitionTable<State,Input,Action> TRANSITION_TABLE;

	static {

		final TransitionTable<State,Input,Action> transitions = new TransitionTable<State, Input, Action> (State.values().length, Input.values().length);

		transitions.add (State.RUNNING,     Input.START,        null,              Action.START);
		transitions.add (State.RUNNING,     Input.STOP,         null,              Action.STOP);
		transitions.add (State.RUNNING,     Input.TERMINATE,    State.TERMINATING, Action.TERMINATE);
		transitions.add (State.TERMINATING, Input.TERMINATED,   State.TERMINATED,  Action.TERMINATED);

		TRANSITION_TABLE = transitions;

	}

	/**
	 * The {@code TorrentSetController}'s state machine
	 */
	private final StateMachine<TorrentSetController,State,Input,Action> stateMachine
		= new StateMachine<TorrentSetController, State, Input, Action> (this, TRANSITION_TABLE, State.RUNNING);

	/**
	 * The listeners to inform of state changes to the TorrentSetController
	 */
	private final Set<TorrentSetControllerListener> listeners = new HashSet<TorrentSetControllerListener>();

	/**
	 * The {@code ConnectionManager} for the managed torrents
	 */
	private final ConnectionManager connectionManager;

	/**
	 * The {@code MetadataProvider} for the managed torrents
	 */
	private final MetadataProvider metadataProvider;

	/**
	 * The set of individual {@code TorrentManager}s, indexed by their info hash
	 */
	private final Map<InfoHash,TorrentManager> torrentManagers = new ConcurrentHashMap<InfoHash,TorrentManager>();

	/**
	 * The local peer's PeerID
	 */
	private final PeerID localPeerID;

	/**
	 * The local peer's port number
	 */
	private final int localPort;

	/**
	 * A work queue used to perform asynchronous tasks
	 */
	private final WorkQueue workQueue;

	/**
	 * An {@code InboundConnectionListener} to accept new incoming peer connections
	 */
	private final InboundConnectionListener inboundListener = new InboundConnectionListener() {

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.connectionmanager.InboundConnectionListener#accepted(org.itadaki.bobbin.connectionmanager.Connection)
		 */
		public void accepted (Connection connection) {

			synchronized (TorrentSetController.this.stateMachine) {
				switch (TorrentSetController.this.stateMachine.getState()) {
					case RUNNING:
						new PeerProtocolNegotiator (connection, TorrentSetController.this.peerConnectionListenerProvider, TorrentSetController.this.localPeerID);
						break;
				}
			}

		}

	};

	/**
	 * A {@code PeerServicesProvider} to connect incoming peers with the correct {@code PeerCoordinator}
	 */
	private final PeerConnectionListenerProvider peerConnectionListenerProvider = new PeerConnectionListenerProvider() {

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.peer.protocol.PeerConnectionListenerProvider#getPeerConnectionListener(org.itadaki.bobbin.torrentdb.InfoHash)
		 */
		public PeerConnectionListener getPeerConnectionListener (InfoHash infoHash) {

			synchronized (TorrentSetController.this.stateMachine) {
				TorrentManager manager = TorrentSetController.this.torrentManagers.get (infoHash);
				if (manager != null) {
					return manager.getPeerConnectionListener();
				}
				return null;
			}

		}

	};

	/**
	 * A {@code TorrentManagerListener} for all registered {@code TorrentManager}s
	 */
	private final TorrentManagerListener torrentManagerListener = new TorrentManagerListener() {

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.peer.TorrentManagerListener#torrentManagerRunning(org.itadaki.bobbin.peer.TorrentManager)
		 */
		public void torrentManagerRunning (TorrentManager torrentManager) {
			// Do nothing
		}

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.peer.TorrentManagerListener#torrentManagerStopped(org.itadaki.bobbin.peer.TorrentManager)
		 */
		public void torrentManagerStopped (TorrentManager torrentManager) {
			// Do nothing
		}

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.peer.TorrentManagerListener#torrentManagerError(org.itadaki.bobbin.peer.TorrentManager)
		 */
		public void torrentManagerError (TorrentManager torrentManager) {
			// Do nothing
		}

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.peer.TorrentManagerListener#torrentManagerTerminated(org.itadaki.bobbin.peer.TorrentManager)
		 */
		public void torrentManagerTerminated (TorrentManager torrentManager) {

			synchronized (TorrentSetController.this.stateMachine) {
				TorrentSetController.this.torrentManagers.remove (torrentManager.getMetaInfo().getInfo().getHash());
				switch (TorrentSetController.this.stateMachine.getState()) {
					case TERMINATING:
						if (TorrentSetController.this.torrentManagers.size() == 0) {
							TorrentSetController.this.stateMachine.input (Input.TERMINATED);
						}
						break;
				}
			}

		}

	};

	/**
	 * The state of a {@code TorrentSetController}
	 */
	private static enum State implements Ordinal {
		/** The TorrentSetController is running */
		RUNNING,
		/** The TorrentSetController is in the process of terminating*/
		TERMINATING,
		/** The TorrentSetController has terminated */
		TERMINATED
	}

	/**
	 * The possible inputs to a {@code TorrentSetController}'s state machine
	 */
	private static enum Input implements Ordinal {
		/** Commands the TorrentSetController to start all its TorrentManagers */
		START,
		/** Commands the TorrentSetController to stop all its TorrentManagers */
		STOP,
		/** Commands the TorrentSetController to terminate */
		TERMINATE,
		/** Indicates that all registered TorrentManagers have terminated*/
		TERMINATED,
	}

	/**
	 * The possible actions of a {@code TorrentSetController}'s state machine
	 */
	private static enum Action implements TargetedAction<TorrentSetController> {
		/** Initiates the starting of all registered TorrentManagers */
		START,
		/** Initiates the stopping of all registered TorrentManagers */
		STOP,
		/** Initiates termination of the TorrentSetController */
		TERMINATE,
		/** Informs listeners that the TorrentSetController has terminated */
		TERMINATED;

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.util.statemachine.TargetedAction#execute(java.lang.Object)
		 */
		public void execute (TorrentSetController target) {
			switch (this) {
				case START:       target.actionStart();       break;
				case STOP:        target.actionStop();        break;
				case TERMINATE:   target.actionTerminate();   break;
				case TERMINATED:  target.actionTerminated();  break;
			}
		}

	}


	/**
	 * Initiates the starting of all registered {@code TorrentManager}s
	 */
	private void actionStart() {

		for (TorrentManager torrentManager : this.torrentManagers.values()) {
			torrentManager.start (false);
		}

	}


	/**
	 * Initiates the stopping of all registered {@code TorrentManager}s
	 */
	private void actionStop() {

		for (TorrentManager torrentManager : this.torrentManagers.values()) {
			torrentManager.stop (false);
		}

	}


	/**
	 * Initiates termination of the {@code TorrentSetController}
	 */
	private void actionTerminate() {

		if (this.torrentManagers.size() == 0) {
			this.workQueue.execute (new Runnable() {
				public void run() {
					TorrentSetController.this.stateMachine.input (Input.TERMINATED);
				}
			});
		} else {
			for (TorrentManager torrentManager : this.torrentManagers.values()) {
				torrentManager.terminate (false);
			}
		}

	}


	/**
	 * Informs listeners that the {@code TorrentSetController} has terminated
	 */
	private void actionTerminated() {

		this.connectionManager.close();

		synchronized (this.listeners) {
			for (TorrentSetControllerListener listener : this.listeners) {
				listener.torrentSetControllerTerminated (this);
			}
		}

	}


	/**
	 * @return The local peer ID used for torrents managed by this controller
	 */
	public PeerID getLocalPeerID() {

		return this.localPeerID;

	}


	/**
	 * @return The local port bound to by the controller
	 */
	public int getLocalPort() {

		return this.localPort;

	}


	/**
	 * @param infoHash An info hash to get a {@link TorrentManager} for
	 * @return The registered {@code TorrentManager} for the given info hash, if any, or
	 *         {@code null}
	 */
	public TorrentManager getTorrentManager (InfoHash infoHash) {

		return this.torrentManagers.get (infoHash);

	}


	/**
	 * @return A list of all registered {@link TorrentManager}s
	 */
	public List<TorrentManager> getAllTorrentManagers() {

		return new ArrayList<TorrentManager> (this.torrentManagers.values());

	}


	/**
	 * Adds a new {@link TorrentManager} for the given {@link MetaInfo}. The added
	 * {@code TorrentManager} will initially be stopped, with all pieces set as wanted.
	 *
	 * @param metaInfo The {@code MetaInfo} that describes the torrent
	 * @param baseDirectory The base directory beneath which to write the file(s) of the torrent
	 * @return The created {@code TorrentManager}
	 * @throws IOException If there was a problem initialising the {@code TorrentManager}'s file
	 *         database
	 */
	public TorrentManager addTorrentManager (MetaInfo metaInfo, File baseDirectory) throws IOException {

		synchronized (this.stateMachine) {

			if (this.stateMachine.getState() != State.RUNNING) {
				throw new IllegalStateException();
			}

			Info info = metaInfo.getInfo();
			Metadata metadata = null;
			if (this.metadataProvider != null) {
				metadata = this.metadataProvider.metadataFor (info.getHash());
			}
			PieceDatabase pieceDatabase = new PieceDatabase (info, metaInfo.getPublicKey(), new FileStorage (baseDirectory), metadata);
			BitField wantedPieces = new BitField (pieceDatabase.getPiecesetDescriptor().getNumberOfPieces());
			wantedPieces.not();

			TorrentManager torrentManager = new TorrentManager (this.localPeerID, this.localPort, metaInfo, this.connectionManager, pieceDatabase, wantedPieces);

			this.torrentManagers.put (metaInfo.getInfo().getHash(), torrentManager);
			torrentManager.addListener (this.torrentManagerListener);

			return torrentManager;

		}

	}


	/**
	 * Adds a new {@link TorrentManager} for the given torrent file. The added
	 * {@code TorrentManager} will initially be stopped, with all pieces set as wanted.
	 *
	 * @param torrentFile The torrent file to create a {@code TorrentManager} for
	 * @param baseDirectory The base directory beneath which to write the file(s) of the torrent
	 * @return The created {@code TorrentManager}
	 * @throws FileNotFoundException If the torrent file does not exist
	 * @throws InvalidEncodingException If the torrent file does not contain valid {@code MetaInfo}
	 *         data
	 * @throws IncompatibleLocationException If the specified base directory contains files or
	 *         directories that are incompatible with the layout of the torrent
	 * @throws IOException On any other error reading from the torrent file
	 */
	public TorrentManager addTorrentManager (File torrentFile, File baseDirectory) throws IOException {

		synchronized (this.stateMachine) {

			if (this.stateMachine.getState() != State.RUNNING) {
				throw new IllegalStateException();
			}

			FileInputStream input = new FileInputStream (torrentFile);
			BDictionary dictionary = new BDecoder(input).decodeDictionary();
			input.close();
			MetaInfo metaInfo = new MetaInfo (dictionary);

			Info info = metaInfo.getInfo();
			Metadata metadata = null;
			if (this.metadataProvider != null) {
				metadata = this.metadataProvider.metadataFor (info.getHash());
			}
			PieceDatabase pieceDatabase = new PieceDatabase (info, metaInfo.getPublicKey(), new FileStorage (baseDirectory), metadata);
			BitField wantedPieces = new BitField (pieceDatabase.getPiecesetDescriptor().getNumberOfPieces()).not();

			TorrentManager torrentManager = new TorrentManager (this.localPeerID, this.localPort, metaInfo, this.connectionManager, pieceDatabase, wantedPieces);

			this.torrentManagers.put (metaInfo.getInfo().getHash(), torrentManager);
			torrentManager.addListener (this.torrentManagerListener);

			return torrentManager;

		}

	}


	/**
	 * Forgets any metadata held about the torrent with the given {@code InfoHash}
	 *
	 * @param infoHash The {@code InfoHash} of the torrent to forget
	 * @throws IOException On any error deleting the metadata
	 * @throws IllegalStateException If the torrent is currently registered
	 */
	public void forgetTorrent (InfoHash infoHash) throws IOException {

		synchronized (this.stateMachine) {

			if (this.torrentManagers.containsKey (infoHash)) {
				throw new IllegalStateException ("Cannot forget currently registered torrent");
			}
			if (this.metadataProvider != null) {
				this.metadataProvider.forget (infoHash);
			}

		}

	}


	/**
	 * Adds a listener for state changes to the {@code TorrentSetController}
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param listener The listener to add
	 */
	public void addListener (TorrentSetControllerListener listener) {

		synchronized (this.listeners) {
			this.listeners.add (listener);
		}

	}


	/**
	 * Removes a listener for state changes to the {@code TorrentSetController}
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param listener The listener to remove
	 */
	public void removeListener (TorrentSetControllerListener listener) {

		synchronized (this.listeners) {
			this.listeners.remove (listener);
		}

	}


	/**
	 * @return {@code true} if the controller is running, or {@code false} if it is terminating or
	 *         has terminated
	 */
	public boolean isRunning() {

		State state = this.stateMachine.getState();
		if ((state == State.TERMINATING) || (state == State.TERMINATED)) {
			return false;
		}

		return true;

	}


	/**
	 * @return {@code true} if the controller has fully terminated, or {@code false} if it running
	 *         or in the process of terminating
	 */
	public boolean isTerminated() {

		return (this.stateMachine.getState() == State.TERMINATED);

	}


	/**
	 * Attempts to asynchronously start all registered {@link TorrentManager}s. Attempting to start
	 * a {@code TorrentManager} does not guarantee that it will reach a running state without error;
	 * see {@link TorrentManager#start(boolean)} for details.
	 *
	 * <p>Individual {@code TorrentManager}s may be started, stopped or terminated separately from
	 * their supervising {@code TorrentSetController}.
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 */
	public void start() {

		this.workQueue.execute (new Runnable() {
			public void run() {
				TorrentSetController.this.stateMachine.input (Input.START);
			}
		});

	}


	/**
	 * Attempts to asynchronously stop all registered {@link TorrentManager}s. Attempting to stop a
	 * {@code TorrentManager} does not guarantee that it will stop without error; see
	 * {@link TorrentManager#stop(boolean)} for details.
	 *
	 * <p>Individual {@code TorrentManager}s may be started, stopped or terminated separately from
	 * their supervising {@code TorrentSetController}.
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 */
	public void stop() {

		this.workQueue.execute (new Runnable() {
			public void run() {
				TorrentSetController.this.stateMachine.input (Input.STOP);
			}
		});

	}


	/**
	 * Terminates the {@code TorrentSetController} and all registered {@code TorrentManager}s. All
	 * thread, file and network resources will be released.
	 *
	 * <p>Individual {@code TorrentManager}s may be started, stopped or terminated separately from
	 * their supervising {@code TorrentSetController}. After invoking this method, and either
	 * waiting by passing the "synchronous" argument as {@code true} or listenering for
	 * {@link TorrentSetControllerListener#torrentSetControllerTerminated(TorrentSetController)} to
	 * be signalled, all registered {@code TorrentManager}s are guaranteed to have terminated.
	 *
	 * <p>{@code TorrentManager}s that have terminated, whether as a result of invoking this method
	 * or calling {@link TorrentManager#terminate(boolean)}, are deregistered from their controlling
	 * {@code TorrentSetController}.
	 *
	 * <p>If a {@code TorrentManager} with fully checked data terminates without error, its
	 * {@code PieceDatabase} will save its current state through a {@code Metadata} instance
	 * provided by the {@code TorrentSetController}'s {@code MetadataProvider}, if one was
	 * registered on construction.
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param synchronous If {@code true}, the controller is terminated synchronously, and this
	 *        method will block until termination is complete. If {@code false}, the controller is
	 *        terminated asynchronously and this method will return immediately.
	 */
	public void terminate (boolean synchronous) {

		if (synchronous) {
			StateMachineUtil.transitionAndWait (this.stateMachine, this.workQueue, Input.TERMINATE, Collections.singleton (State.TERMINATED));
		} else {
			this.workQueue.execute (new Runnable() {
				public void run() {
					TorrentSetController.this.stateMachine.input (Input.TERMINATE);
				}
			});
		}

	}


	/**
	 * Constructs a {@code TorrentSetController} that stores torrent resume metadata using
	 * {@code Metadata} instances created by the supplied {@code MetadataProvider}
	 * 
	 * @param metadataProvider The {@code MetadataProvider} that will supply {@code Metadata}
	 *        instances for the managed torrents
	 * @throws IOException If a server socket to accept incoming connections could not be opened
	 */
	public TorrentSetController (MetadataProvider metadataProvider) throws IOException {

		byte[] localPeerIDBytes = new byte[20];
		new Random().nextBytes (localPeerIDBytes);
		System.arraycopy (localPeerIDPrefix, 0, localPeerIDBytes, 0, localPeerIDPrefix.length);
		this.localPeerID = new PeerID (localPeerIDBytes);

		this.connectionManager = new ConnectionManager();
		this.localPort = this.connectionManager.listen (null, 0, this.inboundListener);

		this.metadataProvider = metadataProvider;

		this.workQueue = new WorkQueue ("TorrentSetController WorkQueue - " + CharsetUtil.hexencode (localPeerIDBytes));

	}


	/**
	 * Constructs a {@code TorrentSetController} that stores torrent resume metadata beneath the
	 * supplied directory
	 * 
	 * @param metadataDirectory A {@code File} that refers to a location that either is, or can be
	 *        created as, a readable directory.
	 * @throws IOException If a server socket to accept incoming connections could not be opened
	 */
	public TorrentSetController (File metadataDirectory) throws IOException {

		this (new FileMetadataProvider (metadataDirectory));

	}


	/**
	 * Constructs a default {@code TorrentSetController}.
	 * <p><b>Note:</b> A {@code TorrentSetController} constructed in this way will not store resume
	 * metadata for its managed torrents. To support resuming torrents, construct using
	 * {@link #TorrentSetController(File)} or {@link #TorrentSetController(MetadataProvider)}.
	 * 
	 * @throws IOException If a server socket to accept incoming connections could not be opened
	 */
	public TorrentSetController() throws IOException {

		this ((MetadataProvider) null);

	}

}
