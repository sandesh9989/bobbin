/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.connectionmanager;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.connectionmanager.ConnectionReadyListener;
import org.itadaki.bobbin.connectionmanager.InboundConnectionListener;
import org.itadaki.bobbin.connectionmanager.OutboundConnectionListener;
import org.junit.Test;



/**
 * Tests ConnectionManager
 */
public class TestConnectionManager {

	/**
	 * An adaptor for OutboundConnectionListener that fails the enclosing unit test on an unexpected call
	 */
	private abstract class OutboundConnectionListenerAdaptor implements OutboundConnectionListener {

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.connectionmanager2.ConnectionLifecycleListener#connected(org.itadaki.bobbin.connectionmanager2.Connection)
		 */
		public void connected (Connection connection) {
			fail();
		}

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.connectionmanager.OutboundConnectionListener#rejected(org.itadaki.bobbin.connectionmanager.Connection)
		 */
		public void rejected (Connection connection) {
			fail();
		}

	}


	// Inbound connections

	/**
	 * Tests that a new inbound connection calls InboundConnectionListener.offered() then .accepted()
	 * @throws Exception
	 */
	@Test
	public void testInboundAccepted() throws Exception {

		final CountDownLatch latch = new CountDownLatch (1);

		ConnectionManager connectionManager = new ConnectionManager();
		InboundConnectionListener connectionManagerListener = new InboundConnectionListener() {
			public void accepted(Connection connection) {
				latch.countDown();
			}
		};
		int port = connectionManager.listen (null, 0, connectionManagerListener);

		// Open a socket to our listener. When offered() has been called we will pass through the barrier
		new Socket (InetAddress.getLocalHost(), port);

		assertTrue (latch.await (5, TimeUnit.SECONDS));

		connectionManager.close();

	}


	// Outbound connections

	/**
	 * Tests that an outbound connection calls InboundConnectionListener.accepted()
	 * @throws Exception
	 */
	@Test
	public void testOutboundAccepted() throws Exception {

		final CountDownLatch latch = new CountDownLatch (1);

		ConnectionManager connectionManager = new ConnectionManager();
		OutboundConnectionListener connectionManagerListener = new OutboundConnectionListenerAdaptor() {
			@Override
			public void connected (Connection connection) {
				latch.countDown();
			}
		};

		ServerSocket serverSocket = new ServerSocket (0);
		connectionManager.connect (InetAddress.getLocalHost(), serverSocket.getLocalPort(), connectionManagerListener, 0);
		Socket socket = serverSocket.accept();

		assertTrue (latch.await (5, TimeUnit.SECONDS));
		assertTrue (socket.isConnected());

		connectionManager.close();

	}


	/**
	 * Tests that an outbound connection calls InboundConnectionListener.rejected() on a closed port
	 * @throws Exception
	 */
	@Test
	public void testOutboundRejected() throws Exception {

		final CountDownLatch latch = new CountDownLatch (1);

		ConnectionManager connectionManager = new ConnectionManager();
		OutboundConnectionListener connectionManagerListener = new OutboundConnectionListenerAdaptor() {
			@Override
			public void rejected (Connection connection) {
				latch.countDown();
			}
		};

		ServerSocket serverSocket = new ServerSocket (0);
		int serverPort = serverSocket.getLocalPort();
		serverSocket.close();
		connectionManager.connect (InetAddress.getLocalHost(), serverPort, connectionManagerListener, 0);

		assertTrue (latch.await (5, TimeUnit.SECONDS));

		connectionManager.close();

	}


	/**
	 * Tests that an outbound connection calls InboundConnectionListener.rejected() on a timeout
	 * @throws Exception
	 */
	@Test
	public void testOutboundTimeout() throws Exception {

		final CountDownLatch latch = new CountDownLatch (1);

		ConnectionManager connectionManager = new ConnectionManager();
		OutboundConnectionListener connectionManagerListener = new OutboundConnectionListenerAdaptor() {
			@Override
			public void rejected (Connection connection) {
				latch.countDown();
			}
		};

		long initialTime = System.currentTimeMillis();
		connectionManager.connect (InetAddress.getByName ("255.255.255.1"), 1024, connectionManagerListener, 1);

		assertTrue (latch.await (5, TimeUnit.SECONDS));
		assertTrue ((System.currentTimeMillis() - initialTime) > 1000);

		connectionManager.close();

	}


	// Flow control

	/**
	 * Tests that ConnectionManager signals a Connection when it is readable
	 * @throws Exception
	 */
	@Test
	public void testConnectionReadable() throws Exception {

		final CountDownLatch latch = new CountDownLatch (1);

		ConnectionManager connectionManager = new ConnectionManager();
		InboundConnectionListener connectionManagerListener = new InboundConnectionListener() {

			public void accepted (Connection connection) {
				connection.setListener (new ConnectionReadyListener() {

					public void connectionReady (Connection connection, boolean readable, boolean writeable) {
						if (readable && !writeable) {
							ByteBuffer buffer = ByteBuffer.allocate (1024);
							try {
								connection.read (buffer);
							} catch (Exception e) {
								// Do nothing
							}
	
							latch.countDown();
						}
					}

				});
			}

		};
		int port = connectionManager.listen (null, 0, connectionManagerListener);

		// Open a socket to our listener. When offered() has been called we will pass through the barrier
		Socket socket = new Socket (InetAddress.getLocalHost(), port);
		socket.getOutputStream().write (1);

		assertTrue (latch.await (5, TimeUnit.SECONDS));

		connectionManager.close();

	}


	/**
	 * Tests that ConnectionManager signals a Connection when it is writeable
	 * @throws Exception
	 */
	@Test
	public void testConnectionWriteable() throws Exception {

		final CountDownLatch latch = new CountDownLatch (1);

		ConnectionManager connectionManager = new ConnectionManager();
		InboundConnectionListener connectionManagerListener = new InboundConnectionListener() {

			public void accepted (Connection connection) {
				connection.setWriteEnabled (true);
				connection.setListener (new ConnectionReadyListener() {
					public void connectionReady (Connection connection, boolean readable, boolean writeable) {
						if (writeable && !readable) {
							assertEquals (false, readable);
							assertEquals (true, writeable);
							ByteBuffer buffer = ByteBuffer.allocate (1);

							try {
								connection.write (buffer);
							} catch (Exception e) {
								return;
							}
							latch.countDown();
						}
					}
				});
			}

		};
		int port = connectionManager.listen (null, 0, connectionManagerListener);

		// Open a socket to our listener. When accepted() has been called we will pass through the barrier
		new Socket (InetAddress.getLocalHost(), port);
		latch.await (5, TimeUnit.SECONDS);

		assertTrue (latch.await (5, TimeUnit.SECONDS));

		connectionManager.close();

	}


	/**
	 * Tests that ConnectionManager does not signal a Connection when it is no longer writeable
	 * @throws Exception
	 */
	@Test
	public void testConnectionNotWriteable() throws Exception {

		final boolean connectionReadyCalled[] = new boolean[1];
		final CyclicBarrier barrier = new CyclicBarrier (2);

		ConnectionManager connectionManager = new ConnectionManager();
		InboundConnectionListener connectionManagerListener = new InboundConnectionListener() {

			public void accepted (Connection connection) {
				connection.setWriteEnabled (true);
				connection.setListener (new ConnectionReadyListener() {

					private int sequence = 1;

					public void connectionReady (Connection connection, boolean readable, boolean writeable) {

						try {
							switch (this.sequence) {
								case 1:
									assertEquals (false, readable);
									assertEquals (true, writeable);
									ByteBuffer writeBuffer = ByteBuffer.allocate (1);
									connection.write (writeBuffer);
									connection.setWriteEnabled (false);
									break;
								case 2:
									assertEquals (true, readable);
									assertEquals (false, writeable);
									ByteBuffer readBuffer = ByteBuffer.allocate (1);
									connection.read (readBuffer);
									connectionReadyCalled[0] = true;
									break;
								default:
									// We may be called at least once more for the connection close, but that's not important for this test 
							}
							barrier.await (5, TimeUnit.SECONDS);
						} catch (Exception e) {
							// Do nothing
						}

						this.sequence++;

					}

				});
			}

		};
		int port = connectionManager.listen (null, 0, connectionManagerListener);

		// Open a socket to our listener. The first time through the barrier we will be writable only ; the
		// listener will then disable writes, we will write a byte for it to read, and the second time through
		// the barrier we will be readable only 
		Socket socket = new Socket (InetAddress.getLocalHost(), port);
		barrier.await (5, TimeUnit.SECONDS);
		socket.getOutputStream().write (1);
		barrier.await (5, TimeUnit.SECONDS);

		assertTrue (connectionReadyCalled[0]);

		connectionManager.close();

	}


	// Close

	/**
	 * Tests that an outbound connection fails after close()
	 * @throws Exception
	 */
	@Test
	public void testConnectAfterClose() throws Exception {

		ConnectionManager connectionManager = new ConnectionManager();
		OutboundConnectionListener connectionManagerListener = new OutboundConnectionListenerAdaptor() {
			@Override
			public void connected (Connection connection) { }
		};

		connectionManager.close();

		boolean caught = false;
		try {
			connectionManager.connect (InetAddress.getLocalHost(), 0, connectionManagerListener, 0);
		} catch (IOException e) {
			caught = true;
		}

		assertTrue (caught);

		connectionManager.close();

	}

	/**
	 * Tests that an inbound connection fails after close()
	 * @throws Exception
	 */
	@Test
	public void testListenAfterClose() throws Exception {

		ConnectionManager connectionManager = new ConnectionManager();
		InboundConnectionListener connectionManagerListener = new InboundConnectionListener() {
			public void accepted(Connection connection) { }
		};

		connectionManager.close();

		boolean caught = false;
		try {
			connectionManager.listen (null, 0, connectionManagerListener);
		} catch (IOException e) {
			caught = true;
		}

		assertTrue (caught);

		connectionManager.close();

	}

}
