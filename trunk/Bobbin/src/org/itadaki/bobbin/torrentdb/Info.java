/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BEncoder;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.bencode.BValue;
import org.itadaki.bobbin.bencode.InvalidEncodingException;


/**
 * Contains and immutably represents the info dictionary of a torrent<br>
 * <br>
 * <b>Thread safety:</b> All public methods of this class are thread safe
 */
public final class Info {

	/**
	 * The info dictionary
	 */
	private final BDictionary dictionary;

	/**
	 * Cached value calculated from the dictionary:<br>
	 * The fileset
	 */
	private final InfoFileset fileset;

	/**
	 * Cached value calculated from the dictionary:<br>
	 * The total length of all files
	 */
	private final long totalLength;

	/**
	 * Cached value calculated from the dictionary:<br>
	 * The piece hashes
	 */
	private final byte[] pieceHashes;

	/**
	 * Cached value calculated from the dictionary:<br>
	 * The SHA1 hash of the dictionary 
	 */
	private final InfoHash hash;

	/**
	 * Cached value calculated from the dictionary:<br>
	 * The style of pieces exchanged within the torrent
	 */
	private final PieceStyle pieceStyle;


	/**
	 * Helper method for dictionary validation. Throws {@link InvalidEncodingException}
	 * with a supplied message if the supplied boolean value is false
	 *
	 * @param statement if false, {@link InvalidEncodingException} is thrown
	 * @param message a message to include in the thrown exception
	 * @throws InvalidEncodingException
	 */
	private static void assertTrue (boolean statement, String message) throws InvalidEncodingException {

		if (!statement) {
			throw new InvalidEncodingException ("Invalid torrent dictionary: " + message);
		}

	}


	/**
	 * Constructs an Info dictionary
	 *
	 * @param fileset The fileset
	 * @param pieceSize The piece size
	 * @param pieceHashes The concatenated piece hashes
	 * @param rootHash The Merkle tree root hash
	 * @param rootSignature The 40-byte, P1363 encoded DSA signature of the root hash
	 * @param elastic If {@code true}, an Elastic info is created
	 * @return The constructed Info dictionary
	 * @throws InvalidEncodingException On any validation error
	 */
	private static BDictionary buildDictionary (InfoFileset fileset, int pieceSize, byte[] pieceHashes, byte[] rootHash, byte[] rootSignature, boolean elastic)
			throws InvalidEncodingException
	{

		BDictionary dictionary = new BDictionary();

		if (fileset.isSingleFile()) {
			dictionary.put ("name", fileset.getFiles().get(0).getName().get (0));
			dictionary.put ("length", fileset.getLength());
		} else {
			dictionary.put ("name", fileset.getBaseDirectoryName());

			BList filesList = new BList();
			for (int i = 0; i < fileset.getFiles().size(); i++) {
				BDictionary fileDictionary = new BDictionary();
				BList filePath = new BList();
				for (String pathElement : fileset.getFiles().get(i).getName()) {
					filePath.add (pathElement);
				}
				fileDictionary.put ("path", filePath);
				fileDictionary.put ("length", fileset.getFiles().get(i).getLength());
				filesList.add (fileDictionary);
			}
			dictionary.put ("files", filesList);
		}

		dictionary.put ("piece length", pieceSize);

		if (pieceHashes != null) {
			dictionary.put ("pieces", pieceHashes);
		} else{
			dictionary.put ("root hash", rootHash);
			if (rootSignature != null) {
				dictionary.put ("root signature", rootSignature);
				if (elastic) {
					dictionary.put ("elastic", new byte[] { 1 });
				}
			}
		}

		return dictionary;

	}


	/**
	 * Package local: Gets the info dictionary. Used by MetaInfo in order that it and Info may
	 * share one (immutable) info dictionary
	 *
	 * @return The info dictionary
	 */
	BDictionary getPrivateDictionary() {

		return this.dictionary;

	}


	/**
	 * @return A copy of the info dictionary
	 */
	public BDictionary getDictionary() {

		return this.dictionary.clone();

	}


	/**
	 * @return The fileset
	 */
	public InfoFileset getFileset() {

		return this.fileset;

	}


	/**
	 * @return The piece size of the torrent
	 */
	public int getPieceSize() {

		return ((BInteger)this.dictionary.get("piece length")).value().intValue();

	}


	/**
	 * @return The piece hashes contained in the torrent as a concatenated byte array
	 */
	public byte[] getPieceHashes() {

		BValue piecesValue = this.dictionary.get ("pieces");
		if (piecesValue != null) {
			byte[] pieces = ((BBinary)piecesValue).value();
			return Arrays.copyOf (pieces, pieces.length);
		}

		return null;

	}


	/**
	 * @return The Merkle tree root hash, if present
	 */
	public byte[] getRootHash() {

		BValue rootHashValue = this.dictionary.get ("root hash");
		if (rootHashValue != null) {
			byte[] rootHash = ((BBinary)rootHashValue).value();
			return Arrays.copyOf (rootHash, rootHash.length);
		}

		return null;

	}


	/**
	 * @return The P1363 encoded root hash signature, if present
	 */
	public byte[] getRootSignature() {

		BValue rootSignatureValue = this.dictionary.get ("root signature");
		if (rootSignatureValue != null) {
			byte[] rootSignature = ((BBinary)rootSignatureValue).value();
			return Arrays.copyOf (rootSignature, rootSignature.length);
		}

		return null;

	}


	/**
	 * @return The style of pieces exchanged within the torrent
	 */
	public PieceStyle getPieceStyle() {

		return this.pieceStyle;

	}


	/**
	 * @return A PiecesetDescriptor appropriate for the Info
	 */
	public PiecesetDescriptor getPiecesetDescriptor() {

		return new PiecesetDescriptor (this.getPieceSize(), this.totalLength);

	}


	/**
	 * @return The SHA1 hash of this Info's dictionary
	 */
	public InfoHash getHash() {

		return this.hash;

	}


	/**
	 * Compares a supplied hash against a hash in the piece hash array
	 * @param pieceNumber The number of the piece
	 * @param hash The hash to verify for the piece
	 * @return {@code true} if the hashes were equal, otherwise {@code false}
	 * @throws NullPointerException if this is not a plain piece Info
	 * @throes IndexOutOfBoundsException if the piece number is invalid
	 */
	public boolean comparePieceHash (int pieceNumber, byte[] hash) {

		return ByteBuffer.wrap(this.pieceHashes, 20 * pieceNumber, 20).equals (ByteBuffer.wrap (hash));

	}


	/**
	 * Creates an Info from the given dictionary
	 *
	 * @param dictionary The dictionary to use
	 * @throws InvalidEncodingException if the dictionary is not a valid Info dictionary
	 */
	public Info (BDictionary dictionary) throws InvalidEncodingException {

		this.dictionary = dictionary;

		// Check: '/info/name' must be present and a non-blank binary string
		BValue nameValue = this.dictionary.get ("name");
		assertTrue (nameValue instanceof BBinary, "'name' missing or of incorrect type");
		assertTrue (((BBinary)nameValue).value().length != 0, "'name' blank");

		// Check: '/info/piece length' must be present and a positive integer
		assertTrue (this.dictionary.get ("piece length") instanceof BInteger, "'piece length' missing or of incorrect type");
		long pieceLength = ((BInteger)this.dictionary.get ("piece length")).value().longValue (); 
		assertTrue (pieceLength > 0, "'piece length' must be greater than zero");

		// Check: '/info/pieces' or '/info/root hash' must be present (length is checked later)
		BValue piecesValue = this.dictionary.get ("pieces");
		BValue rootHashValue = this.dictionary.get ("root hash");
		assertTrue ((piecesValue instanceof BBinary) ^ (rootHashValue instanceof BBinary),
				"'pieces' and/or 'root hash' missing or of incorrect type");


		// Single file or multi-file ?

		BValue lengthValue = this.dictionary.get ("length");
		BValue filesValue = this.dictionary.get ("files");
		String baseDirectoryName;
		List<Filespec> files = new ArrayList<Filespec>();

		if (lengthValue != null) {

			// Single file

			// Check: '/info/length' must be present and a non-negative integer
			assertTrue (lengthValue instanceof BInteger, "'length' of incorrect type");
			assertTrue (((BInteger)lengthValue).value().longValue () >= 0, "'length' is negative");

			// Check: '/info/files' must not be present
			assertTrue (filesValue == null, "both 'length' and 'files' present");

			baseDirectoryName = null;
			List<String> file = new ArrayList<String>();
			file.add (this.dictionary.getString ("name"));
			files.add (new Filespec (file, ((BInteger)lengthValue).value().longValue()));

		} else {

			// Multi-file

			// Check: '/info/files' must be present and a non-empty list
			assertTrue (filesValue instanceof BList, "'files' missing or of incorrect type");
			BList filesList = (BList)filesValue;
			assertTrue (filesList.size() != 0, "'files' empty");

			baseDirectoryName = this.dictionary.getString ("name");

			for (BValue filesListEntry : filesList) {

				// Check: '/info/files/*' must be a dictionary
				assertTrue (filesListEntry instanceof BDictionary, "'files' element of incorrect type");

				BDictionary fileInfo = (BDictionary)filesListEntry;

				// Check: '/info/files/*/length' must be a non-negative integer
				BValue fileLengthValue = fileInfo.get ("length");
				assertTrue (fileLengthValue instanceof BInteger, "'files' element with 'length' missing or of incorrect type");
				assertTrue (((BInteger)fileLengthValue).value().longValue () >= 0, "'files' element with negative 'length'");

				BValue filePathValue = fileInfo.get ("path");

				// Check: '/info/files/*/path' must be present and a non-empty list
				assertTrue (filePathValue instanceof BList, "'files' element with 'path' missing or of incorrect type");
				BList filePath = (BList)filePathValue;
				assertTrue (filePath.size () != 0, "'files' element with blank 'path'");

				List<String> filePathStrings = new ArrayList<String>();
				for (BValue pathElement : filePath) {
					// Check: '/info/files/*/path' must contain only non-blank binary strings
					assertTrue (pathElement instanceof BBinary, "'files' element with 'path' element of incorrect type");
					assertTrue (((BBinary)pathElement).value().length != 0, "'files' element with blank 'path' element");
					filePathStrings.add (((BBinary)pathElement).stringValue());
				}

				files.add (new Filespec (filePathStrings, ((BInteger)fileLengthValue).value().longValue()));

			}

		}

		long totalLength = 0;
		for (Filespec file : files) {
			totalLength += file.getLength();
		}

		// Check: '/info/pieces' or '/info/root hash' must be of the correct length
		if (piecesValue != null) {
			assertTrue (piecesValue instanceof BBinary, "'pieces' of incorrect type");
			int totalPieces = (totalLength == 0) ? 0 : (int)((totalLength - 1) / pieceLength) + 1;
			assertTrue ((((BBinary)piecesValue).value().length / 20) == totalPieces, "incorrect 'pieces' length");
		} else {
			assertTrue (rootHashValue != null, "'root hash' or 'pieces' missing");
			assertTrue (rootHashValue instanceof BBinary, "'root hash' of incorrect type");
			BBinary rootHash = (BBinary)rootHashValue;
			assertTrue (rootHash.value().length == 20, "incorrect 'root hash' length");
		}

		// Check: If '/info/root signature' is present it must be of the correct length
		BValue rootSignatureValue = this.dictionary.get ("root signature");
		if (rootSignatureValue != null) {
			assertTrue (rootSignatureValue instanceof BBinary, "'root signature' of incorrect type");
			BBinary rootSignature = (BBinary)rootSignatureValue;
			assertTrue (rootSignature.value().length == 40, "incorrect 'root signature' length");
		}

		// Check: If '/info/elastic' is present it must equal binary 0x01, and a root signature must
		// also be present
		BValue elasticValue = this.dictionary.get ("elastic");
		if (elasticValue != null) {
			assertTrue (elasticValue instanceof BBinary, "'elastic' of incorrect type");
			byte[] elastic = ((BBinary)elasticValue).value();
			assertTrue ((elastic.length == 1) && (elastic[0] == 0x01), "invalid 'elastic'");
			assertTrue (rootSignatureValue != null, "'root signature' missing");
		}

		this.pieceHashes = (piecesValue == null) ? null : ((BBinary)piecesValue).value();

		if (baseDirectoryName == null) {
			this.fileset = new InfoFileset (files.get (0));
		} else {
			this.fileset = new InfoFileset (baseDirectoryName, files);
		}

		this.totalLength = totalLength;

		// Calculate info hash
		try {
			MessageDigest digest = MessageDigest.getInstance ("SHA");
			this.hash = new InfoHash (digest.digest (BEncoder.encode (this.dictionary)));
		} catch (NoSuchAlgorithmException e) {
			// Shouldn't happen
			throw new InternalError (e.toString());
		}

		// Cache piece style
		if (rootHashValue != null) {
			this.pieceStyle = (elasticValue == null) ? PieceStyle.MERKLE : PieceStyle.ELASTIC;
		} else {
			this.pieceStyle = PieceStyle.PLAIN;
		}

	}


	/**
	 * @param fileset The fileset
	 * @param pieceSize The piece size
	 * @param pieceHashes The concatenated piece hashes
	 * @param rootHash The Merkle tree root hash
	 * @param rootSignature The 40-byte, P1363 encoded DSA signature of the root hash
	 * @param elastic If {@code true}, an Elastic info is created
	 * @throws InvalidEncodingException On any validation error
	 */
	private Info (InfoFileset fileset, int pieceSize, byte[] pieceHashes, byte[] rootHash, byte[] rootSignature, boolean elastic) throws InvalidEncodingException
	{

		this (buildDictionary (fileset, pieceSize, pieceHashes, rootHash, rootSignature, elastic));

	}


	/**
	 * Create a piece array Info from the given parameters
	 *
	 * @param fileset The fileset
	 * @param pieceSize The piece size
	 * @param pieceHashes The concatenated piece hashes
	 * @return The created Info
	 * @throws InvalidEncodingException if any of the passed arguments fail validation
	 */
	public static Info create (InfoFileset fileset, int pieceSize, byte[] pieceHashes) throws InvalidEncodingException {

		return new Info (fileset, pieceSize, pieceHashes, null, null, false);

	}


	/**
	 * Create a Merkle tree Info from the given parameters
	 *
	 * @param fileset The fileset
	 * @param pieceSize The piece size
	 * @param rootHash The Merkle tree root hash
	 * @return The created Info
	 * @throws InvalidEncodingException if any of the passed arguments fail validation
	 */
	public static Info createMerkle (InfoFileset fileset, int pieceSize, byte[] rootHash) throws InvalidEncodingException {

		return new Info (fileset, pieceSize, null, rootHash, null, false);

	}


	/**
	 * Create an Elastic Info from the given parameters
	 *
	 * @param fileset The fileset
	 * @param pieceSize The piece size
	 * @param rootHash The Merkle tree root hash
	 * @param rootSignature The 40-byte, P1363 encoded DSA signature of the root hash
	 * @return The created Info
	 * @throws InvalidEncodingException if any of the passed arguments fail validation
	 */
	public static Info createElastic (InfoFileset fileset, int pieceSize, byte[] rootHash, byte[] rootSignature)
			throws InvalidEncodingException
	{

		return new Info (fileset, pieceSize, null, rootHash, rootSignature, true);

	}


}
