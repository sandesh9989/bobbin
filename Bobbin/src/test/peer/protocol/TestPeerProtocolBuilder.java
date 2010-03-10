/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer.protocol;


import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.CharsetUtil;
import org.junit.Test;

import test.Util;


/**
 * Tests PeerProtocolBuilder
 */
public class TestPeerProtocolBuilder {

	/**
	 * Handshake
	 */
	@Test
	public void testHandshake() {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes(CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes(CharsetUtil.ASCII));

		byte[] handshakeBytes = PeerProtocolBuilder.handshake (false, false, infoHash, peerID).array();

		byte[] expectedBytes = new byte[68];
		expectedBytes[0] = 19;
		System.arraycopy ("BitTorrent protocol".getBytes (CharsetUtil.ASCII), 0, expectedBytes, 1, 19);
		System.arraycopy (infoHash.getBytes(), 0, expectedBytes, 28, 20);
		System.arraycopy (peerID.getBytes(), 0, expectedBytes, 48, 20);

		assertArrayEquals (expectedBytes, handshakeBytes);

	}


	// Protocol messages

	/**
	 * Keepalive message
	 */
	@Test
	public void testKeepaliveMessage() {

		byte[] expectedBytes = { 0, 0, 0, 0 };

		byte[] messageBytes = PeerProtocolBuilder.keepaliveMessage().array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Choke message
	 */
	@Test
	public void testChokeMessage() {

		byte[] expectedBytes = { 0, 0, 0, 1, 0 };

		byte[] messageBytes = PeerProtocolBuilder.chokeMessage().array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Unchoke message
	 */
	@Test
	public void testUnchokeMessage() {

		byte[] expectedBytes = { 0, 0, 0, 1, 1 };

		byte[] messageBytes = PeerProtocolBuilder.unchokeMessage().array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Interested message
	 */
	@Test
	public void testInterestedMessage() {

		byte[] expectedBytes = { 0, 0, 0, 1, 2 };

		byte[] messageBytes = PeerProtocolBuilder.interestedMessage().array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Not interested message
	 */
	@Test
	public void testNotInterestedMessage() {

		byte[] expectedBytes = { 0, 0, 0, 1, 3 };

		byte[] messageBytes = PeerProtocolBuilder.notInterestedMessage().array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Have message
	 */
	@Test
	public void testHaveMessage() {

		byte[] expectedBytes = { 0, 0, 0, 5, 4, 9, 8, 7, 6 };

		byte[] messageBytes = PeerProtocolBuilder.haveMessage ((9 << 24) + (8 << 16) + (7 << 8) + 6).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Bitfield message
	 */
	@Test
	public void testBitfieldMessage() {

		byte[] expectedBytes = { 0, 0, 0, 6, 5, -128, 64, 32, 16, 8 };

		BitField bitField = new BitField (40);
		bitField.set (0);
		bitField.set (9);
		bitField.set (18);
		bitField.set (27);
		bitField.set (36);

		byte[] messageBytes = PeerProtocolBuilder.bitfieldMessage (bitField).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Request message
	 */
	@Test
	public void testRequestMessage() {

		int pieceIndex = (4 << 24) + (3 << 16) + (2 << 8) + 1;
		int offset = (10 << 24) + (9 << 16) + (8 << 8) + 7;
		int length = (14 << 24) + (13 << 16) + (12 << 8) + 11;

		byte[] expectedBytes = new byte[] {
				0, 0, 0, 13,
				6,
				4, 3, 2, 1,
				10, 9, 8, 7,
				14, 13, 12, 11
		};

		byte[] messageBytes = PeerProtocolBuilder.requestMessage (new BlockDescriptor (pieceIndex, offset, length)).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Piece message
	 */
	@Test
	public void testPieceMessage() {

		int pieceIndex = (4 << 24) + (3 << 16) + (2 << 8) + 1;
		int offset = (10 << 24) + (9 << 16) + (8 << 8) + 7;
		byte[] data = new byte[] { 40, 41, 42, 43, 44, 45, 46 };

		byte[] expectedBytes = new byte[] {
				0, 0, 0, 16,
				7,
				4, 3, 2, 1,
				10, 9, 8, 7,
				40, 41, 42, 43, 44, 45, 46
		};

		ByteBuffer[] buffers = PeerProtocolBuilder.pieceMessage (new BlockDescriptor (pieceIndex, offset, data.length), ByteBuffer.wrap (data));
		assertEquals (2, buffers.length);

		byte[] messageBytes = Util.flattenBuffers(buffers).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Piece message with invalid data length
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testPieceMessageInvalidLength() {

		int pieceIndex = 1;
		int offset = 2;
		byte[] data = new byte[] { 3 };

		PeerProtocolBuilder.pieceMessage (new BlockDescriptor (pieceIndex, offset, 16384), ByteBuffer.wrap (data));

	}


	/**
	 * Cancel message
	 */
	@Test
	public void testCancelMessage() {

		int pieceIndex = (4 << 24) + (3 << 16) + (2 << 8) + 1;
		int offset = (10 << 24) + (9 << 16) + (8 << 8) + 7;
		int length = (14 << 24) + (13 << 16) + (12 << 8) + 11;

		byte[] expectedBytes = new byte[] {
				0, 0, 0, 13,
				8,
				4, 3, 2, 1,
				10, 9, 8, 7,
				14, 13, 12, 11
		};

		byte[] messageBytes = PeerProtocolBuilder.cancelMessage (new BlockDescriptor (pieceIndex, offset, length)).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Suggest piece message
	 */
	@Test
	public void testSuggestPieceMessage() {

		byte[] expectedBytes = { 0, 0, 0, 5, 13, 9, 8, 7, 6 };

		byte[] messageBytes = PeerProtocolBuilder.suggestPieceMessage ((9 << 24) + (8 << 16) + (7 << 8) + 6).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Have all message
	 */
	@Test
	public void testHaveAllMessage() {

		byte[] expectedBytes = { 0, 0, 0, 1, 14 };

		byte[] messageBytes = PeerProtocolBuilder.haveAllMessage().array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Have none message
	 */
	@Test
	public void testHaveNoneMessage() {

		byte[] expectedBytes = { 0, 0, 0, 1, 15 };

		byte[] messageBytes = PeerProtocolBuilder.haveNoneMessage().array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Reject request message
	 */
	@Test
	public void testRejectRequestMessage() {

		int pieceIndex = (4 << 24) + (3 << 16) + (2 << 8) + 1;
		int offset = (10 << 24) + (9 << 16) + (8 << 8) + 7;
		int length = (14 << 24) + (13 << 16) + (12 << 8) + 11;

		byte[] expectedBytes = new byte[] {
				0, 0, 0, 13,
				16,
				4, 3, 2, 1,
				10, 9, 8, 7,
				14, 13, 12, 11
		};

		byte[] messageBytes = PeerProtocolBuilder.rejectRequestMessage (new BlockDescriptor (pieceIndex, offset, length)).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Allowed fast message
	 */
	@Test
	public void testAllowedFastMessage() {

		byte[] expectedBytes = { 0, 0, 0, 5, 17, 9, 8, 7, 6 };

		byte[] messageBytes = PeerProtocolBuilder.allowedFastMessage ((9 << 24) + (8 << 16) + (7 << 8) + 6).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Extension handshake message
	 */
	@Test
	public void testExtensionHandshakeMessage() {

		byte[] expectedBytes = {
				0, 0, 0, 54,
				20,
				0,
				100, 49, 58, 109, 100,
				53, 58, 98, 108, 95,
				97, 104, 105, 49, 101,
				55, 58, 119, 105, 95,
				98, 98, 108, 101, 105,
				50, 101, 101, 52, 58,
				114, 101, 113, 113, 105,
				49, 50, 51, 101, 49,
				58, 118, 55, 58, 70,
				111, 111, 32, 49, 46,
				48, 101 
		};

		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put ("bl_ah", 1);
		extensions.put ("wi_bble", 2);
		BDictionary extra = new BDictionary();
		extra.put ("v", "Foo 1.0");
		extra.put ("reqq", 123);
		ByteBuffer[] buffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, extra);

		byte[] messageBytes = Util.flattenBuffers(buffers).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Extension message
	 */
	@Test
	public void testExtensionMessage() {

		byte[] expectedBytes = { 0, 0, 0, 6, 20, 1, 2, 3, 4, 5 };

		ByteBuffer[] buffers = PeerProtocolBuilder.extensionMessage (1, ByteBuffer.wrap (new byte[] { 2, 3, 4, 5 }));

		byte[] messageBytes = Util.flattenBuffers(buffers).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Merkle piece message
	 */
	@Test
	public void testMerklePieceMessageWithHashes() {

		byte[] expectedBytes = {
				0, 0, 0, 58,
				20,
				1,
				0, 0, 0, 1,
				0, 0, 0, 0,
				0, 0, 0, 40,
				108, 108, 105, 45, 50, 49, 52, 55, 52, 56, 51, 54, 52, 56, 101, 50, 48, 58, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 101, 101,
				50, 51, 52, 53, 
		};

		ByteBuffer[] buffers = PeerProtocolBuilder.merklePieceMessage (
				new BlockDescriptor (1, 0, 4),
				ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 }),
				ByteBuffer.wrap (new byte[] { 50, 51, 52, 53 })
		);

		byte[] messageBytes = Util.flattenBuffers(buffers).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Merkle piece message
	 */
	@Test
	public void testMerklePieceMessageWithoutHashes() {

		byte[] expectedBytes = {
				0, 0, 0, 18,
				20,
				1,
				0, 0, 0, 1,
				0, 0, 0, 4,
				0, 0, 0, 0,
				54, 55, 56, 57
		};

		ByteBuffer[] buffers = PeerProtocolBuilder.merklePieceMessage (
				new BlockDescriptor (1, 4, 4),
				null,
				ByteBuffer.wrap (new byte[] { 54, 55, 56, 57 })
		);

		byte[] messageBytes = Util.flattenBuffers(buffers).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Elastic signature message
	 */
	@Test
	public void testElasticSignatureMessage() {

		byte[] expectedBytes = {
				0, 0, 0, 71,
				20,
				2,
				0,
				1, 2, 3, 4, 5, 6, 7, 8,
				0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
				50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
				70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89
		};

		ByteBuffer[] buffers = PeerProtocolBuilder.elasticSignatureMessage (new ViewSignature (
				0x0102030405060708L,
				ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 }),
				ByteBuffer.wrap (new byte[] {
						50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
						70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89
				})
		));

		byte[] messageBytes = Util.flattenBuffers(buffers).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Elastic piece message
	 */
	@Test
	public void testElasticPieceMessageWithHashes() {

		byte[] expectedBytes = {
				0, 0, 0, 44,
				20,
				2,
				1,
				0, 0, 0, 1,
				0, 0, 0, 0,
				1,
				1, 2, 3, 4, 5, 6, 7, 8,
				0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
				50, 51, 52, 53
		};

		ByteBuffer[] buffers = PeerProtocolBuilder.elasticPieceMessage (
				new BlockDescriptor (1, 0, 4),
				0x0102030405060708L,
				ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 }),
				ByteBuffer.wrap (new byte[] { 50, 51, 52, 53 })
		);

		byte[] messageBytes = Util.flattenBuffers(buffers).array();

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Elastic piece message
	 */
	@Test
	public void testElasticPieceMessageWithoutHashes() {

		byte[] expectedBytes = {
				0, 0, 0, 16,
				20,
				2,
				1,
				0, 0, 0, 1,
				0, 0, 0, 4,
				0,
				54, 55, 56, 57
		};

		ByteBuffer[] buffers = PeerProtocolBuilder.elasticPieceMessage (
				new BlockDescriptor (1, 4, 4),
				null,
				null,
				ByteBuffer.wrap (new byte[] { 54, 55, 56, 57 })
		);

		byte[] messageBytes = new byte[buffers[0].capacity() + buffers[1].capacity() + buffers[2].capacity()];
		buffers[0].get (messageBytes, 0, buffers[0].capacity());
		buffers[1].get (messageBytes, buffers[0].capacity(), buffers[1].capacity());
		buffers[2].get (messageBytes, buffers[0].capacity() + buffers[1].capacity(), buffers[2].capacity());

		assertArrayEquals (expectedBytes, messageBytes);

	}


	/**
	 * Elastic bitfield message
	 */
	@Test
	public void testElasticBitfieldMessage() {

		byte[] expectedBytes = {
				0, 0, 0, 8,
				20,
				2,
				2,
				-128, 64, 32, 16, 8
		};

		BitField bitField = new BitField (40);
		bitField.set (0);
		bitField.set (9);
		bitField.set (18);
		bitField.set (27);
		bitField.set (36);

		ByteBuffer[] buffers = PeerProtocolBuilder.elasticBitfieldMessage (bitField);

		byte[] messageBytes = new byte[buffers[0].capacity() + buffers[1].capacity()];
		buffers[0].get (messageBytes, 0, buffers[0].capacity());
		buffers[1].get (messageBytes, buffers[0].capacity(), buffers[1].capacity());

		assertArrayEquals (expectedBytes, messageBytes);

	}

}
