package org.itadaki.bobbin.peer.requestmanager;

import org.itadaki.bobbin.torrentdb.Piece;

/**
 * A listener for RequestManager events
 */
public interface RequestManagerListener {

	/**
	 * Indicates that the RequestManager has assembled a complete, unverified piece
	 * @param piece The assembled piece
	 */
	public void pieceAssembled (Piece piece);

}
