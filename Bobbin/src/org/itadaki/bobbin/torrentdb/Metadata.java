package org.itadaki.bobbin.torrentdb;

import java.io.IOException;


/**
 * A key value store used to hold persistent torrent metadata
 */
public interface Metadata {

	/**
	 * Stores a key / value pair
	 *
	 * @param key The key
	 * @param value The value, which may be {@code null}
	 * @throws IOException On any I/O error 
	 */
	void put (String key, byte[] value) throws IOException;

	/**
	 * Gets the value for a key
	 *
	 * @param key The key
	 * @return The value, if any, or {@code null}
	 * @throws IOException On any I/O error 
	 */
	byte[] get (String key) throws IOException;

	/**
	 * Closes the {@code Metadata}, releasing all system resources
	 */
	void close();

}
