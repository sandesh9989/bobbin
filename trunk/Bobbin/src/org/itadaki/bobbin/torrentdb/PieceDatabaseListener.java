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
