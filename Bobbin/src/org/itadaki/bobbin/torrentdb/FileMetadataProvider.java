/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;

import java.io.File;
import java.io.IOException;

import org.itadaki.bobbin.util.CharsetUtil;



/**
 * A {@link MetadataProvider} that yields {@link FileMetadata} instances
 */
public class FileMetadataProvider implements MetadataProvider {

	/**
	 * The base directory beneath which metadata will be stored
	 */
	private final File baseDirectory;


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.MetadataProvider#metadataFor(org.itadaki.bobbin.torrentdb.InfoHash)
	 */
	public Metadata metadataFor (InfoHash infoHash) throws IOException {

		File metadataDirectory = new File (this.baseDirectory, CharsetUtil.hexencode (infoHash.getBytes()));
		return new FileMetadata (metadataDirectory);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.MetadataProvider#forget(org.itadaki.bobbin.torrentdb.InfoHash)
	 */
	public void forget (InfoHash infoHash) throws IOException {

		File metadataDirectory = new File (this.baseDirectory, CharsetUtil.hexencode (infoHash.getBytes()));

		if (metadataDirectory.exists() && metadataDirectory.isDirectory() && metadataDirectory.canRead()) {

			File[] metadataFiles = metadataDirectory.listFiles();
			for (File file : metadataFiles) {
				file.delete();
			}

			metadataDirectory.delete();

		}

	}


	/**
	 * @param baseDirectory The base directory under which to store the metadata
	 * @throws IOException On any I/O error accessing the base directory
	 */
	public FileMetadataProvider (File baseDirectory) throws IOException {

		if (baseDirectory.exists()) {
			if (!baseDirectory.isDirectory() || !baseDirectory.canRead()) {
				throw new IncompatibleLocationException ("Location '" + baseDirectory + "' is not a readable directory");
			}
		} else {
			if (!baseDirectory.mkdirs()) {
				throw new IncompatibleLocationException ("Directory '" + baseDirectory + "' could not be created");
			}
		}

		this.baseDirectory = baseDirectory;

	}


}
