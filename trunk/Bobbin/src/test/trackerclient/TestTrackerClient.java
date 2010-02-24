package test.trackerclient;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.tracker.HTTPTracker;
import org.itadaki.bobbin.tracker.PeerInfo;
import org.itadaki.bobbin.tracker.Tracker;
import org.itadaki.bobbin.trackerclient.PeerIdentifier;
import org.itadaki.bobbin.trackerclient.TrackerClient;
import org.itadaki.bobbin.trackerclient.TrackerClientListener;
import org.junit.Test;



/**
 * Tests TrackerClient
 */
public class TestTrackerClient {

	/**
	 * Tests starting against an HTTPTracker
	 * @throws Exception 
	 */
	@Test
	public void testStart() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij1234567890".getBytes ("US-ASCII"));
		PeerID peerID = new PeerID ("-NB0001-qwertyuiopas".getBytes ("US-ASCII"));

		// Set up the tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Set up the client
		int trackerPort = httpTracker.getPort();
		TrackerClientListener listener = new TrackerClientListener() {
			public void peersDiscovered(List<PeerIdentifier> identifiers) { }
			public void trackerClientTerminated() { }
		};
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		List<String> tier = new ArrayList<String> (Arrays.asList ("http://localhost:" + trackerPort + "/announce"));
		announceURLs.add (tier);
		TrackerClient client = new TrackerClient (manager, infoHash, peerID, trackerPort, announceURLs, 1234, 50, listener);

		client.setEnabled (true);
		Thread.sleep (1000);
		tracker.lock();
		try {
			List<PeerInfo> peers = tracker.getPeers (infoHash);
			assertEquals (1, peers.size());
			PeerInfo peer = peers.get(0);
			assertEquals (peerID, peer.getPeerID());
		} finally {
			tracker.unlock();
		}

		manager.close();

	}


	/**
	 * Test a start / stop cycle against an HTTPTracker
	 * @throws Exception 
	 */
	@Test
	public void testStartStop() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij1234567890".getBytes ("US-ASCII"));
		PeerID peerID = new PeerID ("-NB0001-qwertyuiopas".getBytes ("US-ASCII"));

		// Set up the tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Set up the client
		int trackerPort = httpTracker.getPort();
		TrackerClientListener listener = new TrackerClientListener() {
			public void peersDiscovered(List<PeerIdentifier> identifiers) { }
			public void trackerClientTerminated() { }
		};
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		List<String> tier = new ArrayList<String> (Arrays.asList ("http://localhost:" + trackerPort + "/announce"));
		announceURLs.add (tier);
		TrackerClient client = new TrackerClient (manager, infoHash, peerID, trackerPort, announceURLs, 1234, 50, listener);
		client.setEnabled (true);

		Thread.sleep (1000);
		tracker.lock();
		try {
			List<PeerInfo> peers = tracker.getPeers (infoHash);
			assertEquals (1, peers.size());
			PeerInfo peer = peers.get(0);
			assertEquals (peerID, peer.getPeerID());
			assertTrue (client.getTimeUntilNextUpdate() > 0);
		} finally {
			tracker.unlock();
		}

		client.setEnabled (false);

		Thread.sleep (1000);
		tracker.lock();
		try {
			List<PeerInfo> peers = tracker.getPeers (infoHash);
			assertEquals (0, peers.size());
			assertNull (client.getTimeUntilNextUpdate());
		} finally {
			tracker.unlock();
		}

		manager.close();

	}


	/**
	 * Test start/stop, start before stop event is finished against an HTTPTracker
	 * @throws Exception 
	 */
	@Test
	public void testStartStopStartBefore() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij1234567890".getBytes ("US-ASCII"));
		PeerID peerID = new PeerID ("-NB0001-qwertyuiopas".getBytes ("US-ASCII"));

		// Set up the tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Set up the client
		int trackerPort = httpTracker.getPort();
		TrackerClientListener listener = new TrackerClientListener() {
			public void peersDiscovered(List<PeerIdentifier> identifiers) { }
			public void trackerClientTerminated() { }
		};
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		List<String> tier = new ArrayList<String> (Arrays.asList ("http://localhost:" + trackerPort + "/announce"));
		announceURLs.add (tier);
		TrackerClient client = new TrackerClient (manager, infoHash, peerID, trackerPort, announceURLs, 1234, 50, listener);
		client.setEnabled (true);

		List<PeerInfo> peers;

		Thread.sleep (1000);
		tracker.lock();
		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		PeerInfo peer = peers.get(0);
		assertEquals (peerID, peer.getPeerID());
		assertTrue (client.getTimeUntilNextUpdate() > 0);
		tracker.unlock();

		tracker.lock();
		client.setEnabled (false);
		Thread.sleep (500);
		client.setEnabled (true);
		tracker.unlock();

		Thread.sleep (1000);
		tracker.lock();
		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		assertNotNull (client.getTimeUntilNextUpdate());
		tracker.unlock();

		manager.close();

	}


	/**
	 * Test start/stop, start after stop event is finished against an HTTPTracker
	 * @throws Exception 
	 */
	@Test
	public void testStartStopStartAfter() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij1234567890".getBytes ("US-ASCII"));
		PeerID peerID = new PeerID ("-NB0001-qwertyuiopas".getBytes ("US-ASCII"));

		// Set up the tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Set up the client
		int trackerPort = httpTracker.getPort();
		TrackerClientListener listener = new TrackerClientListener() {
			public void peersDiscovered(List<PeerIdentifier> identifiers) { }
			public void trackerClientTerminated() { }
		};
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		List<String> tier = new ArrayList<String> (Arrays.asList ("http://localhost:" + trackerPort + "/announce"));
		announceURLs.add (tier);
		TrackerClient client = new TrackerClient (manager, infoHash, peerID, trackerPort, announceURLs, 1234, 50, listener);
		client.setEnabled (true);

		List<PeerInfo> peers;

		Thread.sleep (1000);
		tracker.lock();
		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		PeerInfo peer = peers.get(0);
		assertEquals (peerID, peer.getPeerID());
		assertTrue (client.getTimeUntilNextUpdate() > 0);
		tracker.unlock();

		client.setEnabled (false);
		Thread.sleep (1000);

		client.setEnabled (true);
		Thread.sleep (1000);
		tracker.lock();
		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		assertNotNull (client.getTimeUntilNextUpdate());
		tracker.unlock();

		manager.close();

	}


	/**
	 * Test start, complete event against an HTTPTracker
	 * @throws Exception 
	 */
	@Test
	public void testStartComplete() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij1234567890".getBytes ("US-ASCII"));
		PeerID peerID = new PeerID ("-NB0001-qwertyuiopas".getBytes ("US-ASCII"));

		// Set up the tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Set up the client
		int trackerPort = httpTracker.getPort();
		TrackerClientListener listener = new TrackerClientListener() {
			public void peersDiscovered(List<PeerIdentifier> identifiers) { }
			public void trackerClientTerminated() { }
		};
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		List<String> tier = new ArrayList<String> (Arrays.asList ("http://localhost:" + trackerPort + "/announce"));
		announceURLs.add (tier);
		TrackerClient client = new TrackerClient (manager, infoHash, peerID, trackerPort, announceURLs, 1234, 50, listener);
		client.setEnabled (true);

		List<PeerInfo> peers;

		Thread.sleep (1000);
		tracker.lock();
		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		PeerInfo peer = peers.get(0);
		assertEquals (peerID, peer.getPeerID());
		assertTrue (client.getTimeUntilNextUpdate() > 0);
		tracker.unlock();

		client.peerCompleted();
		Thread.sleep (1000);

		tracker.lock();
		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		assertTrue (peers.get(0).isComplete());
		tracker.unlock();

		manager.close();

	}


	/**
	 * Test start, terminate against an HTTPTracker
	 * @throws Exception 
	 */
	@Test
	public void testStartTerminate() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij1234567890".getBytes ("US-ASCII"));
		PeerID peerID = new PeerID ("-NB0001-qwertyuiopas".getBytes ("US-ASCII"));

		// Set up the tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Set up the client
		int trackerPort = httpTracker.getPort();
		final CountDownLatch latch = new CountDownLatch (1);
		TrackerClientListener listener = new TrackerClientListener() {
			public void peersDiscovered(List<PeerIdentifier> identifiers) { }
			public void trackerClientTerminated() {
				latch.countDown ();
			}
		};
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		List<String> tier = new ArrayList<String> (Arrays.asList ("http://localhost:" + trackerPort + "/announce"));
		announceURLs.add (tier);
		TrackerClient client = new TrackerClient (manager, infoHash, peerID, trackerPort, announceURLs, 1234, 50, listener);
		client.setEnabled (true);

		List<PeerInfo> peers;

		Thread.sleep (1000);
		tracker.lock();
		peers = tracker.getPeers (infoHash);
		assertEquals (1, peers.size());
		PeerInfo peer = peers.get(0);
		assertEquals (peerID, peer.getPeerID());
		assertTrue (client.getTimeUntilNextUpdate() > 0);
		tracker.unlock();

		client.terminate();
		assertTrue (latch.await (1, TimeUnit.SECONDS));

		tracker.lock();
		peers = tracker.getPeers (infoHash);
		assertEquals (0, peers.size());
		tracker.unlock();

		manager.close();

	}


}
