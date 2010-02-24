package org.itadaki.bobbin.torrentdb;

/**
 * Describes a block forming part of a torrent piece
 */
public final class BlockDescriptor {

	/**
	 * The index of the piece
	 */
	private final int pieceIndex;

	/**
	 * The offset of the block within the piece
	 */
	private final int offset;

	/**
	 * The length of the block
	 */
	private final int length;


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return "BlockDescriptor:{" + this.pieceIndex + ":" + this.offset + "," + this.length + "}";

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (Object object) {

		if (this == object) return true;
		if (!(object instanceof BlockDescriptor)) return false;

		BlockDescriptor other = (BlockDescriptor)object;

		if (
				   (other.pieceIndex == this.pieceIndex)
				&& (other.offset == this.offset)
				&& (other.length == this.length)
		   )
		{
			return true;
		}

		return false;

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		return ((this.pieceIndex << 4) + (this.offset >> 16));

	}


	/**
	 * @return the pieceIndex
	 */
	public int getPieceNumber() {

		return this.pieceIndex;

	}


	/**
	 * @return the offset
	 */
	public int getOffset() {

		return this.offset;

	}


	/**
	 * @return the length
	 */
	public int getLength() {

		return this.length;

	}

	
	/**
	 * @param pieceIndex The index of the piece
	 * @param offset The offset of the block within the piece
	 * @param length The length of the block 
	 */
	public BlockDescriptor (int pieceIndex, int offset, int length) {

		this.pieceIndex = pieceIndex;
		this.offset = offset;
		this.length = length;

	}


}