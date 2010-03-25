/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer.protocol;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.protocol.PeerConnectionListener;
import org.itadaki.bobbin.peer.protocol.PeerConnectionListenerProvider;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.peer.protocol.PeerProtocolNegotiator;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.junit.Test;

import test.peer.MockConnection;


/**
 * Tests PeerProtocolNegotiator
 */
public class TestPeerProtocolNegotiator {

	/**
	 * Tests a successful outbound connection with no Fast extension or extension protocol
	 * @throws IOException
	 */
	@Test
	public void testOutboundPlain() throws IOException {

		// Given
		PeerID localPeerID = new PeerID();
		PeerID remotePeerID = new PeerID();
		InfoHash infoHash = new InfoHash (new byte[20]);
		MockConnection connection = new MockConnection();
		PeerConnectionListener listener = mock (PeerConnectionListener.class);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, listener, infoHash, localPeerID);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, remotePeerID).array();

		// When
		connection.mockInput (ByteBuffer.wrap (handshakeBytes));
		negotiator.connectionReady (connection, true, true);

		// Then
		verify(listener).peerConnectionComplete (connection, remotePeerID, false, false);
		verifyNoMoreInteractions (listener);
		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, localPeerID));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests a successful outbound connection with Fast extension
	 * @throws IOException
	 */
	@Test
	public void testOutboundFast() throws IOException {

		// Given
		PeerID localPeerID = new PeerID();
		PeerID remotePeerID = new PeerID();
		InfoHash infoHash = new InfoHash (new byte[20]);
		MockConnection connection = new MockConnection();
		PeerConnectionListener listener = mock (PeerConnectionListener.class);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, listener, infoHash, localPeerID);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, false, infoHash, remotePeerID).array();

		// When
		connection.mockInput (ByteBuffer.wrap (handshakeBytes));
		negotiator.connectionReady (connection, true, true);

		// Then
		verify(listener).peerConnectionComplete (connection, remotePeerID, true, false);
		verifyNoMoreInteractions (listener);
		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, localPeerID));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests a successful outbound connection with extension protocol
	 * @throws IOException
	 */
	@Test
	public void testOutboundExtension() throws IOException {

		// Given
		PeerID localPeerID = new PeerID();
		PeerID remotePeerID = new PeerID();
		InfoHash infoHash = new InfoHash (new byte[20]);
		MockConnection connection = new MockConnection();
		PeerConnectionListener listener = mock (PeerConnectionListener.class);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, listener, infoHash, localPeerID);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, true, infoHash, remotePeerID).array();

		// When
		connection.mockInput (ByteBuffer.wrap (handshakeBytes));
		negotiator.connectionReady (connection, true, true);

		// Then
		verify(listener).peerConnectionComplete (connection, remotePeerID, false, true);
		verifyNoMoreInteractions (listener);
		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, localPeerID));
		connection.mockExpectNoMoreOutput();

	}

	/**
	 * Tests a successful outbound connection with both Fast extension and extension protocol
	 * @throws IOException
	 */
	@Test
	public void testOutboundFastExtension() throws IOException {

		// Given
		PeerID localPeerID = new PeerID();
		PeerID remotePeerID = new PeerID();
		InfoHash infoHash = new InfoHash (new byte[20]);
		MockConnection connection = new MockConnection();
		PeerConnectionListener listener = mock (PeerConnectionListener.class);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, listener, infoHash, localPeerID);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, remotePeerID).array();

		// When
		connection.mockInput (ByteBuffer.wrap (handshakeBytes));
		negotiator.connectionReady (connection, true, true);

		// Then
		verify(listener).peerConnectionComplete (connection, remotePeerID, true, true);
		verifyNoMoreInteractions (listener);
		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, localPeerID));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests an outbound connection receiving a bad header
	 * @throws IOException
	 */
	@Test
	public void testOutboundBadHeader() throws IOException {

		// Given
		PeerID localPeerID = new PeerID();
		PeerID remotePeerID = new PeerID();
		InfoHash infoHash = new InfoHash (new byte[20]);
		MockConnection connection = new MockConnection();
		PeerConnectionListener listener = mock (PeerConnectionListener.class);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, listener, infoHash, localPeerID);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, remotePeerID).array();
		handshakeBytes[1] = 'K';

		// When
		connection.mockInput (ByteBuffer.wrap (handshakeBytes));
		negotiator.connectionReady (connection, true, true);

		// Then
		verify(listener).peerConnectionFailed (connection);
		verifyNoMoreInteractions (listener);
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests an outbound connection receiving the wrong info hash in reply
	 * @throws IOException
	 */
	@Test
	public void testOutboundBadInfoHash() throws IOException {

		// Given
		PeerID localPeerID = new PeerID();
		PeerID remotePeerID = new PeerID();
		InfoHash infoHash = new InfoHash (new byte[20]);
		InfoHash infoHash2 = new InfoHash (new byte[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
		MockConnection connection = new MockConnection();
		PeerConnectionListener listener = mock (PeerConnectionListener.class);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, listener, infoHash, localPeerID);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash2, remotePeerID).array();

		// When
		connection.mockInput (ByteBuffer.wrap (handshakeBytes));
		negotiator.connectionReady (connection, true, true);

		// Then
		verify(listener).peerConnectionFailed (connection);
		verifyNoMoreInteractions (listener);
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests parsing an outbound connection at end-of-stream
	 * @throws IOException
	 */
	@Test
	public void testOutboundEndOfStream() throws IOException {

		// Given
		InfoHash infoHash = new InfoHash (new byte[20]);
		PeerID localPeerID = new PeerID();
		Connection connection = mock (Connection.class);
		when (connection.read (any (ByteBuffer.class))).thenReturn(-1);
		PeerConnectionListener listener = mock (PeerConnectionListener.class);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, listener, infoHash, localPeerID);

		// When
		negotiator.connectionReady (connection, true, true);

		// Then
		verify(listener).peerConnectionFailed (connection);
		verifyNoMoreInteractions (listener);

	}


	/**
	 * Tests failure to close the connection. Probably can't actually happen
	 * @throws IOException
	 */
	@Test
	public void testOutboundCloseFailure() throws IOException {

		// Given
		InfoHash infoHash = new InfoHash (new byte[20]);
		PeerID localPeerID = new PeerID();
		Connection connection = mock (Connection.class);
		when (connection.read (any (ByteBuffer.class))).thenReturn(-1);
		doThrow(new IOException()).when(connection).close();
		PeerConnectionListener listener = mock (PeerConnectionListener.class);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, listener, infoHash, localPeerID);

		// When
		negotiator.connectionReady (connection, true, true);

		// Then
		verify(listener).peerConnectionFailed (connection);
		verifyNoMoreInteractions (listener);

	}


	/**
	 * Tests a successful inbound connection
	 * @throws IOException
	 */
	@Test
	public void testInboundSuccessful() throws IOException {

		// Given
		PeerID localPeerID = new PeerID();
		PeerID remotePeerID = new PeerID();
		InfoHash infoHash = new InfoHash (new byte[20]);
		MockConnection connection = new MockConnection();
		PeerConnectionListener listener = mock (PeerConnectionListener.class);
		PeerConnectionListenerProvider provider = mock (PeerConnectionListenerProvider.class);
		when (provider.getPeerConnectionListener(infoHash)).thenReturn (listener);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, provider, localPeerID);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, remotePeerID).array();

		// When
		connection.mockInput (ByteBuffer.wrap (handshakeBytes));
		negotiator.connectionReady (connection, true, true);

		// Then
		verify(listener).peerConnectionComplete (connection, remotePeerID, true, true);
		verifyNoMoreInteractions (listener);
		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, localPeerID));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests an inbound connection with a bad header
	 * @throws IOException
	 */
	@Test
	public void testInboundBadHeader() throws IOException {

		// Given
		PeerID localPeerID = new PeerID();
		PeerID remotePeerID = new PeerID();
		InfoHash infoHash = new InfoHash (new byte[20]);
		MockConnection connection = new MockConnection();
		PeerConnectionListener listener = mock (PeerConnectionListener.class);
		PeerConnectionListenerProvider provider = mock (PeerConnectionListenerProvider.class);
		when (provider.getPeerConnectionListener(infoHash)).thenReturn (listener);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, provider, localPeerID);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, remotePeerID).array();
		handshakeBytes[1] = 'K';

		// When
		connection.mockInput (ByteBuffer.wrap (handshakeBytes));
		negotiator.connectionReady (connection, true, true);

		// Then
		verifyNoMoreInteractions (listener);
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests an inbound connection with an unknown info hash
	 * @throws IOException
	 */
	@Test
	public void testInboundUnknownInfoHash() throws IOException {

		// Given
		PeerID localPeerID = new PeerID();
		PeerID remotePeerID = new PeerID();
		InfoHash infoHash = new InfoHash (new byte[20]);
		MockConnection connection = new MockConnection();
		PeerConnectionListenerProvider provider = mock (PeerConnectionListenerProvider.class);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, provider, localPeerID);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, remotePeerID).array();

		// When
		connection.mockInput (ByteBuffer.wrap (handshakeBytes));
		negotiator.connectionReady (connection, true, true);

		// Then
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests parsing an inbound connection at end-of-stream
	 * @throws IOException
	 */
	@Test
	public void testInboundEndOfStream() throws IOException {

		// Given
		PeerID localPeerID = new PeerID();
		Connection connection = mock (Connection.class);
		when (connection.read (any (ByteBuffer.class))).thenReturn(-1);
		PeerConnectionListenerProvider provider = mock (PeerConnectionListenerProvider.class);
		PeerProtocolNegotiator negotiator = new PeerProtocolNegotiator (connection, provider, localPeerID);

		// When
		negotiator.connectionReady (connection, true, true);

		// Then
		// ... nothing

	}


}
