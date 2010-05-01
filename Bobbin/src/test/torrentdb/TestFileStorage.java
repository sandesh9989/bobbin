/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.torrentdb;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.itadaki.bobbin.torrentdb.FileStorage;
import org.itadaki.bobbin.torrentdb.Filespec;
import org.itadaki.bobbin.torrentdb.Info;
import org.itadaki.bobbin.torrentdb.InfoFileset;
import org.itadaki.bobbin.torrentdb.PiecesetDescriptor;
import org.itadaki.bobbin.util.BitField;
import org.junit.Test;

import test.Util;


/**
 * Tests FileStorage
 */
public class TestFileStorage {

	/**
	 * Reads a block from a FileStorage and checks that an IndexOutOfBoundsException is thrown
	 *
	 * @param storage The FileStorage to read from
	 * @param index The index of the block to read
	 * @throws IOException
	 */
	private void expectReadOutOfBounds (FileStorage storage, int index) throws IOException {

		boolean exceptionCaught;
		exceptionCaught = false;
		try {
			storage.read (index);
		} catch (IndexOutOfBoundsException e) {
			exceptionCaught = true;
		}
		assertTrue (exceptionCaught);

	}


	/**
	 * Writes a block to a FileStorage and checks that an IndexOutOfBoundsException is thrown
	 *
	 * @param storage The FileStorage to write to
	 * @param index The index of the block to write
	 * @param buffer A buffer containing the data to attempt to write
	 * @throws IOException
	 */
	private void expectWriteOutOfBounds (FileStorage storage, int index, ByteBuffer buffer) throws IOException {

		boolean exceptionCaught;
		exceptionCaught = false;
		try {
			storage.write (index, buffer);
		} catch (IndexOutOfBoundsException e) {
			exceptionCaught = true;
		}
		assertTrue (exceptionCaught);

	}


	/**
	 * A test delegate that performs a standard write -> read test on a given list of underlying file lengths
	 *
	 * @param fileLengths The file lengths to test
	 * @param pieceSize The piece size to use
	 * @throws Exception
	 */
	private void standardLinearWriteReadTestDelegate (List<Long> fileLengths, int pieceSize) throws Exception {

		List<Filespec> files = new ArrayList<Filespec>();
		long totalByteLength = 0;
		for (int i = 0; i < fileLengths.size (); i++) {
			files.add (new Filespec ("test" + i + ".tmp", fileLengths.get (i)));
			totalByteLength += fileLengths.get (i);
		}
		int numPieces = (int)Math.ceil ((double)totalByteLength / pieceSize);
		int numWholePieces = (int)Math.floor ((double)totalByteLength / pieceSize);
		int finalPieceLength = (int)(totalByteLength % pieceSize);

		File baseDirectory = Util.createNonExistentTemporaryFile();
		InfoFileset infoFileset = new InfoFileset (baseDirectory.getName(), files);
		FileStorage storage = new FileStorage (baseDirectory.getParentFile());
		storage.open (pieceSize, infoFileset);

		ByteBuffer buffer = ByteBuffer.allocate (pieceSize);


		// Check that reads / writes below and above the bounds of the FileStorage are rejected
		expectReadOutOfBounds (storage, -1);
		expectReadOutOfBounds (storage, numPieces);
		expectWriteOutOfBounds (storage, -1, buffer);
		expectWriteOutOfBounds (storage, numPieces, buffer);

		// Write whole pieces to the end of the FileStorage
		ByteBuffer[] wholePieces = new ByteBuffer[numPieces];
		for (int i = 0; i < numPieces; i++) {
			wholePieces[i] = ByteBuffer.wrap (Util.pseudoRandomBlock (i, pieceSize, pieceSize));
			storage.write (i, wholePieces[i]);
			wholePieces[i].rewind();
		}

		// Check the file sizes after all writes are complete
		for (int i = 0; i < fileLengths.size (); i++) {
			assertEquals (fileLengths.get(i).longValue(), files.get(i).getLength().longValue());
		}

		// Check that the whole pieces have been written correctly
		for (int i = 0; i < numWholePieces; i++) {
			ByteBuffer readPiece = storage.read (i);
			assertEquals (wholePieces[i], readPiece);
		}

		// Check that the final short piece, if present, has been written correctly
		if (finalPieceLength > 0) {
			ByteBuffer partialFinalPiece = ByteBuffer.wrap (Util.pseudoRandomBlock (numPieces - 1, finalPieceLength, finalPieceLength));
			ByteBuffer readPiece = storage.read (numPieces - 1);
			assertEquals (partialFinalPiece, readPiece);
		}

	}


	// A set of tests to check the behaviour of a write -> read cycle on a FileStorage of all new files

	/**
	 * New file x 0
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile0() throws Exception {

		int pieceSize = 1024;
		InfoFileset fileset = new InfoFileset ("test", new ArrayList<Filespec>());

		FileStorage storage = new FileStorage (Util.createTemporaryDirectory());
		storage.open (pieceSize, fileset);

		ByteBuffer buffer = ByteBuffer.allocate (1024);

		expectReadOutOfBounds (storage, -1);
		expectReadOutOfBounds (storage, 0);
		expectReadOutOfBounds (storage, 1);
		expectWriteOutOfBounds (storage, -1, buffer);
		expectWriteOutOfBounds (storage, 0, buffer);
		expectWriteOutOfBounds (storage, 1, buffer);

	}


	/**
	 * New file x 1, 0 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile1_0() throws Exception {

		int pieceSize = 1024;

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 0L));
		File baseFile = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseFile);
		storage.open (pieceSize, fileset);

		ByteBuffer buffer = ByteBuffer.allocate (1024);

		expectReadOutOfBounds (storage, -1);
		expectReadOutOfBounds (storage, 0);
		expectReadOutOfBounds (storage, 1);
		expectWriteOutOfBounds (storage, -1, buffer);
		expectWriteOutOfBounds (storage, 0, buffer);
		expectWriteOutOfBounds (storage, 1, buffer);

		assertFalse (new File (baseFile, "blah").exists());

	}


	/**
	 * New file x 1, 0.5 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile1_0p5() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {456L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 1, 1 piece
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile1_1() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {1024L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 1, 1.5 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile1_1p5() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {1024L + 456L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	
	/**
	 * New file x 1, 10 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile1_10() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {10 * 1024L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 2, 0 0 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile2_0_0() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {0L, 0L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 2, 0 0.5 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile2_0_0p5() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {0L, 456L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 2, 0 1 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile2_0_1() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {0L, 1024L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 2, 0.5 0 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile2_0p5_0() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {456L, 0L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 2, 0.5 0.5 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile2_0p5_0p5() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {456L, 456L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 2, 0.5 1 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile2_0p5_1() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {456L, 1024L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 2, 1 0 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile2_1_0() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {1024L, 0L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 2, 1 0.5 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile2_1_0p5() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {1024L, 456L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 2, 1 1 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile2_1_1() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {1024L, 1024L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file x 2, 5 10 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFile2_5_10() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {5 * 1024L, 10 * 1024L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file, 0 0.5 1 0.5 0 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFileX_0_0p5_1_0p5_0() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {0L, 456L, 1024L, 456L, 0L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file, 1 1 1 1 0 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFileX_1_1_1_1_0() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {1024L, 1024L, 1024L, 1024L, 0L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	/**
	 * New file, 0.5, 30.5, 1.5, 20, 0, 0.5 pieces
	 *
	 * @throws Exception 
	 */
	@Test
	public void testNewFileX_0p5_30p5_1p5_20_0_0p5() throws Exception {

		List<Long> fileLengths = Arrays.asList (new Long[] {456L, (30 * 1024L) + 456L, 1024L + 456L, (20 * 1024L), 0L, 456L});
		int pieceSize = 1024;

		standardLinearWriteReadTestDelegate (fileLengths, pieceSize);

	}


	// A set of tests that reads through to short or nonexistent underlying files provide the expected semantics

	/**
	 * Read on nonexistent underlying file should not create the file
	 *
	 * @throws Exception
	 */
	@Test
	public void testReadDoesNotCreateFile() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1024L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();
		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		storage.read (0);

		assertFalse (new File (baseDirectory, "blah").exists());

	}


	/**
	 * Read on existent but short underlying file should not extend the file
	 *
	 * @throws Exception
	 */
	@Test
	public void testReadDoesNotExtendFile() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1024L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();
		File file = new File (baseDirectory, "blah");
		new RandomAccessFile(file, "rw").setLength (456);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		storage.read (0);

		assertEquals (456L, file.length());

	}


	/**
	 * Read on nonexistent underlying file should zero fill the missing portion
	 *
	 * @throws Exception
	 */
	@Test
	public void testReadOfNonExistentFileZeroExtends() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1024L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		ByteBuffer buffer = storage.read (0);

		ByteBuffer expectedBuffer = ByteBuffer.allocate (1024);
		assertEquals (expectedBuffer, buffer);

	}


	/**
	 * Read on short underlying file should zero fill the missing portion
	 *
	 * @throws Exception
	 */
	@Test
	public void testReadOfShortFileZeroExtends() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1024L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();
		File file = new File (baseDirectory, "blah");
		new RandomAccessFile(file, "rw").setLength (456);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		ByteBuffer buffer = storage.read (0);

		ByteBuffer expectedBuffer = ByteBuffer.allocate (1024);
		assertEquals (expectedBuffer, buffer);

	}

	/**
	 * Write on nonexistent underlying file should create the file including its subdirectories
	 *
	 * @throws Exception
	 */
	@Test
	public void testWriteCreatesFileWithSubdirectories() throws Exception {

		InfoFileset fileset = new InfoFileset ("base", Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { "1", "2", "blah" }), 1024L) }));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();
		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		ByteBuffer buffer = ByteBuffer.allocate (1024);
		storage.write (0, buffer);

		assertTrue (new File (baseDirectory, "base" + File.separator + "1" + File.separator + "2" + File.separator + "blah").exists());

	}


	// Test closure

	/**
	 * Tests behaviour of close()
	 *
	 * @throws Exception
	 */
	@Test
	public void testClose() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1024L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();
		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		ByteBuffer buffer = storage.read (0);
		storage.write (0, buffer);

		storage.close();

		expectReadOutOfBounds (storage, 0);
		expectWriteOutOfBounds (storage, 0, buffer);

	}


	// Test constructor for existing files

	/**
	 * Read on short underlying file should zero fill the missing portion
	 *
	 * @throws Exception
	 */
	@Test
	public void testExistingFiles() throws Exception {

		InfoFileset fileset = new InfoFileset ("base", Arrays.asList (new Filespec[] {
				new Filespec ("blah1", 456L),
				new Filespec ("blah2", 10 * 1024L),
				new Filespec ("blah3", 0L),
				new Filespec ("blah4", 1024L)
		}));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();
		new File (baseDirectory, "base").mkdir();

		// Create files
		for (Filespec filespec : fileset.getFiles()) {
			File file = new File (baseDirectory, "base" + File.separator + filespec.getName().get(0));
			new RandomAccessFile(file, "rw").setLength (filespec.getLength());
		}
		int numPieces = (int)Math.ceil ((double)fileset.getLength() / pieceSize);
		int numWholePieces = (int)Math.floor ((double)fileset.getLength() / pieceSize);
		int finalPieceLength = (int)(fileset.getLength() % pieceSize);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);


		ByteBuffer buffer = ByteBuffer.allocate (1024);

		// Check that reads / writes below and above the bounds of the FileStorage are rejected
		expectReadOutOfBounds (storage, -1);
		expectReadOutOfBounds (storage, numPieces);
		expectWriteOutOfBounds (storage, -1, buffer);
		expectWriteOutOfBounds (storage, numPieces, buffer);

		// Write whole pieces to the end of the FileStorage
		ByteBuffer[] wholePieces = new ByteBuffer[numPieces];
		for (int i = 0; i < numPieces; i++) {
			wholePieces[i] = ByteBuffer.wrap (Util.pseudoRandomBlock (i, pieceSize, pieceSize));
			storage.write (i, wholePieces[i]);
			wholePieces[i].rewind();
		}

		// Check the file sizes after all writes are complete
		for (Filespec filespec : fileset.getFiles()) {
			File file = new File (baseDirectory, "base" + File.separator + filespec.getName().get(0));
			assertEquals (filespec.getLength().longValue(), file.length());
		}

		// Check that the whole pieces have been written correctly
		for (int i = 0; i < numWholePieces; i++) {
			ByteBuffer readPiece = storage.read (i);
			assertEquals (wholePieces[i], readPiece);
		}

		// Check that the final short piece, if present, has been written correctly
		if (finalPieceLength > 0) {
			ByteBuffer partialFinalPiece = ByteBuffer.wrap (Util.pseudoRandomBlock (numPieces - 1, finalPieceLength, finalPieceLength));
			ByteBuffer readPiece = storage.read (numPieces - 1);
			assertEquals (partialFinalPiece, readPiece);
		}

	}


	/**
	 * Tests getting the piece length on a 1024 byte piece / 0 byte file
	 *
	 * @throws Exception
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testPieceLength1024_0() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 0L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		storage.getPiecesetDescriptor().getPieceLength (0);

	}


	/**
	 * Tests getting the piece length on a 1024 byte piece / 1 byte file
	 *
	 * @throws Exception
	 */
	@Test
	public void testPieceLength1024_1() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		assertEquals (1, storage.getPiecesetDescriptor().getPieceLength (0));

	}


	/**
	 * Tests getting the piece length on a 1024 byte piece / 1023 byte file
	 *
	 * @throws Exception
	 */
	@Test
	public void testPieceLength1024_1023() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1023L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		assertEquals (1023, storage.getPiecesetDescriptor().getPieceLength (0));

	}


	/**
	 * Tests getting the piece length on a 1024 byte piece / 1024 byte file
	 *
	 * @throws Exception
	 */
	@Test
	public void testPieceLength1024_1024() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1024L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		assertEquals (1024, storage.getPiecesetDescriptor().getPieceLength (0));

	}


	/**
	 * Tests getting the piece length on a 1024 byte piece / 1025 byte file
	 *
	 * @throws Exception
	 */
	@Test
	public void testPieceLength1024_1025() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1025L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		assertEquals (1024, storage.getPiecesetDescriptor().getPieceLength (0));
		assertEquals (1, storage.getPiecesetDescriptor().getPieceLength (1));

	}


	/**
	 * Tests getting the piece length on a 1024 byte piece / 2048 byte file
	 *
	 * @throws Exception
	 */
	@Test
	public void testPieceLength1024_2048() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 2048L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		assertEquals (1024, storage.getPiecesetDescriptor().getPieceLength (0));
		assertEquals (1024, storage.getPiecesetDescriptor().getPieceLength (1));

	}


	/**
	 * Tests getting the file backed pieces of a zero length file
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testFileBackedPieces0() throws Exception {

		InfoFileset fileset = new InfoFileset ("base", new ArrayList<Filespec>());
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		assertEquals (0, storage.getStorageBackedPieces().length());

	}


	/**
	 * Tests getting the file backed pieces of a single 1 piece file with 0 pieces backed
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testFileBackedPieces1_0() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1024L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		BitField fileBackedPieces = storage.getStorageBackedPieces();
		assertEquals (1, fileBackedPieces.length());
		assertEquals (false, fileBackedPieces.get (0));

	}


	/**
	 * Tests getting the file backed pieces of a single 1 piece file with 0.5 pieces backed
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testFileBackedPieces1_0x5() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1024L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		File file = new File (baseDirectory, "blah");
		new RandomAccessFile (file, "rw").setLength (512L);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		BitField fileBackedPieces = storage.getStorageBackedPieces();
		assertEquals (1, fileBackedPieces.length());
		assertEquals (false, fileBackedPieces.get (0));

	}


	/**
	 * Tests getting the file backed pieces of a single 1 piece file with 1 piece backed
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testFileBackedPieces1_1() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1024L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		File file = new File (baseDirectory, "blah");
		new RandomAccessFile (file, "rw").setLength (1024L);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		BitField fileBackedPieces = storage.getStorageBackedPieces();
		assertEquals (1, fileBackedPieces.length());
		assertEquals (true, fileBackedPieces.get (0));

	}


	/**
	 * Tests getting the file backed pieces of a single 2 piece file with 1 piece backed
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFileBackedPieces2_1() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 2048L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		File file = new File (baseDirectory, "blah");
		new RandomAccessFile (file, "rw").setLength (1024L);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		BitField fileBackedPieces = storage.getStorageBackedPieces();
		assertEquals (2, fileBackedPieces.length());
		assertEquals (true, fileBackedPieces.get (0));
		assertEquals (false, fileBackedPieces.get (1));

	}


	/**
	 * Tests getting the file backed pieces of a single 2 piece file with 1.5 pieces backed
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFileBackedPieces2_1x5() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 2048L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		File file = new File (baseDirectory, "blah");
		new RandomAccessFile (file, "rw").setLength (1500L);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		BitField fileBackedPieces = storage.getStorageBackedPieces();
		assertEquals (2, fileBackedPieces.length());
		assertEquals (true, fileBackedPieces.get (0));
		assertEquals (false, fileBackedPieces.get (1));

	}


	/**
	 * Tests getting the file backed pieces of a single 2 piece file with 2 pieces backed
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testFileBackedPieces2_2() throws Exception {

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 2048L));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();

		File file = new File (baseDirectory, "blah");
		new RandomAccessFile (file, "rw").setLength (2048L);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		BitField fileBackedPieces = storage.getStorageBackedPieces();
		assertEquals (2, fileBackedPieces.length());
		assertEquals (true, fileBackedPieces.get (0));
		assertEquals (true, fileBackedPieces.get (1));

	}


	/**
	 * Tests getting the file backed pieces of two files covering 0.5 and 1 pieces, with 2 pieces backed
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testFileBackedPieces0x5_1_2() throws Exception {

		InfoFileset fileset = new InfoFileset ("base", Arrays.asList(new Filespec[] {
				new Filespec ("blah1", 512L),
				new Filespec ("blah2", 1024L)
		}));
		int pieceSize = 1024;

		File baseDirectory = Util.createTemporaryDirectory();
		new File (baseDirectory, "base").mkdir();

		File file1 = new File (baseDirectory, "base" + File.separator + "blah1");
		new RandomAccessFile (file1, "rw").setLength (512L);
		File file2 = new File (baseDirectory, "base" + File.separator + "blah2");
		new RandomAccessFile (file2, "rw").setLength (1024L);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		BitField fileBackedPieces = storage.getStorageBackedPieces();
		assertEquals (2, fileBackedPieces.length());
		assertEquals (true, fileBackedPieces.get (0));
		assertEquals (true, fileBackedPieces.get (1));

	}


	/**
	 * Test construction failure if parent directory is not a directory
	 *
	 * @throws Exception 
	 */
	@Test(expected=IOException.class)
	public void testParentDirectoryNotDirectory() throws Exception {

		File testFile = File.createTempFile ("tcf", "tmp");
		new FileStorage (testFile);

	}


	/**
	 * Test construction failure with base directory part "."
	 *
	 * @throws Exception 
	 */
	@Test(expected=IOException.class)
	public void testBaseDirectoryDot() throws Exception {

		int pieceSize = 16384;
		File testFile = Util.createNonExistentTemporaryFile();
		List<Filespec> filespecs = Arrays.asList (new Filespec[] {
				new Filespec (Arrays.asList (new String[] {"A"}), 65536L)
		});

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (16384, 65536);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.create (new InfoFileset (".", filespecs), 16384, pieceHashes);

		FileStorage storage = new FileStorage (testFile);
		storage.open (pieceSize, info.getFileset());

	}


	/**
	 * Test construction failure with base directory part ".."
	 *
	 * @throws Exception 
	 */
	@Test(expected=IOException.class)
	public void testBaseDirectoryDotDot() throws Exception {

		int pieceSize = 16384;
		File testFile = Util.createNonExistentTemporaryFile();
		List<Filespec> filespecs = Arrays.asList (new Filespec[] {
				new Filespec (Arrays.asList (new String[] {"A"}), 65536L)
		});

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (16384, 65536);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.create (new InfoFileset ("..", filespecs), 16384, pieceHashes);

		FileStorage storage = new FileStorage (testFile);
		storage.open (pieceSize, info.getFileset());

	}


	/**
	 * Test construction failure with base directory containing File.separator
	 *
	 * @throws Exception 
	 */
	@Test(expected=IOException.class)
	public void testBaseDirectorySeparator() throws Exception {

		int pieceSize = 16384;
		File testFile = Util.createNonExistentTemporaryFile();
		List<Filespec> filespecs = Arrays.asList (new Filespec[] {
				new Filespec (Arrays.asList (new String[] {"A"}), 65536L)
		});

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (16384, 65536);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.create (new InfoFileset (File.separator, filespecs), 16384, pieceHashes);

		FileStorage storage = new FileStorage (testFile);
		storage.open (pieceSize, info.getFileset());

	}


	/**
	 * Test construction failure with file part "."
	 *
	 * @throws Exception 
	 */
	@Test(expected=IOException.class)
	public void testFilePartDot() throws Exception {

		int pieceSize = 16384;
		File testFile = Util.createNonExistentTemporaryFile();
		List<Filespec> filespecs = Arrays.asList (new Filespec[] {
				new Filespec (Arrays.asList (new String[] {"."}), 32768L)
		});

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (16384, 32768);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.create (new InfoFileset (testFile.getName(), filespecs), 16384, pieceHashes);

		FileStorage storage = new FileStorage (testFile);
		storage.open (pieceSize, info.getFileset());

	}


	/**
	 * Test construction failure with file part ".."
	 *
	 * @throws Exception 
	 */
	@Test(expected=IOException.class)
	public void testFilePartDotDot() throws Exception {

		int pieceSize = 16384;
		File testFile = Util.createNonExistentTemporaryFile();
		List<Filespec> filespecs = Arrays.asList (new Filespec[] {
				new Filespec (Arrays.asList (new String[] {".."}), 32768L)
		});

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (16384, 32768);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.create (new InfoFileset (testFile.getName(), filespecs), 16384, pieceHashes);

		FileStorage storage = new FileStorage (testFile);
		storage.open (pieceSize, info.getFileset());

	}


	/**
	 * Test construction failure with file part containing File.separator
	 *
	 * @throws Exception 
	 */
	@Test(expected=IOException.class)
	public void testFilePartSeparator() throws Exception {

		int pieceSize = 16384;
		File testFile = Util.createNonExistentTemporaryFile();
		List<Filespec> filespecs = Arrays.asList (new Filespec[] {
				new Filespec (Arrays.asList (new String[] {File.separator}), 32768L)
		});

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (16384, 32768);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.create (new InfoFileset (testFile.getName(), filespecs), 16384, pieceHashes);

		FileStorage storage = new FileStorage (testFile);
		storage.open (pieceSize, info.getFileset());

	}


	/**
	 * Checks that a multi-file Info is created in a subdirectory
	 * @throws Exception 
	 */
	@Test
	public void testExistingSubdirectory() throws Exception {

		int pieceSize = 16384;
		File baseDirectory = Util.createTemporaryDirectory();
		List<Filespec> filespecs = Arrays.asList (new Filespec[] {
				new Filespec (Arrays.asList (new String[] {"test1"}), 32768L),
				new Filespec (Arrays.asList (new String[] {"test2"}), 32768L)
		});

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (16384, 65536);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.create (new InfoFileset ("base", filespecs), 16384, pieceHashes);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, info.getFileset());
		storage.write (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)));
		storage.write (2, ByteBuffer.wrap (Util.pseudoRandomBlock (2, 16384, 16384)));

		assertTrue (new File (baseDirectory, "base" + File.separator + "test1").exists());
		assertTrue (new File (baseDirectory, "base" + File.separator + "test2").exists());

	}


	/**
	 * Tests validation with a null cookie
	 * @throws Exception
	 */
	@Test
	public void testValidateNull() throws Exception {

		int pieceSize = 1024;
		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 2048L));

		File baseDirectory = Util.createTemporaryDirectory();
		File file = new File (baseDirectory, "blah");
		new RandomAccessFile (file, "rw").setLength (2048L);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		assertFalse (storage.validate (null));

	}


	/**
	 * Tests validation on an unchanged FileStorage with nonexistent backing file
	 * @throws Exception
	 */
	@Test
	public void testValidateNonexistentUnchanged() throws Exception {

		int pieceSize = 1024;
		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 2048L));
		File baseDirectory = Util.createTemporaryDirectory();

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);
		ByteBuffer cookie = storage.close();
		FileStorage storage2 = new FileStorage (baseDirectory);
		storage2.open (pieceSize, fileset);

		assertTrue (storage2.validate (cookie));

	}


	/**
	 * Tests validation on an unchanged FileStorage
	 * @throws Exception
	 */
	@Test
	public void testValidateUnchanged() throws Exception {

		int pieceSize = 1024;
		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 2048L));
		File baseDirectory = Util.createTemporaryDirectory();

		File file = new File (baseDirectory, "blah");
		new RandomAccessFile (file, "rw").setLength (2048L);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);
		ByteBuffer cookie = storage.close();
		FileStorage storage2 = new FileStorage (baseDirectory);
		storage2.open (pieceSize, fileset);

		assertTrue (storage2.validate (cookie));

	}


	/**
	 * Tests validation on an FileStorage in which a file data has changed
	 * @throws Exception
	 */
	@Test
	public void testValidateDateChanged() throws Exception {

		int pieceSize = 1024;
		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 2048L));
		File baseDirectory = Util.createTemporaryDirectory();

		File file = new File (baseDirectory, "blah");
		new RandomAccessFile (file, "rw").setLength (2048L);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);
		ByteBuffer cookie = storage.close();
		file.setLastModified (file.lastModified() + 1000);
		FileStorage storage2 = new FileStorage (baseDirectory);

		assertFalse (storage2.validate (cookie));

	}


	/**
	 * Tests validation on an FileStorage in which a file data has changed
	 * @throws Exception
	 */
	@Test
	public void testValidateSizeChanged() throws Exception {

		int pieceSize = 1024;
		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 2048L));
		File baseDirectory = Util.createTemporaryDirectory();

		File file = new File (baseDirectory, "blah");
		new RandomAccessFile (file, "rw").setLength (2048L);

		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);
		ByteBuffer cookie = storage.close();
		long lastModified = file.lastModified();
		RandomAccessFile randomAccessFile = new RandomAccessFile (file, "rw");
		randomAccessFile.setLength (2048L);
		randomAccessFile.close();
		file.setLastModified (lastModified);
		FileStorage storage2 = new FileStorage (baseDirectory);

		assertFalse (storage2.validate (cookie));

	}


	/**
	 * Tests extending
	 *
	 * @throws Exception
	 */
	@Test
	public void testExtend() throws Exception {

		int pieceSize = 1024;

		PiecesetDescriptor expectedDescriptor1 = new PiecesetDescriptor (1024, 1024);
		PiecesetDescriptor expectedDescriptor2 = new PiecesetDescriptor (1024, 2048);

		InfoFileset fileset = new InfoFileset (new Filespec ("blah", 1024L));
		File baseDirectory = Util.createTemporaryDirectory();
		FileStorage storage = new FileStorage (baseDirectory);
		storage.open (pieceSize, fileset);

		assertEquals (expectedDescriptor1, storage.getPiecesetDescriptor());

		storage.extend (2 * pieceSize);

		ByteBuffer piece = ByteBuffer.wrap (Util.pseudoRandomBlock (1, pieceSize, pieceSize));
		storage.openOutputChannel (1, 0).write (piece);

		assertEquals (expectedDescriptor2, storage.getPiecesetDescriptor());
		assertEquals (ByteBuffer.wrap (Util.pseudoRandomBlock (1, pieceSize, pieceSize)), storage.read (1));

	}

}
