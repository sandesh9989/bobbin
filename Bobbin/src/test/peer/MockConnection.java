/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedList;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.connectionmanager.ConnectionReadyListener;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer;
import org.itadaki.bobbin.peer.protocol.PeerProtocolParser;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.CharsetUtil;


/**
 * A mock Connection that reads from supplied ByteBuffers and captures output for comparison
 */
public class MockConnection extends Connection {

	/**
	 * A queue of buffers provided to be read through the Connection
	 */
	private final LinkedList<ByteBuffer> inputBuffers = new LinkedList<ByteBuffer>();

	/**
	 * A queue of buffers written through the Connection
	 */
	private final LinkedList<ByteBuffer> outputBuffers = new LinkedList<ByteBuffer>();

	/**
	 * An adaptor to ReadableByteChannel for the captured output bytes
	 */
	private ReadableByteChannel queueReader = new ReadableByteChannel() {

		public int read (ByteBuffer destination) throws IOException {

			int bytesRead = 0;

			while (!MockConnection.this.outputBuffers.isEmpty()) {
				ByteBuffer source = MockConnection.this.outputBuffers.peek();
				int length = Math.min (source.remaining(), destination.remaining());
				byte[] copyArray = new byte[length];
				source.get (copyArray, 0, length);
				destination.put (copyArray);
				bytesRead += length;
				if (!source.hasRemaining()) {
					MockConnection.this.outputBuffers.removeFirst();
				}
				if (!destination.hasRemaining ()) {
					break;
				}
			}

			return bytesRead;

		}

		public void close() throws IOException { }

		public boolean isOpen() {
			return true;
		}

	};

	/**
	 * {@code true} if writing to the connection is currently requested, otherwise {@code false}
	 */
	private boolean writeEnabled;

	/**
	 * The number of bytes that are permitted to be written through the Connection
	 */
	private int permittedWriteBytes = Integer.MAX_VALUE;

	/**
	 * If {@code true}, the connection has been closed
	 */
	private boolean closed = false;

	/**
	 * The remote socket address
	 */
	private InetSocketAddress remoteSocketAddress = null;

	/**
	 * The listener
	 */
	private ConnectionReadyListener listener;


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#read(java.nio.ByteBuffer)
	 */
	@Override
	public int read (ByteBuffer buffer) throws IOException {

		if (this.closed) {
			throw new ClosedChannelException();
		}

		int bytesRead = 0;
		while ((buffer.remaining() > 0) && (!this.inputBuffers.isEmpty())) {
			ByteBuffer inputBuffer = this.inputBuffers.peek();
			int bytesToRead = Math.min (inputBuffer.capacity() - inputBuffer.position(), buffer.remaining());
			inputBuffer.limit (inputBuffer.position() + bytesToRead);
			buffer.put (inputBuffer);
			bytesRead += bytesToRead;
			if (inputBuffer.position() == inputBuffer.capacity()) {
				this.inputBuffers.poll();
			}
		}

		return bytesRead;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#read(java.nio.ByteBuffer[], int, int)
	 */
	@Override
	public long read (ByteBuffer[] buffers, int offset, int length) throws IOException {

		fail();
		return -1;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#read(java.nio.ByteBuffer[])
	 */
	@Override
	public long read (ByteBuffer[] buffers) throws IOException {

		fail();
		return -1;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#write(java.nio.ByteBuffer)
	 */
	@Override
	public int write (ByteBuffer buffer) throws IOException {

		if (this.closed) {
			throw new ClosedChannelException();
		}

		int writeBytesAllowed = Math.min (this.permittedWriteBytes, buffer.remaining());

		ByteBuffer writeBuffer = ByteBuffer.allocate (writeBytesAllowed);
		int oldLimit = buffer.limit();
		buffer.limit (Math.min (oldLimit, buffer.position() + writeBuffer.remaining()));
		writeBuffer.put (buffer);
		buffer.limit (oldLimit);

		writeBuffer.flip();
		this.outputBuffers.add (writeBuffer);
		this.permittedWriteBytes -= writeBytesAllowed;

		return writeBytesAllowed;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#write(java.nio.ByteBuffer[], int, int)
	 */
	@Override
	public long write (ByteBuffer[] buffers, int offset, int length) throws IOException {

		// Not currently needed
		fail();
		return 0;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#write(java.nio.ByteBuffer[])
	 */
	@Override
	public long write (ByteBuffer[] buffers) throws IOException {

		if (this.closed) {
			throw new ClosedChannelException();
		}

		int writeBytesWanted = 0;
		for (ByteBuffer buffer : buffers) {
			writeBytesWanted += buffer.remaining();
		}
		int writeBytesAllowed = Math.min (this.permittedWriteBytes, writeBytesWanted);

		ByteBuffer writeBuffer = ByteBuffer.allocate (writeBytesAllowed);
		for (ByteBuffer buffer : buffers) {
			if (!writeBuffer.hasRemaining())
				break;
			int oldLimit = buffer.limit();
			buffer.limit (Math.min (oldLimit, buffer.position() + writeBuffer.remaining()));
			writeBuffer.put (buffer);
			buffer.limit (oldLimit);
		}

		writeBuffer.flip();
		this.outputBuffers.add (writeBuffer);
		this.permittedWriteBytes -= writeBytesAllowed;

		return writeBytesAllowed;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#close()
	 */
	@Override
	public void close() throws IOException {

		this.closed = true;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#getRemoteAddress()
	 */
	@Override
	public InetAddress getRemoteAddress() {

		return this.remoteSocketAddress.getAddress();

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#getRemotePort()
	 */
	@Override
	public int getRemotePort() {

		return this.remoteSocketAddress.getPort();

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#getRemoteSocketAddress()
	 */
	@Override
	public InetSocketAddress getRemoteSocketAddress() {
		return this.remoteSocketAddress;
	};


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#isOpen()
	 */
	@Override
	public boolean isOpen() {

		return !this.closed;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#setListener(org.itadaki.bobbin.connectionmanager.ConnectionReadyListener)
	 */
	@Override
	public void setListener (ConnectionReadyListener listener) {

		this.listener = listener;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.connectionmanager.Connection#setWriteEnabled(boolean)
	 */
	@Override
	public void setWriteEnabled (boolean enabled) {

		this.writeEnabled = enabled;

	}


	/**
	 * @return {@code true} if writing to the connection is currently being requested
	 */
	public boolean mockIsWriteEnabled() {

		return this.writeEnabled;

	}


	/**
	 * @param permittedWriteBytes The number of bytes that may be written through the Connection
	 */
	public void mockSetPermittedWriteBytes (int permittedWriteBytes) {

		this.permittedWriteBytes = permittedWriteBytes;

	}


	/**
	 * Indicates to the listener that the connection is readable and/or writeable
	 *
	 * @param readable {@code true} to indicate the connection is readable
	 * @param writeable {@code true} to indicate the connection is writeable
	 */
	public void mockTriggerIO (boolean readable, boolean writeable) {

		this.listener.connectionReady (this, readable, writeable);

	}


	/**
	 * Adds a buffer or buffers to be read (in their entirety) through the connection
	 *
	 * @param buffers The buffer(s) to be read
	 */
	public void mockInput (ByteBuffer... buffers) {

		for (ByteBuffer buffer : buffers) {
			this.inputBuffers.add (buffer);
		}

	}


	/**
	 * Asserts that the next bytes of queued output match the passed buffer in content and length
	 *
	 * @param expectedBytes The expected byte sequence
	 * @throws IOException 
	 */
	public void mockExpectOutput (ByteBuffer expectedBytes) throws IOException {

		ByteBuffer outputBytes = ByteBuffer.allocate (expectedBytes.remaining());
		this.queueReader.read (outputBytes);
		outputBytes.flip();

		assertEquals (expectedBytes, outputBytes);

	}


	/**
	 * Asserts that the next bytes of queued output match the passed buffers in content and length
	 *
	 * @param expectedBuffers The expected byte sequence as an array of buffers
	 * @throws IOException 
	 */
	public void mockExpectOutput (ByteBuffer[] expectedBuffers) throws IOException {

		for (ByteBuffer expectedBytes : expectedBuffers) {
			mockExpectOutput (expectedBytes);
		}

	}


	/**
	 * Asserts that there is no more output queued
	 */
	public void mockExpectNoMoreOutput() {

		assertTrue (this.outputBuffers.isEmpty());

	}


	/**
	 * Parses and pretty-prints the contents of the output buffer for debugging purposes.
	 * {@link #mockExpectOutput(ByteBuffer)} must not be called before invoking.
	 * @param fastExtensionOffered Set to {@code true} if the Fast extension is offered to the
	 *        remote peer, otherwise {@code false}
	 * @param extensionProtocolOffered Set to {@code true} if the extension protocol is offered to
	 *        the remote peer, otherwise {@code false}
	 * @throws IOException on any parse error
	 *
	 */
	public void mockDebugParseOutput (boolean fastExtensionOffered, boolean extensionProtocolOffered) throws IOException {

		PeerProtocolParser prettyPrintParser = new PeerProtocolParser (new PeerProtocolConsumer() {

			private int sequence = 0;

			public void unknownMessage (int messageID, byte[] messageBytes) throws IOException {
				System.out.printf ("%2d unknown (ID %d + %d bytes)\n", messageID, messageBytes.length);
			}

			public void suggestPieceMessage (int pieceNumber) throws IOException {
				System.out.printf ("%2d suggest piece (%d)\n", this.sequence++, pieceNumber);
			}

			public void requestMessage (BlockDescriptor descriptor) throws IOException {
				System.out.printf ("%2d request (%d:%d,%d)\n", this.sequence++, descriptor.getPieceNumber(), descriptor.getOffset(), descriptor.getLength());
			}

			public void rejectRequestMessage (BlockDescriptor descriptor) throws IOException {
				System.out.printf ("%2d reject request (%d:%d,%d)\n", this.sequence++, descriptor.getPieceNumber(), descriptor.getOffset(), descriptor.getLength());
			}

			public void pieceMessage (BlockDescriptor descriptor, byte[] data) throws IOException {
				System.out.printf ("%2d piece (%d:%d,%d)\n", this.sequence++, descriptor.getPieceNumber(), descriptor.getOffset(), descriptor.getLength());
			}

			public void keepAliveMessage() throws IOException {
				System.out.printf ("%2d keepalive\n", this.sequence++);
			}

			public void interestedMessage (boolean interested) throws IOException {
				System.out.printf ("%2d interested (%b)\n", this.sequence++, interested);
			}

			public void haveNoneMessage() throws IOException {
				System.out.printf ("%2d have none\n", this.sequence++);
			}

			public void haveMessage (int pieceIndex) throws IOException {
				System.out.printf ("%2d have (%d)\n", this.sequence++, pieceIndex);
			}

			public void haveAllMessage() throws IOException {
				System.out.printf ("%2d have all\n", this.sequence++);
			}

			public void chokeMessage (boolean choked) throws IOException {
				System.out.printf ("%2d choke (%b)\n", this.sequence++, choked);
			}

			public void cancelMessage (BlockDescriptor descriptor) throws IOException {
				System.out.printf ("%2d cancel (%d:%d,%d)\n", this.sequence++, descriptor.getPieceNumber(), descriptor.getOffset(), descriptor.getLength());
			}

			public void bitfieldMessage (byte[] bitField) throws IOException {
				System.out.printf ("%2d bitfield (%s)\n", this.sequence++, CharsetUtil.hexencode (bitField));
			}

			public void allowedFastMessage (int pieceNumber) throws IOException {
				System.out.printf ("%2d allowed fast (%d)\n", this.sequence++, pieceNumber);
			}

			public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra)
					throws IOException
			{
				System.out.printf ("%2d extension handshake\n", this.sequence++);
			}

			public void extensionMessage (String identifier, byte[] data) throws IOException {
				System.out.printf ("%2d extension (%s:%d)\n", this.sequence++, identifier, data.length);
			}

			public void merklePieceMessage (BlockDescriptor descriptor, byte[] hashChain, byte[] block) throws IOException {
				System.out.printf ("%2d Merkle piece (%d:%d,%d %d)\n", this.sequence++, descriptor.getPieceNumber(), descriptor.getOffset(), descriptor.getLength(), hashChain.length);
			}

			public void elasticSignatureMessage (ViewSignature viewSignature) throws IOException {
				System.out.printf ("%2d Elastic signature\n", this.sequence++);
			}

			public void elasticPieceMessage (BlockDescriptor descriptor, Long viewLength, byte[] hashChain, byte[] block) throws IOException {
				System.out.printf ("%2d Elastic piece\n", this.sequence++);
			}

			public void elasticBitfieldMessage (byte[] bitField) throws IOException {
				System.out.printf ("%2d Elastic bitfield\n", this.sequence++);
			}

		}, fastExtensionOffered, extensionProtocolOffered);

		prettyPrintParser.parseBytes (this.queueReader);

	}


	/**
	 * Creates a MockConnection with a default remote socket address
	 */
	public MockConnection() {

		super (null,null);
		try {
			this.remoteSocketAddress = new InetSocketAddress (InetAddress.getByAddress (new byte[] {1, 2, 3, 4}), 5678);
		} catch (UnknownHostException e) {
			// Can't happen
		}

	}


	/**
	 * @param remoteSocketAddress The remote socket address
	 */
	public MockConnection (InetSocketAddress remoteSocketAddress) {

		super (null,null);
		this.remoteSocketAddress  = remoteSocketAddress;

	}

}
