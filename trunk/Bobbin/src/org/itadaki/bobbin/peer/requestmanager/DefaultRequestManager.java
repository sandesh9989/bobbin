/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer.requestmanager;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConstants;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.Piece;
import org.itadaki.bobbin.torrentdb.StorageDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.elastictree.HashChain;



/**
 * A {@link RequestManager} that implements a random piece allocation order
 */
public class DefaultRequestManager implements RequestManager {

	/**
	 * The {@code StorageDescriptor} for the managed torrent
	 */
	private StorageDescriptor storageDescriptor;

	/**
	 * The listener to inform of events
	 */
	private RequestManagerListener listener;

	/**
	 * The set of pieces that are needed and not available in the PieceDatabase
	 */
	private BitField neededPieces;

	/**
	 * The cached number of pieces that are needed
	 */
	private int neededPieceCount;

	/**
	 * The total numbers of each piece that are available from currently
	 * connected peers
	 */
	private short[] pieceAvailability;

	/**
	 * The numbers of pieces that we still need
	 */
	private List<Integer> piecePriority;

	/**
	 * Partially complete pieces that have been abandoned. They will be allocated preferentially to
	 * minimise the quantity of incomplete piece data we hold
	 */
	private Map<Integer,Piece> orphanedPieces = new HashMap<Integer,Piece>();

	/**
	 * The request allocation and piece assembly state of known peers
	 */
	private Map<ManageablePeer,PeerState> peerStates = new HashMap<ManageablePeer,PeerState>();


	/**
	 * The request allocation and piece assembly state of a single peer
	 */
	private class PeerState {

		/**
		 * Pieces being assembled by this peer
		 */
		public Map<Integer,Piece> pieces = new HashMap<Integer,Piece>();

		/**
		 * Requests belonging to a piece allocated to this peer but which have not yet been sent to
		 * the peer
		 */
		private List<BlockDescriptor> unissuedRequests = new LinkedList<BlockDescriptor>();

		/**
		 * Requests that have been sent to the peer
		 */
		private Set<BlockDescriptor> issuedRequests = new HashSet<BlockDescriptor>();

		/**
		 * A set of pieces that are Allowed Fast for this peer
		 */
		private Set<Integer> allowedFastPieces = new HashSet<Integer>(); 

		/**
		 * A set of pieces that are suggested for this peer
		 */
		private Set<Integer> suggestedPieces = new HashSet<Integer>(); 


		/**
		 * @param peerViewLength The peer's current view length
		 * @param peerBitField The peer's current bitfield
		 * @param pieceNumber The piece number
		 * @return {@code true} if the peer has the given piece and it is compatible
		 */
		private boolean peerHasCompatiblePiece (long peerViewLength, BitField peerBitField, int pieceNumber) {

			
			return (pieceNumber < peerBitField.length())
			    && peerBitField.get (pieceNumber)
			    && (
			               (pieceNumber < (peerBitField.length() - 1))
			            || (peerViewLength == DefaultRequestManager.this.storageDescriptor.getLength())
			            || (peerViewLength % DefaultRequestManager.this.storageDescriptor.getPieceSize() == 0)
			       );

		}


		/**
		 * Allocates requests from a given collection
		 *
		 * @param pieceNumbers The collection to allocate from; pieces are removed from the
		 *        collection, then allocated if available from the peer. If {@code null}, pieces are
		 *        allocated directly from the top of the global priority queue. Allocated pieces are
		 *        moved to the back of the global priority queue.
		 * @param peerViewLength The length of the peer's view
		 * @param peerBitField The current bitfield of the peer
		 * @param numRequests The number of requests to allocate
		 * @param peerViewLength The peer's current view length
		 */
		private void allocateRequestsFrom (Collection<Integer> pieceNumbers, long peerViewLength, BitField peerBitField, int numRequests) {

			// Attempt to allocate pieces from the queue
			Iterator<Integer> iterator = (pieceNumbers == null) ? DefaultRequestManager.this.piecePriority.iterator() : pieceNumbers.iterator();
			List<Integer> removedPieces = new LinkedList<Integer>();
			for (; numRequests > this.unissuedRequests.size() && iterator.hasNext();) {
				Integer pieceNumber = iterator.next();
				if ((pieceNumbers != null) && pieceIsAllocated (pieceNumber)) {
					iterator.remove();
				} else {
					if (peerHasCompatiblePiece (peerViewLength, peerBitField, pieceNumber) && !this.pieces.containsKey (pieceNumber)) {
						// Check for orphaned pieces first
						Piece piece = DefaultRequestManager.this.orphanedPieces.remove (pieceNumber);
						if (piece == null) {
							piece = new Piece (pieceNumber, DefaultRequestManager.this.storageDescriptor.getPieceLength (pieceNumber),
									PeerProtocolConstants.BLOCK_LENGTH);
						}
						this.pieces.put (pieceNumber, piece);
						this.unissuedRequests.addAll (piece.getNeededBlocks());

						iterator.remove();
						if (pieceNumbers != null) {
							DefaultRequestManager.this.piecePriority.remove (pieceNumber);
						}
						removedPieces.add (pieceNumber);
					}
				}
			}
			DefaultRequestManager.this.piecePriority.addAll (removedPieces);

		}

		/**
		 * Allocate requests to this peer
		 * @param peerViewLength The peer's current view length
		 * @param peerBitField The peer's current bitfield
		 * @param numRequests The number of requests to allocate
		 * @param allowedFastOnly If {@code true}, only allocate Allowed Fast pieces
		 *
		 * @return A list of up to the requested number of {@code BlockDescriptor}s
		 */
		public List<BlockDescriptor> allocateRequests (long peerViewLength, BitField peerBitField, int numRequests, boolean allowedFastOnly) {

			if (allowedFastOnly) {
				allocateRequestsFrom (this.allowedFastPieces, peerViewLength, peerBitField, numRequests);
			} else {
				// Attempt to allocate orphaned pieces first
				List<Integer> orphanPieceNumbers = new ArrayList<Integer> (DefaultRequestManager.this.orphanedPieces.keySet());
				allocateRequestsFrom (orphanPieceNumbers, peerViewLength, peerBitField, numRequests);
	
				// Attempt to allocate suggested pieces
				allocateRequestsFrom (this.suggestedPieces, peerViewLength, peerBitField, numRequests);
	
				// Attempt to allocate pieces from the queue
				allocateRequestsFrom (null, peerViewLength, peerBitField, numRequests);
			}

			List<BlockDescriptor> allocatedRequests = new ArrayList<BlockDescriptor>();
			int numAllocated = Math.min (numRequests, this.unissuedRequests.size());
			List<BlockDescriptor> requestsToIssue = this.unissuedRequests.subList (0, numAllocated);
			allocatedRequests.addAll (requestsToIssue);
			this.issuedRequests.addAll (allocatedRequests);
			this.unissuedRequests.subList (0, numAllocated).clear();

			return allocatedRequests;

		}

		/**
		 * Adds the given piece to the peer's Allowed Fast set
		 * @param pieceNumber The piece that is Allowed Fast
		 */
		public void setPieceAllowedFast (Integer pieceNumber) {

			if (this.allowedFastPieces.size () <= PeerProtocolConstants.MAXIMUM_SUGGESTED_PIECES) {
				this.allowedFastPieces.add (pieceNumber);
			}

		}

		/**
		 * Adds the given piece to the peer's suggested set
		 * @param pieceNumber The piece that is Allowed Fast
		 */
		public void setPieceSuggested (Integer pieceNumber) {

			if (this.allowedFastPieces.size () <= PeerProtocolConstants.MAXIMUM_SUGGESTED_PIECES) {
				this.suggestedPieces.add (pieceNumber);
			}

		}

		/**
		 * Cancels any requests for the given piece number and returns a list of the cancelled
		 * {@code BlockDescriptor}s
		 *
		 * @param pieceNumber The piece number to cancel
		 * @return The list of cancelled BlockDescriptors if any, or null
		 */
		public List<BlockDescriptor> cancelRequestsForPiece (Integer pieceNumber) {

			if (this.pieces.containsKey (pieceNumber)) {

				List<BlockDescriptor> blocksToCancel = new ArrayList<BlockDescriptor>();

				for (Iterator<BlockDescriptor> iterator = this.issuedRequests.iterator(); iterator.hasNext();) {
					BlockDescriptor descriptor = iterator.next();
					if (descriptor.getPieceNumber() == pieceNumber) {
						blocksToCancel.add (descriptor);
						iterator.remove();
					}
				}

				for (Iterator<BlockDescriptor> iterator = this.unissuedRequests.iterator(); iterator.hasNext();) {
					BlockDescriptor descriptor = iterator.next();
					if (descriptor.getPieceNumber() == pieceNumber) {
						iterator.remove();
					}
				}

				this.pieces.remove (pieceNumber);

				return blocksToCancel;

			}

			return null;

		}


		/**
		 * Cancels any requests for pieces that are not in the given bitfield, and returns a list of
		 * the cancelled {@code BlockDescriptor}s
		 *
		 * @param bitField The bitfield to cancel unneeded requests for
		 * @return The list of cancelled {@code BlockDescriptor}s
		 */
		public List<BlockDescriptor> cancelUnneededRequests (BitField bitField) {

			List<BlockDescriptor> blocksToCancel = new ArrayList<BlockDescriptor>();

			for (Iterator<Integer> pieceIterator = this.pieces.keySet().iterator(); pieceIterator.hasNext();) {

				Integer pieceNumber = pieceIterator.next();

				if (!bitField.get (pieceNumber)) {

					for (Iterator<BlockDescriptor> blockIterator = this.issuedRequests.iterator(); blockIterator.hasNext();) {
						BlockDescriptor descriptor = blockIterator.next();
						if (descriptor.getPieceNumber() == pieceNumber) {
							blocksToCancel.add (descriptor);
							blockIterator.remove();
						}
					}

					for (Iterator<BlockDescriptor> blockIterator = this.unissuedRequests.iterator(); blockIterator.hasNext();) {
						BlockDescriptor descriptor = blockIterator.next();
						if (descriptor.getPieceNumber() == pieceNumber) {
							blockIterator.remove();
						}
					}

					pieceIterator.remove();

				}

			}

			return blocksToCancel;

		}

	}


	/* RequestManager interface */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerCoordinatorListener#peerRegistered(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public void peerRegistered (ManageablePeer peer) {

		if (this.peerStates.containsKey (peer)) {
			throw new IllegalArgumentException();
		}
		PeerState peerState = new PeerState();
		this.peerStates.put (peer, peerState);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerCoordinatorListener#peerDeregistered(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public void peerDeregistered (ManageablePeer peer) {

		// The peer's available pieces are subtracted from the available piece counts.
		// The peer's unfinished requests are orphaned and preferentially allocated to other peers in
		// order to minimise the number of partial pieces held.

		PeerState peerState = this.peerStates.get (peer);

		// Orphan any pieces the peer has in progress
		for (Piece piece : peerState.pieces.values()) {
			if (!this.orphanedPieces.containsKey (piece.getPieceNumber())) {
				this.orphanedPieces.put (piece.getPieceNumber(), piece);
			}
		}

		// Subtract the peer's available pieces from the available piece counts
		BitField bitField = peer.getRemoteBitField();
		for (Integer pieceNumber : bitField) {
			this.pieceAvailability[pieceNumber]--;
		}

		this.peerStates.remove (peer);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerCoordinatorListener#peerCoordinatorCompleted()
	 */
	public void peerCoordinatorCompleted() {

		// Do nothing

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.requestmanager.RequestManager#piecesAvailable(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public boolean piecesAvailable (ManageablePeer peer) {

		BitField bitField = peer.getRemoteBitField();
		for (int i = 0; i < bitField.length(); i++) {
			this.pieceAvailability[i] += bitField.get (i) ? 1 : 0;
		}

		if (bitField.intersects (this.neededPieces)) {
			return true;
		}

		return false;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.requestmanager.RequestManager#pieceAvailable(org.itadaki.bobbin.peer.ManageablePeer, int)
	 */
	public boolean pieceAvailable (ManageablePeer peer, int pieceNumber) {

		this.pieceAvailability[pieceNumber]++;

		return this.neededPieces.get (pieceNumber);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.requestmanager.RequestManager#pieceSuggested(org.itadaki.bobbin.peer.ManageablePeer, int)
	 */
	public void setPieceAllowedFast (ManageablePeer peer, int pieceNumber) {

		PeerState peerState = this.peerStates.get (peer);
		peerState.setPieceAllowedFast (pieceNumber);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.requestmanager.RequestManager#setPieceSuggested(org.itadaki.bobbin.peer.ManageablePeer, int)
	 */
	public void setPieceSuggested (ManageablePeer peer, int pieceNumber) {

		PeerState peerState = this.peerStates.get (peer);
		peerState.setPieceSuggested (pieceNumber);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.requestmanager.RequestManager#allocateRequests(org.itadaki.bobbin.peer.ManageablePeer, int)
	 */
	public List<BlockDescriptor> allocateRequests (ManageablePeer peer, int numRequests, boolean allowedFastOnly) {

		PeerState peerState = this.peerStates.get (peer);

		return peerState.allocateRequests (peer.getPeerState().getRemoteView().getLength(), peer.getRemoteBitField(), numRequests, allowedFastOnly);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.requestmanager.RequestManager#handleBlock(org.itadaki.bobbin.peer.ManageablePeer, org.itadaki.bobbin.torrentdb.BlockDescriptor, org.itadaki.bobbin.torrentdb.ViewSignature, org.itadaki.bobbin.util.elastictree.HashChain, java.nio.ByteBuffer)
	 */
	public void handleBlock (ManageablePeer peer, BlockDescriptor descriptor, ViewSignature viewSignature, HashChain hashChain, ByteBuffer block) {

		Integer pieceIndex = descriptor.getPieceNumber();

		PeerState peerState = this.peerStates.get (peer);
		Piece piece = peerState.pieces.get (pieceIndex);

		peerState.issuedRequests.remove (descriptor);

		if (piece != null) {
			if (hashChain != null) {
				piece.setHashChain (hashChain);
				piece.setViewSignature (viewSignature);
			}
			if (piece.putBlock (descriptor, block)) {
				peerState.pieces.remove (pieceIndex);
				this.listener.pieceAssembled (piece);
			}
		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.requestmanager.RequestManager#setNeededPieces(org.itadaki.bobbin.util.BitField)
	 */
	public void setNeededPieces (BitField neededPieces) {

		this.neededPieces = neededPieces.clone();
		this.neededPieceCount = this.neededPieces.cardinality();

		this.piecePriority = new LinkedList<Integer>();
		for (Integer pieceIndex : this.neededPieces) {
			this.piecePriority.add (pieceIndex);
		}
		Collections.shuffle (this.piecePriority);

		for (ManageablePeer peer : this.peerStates.keySet()) {
			BitField peerBitField = peer.getRemoteBitField();
			peer.setWeAreInterested (peerBitField.intersects (this.neededPieces));

			PeerState peerState = this.peerStates.get (peer);
			List<BlockDescriptor> requestsToCancel = peerState.cancelUnneededRequests (this.neededPieces);
			if (requestsToCancel.size() > 0) {
				peer.cancelRequests (requestsToCancel);
			}
		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.requestmanager.RequestManager#setPieceNotNeeded(int)
	 */
	public void setPieceNotNeeded (int pieceNumber) {

		this.neededPieces.clear (pieceNumber);
		this.neededPieceCount--;
		this.piecePriority.remove (new Integer (pieceNumber));
		this.orphanedPieces.remove (pieceNumber);
		cancelRequestsForPiece (pieceNumber);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.requestmanager.RequestManager#getNeededPieceCount()
	 */
	public int getNeededPieceCount() {

		return this.neededPieceCount;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.requestmanager.RequestManager#extend(org.itadaki.bobbin.torrentdb.StorageDescriptor)
	 */
	public void extend (StorageDescriptor storageDescriptor) {

		if (storageDescriptor.getLength() < this.storageDescriptor.getLength()) {
			throw new IllegalArgumentException ("Cannot extend to shorter length");
		}

		if (storageDescriptor.getPieceSize () != this.storageDescriptor.getPieceSize()) {
			throw new IllegalArgumentException ("Cannot change piece size");
		}

		if (!this.storageDescriptor.isRegular()) {
			// TODO Optimisation - If there is a piece in progress, we could theoretically recycle its blocks
			int lastPieceNumber = this.storageDescriptor.getNumberOfPieces() - 1;
			cancelRequestsForPiece (lastPieceNumber);
			this.orphanedPieces.remove (lastPieceNumber);
		}

		this.pieceAvailability = Arrays.copyOf (this.pieceAvailability, storageDescriptor.getNumberOfPieces());
		this.neededPieces.extend (storageDescriptor.getNumberOfPieces());
		this.storageDescriptor = storageDescriptor;

	}


	/**
	 * Cancels all requests for a given piece
	 *
	 * @param pieceNumber The piece to cancel
	 */
	private void cancelRequestsForPiece(int pieceNumber) {

		for (ManageablePeer peer : this.peerStates.keySet()) {
			PeerState peerState = this.peerStates.get (peer);
			List<BlockDescriptor> requestsToCancel = peerState.cancelRequestsForPiece (pieceNumber);
			if (requestsToCancel != null) {
				peer.cancelRequests (requestsToCancel);
			}
			if (this.neededPieceCount == 0) {
				peer.setWeAreInterested (false);
			}
		}

	}


	/**
	 * Indicates whether a given piece is currently allocated to one or more peers
	 *
	 * @param pieceNumber The piece to check for allocations
	 * @return {@code true} if the piece is allocated, otherwise {@code false}
	 */
	private boolean pieceIsAllocated (Integer pieceNumber) {

		for (PeerState peerState : DefaultRequestManager.this.peerStates.values()) {
			if (peerState.pieces.containsKey (pieceNumber)) {
				return true;
			}
		}

		return false;

	}


	/**
	 * @param storageDescriptor The {@code StorageDescriptor} for the managed torrent
	 * @param listener The listener to inform of events
	 */
	public DefaultRequestManager (StorageDescriptor storageDescriptor, RequestManagerListener listener) {

		this.storageDescriptor = storageDescriptor;
		this.listener = listener;
		this.pieceAvailability = new short [storageDescriptor.getNumberOfPieces()];
		this.neededPieces = new BitField (storageDescriptor.getNumberOfPieces());
		this.neededPieceCount = 0;

	}


}
