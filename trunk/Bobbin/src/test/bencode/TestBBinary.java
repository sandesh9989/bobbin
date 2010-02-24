package test.bencode;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.itadaki.bobbin.bencode.BBinary;
import org.junit.Test;



/**
 * Tests BBinary
 */
public class TestBBinary {

	/**
	 * Store a binary value and retrieve it
	 */
	@Test
	public void testBinary() {

		BBinary binary = new BBinary (new byte[] {0, 1, 2});

		assertArrayEquals (binary.value(), new byte[] {0, 1, 2});

	}


	/**
	 * Check BBinary equality
	 */
	@Test
	public void testBinaryEquals() {

		BBinary binary1 = new BBinary ("Hello");
		BBinary binary2 = new BBinary ("Hello");

		assertEquals (binary1, binary2);

	}


	/**
	 * Check BBinary inequality
	 */
	@Test
	public void testBinaryNotEquals() {

		BBinary binary1 = new BBinary ("Hello");
		BBinary binary2 = new BBinary ("World");

		assertFalse (binary1.equals (binary2));

	}


	/**
	 * Check BBinary compareTo - less than
	 */
	@Test
	public void testBinaryCompareToLessThan() {

		BBinary binary1 = new BBinary (new byte[] {0, 1, 2});
		BBinary binary2 = new BBinary (new byte[] {0, 1, 1});
		BBinary binary3 = new BBinary (new byte[] {0, 1, 1, 0});
		BBinary binary4 = new BBinary (new byte[] {0, 1});
		BBinary binary5 = new BBinary (new byte[] {0});
		BBinary binary6 = new BBinary (new byte[] {});

		assertTrue (binary2.compareTo (binary1) < 0);
		assertTrue (binary3.compareTo (binary1) < 0);
		assertTrue (binary4.compareTo (binary1) < 0);
		assertTrue (binary5.compareTo (binary1) < 0);
		assertTrue (binary6.compareTo (binary1) < 0);

	}


	/**
	 * Check BBinary compareTo - less than
	 */
	@Test
	public void testBinaryCompareToEquals() {

		BBinary binary1a = new BBinary (new byte[] {});
		BBinary binary1b = new BBinary (new byte[] {});
		BBinary binary2a = new BBinary (new byte[] {0});
		BBinary binary2b = new BBinary (new byte[] {0});
		BBinary binary3a = new BBinary (new byte[] {0, 1});
		BBinary binary3b = new BBinary (new byte[] {0, 1});

		assertTrue (binary1a.compareTo (binary1b) == 0);
		assertTrue (binary2a.compareTo (binary2b) == 0);
		assertTrue (binary3a.compareTo (binary3b) == 0);

	}


	/**
	 * Check BBinary compareTo - greater than
	 */
	@Test
	public void testBinaryCompareToGreaterThan() {

		BBinary binary1 = new BBinary (new byte[] {0, 1, 2});
		BBinary binary2 = new BBinary (new byte[] {0, 1, 3});
		BBinary binary3 = new BBinary (new byte[] {0, 1, 2, 0});
		BBinary binary4 = new BBinary (new byte[] {0, 2});
		BBinary binary5 = new BBinary (new byte[] {1});

		assertTrue (binary2.compareTo (binary1) > 0);
		assertTrue (binary3.compareTo (binary1) > 0);
		assertTrue (binary4.compareTo (binary1) > 0);
		assertTrue (binary5.compareTo (binary1) > 0);

	}


	/**
	 * Test cloning a BBinary 
	 */
	@Test
	public void testBinaryClone() {

		BBinary binary = new BBinary (new byte[] {0, 1, 2});
		BBinary clonedBinary = binary.clone();

		assertEquals (binary, clonedBinary);
		assertFalse (System.identityHashCode (binary) == System.identityHashCode (clonedBinary));
		assertFalse (System.identityHashCode (binary.value()) == System.identityHashCode (clonedBinary.value()));

	}


	/**
	 * Test BBinary stringValue
	 *
	 */
	@Test
	public void testBinaryStringValue() {

		BBinary binary = new BBinary ("Test");

		assertEquals ("Test", binary.stringValue());

	}


}
