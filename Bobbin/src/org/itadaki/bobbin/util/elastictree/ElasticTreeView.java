package org.itadaki.bobbin.util.elastictree;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A view onto the tree of a particular length of data. Mutable nodes that may be overwritten
 * by a later view, comprising the path between the view's highest leaf and its root node, are
 * stored internally.
 */
public class ElasticTreeView {

	/**
	 * The tree that this is a view upon
	 */
	private final ElasticTree tree;

	/**
	 * The length of the view
	 */
	private final long viewLength;

	/**
	 * The number of the view leaf
	 */
	private final int viewLeafNumber;

	/**
	 * The height of the node graph
	 */
	private final int graphHeight;

	/**
	 * The graph height at which the first mutable node on the path from the view leaf to the root
	 * is located
	 */
	private final int mutableHeight;

	/**
	 * The hash nodes on the path between the view leaf and the root
	 */
	private final byte[][] viewHashNodes;


	/**
	 * A writable cursor that navigates over the view
	 */
	public class Cursor {

		/**
		 * The cursor leaf node index
		 */
		private final int cursorLeafNodeIndex;

		/**
		 * The cursor leaf number
		 */
		private final int cursorLeafNumber;

		/**
		 * The X index of the cursor's position
		 */
		private int x;

		/**
		 * The Y index of the cursor's position
		 */
		private int y;

		/**
		 * The node index at the cursor's position
		 */
		private int cursorNodeIndex;

		/**
		 * The X position of the cursor path at the cursor's y position
		 */
		private int cursorPathX;

		/**
		 * The node index of the cursor path at the cursor's y position
		 */
		private int cursorPathNodeIndex;

		/**
		 * The X position of the view path at the cursor's y position
		 */
		private int viewPathX;


		/**
		 * @return The sibling of the node under the cursor
		 */
		private byte[] getOtherSibling() {

			int siblingOffset = (1 << (this.y+1)) - 1;
			int otherSiblingX = this.x + (cursorNodeIsLeft() ? 1 : -1);
			int otherSiblingNodeIndex = this.cursorNodeIndex + (cursorNodeIsLeft() ? siblingOffset : - siblingOffset);

			if ((ElasticTreeView.this.mutableHeight <= this.y) && (otherSiblingX == this.viewPathX)) {
				return ElasticTreeView.this.viewHashNodes[this.y - ElasticTreeView.this.mutableHeight];
			} else if (otherSiblingX <= this.viewPathX) {
				return ElasticTreeView.this.tree.getImmutableHash (otherSiblingNodeIndex);
			}

			return ElasticTreeView.this.tree.fillerHash (this.y);

		}


		/**
		 * @return The sibling pair under the cursor
		 */
		private byte[][] getSiblings() {

			byte[][] siblings = new byte[2][];

			int leftSiblingX = this.x - (cursorNodeIsLeft() ? 0 : 1);
			int rightSiblingX = leftSiblingX + 1;
			int siblingOffset = (1 << (this.y+1)) - 1;
			int leftSiblingNodeIndex = this.cursorNodeIndex - (cursorNodeIsLeft() ? 0 : siblingOffset);
			int rightSiblingNodeIndex = leftSiblingNodeIndex + siblingOffset;

			if ((ElasticTreeView.this.mutableHeight <= this.y) && (leftSiblingX == this.viewPathX)) {
				siblings[0] = ElasticTreeView.this.viewHashNodes[this.y - ElasticTreeView.this.mutableHeight];
			} else if (leftSiblingX <= this.viewPathX) {
				siblings[0] = ElasticTreeView.this.tree.getImmutableHash (leftSiblingNodeIndex);
			} else {
				siblings[0] = ElasticTreeView.this.tree.fillerHash (this.y);
			}

			if ((ElasticTreeView.this.mutableHeight <= this.y) && (rightSiblingX == this.viewPathX)) {
				siblings[1] = ElasticTreeView.this.viewHashNodes[this.y - ElasticTreeView.this.mutableHeight];
			} else if (rightSiblingX <= this.viewPathX) {
				siblings[1] = ElasticTreeView.this.tree.getImmutableHash (rightSiblingNodeIndex);
			} else {
				siblings[1] = ElasticTreeView.this.tree.fillerHash (this.y);
			}

			return siblings;

		}


		/**
		 * Sets the sibling pair under the cursor
		 *
		 * @param siblings The sibling pair
		 */
		private void setSiblings (byte[][] siblings) {

			int leftSiblingX = this.x - (cursorNodeIsLeft() ? 0 : 1);
			int rightSiblingX = leftSiblingX + 1;
			int siblingOffset = (1 << (this.y+1)) - 1;
			int leftSiblingNodeIndex = this.cursorNodeIndex - (cursorNodeIsLeft() ? 0 : siblingOffset);
			int rightSiblingNodeIndex = leftSiblingNodeIndex + siblingOffset;

			if ((ElasticTreeView.this.mutableHeight <= this.y) && (leftSiblingX == this.viewPathX)) {
				ElasticTreeView.this.viewHashNodes[this.y - ElasticTreeView.this.mutableHeight] = Arrays.copyOf (siblings[0], 20);
			} else if (leftSiblingX <= this.viewPathX) {
				ElasticTreeView.this.tree.setImmutableNode (leftSiblingNodeIndex, Arrays.copyOf (siblings[0], 20));
			} else {
				if (!ByteBuffer.wrap(ElasticTreeView.this.tree.fillerHash (this.y)).equals (ByteBuffer.wrap (siblings[0]))) {
					throw new IllegalArgumentException();
				}
			}

			if ((ElasticTreeView.this.mutableHeight <= this.y) && (rightSiblingX == this.viewPathX)) {
				ElasticTreeView.this.viewHashNodes[this.y - ElasticTreeView.this.mutableHeight] = Arrays.copyOf (siblings[1], 20);
			} else if (rightSiblingX <= this.viewPathX) {
				ElasticTreeView.this.tree.setImmutableNode (rightSiblingNodeIndex, Arrays.copyOf (siblings[1], 20));
			} else {
				if (!ByteBuffer.wrap(ElasticTreeView.this.tree.fillerHash (this.y)).equals (ByteBuffer.wrap (siblings[1]))) {
					throw new IllegalArgumentException();
				}
			}

		}


		/**
		 * Puts the parent node of the sibling pair under the cursor
		 *
		 * @param hash The hash to put
		 */
		private void setParentHash (byte[] hash) {

			if (
					   (ElasticTreeView.this.mutableHeight <= (this.y + 1))
					&& (
					         (this.x == this.viewPathX)
					      || (this.x == this.viewPathX + (cursorNodeIsLeft() ? -1 : 1))
					   )
			   )
			{
				ElasticTreeView.this.viewHashNodes[this.y - ElasticTreeView.this.mutableHeight + 1] = hash;
			} else {
				int parentNodeIndex = this.cursorNodeIndex + (cursorNodeIsLeft() ? (1 << (this.y + 1)) : 1);
				ElasticTreeView.this.tree.setImmutableNode (parentNodeIndex, hash);
			}

		}


		/**
		 * @return The node index at the current cursor position
		 */
		public int nodeIndex() {

			return this.cursorNodeIndex;

		}


		/**
		 * @return {@code true} if the cursor is at the root node, otherwise {@code false}
		 */
		public boolean atRoot() {

			return (this.y == ElasticTreeView.this.graphHeight - 1);

		}


		/**
		 * @return {@code true} if the cursor is at the leaf level, otherwise {@code false}
		 */
		public boolean atLeaf() {

			return (this.y == 0);

		}


		/**
		 * @return {@code true} if the path node is a left node, otherwise {@code false}
		 */
		public boolean cursorNodeIsLeft() {

			return ((this.x & 1) == 0);

		}


		/**
		 * @return {@code true} there are sibling pairs within or partially within the view to
		 * the right of the sibling pair under the cursor, otherwise {@code false}
		 */
		public boolean isLastViewSiblingPair() {

			int leftSibling = (this.x & ~1);
			return (leftSibling + 1 >= this.viewPathX) && (leftSibling < (1 << (ElasticTreeView.this.graphHeight - this.y - 1)) - 1);

		}


		/**
		 * Move the cursor to the position of the path node on the current level
		 */
		public void goToPath() {

			this.x = this.cursorPathX;
			this.cursorNodeIndex = this.cursorPathNodeIndex;

		}


		/**
		 * Move the cursor right
		 *
		 * @param positions The number of positions to move right
		 */
		public void goRight (int positions) {

			if (this.x >= ((1 << (ElasticTreeView.this.graphHeight - this.y - 1)) - positions)) {
				throw new IllegalStateException ("Can't go right");
			}

			this.x += positions;
			this.cursorNodeIndex = ElasticTree.nodeIndexForLeafNumber (this.x << this.y) + (1 << (this.y + 1)) - 2;

		}


		/**
		 * Move the cursor up. The new cursor position will be the path node at the next
		 * highest position
		 */
		public void goPathUp() {

			if (atRoot()) {
				throw new IllegalStateException ("Already at root");
			}

			this.cursorPathNodeIndex += ((this.cursorLeafNumber & (1 << this.y)) == 0) ? (1 << (this.y + 1)) : 1;
			this.cursorNodeIndex = this.cursorPathNodeIndex;
			this.x = this.cursorPathX >>>= 1;
			this.viewPathX >>>= 1;
			this.y++;

		}


		/**
		 * Move the cursor down. The new cursor position will be the path node at the next
		 * lowest position
		 */
		public void goPathDown() {

			if (atLeaf()) {
				throw new IllegalStateException ("Already at leaf");
			}

			this.cursorPathNodeIndex -= ((this.cursorLeafNumber & (1 << (this.y - 1))) == 0) ? (1 << this.y) : 1;
			this.cursorNodeIndex = this.cursorPathNodeIndex;
			this.x = this.cursorPathX = (this.cursorPathX << 1) + (((this.cursorLeafNumber & (1 << (this.y - 1))) == 0) ? 0 : 1);
			this.viewPathX = (this.viewPathX << 1) + (((ElasticTreeView.this.viewLeafNumber & (1 << (this.y - 1))) == 0) ? 0 : 1);
			this.y--;

		}


		/**
		 * Writes the parent hash for the siblings under the current cursor position
		 */
		public void buildSiblingPairHash() {

			setParentHash (ElasticTreeView.this.tree.buildHash (getSiblings()));

		}


		/**
		 * @return The hash under the current cursor position
		 */
		public byte[] getHash() {

			byte[] hash;

			if ((ElasticTreeView.this.mutableHeight <= this.y) && (this.x == this.viewPathX)) {
				hash = ElasticTreeView.this.viewHashNodes[this.y - ElasticTreeView.this.mutableHeight];
			} else if (this.x <= this.viewPathX) {
				hash = ElasticTreeView.this.tree.getImmutableHash (this.cursorNodeIndex);
			} else {
				hash = ElasticTreeView.this.tree.fillerHash (this.y);
			}

			return Arrays.copyOf (hash, 20);

		}


		/**
		 * Sets the hash at the current cursor position
		 *
		 * @param hash The hash to set
		 */
		public void setHash (byte[] hash) {

			hash = Arrays.copyOf (hash, 20);

			if ((ElasticTreeView.this.mutableHeight <= this.y) && (this.x == this.viewPathX)) {
				ElasticTreeView.this.viewHashNodes[this.y - ElasticTreeView.this.mutableHeight] = hash;
			} else if (this.x <= this.viewPathX) {
				ElasticTreeView.this.tree.setImmutableNode (this.cursorNodeIndex, hash);
			} else {
				if (!ByteBuffer.wrap(ElasticTreeView.this.tree.fillerHash (this.y)).equals (ByteBuffer.wrap (hash))) {
					throw new IllegalArgumentException();
				}
			}

		}


		/**
		 * @param pathLeafNumber The path leaf number
		 * @param fromLeaf If {@code true}, the cursor will be positioned at the path leaf; if
		 *        {@code false}, the cursor will be positioned at the root node
		 */
		public Cursor (int pathLeafNumber, boolean fromLeaf) {

			this.cursorLeafNodeIndex = ElasticTree.nodeIndexForLeafNumber (pathLeafNumber);
			this.cursorLeafNumber = pathLeafNumber;
			this.x = fromLeaf ? pathLeafNumber : 0;
			this.y = fromLeaf ? 0 : ElasticTreeView.this.graphHeight - 1;
			this.cursorNodeIndex = fromLeaf ? this.cursorLeafNodeIndex : ElasticTreeView.this.rootNodeIndex();
			this.cursorPathX = fromLeaf ? pathLeafNumber : 0;
			this.cursorPathNodeIndex = this.cursorNodeIndex;
			this.viewPathX = fromLeaf ? ElasticTreeView.this.viewLeafNumber : 0;

		}

	}


	/**
	 * Inserts leaf hashes into the view and builds parent hashes to the root
	 *
	 * @param firstLeafNumber The leaf number of the first hash to insert
	 * @param leafHashes The hashes
	 */
	private void buildLeafHashes (int firstLeafNumber, byte[][] leafHashes) {

		int numLeaves = (this.viewLength == 0) ? 0 : this.viewLeafNumber + 1;

		if ((firstLeafNumber + leafHashes.length) != numLeaves) {
			throw new IllegalArgumentException ("Incorrect number of hashes");
		}

		Cursor cursor = new Cursor (firstLeafNumber, true);
		for (int i = 0; i < leafHashes.length; i++) {
			cursor.setHash (leafHashes[i]);
			if (i < (leafHashes.length - 1)) {
				cursor.goRight (1);
			}
		}

		cursor.goToPath();
		while (!cursor.atRoot()) {
			for (;;) {
				cursor.buildSiblingPairHash();
				if (cursor.isLastViewSiblingPair()) {
					break;
				}
				cursor.goRight(2);
			}
			cursor.goPathUp();
		}

	}


	/**
	 * @return The node index of the root node
	 */
	public int rootNodeIndex() {

		return (1 << this.graphHeight) - 2;

	}


	/**
	 * @return The root hash
	 */
	public byte[] getRootHash() {

		byte[] rootHash;
		if (this.viewLength == 0) {
			// A zero length tree has a filler hash for its root hash
			rootHash = this.tree.fillerHash (0);
		} else {
			rootHash = (this.mutableHeight < this.graphHeight) ? this.viewHashNodes[this.viewHashNodes.length - 1] : this.tree.getImmutableHash (rootNodeIndex());
		}
		return (rootHash == null) ? null : Arrays.copyOf (rootHash, 20);

	}


	/**
	 * Gets the mutable hashes of the view, if any
	 *
	 * @return The concatenated mutable hashes, or a zero length buffer if none
	 */
	public ByteBuffer getMutableHashes() {

		if (this.viewHashNodes == null) {
			return null;
		}

		ByteBuffer buffer = ByteBuffer.allocate (this.viewHashNodes.length * 20);
		for (byte[] hash : this.viewHashNodes) {
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
	 * Indicates whether the sibling and ancestor sibling pairs of the given leaf are all present
	 *
	 * @param leafNumber The leaf number
	 * @return {@code true} if the leaf can be verified, otherwise {@code false}
	 */
	public boolean canVerifyLeaf (int leafNumber) {

		Cursor cursor = new Cursor (leafNumber, true);

		for (int i = 0; !cursor.atRoot(); i += 2, cursor.goPathUp()) {
			byte[][] siblings = cursor.getSiblings();
			if ((siblings[0] == null) || (siblings[1] == null)) {
				return false;
			}
		}

		if (getRootHash() == null) {
			return false;
		}

		return true;
	}


	/**
	 * Gets the sibling hash chain for a given leaf
	 *
	 * @param leafNumber The leaf number
	 * @return The concatenated sibling hash chain
	 */
	public byte[] getHashChain (int leafNumber) {

		byte[] hashChain = new byte[(20 * this.graphHeight) + ((this.graphHeight > 1) ? 20 : 0)];
		Cursor cursor = new Cursor (leafNumber, true);

		for (int i = 0, offset = 0; !cursor.atRoot(); i += 2, cursor.goPathUp()) {
			if (i == 0) {
				byte[][] siblings = cursor.getSiblings();
				System.arraycopy (siblings[0], 0, hashChain, offset, 20);
				System.arraycopy (siblings[1], 0, hashChain, offset + 20, 20);
				offset += 40;
			} else {
				byte[] sibling = cursor.getOtherSibling();
				System.arraycopy (sibling, 0, hashChain, offset, 20);
				offset += 20;
			}
		}

		System.arraycopy (cursor.getHash(), 0, hashChain, hashChain.length - 20, 20);

		return hashChain;

	}


	/**
	 * Verifies the chain of sibling hashes starting from a given leaf, updating the tree with any
	 * missing nodes if the chain successfully validates against the root hash
	 *
	 * @param leafNumber The number of the leaf
	 * @param hashes The concatenated sibling hash chain
	 * @return {@code true} if the chain verified correctly, otherwise {@code false}
	 */
	public boolean verifyHashChain (int leafNumber, ByteBuffer hashes) {

		if (hashes.capacity() != ((20 * this.graphHeight) + ((this.graphHeight > 1) ? 20 : 0))) {
			throw new IllegalArgumentException ("Incorrect number of hashes");
		}

		// Check root hash
		hashes.position (hashes.capacity() - 20);
		if (!hashes.equals (ByteBuffer.wrap (getRootHash()))) {
			return false;
		}

		if (hashes.capacity() > 20) {

			Cursor cursor = new Cursor (leafNumber, true);
			hashes.position (0);
			ByteBuffer fullHashes = ByteBuffer.allocate (20 * ((2 * this.graphHeight) - 2));
			byte[] parentHash2 = null;
			while (!cursor.atRoot()) {
				byte[] siblingHashes = new byte[40];
				if (cursor.atLeaf()) {
					hashes.get (siblingHashes);
					fullHashes.put (siblingHashes);
				} else {
					hashes.get (siblingHashes, cursor.cursorNodeIsLeft() ? 20 : 0, 20);
					System.arraycopy (parentHash2, 0, siblingHashes, cursor.cursorNodeIsLeft() ? 0 : 20, 20);
					fullHashes.put (siblingHashes);
				}
				parentHash2 = this.tree.buildHash (siblingHashes, 0, 40);
				cursor.goPathUp();
			}

			int hashPosition = fullHashes.capacity() - (2 * 20);

			byte[] siblingHashes = new byte[40];
			while (!cursor.atLeaf()) {
				byte[] parentHash = cursor.getHash();
				fullHashes.position (hashPosition);
				fullHashes.get (siblingHashes);

				if (!ElasticTree.arraysEqual (parentHash, this.tree.buildHash (siblingHashes, 0, 40))) {
					return false;
				}

				cursor.goPathDown();
				cursor.setSiblings (new byte[][] {
						Arrays.copyOfRange (siblingHashes, 0, 20),
						Arrays.copyOfRange (siblingHashes, 20, 40)
				});
				hashPosition -= 40;
			}

		}

		return true;

	}


	/**
	 * Verifies a single leaf hash against the tree
	 *
	 * @param leafNumber The number of the leaf
	 * @param leafHash The hash to verify
	 * @return {@code true} if the hash verified correctly, or {@code false} if it did not verify
	 *         or was absent from the tree
	 */
	public boolean verifyLeafHash (int leafNumber, byte[] leafHash) {

		byte[] verifiedHash = null;

		if ((this.mutableHeight == 0) && (leafNumber == this.viewLeafNumber)) {
			verifiedHash = this.viewHashNodes[0];
		} else if (leafNumber <= this.viewLeafNumber) {
			verifiedHash = this.tree.getImmutableHash (ElasticTree.nodeIndexForLeafNumber (leafNumber));
		} else {
			verifiedHash = this.tree.fillerHash (0);
		}

		if (verifiedHash == null) {
			return false;
		}

		return ElasticTree.arraysEqual (verifiedHash, leafHash);

	}


	/**
	 * @return The view length
	 */
	public long getViewLength() {

		return this.viewLength;

	}


	/**
	 * @param tree The tree upon which this is a view
	 * @param viewLength The view length
	 */
	private ElasticTreeView (ElasticTree tree, long viewLength) {

		this.tree = tree;
		this.viewLength = viewLength;
		this.viewLeafNumber = (int)((viewLength + this.tree.getLeafSize() - 1) / this.tree.getLeafSize()) - 1;
		this.graphHeight = (viewLength == 0) ? 1 : 33 - Integer.numberOfLeadingZeros (this.viewLeafNumber);
		this.mutableHeight = ((viewLength % this.tree.getLeafSize()) > 0) ? 0 : Integer.numberOfTrailingZeros (~this.viewLeafNumber) + 1;
		this.viewHashNodes = ((this.graphHeight - this.mutableHeight) > 0) ? new byte[this.graphHeight - this.mutableHeight][] : null;

	}


	/**
	 * Constructs a view with existing mutable hashes
	 *
	 * @param tree The tree upon which this is a view
	 * @param viewLength The view length
	 * @param mutableHashes The mutable hashes
	 * @return The constructed view
	 */
	public static ElasticTreeView withMutableHashes (ElasticTree tree, long viewLength, byte[] mutableHashes) {

		ElasticTreeView view = new ElasticTreeView (tree, viewLength);
		byte[] blankHash = new byte[20];
		int expectedHashLength = (view.viewHashNodes == null) ? 0 : view.viewHashNodes.length * 20;

		if (mutableHashes.length != expectedHashLength) {
			throw new IllegalArgumentException ("Incorrect number of hashes");
		}

		// Assume that all zero hashes cannot occur and use that value to mean "not present". This
		// assumption is a scurrilous lie, but practically speaking a useful one
		if (expectedHashLength > 0) {
			for (int i = 0, offset = 0; i < view.viewHashNodes.length; i++, offset += 20) {
				byte[] hash = Arrays.copyOfRange (mutableHashes, offset, offset + 20);
				if (!ElasticTree.arraysEqual (blankHash, hash)) {
					view.viewHashNodes[i] = hash;
				}
			}
		}

		return view;

	}


	/**
	 * Creates a View with a given length and known root has
	 * 
	 * @param tree The tree upon which this is a view
	 * @param viewLength The view length
	 * @param rootHash The root hash
	 */
	public ElasticTreeView (ElasticTree tree, long viewLength, ByteBuffer rootHash) {

		this (tree, viewLength);

		byte[] rootHashCopy = new byte[20];
		rootHash.get (rootHashCopy);
		if (this.mutableHeight < this.graphHeight) {
			this.viewHashNodes[this.viewHashNodes.length - 1] = rootHashCopy;
		} else {
			this.tree.setImmutableNode (rootNodeIndex(), rootHashCopy);
		}

	}


	/**
	 * Creates a View with a given length and known leaf hash set
	 * 
	 * @param tree The tree upon which this is a view
	 * @param viewLength The view length
	 * @param leafHashes The leaf hash set
	 */
	public ElasticTreeView (ElasticTree tree, long viewLength, byte[][] leafHashes) {

		this (tree, viewLength);

		buildLeafHashes (0, leafHashes);

	}


	/**
	 * Creates a view that extends an existing view with new leaf hashes
	 *
	 * @param tree The tree upon which this is a view
	 * @param baseView The existing view
	 * @param viewLength The new view length
	 * @param leafHashes The additional leaf hashes
	 */
	public ElasticTreeView (ElasticTree tree, ElasticTreeView baseView, long viewLength, byte[][] leafHashes) {

		this (tree, viewLength);

		if (baseView.viewLength >= viewLength) {
			throw new IllegalArgumentException ("Cannot extend view to shorter length");
		}

		// Ensure the last prior immutable leaf is present
		// TODO Bug - OK as a sanity check, but insufficient
		int viewFirstLeafNumber = baseView.viewLeafNumber + ((baseView.mutableHeight == 0) ? 0 : 1);
		if ((baseView.viewLength >= this.tree.getLeafSize()) && (this.tree.getImmutableHash (ElasticTree.nodeIndexForLeafNumber (viewFirstLeafNumber - 1)) == null)) {
			throw new IllegalArgumentException ("Cannot extend view without hash chain for leaf " + (viewFirstLeafNumber - 1));
		}

		buildLeafHashes (viewFirstLeafNumber, leafHashes);

	}

}