/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.util.counter;

import static org.junit.Assert.*;

import org.itadaki.bobbin.util.counter.PeriodicCounter;
import org.junit.Test;



/**
 * Tests PeriodicCounter
 */
public class TestPeriodicCounter {

	/**
	 * Tests a counter of size 1
	 * @throws Exception 
	 */
	@Test
	public void test1() throws Exception {

		PeriodicCounter counter = new PeriodicCounter (1);

		counter.add (1);
		assertEquals (0, counter.getPeriodTotal());
		counter.advance (1);
		assertEquals (1, counter.getPeriodTotal());
		counter.advance (1);
		assertEquals (0, counter.getPeriodTotal());

	}

	/**
	 * Tests a counter of size 1
	 * @throws Exception 
	 */
	@Test
	public void test3() throws Exception {

		PeriodicCounter counter = new PeriodicCounter (3);

		counter.add (1);
		assertEquals (0, counter.getPeriodTotal());
		counter.advance (1);
		counter.add (3);
		assertEquals (1, counter.getPeriodTotal());
		counter.advance (1);
		counter.add (5);
		assertEquals (4, counter.getPeriodTotal());
		counter.advance (1);
		assertEquals (9, counter.getPeriodTotal());
		counter.advance (1);
		assertEquals (8, counter.getPeriodTotal());
		counter.advance (1);
		assertEquals (5, counter.getPeriodTotal());
		counter.advance (1);
		assertEquals (0, counter.getPeriodTotal());

	}

}
