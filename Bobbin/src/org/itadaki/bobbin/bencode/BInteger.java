/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.bencode;

/**
 * A bencode integer
 */
public class BInteger extends BValue {

	/**
	 * The Number representing the bencode integer's value
	 */
	private Number value;


	/* Cloneable interface */

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public BInteger clone() {

		return (BInteger) super.clone();

	}


	/* Overridden from BValue */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.bencode.BValue#dump(java.lang.StringBuffer, int)
	 */
	@Override
	protected void dump (StringBuffer buffer, int indent) {

		buffer.append ("integer: " + this.value + "\n");
		
	}

	
	/**
	 * Get the data represented by an Integer BValue
	 *
	 * @return The BValue's contents as a Number
	 */
	public Number value() {

		return this.value;

	}


	/**
	 * Creates a BValue representing an integer
	 * 
	 * @param number The number to wrap
	 */
	public BInteger (Number number) {

		this.value = number;

	}


}
