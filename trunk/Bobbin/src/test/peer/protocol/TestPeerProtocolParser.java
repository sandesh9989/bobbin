/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer.protocol;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BEncoder;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.InvalidEncodingException;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConstants;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer;
import org.itadaki.bobbin.peer.protocol.PeerProtocolParser;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.CharsetUtil;
import org.junit.Test;

import test.Util;


/**
 * Tests PeerProtocolParser
 */
public class TestPeerProtocolParser {

	// Tests for handshake parsing

	/**
	 * Tests that PeerProtocolConsumer.handshakeBasicExtensions() is called given just enough bytes
	 * @throws IOException
	 */
	@Test
	public void testBasicExtensionsBlank() throws IOException {

		final Boolean[] parsedFastExtension = new Boolean[1];
		
		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				parsedFastExtension[0] = fastExtensionEnabled;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, new InfoHash (new byte[20]), new PeerID()).array();
		byte[] shortHandshakeBytes = new byte [handshakeBytes.length - 40];
		System.arraycopy (handshakeBytes, 0, shortHandshakeBytes, 0, shortHandshakeBytes.length);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (shortHandshakeBytes));
		assertFalse (parsedFastExtension[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.handshakeBasicExtensions() is called with the fast extension
	 * enabled
	 * @throws IOException
	 */
	@Test
	public void testBasicExtensionsFast() throws IOException {

		final Boolean[] parsedFastExtension = new Boolean[1];
		
		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				parsedFastExtension[0] = fastExtensionEnabled;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, false, new InfoHash (new byte[20]), new PeerID()).array();
		byte[] shortHandshakeBytes = new byte [handshakeBytes.length - 40];
		System.arraycopy (handshakeBytes, 0, shortHandshakeBytes, 0, shortHandshakeBytes.length);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (shortHandshakeBytes));
		assertTrue (parsedFastExtension[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.handshakeInfoHash() is called given just enough bytes
	 * @throws IOException
	 */
	@Test
	public void testInfoHash() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		
		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, new PeerID()).array();
		byte[] shortHandshakeBytes = new byte [handshakeBytes.length - 20];
		System.arraycopy (handshakeBytes, 0, shortHandshakeBytes, 0, shortHandshakeBytes.length);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (shortHandshakeBytes));
		assertEquals (infoHash, parsedInfoHash[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.handshakePeerID() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testPeerID() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		
		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);

	}


	// Protocol messages

	/**
	 * Tests that PeerProtocolConsumer.keepAliveMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testKeepAlive() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final boolean[] keepAliveReceived = new boolean[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void keepAliveMessage() {
				assertEquals (3, this.callCount++);
				keepAliveReceived[0] = true;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		byte[] keepaliveBytes = PeerProtocolBuilder.keepaliveMessage().array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (keepaliveBytes));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertTrue (keepAliveReceived[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.chokeMessage(true) is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testChoke() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final Boolean[] parsedChoke = new Boolean[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void chokeMessage (boolean choked) {
				assertEquals (4, this.callCount++);
				parsedChoke[0] = new Boolean (choked);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.chokeMessage().array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertTrue (parsedChoke[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.chokeMessage(false) is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testUnchoke() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final Boolean[] parsedChoke = new Boolean[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void chokeMessage (boolean choked) {
				assertEquals (4, this.callCount++);
				parsedChoke[0] = new Boolean (choked);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.unchokeMessage().array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertFalse (parsedChoke[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.interestedMessage(true) is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testInterested() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final Boolean[] parsedInterested = new Boolean[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void interestedMessage (boolean choked) {
				assertEquals (4, this.callCount++);
				parsedInterested[0] = new Boolean (choked);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.interestedMessage().array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertTrue (parsedInterested[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.interestedMessage(false) is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testNotInterested() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final Boolean[] parsedInterested = new Boolean[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void interestedMessage (boolean choked) {
				assertEquals (4, this.callCount++);
				parsedInterested[0] = new Boolean (choked);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.notInterestedMessage().array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertFalse (parsedInterested[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.haveMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testHave() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		int pieceIndex = 1234;

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final int[] parsedPieceIndex = new int[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void haveMessage (int pieceIndex) {
				assertEquals (4, this.callCount++);
				parsedPieceIndex[0] = pieceIndex;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveMessage (pieceIndex).array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (pieceIndex, parsedPieceIndex[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.bitFieldMessage() is called
	 * @throws IOException
	 */
	@Test
	public void testBitfield() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BitField bitField = new BitField (40);
		bitField.set (0);
		bitField.set (9);
		bitField.set (18);
		bitField.set (27);
		bitField.set (36);

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final byte[][] parsedBitField = new byte[1][];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void bitfieldMessage (byte[] bitField) {
				assertEquals (3, this.callCount++);
				parsedBitField[0] = bitField;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		byte[] bitFieldBytes = PeerProtocolBuilder.bitfieldMessage (bitField).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (bitFieldBytes));
		assertEquals (bitField, new BitField (parsedBitField[0], 40));

	}


	/**
	 * Tests that PeerProtocolConsumer.requestMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testRequest() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		int pieceIndex = 1234;
		int offset = 5678;
		int length = 9012;

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final BlockDescriptor[] parsedBlockRequest = new BlockDescriptor[1];


		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void requestMessage (BlockDescriptor blockDescriptor) {
				assertEquals (4, this.callCount++);
				parsedBlockRequest[0] = blockDescriptor;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.requestMessage (new BlockDescriptor (pieceIndex, offset, length)).array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (pieceIndex, parsedBlockRequest[0].getPieceNumber());
		assertEquals (offset, parsedBlockRequest[0].getOffset());
		assertEquals (length, parsedBlockRequest[0].getLength());

	}


	/**
	 * Tests that PeerProtocolConsumer.pieceMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testPiece() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		int pieceIndex = 1234;
		int offset = 5678;
		byte[] data = new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1 };

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final BlockDescriptor[] parsedBlockRequest = new BlockDescriptor[1];
		final byte[][] parsedData = new byte[1][];


		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void pieceMessage (BlockDescriptor blockDescriptor, byte[] data) {
				assertEquals (4, this.callCount++);
				parsedBlockRequest[0] = blockDescriptor;
				parsedData[0] = data;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		ByteBuffer[] pieceBuffers = PeerProtocolBuilder.pieceMessage (new BlockDescriptor (pieceIndex, offset, data.length), ByteBuffer.wrap (data));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (pieceBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (pieceBuffers[1].array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (pieceIndex, parsedBlockRequest[0].getPieceNumber());
		assertEquals (offset, parsedBlockRequest[0].getOffset());
		assertEquals (data.length, parsedBlockRequest[0].getLength());
		assertArrayEquals (data, parsedData[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.requestMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testCancel() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		int pieceIndex = 1234;
		int offset = 5678;
		int length = 9012;

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final BlockDescriptor[] parsedBlockRequest = new BlockDescriptor[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void cancelMessage (BlockDescriptor blockDescriptor) {
				assertEquals (4, this.callCount++);
				parsedBlockRequest[0] = blockDescriptor;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.cancelMessage (new BlockDescriptor (pieceIndex, offset, length)).array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (pieceIndex, parsedBlockRequest[0].getPieceNumber());
		assertEquals (offset, parsedBlockRequest[0].getOffset());
		assertEquals (length, parsedBlockRequest[0].getLength());

	}


	/**
	 * Tests that PeerProtocolConsumer.suggestPieceMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testSuggestPiece() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		int pieceIndex = 1234;

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final int[] parsedPieceIndex = new int[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void suggestPieceMessage (int pieceIndex) {
				assertEquals (4, this.callCount++);
				parsedPieceIndex[0] = pieceIndex;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.suggestPieceMessage (pieceIndex).array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (pieceIndex, parsedPieceIndex[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.haveAllMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testHaveAll() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.haveNoneMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testHaveNone() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.rejectRequestMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testRejectRequest() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		int pieceIndex = 1234;
		int offset = 5678;
		int length = 9012;

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final BlockDescriptor[] parsedBlockRequest = new BlockDescriptor[1];


		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void rejectRequestMessage (BlockDescriptor blockDescriptor) {
				assertEquals (4, this.callCount++);
				parsedBlockRequest[0] = blockDescriptor;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.rejectRequestMessage (new BlockDescriptor (pieceIndex, offset, length)).array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (pieceIndex, parsedBlockRequest[0].getPieceNumber());
		assertEquals (offset, parsedBlockRequest[0].getOffset());
		assertEquals (length, parsedBlockRequest[0].getLength());

	}


	/**
	 * Tests that PeerProtocolConsumer.allowedFastMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testAllowedFast() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		int pieceIndex = 1234;

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final int[] parsedPieceIndex = new int[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void allowedFastMessage (int pieceIndex) {
				assertEquals (4, this.callCount++);
				parsedPieceIndex[0] = pieceIndex;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.allowedFastMessage (pieceIndex).array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (pieceIndex, parsedPieceIndex[0]);

	}


	/**
	 * Tests an extension handshake that adds an extension
	 * @throws Exception 
	 */
	@Test
	public void testExtensionHandshakeAdd() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		String version = "Foo 1.0";
		Integer queueDepth = 123;

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final String[] receivedVersion = new String[1];
		final Integer[] receivedRequestQueueDepth = new Integer[1];
		final Set<String> receivedExtensionsEnabled = new TreeSet<String>();
		final Set<String> receivedExtensionsDisabled = new TreeSet<String>();

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra)
					throws IOException
			{
				assertEquals (4, this.callCount++);
				receivedVersion[0] = extra.getString ("v");
				receivedRequestQueueDepth[0] = ((BInteger)extra.get ("reqq")).value().intValue();
				receivedExtensionsEnabled.addAll (extensionsEnabled);
				receivedExtensionsDisabled.addAll (extensionsDisabled);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put ("bl_ah", 1);
		BDictionary extra = new BDictionary();
		extra.put ("v", "Foo 1.0");
		extra.put ("reqq", 123);
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, extra);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[1].array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (version, receivedVersion[0]);
		assertEquals (queueDepth, receivedRequestQueueDepth[0]);
		assertEquals (1, receivedExtensionsEnabled.size());
		assertEquals ("bl_ah", receivedExtensionsEnabled.iterator().next());
		assertEquals (0, receivedExtensionsDisabled.size());

	}


	/**
	 * Tests an extension handshake that subtracts an extension
	 * @throws Exception 
	 */
	@Test
	public void testExtensionHandshakeRemove() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final Set<String> receivedExtensionsEnabled = new TreeSet<String>();
		final Set<String> receivedExtensionsDisabled = new TreeSet<String>();

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra)
					throws IOException
			{
				if (this.callCount++ == 5) {
					receivedExtensionsEnabled.addAll (extensionsEnabled);
					receivedExtensionsDisabled.addAll (extensionsDisabled);
				}
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put ("bl_ah", 1);
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[1].array()));
		extensions.put ("bl_ah", 0);
		ByteBuffer[] extensionHandshakeBuffers2 = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers2[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers2[1].array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (0, receivedExtensionsEnabled.size());
		assertEquals (1, receivedExtensionsDisabled.size());
		assertEquals ("bl_ah", receivedExtensionsDisabled.iterator().next());

	}


	/**
	 * Tests an extension handshake that subtracts an extension not previously enabled
	 * @throws Exception
	 */
	@Test
	public void testExtensionHandshakeRemoveNonexistent() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final String[] receivedVersion = new String[1];
		final Integer[] receivedRequestQueueDepth = new Integer[1];
		final Set<String> receivedExtensionsEnabled = new TreeSet<String>();
		final Set<String> receivedExtensionsDisabled = new TreeSet<String>();

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra)
					throws IOException
			{
				if (this.callCount++ == 5) {
					receivedExtensionsEnabled.addAll (extensionsEnabled);
					receivedExtensionsDisabled.addAll (extensionsDisabled);
				}
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put ("bl_ah", 1);
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[1].array()));
		extensions.put ("wi_bble", 0);
		ByteBuffer[] extensionHandshakeBuffers2 = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers2[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers2[1].array()));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertNull (receivedVersion[0]);
		assertNull (receivedRequestQueueDepth[0]);
		assertEquals (0, receivedExtensionsEnabled.size());
		assertEquals (0, receivedExtensionsDisabled.size());

	}


	// TODO Test - defense against a recursive list/dictionary attack


	/**
	 * Tests an extension message
	 * @throws IOException
	 */
	@Test
	public void testExtensionMessage() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		final String[] receivedIdentifier = new String[1];
		final byte[][] receivedData = new byte[1][];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra)
					throws IOException
			{
				assertEquals (4, this.callCount++);
			}
			@Override
			public void extensionMessage (String identifier, byte[] data) throws IOException {
				assertEquals (5, this.callCount++);
				receivedIdentifier[0] = identifier;
				receivedData[0] = data;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put ("bl_ah", 42);
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[1].array()));

		ByteBuffer[] extensionHandshakeBuffers2 = PeerProtocolBuilder.extensionMessage (42, ByteBuffer.wrap (new byte[] { 1, 2, 3, 4 }));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers2[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers2[1].array()));

		assertEquals ("bl_ah", receivedIdentifier[0]);
		assertArrayEquals (new byte[] { 1, 2, 3, 4}, receivedData[0]);

	}


	/**
	 * Tests a Merkle piece message
	 * @throws IOException
	 */
	@Test
	public void testMerklePieceMessage() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		BlockDescriptor expectedDescriptor = new BlockDescriptor (1, 0, 4);
		ByteBuffer expectedHashChain = ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 });
		ByteBuffer expectedBlock = ByteBuffer.wrap (new byte[] { 50, 51, 52, 53 });

		final BlockDescriptor[] receivedDescriptor = new BlockDescriptor[1];
		final byte[][] receivedHashChain = new byte[1][];
		final byte[][] receivedBlock = new byte[1][];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra)
					throws IOException
			{
				assertEquals (4, this.callCount++);
			}
			@Override
			public void merklePieceMessage (BlockDescriptor descriptor, byte[] hashChain, byte[] block) throws IOException {
				assertEquals (5, this.callCount++);
				receivedDescriptor[0] = descriptor;
				receivedHashChain[0] = hashChain;
				receivedBlock[0] = block;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_MERKLE, 1);
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[1].array()));

		ByteBuffer[] buffers = PeerProtocolBuilder.merklePieceMessage (
				expectedDescriptor,
				expectedHashChain.duplicate(),
				expectedBlock
		);

		for (ByteBuffer buffer : buffers) {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (buffer.array()));
		}

		assertEquals (expectedDescriptor, receivedDescriptor[0]);
		assertEquals (expectedHashChain, ByteBuffer.wrap (receivedHashChain[0]));
		assertEquals (expectedBlock, ByteBuffer.wrap (receivedBlock[0]));

	}


	/**
	 * Tests an elastic signature message
	 * @throws IOException
	 */
	@Test
	public void testElasticSignatureMessage() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		long expectedViewLength = 0x7FFFFEFDFCFBFAF9L;
		ByteBuffer expectedViewRootHash = ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 });
		ByteBuffer expectedSignature = ByteBuffer.wrap (new byte[] {
				50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
				70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89
		});
		ViewSignature expectedViewSignature = new ViewSignature (expectedViewLength, expectedViewRootHash, expectedSignature);


		final ViewSignature[] receivedViewSignature = new ViewSignature[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra)
					throws IOException
			{
				assertEquals (4, this.callCount++);
			}
			@Override
			public void elasticSignatureMessage (ViewSignature viewSignature) throws IOException {
				assertEquals (5, this.callCount++);
				receivedViewSignature[0] = viewSignature;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, 2);
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[1].array()));

		ByteBuffer[] buffers = PeerProtocolBuilder.elasticSignatureMessage (new ViewSignature (
				expectedViewLength,
				expectedViewRootHash,
				expectedSignature
		));

		for (ByteBuffer buffer : buffers) {
			byte[] bufferCopy = new byte[buffer.remaining()];
			buffer.get (bufferCopy);
			parser.parseBytes (Util.infiniteReadableByteChannelFor (bufferCopy));
		}

		assertEquals (expectedViewSignature, receivedViewSignature[0]);

	}


	/**
	 * Tests an elastic piece message
	 * @throws IOException
	 */
	@Test
	public void testElasticPieceMessage() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		long expectedViewLength = 0x7FFFFEFDFCFBFAF9L;
		BlockDescriptor expectedDescriptor = new BlockDescriptor (1, 0, 4);
		ByteBuffer expectedHashChain = ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 });
		ByteBuffer expectedBlock = ByteBuffer.wrap (new byte[] { 50, 51, 52, 53 });

		final long[] receivedViewLength = new long[1];
		final BlockDescriptor[] receivedDescriptor = new BlockDescriptor[1];
		final byte[][] receivedHashChain = new byte[1][];
		final byte[][] receivedBlock = new byte[1][];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra)
					throws IOException
			{
				assertEquals (4, this.callCount++);
			}
			@Override
			public void elasticPieceMessage (BlockDescriptor descriptor, Long viewLength, byte[] hashChain, byte[] block) throws IOException {
				assertEquals (5, this.callCount++);
				receivedViewLength[0] = viewLength;
				receivedDescriptor[0] = descriptor;
				receivedHashChain[0] = hashChain;
				receivedBlock[0] = block;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, 2);
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[1].array()));

		ByteBuffer[] buffers = PeerProtocolBuilder.elasticPieceMessage (
				expectedDescriptor,
				expectedViewLength,
				expectedHashChain,
				expectedBlock
		);

		for (ByteBuffer buffer : buffers) {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (buffer.array()));
		}

		assertEquals (expectedViewLength, receivedViewLength[0]);
		assertEquals (expectedDescriptor, receivedDescriptor[0]);
		assertEquals (expectedHashChain, ByteBuffer.wrap (receivedHashChain[0]));
		assertEquals (expectedBlock, ByteBuffer.wrap (receivedBlock[0]));

	}


	/**
	 * Tests an elastic bitfield message
	 * @throws IOException
	 */
	@Test
	public void testElasticBitfieldMessage() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		byte[] expectedBitfieldBytes = new byte[] { (byte)0xff, 0x00, (byte)0xee, (byte)0xf0 };

		final byte[][] receivedBitField = new byte[1][];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void extensionHandshakeMessage (Set<String> extensionsEnabled, Set<String> extensionsDisabled, BDictionary extra)
					throws IOException
			{
				assertEquals (4, this.callCount++);
			}
			@Override
			public void elasticBitfieldMessage(byte[] bitField) throws IOException {
				assertEquals (5, this.callCount++);
				receivedBitField[0] = bitField;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, 2);
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[1].array()));

		ByteBuffer[] buffers = PeerProtocolBuilder.elasticBitfieldMessage (
				new BitField (expectedBitfieldBytes, 28)
		);

		for (ByteBuffer buffer : buffers) {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (buffer.array()));
		}

		assertArrayEquals (expectedBitfieldBytes, receivedBitField[0]);

	}


	/**
	 * Tests that PeerProtocolConsumer.unknownMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testUnknown() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] unknownData = new byte[] { 0, 0, 0, 5, 99, 1, 2, 3, 4 };
		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final int[] parsedMessageID = new int[1];
		final byte[][] parsedData = new byte[1][];


		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void unknownMessage (int messageID, byte[] data) {
				assertEquals (4, this.callCount++);
				parsedMessageID[0] = messageID;
				parsedData[0] = data;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (unknownData));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (99, parsedMessageID[0]);
		assertArrayEquals (new byte[] { 1, 2, 3, 4 }, parsedData[0]);

	}


	// Protocol errors

	/**
	 * Tests that an exception is thrown on a bad header
	 * @throws IOException
	 */
	@Test
	public void testErrorBadHeader() throws IOException {

		PeerProtocolConsumer consumer = new MockProtocolConsumer() { };

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.chokeMessage().array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid header", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a choke message of the wrong length
	 * @throws IOException
	 */
	@Test
	public void testErrorChokeWrongLength() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_CHOKE, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for an unchoke message of the wrong length
	 * @throws IOException
	 */
	@Test
	public void testErrorUnchokeWrongLength() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_UNCHOKE, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for an interested message of the wrong length
	 * @throws IOException
	 */
	@Test
	public void testErrorInterestedWrongLength() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_INTERESTED, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a not interested message of the wrong length
	 * @throws IOException
	 */
	@Test
	public void testErrorNotInterestedWrongLength() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_NOT_INTERESTED, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a have message that is too short
	 * @throws IOException
	 */
	@Test
	public void testErrorHaveTooShort() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_HAVE };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a have message that is too long
	 * @throws IOException
	 */
	@Test
	public void testErrorHaveTooLong() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 6, PeerProtocolConstants.MESSAGE_TYPE_HAVE, 0, 0, 0, 0, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown on an out of sequence bitfield message
	 * @throws IOException
	 */
	@Test
	public void testErrorBitfieldOutOfSequence() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BitField bitField = new BitField (40);

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void keepAliveMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		byte[] bitFieldBytes = PeerProtocolBuilder.bitfieldMessage (bitField).array();
		byte[] keepaliveBytes = PeerProtocolBuilder.keepaliveMessage().array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (keepaliveBytes));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (bitFieldBytes));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message sequence", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a request message that is too short
	 * @throws IOException
	 */
	@Test
	public void testErrorRequestTooShort() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_REQUEST };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a request message that is too long
	 * @throws IOException
	 */
	@Test
	public void testErrorRequestTooLong() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 14, PeerProtocolConstants.MESSAGE_TYPE_REQUEST, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a piece message that is too short
	 * @throws IOException
	 */
	@Test
	public void testErrorPieceTooShort() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 8, PeerProtocolConstants.MESSAGE_TYPE_PIECE, 0, 0, 0, 0, 0, 0, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a cancel message that is too short
	 * @throws IOException
	 */
	@Test
	public void testErrorCancelTooShort() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_CANCEL };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a cancel message that is too long
	 * @throws IOException
	 */
	@Test
	public void testErrorCancelTooLong() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 14, PeerProtocolConstants.MESSAGE_TYPE_CANCEL, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown only once and no further data processed
	 * @throws IOException
	 */
	@Test
	public void testErrorBehaviour1() throws IOException {

		PeerProtocolConsumer consumer = new MockProtocolConsumer() { };

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.chokeMessage().array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid header", e.getMessage());
		}

		assertTrue (exceptionThrown);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.chokeMessage().array()));

	}


	/**
	 * Tests that an exception is thrown only once and no further data processed
	 * @throws IOException
	 */
	@Test
	public void testErrorBehaviour2() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BitField bitField = new BitField (40);

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			
			@Override
			public void keepAliveMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		byte[] keepaliveBytes = PeerProtocolBuilder.keepaliveMessage().array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (keepaliveBytes));
		byte[] bitFieldBytes = PeerProtocolBuilder.bitfieldMessage (bitField).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (bitFieldBytes));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message sequence", e.getMessage());
		}

		assertTrue (exceptionThrown);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.chokeMessage().array()));

	}


	/**
	 * Tests that PeerProtocolConsumer.protocolError() is not called for a message that is exactly the maximum length
	 * @throws IOException
	 */
	@Test
	public void testErrorMessageExactlyMaximumLength() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] messageData = new byte[4 + 9 + 131072];
		messageData[1] = 2;
		messageData[3] = 9;
		messageData[4] = PeerProtocolConstants.MESSAGE_TYPE_PIECE;

		final boolean[] pieceMessageCalled = new boolean[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
			@Override
			public void pieceMessage (BlockDescriptor blockDescriptor, byte[] data) {
				assertEquals (4, this.callCount++);
				pieceMessageCalled[0] = true;
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageData));

		assertTrue (pieceMessageCalled[0]);

	}


	/**
	 * Tests that an exception is thrown for a message that is too long
	 * @throws IOException
	 */
	@Test
	public void testErrorMessageTooLong() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[4 + 9 + 131072 + 1];
		erroneousData[1] = 2;
		erroneousData[3] = 10;
		erroneousData[4] = PeerProtocolConstants.MESSAGE_TYPE_PIECE;

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Message too large", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a non Fast protocol enabled stream when a suggest piece message
	 * is received
	 * @throws IOException
	 */
	@Test
	public void testErrorNonFastSuggestPiece() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.suggestPieceMessage(0).array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a non Fast protocol enabled stream when a have all message
	 * is received
	 * @throws IOException
	 */
	@Test
	public void testErrorNonFastHaveAll() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a non Fast protocol enabled stream when a have none message
	 * is received
	 * @throws IOException
	 */
	@Test
	public void testErrorNonFastHaveNone() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a non Fast protocol enabled stream when a reject request message
	 * is received
	 * @throws IOException
	 */
	@Test
	public void testErrorNonFastRejectRequest() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.rejectRequestMessage(new BlockDescriptor (0,1,2)).array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a non Fast protocol enabled stream when an allowed fast message
	 * is received
	 * @throws IOException
	 */
	@Test
	public void testErrorNonFastAllowedFast() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.allowedFastMessage(0).array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with an invalid first message
	 * @throws IOException
	 */
	@Test
	public void testErrorFastFirstMessage1() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.keepaliveMessage().array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message sequence", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with an invalid first message
	 * @throws IOException
	 */
	@Test
	public void testErrorFastFirstMessage2() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.unchokeMessage().array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message sequence", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a have all message out of sequence
	 * @throws IOException
	 */
	@Test
	public void testErrorFastHaveAllOutOfSequence() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a have none message out of sequence
	 * @throws IOException
	 */
	@Test
	public void testErrorFastHaveNoneOutOfSequence() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a suggest piece message that is
	 * too short
	 * @throws IOException
	 */
	@Test
	public void testErrorFastSuggestPieceTooShort() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_SUGGEST_PIECE };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a suggest piece message that is
	 * too long
	 * @throws IOException
	 */
	@Test
	public void testErrorFastSuggestPieceTooLong() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 6, PeerProtocolConstants.MESSAGE_TYPE_SUGGEST_PIECE, 0, 0, 0, 0, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a have all message that is
	 * too long
	 * @throws IOException
	 */
	@Test
	public void testErrorFastHaveAllTooLong() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_HAVE_ALL, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a have none message that is
	 * too short
	 * @throws IOException
	 */
	@Test
	public void testErrorFastHaveNoneTooLong() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_HAVE_NONE, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a reject request message that is
	 * too short
	 * @throws IOException
	 */
	@Test
	public void testErrorFastRejectRequestTooShort() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_REJECT_REQUEST };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a reject request message that is
	 * too long
	 * @throws IOException
	 */
	@Test
	public void testErrorFastRejectRequestTooLong() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 14, PeerProtocolConstants.MESSAGE_TYPE_REJECT_REQUEST, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with an allowed fast message that is
	 * too short
	 * @throws IOException
	 */
	@Test
	public void testErrorFastAllowedFastTooShort() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_ALLOWED_FAST };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with an allowed fast message that is
	 * too long
	 * @throws IOException
	 */
	@Test
	public void testErrorFastAllowedFastTooLong() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		byte[] erroneousData = new byte[] { 0, 0, 0, 6, PeerProtocolConstants.MESSAGE_TYPE_ALLOWED_FAST, 0, 0, 0, 0, 0 };

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveAllMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.handshake (true, false, infoHash, peerID).array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage().array()));

		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown on receiving an extension message when the extension
	 * protocol is disabled
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionDisabled() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put ("bl_ah", 1);
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[1].array()));

	}


	/**
	 * Tests an extension handshake with an undecodable dictionary
	 * @throws Exception
	 */
	@Test(expected=InvalidEncodingException.class)
	public void testErrorExtensionHandshakeInvalid() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 3 , 20, 0, 1 }));

	}


	/**
	 * Tests an extension handshake with a valid dictionary followed by extra data
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionHandshakeTooLong() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put ("bl_ah", 1);
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		extensionHandshakeBuffers[0].array()[3]++;
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[0].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers[1].array()));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0 }));

	}


	/**
	 * Tests an extension handshake with an identifier dictionary of the wrong type
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionHandshakeInvalidIdentifierDictionaryType() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		BDictionary dictionary = new BDictionary();
		dictionary.put ("m", "Not a dictionary");
		byte[] messageBytes = BEncoder.encode (dictionary);
		byte[] header = new byte[] {
				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
		};
		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));

	}


	/**
	 * Tests an extension handshake with an identifier dictionary entry of the wrong type
	 * @throws Exception 
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionHandshakeInvalidExtensionIDType() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		BDictionary dictionary = new BDictionary();
		BDictionary identifierDictionary = new BDictionary();
		dictionary.put ("m", identifierDictionary);
		identifierDictionary.put ("bl_ah", new BDictionary());
		byte[] messageBytes = BEncoder.encode (dictionary);
		byte[] header = new byte[] {
				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
		};
		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));

	}


	/**
	 * Tests an extension handshake with an identifier dictionary entry that has too low a value
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionHandshakeInvalidExtensionIDTooLow() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		BDictionary dictionary = new BDictionary();
		BDictionary identifierDictionary = new BDictionary();
		dictionary.put ("m", identifierDictionary);
		identifierDictionary.put ("bl_ah", -1);
		byte[] messageBytes = BEncoder.encode (dictionary);
		byte[] header = new byte[] {
				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
		};
		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));

	}


	/**
	 * Tests an extension handshake with an identifier dictionary entry that has too low a value
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionHandshakeInvalidExtensionIDTooHigh() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
			}
			@Override
			public void haveNoneMessage() {
				assertEquals (3, this.callCount++);
			}
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
		BDictionary dictionary = new BDictionary();
		BDictionary identifierDictionary = new BDictionary();
		dictionary.put ("m", identifierDictionary);
		identifierDictionary.put ("bl_ah", 256);
		byte[] messageBytes = BEncoder.encode (dictionary);
		byte[] header = new byte[] {
				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
		};
		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));

	}


// TODO Test - reinstate this test when something consumes the data
//	/**
//	 * Tests an extension handshake with a request queue depth that is not a number
//	 * @throws IOException
//	 */
//	@Test(expected=IOException.class)
//	public void testErrorExtensionHandshakeInvalidQueueType() throws IOException {
//
//		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
//		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
//
//		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
//			private int callCount = 0;
//			@Override
//			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
//				assertEquals (0, this.callCount++);
//			}
//			@Override
//			public void handshakeInfoHash (InfoHash infoHash) {
//				assertEquals (1, this.callCount++);
//			}
//			@Override
//			public void handshakePeerID (PeerID peerID) {
//				assertEquals (2, this.callCount++);
//			}
//			@Override
//			public void haveNoneMessage() {
//				assertEquals (3, this.callCount++);
//			}
//		};
//
//		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
//
//		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
//		BDictionary dictionary = new BDictionary();
//		dictionary.put ("m", new BDictionary());
//		dictionary.put ("reqq", "Not a number");
//		byte[] messageBytes = BEncoder.encode (dictionary);
//		byte[] header = new byte[] {
//				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
//		};
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));
//
//	}


// TODO Test - reinstate this test when something consumes the data
//	/**
//	 * Tests an extension handshake with a request queue depth that is not a number
//	 * @throws IOException
//	 */
//	@Test(expected=IOException.class)
//	public void testErrorExtensionHandshakeInvalidQueueTooLow() throws IOException {
//
//		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
//		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
//
//		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
//			private int callCount = 0;
//			@Override
//			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
//				assertEquals (0, this.callCount++);
//			}
//			@Override
//			public void handshakeInfoHash (InfoHash infoHash) {
//				assertEquals (1, this.callCount++);
//			}
//			@Override
//			public void handshakePeerID (PeerID peerID) {
//				assertEquals (2, this.callCount++);
//			}
//			@Override
//			public void haveNoneMessage() {
//				assertEquals (3, this.callCount++);
//			}
//		};
//
//		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
//
//		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
//		BDictionary dictionary = new BDictionary();
//		dictionary.put ("m", new BDictionary());
//		dictionary.put ("reqq", -1);
//		byte[] messageBytes = BEncoder.encode (dictionary);
//		byte[] header = new byte[] {
//				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
//		};
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));
//
//	}


// TODO Test - reinstate this test when something consumes the data
//	/**
//	 * Tests an extension handshake with a version that is not a string
//	 * @throws IOException
//	 */
//	@Test(expected=IOException.class)
//	public void testErrorExtensionHandshakeInvalidVersionType() throws IOException {
//
//		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
//		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
//
//		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
//			private int callCount = 0;
//			@Override
//			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
//				assertEquals (0, this.callCount++);
//			}
//			@Override
//			public void handshakeInfoHash (InfoHash infoHash) {
//				assertEquals (1, this.callCount++);
//			}
//			@Override
//			public void handshakePeerID (PeerID peerID) {
//				assertEquals (2, this.callCount++);
//			}
//			@Override
//			public void haveNoneMessage() {
//				assertEquals (3, this.callCount++);
//			}
//		};
//
//		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
//
//		byte[] handshakeBytes = PeerProtocolBuilder.handshake (true, true, infoHash, peerID).array();
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (handshakeBytes));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage().array()));
//		BDictionary dictionary = new BDictionary();
//		dictionary.put ("m", new BDictionary());
//		dictionary.put ("v", new BDictionary());
//		byte[] messageBytes = BEncoder.encode (dictionary);
//		byte[] header = new byte[] {
//				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
//		};
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));
//
//	}


	/**
	 * Tests parsing of a stream one byte at a time
	 * @throws IOException
	 */
	@Test
	public void testSingleBytes() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BitField bitField = new BitField (40);
		bitField.set (0);
		bitField.set (9);
		bitField.set (18);
		bitField.set (27);
		bitField.set (36);

		final InfoHash[] parsedInfoHash = new InfoHash[1];
		final PeerID[] parsedPeerID = new PeerID[1];
		final byte[][] parsedBitField = new byte[1][];
		final Boolean[] parsedChoke = new Boolean[1];
		final Boolean[] parsedInterested = new Boolean[1];

		PeerProtocolConsumer consumer = new MockProtocolConsumer() {
			private int callCount = 0;
			@Override
			public void handshakeBasicExtensions (boolean fastExtensionEnabled, boolean extensionProtocolEnabled) {
				assertEquals (0, this.callCount++);
			}
			@Override
			public void handshakeInfoHash (InfoHash infoHash) {
				assertEquals (1, this.callCount++);
				parsedInfoHash[0] = infoHash;
			}
			@Override
			public void handshakePeerID (PeerID peerID) {
				assertEquals (2, this.callCount++);
				parsedPeerID[0] = peerID;
			}
			@Override
			public void bitfieldMessage (byte[] bitField) {
				assertEquals (3, this.callCount++);
				parsedBitField[0] = bitField;
			}
			@Override
			public void chokeMessage(boolean choked) {
				assertEquals (4, this.callCount++);
				parsedChoke[0] = choked;
			}
			@Override
			public void interestedMessage(boolean interested) {
				assertEquals (5, this.callCount++);
				parsedInterested[0] = interested;
			}			
		};

		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();
		byte[] bitFieldBytes = PeerProtocolBuilder.bitfieldMessage (bitField).array();
		byte[] unchokeBytes = PeerProtocolBuilder.unchokeMessage().array();
		byte[] interestedBytes = PeerProtocolBuilder.interestedMessage().array();

		byte[] streamBytes = new byte[handshakeBytes.length + bitFieldBytes.length + unchokeBytes.length + interestedBytes.length];
		System.arraycopy (handshakeBytes, 0, streamBytes, 0, handshakeBytes.length);
		System.arraycopy (bitFieldBytes, 0, streamBytes, handshakeBytes.length, bitFieldBytes.length);
		System.arraycopy (unchokeBytes, 0, streamBytes, handshakeBytes.length + bitFieldBytes.length, unchokeBytes.length);
		System.arraycopy (interestedBytes, 0, streamBytes, handshakeBytes.length + bitFieldBytes.length + unchokeBytes.length, interestedBytes.length);

		for (byte b : streamBytes) {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { b }));
		}

		assertEquals (infoHash, parsedInfoHash[0]);
		assertEquals (peerID, parsedPeerID[0]);
		assertEquals (bitField, new BitField (parsedBitField[0], 40));
		assertFalse (parsedChoke[0]);
		assertTrue (parsedInterested[0]);

	}


	/**
	 * Tests parsing of a closed stream
	 * @throws Exception
	 */
	@Test(expected=ClosedChannelException.class)
	public void testClosed() throws Exception {

		PeerProtocolConsumer consumer = new MockProtocolConsumer() { };
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		ReadableByteChannel closedChannel = new ReadableByteChannel() {

			public int read (ByteBuffer dst) throws IOException {
				throw new ClosedChannelException();
			}

			public void close() throws IOException { }

			public boolean isOpen() {
				return false;
			}

		};

		parser.parseBytes (closedChannel);

	}


}
