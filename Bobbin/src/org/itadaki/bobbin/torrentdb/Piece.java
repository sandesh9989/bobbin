/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.itadaki.bobbin.util.elastictree.HashChain;


/**
 * Represents a single torrent piece
 */
public class Piece {

	/**
	 * The piece number
	 */
	private final int pieceNumber;

	/**
	 * The length of the piece
	 */
	private final int pieceLength;

	/**
	 * The piece blocks that are not yet present
	 */
	private final LinkedHashSet<BlockDescriptor> neededBlocks = new LinkedHashSet<BlockDescriptor>();

	/**
	 * The content of the piece
	 */
	private final ByteBuffer content;

	/**
	 * The view signature corresponding to the hash chain
	 */
	private ViewSignature viewSignature;

	/**
	 * The Merkle hash chain
	 */
	private HashChain hashChain;


	/**
	 * @return The piece number
	 */
	public int getPieceNumber() {

		return this.pieceNumber;

	}


	/**
	 * @return The view signature corresponding to the hash chain, if present, or {@code null}
	 */
	public ViewSignature getViewSignature() {

		return this.viewSignature;

	}


	/**
	 * @param viewSignature The view signature corresponding to the hash chain
	 */
	public void setViewSignature (ViewSignature viewSignature) {

		this.viewSignature = viewSignature;

	}


	/**
	 * @return The Merkle hash chain, if present, or {@code null}
	 */
	public HashChain getHashChain() {

		return this.hashChain;

	}


	/**
	 * @param hashChain The Merkle tree hash chain
	 */
	public void setHashChain (HashChain hashChain) {

		this.hashChain = hashChain;

	}


	/**
	 * @return A list of blocks that are not yet present
	 */
	public List<BlockDescriptor> getNeededBlocks() {

		return new ArrayList<BlockDescriptor> (this.neededBlocks);

	}


	/**
	 * @return The content of the piece
	 */
	public ByteBuffer getContent() {

		return this.content.asReadOnlyBuffer();

	}


	/**
	 * Gets a block from the piece
	 * 
	 * @param descriptor The block's descriptor
	 * @return The block
	 */
	public ByteBuffer getBlock (BlockDescriptor descriptor) {

		if (this.pieceNumber != descriptor.getPieceNumber()) {
			throw new IllegalArgumentException();
		}

		return ByteBuffer.wrap (this.content.array(), descriptor.getOffset(), descriptor.getLength()).asReadOnlyBuffer();

	}


	/**
	 * Puts a block into the piece
	 *
	 * @param descriptor The block's descriptor
	 * @param block The bytes of the block
	 * @return {@code true} if the piece has been assembled, otherwise
	 * {@code false}
	 */
	public boolean putBlock (BlockDescriptor descriptor, ByteBuffer block) {

		if (
				   (!this.neededBlocks.contains (descriptor))
				|| (block.remaining() != descriptor.getLength())
		   )
		{
			throw new IllegalArgumentException();
		}

		this.neededBlocks.remove (descriptor);
		this.content.position (descriptor.getOffset());
		this.content.put (block);
		this.content.rewind();

		return (this.neededBlocks.size() == 0);

	}


	/**
	 * Creates a fully populated piece
	 * 
	 * @param pieceNumber The piece number
	 * @param content The length of the piece
	 * @param hashChain The Merkle hash chain
	 */
	public Piece (int pieceNumber, ByteBuffer content, HashChain hashChain) {

		if ((pieceNumber < 0) || (content == null) || (content.remaining() <= 0)) {
			throw new IllegalArgumentException();
		}

		this.pieceNumber = pieceNumber;
		this.pieceLength = content.remaining();
		this.content = content;
		this.hashChain = hashChain;

	}


	/**
	 * Creates an empty piece
	 *
	 * @param pieceNumber The piece number 
	 * @param pieceLength The length of the piece
	 * @param blockLength The maximum length of the blocks to divide the piece into
	 */
	public Piece (int pieceNumber, int pieceLength, int blockLength) {

		if ((pieceNumber < 0) || (pieceLength <= 0) || (blockLength <= 0)) {
			throw new IllegalArgumentException();
		}

		this.pieceNumber = pieceNumber;
		this.pieceLength = pieceLength;
		this.content = ByteBuffer.allocate (pieceLength);
		this.hashChain = null;

		int remaining = this.pieceLength;
		int offset = 0;
		while (remaining > 0) {
			this.neededBlocks.add (new BlockDescriptor (this.pieceNumber, offset, Math.min (remaining, blockLength)));
			remaining -= blockLength;
			offset += blockLength;
		}

	}


}
