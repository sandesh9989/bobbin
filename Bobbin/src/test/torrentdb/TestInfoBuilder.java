package test.torrentdb;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.itadaki.bobbin.torrentdb.Info;
import org.itadaki.bobbin.torrentdb.InfoBuilder;
import org.junit.Test;

import test.Util;


/**
 * Tests InfoBuilder
 */
public class TestInfoBuilder {

	/**
	 * Tests the hash resulting from creating an Info on a single complete file
	 *
	 * @throws Exception 
	 */
	@Test
	public void testCompleteSingleFileHash() throws Exception {

		byte[] expectedInfoHash = new byte[] { -30, -55, 38, 50, -78, 78, -11, 86, -62, 93, -124, 23, 118, 101, -33, -62, -116, 8, -101, -107 };

		// Create single reproducible file
		File directory = Util.createTemporaryDirectory();
		File testFile = Util.createReproducibleFile (directory, "Test File.bin", 12345);

		Info info = InfoBuilder.createPlain (testFile, 262144).build();

		assertArrayEquals (expectedInfoHash, info.getHash().getBytes());

	}


	/**
	 * Tests the hash resulting from creating an Info on a complete directory structure
	 *
	 * @throws IOException 
	 */
	@Test
	public void testCompleteMultiFileHash() throws IOException {

		byte[] expectedInfoHash = new byte[] { 69, -10, 96, -117, -49, -15, 73, -24, 23, 18, 86, 109, -117, -63, 25, 39, 116, 20, 84, 69 };

		// Create reproducible files
		File directory = Util.createTemporaryDirectory();
		File subDirectory1 = new File (directory, "dir1");
		subDirectory1.mkdir();
		subDirectory1.deleteOnExit();
		File subDirectory2 = new File (subDirectory1, "dir2");
		subDirectory2.mkdir();
		subDirectory2.deleteOnExit();
		Util.createReproducibleFile (subDirectory1, "Test File 1.bin", 12345);
		Util.createReproducibleFile (subDirectory2, "Test File 2.bin", 67890);
		Util.createReproducibleFile (subDirectory2, "Test File 3.bin", 123456);

		Info info = InfoBuilder.createPlain (subDirectory1, 262144).build();

		assertArrayEquals (expectedInfoHash, info.getHash().getBytes());

	}


	/**
	 * Tests the hash resulting from creating a Merkle Info on a single complete file
	 *
	 * @throws Exception 
	 */
	@Test
	public void testCompleteSingleFileMerkleHash() throws Exception {

		byte[] expectedInfoHash = new byte[] { -105, -114, 114, 5, -118, -52, -39, -53, 115, -113, -110, -121, 27, -36, -14, -51, 97, 73, -6, -14 };

		// Create single reproducible file
		File directory = Util.createTemporaryDirectory();
		File testFile = Util.createReproducibleFile (directory, "Test File.bin", 12345);

		Info info = InfoBuilder.createMerkle (testFile, 262144).build();

		assertArrayEquals (expectedInfoHash, info.getHash().getBytes());

	}


	/**
	 * Tests the hash resulting from creating a Merkle Info on a complete directory structure
	 *
	 * @throws IOException 
	 */
	@Test
	public void testCompleteMultiFileMerkleHash() throws IOException {

		byte[] expectedInfoHash = new byte[] { 125, 100, -125, -56, -34, -50, -102, -18, -30, 118, -121, 79, 43, 29, -50, -100, -123, 38, -111, -61 };

		// Create reproducible files
		File directory = Util.createTemporaryDirectory();
		File subDirectory1 = new File (directory, "dir1");
		subDirectory1.mkdir();
		subDirectory1.deleteOnExit();
		File subDirectory2 = new File (subDirectory1, "dir2");
		subDirectory2.mkdir();
		subDirectory2.deleteOnExit();
		Util.createReproducibleFile (subDirectory1, "Test File 1.bin", 12345);
		Util.createReproducibleFile (subDirectory2, "Test File 2.bin", 67890);
		Util.createReproducibleFile (subDirectory2, "Test File 3.bin", 123456);

		Info info = InfoBuilder.createMerkle (subDirectory1, 262144).build();

		assertArrayEquals (expectedInfoHash, info.getHash().getBytes());

	}


	// TODO Test - build Elastic Info


}
