/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDecoder;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.bencode.BValue;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.ViewSignature;


/**
 * A state machine to incrementally parse a peer protocol stream.
 * <p>Complete, successful decoding of the header and subsequent messages through successive calls
 * to {@link #parseBytes(ReadableByteChannel)} will result in calls to methods of the supplied
 * {@link PeerProtocolConsumer}. If the stream is closed, or an error in the format of the stream is
 * detected, {@code IOException} will be thrown. 
 * <p>The content of the message data is not validated by the parser. Members of
 * {@code PeerProtocolConsumer} may throw IOException to indicate that a content error has been
 * detected and further parsing should be aborted. The exception will be passed back to the caller
 * of {@link #parseBytes(ReadableByteChannel)}.
 *
 * <p>Stream format errors that are detected by the parser include:
 * <ul>
 *   <li>An incorrect handshake header</li>
 *   <li>A known message of provably incorrect size</li>
 *   <li>A "bitfield", "have all" or "have none" message sent at any time but the beginning of the
 *       stream</li>
 *   <li>A Fast extension message sent when the Fast extension has not been negotiated</li>
 * </ul>
 *
 *<p>The parser assumes that all parsed extensions are automatically supported by the outgoing
 *stream.
 */
public class PeerProtocolParser {

	/**
	 * The bytes of the mandatory stream header
	 */
	private static final byte[] streamHeaderBytes = { 19, 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' };

	/**
	 * The parser's current state
	 */
	private static enum ParserState {

		/**
		 * Parser is reading the 20 byte header + 8 byte reserved data
		 */
		HEADER, 

		/**
		 * Parser is reading the 20 byte info hash
		 */
		INFO_HASH,

		/**
		 * Parser is reading the 20 byte peer ID
		 */
		PEER_ID,

		/**
		 * Parser is reading the 4 byte message length header
		 */
		MESSAGE_LENGTH,

		/**
		 * Parser is reading the content of a message
		 */
		MESSAGE,

		/**
		 * Parser has encountered an error
		 */
		ERROR

	}

	/**
	 * The {@code PeerProtocolConsumer} to call with complete messages
	 */
	private PeerProtocolConsumer consumer = null;

	/**
	 * The parser's current state
	 */
	private ParserState parserState = ParserState.HEADER;

	/**
	 * The message data currently being assembled from input
	 */
	private ByteBuffer messageData = ByteBuffer.allocate (streamHeaderBytes.length + 8);

	/**
	 * The number of remaining bytes that are expected of the current message
	 */
	private int messageBytesExpected = streamHeaderBytes.length + 8;

	/**
	 * {@code true} if the remote peer supports the Fast extension
	 */
	private boolean fastExtensionEnabled = false;

	/**
	 * {@code true} if the remote peer supports the extension protocol
	 */
	private boolean extensionProtocolEnabled = false;

	/**
	 * A map of extension strings and assigned extension message IDs that the remote peer is
	 * offering
	 */
	private Map<Integer,String> extensionIdentifiers = new TreeMap<Integer,String>();

	/**
	 * {@code false} until a "Bitfield", "Have None" or "Have All" message has been received
	 */
	private boolean bitfieldReceived = false;


	/**
	 * Resets the messageData buffer ready to receive a given number of bytes
	 *
	 * @param numBytes The number of bytes to expect
	 */
	private void resetMessageBuffer (int numBytes) {

		if (numBytes > this.messageData.capacity()) {
			this.messageData = ByteBuffer.allocate (numBytes);
		}
		this.messageData.clear ();
		this.messageData.limit (numBytes);
		this.messageBytesExpected = numBytes;

	}


	/**
	 * Reads a big endian integer from the message data
	 * @return The integer that was read
	 */
	private int readInt () {

		return    ((this.messageData.get() & 0xff) << 24)
				+ ((this.messageData.get() & 0xff) << 16)
				+ ((this.messageData.get() & 0xff) << 8)
				+  (this.messageData.get() & 0xff);

	}


	/**
	 * Reads a big endian long from the message data
	 * @return The long that was read
	 */
	private long readLong () {

		return    ((long)(this.messageData.get() & 0xff) << 56)
				+ ((long)(this.messageData.get() & 0xff) << 48)
				+ ((long)(this.messageData.get() & 0xff) << 40)
				+ ((long)(this.messageData.get() & 0xff) << 32)
				+ ((long)(this.messageData.get() & 0xff) << 24)
				+ ((this.messageData.get() & 0xff) << 16)
				+ ((this.messageData.get() & 0xff) << 8)
				+  (this.messageData.get() & 0xff);

	}


	/**
	 * Parses a complete extension handshake message
	 *
	 * @throws IOException if a parse error occurs
	 */
	private void parseExtensionHandshakeMessage() throws IOException {

		byte[] handshakeBytes = new byte[this.messageData.limit() - 2];
		this.messageData.get (handshakeBytes);

		// The content must be a valid BDictionary with no additional data
		InputStream extensionDataInputStream = new ByteArrayInputStream (handshakeBytes);
		BDictionary handshakeDictionary = new BDecoder(extensionDataInputStream).decodeDictionary();
		if (extensionDataInputStream.available() > 0) {
			throw new IOException ("Extra data after extension protocol handshake");
		}

		// Enabled and disabled extensions
		Set<String> extensionsEnabled = new TreeSet<String>();
		Set<String> extensionsDisabled = new TreeSet<String>();
		BValue extensionDictionaryValue = handshakeDictionary.get ("m");
		if (!(extensionDictionaryValue instanceof BDictionary)) {
			throw new IOException ("Invalid extension message ID definition");
		}
		BDictionary extensionDictionary = (BDictionary) extensionDictionaryValue;
		for (BBinary identifierBinary : extensionDictionary.keySet()) {
			BValue extensionIDValue = extensionDictionary.get (identifierBinary);
			if (!(extensionIDValue instanceof BInteger)) {
				throw new IOException ("Invalid extension message ID definition");
			}
			Integer messageID = ((BInteger) extensionIDValue).value().intValue();
			if ((messageID < 0) || (messageID > 255)) {
				throw new IOException ("Invalid extension message ID definition");
			}
			String identifier = identifierBinary.stringValue();
			if (messageID == 0) {
				// If it was enabled, disable it and report the change. If it was not enabled, we don't
				// care much
				if (this.extensionIdentifiers.values().remove (identifier)) {
					extensionsDisabled.add (identifier);
				}
			} else {
				// If it was disabled, enable it and report the change
				if (!this.extensionIdentifiers.containsValue (identifier)) {
					extensionsEnabled.add (identifier);
				}
				this.extensionIdentifiers.put (messageID, identifier);
			}
		}
		handshakeDictionary.remove ("m");
		this.consumer.extensionHandshakeMessage (extensionsEnabled, extensionsDisabled, handshakeDictionary);
	}


	/**
	 * Parses a complete Merkle piece message
	 *
	 * @throws IOException if a parse error occurs
	 */
	private void parseMerklePieceMessage() throws IOException {

		if (this.messageData.limit() < 14) {
			throw new IOException ("Invalid message size");
		}

		int pieceNumber = readInt();
		int offset = readInt();
		int hashChainLength = readInt();

		if ((hashChainLength < 0) || (hashChainLength > this.messageData.limit() - 14)) {
			throw new IOException ("Invalid hash chain");
		}

		byte[] encodedHashChain = null;
		byte[] hashChain = null;
		if (hashChainLength > 0) {
			encodedHashChain = new byte[hashChainLength];
			this.messageData.get (encodedHashChain);

			BValue hashChainValue = BDecoder.decode (encodedHashChain);
			if (!(hashChainValue instanceof BList)) {
				throw new IOException ("Invalid hash chain");
			}
			BList hashChainList = (BList)hashChainValue;

			hashChain = new byte[20 * hashChainList.size()];
			int position = 0;
			for (BValue hashChainListElement : hashChainList) {
				if (!(hashChainListElement instanceof BList)) {
					throw new IOException ("Invalid hash chain");
				}
				BList elementList = (BList)hashChainListElement;
				if ((elementList.size() != 2) || (!(elementList.get (0) instanceof BInteger)) || (!(elementList.get (1) instanceof BBinary))) {
					throw new IOException ("Invalid hash chain");
				}
				BBinary hashBinary = (BBinary)(elementList.get (1));
				System.arraycopy (hashBinary.value(), 0, hashChain, position, 20);
				position += 20;
			}

		}

		byte[] block = new byte[this.messageData.limit() - hashChainLength - 14];
		this.messageData.get (block);
		this.consumer.merklePieceMessage (new BlockDescriptor (pieceNumber, offset, block.length), hashChain, block);

	}


	/**
	 * Parses a complete Elastic message
	 *
	 * @throws IOException if a parse error occurs
	 */
	private void parseElasticMessage() throws IOException {

		if (this.messageData.limit() < 3) {
			throw new IOException ("Invalid message size");
		}

		int subMessageType = this.messageData.get();

		switch (subMessageType) {

			case PeerProtocolConstants.ELASTIC_MESSAGE_TYPE_SIGNATURE:
				if (this.messageData.limit() < 70) {
					throw new IOException ("Invalid message size");
				}

				long signatureViewLength = readLong();
				byte[] viewRootHash = new byte[20];
				this.messageData.get (viewRootHash);
				byte[] signature = new byte[40];
				this.messageData.get (signature);
				this.consumer.elasticSignatureMessage (new ViewSignature (
						signatureViewLength,
						ByteBuffer.wrap (viewRootHash),
						ByteBuffer.wrap (signature)
				));
				break;

			case PeerProtocolConstants.ELASTIC_MESSAGE_TYPE_PIECE:
				if (this.messageData.limit() < 15) {
					throw new IOException ("Invalid message size");
				}

				int pieceNumber = readInt();
				int offset = readInt();
				int hashCount = this.messageData.get() & 0xff;

				if (this.messageData.limit() < 12 + (hashCount == 0 ? 0 : 8 + (20 * hashCount))) {
					throw new IOException ("Invalid message size");
				}

				Long viewLength = null;
				byte[] hashChain = null;
				if (hashCount > 0) {
					viewLength = readLong();
					hashChain = new byte[20 * hashCount];
					this.messageData.get (hashChain);
				}

				byte[] block = new byte[this.messageData.limit() - this.messageData.position()];
				this.messageData.get (block);
				this.consumer.elasticPieceMessage (new BlockDescriptor (pieceNumber, offset, block.length), viewLength, hashChain, block);
				break;

			case PeerProtocolConstants.ELASTIC_MESSAGE_TYPE_BITFIELD:
				byte[] bitFieldBytes = new byte[this.messageData.limit() - 3];
				this.messageData.get (bitFieldBytes);
				this.consumer.elasticBitfieldMessage (bitFieldBytes);
				break;

			default:
				throw new IOException ("Invalid Elastic message type");

		}

	}


	/**
	 * Parses input bytes of peer protocol, assembling them internally until one or more complete
	 * messages have been received, and calling the relevant message methods on the registered
	 * {@code PeerProtocolConsumer} in sequence with the decoded message arguments
	 *
	 * @param inputChannel The input channel to read bytes from
	 * @return The number of bytes successfully parsed, possibly zero
	 * @throws IOException if the input channel is closed or a parse error occurred
	 */
	public int parseBytes (ReadableByteChannel inputChannel) throws IOException {

		int totalBytesRead = 0;

		while (this.parserState != ParserState.ERROR) {

			int bytesRead = inputChannel.read (this.messageData);
			totalBytesRead += bytesRead;

			if (bytesRead == 0) {
				return totalBytesRead;
			} else if (bytesRead == -1) {
				throw new ClosedChannelException();
			}

			this.messageBytesExpected -= bytesRead;

			// Check the header contents even before the expected number of bytes have been received - abort
			// early if someone is speaking the wrong protocol to us
			if (this.parserState == ParserState.HEADER) {
				for (int i = 0; (i < streamHeaderBytes.length) && (i < this.messageData.position()); i++) {
					if (this.messageData.get(i) != streamHeaderBytes[i]) {
						this.parserState = ParserState.ERROR;
						throw new IOException ("Invalid header");
					}
				}
			}

			if (this.messageBytesExpected == 0) {
				this.messageData.rewind();
				switch (this.parserState) {

					case HEADER:
						this.messageData.position (20);
						byte[] extensionBytes = new byte[8];
						this.messageData.get (extensionBytes);
						this.fastExtensionEnabled &= ((extensionBytes[7] & 0x04) != 0);
						this.extensionProtocolEnabled &= ((extensionBytes[5] & 0x10) != 0);
						this.consumer.handshakeBasicExtensions (this.fastExtensionEnabled, this.extensionProtocolEnabled);
						this.parserState = ParserState.INFO_HASH;
						resetMessageBuffer (20);
						continue;

					case INFO_HASH:
						byte[] infoHashBytes = new byte[20];
						this.messageData.get (infoHashBytes);
						this.consumer.handshakeInfoHash (new InfoHash (infoHashBytes));
						this.parserState = ParserState.PEER_ID;
						resetMessageBuffer (20);
						continue;

					case PEER_ID:
						byte[] peerIDBytes = new byte[20];
						this.messageData.get (peerIDBytes);
						this.consumer.handshakePeerID (new PeerID (peerIDBytes));
						this.parserState = ParserState.MESSAGE_LENGTH;
						resetMessageBuffer (4);
						continue;

					case MESSAGE_LENGTH:
						int length = readInt();
						if (length == 0) {
							this.consumer.keepAliveMessage();
							resetMessageBuffer (4);
						} else if (length > PeerProtocolConstants.MAXIMUM_MESSAGE_LENGTH) {
							this.parserState = ParserState.ERROR;
							throw new IOException ("Message too large");
						} else {
							this.parserState = ParserState.MESSAGE;
							resetMessageBuffer (length);
						}
						continue;

					case MESSAGE:
						byte messageType = this.messageData.get();

						switch (messageType) {

							case PeerProtocolConstants.MESSAGE_TYPE_CHOKE:
								if (this.messageData.limit() == 1) {
									this.consumer.chokeMessage (true);
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_UNCHOKE:
								if (this.messageData.limit() == 1) {
									this.consumer.chokeMessage (false);
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_INTERESTED:
								if (this.messageData.limit() == 1) {
									this.consumer.interestedMessage (true);
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_NOT_INTERESTED:
								if (this.messageData.limit() == 1) {
									this.consumer.interestedMessage (false);
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_HAVE:
								if (this.messageData.limit() == 5) {
									int havePieceIndex = readInt();
									this.consumer.haveMessage (havePieceIndex);
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_BITFIELD:
								if (!this.bitfieldReceived) {
									this.bitfieldReceived = true;
									byte[] bitFieldBytes = new byte[this.messageData.limit() - 1];
									this.messageData.get (bitFieldBytes);
									this.consumer.bitfieldMessage (bitFieldBytes);
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message sequence");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_REQUEST:
								if (this.messageData.limit() == 13) {
									int requestPieceIndex = readInt();
									int requestOffset = readInt();
									int requestLength = readInt();
									this.consumer.requestMessage (new BlockDescriptor (requestPieceIndex, requestOffset, requestLength));
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_PIECE:
								if (this.messageData.limit() >= 9) {
									int piecePieceIndex = readInt();
									int pieceOffset = readInt();
									byte[] pieceData = new byte[this.messageData.limit() - 9];
									this.messageData.get (pieceData);
									this.consumer.pieceMessage (new BlockDescriptor (piecePieceIndex, pieceOffset, pieceData.length), pieceData);
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_CANCEL:
								if (this.messageData.limit() == 13) {
									int cancelPieceIndex = readInt();
									int cancelOffset = readInt();
									int cancelLength = readInt();
									this.consumer.cancelMessage (new BlockDescriptor (cancelPieceIndex, cancelOffset, cancelLength));
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_SUGGEST_PIECE:
								if (this.fastExtensionEnabled && (this.messageData.limit() == 5)) {
									int suggestPieceIndex = readInt();
									this.consumer.suggestPieceMessage (suggestPieceIndex);
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size or Fast extension disabled");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_HAVE_ALL:
								if (this.fastExtensionEnabled && (this.messageData.limit() == 1) && !this.bitfieldReceived) {
									this.bitfieldReceived = true;
									this.consumer.haveAllMessage();
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size, sequence or Fast extension disabled");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_HAVE_NONE:
								if (this.fastExtensionEnabled && (this.messageData.limit() == 1) && !this.bitfieldReceived) {
									this.bitfieldReceived = true;
									this.consumer.haveNoneMessage();
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size, sequence or Fast extension disabled");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_REJECT_REQUEST:
								if (this.fastExtensionEnabled && (this.messageData.limit() == 13)) {
									int requestPieceIndex = readInt();
									int requestOffset = readInt();
									int requestLength = readInt();
									this.consumer.rejectRequestMessage (new BlockDescriptor (requestPieceIndex, requestOffset, requestLength));
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size or Fast extension disabled");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_ALLOWED_FAST:
								if (this.fastExtensionEnabled && (this.messageData.limit() == 5)) {
									int allowedFastIndex = readInt();
									this.consumer.allowedFastMessage (allowedFastIndex);
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Invalid message size or Fast extension disabled");
								}
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_EXTENDED:
								if (this.extensionProtocolEnabled && (this.messageData.limit() >= 2)) {
									int extensionID = this.messageData.get() & 0xff;
									if (extensionID == 0) {
										parseExtensionHandshakeMessage();
									} else {
										// Extension message
										String identifier = this.extensionIdentifiers.get (extensionID);
										if (PeerProtocolConstants.EXTENSION_MERKLE.equals (identifier)) {
											parseMerklePieceMessage();
										} else if (PeerProtocolConstants.EXTENSION_ELASTIC.equals (identifier)) {
											parseElasticMessage();
										} else {
											byte[] extensionData = new byte[this.messageData.limit() - 2];
											this.messageData.get (extensionData);
											this.consumer.extensionMessage (identifier, extensionData);
										}
									}
								} else {
									this.parserState = ParserState.ERROR;
									throw new IOException ("Extension protocol disabled");
								}
								break;

							default:
								byte[] unknownData = new byte[this.messageData.limit() - 1];
								this.messageData.get (unknownData);
								this.consumer.unknownMessage (messageType, unknownData);
								break;

						}

						this.parserState = ParserState.MESSAGE_LENGTH;
						resetMessageBuffer (4);
						continue;

					case ERROR:
						// Do nothing
						break;

				}

			}

		}

		return 0;

	}


	/**
	 * @param consumer The PeerProtocolConsumer to inform of received completed messages
	 * @param fastExtensionOffered If {@code true}, the Fast extension has been offered to the
	 *        remote peer
	 * @param extensionProtocolOffered If {@code true}, the extension protocol has been offered to
	 *        the remote peer
	 */
	public PeerProtocolParser (PeerProtocolConsumer consumer, boolean fastExtensionOffered, boolean extensionProtocolOffered) {

		this.consumer = consumer;
		this.fastExtensionEnabled = fastExtensionOffered;
		this.extensionProtocolEnabled = extensionProtocolOffered;

	}

}
