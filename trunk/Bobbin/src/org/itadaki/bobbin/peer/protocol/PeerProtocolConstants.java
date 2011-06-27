/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer.protocol;


/**
 * Constants used in the peer protocol
 */
public class PeerProtocolConstants {

	/**
	 * The maximum length of a piece request - 128K
	 */
	public static final int MAXIMUM_PIECE_LENGTH = 131072;

	/**
	 * The maximum length of a protocol message (after its length header)<br>
	 * Large enough for the largest possible "piece" message.
	 */
	public static final int MAXIMUM_MESSAGE_LENGTH = MAXIMUM_PIECE_LENGTH + 9;

	/**
	 * The length used when requesting piece blocks
	 */
	public static final int BLOCK_LENGTH = 16384;

	/**
	 * The maximum length allowed for requests from remote peers
	 */
	public static final int MAXIMUM_BLOCK_LENGTH = 32768;

	/**
	 * The maximum number of pending requests to a single remote peer
	 */
	public static final int MAXIMUM_OUTBOUND_REQUESTS = 30;

	/**
	 * The maximum number of pending requests a single remote peer may have before we start
	 * ignoring some
	 */
	public static final int MAXIMUM_INBOUND_REQUESTS = 250;

	/**
	 * The number of seconds of outbound data inactivity after which a keepalive should be sent
	 */
	public static final int KEEPALIVE_INTERVAL = 120;

	/**
	 * The number of seconds of inbound data inactivity after which the connection should be
	 * considered for termination
	 */
	public static final int IDLE_INTERVAL = 240;

	/**
	 * The number of pieces a peer may report beneath which allowed fast pieces should be allocated
	 */
	public static final int ALLOWED_FAST_THRESHOLD = 10;

	/**
	 * The maximum simultaneous number of remote piece suggestions, including through both Allowed
	 * Fast and Suggest Piece messages that are honoured before additional suggestions are dropped
	 */
	public static final int MAXIMUM_SUGGESTED_PIECES = 64;


	/* Basic protocol messages */

	/**
	 * A "choke" message
	 */
	public static final byte MESSAGE_TYPE_CHOKE = 0;

	/**
	 * An "unchoke" message
	 */
	public static final byte MESSAGE_TYPE_UNCHOKE = 1;

	/**
	 * An "interested" message
	 */
	public static final byte MESSAGE_TYPE_INTERESTED = 2;

	/**
	 * A "not interested" message
	 */
	public static final byte MESSAGE_TYPE_NOT_INTERESTED = 3;

	/**
	 * A "have" message
	 */
	public static final byte MESSAGE_TYPE_HAVE = 4;

	/**
	 * A "bitfield" message
	 */
	public static final byte MESSAGE_TYPE_BITFIELD = 5;

	/**
	 * A "request" message
	 */
	public static final byte MESSAGE_TYPE_REQUEST = 6;

	/**
	 * A "piece" message
	 */
	public static final byte MESSAGE_TYPE_PIECE = 7;

	/**
	 * A "cancel" message
	 */
	public static final byte MESSAGE_TYPE_CANCEL = 8;


	/* DHT extension messages */

	/**
	 * A "DHT port" message
	 */
	public static final byte MESSAGE_TYPE_DHT_PORT = 9;


	/* Fast extension messages */

	/**
	 * A "suggest piece" message
	 */
	public static final byte MESSAGE_TYPE_SUGGEST_PIECE = 13;

	/**
	 * A "have all" message
	 */
	public static final byte MESSAGE_TYPE_HAVE_ALL = 14;

	/**
	 * A "have none" message
	 */
	public static final byte MESSAGE_TYPE_HAVE_NONE = 15;

	/**
	 * A "reject request" message
	 */
	public static final byte MESSAGE_TYPE_REJECT_REQUEST = 16;

	/**
	 * An "allowed fast" message
	 */
	public static final byte MESSAGE_TYPE_ALLOWED_FAST = 17;


	/* Extension protocol messages */

	/**
	 * An "extended" message
	 */
	public static final byte MESSAGE_TYPE_EXTENDED = 20;


	/* Extended messages. These are the identifiers we ask the remote peer to use */

	/**
	 * BEP 0009 Extension for Peers to Send Metadata Files
	 */
	public static final byte EXTENDED_MESSAGE_TYPE_PEER_METADATA = 1;

	/**
	 * BEP 0030 Merkle hash torrent extension
	 */
	public static final byte EXTENDED_MESSAGE_TYPE_MERKLE = 2;

	/**
	 * Elastic extension
	 */
	public static final byte EXTENDED_MESSAGE_TYPE_ELASTIC = 3;

	/**
	 * Resource extension
	 */
	public static final byte EXTENDED_MESSAGE_TYPE_RESOURCE = 4;

	/**
	 * The first available custom extended message
	 */
	public static final byte EXTENDED_MESSAGE_TYPE_CUSTOM = 5;


	/* Elastic extension message subtypes */

	/**
	 * Elastic signature message
	 */
	public static final byte ELASTIC_MESSAGE_TYPE_SIGNATURE = 0;

	/**
	 * Elastic piece message
	 */
	public static final byte ELASTIC_MESSAGE_TYPE_PIECE = 1;

	/**
	 * Elastic bitfield message
	 */
	public static final byte ELASTIC_MESSAGE_TYPE_BITFIELD = 2;


	/* Resource extension message subtypes */

	/**
	 * Resource directory message
	 */
	public static final byte RESOURCE_MESSAGE_TYPE_DIRECTORY = 0;

	/**
	 * Resource transfer message
	 */
	public static final byte RESOURCE_MESSAGE_TYPE_TRANSFER = 1;

	/**
	 * Resource subscribe message
	 */
	public static final byte RESOURCE_MESSAGE_TYPE_SUBSCRIBE = 2;


	/* Extension protocol identifiers */

	/**
	 * Peer exchange extension
	 */
	public static final String EXTENSION_PEER_EXCHANGE = "ut_pex";

	/**
	 * BEP 0009 metadata extension
	 */
	public static final String EXTENSION_PEER_METADATA = "ut_metadata";

	/**
	 * BEP 0028 tracker exchange extension
	 */
	public static final String EXTENSION_TRACKER_EXCHANGE = "lt_tex";

	/**
	 * BEP 0030 Merkle extension
	 */
	public static final String EXTENSION_MERKLE = "Tr_hashpiece";

	/**
	 * Elastic extension
	 */
	public static final String EXTENSION_ELASTIC = "bo_elastic";

	/**
	 * Resource extension
	 */
	public static final String EXTENSION_RESOURCE = "bo_resource";


	/**
	 * Not instantiable
	 */
	private PeerProtocolConstants() { }

}
