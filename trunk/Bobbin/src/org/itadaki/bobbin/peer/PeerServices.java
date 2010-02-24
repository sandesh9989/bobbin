package org.itadaki.bobbin.peer;

import java.util.List;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.counter.StatisticCounter;
import org.itadaki.bobbin.util.elastictree.HashChain;


/**
 * A PeerHandler's view of the peer set management services provided by a PeerCoordinator
 */
public interface PeerServices {

	/**
	 * Returns the local peer ID
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 * 
	 * @return The local peer ID
	 */
	public PeerID getLocalPeerID();

	/**
	 * Registers a new peer that has completed its handshake. If a peer with the same ID is already
	 * connected, the peer will not be registered.
	 *
	 *<p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @param peer The peer to register
	 * @return True if the peer was registered, false if it was not registered
	 */
	public boolean peerConnected (ManageablePeer peer);

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
	 * Offers all available extensions to a peer
	 *
	 * @param peerHandler
	 */
	public void offerExtensionsToPeer (ExtensiblePeer peerHandler);

	/**
	 * Enables and disables extensions in response to a peer's extension handshake message
	 *
	 * @param peer The peer that has enabled and/or disabled extensions
	 * @param extensionsEnabled The extension identifiers that have been enabled, or {@code null}
	 * @param extensionsDisabled The extension identifiers that have been disabled, or {@code null}
	 * @param extra Additional key/value pairs 
	 */
	public void enableDisablePeerExtensions (ExtensiblePeer peer, Set<String> extensionsEnabled, Set<String> extensionsDisabled,
			BDictionary extra);

	/**
	 * Processes a peer extension message through the appropriate handler
	 *
	 * @param peer The peer that sent the message
	 * @param identifier The extension's identifier
	 * @param messageData The message payload
	 */
	public void processExtensionMessage (ExtensiblePeer peer, String identifier, byte[] messageData);

	/**
	 * Adds all of a peer's available pieces to the available piece map
	 *
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @param peer The peer to add pieces from
	 * @return {@code true} if the peer has one or more pieces we are interested in, otherwise
	 *         {@code false}
	 */
	public boolean addAvailablePieces (ManageablePeer peer);

	/**
	 * Adds a single piece to the available piece map
	 *
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @param peer The peer that has the piece
	 * @param pieceNumber The piece number to add to the available piece map
	 * @return {@code true} if the peer has one or more pieces we are interested in, otherwise
	 *         {@code false}
	 */
	public boolean addAvailablePiece (ManageablePeer peer, int pieceNumber);

	/**
	 * Get requests to send to a remote peer
	 *
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @param peer The peer that we should send requests to
	 * @param numRequests The number of requests we should send
	 * @param allowedFastOnly If {@code true}, only pieces that have been marked Allowed Fast for
	 *        the given peer will be allocated
	 * @return The requests to send
	 */
	public List<BlockDescriptor> getRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly);

	/**
	 * Indicates that the remote peer has Allowed Fast a given piece
	 *
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @param peer The peer that Allowed Fast the piece
	 * @param pieceNumber The piece that was Allowed Fast. Must be a piece actually advertised by
	 *        the given peer
	 */
	public void setPieceAllowedFast (ManageablePeer peer, int pieceNumber);

	/**
	 * Indicates that the remote peer has suggested a given piece
	 *
	 * @param peer The peer that suggested the piece
	 * @param pieceNumber The piece that was suggested
	 */
	public void setPieceSuggested (ManageablePeer peer, int pieceNumber);

	/**
	 * Handle a piece block that has been received
	 *
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @param peer The peer that received the block
	 * @param request The request that was fulfilled
	 * @param viewSignature The view signature for the supplied hash chain, or {@code null}
	 * @param hashChain The supplied hash chain, if present, or {@code null}
	 * @param block The data of the block
	 */
	public void handleBlock (ManageablePeer peer, BlockDescriptor request, ViewSignature viewSignature, HashChain hashChain, byte[] block);

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
	 * <p><b>Thread safety:</b> This method must be called with the peer context lock held
	 *
	 * @return The PieceDatabase for this torrent
	 */
	public PieceDatabase getPieceDatabase();

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

	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The shared counter for protocol bytes sent from this peer to the peer set
	 */
	public StatisticCounter getProtocolBytesSentCounter();

	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The shared counter for protocol bytes received by this peer from the peer set
	 */
	public StatisticCounter getProtocolBytesReceivedCounter();

	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The shared counter for whole blocks sent from this peer to the peer set
	 */
	public StatisticCounter getBlockBytesSentCounter();

	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The shared counter for whole blocks received by this peer from the peer set
	 */
	public StatisticCounter getBlockBytesReceivedCounter();

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

}