/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util.elastictree;


import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;


/**
 * An expandable Merkle hash tree
 * TODO Documentation - exposition on structure and algorithms
 */
public class ElasticTree {

	/**
	 * The size in bytes of a leaf
	 */
	private final int leafSize;

	/**
	 * The tree's immutable hash nodes in post order. Each node is a 20 byte SHA1 hash
	 */
	private final List<byte[]> hashNodes;

	/**
	 * The set of views onto the tree
	 */
	private final NavigableMap<Long,ElasticTreeView> views = new TreeMap<Long,ElasticTreeView>();

	/**
	 * Pre-calculated filler nodes. Each entry corresponds to a composed filler node for a given
	 * height within the graph, such that :
	 *   fillerNodes[0] = new byte[20]
	 *   fillerNodes[n] = hash(fillerNodes[n-1],fillerNodes[n-1])
	 */
	private final List<byte[]> fillerNodes = new ArrayList<byte[]> (Arrays.asList (new byte[20]));

	/**
	 * An SHA1 digester
	 */
	private final MessageDigest digest;


	/**
	 * Tests the equality of two arrays
	 *
	 * @param array1 The first array
	 * @param array2 The second array
	 * @return {@code true} if the arrays are equal
	 */
	static boolean arraysEqual (byte[] array1, byte[] array2) {

		return ByteBuffer.wrap (array1).equals (ByteBuffer.wrap (array2));

	}


	/**
	 * Tests the equality of an array to a partial array
	 *
	 * @param array1 The first array
	 * @param array2 The second array
	 * @param array2Offset The offset within the second array
	 * @param array2Length The length within the second array
	 * @return {@code true} if the arrays are equal
	 */
	static boolean arraysEqual (byte[] array1, byte[] array2, int array2Offset, int array2Length) {

		return ByteBuffer.wrap (array1).equals (ByteBuffer.wrap (array2, array2Offset, array2Length));

	}


	/**
	 * Returns the node index for a given leaf number
	 *
	 * @param leafNumber The piece number
	 * @return The node index
	 */
	static int nodeIndexForLeafNumber (int leafNumber) {

		return (2 * leafNumber)- Integer.bitCount (leafNumber);

	}


	/**
	 * Returns a cached filler hash node of a given height
	 *
	 * @param height The height
	 * @return The filler node
	 */
	byte[] fillerHash (int height) {

		for (int i = this.fillerNodes.size(); i <= height; i++) {
			byte[] hash = this.fillerNodes.get (i - 1);
			this.fillerNodes.add (buildHash (hash, hash));
		}

		return this.fillerNodes.get (height);

	}


	/**
	 * Gets an immutable hash node
	 *
	 * @param nodeIndex The postorder index of the node to get
	 * @return The hash, if present, or {@code null}
	 */
	byte[] getImmutableHash (int nodeIndex) {
		if (nodeIndex >= this.hashNodes.size()) {
			return null;
		}
		return this.hashNodes.get (nodeIndex);
	}


	/**
	 * Sets an immutable hash node, expanding the immutable hash node array as required
	 *
	 * @param nodeIndex The postorder index of the node to set
	 * @param hash The hash to set
	 */
	void setImmutableNode (int nodeIndex, byte[] hash) {

		if (nodeIndex >= this.hashNodes.size()) {
			int nodesToAdd = nodeIndex - this.hashNodes.size() + 1;
			this.hashNodes.addAll (Arrays.asList(new byte[nodesToAdd][]));
		}
		this.hashNodes.set (nodeIndex, hash);

	}


	/**
	 * Builds an SHA1 hash from one or more byte arrays
	 *
	 * @param sources The data to hash
	 * @return The hash
	 */
	byte[] buildHash (byte[]... sources) {

		byte[] hash = new byte[20];

		this.digest.reset();
		for (byte[] data : sources) {
			this.digest.update (data, 0, data.length);
		}
		try {
			this.digest.digest (hash, 0, 20);
		} catch (DigestException e) {
			// Shouldn't happen
			throw new InternalError (e.getMessage());
		}

		return hash;

	}


	/**
	 * Builds an SHA1 hash from one or more byte arrays
	 *
	 * @param source The data to hash
	 * @param offset The offset within the data
	 * @param length The length within the data
	 * @return The hash
	 */
	byte[] buildHash (byte[] source, int offset, int length) {

		byte[] hash = new byte[20];

		this.digest.reset();
		this.digest.update (source, offset, length);
		try {
			this.digest.digest (hash, 0, 20);
		} catch (DigestException e) {
			// Shouldn't happen
			throw new InternalError (e.getMessage());
		}

		return hash;

	}


	/**
	 * @return the leafSize
	 */
	public int getLeafSize() {

		return this.leafSize;

	}


	/**
	 * Gets the first available hash chain for the given leaf number
	 *
	 * @param leafNumber The leaf number
	 * @param leafContentLength The length of the leaf content
	 * @return The hash chain
	 */
	public HashChain getHashChain (int leafNumber, int leafContentLength) {

		ElasticTreeView view = getCeilingView ((leafNumber * this.getLeafSize()) + leafContentLength);

		return new HashChain (view.getViewLength(), ByteBuffer.wrap (view.getHashChain (leafNumber)));

	}


	/**
	 * Gets the complete set of immutable hashes within the tree
	 *
	 * @return The concatenated hashes
	 */
	public ByteBuffer getImmutableHashes() {

		ByteBuffer buffer = ByteBuffer.allocate (this.hashNodes.size() * 20);

		for (byte[] hash : this.hashNodes) {
			if (hash != null) {
				buffer.put(hash);
			} else{
				buffer.position (buffer.position() + 20);
			}
		}
		buffer.rewind();

		return buffer;

	}


	/**
	 * Verifies a hash chain against the tree
	 *
	 * @param leafNumber The leaf number
	 * @param hashChain The hash chain
	 * @return {@code true} if the hash chain verified correctly, otherwise {@code false}
	 */
	public boolean verifyHashChain (int leafNumber, HashChain hashChain) {

		ElasticTreeView view = getView (hashChain.getViewLength());

		return view.verifyHashChain (leafNumber, hashChain.getHashes());

	}


	/**
	 * Adds a new view onto the tree
	 *
	 * @param view The view to add
	 */
	public void addView (ElasticTreeView view) {

		this.views.put (view.getViewLength(), view);

	}


	/**
	 * Adds a new view onto the tree
	 *
	 * @param viewLength The length of the view
	 * @param rootHash The root hash of the view
	 */
	public void addView (long viewLength, ByteBuffer rootHash) {

		ElasticTreeView view = new ElasticTreeView (this, viewLength, rootHash);
		this.views.put (viewLength, view);

	}


	/**
	 * Adds a new view onto the tree
	 *
	 * @param viewLength The length of the view
	 * @param leafHashes A set of leaf hashes starting after the last immutable leaf hash in the
	 *        tree 
	 */
	public void addView (long viewLength, byte[][] leafHashes) {

		ElasticTreeView view = this.views.lastEntry().getValue();
		ElasticTreeView extendedView = new ElasticTreeView (this, view, viewLength, leafHashes);

		this.views.put (viewLength, extendedView);

	}


	/**
	 * Returns the first view of equal to or greater than the given length
	 *
	 * @param viewLength The view length
	 * @return The view, if any, or {@code null}
	 */
	public ElasticTreeView getCeilingView (long viewLength) {

		return this.views.ceilingEntry(viewLength).getValue();

	}


	/**
	 * Returns the view exactly equal to the given length
	 *
	 * @param viewLength The view length
	 * @return The view, if any, or {@code null}
	 */
	public ElasticTreeView getView (long viewLength) {

		return this.views.get (viewLength);

	}


	/**
	 * @return All the tree's views
	 */
	public Set<ElasticTreeView> getAllViews() {

		return new HashSet<ElasticTreeView> (this.views.values());

	}


	/**
	 * Deletes views where a higher view could produce a hash chain for every immutable leaf within
	 * the lower view. This is heuristic rather than exhaustive
	 *
	 * @return A list of the view lengths of views that have been deleted
	 */
	public List<Long> garbageCollectViews() {

		Set<Long> viewLengths = this.views.descendingKeySet();
		List<Long> evictedViews = new LinkedList<Long>();

		if (viewLengths.size() >= 2) {
			Iterator<Long> iterator = viewLengths.iterator();
			ElasticTreeView higherView = this.views.get (iterator.next());
	
			while (iterator.hasNext()) {
				Long lowerViewLength = iterator.next();
				int lowerLeafNumber = (int)((lowerViewLength + this.leafSize - 1) / this.leafSize);
				if (higherView.canVerifyLeaf (lowerLeafNumber)) {
					iterator.remove();
					evictedViews.add (lowerViewLength);
				} else {
					higherView = this.views.get (lowerViewLength);
				}
			}
		}

		return evictedViews;

	}


	/**
	 * Common constructor
	 * 
	 * @param leafSize The leaf size
	 * @param viewLength The view length
	 */
	private ElasticTree (int leafSize, long viewLength) {

		this.leafSize = leafSize;

		int numLeaves = (int)((viewLength + this.getLeafSize() - 1) / this.getLeafSize());
		int graphHeight = 33 - Integer.numberOfLeadingZeros (numLeaves - 1);

		int rootNodeIndex = (1 << graphHeight) - 2;
		this.hashNodes = new ArrayList<byte[]>(rootNodeIndex + 1);

		// Create message digester
		try {
			this.digest = MessageDigest.getInstance ("SHA");
		} catch (NoSuchAlgorithmException e) {
			// Shouldn't happen
			throw new InternalError (e.getMessage());
		}

	}


	/**
	 * Constructs a tree with a known root hash and length
	 *
	 * @param leafSize The leaf size
	 * @param viewLength The view length
	 * @param rootHash The root hash
	 * @return The constructed tree
	 */
	public static ElasticTree emptyTree (int leafSize, long viewLength, ByteBuffer rootHash) {

		ElasticTree tree = new ElasticTree (leafSize, viewLength);

		tree.views.put (viewLength, new ElasticTreeView (tree, viewLength, rootHash));

		return tree;

	}


	/**
	 * Constructs a tree with a known root hash and length
	 *
	 * @param leafSize The leaf size
	 * @param viewLength The view length
	 * @param nodeHashes The concatenated node hashes
	 * @return The constructed tree
	 */
	public static ElasticTree withNodeHashes (int leafSize, long viewLength, byte[] nodeHashes) {

		ElasticTree tree = new ElasticTree (leafSize, viewLength);
		byte[] blankHash = new byte[20];

		// Assume that all zero hashes cannot occur and use that value to mean "not present". This
		// assumption is a scurrilous lie, but practically speaking a useful one
		for (int i = 0, offset = 0; i < nodeHashes.length / 20; i++, offset += 20) {
			byte[] hash = Arrays.copyOfRange (nodeHashes, offset, offset + 20);
			if (!arraysEqual (blankHash, hash)) {
				tree.setImmutableNode (i, hash);
			}
		}

		return tree;

	}


	/**
	 * Constructs a tree with a known leaf hash set
	 *
	 * @param leafSize The leaf size
	 * @param viewLength The view length
	 * @param hashes The leaf hashes
	 * @return The constructed tree
	 */
	public static ElasticTree buildFromLeaves (int leafSize, long viewLength, byte[][] hashes) {

		ElasticTree tree = new ElasticTree (leafSize, viewLength);

		tree.views.put (viewLength, new ElasticTreeView (tree, viewLength, hashes));

		return tree;

	}


	/**
	 * Constructs a tree with a known leaf hash set
	 *
	 * @param leafSize The leaf size
	 * @param viewLength The view length
	 * @param hashes The concatenated leaf hashes
	 * @return The constructed tree
	 */
	public static ElasticTree buildFromLeaves (int leafSize, long viewLength, byte[] hashes) {

		ElasticTree tree = new ElasticTree (leafSize, viewLength);

		byte[][] splitHashes = new byte[hashes.length / 20][];
		for (int i = 0, offset = 0; i < splitHashes.length; i++, offset += 20) {
			splitHashes[i] = Arrays.copyOfRange (hashes, offset, offset + 20);
		}

		tree.views.put (viewLength, new ElasticTreeView (tree, viewLength, splitHashes));

		return tree;

	}


}