package test.torrentdb;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Random;

import org.itadaki.bobbin.torrentdb.FileMetadataProvider;
import org.itadaki.bobbin.torrentdb.IncompatibleLocationException;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.Metadata;
import org.junit.Test;

import test.Util;


/**
 * Tests FileMetadataProvider
 */
public class TestFileMetadataProvider {

	/**
	 * Tests Metadata creation
	 * @throws Exception
	 */
	@Test
	public void testCreate() throws Exception {

		File directory = Util.createTemporaryDirectory();
		FileMetadataProvider provider = new FileMetadataProvider (directory);

		byte[] infoHashBytes = new byte[20];
		new Random().nextBytes (infoHashBytes);
		InfoHash infoHash = new InfoHash (infoHashBytes);
		Metadata metadata = provider.metadataFor (infoHash);

		metadata.put ("key", "value".getBytes());

		assertArrayEquals ("value".getBytes(), metadata.get ("key"));

	}


	/**
	 * Tests forgetting
	 * @throws Exception
	 */
	@Test
	public void testForget() throws Exception {

		File directory = Util.createTemporaryDirectory();
		FileMetadataProvider provider = new FileMetadataProvider (directory);

		byte[] infoHashBytes = new byte[20];
		new Random().nextBytes (infoHashBytes);
		InfoHash infoHash = new InfoHash (infoHashBytes);
		Metadata metadata = provider.metadataFor (infoHash);

		metadata.put ("key", "value".getBytes());

		provider.forget (infoHash);
		metadata = provider.metadataFor (infoHash);

		assertEquals (null, metadata.get ("key"));

	}


	/**
	 * Tests creating a FileMetadataProvider in an invalid location
	 * @throws Exception
	 */
	@Test(expected=IncompatibleLocationException.class)
	public void testIncompatibleLocation1() throws Exception {

		new FileMetadataProvider (File.createTempFile ("tcf", "tmp"));

	}


	/**
	 * Tests creating a FileMetadataProvider in an invalid location
	 * @throws Exception
	 */
	@Test(expected=IncompatibleLocationException.class)
	public void testIncompatibleLocation2() throws Exception {

		new FileMetadataProvider (new File (File.createTempFile ("tcf", "tmp"), "blah"));

	}
}
