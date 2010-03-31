/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;

import org.itadaki.bobbin.torrentdb.ViewSignature;


/**
 * A PeerHandler's view of the peer set management services provided by a PeerCoordinator
 */
public interface PeerServices {

	/**
	 * Acquires the reentrant peer context lock. All peer and peer set management (including that in
	 * {@link PeerHandler}, the addition of new peers discovered by the tracker client, and the
	 * periodic asynchronous torrent wide peer management) synchronises on this lock; while it is
	 * held, the peer set and its attributes will not change.
	 *
	 * <p>This method may block for an unspecified period of time waiting for the lock. Once
	 * acquired, processing inside the lock should be kept to the shortest time possible, as all
	 * network I/O will be suspended while the lock is held. 
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 */
	public void lock();

	/**
	 * Releases the peer context lock
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 */
	public void unlock();

	/**
	 * Deregisters a peer that has disconnected. The peer's available pieces will be subtracted
	 * from the available piece map; if the peer was unchoked then the choking algorithm will be
	 * invoked.
	 *
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @param peer The peer to deregister
	 */
	public void peerDisconnected (ManageablePeer peer);

	/**
	 * Adjust the choked and unchoked peers
	 *
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 * @param opportunistic If {@code false}, the peer is presently interested and unchoked, and a
	 *        regular choking round will result. If {@code true}, the peer is presently interested
	 *        and choked, and may be opportunistically unchoked if there are few peers 
	 */
	public void adjustChoking (boolean opportunistic);

	/**
	 * Handle a view signature, updating the piece database and request manager, and propagating to
	 * other peers as appropriate
	 *
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @param viewSignature The view signature
	 * @return {@code true} if the signature verified correctly, otherwise {@code false}
	 */
	public boolean handleViewSignature (ViewSignature viewSignature);

}