package org.itadaki.bobbin.torrentdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A delta to apply to a MutableFileset
 */
public class FilesetDelta {

	/**
	 * The length to which to extend the last existing file in the fileset this delta is applied to
	 */
	private final long lastFileLength;

	/**
	 * Additional files to add to the fileset this delta is applied to
	 */
	private final List<Filespec> additionalFiles;

	/**
	 * If {@code true}, no more files may be added after this delta is applied
	 */
	private final boolean filesSealed;

	/**
	 * If {@code true}, the last file may no longer be extended after this delta is applied
	 */
	private final boolean dataSealed;


	/**
	 * @return The length to which to extend the last existing file in the fileset this delta is applied to
	 */
	public long getLastFileLength() {

		return this.lastFileLength;

	}


	/**
	 * @return Additional files to add to the fileset this delta is applied to
	 */
	public List<Filespec> getAdditionalFiles() {

		return this.additionalFiles;

	}


	/**
	 * @return If {@code true}, no more files may be added after this delta is applied
	 */
	public boolean isFilesSealed() {

		return this.filesSealed;

	}


	/**
	 * @return If {@code true}, the last file may no longer be extended after this delta is applied
	 */
	public boolean isDataSealed() {

		return this.dataSealed;

	}


	/**
	 * @param lastFileLength The length to which to extend the last existing file in the fileset
	 *        this delta is applied to. Must be greater than or equal to the previous length
	 * @param additionalFiles Additional files to add to the fileset this delta is applied to
	 * @param filesSealed If {@code true}, no more files may be added after this delta is applied
	 * @param dataSealed If {@code true}, the last file may no longer be extended after this delta
	 *        is applied
	 * @throws IllegalArgumentException if lastFileLength is less than 0 or additionalFiles is null
	 */
	public FilesetDelta (long lastFileLength, List<Filespec> additionalFiles, boolean filesSealed, boolean dataSealed) {

		if (lastFileLength < 0) {
			throw new IllegalArgumentException ("lastFileLength < 0");
		}

		if (additionalFiles == null) {
			throw new IllegalArgumentException ("additionalFiles == null");
		}

		this.lastFileLength = lastFileLength;
		this.additionalFiles = Collections.unmodifiableList (new ArrayList<Filespec> (additionalFiles));
		this.filesSealed = filesSealed;
		this.dataSealed = dataSealed;

	}


}
