package org.itadaki.bobbin.trackerclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDecoder;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.bencode.BValue;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.tracker.TrackerEvent;
import org.itadaki.bobbin.util.CharsetUtil;
import org.itadaki.bobbin.util.WorkQueue;
import org.itadaki.bobbin.util.statemachine.Ordinal;
import org.itadaki.bobbin.util.statemachine.StateMachine;
import org.itadaki.bobbin.util.statemachine.TargetedAction;
import org.itadaki.bobbin.util.statemachine.TransitionTable;



/**
 * Updates a tracker with the status of the local peer, and gathers new candidate peers to connect
 * to
 */
public class TrackerClient implements TrackerClientStatus {

	/**
	 * The length of time to wait before trying again if an update attempt failed, in seconds
	 */
	private static final int CONNECTION_FAILED_INTERVAL = 60;

	/**
	 * The transition table for a TrackerClient's state machine
	 */
	private static final TransitionTable<State,Input,Action> TRANSITION_TABLE;

	static {

		final TransitionTable<State,Input,Action> transitions = new TransitionTable<State,Input,Action> (State.cardinality(),Input.values().length);

		final EnumSet<TrackerEvent> TE_ALL = EnumSet.allOf (TrackerEvent.class);
		final EnumSet<TrackerEvent> TE_NOT_STOPPED = EnumSet.complementOf (EnumSet.of (TrackerEvent.STOPPED));

		transitions.add (State.all (BaseState.STOPPED,       TE_ALL),                 Input.START,            new State (BaseState.SENDING, TrackerEvent.STARTED),       Action.SEND);
		transitions.add (new State (BaseState.SENDING,       TrackerEvent.STOPPED),   Input.START,            new State (BaseState.CANCELLING, TrackerEvent.STARTED),    Action.CANCEL);
		transitions.add (State.all (BaseState.CANCELLING,    TE_ALL),                 Input.START,            new State (BaseState.CANCELLING, TrackerEvent.STARTED),    null);

		transitions.add (new State (BaseState.SENDING,       TrackerEvent.UPDATE),    Input.COMPLETED,        new State (BaseState.CANCELLING, TrackerEvent.COMPLETED),  Action.CANCEL);
		transitions.add (new State (BaseState.WAITING,       TrackerEvent.UPDATE),    Input.COMPLETED,        new State (BaseState.WAITING, TrackerEvent.COMPLETED),     Action.TRIGGER_TIMER);

		transitions.add (State.all (BaseState.SENDING,       TE_NOT_STOPPED),         Input.STOP,             new State (BaseState.CANCELLING, TrackerEvent.STOPPED),    Action.CANCEL);
		transitions.add (State.all (BaseState.CANCELLING,    TE_ALL),                 Input.STOP,             new State (BaseState.CANCELLING, TrackerEvent.STOPPED),    null);
		transitions.add (State.all (BaseState.WAITING,       TE_ALL),                 Input.STOP,             new State (BaseState.WAITING, TrackerEvent.STOPPED),       Action.TRIGGER_TIMER);

		transitions.add (State.all (BaseState.STOPPED,       TE_ALL),                 Input.REQUEST_COMPLETE, null,                                                      Action.ERROR);
		transitions.add (new State (BaseState.SENDING,       TrackerEvent.STOPPED),   Input.REQUEST_COMPLETE, new State (BaseState.STOPPED, TrackerEvent.STARTED),       Action.STOPPED);
		transitions.add (State.all (BaseState.SENDING,       TE_NOT_STOPPED),         Input.REQUEST_COMPLETE, new State (BaseState.WAITING, TrackerEvent.UPDATE),        Action.SCHEDULE_UPDATE);
		transitions.add (new State (BaseState.CANCELLING,    TrackerEvent.STARTED),   Input.REQUEST_COMPLETE, new State (BaseState.SENDING, TrackerEvent.STARTED),       Action.SEND);
		transitions.add (new State (BaseState.CANCELLING,    TrackerEvent.UPDATE),    Input.REQUEST_COMPLETE, new State (BaseState.SENDING, TrackerEvent.UPDATE),        Action.SEND);
		transitions.add (new State (BaseState.CANCELLING,    TrackerEvent.COMPLETED), Input.REQUEST_COMPLETE, new State (BaseState.SENDING, TrackerEvent.COMPLETED),     Action.SEND);
		transitions.add (new State (BaseState.CANCELLING,    TrackerEvent.STOPPED),   Input.REQUEST_COMPLETE, new State (BaseState.SENDING, TrackerEvent.STOPPED),       Action.SEND);
		transitions.add (State.all (BaseState.WAITING,       TE_ALL),                 Input.REQUEST_COMPLETE, null,                                                      Action.ERROR);
		transitions.add (new State (BaseState.TERMINATING_1, TrackerEvent.STOPPED),   Input.REQUEST_COMPLETE, new State (BaseState.TERMINATING_2, TrackerEvent.STOPPED), Action.SEND);
		transitions.add (new State (BaseState.TERMINATING_2, TrackerEvent.STOPPED),   Input.REQUEST_COMPLETE, new State (BaseState.TERMINATED, TrackerEvent.STOPPED),    Action.TERMINATED);
		transitions.add (State.all (BaseState.TERMINATED,    TE_ALL),                 Input.REQUEST_COMPLETE, null,                                                      Action.ERROR);

		transitions.add (State.all (BaseState.STOPPED,       TE_ALL),                 Input.REQUEST_FAILED,   null,                                                      Action.ERROR);
		transitions.add (new State (BaseState.SENDING,       TrackerEvent.STOPPED),   Input.REQUEST_FAILED,   new State (BaseState.STOPPED, TrackerEvent.STARTED),       Action.STOPPED);
		transitions.add (new State (BaseState.SENDING,       TrackerEvent.STARTED),   Input.REQUEST_FAILED,   new State (BaseState.WAITING, TrackerEvent.STARTED),       Action.RETRY);
		transitions.add (new State (BaseState.SENDING,       TrackerEvent.UPDATE),    Input.REQUEST_FAILED,   new State (BaseState.WAITING, TrackerEvent.STARTED),       Action.RETRY);
		transitions.add (new State (BaseState.SENDING,       TrackerEvent.COMPLETED), Input.REQUEST_FAILED,   new State (BaseState.WAITING, TrackerEvent.STARTED),       Action.RETRY);
		transitions.add (new State (BaseState.CANCELLING,    TrackerEvent.STARTED),   Input.REQUEST_FAILED,   new State (BaseState.SENDING, TrackerEvent.STARTED),       Action.SEND);
		transitions.add (new State (BaseState.CANCELLING,    TrackerEvent.UPDATE),    Input.REQUEST_FAILED,   new State (BaseState.SENDING, TrackerEvent.UPDATE),        Action.SEND);
		transitions.add (new State (BaseState.CANCELLING,    TrackerEvent.COMPLETED), Input.REQUEST_FAILED,   new State (BaseState.SENDING, TrackerEvent.COMPLETED),     Action.SEND);
		transitions.add (new State (BaseState.CANCELLING,    TrackerEvent.STOPPED),   Input.REQUEST_FAILED,   new State (BaseState.SENDING, TrackerEvent.STOPPED),       Action.SEND);
		transitions.add (State.all (BaseState.WAITING,       TE_ALL),                 Input.REQUEST_FAILED,   null,                                                      Action.ERROR);
		transitions.add (new State (BaseState.TERMINATING_1, TrackerEvent.STOPPED),   Input.REQUEST_FAILED,   new State (BaseState.TERMINATING_2, TrackerEvent.STOPPED), Action.SEND);
		transitions.add (new State (BaseState.TERMINATING_2, TrackerEvent.STOPPED),   Input.REQUEST_FAILED,   new State (BaseState.TERMINATED, TrackerEvent.STOPPED),    Action.TERMINATED);
		transitions.add (State.all (BaseState.TERMINATED,    TE_ALL),                 Input.REQUEST_FAILED,   null,                                                      Action.TERMINATED);

		transitions.add (State.all (BaseState.STOPPED,       TE_ALL),                 Input.TIMEOUT,          null,                                                      Action.ERROR);
		transitions.add (State.all (BaseState.SENDING,       TE_ALL),                 Input.TIMEOUT,          null,                                                      Action.ERROR);
		transitions.add (State.all (BaseState.CANCELLING,    TE_ALL),                 Input.TIMEOUT,          null,                                                      Action.ERROR);
		transitions.add (new State (BaseState.WAITING,       TrackerEvent.STARTED),   Input.TIMEOUT,          new State (BaseState.SENDING, TrackerEvent.STARTED),       Action.SEND);
		transitions.add (new State (BaseState.WAITING,       TrackerEvent.UPDATE),    Input.TIMEOUT,          new State (BaseState.SENDING, TrackerEvent.UPDATE),        Action.SEND);
		transitions.add (new State (BaseState.WAITING,       TrackerEvent.COMPLETED), Input.TIMEOUT,          new State (BaseState.SENDING, TrackerEvent.COMPLETED),     Action.SEND);
		transitions.add (new State (BaseState.WAITING,       TrackerEvent.STOPPED),   Input.TIMEOUT,          new State (BaseState.SENDING, TrackerEvent.STOPPED),       Action.SEND);
		transitions.add (new State (BaseState.TERMINATING_1, TrackerEvent.STOPPED),   Input.TIMEOUT,          new State (BaseState.TERMINATING_2, TrackerEvent.STOPPED), Action.SEND);
		transitions.add (new State (BaseState.TERMINATING_2, TrackerEvent.STOPPED),   Input.TIMEOUT,          null,                                                      Action.ERROR);
		transitions.add (State.all (BaseState.TERMINATED,    TE_ALL),                 Input.TIMEOUT,          null,                                                      Action.TERMINATED);

		transitions.add (State.all (BaseState.STOPPED,       TE_ALL),                 Input.TERMINATE,        new State (BaseState.TERMINATED, TrackerEvent.STOPPED),    Action.TERMINATED);
		transitions.add (new State (BaseState.SENDING,       TrackerEvent.STOPPED),   Input.TERMINATE,        new State (BaseState.TERMINATING_2, TrackerEvent.STOPPED), null);
		transitions.add (State.all (BaseState.SENDING,       TE_NOT_STOPPED),         Input.TERMINATE,        new State (BaseState.TERMINATING_1, TrackerEvent.STOPPED), Action.CANCEL);
		transitions.add (State.all (BaseState.CANCELLING,    TE_ALL),                 Input.TERMINATE,        new State (BaseState.TERMINATING_1, TrackerEvent.STOPPED), null);
		transitions.add (State.all (BaseState.WAITING,       TE_ALL),                 Input.TERMINATE,        new State (BaseState.TERMINATING_1, TrackerEvent.STOPPED), Action.TRIGGER_TIMER);

		TRANSITION_TABLE = transitions;

	}

	/**
	 * The client's state machine
	 */
	private final StateMachine<TrackerClient,State,Input,Action> stateMachine =
		new StateMachine<TrackerClient, State, Input, Action> (this, TRANSITION_TABLE, new State (BaseState.STOPPED, TrackerEvent.STARTED));

	/**
	 * The tracker client's ConnectionManager
	 */
	private final ConnectionManager connectionManager;

	/**
	 * The listener to inform of newly discovered peers and client lifecycle events
	 */
	private final TrackerClientListener listener;

	/**
	 * A work queue used to perform state changes asynchronously
	 */
	private final WorkQueue workQueue;

	/**
	 * The info hash of the torrent that we are talking to the tracker about
	 */
	private final InfoHash infoHash;

	/**
	 * The local peer's peer ID
	 */
	private final PeerID localPeerID;

	/**
	 * The local peer's port
	 */
	private final int localPort;

	/**
	 * The set of tracker URLs
	 */
	private final List<List<String>> announceURLs;

	/**
	 * The peer key used to prove our identity (to trackers that support it) should our IP change
	 */
	private final String peerKey = "" + new Random().nextLong();

	/**
	 * The ScheduledFuture of a task that will invoke stateMachine.inputTimeout() after a timeout
	 * has elapsed
	 */
	private ScheduledFuture<?> timeoutFuture;

	/**
	 * The index of the current tracker tier
	 */
	private int currentTierIndex = 0;

	/**
	 * The index of the current tracker within the current tier
	 */
	private int currentTrackerIndex = 0;

	/**
	 * If not {@code null}, a token sent by the tracker 
	 */
	private String trackerID = null;

	/**
	 * The system time in milliseconds of the next scheduled update
	 */
	private Long nextUpdateTime;

	/**
	 * The number of peers to be requested from the tracker
	 */
	private int peersWanted = 50;

	/**
	 * The number of bytes uploaded by the local peer
	 */
	private long uploaded = 0;

	/**
	 * The number of bytes downloaded by the local peer
	 */
	private long downloaded = 0;

	/**
	 * The number of bytes the local peer needs to download to have a complete torrent
	 */
	private long remaining = 0;

	/**
	 * The tracker's minimum interval between updates, in seconds
	 */
	private int minimumUpdateInterval = CONNECTION_FAILED_INTERVAL;

	/**
	 * The tracker's preferred interval between updates, in seconds
	 */
	private int preferredUpdateInterval = 30 * 60;

	/**
	 * The tracker's reported number of complete peers (seeds)
	 */
	private int complete = 0;

	/**
	 * The tracker's reported number of incomplete peers (downloaders)
	 */
	private int incomplete = 0;

	/**
	 * The system time in milliseconds of the last successful tracker update
	 */
	private Long lastUpdateTime = null;

	/**
	 * The most recent failure or warning returned by the tracker
	 */
	private String failureReason = null;

	/**
	 * The handler for the HTTP request that is in progress if any, or {@code null}
	 */
	private HTTPRequestHandler requestHandler = null;

	/**
	 * The listener for the result of the tracker HTTP requests
	 */
	private final HTTPRequestListener requestListener = new HTTPRequestListener() {

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.trackerclient.HTTPRequestListener#requestComplete(org.itadaki.bobbin.trackerclient.HTTPRequestHandler)
		 */
		public void requestComplete (HTTPRequestHandler request) {

			List<PeerIdentifier> identifiers = null;

			synchronized (TrackerClient.this) {
				TrackerClient.this.requestHandler = null;

				// Attempt to process the response
				BDictionary responseDictionary = null;
				HTTPResponse response = request.getResponse();

				if (response.isStateComplete()) {
					byte[] responseData = response.getResponseBody();
					try {
						responseDictionary = new BDecoder(responseData).decodeDictionary();

						TrackerClient.this.lastUpdateTime = System.currentTimeMillis();
						TrackerClient.this.failureReason = responseDictionary.getString ("failure reason");

						if (TrackerClient.this.failureReason == null) {

							BValue rawMinimumUpdateInterval = responseDictionary.get ("min interval");
							if ((rawMinimumUpdateInterval != null) && (rawMinimumUpdateInterval instanceof BInteger) ) {
								int minimumUpdateInterval = ((BInteger)rawMinimumUpdateInterval).value().intValue();
								if (minimumUpdateInterval > 0) {
									TrackerClient.this.minimumUpdateInterval = Math.max (CONNECTION_FAILED_INTERVAL, minimumUpdateInterval);
								}
							}

							BValue rawPreferredUpdateInterval = responseDictionary.get ("interval");
							if ((rawPreferredUpdateInterval != null) && (rawPreferredUpdateInterval instanceof BInteger) ) {
								int preferredUpdateInterval = ((BInteger)rawPreferredUpdateInterval).value().intValue();
								if (preferredUpdateInterval > 0) {
									TrackerClient.this.preferredUpdateInterval = Math.max (CONNECTION_FAILED_INTERVAL, preferredUpdateInterval);
								}
							}

							String trackerID = responseDictionary.getString ("tracker id");
							if (trackerID != null) {
								TrackerClient.this.trackerID = trackerID;
							}

							String warningMessage = responseDictionary.getString ("warning message");
							if (warningMessage != null) {
								TrackerClient.this.failureReason = warningMessage;
							}

							BValue complete = responseDictionary.get ("complete");
							if (complete instanceof BInteger) {
								TrackerClient.this.complete = ((BInteger)complete).value().intValue();
							}

							BValue incomplete = responseDictionary.get ("incomplete");
							if (incomplete instanceof BInteger) {
								TrackerClient.this.incomplete = ((BInteger)incomplete).value().intValue();
							}

							// Decode received peers
							identifiers = decodePeers (responseDictionary.get ("peers"));

						}

					} catch (IOException e) {
						// On connection error, fall back to the minimum interval
						TrackerClient.this.minimumUpdateInterval = CONNECTION_FAILED_INTERVAL;
					}
				}

			}

			final List<PeerIdentifier> finalIdentifiers = identifiers;
			TrackerClient.this.workQueue.execute (new Runnable() {
				public void run() {
					if (finalIdentifiers != null) {
						TrackerClient.this.listener.peersDiscovered (finalIdentifiers);
						TrackerClient.this.stateMachine.input (Input.REQUEST_COMPLETE);
					} else {
						TrackerClient.this.stateMachine.input (Input.REQUEST_FAILED);
					}
				}
			});

		}

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.trackerclient.HTTPRequestListener#requestError(org.itadaki.bobbin.trackerclient.HTTPRequestHandler)
		 */
		public void requestError (HTTPRequestHandler requestHandler) {

			TrackerClient.this.workQueue.execute (new Runnable() {
				public void run() {
					TrackerClient.this.stateMachine.input (Input.REQUEST_FAILED);
				}
			});

		}

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.trackerclient.HTTPRequestListener#requestCancelled(org.itadaki.bobbin.trackerclient.HTTPRequestHandler)
		 */
		public void requestCancelled (HTTPRequestHandler requestHandler) {

			requestError (requestHandler);

		}

	};


	/**
	 * The possible basic states of a TrackerClient
	 */
	public static enum BaseState {
		/** The client is stopped */
		STOPPED,
		/** The client is sending an event */
		SENDING,
		/** The client is waiting to send or retry an event */
		WAITING,
		/** The client is cancelling the HTTP request of a previously started event */
		CANCELLING,
		/** The client is waiting for a previous event or timeout before termination */
		TERMINATING_1,
		/** The client is sending a stopped event before termination */
		TERMINATING_2,
		/** The client has been terminated */
		TERMINATED
	}


	/**
	 * The state machine state of a TrackerClient
	 */
	public static class State implements Ordinal {

		/**
		 * The basic state
		 */
		public final BaseState baseState;

		/**
		 * The current event
		 */
		public final TrackerEvent event;

		/**
		 * @return The cardinality of the state
		 */
		public static int cardinality() {
			return BaseState.values().length * TrackerEvent.values().length;
		}

		/**
		 * @param baseState A base state
		 * @param events A set of events
		 * @return A set of states permuting the base state with the events
		 */
		public static Set<State> all (BaseState baseState, Set<TrackerEvent> events) {
			Set<State> states = new HashSet<State>();
			for (TrackerEvent event : events)
				states.add (new State (baseState,event));
			return states;
		}

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

			if ((other == null) || (getClass () != other.getClass ()))
				return false;
			State otherState = (State) other;
			if (!this.baseState.equals (otherState.baseState) || !this.event.equals (otherState.event))
				return false;

			return true;

		}

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.util.statemachine.Ordinal#ordinal()
		 */
		public int ordinal() {

			return (this.event.ordinal() * BaseState.values().length) + this.baseState.ordinal();

		}

		/**
		 * @param baseState
		 * @param event
		 */
		public State (BaseState baseState, TrackerEvent event) {

			this.baseState = baseState;
			this.event = event;

		}

	}


	/**
	 * The possible inputs to the TrackerClient's state machine
	 */
	private enum Input implements Ordinal {
		/** Send a "started" event and then periodic "updated" events until told otherwise */
		START,
		/** Send a "completed" event */
		COMPLETED,
		/** Send a "stopped" event, then do nothing until told otherwise */
		STOP,
		/** A tracker action has completed successfully */
		REQUEST_COMPLETE,
		/** A tracker action has failed*/
		REQUEST_FAILED,
		/** The timeout before sending or retrying an event has elapsed*/
		TIMEOUT,
		/** Send a "stopped" event if possible to do so quickly, then do nothing forever */
		TERMINATE
	}


	/**
	 * The possible actions of the TrackerClient's state machine
	 */
	private enum Action implements TargetedAction<TrackerClient> {
		/** Initiate the sending of an HTTP request */
		SEND,
		/** Initiate a timer for the next tracker action */
		SCHEDULE_UPDATE,
		/** Initiate a timer to retry the current tracker action */
		RETRY,
		/** Cancel the currently in progress HTTP request*/
		CANCEL,
		/** Trigger the currently in progress timer to occur immediately */
		TRIGGER_TIMER,
		/** Update peripheral state to reflect the fact that no update is scheduled */
		STOPPED,
		/** Notify the listener that the client has terminated*/
		TERMINATED,
		/** Do something in response to an invalid state transition */
		ERROR;

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.util.statemachine.TargetedAction#execute(java.lang.Object)
		 */
		public void execute (TrackerClient target) {

			switch (this) {
				case SEND:             target.actionSend();            break;
				case SCHEDULE_UPDATE:  target.actionScheduleUpdate();  break;
				case RETRY:            target.actionRetry();           break;
				case CANCEL:           target.actionCancelRequest();   break;
				case TRIGGER_TIMER:    target.actionTriggerTimer();    break;
				case STOPPED:          target.actionStopped();         break;
				case TERMINATED:       target.actionTerminated();      break;
				case ERROR:            target.actionError();           break;
			}

		}

	}


	/* TrackerClientStatus interface */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.TrackerClientStatus#isUpdating()
	 */
	public boolean isUpdating() {

		return this.stateMachine.getState().baseState == BaseState.SENDING;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.TrackerClientStatus#getTimeUntilNextUpdate()
	 */
	public synchronized Integer getTimeUntilNextUpdate() {

		Long nextUpdateTime = this.nextUpdateTime;
		if (nextUpdateTime == null) {
			return null;
		}
		return Math.max (0, (int)((nextUpdateTime - System.currentTimeMillis()) / 1000));

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.TrackerClientStatus#getTimeOfLastUpdate()
	 */
	public synchronized Long getTimeOfLastUpdate() {

		return this.lastUpdateTime;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.TrackerClientStatus#getFailureReason()
	 */
	public synchronized String getFailureReason() {

		return this.failureReason;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.TrackerClientStatus#getCompleteCount()
	 */
	public synchronized int getCompleteCount() {

		return this.complete;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.TrackerClientStatus#getIncompleteCount()
	 */
	public synchronized int getIncompleteCount() {

		return this.incomplete;

	}


	/**
	 * Decodes a "peers" BValue into a list of PeerIdentifiers
	 *
	 * @param rawPeers The BValue representing the peers
	 * @return A list of PeerIdentifiers
	 */
	private static List<PeerIdentifier> decodePeers (BValue rawPeers) {

		List<PeerIdentifier> identifiers = new ArrayList<PeerIdentifier>();

		if (rawPeers instanceof BList) {
			BList peers = (BList) rawPeers;
			for (BValue rawPeer : peers) {
				if (rawPeer instanceof BDictionary) {
					BDictionary peer = (BDictionary) rawPeer;
					BValue rawPeerID = peer.get ("peer id");
					BValue rawHost = peer.get ("ip");
					BValue rawPort = peer.get ("port");
					if ((rawPeerID instanceof BBinary) && (rawHost instanceof BBinary) && (rawPort instanceof BInteger)) {
						byte[] peerID = ((BBinary)rawPeerID).value();
						String host = ((BBinary)rawHost).stringValue();
						int port = ((BInteger)rawPort).value().intValue();
						if ((peerID.length == 20) && (host.length() > 0) && (port > 0) && (port < 65536)) {
							PeerIdentifier identifier = new PeerIdentifier (peerID, host, port);
							identifiers.add (identifier);
						}
					}
				}
			}
		} else if (rawPeers instanceof BBinary) {
			BBinary peers = (BBinary) rawPeers;
			byte[] peerBytes = peers.value();
			if ((peerBytes.length % 6) == 0) {
				for (int position = 0; position < peerBytes.length; position += 6) {
					String host = String.format ("%d.%d.%d.%d", (peerBytes[position] & 0xff), (peerBytes[position+1] & 0xff),
							(peerBytes[position+2] & 0xff), (peerBytes[position+3] & 0xff));
					byte[] addressBytes = new byte[4];
					System.arraycopy (peerBytes, position, addressBytes, 0, 4);
					int port = ((peerBytes[position + 4] & 0xff) << 8) + (peerBytes[position + 5] & 0xff);
					PeerIdentifier identifier = new PeerIdentifier (null, host, port);
					identifiers.add (identifier);
				}
			}
		}

		return identifiers;

	}


	/**
	 * Encodes a tracker request into a GET request string
	 *
	 * @param hostname The tracker's hostname if known, or {@code null}
	 * @param filePath The URL path of the tracker
	 * @param infoHash The local peer's info hash
	 * @param peerID The local peer's peer ID
	 * @param peerKey The peer key
	 * @param trackerID The tracker ID
	 * @param port The local peer's port
	 * @param uploaded The bytes uploaded
	 * @param downloaded The bytes downloaded
	 * @param remaining The bytes remaining
	 * @param peersWanted The number of peers to ask the tracker for
	 * @param event The event to send
	 * @param compact If {@code true}, request peers in the "compact" format
	 * @return An encoded GET request string
	 */
	private static String buildTrackerRequest (String hostname, String filePath, InfoHash infoHash, PeerID peerID, String peerKey,
			String trackerID, Integer port, long uploaded, long downloaded, long remaining, int peersWanted, String event, boolean compact)
	{

		String[] parameters = new String[] {
				"info_hash", CharsetUtil.urlencode (infoHash.getBytes()),
				"peer_id", CharsetUtil.urlencode (peerID.getBytes()),
				"trackerid", trackerID,
				"key", peerKey,
				"port", "" + port,
				"uploaded", "" + uploaded,
				"downloaded", "" + downloaded,
				"left", "" + remaining,
				"numwant", "" + peersWanted,
				"event", event,
				"compact", compact ? "1" : "0"
		};

		StringBuilder encodedRequest = new StringBuilder();
		encodedRequest.append (filePath);
		encodedRequest.append ("?");

		for (int i = 0; i < parameters.length; i += 2) {
			if (parameters[i+1] != null) {
				encodedRequest.append (parameters[i]);
				encodedRequest.append ("=");
				encodedRequest.append (parameters[i+1]);
				if (i < (parameters.length - 2)) {
					encodedRequest.append ("&");
				}
			}
		}

		return encodedRequest.toString();

	}


	/**
	 * Initiates sending a request for the event in the register
	 */
	private void actionSend() {

		URL url = null;
		InetAddress trackerAddress = null;
		String trackerHostname = null;
		int trackerPort = -1;
		String trackerPath = null;

		// Gather the tracker's contact details
		try {

			// Resolve the tracker's address
			url = new URL (this.announceURLs.get(this.currentTierIndex).get (this.currentTrackerIndex));
			if (!url.getProtocol().equals ("http") || (url.getUserInfo() != null)) {
				throw new MalformedURLException();
			}
			trackerAddress = InetAddress.getByName (url.getHost());

			// The hostname will be null if the URL contains an IPv4 or IPv6 literal address
			if (!(url.getHost().matches ("\\A\\d+\\.\\d+\\.\\d+\\.\\d+\\z") || url.getHost().startsWith ("["))) {
				trackerHostname = url.getHost();
			}

			trackerPort = url.getPort() == -1 ? 80 : url.getPort();
			trackerPath = url.getFile();
			State state = this.stateMachine.getState();

			// Build the request to be sent to the tracker
			String request;
			synchronized (this) {
				request = buildTrackerRequest (trackerHostname, trackerPath, this.infoHash, this.localPeerID, this.peerKey,
						this.trackerID, this.localPort, this.uploaded, this.downloaded, this.remaining, this.peersWanted,
						state.event.getName(), true);
			}

			// Stopped event on termination is sent with a short timeout
			int timeout;
			if (state.baseState == BaseState.TERMINATING_2) {
				timeout = 10;
			} else {
				timeout = 60;
			}

			// Initiate a connection to the tracker
			this.requestHandler = new HTTPRequestHandler (this.connectionManager, this.workQueue, trackerAddress, trackerPort, trackerHostname,
					request, timeout, this.requestListener);

			synchronized (this) {
				this.nextUpdateTime = null;
			}

		} catch (IOException e) {
			this.workQueue.execute (new Runnable() {
				public void run() {
					TrackerClient.this.stateMachine.input (Input.REQUEST_FAILED);
				}
			});
		}

	}


	/**
	 * Initiates a timer for the regular update interval
	 */
	private void actionScheduleUpdate() {

		long delay = this.preferredUpdateInterval * 1000;
		this.timeoutFuture = this.workQueue.schedule (new Runnable() {
			public void run() {
				TrackerClient.this.stateMachine.input (Input.TIMEOUT);
			}
		}, delay, TimeUnit.MILLISECONDS);
		this.nextUpdateTime = System.currentTimeMillis() + delay;

	}


	/**
	 * Initiates a timer for the retry interval
	 */
	private void actionRetry() {

		// Select the next tracker if one is available
		if (this.currentTrackerIndex < (this.announceURLs.get (this.currentTierIndex).size() - 1)) {
			this.currentTrackerIndex++;
		} else {
			this.currentTierIndex = (this.currentTierIndex + 1) % this.announceURLs.size();
			this.currentTrackerIndex = 0;
		}

		long delay = this.minimumUpdateInterval * 1000;
		this.timeoutFuture = this.workQueue.schedule (new Runnable() {
			public void run() {
				TrackerClient.this.stateMachine.input (Input.TIMEOUT);
			}
		}, delay, TimeUnit.MILLISECONDS);
		this.nextUpdateTime = System.currentTimeMillis() + delay;

	}


	/**
	 * Attempts to cancel the currently issued request
	 */
	private void actionCancelRequest() {

		this.requestHandler.cancel();

	}


	/**
	 * Triggers the currently effective timer
	 *
	 */
	private void actionTriggerTimer() {

		if (this.timeoutFuture.cancel (false)) {
			this.workQueue.execute (new Runnable() {
				public void run() {
					TrackerClient.this.stateMachine.input (Input.TIMEOUT);
				}
			});
		}

	}


	/**
	 * Updates peripheral state to reflect the fact that the client is stopped
	 */
	private void actionStopped() {

		this.nextUpdateTime = null;

	}


	/**
	 * Notifies listeners that the client has terminated
	 */
	private void actionTerminated() {

		this.workQueue.shutdown();
		this.listener.trackerClientTerminated();

	}


	/**
	 * Dies on an invalid state change
	 */
	private void actionError() {

		throw new IllegalStateException();

	}


	/**
	 * Sets the number of peers to ask the tracker for on the next update
	 *
	 * @param peersWanted The number of peers to ask for
	 */
	public synchronized void setPeersWanted (int peersWanted) {

		this.peersWanted = peersWanted;

	}


	/**
	 * Updates the statistics to be sent to the tracker
	 *
	 * @param uploaded The peer's bytes uploaded
	 * @param downloaded The peer's bytes downloaded
	 * @param remaining The peer's bytes remaining
	 */
	public synchronized void updateLocalPeerStatistics (long uploaded, long downloaded, long remaining) {

		this.uploaded = uploaded;
		this.downloaded = downloaded;
		this.remaining = remaining;

	}


	/**
	 * Triggers the sending of a "completed" event to the tracker.
	 */
	public void peerCompleted() {

		this.workQueue.execute (new Runnable() {
			public void run() {
				TrackerClient.this.stateMachine.input (Input.COMPLETED);
			}
		});

	}


	/**
	 * Terminates the tracker client
	 */
	public void terminate() {

		this.workQueue.execute (new Runnable() {
			public void run() {
				TrackerClient.this.stateMachine.input (Input.TERMINATE);
			}
		});

	}


	/**
	 * Sets the enabled state. If the requested state is already set, no action will be taken.
	 *
	 * @param enabled If {@code true}, the tracker client will be enabled and will send updates to
	 *                the tracker. If {@code false}, no further updates are sent after a final
	 *                "stopped" event.
	 */
	public void setEnabled (final boolean enabled) {

		this.workQueue.execute (new Runnable() {
			public void run() {
				if (enabled) {
					TrackerClient.this.stateMachine.input (Input.START);
				} else {
					TrackerClient.this.stateMachine.input (Input.STOP);
				}
			}
		});

	}


	/**
	 * @param connectionManager The ConnectionManager to manage the client's connections
	 * @param infoHash The info hash of the torrent to report on
	 * @param localPeerID The local peer's peer ID
	 * @param localPort The local peer's port
	 * @param announceURLs The URLs of the trackers to contact
	 * @param remaining The initial remaining bytes needed for the peer to have a complete torrent
	 * @param peersWanted The initial number of peers wanted
	 * @param listener The PeerSourceListener to inform of peers returned by the tracker
	 */
	public TrackerClient (ConnectionManager connectionManager, InfoHash infoHash, PeerID localPeerID, int localPort,
			List<List<String>> announceURLs, long remaining, int peersWanted, TrackerClientListener listener)
	{

		this.connectionManager = connectionManager;
		this.infoHash = infoHash;
		this.localPeerID = localPeerID;
		this.localPort = localPort;
		this.announceURLs = announceURLs;
		this.remaining = remaining;
		this.listener = listener;

		for (List<String> tierAnnounceURLs : this.announceURLs) {
			Collections.shuffle (tierAnnounceURLs);
		}

		this.workQueue = new WorkQueue ("TrackerClient WorkQueue - " + CharsetUtil.hexencode (infoHash.getBytes()));

	}


}


