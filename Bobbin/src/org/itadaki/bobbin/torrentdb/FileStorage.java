package org.itadaki.bobbin.torrentdb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.CharsetUtil;



/**
 * A {@link Storage} backed by a list of ordinary files of known length
 * 
 * <p>The files used may be nonexistent or partially existent. Neither the list of files nor their
 * declared limits may be changed after creation.
 * 
 * <p>Reads and writes are defined to behave as follows :
 * <ul>
 *   <li>Writes to a position mapping to an underlying file or files that do not yet exist, or are
 *       shorter than their declared limits, will cause those files to be created and/or extended
 *       (up to their declared limits) to a length sufficient to contain the last byte of the piece
 *       to be written.</li>
 *   <li>Reads from a position mapping to an underlying file or files that do not yet exist, or are
 *       shorter than their declared limits will be zero filled to cover the missing sections. The
 *       underlying files will neither be created nor extended.</li>
 * </ul>
 */
public class FileStorage implements Storage {

	/**
	 * The header of a FileStorage validation cookie, as used by {@link #validate(byte[])} and
	 * {@link #close()}
	 */
	private static final byte[] VALIDATION_COOKIE_HEADER = "\0FileStorage\0".getBytes (CharsetUtil.UTF8);

	/**
	 * The descriptor of the {@code Storage}'s characteristics
	 */
	private StorageDescriptor descriptor;

	/**
	 * The underlying files
	 */
	private final List<File> files = new ArrayList<File>();

	/**
	 * The declared lengths of the underlying files. May be longer than the current, actual lengths
	 * of the files in the filesystem.
	 */
	private final List<Long> fileLengths = new ArrayList<Long>();

	/**
	 * A navigable map linking linear addresses to file indices. The key is the linear byte address
	 * of the start of each file
	 */
	private final NavigableMap<Long,Integer> fileIndexMap = new TreeMap<Long,Integer>();

	/**
	 * The size of the standard piece
	 */
	private final int pieceSize;

	/**
	 * The total length of the represented data in bytes
	 */
	private long totalByteLength;

	/**
	 * An internal cache of RandomAccessFiles pointing to the underlying files
	 * TODO Optimisation - make this a size limited LRU cache 
	 */
	private List<RandomAccessFile> randomAccessFiles = new ArrayList<RandomAccessFile>();


	/**
	 * @return An opaque cookie representing the current state of the {@code FileStorage}
	 */
	private ByteBuffer buildValidationCookie() {

		ByteBuffer cookieBuffer = ByteBuffer.allocate (VALIDATION_COOKIE_HEADER.length + (this.files.size() * 8 * 2));
		cookieBuffer.put (VALIDATION_COOKIE_HEADER);
		LongBuffer longBuffer = cookieBuffer.asLongBuffer();
		for (File file : this.files) {
			if (file.exists()) {
				longBuffer.put (file.lastModified());
				longBuffer.put (file.length());
			} else {
				longBuffer.put (0L);
				longBuffer.put (0L);
			}
		}

		cookieBuffer.rewind();

		return cookieBuffer;

	}


	/**
	 * Checks that a given File is either
	 * <ul>
	 *   <li>Nonexistent, or</li>
	 *   <li>An existing, readable directory</li>
	 * </ul>
	 *
	 * @param directory The file to test
	 * @throws IncompatibleLocationException if the file is not a valid directory
	 */
	private static void checkDirectoryIsValid (File directory) throws IncompatibleLocationException {

		if (
				   directory.exists()
				&& !(directory.isDirectory() && directory.canRead())
		   )
		{
			throw new IncompatibleLocationException ("Invalid directory name: " + directory.getAbsolutePath());
		}

	}


	/**
	 * Checks that a given File is either
	 * <ul>
	 *   <li>Nonexistent, or</li>
	 *   <li>An existing, readable ordinary file</li>
	 * </ul>
	 *
	 * @param file The file to test
	 * @throws IncompatibleLocationException if the file is not a valid ordinary file
	 */
	private static void checkFileIsValid (File file) throws IncompatibleLocationException {

		if (
				   file.exists()
				&& !(file.isFile() && file.canRead())
		   )
		{
			throw new IncompatibleLocationException ("Invalid filename: " + file.getAbsolutePath());
		}

	}


	/**
	 * Checks that a given child file is strictly a child of given parent directory
	 *<p>Note: This method is used to provide defense in depth; invalid filenames are expected to be
	 *fully eliminated before it is invoked.
	 *
	 * @param parent The parent directory
	 * @param child The presumed child file
	 * @throws IncompatibleLocationException If the given child file is not strictly a child of the
	 *         parent directory
	 */
	private static void checkIsChild (File parent, File child) throws IncompatibleLocationException {

		String parentPath;
		String childPath;
		try {
			parentPath = parent.getCanonicalPath() + File.separator;
			childPath = child.getCanonicalPath();
		} catch (IOException e) {
			throw new IncompatibleLocationException (e);
		}

		if (
				(childPath.length() <= parentPath.length())
				|| !(childPath.startsWith (parentPath))
		   )
		{
			throw new IncompatibleLocationException ("The chickens are escaping!! : " + parentPath + " " + child.getPath());
		}

	}


	/**
	 * Checks that a string representing a single element of a file path is valid in the following
	 * sense:
	 * <ul>
	 *   <li>The string is not zero length</li>
	 *   <li>The string is not "." or ".."</li>
	 *   <li>The string is not the filesystem-dependent separator character</li>
	 * </ul>
	 *
	 * @param pathElement The path element to test
	 * @throws IncompatibleLocationException If the path element is invalid
	 */
	private static void checkFilePartIsValid (String pathElement) throws IncompatibleLocationException {

		if (
				   (pathElement.length() == 0)
				|| pathElement.equals (".")
				|| pathElement.equals ("..")
				|| pathElement.contains (File.separator)
		   )
		{
			throw new IncompatibleLocationException ("Invalid path element '" + pathElement + "'");
		}

	}


	/**
	 * Gets a RandomAccessFile for a given file index
	 *
	 * @param i the file index
	 * @return A RandomAccessFile for the given file index
	 * @throws IOException If an error occurred creating the requested RandomAccessFile
	 */
	private RandomAccessFile getRandomAccessFileForIndex (int i) throws IOException {

		RandomAccessFile randomAccessFile = this.randomAccessFiles.get (i);
		if (randomAccessFile == null) {
			File file = this.files.get (i);
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			randomAccessFile = new RandomAccessFile (file, "rw");
			this.randomAccessFiles.set (i, randomAccessFile);
		}

		return randomAccessFile;

	}


	/**
	 * Finds the starting file / byte index for a given linear byte index
	 *
	 * <p>Note: The byte index returned is defined as <i>the number of bytes to advance starting at
	 * the beginning of the returned file index</i>. This is in order that, when the passed byte
	 * index starts on a sequence of one or more zero length files, writes to that index will create
	 * all the intervening zero length files as a side effect.
	 *
	 * @param linearByteIndex The linear byte index to search for
	 * @return A two member array containing the file index and the number of bytes to advance
	 * @throws IndexOutOfBoundsException if the linear byte index is beyond the
	 *           end of the last file
	 */
	private long[] getFileByteIndexForLinearByteIndex (long linearByteIndex) {

		if ((linearByteIndex >= 0) && (linearByteIndex < this.totalByteLength)) {
			Entry<Long,Integer> entry = this.fileIndexMap.floorEntry (linearByteIndex);
			return new long[] { entry.getValue(), linearByteIndex - entry.getKey() };
		}

		throw new IndexOutOfBoundsException ("Invalid position " + linearByteIndex);

	}


	/* Storage interface */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#getDescriptor()
	 */
	public StorageDescriptor getDescriptor() {

		return this.descriptor;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#getFileBackedPieces()
	 */
	public BitField getStorageBackedPieces() {

		int numberOfPieces = this.descriptor.getNumberOfPieces();
		BitField fileBackedPieces = new BitField (numberOfPieces);

		if (numberOfPieces > 0) {

			int fileIndex = 0;
			long fileByteIndex = 0;
			boolean currentFileExists = this.files.get(fileIndex).exists();
			long currentActualFileLength = this.files.get(fileIndex).length();
			long currentSpecifiedFileLength = this.fileLengths.get (fileIndex);
			long fileBytesLeft = currentSpecifiedFileLength;

			for (int i = 0; i < numberOfPieces; i++) {
				boolean currentPiecePresent = true;
				int bytesToRead = this.descriptor.getPieceLength (i);

				while (bytesToRead > 0) {
					int bytesRead = (int) Math.min (bytesToRead, fileBytesLeft);
					if (!currentFileExists || (fileByteIndex + bytesRead) > currentActualFileLength) {
						currentPiecePresent = false;
					}
					fileByteIndex += bytesRead;
					bytesToRead -= bytesRead;
					fileBytesLeft -= bytesRead;
					if ((fileByteIndex == currentSpecifiedFileLength) && (fileIndex < (this.files.size() - 1))) {
						fileIndex++;
						fileByteIndex = 0;
						currentFileExists = this.files.get(fileIndex).exists();
						currentActualFileLength = this.files.get(fileIndex).length();
						currentSpecifiedFileLength = this.fileLengths.get (fileIndex);
						fileBytesLeft = currentSpecifiedFileLength;
					}
				}
				fileBackedPieces.set (i, currentPiecePresent);
			}

		}

		return fileBackedPieces;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#validate(java.nio.ByteBuffer)
	 */
	public boolean validate (ByteBuffer cookie) throws IOException {

		if (cookie == null) {
			return false;
		}

		return buildValidationCookie().equals (cookie);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#read(int)
	 */
	public ByteBuffer read (int index) throws IOException {

		if (index < 0) {
			throw new IndexOutOfBoundsException ("Invalid index " + index);
		}

		// Find the file / byte index
		long[] indices = getFileByteIndexForLinearByteIndex (((long)index) * this.pieceSize);
		int fileIndex = (int)indices[0];
		long fileByteIndex = indices[1];

		int bufferByteIndex = 0;
		int bytesLeftToRead = this.descriptor.getPieceLength (index);
		byte[] buffer = new byte[bytesLeftToRead];

		// Read fragments until complete
		while (bytesLeftToRead > 0) {

			long bytesInThisFragment = Math.max (0, this.fileLengths.get (fileIndex) - fileByteIndex);
			int bytesToRead = Math.min (bytesLeftToRead, ((int) Math.min (Integer.MAX_VALUE, bytesInThisFragment)));

			if (this.fileLengths.get (fileIndex) > 0) {
				if (this.files.get (fileIndex).exists()) {
					RandomAccessFile randomAccessFile = getRandomAccessFileForIndex (fileIndex);
					randomAccessFile.seek (fileByteIndex);
					randomAccessFile.read (buffer, bufferByteIndex, bytesToRead);
				}
				fileByteIndex = 0;
			}

			fileIndex++;
			bufferByteIndex += bytesToRead;
			bytesLeftToRead -= bytesToRead;

		}

		return ByteBuffer.wrap (buffer);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#write(int, java.nio.ByteBuffer)
	 */
	public void write (int index, ByteBuffer buffer) throws IOException {

		if (index < 0) {
			throw new IndexOutOfBoundsException ("Invalid index " + index);
		}

		// Find the file / byte index
		long[] indices = getFileByteIndexForLinearByteIndex (((long)index) * this.pieceSize);
		int fileIndex = (int)indices[0];
		long fileByteIndex = indices[1];

		int numFiles = this.files.size();

		// Write fragments until complete
		int bytesLeftToWrite = this.descriptor.getPieceLength (index);
		while (bytesLeftToWrite > 0) {

			long bytesInThisFragment = Math.max (0, this.fileLengths.get (fileIndex) - fileByteIndex);
			int bytesToWrite = Math.min (bytesLeftToWrite, ((int) Math.min (Integer.MAX_VALUE, bytesInThisFragment)));

			FileChannel channel = getRandomAccessFileForIndex(fileIndex).getChannel();

			if (this.fileLengths.get (fileIndex) == 0) {
				this.files.get (fileIndex).createNewFile();
			} else {
				channel.position (fileByteIndex);
				buffer.limit (buffer.position() + bytesToWrite);
				channel.write (buffer);
				fileByteIndex = 0;
			}

			fileIndex++;
			if (fileIndex == numFiles) {
				return;
			}

			bytesLeftToWrite -= bytesToWrite;

		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#openOutputChannel(int, int)
	 */
	public WritableByteChannel openOutputChannel (final int pieceNumber, final int offset) throws IOException {

		WritableByteChannel channel = new WritableByteChannel() {

			int fileIndex;
			long fileByteIndex;

			{
				long[] indices = getFileByteIndexForLinearByteIndex ((pieceNumber * getDescriptor().getPieceSize()) + offset);
				this.fileIndex = (int)indices[0];
				this.fileByteIndex = indices[1];
			}

			public boolean isOpen() {
				return true;
			}

			public void close() throws IOException {
			}

			public int write (ByteBuffer src) throws IOException {

				int bytesWritten = src.remaining();
				int bytesLeftToWrite = src.remaining();
				while (bytesLeftToWrite > 0) {

					long bytesInThisFragment = Math.max (0, FileStorage.this.fileLengths.get (this.fileIndex) - this.fileByteIndex);
					int bytesToWrite = Math.min (src.remaining(), ((int) Math.min (Integer.MAX_VALUE, bytesInThisFragment)));

					FileChannel channel = getRandomAccessFileForIndex(this.fileIndex).getChannel();

					if (FileStorage.this.fileLengths.get (this.fileIndex) == 0) {
						FileStorage.this.files.get (this.fileIndex).createNewFile();
					} else {
						channel.position (this.fileByteIndex);
						src.limit (src.position() + bytesToWrite);
						channel.write (src);
					}

					if (channel.position() == FileStorage.this.fileLengths.get (this.fileIndex)) {
						this.fileIndex++;
						this.fileByteIndex = 0;
					}

					bytesLeftToWrite -= bytesToWrite;

				}

				return bytesWritten;

			}

		};

		return channel;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#extend(long)
	 */
	public void extend (long length) throws IOException {

		if (length < this.totalByteLength) {
			throw new IllegalArgumentException ("Cannot extend to shorter length");
		}

		int lastFileIndex = this.files.size() - 1;
		this.fileLengths.set (lastFileIndex, this.fileLengths.get (lastFileIndex) + (length - this.totalByteLength));
		this.totalByteLength = length;
		this.descriptor = new StorageDescriptor (this.descriptor.getPieceSize(), length);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.torrentdb.Storage#close()
	 */
	public ByteBuffer close() throws IOException {

		for (RandomAccessFile randomAccessFile : this.randomAccessFiles) {
			if (randomAccessFile != null) randomAccessFile.close();
		}

		// Create validation cookie
		ByteBuffer cookie = null;
		if (this.files.size() > 0) {
			cookie = buildValidationCookie();
		}

		this.files.clear();
		this.fileLengths.clear();
		this.randomAccessFiles = null;

		return cookie;

	}


	/**
	 * Creates a FileStorage for a list of possibly nonexistent files
	 * 
	 * @param files The files to create the FileStorage for
	 * @param fileLengths The lengths of the files
	 * @param pieceSize The piece size
	 */
	public FileStorage (List<File> files, List<Long> fileLengths, int pieceSize) {

		int numFiles = files.size();
		long totalByteLength = 0;
		for (int i = 0; i < numFiles; i++) {
			File file = files.get (i);
			this.files.add (file);
			long thisFileLength = (fileLengths == null) ? file.length() : fileLengths.get(i);
			this.fileLengths.add (thisFileLength);
			if (!this.fileIndexMap.containsKey (totalByteLength))
				this.fileIndexMap.put (totalByteLength, i);
			totalByteLength += thisFileLength;
		}

		this.descriptor = new StorageDescriptor (pieceSize, totalByteLength);
		this.totalByteLength = totalByteLength;
		this.pieceSize = pieceSize;
		this.randomAccessFiles.addAll (Arrays.asList (new RandomAccessFile[files.size()]));

	}


	/**
	 * Constructs a {@code FileStorage} based on a parent directory and the files contained in an
	 * {@code Info}, checking the validity of the resulting filenames with respect to the local
	 * filesystem
	 * 
	 * @param parentDirectory The parent directory
	 * @param info The {@code Info}
	 * @return A {@code FileStorage} for the given {@code Info} that will write files beneath the
	 *         given parent directory
	 * @throws IncompatibleLocationException If any file or directory specified by the {@code Info}
	 *         cannot be written beneath the given parent directory or has an invalid name
	 */
	public static FileStorage create (File parentDirectory, Info info) throws IncompatibleLocationException {

		parentDirectory = parentDirectory.getAbsoluteFile();

		File effectiveParentDirectory = parentDirectory;
		if (info.getBaseDirectoryName() != null) {
			checkFilePartIsValid (info.getBaseDirectoryName());
			effectiveParentDirectory = new File (effectiveParentDirectory, info.getBaseDirectoryName());
			checkIsChild (parentDirectory, effectiveParentDirectory);
		}
		checkDirectoryIsValid (effectiveParentDirectory);

		// Create list of files to include in the torrent
		List<File> files = new ArrayList<File>();
		for (List<String> fileParts : info.getFilePaths()) {
			int length = fileParts.size();
			StringBuilder fileBuilder = new StringBuilder();
			for (int i = 0; i < length; i++) {
				String filePart = fileParts.get (i);
				checkFilePartIsValid (filePart);
				fileBuilder.append (filePart);
				if (i < (length - 1)) {
					fileBuilder.append (File.separator);
				}
			}
			File assembledFile = new File (effectiveParentDirectory, fileBuilder.toString());
			checkFileIsValid (assembledFile);
			checkIsChild (effectiveParentDirectory, assembledFile);
			files.add (assembledFile);
		}

		return new FileStorage (files, info.getFileLengths(), info.getPieceLength());

	}

}
