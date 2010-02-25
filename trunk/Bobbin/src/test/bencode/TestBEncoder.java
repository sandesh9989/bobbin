
/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.bencode;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BEncoder;
import org.itadaki.bobbin.bencode.BInteger;
import org.junit.Test;



/**
 * Test BEncoder
 */
public class TestBEncoder {

	/* Tests on plain Integers */

	/**
	 * Encode an integer 0 
	 */
	@Test
	public void testInteger0() {

		byte[] expectedBytes = {'i', '0', 'e'};

		byte[] encodedBytes = BEncoder.encodeInteger (0);

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode a BValue integer 0 
	 */
	@Test
	public void testBValueInteger0() {

		byte[] expectedBytes = {'i', '0', 'e'};

		byte[] encodedBytes = BEncoder.encode (new BInteger (0));

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode an integer 42
	 */
	@Test
	public void testInteger42() {

		byte[] expectedBytes = {'i', '4', '2', 'e'};

		byte[] encodedBytes = BEncoder.encodeInteger (42);

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode a BValue integer 42
	 */
	@Test
	public void testBValueInteger42() {

		byte[] expectedBytes = {'i', '4', '2', 'e'};

		byte[] encodedBytes = BEncoder.encode (new BInteger (42));

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode an integer -1
	 */
	@Test
	public void testIntegerMinus1() {

		byte[] expectedBytes = {'i', '-', '1', 'e'};

		byte[] encodedBytes = BEncoder.encodeInteger (-1);

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode a BValue integer -1
	 */
	@Test
	public void testBValueIntegerMinus1() {

		byte[] expectedBytes = {'i', '-', '1', 'e'};

		byte[] encodedBytes = BEncoder.encode (new BInteger (-1));

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/* Tests on plain strings */

	/**
	 * Encode a blank string as a binary string
	 */
	@Test
	public void testStringAsByteStringBlank() {

		byte[] expectedBytes = {'0', ':'};

		byte[] encodedBytes = BEncoder.encodeBinary ("");

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode a BValue blank string as a binary string
	 */
	@Test
	public void testBValueStringAsByteStringBlank() {

		byte[] expectedBytes = {'0', ':'};

		byte[] encodedBytes = BEncoder.encode (new BBinary (""));

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode "Hello" as a binary string 
	 */
	@Test
	public void testStringAsBinaryHello() {

		byte[] expectedBytes = {'5', ':', 'H', 'e', 'l', 'l', 'o'};

		byte[] encodedBytes = BEncoder.encodeBinary ("Hello");

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode a BValue "Hello" as a binary string 
	 */
	@Test
	public void testBValueStringAsBinaryHello() {

		byte[] expectedBytes = {'5', ':', 'H', 'e', 'l', 'l', 'o'};

		byte[] encodedBytes = BEncoder.encode (new BBinary ("Hello"));

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode some Japanese as a binary string (checks we are counting bytes, not characters)
	 * @throws Exception
	 */
	@Test
	public void testStringAsBinaryJapanese() throws Exception {

		byte[] expectedBytes = "9:日本語".getBytes ("UTF-8");

		byte[] encodedBytes = BEncoder.encodeBinary ("日本語");

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode a byte array as a binary string 
	 */
	@Test
	public void testBytesAsBinary() {

		byte[] expectedBytes = {'5', ':', 0, 1, 2, 3, 4};

		byte[] encodedBytes = BEncoder.encodeBinary (new byte[] {0, 1, 2, 3, 4});

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encode a BValue byte array as a binary string 
	 */
	@Test
	public void testBValueBytesAsBinary() {

		byte[] expectedBytes = {'5', ':', 0, 1, 2, 3, 4};

		byte[] encodedBytes = BEncoder.encode (new BBinary (new byte[] {0, 1, 2, 3, 4}));

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/* Tests for lists */

	/**
	 * Encodes an empty list
	 */
	@Test
	public void testEmptyList() {

		byte[] expectedBytes = { 'l', 'e' };

		List<Object> list = new ArrayList<Object>();
		byte[] encodedBytes = BEncoder.encodeList (list);

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encodes a list of one integer
	 */
	@Test
	public void testListOneInteger() {

		byte[] expectedBytes = { 'l', 'i', '4', '2', 'e', 'e' };

		List<Integer> list = new ArrayList<Integer>();
		list.add (42);
		byte[] encodedBytes = BEncoder.encodeList (list);

		assertArrayEquals(expectedBytes, encodedBytes);		

	}


	/**
	 * Encodes a list of many integers
	 */
	@Test
	public void testListManyIntegers() {

		byte[] expectedBytes = {
				'l',
				'i', '-', '1', 'e',
				'i', '4', '2', 'e',
				'i', '7', 'e',
				'e'		
		};

		List<Integer> list = new ArrayList<Integer>();
		list.add (-1);
		list.add (42);
		list.add (7);
		byte[] encodedBytes = BEncoder.encodeList (list);

		assertArrayEquals(expectedBytes, encodedBytes);		

	}


	/**
	 * Encodes a list of one string
	 */
	@Test
	public void testListOneString() {

		byte[] expectedBytes = {
				'l',
				'5', ':', 'H', 'e', 'l', 'l', 'o',
				'e'
		};

		List<String> list = new ArrayList<String>();
		list.add ("Hello");
		byte[] encodedBytes = BEncoder.encodeList (list);

		assertArrayEquals(expectedBytes, encodedBytes);		

	}


	/**
	 * Encodes a list of many strings
	 */
	@Test
	public void testListManyStrings() {

		byte[] expectedBytes = {
				'l',
				'5', ':', 'H', 'e', 'l', 'l', 'o',
				'3', ':', 't', 'h', 'e',
				'5', ':', 'W', 'o', 'r', 'l', 'd',
				'e'
		};

		List<String> list = new ArrayList<String>();
		list.add ("Hello");
		list.add ("the");
		list.add ("World");
		byte[] encodedBytes = BEncoder.encodeList (list);

		assertArrayEquals(expectedBytes, encodedBytes);		
	}


	/**
	 * Encodes a mixed list of strings and integers
	 */
	@Test
	public void testListMixedIntegersAndStrings() {

		byte[] expectedBytes = {
				'l',
				'i', '1', '2', '3', '4', 'e',
				'4', ':', 'T', 'e', 's', 't',
				'i', '4', '2', 'e',
				'i', '-', '1', 'e',
				'6', ':', 'S', 't', 'r', 'i', 'n', 'g',
				'e'
		};

		List<Object> list = new ArrayList<Object>();
		list.add (new Integer (1234));
		list.add ("Test");
		list.add (new Integer (42));
		list.add (new Integer (-1));
		list.add ("String");
		byte[] encodedBytes = BEncoder.encodeList (list);

		assertArrayEquals(expectedBytes, encodedBytes);		

	}


	/**
	 * Encodes a list containing a dictionary
	 */
	@Test
	public void testListContainingDictionary() {

		byte[] expectedBytes = {
				'l',
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'e',
				'e'
		};

		List<Object> list = new ArrayList<Object>();
		SortedMap<String, Object> map = new TreeMap<String, Object>();
		map.put ("Test", new Integer (1234));
		list.add (map);
		byte[] encodedBytes = BEncoder.encodeList (list);

		assertArrayEquals(expectedBytes, encodedBytes);		

	}


	/**
	 * Encodes a list containing a list
	 */
	@Test
	public void testListContainingList() {

		byte[] expectedBytes = {
				'l',
				'l',
				'5', ':', 'H', 'e', 'l', 'l', 'o',
				'5', ':', 'W', 'o', 'r', 'l', 'd',
				'e',
				'e'
		};

		List<Object> list = new ArrayList<Object>();
		List<Object> innerList = new ArrayList<Object>();
		innerList.add ("Hello");
		innerList.add ("World");
		list.add (innerList);
		byte[] encodedBytes = BEncoder.encodeList (list);

		assertArrayEquals(expectedBytes, encodedBytes);		

	}


	/* Tests for dictionaries */

	/**
	 * Encodes an empty dictionary 
	 */
	@Test
	public void testEmptyDictionary() {

		byte[] expectedBytes = { 'd', 'e' };

		SortedMap<String, Object> map = new TreeMap<String, Object>(); 
		byte[] encodedBytes = BEncoder.encodeDictionary (map);

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encodes a dictionary with one integer 
	 */
	@Test
	public void testDictionaryOneInteger() {

		byte[] expectedBytes = {
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'e'
		};

		SortedMap<String, Object> map = new TreeMap<String, Object>();
		map.put ("Test", new Integer (1234));
		byte[] encodedBytes = BEncoder.encodeDictionary (map);

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encodes a dictionary with mixed scalar values
	 */
	@Test
	public void testDictionaryMixedScalars() {

		byte[] expectedBytes = {
				'd',
				'6', ':', 'a', 'n', 's', 'w', 'e', 'r',
				'i', '4', '2', 'e',
				'3', ':', 'c', 'o', 'w',
				'5', ':', 'd', 'a', 'i', 's', 'y',
				'4', ':', 'f', 'i', 's', 'h',
				'6', ':', 'S', 'a', 'l', 'm', 'o', 'n',
				'6', ':', 'z', 'e', 'r', 'o', 'e', 's',
				'2', ':', 0, 0,
				'e'
		};

		SortedMap<String, Object> map = new TreeMap<String, Object>();
		map.put ("fish", "Salmon");
		map.put ("cow", "daisy");
		map.put ("answer", new Integer (42));
		map.put ("zeroes", new byte[] {0, 0});
		byte[] encodedBytes = BEncoder.encodeDictionary (map);

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encodes a dictionary containing a dictionary 
	 */
	@Test
	public void testDictionaryContainingDictionary() {

		byte[] expectedBytes = {
				'd',
				'4', ':', 'f', 'i', 's', 'h',
				'd',
				'5', ':', 'W', 'a', 'n', 'd', 'a',
				'i', '1', '2', '3', '4', 'e',
				'e',
				'e'
		};

		SortedMap<String, Object> map = new TreeMap<String, Object>();
		SortedMap<String, Object> innerMap = new TreeMap<String, Object>();
		innerMap.put ("Wanda", new Integer (1234));
		map.put ("fish", innerMap);
		byte[] encodedBytes = BEncoder.encodeDictionary (map);

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encodes a dictionary containing a list
	 */
	@Test
	public void testDictionaryContainingList() {

		byte[] expectedBytes = {
				'd',
				'5', ':', 'H', 'e', 'l', 'l', 'o',
				'l',
				'3', ':', 't', 'h', 'e',
				'5', ':', 'W', 'o', 'r', 'l', 'd',
				'e',
				'e'
		};

		SortedMap<String, Object> map = new TreeMap<String, Object>();
		List<Object> innerList = new ArrayList<Object>();
		innerList.add ("the");
		innerList.add ("World");
		map.put ("Hello", innerList);
		byte[] encodedBytes = BEncoder.encodeDictionary (map);

		assertArrayEquals(expectedBytes, encodedBytes);

	}


	/**
	 * Encodes a list containing an invalid type
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidList() {

		List<Object> list = new ArrayList<Object>();
		list.add (new Date());
		BEncoder.encodeList (list);

	}


	/**
	 * Tests encode(OutputStream,BValue)
	 * @throws Exception
	 */
	@Test
	public void testEncodeOutputStreamBValue() throws Exception{

		byte[] expectedBytes = {'5', ':', 'H', 'e', 'l', 'l', 'o'};

		BBinary binary = new BBinary ("Hello");

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		BEncoder.encode (outputStream, binary); 

		assertArrayEquals (expectedBytes, outputStream.toByteArray());

	}


}
