package org.itadaki.bobbin.peer;

import java.util.List;

import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.counter.StatisticCounter;


/**
 * Interface representing a peer that can have global management applied to it
 * by a TorrentManager. Only TorrentManager and its delegate classes should
 * use this interface.
 */
public interface ManageablePeer extends ExtensiblePeer {

	/**
	 * @return The remote peer's available piece bitfield. The returned bitfield must not be changed
	 */
	public BitField getRemoteBitField();

	/**
	 * @return The peer's block bytes sent counter
	 */
	public StatisticCounter getBlockBytesSentCounter();

	/**
	 * @return The peer's block bytes received counter
	 */
	public StatisticCounter getBlockBytesReceivedCounter();

	/**
	 * @return {@code true} if the remote peer has any outstanding requests to us, otherwise false
	 */
	public boolean getTheyHaveOutstandingRequests();

	/**
	 * @param weAreChokingThem If {@code true}, we will choke the peer; if false, we will not choke
	 *        the peer
	 * @return If {@code true}, the choke state changed; if {@code false}, the requested state was
	 *         already set
	 */
	public boolean setWeAreChoking (boolean weAreChokingThem);

	/**
	 * @param weAreInterested If {@code true}, we are interested in the peer; if {@code false}, we
	 *                        are not interested in the peer
	 */
	public void setWeAreInterested (boolean weAreInterested);

	/**
	 * Cancels one or more block requests previously assigned to this peer
	 *
	 * @param requests The list of block requests to cancel
	 */
	public void cancelRequests (List<BlockDescriptor> requests);

	/**
	 * Rejects block requests by the peer for the given piece number
	 *
	 * @param pieceNumber The piece to reject
	 */
	public void rejectPiece (int pieceNumber);

	/**
	 * Informs the peer that we have a particular piece
	 *
	 * @param pieceNumber The number of the piece that we have
	 */
	public void sendHavePiece (int pieceNumber);

	/**
	 * Sends a keepalive to the peer if we are idle, or closes the connection if they are idle
	 */
	public void sendKeepaliveOrClose();

	/**
	 * Sends a ViewSignature to the peer
	 *
	 * @param viewSignature The signature to send
	 */
	public void sendViewSignature (ViewSignature viewSignature);

	/**
	 * Closes the connection to the remote peer
	 */
	public void close();


}