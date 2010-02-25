/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.connectionmanager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;


/**
 * A proxy onto a SocketChannel providing a listener callback facility that is
 * informed when the Connection is readable or writeable
 */
public class Connection implements ScatteringByteChannel, GatheringByteChannel {

	/**
	 * The ConnectionManager that manages this Connection
	 */
	private final ConnectionManager connectionManager;

	/**
	 * The SocketChannel that this Connection proxies
	 */
	private final SocketChannel socketChannel;

	/**
	 * A listener that is informed when data may be read or written
	 */
	private ConnectionReadyListener listener;

	/**
	 * {@code true} if writing has been requested, otherwise {@code false}
	 */
	private boolean writeEnabled = false;

	/**
	 * When the Connection is readable, the Connection's ConnectionManager will
	 * first call setReadable(), which sets socketReadable, and later
	 * informListener() to inform the Connection's listener
	 */
	private boolean socketReadable = false;

	/**
	 * When the Connection is writeable, the Connection's ConnectionManager will
	 * first call setWriteable(), which sets socketWriteable, and later
	 * informListener() to inform the Connection's listener
	 */
	private boolean socketWriteable = false;


	// ScatteringByteChannel interface

	/* (non-Javadoc)
	 * @see java.nio.channels.ScatteringByteChannel#read(java.nio.ByteBuffer[])
	 */
	public long read (ByteBuffer[] buffers) throws IOException {

		long bytesRead = this.socketChannel.read (buffers);
		return bytesRead;

	}

	/* (non-Javadoc)
	 * @see java.nio.channels.ScatteringByteChannel#read(java.nio.ByteBuffer[], int, int)
	 */
	public long read (ByteBuffer[] buffers, int offset, int length) throws IOException {

		long bytesRead = this.socketChannel.read (buffers, offset, length);
		return bytesRead;

	}

	/* (non-Javadoc)
	 * @see java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)
	 */
	public int read (ByteBuffer buffer) throws IOException {

		int bytesRead = this.socketChannel.read (buffer);
		return bytesRead;

	}


	// GatheringByteChannel interface

	/* (non-Javadoc)
	 * @see java.nio.channels.GatheringByteChannel#write(java.nio.ByteBuffer[])
	 */
	public long write (ByteBuffer[] buffers) throws IOException {

		long bytesWritten = this.socketChannel.write (buffers);
		return bytesWritten;

	}

	/* (non-Javadoc)
	 * @see java.nio.channels.GatheringByteChannel#write(java.nio.ByteBuffer[], int, int)
	 */
	public long write (ByteBuffer[] buffers, int offset, int length) throws IOException {

		long bytesWritten = this.socketChannel.write (buffers, offset, length);
		return bytesWritten;

	}

	/* (non-Javadoc)
	 * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
	 */
	public int write (ByteBuffer buffer) throws IOException {

		int bytesWritten = this.socketChannel.write (buffer);
		return bytesWritten;

	}


	/* (non-Javadoc)
	 * @see java.nio.channels.Channel#close()
	 */
	public void close() throws IOException {

		this.socketChannel.close();
		this.connectionManager.connectionClosed (this);

	}

	/* (non-Javadoc)
	 * @see java.nio.channels.Channel#isOpen()
	 */
	public boolean isOpen() {

		return this.socketChannel.isOpen();

	}


	/**
	 * Gets the remote socket address of the connection
	 *
	 * @return The remote socket address of the connection
	 */
	public InetSocketAddress getRemoteSocketAddress() {

		return (InetSocketAddress) this.socketChannel.socket().getRemoteSocketAddress();

	}


	/**
	 * Gets the remote address of the connection
	 *
	 * @return The remote address of the connection
	 */
	public InetAddress getRemoteAddress() {

		return this.socketChannel.socket().getInetAddress();

	}


	/**
	 * Gets the remote port of the connection
	 *
	 * @return The remote port of the connection
	 */
	public int getRemotePort() {

		return this.socketChannel.socket().getPort();

	}


	/**
	 * Sets the listener that will be informed when the Connection is readable
	 * or writeable
	 *
	 * @param listener The listener to inform when the Connection is readable or
	 *                 writeable
	 */
	public void setListener (ConnectionReadyListener listener) {

		this.listener = listener;

	}


	/**
	 * Sets whether the Connection wishes to write data
	 *
	 * @param enabled If {@code true}, the connection should report when it is
	 * writeable to its listener, if any
	 */
	public synchronized void setWriteEnabled (boolean enabled) {

		if (enabled != this.writeEnabled) {
			this.writeEnabled = enabled;
			this.connectionManager.setWriteEnabled (this, enabled);
		}

	}


	/**
	 * Called by ConnectionManager when it needs to find the SocketChannel
	 * @return The Connection's SocketChannel
	 */
	SocketChannel getSocketChannel() {

		return this.socketChannel;

	}


	/**
	 * Called by ConnectionManager to hint that the Connection is readable.
	 * informListener() will be called after this method.
	 */
	void setReadable() {

		this.socketReadable = true;

	}

	/**
	 * Called by ConnectionManager to hint that the Connection is writeable.
	 * informListener() will be called after this method.
	 */
	void setWriteable() {

		this.socketWriteable = true;

	}


	/**
	 * Inform the Connection's listener, if any, that it is ready to be read
	 * from and / or written to.<br>
	 * If the listener throws an exception, the socket is closed.
	 */
	void informListener() {

		if (this.listener != null) {
			try {
				this.listener.connectionReady (this, this.socketReadable, this.socketWriteable);
			} catch (Throwable t) {
				try {
					t.printStackTrace();
					close();
				} catch (IOException e) {
					// Can't do anything and therefore don't care
					e.printStackTrace();
				}
			}
		}

		this.socketReadable = false;
		this.socketWriteable = false;

	}


	/**
	 * @param connectionManager The ConnectionManager that manages this Connection
	 * @param socketChannel The SocketChannel that this Connection will proxy
	 */
	public Connection (ConnectionManager connectionManager, SocketChannel socketChannel) {

		this.connectionManager = connectionManager;
		this.socketChannel = socketChannel;

	}

}
