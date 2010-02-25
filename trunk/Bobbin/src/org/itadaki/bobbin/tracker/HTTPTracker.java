/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.tracker;

import static org.itadaki.bobbin.util.CharsetUtil.ASCII;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BEncoder;
import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.connectionmanager.ConnectionReadyListener;
import org.itadaki.bobbin.connectionmanager.InboundConnectionListener;
import org.itadaki.bobbin.util.CharsetUtil;



/**
 * Implements a Bittorrent tracker as an asynchronous, nonblocking data processor
 */
public class HTTPTracker implements InboundConnectionListener, ConnectionReadyListener {

	/**
	 * HTTP parsers for open connection
	 */
	private Map<Connection,HTTPRequestParser> parsers = new HashMap<Connection, HTTPRequestParser>();

	/**
	 * Responses that are in the process of being returned
	 */
	private Map<Connection,ByteBuffer> responses = new HashMap<Connection,ByteBuffer>();

	/**
	 * The port number the tracker is listening on
	 */
	private int port;

	/**
	 * The tracker manager
	 */
	private Tracker tracker = new Tracker();


	/* InboundConnectionListener interface */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager2.ConnectionLifecycleListener#accepted(org.itadaki.bobbin.connectionmanager2.Connection)
	 */
	public void accepted (Connection connection) {

		// Create parser for the connection
		connection.setListener (this);
		this.parsers.put (connection, new HTTPRequestParser());

	}


	/* ConnectionReadyListener interface */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager2.ConnectionListener#connectionReady(Connection, boolean, boolean)
	 */
	public void connectionReady (Connection connection, boolean readable, boolean writeable) {

		if (readable) {
			if (this.parsers.containsKey (connection)) {
				readData (connection);
			} else {
				try {
					connection.close();
				} catch (IOException e) {
					// Shouldn't happen, and nothing we can do if it does
				}
				cleanUpConnection (connection);
				return;
			}

		}

		if (writeable) {
			ByteBuffer buffer = this.responses.get (connection);
			boolean closeConnection = false;
			try {
				connection.write (buffer);
			} catch (IOException e) {
				closeConnection = true;
			}
			if (closeConnection || !buffer.hasRemaining()) {
				try {
					connection.close();
				} catch (IOException e) {
					// Shouldn't happen, and nothing we can do if it does
				}
				cleanUpConnection (connection);
			}
		}

	}


	/**
	 * Builds an HTTP response
	 * 
	 * @param responseCode The HTTP result code
	 * @param responseMessage The HTTP result message
	 * @param contentType The response content type
	 * @param content The response content
	 *
	 * @return a byte array exactly containing the HTTP response
	 */
	private byte[] httpResponse (int responseCode, String responseMessage, String contentType, byte[] content) {

		StringBuffer headerBuffer = new StringBuffer();

		headerBuffer.append ("HTTP/1.0 " + responseCode + " " + responseMessage + "\r\n");

		headerBuffer.append ("Content-Length: " + content.length + "\r\n");
		headerBuffer.append ("Content-Type: text/html\r\n");
		headerBuffer.append ("\r\n");

		byte[] headerBytes = headerBuffer.toString().getBytes(ASCII);

		byte[] response = new byte[headerBytes.length + content.length];
		System.arraycopy (headerBytes, 0, response, 0, headerBytes.length);
		System.arraycopy (content, 0, response, headerBytes.length, content.length);

		return response;

	}


	/**
	 * Builds a plain text HTTP OK response
	 *
	 * @param contentBytes The content of the response
	 * @return a byte array exactly containing the HTTP response
	 */
	private byte[] httpOKResponse (byte[] contentBytes) {

		return httpResponse (200, "OK", "text/plain", contentBytes);

	}


	/**
	 * Builds a plain text HTTP OK response
	 *
	 * @param content The content of the response
	 * @return a byte array exactly containing the HTTP response
	 */
	private byte[] httpOKResponse (String content) {

		return httpOKResponse (content.getBytes (ASCII));

	}


	/**
	 * Builds a plain text HTTP not found error response
	 *
	 * @param message The response content
	 * @return a byte array exactly containing the HTTP response
	 */
	private byte[] httpNotFoundResponse (String message) {

		return httpResponse (404, "Not Found", "text/plain", message.getBytes (ASCII));

	}


	/**
	 * Builds a plain text HTTP not implemented error response
	 *
	 * @param message The response content
	 * @return a byte array exactly containing the HTTP response
	 */
	private byte[] httpNotImplementedResponse (String message) {

		return httpResponse (501, "Not Implemented", "text/plain", message.getBytes (ASCII));

	}


	/**
	 * Builds a plain text HTTP internal error response
	 *
	 * @param message The response content
	 * @return a byte array exactly containing the HTTP response
	 */
	private byte[] httpInternalErrorResponse (String message) {

		return httpResponse (500, "Internal Error", "text/plain", message.getBytes (ASCII));

	}


	/**
	 * Decodes an HTTP GET argument string into a map. The keys are assumed to
	 * be UTF-8. Any tokens with no "=" are ignored. Invalid urlencoding will
	 * result in null values.
	 *
	 * @param encodedArguments The encoded argument string
	 * @return The decoded arguments
	 */
	private Map<String, byte[]> decodeArguments (String encodedArguments) {

		Map<String, byte[]> arguments = new HashMap<String, byte[]>();

		StringTokenizer tokeniser = new StringTokenizer (encodedArguments, "&");
		while (tokeniser.hasMoreElements()) {
			String keyValuePair = tokeniser.nextToken();
			int middle = keyValuePair.indexOf ('=');
			if (middle != -1) {
				String key = keyValuePair.substring (0, middle);
				String value = keyValuePair.substring (middle + 1);
				arguments.put (key, CharsetUtil.urldecode (value));
			}
		}
		
		return arguments;

	}


	/**
	 * Decodes an HTTP GET argument string into a list. The keys are assumed to
	 * be UTF-8. Any tokens with no "=" are ignored. Invalid urlencoding will
	 * result in null values.
	 *
	 * @param encodedArguments The encoded argument string
	 * @return The decoded arguments, as alternate Strings and byte[]s
	 */
	private List<Object> decodeArgumentsToList (String encodedArguments) {

		List<Object> arguments = new ArrayList<Object>();

		StringTokenizer tokeniser = new StringTokenizer (encodedArguments, "&");
		while (tokeniser.hasMoreElements()) {
			String keyValuePair = tokeniser.nextToken();
			int middle = keyValuePair.indexOf ('=');
			if (middle != -1) {
				String key = keyValuePair.substring (0, middle);
				String value = keyValuePair.substring (middle + 1);
				arguments.add (key);
				arguments.add (CharsetUtil.urldecode (value));
			}
		}
		
		return arguments;

	}


	/**
	 * If the supplied bytes are a valid, ASCII-encoded non-negative integer,
	 * returns its value, otherwise returns -1
	 *
	 * @param argumentBytes The bytes of an ASCII-encoded non-negative integer
	 * @return The integer's value, or -1
	 */
	private int nonNegativeIntegerOrMinusOne (byte[] argumentBytes) {

		int argument = -1;

		if (argumentBytes != null) {
			try {
				argument = Integer.parseInt (new String (argumentBytes, ASCII));
				argument = (argument >= 0) ? argument : -1;
			} catch (NumberFormatException e) {
				// Invalid. Leave it as -1
			}
		}

		return argument;

	}


	/**
	 * Processes a tracker announce
	 *
	 * @param encodedArguments The encoded arguments of the announce's http request
	 * @param address The source address of the announcer
	 * @return The encoded HTTP response to return
	 */
	private byte[] processAnnounce (String encodedArguments, InetAddress address) {

		Map<String,byte[]> arguments = decodeArguments (encodedArguments);
		byte[] infoHashBytes = arguments.get ("info_hash");
		byte[] peerIDBytes = arguments.get ("peer_id");
		byte[] keyBytes = (arguments.get ("key") == null) ? new byte[]{} : arguments.get ("key");

		int port = nonNegativeIntegerOrMinusOne (arguments.get ("port"));
		long uploaded = nonNegativeIntegerOrMinusOne (arguments.get ("uploaded"));
		long downloaded = nonNegativeIntegerOrMinusOne (arguments.get ("downloaded"));
		long remaining = nonNegativeIntegerOrMinusOne (arguments.get ("left"));
		TrackerEvent event = TrackerEvent.forName (arguments.containsKey ("event") ? new String (arguments.get ("event"), ASCII) : "");
		boolean compact = (nonNegativeIntegerOrMinusOne (arguments.get ("compact")) == 1);

		this.tracker.lock();
		try {
			BDictionary responseDictionary = this.tracker.event (infoHashBytes, peerIDBytes, keyBytes, address, port, uploaded, downloaded, remaining, event, compact);
			return httpOKResponse (BEncoder.encode (responseDictionary));
		} finally {
			this.tracker.unlock ();
		}


	}


	/**
	 * Processes a tracker scrape
	 *
	 * @param encodedArguments The encoded arguments of the announce's http request
	 * @return The encoded HTTP response to return
	 */
	private byte[] processScrape (String encodedArguments) {

		Set<ByteBuffer> hashes = null;
		if (encodedArguments != null) {
			hashes = new HashSet<ByteBuffer>();
			List<Object> arguments = decodeArgumentsToList (encodedArguments);
			for (int i = 0; i < arguments.size(); i+=2) {
				String key = (String) arguments.get (i);
				byte[] value = (byte[]) arguments.get (i+1);
				if ("info_hash".equals (key)) {
					hashes.add (ByteBuffer.wrap (value));
				}
			}
		}

		this.tracker.lock();
		try {
			byte[] response = httpOKResponse (BEncoder.encode (this.tracker.scrape (hashes)));
			return response;
		} finally {
			this.tracker.unlock();
		}

	}


	/**
	 * Clean up a connection's data on close
	 *
	 * @param connection The connection to clean up
	 */
	private void cleanUpConnection (Connection connection) {

		this.parsers.remove (connection);
		this.responses.remove (connection);

	}


	/**
	 * Process received data
	 *
	 * @param connection
	 */
	private void readData (Connection connection) {

		HTTPRequestParser requestParser = this.parsers.get (connection);

		byte[] response = null;

		try {

			requestParser.parseRequestBytes (connection);

			if (requestParser.isStateError()) {

				// The request failed to parse
				response = httpInternalErrorResponse ("Sorry, I didn't understand that.");

			} else if (requestParser.isStateComplete()) {

				// The request was parsed successfully
				if ("GET".equals (requestParser.getMethod())) {

					String resource = requestParser.getResource();
					String encodedArguments = requestParser.getArgumentString();

					if ("/".equals (resource) && (encodedArguments == null)) {
						response = httpOKResponse ("Hi. I'm a Bittorrent tracker. How are you?");
					} else if ("/announce".equals (resource) && (encodedArguments != null)) {
						InetAddress address = connection.getRemoteAddress();
						response = processAnnounce (encodedArguments, address);
					} else if ("/scrape".equals (resource)) {
						response = processScrape (encodedArguments);
					} else {
						response = httpNotFoundResponse ("Sorry, I'm not a real webserver. Maybe when I grow up.");
					}

				} else {
					response = httpNotImplementedResponse ("Sorry, I'm not a real webserver. Maybe when I grow up.");
				}

			}

			if (response != null) {
				this.responses.put (connection, ByteBuffer.wrap (response));
				connection.setWriteEnabled (true);
				this.parsers.remove (connection);
			}

		} catch (IOException e) {
			try {
				connection.close();
			} catch (IOException e1) {
				// Nothing much we can do
				e1.printStackTrace();
			}
			cleanUpConnection (connection);
			return;
		}

	}


	/**
	 * Returns the port the tracker is listening on. If an ephemeral port was
	 * requested, this will be the actual port number bound to.
	 *
	 * @return The port the tracker is listening on
	 */
	public int getPort() {

		return this.port;

	}


	/**
	 * Returns the tracker manager
	 *
	 * @return The tracker manager
	 */
	public Tracker getTracker() {

		return this.tracker;

	}


	/**
	 * Creates an HTTP Bittorrent tracker
	 * 
	 * @param manager The connection manager to manage the tracker's connections
	 * @param listenAddress The host address to bind to. If null, bind to the wildcard address
	 * @param listenPort The port to bind to. If zero, bind to an ephemeral port
	 * @throws IOException if the specified address or port could not be bound to
	 */
	public HTTPTracker (ConnectionManager manager, InetAddress listenAddress, int listenPort) throws IOException {

		this.port = manager.listen (listenAddress, listenPort, this);

	}

}
