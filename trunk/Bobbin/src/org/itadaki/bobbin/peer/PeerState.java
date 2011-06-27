package org.itadaki.bobbin.peer;

import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.itadaki.bobbin.torrentdb.PiecesetDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;

/**
 * The connection state of a peer
 */
public class PeerState {

	/**
	 * The remote peer's ID
	 */
	PeerID remotePeerID = null;

	/**
	 * {@code true} if the Fast extension has been negotiated, otherwise {@code false}
	 */
	boolean fastExtensionEnabled = true;

	/**
	 * {@code true} if the extension protocol has been negotiated, otherwise {@code false}
	 */
	boolean extensionProtocolEnabled = true;

	/**
	 * The set of extensions offered by the remote peer
	 */
	Set<String> remoteExtensions = new HashSet<String>();

	/**
	 * The remote peer's bitfield
	 */
	BitField remoteBitField = null;

	/**
	 * The remote peer's view
	 */
	PiecesetDescriptor remoteView = null;

	/**
	 * The set of signatures implicitly valid from the remote peer
	 */
	NavigableMap<Long,ViewSignature> remoteViewSignatures = new TreeMap<Long,ViewSignature>();

	/**
	 * {@code true} if we are choking the remote peer, otherwise {@code false}
	 */
	boolean weAreChoking = true;

	/**
	 * {@code true} if we are interested in the remote peer, otherwise {@code false}
	 */
	boolean weAreInterested = false;

	/**
	 * {@code true} if the remote peer is choking us, otherwise {@code false}
	 */
	boolean theyAreChoking = true;

	/**
	 * {@code true} if the remote peer is interested in us, otherwise {@code false}
	 */
	boolean theyAreInterested = false;

	/**
	 * The time in system milliseconds that the last data was received from the remote peer
	 */
	long lastDataReceivedTime = 0;


	/**
	 * @return The remote peer's ID
	 */
	public PeerID getRemotePeerID() {

		return this.remotePeerID;

	}


	/**
	 * @return {@code true} if the Fast extension has been negotiated, otherwise {@code false}
	 */
	public boolean isFastExtensionEnabled() {

		return this.fastExtensionEnabled;

	}


	/**
	 * @return {@code true} if the extension protocol has been negotiated, otherwise {@code false}
	 */
	public boolean isExtensionProtocolEnabled() {

		return this.extensionProtocolEnabled;

	}


	/**
	 * @return The set of extensions offered by the remote peer
	 */
	public Set<String> getRemoteExtensions() {

		return new HashSet<String> (this.remoteExtensions);

	}


	/**
	 * @return The remote peer's bitfield
	 */
	public BitField getRemoteBitField() {

		return this.remoteBitField.clone();

	}


	/**
	 * @return The remote peer's view
	 */
	public PiecesetDescriptor getRemoteView() {

		return this.remoteView;

	}


	/**
	 * @return The set of signatures implicitly valid from the remote peer
	 */
	public NavigableMap<Long, ViewSignature> getRemotePeerSignatures() {

		return new TreeMap<Long,ViewSignature> (this.remoteViewSignatures);

	}


	/**
	 * @return {@code true} if we are choking the remote peer, otherwise {@code false}
	 */
	public boolean getWeAreChoking() {

		return this.weAreChoking;

	}


	/**
	 * @return {@code true} if we are interested in the remote peer, otherwise {@code false}
	 */
	public boolean getWeAreInterested() {

		return this.weAreInterested;

	}


	/**
	 * @return {@code true} if the remote peer is choking us, otherwise {@code false}
	 */
	public boolean getTheyAreChoking() {

		return this.theyAreChoking;

	}


	/**
	 * @return {@code true} if the remote peer is interested in us, otherwise {@code false}
	 */
	public boolean getTheyAreInterested() {

		return this.theyAreInterested;

	}


	/**
	 * @return The time in system milliseconds that the last data was received from the remote peer
	 */
	public long getLastDataReceivedTime() {

		return this.lastDataReceivedTime;

	}


}
