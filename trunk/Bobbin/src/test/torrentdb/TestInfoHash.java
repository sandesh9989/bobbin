package test.torrentdb;

import static org.junit.Assert.*;

import org.itadaki.bobbin.torrentdb.InfoHash;
import org.junit.Test;



/**
 * Tests InfoHash
 */
public class TestInfoHash {

	/**
	 * Tests InfoHash (byte[])
	 */
	@Test
	public void testInfoHashBytes() {

		InfoHash infoHash = new InfoHash (new byte[20]);

		assertEquals (20, infoHash.getBytes().length);

	}


	/**
	 * Tests InfoHash (byte[]) with an invalid length
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInfoHashBytesInvalid() {

		new InfoHash (new byte[19]);

	}


	/**
	 * Tests equality to null
	 */
	@Test
	public void testInfoHashEqualsNull() {

		InfoHash infoHash = new InfoHash (new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 });

		assertFalse (infoHash.equals (null));

	}


	/**
	 * Tests equality to the same object
	 */
	@Test
	public void testInfoHashEqualsSame() {

		InfoHash infoHash = new InfoHash (new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 });

		assertTrue (infoHash.equals (infoHash));

	}


	/**
	 * Tests equality to an identical object
	 */
	@Test
	public void testInfoHashEqualsIdentical() {

		InfoHash infoHash1 = new InfoHash (new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 });
		InfoHash infoHash2 = new InfoHash (new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 });

		assertTrue (infoHash1.equals (infoHash2));

	}


	/**
	 * Tests equality to an non-identical object
	 */
	@Test
	public void testInfoHashEqualsNonIdentical() {

		InfoHash infoHash1 = new InfoHash (new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 });
		InfoHash infoHash2 = new InfoHash (new byte[] { 40, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 });

		assertFalse (infoHash1.equals (infoHash2));

	}


}
