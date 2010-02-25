/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.trackerclient;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.connectionmanager.ConnectionReadyListener;
import org.itadaki.bobbin.connectionmanager.InboundConnectionListener;
import org.itadaki.bobbin.trackerclient.HTTPRequestHandler;
import org.itadaki.bobbin.trackerclient.HTTPRequestListener;
import org.itadaki.bobbin.trackerclient.HTTPResponse;
import org.itadaki.bobbin.util.CharsetUtil;
import org.itadaki.bobbin.util.WorkQueue;
import org.junit.Test;



/**
 * Tests HTTPRequestHandler
 */
public class TestHTTPRequestHandler {

	/**
	 * A single use mock HTTP server that performs no parsing of the request it receives, but simply
	 * reads all data that is presented. Optionally, it may send a pre-defined response upon
	 * detecting "\r\n\r\n" in the input data.
	 */
	public static class MockHTTPServer implements InboundConnectionListener, ConnectionReadyListener {

		/**
		 * The contents of the incoming request
		 */
		private ByteBuffer request = ByteBuffer.allocate (16384);

		/**
		 * The response to send to the request
		 */
		private ByteBuffer response = null;

		public void accepted (Connection connection) {
			connection.setListener (this);
		}

		public void connectionReady (Connection connection, boolean readable, boolean writeable) {
			if (readable) {
				if (this.response != null) {
					try {
						connection.read (this.request);
						if (this.request.position() >= 4) {
							for (int i = 0; i < this.request.position() - 3; i++) {
								if (
										   (this.request.get (i) == '\r')
										&& (this.request.get (i+1) == '\n')
										&& (this.request.get (i+2) == '\r')
										&& (this.request.get (i+3) == '\n')
								   )
								{
									connection.setWriteEnabled (true);
								}
							}
						}
					} catch (IOException e){
						// Do nothing
					}
				}
			}
			if (writeable) {
				try {
					connection.write (this.response);
					if (!this.response.hasRemaining ()) {
						connection.close();
					}
				} catch (IOException e) {
					// Do nothing
				}
			}

		}


		/**
		 * Creates a mock server that sends a response after "\r\n\r\n" has been seen in the input
		 * stream
		 * 
		 * @param response The response that should be sent
		 */
		public MockHTTPServer (ByteBuffer response) {
			this.response = response;
		}


		/**
		 * Creates a mock server that never responds
		 */
		public MockHTTPServer() {
			// Do nothing
		}

	}


	/**
	 * Test the response to a simple, valid request
	 * @throws Exception 
	 */
	@Test
	public void testSimpleRequest() throws Exception {

		ConnectionManager connectionManager = new ConnectionManager();
		MockHTTPServer server = new MockHTTPServer (ByteBuffer.wrap ("HTTP/1.0 200 OK\r\n\r\nHello".getBytes (CharsetUtil.ASCII)));
		int port = connectionManager.listen (null, 0, server);

		final HTTPResponse[] response = new HTTPResponse[1];

		final CountDownLatch latch = new CountDownLatch (1);
		HTTPRequestHandler requestHandler = new HTTPRequestHandler (connectionManager, new WorkQueue (""), InetAddress.getLocalHost(), port, null, "/", 5, new HTTPRequestListener() {

			public void requestComplete (HTTPRequestHandler requestHandler) {
				response[0] = requestHandler.getResponse();
				latch.countDown();
			}

			public void requestCancelled(HTTPRequestHandler requestHandler) { }

			public void requestError(HTTPRequestHandler requestHandler) { }

		});

		assertTrue (latch.await (2, TimeUnit.SECONDS));

		assertTrue (requestHandler.getState() == HTTPRequestHandler.State.COMPLETE);
		assertTrue (response[0].isStateComplete());
		assertFalse (response[0].isStateError());
		assertEquals (200, response[0].getResponseCode());
		assertEquals ("OK", response[0].getResponseMessage());
		assertArrayEquals ("Hello".getBytes (CharsetUtil.ASCII), response[0].getResponseBody());

		connectionManager.close();

	}


	/**
	 * Tests a connection ending in a timeout
	 * @throws Exception 
	 */
	@Test
	public void testTimeout() throws Exception {

		ConnectionManager connectionManager = new ConnectionManager();
		MockHTTPServer server = new MockHTTPServer();
		int port = connectionManager.listen (null, 0, server);

		final HTTPResponse[] response = new HTTPResponse[1];

		final CountDownLatch latch = new CountDownLatch (1);		HTTPRequestHandler requestHandler = new HTTPRequestHandler (connectionManager, new WorkQueue (""), InetAddress.getLocalHost(), port, null, "/", 1, new HTTPRequestListener() {

			public void requestComplete (HTTPRequestHandler requestHandler) { }

			public void requestCancelled(HTTPRequestHandler requestHandler) {
				response[0] = requestHandler.getResponse();
				latch.countDown();
			}

			public void requestError(HTTPRequestHandler requestHandler) { }

		});

		assertTrue (latch.await (2, TimeUnit.SECONDS));

		assertTrue (requestHandler.getState() == HTTPRequestHandler.State.ERROR);
		assertNotNull (response[0]);
		assertFalse (response[0].isStateComplete());
		assertFalse (response[0].isStateError());

		connectionManager.close();

	}


	/**
	 * Test that a second requestComplete() is not triggered by timeout on a successfully completed
	 * request
	 * @throws Exception 
	 */
	@Test
	public void testCompleteTimeout() throws Exception {

		ConnectionManager connectionManager = new ConnectionManager();
		MockHTTPServer server = new MockHTTPServer (ByteBuffer.wrap ("HTTP/1.0 200 OK\r\n\r\nHello".getBytes (CharsetUtil.ASCII)));
		int port = connectionManager.listen (null, 0, server);

		final CyclicBarrier barrier = new CyclicBarrier (2);
		final int[] completeCount = new int[1];

		HTTPRequestHandler requestHandler = new HTTPRequestHandler (connectionManager, new WorkQueue (""), InetAddress.getLocalHost(), port, null, "/", 2, new HTTPRequestListener() {

			public void requestComplete (HTTPRequestHandler requestHandler) {
				try {
					completeCount[0]++;
					barrier.await();
				} catch (Exception e) {
					// Do nothing
				}
			}

			public void requestCancelled(HTTPRequestHandler requestHandler) {
				try {
					completeCount[0]++;
					barrier.await();
				} catch (Exception e) {
					// Do nothing
				}
			}

			public void requestError(HTTPRequestHandler requestHandler) { }

		});

		barrier.await (2, TimeUnit.SECONDS);
		assertTrue (requestHandler.getState() == HTTPRequestHandler.State.COMPLETE);
		try {
			barrier.await (5, TimeUnit.SECONDS);
			connectionManager.close();
		} catch (TimeoutException e) {
			return;
		}

		fail ("Second complete received");

	}

}
