/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.bencode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Iterator;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.bencode.BValue;
import org.junit.Test;



/**
 * Tests BList
 */
public class TestBList {

	/**
	 * Store an empty list of BValues and retrieve it
	 */
	@Test
	public void testListBValueEmpty() {

		BList list = new BList (new ArrayList<BValue>());

		assertEquals (list.size(), 0);

	}


	/**
	 * Store an list of strings and retrieve it
	 */
	@Test
	public void testListStrings() {

		BList list = new BList ("Test", "String");

		assertEquals (list.size(), 2);
		assertEquals (new BBinary ("Test"), list.value().get (0));
		assertEquals (new BBinary ("String"), list.value().get (1));

	}


	/**
	 * Store an list of strings and retrieve it
	 */
	@Test
	public void testListIterator() {

		BList list = new BList ("Test", "String");

		Iterator<BValue> iterator = list.iterator();
		assertEquals (new BBinary ("Test"), iterator.next());
		assertEquals (new BBinary ("String"), iterator.next());
		assertFalse (iterator.hasNext());

	}


	/**
	 * Tests clear
	 */
	@Test
	public void testListClear() {

		BList list = new BList ("Test", "String");
		list.clear();

		assertEquals (0, list.size());

	}


	/**
	 * Tests set and get of a BValue
	 */
	@Test
	public void testListSetGetBValue() {

		BList list = new BList ("Test", "String");
		list.set (1, new BInteger (1234));

		assertEquals (new BInteger (1234), list.get (1));

	}


	/**
	 * Tests set and get of a String
	 */
	@Test
	public void testListSetGetString() {

		BList list = new BList ("Test", "String");
		list.set (1, "Replacement");

		assertEquals (new BBinary ("Replacement"), list.get (1));

	}


	/**
	 * Tests adding a BValue
	 */
	@Test
	public void testListAddBValue() {

		BList list = new BList ("Test", "String");
		list.add (new BInteger (1234));

		assertEquals (new BInteger (1234), list.get (2));

	}


	/**
	 * Tests set and get of a String
	 */
	@Test
	public void testListAddString() {

		BList list = new BList ("Test", "String");
		list.add ("Addition");

		assertEquals (new BBinary ("Addition"), list.get (2));

	}


	/**
	 * Check equality of two list BValues
	 */
	@Test
	public void testListEquals() {

		BList list1 = new BList (new BBinary ("Hello"), new BBinary ("Testing"), new BInteger (123));
		BList list2 = new BList (new BBinary ("Hello"), new BBinary ("Testing"), new BInteger (123));

		assertEquals (list1, list2);

	}

	/**
	 * Check inequality of two list BValues
	 */
	@Test
	public void testListNotEquals() {

		BList list1 = new BList (new BBinary ("Hello"), new BBinary ("Testing"), new BInteger (123));
		BList list2 = new BList (new BInteger (123), new BInteger (456), new BInteger (789));

		assertFalse (list1.equals (list2));

	}

	/**
	 * Test cloning a BList
	 */
	@Test
	public void testListClone() {

		BList list = new BList (new BInteger (1234));
		BList clonedList = list.clone();

		assertEquals (list, clonedList);
		assertFalse (System.identityHashCode (list) == System.identityHashCode (clonedList));
		assertFalse (System.identityHashCode (list.value()) == System.identityHashCode (clonedList.value()));
		assertFalse (System.identityHashCode (list.value().get (0)) == System.identityHashCode (clonedList.value().get (0)));

	}

}
