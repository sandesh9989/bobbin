/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.bencode;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A bencode dictionary
 */
public class BDictionary extends BValue implements Cloneable {

	/**
	 * The map representing the bencode dictionary's content
	 */
	private SortedMap<BBinary,BValue> value;


	/* Cloneable interface */

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public BDictionary clone() {

		return (BDictionary) super.clone();

	}


	/* Overridden from BValue */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.bencode.BValue#dump(java.lang.StringBuffer, int)
	 */
	@Override
	protected void dump (StringBuffer buffer, int indent) {

		buffer.append ("dictionary:\n");
		indent += 2;

		for (BBinary key : this.value.keySet()) {
			for (int i = 0; i < indent; i++) {
				buffer.append (" ");
			}
			buffer.append ("\"" + key.stringValue() + "\" => ");
			BValue value = this.value.get(key);
			value.dump (buffer, indent);
		}

		if (this.value.size () == 0) {
			buffer.append ("\n");
		}

	}


	/**
	 * Get the data represented by a dictionary BValue
	 *
	 * @return The BValue's contents as a Map
	 */
	public SortedMap<BBinary,BValue> value() {

		return this.value;

	}


	/**
	 * Convenience method to get a binary string value from the dictionary, assuming that both the
	 * key and value are UTF-8 encoded strings
	 *
	 * @param key The key to retrieve a value for
	 * @return If the value corresponding to the key is present and a binary string, the
	 *         {@code String} representation of the binary string interpreted as UTF-8, else
	 *         {@code null}
	 */
	public String getString (String key) {

		BValue keyValue = this.value.get (new BBinary (key));

		if ((keyValue != null) && (keyValue instanceof BBinary)) {
			return ((BBinary)keyValue).stringValue ();
		}

		return null;

	}


	/**
	 * Convenience method to get a binary value from the dictionary, assuming that the key is a
	 * UTF-8 encoded string
	 *
	 * @param key The key to retrieve a value for
	 * @return If the value corresponding to the key is present and binary, the value's bytes,
	 *         otherwise {@code null}
	 */
	public byte[] getBytes (String key) {

		BValue keyValue = this.value.get (new BBinary (key));

		if ((keyValue != null) && (keyValue instanceof BBinary)) {
			return ((BBinary)keyValue).value();
		}

		return null;

	}


	/**
	 * Gets a value from the dictionary
	 *
	 * @param key The key to retrieve a value for
	 * @return The value corresponding to the key if present, or null
	 */
	public BValue get (BBinary key) {

		return this.value.get (key);

	}


	/**
	 * Convenience method to get a value from the dictionary, assuming that the key is a UTF-8
	 * encoded string
	 *
	 * @param key The key to retrieve a value for
	 * @return The value corresponding to the key if present, or null
	 */
	public BValue get (String key) {

		return this.value.get (new BBinary (key));

	}


	/**
	 * Convenience method to get a value from the dictionary
	 *
	 * @param key The key to retrieve a value for
	 * @return The value corresponding to the key if present, or null
	 */
	public BValue get (byte[] key) {

		return this.value.get (new BBinary (key));

	}


	/**
	 * Puts a value into the dictionary
	 *
	 * @param key The key to put
	 * @param value The value to put
	 */
	public void put (BBinary key, BValue value) {

		this.value.put (key, value);

	}


	/**
	 * Convenience method to put a value into the dictionary
	 *
	 * @param key The key to put
	 * @param value The value to put
	 */
	public void put (String key, BValue value) {

		this.value.put (new BBinary (key), value);

	}


	/**
	 * Convenience method to put a value into the dictionary
	 *
	 * @param key The key to put
	 * @param value The value to put
	 */
	public void put (String key, String value) {

		this.value.put (new BBinary (key), new BBinary (value));

	}


	/**
	 * Convenience method to put a value into the dictionary
	 *
	 * @param key The key to put
	 * @param value The value to put
	 */
	public void put (String key, byte[] value) {

		this.value.put (new BBinary (key), new BBinary (value));

	}


	/**
	 * Convenience method to put a value into the dictionary
	 *
	 * @param key The key to put
	 * @param value The value to put
	 */
	public void put (String key, Number value) {

		this.value.put (new BBinary (key), new BInteger (value));

	}


	/**
	 * Returns a set of the dictionary's keys
	 *
	 * @return A set of the dictionary's keys
	 */
	public Set<BBinary> keySet() {

		return this.value.keySet();

	}


	/**
	 * Removes a value from the dictionary
	 *
	 * @param key The key to remove
	 */
	public void remove (BBinary key) {

		this.value.remove (key);

	}


	/**
	 * Convenience method to remove a value from the dictionary
	 *
	 * @param key The key to remove
	 */
	public void remove (String key) {

		this.value.remove (new BBinary (key));

	}


	/**
	 * Removes all keys from the dictionary
	 */
	public void clear() {

		this.value.clear();

	}


	/**
	 * Returns the dictionary's size
	 *
	 * @return The dictionary's size
	 */
	public int size() {

		return this.value.size();

	}


	/**
	 * Creates a BValue representing a dictionary. The Map passed in is NOT copied internally<br>
	 * <br>
	 * Note: The natural sort order of Strings is a lexicographical sort over the unicode points of
	 * their characters. This is the correct order for bencoded data, so the default comparator
	 * should be used for the map.
	 * 
	 * @param dictionary the Map to wrap
	 */
	public BDictionary (SortedMap<BBinary,BValue> dictionary) {

		this.value = dictionary;

	}


	/**
	 * Creates a BValue representing a dictionary
	 */
	public BDictionary() {

		this.value = new TreeMap<BBinary,BValue>();

	}


}
