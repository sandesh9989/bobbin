package org.itadaki.bobbin.torrentdb;

import java.util.ArrayList;
import java.util.List;


/**
 * A mutable fileset used to represent the current (rather than initial) fileset of a torrent. A
 * MutableFileset is created either with an initial single or multi-file fileset as specified in an
 * Info, or as an empty fileset that can be differentiated into a single or multi-file fileset at a
 * later point
 */
public class MutableFileset {

	/**
	 * If {@code false}, the fileset has not yet been differentiated into a single or multi-file
	 * fileset, and cannot be extended even if unsealed. If {@code true}, the fileset has been
	 * differentiated, and can be extended if unsealed.
	 */
	private boolean initialised = false;

	/**
	 * If {@code false}, files can be added to the fileset if it has been initialised. If
	 * {@code true}, the fileset cannot be extended.
	 */
	private boolean filesSealed = false;

	/**
	 * If {@code false}, the last file can be extended if the fileset has been initialised. If
	 * {@code true}, the last file cannot be extended.
	 */
	private boolean dataSealed = false;

	/**
	 * The base directory name of the fileset. This may be set once during initialisation if the
	 * fileset is to be a multi-file fileset, or remain null for a single-file fileset
	 */
	private String baseDirectoryName = null;

	/**
	 * The files currently contained within the fileset
	 */
	private List<Filespec> files = new ArrayList<Filespec>();


	/**
	 * @return The base directory name of the fileset. May be {code null} before the fileset is
	 * differentiated, or in the case of a single-file fileset after differentiation
	 */
	public String getBaseDirectoryName() {

		return this.baseDirectoryName;

	}


	/**
	 * @return The current files of the fileset
	 */
	public List<Filespec> getFiles() {

		return new ArrayList<Filespec> (this.files);

	}


	/**
	 * Applies a fileset delta to the fileset
	 *
	 * @param delta The delta to apply
	 */
	public void applyDelta (FilesetDelta delta) {

		if (!this.initialised) {
			throw new IllegalStateException();
		}

		if (!canExtendData()) {
			throw new IllegalArgumentException ("Cannot extend fileset data");
		}

		if (!canExtendFiles() && (delta.getAdditionalFiles().size() > 0)) {
			throw new IllegalArgumentException ("Cannot add files to fileset");
		}

		if ((this.files.size() == 0) && (delta.getLastFileLength() != 0)) {
			throw new IllegalArgumentException ("Cannot extend nonexistent file");
		}

		if (this.files.size() > 0) {
			Filespec currentFile = this.files.get (this.files.size() - 1);

			if (delta.getLastFileLength() < currentFile.getLength()) {
				throw new IllegalArgumentException ("Cannot shrink existing file");
			}

			Filespec updatedFile = new Filespec (currentFile.getName(), delta.getLastFileLength());

			this.files.set (this.files.size() - 1, updatedFile);
		}

		this.filesSealed = (delta.isDataSealed() | delta.isFilesSealed());
		this.dataSealed = delta.isDataSealed();

		this.files.addAll (delta.getAdditionalFiles());

	}


	/**
	 * @return {@code true} if the data of the final file in the fileset can currently be extended,
	 * otherwise {@code false}
	 */
	public boolean canExtendData() {

		return this.initialised && !this.dataSealed;

	}


	/**
	 * @return {@code true} if new files can currently be added to the fileset, otherwise
	 * {@code false}
	 */
	public boolean canExtendFiles() {

		return this.initialised && !this.filesSealed && (this.baseDirectoryName != null);

	}


	/**
	 * @param infoFileset
	 * @throws IllegalStateException If the MutableFileset is already initialised
	 */
	public void setInfoFileset (InfoFileset infoFileset) {

		if (this.initialised) {
			throw new IllegalStateException();
		}

		this.baseDirectoryName = infoFileset.getBaseDirectoryName();
		this.files.addAll (infoFileset.getFiles());
		this.initialised = true;

	}


	/**
	 * Creates an undifferentiated MutableFileset
	 */
	public MutableFileset() {

	}


	/**
	 * Creates a MutableFileset with an initial InfoFileset
	 *
	 * @param infoFileset The initial InfoFileset
	 */
	public MutableFileset (InfoFileset infoFileset) {

		this.initialised = true;
		this.baseDirectoryName = infoFileset.getBaseDirectoryName();
		this.files.addAll (infoFileset.getFiles());

	}


}
