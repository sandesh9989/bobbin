/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;


/**
 * A peer's unique 20 byte ID
 */
public final class PeerID {

	/**
	 * A byte buffer wrapping the peer ID
	 */
	private final ByteBuffer peerIDBuffer = ByteBuffer.allocate (20);


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + this.peerIDBuffer.hashCode();

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
				|| (!this.peerIDBuffer.equals (((PeerID) other).peerIDBuffer)))
		   )
		{
			return false;
		}

		return true;

	}


	/**
	 * @return A new byte array containing the bytes of the peer ID
	 */
	public byte[] getBytes() {

		return Arrays.copyOf (this.peerIDBuffer.array(), 20);

	}


	/**
	 * Creates a peer ID with the given bytes
	 *
	 * @param peerIDBytes The bytes of the peer ID
	 */
	public PeerID (byte[] peerIDBytes) {

		if ((peerIDBytes == null) || (peerIDBytes.length != 20)) {
			throw new IllegalArgumentException();
		}

		this.peerIDBuffer.put (peerIDBytes);
		this.peerIDBuffer.rewind();

	}


	/**
	 * Creates a random peer ID
	 */
	public PeerID() {

		new Random().nextBytes (this.peerIDBuffer.array());

	}

}
