/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.itadaki.bobbin.util.BitField;



/**
 * A linear array of bytes, presented as an array of pieces such that all but the last piece are of
 * equal length.
 */
public interface Storage {

	/**
	 * @return A descriptor of the {@code Storage}'s piece set characteristics
	 */
	public PiecesetDescriptor getPiecesetDescriptor();

	/**
	 * @return A bitfield containing containing a {@code true} at every piece index that is fully
	 *         backed by allocated storage, and a {@code false} at every other position. Underlying
	 *         storage must be allocated when a piece is written, but may also be allocated
	 *         incidentally (such as when the storage is implemented as being backed by one or more
	 *         ordinary files). {@link PieceDatabase} consumes this information to decide which
	 *         pieces are worth verifying.
	 */
	public BitField getStorageBackedPieces();

	/**
	 * Validates the state of the {@code Storage} against the given opaque cookie that was
	 * returned by {@link #close()} on a previous, identically constructed {@code Storage}. It is
	 * assumed that this method, if invoked, is called before any writes to this {@code Storage}
	 * instance; its use at any other time is undefined.
	 *
	 * @param cookie The opaque cookie to validate against. Passing {@code null} will always result
	 *        in a return of {@code false}
	 * @return {@code true} if the state of the {@code Storage} is unchanged since the previous
	 *         {@link #close()}, otherwise {@code false}
	 * @throws IOException If an error occurs validating the underlying storage
	 */
	public boolean open (ByteBuffer cookie) throws IOException;

	/**
	 * Extends the total length of the storage
	 *
	 * @param length The new length of the storage, which must be strictly greater than the previous
	 *        length
	 * @throws IOException if an error occurred extending the underlying storage
	 */
	public void extend (long length) throws IOException;

	/**
	 * Closes the storage. All associated system resources will be released.
	 * Reading from or writing to the {@code Storage} after closure will result in an exception.
	 * 
	 * @return An opaque cookie that can be passed to {@link #open(ByteBuffer)} on a subsequent
	 *         identically constructed {@code Storage}, or {@code null} if validation is
	 *         unsupported
	 * @throws IOException if an error occurred closing the storage
	 */
	public ByteBuffer close() throws IOException;

	/**
	 * Reads a piece from storage.
	 * No underlying storage is allocated as a result of invoking this method
	 *
	 * @param pieceNumber The index of the piece to read
	 * @return The number of bytes actually within the piece
	 * @throws IOException if an error occurred reading from the underlying storage
	 * @throws IndexOutOfBoundsException if the requested index is out of bounds
	 */
	public ByteBuffer read (int pieceNumber) throws IOException;

	/**
	 * Writes a piece to storage
	 *
	 * @param pieceNumber The index of the piece to write
	 * @param buffer The buffer containing the piece to write, which must be at least as long as the
	 *               piece length. No bytes beyond the piece length will be stored. In the case of the
	 *               final piece, any bytes after the end of the underlying storage will be
	 *               discarded.
	 * @throws IOException if an error occurred writing to the underlying storage
	 * @throws IndexOutOfBoundsException if the requested index is out of bounds
	 */
	public void write (int pieceNumber, ByteBuffer buffer) throws IOException;

	/**
	 * Creates a WritableByteChannel to write to the storage
	 *
	 * @param pieceNumber The piece number to start writing from
	 * @param offset The offset in bytes from the initial piece
	 * @return A WritableByteChannel to write to the storage
	 * @throws IOException if an error occurred writing to the underlying storage
	 * @throws IndexOutOfBoundsException if the requested index or offset is out of bounds
	 */
	public WritableByteChannel openOutputChannel (int pieceNumber, int offset) throws IOException;

}
