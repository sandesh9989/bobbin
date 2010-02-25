/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;


/**
 * A listener for state changes in a PieceDatabase
 */
public interface PieceDatabaseListener {

	/**
	 * Indicates that the PieceDatabase is available
	 */
	public void pieceDatabaseAvailable();

	/**
	 * Indicates that the PieceDatabase is stopped
	 */
	public void pieceDatabaseStopped();

	/**
	 * Indicates that the PieceDatabase is stopped due to an I/O error
	 */
	public void pieceDatabaseError();

	/**
	 * Indicates that the PieceDatabase has terminated
	 */
	public void pieceDatabaseTerminated();

}
