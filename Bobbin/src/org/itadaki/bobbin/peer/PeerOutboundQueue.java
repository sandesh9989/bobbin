/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.connectionmanager.Connection;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConstants;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.Piece;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.counter.StatisticCounter;


/**
 * Manages messages to be sent to the remote peer
 */
public class PeerOutboundQueue {

	/**
	 * The Connection to write data to
	 */
	private Connection connection;

	/**
	 * The PieceDatabase to read pieces from
	 */
	private PieceDatabase pieceDatabase;

	/**
	 * The last time any data was successfully written to the connection, in system milliseconds
	 */
	private long lastDataSentTime = 0;

	/**
	 * A counter to collect statistics on the number of block bytes that have been sent to the
	 * remote peer. PeerHandler consumes this information to provide to the choking algorithm
	 */
	private StatisticCounter blockBytesSentCounter;

	/**
	 * If {@code true}, a keepalive should be sent if more than KEEPALIVE_INTERVAL seconds have
	 * elapsed since the last data was sent
	 */
	private boolean queuedKeepalive = false;
	
	/**
	 * The queued interested message to send, if any, or {@code null}
	 */
	private Boolean queuedInterested = null;

	/**
	 * A list of queued piece messages to send to the remote peer
	 */
	private LinkedList<BlockDescriptor> queuedPieces = new LinkedList<BlockDescriptor>();

	/**
	 * A list of queued have messages to send to the remote peer
	 */
	private LinkedList<Integer> queuedHaves = new LinkedList<Integer>();

	/**
	 * A list of queued request messages to send to the remote peer
	 */
	private LinkedList<BlockDescriptor> queuedRequests = new LinkedList<BlockDescriptor>();

	/**
	 * A list of queued cancel messages to send to the remote peer
	 */
	private LinkedList<BlockDescriptor> queuedCancels = new LinkedList<BlockDescriptor>();

	/**
	 * A set of requests that have been sent but not yet received
	 */
	private Set<BlockDescriptor> sentRequests = new LinkedHashSet<BlockDescriptor>();

	/**
	 * A set of requests that have been cancelled, but not yet completed by the receipt of either
	 * a piece or a reject. Only used when the Fast extension is enabled
	 */
	private Set<BlockDescriptor> outstandingCancels = new LinkedHashSet<BlockDescriptor>();

	/**
	 * The set of piece numbers that we have Allowed Fast to the remote peer; piece messages with
	 * these indices may be sent even when we are choking the remote peer
	 */
	private Set<Integer> allowedFastPieceNumbers = new HashSet<Integer>();

	/**
	 * The set of piece numbers that the remote peer has Allowed Fast to us; request messages with
	 * these indices may be sent even when the remote peer is choking us
	 */
	private Set<Integer> allowedFastRequestNumbers = new HashSet<Integer>();

	/**
	 * If {@code true}, requests are plugged; no requests except Allowed Fast requests will be sent
	 * from the queue
	 */
	private boolean requestsPlugged = true;

	/**
	 * A map linking extension protocol identifier strings to their currently assigned ID number
	 * within the outgoing protocol stream
	 */
	private Map<String,Integer> extensionIdentifiers = new HashMap<String,Integer>();

	/**
	 * A list of extension protocol messages to send to the remote peer encoded as pairs of
	 * ByteBuffers
	 */
	private LinkedList<ByteBuffer[]> extensionMessageQueue = new LinkedList<ByteBuffer[]>();

	/**
	 * A list of ByteBuffers to send to the remote peer. When the peer is writeable, messages are
	 * added to the send queue one by one in order of importance and a write attempted until no
	 * more data can be written. When the network write capacity is full, therefore, one or more
	 * ByteBuffers may be left here to be completed when the peer becomes writeable again.
	 */
	private LinkedList<ByteBuffer> sendQueue = new LinkedList<ByteBuffer>();

	/**
	 * The style of pieces to send to the remote peer
	 */
	private PieceStyle pieceStyle = PieceStyle.PLAIN;

	/**
	 * The set of views implicitly valid to the remote peer
	 */
	private NavigableSet<Long> remotePeerViews = new TreeSet<Long>();

	/**
	 * Represents possible piece styles
	 */
	private static enum PieceStyle {

		/**
		 * Plain pieces
		 */
		PLAIN,

		/**
		 * Merkle pieces
		 */
		MERKLE,

		/**
		 * Elastic pieces
		 */
		ELASTIC

	}


	/**
	 * Sends a keepalive message if no data has been sent for the defined keepalive interval
	 */
	public void sendKeepaliveMessage() {

		if ((System.currentTimeMillis() - this.lastDataSentTime) > (PeerProtocolConstants.KEEPALIVE_INTERVAL * 1000)) {
			this.queuedKeepalive = true;
			this.connection.setWriteEnabled (true);
		}

	}


	/**
	 * Sends a bitfield message. Only valid as the first message that is sent after the initial
	 * handshake
	 *
	 * @param bitField The bitfield to send
	 */
	public void sendBitfieldMessage (BitField bitField) {

		this.sendQueue.add (PeerProtocolBuilder.bitfieldMessage (bitField));
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends a choke message. The remote peer will coordinate their request sending / resending
	 * based on this, so we put it in the send queue unconditionally.
	 * 
	 * <p>Any queued pieces for the remote peer are removed from the queue and returned
	 *
	 * @param choke The choke message to send
	 * @return The queued pieces that were removed from the queue
	 */
	public List<BlockDescriptor> sendChokeMessage (boolean choke) {

		List<BlockDescriptor> dequeuedPieces = new ArrayList<BlockDescriptor>();

		if (choke) {
			for (Iterator<BlockDescriptor> iterator = this.queuedPieces.iterator (); iterator.hasNext (); ) {
				BlockDescriptor request = iterator.next();
				if (!this.allowedFastPieceNumbers.contains (request.getPieceNumber())) {
					dequeuedPieces.add (request);
					iterator.remove();
				}
			}
		}

		this.sendQueue.add (choke ? PeerProtocolBuilder.chokeMessage() : PeerProtocolBuilder.unchokeMessage());
		this.connection.setWriteEnabled (true);

		return dequeuedPieces;

	}


	/**
	 * Sends an interested message. If there is already an interested message queued to be sent,
	 * and it is the opposite message, that message is cancelled from the queue instead.
	 *
	 * @param interested The interested message to send
	 */
	public void sendInterestedMessage (boolean interested) {

		if ((this.queuedInterested != null) && (interested == !this.queuedInterested)) {
			this.queuedInterested = null;
		} else {
			this.queuedInterested = interested;
			this.connection.setWriteEnabled (true);
		}

	}


	/**
	 * Sends a piece message. If there are too many requests alread in progress, the most recent
	 * request will be ignored
	 *
	 * @param descriptor The block to send
	 */
	public void sendPieceMessage (BlockDescriptor descriptor) {

		if (this.queuedPieces.size () < PeerProtocolConstants.MAXIMUM_INBOUND_REQUESTS) {
			this.queuedPieces.add (descriptor);
			this.connection.setWriteEnabled (true);
		}

	}


	/**
	 * Sends a request message
	 *
	 * @param requestDescriptor The request to send
	 */
	public void sendRequestMessage (BlockDescriptor requestDescriptor) {

		this.queuedRequests.add (requestDescriptor);
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends multiple request messages
	 *
	 * @param requestDescriptors The requests to send
	 */
	public void sendRequestMessages (List<BlockDescriptor> requestDescriptors) {

		this.queuedRequests.addAll (requestDescriptors);
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends a cancel message if a matching request has already been sent, or removes the matching
	 * request from the send queue
	 *
	 * @param request The request to cancel
	 * @param trackCancelled If {@code true}, the cancellation is tracked, and will be taken account
	 *        of later in {@link #requestReceived(BlockDescriptor)} and
	 *        {@link #rejectReceived(BlockDescriptor)}
	 */
	public void sendCancelMessage (BlockDescriptor request, boolean trackCancelled) {

		if (this.sentRequests.remove (request)) {
			this.queuedCancels.add (request);
			if (trackCancelled) {
				this.outstandingCancels.add (request);
			}
			this.connection.setWriteEnabled (true);
		}
		this.queuedRequests.remove (request);

	}


	/**
	 * Sends a have message
	 *
	 * @param pieceNumber The piece to send a have message for
	 */
	public void sendHaveMessage (int pieceNumber) {

		this.queuedHaves.add (pieceNumber);
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends a have all message. Only valid when the Fast extension is enabled, as the first message
	 * that is sent after the initial handshake
	 */
	public void sendHaveAllMessage() {

		this.sendQueue.add (PeerProtocolBuilder.haveAllMessage());
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends a have none message. Only valid when the Fast extension is enabled, as the first
	 * message that is sent after the initial handshake
	 */
	public void sendHaveNoneMessage() {

		this.sendQueue.add (PeerProtocolBuilder.haveNoneMessage());
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends a reject request message. Only valid when the Fast extension is enabled.
	 *
	 * @param descriptor The request to reject
	 */
	public void sendRejectRequestMessage (BlockDescriptor descriptor) {

		this.sendQueue.add (PeerProtocolBuilder.rejectRequestMessage (descriptor));
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends one or more reject request messages. Only valid when the Fast extension is enabled.
	 *
	 * @param descriptors The requests to reject
	 */
	public void sendRejectRequestMessages (List<BlockDescriptor> descriptors) {

		for (BlockDescriptor descriptor : descriptors) {
			this.sendQueue.add (PeerProtocolBuilder.rejectRequestMessage (descriptor));
		}
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends one or more Allowed Fast messages. The supplied indices are tracked for later use in
	 * {@link #sendChokeMessage(boolean)}. Only valid when the Fast extension is enabled.
	 *
	 * @param allowedFastSet The set of pieces that are Allowed Fast
	 */
	public void sendAllowedFastMessages (Set<Integer> allowedFastSet) {

		this.allowedFastPieceNumbers.addAll (allowedFastSet);

		for (Integer allowedFastPiece : allowedFastSet) {
			this.sendQueue.add (PeerProtocolBuilder.allowedFastMessage (allowedFastPiece));
		}
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends an Elastic Signature message
	 *
	 * @param viewSignature The view signature to send 
	 */
	public void sendElasticSignatureMessage (ViewSignature viewSignature) {

		if (this.remotePeerViews.size() > 1) {
			this.remotePeerViews.pollFirst();
		}
		this.remotePeerViews.add (viewSignature.getViewLength());

		this.extensionMessageQueue.add (PeerProtocolBuilder.elasticSignatureMessage (viewSignature));
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends an Elastic Bitfield message
	 *
	 * @param bitField The bitfield to send
	 */
	public void sendElasticBitfieldMessage (BitField bitField) {

		this.extensionMessageQueue.add (PeerProtocolBuilder.elasticBitfieldMessage (bitField));
		this.connection.setWriteEnabled (true);

	}

	/**
	 * Indicates whether a given piece is within the set of pieces we have Allowed Fast to the
	 * remote peer
	 *
	 * @param pieceNumber The piece number to check
	 * @return {@code true} if the given piece is Allowed Fast, otherwise {@code false}
	 */
	public boolean isPieceAllowedFast (Integer pieceNumber) {

		return this.allowedFastPieceNumbers.contains (pieceNumber);

	}


	/**
	 * Dishonour all previously issued Allowed Fast messages
	 */
	public void clearAllowedFastPieces() {

		this.allowedFastPieceNumbers.clear();

	}


	/**
	 * Indicates that the remote peer has Allowed Fast a given piece number
	 *
	 * @param pieceNumber The piece number that has been Allowed Fast
	 */
	public void setRequestAllowedFast (Integer pieceNumber) {

		this.allowedFastRequestNumbers.add (pieceNumber);

	}


	/**
	 * Sets whether requests are plugged. Requests are plugged as a response to the remote peer
	 * choking us; while plugged, no request messages will be sent from the queue
	 * @param requestsPlugged If {@code true}, requests are plugged; otherwise, requests are
	 *        unplugged
	 */
	public void setRequestsPlugged (boolean requestsPlugged) {

		this.requestsPlugged = requestsPlugged;

		// If there are any requests waiting, let them out
		if (!requestsPlugged && !this.queuedRequests.isEmpty()) {
			this.connection.setWriteEnabled (true);
		}

	}


	/**
	 * Indicates to the queue that a block has been received, and its request will no longer need
	 * to be resent
	 * @param request The block that has been received
	 * @return {@code true} if the block had been requested, {@code false} if it was unknown
	 */
	public boolean requestReceived (BlockDescriptor request) {

		if (this.sentRequests.remove (request)) {
			return true;
		} else if (this.outstandingCancels.remove (request)) {
			this.queuedCancels.remove (request);
			return true;
		}

		return false;

	}


	/**
	 * Indicates to the queue that a request has been rejected; if we have not cancelled the
	 * request, it is requeued. Only used when the Fast extension is enabled.
	 * @param requestDescriptor The request message to resend. If the request was not outstanding,
	 *        no action will be taken
	 * @return {@code true} if a previously unacknowledged requested piece (including cancelled
	 *         requests) corresponds to the given rejection
	 */
	public boolean rejectReceived (BlockDescriptor requestDescriptor) {

		if (this.sentRequests.remove (requestDescriptor)) {
			// If the remote peer dishonours an Allowed Fast request (for instance because we now
			// have enough pieces), we should avoid spamming them with the same request
			this.allowedFastRequestNumbers.remove (new Integer (requestDescriptor.getPieceNumber()));

			this.queuedRequests.add (requestDescriptor);
			this.connection.setWriteEnabled (true);
			return true;
		} else if (this.outstandingCancels.remove (requestDescriptor)) {
			return true;
		}

		return false;

	}


	/**
	 * Requeues all previously sent request messages that are still outstanding
	 */
	public void requeueAllRequestMessages() {

		this.queuedRequests.addAll (this.sentRequests);
		this.sentRequests.clear();

	}


	/**
	 * Discards a piece message from the unsent pieces queue if it is present
	 *
	 * @param blockDescriptor The descriptor of the piece message to discard
	 * @return {@code true} if the message was removed from the queue
	 */
	public boolean discardPieceMessage (BlockDescriptor blockDescriptor) {

		return this.queuedPieces.remove (blockDescriptor);

	}


	/**
	 * Discards piece messages from the unsent pieces queue and sends reject request messages
	 *
	 * @param pieceNumber The piece number to reject
	 */
	public void rejectPieceMessages (int pieceNumber) {

		for (Iterator<BlockDescriptor> iterator = this.queuedPieces.iterator (); iterator.hasNext (); ) {
			BlockDescriptor request = iterator.next();
			if (request.getPieceNumber() == pieceNumber) {
				sendRejectRequestMessage (request);
				iterator.remove();
			}
		}

	}


	/**
	 * Returns the number of requests by the remote peer that have not yet started to be sent
	 *
	 * @return The number of requests by the remote peer that have not yet started to be sent
	 */
	public int getUnsentPieceCount() {

		return this.queuedPieces.size();

	}


	/**
	 * Tests if there are outstanding requests to the remote peer (requests that have been sent or
	 * queued to be sent but not yet acknowledged as having been received
	 *
	 * @return {@code true} if there are outstanding requests to the remote peer, otherwise
	 * {@code false}
	 */
	public boolean hasOutstandingRequests() {

		return (!this.queuedRequests.isEmpty() || !this.sentRequests.isEmpty());

	}


	/**
	 * Returns the number of requests that can be made to the remote peer
	 *
	 * @return The number of requests that can be made to the remote peer
	 */
	public int getRequestsNeeded() {

		return Math.max (0, PeerProtocolConstants.MAXIMUM_OUTBOUND_REQUESTS - this.sentRequests.size() - this.queuedRequests.size());

	}


	/**
	 * Indicates whether a given extension is enabled for the outbound protocol
	 *
	 * @param identifier The extension indicator to test for
	 * @return {@code true} if the extension is enabled, otherwise {@code false}
	 */
	public boolean isExtensionEnabled (String identifier) {

		return this.extensionIdentifiers.containsKey (identifier);

	}


	/**
	 * Sends an extension handshake message
	 *
	 * @param extensionsAdded The set of extensions enabled by this handshake
	 * @param extensionsRemoved The set of extensions disabled by this handshake
	 * @param extra A set of key/value pairs to include in the handshake message, or {@code null}
	 */
	public void sendExtensionHandshake (Set<String> extensionsAdded, Set<String> extensionsRemoved, BDictionary extra)
	{

		Map<String,Integer> extensions = new HashMap<String,Integer>();

		if (extensionsAdded != null) {
			SortedSet<Integer> extensionMessageIDs = new TreeSet<Integer> (this.extensionIdentifiers.values());
			int nextExtensionMessageID = (extensionMessageIDs.isEmpty() ?
					PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_CUSTOM :
					Math.max (PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_CUSTOM, extensionMessageIDs.last() + 1)
			);
			for (String identifier : extensionsAdded) {
				int extensionMessageID;
				if (PeerProtocolConstants.EXTENSION_MERKLE.equals (identifier)) {
					extensionMessageID = PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE;
				} else if (PeerProtocolConstants.EXTENSION_ELASTIC.equals (identifier)) {
					extensionMessageID = PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC;
				} else {
					extensionMessageID = nextExtensionMessageID++;
				}
				this.extensionIdentifiers.put (identifier, extensionMessageID);
				extensions.put (identifier, extensionMessageID);
			}
		}

		if (extensionsRemoved != null) {
			for (String identifier : extensionsRemoved) {
				this.extensionIdentifiers.remove (identifier);
				extensions.put (identifier, 0);
			}
		}

		// TODO Refactor - Better Merkle/Elastic negotiation needed

		if (this.extensionIdentifiers.containsKey (PeerProtocolConstants.EXTENSION_MERKLE)) {
			this.pieceStyle = PieceStyle.MERKLE;
		}

		if (this.extensionIdentifiers.containsKey (PeerProtocolConstants.EXTENSION_ELASTIC)) {
			this.pieceStyle = PieceStyle.ELASTIC;
		}

		this.extensionMessageQueue.add (PeerProtocolBuilder.extensionHandshakeMessage (extensions, extra));
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Sends an extension message
	 *
	 * @param identifier The extension identifier
	 * @param data The message data
	 */
	public void sendExtensionMessage (String identifier, ByteBuffer data) {

		Integer extendedMessageID = this.extensionIdentifiers.get (identifier);
		this.extensionMessageQueue.add (PeerProtocolBuilder.extensionMessage (extendedMessageID, data));
		this.connection.setWriteEnabled (true);

	}


	/**
	 * Indicates whether the queue contains one or more unsent extension messages for a given
	 * extension identifier
	 *
	 * @param identifier The extension identifier to test for
	 * @return {@code true} if one or more extension messages for the given identifier have been
	 *         queued, but have not yet started to be sent
	 */
	public boolean hasOutstandingExtensionMessage (String identifier) {

		if (this.extensionIdentifiers.containsKey (identifier)) {
			for (ByteBuffer[] buffers : this.extensionMessageQueue) {
				byte extensionMessageID = (byte) (this.extensionIdentifiers.get (identifier) & 0xff);
				if (buffers[0].get (5) == extensionMessageID) {
					return true;
				}
			}
		}

		return false;

	}


	/**
	 * Sends as much queued data as possible
	 * @return The number of bytes written, possibly zero
	 * @throws IOException If the connection is closed or on any other I/O error
	 */
	public int sendData() throws IOException {

		int bytesSent = 0;

		try {

			// Try to write any buffers waiting in the send queue
			while (!this.sendQueue.isEmpty()) {
				ByteBuffer buffer = this.sendQueue.peek();
				bytesSent += this.connection.write (buffer);
				if (buffer.remaining() == 0) {
					this.sendQueue.remove();
				} else {
					return bytesSent;
				}
			}

			// Try to write extension messages, if any
			while (!this.extensionMessageQueue.isEmpty ()) {
				ByteBuffer[] buffers = this.extensionMessageQueue.poll();
				bytesSent += this.connection.write (buffers);
				if (buffers[1].hasRemaining()) {
					this.sendQueue.add (buffers[0]);
					this.sendQueue.add (buffers[1]);
					return bytesSent;
				}
			}

			// Try to write an interested message, if any
			if (this.queuedInterested != null) {
				ByteBuffer buffer = (this.queuedInterested ? PeerProtocolBuilder.interestedMessage() : PeerProtocolBuilder.notInterestedMessage());
				this.queuedInterested = null;

				bytesSent += this.connection.write (buffer);
				if (buffer.remaining() > 0) {
					this.sendQueue.add (buffer);
					return bytesSent;
				}
			}

			// Try to write cancel messages, if any
			while (!this.queuedCancels.isEmpty()) {
				BlockDescriptor descriptor = this.queuedCancels.poll();
				ByteBuffer buffer = PeerProtocolBuilder.cancelMessage (descriptor);

				bytesSent += this.connection.write (buffer);
				if (buffer.remaining() > 0) {
					this.sendQueue.add (buffer);
					return bytesSent;
				}
			}

			// Try to write request messages, if any
			for (Iterator<BlockDescriptor> iterator = this.queuedRequests.iterator(); iterator.hasNext(); ) {
				BlockDescriptor descriptor = iterator.next();
				if (!this.requestsPlugged || this.allowedFastRequestNumbers.contains (descriptor.getPieceNumber())) {
					iterator.remove();
					this.sentRequests.add (descriptor);
					ByteBuffer buffer = PeerProtocolBuilder.requestMessage (descriptor);

					bytesSent += this.connection.write (buffer);
					if (buffer.remaining() > 0) {
						this.sendQueue.add (buffer);
						return bytesSent;
					}
				}
			}

			// Try to write have messages, if any
			while (!this.queuedHaves.isEmpty()) {
				Integer pieceNumber = this.queuedHaves.poll();
				ByteBuffer buffer = PeerProtocolBuilder.haveMessage (pieceNumber);

				bytesSent += this.connection.write (buffer);
				if (buffer.hasRemaining()) {
					this.sendQueue.add (buffer);
					return bytesSent;
				}
			}

			// Try to write piece messages, if any
			while (!this.queuedPieces.isEmpty()) {
				BlockDescriptor request = this.queuedPieces.poll();
				this.blockBytesSentCounter.add (request.getLength());
				Piece piece = this.pieceDatabase.readPiece (request.getPieceNumber());
				ByteBuffer block = piece.getBlock (request);

				ByteBuffer[] buffers = null;
				switch (this.pieceStyle) {
					case PLAIN:
						buffers = PeerProtocolBuilder.pieceMessage (request, block);
						break;
					case MERKLE:
						ByteBuffer merkleHashChain = (request.getOffset() == 0) ? piece.getHashChain().getHashes() : null;
						buffers = PeerProtocolBuilder.merklePieceMessage (request, merkleHashChain, block);
						break;
					case ELASTIC:
						long viewLength = piece.getHashChain().getViewLength();
						ArrayList<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
						if (!this.remotePeerViews.contains (viewLength) && (viewLength > this.pieceDatabase.getInfo().getStorageDescriptor().getLength())) {
							ViewSignature viewSignature = this.pieceDatabase.getViewSignature (viewLength);
							bufferList.addAll (Arrays.asList (PeerProtocolBuilder.elasticSignatureMessage (viewSignature)));
							if (this.remotePeerViews.size() > 1) {
								this.remotePeerViews.pollFirst();
							}
							this.remotePeerViews.add (viewLength);
						}
						ByteBuffer elasticHashChain = (request.getOffset() == 0) ? piece.getHashChain().getHashes() : null;
						bufferList.addAll (Arrays.asList (PeerProtocolBuilder.elasticPieceMessage (request, viewLength, elasticHashChain, block)));
						buffers = bufferList.toArray (new ByteBuffer[0]);
						break;
					default:
						// Can't happen
						throw new InternalError();
				}

				bytesSent += this.connection.write (buffers);
				if (buffers[buffers.length - 1].hasRemaining()) {
					this.sendQueue.addAll (Arrays.asList (buffers));
					return bytesSent;
				}
			}

			// Send a keepalive if necessary
			if (
					   this.queuedKeepalive
					&& ((System.currentTimeMillis() - this.lastDataSentTime) > (PeerProtocolConstants.KEEPALIVE_INTERVAL * 1000))
			   )
			{
				ByteBuffer buffer = PeerProtocolBuilder.keepaliveMessage();
				bytesSent += this.connection.write (buffer);
				if (buffer.hasRemaining()) {
					this.sendQueue.add (buffer);
					return bytesSent;
				}
			}
			this.queuedKeepalive = false;

			// If there are no more messages, tell the connection we don't need to write anything
			this.connection.setWriteEnabled (false);

			return bytesSent;

		} finally {
			this.lastDataSentTime = System.currentTimeMillis();
		}

	}


	/**
	 * @param connection The Connection to write to the remote peer through
	 * @param pieceDatabase The PieceDatabase to read piece data from
	 * @param sentBlockCounter A periodic counter to collect statistics on the number of blocks that
	 *        have been sent to the remote peer
	 */
	public PeerOutboundQueue (Connection connection, PieceDatabase pieceDatabase, StatisticCounter sentBlockCounter) {

		this.connection = connection;
		this.pieceDatabase = pieceDatabase;
		this.blockBytesSentCounter = sentBlockCounter;

	}


}
