/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.tracker;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.util.List;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.tracker.PeerInfo;
import org.itadaki.bobbin.tracker.Tracker;
import org.itadaki.bobbin.tracker.TrackerEvent;
import org.junit.Test;



/**
 * Tests the Tracker core
 */
public class TestTracker {

	/**
	 * Test that a STARTED event results in a peer being stored
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEvent() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);

		List<PeerInfo> peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		PeerInfo peer = peers.get (0);
		assertEquals (new PeerID (peerIDBytes), peer.getPeerID());
		assertArrayEquals (peerKey, peer.getKey().array());
		assertEquals (address, peer.getAddress());
		assertEquals (port, peer.getPort());
		assertEquals (uploaded, peer.getUploaded());
		assertEquals (downloaded, peer.getDownloaded());
		assertEquals (remaining, peer.getRemaining());

	}


	/**
	 * Test that a STOPPED event results in a peer being removed
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStoppedEvent() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STOPPED, false);

		List<PeerInfo> peers = tracker.getPeers (infoHash);
		assertEquals (0, peers.size());

	}


	/**
	 * Test that a COMPLETED event results in a peer being marked complete and totals updated
	 *
	 * @throws Exception
	 */
	@Test
	public void testCompletedEvent() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		List<PeerInfo> peers;
		PeerInfo peer;
		tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);

		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		peer = peers.get (0);
		assertFalse (peer.isComplete());

		uploaded = 12345;
		downloaded = 1000;
		remaining = 0;

		tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.COMPLETED, false);

		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		peer = peers.get (0);
		assertTrue (peer.isComplete());
		assertEquals (uploaded, peer.getUploaded());
		assertEquals (downloaded, peer.getDownloaded());
		assertEquals (remaining, peer.getRemaining());

	}


	/**
	 * Test that an UPDATE event results in a peer's update time being changed
	 *
	 * @throws Exception 
	 */
	@Test
	public void testUpdateEvent() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		List<PeerInfo> peers;
		PeerInfo peer;
		tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);

		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		peer = peers.get (0);
		long updateTime = peer.getLastUpdated();

		uploaded = 123;
		downloaded = 456;
		remaining = 544;

		Thread.sleep (1);
		tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.UPDATE, false);

		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		peer = peers.get (0);
		long updateTimeAfter = peer.getLastUpdated();

		assertTrue (updateTimeAfter > updateTime);
		assertEquals (uploaded, peer.getUploaded());
		assertEquals (downloaded, peer.getDownloaded());
		assertEquals (remaining, peer.getRemaining());

	}


	/**
	 * Test that an UPDATE event with an incorrect key is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testUpdateEventIncorrectKey() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);

		peerKey = "Itsfleecewaswhiteassnow".getBytes ("US-ASCII");

		String message = null;
		BDictionary response = tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.UPDATE, false);
		message = response.getString ("failure reason");

		assertEquals ("Invalid peer key", message);

	}


	/**
	 * Test that an COMPLETED event with an unknown peer ID is treated like a START event
	 *
	 * @throws Exception 
	 */
	@Test
	public void testCompletedEventUnknownPeer() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.COMPLETED, false);

		List<PeerInfo> peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		PeerInfo peer = peers.get (0);
		assertEquals (new PeerID (peerIDBytes), peer.getPeerID());
		assertArrayEquals (peerKey, peer.getKey().array());
		assertEquals (address, peer.getAddress());
		assertEquals (port, peer.getPort());
		assertEquals (uploaded, peer.getUploaded());
		assertEquals (downloaded, peer.getDownloaded());
		assertEquals (remaining, peer.getRemaining());

	}


	/**
	 * Test that an STOPPED event with an unknown peer ID is ignored
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStoppedEventUnknownPeer() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STOPPED, false);

		List<PeerInfo> peers = tracker.getPeers (infoHash);
		assertEquals (0, peers.size());

	}


	/**
	 * Test that an STARTED event with an invalid info hash is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventInvalidInfoHash() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		String message;
		for (String badInfoHash : new String[] { "", "abcdefghij012345678", "abcdefghij0123456789a" }) {
			message = "";
			BDictionary response = 	tracker.event (badInfoHash.getBytes ("US-ASCII"), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
			message = response.getString ("failure reason");
			assertEquals ("Invalid info hash", message);
		}

		message = "";
		BDictionary response = tracker.event (null, peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		message = response.getString ("failure reason");
		assertEquals ("Invalid info hash", message);

	}


	/**
	 * Test that an STARTED event with an invalid peer ID is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventInvalidPeerID() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		String message;
		for (String badPeerID : new String[] { "", "-NB0001-qwertyuiopa", "-NB0001-qwertyuiopasd" }) {
			message = "";
			BDictionary response = 	tracker.event (infoHash.getBytes(), badPeerID.getBytes ("US-ASCII"), peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
			message = response.getString ("failure reason");
			assertEquals ("Invalid peer ID", message);
			List<PeerInfo> peers = tracker.getPeers (infoHash);
			assertEquals (0, peers.size());
		}

		message = "";
		BDictionary response = tracker.event (infoHash.getBytes(), null, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		message = response.getString ("failure reason");
		assertEquals ("Invalid peer ID", message);
		List<PeerInfo> peers = tracker.getPeers (infoHash);
		assertEquals (0, peers.size());

	}


	/**
	 * Test that an STARTED event with an invalid peer key is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventInvalidPeerKey() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		byte[] badPeerKey = new byte [256];

		String message = "";
		BDictionary response1 = tracker.event (infoHash.getBytes(), peerIDBytes, badPeerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		message = response1.getString ("failure reason");
		assertEquals ("Invalid peer key", message);

		BDictionary response2 = tracker.event (infoHash.getBytes(), peerIDBytes, null, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		message = response2.getString ("failure reason");
		assertEquals ("Invalid peer key", message);

	}


	/**
	 * Test that an STARTED event with an invalid address is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventInvalidAddress() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		String message = "";
		BDictionary response = tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, null, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		message = response.getString ("failure reason");
		assertEquals ("Invalid address", message);

	}


	/**
	 * Test that an STARTED event with an invalid port is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventInvalidPortLow() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = -1;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		String message = "";
		BDictionary response = tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		message = response.getString ("failure reason");
		assertEquals ("Invalid port", message);

	}


	/**
	 * Test that an STARTED event with an invalid port is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventInvalidPortHigh() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 65536;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		String message = "";
		BDictionary response = tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		message = response.getString ("failure reason");
		assertEquals ("Invalid port", message);

	}


	/**
	 * Test that an STARTED event with an invalid uploaded value is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventInvalidUploaded() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = -1;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		String message = "";
		BDictionary response = tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		message = response.getString ("failure reason");
		assertEquals ("Invalid uploaded", message);

	}


	/**
	 * Test that an STARTED event with an invalid downloaded value is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventInvalidDownloaded() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = -1;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		String message = "";
		BDictionary response = tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		message = response.getString ("failure reason");
		assertEquals ("Invalid downloaded", message);

	}


	/**
	 * Test that an STARTED event with an invalid remaining value is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventInvalidRemaining() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = -1;

		tracker.lock();

		tracker.register (infoHash);

		String message = "";
		BDictionary response = tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, TrackerEvent.STARTED, false);
		message = response.getString ("failure reason");
		assertEquals ("Invalid left", message);

	}


	/**
	 * Test that an STARTED event with an invalid event value is rejected
	 *
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventInvalidEvent() throws Exception {

		Tracker tracker = new Tracker();

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerIDBytes = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "Maryhadalittlelamb".getBytes ("US-ASCII");
		InetAddress address = InetAddress.getLocalHost();;
		int port = 40000;
		long uploaded = 0;
		long downloaded = 0;
		long remaining = 1000;

		tracker.lock();

		tracker.register (infoHash);

		String message = "";
		BDictionary response = tracker.event (infoHash.getBytes(), peerIDBytes, peerKey, address, port, uploaded, downloaded, remaining, null, false);
		message = response.getString ("failure reason");
		assertEquals ("Invalid event", message);

	}


}
