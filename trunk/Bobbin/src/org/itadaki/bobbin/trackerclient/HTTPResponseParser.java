/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.trackerclient;

import static org.itadaki.bobbin.util.CharsetUtil.ASCII;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A parser for an HTTP response
 */
public class HTTPResponseParser implements HTTPResponse {

	/**
	 * A shared pattern to match the header line of an HTTP response
	 */
	private static Pattern responseHeaderPattern = Pattern.compile ("^HTTP/1.[01] (\\d{3}) (.*)\r$");

	/**
	 * Enum representing the parser's state 
	 */
	private static enum ParserState {

		/**
		 * Parser is reading the response's status line
		 */
		STATUS,

		/**
		 * Parser is reading one of the response's headers
		 */
		HEADER,

		/**
		 * Parser is reading the response body
		 */
		BODY,

		/**
		 * Parser has successfully reached the end of the response
		 */
		COMPLETE,

		/**
		 * Parser encountered an error in the format of the data
		 */
		ERROR

	};

	/**
	 * The parser's current state
	 */
	private ParserState parserState = ParserState.STATUS;

	/**
	 * Explanatory text describing any error in parsing
	 */
	private String errorString = null;

	/**
	 * The response code of the HTTP response
	 */
	private int responseCode = -1;

	/**
	 * The response message of the HTTP response
	 */
	private String responseMessage = null;

	/**
	 * The data of the response being parsed
	 */
	private ByteBuffer responseData = ByteBuffer.allocate (16384);

	/**
	 * The current parsing position within the response
	 */
	private int parserPosition = 0;


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.HTTPResponse#isStateComplete()
	 */
	public boolean isStateComplete() {

		return (this.parserState == ParserState.COMPLETE);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.HTTPResponse#isStateError()
	 */
	public boolean isStateError() {

		return (this.parserState == ParserState.ERROR);

	}


	/**
	 * Provides a textual description of any parse error that has occurred
	 *
	 * @return A textual description of any parse error that has occurred
	 */
	public String getErrorString() {

		return this.errorString;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.HTTPResponse#getResponseCode()
	 */
	public int getResponseCode() {

		return this.responseCode;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.HTTPResponse#getResponseMessage()
	 */
	public String getResponseMessage() {

		return this.responseMessage;

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.trackerclient.HTTPResponse#getResponseBody()
	 */
	public byte[] getResponseBody() {

		byte[] responseBody = null;

		if (this.parserState == ParserState.COMPLETE) {
			int length = this.responseData.position() - this.parserPosition;
			responseBody = new byte[length];
			System.arraycopy (this.responseData.array(), this.parserPosition, responseBody, 0, length);
		}

		return responseBody;

	}


	/**
	 * Parses as many bytes of input as are available, then returns
	 * 
	 * @param inputChannel The input channel to read from 
	 * @return {@code true} if the parse is ongoing, {@code false} if the parse is complete or an
	 *         error has been encountered
	 */
	public boolean parseBytes (ReadableByteChannel inputChannel) {

		while ((this.parserState != ParserState.ERROR) && (this.parserState != ParserState.COMPLETE)){

			int startingPoint = this.responseData.position();

			int bytesRead = 0;
			try {
				bytesRead = inputChannel.read (this.responseData);
			} catch (IOException e) {
				bytesRead = -1;
			}

			if (bytesRead == 0) {
				return true;
			} else if (bytesRead == -1) {
				if (this.parserState == ParserState.BODY) {
					this.parserState = ParserState.COMPLETE;
				} else {
					this.parserState = ParserState.ERROR;
				}
				return false;
			}

			byte[] responseBytes = this.responseData.array();
			int requestLength = this.responseData.position();

			// Find a newline in the supplied data
			int newlinePosition = -1;
			for (int i = startingPoint; i < requestLength; i++) {
				if (responseBytes[i] == '\n') {
					newlinePosition = i;
					break;
				}
			}


			if (newlinePosition >= 0) {

				boolean doContinue;
				do {
					doContinue = false;

					switch (this.parserState) {

						case STATUS:
							for (int i = this.parserPosition; i < newlinePosition - 1; i++) {
								if (responseBytes[i] < 32) {
									this.errorString = "Invalid character in response";
									this.parserState = ParserState.ERROR;
									continue;
								}
							}
							String responseLine = new String (responseBytes, this.parserPosition, newlinePosition, ASCII);
							Matcher responseMatcher = responseHeaderPattern.matcher (responseLine);

							if (!responseMatcher.matches ()) {
								this.errorString = "Invalid response header format";
								this.parserState = ParserState.ERROR;
								continue;
							}

							this.responseCode = new Integer (responseMatcher.group (1));
							this.responseMessage = responseMatcher.group (2);

							this.parserPosition = newlinePosition + 1;
							this.parserState = ParserState.HEADER;
							doContinue = true;
							break;

						case HEADER:
							String headerLine = new String (responseBytes, this.parserPosition, newlinePosition - this.parserPosition, ASCII);
							if (headerLine.length() > 0) {
								if (headerLine.charAt (headerLine.length()-1) == '\r') {
									if (headerLine.length() == 1) {
										this.parserState = ParserState.BODY;
									}
									this.parserPosition = newlinePosition + 1;
									doContinue = true;
									break;
								}
							}
							this.errorString = "Invalid header line";
							this.parserState = ParserState.ERROR;
							continue;

						case BODY:
							// Just accumulate bytes
							break;

						case COMPLETE:
							break;

						case ERROR:
							break;

					}

					newlinePosition = -1;
					for (int i = this.parserPosition; i < requestLength; i++) {
						if (responseBytes[i] == '\n') {
							newlinePosition = i;
							break;
						}
					}

				} while (doContinue && (newlinePosition > 0));

			}

		}

		return false;

	}


}
