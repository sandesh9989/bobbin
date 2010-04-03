package org.itadaki.bobbin.torrentdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The specification of a single torrent file
 */
public class Filespec {

	/**
	 * The parts of the filename
	 */
	public final List<String> name;

	/**
	 * The file length
	 */
	public final Long length;


	/**
	 * @param name The parts of the filename
	 * @param length The file length
	 */
	public Filespec (List<String> name, Long length) {

		this.name = Collections.unmodifiableList (new ArrayList<String> (name));
		this.length = length;

	}

}
