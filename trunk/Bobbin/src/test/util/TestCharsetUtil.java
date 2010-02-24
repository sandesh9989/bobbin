package test.util;

import static org.junit.Assert.*;

import org.itadaki.bobbin.util.CharsetUtil;
import org.junit.Test;



/**
 * Tests CharsetUtil
 */
public class TestCharsetUtil {

	/**
	 * Tests round trip of every single character
	 */
	@Test
	public void testUrlEncodeRoundTrip() {

		for (int i = 0; i < 256; i++) {
			assertArrayEquals (new byte[] { (byte) (i & 0xff) }, CharsetUtil.urldecode (CharsetUtil.urlencode (new byte[] { (byte) (i & 0xff) })));
		}

	}


	/**
	 * Tests decoding a string with a good "%" entity
	 */
	@Test
	public void testUrlDecodeGoodPercent() {

		assertArrayEquals (new byte[] { (byte) (0xff) }, CharsetUtil.urldecode ("%ff"));

	}


	/**
	 * Tests decoding a string with a bad "%" entity
	 */
	@Test
	public void testUrlDecodeBadPercent1() {

		assertNull (CharsetUtil.urldecode ("%a"));

	}


	/**
	 * Tests decoding a string with a bad "%" entity
	 */
	@Test
	public void testUrlDecodeBadPercent2() {

		assertNull (CharsetUtil.urldecode ("%ag"));

	}


	/**
	 * Tests hexEncode
	 */
	@Test
	public void testHexEncode() {

		assertEquals ("00aaff", CharsetUtil.hexencode (new byte[] { 0x00, (byte) 0xaa, (byte) 0xff }));

	}


}
