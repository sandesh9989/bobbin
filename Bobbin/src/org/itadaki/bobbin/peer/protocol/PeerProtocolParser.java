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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDecoder;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.bencode.BValue;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.PieceStyle;
import org.itadaki.bobbin.torrentdb.ResourceType;
import org.itadaki.bobbin.torrentdb.ViewSignature;


/**
 * A state machine to incrementally parse peer protocol messages. Successive calls to
 * {@link #parseBytes(ReadableByteChannel)} will result in calls to methods of the supplied
 * {@link PeerProtocolConsumer}. If the stream is closed, or an error in the format of the stream is
 * detected, {@code IOException} will be thrown. 
 * <p>The content of the message data is not validated by the parser. Members of
 * {@code PeerProtocolConsumer} may throw IOException to indicate that a content error has been
 * detected and further parsing should be aborted. The exception will be passed back to the caller
 * of {@link #parseBytes(ReadableByteChannel)}.
 *
 * <p>Stream format errors that are detected by the parser include:
 * <ul>
 *   <li>A known message of provably incorrect size</li>
 *   <li>A "bitfield", "have all" or "have none" message sent at any time but the beginning of the
 *       stream</li>
 *   <li>A Fast extension message sent when the Fast extension has not been negotiated</li>
 * </ul>
 */
public class PeerProtocolParser {

	/**
	 * The parser's current state
	 */
	private static enum ParserState {

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
	private ParserState parserState = ParserState.MESSAGE_LENGTH;

	/**
	 * The message data currently being assembled from input
	 */
	private ByteBuffer messageData = ByteBuffer.allocate (4);

	/**
	 * The number of remaining bytes that are expected of the current message
	 */
	private int messageBytesExpected = 4;

	/**
	 * {@code true} if the remote peer supports the Fast extension
	 */
	private boolean fastExtensionEnabled = false;

	/**
	 * {@code true} if the remote peer supports the extension protocol
	 */
	private boolean extensionProtocolEnabled = false;

	/**
	 * A map of extension message IDs and corresponding extension identifier strings offered by the
	 * remote peer
	 */
	private Map<Integer,String> extensionIdentifiers = new HashMap<Integer,String>();

	/**
	 * A map of resource IDs offered by the remote peer and the known resources they correspond to
	 */
	private Map<Integer,ResourceType> resourceIdentifiers = new HashMap<Integer,ResourceType>();

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
		this.messageData.clear();
		this.messageData.limit (numBytes);
		this.messageBytesExpected = numBytes;

	}


	/**
	 * Reads a big endian integer from the message data
	 *
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
	 *
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
	 * Parses the content of a Choke message at the current position
	 *
	 * @throws IOException
	 */
	private void parseChokeMessage() throws IOException {

		if (this.messageData.remaining() == 0) {
			this.consumer.chokeMessage (true);
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size");
		}

	}


	/**
	 * Parses the content of an Unchoke message at the current position
	 *
	 * @throws IOException
	 */
	private void parseUnchokeMessage() throws IOException {

		if (this.messageData.remaining() == 0) {
			this.consumer.chokeMessage (false);
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size");
		}

	}


	/**
	 * Parses the content of an Interested message at the current position
	 *
	 * @throws IOException
	 */
	private void parseInterestedMessage() throws IOException {

		if (this.messageData.remaining() == 0) {
			this.consumer.interestedMessage (true);
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size");
		}

	}


	/**
	 * Parses the content of a Not Interested message at the current position
	 *
	 * @throws IOException
	 */
	private void parseNotInterestedMessage() throws IOException {

		if (this.messageData.remaining() == 0) {
			this.consumer.interestedMessage (false);
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size");
		}

	}


	/**
	 * Parses the content of a Have message at the current position
	 *
	 * @param resource The resource to which the message applies
	 * @throws IOException
	 */
	private void parseHaveMessage (ResourceType resource) throws IOException {

		if (this.messageData.remaining() == 4) {
			int havePieceIndex = readInt();
			this.consumer.haveMessage (resource, havePieceIndex);
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size");
		}

	}


	/**
	 * Parses the content of a Bitfield message at the current position
	 *
	 * @param resource The resource to which the message applies
	 * @throws IOException
	 */
	private void parseBitfieldMessage (ResourceType resource) throws IOException {

		if (!this.bitfieldReceived) {
			this.bitfieldReceived = true;
			byte[] bitFieldBytes = new byte[this.messageData.remaining()];
			this.messageData.get (bitFieldBytes);
			this.consumer.bitfieldMessage (resource, bitFieldBytes);
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message sequence");
		}

	}


	/**
	 * Parses the content of a Request message at the current position
	 *
	 * @param resource The resource to which the message applies
	 * @throws IOException
	 */
	private void parseRequestMessage (ResourceType resource) throws IOException {

		if (this.messageData.remaining() == 12) {
			int requestPieceIndex = readInt();
			int requestOffset = readInt();
			int requestLength = readInt();
			this.consumer.requestMessage (resource, new BlockDescriptor (requestPieceIndex, requestOffset, requestLength));
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size");
		}

	}


	/**
	 * Parses the content of a Piece message at the current position
	 *
	 * @param resource The resource to which the message applies
	 * @throws IOException
	 */
	private void parsePieceMessage (ResourceType resource) throws IOException {

		if (this.messageData.remaining() >= 8) {
			int piecePieceIndex = readInt();
			int pieceOffset = readInt();
			ByteBuffer block = ByteBuffer.allocate (this.messageData.remaining());
			block.put (this.messageData);
			block.rewind();
			this.consumer.pieceMessage (PieceStyle.PLAIN, resource, new BlockDescriptor (piecePieceIndex, pieceOffset, block.remaining()), null, null, block);
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size");
		}

	}


	/**
	 * Parses the content of a Cancel message at the current position
	 *
	 * @param resource The resource to which the message applies
	 * @throws IOException
	 */
	private void parseCancelMessage (ResourceType resource) throws IOException {

		if (this.messageData.remaining() == 12) {
			int cancelPieceIndex = readInt();
			int cancelOffset = readInt();
			int cancelLength = readInt();
			this.consumer.cancelMessage (resource, new BlockDescriptor (cancelPieceIndex, cancelOffset, cancelLength));
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size");
		}

	}


	/**
	 * Parses the content of a Suggest Piece message at the current position
	 *
	 * @throws IOException
	 */
	private void parseSuggestPieceMessage() throws IOException {

		if (this.fastExtensionEnabled && (this.messageData.remaining() == 4)) {
			int suggestPieceIndex = readInt();
			this.consumer.suggestPieceMessage (suggestPieceIndex);
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size or Fast extension disabled");
		}

	}


	/**
	 * Parses the content of a Have All message at the current position
	 *
	 * @throws IOException
	 */
	private void parseHaveAllMessage() throws IOException {

		if (this.fastExtensionEnabled && (this.messageData.remaining() == 0) && !this.bitfieldReceived) {
			this.bitfieldReceived = true;
			this.consumer.haveAllMessage();
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size, sequence or Fast extension disabled");
		}

	}

	/**
	 * Parses the content of a Have None message at the current position
	 *
	 * @throws IOException
	 */
	private void parseHaveNoneMessage() throws IOException {

		if (this.fastExtensionEnabled && (this.messageData.remaining() == 0) && !this.bitfieldReceived) {
			this.bitfieldReceived = true;
			this.consumer.haveNoneMessage();
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size, sequence or Fast extension disabled");
		}

	}


	/**
	 * Parses the content of a Reject Request message at the current position
	 *
	 * @param resource The resource to which the message applies
	 * @throws IOException
	 */
	private void parseRejectRequestMessage (ResourceType resource) throws IOException {

		if (this.fastExtensionEnabled && (this.messageData.remaining() == 12)) {
			int requestPieceIndex = readInt();
			int requestOffset = readInt();
			int requestLength = readInt();
			this.consumer.rejectRequestMessage (resource, new BlockDescriptor (requestPieceIndex, requestOffset, requestLength));
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size or Fast extension disabled");
		}

	}


	/**
	 * Parses the content of an Allowed Fast message at the current position
	 *
	 * @throws IOException
	 */
	private void parseAllowedFastMessage() throws IOException {

		if (this.fastExtensionEnabled && (this.messageData.remaining() == 4)) {
			int allowedFastIndex = readInt();
			this.consumer.allowedFastMessage (allowedFastIndex);
		} else {
			this.parserState = ParserState.ERROR;
			throw new IOException ("Invalid message size or Fast extension disabled");
		}

	}


	/**
	 * Parses a complete extension handshake message at the current position
	 *
	 * @throws IOException if a parse error occurs
	 */
	private void parseExtensionHandshakeMessage() throws IOException {

		byte[] handshakeBytes = new byte[this.messageData.remaining()];
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
	 * Parses a complete Merkle piece message at the current position
	 *
	 * @throws IOException if a parse error occurs
	 */
	private void parseMerklePieceMessage() throws IOException {

		if (this.messageData.remaining() < 12) {
			throw new IOException ("Invalid message size");
		}

		int pieceNumber = readInt();
		int offset = readInt();
		int hashChainLength = readInt();

		if (
				   (hashChainLength < 0)
				|| (hashChainLength > this.messageData.remaining())
				|| ((offset == 0) && (hashChainLength == 0))
				|| ((offset != 0) && (hashChainLength != 0))
		)
		{
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

		ByteBuffer block = ByteBuffer.allocate (this.messageData.remaining());
		block.put (this.messageData);
		block.rewind();

		this.consumer.pieceMessage (PieceStyle.MERKLE, null, new BlockDescriptor (pieceNumber, offset, block.remaining()), null, ByteBuffer.wrap (hashChain), block);

	}


	/**
	 * Parses a complete Elastic message at the current position
	 *
	 * @throws IOException if a parse error occurs
	 */
	private void parseElasticMessage() throws IOException {

		if (this.messageData.remaining() < 1) {
			throw new IOException ("Invalid message size");
		}

		int subMessageType = this.messageData.get();

		switch (subMessageType) {

			case PeerProtocolConstants.ELASTIC_MESSAGE_TYPE_SIGNATURE:
				if (this.messageData.remaining() != 68) {
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
				if (this.messageData.remaining() < 9) {
					throw new IOException ("Invalid message size");
				}

				int pieceNumber = readInt();
				int offset = readInt();
				int hashCount = this.messageData.get() & 0xff;

				if (((offset == 0) && (hashCount == 0)) || ((offset != 0) && (hashCount != 0))) {
					throw new IOException ("Invalid hash count");
				}
				if (this.messageData.remaining() < (20 * hashCount)) {
					throw new IOException ("Invalid message size");
				}

				Long viewLength = null;
				byte[] hashChain = null;
				if (hashCount > 0) {
					viewLength = readLong();
					hashChain = new byte[20 * hashCount];
					this.messageData.get (hashChain);
				}

				ByteBuffer block = ByteBuffer.allocate (this.messageData.remaining());
				block.put (this.messageData);
				block.rewind();
				this.consumer.pieceMessage (PieceStyle.ELASTIC, null, new BlockDescriptor (pieceNumber, offset, block.remaining()), viewLength, ByteBuffer.wrap (hashChain),
						block);
				break;

			case PeerProtocolConstants.ELASTIC_MESSAGE_TYPE_BITFIELD:
				byte[] bitFieldBytes = new byte[this.messageData.remaining()];
				this.messageData.get (bitFieldBytes);
				this.consumer.elasticBitfieldMessage (bitFieldBytes);
				break;

			default:
				throw new IOException ("Invalid Elastic message type");

		}

	}


	/**
	 * Parses a complete Resource message at the current position
	 *
	 * @throws IOException if a parse error occurs
	 */
	private void parseResourceMessage() throws IOException {

		int subMessageType = this.messageData.get();

		switch (subMessageType) {

			case PeerProtocolConstants.RESOURCE_MESSAGE_TYPE_DIRECTORY:
				byte[] directoryBytes = new byte[this.messageData.remaining()];
				this.messageData.get (directoryBytes);

				// The content must be a valid BList
				InputStream directoryInputStream = new ByteArrayInputStream (directoryBytes);
				BList directory = new BDecoder(directoryInputStream).decodeList();
				if (directoryInputStream.available() > 0) {
					throw new IOException ("Extra data after resource directory");
				}
				List<ResourceType> resources = new ArrayList<ResourceType>();
				List<Integer> lengths = new ArrayList<Integer>();
				for (BValue resourceValue : directory) {
					if (!(resourceValue instanceof BList)) {
						throw new IOException ("Invalid resource");
					}
					BList resourceList = (BList)resourceValue;
					if (
							(resourceList.size() != 3)
							|| !(resourceList.get (0) instanceof BInteger)
							|| !(resourceList.get (1) instanceof BBinary)
							|| !(resourceList.get (2) instanceof BInteger)
					   )
					{
						throw new IOException ("Invalid resource");
					}
					int resourceID = ((BInteger)(resourceList.get (0))).value().intValue();
					String resourceName = ((BBinary)(resourceList.get (1))).stringValue();
					int resourceLength = ((BInteger)(resourceList.get (2))).value().intValue();
					if ((resourceID < 0) || (resourceID > 255) || (resourceLength < 0)) {
						throw new IOException ("Invalid resource");
					}
					ResourceType resource = ResourceType.forName (resourceName);
					if (resource != null) {
						this.resourceIdentifiers.put (resourceID, resource);
						resources.add (resource);
						lengths.add (resourceLength);
					}
				}
				this.consumer.resourceDirectoryMessage (resources, lengths);
				break;

			case PeerProtocolConstants.RESOURCE_MESSAGE_TYPE_TRANSFER:
				if (this.messageData.remaining() < 5) {
					throw new IOException ("Invalid message size");
				}
				ResourceType resource = this.resourceIdentifiers.get (this.messageData.get() & 0xff);
				if (resource == null) {
					throw new IOException ("Unknown resource");
				}
				int transferMessageType = this.messageData.get();
				switch (transferMessageType) {
					case PeerProtocolConstants.MESSAGE_TYPE_HAVE:
						parseHaveMessage (resource);
						break;
					case PeerProtocolConstants.MESSAGE_TYPE_BITFIELD:
						parseBitfieldMessage (resource);
						break;
					case PeerProtocolConstants.MESSAGE_TYPE_REQUEST:
						parseRequestMessage (resource);
						break;
					case PeerProtocolConstants.MESSAGE_TYPE_CANCEL:
						parseCancelMessage (resource);
						break;
					case PeerProtocolConstants.MESSAGE_TYPE_PIECE:
						parsePieceMessage (resource);
						break;
					case PeerProtocolConstants.MESSAGE_TYPE_REJECT_REQUEST:
						parseRejectRequestMessage (resource);
						break;
				}
				break;

			case PeerProtocolConstants.RESOURCE_MESSAGE_TYPE_SUBSCRIBE:
				if (this.messageData.remaining() != 1) {
					throw new IOException ("Invalid message size");
				}
				ResourceType subscriptionResource = this.resourceIdentifiers.get (this.messageData.get() & 0xff);
				if (subscriptionResource == null) {
					throw new IOException ("Unknown resource");
				}
				this.consumer.resourceSubscribeMessage (subscriptionResource);
				break;

			default:
				throw new IOException ("Invalid resource message");

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

			if (this.messageBytesExpected == 0) {
				this.messageData.rewind();
				switch (this.parserState) {

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
								parseChokeMessage();
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_UNCHOKE:
								parseUnchokeMessage();
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_INTERESTED:
								parseInterestedMessage();
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_NOT_INTERESTED:
								parseNotInterestedMessage();
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_HAVE:
								parseHaveMessage (null);
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_BITFIELD:
								parseBitfieldMessage (null);
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_REQUEST:
								parseRequestMessage (null);
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_PIECE:
								parsePieceMessage (null);
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_CANCEL:
								parseCancelMessage (null);
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_SUGGEST_PIECE:
								parseSuggestPieceMessage();
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_HAVE_ALL:
								parseHaveAllMessage();
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_HAVE_NONE:
								parseHaveNoneMessage();
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_REJECT_REQUEST:
								parseRejectRequestMessage (null);
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_ALLOWED_FAST:
								parseAllowedFastMessage();
								break;

							case PeerProtocolConstants.MESSAGE_TYPE_EXTENDED:
								if (this.extensionProtocolEnabled && (this.messageData.remaining() >= 1)) {
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
										} else if (PeerProtocolConstants.EXTENSION_RESOURCE.equals (identifier)) {
											parseResourceMessage();
										} else {
											byte[] extensionData = new byte[this.messageData.remaining()];
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
								byte[] unknownData = new byte[this.messageData.remaining()];
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
	 * @param fastExtensionEnabled If {@code true}, the Fast extension has been negotiated
	 * @param extensionProtocolEnabled If {@code true}, the extension protocol has negotiated
	 */
	public PeerProtocolParser (PeerProtocolConsumer consumer, boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {

		this.consumer = consumer;
		this.fastExtensionEnabled = fastExtensionEnabled;
		this.extensionProtocolEnabled = extensionProtocolEnabled;

	}


}
