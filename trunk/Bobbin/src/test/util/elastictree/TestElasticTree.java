/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.util.elastictree;


import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.itadaki.bobbin.util.elastictree.ElasticTree;
import org.itadaki.bobbin.util.elastictree.ElasticTreeView;
import org.junit.Test;

import test.Util;


/**
 * Tests MerkleTree
 */
public class TestElasticTree {

	/**
	 * Builds a populated, repeatable  specimen tree with a given leaf size and view length
	 * 
	 * @param leafSize The leaf size
	 * @param viewLength The view length
	 * @return The specimen tree
	 * @throws Exception
	 */
	private ElasticTree specimenTree (int leafSize, long viewLength) throws Exception {

		byte[][] hashes = Util.pseudoRandomBlockHashes (leafSize, (int)viewLength);

		return ElasticTree.buildFromLeaves (leafSize, viewLength, hashes);

	}


	/**
	 * Tests the root hash of a zero length tree
	 *
	 * @throws Exception
	 */
	@Test
	public void testZeroRootHash() throws Exception {

		ElasticTree specimenTree = specimenTree (1024, 0);

		assertArrayEquals (new byte[20], specimenTree.getView (0).getRootHash());

	}

	/**
	 * Test that blocks verify against a fully populated 2^n tree
	 * @throws Exception
	 */
	@Test
	public void testVerifyFullyPopulated() throws Exception {

		long viewLength = 1024 * 16;
		ElasticTree specimenTree = specimenTree (1024, viewLength);

		byte[][] hashes = Util.pseudoRandomBlockHashes (1024, (int)viewLength);

		for (int i = 0; i < 16; i++) {
			assertTrue (specimenTree.getView(viewLength).verifyLeafHash (i, hashes[i]));
		}

	}


	/**
	 * Test that sibling chains verify against an unpopulated 2^n tree
	 * @throws Exception
	 */
	@Test
	public void testVerifyUnpopulated() throws Exception {

		for (int numLeaves = 1; numLeaves <= 16; numLeaves++) {
			long viewLength = 1024 * numLeaves;
			ElasticTree specimenTree = specimenTree (1024, viewLength);

			for (int i = 0; i < numLeaves; i++) {
				ElasticTree testTree = ElasticTree.emptyTree (1024, 1024 * numLeaves, ByteBuffer.wrap (specimenTree.getView(viewLength).getRootHash()));
				byte[] hashChain = specimenTree.getView(viewLength).getHashChain (i);
				assertTrue (testTree.getView(viewLength).verifyHashChain (i, ByteBuffer.wrap (hashChain)));
			}
		}

	}


	/**
	 * Test that sibling chains fail against an unpopulated 2^n tree
	 * @throws Exception
	 */
	@Test
	public void testVerifyFailUnpopulated() throws Exception {

		for (int numLeaves = 1; numLeaves <= 16; numLeaves++) {
			long viewLength = 1024 * numLeaves;
			ElasticTree specimenTree = specimenTree (1024, viewLength);

			for (int i = 0; i < numLeaves; i++) {
				ElasticTree testTree = ElasticTree.emptyTree (1024, 1024 * numLeaves, ByteBuffer.wrap (specimenTree.getView(viewLength).getRootHash()));
				byte[] hashChain = specimenTree.getView(viewLength).getHashChain (i);
				hashChain[0]++;
				assertFalse (testTree.getView(viewLength).verifyHashChain (i, ByteBuffer.wrap (hashChain)));
			}
		}

	}



	/**
	 * Test that sibling chains verify progressively against an initially unpopulated 2^n tree
	 * @throws Exception
	 */
	@Test
	public void testVerifyUnpopulatedProgressive() throws Exception {

		for (int numLeaves = 1; numLeaves <= 16; numLeaves++) {
			long viewLength = 1024 * numLeaves;
			ElasticTree specimenTree = specimenTree (1024, viewLength);
			ElasticTree testTree = ElasticTree.emptyTree (1024, 1024 * numLeaves, ByteBuffer.wrap (specimenTree.getView(viewLength).getRootHash()));

			for (int i = 0; i < numLeaves; i++) {
				byte[] hashChain = specimenTree.getView(viewLength).getHashChain (i);
				assertTrue (testTree.getView(viewLength).verifyHashChain (i, ByteBuffer.wrap (hashChain)));
			}
		}

	}


	/**
	 * Test that sibling chains fail progressively against an initially unpopulated 2^n tree
	 * @throws Exception
	 */
	@Test
	public void testVerifyFailUnpopulatedProgressive() throws Exception {

		for (int numLeaves = 1; numLeaves <= 16; numLeaves++) {
			long viewLength = 1024 * numLeaves;
			ElasticTree specimenTree = specimenTree (1024, viewLength);
			ElasticTree testTree = ElasticTree.emptyTree (1024, 1024 * numLeaves, ByteBuffer.wrap (specimenTree.getView(viewLength).getRootHash()));

			for (int i = 0; i < numLeaves; i++) {
				byte[] hashChain = specimenTree.getView(viewLength).getHashChain (i);
				hashChain[0]++;
				assertFalse (testTree.getView(viewLength).verifyHashChain (i, ByteBuffer.wrap (hashChain)));
			}
		}

	}


	/**
	 * Tests a cursor on a view with one leaf
	 * @throws Exception
	 */
	@Test
	public void testCursor1() throws Exception {

		long viewLength = 1024 * 1;
		ElasticTree specimenTree = specimenTree (1024, viewLength);

		ElasticTreeView.Cursor cursor = specimenTree.getView(viewLength).new Cursor (0, true);

		assertTrue (cursor.atLeaf());
		assertTrue (cursor.atRoot());
		assertTrue (cursor.cursorNodeIsLeft());
		assertEquals (0, cursor.nodeIndex());

	}


	/**
	 * Tests moving a cursor on a view with two leaves
	 * @throws Exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testCursor1MotionDown() throws Exception {

		long viewLength = 1024 * 1;
		ElasticTree specimenTree = specimenTree (1024, viewLength);

		ElasticTreeView.Cursor cursor = specimenTree.getView(viewLength).new Cursor (0, true);

		cursor.goPathDown();

	}


	/**
	 * Tests moving a cursor on a view with two leaves
	 * @throws Exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testCursor1MotionUp() throws Exception {

		long viewLength = 1024 * 1;
		ElasticTree specimenTree = specimenTree (1024, viewLength);

		ElasticTreeView.Cursor cursor = specimenTree.getView(viewLength).new Cursor (0, true);

		cursor.goPathUp();

	}


	/**
	 * Tests moving a cursor on a view with two leaves
	 * @throws Exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testCursor1MotionRight() throws Exception {

		long viewLength = 1024 * 1;
		ElasticTree specimenTree = specimenTree (1024, viewLength);

		ElasticTreeView.Cursor cursor = specimenTree.getView(viewLength).new Cursor (0, true);

		cursor.goRight (1);

	}


	/**
	 * Tests moving a cursor on a view with two leaves
	 * @throws Exception
	 */
	@Test
	public void testCursor2Motion() throws Exception {

		long viewLength = 1024 * 2;
		ElasticTree specimenTree = specimenTree (1024, viewLength);

		ElasticTreeView.Cursor cursor = specimenTree.getView(viewLength).new Cursor (0, true);

		assertTrue (cursor.atLeaf());
		assertFalse (cursor.atRoot());
		assertTrue (cursor.cursorNodeIsLeft());
		assertEquals (0, cursor.nodeIndex());

		cursor.goRight (1);

		assertTrue (cursor.atLeaf());
		assertFalse (cursor.atRoot());
		assertFalse (cursor.cursorNodeIsLeft());
		assertEquals (1, cursor.nodeIndex());

		cursor.goPathUp();

		assertFalse (cursor.atLeaf());
		assertTrue (cursor.atRoot());
		assertTrue (cursor.cursorNodeIsLeft());
		assertEquals (2, cursor.nodeIndex());

	}


	/**
	 * Tests moving a cursor on a view with two leaves
	 * @throws Exception
	 */
	@Test
	public void testCursor2MotionDown() throws Exception {

		long viewLength = 1024 * 2;
		ElasticTree specimenTree = specimenTree (1024, viewLength);

		ElasticTreeView.Cursor cursor = specimenTree.getView(viewLength).new Cursor (0, false);

		assertFalse (cursor.atLeaf());
		assertTrue (cursor.atRoot());
		assertTrue (cursor.cursorNodeIsLeft());
		assertEquals (2, cursor.nodeIndex());

		cursor.goPathDown();

		assertTrue (cursor.atLeaf());
		assertFalse (cursor.atRoot());
		assertTrue (cursor.cursorNodeIsLeft());
		assertEquals (0, cursor.nodeIndex());

	}


	/**
	 * Tests moving a cursor on a view with two leaves
	 * @throws Exception
	 */
	@Test
	public void testCursor2BuildHash() throws Exception {

		long viewLength = 1024 * 2;
		ElasticTree specimenTree = specimenTree (1024, viewLength);

		ElasticTreeView.Cursor cursor = specimenTree.getView(viewLength).new Cursor (0, true);

		byte[] blockA = Util.pseudoRandomBlock (1000, 1024, 1024);
		byte[] blockB = Util.pseudoRandomBlock (2345, 1024, 1024);
		byte[] blockAHash = Util.buildHash (blockA);
		byte[] blockBHash = Util.buildHash (blockB);

		cursor.setHash (blockAHash);
		cursor.goRight (1);
		cursor.setHash (blockBHash);

		cursor.buildSiblingPairHash();
		cursor.goPathUp();

		assertArrayEquals (Util.buildHash (blockAHash, blockBHash), cursor.getHash());

	}


	/**
	 * Tests moving a cursor on a view with 16 leaves
	 * @throws Exception
	 */
	@Test
	public void testCursor16Motion1() throws Exception {

		long viewLength = 1024 * 16;
		ElasticTree specimenTree = specimenTree (1024, viewLength);

		ElasticTreeView.Cursor cursor = specimenTree.getView(viewLength).new Cursor (0, true);

		assertTrue (cursor.atLeaf());
		assertFalse (cursor.atRoot());
		assertTrue (cursor.cursorNodeIsLeft());
		assertEquals (0, cursor.nodeIndex());

		cursor.goPathUp();

		assertFalse (cursor.atLeaf());
		assertFalse (cursor.atRoot());
		assertTrue (cursor.cursorNodeIsLeft());
		assertEquals (2, cursor.nodeIndex());

		cursor.goPathUp();

		assertFalse (cursor.atLeaf());
		assertFalse (cursor.atRoot());
		assertTrue (cursor.cursorNodeIsLeft());
		assertEquals (6, cursor.nodeIndex());

		cursor.goPathUp();

		assertFalse (cursor.atLeaf());
		assertFalse (cursor.atRoot());
		assertTrue (cursor.cursorNodeIsLeft());
		assertEquals (14, cursor.nodeIndex());

		cursor.goPathUp();

		assertFalse (cursor.atLeaf());
		assertTrue (cursor.atRoot());
		assertTrue (cursor.cursorNodeIsLeft());
		assertEquals (30, cursor.nodeIndex());

	}


	/**
	 * Test that blocks verify against an extended tree
	 * @throws Exception
	 */
	@Test
	public void testVerifyExtended0x1() throws Exception {

		ElasticTree testTree = specimenTree (1024, 0);
		byte[][] hashes = Util.pseudoRandomBlockHashes (1024, 1024);
		testTree.addView (1024, hashes);

		assertTrue (testTree.getView (1024).verifyHashChain (0, ByteBuffer.wrap (hashes[0])));

	}


	/**
	 * Test that blocks verify against an extended tree
	 * @throws Exception
	 */
	@Test
	public void testVerifyExtended14x14() throws Exception {

		ElasticTree testTree = specimenTree (1024, (1024 * 13) + 10);
		byte[][] hashes = Util.pseudoRandomBlockHashes (1024, (1024 * 13) + 20);
		testTree.addView ((1024 * 13) + 20, new byte[][] { hashes[13] });

		ElasticTree specimenTree = specimenTree (1024, (1024 * 13) + 20);
		for (int i = 0; i < 14; i++) {
			assertTrue (testTree.getView ((1024 * 13) + 20).verifyHashChain (i, ByteBuffer.wrap (specimenTree.getView ((1024 * 13) + 20).getHashChain (i))));
		}

	}


	/**
	 * Test that blocks verify against an extended tree
	 * @throws Exception
	 */
	@Test
	public void testVerifyExtended14x16() throws Exception {

		ElasticTree specimenTree = specimenTree (1024, 1024 * 16);

		ElasticTree testTree = specimenTree (1024, 1024 * 14);
		byte[][] hashes = Util.pseudoRandomBlockHashes (1024, 1024 * 16);
		testTree.addView (1024 * 16, new byte[][] { hashes[14], hashes[15] });

		for (int i = 0; i < 16; i++) {
			assertTrue (testTree.getView (1024*16).verifyHashChain (i, ByteBuffer.wrap (specimenTree.getView (1024 * 16).getHashChain (i))));
		}

	}


	/**
	 * Tests verifying against a progressively extended tree
	 * @throws Exception
	 */
	@Test
	public void testVerifyExtendedProgressive() throws Exception {

		ElasticTree tree1 = specimenTree (1024, 0);
		for (int i = 0; i < 16; i++) {
			tree1.addView (1024 * (i + 1), new byte[][] { Util.buildHash (new byte[1024]) });
		}

		for (int i = 0; i < 16; i++) {
			assertTrue (tree1.verifyHashChain (i, tree1.getHashChain (i, 1024)));
		}

	}


	/**
	 * Tests garbage collection
	 * @throws Exception
	 */
	@Test
	public void testGarbageCollection1() throws Exception {

		ElasticTree specimenTree = specimenTree (1024, 1024 * 18);

		ElasticTree testTree = specimenTree (1024, 1024 * 14);
		testTree.addView (1024 * 18, ByteBuffer.wrap (specimenTree.getView(1024 * 18).getRootHash()));

		testTree.garbageCollectViews();

		assertNotNull (testTree.getView (1024 * 14));
		assertNotNull (testTree.getView (1024 * 18));

	}


	/**
	 * Tests garbage collection
	 * @throws Exception
	 */
	@Test
	public void testGarbageCollection2() throws Exception {

		ElasticTree specimenTree = specimenTree (1024, 1024 * 18);

		ElasticTree testTree = specimenTree (1024, 1024 * 14);
		testTree.addView (1024 * 18, ByteBuffer.wrap (specimenTree.getView(1024 * 18).getRootHash()));
		testTree.getView(1024 * 18).verifyHashChain (14, specimenTree.getHashChain (14, 1024).getHashes());

		testTree.garbageCollectViews();

		assertNull (testTree.getView (1024 * 14));
		assertNotNull (testTree.getView (1024 * 18));

	}


}
