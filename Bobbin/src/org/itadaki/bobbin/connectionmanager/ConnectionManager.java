/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.connectionmanager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A non-blocking network multiplexer
 */
public class ConnectionManager {

	/**
	 * The selection thread
	 */
	private final Thread selectionThread;

	/**
	 * The selector for all monitored channels
	 */
	private final Selector selector;

	/**
	 * A list of changes waiting to be made to the selector environment. All
	 * changes are executed from within the main selection loop in the running
	 * manager thread
	 */
	private final List<Runnable> queuedTasks = new LinkedList<Runnable>();

	/**
	 * A map connecting a server socket channel to its designated InboundConnectionListener
	 */
	private final Map<ServerSocketChannel,InboundConnectionListener> inboundConnectionListeners = new HashMap<ServerSocketChannel,InboundConnectionListener>();

	/**
	 * A map connecting a client socket channel to its designated OutboundConnectionListener
	 */
	private final Map<SocketChannel,OutboundConnectionListener> outboundConnectionListeners = new HashMap<SocketChannel,OutboundConnectionListener>();

	/**
	 * A map of pending connections and their timeouts
	 */
	private final Map<SocketChannel,Long> pendingConnections = new HashMap<SocketChannel,Long>();

	/**
	 * If {@code true}, the connection manager is shut down and the selection thread will exit
	 */
	private boolean closed = false;

	/**
	 * A Runnable for the selection loop thread
	 */
	private final Runnable selectionRunnable = new Runnable() {

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {

			while (true) {

				synchronized (ConnectionManager.this) {
					if (ConnectionManager.this.closed) {
						for (SelectionKey key : ConnectionManager.this.selector.keys()) {
							try {
								key.channel().close();
							} catch (IOException e) {
								// Shouldn't happen and nothing much we can do
							}
						}
						return;
					}
				}

				try {

					// Evaluate pending connections for timeouts
					long currentTime = System.currentTimeMillis();
					for (Iterator<SocketChannel> iterator = ConnectionManager.this.pendingConnections.keySet().iterator(); iterator.hasNext();) {
						SocketChannel socketChannel = iterator.next();
						long deadline = ConnectionManager.this.pendingConnections.get (socketChannel);
						if (currentTime > deadline) {
							// Cancel the connection
							iterator.remove();
							SelectionKey key = socketChannel.keyFor (ConnectionManager.this.selector);
							Connection connection = (Connection) key.attachment();
							OutboundConnectionListener listener = ConnectionManager.this.outboundConnectionListeners.remove (socketChannel);
							listener.rejected (connection);
							socketChannel.close();
							key.cancel();
						}
					}

					// Execute any requested actions
					synchronized (ConnectionManager.this.queuedTasks) {
						for (Runnable change : ConnectionManager.this.queuedTasks) {
							change.run();
						}
						ConnectionManager.this.queuedTasks.clear();
					}

					// Wait for some data to come calling, or an intentional wakeup
					ConnectionManager.this.selector.select (1000);

					// Respond to any incoming events
					Set<Connection> readyConnections = new HashSet<Connection>();
					Iterator<SelectionKey> selectedKeys = ConnectionManager.this.selector.selectedKeys().iterator();

					while (selectedKeys.hasNext()) {
						SelectionKey key = selectedKeys.next();
						selectedKeys.remove();

						if (key.isValid() && key.isAcceptable()) {
							processAccept (key);
						}
						if (key.isValid() && key.isConnectable()) {
							processConnect (key);
						}
						if (key.isValid() && key.isReadable()) {
							Connection connection = (Connection) key.attachment();
							connection.setReadable();
							readyConnections.add (connection);
						}
						if (key.isValid() && key.isWritable()) {
							Connection connection = (Connection) key.attachment();
							connection.setWriteable();
							readyConnections.add (connection);
						}
					}

					for (Connection connection : readyConnections) {
						connection.informListener();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}

	};


	/**
	 * Accept a new incoming connection
	 * 
	 * @param key a ServerSocketChannel's selection key
	 * @throws IOException
	 */
	private void processAccept (SelectionKey key) throws IOException {

		// Accept the new socket and register it with the selector
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking (false);

		// Set up and track the Connection
		Connection connection = new Connection (this, socketChannel);
		SelectionKey socketKey = socketChannel.register (this.selector, SelectionKey.OP_READ);
		socketKey.attach (connection);

		// Notify the InboundConnectionListener of the socket's connection
		InboundConnectionListener listener = this.inboundConnectionListeners.get (serverSocketChannel);
		listener.accepted (connection);

	}


	/**
	 * Complete a new outgoing connection
	 * 
	 * @param key a ServerSocketChannel's selection key
	 */
	private void processConnect (SelectionKey key) {

		SocketChannel socketChannel = (SocketChannel) key.channel();
		OutboundConnectionListener listener = this.outboundConnectionListeners.get (socketChannel);
		Connection connection = (Connection) key.attachment();
		if (socketChannel.isConnectionPending()) {
			try {
				if (socketChannel.finishConnect()) {
					key.interestOps (SelectionKey.OP_READ);
					listener.connected (connection);
					this.pendingConnections.remove (socketChannel);
					this.outboundConnectionListeners.remove (socketChannel);
				}
			} catch (IOException e) {
				listener.rejected (connection);
				try {
					socketChannel.close();
				} catch (IOException e1) {
					// Shouldn't happen
				}
				key.cancel();
				this.pendingConnections.remove (socketChannel);
				this.outboundConnectionListeners.remove (socketChannel);
			}
		}


	}


	/**
	 * Add or remove a Connection to the selection set for writing
	 *
	 * @param connection
	 * @param enabled
	 */
	void setWriteEnabled (final Connection connection, final boolean enabled) {

		// Queue the action to be carried out before the next select cycle
		synchronized (this.queuedTasks) {
			this.queuedTasks.add (new Runnable() {
				public void run() {
					SelectionKey key = connection.getSocketChannel().keyFor (ConnectionManager.this.selector);
					if ((key != null) && (key.isValid())) {
						// We may have already closed the socket
						if (enabled) {
							key.interestOps (key.interestOps() | SelectionKey.OP_WRITE);
						} else {
							key.interestOps (key.interestOps() & ~SelectionKey.OP_WRITE);
						}
					}
				}
			});
		}

	}


	/**
	 * Informs the ConnectionManager that a connection has closed. A Connection
	 * calls this method with itself as an argument when
	 * {@link Connection#close()} is called
	 *
	 * @param connection The Connection to close
	 */
	void connectionClosed (final Connection connection) {

		// Queue the action to be carried out before the next select cycle
		synchronized (this.queuedTasks) {
			this.queuedTasks.add (new Runnable() {
				public void run() {
					SelectionKey key = connection.getSocketChannel().keyFor (ConnectionManager.this.selector);
					if (key != null) {
						key.cancel();
						try {
							key.channel().close();
						} catch (IOException e) {
							// Shouldn't happen and nothing much we can do
							e.printStackTrace();
						}
					}
				}
			});
		}

	}


	/**
	 * Binds to a given address and TCP port
	 * 
	 * @param listenAddress The host address to bind to. If null, bind to the wildcard address
	 * @param listenPort The port to bind to. If zero, bind to an ephemeral port
	 * @param listener The listener to inform of connections to the given address and port
	 * @return the actual port bound to
	 * @throws IOException if the specified address or port could not be bound to, or if the manager
	 *         is closed
	 */
	public synchronized int listen (InetAddress listenAddress, int listenPort, InboundConnectionListener listener) throws IOException {

		if (this.closed) {
			throw new IOException ("ConnectionManager is closed");
		}

		// Open a server channel and register it with the selector
		final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking (false);
		InetSocketAddress socketAddress = new InetSocketAddress (listenAddress, listenPort);
		serverSocketChannel.socket().bind (socketAddress);
		int boundPort = serverSocketChannel.socket().getLocalPort();

		// Associate the server channel with the specified listener
		this.inboundConnectionListeners.put (serverSocketChannel, listener);

		// Queue the server socket for registration, asynchronously
		synchronized (this.queuedTasks) {
			this.queuedTasks.add (new Runnable() {
				public void run() {
					try {
						serverSocketChannel.register (ConnectionManager.this.selector, SelectionKey.OP_ACCEPT);
					} catch (ClosedChannelException e) {
						// Nothing much we can do about this
						e.printStackTrace();
					}
				}
			});
		}

		// Kick the main loop to process the socket registration
		this.selector.wakeup();

		// Return the actual port number we bound to
		return boundPort;

	}


	/**
	 * Asynchronously makes a connection to a given address and TCP port
	 *
	 * @param remoteAddress The address to connect to
	 * @param remotePort The port to connect to
	 * @param listener The listener to inform of the connection's status
	 * @param connectTimeout The number of milliseconds to wait before giving up trying to connect
	 * @return The Connection object for the new connection
	 * @throws IOException if a socket could not be created or the manager is closed
	 */
	public synchronized Connection connect (final InetAddress remoteAddress, final int remotePort, final OutboundConnectionListener listener, 
			final int connectTimeout) throws IOException
	{

		if (this.closed) {
			throw new IOException ("ConnectionManager is closed");
		}

		final SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking (false);
		final Connection connection = new Connection (this, socketChannel);

		// Queue the connection to be opened
		synchronized (this.queuedTasks) {
			this.queuedTasks.add (new Runnable() {
				public void run() {
					try {
						boolean completeConnection = socketChannel.connect (new InetSocketAddress (remoteAddress, remotePort));
						SelectionKey key = socketChannel.register (ConnectionManager.this.selector, SelectionKey.OP_CONNECT);
						key.attach (connection);
						Long deadline = (connectTimeout == 0) ? Long.MAX_VALUE : System.currentTimeMillis() + (connectTimeout * 1000);
						ConnectionManager.this.pendingConnections.put (socketChannel, deadline);
						ConnectionManager.this.outboundConnectionListeners.put (socketChannel, listener);
						if (completeConnection) {
							processConnect (key);
						}
					} catch (IOException e) {
						// Shouldn't happen and nothing much we can do
						e.printStackTrace();
					}
				}
			});
		}

		// Kick the main loop to process the socket registration
		this.selector.wakeup();

		return connection;

	}


	/**
	 * Shuts down the selection thread, closing all pending and open connections and open sockets.
	 */
	public void close() {

		synchronized (this) {
			if (this.closed) {
				return;
			}
			this.closed = true;
		}
		this.selector.wakeup();

		while (this.selectionThread.isAlive ()) {
			try {
				this.selectionThread.join();
			} catch (InterruptedException e) {
				// Retry
			}
		}

	}


	/**
	 * Creates a ConnectionManager
	 * 
	 * @throws IOException
	 */
	public ConnectionManager() throws IOException {

		this.selector = SelectorProvider.provider().openSelector();

		this.selectionThread = new Thread (this.selectionRunnable, "ConnectionManager thread");
		this.selectionThread.setDaemon (true);
		this.selectionThread.start();

	}


}
