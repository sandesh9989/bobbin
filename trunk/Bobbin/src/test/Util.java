/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Random;

import org.itadaki.bobbin.util.DSAUtil;


/**
 * Miscellaneous utility functions used during testing
 */
public class Util {

	/**
	 * Builds an SHA1 hash from one or more byte arrays
	 *
	 * @param sources The data to hash
	 * @return The hash
	 * @throws Exception
	 */
	public static byte[] buildHash (byte[]... sources) throws Exception {

		MessageDigest digest = MessageDigest.getInstance ("SHA");

		byte[] hash = new byte[20];

		for (byte[] data : sources) {
			digest.update (data, 0, data.length);
		}
		digest.digest (hash, 0, 20);

		return hash;

	}


	/**
	 * Creates a DSA signature of the SHA1 hash of a set of inputs
	 * @param privateKey The DSA private key
	 * @param inputs The inputs
	 * @return The P1363 encoded DSA signature
	 * @throws Exception
	 */
	public static byte[] dsaSign (PrivateKey privateKey, byte[]... inputs) throws Exception {

		byte[] derSignature = null;
		try {
			Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
			dsa.initSign (privateKey);
			for (byte[] input : inputs) {
				dsa.update (input);
			}
			derSignature = dsa.sign();
		} catch (GeneralSecurityException e) {
			throw new InternalError (e.getMessage());
		}

		return DSAUtil.derSignatureToP1363Signature (derSignature);

	}


	/**
	 * Creates a repeatable pseudo-random block of data
	 *
	 * @param seed The seed to use for the PRNG
	 * @param blockSize The size of the array to return
	 * @param contentSize The number of bytes to actually fill
	 * @return The generated pseudo-random data
	 */
	public static byte[] pseudoRandomBlock (int seed, int blockSize, int contentSize) {

		Random random = new Random (seed);

		byte[] block = new byte[blockSize];
		for (int i = 0; i < contentSize; i++) {
			block[i] = (byte) random.nextInt(256);
		}
		return block;

	}


	/**
	 * Calculates SHA hashes for a sequence of pseudo random blocks as created by pseudoRandomBlock()
	 *
	 * @param blockSize The block size
	 * @param totalLength The total length of the data to hash
	 * @return A list of block hashes
	 * @throws Exception
	 */
	public static byte[][] pseudoRandomBlockHashes (int blockSize, int totalLength) throws Exception {

		if (totalLength == 0) {
			return new byte[0][];
		}

		int remaining = totalLength;
		int seed = 0;
		int blockNumber = 0;
		byte[][] blockHashes = new byte[(((totalLength - 1) / blockSize) + 1)][20];

		MessageDigest digest = MessageDigest.getInstance ("SHA");

		while (remaining > 0) {
			int thisPieceLength = (remaining > blockSize) ? blockSize : remaining;
			byte[] thisBlock = pseudoRandomBlock (seed, blockSize, thisPieceLength);
			digest.update (thisBlock, 0, thisPieceLength);
			digest.digest (blockHashes[blockNumber], 0, 20);
			remaining -= blockSize;
			seed++;
			blockNumber++;
		}

		return blockHashes;

	}


	/**
	 * Flattens a uniform 2D array into a 1D array
	 *
	 * @param array The uniform 2D array to flatten
	 * @return The flattened 1D array
	 */
	public static byte[] flatten2DArray (byte[][] array) {

		byte[] flattenedArray = new byte[array.length * array[0].length];
		for (int i = 0; i < array.length; i++) {
			System.arraycopy (array[i], 0, flattenedArray, i * array[0].length, array[0].length);
		}
		return flattenedArray;

	}


	/**
	 * Flattens an array of ByteBuffers into a single concatenated buffer
	 * @param buffers The buffers
	 * @return The concatenated buffer
	 */
	public static ByteBuffer flattenBuffers (ByteBuffer[] buffers) {

		int length = 0;
		for (ByteBuffer buffer : buffers) {
			length += buffer.remaining();
		}

		ByteBuffer flattenedBuffer = ByteBuffer.allocate (length);
		for (ByteBuffer buffer : buffers) {
			flattenedBuffer.put (buffer);
		}

		flattenedBuffer.rewind();
		return flattenedBuffer;

	}


	/**
	 * Returns the file name of a new, non existent temporary file
	 *
	 * @return The file name of a new, non existent temporary file
	 * @throws IOException
	 */
	public static File createNonExistentTemporaryFile() throws IOException {

		File file = File.createTempFile ("tcf", "tmp");
		file.delete();

		return file;

	}


	/**
	 * Creates a temporary directory set to be deleted on exit
	 *
	 * @return A File pointing to the created temporary directory
	 * @throws IOException
	 */
	public static File createTemporaryDirectory() throws IOException {

		// Not theoretically safe, but OK for a unit test
		File file = File.createTempFile ("bbt", null);
		file.delete();
		file.mkdir();
		file.deleteOnExit();

		return file;

	}


	/**
	 * Create an arbitrary but reproducible file set to be deleted on exit
	 *
	 * @param baseDirectory The directory to create the file in 
	 * @param fileName The name of the file to create
	 * @param length The length of the file to create
	 * @return A File pointing to the created file
	 * @throws IOException 
	 */
	public static File createReproducibleFile (File baseDirectory, String fileName, long length) throws IOException {

		File file = new File (baseDirectory, fileName);
		file.createNewFile();
		file.deleteOnExit();

		FileOutputStream output = new FileOutputStream (file);
		for (int i = 0; i < length; i++) {
			output.write (i & 0xff);
		}
		output.close ();

		return file;

	}


	/**
	 * Creates a ReadableByteChannel for a given byte array
	 *
	 * @param bytes The byte array to create a ReadableByteChannel for
	 * @return The created ReadableByteChannel
	 */
	public static ReadableByteChannel readableByteChannelFor (byte[] bytes) {

		ByteArrayInputStream inputStream = new ByteArrayInputStream (bytes);

		return Channels.newChannel (inputStream);

	}


	/**
	 * Creates a ReadableByteChannel for a given byte array that does not report the end of the
	 * stream
	 *
	 * @param bytes The byte array to create a ReadableByteChannel for
	 * @return The created ReadableByteChannel
	 */
	public static ReadableByteChannel infiniteReadableByteChannelFor (byte[] bytes) {

		ByteArrayInputStream inputStream = new ByteArrayInputStream (bytes);
		final ReadableByteChannel byteChannel = Channels.newChannel (inputStream);

		ReadableByteChannel wrappedChannel = new ReadableByteChannel() {
			public int read (ByteBuffer dst) throws IOException {
				int bytesRead = byteChannel.read (dst);
				return bytesRead < 0 ? 0 : bytesRead;

			}
			public void close() throws IOException { }
			public boolean isOpen() {
				return true;
			}
		};

		return wrappedChannel;

	}


	/**
	 * Creates a ReadableByteChannel for a given list of ByteBuffers that does not report the end of
	 * the stream
	 *
	 * @param buffers The buffers to read frmo
	 * @return The created ReadableByteChannel
	 */
	public static ReadableByteChannel infiniteReadableByteChannelFor (ByteBuffer... buffers) {

		int totalSize = 0;
		for (ByteBuffer buffer : buffers) {
			totalSize += buffer.remaining();
		}
		final ByteBuffer assembledBuffer = ByteBuffer.allocate (totalSize);
		for (ByteBuffer buffer : buffers) {
			assembledBuffer.put (buffer);
		}
		assembledBuffer.rewind();

		ReadableByteChannel wrappedChannel = new ReadableByteChannel() {
			public int read (ByteBuffer dst) throws IOException {
				int bytesRead = Math.min (assembledBuffer.capacity() - assembledBuffer.position(), dst.remaining());
				assembledBuffer.limit (assembledBuffer.position() + bytesRead);
				dst.put (assembledBuffer);
				return bytesRead;

			}
			public void close() throws IOException { }
			public boolean isOpen() {
				return true;
			}
		};

		return wrappedChannel;

	}

}
