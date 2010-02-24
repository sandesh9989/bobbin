package test.util.counter;

import static org.junit.Assert.*;

import org.itadaki.bobbin.util.counter.Period;
import org.junit.Test;



/**
 * Tests Period
 */
public class TestPeriod {

	/**
	 * Tests equality to null
	 */
	@Test
	public void testEqualsNull() {

		Period period = new Period (1, 1);
		assertFalse (period.equals (null));

	}


	/**
	 * Tests equality to a non-Period
	 */
	@Test
	public void testEqualsWrongType() {

		Period period = new Period (1, 1);
		assertFalse (period.equals (new Integer (1)));

	}


	/**
	 * Tests equality to null
	 */
	@Test
	public void testEqualsDifferent() {

		Period period = new Period (1, 1);
		assertFalse (period.equals (new Period (1, 2)));

	}


	/**
	 * Tests equality to the same Period
	 */
	@Test
	public void testEqualsSame() {

		Period period = new Period (1, 1);
		assertTrue (period.equals (period));

	}


	/**
	 * Tests equality to an identical Period
	 */
	@Test
	public void testEqualsIdentical() {

		Period period = new Period (1, 1);
		assertTrue (period.equals (new Period (1, 1)));

	}

}
