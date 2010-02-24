package org.itadaki.bobbin.trackerclient;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.connectionmanager.ConnectionReadyListener;
import org.itadaki.bobbin.connectionmanager.OutboundConnectionListener;
import org.itadaki.bobbin.util.CharsetUtil;
import org.itadaki.bobbin.util.WorkQueue;
import org.itadaki.bobbin.util.statemachine.Ordinal;
import org.itadaki.bobbin.util.statemachine.StateMachine;
import org.itadaki.bobbin.util.statemachine.TargetedAction;
import org.itadaki.bobbin.util.statemachine.TransitionTable;



/**
 * Asynchronously launches an HTTP request with a timeout, and informs a listener when the entire
 * response has been buffered, an error occurred, or the request timed out
 */
public class HTTPRequestHandler {

	/**
	 * The transition table for an HTTPRequestHandler's state machine
	 */
	private static final TransitionTable<State,Input,Action> TRANSITION_TABLE = buildTransitionTable();

	/**
	 * The request handler's state machine
	 */
	private final StateMachine<HTTPRequestHandler,State,Input,Action> stateMachine =
		new StateMachine<HTTPRequestHandler, State, Input, Action> (this, TRANSITION_TABLE, State.STOPPED);

	/**
	 * The ConnectionManager used to manage the outgoing connection to the web server
	 */
	private final ConnectionManager connectionManager;

	/**
	 * The Timer used to monitor the timeout for the request
	 */
	private final WorkQueue workQueue;

	/**
	 * The listener to notify when the request is complete or has failed
	 */
	private final HTTPRequestListener listener;

	/**
	 * The bytes of the request being sent to the web server
	 */
	private final ByteBuffer sendBuffer;

	/**
	 * The parser used to process the response received from the web server
	 */
	private final HTTPResponseParser responseParser;

	/**
	 * The address of the host to connect to
	 */
	private final InetAddress serverAddress;

	/**
	 * The port of the host to connect to
	 */
	private final int serverPort;

	/**
	 * The length in milliseconds of the timeout to apply once the request handler has started
	 */
	private final int timeoutLength;

	/**
	 * The ScheduledFuture of a task that will invoke stateMachine.inputCancel() after a timeout
	 * has elapsed
	 */
	private ScheduledFuture<?> timeoutFuture;

	/**
	 * The deadline after which the request will be cancelled, in system milliseconds (calculated
	 * after the connection is open)
	 */
	private Long deadline;

	/**
	 * The request's Connection
	 */
	private Connection connection;

	/**
	 * A listener for the connection status of the connection to the remote host
	 */
	private OutboundConnectionListener outboundConnectionListener = new OutboundConnectionListener() {

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.connectionmanager.OutboundConnectionListener#connected(org.itadaki.bobbin.connectionmanager.Connection)
		 */
		public void connected (final Connection connection) {

			HTTPRequestHandler.this.stateMachine.input (Input.CONNECTED);

		}

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.connectionmanager.OutboundConnectionListener#rejected(org.itadaki.bobbin.connectionmanager.Connection)
		 */
		public void rejected (Connection connection) {

			HTTPRequestHandler.this.stateMachine.input (Input.ERROR);

		}

	};

	/**
	 * A listener for the connection to the remote host
	 */
	private ConnectionReadyListener connectionReadyListener = new ConnectionReadyListener() {

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.connectionmanager.ConnectionReadyListener#connectionReady(org.itadaki.bobbin.connectionmanager.Connection, boolean, boolean)
		 */
		public void connectionReady (Connection connection, boolean readable, boolean writeable) {

			if (readable) {
				boolean parserResult;
				synchronized (HTTPRequestHandler.this.responseParser) {
					parserResult = HTTPRequestHandler.this.responseParser.parseBytes (connection);
				}
				if (!parserResult) {
					if (HTTPRequestHandler.this.responseParser.isStateComplete()) {
						HTTPRequestHandler.this.stateMachine.input (Input.COMPLETED);
					} else {
						HTTPRequestHandler.this.stateMachine.input (Input.ERROR);
					}
				}
			}

			if (writeable) {
				try {
					connection.write (HTTPRequestHandler.this.sendBuffer);
					if (!HTTPRequestHandler.this.sendBuffer.hasRemaining()) {
						connection.setWriteEnabled (false);
					}
				} catch (IOException e) {
					HTTPRequestHandler.this.stateMachine.input (Input.ERROR);
				}
			}

		}

	};


	/**
	 * The state of an HTTPRequestHandler
	 */
	public enum State implements Ordinal {
		/** The HTTPRequestHandler has not yet started */
		STOPPED,
		/** The HTTPRequestHandler is connecting to the remote host */
		CONNECTING,
		/** The HTTPRequestHandler is communicating with the remote host */
		RUNNING,
		/** The HTTPRequestHandler successfully completed */
		COMPLETE,
		/** The HTTPRequestHandler encountered an I/O or parse error, was cancelled or timed out*/
		ERROR
	}


	/**
	 * The possible inputs to an HTTPRequestHandler's state machine
	 */
	public enum Input implements Ordinal {
		/** Starts the request */
		START,
		/** Cancels the request */
		CANCEL,
		/** Signals that a connection to the remote host has been opened */
		CONNECTED,
		/** Signals that the request has completed successfully */
		COMPLETED,
		/** Signals that an I/O error has occurred */
		ERROR,
	}


	/**
	 * The possible actions of an HTTPRequestHandler's state machine
	 */
	public enum Action implements TargetedAction<HTTPRequestHandler> {
		/** Starts the request */
		START,
		/** Initiates the watchdog timer and sets up the connection to write the request */
		RUN,
		/** Notifies the listener that the request has completed */
		COMPLETED,
		/** Cancels the timer and notifies the listener that the request has been cancelled */
		CANCEL,
		/** Notifies the listener that the request has encountered an I/O error */
		ERROR,
		/** Indicates an invalid state transition has occurred */
		INTERNAL_ERROR;

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.util.statemachine.TargetedAction#execute(java.lang.Object)
		 */
		public void execute(HTTPRequestHandler target) {

			switch (this) {
				case START:           target.actionStart();          break;
				case RUN:             target.actionRun();            break;
				case COMPLETED:       target.actionCompleted();      break;
				case CANCEL:          target.actionCancel();         break;
				case ERROR:           target.actionError();          break;
				case INTERNAL_ERROR:  target.actionInternalError();  break;
			}

		}

	}


	/**
	 * @return The transition table for an HTTPRequestHandler
	 */
	private static TransitionTable<State, Input, Action> buildTransitionTable() {

		TransitionTable<State,Input,Action> transitions = new TransitionTable<State,Input,Action>(State.values().length, Input.values().length);

		EnumSet<State> NOT_STOPPED = EnumSet.complementOf (EnumSet.of (State.STOPPED));
		EnumSet<State> NOT_CONNECTING = EnumSet.complementOf (EnumSet.of (State.CONNECTING));

		transitions.add (State.STOPPED,    Input.START,     State.CONNECTING, Action.START);
		transitions.add (NOT_STOPPED,      Input.START,     null,             Action.INTERNAL_ERROR);
		transitions.add (State.STOPPED,    Input.CANCEL,    null,             Action.INTERNAL_ERROR);
		transitions.add (State.CONNECTING, Input.CANCEL,    State.ERROR,      Action.CANCEL);
		transitions.add (State.RUNNING,    Input.CANCEL,    State.ERROR,      Action.CANCEL);
		transitions.add (State.CONNECTING, Input.CONNECTED, State.RUNNING,    Action.RUN);
		transitions.add (NOT_CONNECTING,   Input.CONNECTED, null,             Action.INTERNAL_ERROR);
		transitions.add (State.STOPPED,    Input.COMPLETED, null,             Action.INTERNAL_ERROR);
		transitions.add (State.CONNECTING, Input.COMPLETED, null,             Action.INTERNAL_ERROR);
		transitions.add (State.RUNNING,    Input.COMPLETED, State.COMPLETE,   Action.COMPLETED);
		transitions.add (State.COMPLETE,   Input.COMPLETED, null,             Action.INTERNAL_ERROR);
		transitions.add (State.STOPPED,    Input.ERROR,     null,             Action.INTERNAL_ERROR);
		transitions.add (State.CONNECTING, Input.ERROR,     State.ERROR,      Action.ERROR);
		transitions.add (State.RUNNING,    Input.ERROR,     State.ERROR,      Action.ERROR);
		transitions.add (State.COMPLETE,   Input.ERROR,     null,             Action.INTERNAL_ERROR);

		return transitions;

	}


	/**
	 * Starts the request
	 */
	private void actionStart() {

		try {
			this.deadline = System.currentTimeMillis() + (this.timeoutLength * 1000);
			this.connection = this.connectionManager.connect (this.serverAddress, this.serverPort, this.outboundConnectionListener, this.timeoutLength);
			this.connection.setListener (this.connectionReadyListener);
		} catch (IOException e) {
			this.workQueue.execute (new Runnable() {
				public void run() {
					HTTPRequestHandler.this.stateMachine.input (Input.ERROR);
				}
			});
		}

	}


	/**
	 * Initiates the watchdog timer and sets up the connection to write the request
	 */
	private void actionRun() {

		// Start the timeout to kill the connection if the request isn't finished by the deadline
		long delay = this.deadline - System.currentTimeMillis();
		this.timeoutFuture = this.workQueue.schedule (new Runnable() {
			public void run() {
				HTTPRequestHandler.this.stateMachine.input (Input.CANCEL);
			}
		}, delay, TimeUnit.MILLISECONDS);

		this.connection.setWriteEnabled (true);

	}


	/**
	 * Notifies the listener that the request has completed
	 */
	private void actionCompleted() {

		this.timeoutFuture.cancel (false);
		try {
			this.connection.close();
		} catch (IOException e) {
			// Shouldn't happen and nothing much we can do
		}

		HTTPRequestHandler.this.listener.requestComplete (HTTPRequestHandler.this);

	}


	/**
	 * Cancels the timer and notifies the listener that the request has been cancelled
	 */
	private void actionCancel() {

		this.timeoutFuture.cancel (false);
		try {
			this.connection.close();
		} catch (IOException e) {
			// Shouldn't happen and nothing much we can do
		}
		HTTPRequestHandler.this.listener.requestCancelled (HTTPRequestHandler.this);

	}


	/**
	 * Notifies the listener that the request has encountered an I/O error
	 */
	private void actionError() {

		if (this.timeoutFuture != null) {
			this.timeoutFuture.cancel (false);
		}
		if (this.connection != null) {
			try {
				this.connection.close();
			} catch (IOException e) {
				// Shouldn't happen and nothing much we can do
			}
		}

		HTTPRequestHandler.this.listener.requestError (HTTPRequestHandler.this);

	}


	/**
	 * Indicates an invalid state transition has occurred
	 */
	private void actionInternalError() {

		throw new IllegalStateException();

	}


	/**
	 * Gets the result of the HTTP request. This method should only be called once the listener has
	 * been informed that the request is complete
	 *
	 * @return The result of the HTTP request
	 */
	public HTTPResponse getResponse() {

		synchronized (this.responseParser) {
			return this.responseParser;
		}

	}


	/**
	 * @return The current state of the request handler
	 */
	public State getState() {

		return this.stateMachine.getState();

	}


	/**
	 * Cancels the request
	 */
	public void cancel() {

		this.workQueue.execute (new Runnable() {
			public void run() {
				HTTPRequestHandler.this.stateMachine.input (Input.CANCEL);
			}
		});

	}


	/**
	 * @param connectionManager The ConnectionManager used to manage the outgoing connection to the web server
	 * @param workQueue The WorkQueue used to monitor the timeout for the request
	 * @param serverAddress The address of the web server to contact
	 * @param serverPort The port of the web server to contact
	 * @param hostname The hostname of the web server to contact
	 * @param getRequest The GET request string to send
	 * @param timeout The timeout after which to abort the request, in seconds
	 * @param listener The listener to notify when the request is complete or has failed
	 * @throws IOException if a socket could not be created
	 */
	public HTTPRequestHandler (ConnectionManager connectionManager, WorkQueue workQueue, InetAddress serverAddress,
			int serverPort, String hostname, String getRequest, int timeout, HTTPRequestListener listener) throws IOException
	{

		this.connectionManager = connectionManager;
		this.workQueue = workQueue;
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.timeoutLength = timeout;
		this.listener = listener;

		this.responseParser = new HTTPResponseParser();

		StringBuilder requestBuilder = new StringBuilder();
		requestBuilder.append ("GET ");
		requestBuilder.append (getRequest);
		requestBuilder.append (" HTTP/1.0\r\n");
		requestBuilder.append ("Host: ");
		if (hostname != null) {
			requestBuilder.append (hostname);
		}
		requestBuilder.append ("\r\n\r\n");
		this.sendBuffer = ByteBuffer.wrap (requestBuilder.toString().getBytes (CharsetUtil.ASCII));

		this.workQueue.execute (new Runnable() {
			public void run() {
				HTTPRequestHandler.this.stateMachine.input (Input.START);
			}
		});

	}


}
