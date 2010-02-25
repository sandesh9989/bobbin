/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;


/**
 * A class of miscellaneous character set utilities
 */
public class CharsetUtil {

	/**
	 * A Charset for US-ASCII
	 */
	public static final Charset ASCII = Charset.forName ("US-ASCII");

	/**
	 * A Charset for UTF-8
	 */
	public static final Charset UTF8 = Charset.forName ("UTF-8");


	/**
	 * Decodes a urlencoded string into a byte array. If the string is not a valid urlencoded
	 * string, {@code null} is returned.
	 *
	 * @param encodedString The urlencoded string to decode
	 * @return The decoded byte array, or {@code null}
	 */
	public static byte[] urldecode (String encodedString) {

		char[] encodedChars = encodedString.toCharArray();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		int i = 0;
		while (i < encodedChars.length) {
			if (encodedChars[i] == '+') {
				output.write (' ');
				i++;
			} else if (encodedChars[i] == '%') {
				if (i > encodedChars.length - 3) {
					return null;
				}
				int x = Character.digit (encodedChars[i + 1], 16);
				int y = Character.digit (encodedChars[i + 2], 16);
				if ((x == -1) || (y == -1)) {
					return null;
				}
				output.write ((x << 4) + y);
				i += 3;
			} else {
				output.write (encodedChars[i]);
				i++;
			}
		}

		return output.toByteArray();

	}


	/**
	 * Encodes an array of plain bytes into a urlencoded string
	 *
	 * @param unencodedBytes The bytes to encode
	 * @return A urlencoded string
	 */
	public static String urlencode (byte[] unencodedBytes) {

		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < unencodedBytes.length; i++) {

			if (
					   ((unencodedBytes[i] >= 'a') && (unencodedBytes[i] <= 'z'))
					|| ((unencodedBytes[i] >= 'A') && (unencodedBytes[i] <= 'Z'))
					|| ((unencodedBytes[i] >= '0') && (unencodedBytes[i] <= '9'))
					|| (unencodedBytes[i] == '.')
					|| (unencodedBytes[i] == '-')
					|| (unencodedBytes[i] == '*')
					|| (unencodedBytes[i] == '_')
			   )
			{
				buffer.append ((char)unencodedBytes[i]);
			} else if (unencodedBytes[i] == ' ') {
				buffer.append ('+');
			} else {
				buffer.append (String.format ("%%%02x", unencodedBytes[i]));
			}

		}

		return buffer.toString();

	}


	/**
	 * Encodes an array of plain bytes into a hex encoded string
	 *
	 * @param unencodedBytes The bytes to encode
	 * @return A hex encoded string
	 */
	public static String hexencode (byte[] unencodedBytes) {

		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < unencodedBytes.length; i++) {
			buffer.append (String.format ("%02x", unencodedBytes[i]));
		}

		return buffer.toString();

	}


	/**
	 * Not instantiable
	 */
	private CharsetUtil() {

	}

}
