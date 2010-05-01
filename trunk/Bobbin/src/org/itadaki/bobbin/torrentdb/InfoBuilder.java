/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.PrivateKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.itadaki.bobbin.util.DSAUtil;
import org.itadaki.bobbin.util.elastictree.ElasticTree;


/**
 * Builds an Info based on existing files on disk
 */
public class InfoBuilder {

	/**
	 * The base file of the {@code Info}, which may be an ordinary file or a directory
	 */
	private final File baseFile;

	/**
	 * The piece size of the {@code Info}
	 */
	private final int pieceSize;

	/**
	 * If {@code true}, an Info with a Merkle tree "root hash" will be created; if {@code false}, an
	 * Info with a "pieces" array will be created
	 */
	private final boolean merkleTorrent;

	/**
	 * If {@code true}, an Info for the Elastic extension will be created using the given private key
	 */
	private final boolean elasticTorrent;

	/**
	 * The private key used to sign the root hash of an Elastic Info, or {@code null}
	 */
	private final PrivateKey privateKey;


	/**
	 * Checks that a given File is readable
	 *
	 * @param file The file to test
	 * @throws IncompatibleLocationException if the file is not readable
	 */
	private static void checkFileReadable (File file) throws IncompatibleLocationException {

		if (!file.canRead()) {
			throw new IncompatibleLocationException ("Unreadable file or directory: " + file.getAbsolutePath());
		}

	}


	/**
	 * Returns a lexicographically sorted list of files beneath a given location.
	 *
	 * @param baseFile A file or directory
	 * @return A lexicographically sorted list of files
	 * @throws IncompatibleLocationException if any directory entry encountered is not a readable
	 *         plain file or directory
	 */
	private static List<File> findFiles (File baseFile) throws IncompatibleLocationException {

		List<File> files = new ArrayList<File>();

		if (baseFile.isFile()) {
			files.add (baseFile);
		} else if (baseFile.isDirectory ()) {
			LinkedList<File> directories = new LinkedList<File>();
			directories.add (baseFile);
			while (!directories.isEmpty ()) {
				File directory = directories.removeFirst();
				for (File file : directory.listFiles()) {
					if (file.isFile()) {
						checkFileReadable (file);
						files.add (file);
					} else if (file.isDirectory()) {
						checkFileReadable (file);
						directories.add (file);
					} else {
						throw new IncompatibleLocationException ("Cannot add special file : " + file.getPath());
					}
				}
			}
		} else {
			throw new IncompatibleLocationException ("Cannot add special file : " + baseFile.getPath());
		}

		// Sort the file list. File's own comparator is defined to have filesystem-dependent behaviour
		Collections.sort (files, new Comparator<File>() {
			public int compare (File o1, File o2) {
				return o1.getPath().compareTo (o2.getPath());
			}
		});

		return files;

	}


	/**
	 * Calculates the piece hashes for a given Storage
	 *
	 * @param storage The Storage to calculate hashes for
	 * @return The calculated piece hashes
	 * @throws IOException If any error occurred reading from the Storage
	 */
	private static byte[] calculatePiecesHashes (Storage storage) throws IOException {

		// Create message digester
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance ("SHA");
		} catch (NoSuchAlgorithmException e) {
			// Shouldn't happen
			throw new InternalError (e.getMessage());
		}

		int numPieces = storage.getPiecesetDescriptor().getNumberOfPieces();

		// Create hashes
		byte[] pieceHashes = new byte[20 * numPieces];
		int piecePosition = 0;
		for (int i = 0; i < numPieces; i++) {
			ByteBuffer piece = storage.read (i);
			digest.update (piece);
			try {
				digest.digest (pieceHashes, piecePosition, 20);
			} catch (DigestException e) {
				// Shouldn't happen
				throw new InternalError (e.getMessage());
			}
			piecePosition += 20;
		}

		return pieceHashes;

	}


	/**
	 * Constructs the {@code Info} based on the supplied data
	 *
	 * @return A constructed {@code Info}
	 * @throws IOException 
	 */
	public Info build() throws IOException {

		// Create fileset to include in the torrent
		List<File> files = findFiles (this.baseFile);
		InfoFileset fileset;
		if (this.baseFile.isFile()) {
			fileset = new InfoFileset (new Filespec (this.baseFile.getName(), this.baseFile.length()));
		} else {
			List<Filespec> filespecs = new ArrayList<Filespec>();
			int baseLength = this.baseFile.getPath().length();
			for (File file : files) {
				List<String> pathElements = new ArrayList<String>();
				String filePath = file.getPath().substring (baseLength);
				StringTokenizer tokeniser = new StringTokenizer (filePath, File.separator);
				while (tokeniser.hasMoreElements()) {
					pathElements.add (tokeniser.nextToken());
				}
				filespecs.add (new Filespec (pathElements, file.length()));
			}
			fileset = new InfoFileset (this.baseFile.getName(), filespecs);
		}

		// Create piece hashes
		Storage storage = new FileStorage (this.baseFile.getParentFile());
		storage.open (this.pieceSize, fileset);
		byte[] pieceHashes = calculatePiecesHashes (storage);
		storage.close();

		// Create Info
		Info info;
		if (this.merkleTorrent) {
			ElasticTree elasticTree = ElasticTree.buildFromLeaves (this.pieceSize, this.baseFile.length(), pieceHashes);
			byte[] rootHash = elasticTree.getView(this.baseFile.length()).getRootHash();
			if (!this.elasticTorrent) {
				info = Info.createMerkle (fileset, this.pieceSize, rootHash);
			} else {
				byte[] rootSignature = null;
				try {
					Signature signature = Signature.getInstance ("NONEwithDSA", "SUN");
					signature.initSign (this.privateKey);
					signature.update (rootHash);
					byte[] derSignature = signature.sign();
					rootSignature = DSAUtil.derSignatureToP1363Signature (derSignature);
				} catch (Exception e) {
					throw new IOException (e);
				}
				info = Info.createElastic (fileset, this.pieceSize, rootHash, rootSignature);
			}
		} else {
			info = Info.create (fileset, this.pieceSize, pieceHashes);
		}

		return info;

	}


	/**
	 * @param baseFile The base file of the {@code Info}, which may be an ordinary file or a
	 *        directory
	 * @param pieceSize The piece size of the {@code Info}
	 * @param merkleTorrent If {@code true}, an Info with a Merkle tree "root hash" will be created;
	 *        if {@code false}, an Info with a "pieces" array will be created
	 * @param elasticTorrent If {@code true}, an Info for the Elastic extension will be created
	 *        using the given private key
	 * @param privateKey The private key used to sign the root hash of an Elastic Info, or {@code null}
	 */
	private InfoBuilder (File baseFile, int pieceSize, boolean merkleTorrent, boolean elasticTorrent, PrivateKey privateKey) {

		this.baseFile = baseFile.getAbsoluteFile();
		this.pieceSize = pieceSize;
		this.merkleTorrent = merkleTorrent;
		this.elasticTorrent = elasticTorrent;
		this.privateKey = privateKey;

	}


	/**
	 * Creates an InfoBuilder for a plain Info
	 *
	 * @param baseFile The base file of the {@code Info}, which may be an ordinary file or a
	 *        directory
	 * @param pieceSize The piece size of the {@code Info}
	 * @return The constructed InfoBuilder
	 */
	public static InfoBuilder createPlain (File baseFile, int pieceSize) {

		return new InfoBuilder (baseFile, pieceSize, false, false, null);

	}


	/**
	 * Creates an InfoBuilder for a Merkle Info
	 *
	 * @param baseFile The base file of the {@code Info}, which may be an ordinary file or a
	 *        directory
	 * @param pieceSize The piece size of the {@code Info}
	 * @return The constructed InfoBuilder
	 */
	public static InfoBuilder createMerkle (File baseFile, int pieceSize) {

		return new InfoBuilder (baseFile, pieceSize, true, false, null);

	}


	/**
	 * Creates an InfoBuilder for a Merkle Info
	 *
	 * @param baseFile The base file of the {@code Info}, which may be an ordinary file or a
	 *        directory
	 * @param pieceSize The piece size of the {@code Info}
	 * @param key The DSA private key
	 * @return The constructed InfoBuilder
	 */
	public static InfoBuilder createElastic (File baseFile, int pieceSize, PrivateKey key) {

		return new InfoBuilder (baseFile, pieceSize, true, true, key);

	}


}
