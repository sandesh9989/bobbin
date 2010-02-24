package org.itadaki.bobbin.torrentdb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * A {@link Metadata} that stores data in ordinary files
 */
public class FileMetadata implements Metadata {

	/**
	 * The directory in which the metadata will be stored
	 */
	private final File metadataDirectory;


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Metadata#put(java.lang.String, byte[])
	 */
	public void put (String key, byte[] value) throws IOException {

		File file = new File (this.metadataDirectory, key);
		file.delete();
		if (value != null) {
			RandomAccessFile randomAccessFile = new RandomAccessFile (file, "rw");
			randomAccessFile.write (value);
			randomAccessFile.close();
		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Metadata#get(java.lang.String)
	 */
	public byte[] get (String key) throws IOException {

		File file = new File (this.metadataDirectory, key);

		if (file.exists()) {
			RandomAccessFile randomAccessFile = new RandomAccessFile (file, "r");
			byte[] value = new byte [(int) file.length()];
			randomAccessFile.readFully (value);
			randomAccessFile.close();

			return value;
		}

		return null;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Metadata#close()
	 */
	public void close() {

		// Do nothing

	}


	/**
	 * @param metadataDirectory The directory in which the metadata will be stored
	 * @throws IOException On any I/O excepton accessing the directory
	 */
	public FileMetadata (File metadataDirectory) throws IOException {

		if (metadataDirectory.exists()) {
			if (!metadataDirectory.isDirectory() || !metadataDirectory.canRead()) {
				throw new IncompatibleLocationException ("Location '" + metadataDirectory + "' is not a readable directory");
			}
		} else {
			if (!metadataDirectory.mkdirs()) {
				throw new IncompatibleLocationException ("Directory '" + metadataDirectory + "' could not be created");
			}
		}

		this.metadataDirectory = metadataDirectory;

	}


}
