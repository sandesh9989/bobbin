/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;

import java.io.IOException;


/**
 * An exception representing the detection of an incompatible location on disk. This is typically
 * thrown when a torrent is attempted to be written to a location where a directory that is
 * specified by the torrent is a file on disk, or vice versa. It may also indicate that one or more
 * of the torrent files is unreadable, or "special" as defined by the local operating system.
 */
public class IncompatibleLocationException extends IOException {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * An IncompatibleLocationException with a reason message
	 * 
	 * @param message The reason message
	 */
	public IncompatibleLocationException (String message) {

		super (message);

	}


	/**
	 * An IncompatibleLocationException with a wrapped exception
	 *  
	 * @param exception The wrapped exception
	 */
	public IncompatibleLocationException (Exception exception) {

		super (exception);

	}


}
