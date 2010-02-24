package org.itadaki.bobbin.torrentdb;

import java.io.IOException;


/**
 * A provider interface to supply {@link Metadata} instances for a given {@link InfoHash}
 */
public interface MetadataProvider {

	/**
	 * @param infoHash The {@code InfoHash} to get a {@code Metadata} for
	 * @return A {@code Metadata} for the given {@code InfoHash}
	 * @throws IOException On any I/O error constructing the {@code Metadata}
	 */
	public Metadata metadataFor (InfoHash infoHash) throws IOException;

	/**
	 * Forgets all stored metadata for the given {@code InfoHash}
	 *
	 * @param infoHash The {@code InfoHash} to forget metadata for
	 * @throws IOException On any I/O error deleting the metadata
	 */
	public void forget (InfoHash infoHash) throws IOException;

}
