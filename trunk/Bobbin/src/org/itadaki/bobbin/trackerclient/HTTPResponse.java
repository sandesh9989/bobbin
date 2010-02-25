/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.trackerclient;

/**
 * A response received in reply to an HTTP request
 */
public interface HTTPResponse {

	/**
	 * Determines if the response has been received successfully
	 *
	 * @return {@code true} if the response has been received successfully, otherwise
	 *         {@code false}
	 */
	public abstract boolean isStateComplete();

	/**
	 * Determines if an error has occurred in receiving the response
	 *
	 * @return {@code true} if the parser has encountered a parse error,
	 *         otherwise {@code false}
	 */
	public abstract boolean isStateError();

	/**
	 * Returns the HTTP response code
	 *
	 * @return The HTTP response code
	 */
	public abstract int getResponseCode();

	/**
	 * Returns the HTTP response message
	 *
	 * @return The HTTP response message
	 */
	public abstract Object getResponseMessage();

	/**
	 * Returns the HTTP response body
	 *
	 * @return The HTTP response body
	 */
	public abstract byte[] getResponseBody();

}