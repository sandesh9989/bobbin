/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util;

import java.nio.ByteBuffer;


/**
 * Utility functions for DSA signatures
 */
public class DSAUtil {

	/**
	 * Converts a variable length DER encoded DSA signature to a P1363 40-byte DSA signature
	 *
	 * @param derSignature The DER encoded DSA signature
	 * @return The P1363 encoded signature
	 */
	public static byte[] derSignatureToP1363Signature (byte[] derSignature) {

		byte[] p1363Signature = new byte[40];

		int rOffset = (derSignature[4] == 0) ? 5 : 4;
		int rLength = derSignature[3] - ((derSignature[4] == 0) ? 1 : 0);

		int sBase = rOffset + rLength;
		int sOffset = sBase + ((derSignature[sBase + 2] == 0) ? 3 : 2);
		int sLength = (derSignature[sBase + 1] - ((derSignature[sBase + 2] == 0) ? 1 : 0));

		System.arraycopy (derSignature, rOffset, p1363Signature, 20 - rLength, rLength);
		System.arraycopy (derSignature, sOffset, p1363Signature, 40 - sLength, sLength);

		return p1363Signature;

	}


	/**
	 * Converts a P1363 40-byte DSA signature to a variable length DER encoded DSA signature
	 *
	 * @param p1363Signature The P1363 encoded DSA signature
	 * @return The DER encoded signature, or {@code null} if the input signature was invalid
	 */
	public static ByteBuffer p1363SignatureToDerSignature (ByteBuffer p1363Signature) {

		byte[] p1363SignatureBytes = new byte[p1363Signature.remaining()];
		p1363Signature.get (p1363SignatureBytes);

		byte rLength = 20;
		for (int i = 0; (i < 20) && (p1363SignatureBytes[i] == 0); i++) {
			rLength--;
		}

		byte sLength = 20;
		for (int i = 20; (i < 40) && (p1363SignatureBytes[i] == 0); i++) {
			sLength--;
		}

		if ((rLength == 0) || (sLength == 0)) {
			return null;
		}

		boolean rLeadingZero = (p1363SignatureBytes[20 - rLength] < 0);
		boolean sLeadingZero = (p1363SignatureBytes[40 - sLength] < 0);

		int derLength = 6 + rLength + sLength + (rLeadingZero ? 1 : 0) + (sLeadingZero ? 1 : 0);

		byte[] derSignature = new byte[derLength];
		int i = 0;
		derSignature[i++] = 48;
		derSignature[i++] = (byte)(derLength - 2);
		derSignature[i++] = (byte)2;
		derSignature[i++] = (byte)(rLength + (rLeadingZero ? 1 : 0));
		if (rLeadingZero) i++;
		System.arraycopy (p1363SignatureBytes, 20 - rLength, derSignature, i, rLength);
		i += rLength;
		derSignature[i++] = (byte)2;
		derSignature[i++] = (byte)(sLength + (sLeadingZero ? 1 : 0));
		if (sLeadingZero) i++;
		System.arraycopy (p1363SignatureBytes, 40 - sLength, derSignature, i, sLength);

		return ByteBuffer.wrap (derSignature);

	}

}
