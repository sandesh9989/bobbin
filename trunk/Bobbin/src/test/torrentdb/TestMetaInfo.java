/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.torrentdb;

import static org.junit.Assert.*;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.bencode.BValue;
import org.itadaki.bobbin.bencode.InvalidEncodingException;
import org.itadaki.bobbin.torrentdb.Filespec;
import org.itadaki.bobbin.torrentdb.Info;
import org.itadaki.bobbin.torrentdb.MetaInfo;
import org.itadaki.bobbin.util.DSAUtil;
import org.junit.Test;

import test.Util;


/**
 * Test MetaInfo functionality
 */
public class TestMetaInfo {

	/**
	 * A standard test private key
	 */
	private static byte[] testPrivateKeyBytes = new byte[] { 48, -126, 1, 75, 2, 1, 0, 48, -126, 1, 44, 6, 7, 42, -122, 72, -50, 56, 4, 1, 48, -126, 1, 31, 2, -127, -127, 0, -3, 127, 83, -127, 29, 117, 18, 41, 82, -33, 74, -100, 46, -20, -28, -25, -10, 17, -73, 82, 60, -17, 68, 0, -61, 30, 63, -128, -74, 81, 38, 105, 69, 93, 64, 34, 81, -5, 89, 61, -115, 88, -6, -65, -59, -11, -70, 48, -10, -53, -101, 85, 108, -41, -127, 59, -128, 29, 52, 111, -14, 102, 96, -73, 107, -103, 80, -91, -92, -97, -97, -24, 4, 123, 16, 34, -62, 79, -69, -87, -41, -2, -73, -58, 27, -8, 59, 87, -25, -58, -88, -90, 21, 15, 4, -5, -125, -10, -45, -59, 30, -61, 2, 53, 84, 19, 90, 22, -111, 50, -10, 117, -13, -82, 43, 97, -41, 42, -17, -14, 34, 3, 25, -99, -47, 72, 1, -57, 2, 21, 0, -105, 96, 80, -113, 21, 35, 11, -52, -78, -110, -71, -126, -94, -21, -124, 11, -16, 88, 28, -11, 2, -127, -127, 0, -9, -31, -96, -123, -42, -101, 61, -34, -53, -68, -85, 92, 54, -72, 87, -71, 121, -108, -81, -69, -6, 58, -22, -126, -7, 87, 76, 11, 61, 7, -126, 103, 81, 89, 87, -114, -70, -44, 89, 79, -26, 113, 7, 16, -127, -128, -76, 73, 22, 113, 35, -24, 76, 40, 22, 19, -73, -49, 9, 50, -116, -56, -90, -31, 60, 22, 122, -117, 84, 124, -115, 40, -32, -93, -82, 30, 43, -77, -90, 117, -111, 110, -93, 127, 11, -6, 33, 53, 98, -15, -5, 98, 122, 1, 36, 59, -52, -92, -15, -66, -88, 81, -112, -119, -88, -125, -33, -31, 90, -27, -97, 6, -110, -117, 102, 94, -128, 123, 85, 37, 100, 1, 76, 59, -2, -49, 73, 42, 4, 22, 2, 20, 106, 35, 120, 42, -95, -94, 125, -23, -52, -8, 54, 31, -42, 25, 44, -89, -58, -118, 55, -47, };

	/**
	 * A standard test public key
	 */
	private static byte[] testPublicKeyBytes = new byte[] { 48, -126, 1, -73, 48, -126, 1, 44, 6, 7, 42, -122, 72, -50, 56, 4, 1, 48, -126, 1, 31, 2, -127, -127, 0, -3, 127, 83, -127, 29, 117, 18, 41, 82, -33, 74, -100, 46, -20, -28, -25, -10, 17, -73, 82, 60, -17, 68, 0, -61, 30, 63, -128, -74, 81, 38, 105, 69, 93, 64, 34, 81, -5, 89, 61, -115, 88, -6, -65, -59, -11, -70, 48, -10, -53, -101, 85, 108, -41, -127, 59, -128, 29, 52, 111, -14, 102, 96, -73, 107, -103, 80, -91, -92, -97, -97, -24, 4, 123, 16, 34, -62, 79, -69, -87, -41, -2, -73, -58, 27, -8, 59, 87, -25, -58, -88, -90, 21, 15, 4, -5, -125, -10, -45, -59, 30, -61, 2, 53, 84, 19, 90, 22, -111, 50, -10, 117, -13, -82, 43, 97, -41, 42, -17, -14, 34, 3, 25, -99, -47, 72, 1, -57, 2, 21, 0, -105, 96, 80, -113, 21, 35, 11, -52, -78, -110, -71, -126, -94, -21, -124, 11, -16, 88, 28, -11, 2, -127, -127, 0, -9, -31, -96, -123, -42, -101, 61, -34, -53, -68, -85, 92, 54, -72, 87, -71, 121, -108, -81, -69, -6, 58, -22, -126, -7, 87, 76, 11, 61, 7, -126, 103, 81, 89, 87, -114, -70, -44, 89, 79, -26, 113, 7, 16, -127, -128, -76, 73, 22, 113, 35, -24, 76, 40, 22, 19, -73, -49, 9, 50, -116, -56, -90, -31, 60, 22, 122, -117, 84, 124, -115, 40, -32, -93, -82, 30, 43, -77, -90, 117, -111, 110, -93, 127, 11, -6, 33, 53, 98, -15, -5, 98, 122, 1, 36, 59, -52, -92, -15, -66, -88, 81, -112, -119, -88, -125, -33, -31, 90, -27, -97, 6, -110, -117, 102, 94, -128, 123, 85, 37, 100, 1, 76, 59, -2, -49, 73, 42, 3, -127, -124, 0, 2, -127, -128, 127, 98, 8, 74, -33, -73, -124, 85, 31, 64, 80, -84, 5, -125, 11, -24, 67, -24, -22, 28, -39, -119, -116, 80, -91, -62, 58, 110, 119, 24, -74, 78, -104, 80, -116, 110, -45, 100, 44, 119, 93, -36, -65, -28, -55, 38, -62, -14, -37, 45, 92, -63, 33, 13, 43, 12, 14, 10, 72, -25, -18, 108, -24, -84, -60, 24, -46, 92, 94, 60, 121, -102, -36, 91, -24, -49, -49, 67, 110, 35, -76, 11, 110, 22, -54, 124, 11, 121, -72, -125, 20, -47, -106, 5, -84, -55, 74, -85, -120, 3, -107, 56, -114, -14, 14, -120, 89, -35, 93, 15, -35, 69, -12, 20, 4, -60, -48, -125, 101, 122, 39, 116, -100, 54, 18, -125, 107, 45, };


	/**
	 * Returns a standard, valid single file meta info dictionary
	 *
	 * @return a standard, valid single file meta info dictionary
	 */
	private BDictionary standardSingleFileMetaInfo() {

		BDictionary info = new BDictionary();
		info.put ("length", 1024);
		info.put ("name", "TestTorrent.txt");
		info.put ("piece length", 262144);
		info.put ("pieces", "01234567890123456789");

		BDictionary torrent = new BDictionary();
		torrent.put ("announce", "http://te.st:6666/announce");
		torrent.put ("info", info);
		
		return torrent;

	}


	/**
	 * Returns a standard, valid multi-file meta info dictionary
	 *
	 * @return a standard, valid multi-file meta info dictionary
	 */
	private BDictionary standardMultiFileMetaInfo() {

		BDictionary file1 = new BDictionary();
		file1.put ("path", new BList ("dir1", "file1.txt"));
		file1.put ("length", 123);
		
		BDictionary file2 = new BDictionary();
		file2.put ("path", new BList ("file2.txt"));
		file2.put ("length", 456);

		BList files = new BList();
		files.add (file1);
		files.add (file2);

		BDictionary info = new BDictionary();
		info.put ("files", files);
		info.put ("name", "TestTorrent");
		info.put ("piece length", 262144);
		info.put ("pieces", "01234567890123456789");

		BDictionary torrent = new BDictionary();
		torrent.put ("announce", "http://te.st:6666/announce");
		torrent.put ("info", info);
		
		return torrent;
	}


	/* Simple valid dictionaries */

	/**
	 * Test MetaInfo instantiation with a valid single file dictionary
	 *
	 * @throws InvalidEncodingException 
	 */
	@Test
	public void testDictionaryValidSingleFile() throws InvalidEncodingException {

		BDictionary dictionary = standardSingleFileMetaInfo();

		MetaInfo metaInfo = new MetaInfo (dictionary);

		List<Filespec> files = metaInfo.getInfo().getFiles();
		assertEquals (1, files.size());
		assertEquals (1, files.get(0).name.size());
		assertEquals ("TestTorrent.txt", files.get(0).name.get(0));
		assertEquals (new Long (1024), files.get(0).length);

		assertEquals ("http://te.st:6666/announce", metaInfo.getAnnounceURL());
		assertArrayEquals (new byte[] {-17, 25, -10, 77, 107, -18, 22, 120, -117, -11, 0, 43, -15, 94, 67, -74, -36, -66, -63, -73},
				metaInfo.getInfo().getHash().getBytes());
		assertNull (metaInfo.getCreationDate());

	}

	/**
	 * Test MetaInfo instantiation with a valid multi file dictionary
	 *
	 * @throws InvalidEncodingException 
	 */
	@Test
	public void testDictionaryValidMultiFile() throws InvalidEncodingException {

		BDictionary dictionary = standardMultiFileMetaInfo();
		MetaInfo metaInfo = new MetaInfo (dictionary);

		List<Filespec> files = metaInfo.getInfo().getFiles();
		assertEquals (2, files.size());
		assertEquals (2, files.get(0).name.size());
		assertEquals ("dir1", files.get(0).name.get(0));
		assertEquals ("file1.txt", files.get(0).name.get(1));
		assertEquals (new Long (123), files.get(0).length);
		assertEquals (1, files.get(1).name.size());
		assertEquals ("file2.txt", files.get(1).name.get(0));
		assertEquals (new Long (456), files.get(1).length);

		assertEquals ("http://te.st:6666/announce", metaInfo.getAnnounceURL());
		assertArrayEquals (new byte[] {94, 97, -78, 84, 43, -31, 33, 55, 91, -103, 83, 98, -112, -75, -106, -23, -120, 90, 81, 7},
				metaInfo.getInfo().getHash().getBytes());
		assertNull (metaInfo.getCreationDate());

	}


	/**
	 * Test MetaInfo instantiation with a creation date
	 *
	 * @throws InvalidEncodingException 
	 */
	@Test
	public void testDictionaryValidSingleFileCreationDate() throws InvalidEncodingException {

		BDictionary dictionary = standardSingleFileMetaInfo();
		dictionary.put ("creation date", 12345678);

		MetaInfo metaInfo = new MetaInfo (dictionary);

		assertEquals (new Long (12345678), metaInfo.getCreationDate());

	}


	/* Dictionaries broken at the top level */

	/**
	 * Test MetaInfo instantiation with an invalid, blank dictionary
	 * 
	 * @throws InvalidEncodingException 
	 */
	@Test(expected = InvalidEncodingException.class)
	public void testDictionaryInvalidBlank() throws InvalidEncodingException {

		BDictionary dictionary = new BDictionary (new TreeMap<BBinary, BValue>());

		new MetaInfo (dictionary);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with missing 'announce'
	 */
	@Test
	public void testDictionaryInvalidMissingAnnounce() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			dictionary.remove ("announce");
			
			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'announce' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with a non-binary 'announce'
	 */
	@Test
	public void testDictionaryInvalidNonBinaryAnnounce() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			dictionary.put ("announce", new BList());

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'announce' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with missing 'info'
	 */
	@Test
	public void testDictionaryInvalidMissingInfo() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			dictionary.remove ("info");

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'info' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with a non-dictionary 'info'
	 */
	@Test
	public void testDictionaryInvalidNonDictionaryInfo() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			dictionary.put ("info", new BList());

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'info' missing or of incorrect type", message);

	}



	/* Dictionaries broken inside info */

	/**
	 * Test MetaInfo instantiation with an invalid dictionary with missing 'info.name'
	 */
	@Test
	public void testDictionaryInvalidMissingInfoName() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get ("info")).remove ("name");

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'name' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with non-binary 'info.name'
	 */
	@Test
	public void testDictionaryInvalidNonBinaryInfoName() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get("info")).put ("name", new BList());

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'name' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with blank 'info.name'
	 */
	@Test
	public void testDictionaryInvalidBlankInfoName() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get("info")).put ("name", new BBinary (""));

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'name' blank", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with missing 'info.piece length'
	 */
	@Test
	public void testDictionaryInvalidMissingInfoPieceLength() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get("info")).remove ("piece length");

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'piece length' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with missing 'info.piece length'
	 */
	@Test
	public void testDictionaryInvalidNonIntegerInfoPieceLength() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get("info")).put ("piece length", new BList());

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'piece length' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with zero 'info.piece length'
	 */
	@Test
	public void testDictionaryInvalidZeroInfoPieceLength() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get("info")).put ("piece length", 0);

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'piece length' must be greater than zero", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with negative 'info.piece length'
	 */
	@Test
	public void testDictionaryInvalidNegativeInfoPieceLength() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get("info")).put ("piece length", -1);

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'piece length' must be greater than zero", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with neither 'info.length' nor 'info.files'
	 */
	@Test
	public void testDictionaryInvalidMissingInfoPieces() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get("info")).remove ("pieces");

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'pieces' and/or 'root hash' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an valid dictionary with length % piece length == 0 (exactly one piece)
	 */
	@Test
	public void testDictionaryValidInfoLengthPiecesModuloZero() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get("info")).put ("length", 262144);

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertNull (message);

	}


	/**
	 * Test MetaInfo instantiation with an valid dictionary with length % piece length != 0 (2 pieces by one byte)
	 */
	@Test
	public void testDictionaryValidInfoLengthPiecesNonModuloZero() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			BDictionary info = (BDictionary) dictionary.get ("info");
			info.put ("length", 262145);
			info.put ("pieces", "0123456789012345678901234567890123456789");

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertNull (message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with incorrect 'pieces' length
	 */
	@Test
	public void testDictionaryInvalidInfoLengthPieces() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			BDictionary info = (BDictionary) dictionary.get ("info");
			info.put ("length", 262145);

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: incorrect 'pieces' length", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with both 'info.length' and 'info.files'
	 */
	@Test
	public void testDictionaryInvalidPresentInfoLengthPresentInfoFiles() {

		String message = null;

		try {

			BDictionary dictionary = standardMultiFileMetaInfo();
			((BDictionary)dictionary.get("info")).put ("length", 1024);

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: both 'length' and 'files' present", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with neither 'info.length' nor 'info.files'
	 */
	@Test
	public void testDictionaryInvalidMissingInfoLengthMissingInfoFiles() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get("info")).remove ("length");

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'files' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with negative 'info.length'
	 */
	@Test
	public void testDictionaryInvalidNegativeInfoLength() {

		String message = null;

		try {

			BDictionary dictionary = standardSingleFileMetaInfo();
			((BDictionary)dictionary.get("info")).put ("length", -1);

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'length' is negative", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with empty 'info.files'
	 */
	@Test
	public void testDictionaryInvalidEmptyInfoFiles() {

		String message = null;

		try {

			BDictionary dictionary = standardMultiFileMetaInfo();
			((BList)((BDictionary)dictionary.get("info")).get("files")).clear();

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'files' empty", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with files[...] with missing 'path'
	 */
	@Test
	public void testDictionaryInvalidFilesEntryMissingPath() {

		String message = null;

		try {

			BDictionary dictionary = standardMultiFileMetaInfo();
			((BDictionary)((BList)((BDictionary)dictionary.get("info")).get("files")).get(0)).remove ("path");

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'files' element with 'path' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with files[...] with missing 'length'
	 */
	@Test
	public void testDictionaryInvalidFilesEntryMissingLength() {

		String message = null;

		try {

			BDictionary dictionary = standardMultiFileMetaInfo();
			((BDictionary)((BList)((BDictionary)dictionary.get("info")).get("files")).get(0)).remove ("length");

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'files' element with 'length' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with files[...] with missing 'length'
	 */
	@Test
	public void testDictionaryInvalidFilesEntryNegativeLength() {

		String message = null;

		try {

			BDictionary dictionary = standardMultiFileMetaInfo();
			((BDictionary)((BList)((BDictionary)dictionary.get("info")).get("files")).get(0)).put ("length", -1);

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'files' element with negative 'length'", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with files[...] with non-list 'path'
	 */
	@Test
	public void testDictionaryInvalidFilesEntryNonListPath() {

		String message = null;

		try {

			BDictionary dictionary = standardMultiFileMetaInfo();
			((BDictionary)((BList)((BDictionary)dictionary.get("info")).get("files")).get(0)).put ("path", "");

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'files' element with 'path' missing or of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with files[...] blank 'path'
	 */
	@Test
	public void testDictionaryInvalidFilesEntryBlankPath() {

		String message = null;

		try {

			BDictionary dictionary = standardMultiFileMetaInfo();
			(((BList)((BDictionary)((BList)((BDictionary)dictionary.get("info")).get("files")).get(0)).get("path"))).clear();


			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'files' element with blank 'path'", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with files[...] blank 'path' element
	 */
	@Test
	public void testDictionaryInvalidFilesEntryBlankPathElement() {

		String message = null;

		try {

			BDictionary dictionary = standardMultiFileMetaInfo();
			(((BList)((BDictionary)((BList)((BDictionary)dictionary.get("info")).get("files")).get(0)).get("path"))).set (0, "");

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'files' element with blank 'path' element", message);

	}


	/**
	 * Test MetaInfo instantiation with an invalid dictionary with files[...] blank 'path' element
	 */
	@Test
	public void testDictionaryInvalidFilesEntryNonBinaryPathElement() {

		String message = null;

		try {

			BDictionary dictionary = standardMultiFileMetaInfo();
			(((BList)((BDictionary)((BList)((BDictionary)dictionary.get("info")).get("files")).get(0)).get("path"))).set (0, new BList());

			new MetaInfo (dictionary);

		} catch (InvalidEncodingException e) {
			message = e.getMessage ();
		}

		assertEquals ("Invalid torrent dictionary: 'files' element with 'path' element of incorrect type", message);

	}


	/**
	 * Test MetaInfo instantiation with arguments representing a single file
	 *
	 * @throws Exception 
	 */
	@Test
	public void testArgumentsSingleFile() throws Exception {

		String announceURL = "http://fa.ke/announce";
		List<String> tier = Arrays.asList (new String[] { announceURL });
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		announceURLs.add (tier);
		String name = "Test file.txt";
		long length = 1234;
		int pieceSize = 262144;
		byte[] pieces = "01234567890123456789".getBytes();

		MetaInfo metaInfo = new MetaInfo (announceURLs, Info.createSingleFile (name, length, pieceSize, pieces), null);

		assertEquals (announceURL, metaInfo.getAnnounceURL());
		assertEquals (1, metaInfo.getAnnounceURLs().size());
		assertEquals (1, metaInfo.getAnnounceURLs().get(0).size());
		assertEquals (announceURL, metaInfo.getAnnounceURLs().get(0).get (0));

		List<Filespec> files = metaInfo.getInfo().getFiles();
		assertEquals (1, files.size());
		assertEquals (1, files.get(0).name.size());
		assertEquals (name, files.get(0).name.get(0));
		assertEquals (new Long (length), files.get(0).length);

		assertEquals (pieceSize, metaInfo.getInfo().getPieceLength());
		assertArrayEquals (pieces, metaInfo.getInfo().getPieces());
		assertNull (metaInfo.getInfo().getRootHash());

		assertNotNull (metaInfo.getCreationDate());
		assertNotNull (metaInfo.getInfo().getHash());

	}


	/**
	 * Test MetaInfo instantiation with arguments representing a single file Merkle torrent
	 *
	 * @throws Exception 
	 */
	@Test
	public void testArgumentsSingleFileMerkle() throws Exception {

		String announceURL = "http://fa.ke/announce";
		List<String> tier = Arrays.asList (new String[] { announceURL });
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		announceURLs.add (tier);
		String name = "Test file.txt";
		long length = 1234;
		int pieceSize = 262144;
		byte[] rootHash = "01234567890123456789".getBytes();

		MetaInfo metaInfo = new MetaInfo (announceURLs, Info.createSingleFileMerkle (name, length, pieceSize, rootHash), null);

		assertEquals (announceURL, metaInfo.getAnnounceURL());
		assertEquals (1, metaInfo.getAnnounceURLs().size());
		assertEquals (1, metaInfo.getAnnounceURLs().get(0).size());
		assertEquals (announceURL, metaInfo.getAnnounceURLs().get(0).get (0));

		List<Filespec> files = metaInfo.getInfo().getFiles();
		assertEquals (1, files.size());
		assertEquals (1, files.get(0).name.size());
		assertEquals (name, files.get(0).name.get(0));
		assertEquals (new Long (length), files.get(0).length);

		assertEquals (pieceSize, metaInfo.getInfo().getPieceLength());
		assertNull (metaInfo.getInfo().getPieces());
		assertArrayEquals (rootHash, metaInfo.getInfo().getRootHash());

		assertNotNull (metaInfo.getCreationDate());
		assertNotNull (metaInfo.getInfo().getHash());

	}


	/**
	 * Test MetaInfo instantiation with arguments representing a single file Merkle torrent
	 *
	 * @throws Exception 
	 */
	@Test
	public void testArgumentsSingleFileElastic() throws Exception {

		String announceURL = "http://fa.ke/announce";
		List<String> tier = Arrays.asList (new String[] { announceURL });
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		announceURLs.add (tier);
		String name = "Test file.txt";
		long length = 16384;
		int pieceSize = 16384;
		byte[] rootHash = Util.buildHash (Util.pseudoRandomBlockHashes (pieceSize, pieceSize));

		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec (testPrivateKeyBytes);
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec (testPublicKeyBytes);

		KeyFactory keyFactory = KeyFactory.getInstance ("DSA", "SUN");
		PrivateKey privateKey = keyFactory.generatePrivate (privateKeySpec);
		PublicKey publicKey = keyFactory.generatePublic (publicKeySpec);
		Signature sign = Signature.getInstance ("NONEwithDSA", "SUN");
		sign.initSign (privateKey);
		sign.update (rootHash);
		byte[] rootSignature = DSAUtil.derSignatureToP1363Signature (sign.sign());

		MetaInfo metaInfo = new MetaInfo (announceURLs, Info.createSingleFileElastic (name, length, pieceSize, rootHash, rootSignature), new KeyPair (publicKey, privateKey));

		// Would throw exception if the signatures didn't match the key
		new MetaInfo (metaInfo.getDictionary());

	}


	/**
	 * Test MetaInfo instantiation with arguments representing multiple files
	 *
	 * @throws Exception 
	 */
	@Test
	public void testArgumentsMultiFile() throws Exception {

		String announceURL = "http://fa.ke/announce";
		List<String> tier = Arrays.asList (new String[] { announceURL });
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		announceURLs.add (tier);
		String name = "dir1";
		List<List<String>> filePaths = new ArrayList<List<String>>();
		filePaths.add (Arrays.asList (new String[] { "file1.txt" }));
		filePaths.add (Arrays.asList (new String[] { "dir2", "file2.txt" }));
		List<Long> lengths = Arrays.asList (
				(long)1234,
				(long)5678
		);
		int pieceSize = 262144;
		byte[] pieces = "01234567890123456789".getBytes();

		MetaInfo metaInfo = new MetaInfo (announceURLs, Info.createMultiFile (name, filePaths, lengths, pieceSize, pieces), null);

		assertEquals (announceURL, metaInfo.getAnnounceURL());
		assertEquals (1, metaInfo.getAnnounceURLs().size());
		assertEquals (1, metaInfo.getAnnounceURLs().get(0).size());
		assertEquals (announceURL, metaInfo.getAnnounceURLs().get(0).get (0));

		List<Filespec> metaFiles = metaInfo.getInfo().getFiles();
		for (int i = 0; i < filePaths.size(); i++) {
			List<String> filePath = filePaths.get (i);
			List<String> metaFilePath = metaFiles.get(i).name;
			assertArrayEquals (filePath.toArray(), metaFilePath.toArray());
			assertEquals (lengths.get (i), metaFiles.get(i).length);
		}

		assertEquals (pieceSize, metaInfo.getInfo().getPieceLength());
		assertArrayEquals (pieces, metaInfo.getInfo().getPieces());
		assertNull (metaInfo.getInfo().getRootHash());

		assertNotNull (metaInfo.getCreationDate());
		assertNotNull (metaInfo.getInfo().getHash());

	}


	/**
	 * Test MetaInfo instantiation with arguments representing multiple files
	 *
	 * @throws Exception 
	 */
	@Test
	public void testArgumentsMultiFileMerkle() throws Exception {

		String announceURL = "http://fa.ke/announce";
		List<String> tier = Arrays.asList (new String[] { announceURL });
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		announceURLs.add (tier);
		String name = "dir1";
		List<List<String>> filePaths = new ArrayList<List<String>>();
		filePaths.add (Arrays.asList (new String[] { "file1.txt" }));
		filePaths.add (Arrays.asList (new String[] { "dir2", "file2.txt" }));
		List<Long> lengths = Arrays.asList (
				(long)1234,
				(long)5678
		);
		int pieceSize = 262144;
		byte[] rootHash = "01234567890123456789".getBytes();

		MetaInfo metaInfo = new MetaInfo (announceURLs, Info.createMultiFileMerkle (name, filePaths, lengths, pieceSize, rootHash), null);

		assertEquals (announceURL, metaInfo.getAnnounceURL());
		assertEquals (1, metaInfo.getAnnounceURLs().size());
		assertEquals (1, metaInfo.getAnnounceURLs().get(0).size());
		assertEquals (announceURL, metaInfo.getAnnounceURLs().get(0).get (0));

		List<Filespec> metaFiles = metaInfo.getInfo().getFiles();
		for (int i = 0; i < filePaths.size(); i++) {
			List<String> filePath = filePaths.get (i);
			List<String> metaFilePath = metaFiles.get(i).name;
			assertArrayEquals (filePath.toArray(), metaFilePath.toArray());
			assertEquals (lengths.get (i), metaFiles.get(i).length);
		}

		assertEquals (pieceSize, metaInfo.getInfo().getPieceLength());
		assertNull (metaInfo.getInfo().getPieces());
		assertArrayEquals (rootHash, metaInfo.getInfo().getRootHash());

		assertNotNull (metaInfo.getCreationDate());
		assertNotNull (metaInfo.getInfo().getHash());

	}


}
