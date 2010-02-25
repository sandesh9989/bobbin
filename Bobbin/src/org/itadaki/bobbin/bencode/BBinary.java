/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.bencode;

import static org.itadaki.bobbin.util.CharsetUtil.UTF8;

/**
 * A bencode binary string
 */
public class BBinary extends BValue implements Cloneable, Comparable<BBinary> {

	/**
	 * The byte array representing the bencode binary string's value
	 */
	private byte[] value;


	/* Cloneable interface */

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public BBinary clone() {

		return (BBinary) super.clone();

	}


	/* Comparable<BBinary> interface */

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo (BBinary other) {

		// Binary lexicographic sort

		int minLength = Math.min (this.value.length, other.value.length);

		for (int i = 0; i < minLength; i++) {
			if (this.value[i] != other.value[i]) {
				return (this.value[i] & 0xff) - (other.value[i] & 0xff);
			}
		}

		return this.value.length - other.value.length;

	}


	/**
	 * Get the data represented by a binary BValue
	 *
	 * @return The BValue's contents as a byte array
	 */
	public byte[] value() {

		return this.value;

	}


	/**
	 * Get the data represented by a binary BValue interpreted as UTF-8, as a string
	 *
	 * @return The BValue's contents as a string 
	 */
	public String stringValue() {

		return new String (this.value, UTF8);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.bencode.BValue#dump(java.lang.StringBuffer, int)
	 */
	@Override
	protected void dump (StringBuffer buffer, int indent) {

		int i;
		buffer.append ("binary: \"");
		byte[] bytes = this.value;
		for (i = 0; (i < bytes.length) && (i < 80); i++) {
			if ((bytes[i] >= 32) && (bytes[i] <= 127)) {
				buffer.append ((char)bytes[i]);
			} else {
				buffer.append ("<0x" + Integer.toHexString (bytes[i] & 0xff) + ">");
			}
		}
		if (i < bytes.length) {
			buffer.append ("...");
		}
		buffer.append ("\"\n");
		
	}

	
	/**
	 * Creates a BValue representing a binary string
	 * 
	 * @param data The bytes to wrap. The byte array passed in is NOT copied internally
	 */
	public BBinary (byte[] data) {

		this.value = data;

	}


	/**
	 * Creates a BValue representing a binary string
	 * 
	 * @param text A string to represent. The bytes of the UTF-8 representation of the string are used
	 */
	public BBinary (String text) {

		this.value = text.getBytes (UTF8);

	}


}
