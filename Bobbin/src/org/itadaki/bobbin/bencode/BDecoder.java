package org.itadaki.bobbin.bencode;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Decodes data in bencode format<br>
 * <br>
 * Limits and limitations:<br>
 * <ul>
 *   <li>Will refuse to decode a binary string greater than Integer.MAX_VALUE (2^31-1) in size
 *       (although typically heap size will prevent decoding of much smaller strings than that)</li>
 *   <li>Will refuse to decode an integer greater than 80 input characters in size. This is
 *       sufficient for a 256-bit number</li>
 * </ul>
 */
public class BDecoder {

	/**
	 * The length of the biggest binary string that can be decoded, as a BigInteger
	 */
	private static BigInteger MAXIMUM_BINARY_LENGTH = new BigInteger ("" + Integer.MAX_VALUE);

	/**
	 * The input stream to read bencoded data from
	 */
	private InputStream input = null;

	/**
	 * The next byte from the input stream, if available (or -1 if not)  
	 */
	private int storedByte = -1;


	/**
	 * Returns the next byte to be processed from the stream
	 *
	 * @return the next byte, or -1 if no more bytes are available
	 * @throws EOFException if there are no more bytes to be read
	 * @throws IOException on an error reading from the InputStream
	 */
	private int getStoredByte() throws IOException {

		if (this.storedByte == -1) {
			this.storedByte = readByte();
		}

		return this.storedByte;

	}


	/**
	 * Resets the next byte to be processed, so a fresh byte will be read on the next call to
	 * getNextByte()
	 */
	private void resetStoredByte() {

		this.storedByte = -1;

	}


	/**
	 * Reads a byte from the input stream
	 *
	 * @return The byte read
	 * @throws EOFException if there are no more bytes to be read
	 * @throws IOException on an error reading from the InputStream
	 */
	private int readByte() throws IOException {

		int character = this.input.read();

		if (character == -1) {
			throw new EOFException();
		}

		return character;

	}


	/**
	 * Reads bytes from the input stream
	 * 
	 * @param length The number of bytes to read
	 * @return The bytes read
	 * @throws EOFException if there are no more bytes to be read
	 * @throws IOException on an error reading from the InputStream
	 */
	private byte[] readBytes (int length) throws IOException {

		byte[] bytes = new byte[length];
		int totalBytesRead = 0;

		while (totalBytesRead < length) {
			int bytesRead = this.input.read (bytes, totalBytesRead, length - totalBytesRead);
			if (bytesRead == -1) {
				throw new EOFException();
			}
			totalBytesRead += bytesRead;
		}

		return bytes;

	}


	/**
	 * Decodes a binary string
	 *
	 * @return A BValue equivalent to the binary string read
	 * @throws IOException on an error reading from the InputStream
	 * @throws InvalidEncodingException if the type of the next bencode object in the stream was not
	 *         binary, invalid bencode data was encountered, or the binary contente were larger than
	 *         2^31-1 bytes
	 */
	public BBinary decodeBinary() throws IOException {

		// Valid patterns are:
		//   ^0:$
		//   ^[1-9][0-9]*:<data...>$

		// 10 characters is enough for a binary string of 2^32 bytes
		char[] numberChars = new char[10];
		int numberLength = 0;
		int character = getStoredByte();

		// Read the binary string length
		if (character >= '1' && character <= '9') {
			// ... [1-9][0-9]*
			do {
				numberChars[numberLength++] = (char) character;
				character = readByte();
			} while (character >= '0' && character <= '9');
		} else if (character == '0') {
			// ... 0
			numberChars[numberLength++] = (char) character;
			character = readByte();
		} else {
			// There must be one or more number characters
			throw new InvalidEncodingException ("Expected [0-9], but read 0x" + Integer.toHexString (character & 0xff));
		}

		BigInteger binaryLength = new BigInteger (new String (numberChars, 0, numberLength));
		if (binaryLength.compareTo (MAXIMUM_BINARY_LENGTH) == 1) {
			throw new InvalidEncodingException ("Cannot decode a binary string of greater than 2^32-1 bytes");
		}

		// Next character must be ':'
		if (character != ':') {
			throw new InvalidEncodingException ("Expected ':', but read 0x" + Integer.toHexString (character & 0xff));
		}

		// Read the data
		byte[] binaryString = readBytes (binaryLength.intValue());

		return new BBinary (binaryString);

	}


	/**
	 * Decodes an integer
	 *
	 * @return A BValue equivalent to the integer read
	 * @throws IOException on an error reading from the InputStream
	 * @throws InvalidEncodingException if the type of the next bencode object in the stream was not
	 *         integer, invalid bencode data was encountered, or the integer was larger than 80
	 *         input characters
	 */
	public BInteger decodeInteger() throws IOException {

		// Valid patterns are:
		//   ^i0e$
		//   ^i-?[1-9][0-9]*e$

		// 80 characters is large enough for a 256-bit number. Is anyone crazy enough to use one larger?
		char[] numberChars = new char[80];
		int numberLength = 0;
		int character;

		// First character must be 'i'
		character = getStoredByte();
		if (character != 'i') {
			throw new InvalidEncodingException ("Expected 'i', but read 0x" + Integer.toHexString (character & 0xff));
		}

		// Second character may be '-'
		character = readByte();
		if (character == '-') {
			numberChars[numberLength++] = (char) character;
			character = readByte();
			// Character following '-' may not be '0'
			if (character == '0') {
				throw new InvalidEncodingException ("A bencode integer cannot begin '-0...'");
			}
		}

		// Read any remaining number characters
		if (character >= '1' && character <= '9') {
			// ... [1-9][0-9]*
			do {
				numberChars[numberLength++] = (char) character;
				character = readByte();
			} while (character >= '0' && character <= '9');
		} else if (character == '0') {
			// ... 0
			numberChars[numberLength++] = (char) character;
			character = readByte();
		} else {
			// There must be one or more number characters
			throw new InvalidEncodingException ("A bencode integer cannot be zero-length");
		}

		// Last character must be 'e'
		if (character != 'e') {
			throw new InvalidEncodingException ("Expected 'e', but read 0x" + Integer.toHexString (character & 0xff));
		}

		resetStoredByte();

		return new BInteger (new BigInteger (new String (numberChars, 0, numberLength)));

	}


	/**
	 * Decodes a list
	 *
	 * @return A BValue equivalent to the list read
	 * @throws IOException on an error reading from the InputStream
	 * @throws InvalidEncodingException if the type of the next bencode object in the stream was not
	 *         list, or invalid bencode data was encountered
	 */
	public BList decodeList() throws IOException {

		List<BValue> values = new ArrayList<BValue>();
		int character;

		// First character must be 'l'
		character = getStoredByte();
		if (character != 'l') {
			throw new InvalidEncodingException ("Expected 'l', but read 0x" + Integer.toHexString (character & 0xff));
		}

		// Read list contents
		resetStoredByte();
		character = getStoredByte();
		while (character != 'e') {
			BValue value = decode();
			values.add (value);
			resetStoredByte();
			character = getStoredByte();
		}

		resetStoredByte();

		return new BList (values);

	}


	/**
	 * Decodes a dictionary
	 *
	 * @return a BValue equivalent to the dictionary read
	 * @throws IOException on an error reading from the InputStream
	 * @throws InvalidEncodingException if the type of the next bencode object in the stream was not
	 *         dictionary, or invalid bencode data was encountered
	 */
	public BDictionary decodeDictionary() throws IOException {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		int character;

		// First character must be 'd'
		character = getStoredByte();
		if (character != 'd') {
			throw new InvalidEncodingException ("Expected 'd', but read 0x" + Integer.toHexString (character & 0xff));
		}

		// Read dictionary contents
		resetStoredByte();
		character = getStoredByte();
		while (character != 'e') {
			// Dictionary key must be a binary string
			BBinary key = decodeBinary();
			resetStoredByte();
			character = getStoredByte();
			BValue value = decode();
			resetStoredByte();
			character = getStoredByte();
			map.put (key, value);
		}

		resetStoredByte();

		return new BDictionary (map);

	}


	/**
	 * Decodes the next bencode value from the input stream<br>
	 * If an exception is thrown, the state of the input stream is undefined
	 * 
	 * @return A BValue equivalent to the encoded data
	 * @throws IOException on an error reading from the InputStream
	 * @throws InvalidEncodingException if invalid bencode data was encountered, or an integer
	 *         greater than 80 input characters, or a binary string greater than 2^31-1 bytes
	 */
	public BValue decode() throws IOException {

		int type = getStoredByte();

		if (type >= '0' && type <= '9') {
			return decodeBinary();
		} else if (type == 'i') {
			return decodeInteger();
		} else if (type == 'l') {
			return decodeList();
		} else if (type == 'd') {
			return decodeDictionary();
		}

		throw new InvalidEncodingException ("Expected '[0-9ild]', but read 0x" + Integer.toHexString (type & 0xff));

	}


	/**
	 * Decodes bencode format data
	 * 
	 * @param encodedData The encoded data
	 * @return A BValue equivalent to the encoded data
	 * @throws EOFException if the bencode data was apparently valid, but ended prematurely
	 * @throws InvalidEncodingException if invalid bencode data was encountered, or an integer
	 *         greater than 80 input characters, or a binary string greater than 2^31-1 bytes
	 */
	public static BValue decode (byte[] encodedData) throws EOFException, InvalidEncodingException {

		ByteArrayInputStream input = new ByteArrayInputStream (encodedData);

		try {
			return new BDecoder(input).decode();
		} catch (InvalidEncodingException e) {
			throw e;
		} catch (EOFException e) {
			throw e;
		} catch (IOException e) {
			// ByteArrayInputStream does not actually throw IOException
			throw new InternalError (e.toString());
		}

	}


	/**
	 * Creates a BDecoder that reads from the specified byte array
	 * 
	 * @param encodedData The byte array to read from
	 */
	public BDecoder (byte[] encodedData) {

		this.input = new ByteArrayInputStream (encodedData);

	}


	/**
	 * Creates a BDecoder that reads from the specified input stream
	 * 
	 * @param input The input stream to read from
	 */
	public BDecoder (InputStream input) {

		this.input = input;

	}


}
