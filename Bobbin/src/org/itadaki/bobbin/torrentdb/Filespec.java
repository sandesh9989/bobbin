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
	private final List<String> name;

	/**
	 * The file length
	 */
	private final Long length;


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + this.length.hashCode();
		result = prime * result + this.name.hashCode();

		return result;

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (Object other) {

		if (this == other) {
			return true;
		}

		if ((other == null) || (getClass() != other.getClass())) {
			return false;
		}

		Filespec otherFilespec = (Filespec) other;

		if ((!this.length.equals (otherFilespec.length)) || (this.name.size() != otherFilespec.name.size())) {
			return false;
		}

		for (int i = 0; i < this.name.size(); i++) {
			if (!this.name.get(i).equals(otherFilespec.name.get(i))) {
				return false;
			}
		}

		return true;

	}


	/**
	 * @return The parts of the filename
	 */
	public List<String> getName() {

		return this.name;

	}


	/**
	 * @return The file length
	 */
	public Long getLength() {

		return this.length;

	}


	/**
	 * Constructs a Filespec with a multi-part filename
	 *
	 * @param name The parts of the filename
	 * @param length The file length
	 */
	public Filespec (List<String> name, Long length) {

		if (length < 0) {
			throw new IllegalArgumentException ("file length < 0");
		}

		if ((name == null) || (name.size() == 0)) {
			throw new IllegalArgumentException ("null or empty filename");
		}

		for (String part : name) {
			if ((part == null) || (part.length() == 0)) {
				throw new IllegalArgumentException ("null or empty filename component");
			}
		}

		this.name = Collections.unmodifiableList (new ArrayList<String> (name));
		this.length = length;

	}


	/**
	 * Constructs a filespec with a single-part filename (that is, a bare filename with no
	 * directories)
	 *
	 * @param name The filename
	 * @param length The file length
	 */
	public Filespec (String name, Long length) {

		this (Collections.singletonList (name), length);

	}


}
