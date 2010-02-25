/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.util.counter;

import static org.junit.Assert.assertEquals;

import org.itadaki.bobbin.util.counter.Period;
import org.itadaki.bobbin.util.counter.TemporalCounter;
import org.junit.Test;



/**
 * Tests TemporalCounter
 */
public class TestTemporalCounter {

	/**
	 * Tests a counter of size 1
	 * @throws Exception 
	 */
	@Test
	public void test1() throws Exception {

		TemporalCounter counter = new TemporalCounter (new Period (500, 1), System.currentTimeMillis());

		counter.add (1);
		assertEquals (0, counter.getPeriodTotal());
		Thread.sleep (510);
		assertEquals (1, counter.getPeriodTotal());
		Thread.sleep (510);
		assertEquals (0, counter.getPeriodTotal());

	}

}
