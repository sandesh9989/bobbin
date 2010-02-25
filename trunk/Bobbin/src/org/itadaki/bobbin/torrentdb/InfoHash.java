/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;

import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * A 20 byte info hash
 */
public final class InfoHash {

	/**
	 * A byte buffer wrapping the info hash
	 */
	private final ByteBuffer infoHashBuffer = ByteBuffer.allocate (20);


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + this.infoHashBuffer.hashCode();

		return result;

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (Object other) {

		if (this == other)
			return true;

		if (
				   (other == null)
				|| ((getClass() != other.getClass())
				|| (!this.infoHashBuffer.equals (((InfoHash) other).infoHashBuffer)))
		   )
		{
			return false;
		}

		return true;

	}


	/**
	 * @return A new byte array containing the bytes of the info hash
	 */
	public byte[] getBytes() {

		return Arrays.copyOf (this.infoHashBuffer.array(), 20);

	}


	/**
	 * Wraps an info hash with the given bytes
	 *
	 * @param infoHashBytes The bytes of the info hash
	 */
	public InfoHash (byte[] infoHashBytes) {

		if ((infoHashBytes == null) || (infoHashBytes.length != 20)) {
			throw new IllegalArgumentException();
		}

		this.infoHashBuffer.put (infoHashBytes);
		this.infoHashBuffer.rewind();

	}


}
