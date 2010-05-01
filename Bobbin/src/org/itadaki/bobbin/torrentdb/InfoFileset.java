package org.itadaki.bobbin.torrentdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the fileset contained within an Info
 */
public class InfoFileset {

	/**
	 * The base directory name for a multi-file fileset, or {@code null} for a single-file fileset
	 */
	private final String baseDirectoryName;

	/**
	 * The files of the fileset
	 */
	private final List<Filespec> files;

	/**
	 * The cached total length of the fileset
	 */
	private final long length;


	/**
	 * @return The base directory name for a multi-file fileset, or {@code null} for a single-file
	 *         fileset
	 */
	public String getBaseDirectoryName() {

		return this.baseDirectoryName;

	}


	/**
	 * @return The files of the fileset
	 */
	public List<Filespec> getFiles() {

		return this.files;

	}


	/**
	 * @return {@code true} if the fileset is a single-file fileset, or {@code false} if it is
	 *         multi-file
	 */
	public boolean isSingleFile() {

		return (this.baseDirectoryName == null);

	}


	/**
	 * @return The total byte length of the fileset
	 */
	public long getLength() {

		return this.length;

	}


	/**
	 * Creates a multi-file InfoFileset
	 * @param baseDirectoryName The base directory name
	 * @param files The files
	 */
	public InfoFileset (String baseDirectoryName, List<Filespec> files) {

		if (baseDirectoryName == null) {
			throw new IllegalArgumentException ("Null baseDirectoryName");
		}

		this.baseDirectoryName = baseDirectoryName;
		this.files = Collections.unmodifiableList (new ArrayList<Filespec> (files));

		long length = 0;
		for (Filespec filespec : files) {
			length += filespec.getLength();
		}
		this.length = length;

	}


	/**
	 * Creates a single-file InfoFileset
	 * @param file The file
	 */
	public InfoFileset (Filespec file) {

		if (file.getName().size() != 1) {
			throw new IllegalArgumentException ("Single-file InfoFileset may not contain directories");
		}

		List<Filespec> files = Collections.singletonList (file);
		this.baseDirectoryName = null;
		this.files = Collections.unmodifiableList (new ArrayList<Filespec> (files));
		this.length = file.getLength();

	}


}
