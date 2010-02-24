package test.bencode;

import static org.junit.Assert.*;

import java.util.SortedMap;
import java.util.TreeMap;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.bencode.BValue;
import org.junit.Test;



/**
 * Test BValue
 */
public class TestBValue {

	/**
	 * Tests toString on a BBinary with string data
	 */
	@Test
	public void testBinaryToStringString() {

		BBinary binary = new BBinary ("Test");

		assertEquals ("binary: \"Test\"\n", binary.toString());

	}


	/**
	 * Tests toString on a BBinary with binary data
	 */
	@Test
	public void testBinaryToStringBinary() {

		BBinary binary = new BBinary (new byte[] {2, 3});

		assertEquals ("binary: \"<0x2><0x3>\"\n", binary.toString());

	}

	/**
	 * Tests toString on a BBinary with long content
	 */
	@Test
	public void testBinaryToStringLong() {

		BBinary binary = new BBinary ("ABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJK");

		assertEquals ("binary: \"ABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJ...\"\n", binary.toString());

	}


	/**
	 * Tests toString on a BInteger
	 */
	@Test
	public void testIntegerToString() {

		BInteger binary = new BInteger (1234);

		assertEquals ("integer: 1234\n", binary.toString());

	}


	/**
	 * Tests toString on an empty BList
	 */
	@Test
	public void testListToStringEmpty() {

		BList list = new BList();

		assertEquals ("list:\n\n", list.toString());
	}


	/**
	 * Tests toString on a BList
	 */
	@Test
	public void testListToString() {

		BList list = new BList (new BBinary ("Test"), new BInteger (1234));

		assertEquals ("list:\n  binary: \"Test\"\n  integer: 1234\n", list.toString());
	}


	/**
	 * Tests toString on an empty BDictionary
	 */
	@Test
	public void testDictionaryToStringEmpty() {

		BDictionary dictionary = new BDictionary();

		assertEquals ("dictionary:\n\n", dictionary.toString());

	}


	/**
	 * Tests toString on a BDictionary
	 */
	@Test
	public void testDictionaryToString() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("Key 1"), new BInteger (1234));
		BDictionary dictionary = new BDictionary (map);

		assertEquals ("dictionary:\n  \"Key 1\" => integer: 1234\n", dictionary.toString());

	}


	/**
	 * Tests comparing a BValue to null
	 */
	@Test
	public void testEqualsNull() {

		BValue value = new BBinary ("Test");

		assertFalse (value.equals (null));

	}


	/**
	 * Tests comparing a BValue to itself
	 */
	@Test
	public void testEqualsSame() {

		BValue value = new BBinary ("Test");

		assertTrue (value.equals (value));
	}


	/**
	 * Tests comparing a BValue to a different type
	 */
	@Test
	public void testEqualsDifferent() {

		BValue value = new BInteger (1);

		assertFalse (value.equals (new Integer (1)));

	}

}
