package org.itadaki.bobbin.bencode;

import static org.itadaki.bobbin.util.CharsetUtil.UTF8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.SortedMap;


/**
 * Encodes data in bencode format
 */
public class BEncoder {

	/**
	 * Encodes any bencodable object
	 * 
	 * @param output The OutputStream to write to
	 * @param object The object to encode
	 * @throws IOException if an error occurred writing to the output stream
	 */
	@SuppressWarnings("unchecked")
	private static void encodeAny (OutputStream output, Object object) throws IOException {

		if (object instanceof String) {
			encodeBinary (output, (String)object);
		} else if (object instanceof byte[]) {
			encodeBinary (output, (byte[])object);
		} else if (object instanceof Number) {
			encodeInteger (output, (Number)object);
		} else if (object instanceof List) {
			encodeList (output, (List<?>)object);
		} else if (object instanceof SortedMap) {
			encodeDictionary (output, (SortedMap<String, ?>)object);
		} else if (object instanceof BBinary) {
			encodeBinary (output, ((BBinary)object).value());
		} else if (object instanceof BInteger) {
			encodeInteger (output, ((BInteger)object).value());
		} else if (object instanceof BList) {
			encodeList (output, ((BList)object).value());
		} else if (object instanceof BDictionary) {
			encodeDictionary (output, (BDictionary)object);
		} else {
			throw new IllegalArgumentException();
		}

	}


	/**
	 * Encodes any BValue, writing the result to the specified OutputStream
	 *
	 * @param output The OutputStream to write to
	 * @param value The BValue to encode
	 * @throws IOException if an error occurred writing to the output stream
	 */
	public static void encode (OutputStream output, BValue value) throws IOException {

		encodeAny (output, value);

	}


	/**
	 * Encodes any BValue
	 *
	 * @param value The BValue to encode
	 * @return The encoded BValue
	 */
	public static byte[] encode (BValue value) {

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try {
			encodeAny (output, value);
			return output.toByteArray ();
		} catch (IOException e) {
			// ByteArrayOutputStream does not actually throw IOException
			throw new InternalError (e.toString());
		}

	}


	/**
	 * Encodes a byte array as a bencode binary string, writing the result to the specified OutputStream
	 *
	 * @param output The OutputStream to write to
	 * @param bytes The byte array to encode
	 * @throws IOException if an error occurred writing to the output stream
	 */
	public static void encodeBinary (OutputStream output, byte[] bytes) throws IOException {

		output.write (Integer.toString (bytes.length).getBytes (UTF8));
		output.write (':');
		output.write (bytes);

	}


	/**
	 * Encodes a byte array as a bencode binary string
	 * 
	 * @param bytes The bytes to encode 
	 * @return The bencoded bytes
	 */
	public static byte[] encodeBinary (byte[] bytes) {

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try {
			encodeBinary (output, bytes);
			return output.toByteArray();
		} catch (IOException e) {
			// ByteArrayOutputStream does not actually throw IOException
			throw new InternalError (e.toString());
		}

	}


	/**
	 * Encodes a String as a bencode binary string
	 * 
	 * @param string The String to encode 
	 * @return The bencoded String
	 */
	public static byte[] encodeBinary (String string) {

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try {
			byte[] stringBytes = string.getBytes (UTF8);
			encodeBinary (output, stringBytes);
			return output.toByteArray ();
		} catch (IOException e) {
			// ByteArrayOutputStream does not actually throw IOException
			throw new InternalError (e.toString());
		}

	}


	/**
	 * Encodes a String as a bencode binary string
	 * 
	 * @param output The OutputStream to write to
	 * @param string The String to encode 
	 * @throws IOException if an error occurred writing to the output stream
	 */
	public static void encodeBinary (OutputStream output, String string) throws IOException {

		byte[] stringBytes = string.getBytes (UTF8);
		encodeBinary (output, stringBytes);

	}


	/**
	 * Encodes a Number as a bencode integer
	 * 
	 * @param output The OutputStream to write to
	 * @param number The Number to encode
	 * @throws IOException if an error occurred writing to the output stream 
	 */
	public static void encodeInteger (OutputStream output, Number number) throws IOException {

		output.write (("i" + number.toString() + "e").getBytes (UTF8));

	}


	/**
	 * Encodes a Number as a bencode integer
	 * 
	 * @param number the Number to encode
	 * @return the bencoded Number
	 */
	public static byte[] encodeInteger (Number number) {

		return ("i" + number.toString() + "e").getBytes (UTF8);

	}


	/**
	 * Encodes a List of bencodable values as a bencode list, writing the result
	 * to the specified OutputStream
	 * 
	 * @param output The OutputStream to write to
	 * @param list The List to encode
	 * @throws IOException if an error occurred writing to the output stream
	 */
	public static void encodeList (OutputStream output, List<?> list) throws IOException {

		output.write ('l');
		for (Object element : list) {
			encodeAny (output, element);
		}
		output.write ('e');

	}


	/**
	 * Encodes a List of bencodable values
	 * 
	 * @param list The List to encode
	 * @return The bencoded List
	 */
	public static byte[] encodeList (List<?> list) {

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try {
			encodeList (output, list);
			return output.toByteArray ();
		} catch (IOException e) {
			// ByteArrayOutputStream does not actually throw IOException
			throw new InternalError (e.toString());
		}

	}


	/**
	 * Encodes a bencode dictionary, writing the result to the specified
	 * OutputStream
	 * 
	 * @param output The OutputStream to write to
	 * @param dictionary The dictionary to encode
	 * @throws IOException if an error occurred writing to the output stream
	 */
	public static void encodeDictionary (OutputStream output, BDictionary dictionary) throws IOException {

		SortedMap<BBinary, BValue> map = dictionary.value();

		output.write ('d');
		for (Object key : map.keySet()) {
			Object value = map.get (key);
			encodeAny (output, key);
			encodeAny (output, value);
		}
		output.write ('e');

	}


	/**
	 * Encodes a SortedMap as a bencode dictionary, writing the result to the
	 * specified OutputStream<br>
	 * <br>
	 * Note: The natural sort order of Strings is a lexicographical sort over
	 * the unicode points of their characters. This is the correct order for
	 * bencoded data, so the default comparator should be used for the map. 
	 * 
	 * @param output The OutputStream to write to
	 * @param map The map to encode
	 * @throws IOException if an error occurred writing to the output stream
	 */
	public static void encodeDictionary (OutputStream output, SortedMap<String, ?> map) throws IOException {

		output.write ('d');
		for (Object key : map.keySet()) {
			Object value = map.get (key);
			encodeAny (output, key);
			encodeAny (output, value);
		}
		output.write ('e');

	}


	/**
	 * Encodes a SortedMap as a bencode dictionary
	 * 
	 * @param map The map to encode
	 * @return The bencoded map as a dictionary
	 */
	public static byte[] encodeDictionary (SortedMap<String, ?> map) {

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try {
			encodeDictionary (output, map);
			return output.toByteArray ();
		} catch (IOException e) {
			// ByteArrayOutputStream does not actually throw IOException
			throw new InternalError (e.toString());
		}

	}


}
