/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;

import static org.junit.Assert.*;


import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.TorrentManager;
import org.itadaki.bobbin.torrentdb.MetaInfo;
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

		BDictionary info = new BDictionary();
		info.put ("length", 1024);
		info.put ("name", "TestTorrent.txt");
		info.put ("piece length", 262144);
		info.put ("pieces", "01234567890123456789");
		BDictionary torrent = new BDictionary();
		torrent.put ("announce", "http://te.st.zz:6666/announce");
		torrent.put ("info", info);
		MetaInfo metaInfo = new MetaInfo (torrent);

		ConnectionManager connectionManager = new ConnectionManager();

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);
		BitField wantedPieces = new BitField (1);
		wantedPieces.not();
		TorrentManager torrentManager = new TorrentManager (new PeerID(), 0, metaInfo, connectionManager, pieceDatabase, wantedPieces);

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

		BDictionary info = new BDictionary();
		info.put ("length", 1024);
		info.put ("name", "TestTorrent.txt");
		info.put ("piece length", 262144);
		info.put ("pieces", "01234567890123456789");
		BDictionary torrent = new BDictionary();
		torrent.put ("announce", "http://te.st.zz:6666/announce");
		torrent.put ("info", info);
		MetaInfo metaInfo = new MetaInfo (torrent);

		ConnectionManager connectionManager = new ConnectionManager();

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);

		BitField wantedPieces = new BitField (1);
		wantedPieces.not();
		TorrentManager torrentManager = new TorrentManager (new PeerID(), 0, metaInfo, connectionManager, pieceDatabase, wantedPieces);

		torrentManager.start (true);

		assertTrue (torrentManager.isComplete());

		torrentManager.stop (true);

		connectionManager.close();

	}



}
