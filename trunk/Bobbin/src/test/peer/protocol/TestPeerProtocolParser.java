/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer.protocol;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BEncoder;
import org.itadaki.bobbin.bencode.InvalidEncodingException;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConstants;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConsumer;
import org.itadaki.bobbin.peer.protocol.PeerProtocolParser;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.PieceStyle;
import org.itadaki.bobbin.torrentdb.ResourceType;
import org.itadaki.bobbin.util.BitField;
import org.junit.Test;
import org.mockito.InOrder;

import test.Util;


/**
 * Tests PeerProtocolParser
 */
public class TestPeerProtocolParser {

	/**
	 * Tests that PeerProtocolConsumer.keepAliveMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testKeepAlive() throws IOException {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);
		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.keepaliveMessage()));

		// Then
		verify(mockConsumer).keepAliveMessage();
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.chokeMessage(true) is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testChoke() throws IOException {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.chokeMessage()));

		// Then
		verify(mockConsumer).chokeMessage (true);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.chokeMessage(false) is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testUnchoke() throws IOException {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.unchokeMessage()));

		// Then
		verify(mockConsumer).chokeMessage (false);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.interestedMessage(true) is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testInterested() throws IOException {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.interestedMessage()));

		// Then
		verify(mockConsumer).interestedMessage (true);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.interestedMessage(false) is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testNotInterested() throws IOException {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.notInterestedMessage()));

		// Then
		verify(mockConsumer).interestedMessage (false);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.haveMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testHave() throws IOException {

		// Given
		int pieceIndex = 1234;
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveMessage(pieceIndex)));

		// Then
		verify(mockConsumer).haveMessage (null, pieceIndex);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.bitFieldMessage() is called
	 * @throws IOException
	 */
	@Test
	public void testBitfield() throws IOException {

		// Given
		BitField bitField = new BitField (40);
		bitField.set (0);
		bitField.set (9);
		bitField.set (18);
		bitField.set (27);
		bitField.set (36);
		byte[] bitFieldBytes = new byte[bitField.byteLength()];
		bitField.copyTo(bitFieldBytes, 0);
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.bitfieldMessage(bitField)));

		// Then
		verify(mockConsumer).bitfieldMessage (null, bitFieldBytes);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.requestMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testRequest() throws IOException {

		// Given
		BlockDescriptor requestDescriptor = new BlockDescriptor (1234, 5678, 9012);
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.requestMessage(requestDescriptor)));

		// Then
		verify(mockConsumer).requestMessage (null, requestDescriptor);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.pieceMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testPiece() throws IOException {

		// Given
		byte[] data = new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1 };
		BlockDescriptor requestDescriptor = new BlockDescriptor (1234, 5678, data.length);
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.pieceMessage (requestDescriptor, ByteBuffer.wrap (data))));

		// Then
		verify(mockConsumer).pieceMessage (PieceStyle.PLAIN, null, requestDescriptor, null, null, ByteBuffer.wrap (data));
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.cancelMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testCancel() throws IOException {

		// Given
		BlockDescriptor requestDescriptor = new BlockDescriptor (1234, 5678, 9012);
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.cancelMessage (requestDescriptor)));

		// Then
		verify(mockConsumer).cancelMessage (null, requestDescriptor);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.suggestPieceMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testSuggestPiece() throws IOException {

		// Given
		int pieceIndex = 1234;
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.suggestPieceMessage (pieceIndex)));

		// Then
		verify(mockConsumer).suggestPieceMessage (pieceIndex);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.haveAllMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testHaveAll() throws IOException {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage()));

		// Then
		verify(mockConsumer).haveAllMessage();
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.haveNoneMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testHaveNone() throws IOException {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage()));

		// Then
		verify(mockConsumer).haveNoneMessage();
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.rejectRequestMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testRejectRequest() throws IOException {

		// Given
		BlockDescriptor requestDescriptor = new BlockDescriptor (1234, 5678, 9012);
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.rejectRequestMessage (requestDescriptor)));

		// Then
		verify(mockConsumer).rejectRequestMessage (null, requestDescriptor);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests that PeerProtocolConsumer.allowedFastMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testAllowedFast() throws IOException {

		// Given
		int pieceIndex = 1234;
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.allowedFastMessage (pieceIndex)));

		// Then
		verify(mockConsumer).allowedFastMessage (pieceIndex);
		verifyNoMoreInteractions (mockConsumer);

	}


//	/**
//	 * Tests an extension handshake that adds an extension
//	 * @throws Exception 
//	 */
//	@Test
//	public void testExtensionHandshakeAdd() throws Exception {
//
//		// Given
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put ("bl_ah", 1);
//		BDictionary extra = new BDictionary();
//		extra.put ("v", "Foo 1.0");
//		extra.put ("reqq", 123);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, extra)));
//
//		// Then
//		verify(mockConsumer).extensionHandshakeMessage (new HashSet<String> (Arrays.asList ("bl_ah")), new HashSet<String>(), extra);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests an extension handshake that subtracts an extension
//	 * @throws Exception 
//	 */
//	@Test
//	public void testExtensionHandshakeRemove() throws Exception {
//
//		// Given
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put ("bl_ah", 1);
//		BDictionary extra = new BDictionary();
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, extra)));
//		extensions.put ("bl_ah", 0);
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, extra)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (new HashSet<String> (Arrays.asList ("bl_ah")), new HashSet<String>(), extra);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (new HashSet<String>(), new HashSet<String> (Arrays.asList ("bl_ah")), extra);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests an extension handshake that subtracts an extension not previously enabled
//	 * @throws Exception
//	 */
//	@Test
//	public void testExtensionHandshakeRemoveNonexistent() throws Exception {
//
//		// Given
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put ("bl_ah", 1);
//		BDictionary extra = new BDictionary();
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, extra)));
//		extensions.put ("wi_bble", 0);
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, extra)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (new HashSet<String> (Arrays.asList ("bl_ah")), new HashSet<String>(), extra);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (new HashSet<String>(), new HashSet<String>(), extra);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	// TODO Test - defense against a recursive list/dictionary attack
//
//
//	/**
//	 * Tests an extension message
//	 * @throws IOException
//	 */
//	@Test
//	public void testExtensionMessage() throws IOException {
//
//		// Given
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put ("bl_ah", 42);
//		BDictionary extra = new BDictionary();
//		extra.put ("v", "Foo 1.0");
//		extra.put ("reqq", 123);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, extra)));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionMessage (42, ByteBuffer.wrap (new byte[] { 1, 2, 3, 4 }))));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (extensions, new HashSet<String>(), extra);
//		sequence.verify(mockConsumer).extensionMessage ("bl_ah", new byte[] { 1, 2, 3, 4 });
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests a Merkle piece message
//	 * @throws IOException
//	 */
//	@Test
//	public void testMerklePieceMessage() throws IOException {
//
//		// Given
//		BlockDescriptor expectedDescriptor = new BlockDescriptor (1, 0, 4);
//		ByteBuffer expectedHashChain = ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 });
//		ByteBuffer expectedBlock = ByteBuffer.wrap (new byte[] { 50, 51, 52, 53 });
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_MERKLE, 1);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.merklePieceMessage (
//				expectedDescriptor,
//				expectedHashChain.duplicate(),
//				expectedBlock.duplicate()
//		)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (new HashSet<String> (
//				Arrays.asList (PeerProtocolConstants.EXTENSION_MERKLE)),
//				new HashSet<String>(),
//				new BDictionary()
//		);
//		sequence.verify(mockConsumer).pieceMessage (PieceStyle.MERKLE, null, expectedDescriptor, null, expectedHashChain, expectedBlock);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests an elastic signature message
//	 * @throws IOException
//	 */
//	@Test
//	public void testElasticSignatureMessage() throws IOException {
//
//		// Given
//		long expectedViewLength = 0x7FFFFEFDFCFBFAF9L;
//		ByteBuffer expectedViewRootHash = ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 });
//		ByteBuffer expectedSignature = ByteBuffer.wrap (new byte[] {
//				50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
//				70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89
//		});
//		ViewSignature expectedViewSignature = new ViewSignature (expectedViewLength, expectedViewRootHash, expectedSignature);
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, 2);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		ByteBuffer[] elasticSignatureBuffers = PeerProtocolBuilder.elasticSignatureMessage (new ViewSignature (
//				expectedViewLength,
//				expectedViewRootHash,
//				expectedSignature
//		));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (elasticSignatureBuffers));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (
//				new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_ELASTIC)),
//				new HashSet<String>(),
//				new BDictionary()
//		);
//		sequence.verify(mockConsumer).elasticSignatureMessage (expectedViewSignature);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests an elastic piece message
//	 * @throws IOException
//	 */
//	@Test
//	public void testElasticPieceMessage() throws IOException {
//
//		// Given
//		long expectedViewLength = 0x7FFFFEFDFCFBFAF9L;
//		BlockDescriptor expectedDescriptor = new BlockDescriptor (1, 0, 4);
//		ByteBuffer expectedHashChain = ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 });
//		ByteBuffer expectedBlock = ByteBuffer.wrap (new byte[] { 50, 51, 52, 53 });
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, 2);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.elasticPieceMessage (
//				expectedDescriptor,
//				expectedViewLength,
//				expectedHashChain.duplicate(),
//				expectedBlock.duplicate()
//		)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (
//				new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_ELASTIC)),
//				new HashSet<String>(),
//				new BDictionary()
//		);
//		sequence.verify(mockConsumer).pieceMessage (PieceStyle.ELASTIC, null, expectedDescriptor, expectedViewLength, expectedHashChain, expectedBlock);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests an elastic bitfield message
//	 * @throws IOException
//	 */
//	@Test
//	public void testElasticBitfieldMessage() throws IOException {
//
//		// Given
//		byte[] expectedBitfieldBytes = new byte[] { (byte)0xff, 0x00, (byte)0xee, (byte)0xf0 };
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, 2);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.elasticBitfieldMessage (new BitField (expectedBitfieldBytes, 28))));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (
//				new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_ELASTIC)),
//				new HashSet<String>(),
//				new BDictionary()
//		);
//		sequence.verify(mockConsumer).elasticBitfieldMessage (expectedBitfieldBytes);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests a resource have message
//	 * @throws IOException
//	 */
//	@Test
//	public void testResourceHave() throws IOException {
//
//		// Given
//		int pieceNumber = 1234;
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_RESOURCE, 3);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceDirectoryMessage (
//				Arrays.asList (ResourceType.INFO),
//				Arrays.asList (new Integer[] { 1 })
//		)));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceHaveMessage (ResourceType.INFO.id, pieceNumber)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (
//				new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_RESOURCE)),
//				new HashSet<String>(),
//				new BDictionary()
//		);
//		sequence.verify(mockConsumer).resourceDirectoryMessage (Arrays.asList (new ResourceType[] { ResourceType.INFO }), Arrays.asList (new Integer[] { 1 }));
//		sequence.verify(mockConsumer).haveMessage (ResourceType.INFO, pieceNumber);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests a resource bitfield message
//	 * @throws IOException
//	 */
//	@Test
//	public void testResourceBitfield() throws IOException {
//
//		// Given
//		BitField bitField = new BitField (40);
//		bitField.set (0);
//		bitField.set (9);
//		bitField.set (18);
//		bitField.set (27);
//		bitField.set (36);
//		byte[] bitFieldBytes = new byte[bitField.byteLength()];
//		bitField.copyTo(bitFieldBytes, 0);
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_RESOURCE, 3);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceDirectoryMessage (
//				Arrays.asList (ResourceType.INFO),
//				Arrays.asList (new Integer[] { 1 })
//		)));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceBitfieldMessage (ResourceType.INFO.id, bitField)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (
//				new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_RESOURCE)),
//				new HashSet<String>(),
//				new BDictionary()
//		);
//		sequence.verify(mockConsumer).resourceDirectoryMessage (Arrays.asList (new ResourceType[] { ResourceType.INFO }), Arrays.asList (new Integer[] { 1 }));
//		sequence.verify(mockConsumer).bitfieldMessage (ResourceType.INFO, bitFieldBytes);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests a resource request message
//	 * @throws IOException
//	 */
//	@Test
//	public void testResourceRequest() throws IOException {
//
//		// Given
//		BlockDescriptor requestDescriptor = new BlockDescriptor (1234, 5678, 9012);
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_RESOURCE, 3);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceDirectoryMessage (
//				Arrays.asList (ResourceType.INFO),
//				Arrays.asList (new Integer[] { 1 })
//		)));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceRequestMessage (ResourceType.INFO.id, requestDescriptor)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (
//				new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_RESOURCE)),
//				new HashSet<String>(),
//				new BDictionary()
//		);
//		sequence.verify(mockConsumer).resourceDirectoryMessage (Arrays.asList (new ResourceType[] { ResourceType.INFO }), Arrays.asList (new Integer[] { 1 }));
//		sequence.verify(mockConsumer).requestMessage (ResourceType.INFO, requestDescriptor);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests a resource cancel message
//	 * @throws IOException
//	 */
//	@Test
//	public void testResourceCancel() throws IOException {
//
//		// Given
//		BlockDescriptor requestDescriptor = new BlockDescriptor (1234, 5678, 9012);
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_RESOURCE, 3);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceDirectoryMessage (
//				Arrays.asList (ResourceType.INFO),
//				Arrays.asList (new Integer[] { 1 })
//		)));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceCancelMessage (ResourceType.INFO.id, requestDescriptor)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (
//				new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_RESOURCE)),
//				new HashSet<String>(),
//				new BDictionary()
//		);
//		sequence.verify(mockConsumer).resourceDirectoryMessage (Arrays.asList (new ResourceType[] { ResourceType.INFO }), Arrays.asList (new Integer[] { 1 }));
//		sequence.verify(mockConsumer).cancelMessage (ResourceType.INFO, requestDescriptor);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests a resource piece message
//	 * @throws IOException
//	 */
//	@Test
//	public void testResourcePiece() throws IOException {
//
//		// Given
//		BlockDescriptor requestDescriptor = new BlockDescriptor (1234, 5678, 4);
//		byte[] pieceData = new byte[] { 1, 2, 3, 4 };
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_RESOURCE, 3);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceDirectoryMessage (
//				Arrays.asList (ResourceType.INFO),
//				Arrays.asList (new Integer[] { 1 })
//		)));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourcePieceMessage (
//				ResourceType.INFO.id,
//				requestDescriptor,
//				ByteBuffer.wrap (pieceData)
//		)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (
//				new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_RESOURCE)),
//				new HashSet<String>(), new BDictionary()
//		);
//		sequence.verify(mockConsumer).resourceDirectoryMessage (Arrays.asList (new ResourceType[] { ResourceType.INFO }), Arrays.asList (new Integer[] { 1 }));
//		sequence.verify(mockConsumer).pieceMessage (PieceStyle.PLAIN, ResourceType.INFO, requestDescriptor, null, null, ByteBuffer.wrap (pieceData));
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests a resource reject request message
//	 * @throws IOException
//	 */
//	@Test
//	public void testResourceRejectRequest() throws IOException {
//
//		// Given
//		BlockDescriptor requestDescriptor = new BlockDescriptor (1234, 5678, 9012);
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_RESOURCE, 3);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceDirectoryMessage (
//				Arrays.asList (ResourceType.INFO),
//				Arrays.asList (new Integer[] { 1 })
//		)));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceRejectRequestMessage (ResourceType.INFO.id, requestDescriptor)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (
//				new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_RESOURCE)),
//				new HashSet<String>(),
//				new BDictionary()
//		);
//		sequence.verify(mockConsumer).resourceDirectoryMessage (Arrays.asList (new ResourceType[] { ResourceType.INFO }), Arrays.asList (new Integer[] { 1 }));
//		sequence.verify(mockConsumer).rejectRequestMessage (ResourceType.INFO, requestDescriptor);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}
//
//
//	/**
//	 * Tests a resource subscribe message
//	 * @throws IOException
//	 */
//	@Test
//	public void testResourceSubscribeRequest() throws IOException {
//
//		// Given
//		Map<String,Integer> extensions = new TreeMap<String,Integer>();
//		extensions.put (PeerProtocolConstants.EXTENSION_RESOURCE, 3);
//		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
//		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);
//
//		// When
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceDirectoryMessage (
//				Arrays.asList (ResourceType.INFO),
//				Arrays.asList (new Integer[] { 1 })
//		)));
//		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.resourceSubscribeMessage (ResourceType.INFO.id)));
//
//		// Then
//		InOrder sequence = inOrder (mockConsumer);
//		sequence.verify(mockConsumer).extensionHandshakeMessage (
//				new HashSet<String> (Arrays.asList (PeerProtocolConstants.EXTENSION_RESOURCE)),
//				new HashSet<String>(),
//				new BDictionary()
//		);
//		sequence.verify(mockConsumer).resourceDirectoryMessage (Arrays.asList (new ResourceType[] { ResourceType.INFO }), Arrays.asList (new Integer[] { 1 }));
//		sequence.verify(mockConsumer).resourceSubscribeMessage (ResourceType.INFO);
//		verifyNoMoreInteractions (mockConsumer);
//
//	}


	/**
	 * Tests that PeerProtocolConsumer.unknownMessage() is called in sequence
	 * @throws IOException
	 */
	@Test
	public void testUnknown() throws IOException {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 5, 99, 1, 2, 3, 4 }));

		// Then
		verify(mockConsumer).unknownMessage (99, new byte[] { 1, 2, 3, 4 });
		verifyNoMoreInteractions (mockConsumer);

	}


	// Protocol errors

	/**
	 * Tests that an exception is thrown for a choke message of the wrong length
	 * @throws IOException
	 */
	@Test
	public void testErrorChokeWrongLength() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_CHOKE, 0 }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for an unchoke message of the wrong length
	 * @throws IOException
	 */
	@Test
	public void testErrorUnchokeWrongLength() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_UNCHOKE, 0 }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for an interested message of the wrong length
	 * @throws IOException
	 */
	@Test
	public void testErrorInterestedWrongLength() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_INTERESTED, 0 }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a not interested message of the wrong length
	 * @throws IOException
	 */
	@Test
	public void testErrorNotInterestedWrongLength() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_NOT_INTERESTED, 0 }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a have message that is too short
	 * @throws IOException
	 */
	@Test
	public void testErrorHaveTooShort() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_HAVE }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a have message that is too long
	 * @throws IOException
	 */
	@Test
	public void testErrorHaveTooLong() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 6, PeerProtocolConstants.MESSAGE_TYPE_HAVE, 0, 0, 0, 0, 0 }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown on an out of sequence bitfield message
	 * @throws IOException
	 */
	@Test
	public void testErrorBitfieldOutOfSequence() throws IOException {

		// Given
		BitField bitField = new BitField (40);
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.bitfieldMessage (bitField)));
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.bitfieldMessage (bitField)));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message sequence", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a request message that is too short
	 * @throws IOException
	 */
	@Test
	public void testErrorRequestTooShort() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_REQUEST }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a request message that is too long
	 * @throws IOException
	 */
	@Test
	public void testErrorRequestTooLong() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (
					new byte[] { 0, 0, 0, 14, PeerProtocolConstants.MESSAGE_TYPE_REQUEST, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
			));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a piece message that is too short
	 * @throws IOException
	 */
	@Test
	public void testErrorPieceTooShort() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 8, PeerProtocolConstants.MESSAGE_TYPE_PIECE, 0, 0, 0, 0, 0, 0, 0 }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a cancel message that is too short
	 * @throws IOException
	 */
	@Test
	public void testErrorCancelTooShort() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_CANCEL }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a cancel message that is too long
	 * @throws IOException
	 */
	@Test
	public void testErrorCancelTooLong() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (
					new byte[] { 0, 0, 0, 14, PeerProtocolConstants.MESSAGE_TYPE_CANCEL, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
			));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown only once and no further data processed
	 * @throws IOException
	 */
	@Test
	public void testErrorBehaviour1() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.allowedFastMessage (1234)));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.allowedFastMessage (1234)));

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown only once and no further data processed
	 * @throws IOException
	 */
	@Test
	public void testErrorBehaviour2() throws IOException {

		// Given
		BitField bitField = new BitField (40);
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.bitfieldMessage (bitField)));
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.bitfieldMessage (bitField)));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message sequence", e.getMessage());
		}
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.chokeMessage()));

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that PeerProtocolConsumer.protocolError() is not called for a message that is exactly the maximum length
	 * @throws IOException
	 */
	@Test
	public void testErrorMessageExactlyMaximumLength() throws IOException {

		// Given
		byte[] messageData = new byte[4 + 9 + 131072];
		messageData[1] = 2;
		messageData[3] = 9;
		messageData[4] = PeerProtocolConstants.MESSAGE_TYPE_PIECE;
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageData));

		// Then
		verify(consumer).pieceMessage (eq (PieceStyle.PLAIN), any (ResourceType.class), any (BlockDescriptor.class), any (Long.class), any (ByteBuffer.class),
				any (ByteBuffer.class));

	}


	/**
	 * Tests that an exception is thrown for a message that is too long
	 * @throws IOException
	 */
	@Test
	public void testErrorMessageTooLong() throws IOException {

		// Given
		byte[] erroneousData = new byte[4 + 9 + 131072 + 1];
		erroneousData[1] = 2;
		erroneousData[3] = 10;
		erroneousData[4] = PeerProtocolConstants.MESSAGE_TYPE_PIECE;
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (erroneousData));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Message too large", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a non Fast protocol enabled stream when a suggest piece message
	 * is received
	 * @throws IOException
	 */
	@Test
	public void testErrorNonFastSuggestPiece() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.suggestPieceMessage (0)));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a non Fast protocol enabled stream when a have all message
	 * is received
	 * @throws IOException
	 */
	@Test
	public void testErrorNonFastHaveAll() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a non Fast protocol enabled stream when a have none message
	 * is received
	 * @throws IOException
	 */
	@Test
	public void testErrorNonFastHaveNone() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a non Fast protocol enabled stream when a reject request message
	 * is received
	 * @throws IOException
	 */
	@Test
	public void testErrorNonFastRejectRequest() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.rejectRequestMessage (new BlockDescriptor (0,1,2))));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a non Fast protocol enabled stream when an allowed fast message
	 * is received
	 * @throws IOException
	 */
	@Test
	public void testErrorNonFastAllowedFast() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.allowedFastMessage (0)));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a have all message out of sequence
	 * @throws IOException
	 */
	@Test
	public void testErrorFastHaveAllOutOfSequence() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage()));
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a have none message out of sequence
	 * @throws IOException
	 */
	@Test
	public void testErrorFastHaveNoneOutOfSequence() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveAllMessage()));
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.haveNoneMessage()));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a suggest piece message that is
	 * too short
	 * @throws IOException
	 */
	@Test
	public void testErrorFastSuggestPieceTooShort() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_SUGGEST_PIECE }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a suggest piece message that is
	 * too long
	 * @throws IOException
	 */
	@Test
	public void testErrorFastSuggestPieceTooLong() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 6, PeerProtocolConstants.MESSAGE_TYPE_SUGGEST_PIECE, 0, 0, 0, 0, 0 }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a have all message that is
	 * too long
	 * @throws IOException
	 */
	@Test
	public void testErrorFastHaveAllTooLong() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_HAVE_ALL, 0 }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a have none message that is
	 * too short
	 * @throws IOException
	 */
	@Test
	public void testErrorFastHaveNoneTooLong() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 2, PeerProtocolConstants.MESSAGE_TYPE_HAVE_NONE, 0 }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size, sequence or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a reject request message that is
	 * too short
	 * @throws IOException
	 */
	@Test
	public void testErrorFastRejectRequestTooShort() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_REJECT_REQUEST }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with a reject request message that is
	 * too long
	 * @throws IOException
	 */
	@Test
	public void testErrorFastRejectRequestTooLong() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] {
					0, 0, 0, 14, PeerProtocolConstants.MESSAGE_TYPE_REJECT_REQUEST,
					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
			}));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with an allowed fast message that is
	 * too short
	 * @throws IOException
	 */
	@Test
	public void testErrorFastAllowedFastTooShort() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 1, PeerProtocolConstants.MESSAGE_TYPE_ALLOWED_FAST }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown for a Fast protocol enabled stream with an allowed fast message that is
	 * too long
	 * @throws IOException
	 */
	@Test
	public void testErrorFastAllowedFastTooLong() throws IOException {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		boolean exceptionThrown = false;
		try {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 6, PeerProtocolConstants.MESSAGE_TYPE_ALLOWED_FAST, 0, 0, 0, 0, 0 }));
		} catch (IOException e) {
			exceptionThrown = true;
			assertEquals ("Invalid message size or Fast extension disabled", e.getMessage());
		}

		// Then
		assertTrue (exceptionThrown);

	}


	/**
	 * Tests that an exception is thrown on receiving an extension message when the extension
	 * protocol is disabled
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionDisabled() throws Exception {

		// Given
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put ("bl_ah", 1);
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, false);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, null)));

		// Then
		// ... exception

	}


	/**
	 * Tests an extension handshake with an undecodable dictionary
	 * @throws Exception
	 */
	@Test(expected=InvalidEncodingException.class)
	public void testErrorExtensionHandshakeInvalid() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 3, 20, 0, 1 }));

		// Then
		// ... exception

	}


	/**
	 * Tests an extension handshake with an incorrect payload type
	 * @throws Exception
	 */
	@Test(expected=InvalidEncodingException.class)
	public void testErrorExtensionHandshakeInvalidNotDictionary() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 4, 20, 0, 'l', 'e' }));

		// Then
		// ... exception

	}


	/**
	 * Tests an extension handshake with a valid dictionary followed by extra data
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionHandshakeTooLong() throws Exception {

		// Given
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put ("bl_ah", 1);
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		// When
		ByteBuffer[] extensionHandshakeBuffers = PeerProtocolBuilder.extensionHandshakeMessage (extensions, null);
		extensionHandshakeBuffers[0].array()[3]++;
		parser.parseBytes (Util.infiniteReadableByteChannelFor (extensionHandshakeBuffers));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0 }));

		// Then
		// ... exception

	}


	/**
	 * Tests an extension handshake with an identifier dictionary of the wrong type
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionHandshakeInvalidIdentifierDictionaryType() throws Exception {

		// Given
		BDictionary dictionary = new BDictionary();
		dictionary.put ("m", "Not a dictionary");
		byte[] messageBytes = BEncoder.encode (dictionary);
		byte[] header = new byte[] {
				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
		};
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an extension handshake with an identifier dictionary entry of the wrong type
	 * @throws Exception 
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionHandshakeInvalidExtensionIDType() throws Exception {

		// Given
		BDictionary dictionary = new BDictionary();
		BDictionary identifierDictionary = new BDictionary();
		dictionary.put ("m", identifierDictionary);
		identifierDictionary.put ("bl_ah", new BDictionary());
		byte[] messageBytes = BEncoder.encode (dictionary);
		byte[] header = new byte[] {
				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
		};
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an extension handshake with an identifier dictionary entry that has too low a value
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionHandshakeInvalidExtensionIDTooLow() throws Exception {

		// Given
		BDictionary dictionary = new BDictionary();
		BDictionary identifierDictionary = new BDictionary();
		dictionary.put ("m", identifierDictionary);
		identifierDictionary.put ("bl_ah", -1);
		byte[] messageBytes = BEncoder.encode (dictionary);
		byte[] header = new byte[] {
				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
		};
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an extension handshake with an identifier dictionary entry that has too low a value
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorExtensionHandshakeInvalidExtensionIDTooHigh() throws Exception {

		// Given
		BDictionary dictionary = new BDictionary();
		BDictionary identifierDictionary = new BDictionary();
		dictionary.put ("m", identifierDictionary);
		identifierDictionary.put ("bl_ah", 256);
		byte[] messageBytes = BEncoder.encode (dictionary);
		byte[] header = new byte[] {
				0, 0, 0, (byte)(messageBytes.length + 2) , 20, 0
		};
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);

		parser.parseBytes (Util.infiniteReadableByteChannelFor (header));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (messageBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests a Merkle piece message that is too short
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorMerkleTooShort() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 2, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE }));

		// Then
		// ... exception

	}


	/**
	 * Tests a Merkle piece message for a first block with no hash chain
	 * @throws IOException
	 */
	@Test(expected=IOException.class)
	public void testErrorMerkleFirstBlockNoHashChain() throws IOException {

		// Given
		BlockDescriptor expectedDescriptor = new BlockDescriptor (1, 0, 4);
		ByteBuffer expectedBlock = ByteBuffer.wrap (new byte[] { 50, 51, 52, 53 });
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.merklePieceMessage (
				PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE,
				expectedDescriptor,
				null, expectedBlock
		)));

		// Then
		// ... exception

	}


	/**
	 * Tests a Merkle piece message for a non-first block with a hash chain
	 * @throws IOException
	 */
	@Test(expected=IOException.class)
	public void testErrorMerkleNonFirstBlockHashChain() throws IOException {

		// Given
		BlockDescriptor expectedDescriptor = new BlockDescriptor (1, 1, 4);
		ByteBuffer expectedHashChain = ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 });
		ByteBuffer expectedBlock = ByteBuffer.wrap (new byte[] { 50, 51, 52, 53 });
		Map<String,Integer> extensions = new TreeMap<String,Integer>();
		extensions.put (PeerProtocolConstants.EXTENSION_MERKLE, 1);
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.extensionHandshakeMessage (extensions, new BDictionary())));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.merklePieceMessage (
				PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE,
				expectedDescriptor,
				expectedHashChain.duplicate(), expectedBlock
		)));

		// Then
		// ... exception

	}


	/**
	 * Tests a Merkle piece message with a bad hash chain length
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorMerkleBadHashChainLength1() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 14, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 0, 0, 0, 0, 0, 0, 0, 0, -128, -128, -128, -128 }));

		// Then
		// ... exception

	}


	/**
	 * Tests a Merkle piece message with a bad hash chain length
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorMerkleBadHashChainLength2() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 14, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));

		// Then
		// ... exception

	}


	/**
	 * Tests a Merkle piece message with a bad hash chain
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorMerkleBadHashChain1() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 16, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 'd', 'e' }));

		// Then
		// ... exception

	}


	/**
	 * Tests a Merkle piece message with a bad hash chain
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorMerkleBadHashChain2() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 18, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 'l', 'd', 'e', 'e' }));

		// Then
		// ... exception

	}


	/**
	 * Tests a Merkle piece message with a bad hash chain
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorMerkleBadHashChain3() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 21, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,
				'l',
				'l',
				'i', '0', 'e',
				'e',
				'e'
		}));

		// Then
		// ... exception

	}


	/**
	 * Tests a Merkle piece message with a bad hash chain
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorMerkleBadHashChain4() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 23, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9,
				'l',
				'l',
				'd', 'e', '1', ':', 'a',
				'e',
				'e'
		}));

		// Then
		// ... exception

	}


	/**
	 * Tests a Merkle piece message with a bad hash chain
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorMerkleBadHashChain5() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 23, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9,
				'l',
				'l',
				'i', '0', 'e', 'd', 'e',
				'e',
				'e' 
		}));

		// Then
		// ... exception

	}


	/**
	 * Tests an Elastic message that's too short
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorElasticTooShort() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 2, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC }));

		// Then
		// ... exception

	}


	/**
	 * Tests an Elastic signature message that's too short
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorElasticSignatureTooShort() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 70, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 0, 
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0
		}));

		// Then
		// ... exception

	}


	/**
	 * Tests an Elastic signature message that's too short
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorElasticSignatureTooLong() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 72, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 0, 
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0
		}));

		// Then
		// ... exception

	}


	/**
	 * Tests an Elastic piece message that's too short
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorElasticPieceTooShort1() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 11, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 1, 
				0, 0, 0, 0, 0, 0, 0, 0
		}));

		// Then
		// ... exception

	}


	/**
	 * Tests an Elastic piece message that's too short
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorElasticPieceTooShort2() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 12, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 1, 
				0, 0, 0, 0, 0, 0, 0, 0, 1
		}));

		// Then
		// ... exception

	}


	/**
	 * Tests an Elastic piece message for the first block with no hash chain
	 * @throws IOException
	 */
	@Test(expected=IOException.class)
	public void testErrorElasticPieceFirstBlockNoHashChain() throws IOException {

		// Given
		long expectedViewLength = 0x7FFFFEFDFCFBFAF9L;
		BlockDescriptor expectedDescriptor = new BlockDescriptor (1, 0, 4);
		ByteBuffer expectedBlock = ByteBuffer.wrap (new byte[] { 50, 51, 52, 53 });
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.elasticPieceMessage (
				PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC,
				expectedDescriptor,
				expectedViewLength,
				null, expectedBlock
		)));

		// Then
		// ... exception

	}


	/**
	 * Tests an Elastic piece message for a non-first block with a hash chain
	 * @throws IOException
	 */
	@Test(expected=IOException.class)
	public void testErrorElasticPieceNonFirstBlockHashChain() throws IOException {

		// Given
		long expectedViewLength = 0x7FFFFEFDFCFBFAF9L;
		BlockDescriptor expectedDescriptor = new BlockDescriptor (1, 1, 4);
		ByteBuffer expectedHashChain = ByteBuffer.wrap (new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 });
		ByteBuffer expectedBlock = ByteBuffer.wrap (new byte[] { 50, 51, 52, 53 });
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (PeerProtocolBuilder.elasticPieceMessage (
				PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC,
				expectedDescriptor,
				expectedViewLength,
				expectedHashChain, expectedBlock
		)));

		// Then
		// ... exception

	}


	/**
	 * Tests an Elastic piece message that's too short
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorElasticInvalidSubMessageType() throws Exception {

		// Given
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, true, true);

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0, 0, 0, 3, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE, 3 }));

		// Then
		// ... exception

	}


	/**
	 * Tests a valid resource directory followed by extra data
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorResourceDirectoryTooLong() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		ByteBuffer[] resourceDirectoryBuffers = PeerProtocolBuilder.resourceDirectoryMessage (
				PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_RESOURCE,
				Arrays.asList (ResourceType.INFO), Arrays.asList (new Integer[] { 1 })
		);
		resourceDirectoryBuffers[0].array()[3]++;

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceDirectoryBuffers));
		parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { 0 }));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource directory
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceDirectory1() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceDirectoryBytes = new byte[] { 0, 0, 0, 5, 20, 3, 0, 'd', 'e' };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceDirectoryBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource directory
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceDirectory2() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceDirectoryBytes = new byte[] { 0, 0, 0, 7, 20, 3, 0, 'l', 'd', 'e', 'e' };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceDirectoryBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource directory
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceDirectory3() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceDirectoryBytes = new byte[] { 0, 0, 0, 16, 20, 3, 0, 'l', 'l', '1', ':', 'a', '1', ':', 'b', 'i', '0', 'e', 'e', 'e' };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceDirectoryBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource directory
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceDirectory4() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceDirectoryBytes = new byte[] { 0, 0, 0, 16, 20, 3, 0, 'l', 'l', 'i', '0', 'e', 'i', '0', 'e', 'i', '0', 'e', 'e', 'e' };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceDirectoryBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource directory
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceDirectory5() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceDirectoryBytes = new byte[] { 0, 0, 0, 16, 20, 3, 0, 'l', 'l', 'i', '0', 'e', '1', ':', 'a', '1', ':', 'b', 'e', 'e' };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceDirectoryBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource directory
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceDirectory6() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceDirectoryBytes = new byte[] { 0, 0, 0, 17, 20, 3, 0, 'l', 'l', 'i', '-', '1', 'e', '1', ':', 'a', 'i', '0', 'e', 'e', 'e' };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceDirectoryBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource directory
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceDirectory7() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceDirectoryBytes = new byte[] { 0, 0, 0, 18, 20, 3, 0, 'l', 'l', 'i', '2', '5', '6', 'e', '1', ':', 'a', 'i', '0', 'e', 'e', 'e' };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceDirectoryBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource directory
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceDirectory8() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceDirectoryBytes = new byte[] { 0, 0, 0, 17, 20, 3, 0, 'l', 'l', 'i', '0', 'e', '1', ':', 'a', 'i', '-', '1', 'e', 'e', 'e' };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceDirectoryBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource transfer message
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceTransfer1() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceTransferBytes = new byte[] { 0, 0, 0, 3, 20, 3, 1 };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceTransferBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource transfer message
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceTransfer2() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceTransferBytes = new byte[] { 0, 0, 0, 8, 20, 3, 1, 0, 0, 0, 0, 0 };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceTransferBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource subscribe message
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceSubscribe1() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceTransferBytes = new byte[] { 0, 0, 0, 3, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_RESOURCE, 2 };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceTransferBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource subscribe message
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResourceSubscribe2() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceTransferBytes = new byte[] { 0, 0, 0, 4, 20, PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_RESOURCE, 2, 0 };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceTransferBytes));

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid resource message
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testErrorInvalidResource() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (consumer, true, true);
		byte[] resourceTransferBytes = new byte[] { 0, 0, 0, 3, 20, 3, 3 };

		// When
		parser.parseBytes (Util.infiniteReadableByteChannelFor (resourceTransferBytes));

		// Then
		// ... exception

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

		// Given
		BitField bitField = new BitField (40);
		bitField.set (0);
		bitField.set (9);
		bitField.set (18);
		bitField.set (27);
		bitField.set (36);
		byte[] bitFieldBytes = PeerProtocolBuilder.bitfieldMessage (bitField).array();
		byte[] unchokeBytes = PeerProtocolBuilder.unchokeMessage().array();
		byte[] interestedBytes = PeerProtocolBuilder.interestedMessage().array();
		byte[] streamBytes = new byte[bitFieldBytes.length + unchokeBytes.length + interestedBytes.length];
		System.arraycopy (bitFieldBytes, 0, streamBytes, 0, bitFieldBytes.length);
		System.arraycopy (unchokeBytes, 0, streamBytes, bitFieldBytes.length, unchokeBytes.length);
		System.arraycopy (interestedBytes, 0, streamBytes, bitFieldBytes.length + unchokeBytes.length, interestedBytes.length);
		PeerProtocolConsumer mockConsumer = mock (PeerProtocolConsumer.class); 
		PeerProtocolParser parser = new PeerProtocolParser (mockConsumer, false, false);

		// When
		for (byte b : streamBytes) {
			parser.parseBytes (Util.infiniteReadableByteChannelFor (new byte[] { b }));
		}

		// Then
		InOrder sequence = inOrder (mockConsumer);
		sequence.verify(mockConsumer).bitfieldMessage (null, bitField.content());
		sequence.verify(mockConsumer).chokeMessage (false);
		sequence.verify(mockConsumer).interestedMessage (true);
		verifyNoMoreInteractions (mockConsumer);

	}


	/**
	 * Tests parsing of a closed stream
	 * @throws Exception
	 */
	@Test(expected=ClosedChannelException.class)
	public void testClosed() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class);
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

		// When
		parser.parseBytes (closedChannel);

		// Then
		// ... exception

	}


	/**
	 * Tests parsing of a closed stream
	 * @throws Exception
	 */
	@Test(expected=ClosedChannelException.class)
	public void testEndOfStream() throws Exception {

		// Given
		PeerProtocolConsumer consumer = mock (PeerProtocolConsumer.class);
		PeerProtocolParser parser = new PeerProtocolParser (consumer, false, false);
		ReadableByteChannel closedChannel = new ReadableByteChannel() {

			public int read (ByteBuffer dst) throws IOException {
				return -1;
			}

			public void close() throws IOException { }

			public boolean isOpen() {
				return false;
			}

		};

		// When
		parser.parseBytes (closedChannel);

		// Then
		// ... exception

	}

}
