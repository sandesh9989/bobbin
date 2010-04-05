package test.torrentdb;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.itadaki.bobbin.torrentdb.FilesetDelta;
import org.itadaki.bobbin.torrentdb.Filespec;
import org.itadaki.bobbin.torrentdb.InfoFileset;
import org.itadaki.bobbin.torrentdb.MutableFileset;
import org.junit.Test;

/**
 * Tests MutableFileset
 */
public class TestMutableFileset {

	/**
	 * Tests an undifferentiated MutableFileset
	 */
	@Test
	public void testUndifferentiated() {

		// Given
		MutableFileset mutableFileset = new MutableFileset();

		// Then
		List<Filespec> files = mutableFileset.getFiles();
		assertEquals (0, files.size());
		assertNull (mutableFileset.getBaseDirectoryName());
		assertFalse (mutableFileset.canExtendData());
		assertFalse (mutableFileset.canExtendFiles());

	}


	/**
	 * Tests differentiating to a single file InfoFileset
	 */
	@Test
	public void testDifferentiateSingleFile() {

		// Given
		Filespec filespec = new Filespec (Arrays.asList (new String[] { " A " }), 1234L);
		InfoFileset infoFileset = new InfoFileset (filespec);
		MutableFileset mutableFileset = new MutableFileset();

		// When
		mutableFileset.setInfoFileset (infoFileset);

		// Then
		List<Filespec> files = mutableFileset.getFiles();
		assertEquals (1, files.size());
		assertEquals (1234L, files.get(0).getLength());
		assertNull (mutableFileset.getBaseDirectoryName());
		assertTrue (mutableFileset.canExtendData());
		assertFalse (mutableFileset.canExtendFiles());

	}


	/**
	 * Tests differentiating to a multi-file InfoFileset
	 */
	@Test
	public void testDifferentiateMultiFile() {

		// Given
		List<Filespec> infoFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " A " }), 1234L) });
		InfoFileset infoFileset = new InfoFileset ("base", infoFiles);
		MutableFileset mutableFileset = new MutableFileset();

		// When
		mutableFileset.setInfoFileset (infoFileset);

		// Then
		List<Filespec> files = mutableFileset.getFiles();
		assertEquals (1, files.size());
		assertEquals (1234L, files.get(0).getLength());
		assertEquals ("base", mutableFileset.getBaseDirectoryName());
		assertTrue (mutableFileset.canExtendData());
		assertTrue (mutableFileset.canExtendFiles());

	}


	/**
	 * Tests applying a delta
	 */
	@Test
	public void testSingleFileApplyDelta() {

		// Given
		Filespec filespec = new Filespec (Arrays.asList (new String[] { " A " }), 1234L);
		InfoFileset infoFileset = new InfoFileset (filespec);
		MutableFileset mutableFileset = new MutableFileset (infoFileset);
		FilesetDelta delta = new FilesetDelta (4567L, new ArrayList<Filespec>(), false, false);

		// When
		mutableFileset.applyDelta (delta);

		// Then
		List<Filespec> updatedFiles = mutableFileset.getFiles();
		assertEquals (1, updatedFiles.size());
		assertEquals (4567L, updatedFiles.get(0).getLength());
		assertNull (mutableFileset.getBaseDirectoryName());
		assertTrue (mutableFileset.canExtendData());
		assertFalse (mutableFileset.canExtendFiles());

	}


	/**
	 * Tests applying a delta
	 */
	@Test
	public void testMultiFileApplyDelta() {

		// Given
		List<Filespec> infoFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " A " }), 1234L) });
		InfoFileset infoFileset = new InfoFileset ("base", infoFiles);
		MutableFileset mutableFileset = new MutableFileset (infoFileset);
		List<Filespec> additionalFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " B " }), 1234L) });
		FilesetDelta delta = new FilesetDelta (4567L, additionalFiles, false, false);

		// When
		mutableFileset.applyDelta (delta);

		// Then
		List<Filespec> updatedFiles = mutableFileset.getFiles();
		assertEquals (2, updatedFiles.size());
		assertEquals (4567L, updatedFiles.get(0).getLength());
		assertEquals ("base", mutableFileset.getBaseDirectoryName());
		assertTrue (mutableFileset.canExtendData());
		assertTrue (mutableFileset.canExtendFiles());

	}


	/**
	 * Tests applying a delta
	 */
	@Test
	public void testMultiFileApplyDeltaSealFiles() {

		// Given
		List<Filespec> infoFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " A " }), 1234L) });
		InfoFileset infoFileset = new InfoFileset ("base", infoFiles);
		MutableFileset mutableFileset = new MutableFileset (infoFileset);
		List<Filespec> additionalFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " B " }), 1234L) });
		FilesetDelta delta = new FilesetDelta (4567L, additionalFiles, true, false);

		// When
		mutableFileset.applyDelta (delta);

		// Then
		List<Filespec> updatedFiles = mutableFileset.getFiles();
		assertEquals (2, updatedFiles.size());
		assertEquals (4567L, updatedFiles.get(0).getLength());
		assertEquals ("base", mutableFileset.getBaseDirectoryName());
		assertTrue (mutableFileset.canExtendData());
		assertFalse (mutableFileset.canExtendFiles());

	}


	/**
	 * Tests applying a delta
	 */
	@Test
	public void testMultiFileApplyDeltaSealData() {

		// Given
		List<Filespec> infoFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " A " }), 1234L) });
		InfoFileset infoFileset = new InfoFileset ("base", infoFiles);
		MutableFileset mutableFileset = new MutableFileset (infoFileset);
		List<Filespec> additionalFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " B " }), 1234L) });
		FilesetDelta delta = new FilesetDelta (4567L, additionalFiles, false, true);

		// When
		mutableFileset.applyDelta (delta);

		// Then
		List<Filespec> updatedFiles = mutableFileset.getFiles();
		assertEquals (2, updatedFiles.size());
		assertEquals (4567L, updatedFiles.get(0).getLength());
		assertEquals ("base", mutableFileset.getBaseDirectoryName());
		assertFalse (mutableFileset.canExtendData());
		assertFalse (mutableFileset.canExtendFiles());

	}


	/**
	 * Tests differentiating to a single file InfoFileset
	 */
	@Test(expected=IllegalStateException.class)
	public void testInvalidDoubleDifferentiate() {

		// Given
		Filespec filespec = new Filespec (Arrays.asList (new String[] { " A " }), 1234L);
		InfoFileset infoFileset = new InfoFileset (filespec);
		MutableFileset mutableFileset = new MutableFileset();

		// When
		mutableFileset.setInfoFileset (infoFileset);
		mutableFileset.setInfoFileset (infoFileset);

		// Then
		// ... exception

	}


	/**
	 * Tests applying a delta
	 */
	@Test(expected=IllegalStateException.class)
	public void testInvalidUndifferentiatedApplyDelta() {

		// Given
		MutableFileset mutableFileset = new MutableFileset();
		FilesetDelta delta = new FilesetDelta (4567L, new ArrayList<Filespec>(), false, false);

		// When
		mutableFileset.applyDelta (delta);

		// Then
		// ... exception

	}


	/**
	 * Tests applying an invalid delta
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidSingleFileApplyDelta() {

		// Given
		Filespec filespec = new Filespec (Arrays.asList (new String[] { " A " }), 1234L);
		InfoFileset infoFileset = new InfoFileset (filespec);
		MutableFileset mutableFileset = new MutableFileset (infoFileset);
		List<Filespec> additionalFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " B " }), 1234L) });
		FilesetDelta delta = new FilesetDelta (4567L, additionalFiles, false, false);

		// When
		mutableFileset.applyDelta (delta);

		// Then
		// ... exception

	}


	/**
	 * Tests applying an invalid delta
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidApplyDeltaSealedFiles() {

		// Given
		List<Filespec> infoFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " A " }), 1234L) });
		InfoFileset infoFileset = new InfoFileset ("base", infoFiles);
		MutableFileset mutableFileset = new MutableFileset (infoFileset);
		List<Filespec> additionalFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " B " }), 1234L) });
		List<Filespec> additionalFiles2 = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " C " }), 1234L) });
		FilesetDelta delta = new FilesetDelta (4567L, additionalFiles, true, false);

		// When
		mutableFileset.applyDelta (delta);
		mutableFileset.applyDelta (new FilesetDelta (8901L, additionalFiles2, false, false));

		// Then
		// ... exception

	}


	/**
	 * Tests applying an invalid delta
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidApplyDeltaSealedData() {

		// Given
		List<Filespec> infoFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " A " }), 1234L) });
		InfoFileset infoFileset = new InfoFileset ("base", infoFiles);
		MutableFileset mutableFileset = new MutableFileset (infoFileset);
		List<Filespec> additionalFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " B " }), 1234L) });
		FilesetDelta delta = new FilesetDelta (4567L, additionalFiles, false, true);

		// When
		mutableFileset.applyDelta (delta);
		mutableFileset.applyDelta (new FilesetDelta (8901L, new ArrayList<Filespec>(), false, false));

		// Then
		// ... exception

	}


	/**
	 * Tests applying an invalid delta
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidApplyDeltaExtendNoFiles() {

		// Given
		List<Filespec> infoFiles = new ArrayList<Filespec>();
		InfoFileset infoFileset = new InfoFileset ("base", infoFiles);
		MutableFileset mutableFileset = new MutableFileset (infoFileset);
		List<Filespec> additionalFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " B " }), 1234L) });
		FilesetDelta delta = new FilesetDelta (4567L, additionalFiles, false, true);

		// When
		mutableFileset.applyDelta (delta);

		// Then
		// ... exception

	}


	/**
	 * Tests applying an invalid delta
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidApplyDeltaShrinksFile() {

		// Given
		List<Filespec> infoFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " A " }), 1234L) });
		InfoFileset infoFileset = new InfoFileset ("base", infoFiles);
		MutableFileset mutableFileset = new MutableFileset (infoFileset);
		List<Filespec> additionalFiles = Arrays.asList (new Filespec[] { new Filespec (Arrays.asList (new String[] { " B " }), 1234L) });
		FilesetDelta delta = new FilesetDelta (0L, additionalFiles, false, true);

		// When
		mutableFileset.applyDelta (delta);

		// Then
		// ... exception

	}

}
