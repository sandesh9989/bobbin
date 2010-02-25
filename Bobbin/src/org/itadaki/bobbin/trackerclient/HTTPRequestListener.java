/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.trackerclient;


/**
 * A listener for the completion of an HTTP request
 */
public interface HTTPRequestListener {

	/**
	 * Indicates that an HTTP request has successully completed its request
	 *
	 * @param requestHandler The HTTPRequestHandler that has completed its request
	 */
	public void requestComplete (HTTPRequestHandler requestHandler);

	/**
	 * Indicates that an HTTP request has failed due to an I/O or parse error
	 *
	 * @param requestHandler The HTTPRequestHandler that has encountered an error
	 */
	public void requestError (HTTPRequestHandler requestHandler);

	/**
	 * Indicates that an HTTP request has been cancelled or timed out
	 *
	 * @param requestHandler The HTTPRequestHandler that has been cancelled
	 */
	public void requestCancelled (HTTPRequestHandler requestHandler);

}
