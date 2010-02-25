/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util.elastictree;

import java.nio.ByteBuffer;


/**
 * Represents a hash chain
 */
public class HashChain {

	/**
	 * The tree view length that the hashes pertain to
	 */
	private final long viewLength;

	/**
	 * The concatenated chain hashes
	 */
	private final ByteBuffer hashes;


	/**
	 * @return The concatenated chain hashes
	 */
	public ByteBuffer getHashes() {
		return this.hashes.asReadOnlyBuffer();
	}


	/**
	 * @return The tree view length that the hashes pertain to
	 */
	public long getViewLength() {
		return this.viewLength;
	}


	/**
	 * @param viewLength The view length onto the Merkle tree
	 * @param hashes The concatenated chain hashes
	 */
	public HashChain (long viewLength, ByteBuffer hashes) {

		this.viewLength = viewLength;
		this.hashes = hashes;
	
	}

}
