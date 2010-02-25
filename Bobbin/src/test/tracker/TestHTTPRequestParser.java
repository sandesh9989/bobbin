/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.tracker;

import static org.junit.Assert.*;

import java.io.IOException;

import org.itadaki.bobbin.tracker.HTTPRequestParser;
import org.junit.Test;

import test.Util;


/**
 * Tests HTTPRequestParser
 */
public class TestHTTPRequestParser {

	/**
	 * Test parsing of a simple valid request in one go
	 * @throws IOException 
	 */
	@Test
	public void testSimpleRequest() throws IOException {

		String request = "GET / HTTP/1.0\r\n\r\n";
		byte[] requestBytes = request.getBytes ("US-ASCII");

		HTTPRequestParser parser = new HTTPRequestParser();

		parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (requestBytes));
		assertTrue (parser.isStateComplete());
		assertEquals ("GET", parser.getMethod());
		assertEquals ("/", parser.getResource());
		assertEquals (null, parser.getArgumentString());

	}


	/**
	 * Test parsing of a simple valid request one byte at a time
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testSimpleRequestByBytes() throws IOException {

		String request = "GET / HTTP/1.0\r\n\r\n";
		byte[] requestBytes = request.getBytes ("US-ASCII");

		HTTPRequestParser parser = new HTTPRequestParser();

		for (int i = 0; i < requestBytes.length; i++) {

			parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (new byte[] {requestBytes[i]}));
			if (i < (requestBytes.length - 1)) {
				assertFalse (parser.isStateComplete());
			}
		}

		assertTrue (parser.isStateComplete());
		assertEquals ("GET", parser.getMethod());
		assertEquals ("/", parser.getResource());
		assertEquals (null, parser.getArgumentString());

	}


	/**
	 * Test parsing of a valid request with one argument, in one go
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testRequestWithOneArgument() throws IOException {

		String request = "GET /foo/bar?hello=world HTTP/1.0\r\n\r\n";
		byte[] requestBytes = request.getBytes ("US-ASCII");

		HTTPRequestParser parser = new HTTPRequestParser();

		parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (requestBytes));
		assertTrue (parser.isStateComplete());
		assertEquals ("GET", parser.getMethod());
		assertEquals ("/foo/bar", parser.getResource());
		assertEquals ("hello=world", parser.getArgumentString ());

	}


	/**
	 * Test parsing of a valid request with one argument, one byte at a time
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testRequestWithOneArgumentByBytes() throws IOException {

		String request = "GET /foo/bar?hello=world HTTP/1.0\r\n\r\n";
		byte[] requestBytes = request.getBytes ("US-ASCII");

		HTTPRequestParser parser = new HTTPRequestParser();

		for (int i = 0; i < requestBytes.length; i++) {

			parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (new byte[] {requestBytes[i]}));
			if (i < (requestBytes.length - 1)) {
				assertFalse (parser.isStateComplete());
			}
		}

		assertTrue (parser.isStateComplete());
		assertEquals ("GET", parser.getMethod());
		assertEquals ("/foo/bar", parser.getResource());
		assertEquals ("hello=world", parser.getArgumentString ());

	}


	/**
	 * Test parsing of a simple valid request with a header, in one go
	 * @throws IOException 
	 */
	@Test
	public void testSimpleRequestWithOneHeader() throws IOException {

		String request = "GET / HTTP/1.0\r\nHost: te.st\r\n\r\n";
		byte[] requestBytes = request.getBytes ("US-ASCII");

		HTTPRequestParser parser = new HTTPRequestParser();

		parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (requestBytes));
		assertTrue (parser.isStateComplete());
		assertEquals ("GET", parser.getMethod());
		assertEquals ("/", parser.getResource());
		assertEquals (null, parser.getArgumentString());

	}


	/**
	 * Test parsing of a simple valid request with a header, one byte at a time
	 * @throws IOException 
	 */
	@Test
	public void testSimpleRequestWithOneHeaderByBytes() throws IOException {

		String request = "GET / HTTP/1.0\r\nHost: te.st\r\n\r\n";
		byte[] requestBytes = request.getBytes ("US-ASCII");

		HTTPRequestParser parser = new HTTPRequestParser();

		for (int i = 0; i < requestBytes.length; i++) {

			parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (new byte[] {requestBytes[i]}));
			if (i < (requestBytes.length - 1)) {
				assertFalse (parser.isStateComplete());
			}
		}

		assertTrue (parser.isStateComplete());
		assertEquals ("GET", parser.getMethod());
		assertEquals ("/", parser.getResource());
		assertEquals (null, parser.getArgumentString());

	}


	/**
	 * Test parsing of a simple valid request with a header, in one go
	 * @throws IOException 
	 */
	@Test
	public void testSimpleRequestWithHeaders() throws IOException {

		String request = "GET / HTTP/1.0\r\nHost: te.st\r\nUser-Agent: Bobbin/1.0\r\n\r\n";
		byte[] requestBytes = request.getBytes ("US-ASCII");

		HTTPRequestParser parser = new HTTPRequestParser();

		parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (requestBytes));
		assertTrue (parser.isStateComplete());
		assertEquals ("GET", parser.getMethod());
		assertEquals ("/", parser.getResource());
		assertEquals (null, parser.getArgumentString());

	}


	/**
	 * Test parsing of a simple valid request with a header, one byte at a time
	 * @throws IOException 
	 */
	@Test
	public void testSimpleRequestWithHeadersByBytes() throws IOException {

		String request = "GET / HTTP/1.0\r\nHost: te.st\r\nUser-Agent: Bobbin/1.0\r\n\r\n";
		byte[] requestBytes = request.getBytes ("US-ASCII");

		HTTPRequestParser parser = new HTTPRequestParser();

		for (int i = 0; i < requestBytes.length; i++) {

			parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (new byte[] {requestBytes[i]}));
			if (i < (requestBytes.length - 1)) {
				assertFalse (parser.isStateComplete());
			}
		}

		assertTrue (parser.isStateComplete());
		assertEquals ("GET", parser.getMethod());
		assertEquals ("/", parser.getResource());
		assertEquals (null, parser.getArgumentString());

	}


	/**
	 * Test parsing of a request with \n at an invalid position 
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testInvalidNewline0a() throws IOException {

		String request = "GET /foo/bar?hello=world HTTP/1.0";
		byte[] requestBytes = request.getBytes ("US-ASCII");

		for (int i = 0; i <= requestBytes.length; i++) {

			HTTPRequestParser parser = new HTTPRequestParser();

			byte[] subRequestBytes = new byte [i+1];
			System.arraycopy (requestBytes, 0, subRequestBytes, 0, i);
			subRequestBytes[i] = '\n';

			parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (subRequestBytes));
			assertTrue (parser.isStateError());

		}


	}


	/**
	 * Test parsing of a request with \r at an invalid position 
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testInvalidNewline0d() throws IOException {

		String request = "GET /foo/bar?hello=world HTTP/1.0\r\n";
		byte[] requestBytes = request.getBytes ("US-ASCII");

		for (int i = 0; i < requestBytes.length - 2; i++) {

			HTTPRequestParser parser = new HTTPRequestParser();

			byte[] subRequestBytes = new byte [requestBytes.length];
			System.arraycopy (requestBytes, 0, subRequestBytes, 0, requestBytes.length);
			subRequestBytes[i] = '\r';

			parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (subRequestBytes));
			assertTrue (parser.isStateError());

		}

	}


	/**
	 * Test parsing of a oversize request 
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testOversize() throws IOException {

		byte[] requestBytes = new byte[16385];

		HTTPRequestParser parser = new HTTPRequestParser();

		parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (requestBytes));

		assertTrue (parser.isStateError());
		assertEquals ("Request too large", parser.getErrorString());

	}


	/**
	 * Test parsing of a oversize request, one byte at a time 
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testOversizeByBytes() throws IOException {

		HTTPRequestParser parser = new HTTPRequestParser();

		for (int i = 0; i <= 16384; i++) {
			parser.parseRequestBytes (Util.infiniteReadableByteChannelFor (new byte[] { ' ' }));
		}

		assertTrue (parser.isStateError());
		assertEquals ("Request too large", parser.getErrorString());

	}

}
