/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.itadaki.bobbin.util.BitField;
import org.junit.Test;



/**
 * Tests BitField
 */
public class TestBitField {

	/**
	 * Negative bit array - create
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testNegativeBitCreate() {

		new BitField (-1);

	}


	/**
	 * 0 bit array - create
	 */
	@Test
	public void test0BitCreate() {

		BitField bitField = new BitField (0);
		assertEquals (0, bitField.byteLength());

	}


	/**
	 * 0 bit array - get bit -1
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test0BitGetNegative() {

		BitField bitField = new BitField (0);
		bitField.get (-1);

	}


	/**
	 * 0 bit array - get bit 0
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test0BitGetZero() {

		BitField bitField = new BitField (0);
		bitField.get (0);

	}


	/**
	 * 0 bit array - set bit -1
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test0BitSetNegative() {

		BitField bitField = new BitField (0);
		bitField.set (-1);

	}


	/**
	 * 0 bit array - set bit 0
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test0BitSetZero() {

		BitField bitField = new BitField (0);
		bitField.set (0);

	}


	/**
	 * 0 bit array - clear bit -1
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test0BitClearNegative() {

		BitField bitField = new BitField (0);
		bitField.clear (-1);

	}


	/**
	 * 0 bit array - clear bit 0
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test0BitClearZero() {

		BitField bitField = new BitField (0);
		bitField.clear (0);

	}


	/**
	 * 0 bit array - copy
	 */
	@Test
	public void test0BitCopy() {

		byte[] destination = new byte[0];
		BitField bitField = new BitField (0);
		bitField.copyTo (destination, 0);

	}


	/**
	 * 1 bit array - create
	 */
	@Test
	public void test1BitCreate() {

		BitField bitField = new BitField (1);
		assertEquals (1, bitField.length());
		assertEquals (1, bitField.byteLength());

	}


	/**
	 * 1 bit array - get bit -1
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test1BitGetNegative() {

		BitField bitField = new BitField (1);
		bitField.get (-1);

	}


	/**
	 * 1 bit array - get bit 0
	 */
	@Test
	public void test1BitGetZero() {

		BitField bitField = new BitField (1);
		assertEquals (false, bitField.get (0));

	}


	/**
	 * 1 bit array - get bit 1
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test1BitGetOne() {

		BitField bitField = new BitField (1);
		bitField.get (1);

	}


	/**
	 * 1 bit array - set bit -1
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test1BitSetNegative() {

		BitField bitField = new BitField (1);
		bitField.set (-1);

	}


	/**
	 * 1 bit array - set bit 0
	 */
	@Test
	public void test1BitSetZero() {

		BitField bitField = new BitField (1);
		bitField.set (0);
		assertEquals (true, bitField.get (0));

	}


	/**
	 * 1 bit array - set bit 1
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test1BitSetOne() {

		BitField bitField = new BitField (1);
		bitField.set (1);

	}


	/**
	 * 1 bit array - clear bit -1
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test1BitClearNegative() {

		BitField bitField = new BitField (1);
		bitField.clear (-1);

	}


	/**
	 * 1 bit array - clear bit 0
	 */
	public void test1BitClearZero() {

		BitField bitField = new BitField (1);
		bitField.set (0);
		bitField.clear (0);
		assertEquals (false, bitField.get (0));

	}


	/**
	 * 1 bit array - clear bit 0
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test1BitClearOne() {

		BitField bitField = new BitField (1);
		bitField.clear (1);

	}


	/**
	 * 1 bit array - copy
	 */
	@Test
	public void test1BitCopy() {

		byte[] destination = new byte[1];
		BitField bitField = new BitField (1);
		bitField.set (0);
		bitField.copyTo (destination, 0);
		assertEquals (-128, destination[0]);

	}


	/**
	 * 1 bit array - copy to non zero position
	 */
	@Test
	public void test1BitCopyNonZero() {

		byte[] destination = { 2, 4, 8, 16, 32 };
		BitField bitField = new BitField (1);
		bitField.set (0);
		bitField.copyTo (destination, 3);
		assertArrayEquals ( new byte[] { 2, 4, 8, -128, 32 }, destination);

	}


	/**
	 * 101 bit array - create
	 */
	@Test
	public void test101BitCreate() {

		BitField bitField = new BitField (101);
		assertEquals (101, bitField.length());
		assertEquals (13, bitField.byteLength());

	}


	/**
	 * 101 bit array - get bit -1
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test101BitGetNegative() {

		BitField bitField = new BitField (1);
		bitField.get (-1);

	}


	/**
	 * 101 bit array - get bit 0
	 */
	@Test
	public void test101BitGetZero() {

		BitField bitField = new BitField (101);
		assertEquals (false, bitField.get (0));

	}


	/**
	 * 101 bit array - get bit 1
	 */
	@Test
	public void test101BitGetOne() {

		BitField bitField = new BitField (101);
		assertEquals (false, bitField.get (1));

	}


	/**
	 * 101 bit array - get bit 100
	 */
	@Test
	public void test101BitGet100() {

		BitField bitField = new BitField (101);
		assertEquals (false, bitField.get (100));

	}



	/**
	 * 101 bit array - get bit 101
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test101BitGet101() {

		BitField bitField = new BitField (101);
		bitField.get (101);

	}


	/**
	 * 101 bit array - set bit -1
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test101BitSetNegative() {

		BitField bitField = new BitField (101);
		bitField.set (-1);

	}


	/**
	 * 101 bit array - set bit 0
	 */
	@Test
	public void test101BitSetZero() {

		BitField bitField = new BitField (101);
		bitField.set (0);
		assertEquals (true, bitField.get (0));

	}


	/**
	 * 101 bit array - set bit 1
	 */
	@Test
	public void test101BitSetOne() {

		BitField bitField = new BitField (101);
		bitField.set (1);
		assertEquals (true, bitField.get (1));

	}


	/**
	 * 101 bit array - set bit 100
	 */
	@Test
	public void test101BitSet100() {

		BitField bitField = new BitField (101);
		bitField.set (100);
		assertEquals (true, bitField.get (100));

	}


	/**
	 * 101 bit array - set bit 101
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void test101BitSet101() {

		BitField bitField = new BitField (101);
		bitField.set (101);

	}


	/**
	 * 101 bit array - copy
	 */
	@Test
	public void test101BitCopy() {

		byte[] destination = new byte[13];
		BitField bitField = new BitField (101);
		bitField.set (0);
		bitField.copyTo (destination, 0);
		assertArrayEquals (new byte[] {-128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, destination);

	}


	/**
	 * 101 bit array - clear
	 */
	@Test
	public void test101BitClear() {

		byte[] destination = new byte[13];
		BitField bitField = new BitField (101);
		bitField.not();
		bitField.clear();
		bitField.copyTo (destination, 0);
		assertArrayEquals (new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, destination);

	}


	/**
	 * 0 bit array - initialised from byte array
	 */
	@Test
	public void testCreateFromByteArray0() {

		new BitField (new byte[] { }, 0);

	}


	/**
	 * 1 bit array - initialised from byte array
	 */
	@Test
	public void testCreateFromByteArray1() {

		BitField bitField = new BitField (new byte[] { -128 }, 1);
		assertEquals (true, bitField.get (0));

	}


	/**
	 * 1 bit array - initialised from byte array, too few bytes
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateFromByteArray1OutOfBounds0() {
		
		new BitField (new byte[] { }, 1);

	}


	/**
	 * 1 bit array - initialised from byte array, too many bits
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateFromByteArray1OutOfBounds2() {

		new BitField (new byte[] { 3 }, 1);

	}


	/**
	 * 1 bit array - initialised from byte array, too many bytes
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateFromByteArray1OutOfBounds16() {

		new BitField (new byte[] { 0, 0 }, 1);

	}


	/**
	 * 33 bit array - initialised from byte array
	 */
	@Test
	public void testCreateFromByteArray33() {

		BitField bitField = new BitField (new byte[] { -1, -1, -1, -1, -128 }, 33);
		assertEquals (true, bitField.get (0));
		assertEquals (true, bitField.get (32));

	}


	/**
	 * 33 bit array - initialised from byte array, too few bytes
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateFromByteArray33OutOfBounds32() {
		
		new BitField (new byte[] { -1, -1, -1, -1 }, 33);

	}


	/**
	 * 33 bit array - initialised from byte array, too many bits
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateFromByteArray33OutOfBounds34() {

		new BitField (new byte[] { -1, -1, -1, -1, -64 }, 33);

	}


	/**
	 * 33 bit array - initialised from byte array, too many bytes
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateFromByteArray33OutOfBounds40() {

		new BitField (new byte[] { -1, -1, -1, -1, -128, 0 }, 1);

	}


	
	/**
	 * Test cardinality on a 0 bit field
	 */
	@Test
	public void testCardinality0() {

		BitField bitField = new BitField (0);
		assertEquals (0, bitField.cardinality());

	}


	/**
	 * Test cardinality on a 0 bit field
	 */
	@Test
	public void testCardinality1() {

		BitField bitField = new BitField (1);
		assertEquals (0, bitField.cardinality());

		bitField.set (0);
		assertEquals (1, bitField.cardinality());

	}


	/**
	 * Test cardinality on an 8 bit field
	 */
	@Test
	public void testCardinality8() {

		for (int i = 0; i <= 255; i++) {
			BitField bitField = new BitField (new byte[] { (byte)i }, 8);
			assertEquals (Integer.bitCount(i), bitField.cardinality());
		}

	}


	/**
	 * Test cardinality on a 16 bit field
	 */
	@Test
	public void testCardinality16() {

		for (int i = 0; i <= 255; i++) {
			for (int j = 0; j <= 255; j++) {
				BitField bitField = new BitField (new byte[] { (byte)i, (byte)j }, 16);
				assertEquals (Integer.bitCount(i) + Integer.bitCount(j), bitField.cardinality());
			}
		}

	}


	/**
	 * Test cardinality update through and(BitField)
	 */
	@Test
	public void testCardinalityAnd() {

		BitField bitField1 = new BitField (new byte[] { (byte)0xff, (byte)0xff }, 16);
		BitField bitField2 = new BitField (new byte[] { 0, (byte)0xff }, 16);
		bitField1.and (bitField2);
		assertEquals (8, bitField1.cardinality());

	}


	/**
	 * Test cardinality update through not()
	 */
	@Test
	public void testCardinalityNot() {

		BitField bitField1 = new BitField (16);
		bitField1.not();
		assertEquals (16, bitField1.cardinality());

	}


	/**
	 * Test cardinality update through set(int) over 0
	 */
	@Test
	public void testCardinalitySet01() {

		BitField bitField1 = new BitField (2);
		bitField1.set (0);
		assertEquals (1, bitField1.cardinality());

	}


	/**
	 * Test cardinality update through set(int) over 1
	 */
	@Test
	public void testCardinalitySet11() {

		BitField bitField1 = new BitField (2);
		bitField1.not();
		bitField1.set (1);
		assertEquals (2, bitField1.cardinality());

	}


	/**
	 * Test cardinality update through set(int, boolean) from 0 to 0
	 */
	@Test
	public void testCardinalitySetTo00() {

		BitField bitField1 = new BitField (2);
		bitField1.set (0, false);
		assertEquals (0, bitField1.cardinality());

	}


	/**
	 * Test cardinality update through set(int, boolean) from 0 to 1
	 */
	@Test
	public void testCardinalitySetTo01() {

		BitField bitField1 = new BitField (2);
		bitField1.set (0, true);
		assertEquals (1, bitField1.cardinality());

	}


	/**
	 * Test cardinality update through set(int, boolean) from 1 to 0
	 */
	@Test
	public void testCardinalitySetTo10() {

		BitField bitField1 = new BitField (2);
		bitField1.not();
		bitField1.set (0, false);
		assertEquals (1, bitField1.cardinality());

	}


	/**
	 * Test cardinality update through set(int, boolean) from 1 to 1
	 */
	@Test
	public void testCardinalitySetTo11() {

		BitField bitField1 = new BitField (2);
		bitField1.not();
		bitField1.set (0, true);
		assertEquals (2, bitField1.cardinality());
}


	/**
	 * Test cardinality update through clear()
	 */
	@Test
	public void testCardinalityClear() {

		BitField bitField1 = new BitField (2);
		bitField1.not();
		bitField1.clear();
		assertEquals (0, bitField1.cardinality());

	}


	/**
	 * Test cardinality update through clear(int) over 1
	 */
	@Test
	public void testCardinalityClear10() {

		BitField bitField1 = new BitField (2);
		bitField1.not();
		bitField1.clear (0);
		assertEquals (1, bitField1.cardinality());

	}


	/**
	 * Test cardinality update through clear(int) over 0
	 */
	@Test
	public void testCardinalityClear00() {

		BitField bitField1 = new BitField (2);
		bitField1.clear (0);
		assertEquals (0, bitField1.cardinality());

	}


	/**
	 * Test not on a 0 bit field
	 */
	@Test
	public void testNot0() {

		BitField bitField = new BitField(0);
		bitField.not();

	}


	/**
	 * Test not on a 1 bit field
	 */
	@Test
	public void testNot1() {

		BitField bitField = new BitField(1);
		bitField.not ();
		assertTrue (bitField.get(0)); 
	}


	/**
	 * Test not on an 8 bit field
	 */
	@Test
	public void testNot8() {

		byte[] fieldBytes = new byte[1];
		for (int i = 0; i <= 255; i++) {
			BitField bitField = new BitField (new byte[] { (byte)i }, 8);
			bitField.not();
			bitField.copyTo (fieldBytes, 0);
			assertArrayEquals (new byte[] { (byte)(~i) } , fieldBytes);
		}

	}


	/**
	 * Test not on a 16 bit field
	 */
	@Test
	public void testNot15() {

		byte[] fieldBytes = new byte[2];
		for (int i = 0; i <= 255; i++) {
			for (int j = 0; j <= 127; j++) {
				BitField bitField = new BitField (new byte[] { (byte)i, (byte)(Integer.reverse (j) >>> 24) }, 15);
				bitField.not();
				bitField.copyTo (fieldBytes, 0);
				assertArrayEquals (new byte[] { (byte)(~i), (byte)((~(Integer.reverse (j) >>> 24)) & (0xff << 1)) } , fieldBytes);
			}
		}

	}


	/**
	 * Test not on a 16 bit field
	 */
	@Test
	public void testNot16() {

		byte[] fieldBytes = new byte[2];
		for (int i = 0; i <= 255; i++) {
			for (int j = 0; j <= 255; j++) {
				BitField bitField = new BitField (new byte[] { (byte)i, (byte)j }, 16);
				bitField.not();
				bitField.copyTo (fieldBytes, 0);
				assertArrayEquals (new byte[] { (byte)(~i), (byte)(~j) } , fieldBytes);
			}
		}

	}


	/**
	 * Test AND of two 0 bit fields
	 */
	@Test
	public void testAnd0() {

		BitField bitField1 = new BitField (0);
		BitField bitField2 = new BitField (0);
		bitField1.and (bitField2);

	}


	/**
	 * Test AND of two 1 bit fields
	 */
	@Test
	public void testAnd1() {

		BitField bitField1 = new BitField (1);
		BitField bitField2 = new BitField (1);

		// 0 AND 0
		bitField1.and (bitField2);
		assertEquals (false, bitField1.get (0));
		assertEquals (false, bitField2.get (0));

		// 0 AND 1
		bitField1.clear (0);
		bitField2.set (0);
		bitField1.and (bitField2);
		assertEquals (false, bitField1.get (0));
		assertEquals (true, bitField2.get (0));

		// 1 AND 0
		bitField1.set (0);
		bitField2.clear (0);
		bitField1.and (bitField2);
		assertEquals (false, bitField1.get (0));
		assertEquals (false, bitField2.get (0));

		// 1 AND 1
		bitField1.set (0);
		bitField2.set (0);
		bitField1.and (bitField2);
		assertEquals (true, bitField1.get (0));
		assertEquals (true, bitField2.get (0));

	}


	/**
	 * Test AND of two 8 bit fields
	 */
	@Test
	public void testAnd8() {

		BitField bitField1 = new BitField (8);
		BitField bitField2 = new BitField (8);

		// 0000 0000 AND 0000 0000
		bitField1.and (bitField2);
		assertEquals (0, bitField1.cardinality());
		assertEquals (0, bitField2.cardinality());

		// 0000 0000 AND 1111 1111
		bitField2.not();
		bitField1.and (bitField2);
		assertEquals (0, bitField1.cardinality());
		assertEquals (8, bitField2.cardinality());

		// 1111 1111 AND 0000 0000
		bitField1.not();
		bitField2.not();
		bitField1.and (bitField2);
		assertEquals (0, bitField1.cardinality());
		assertEquals (0, bitField2.cardinality());
		
		// 1111 1111 AND 1111 1111
		bitField1.not();
		bitField2.not();
		bitField1.and (bitField2);
		assertEquals (8, bitField1.cardinality());
		assertEquals (8, bitField2.cardinality());

	}


	/**
	 * Test AND of a 1 bit field and a 2 bit field
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testAndDifferingSizes12() {

		BitField bitField1 = new BitField (1);
		BitField bitField2 = new BitField (2);
		bitField1.and (bitField2);

	}


	/**
	 * Test AND of an 8 bit field and a 9 bit field
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testAndDifferingSizes89() {

		BitField bitField1 = new BitField (8);
		BitField bitField2 = new BitField (9);
		bitField1.and (bitField2);

	}


	/**
	 * Tests the iterator of a 0 bit field
	 */
	@Test
	public void testIterator0() {

		BitField bitField = new BitField (0);

		for (int index : bitField) {
			fail ("" + index);
		}

	}


	/**
	 * Tests the iterator of a 1 bit field
	 */
	@Test
	public void testIterator1() {

		BitField bitField = new BitField (1);

		for (int index : bitField) {
			fail ("" + index);
		}

		bitField.set (0);

		int count = 0;
		int lastIndex = -1;
		for (int index : bitField) {
			count++;
			lastIndex = index;
		}

		assertEquals (1, count);
		assertEquals (0, lastIndex);

	}


	/**
	 * Tests the iterator of an 8 bit field
	 */
	@Test
	public void testIterator8() {

		BitField bitField = new BitField (8);

		for (int index : bitField) {
			fail ("" + index);
		}

		bitField.set (0);
		bitField.set (2);
		bitField.set (4);
		bitField.set (6);
		List<Integer> indices = new ArrayList<Integer>();
		for (int index : bitField) {
			indices.add (index);
		}
		assertArrayEquals (new Integer[] {0, 2, 4, 6}, indices.toArray());

		indices.clear();
		bitField.not ();
		for (int index : bitField) {
			indices.add (index);
		}
		assertArrayEquals (new Integer[] {1, 3, 5, 7}, indices.toArray());

		indices.clear();
		bitField.set (0);
		bitField.set (2);
		bitField.set (4);
		bitField.set (6);
		for (int index : bitField) {
			indices.add (index);
		}
		assertArrayEquals (new Integer[] {0, 1, 2, 3, 4, 5, 6, 7}, indices.toArray());
	}


	/**
	 * Tests moving an iterator off the end of the bitfield
	 */
	@Test(expected=NoSuchElementException.class)
	public void testIteratorTooFar() {

		BitField bitField = new BitField(1).not();

		Iterator<Integer> iterator = bitField.iterator();

		iterator.next();
		iterator.next();

	}


	/**
	 * Tests iterator.remove()
	 */
	@Test(expected=UnsupportedOperationException.class)
	public void testIteratorRemove() {

		BitField bitField = new BitField(1).not();

		Iterator<Integer> iterator = bitField.iterator();

		iterator.next();
		iterator.remove();

	}


	/**
	 * Tests intersect()
	 */
	@Test
	public void testIntersects() {

		BitField bitField1 = new BitField (8);
		BitField bitField2 = new BitField (8);

		assertFalse (bitField1.intersects (bitField2));

		bitField1.set (0);
		assertFalse (bitField1.intersects (bitField2));

		bitField2.set (7);
		assertFalse (bitField1.intersects (bitField2));

		bitField2.set (0);
		assertTrue (bitField1.intersects (bitField2));

		bitField1.set (7);
		assertTrue (bitField1.intersects (bitField2));

	}


	/**
	 * Tests intersect() on different size bitfields
	 */
	@Test
	public void testIntersectsDifferingSizes1() {

		BitField bitField1 = new BitField (8);
		BitField bitField2 = new BitField (9);

		bitField1.set (7);
		bitField2.set (8);

		assertFalse (bitField1.intersects (bitField2));

	}


	/**
	 * Tests intersect() on different size bitfields
	 */
	@Test
	public void testIntersectsDifferingSizes2() {

		BitField bitField1 = new BitField (8);
		BitField bitField2 = new BitField (9);

		bitField1.set (7);
		bitField2.set (7);

		assertTrue (bitField1.intersects (bitField2));

	}


	/**
	 * Tests equality to null
	 */
	@Test
	public void testEqualsNull() {

		BitField bitField1 = new BitField (8);
		assertFalse (bitField1.equals (null));

	}


	/**
	 * Tests equality to self
	 */
	@Test
	public void testEqualsSelf() {

		BitField bitField1 = new BitField (8);
		assertTrue (bitField1.equals (bitField1));

	}


	/**
	 * Tests equality to an unequal bitfield
	 */
	@Test
	public void testEqualsDifferent() {

		BitField bitField1 = new BitField (8);
		BitField bitField2 = new BitField (8).not();

		assertFalse (bitField1.equals (bitField2));

	}


	/**
	 * Tests equality to an unequal bitfield
	 */
	@Test
	public void testEqualsDifferentLength() {

		BitField bitField1 = new BitField (8);
		BitField bitField2 = new BitField (7);

		assertFalse (bitField1.equals (bitField2));

	}


	/**
	 * Tests equality to an identical bitfield
	 */
	@Test
	public void testEqualsIdentical() {

		BitField bitField1 = new BitField (8);
		BitField bitField2 = new BitField (8);

		assertTrue (bitField1.equals (bitField2));

	}


	/**
	 * Tests toString on a zero length bitfield
	 */
	@Test
	public void testToStringBlank() {

		assertEquals ("BitField[ 0:  ]", new BitField(0).toString());

	}


	/**
	 * Tests toString on a 1 bit bitfield
	 */
	@Test
	public void testToString1Zero() {

		assertEquals ("BitField[ 1: 0 ]", new BitField(1).toString());

	}


	/**
	 * Tests toString on a 1 bit bitfield
	 */
	@Test
	public void testToString1One() {

		assertEquals ("BitField[ 1: 1 ]", new BitField(1).not().toString());

	}


	/**
	 * Tests toString on an 8 bit bitfield
	 */
	@Test
	public void testToString8One() {

		assertEquals ("BitField[ 8: 1111 1111 ]", new BitField(8).not().toString());

	}


	/**
	 * Tests extend on a 7 bit bitfield
	 */
	@Test
	public void testExtend7() {

		BitField bitField = new BitField(7).not();
		bitField.extend (8);

		assertFalse (bitField.get (7));
		assertEquals (1, bitField.byteLength());
		byte[] bytes = new byte[1];
		bitField.copyTo (bytes, 0);
		assertArrayEquals (new byte[] { (byte)~1 }, bytes);

	}


	/**
	 * Tests extend on an 8 bit bitfield
	 */
	@Test
	public void testExtend8() {

		BitField bitField = new BitField(8).not();
		bitField.extend (9);

		assertFalse (bitField.get (8));
		assertEquals (2, bitField.byteLength());
		byte[] bytes = bitField.content();
		assertArrayEquals (new byte[] { (byte)~0, 0 }, bytes);

	}


}
