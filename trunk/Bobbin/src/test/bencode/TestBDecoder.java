/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.bencode;

import static org.junit.Assert.*;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDecoder;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.bencode.BValue;
import org.itadaki.bobbin.bencode.InvalidEncodingException;
import org.junit.Test;



/**
 * Test BDecoder
 */
public class TestBDecoder {

	/* Tests on integers */

	/**
	 * Decode a valid integer 0
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testInteger0() throws IOException {

		byte[] encodedData = {'i', '0', 'e'};

		BInteger integer = new BDecoder(encodedData).decodeInteger();

		assertEquals (0, integer.value().intValue());

	}


	/**
	 * Decode a valid integer 42
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testInteger42() throws IOException {

		byte[] encodedData = {'i', '4', '2', 'e'};

		BInteger integer = new BDecoder(encodedData).decodeInteger();

		assertEquals (42, integer.value().intValue());

	}


	/**
	 * Decode a valid integer -1
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testIntegerMinus1() throws IOException {

		byte[] encodedData = {'i', '-', '1', 'e'};

		BInteger integer = new BDecoder(encodedData).decodeInteger();

		assertEquals (-1, integer.value().intValue());

	}


	/**
	 * Decode an invalid integer -0
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testIntegerMinus0() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'i', '-', '0', 'e'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode an invalid integer 01
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testInteger01() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'i', '0', '1', 'e'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode an invalid integer 01
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testIntegerBlank() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'i', 'e'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode an invalid integer 1-1
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testInteger1Minus1() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'i', '1', '-', '1', 'e'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode an invalid integer '^i$'
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = EOFException.class)
	public void testIntegerI() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'i'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode an invalid integer '^i-$'
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = EOFException.class)
	public void testIntegerIMinus() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'i', '-'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode an invalid integer '^i0$'
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = EOFException.class)
	public void testIntegerI0() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'i', '0'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode a valid integer incorrectly as a binary string
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testIntegerDecodeAsBinary() throws IOException {

		byte[] encodedData = {'i', '0', 'e'};

		new BDecoder(encodedData).decodeBinary();

	}


	/**
	 * Decode a valid integer incorrectly as a list
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testIntegerDecodeAsList() throws IOException {

		byte[] encodedData = {'i', '0', 'e'};

		new BDecoder(encodedData).decodeList();

	}


	/**
	 * Decode a valid integer incorrectly as a dictionary
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testIntegerDecodeAsDictionary() throws IOException {

		byte[] encodedData = {'i', '0', 'e'};

		new BDecoder(encodedData).decodeDictionary();

	}


	/* Tests on binary strings */

	/**
	 * Decode a valid zero-length binary string
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testBinaryBlank() throws IOException {

		byte[] encodedData = {'0', ':'};

		BBinary binary = new BDecoder(encodedData).decodeBinary();

		assertArrayEquals (new byte[] {}, binary.value());

	}


	/**
	 * Decode a valid non-zero length binary string
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testBinaryNonBlank() throws IOException {

		byte[] encodedData = {'4', ':', 'a', 'b', 'c', 'd'};

		BBinary binary = new BDecoder(encodedData).decodeBinary();

		assertArrayEquals (new byte[] {'a', 'b', 'c', 'd'}, binary.value());

	}


	/**
	 * Decode a prematurely terminating binary string
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = EOFException.class)
	public void testBinaryEOFBeforeColon() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'4'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode a prematurely terminating binary string
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = EOFException.class)
	public void testBinaryEOFAfterColon() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'4', ':', 'a', 'b', 'c'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode a binary string with an invalid separator
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testBinaryInvalidSeparator() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'4', 'q', 'a', 'b', 'c', 'd'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode a too large binary string
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testBinaryTooLarge() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'2', '1', '4', '7', '4', '8', '3', '6', '4', '8', ':'};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode a valid binary string incorrectly as an integer
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testBinaryDecodeAsInteger() throws IOException {

		byte[] encodedData = {'4', ':', 'a', 'b', 'c', 'd'};

		new BDecoder(encodedData).decodeInteger();

	}


	/**
	 * Decode a valid binary string incorrectly as a list
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testBinaryDecodeAsList() throws IOException {

		byte[] encodedData = {'4', ':', 'a', 'b', 'c', 'd'};

		new BDecoder(encodedData).decodeList();

	}


	/**
	 * Decode a valid binary string incorrectly as a dictionary
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testBinaryDecodeAsDictionary() throws IOException {

		byte[] encodedData = {'4', ':', 'a', 'b', 'c', 'd'};

		new BDecoder(encodedData).decodeDictionary();

	}


	/* Tests on lists */

	/**
	 * Decode an valid, empty list
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testEmptyList() throws IOException {

		byte[] encodedData = {'l', 'e'};

		BList list = new BDecoder(encodedData).decodeList();

		assertEquals (0, list.value().size());

	}


	/**
	 * Decode a list of one integer
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testListOneInteger() throws IOException {

		byte[] encodedData = { 'l', 'i', '4', '2', 'e', 'e' };

		BList list = new BDecoder(encodedData).decodeList();

		assertEquals (1, list.value().size());
		assertEquals (42, ((BInteger)list.value().get(0)).value().intValue());

	}


	/**
	 * Decode a list of many integers
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testListManyIntegers() throws IOException {

		byte[] encodedData = {
				'l',
				'i', '-', '1', 'e',
				'i', '4', '2', 'e',
				'i', '7', 'e',
				'e'		
		};

		BList list = new BDecoder(encodedData).decodeList();

		List<? extends BValue> listValue = list.value();
		assertEquals (3, listValue.size());
		assertEquals (-1, ((BInteger)listValue.get(0)).value().intValue());
		assertEquals (42, ((BInteger)listValue.get(1)).value().intValue());
		assertEquals (7, ((BInteger)listValue.get(2)).value().intValue());

	}


	/**
	 * Decode a list of one binary string
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testListOneBinaryString() throws IOException {

		byte[] encodedData = {
				'l',
				'5', ':', 'H', 'e', 'l', 'l', 'o',
				'e'
		};

		BList list = new BDecoder(encodedData).decodeList();

		List<? extends BValue> listValue = list.value();
		assertEquals (1, listValue.size());
		assertArrayEquals (new byte[] {'H', 'e', 'l', 'l', 'o',}, ((BBinary)listValue.get(0)).value());

	}


	/**
	 * Decode a list of many binary strings
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testListManyBinaryStrings() throws IOException {

		byte[] encodedData = {
				'l',
				'5', ':', 'H', 'e', 'l', 'l', 'o',
				'3', ':', 't', 'h', 'e',
				'5', ':', 'W', 'o', 'r', 'l', 'd',
				'e'
		};

		BList list = new BDecoder(encodedData).decodeList();

		List<? extends BValue> listValue = list.value();
		assertEquals (3, listValue.size());
		assertArrayEquals (new byte[] {'H', 'e', 'l', 'l', 'o',}, ((BBinary)listValue.get(0)).value());
		assertArrayEquals (new byte[] {'t', 'h', 'e'}, ((BBinary)listValue.get(1)).value());
		assertArrayEquals (new byte[] {'W', 'o', 'r', 'l', 'd',}, ((BBinary)listValue.get(2)).value());

	}


	/**
	 * Decode a list of mixed integers and binary strings
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testListMixedIntegersAndBinaryStrings() throws IOException {

		byte[] encodedData = {
				'l',
				'i', '1', '2', '3', '4', 'e',
				'4', ':', 'T', 'e', 's', 't',
				'i', '4', '2', 'e',
				'i', '-', '1', 'e',
				'6', ':', 'S', 't', 'r', 'i', 'n', 'g',
				'e'
		};

		BList list = new BDecoder(encodedData).decodeList();

		List<? extends BValue> listValue = list.value();
		assertEquals (5, listValue.size());
		assertEquals (1234, ((BInteger)listValue.get(0)).value().intValue());
		assertArrayEquals (new byte[] {'T', 'e', 's', 't',}, ((BBinary)listValue.get(1)).value());
		assertEquals (42, ((BInteger)listValue.get(2)).value().intValue());
		assertEquals (-1, ((BInteger)listValue.get(3)).value().intValue());
		assertArrayEquals (new byte[] {'S', 't', 'r', 'i', 'n', 'g'}, ((BBinary)listValue.get(4)).value());

	}


	/**
	 * Decode a list containing a dictionary
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testListContainingDictionary() throws IOException {

		byte[] encodedData = {
				'l',
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'e',
				'e'
		};

		BList list = new BDecoder(encodedData).decodeList();

		List<? extends BValue> listValue = list.value();
		assertEquals (1, listValue.size());
		BDictionary dictionary = (BDictionary)listValue.get (0);
		assertEquals (1, dictionary.value().size());
		assertEquals (1234, ((BInteger)dictionary.value().get(new BBinary("Test"))).value().intValue());

	}


	/**
	 * Check that every initial subset of a list throws EOFException
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test
	public void testUnfinishedList() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {
				'l',
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'e',
				'e'
		};

		for (int i = 0; i < encodedData.length; i++) {
			boolean eofCaught = false;
			byte[] subset = new byte[i];
			System.arraycopy (encodedData, 0, subset, 0, i);
			try {
				BDecoder.decode (subset);
			} catch (EOFException e) {
				eofCaught = true;
			}
			assertTrue (eofCaught);
		}

	}


	/**
	 * Tests a list with an invalid terminator
	 * 
	 * @throws Exception
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testListInvalidTerminator() throws Exception {

		byte[] encodedData = {
				'l',
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'e',
				'E'
		};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode a valid list incorrectly as a binary string
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testListDecodeAsBinary() throws IOException {

		byte[] encodedData = { 'l', 'i', '4', '2', 'e', 'e' };

		new BDecoder(encodedData).decodeBinary();

	}


	/**
	 * Decode a valid list incorrectly as an integer
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testListDecodeAsInteger() throws IOException {

		byte[] encodedData = { 'l', 'i', '4', '2', 'e', 'e' };

		new BDecoder(encodedData).decodeInteger();

	}


	/**
	 * Decode a valid list incorrectly as a dictionary
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testListDecodeAsDictionary() throws IOException {

		byte[] encodedData = { 'l', 'i', '4', '2', 'e', 'e' };

		new BDecoder(encodedData).decodeDictionary();

	}


	/* Tests on dictionaries */

	/**
	 * Decode an valid, empty dictionary
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testEmptyDictionary() throws IOException {

		byte[] encodedData = {'d', 'e'};

		BDictionary dictionary = new BDecoder(encodedData).decodeDictionary();

		assertEquals (0, dictionary.value().size());

	}


	/**
	 * Decode an dictionary containing one integer
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testDictionaryOneInteger() throws IOException {

		byte[] encodedData = {
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'e'
		};

		BDictionary dictionary = new BDecoder(encodedData).decodeDictionary();

		assertEquals (1, dictionary.value().size());

	}


	/**
	 * Tests a dictionary with an invalid integer key type
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testDictionaryInvalidKeyInteger() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {
				'd',
				'i', '1', '2', '3', '4', 'e',
				'4', ':', 'T', 'e', 's', 't',
				'3', ':', 'c', 'o', 'w',
				'5', ':', 'D', 'a', 'i', 's', 'y',
				'e'
		};

		BDecoder.decode (encodedData);

	}


	/**
	 * Tests a dictionary with an invalid list key type
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testDictionaryInvalidKeyList() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {
				'd',
				'l',
				'4', ':', 'T', 'e', 's', 't',
				'e',
				'i', '1', '2', '3', '4', 'e',
				'3', ':', 'c', 'o', 'w',
				'5', ':', 'D', 'a', 'i', 's', 'y',
				'e'
		};

		BDecoder.decode (encodedData);

	}


	/**
	 * Tests a dictionary with an invalid list key type
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testDictionaryInvalidKeyDictionary() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {
				'd',
				'd', 
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'e',
				'5', ':', 'D', 'a', 'i', 's', 'y',
				'e'
		};

		BDecoder.decode (encodedData);

	}


	/**
	 * Check that every initial subset of a valid dictionary throws EOFException
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test
	public void testUnfinishedDictionary() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'3', ':', 'c', 'o', 'w',
				'5', ':', 'D', 'a', 'i', 's', 'y',
				'e'
		};

		for (int i = 0; i < encodedData.length; i++) {
			boolean eofCaught = false;
			try {
				byte[] subset = new byte[i];
				System.arraycopy (encodedData, 0, subset, 0, i);
				BDecoder.decode (subset);
			} catch (EOFException e) {
				eofCaught = true;
			}
			assertTrue (eofCaught);
		}

	}


	/**
	 * Tests a dictionary with an invalid terminator
	 * 
	 * @throws Exception 
	 */
	@Test(expected=InvalidEncodingException.class)
	public void testDictionaryInvalidTerminator() throws Exception {

		byte[] encodedData = {
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'3', ':', 'c', 'o', 'w',
				'5', ':', 'D', 'a', 'i', 's', 'y',
				'E'
		};

		BDecoder.decode (encodedData);

	}


	/**
	 * Decode a valid dictionary incorrectly as a binary string
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testDictionaryDecodeAsBinary() throws IOException {

		byte[] encodedData = {
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'e'
		};

		new BDecoder(encodedData).decodeBinary();

	}


	/**
	 * Decode a valid dictionary incorrectly as an integer
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testDictionaryDecodeAsInteger() throws IOException {

		byte[] encodedData = {
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'e'
		};

		new BDecoder(encodedData).decodeInteger();

	}


	/**
	 * Decode a valid dictionary incorrectly as a list
	 * 
	 * @throws IOException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testDictionaryDecodeAsList() throws IOException {

		byte[] encodedData = {
				'd',
				'4', ':', 'T', 'e', 's', 't',
				'i', '1', '2', '3', '4', 'e',
				'e'
		};

		new BDecoder(encodedData).decodeList();

	}


	/* Other tests */

	/**
	 * Decode an invalid string
	 * 
	 * @throws EOFException 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testInvalid() throws EOFException, InvalidEncodingException {

		byte[] encodedData = {'q'};

		BDecoder.decode (encodedData);

	}


}
