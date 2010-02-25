/**
 * 
 */
/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.bencode;

import java.util.Arrays;

/**
 * A bencode value (binary string, integer, list or dictionary)
 */
public abstract class BValue implements Cloneable {

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (Object object) {

		if (object == this) {
			return true;
		} else if ((object == null) || (object.getClass() != this.getClass())) {
			return false;
		}
		return Arrays.equals (BEncoder.encode (this), BEncoder.encode ((BValue) object));

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuffer buffer = new StringBuffer();
		dump (buffer, 0);
		return buffer.toString ();

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public BValue clone() {

		try {
			return BDecoder.decode (BEncoder.encode (this));
		} catch (Exception e) {
			// In theory, we can't fail to clone a BValue
			throw new InternalError ("BValue clone failed: " + e.toString());
		}

	}


	/**
	 * Dumps the contents of the BValue as a human readable string
	 *
	 * @param buffer A StringBuffer to write the content to
	 * @param indent The current indent level
	 */
	protected abstract void dump (StringBuffer buffer, int indent);


}
