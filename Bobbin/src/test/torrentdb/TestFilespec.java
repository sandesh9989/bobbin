package test.torrentdb;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.itadaki.bobbin.torrentdb.Filespec;
import org.junit.Test;

/**
 * Tests Filespec
 */
public class TestFilespec {

	/**
	 * Tests creating a valid Filespec
	 */
	@Test
	public void testValid1() {

		// Given
		List<String> name = Arrays.asList (new String[] { "A" });
		Long length = 1234L;

		// When
		Filespec filespec = new Filespec (name, length);

		// Then
		assertArrayEquals (name.toArray(), filespec.getName().toArray());
		assertEquals (length, filespec.getLength());

	}


	/**
	 * Tests creating a valid Filespec
	 */
	@Test
	public void testValid2() {

		// Given
		List<String> name = Arrays.asList (new String[] { "A", "B" });
		Long length = 1234L;

		// When
		Filespec filespec = new Filespec (name, length);

		// Then
		assertArrayEquals (name.toArray(), filespec.getName().toArray());
		assertEquals (length, filespec.getLength());

	}


	/**
	 * Tests creating a Filespec with an invalid file length
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidFileLength() {

		// When
		new Filespec (Arrays.asList (new String[] { "A" }), -1L);

		// Then
		// ... exception

	}


	/**
	 * Tests creating a Filespec with an invalid filename
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidFilename1() {

		// When
		new Filespec (Arrays.asList (new String[] { }), 1234L);

		// Then
		// ... exception
	}


	/**
	 * Tests creating a Filespec with an invalid filename
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidFilename2() {

		// When
		new Filespec (Arrays.asList (new String[] { "A", "" }), 1234L);

		// Then
		// ... exception

	}


}
