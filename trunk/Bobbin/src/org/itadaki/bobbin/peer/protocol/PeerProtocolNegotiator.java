package org.itadaki.bobbin.peer.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.connectionmanager.ConnectionReadyListener;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.torrentdb.InfoHash;


/**
 * Performs base protocol negotiation for both incoming and outgoing connections
 */
public class PeerProtocolNegotiator implements ConnectionReadyListener {

	/**
	 * The bytes of the mandatory stream header
	 */
	private static final byte[] streamHeaderBytes = { 19, 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' };

	/**
	 * The parser's current state
	 */
	private static enum State {

		/**
		 * Parser is reading the 20 byte header + 8 byte reserved data
		 */
		HEADER, 

		/**
		 * Parser is reading the 20 byte info hash
		 */
		INFO_HASH,

		/**
		 * Parser is reading the 20 byte peer ID
		 */
		PEER_ID,

		/**
		 * Parser is waiting to finish sending our handshake to the remote peer
		 */
		SENDING,

		/**
		 * Parser has completed, either successfully or on error
		 */
		COMPLETE

	}

	/**
	 * The connection to the remote peer
	 */
	private final Connection connection;

	/**
	 * If not {@code null}, the info hash received from the remote peer must equal this info hash
	 */
	private final InfoHash infoHash;

	/**
	 * The peer ID to send to the remote peer
	 */
	private PeerID localPeerID;

	/**
	 * The peer ID received from the remote peer
	 */
	private PeerID remotePeerID;

	/**
	 * The provider to get a PeerConnectionListener from for an incoming connection
	 */
	private PeerConnectionListenerProvider listenerProvider = null;

	/**
	 * The listener to inform on completion of negotiation
	 */
	private PeerConnectionListener listener = null;

	/**
	 * The negotiator's current state
	 */
	private State state = State.HEADER;

	/**
	 * The header data currently being assembled from input
	 */
	private ByteBuffer inputHeaderData = ByteBuffer.allocate (streamHeaderBytes.length + 8);

	/**
	 * The header data currently being sent as output
	 */
	private ByteBuffer outputHeaderData = ByteBuffer.allocate (streamHeaderBytes.length + 8 + 20 + 20);

	/**
	 * The number of remaining bytes that are expected of the current header section
	 */
	private int headerBytesExpected = streamHeaderBytes.length + 8;

	/**
	 * {@code true} if the Fast extension has been negotiated, otherwise {@code false}
	 */
	boolean fastExtensionEnabled = true;

	/**
	 * {@code true} if the extension protocol has been negotiated, otherwise {@code false}
	 */
	boolean extensionProtocolEnabled = true;


	/* ConnectionReadyListener interface */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.ConnectionReadyListener#connectionReady(org.itadaki.bobbin.connectionmanager.Connection, boolean, boolean)
	 */
	public void connectionReady (Connection connection, boolean readable, boolean writeable) {

		try {

			if (readable) {
				parseBytes (connection);
			}

			if (writeable) {
				connection.write (this.outputHeaderData);
				if (!this.outputHeaderData.hasRemaining()) {
					connection.setWriteEnabled (false);
				}
			}

			if ((this.state == State.SENDING) && !this.outputHeaderData.hasRemaining()) {
				this.state = State.COMPLETE;
				this.listener.peerConnectionComplete (connection, this.remotePeerID, this.fastExtensionEnabled, this.extensionProtocolEnabled);
			}

		} catch (IOException e) {

			try {
				connection.close();
			} catch (IOException e1) {
				// Nothing to do
			}
			this.state = State.COMPLETE;
			if (this.infoHash != null) {
				this.listener.peerConnectionFailed (connection);
			}

		}

	}


	/**
	 * Resets the messageData buffer ready to receive a given number of bytes
	 *
	 * @param numBytes The number of bytes to expect
	 */
	private void resetMessageBuffer (int numBytes) {

		this.inputHeaderData.clear();
		this.inputHeaderData.limit (numBytes);
		this.headerBytesExpected = numBytes;

	}


	/**
	 * Sends an initial handshake
	 * @param fastExtensionEnabled If {@code true}, advertise the Fast extension to the remote peer
	 * @param extensionProtocolEnabled  If {@code true}, advertise the extension protocol to the remote peer
	 * @param infoHash The info hash to use
	 * @param localPeerID The peer ID of the local peer
	 */
	private void sendHandshake (InfoHash infoHash, PeerID localPeerID) {

		this.outputHeaderData = PeerProtocolBuilder.handshake (true, true, infoHash, localPeerID);
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Parses header bytes, advancing the state machine as appropriate
	 *
	 * @param inputChannel The input channel to read bytes from
	 * @return The number of bytes successfully parsed, possibly zero
	 * @throws IOException if the input channel is closed or a parse error occurred
	 */
	private int parseBytes (ReadableByteChannel inputChannel) throws IOException {

		int totalBytesRead = 0;

		while (this.state != State.COMPLETE) {

			int bytesRead = inputChannel.read (this.inputHeaderData);
			totalBytesRead += bytesRead;

			if (bytesRead == 0) {
				return totalBytesRead;
			} else if (bytesRead == -1) {
				throw new ClosedChannelException();
			}

			this.headerBytesExpected -= bytesRead;

			// Check the header contents even before the expected number of bytes have been received - abort
			// early if someone is speaking the wrong protocol to us
			if (this.state == State.HEADER) {
				for (int i = 0; (i < streamHeaderBytes.length) && (i < this.inputHeaderData.position()); i++) {
					if (this.inputHeaderData.get(i) != streamHeaderBytes[i]) {
						throw new IOException ("Invalid header");
					}
				}
			}

			if (this.headerBytesExpected == 0) {
				this.inputHeaderData.rewind();
				switch (this.state) {

					case HEADER:
						this.inputHeaderData.position (20);
						byte[] extensionBytes = new byte[8];
						this.inputHeaderData.get (extensionBytes);
						this.fastExtensionEnabled &= ((extensionBytes[7] & 0x04) != 0);
						this.extensionProtocolEnabled &= ((extensionBytes[5] & 0x10) != 0);
						this.state = State.INFO_HASH;
						resetMessageBuffer (20);
						continue;

					case INFO_HASH:
						byte[] infoHashBytes = new byte[20];
						this.inputHeaderData.get (infoHashBytes);
						InfoHash receivedInfoHash = new InfoHash (infoHashBytes);
						if (this.infoHash == null) {
							this.listener = this.listenerProvider.getPeerConnectionListener (receivedInfoHash);
							if (this.listener != null) {
								sendHandshake (receivedInfoHash, this.localPeerID);
								this.state = State.PEER_ID;
							} else {
								throw new IOException ("Unknown info hash");
							}
						} else {
							if (this.infoHash.equals (receivedInfoHash)) {
								this.state = State.PEER_ID;
							} else {
								throw new IOException ("Incorrect info hash");
							}
						}
						resetMessageBuffer (20);
						continue;

					case PEER_ID:
						byte[] peerIDBytes = new byte[20];
						this.inputHeaderData.get (peerIDBytes);
						this.remotePeerID = new PeerID (peerIDBytes);
						this.state = State.SENDING;
						continue;

					case SENDING:
					case COMPLETE:
						// Do nothing
						break;

				}

			}

		}

		return 0;

	}


	/**
	 * Constructor for an incoming connection
	 * @param connection The connection to the remote peer
	 * @param listenerProvider The provider to ask for a PeerConnectionListener once the remote peer
	 *        sends its info hash
	 * @param localPeerID The peer ID to send to the remote peer
	 */
	public PeerProtocolNegotiator (Connection connection, PeerConnectionListenerProvider listenerProvider, PeerID localPeerID) {

		this.connection = connection;
		this.infoHash = null;

		this.listenerProvider = listenerProvider;
		this.localPeerID = localPeerID;

		this.connection.setListener (this);

	}


	/**
	 * Constructor for an outgoing connection
	 * @param connection The connection to the remote peer
	 * @param listener The listener to inform on completion of negotiation
	 * @param infoHash The info hash to negotiate with the remote peer for
	 * @param localPeerID The peer ID to send to the remote peer
	 */
	public PeerProtocolNegotiator (Connection connection, PeerConnectionListener listener, InfoHash infoHash, PeerID localPeerID) {

		this.connection = connection;
		this.infoHash = infoHash;

		this.listener = listener;

		this.connection.setListener (this);

		sendHandshake (infoHash, localPeerID);

	}


}
