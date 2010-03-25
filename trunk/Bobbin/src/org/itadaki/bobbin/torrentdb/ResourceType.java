package org.itadaki.bobbin.torrentdb;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * The set of possible Resource extension resources
 */
public enum ResourceType {

	/**
	 * A resource containing a torrent's Info
	 */
	INFO (1, "info"),

	/**
	 * A resource containing a torrent's x.509 certificate
	 */
	X509_CERTIFICATE (2, "x.509 certificate"),

	/**
	 * A resource containing a torrent's x.509 signature
	 */
	X509_SIGNATURE (3, "x.509 signature"),

	/**
	 * A resource containing an Elastic torrent's fileset deltas
	 */
	FILESET_DELTAS (4, "fileset deltas");


	/**
	 * A map of ResourceTypes used for lookups by name
	 */
	private static final Map<String,ResourceType> types = new HashMap<String,ResourceType>();

	static {
		
		for (ResourceType type : EnumSet.allOf (ResourceType.class)) {
			types.put (type.name, type);
		}
	}

	/**
	 * The resource's ID as used in the outbound protocol
	 */
	public final int id;

	/**
	 * The resource's name
	 */
	public final String name;


	/**
	 * @param name The name to get a ResourceType for
	 * @return The ResourceType for the given name, or {@code null}
	 */
	public static ResourceType forName (String name) {

		return types.get (name);

	}


	/**
	 * @param id The resource's ID as used in the outbound protocol
	 * @param name The resource's name
	 */
	private ResourceType (int id, String name) {

		this.id = id;
		this.name = name;

	}

}
