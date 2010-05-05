/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.torrentdb;


import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.itadaki.bobbin.torrentdb.Filespec;
import org.itadaki.bobbin.torrentdb.Info;
import org.itadaki.bobbin.torrentdb.InfoFileset;
import org.itadaki.bobbin.torrentdb.MemoryStorage;
import org.itadaki.bobbin.torrentdb.Piece;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.util.DSAUtil;
import org.itadaki.bobbin.util.elastictree.ElasticTree;
import org.itadaki.bobbin.util.elastictree.HashChain;

import test.Util;


/**
 * Creates mock PieceDatabase instances
 */
public class MockPieceDatabase {

	/**
	 * Pregenerated private key bytes for testing use
	 */
	private static final byte[] mockPrivateKeyBytes = new byte[] { 48, -126, 1, 75, 2, 1, 0, 48, -126, 1, 44, 6, 7, 42, -122, 72, -50, 56, 4, 1, 48, -126, 1, 31, 2, -127, -127, 0, -3, 127, 83, -127, 29, 117, 18, 41, 82, -33, 74, -100, 46, -20, -28, -25, -10, 17, -73, 82, 60, -17, 68, 0, -61, 30, 63, -128, -74, 81, 38, 105, 69, 93, 64, 34, 81, -5, 89, 61, -115, 88, -6, -65, -59, -11, -70, 48, -10, -53, -101, 85, 108, -41, -127, 59, -128, 29, 52, 111, -14, 102, 96, -73, 107, -103, 80, -91, -92, -97, -97, -24, 4, 123, 16, 34, -62, 79, -69, -87, -41, -2, -73, -58, 27, -8, 59, 87, -25, -58, -88, -90, 21, 15, 4, -5, -125, -10, -45, -59, 30, -61, 2, 53, 84, 19, 90, 22, -111, 50, -10, 117, -13, -82, 43, 97, -41, 42, -17, -14, 34, 3, 25, -99, -47, 72, 1, -57, 2, 21, 0, -105, 96, 80, -113, 21, 35, 11, -52, -78, -110, -71, -126, -94, -21, -124, 11, -16, 88, 28, -11, 2, -127, -127, 0, -9, -31, -96, -123, -42, -101, 61, -34, -53, -68, -85, 92, 54, -72, 87, -71, 121, -108, -81, -69, -6, 58, -22, -126, -7, 87, 76, 11, 61, 7, -126, 103, 81, 89, 87, -114, -70, -44, 89, 79, -26, 113, 7, 16, -127, -128, -76, 73, 22, 113, 35, -24, 76, 40, 22, 19, -73, -49, 9, 50, -116, -56, -90, -31, 60, 22, 122, -117, 84, 124, -115, 40, -32, -93, -82, 30, 43, -77, -90, 117, -111, 110, -93, 127, 11, -6, 33, 53, 98, -15, -5, 98, 122, 1, 36, 59, -52, -92, -15, -66, -88, 81, -112, -119, -88, -125, -33, -31, 90, -27, -97, 6, -110, -117, 102, 94, -128, 123, 85, 37, 100, 1, 76, 59, -2, -49, 73, 42, 4, 22, 2, 20, 71, 83, 120, 66, -86, 51, 11, 8, -80, 5, 4, -65, -5, -33, -8, -120, -103, -4, 121, 64 }; 

	/**
	 * Pregenerated public key bytes for testing use
	 */
	private static final byte[] mockPublicKeyBytes = new byte[] { 	48, -126, 1, -73, 48, -126, 1, 44, 6, 7, 42, -122, 72, -50, 56, 4, 1, 48, -126, 1, 31, 2, -127, -127, 0, -3, 127, 83, -127, 29, 117, 18, 41, 82, -33, 74, -100, 46, -20, -28, -25, -10, 17, -73, 82, 60, -17, 68, 0, -61, 30, 63, -128, -74, 81, 38, 105, 69, 93, 64, 34, 81, -5, 89, 61, -115, 88, -6, -65, -59, -11, -70, 48, -10, -53, -101, 85, 108, -41, -127, 59, -128, 29, 52, 111, -14, 102, 96, -73, 107, -103, 80, -91, -92, -97, -97, -24, 4, 123, 16, 34, -62, 79, -69, -87, -41, -2, -73, -58, 27, -8, 59, 87, -25, -58, -88, -90, 21, 15, 4, -5, -125, -10, -45, -59, 30, -61, 2, 53, 84, 19, 90, 22, -111, 50, -10, 117, -13, -82, 43, 97, -41, 42, -17, -14, 34, 3, 25, -99, -47, 72, 1, -57, 2, 21, 0, -105, 96, 80, -113, 21, 35, 11, -52, -78, -110, -71, -126, -94, -21, -124, 11, -16, 88, 28, -11, 2, -127, -127, 0, -9, -31, -96, -123, -42, -101, 61, -34, -53, -68, -85, 92, 54, -72, 87, -71, 121, -108, -81, -69, -6, 58, -22, -126, -7, 87, 76, 11, 61, 7, -126, 103, 81, 89, 87, -114, -70, -44, 89, 79, -26, 113, 7, 16, -127, -128, -76, 73, 22, 113, 35, -24, 76, 40, 22, 19, -73, -49, 9, 50, -116, -56, -90, -31, 60, 22, 122, -117, 84, 124, -115, 40, -32, -93, -82, 30, 43, -77, -90, 117, -111, 110, -93, 127, 11, -6, 33, 53, 98, -15, -5, 98, 122, 1, 36, 59, -52, -92, -15, -66, -88, 81, -112, -119, -88, -125, -33, -31, 90, -27, -97, 6, -110, -117, 102, 94, -128, 123, 85, 37, 100, 1, 76, 59, -2, -49, 73, 42, 3, -127, -124, 0, 2, -127, -128, 11, -86, -78, -80, 76, -47, -12, -96, 30, -70, 73, -71, -75, -93, -29, -7, -49, -20, -24, 69, -29, 31, 84, -126, 21, -115, -66, 114, -72, 81, 14, -7, -23, 26, -59, 95, -34, 86, -43, 5, -108, 23, 61, 0, -125, 33, -27, 83, 102, 76, -2, 126, -99, -111, -12, -77, -18, 2, -57, 4, 104, -61, -60, 26, 69, 87, 108, 15, 17, 35, 70, 1, 114, -4, -43, -92, -12, -39, 51, 44, -54, -111, 24, 27, 89, 105, 96, 26, -37, -12, 61, 126, -3, -17, 36, 47, -74, 65, -43, -54, -23, -103, 59, 97, -73, -33, -108, -68, 5, 73, 24, 61, -43, 120, 72, -95, 101, -125, 5, -52, -119, 54, -88, -95, 51, -82, -16, 19 };

	/**
	 * Pregenerated private key for testing use
	 */
	public static final PrivateKey mockPrivateKey;

	/**
	 * Pregenerated public key for testing use
	 */
	public static final PublicKey mockPublicKey;

	static {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance ("DSA", "SUN");
			mockPrivateKey = keyFactory.generatePrivate (new PKCS8EncodedKeySpec (MockPieceDatabase.mockPrivateKeyBytes));
			mockPublicKey = keyFactory.generatePublic (new X509EncodedKeySpec (MockPieceDatabase.mockPublicKeyBytes));
		} catch (GeneralSecurityException e) {
			throw new InternalError (e.getMessage());
		}
	}


	/**
	 * Creates a MemoryStorage with repeatable pseudo-random content
	 *
	 * @param piecesPresent
	 * @param pieceSize
	 * @return
	 * @throws Exception
	 */
	private static MemoryStorage pseudoRandomStorage (String piecesPresent, int pieceSize) throws Exception {

		int numPieces = piecesPresent.length();

		ByteBuffer data = ByteBuffer.allocate (numPieces * pieceSize);
		long position = 0;
		int index = 0;
		for (char c : piecesPresent.toCharArray()) {
			switch (c) {
				case '0':
					data.position (data.position() + pieceSize);
					break;
				case '1':
					data.put (Util.pseudoRandomBlock (index, pieceSize, pieceSize));
					break;
				default:
					throw new Exception();
			}
			index++;
			position += 16384;
		}

		MemoryStorage storage = new MemoryStorage (data.array());

		return storage;

	}


	/**
	 * Creates an PieceDatabase of pseudo-random data based on a string that represents the pieces
	 * that should be present
	 *
	 * @param piecesPresent The pieces that should be present - a list of "0" and "1" characters
	 * @param pieceSize The piece length
	 * @return The created PieceDatabase
	 * @throws Exception
	 */
	public static PieceDatabase create (String piecesPresent, int pieceSize) throws Exception {

		int numPieces = piecesPresent.length();

		MemoryStorage storage = pseudoRandomStorage (piecesPresent, pieceSize);

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (pieceSize, numPieces * pieceSize);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.create (new InfoFileset (new Filespec ("test", (long)(numPieces * pieceSize))), pieceSize, pieceHashes);

		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, null);

		return pieceDatabase;

	}


	/**
	 * Creates an empty Merkle database with a root hash representing pseudo random data
	 *
	 * @param pieceSize The piece length
	 * @param totalLength The total length
	 * @param rootHash The root hash
	 * @return The created PieceDatabase
	 * @throws Exception
	 */
	public static PieceDatabase createEmptyMerkle (int pieceSize, int totalLength, byte[] rootHash) throws Exception {

		MemoryStorage storage = new MemoryStorage();
		Info info = Info.createMerkle (new InfoFileset (new Filespec ("test", (long)totalLength)), pieceSize, rootHash);
		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, null);

		return pieceDatabase;

	}


	/**
	 * Creates an Merkle PieceDatabase of pseudo-random data based on a string that represents the
	 * pieces that should be present
	 *
	 * @param piecesPresent The pieces that should be present - a list of "0" and "1" characters
	 * @param pieceSize The piece length
	 * @return The created PieceDatabase
	 * @throws Exception
	 */
	public static PieceDatabase createMerkle (String piecesPresent, int pieceSize) throws Exception {

		int numPieces = piecesPresent.length();
		int totalLength = pieceSize * numPieces;

		MemoryStorage storage = pseudoRandomStorage (piecesPresent, pieceSize);
		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		Info info = Info.createMerkle (
				new InfoFileset (new Filespec ("test", (long)totalLength)), pieceSize, tree.getView (totalLength).getRootHash()
		);

		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, null);

		return pieceDatabase;

	}


	/**
	 * Creates an Elastic PieceDatabase of pseudo-random data based on a string that represents the
	 * pieces that should be present
	 *
	 * @param piecesPresent The pieces that should be present - a list of "0" and "1" characters
	 * @param pieceSize The piece length
	 * @return The created PieceDatabase
	 * @throws Exception
	 */
	public static PieceDatabase createElastic (String piecesPresent, int pieceSize) throws Exception {

		int numPieces = piecesPresent.length();
		int totalLength = pieceSize * numPieces;

		MemoryStorage storage = pseudoRandomStorage (piecesPresent, pieceSize);
		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		byte[] derSignature = null;
		try {
			Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
			dsa.initSign (MockPieceDatabase.mockPrivateKey);
			dsa.update (tree.getView(totalLength).getRootHash());
			derSignature = dsa.sign();
		} catch (GeneralSecurityException e) {
			throw new InternalError (e.getMessage());
		}

		Info info = Info.createElastic (
				new InfoFileset (new Filespec ("test", (long)totalLength)),
				pieceSize,
				tree.getView (totalLength).getRootHash(),
				DSAUtil.derSignatureToP1363Signature (derSignature)
		);

		PieceDatabase pieceDatabase = new PieceDatabase (info, mockPublicKey, storage, null);

		pieceDatabase.start (true);
		for (int i = 0; i < piecesPresent.length(); i++) {
			pieceDatabase.writePiece (new Piece (i, storage.read (i), new HashChain (totalLength, ByteBuffer.wrap (tree.getView(totalLength).getHashChain (i)))));
		}

		return pieceDatabase;

	}


	/**
	 * Not instantiable
	 */
	private MockPieceDatabase() {

	}

}
