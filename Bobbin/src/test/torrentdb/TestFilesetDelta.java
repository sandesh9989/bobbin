package test.torrentdb;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.itadaki.bobbin.torrentdb.FilesetDelta;
import org.itadaki.bobbin.torrentdb.Filespec;
import org.junit.Test;

/**
 * Tests FilesetDelta
 */
public class TestFilesetDelta {

	/**
	 * Tests a valid FilesetDelta
	 */
	@Test
	public void testValid() {

		// Given
		long lastFileLength = 1234L;
		List<Filespec> files = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { "A" }), 4567L)});

		// When
		FilesetDelta delta = new FilesetDelta (lastFileLength, files, true, true);

		// Then
		assertEquals (lastFileLength, delta.getLastFileLength());
		assertArrayEquals (files.toArray(), delta.getAdditionalFiles().toArray());
		assertTrue (delta.isFilesSealed());
		assertTrue (delta.isDataSealed());

	}


	/**
	 * Tests an invalid FilesetDelta
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidLength() {

		// Given
		long lastFileLength = -1L;
		List<Filespec> files = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { "A" }), 4567L)});

		// When
		new FilesetDelta (lastFileLength, files, true, true);

		// Then
		// ... exception

	}


	/**
	 * Tests an invalid FilesetDelta
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidFiles() {

		// Given
		long lastFileLength = 1234L;
		List<Filespec> files = null;

		// When
		new FilesetDelta (lastFileLength, files, true, true);

		// Then
		// ... exception

	}


}
