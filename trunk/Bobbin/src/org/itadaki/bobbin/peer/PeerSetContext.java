package org.itadaki.bobbin.peer;

import org.itadaki.bobbin.peer.extensionmanager.ExtensionManager;
import org.itadaki.bobbin.peer.requestmanager.RequestManager;
import org.itadaki.bobbin.torrentdb.PieceDatabase;

/**
 * A shared context for torrent-wide services
 */
public class PeerSetContext {

	/**
	 * The peer set's PeerServices
	 */
	public final PeerServices peerServices;

	/**
	 * The peer set's PieceDatabase
	 */
	public final PieceDatabase pieceDatabase;

	/**
	 * The peer set's RequestManager
	 */
	public final RequestManager requestManager;

	/**
	 * The peer set's ExtensionManager
	 */
	public final ExtensionManager extensionManager;


	/**
	 * @param peerServices The peer set's PeerServices
	 * @param pieceDatabase The peer set's PieceDatabase
	 * @param requestManager The peer set's RequestManager
	 * @param extensionManager The peer set's ExtensionManager
	 */
	public PeerSetContext (PeerServices peerServices, PieceDatabase pieceDatabase, RequestManager requestManager, ExtensionManager extensionManager) {

		this.peerServices = peerServices;
		this.pieceDatabase = pieceDatabase;
		this.requestManager = requestManager;
		this.extensionManager = extensionManager;

	}


}
