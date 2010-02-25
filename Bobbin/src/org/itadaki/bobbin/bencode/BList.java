/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.bencode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * A bencode list
 */
public class BList extends BValue implements Iterable<BValue> {

	/**
	 * The list representing the bencode list's content
	 */
	private List<BValue> value;


	/* Cloneable interface */

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public BList clone() {

		return (BList) super.clone();

	}


	/* Iterable<BValue> interface */

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<BValue> iterator() {

		return this.value.iterator();

	}


	/* Overridden from BValue */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.bencode.BValue#dump(java.lang.StringBuffer, int)
	 */
	@Override
	protected void dump (StringBuffer buffer, int indent) {

		buffer.append ("list:\n");
		indent += 2;

		List<? extends BValue> list = this.value;
		for (BValue value : list) {
			for (int i = 0; i < indent; i++) {
				buffer.append (" ");
			}
			value.dump (buffer, indent);
		}

		if (list.size () == 0) {
			buffer.append ("\n");
		}
		indent -= 2;
		
	}


	/**
	 * Get the data represented by a list BValue
	 *
	 * @return The BValue's contents as a List
	 */
	public List<BValue> value() {

		return this.value;

	}


	/**
	 * Adds an entry to the list
	 *
	 * @param entry The entry to add
	 */
	public void add (BValue entry) {

		this.value.add (entry);

	}


	/**
	 * Convenience method to add an entry to the list
	 *
	 * @param entry The entry to add
	 */
	public void add (String entry) {

		this.value.add (new BBinary (entry));

	}


	/**
	 * Gets an entry from the list
	 *
	 * @param index The index of the entry to get
	 * @return The entry
	 */
	public BValue get (int index) {

		return this.value.get (index);

	}


	/**
	 * Sets an entry within the list
	 *
	 * @param index The index of the entry to get
	 * @param entry The entry to set
	 */
	public void set (int index, BValue entry) {

		this.value.set (index, entry);

	}


	/**
	 * Convenience method to set an entry within the list
	 *
	 * @param index The index of the entry to get
	 * @param entry The entry to set
	 */
	public void set (int index, String entry) {

		this.value.set (index, new BBinary (entry));

	}


	/**
	 * Returns the size of the list
	 *
	 * @return The list's size
	 */
	public int size() {

		return this.value.size();

	}


	/**
	 * Removes all entries from the list
	 */
	public void clear() {

		this.value.clear();

	}


	/**
	 * Creates a BValue representing a list
	 * 
	 * @param list the values of the list
	 */
	public BList (String... list) {

		this.value = new ArrayList<BValue>();
		for (String entry : list) {
			this.value.add (new BBinary (entry));
		}

	}


	/**
	 * Creates a BValue representing a list
	 * 
	 * @param list the values of the list
	 */
	public BList (BValue... list) {

		// The list returned by Arrays.asList doesn't implement #clear()
		this.value = new ArrayList<BValue>();
		this.value.addAll (Arrays.asList (list));

	}


	/**
	 * Creates a BValue representing a list. The List passed in is NOT copied internally
	 * 
	 * @param list the List to wrap
	 */
	public BList (List<BValue> list) {

		this.value = list;

	}


	/**
	 * Creates a BValue representing a list.
	 */
	public BList() {

		this.value = new ArrayList<BValue>();

	}


}
