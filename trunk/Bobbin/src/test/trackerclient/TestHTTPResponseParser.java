/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.trackerclient;

import static org.junit.Assert.*;

import org.itadaki.bobbin.trackerclient.HTTPResponseParser;
import org.itadaki.bobbin.util.CharsetUtil;
import org.junit.Test;

import test.Util;


/**
 * Tests HTTPResponseParser
 */
public class TestHTTPResponseParser {

	/**
	 * Tests a simple valid OK response
	 */
	@Test
	public void testSimpleOKResponse() {

		String response = "HTTP/1.0 200 OK\r\n\r\n";

		HTTPResponseParser parser = new HTTPResponseParser();

		parser.parseBytes (Util.readableByteChannelFor (response.getBytes (CharsetUtil.ASCII)));

		assertTrue (parser.isStateComplete());
		assertFalse (parser.isStateError());
		assertEquals (200, parser.getResponseCode());
		assertEquals ("OK", parser.getResponseMessage());
		assertEquals (0, parser.getResponseBody().length);

	}

	/**
	 * Tests an OK response with a body
	 */
	@Test
	public void testSimpleOKResponseWithBody() {

		String response = "HTTP/1.0 200 OK\r\n\r\nBlah";

		HTTPResponseParser parser = new HTTPResponseParser();

		parser.parseBytes (Util.readableByteChannelFor (response.getBytes (CharsetUtil.ASCII)));

		assertTrue (parser.isStateComplete());
		assertFalse (parser.isStateError());
		assertEquals (200, parser.getResponseCode());
		assertEquals ("OK", parser.getResponseMessage());
		assertArrayEquals ("Blah".getBytes (CharsetUtil.ASCII), parser.getResponseBody());

	}

	/**
	 * Tests an OK response with headers
	 */
	@Test
	public void testSimpleOKResponseWithHeaders() {

		String response = "HTTP/1.0 200 OK\r\nFoo: bar\r\n\r\n";

		HTTPResponseParser parser = new HTTPResponseParser();

		parser.parseBytes (Util.readableByteChannelFor (response.getBytes (CharsetUtil.ASCII)));

		assertTrue (parser.isStateComplete());
		assertFalse (parser.isStateError());
		assertEquals (200, parser.getResponseCode());
		assertEquals ("OK", parser.getResponseMessage());
		assertEquals (0, parser.getResponseBody().length);

	}

	/**
	 * Tests an OK response with headers and a body
	 */
	@Test
	public void testSimpleOKResponseWithHeadersAndBody() {

		String response = "HTTP/1.0 200 OK\r\nFoo: bar\r\n\r\nBlah";

		HTTPResponseParser parser = new HTTPResponseParser();

		parser.parseBytes (Util.readableByteChannelFor (response.getBytes (CharsetUtil.ASCII)));

		assertTrue (parser.isStateComplete());
		assertFalse (parser.isStateError());
		assertEquals (200, parser.getResponseCode());
		assertEquals ("OK", parser.getResponseMessage());
		assertArrayEquals ("Blah".getBytes (CharsetUtil.ASCII), parser.getResponseBody());

	}


	/**
	 * Tests a bad status line with invalid protocol
	 */
	@Test
	public void testBadStatusLineInvalidProtocol() {

		String response = "STTP/1.0 200 OK\r\n\r\n";

		HTTPResponseParser parser = new HTTPResponseParser();

		parser.parseBytes (Util.readableByteChannelFor (response.getBytes (CharsetUtil.ASCII)));

		assertFalse (parser.isStateComplete());
		assertTrue (parser.isStateError());

	}


	/**
	 * Tests a bad status line with invalid status code
	 */
	@Test
	public void testBadStatusLineInvalidStatusCode() {

		String response = "HTTP/1.0 X00 OK\r\n\r\n";

		HTTPResponseParser parser = new HTTPResponseParser();

		parser.parseBytes (Util.readableByteChannelFor (response.getBytes (CharsetUtil.ASCII)));

		assertFalse (parser.isStateComplete());
		assertTrue (parser.isStateError());

	}


	/**
	 * Tests a bad status line with no trailing \r
	 */
	@Test
	public void testBadStatusLineNoSlashR() {

		String response = "HTTP/1.0 200 OK\n\r\n";

		HTTPResponseParser parser = new HTTPResponseParser();

		parser.parseBytes (Util.readableByteChannelFor (response.getBytes (CharsetUtil.ASCII)));

		assertFalse (parser.isStateComplete());
		assertTrue (parser.isStateError());

	}


	/**
	 * Tests a bad status line with two trailing \rs
	 */
	@Test
	public void testBadStatusLineTwoSlashRs() {

		String response = "HTTP/1.0 200 OK\r\r\n\r\n";

		HTTPResponseParser parser = new HTTPResponseParser();

		parser.parseBytes (Util.readableByteChannelFor (response.getBytes (CharsetUtil.ASCII)));

		assertFalse (parser.isStateComplete());
		assertTrue (parser.isStateError());

	}


}
