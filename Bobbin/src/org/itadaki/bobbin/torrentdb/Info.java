/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;

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
	 * The recommended base directory of the torrent file path, if any
	 */
	private String baseDirectoryName;

	/**
	 * Cached value calculated from the dictionary:<br>
	 * A list of file paths, each encoded as a list of path elements
	 */
	private List<List<String>> filePaths;

	/**
	 * Cached value calculated from the dictionary:<br>
	 * A list of file lengths
	 */
	private List<Long> fileLengths;

	/**
	 * Cached value calculated from the dictionary:<br>
	 * The total length of all files
	 */
	private long totalLength;

	/**
	 * Cached value calculated from the dictionary:<br>
	 * The SHA1 hash of the dictionary 
	 */
	private InfoHash hash;


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
	 * Checks the contents of the metainfo dictionary and caches some of the
	 * values in a more convenient form
	 *
	 * @throws InvalidEncodingException if any mandatory values are missing or
	 *         incorrect
	 */
	private void checkAndCacheValues() throws InvalidEncodingException {

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
		List<List<String>> filePaths = new ArrayList<List<String>>();
		List<Long> fileLengths = new ArrayList<Long>();

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
			filePaths.add (file);
			fileLengths.add (((BInteger)lengthValue).value().longValue());

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

				filePaths.add (filePathStrings);
				fileLengths.add (((BInteger)fileLengthValue).value().longValue());

			}

		}

		long totalLength = 0;
		for (Long length : fileLengths) {
			totalLength += length;
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


		this.baseDirectoryName = baseDirectoryName;
		this.filePaths = filePaths;
		this.fileLengths = fileLengths;
		this.totalLength = totalLength;

		// Calculate info hash
		try {
			MessageDigest digest = MessageDigest.getInstance ("SHA");
			this.hash = new InfoHash (digest.digest (BEncoder.encode (this.dictionary)));
		} catch (NoSuchAlgorithmException e) {
			// Shouldn't happen
			throw new InternalError (e.toString());
		}

	}


	/**
	 * @return The suggested base directory name of the torrent file path, if present, or null 
	 */
	public String getBaseDirectoryName() {

		return this.baseDirectoryName;

	}


	/**
	 * Package local: Gets the info dictionary. Used by MetaInfo in order that it and Info may
	 * share one (immutable) info dictionary
	 *
	 * @return The info dictionary
	 */
	BDictionary getDictionary() {

		return this.dictionary;

	}


	/**
	 * @return A list of the files contained in the torrent, represented as a
	 *         list of path elements
	 */
	public List<List<String>> getFilePaths() {

		List<List<String>> filePathsCopy = new ArrayList<List<String>>();

		for (List<String> filePath : this.filePaths) {
			filePathsCopy.add (new ArrayList<String> (filePath));
		}

		return filePathsCopy;

	}


	/**
	 * @return A list of the lengths of the files contained in the torrent
	 */
	public List<Long> getFileLengths() {
		
		List<Long> lengthsCopy = new ArrayList<Long> (this.fileLengths);

		return lengthsCopy;

	}


	/**
	 * @return The piece length of the torrent
	 */
	public int getPieceLength() {

		return ((BInteger)this.dictionary.get("piece length")).value().intValue();

	}


	/**
	 * @return The piece hashes contained in the torrent as a concatenated byte array
	 */
	public byte[] getPieces() {

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
	 * @return {@code true} if the Info represents a plain torrent, else {@code false}
	 */
	public boolean isPlain() {

		return (this.dictionary.get ("pieces") != null);

	}


	/**
	 * @return {@code true} if the Info represents a Merkle torrent, else {@code false}
	 */
	public boolean isMerkle() {

		return ((this.dictionary.get ("root hash") != null) && (this.dictionary.get ("elastic") == null));

	}


	/**
	 * @return {@code true} if the Info represents an Elastic torrent, else {@code false}
	 */
	public boolean isElastic() {

		BValue elasticValue = this.dictionary.get ("elastic");
		if (elasticValue != null) {
			byte[] elastic = ((BBinary)elasticValue).value();
			return ((elastic.length == 1) && (elastic[0] == 1));
		}

		return false;

	}


	/**
	 * @return A StorageDescriptor appropriate for the Info
	 */
	public StorageDescriptor getStorageDescriptor() {
		return new StorageDescriptor (this.getPieceLength(), this.totalLength);
	}


	/**
	 * @return The SHA1 hash of this Info's dictionary
	 */
	public InfoHash getHash() {

		return this.hash;

	}


	/**
	 * @param name The filename (single file) or suggested base directory name (multiple file)
	 * @param length The file's length (single file)
	 * @param filePaths The filenames (relative to the base directory), each
	 *                  represented as a list of path elements
	 * @param fileLengths The file lengths
	 * @param pieceSize The piece size
	 * @param pieceHashes The concatenated piece hashes
	 * @param rootHash The Merkle tree root hash
	 * @param rootSignature The 40-byte, P1363 encoded DSA signature of the root hash
	 * @param elastic If {@code true}, an Elastic info is created
	 * @throws InvalidEncodingException
	 */
	private Info (String name, Long length, List<List<String>> filePaths, List<Long> fileLengths, int pieceSize, byte[] pieceHashes,
			byte[] rootHash, byte[] rootSignature, boolean elastic) throws InvalidEncodingException
	{

		BDictionary dictionary = new BDictionary();
		dictionary.put ("name", name);
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

		if (length != null) {
			dictionary.put ("length", length);
		} else {
			if (filePaths.size() != fileLengths.size()) {
				throw new InvalidEncodingException ("Different number of file paths and file lengths");
			}
	
			BList filesList = new BList();
			for (int i = 0; i < filePaths.size(); i++) {
				BDictionary fileDictionary = new BDictionary();
				BList filePath = new BList();
				for (String pathElement : filePaths.get (i)) {
					filePath.add (pathElement);
				}
				fileDictionary.put ("path", filePath);
				fileDictionary.put ("length", fileLengths.get(i));
				filesList.add (fileDictionary);
			}
			dictionary.put ("files", filesList);
		}

		this.dictionary = dictionary;
		checkAndCacheValues();

	}


	/**
	 * Create a multi-file piece array Info from the given parameters
	 * 
	 * @param name The suggested base directory name
	 * @param filePaths The filenames (relative to the base directory), each
	 *                  represented as a list of path elements
	 * @param fileLengths The file lengths
	 * @param pieceSize The piece size
	 * @param pieceHashes The concatenated piece hashes
	 * @return The created Info
	 * @throws InvalidEncodingException if any of the passed arguments fail validation 
	 */
	public static Info createMultiFile (String name, List<List<String>> filePaths, List<Long> fileLengths, int pieceSize, byte[] pieceHashes)
			throws InvalidEncodingException
	{

		return new Info (name, null, filePaths, fileLengths, pieceSize, pieceHashes, null, null, false);

	}


	/**
	 * Create a multi-file Merkle tree Info from the given parameters
	 * 
	 * @param name The suggested base directory name
	 * @param filePaths The filenames (relative to the base directory), each
	 *                  represented as a list of path elements
	 * @param fileLengths The file lengths
	 * @param pieceSize The piece size
	 * @param rootHash The Merkle tree root hash
	 * @return The created Info
	 * @throws InvalidEncodingException if any of the passed arguments fail validation 
	 */
	public static Info createMultiFileMerkle (String name, List<List<String>> filePaths, List<Long> fileLengths, int pieceSize, byte[] rootHash)
			throws InvalidEncodingException
	{

		return new Info (name, null, filePaths, fileLengths, pieceSize, null, rootHash, null, false);

	}


	/**
	 * Create a multi-file Elastic Info from the given parameters
	 * 
	 * @param name The suggested base directory name
	 * @param filePaths The filenames (relative to the base directory), each
	 *                  represented as a list of path elements
	 * @param fileLengths The file lengths
	 * @param pieceSize The piece size
	 * @param rootHash The Merkle tree root hash
	 * @param rootSignature The 40-byte, P1363 encoded DSA signature of the root hash
	 * @return The created Info
	 * @throws InvalidEncodingException if any of the passed arguments fail validation 
	 */
	public static Info createMultiFileElastic (String name, List<List<String>> filePaths, List<Long> fileLengths, int pieceSize,
			byte[] rootHash, byte[] rootSignature) throws InvalidEncodingException
	{

		return new Info (name, null, filePaths, fileLengths, pieceSize, null, rootHash, null, true);

	}


	/**
	 * Create a single file piece array Info from the given parameters
	 * 
	 * @param name The filename
	 * @param length The file's length
	 * @param pieceSize The piece size
	 * @param pieceHashes The concatenated piece hashes
	 * @return The created Info
	 * @throws InvalidEncodingException if any of the passed arguments fail validation 
	 */
	public static Info createSingleFile (String name, long length, int pieceSize, byte[] pieceHashes) throws InvalidEncodingException {

		return new Info (name, length, null, null, pieceSize, pieceHashes, null, null, false);

	}


	/**
	 * Create a single file Merkle tree Info from the given parameters
	 * 
	 * @param name The filename
	 * @param length The file's length
	 * @param pieceSize The piece size
	 * @param rootHash The Merkle tree root hash
	 * @return The created Info
	 * @throws InvalidEncodingException if any of the passed arguments fail validation 
	 */
	public static Info createSingleFileMerkle (String name, long length, int pieceSize, byte[] rootHash) throws InvalidEncodingException {

		return new Info (name, length, null, null, pieceSize, null, rootHash, null, false);

	}


	/**
	 * Create a single file ELastic Info from the given parameters
	 *
	 * @param name The filename
	 * @param length The file's length
	 * @param pieceSize The piece size
	 * @param rootHash The Merkle tree root hash
	 * @param rootSignature The 40-byte, P1363 encoded DSA signature of the root hash
	 * @return The created Info
	 * @throws InvalidEncodingException if any of the passed arguments fail validation 
	 */
	public static Info createSingleFileElastic (String name, long length, int pieceSize, byte[] rootHash, byte[] rootSignature)
			throws InvalidEncodingException
	{

		return new Info (name, length, null, null, pieceSize, null, rootHash, rootSignature, true);

	}


	/**
	 * Creates an Info from the given dictionary
	 * 
	 * @param dictionary The dictionary to use
	 * @throws InvalidEncodingException if the dictionary is not a valid Info dictionary
	 */
	public Info (BDictionary dictionary) throws InvalidEncodingException {

		this.dictionary = dictionary;
		checkAndCacheValues();

	}


}
