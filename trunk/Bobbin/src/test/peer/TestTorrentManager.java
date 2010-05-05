/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;


import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.TorrentManager;
import org.itadaki.bobbin.torrentdb.Info;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.util.BitField;
import org.junit.Test;

import test.torrentdb.MockPieceDatabase;


/**
 * Tests TorrentManager
 */
public class TestTorrentManager {

	// Test download state

	/**
	 * Test that the state is not seeding when initialised with an incomplete PieceDatabase
	 * @throws Exception 
	 */
	@Test
	public void testIncompleteStateNotSeeding() throws Exception {

		BDictionary infoDictionary = new BDictionary();
		infoDictionary.put ("length", 1024);
		infoDictionary.put ("name", "TestTorrent.txt");
		infoDictionary.put ("piece length", 262144);
		infoDictionary.put ("pieces", "01234567890123456789");
		List<List<String>> announceURLs = Collections.singletonList (Collections.singletonList ("http://te.st.zz:6666/announce"));

		ConnectionManager connectionManager = new ConnectionManager();

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		BitField wantedPieces = new BitField (1);
		wantedPieces.not();
		TorrentManager torrentManager = new TorrentManager (new PeerID(), 0, new Info(infoDictionary).getHash(), announceURLs, connectionManager, pieceDatabase);
		torrentManager.setWantedPieces (wantedPieces);

		torrentManager.start (true);

		assertFalse (torrentManager.isComplete());

		torrentManager.stop (true);

		connectionManager.close();

	}


	/**
	 * Test that the state is seeding when initialised with a complete PieceDatabase
	 * @throws Exception 
	 */
	@Test
	public void testCompleteStateSeeding() throws Exception {

		BDictionary infoDictionary = new BDictionary();
		infoDictionary.put ("length", 1024);
		infoDictionary.put ("name", "TestTorrent.txt");
		infoDictionary.put ("piece length", 262144);
		infoDictionary.put ("pieces", "01234567890123456789");
		List<List<String>> announceURLs = Collections.singletonList (Collections.singletonList ("http://te.st.zz:6666/announce"));

		ConnectionManager connectionManager = new ConnectionManager();

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);

		BitField wantedPieces = new BitField (1);
		wantedPieces.not();
		TorrentManager torrentManager = new TorrentManager (new PeerID(), 0, new Info(infoDictionary).getHash(), announceURLs, connectionManager, pieceDatabase);
		torrentManager.setWantedPieces (wantedPieces);

		torrentManager.start (true);

		assertTrue (torrentManager.isComplete());

		torrentManager.stop (true);

		connectionManager.close();

	}



}
