/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.torrentdb;

import static org.junit.Assert.*;

import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.junit.Test;



/**
 * Tests BlockDescriptor
 */
public class TestBlockDescriptor {

	/**
	 * Tests content
	 */
	@Test
	public void testContent() {

		BlockDescriptor descriptor = new BlockDescriptor (1, 2, 3);

		assertEquals (1, descriptor.getPieceNumber());
		assertEquals (2, descriptor.getOffset());
		assertEquals (3, descriptor.getLength());

	}


	/**
	 * Tests toString output
	 */
	@Test
	public void testToString() {

		BlockDescriptor descriptor = new BlockDescriptor (1, 2, 3);

		assertEquals ("BlockDescriptor:{1:2,3}", descriptor.toString());

	}


	/**
	 * Tests equals on null
	 */
	@Test
	public void testEqualsNull() {

		BlockDescriptor descriptor = new BlockDescriptor (1, 2, 3);

		assertFalse (descriptor.equals (null));

	}


	/**
	 * Tests equals on another type
	 */
	@Test
	public void testEqualsWrongType() {

		BlockDescriptor descriptor = new BlockDescriptor (1, 2, 3);

		assertFalse (descriptor.equals (new Integer (1)));

	}


	/**
	 * Tests equals on another type
	 */
	@Test
	public void testEqualsDifferentValue() {

		BlockDescriptor descriptor1 = new BlockDescriptor (1, 2, 3);
		BlockDescriptor descriptor2 = new BlockDescriptor (4, 2, 3);

		assertFalse (descriptor1.equals (descriptor2));
		assertFalse (descriptor1.hashCode() == descriptor2.hashCode());

	}


	/**
	 * Tests equals on the same content
	 */
	@Test
	public void testEqualsSameContent() {

		BlockDescriptor descriptor1 = new BlockDescriptor (1, 2, 3);
		BlockDescriptor descriptor2 = new BlockDescriptor (1, 2, 3);

		assertTrue (descriptor1.equals (descriptor2));
		assertEquals (descriptor1.hashCode(), descriptor2.hashCode());

	}


	/**
	 * Tests equals on the same object
	 */
	@Test
	public void testEqualsSameObject() {

		BlockDescriptor descriptor = new BlockDescriptor (1, 2, 3);

		assertTrue (descriptor.equals (descriptor));

	}

}
