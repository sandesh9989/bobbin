/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.bencode;

import java.io.IOException;


/**
 * An exception representing a failure to decode bencoded data<br>
 * It extends IOException for convenience, such that if you don't particularly
 * care why decoding failed (stream error, EOF, invalid data) you can simply
 * catch IOException and not worry about it.
 */
public class InvalidEncodingException extends IOException {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * An InvalidEncodingException with a wrapped exception
	 * 
	 * @param t The wrapped throwable
	 */
	public InvalidEncodingException (Throwable t) {

		super (t);

	}


	/**
	 * An InvalidEncodingException with a reason message
	 * 
	 * @param message The reason message
	 */
	public InvalidEncodingException (String message) {

		super (message);

	}


	/**
	 * An InvalidEncodingException with a reason message and a wrapped throwable
	 * 
	 * @param message The reason message
	 * @param t The wrapped throwable
	 */
	public InvalidEncodingException (String message, Throwable t) {

		super (message, t);

	}

}
