package test.torrentdb;

import static org.junit.Assert.*;

import java.io.File;

import org.itadaki.bobbin.torrentdb.FileMetadata;
import org.itadaki.bobbin.torrentdb.IncompatibleLocationException;
import org.junit.Test;

import test.Util;


/**
 * Tests FileMetadata
 */
public class TestFileMetadata {

	/**
	 * Tests put / get
	 * @throws Exception
	 */
	@Test
	public void testPutGet() throws Exception {

		File directory = Util.createTemporaryDirectory();
		FileMetadata metadata = new FileMetadata (directory);

		metadata.put ("key", "value".getBytes());

		assertArrayEquals ("value".getBytes(), metadata.get ("key"));

	}


	/**
	 * Tests put / put / get
	 * @throws Exception
	 */
	@Test
	public void testPutPutGet() throws Exception {

		File directory = Util.createTemporaryDirectory();
		FileMetadata metadata = new FileMetadata (directory);

		metadata.put ("key", "value".getBytes());
		metadata.put ("key", "door".getBytes());

		assertArrayEquals ("door".getBytes(), metadata.get ("key"));

	}


	/**
	 * Tests put / put null / get
	 * @throws Exception
	 */
	@Test
	public void testPutPutNullGet() throws Exception {

		File directory = Util.createTemporaryDirectory();
		FileMetadata metadata = new FileMetadata (directory);

		metadata.put ("key", "value".getBytes());
		metadata.put ("key", null);

		assertNull (metadata.get ("key"));

	}


	/**
	 * Tests two keys
	 * @throws Exception
	 */
	@Test
	public void testTwoKeys() throws Exception {

		File directory = Util.createTemporaryDirectory();
		FileMetadata metadata = new FileMetadata (directory);

		metadata.put ("key1", "value 1".getBytes());
		metadata.put ("key2", "value 2".getBytes());

		assertArrayEquals ("value 1".getBytes(), metadata.get ("key1"));
		assertArrayEquals ("value 2".getBytes(), metadata.get ("key2"));

	}


	/**
	 * Tests close() (not supposed to do anything in particular)
	 * @throws Exception
	 */
	@Test
	public void testClose() throws Exception {

		File directory = Util.createTemporaryDirectory();
		FileMetadata metadata = new FileMetadata (directory);

		metadata.close();

	}

	/**
	 * Tests creating a FileMetadata in an invalid location
	 * @throws Exception
	 */
	@Test(expected=IncompatibleLocationException.class)
	public void testIncompatibleLocation1() throws Exception {

		new FileMetadata (File.createTempFile ("tcf", "tmp"));

	}


	/**
	 * Tests creating a FileMetadata in an invalid location
	 * @throws Exception
	 */
	@Test(expected=IncompatibleLocationException.class)
	public void testIncompatibleLocation2() throws Exception {

		new FileMetadata (new File (File.createTempFile ("tcf", "tmp"), "blah"));

	}

}
