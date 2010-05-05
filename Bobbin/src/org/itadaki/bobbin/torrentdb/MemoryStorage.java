/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

import org.itadaki.bobbin.util.BitField;



/**
 * A memory based {@link Storage} that can hold up to {@link Integer#MAX_VALUE} bytes (available
 * memory allowing)
 */
public class MemoryStorage implements Storage {

	/**
	 * The descriptor of the {@code Storage}'s piece set characteristics
	 */
	private PiecesetDescriptor descriptor = new PiecesetDescriptor (0, 0);

	/**
	 * The bytes of the stored data
	 */
	private byte[] data = new byte[0];


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#getDescriptor()
	 */
	public PiecesetDescriptor getPiecesetDescriptor() {

		return this.descriptor;

	}

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#getStorageBackedPieces()
	 */
	public BitField getStorageBackedPieces() {

		return new BitField (this.descriptor.getNumberOfPieces()).not();

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#open(int, org.itadaki.bobbin.torrentdb.InfoFileset)
	 */
	public void open (int pieceSize, InfoFileset infoFileset) throws IOException {

		if (infoFileset.getLength() > Integer.MAX_VALUE) {
			throw new IllegalArgumentException ("Requested storage is too large");
		}

		int length = (int) infoFileset.getLength();

		this.descriptor = new PiecesetDescriptor (pieceSize, length);
		this.data = Arrays.copyOf (this.data, length);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#validate(java.nio.ByteBuffer)
	 */
	public boolean validate (ByteBuffer cookie) {

		return false;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#extend(long)
	 */
	public void extend (long length) throws IOException {

		if (length <= this.descriptor.getLength()) {
			throw new IllegalArgumentException ("New length must be greater than existing length");
		}
		if (length > Integer.MAX_VALUE) {
			throw new IllegalArgumentException ("Requested storage is too large");
		}

		this.descriptor = new PiecesetDescriptor (this.descriptor.getPieceSize(), length);
		this.data = Arrays.copyOf (this.data, (int)length);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#close()
	 */
	public ByteBuffer close() throws IOException {

		this.data = null;

		return null;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#read(int)
	 */
	public ByteBuffer read (int pieceNumber) throws IOException {

		if ((pieceNumber < 0) || (pieceNumber >= this.descriptor.getNumberOfPieces())) {
			throw new IndexOutOfBoundsException ("Invalid index " + pieceNumber);
		}

		int pieceSize = this.descriptor.getPieceSize();
		int thisPieceLength = this.descriptor.getPieceLength (pieceNumber);
		byte[] content = new byte[thisPieceLength];
		System.arraycopy (this.data, pieceNumber * pieceSize, content, 0, thisPieceLength);

		return ByteBuffer.wrap (content);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#write(int, java.nio.ByteBuffer)
	 */
	public void write (int pieceNumber, ByteBuffer buffer) throws IOException {

		if ((pieceNumber < 0) || (pieceNumber >= this.descriptor.getNumberOfPieces())) {
			throw new IndexOutOfBoundsException ("Invalid index " + pieceNumber);
		}

		buffer.get (this.data, pieceNumber * this.descriptor.getPieceSize(), this.descriptor.getPieceLength (pieceNumber));

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#openOutputChannel(int, int)
	 */
	public WritableByteChannel openOutputChannel (int pieceNumber, int offset) throws IOException {

		final int initialPosition = (pieceNumber * this.descriptor.getPieceSize()) + offset;

		if ((offset > this.descriptor.getPieceSize ()) || (initialPosition > this.descriptor.getLength() - 1)) {
			throw new IllegalArgumentException ("Invalid channel position");
		}

		WritableByteChannel channel = new WritableByteChannel() {

			int position = initialPosition;

			public boolean isOpen() {
				return true;
			}

			public void close() throws IOException { }

			public int write (ByteBuffer src) throws IOException {
				int length = src.remaining();
				src.get (MemoryStorage.this.data, this.position, length);
				this.position += length;
				return length;
			}

		};

		return channel;

	}


	/**
	 * Creates a MemoryStorage with known initial data
	 * @param data The data
	 */
	public MemoryStorage (byte[] data) {

		this.data = data;

	}


	/**
	 * Default constructor
	 */
	public MemoryStorage() {

		// Nothing to do

	}

}
