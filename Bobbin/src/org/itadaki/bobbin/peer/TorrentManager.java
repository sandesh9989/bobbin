/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.peer.protocol.PeerConnectionListener;
import org.itadaki.bobbin.torrentdb.MetaInfo;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.PieceDatabaseListener;
import org.itadaki.bobbin.trackerclient.PeerIdentifier;
import org.itadaki.bobbin.trackerclient.TrackerClient;
import org.itadaki.bobbin.trackerclient.TrackerClientListener;
import org.itadaki.bobbin.trackerclient.TrackerClientStatus;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.CharsetUtil;
import org.itadaki.bobbin.util.WorkQueue;
import org.itadaki.bobbin.util.statemachine.Ordinal;
import org.itadaki.bobbin.util.statemachine.StateMachine;
import org.itadaki.bobbin.util.statemachine.StateMachineUtil;
import org.itadaki.bobbin.util.statemachine.TargetedAction;
import org.itadaki.bobbin.util.statemachine.TransitionTable;



/**
 * Administrates the peer set, the piece database, and the tracker client of a single torrent.
 * 
 * <p>The application of global peer and bandwidth limits is handled by the
 * {@link TorrentSetController} that administrates this {@code TorrentManager}
 * 
 * @see PeerCoordinator
 * @see PieceDatabase
 * @see TrackerClient
 */
public class TorrentManager {

	/**
	 * The transition table for a TorrentManager's state machine
	 */
	private static final TransitionTable<State,Input,Action> TRANSITION_TABLE;

	static {

		final TransitionTable<State,Input,Action> transitions = new TransitionTable<State, Input, Action> (State.cardinality(), Input.values().length);

		addTransitionRules (transitions, BaseState.STOPPED,     null,  null,  Input.START,                     BaseState.CHECKING,    null,  null,  Action.START);
		addTransitionRules (transitions, BaseState.ERROR,       null,  null,  Input.START,                     BaseState.CHECKING,    null,  null,  Action.START);

		addTransitionRules (transitions, BaseState.CHECKING,    null,  null,  Input.STOP,                      BaseState.STOPPING,    null,  null,  Action.STOP);
		addTransitionRules (transitions, BaseState.RUNNING,     null,  null,  Input.STOP,                      BaseState.STOPPING,    null,  null,  Action.STOP);

		addTransitionRules (transitions, null,                  null,  null,  Input.TERMINATE,                 BaseState.TERMINATING, null,  null,  Action.TERMINATE);

		addTransitionRules (transitions, BaseState.CHECKING,    null,  null,  Input.DATABASE_AVAILABLE,        BaseState.RUNNING,     null,  null,  Action.RUNNING);
		addTransitionRules (transitions, BaseState.STOPPED,     null,  null,  Input.DATABASE_AVAILABLE,        null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.ERROR,       null,  null,  Input.DATABASE_AVAILABLE,        null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.TERMINATED,  null,  null,  Input.DATABASE_AVAILABLE,        null,                  null,  null,  Action.INTERNAL_ERROR);

		addTransitionRules (transitions, BaseState.CHECKING,    null,  null,  Input.DATABASE_STOPPED,          null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.RUNNING,     null,  null,  Input.DATABASE_STOPPED,          null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.STOPPING,    null,  null,  Input.DATABASE_STOPPED,          BaseState.STOPPED,     null,  null,  Action.STOPPED);
		addTransitionRules (transitions, BaseState.STOPPED,     null,  null,  Input.DATABASE_STOPPED,          null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.ERROR,       null,  null,  Input.DATABASE_STOPPED,          null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.TERMINATING, null,  null,  Input.DATABASE_STOPPED,          null,                  null,  null,  null);
		addTransitionRules (transitions, BaseState.TERMINATED,  null,  null,  Input.DATABASE_STOPPED,          null,                  null,  null,  Action.INTERNAL_ERROR);

		addTransitionRules (transitions, BaseState.CHECKING,    null,  null,  Input.DATABASE_ERROR,            BaseState.ERROR,       null,  null,  Action.ERROR);
		addTransitionRules (transitions, BaseState.RUNNING,     null,  null,  Input.DATABASE_ERROR,            BaseState.ERROR,       null,  null,  Action.ERROR);
		addTransitionRules (transitions, BaseState.STOPPING,    null,  null,  Input.DATABASE_ERROR,            BaseState.ERROR,       null,  null,  Action.ERROR);
		addTransitionRules (transitions, BaseState.STOPPED,     null,  null,  Input.DATABASE_ERROR,            null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.ERROR,       null,  null,  Input.DATABASE_ERROR,            null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.TERMINATING, true,  false, Input.DATABASE_ERROR,            null,                  null,  null,  null);
		addTransitionRules (transitions, BaseState.TERMINATED,  null,  null,  Input.DATABASE_ERROR,            null,                  null,  null,  Action.INTERNAL_ERROR);

		addTransitionRules (transitions, BaseState.CHECKING,    null,  null,  Input.DATABASE_TERMINATED,       null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.RUNNING,     null,  null,  Input.DATABASE_TERMINATED,       null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.STOPPING,    null,  null,  Input.DATABASE_TERMINATED,       null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.STOPPED,     null,  null,  Input.DATABASE_TERMINATED,       null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.ERROR,       null,  null,  Input.DATABASE_TERMINATED,       null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.TERMINATING, false, true,  Input.DATABASE_TERMINATED,       BaseState.TERMINATED,  true,  null,  Action.TERMINATED);
		addTransitionRules (transitions, BaseState.TERMINATING, false, false, Input.DATABASE_TERMINATED,       null,                  true,  null,  null);
		addTransitionRules (transitions, BaseState.TERMINATING, true,  null,  Input.DATABASE_TERMINATED,       null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.TERMINATED,  null,  null,  Input.DATABASE_TERMINATED,       null,                  null,  null,  Action.INTERNAL_ERROR);

		addTransitionRules (transitions, BaseState.CHECKING,    null,  null,  Input.TRACKER_CLIENT_TERMINATED, null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.RUNNING,     null,  null,  Input.TRACKER_CLIENT_TERMINATED, null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.STOPPING,    null,  null,  Input.TRACKER_CLIENT_TERMINATED, null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.STOPPED,     null,  null,  Input.TRACKER_CLIENT_TERMINATED, null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.ERROR,       null,  null,  Input.TRACKER_CLIENT_TERMINATED, null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.TERMINATING, true,  false, Input.TRACKER_CLIENT_TERMINATED, BaseState.TERMINATED,  null,  true,  Action.TERMINATED);
		addTransitionRules (transitions, BaseState.TERMINATING, false, false, Input.TRACKER_CLIENT_TERMINATED, null,                  null,  true,  null);
		addTransitionRules (transitions, BaseState.TERMINATING, null,  true,  Input.TRACKER_CLIENT_TERMINATED, null,                  null,  null,  Action.INTERNAL_ERROR);
		addTransitionRules (transitions, BaseState.TERMINATED,  null,  null,  Input.TRACKER_CLIENT_TERMINATED, null,                  null,  null,  Action.INTERNAL_ERROR);

		TRANSITION_TABLE = transitions;

	}

	/**
	 * The set of states that can eventually result from Input.START
	 */
	private static final Set<State> START_FINAL_STATES = Collections.unmodifiableSet (new HashSet<State> (Arrays.asList (new State[] {
			new State (BaseState.RUNNING, false, false),
			new State (BaseState.STOPPED, false, false),
			new State (BaseState.ERROR, false, false),
			new State (BaseState.TERMINATED, true, true)
	})));

	/**
	 * The set of states that can eventually result from Input.STOP
	 */
	private static final Set<State> STOP_FINAL_STATES = Collections.unmodifiableSet (new HashSet<State> (Arrays.asList (new State[] {
			new State (BaseState.STOPPED, false, false),
			new State (BaseState.ERROR, false, false),
			new State (BaseState.TERMINATED, true, true)
	})));

	/**
	 * The set of states that can eventually result from Input.TERMINATE
	 */
	private static final Set<State> TERMINATE_FINAL_STATES = Collections.unmodifiableSet (new HashSet<State> (Arrays.asList (new State[] {
			new State (BaseState.TERMINATED, true, true)
	})));

	/**
	 * The TorrentManager's state machine
	 */
	private final StateMachine<TorrentManager,State,Input,Action> stateMachine
		= new StateMachine<TorrentManager, State, Input, Action> (this, TRANSITION_TABLE, new State (BaseState.STOPPED, false, false));

	/**
	 * The listeners to inform of state changes to the TorrentManager
	 */
	private final Set<TorrentManagerListener> listeners = new HashSet<TorrentManagerListener>();

	/**
	 * A work queue used to perform asynchronous work
	 */
	private final WorkQueue workQueue;

	/**
	 * The coordinator for the torrent's peer set
	 */
	private final PeerCoordinator peerCoordinator;

	/**
	 * The PieceDatabase of the managed torrent
	 */
	private final PieceDatabase pieceDatabase;

	/**
	 * The tracker client for the managed torrent
	 */
	private final TrackerClient trackerClient;

	/**
	 * The peer ID of our local peer
	 */
	private final PeerID localPeerID;

	/**
	 * The {@code MetaInfo} of the managed torrent
	 */
	private final MetaInfo metaInfo;

	/**
	 * A Future for the scheduled maintenance task allowing it to be cancelled
	 */
	private ScheduledFuture<?> maintenanceFuture;


	/**
	 * A listener for PieceDatabase state changes 
	 */
	private final PieceDatabaseListener pieceDatabaseListener = new PieceDatabaseListener() {

		public void pieceDatabaseAvailable() {
			TorrentManager.this.stateMachine.input (Input.DATABASE_AVAILABLE);
		}

		public void pieceDatabaseStopped() {
			TorrentManager.this.stateMachine.input (Input.DATABASE_STOPPED);
		}

		public void pieceDatabaseError() {
			TorrentManager.this.stateMachine.input (Input.DATABASE_ERROR);
		}

		public void pieceDatabaseTerminated() {
			TorrentManager.this.stateMachine.input (Input.DATABASE_TERMINATED);
		}

	};


	/**
	 * A listener for TrackerClient events
	 */
	private final TrackerClientListener trackerClientListener = new TrackerClientListener() {

		public void peersDiscovered(List<PeerIdentifier> identifiers) {
			TorrentManager.this.peerCoordinator.peersDiscovered (identifiers);
		}

		public void trackerClientTerminated() {
			TorrentManager.this.stateMachine.input (Input.TRACKER_CLIENT_TERMINATED);
		}

	};


	/**
	 * A listener for events from the PeerCoordinator
	 */
	private final PeerCoordinatorListener peerCoordinatorListener = new PeerCoordinatorListener() {

		public void peerCoordinatorCompleted() {
			TorrentManager.this.trackerClient.peerCompleted();
		}

		public void peerRegistered (ManageablePeer peer) { }

		public void peerDeregistered (ManageablePeer peer) { }

	};


	/**
	 * A task to perform regular maintenance on the torrent manager
	 */
	private Runnable maintenanceRunnable = new Runnable() {
		public void run() {
			synchronized (TorrentManager.this.stateMachine) {
				TorrentManager.this.peerCoordinator.lock();
				try {
					if (TorrentManager.this.stateMachine.getState().baseState == BaseState.RUNNING) {

						// Update the tracker client with the number of peers we want to connect to and the current statistics
						TorrentManager.this.trackerClient.setPeersWanted (TorrentManager.this.peerCoordinator.getPeersWanted());
						TorrentManager.this.trackerClient.updateLocalPeerStatistics (
								TorrentManager.this.peerCoordinator.getStatistics().getCounter (PeerStatistics.Type.BLOCK_BYTES_SENT).getTotal(),
								TorrentManager.this.peerCoordinator.getStatistics().getCounter (PeerStatistics.Type.BLOCK_BYTES_RECEIVED_RAW).getTotal(),
								TorrentManager.this.peerCoordinator.getNeededPieceCount() *
										TorrentManager.this.pieceDatabase.getPiecesetDescriptor().getPieceSize()
						);

					}
				} finally {
					TorrentManager.this.peerCoordinator.unlock();
				}
			}
		}
	};


	/**
	 * Represents the basic state of a TorrentManager
	 */
	public enum BaseState {
		/** The TorrentManager is checking its data */
		CHECKING (true),
		/** The TorrentManager is running normally */
		RUNNING (true),
		/** The TorrentManager is stopping. File data may still be being written to disk */
		STOPPING (false),
		/** The TorrentManager is stopped */
		STOPPED (false),
		/** The TorrentManager is stopped after encountering an error */
		ERROR (false),
		/** */
		TERMINATING (false),
		/** */
		TERMINATED (false);

		/**
		 * If {@code true}, the TorrentManager is enabled
		 */
		private final boolean enabled;

		/**
		 * Indicates whether a TorrentManager in this state is enabled
		 *
		 * @return {@code true} if a TorrentManager in this state is enabled, otherwise {@code false}
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * @param enabled {@code true} if a TorrentManager in this state is enabled, otherwise
		 *        {@code false}
		 */
		private BaseState (boolean enabled) {
			this.enabled = enabled;
		}

	}


	/**
	 * The state machine state of a TorrentManager
	 */
	public static class State implements Ordinal {

		/**
		 * The base state
		 */
		public final BaseState baseState;

		/**
		 * If {@code true}, the PieceDatabase has terminated
		 */
		public final boolean databaseTerminated;

		/**
		 * If {@code true}, the tracker client has terminated
		 */
		public final boolean trackerClientTerminated;

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return ordinal();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals (Object other) {
			if (this == other)
				return true;

			if ((other == null)|| (getClass() != other.getClass()))
				return false;

			State otherState = (State) other;
			if (
					   ((this.baseState == null) && (otherState.baseState != null))
					|| (!this.baseState.equals (otherState.baseState))
					|| (this.databaseTerminated != otherState.databaseTerminated)
					|| (this.trackerClientTerminated != otherState.trackerClientTerminated)
			   )
			{
				return false;
			}

			return true;
		}

		/**
		 * @return The cardinality of the state
		 */
		public static int cardinality() {
			return BaseState.values().length * 2 * 2;
		}

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.util.statemachine.Ordinal#ordinal()
		 */
		public int ordinal() {
			return (this.baseState.ordinal() * 2 * 2) + (this.databaseTerminated ? 2 : 0) + (this.trackerClientTerminated ? 1 : 0);
		}

		/**
		 * @param baseState A base state
		 * @param databaseTerminated If {@code true}, the PieceDatabase has terminated
		 * @param trackerClientTerminated If {@code true}, the TrackerClient has terminated
		 */
		public State (BaseState baseState, boolean databaseTerminated, boolean trackerClientTerminated) {
			this.baseState = baseState;
			this.databaseTerminated = databaseTerminated;
			this.trackerClientTerminated = trackerClientTerminated;
		}

	}


	/**
	 * The possible inputs to a TorrentManager's state machine
	 */
	private enum Input implements Ordinal {
		/** Starts the TorrentManager */
		START,
		/** Stops the TorrentManager */
		STOP,
		/** Terminates the TorrentManager */
		TERMINATE,
		/** Indicates that the piece database is available */
		DATABASE_AVAILABLE,
		/** Indicates that the piece database is stopped */
		DATABASE_STOPPED,
		/** Indicates that the piece database is stopped due to an error */
		DATABASE_ERROR,
		/** Indicates that the piece database has terminated */
		DATABASE_TERMINATED,
		/** Indicates that the tracker client has terminated */
		TRACKER_CLIENT_TERMINATED
	}


	/**
	 * The possible actions of a TorrentManager's state machine
	 */
	private enum Action implements TargetedAction<TorrentManager> {
		/** Starts the TorrentManager */
		START,
		/** Stops the TorrentManager */
		STOP,
		/** Terminates the TorrentManager */
		TERMINATE,
		/** Indicates that the TorrentManager is running */
		RUNNING,
		/** Indicates that the TorrentManager is stopped */
		STOPPED,
		/** Indicates that the TorrentManager is stopped due to an error */
		ERROR,
		/** Indicates that the TorrentManager has terminated */
		TERMINATED,
		/** Indicates that an internal error has occurred */
		INTERNAL_ERROR;

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.util.statemachine.TargetedAction#execute(java.lang.Object)
		 */
		public void execute (TorrentManager target) {

			switch (this) {
				case START:           target.actionStart();          break;
				case STOP:            target.actionStop();           break;
				case TERMINATE:       target.actionTerminate();      break;
				case RUNNING:         target.actionRunning();        break;
				case STOPPED:         target.actionStopped();        break;
				case ERROR:           target.actionError();          break;
				case TERMINATED:      target.actionTerminated();     break;
				case INTERNAL_ERROR:  target.actionInternalError();  break;
			}

		}

	}


	/**
	 * Adds a rule or sets of rules to the transition table for a TorrentManager
	 *
	 * @param transitions The transition table
	 * @param baseStateIn The input base state
	 * @param databaseIn The input piece database state
	 * @param trackerIn The input tracker client state
	 * @param input The input
	 * @param baseStateOut The output base state
	 * @param databaseOut The output piece database state
	 * @param trackerOut The output tracker client state
	 * @param action The action
	 */
	private static void addTransitionRules (TransitionTable<State,Input,Action> transitions, BaseState baseStateIn,
			Boolean databaseIn, Boolean trackerIn, Input input, BaseState baseStateOut,
			Boolean databaseOut, Boolean trackerOut, Action action)
	{

		EnumSet<BaseState> baseStateInSet = (baseStateIn == null) ? EnumSet.allOf (BaseState.class) : EnumSet.of (baseStateIn);
		Boolean[] databaseInSet = (databaseIn == null) ? new Boolean[] { true, false } : new Boolean[] { databaseIn };
		Boolean[] trackerInSet = (trackerIn == null) ? new Boolean[] { true, false } : new Boolean[] { trackerIn };

		for (BaseState tableBaseStateIn : baseStateInSet) {
			for (Boolean tableDatabaseIn : databaseInSet) {
				for (Boolean tableTrackerIn : trackerInSet) {
					BaseState tableBaseStateOut = (baseStateOut == null) ? tableBaseStateIn : baseStateOut;
					Boolean tableDatabaseOut = (databaseOut ==  null) ? tableDatabaseIn : databaseOut;
					Boolean tableTrackerOut = (trackerOut == null) ? tableTrackerIn : trackerOut;
					transitions.add (
							new State (tableBaseStateIn, tableDatabaseIn, tableTrackerIn),
							input,
							new State (tableBaseStateOut, tableDatabaseOut, tableTrackerOut),
							action
					);
				}
			}
		}
	}


	/**
	 * Starts the TorrentManager
	 */
	private void actionStart() {

		this.pieceDatabase.start (false);

	}


	/**
	 * Stops the TorrentManager
	 */
	private void actionStop() {

		if (this.maintenanceFuture != null) {
			this.maintenanceFuture.cancel (false);
		}

		this.trackerClient.setEnabled (false);
		this.peerCoordinator.stop();
		this.pieceDatabase.stop (false);

	}


	/**
	 * Terminates the TorrentManager
	 */
	private void actionTerminate() {

		if (this.maintenanceFuture != null) {
			this.maintenanceFuture.cancel (false);
		}
		this.trackerClient.terminate();
		this.peerCoordinator.terminate();
		this.pieceDatabase.terminate (false);

	}


	/**
	 * Indicates that the TorrentManager is running
	 */
	private void actionRunning() {

		// Start the peer set
		TorrentManager.this.peerCoordinator.start();

		// Start the tracker client
		TorrentManager.this.trackerClient.setEnabled (true);

		// Start the periodic maintenance task
		this.maintenanceFuture = TorrentManager.this.workQueue.scheduleWithFixedDelay (this.maintenanceRunnable, 10, 10, TimeUnit.SECONDS);

		// Inform listeners
		synchronized (TorrentManager.this.listeners) {
			for (TorrentManagerListener listener : TorrentManager.this.listeners) {
				listener.torrentManagerRunning (TorrentManager.this);
			}
		}

	}


	/**
	 * Indicates that the TorrentManager is stopped
	 */
	private void actionStopped() {

		synchronized (TorrentManager.this.listeners) {
			for (TorrentManagerListener listener : TorrentManager.this.listeners) {
				listener.torrentManagerStopped (TorrentManager.this);
			}
		}

	}


	/**
	 * Indicates that the TorrentManager is stopped due to an error
	 */
	private void actionError() {

		TorrentManager.this.trackerClient.setEnabled (false);
		TorrentManager.this.maintenanceFuture.cancel (false);
		TorrentManager.this.peerCoordinator.stop();
		synchronized (TorrentManager.this.listeners) {
			for (TorrentManagerListener listener : TorrentManager.this.listeners) {
				listener.torrentManagerError (TorrentManager.this);
			}
		}

	}


	/**
	 * Indicates that the TorrentManager has terminated
	 */
	private void actionTerminated() {

		this.workQueue.shutdown();
		synchronized (TorrentManager.this.listeners) {
			for (TorrentManagerListener listener : TorrentManager.this.listeners) {
				listener.torrentManagerTerminated (TorrentManager.this);
			}
		}

	}


	/**
	 * Indicates that an internal error has occurred
	 */
	private void actionInternalError() {

		throw new IllegalStateException();

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 * 
	 * @return The torrent's peer connection listener
	 */
	PeerConnectionListener getPeerConnectionListener() {

		return this.peerCoordinator;

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 * 
	 * @return The local peer ID
	 */
	public PeerID getLocalPeerID() {

		return this.localPeerID;

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The {@code MetaInfo} of the managed torrent
	 */
	public MetaInfo getMetaInfo() {

		return this.metaInfo;

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The set of pieces that are present
	 */
	public BitField getPresentPieces() {

		// PieceDatabase returns a copy so we don't have to copy again here
		return this.pieceDatabase.getPresentPieces();

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The total number of pieces in the torrent
	 */
	public int getNumberOfPieces() {

		return this.pieceDatabase.getPiecesetDescriptor().getNumberOfPieces();

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The number of pieces that have been verified as either present or absent
	 */
	public int getVerifiedPieceCount() {
		
		return this.pieceDatabase.getVerifiedPieceCount();

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The maximum number of peer connections that this TorrentManager may be connected to
	 */
	public int getMaximumPeerConnections() {

		return this.peerCoordinator.getMaximumPeerConnections();

	}


	/**
	 * Sets the maximum number of peer connections that this TorrentManager may be connected to.
	 * If there are currently more connections than the maximum, currently open connections will be
	 * closed to comply with the limit
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param maximumPeerConnections The maximum number of peer connections
	 */
	public void setMaximumPeerConnections (int maximumPeerConnections) {

		this.peerCoordinator.setMaximumPeerConnections (maximumPeerConnections);

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The maximum number of peer connections that this TorrentManager should proactively
	 *         connect to
	 */
	public int getDesiredPeerConnections() {

		return this.peerCoordinator.getDesiredPeerConnections();

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param desiredPeerConnections The desired number of peer connections. If the value passed is
	 *        greater than the maximum number of connections, the maximum number of connections is
	 *        used instead
	 */
	public void setDesiredPeerConnections (int desiredPeerConnections) {

		this.peerCoordinator.setDesiredPeerConnections (desiredPeerConnections);

	}


	/**
	 * Returns the set of all fully connected peers. The returned set will not be affected by later
	 * additions to or removals from the live peer set, but the attributes of its members may change
	 * at any time.
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 * 
	 * @return The set of all fully connected peers
	 */
	public Set<Peer> getPeers() {

		return new HashSet<Peer> (this.peerCoordinator.getConnectedPeers());

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The set of pieces that are wanted
	 */
	public BitField getWantedPieces() {

		return this.peerCoordinator.getWantedPieces();

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param wantedPieces The set of pieces that are wanted
	 */
	public void setWantedPieces (BitField wantedPieces) {

		this.peerCoordinator.setWantedPieces (wantedPieces);

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 * 
	 * @return The number of bytes per second sent to the remote peer set
	 */
	public int getProtocolBytesSentPerSecond() {

		return (int) this.peerCoordinator.getStatistics().getCounter (PeerStatistics.Type.PROTOCOL_BYTES_SENT).getPeriodTotal (PeerCoordinator.TWO_SECOND_PERIOD) / 2;

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The number of bytes per second received the remote peer set
	 */
	public int getProtocolBytesReceivedPerSecond() {

		return (int) this.peerCoordinator.getStatistics().getCounter (PeerStatistics.Type.PROTOCOL_BYTES_RECEIVED).getPeriodTotal (PeerCoordinator.TWO_SECOND_PERIOD) / 2;

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return An object that can be queried at any time for the current status of the tracker
	 *         client
	 */
	public TrackerClientStatus getTrackerClientStatus() {

		return this.trackerClient;

	}


	/**
	 * Adds a listener for state changes to the TorrentManager
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param listener The listener to add
	 */
	public void addListener (TorrentManagerListener listener) {

		synchronized (this.listeners) {
			this.listeners.add (listener);
		}

	}


	/**
	 * Removes a listener for state changes to the TorrentManager
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param listener The listener to remove
	 */
	public void removeListener (TorrentManagerListener listener) {

		synchronized (this.listeners) {
			this.listeners.remove (listener);
		}

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return {@code true} if the TorrentManager is currently enabled, otherwise {@code false}
	 */
	public boolean isEnabled() {

		return this.stateMachine.getState().baseState.isEnabled();

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return {@code true} if all wanted pieces are present, otherwise {@code false}
	 */
	public boolean isComplete() {

		return (this.peerCoordinator.getNeededPieceCount() == 0);

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 * 
	 * @return The current state of the TorrentManager
	 */
	public TorrentManager.State getState() {

		return this.stateMachine.getState();

	}


	/**
	 * Extends the piece database with additional explicit data, notifying peers as appropriate
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param privateKey The torrent's private key to sign the additional data
	 * @param additionalData The data to append to the database
	 * @throws IOException on any I/O error extending the database
	 */
	public void extendData (PrivateKey privateKey, ByteBuffer additionalData) throws IOException {

		this.peerCoordinator.extendData (privateKey, additionalData);

	}


	/**
	 * Extends the piece database by growing it to cover more of an existing file
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param privateKey The torrent's private key to sign the additional data
	 * @param length The new total length of the torrent, which must be strictly greater than the
	 *        current length
	 * @throws IOException on any I/O error extending the database
	 */
	public void extendDataInPlace (PrivateKey privateKey, long length) throws IOException {

		this.peerCoordinator.extendDataInPlace (privateKey, length);

	}


	/**
	 * Commands the TorrentManager to attempt to start. File data will be checked if required, the
	 * tracker client will be enabled, outbound connections will be proactively formed as peers are
	 * discovered, and inbound connections will be accepted.
	 *
	 * <p>A TorrentManager may be started and stopped multiple times, and may be restarted after
	 * signalling an error.
	 *
	 * <p>Calling this method does not guarantee that the TorrentManager will start successfully; an
	 * error may be encountered before the process of starting is completed. If a call to
	 * {@link #stop(boolean)} or {@link #terminate(boolean)} is made while the start is in progress,
	 * the TorrentManager may be stopped or terminated without ever reaching a running state. If a
	 * stop or termination is requested, or an error occurs while this method is waiting after being
	 * invoked with the {@code synchronous} flag set, it will return immediately.
	 *
	 * <p>If this method is invoked while the TorrentManager is already running, is in the process of
	 * being stopped, or has been terminated, it will have no effect.
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 * 
	 * @param synchronous If {@code true}, the manager is started synchronously, and this method
	 *        will block until the database reaches any of the states RUNNING, STOPPED, ERROR or
	 *        TERMINATED. If {@code false}, the manager is started asynchronously and this method
	 *        will return immediately.
	 */
	public void start (boolean synchronous) {

		if (synchronous) {
			StateMachineUtil.transitionAndWait (this.stateMachine, this.workQueue, Input.START, START_FINAL_STATES);
		} else {
			this.workQueue.execute (new Runnable() {
				public void run() {
					TorrentManager.this.stateMachine.input (Input.START);
				}
			});
		}

	}


	/**
	 * Commands the TorrentManager to stop. All pending and open connections will be closed
	 * immediately, and the tracker client will be instructed to send a "stopped" message to
	 * the tracker, then disabled. (The "stopped" message is sent on a best effort basis, with no
	 * retries.)
	 *
	 * <p>A TorrentManager may be started and stopped multiple times, and may be restarted after
	 * signalling an error.
	 *
	 * <p>Calling this method does not guarantee that the TorrentManager will stop successfully; an
	 * error may be encountered before the process of stopping is completed. If a call to
	 * {@link #terminate(boolean)} is made while the stop is in progress, the TorrentManager may be
	 * terminated without ever reaching a stopped state. If a termination is requested, or an error
	 * occurs while this method is waiting after being invoked with the {@code synchronous} flag set,
	 * it will return immediately.
	 *
	 * <p>If this method is invoked while the TorrentManager is already stopped, is in the process of
	 * being stopped, or has been terminated, it will have no effect.
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 * 
	 * @param synchronous If {@code true}, the manager is stopped synchronously, and this method
	 *        will block until the database reaches any of the states STOPPED, ERROR, or TERMINATED
	 *        If {@code false}, the manager is stopped asynchronously and this method will return
	 *        immediately.
	 */
	public void stop (boolean synchronous) {

		if (synchronous) {
			StateMachineUtil.transitionAndWait (this.stateMachine, this.workQueue, Input.STOP, STOP_FINAL_STATES);
		} else {
			this.workQueue.execute (new Runnable() {
				public void run() {
					TorrentManager.this.stateMachine.input (Input.STOP);
				}
			});
		}

	}


	/**
	 * Commands the TorrentManager to terminate. All internal threads will be terminated and file
	 * resources released. A brief, best effort attempt to sent a "stopped" message to the tracker
	 * will be made.
	 * 
	 * <p>Once terminated, a TorrentManager cannot be restarted.
	 *
	 * <p>If this method is invoked while the TorrentManager is already in the process of being
	 * terminated, or has been terminated, it will have no effect.
	 *
	 * If the {@code PieceDatabase} is fully checked and terminates without error, it will save its
	 * current state through a {@code Metadata} instance if one was registered on construction.
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 * 
	 * @param synchronous If {@code true}, the manager is terminated synchronously, and this method
	 *        will block until the database reaches the state TERMINATED. If {@code false}, the
	 *        manager is terminated asynchronously and this method will return immediately.
	 */
	public void terminate (boolean synchronous) {

		if (synchronous) {
			StateMachineUtil.transitionAndWait (this.stateMachine, this.workQueue, Input.TERMINATE, TERMINATE_FINAL_STATES);
		} else {
			this.workQueue.execute (new Runnable() {
				public void run() {
					TorrentManager.this.stateMachine.input (Input.TERMINATE);
				}
			});
		}

	}


	/**
	 * @param localPeerID The local peer's ID
	 * @param localPort The local peer's port
	 * @param metaInfo The MetaInfo describing the managed torrent
	 * @param connectionManager The ConnectionManager for the managed torrent
	 * @param pieceDatabase The PieceDatabase of the managed torrent
	 * @param wantedPieces The set of pieces that are wanted
	 */
	public TorrentManager (PeerID localPeerID, int localPort, MetaInfo metaInfo, ConnectionManager connectionManager,
			PieceDatabase pieceDatabase, BitField wantedPieces)
	{

		this.localPeerID = localPeerID;
		this.metaInfo = metaInfo;
		this.pieceDatabase = pieceDatabase;

		this.workQueue = new WorkQueue ("TorrentManager WorkQueue - " + CharsetUtil.hexencode (metaInfo.getInfo().getHash().getBytes()));
		this.peerCoordinator = new PeerCoordinator (localPeerID, connectionManager, pieceDatabase, wantedPieces);
		this.peerCoordinator.addListener (this.peerCoordinatorListener);
		this.trackerClient = new TrackerClient (connectionManager, metaInfo.getInfo().getHash(), localPeerID,
				localPort, metaInfo.getAnnounceURLs(), 0, this.peerCoordinator.getDesiredPeerConnections(), this.trackerClientListener);

		pieceDatabase.addListener (this.pieceDatabaseListener);

	}


}
