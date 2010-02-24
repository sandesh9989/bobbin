package org.itadaki.bobbin.tracker;

import static org.itadaki.bobbin.util.CharsetUtil.ASCII;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * A state machine to parse an HTTP request incrementally
 */
public class HTTPRequestParser {

	/**
	 * The set of valid HTTP methods
	 */
	private static final Set<String> httpMethods = new HashSet<String> (Arrays.asList ("CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "POST", "PUT", "TRACE"));

	/**
	 * Enum representing the parser's state 
	 */
	private static enum ParserState {

		/**
		 * Parser is reading the request line
		 */
		REQUEST,

		/**
		 * Parser is reading one of the request's headers
		 */
		HEADER,

		/**
		 * Parser has successfully reached the end of the request
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
	private ParserState parserState = ParserState.REQUEST;

	/**
	 * Explanatory text describing any error in parsing
	 */
	private String errorString = null;

	/**
	 * The request being parsed
	 */
	private ByteBuffer request = ByteBuffer.allocate (16384);

	/**
	 * The current parsing position within the request
	 */
	private int parserPosition = 0;

	/**
	 * The parsed method string
	 */
	private String method = null;

	/**
	 * The parsed resource string
	 */
	private String resource = null;

	/**
	 * The parsed argument string
	 */
	private String argumentString = null;


	/**
	 * Determines if the parser has completed successfully
	 *
	 * @return {@code true} if the parser has completed successfully, otherwise
	 *         {@code false}
	 */
	public boolean isStateComplete() {

		return (this.parserState == ParserState.COMPLETE);

	}


	/**
	 * Determines if the parser has encountered an error
	 *
	 * @return {@code true} if the parser has encountered a parse error,
	 *         otherwise {@code false}
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


	/**
	 * Gets the request's method. This method should not be called before
	 * requestComplete() returns true
	 *
	 * @return the request's method
	 */
	public String getMethod() {

		return this.method;

	}

	/**
	 * Gets the request's resource path. This method should not be called before
	 * requestComplete() returns true
	 *
	 * @return the request's resource path
	 */
	public String getResource() {

		return this.resource;

	}

	/**
	 * Gets the request's argument string, if any. This method should not be
	 * called before requestComplete() returns true
	 *
	 * @return the request's argument string, if any, or null
	 */
	public String getArgumentString() {

		return this.argumentString;

	}


	/**
	 * Parses some bytes from an HTTP request. This method should be called
	 * repeatedly with new data until true is returned
	 *
	 * @param channel The channel to read bytes from
	 * @throws IOException on a parse error
	 */
	public void parseRequestBytes (ReadableByteChannel channel) throws IOException {

		// Abort immediately if we have already hit a parse error or completed
		if ((this.parserState == ParserState.ERROR) || (this.parserState == ParserState.COMPLETE)) {
			return;
		}

		int startingPoint = this.request.position();

		// Buffer the data. May throw an exception if the channel is closed
		int bytesRead = channel.read (this.request);
		if (bytesRead == -1) {
			this.errorString = "Connection closed";
			this.parserState = ParserState.ERROR;
			return;
		}
		if (!this.request.hasRemaining()) {
			this.errorString = "Request too large";
			this.parserState = ParserState.ERROR;
			return;
		}

		byte[] requestBytes = this.request.array();
		int requestLength = this.request.position();

		// Find a newline in the supplied data
		int newlinePosition = -1;
		for (int i = startingPoint; i < requestLength; i++) {
			if (requestBytes[i] == '\n') {
				newlinePosition = i;
				break;
			}
		}

		if (newlinePosition >= 0) {

			boolean doContinue;
			do {
				doContinue = false;

				switch (this.parserState) {

					case REQUEST:
						for (int i = this.parserPosition; i < newlinePosition - 1; i++) {
							if (requestBytes[i] < 32) {
								this.errorString = "Invalid character in request";
								this.parserState = ParserState.ERROR;
								return;
							}
						}
						String requestLine = new String (requestBytes, this.parserPosition, newlinePosition, ASCII);
						String[] requestParts = requestLine.split (" ");

						if (requestParts.length != 3) {
							this.errorString = "Invalid request format";
							this.parserState = ParserState.ERROR;
							return;
						}

						if (httpMethods.contains (requestParts[0])) {
							this.method = requestParts[0];
						} else {
							this.errorString = String.format ("Invalid method '" + requestParts[0] + "'");
							this.parserState = ParserState.ERROR;
							return;
						}

						int separatorIndex = requestParts[1].indexOf('?');
						if (separatorIndex >= 0) {
							this.resource = requestParts[1].substring (0, separatorIndex);
							this.argumentString = requestParts[1].substring (separatorIndex+1, requestParts[1].length());
						} else {
							this.resource = requestParts[1];
						}
						

						if (!(requestParts[2].equals ("HTTP/1.0\r")) && !(requestParts[2].equals ("HTTP/1.1\r"))) {
							this.errorString = String.format ("Invalid HTTP version '" + requestParts[2] + "'");
							this.parserState = ParserState.ERROR;
							return;
						}

						this.parserPosition = newlinePosition + 1;
						this.parserState = ParserState.HEADER;
						doContinue = true;
						break;

					case HEADER:
						String headerLine = new String (requestBytes, this.parserPosition, newlinePosition - this.parserPosition, ASCII);
						if (headerLine.length() > 0) {
							if (headerLine.charAt (headerLine.length()-1) == '\r') {
								if (headerLine.length() == 1) {
									this.parserState = ParserState.COMPLETE;
									break;
								}
								this.parserPosition = newlinePosition + 1;
								doContinue = true;
								break;
							}
						}
						this.errorString = "Invalid header line";
						this.parserState = ParserState.ERROR;
						return;

					case COMPLETE:
						break;

					case ERROR:
						break;

				}

				newlinePosition = -1;
				for (int i = this.parserPosition; i < requestLength; i++) {
					if (requestBytes[i] == '\n') {
						newlinePosition = i;
						break;
					}
				}

			} while (doContinue && (newlinePosition > 0));

		}

	}


}
