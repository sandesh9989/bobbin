/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.torrentdb;

import static 
org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.List;

import org.itadaki.bobbin.peer.protocol.PeerProtocolConstants;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.Piece;
import org.junit.Test;

import test.Util;


/**
 * Tests Piece
 */
public class TestPiece {

	/**
	 * Test instantiating a Piece with a negative piece number
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testGetPieceNumberNegative() {

		new Piece (-1, 16384, PeerProtocolConstants.BLOCK_LENGTH);

	}


	/**
	 * Tests instantiating a Piece with a zero piece number
	 */
	@Test
	public void testGetPieceNumber0() {

		Piece piece = new Piece (0, 16384, PeerProtocolConstants.BLOCK_LENGTH);
		assertEquals (0, piece.getPieceNumber());

	}


	/**
	 * Tests instantiating a Piece with a non-zero piece number
	 */
	@Test
	public void testGetPieceNumber1234() {

		Piece piece = new Piece (1234, 16384, PeerProtocolConstants.BLOCK_LENGTH);
		assertEquals (1234, piece.getPieceNumber());

	}


	/**
	 * Tests instantiating a Piece with a negative piece length
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testGetNeededBlocksNegative() {

		new Piece (1234, -1, PeerProtocolConstants.BLOCK_LENGTH);

	}


	/**
	 * Tests instantiating a Piece with a zero piece length
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testGetNeededBlocks0() {

		new Piece (1234, 0, PeerProtocolConstants.BLOCK_LENGTH);

	}


	/**
	 * Tests the  creation for a piece of length 1
	 */
	@Test
	public void testGetNeededBlocks1() {

		Piece piece = new Piece (1234, 1, PeerProtocolConstants.BLOCK_LENGTH);
		List<BlockDescriptor> descriptors = piece.getNeededBlocks();

		assertEquals (1, descriptors.size());
		assertEquals (new BlockDescriptor (1234, 0, 1), descriptors.get (0));

	}


	/**
	 * Tests the  creation for a piece of length 16383
	 */
	@Test
	public void testGetNeededBlocks16383() {

		Piece piece = new Piece (1234, 16383, PeerProtocolConstants.BLOCK_LENGTH);
		List<BlockDescriptor> descriptors = piece.getNeededBlocks();

		assertEquals (1, descriptors.size());
		assertEquals (new BlockDescriptor (1234, 0, 16383), descriptors.get (0));

	}


	/**
	 * Tests the  creation for a piece of length 16384
	 */
	@Test
	public void testGetNeededBlocks16384() {

		Piece piece = new Piece (1234, 16384, PeerProtocolConstants.BLOCK_LENGTH);
		List<BlockDescriptor> descriptors = piece.getNeededBlocks();

		assertEquals (1, descriptors.size());
		assertEquals (new BlockDescriptor (1234, 0, 16384), descriptors.get (0));

	}


	/**
	 * Tests the  creation for a piece of length 16385
	 */
	@Test
	public void testGetNeededBlocks16385() {

		Piece piece = new Piece (1234, 16385, PeerProtocolConstants.BLOCK_LENGTH);
		List<BlockDescriptor> descriptors = piece.getNeededBlocks();

		assertEquals (2, descriptors.size());
		assertEquals (new BlockDescriptor (1234, 0, 16384), descriptors.get (0));
		assertEquals (new BlockDescriptor (1234, 16384, 1), descriptors.get (1));

	}


	/**
	 * Tests the behaviour of putBlock()
	 */
	@Test
	public void testPutBlock() {

		Piece piece = new Piece (1234, 32769, PeerProtocolConstants.BLOCK_LENGTH);

		assertFalse (piece.putBlock (new BlockDescriptor (1234, 0, 16384), ByteBuffer.allocate (16384)));
		assertFalse (piece.putBlock (new BlockDescriptor (1234, 16384, 16384), ByteBuffer.allocate (16384)));
		assertTrue (piece.putBlock (new BlockDescriptor (1234, 32768, 1), ByteBuffer.allocate (1)));

	}


	/**
	 * Tests the behaviour of putBlock() with incorrect length data
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testPutBlockBadLength() {

		Piece piece = new Piece (1234, 32769, PeerProtocolConstants.BLOCK_LENGTH);

		piece.putBlock (new BlockDescriptor (1234, 0, 16384), ByteBuffer.allocate (1));

	}


	/**
	 * Tests the behaviour of putBlock() with an invalid descriptor
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testPutBlockBadBlock() {

		Piece piece = new Piece (1234, 16384, PeerProtocolConstants.BLOCK_LENGTH);

		piece.putBlock (new BlockDescriptor (1234, 16384, 16384), ByteBuffer.allocate (16384));

	}


	/**
	 * Tests the behaviour of getPiece()
	 */
	@Test
	public void testGetPiece() {

		byte[] expectedContent = Util.pseudoRandomBlock (0, 32768, 16384);
		System.arraycopy (Util.pseudoRandomBlock (1, 16384, 16384), 0, expectedContent, 16384, 16384);

		Piece piece = new Piece (1234, 32768, PeerProtocolConstants.BLOCK_LENGTH);

		assertFalse (piece.putBlock (new BlockDescriptor (1234, 0, 16384), ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384))));
		assertTrue (piece.putBlock (new BlockDescriptor (1234, 16384, 16384), ByteBuffer.wrap (Util.pseudoRandomBlock (1, 16384, 16384))));

		assertEquals (ByteBuffer.wrap (expectedContent), piece.getContent());

	}


}
