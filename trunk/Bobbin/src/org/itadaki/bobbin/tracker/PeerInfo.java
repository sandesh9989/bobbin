package org.itadaki.bobbin.tracker;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import org.itadaki.bobbin.peer.PeerID;



/**
 * Information about a peer known to the tracker 
 */
public class PeerInfo {

	/**
	 * The peer's ID 
	 */
	private PeerID peerID;

	/**
	 * The peer's key
	 */
	private ByteBuffer key;

	/**
	 * The peer's address
	 */
	private InetAddress address;

	/**
	 * The peer's ports
	 */
	private int port;

	/**
	 * The peer's total bytes uploaded
	 */
	private long uploaded;

	/**
	 * The peer's total bytes downloaded
	 */
	private long downloaded;

	/**
	 * The peer's total bytes remaining 
	 */
	private long remaining;

	/**
	 * True if the peer is complete 
	 */
	private boolean complete;

	/**
	 * The last time the peer sent a valid event, in milliseconds since the epoch 
	 */
	private long lastUpdated;


	/**
	 * @return the peer's ID
	 */
	public PeerID getPeerID() {

		return this.peerID;
	}


	/**
	 * @return the peer's key
	 */
	public ByteBuffer getKey() {

		return this.key;

	}


	/**
	 * @return the address
	 */
	public InetAddress getAddress() {

		return this.address;

	}


	/**
	 * @param address the address to set
	 */
	public void setAddress (InetAddress address) {

		this.address = address;
	}


	/**
	 * @return the port
	 */
	public int getPort() {

		return this.port;
	}


	/**
	 * @param port the port to set
	 */
	public void setPort (int port) {

		this.port = port;

	}


	/**
	 * @return the peer's total bytes uploaded
	 */
	public long getUploaded() {

		return this.uploaded;
	}


	/**
	 * @param uploaded the peer's total bytes uploaded
	 */
	public void setUploaded (long uploaded) {

		this.uploaded = uploaded;
	}


	/**
	 * @return the peer's total bytes downloaded
	 */
	public long getDownloaded() {

		return this.downloaded;
	}


	/**
	 * @param downloaded the peer's total bytes downloaded
	 */
	public void setDownloaded (long downloaded) {

		this.downloaded = downloaded;
	}


	/**
	 * @return the peer's total bytes remaining
	 */
	public long getRemaining() {

		return this.remaining;
	}


	/**
	 * @param remaining the peer's total bytes remaining
	 */
	public void setRemaining (long remaining) {

		this.remaining = remaining;
	}


	/**
	 * @return true if the peer is complete
	 */
	public boolean isComplete() {

		return this.complete;

	}


	/**
	 * @param complete the complete status to set
	 */
	public void setComplete (boolean complete) {

		this.complete = complete;

	}


	/**
	 * @param lastUpdated the last updated time to set, in milliseconds since the epoch
	 */
	public void setLastUpdated (long lastUpdated) {

		this.lastUpdated = lastUpdated;
	}


	/**
	 * @return the last updated time, in milliseconds since the epoch
	 */
	public long getLastUpdated() {

		return this.lastUpdated;
	}


	/**
	 * Creates a PeerInfo with the specified parameters
	 * 
	 * @param peerID The peer's ID
	 * @param key The peer's key
	 * @param address The peer's address
	 * @param port The peer's port
	 * @param uploaded The peer's total bytes uploaded
	 * @param downloaded The peer's total bytes downloaded
	 * @param remaining The peer's total bytes remaining
	 */
	public PeerInfo (PeerID peerID, ByteBuffer key, InetAddress address, int port, long uploaded, long downloaded, long remaining) {

		this.peerID = peerID;
		this.key = key;
		this.address = address;
		this.port = port;
		this.uploaded = uploaded;
		this.downloaded = downloaded;
		this.remaining = remaining;
		this.complete = false;
		this.lastUpdated = System.currentTimeMillis();

	}


}
