/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.tracker;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.torrentdb.InfoHash;



/**
 * The core of a Bittorrent tracker
 */
public class Tracker {

	/**
	 * Default requested announce interval
	 */
	private static int INTERVAL = 15 * 60;

	/**
	 * A lock used to serialise access to the state of the tracked torrents
	 */
	private ReentrantLock trackerContextLock = new ReentrantLock();

	/**
	 * A map of all tracked torrents, indexed by info hash<br>
	 * All tracker data updates must be synchronised on this field
	 */
	private Map<InfoHash,TorrentInfo> torrents = new HashMap<InfoHash,TorrentInfo>();


	/**
	 * Builds an error response dictionary
	 *
	 * @param message The error message to include
	 * @return The error response dictionary
	 */
	private static BDictionary errorResponse (String message) {

		BDictionary errorDictionary = new BDictionary();
		errorDictionary.put ("failure reason", message);

		return errorDictionary;

	}


	/**
	 * Acquires the tracker context lock. All tracker state updates synchronise on this lock, and so
	 * while it is held the tracked peer set and its attributes will not change.
	 *
	 * <p>As all I/O processing will be suspended while the lock is held, processing inside the lock
	 * should be kept to the shortest time possible.
	 *
	 * <p>This method may block for an unspecified period of time waiting for the lock.
	 *
	 */
	public void lock() {

		this.trackerContextLock.lock();

	}


	/**
	 * Releases the tracker context lock
	 */
	public void unlock() {

		this.trackerContextLock.unlock();

	}


	/**
	 * Registers a hash to track
	 *
	 * <p><b>Thread safety:</b> This method must be called with the tracker context lock held
	 * 
	 * @param infoHash The hash to track
	 * @throws IllegalStateException if the current thread is not the owner of the tracker context
	 *         lock
	 */
	public void register (InfoHash infoHash) {

		if (!this.trackerContextLock.isHeldByCurrentThread ()) {
			throw new IllegalStateException();
		}

		this.torrents.put (infoHash, new TorrentInfo());

	}


	/**
	 * Processes a peer event
	 *
	 * <p><b>Thread safety:</b> This method must be called with the tracker context lock held
	 *
	 * @param infoHashBytes The info hash as a byte array
	 * @param peerIDBytes The peer's ID as a byte array
	 * @param peerKeyBytes The peer's key as a byte array
	 * @param address The peer's address
	 * @param port The peer's port
	 * @param uploaded The peer's bytes uploaded 
	 * @param downloaded The peer's bytes downloaded
	 * @param remaining The peer's bytes remaining
	 * @param event The event type
	 * @param compact If {@code true}, the peer dictionary in the response will be in the "compact"
	 *        format 
	 * @return a dictionary containing the tracker response or error
	 * @throws IllegalStateException if the current thread is not the owner of the tracker context
	 *         lock
	 */
	public BDictionary event (byte[] infoHashBytes, byte[] peerIDBytes, byte[] peerKeyBytes, InetAddress address, int port,
			long uploaded, long downloaded, long remaining, TrackerEvent event, boolean compact)
	{

		if (!this.trackerContextLock.isHeldByCurrentThread ()) {
			throw new IllegalStateException();
		}

		if ((infoHashBytes == null) || (infoHashBytes.length != 20)) {
			return errorResponse ("Invalid info hash");
		}

		if ((peerIDBytes == null) || (peerIDBytes.length != 20)) {
			return errorResponse ("Invalid peer ID");
		}

		if ((peerKeyBytes == null) || peerKeyBytes.length > 255) {
			return errorResponse ("Invalid peer key");
		}

		if (address == null) {
			return errorResponse ("Invalid address");
		}

		if ((port < 0) || (port > 65535)) {
			return errorResponse ("Invalid port");
		}

		if (uploaded < 0) {
			return errorResponse ("Invalid uploaded");
		}

		if (downloaded < 0) {
			return errorResponse ("Invalid downloaded");
		}

		if (remaining < 0) {
			return errorResponse ("Invalid left");
		}

		if (event == null) {
			return errorResponse ("Invalid event");
		}


		PeerID peerID = new PeerID (peerIDBytes);
		InfoHash infoHash = new InfoHash (infoHashBytes);
		ByteBuffer peerKey = ByteBuffer.wrap (peerKeyBytes);

		TorrentInfo torrentInfo = this.torrents.get (infoHash);
		if (torrentInfo == null) {
			return errorResponse ("Unknown info hash");
		}

		if (!torrentInfo.event (peerID, peerKey, address, port, uploaded, downloaded, remaining, event)) {
			return errorResponse ("Invalid peer key");
		}

		BDictionary response = new BDictionary();

		response.put ("interval", INTERVAL);
		response.put ("complete", torrentInfo.getCompletePeerCount());
		response.put ("incomplete", torrentInfo.getIncompletePeerCount());

		if (compact) {

			if (event != TrackerEvent.STOPPED) {

				Map<PeerID,PeerInfo> peers = torrentInfo.getPeers();

				int numPeers = 0;
				for (PeerInfo peer : peers.values()) {
					if (!peerID.equals (peer.getPeerID())) {
						numPeers++;
					}
				}

				byte[] peerBytes = new byte[6 * numPeers];
				int position = 0;
				for (PeerInfo peer : peers.values()) {
					if (!peerID.equals (peer.getPeerID())) {
						System.arraycopy (peer.getAddress().getAddress(), 0, peerBytes, position, 4);
						peerBytes [position + 4] = (byte) (peer.getPort () >> 8);
						peerBytes [position + 5] = (byte) (peer.getPort () & 0xff);
						position += 6;
					}
				}
	
				response.put ("peers", new BBinary (peerBytes));

			} else {

				response.put ("peers", new BBinary (new byte[0]));
			}

		} else {

			BList peerList = new BList();

			if (event != TrackerEvent.STOPPED) {

				for (PeerInfo peer : torrentInfo.getPeers().values()) {
					if (!peerID.equals (peer.getPeerID())) {
						BDictionary onePeer = new BDictionary();
						onePeer.put ("peer id", peer.getPeerID().getBytes());
						onePeer.put ("ip", peer.getAddress().getHostAddress());
						onePeer.put ("port", peer.getPort());
						peerList.add (onePeer);
					}
				}

			}

			response.put ("peers", peerList);

		}


		return response;

	}


	/**
	 * Builds a tracker scrape response
	 *
	 * <p><b>Thread safety:</b> This method must be called with the tracker context lock held
	 *
	 * @param infoHashBuffers A list of info hashes to scrape for. If null, scrape for all tracked
	 *        hashes
	 * @return A dictionary containing the scrape response
	 * @throws IllegalStateException if the current thread is not the owner of the tracker context
	 *         lock
	 */
	public BDictionary scrape (Set<ByteBuffer> infoHashBuffers) {

		if (!this.trackerContextLock.isHeldByCurrentThread ()) {
			throw new IllegalStateException();
		}

		BDictionary files = new BDictionary();

		List<InfoHash> infoHashes = new ArrayList<InfoHash>();
		if (infoHashBuffers == null) {
			infoHashes.addAll (this.torrents.keySet());
		} else {

			for (ByteBuffer infoHashBuffer : infoHashBuffers) {

				if ((infoHashBuffer == null) || (infoHashBuffer.capacity() != 20)) {
					return errorResponse ("Invalid info hash");
				}

				InfoHash infoHash = new InfoHash (infoHashBuffer.array());
				if (!this.torrents.containsKey (infoHash)) {
					return errorResponse ("Unknown info hash");
				}

				infoHashes.add (infoHash);

			}

		}

		for (InfoHash infoHash : infoHashes) {

			TorrentInfo torrentInfo = this.torrents.get (infoHash);
			BDictionary info = new BDictionary();
			info.put ("complete", torrentInfo.getCompletePeerCount());
			info.put ("downloaded", torrentInfo.getDownloadedPeerCount());
			info.put ("incomplete", torrentInfo.getIncompletePeerCount());

			files.put (new BBinary (infoHash.getBytes()), info);

		}

		BDictionary response = new BDictionary();
		response.put ("files", files);

		return response;

	}


	/**
	 * Gets a list of all the peers for a given hash
	 * 
	 * <p>TODO Refactor - Split PeerInfo into TrackedPeer, PeerInfo, ManageablePeerInfo
	 *
	 * <p><b>Thread safety:</b> This method must be called with the tracker context lock held
	 * 
	 * @param infoHash The hash to get peers for
	 * @return a list of all the peers for the given hash
	 * @throws IllegalStateException if the current thread is not the owner of the tracker context
	 *         lock
	 */
	public List<PeerInfo> getPeers (InfoHash infoHash) {

		if (!this.trackerContextLock.isHeldByCurrentThread ()) {
			throw new IllegalStateException();
		}

		return new ArrayList<PeerInfo> (this.torrents.get(infoHash).getPeers().values());

	}


}
