package org.itadaki.bobbin.torrentdb;


import java.nio.ByteBuffer;


/**
 * The signature of a particular view of the piece database
 */
public class ViewSignature {

	/**
	 * The length of the signed view
	 */
	private final long viewLength;

	/**
	 * The root hash of the signed view
	 */
	private final ByteBuffer viewRootHash;

	/**
	 * The P1363 encoded DSA signature of the SHA1 hash of (info hash<20>, viewLength<8>,
	 * viewRootHash<20>)
	 */
	private final ByteBuffer signature;


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + this.signature.hashCode ();
		result = prime * result + (int) (this.viewLength ^ (this.viewLength >>> 32));
		result = prime * result + this.viewRootHash.hashCode ();

		return result;

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (Object other) {

		if (this == other)
			return true;
		if ((other == null) || (getClass() != other.getClass()))
			return false;

		ViewSignature otherSignature = (ViewSignature)other;
		if (
			   !this.signature.equals (otherSignature.signature)
			|| (this.viewLength != otherSignature.viewLength)
			|| (!this.viewRootHash.equals (otherSignature.viewRootHash))
		   )
		{
			return false;
		}

		return true;

	}


	/**
	 * @return the viewLength
	 */
	public long getViewLength() {

		return this.viewLength;

	}


	/**
	 * @return the viewRootHash
	 */
	public ByteBuffer getViewRootHash() {

		return this.viewRootHash.asReadOnlyBuffer();

	}


	/**
	 * @return the signature
	 */
	public ByteBuffer getSignature() {

		return this.signature.asReadOnlyBuffer();

	}


	/**
	 * @param viewLength The length of the signed view
	 * @param viewRootHash The root hash of the signed view
	 * @param signature The P1363 encoded DSA signature of the SHA1 hash of (info hash<20>,
	 *        viewLength<8>, viewRootHash<20>)
	 */
	public ViewSignature (long viewLength, ByteBuffer viewRootHash, ByteBuffer signature) {

		this.viewLength = viewLength;
		this.viewRootHash = viewRootHash;
		this.signature = signature;

	}


}
