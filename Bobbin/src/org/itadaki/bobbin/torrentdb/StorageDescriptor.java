package org.itadaki.bobbin.torrentdb;


/**
 * Describes the characteristics of a Storage
 */
public final class StorageDescriptor {

	/**
	 * The standard piece size of the Storage
	 */
	private final int pieceSize;

	/**
	 * The total byte length of the Storage
	 */
	private final long length;

	/**
	 * The total number of pieces in the Storage
	 */
	private final int numberOfPieces;


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return "StorageDescriptor:{" + this.pieceSize + ":" + this.length + "," + this.numberOfPieces + "}";

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.length ^ (this.length >>> 32));
		result = prime * result + this.pieceSize;

		return result;

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (Object other) {

		if ((this == other) || (other == null) || (getClass() != other.getClass())) {
			return false;
		}
		StorageDescriptor otherDescriptor = (StorageDescriptor) other;
		if ((this.pieceSize != otherDescriptor.pieceSize) || (this.length != otherDescriptor.length)) {
			return false;
		}

		return true;
	}


	/**
	 * @return The total byte length of the Storage
	 */
	public long getLength() {

		return this.length;

	}


	/**
	 * @return The standard piece size of the Storage
	 */
	public int getPieceSize() {

		return this.pieceSize;

	}


	/**
	 * Returns the length of a particular piece. All pieces are of the same length except for the
	 * final piece, which may be shorter.
	 *
	 * @param pieceNumber The piece number
	 * @return The length of the given piece
	 */
	public int getPieceLength (int pieceNumber) {

		if ((pieceNumber < 0) || (pieceNumber > (this.numberOfPieces - 1))) {
			throw new IllegalArgumentException();
		}

		if (pieceNumber == (this.numberOfPieces - 1)) {
			return getLastPieceLength();
		}

		return this.pieceSize;

	}


	/**
	 * @return The length of the last piece
	 */
	public int getLastPieceLength() {

		int remainder = (int) (this.length % this.pieceSize);
		if ((this.length == 0) || (remainder != 0)) {
			return remainder;
		}

		return this.pieceSize;

	}


	/**
	 * @return {@code true} if the last piece is of standard size, or {@code false} if it is shorter
	 */
	public boolean isRegular() {

		return ((this.length % this.pieceSize) == 0);

	}


	/**
	 * @return The total number of pieces in the Storage
	 */
	public int getNumberOfPieces() {

		return this.numberOfPieces;

	}


	/**
	 * @param pieceSize The standard piece size of the Storage
	 * @param length The total byte length of the Storage
	 */
	public StorageDescriptor (int pieceSize, long length) {

		this.pieceSize = pieceSize;
		this.length = length;
		this.numberOfPieces = (length == 0) ? 0 : (int)((length - 1) / pieceSize) + 1;

	}

}
