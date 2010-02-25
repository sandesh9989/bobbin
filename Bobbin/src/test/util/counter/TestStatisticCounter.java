/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.util.counter;

import static org.junit.Assert.assertEquals;

import org.itadaki.bobbin.util.counter.Period;
import org.itadaki.bobbin.util.counter.StatisticCounter;
import org.junit.Test;



/**
 * Tests StatisticCounter
 */
public class TestStatisticCounter {

	/**
	 * Tests a counter with a single collection period
	 * @throws Exception
	 */
	@Test
	public void test1() throws Exception {

		StatisticCounter counter = new StatisticCounter();
		Period period = new Period (500, 1);
		counter.addCountedPeriod (period);

		counter.add (1);
		assertEquals (0, counter.getPeriodTotal (period));
		Thread.sleep (510);
		assertEquals (1, counter.getPeriodTotal (period));
		Thread.sleep (510);
		assertEquals (0, counter.getPeriodTotal (period));

	}


	/**
	 * Tests a counter with two collection periods
	 * @throws Exception
	 */
	@Test
	public void test2() throws Exception {

		StatisticCounter counter = new StatisticCounter();
		Period period1 = new Period (500, 1);
		Period period2 = new Period (1000, 2);
		counter.addCountedPeriod (period1);
		counter.addCountedPeriod (period2);

		counter.add (1);
		assertEquals (0, counter.getPeriodTotal (period1));
		assertEquals (0, counter.getPeriodTotal (period2));
		Thread.sleep (510);
		counter.add (5);
		assertEquals (1, counter.getPeriodTotal (period1));
		assertEquals (0, counter.getPeriodTotal (period2));
		Thread.sleep (510);
		assertEquals (5, counter.getPeriodTotal (period1));
		assertEquals (6, counter.getPeriodTotal (period2));
		Thread.sleep (510);
		assertEquals (0, counter.getPeriodTotal (period1));
		assertEquals (6, counter.getPeriodTotal (period2));
		Thread.sleep (510);
		assertEquals (0, counter.getPeriodTotal (period1));
		assertEquals (6, counter.getPeriodTotal (period2));
		Thread.sleep (510);
		assertEquals (0, counter.getPeriodTotal (period1));
		assertEquals (6, counter.getPeriodTotal (period2));
		Thread.sleep (510);
		assertEquals (0, counter.getPeriodTotal (period1));
		assertEquals (0, counter.getPeriodTotal (period2));

	}


	/**
	 * Tests a hierarchy of two counters
	 * @throws Exception
	 */
	@Test
	public void testParent() throws Exception {

		StatisticCounter parentCounter = new StatisticCounter();
		StatisticCounter counter = new StatisticCounter();
		counter.setParent (parentCounter);
		counter.add (1);

		assertEquals (1, counter.getTotal());
		assertEquals (1, parentCounter.getTotal());

	}


}
