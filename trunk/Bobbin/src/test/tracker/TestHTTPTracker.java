package test.tracker;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDecoder;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.tracker.HTTPTracker;
import org.itadaki.bobbin.tracker.Tracker;
import org.itadaki.bobbin.util.CharsetUtil;
import org.junit.Test;



/**
 * Test HTTPTracker
 */
public class TestHTTPTracker {

	/**
	 * Opens an HTTP connection to a tracker
	 *
	 * @param tracker The tracker to connect to
	 * @param path The path to request
	 * @return An open HTTP connection to the tracker
	 * @throws Exception
	 */
	private static HttpURLConnection trackerConnection (HTTPTracker tracker, String path) throws Exception {

		int port = tracker.getPort();
		URL trackerURL = new URI("http://localhost:" + port + path).toURL();
		HttpURLConnection connection = (HttpURLConnection) trackerURL.openConnection();

		return connection;

	}


	/**
	 * Reads an input stream fully into a String, interpreting it as UTF-8
	 *
	 * @param input The input stream to read from
	 * @return A string containing the content of the input stream interpreted as UTF-8
	 * @throws Exception 
	 */
	private static String readFully (InputStream input) throws Exception {

		BufferedReader reader = new BufferedReader (new InputStreamReader (input, "UTF-8"));

		StringBuffer buffer = new StringBuffer();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append (line);
		}

		return buffer.toString();

	}

	
	/**
	 * Encodes a tracker request into a GET parameter string
	 * 
	 * @param infoHash 
	 * @param peerID 
	 * @param peerKey
	 * @param port 
	 * @param uploaded 
	 * @param downloaded 
	 * @param remaining 
	 * @param event 
	 * @param compact 
	 * @return An encoded GET parameter string
	 */
	public String encodeTrackerParameters (byte[] infoHash, byte[] peerID, byte[] peerKey, String port,
			String uploaded, String downloaded, String remaining, String event, boolean compact)
	{

		String[] parameters = new String[] {
				"info_hash", CharsetUtil.urlencode (infoHash),
				"peer_id", CharsetUtil.urlencode (peerID),
				"key", CharsetUtil.urlencode (peerKey),
				"port", port,
				"uploaded", uploaded,
				"downloaded", downloaded,
				"left", remaining,
				"event", event,
				"compact", compact ? "1" : "0"
		};

		String encodedParameters = "";
		for (int i = 0; i < parameters.length; i += 2) {
			if (parameters[i+1] != null) {
				encodedParameters += parameters[i] + "=" + parameters[i+1];
				if (i < (parameters.length - 2)) {
					encodedParameters += "&";
				}
			}
		}

		return encodedParameters;

	}


	/**
	 * Tests friendly response for getting "/"
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testFriendlyResponse() throws Exception {

		ConnectionManager manager = new ConnectionManager();
		HTTPTracker tracker = new HTTPTracker (manager, null, 0);
		
		HttpURLConnection connection = trackerConnection (tracker, "/");
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		String responseContent = readFully (connection.getInputStream());

		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertEquals ("Hi. I'm a Bittorrent tracker. How are you?", responseContent.toString());

		manager.close();

	}


	/**
	 * Tests response to a "started" event
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testStartedEventResponse() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "ABCDEFGH".getBytes ("US-ASCII");
		int port = 34567;

		String encodedParameters = encodeTrackerParameters (infoHash.getBytes(), peerID, peerKey,
				"" + port, "1234", "5678", "9012", "started", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Send event to tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/announce?" + encodedParameters);
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertNull (response.get ("failure reason"));
		assertEquals (15 * 60, ((BInteger)response.get ("interval")).value().intValue());
		assertEquals (0, ((BInteger)response.get ("complete")).value().intValue());
		assertEquals (1, ((BInteger)response.get ("incomplete")).value().intValue());

		BList peers = (BList) response.get ("peers");

		assertNotNull (peers);
		// Tracker shouldn't give us ourselves
		assertEquals (0, peers.size());

		manager.close();

	}


	/**
	 * Tests response to a "completed" event
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testCompletedEventResponse() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "ABCDEFGH".getBytes ("US-ASCII");
		int port = 34567;

		String encodedParameters = encodeTrackerParameters (infoHash.getBytes(), peerID, peerKey,
				"" + port, "1234", "5678", "9012", "completed", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Send event to tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/announce?" + encodedParameters);
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertNull (response.get ("failure reason"));
		assertEquals (15 * 60, ((BInteger)response.get ("interval")).value().intValue());
		// Note: the current complete/incomplete tally is based on the count of peers with 0 remaining bytes, not "completed" events
		assertEquals (0, ((BInteger)response.get ("complete")).value().intValue());
		assertEquals (1, ((BInteger)response.get ("incomplete")).value().intValue());

		BList peers = (BList) response.get ("peers");

		assertNotNull (peers);
		// Tracker shouldn't give us ourselves
		assertEquals (0, peers.size());

		manager.close();

	}


	/**
	 * Tests response to a "stopped" event
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testStoppedEventResponse() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "ABCDEFGH".getBytes ("US-ASCII");
		int port = 34567;

		String encodedParameters = encodeTrackerParameters (infoHash.getBytes(), peerID, peerKey,
				"" + port, "1234", "5678", "9012", "stopped", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Send event to tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/announce?" + encodedParameters);
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertNull (response.get ("failure reason"));
		assertEquals (15 * 60, ((BInteger)response.get ("interval")).value().intValue());
		assertEquals (0, ((BInteger)response.get ("complete")).value().intValue());
		assertEquals (0, ((BInteger)response.get ("incomplete")).value().intValue());

		BList peers = (BList) response.get ("peers");

		assertNotNull (peers);
		assertEquals (0, peers.size());

		manager.close();

	}


	/**
	 * Tests response to an update event (event="")
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testBlankUpdateEventResponse() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "ABCDEFGH".getBytes ("US-ASCII");
		int port = 34567;

		String encodedParameters = encodeTrackerParameters (infoHash.getBytes(), peerID, peerKey,
				"" + port, "1234", "5678", "9012", "", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Send event to tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/announce?" + encodedParameters);
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertNull (response.get ("failure reason"));
		assertEquals (15 * 60, ((BInteger)response.get ("interval")).value().intValue());
		assertEquals (0, ((BInteger)response.get ("complete")).value().intValue());
		assertEquals (1, ((BInteger)response.get ("incomplete")).value().intValue());

		BList peers = (BList) response.get ("peers");

		assertNotNull (peers);
		// Tracker shouldn't give us ourselves
		assertEquals (0, peers.size());

		manager.close();

	}


	/**
	 * Tests response to an update event (no event=)
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testAbsentUpdateEventResponse() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "ABCDEFGH".getBytes ("US-ASCII");
		int port = 34567;

		String encodedParameters = encodeTrackerParameters (infoHash.getBytes(), peerID, peerKey,
				"" + port, "1234", "5678", "9012", null, false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Send event to tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/announce?" + encodedParameters);
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertNull (response.get ("failure reason"));
		assertEquals (15 * 60, ((BInteger)response.get ("interval")).value().intValue());
		assertEquals (0, ((BInteger)response.get ("complete")).value().intValue());
		assertEquals (1, ((BInteger)response.get ("incomplete")).value().intValue());

		BList peers = (BList) response.get ("peers");

		assertNotNull (peers);
		// Tracker shouldn't give us ourselves
		assertEquals (0, peers.size());

		manager.close();

	}


	/**
	 * Tests response to two peers sending "started" events
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testTwoPeersStarted() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID1 = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerID2 = "-NB0001-dfghjklzxcvb".getBytes ("US-ASCII");
		byte[] peerKey1 = "ABCDEFGH".getBytes ("US-ASCII");
		byte[] peerKey2 = "IJKLMNOP".getBytes ("US-ASCII");
		int port1 = 34567;
		int port2 = 23456;

		String encodedParameters1 = encodeTrackerParameters (infoHash.getBytes(), peerID1, peerKey1,
				"" + port1, "1234", "5678", "9012", "started", false);
		String encodedParameters2 = encodeTrackerParameters (infoHash.getBytes(), peerID2, peerKey2,
				"" + port2, "1234", "5678", "9012", "started", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Send first event to tracker and get response
		HttpURLConnection connection1 = trackerConnection (httpTracker, "/announce?" + encodedParameters1);
		new BDecoder (connection1.getInputStream()).decodeDictionary();

		// Send second event to tracker and get response
		HttpURLConnection connection2 = trackerConnection (httpTracker, "/announce?" + encodedParameters2);
		int responseCode2 = connection2.getResponseCode();
		String responseMessage2 = connection2.getResponseMessage();
		BDecoder decoder2 = new BDecoder (connection2.getInputStream());
		BDictionary response2 = decoder2.decodeDictionary();

		// Test response
		assertEquals (200, responseCode2);
		assertEquals ("OK", responseMessage2);
		assertNull (response2.get ("failure reason"));
		assertEquals (15 * 60, ((BInteger)response2.get ("interval")).value().intValue());
		assertEquals (0, ((BInteger)response2.get ("complete")).value().intValue());
		assertEquals (2, ((BInteger)response2.get ("incomplete")).value().intValue());

		BList peers = (BList) response2.get ("peers");

		assertNotNull (peers);
		assertTrue (peers.size() == 1);

		BDictionary peerMap = (BDictionary) peers.get (0);

		// Peer #2 should get peer #1's details
		assertArrayEquals (peerID1, ((BBinary)peerMap.get ("peer id")).value());
		// Don't technically know what this will be, although in practice it will probably be 127.0.0.1
		assertTrue (((BBinary)peerMap.get ("ip")).stringValue().length() > 0);
		assertEquals (port1, ((BInteger)peerMap.get ("port")).value().intValue());

		manager.close();

	}


	/**
	 * Tests response to two peers sending "started" events with "compact"
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testTwoPeersStartedCompact() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID1 = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerID2 = "-NB0001-dfghjklzxcvb".getBytes ("US-ASCII");
		byte[] peerKey1 = "ABCDEFGH".getBytes ("US-ASCII");
		byte[] peerKey2 = "IJKLMNOP".getBytes ("US-ASCII");
		int port1 = 34567;
		int port2 = 23456;

		String encodedParameters1 = encodeTrackerParameters (infoHash.getBytes(), peerID1, peerKey1,
				"" + port1, "1234", "5678", "9012", "started", true);
		String encodedParameters2 = encodeTrackerParameters (infoHash.getBytes(), peerID2, peerKey2,
				"" + port2, "1234", "5678", "9012", "started", true);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Send first event to tracker and get response
		HttpURLConnection connection1 = trackerConnection (httpTracker, "/announce?" + encodedParameters1);
		new BDecoder (connection1.getInputStream()).decodeDictionary();

		// Send second event to tracker and get response
		HttpURLConnection connection2 = trackerConnection (httpTracker, "/announce?" + encodedParameters2);
		int responseCode2 = connection2.getResponseCode();
		String responseMessage2 = connection2.getResponseMessage();
		BDecoder decoder2 = new BDecoder (connection2.getInputStream());
		BDictionary response2 = decoder2.decodeDictionary();

		// Test response
		assertEquals (200, responseCode2);
		assertEquals ("OK", responseMessage2);
		assertNull (response2.get ("failure reason"));
		assertEquals (15 * 60, ((BInteger)response2.get ("interval")).value().intValue());
		assertEquals (0, ((BInteger)response2.get ("complete")).value().intValue());
		assertEquals (2, ((BInteger)response2.get ("incomplete")).value().intValue());

		BBinary peers = (BBinary) response2.get ("peers");

		assertNotNull (peers);
		assertEquals (6, peers.value().length);

		manager.close();

	}


	/**
	 * Tests that the peer tally is per-hash
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testPeerTallyIndependent() throws Exception {

		InfoHash infoHash1 = new InfoHash ("abcdefghij1234567890".getBytes ("US-ASCII"));
		InfoHash infoHash2 = new InfoHash ("1234567890klmnopqrst".getBytes ("US-ASCII"));
		byte[] peerID1 = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerID2 = "-NB0001-dfghjklzxcvb".getBytes ("US-ASCII");
		byte[] peerKey1 = "ABCDEFGH".getBytes ("US-ASCII");
		byte[] peerKey2 = "IJKLMNOP".getBytes ("US-ASCII");
		int port1 = 34567;
		int port2 = 23456;

		String encodedParameters1 = encodeTrackerParameters (infoHash1.getBytes(), peerID1, peerKey1,
				"" + port1, "1234", "5678", "9012", "started", false);
		String encodedParameters2 = encodeTrackerParameters (infoHash2.getBytes(), peerID2, peerKey2,
				"" + port2, "1234", "5678", "9012", "started", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash1);
		tracker.register (infoHash2);
		tracker.unlock();

		// Send first event to tracker and get response
		HttpURLConnection connection1 = trackerConnection (httpTracker, "/announce?" + encodedParameters1);
		new BDecoder (connection1.getInputStream()).decodeDictionary();

		// Send second event to tracker and get response
		HttpURLConnection connection2 = trackerConnection (httpTracker, "/announce?" + encodedParameters2);
		int responseCode2 = connection2.getResponseCode();
		String responseMessage2 = connection2.getResponseMessage();
		BDecoder decoder2 = new BDecoder (connection2.getInputStream());
		BDictionary response2 = decoder2.decodeDictionary();

		// Test response
		assertEquals (200, responseCode2);
		assertEquals ("OK", responseMessage2);
		assertNull (response2.get ("failure reason"));
		assertEquals (15 * 60, ((BInteger)response2.get ("interval")).value().intValue());
		assertEquals (0, ((BInteger)response2.get ("complete")).value().intValue());
		assertEquals (1, ((BInteger)response2.get ("incomplete")).value().intValue());

		manager.close();

	}


	/**
	 * Tests response to a "started" event by a peer that is already complete
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testStartedComplete() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "ABCDEFGH".getBytes ("US-ASCII");
		int port = 34567;

		String encodedParameters = encodeTrackerParameters (infoHash.getBytes(), peerID, peerKey,
				"" + port, "1234", "5678", "0", "started", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Send event to tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/announce?" + encodedParameters);
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertNull (response.get ("failure reason"));
		assertEquals (15 * 60, ((BInteger)response.get ("interval")).value().intValue());
		assertEquals (1, ((BInteger)response.get ("complete")).value().intValue());
		assertEquals (0, ((BInteger)response.get ("incomplete")).value().intValue());

		BList peers = (BList) response.get ("peers");

		assertNotNull (peers);
		// Tracker shouldn't give us ourselves
		assertEquals (0, peers.size());

		manager.close();

	}


	/**
	 * Tests that a "stopped" event doesn't actually return any peers
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testStoppedEventPeerList() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID1 = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerID2 = "-NB0001-dfghjklzxcvb".getBytes ("US-ASCII");
		byte[] peerKey1 = "ABCDEFGH".getBytes ("US-ASCII");
		byte[] peerKey2 = "IJKLMNOP".getBytes ("US-ASCII");
		int port1 = 34567;
		int port2 = 23456;

		String encodedParameters1 = encodeTrackerParameters (infoHash.getBytes(), peerID1, peerKey1,
				"" + port1, "1234", "5678", "9012", "started", false);
		String encodedParameters2 = encodeTrackerParameters (infoHash.getBytes(), peerID2, peerKey2,
				"" + port2, "1234", "5678", "9012", "stopped", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Send first event to tracker and get response
		HttpURLConnection connection1 = trackerConnection (httpTracker, "/announce?" + encodedParameters1);
		new BDecoder (connection1.getInputStream()).decodeDictionary();

		// Send second event to tracker and get response
		HttpURLConnection connection2 = trackerConnection (httpTracker, "/announce?" + encodedParameters2);
		int responseCode2 = connection2.getResponseCode();
		String responseMessage2 = connection2.getResponseMessage();
		BDecoder decoder2 = new BDecoder (connection2.getInputStream());
		BDictionary response2 = decoder2.decodeDictionary();

		// Test response
		assertEquals (200, responseCode2);
		assertEquals ("OK", responseMessage2);
		assertNull (response2.get ("failure reason"));
		assertEquals (15 * 60, ((BInteger)response2.get ("interval")).value().intValue());
		assertEquals (0, ((BInteger)response2.get ("complete")).value().intValue());
		assertEquals (1, ((BInteger)response2.get ("incomplete")).value().intValue());

		BList peers = (BList) response2.get ("peers");

		assertNotNull (peers);
		assertEquals (0, peers.size());

		manager.close();

	}


	/**
	 * Tests response to an update event
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testInvalidEventResponse() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "ABCDEFGH".getBytes ("US-ASCII");
		int port = 34567;

		String encodedParameters = encodeTrackerParameters (infoHash.getBytes(), peerID, peerKey,
				"" + port, "1234", "5678", "9012", "cheese", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Send event to tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/announce?" + encodedParameters);
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertEquals ("Invalid event", ((BBinary)response.get ("failure reason")).stringValue());

		manager.close();

	}


	/**
	 * Tests response to getting an invalid resource
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testInvalidResource() throws Exception {

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker tracker = new HTTPTracker (manager, null, 0);

		// Send event to tracker and get response
		HttpURLConnection connection = trackerConnection (tracker, "/sheep");
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();

		// Test response
		assertEquals (404, responseCode);
		assertEquals ("Not Found", responseMessage);

		manager.close();

	}


	/**
	 * Tests response to a scrape request on a tracker with one hash and no peers
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testScrapeOneHashNoPeers() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Request scrape from tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/scrape");
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertNull (response.get ("failure reason"));

		BDictionary files = (BDictionary) response.get ("files");

		assertEquals (1, files.size());
		BDictionary hashInfo = (BDictionary) files.get (infoHash.getBytes());
		assertEquals (new BInteger (0), hashInfo.get ("complete"));
		assertEquals (new BInteger (0), hashInfo.get ("downloaded"));
		assertEquals (new BInteger (0), hashInfo.get ("incomplete"));

		manager.close();

	}


	/**
	 * Tests response to a scrape request on a tracker with two hashes and no
	 * peers
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testScrapeTwoHashesNoPeers() throws Exception {

		InfoHash infoHash1 = new InfoHash ("abcdefghij1234567890".getBytes ("US-ASCII"));
		InfoHash infoHash2 = new InfoHash ("1234567890klmnopqrst".getBytes ("US-ASCII"));

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash1);
		tracker.register (infoHash2);
		tracker.unlock();

		// Request scrape from tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/scrape");
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertNull (response.get ("failure reason"));

		BDictionary files = (BDictionary) response.get ("files");

		assertEquals (2, files.size());
		BDictionary hashInfo1 = (BDictionary) files.get (infoHash1.getBytes());
		assertEquals (new BInteger (0), hashInfo1.get ("complete"));
		assertEquals (new BInteger (0), hashInfo1.get ("downloaded"));
		assertEquals (new BInteger (0), hashInfo1.get ("incomplete"));
		BDictionary hashInfo2 = (BDictionary) files.get (infoHash2.getBytes());
		assertEquals (new BInteger (0), hashInfo2.get ("complete"));
		assertEquals (new BInteger (0), hashInfo2.get ("downloaded"));
		assertEquals (new BInteger (0), hashInfo2.get ("incomplete"));

		manager.close();

	}


	/**
	 * Tests response to a scrape request on a tracker with one hash and one
	 * incomplete peer
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testScrapeOneHashOneIncompletePeer() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "ABCDEFGH".getBytes ("US-ASCII");
		int port = 34567;

		String encodedParameters = encodeTrackerParameters (infoHash.getBytes(), peerID, peerKey,
				"" + port, "1234", "5678", "9012", "started", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Request scrape from tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/announce?" + encodedParameters);
		new BDecoder (connection.getInputStream()).decodeDictionary();

		// Send event to tracker and get response
		HttpURLConnection connection2 = trackerConnection (httpTracker, "/scrape");
		int responseCode2 = connection2.getResponseCode();
		String responseMessage2 = connection2.getResponseMessage();
		BDecoder decoder2 = new BDecoder (connection2.getInputStream());
		BDictionary response2 = decoder2.decodeDictionary();

		// Test response
		assertEquals (200, responseCode2);
		assertEquals ("OK", responseMessage2);
		assertNull (response2.get ("failure reason"));

		BDictionary files = (BDictionary) response2.get ("files");

		assertEquals (1, files.size());
		BDictionary hashInfo = (BDictionary) files.get (infoHash.getBytes());
		assertEquals (new BInteger (0), hashInfo.get ("complete"));
		assertEquals (new BInteger (0), hashInfo.get ("downloaded"));
		assertEquals (new BInteger (1), hashInfo.get ("incomplete"));

		manager.close();

	}


	/**
	 * Tests response to a scrape request on a tracker with one hash and one
	 * downloaded peer
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testScrapeOneHashOneDownloadedPeer() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "ABCDEFGH".getBytes ("US-ASCII");
		int port = 34567;

		String encodedParameters = encodeTrackerParameters (infoHash.getBytes(), peerID, peerKey,
				"" + port, "1234", "5678", "0", "completed", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Request scrape from tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/announce?" + encodedParameters);
		new BDecoder (connection.getInputStream()).decodeDictionary();

		// Send event to tracker and get response
		HttpURLConnection connection2 = trackerConnection (httpTracker, "/scrape");
		int responseCode2 = connection2.getResponseCode();
		String responseMessage2 = connection2.getResponseMessage();
		BDecoder decoder2 = new BDecoder (connection2.getInputStream());
		BDictionary response2 = decoder2.decodeDictionary();

		// Test response
		assertEquals (200, responseCode2);
		assertEquals ("OK", responseMessage2);
		assertNull (response2.get ("failure reason"));

		BDictionary files = (BDictionary) response2.get ("files");

		assertEquals (1, files.size());
		BDictionary hashInfo = (BDictionary) files.get (infoHash.getBytes());
		assertEquals (new BInteger (1), hashInfo.get ("complete"));
		assertEquals (new BInteger (1), hashInfo.get ("downloaded"));
		assertEquals (new BInteger (0), hashInfo.get ("incomplete"));

		manager.close();

	}


	/**
	 * Tests response to a scrape request on a tracker with one hash and one
	 * complete peer
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testScrapeOneHashOneCompletePeer() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));
		byte[] peerID = "-NB0001-qwertyuiopas".getBytes ("US-ASCII");
		byte[] peerKey = "ABCDEFGH".getBytes ("US-ASCII");
		int port = 34567;

		String encodedParameters = encodeTrackerParameters (infoHash.getBytes(), peerID, peerKey,
				"" + port, "1234", "5678", "0", "started", false);

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Request scrape from tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/announce?" + encodedParameters);
		new BDecoder (connection.getInputStream()).decodeDictionary();

		// Send event to tracker and get response
		HttpURLConnection connection2 = trackerConnection (httpTracker, "/scrape");
		int responseCode2 = connection2.getResponseCode();
		String responseMessage2 = connection2.getResponseMessage();
		BDecoder decoder2 = new BDecoder (connection2.getInputStream());
		BDictionary response2 = decoder2.decodeDictionary();

		// Test response
		assertEquals (200, responseCode2);
		assertEquals ("OK", responseMessage2);
		assertNull (response2.get ("failure reason"));

		BDictionary files = (BDictionary) response2.get ("files");

		assertEquals (1, files.size());
		BDictionary hashInfo = (BDictionary) files.get (infoHash.getBytes());
		assertEquals (new BInteger (1), hashInfo.get ("complete"));
		assertEquals (new BInteger (0), hashInfo.get ("downloaded"));
		assertEquals (new BInteger (0), hashInfo.get ("incomplete"));

		manager.close();

	}


	/**
	 * Tests response to a selective scrape request on a tracker with three
	 * hashes
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testScrapeThreeHashesSelectively1() throws Exception {

		InfoHash infoHash1 = new InfoHash ("abcdefghij1234567890".getBytes ("US-ASCII"));
		InfoHash infoHash2 = new InfoHash ("1234567890klmnopqrst".getBytes ("US-ASCII"));
		InfoHash infoHash3 = new InfoHash ("uvwxyzabcd1234567890".getBytes ("US-ASCII"));

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash1);
		tracker.register (infoHash2);
		tracker.register (infoHash3);
		tracker.unlock();

		// Request scrape from tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/scrape?info_hash=" + CharsetUtil.urlencode (infoHash3.getBytes()));
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertNull (response.get ("failure reason"));

		BDictionary files = (BDictionary) response.get ("files");

		assertEquals (1, files.size());
		BDictionary hashInfo3 = (BDictionary) files.get (new BBinary (infoHash3.getBytes()));
		assertEquals (new BInteger (0), hashInfo3.get ("complete"));
		assertEquals (new BInteger (0), hashInfo3.get ("downloaded"));
		assertEquals (new BInteger (0), hashInfo3.get ("incomplete"));

		manager.close();

	}


	/**
	 * Tests response to a selective scrape request on a tracker with three
	 * hashes
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testScrapeThreeHashesSelectively2() throws Exception {

		InfoHash infoHash1 = new InfoHash ("abcdefghij1234567890".getBytes ("US-ASCII"));
		InfoHash infoHash2 = new InfoHash ("1234567890klmnopqrst".getBytes ("US-ASCII"));
		InfoHash infoHash3 = new InfoHash ("uvwxyzabcd1234567890".getBytes ("US-ASCII"));

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash1);
		tracker.register (infoHash2);
		tracker.register (infoHash3);
		tracker.unlock();

		// Request scrape from tracker and get response
		HttpURLConnection connection = trackerConnection (
				httpTracker,
				"/scrape?info_hash=" + CharsetUtil.urlencode (infoHash3.getBytes()) + "&info_hash=" + CharsetUtil.urlencode (infoHash2.getBytes())
		);
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertNull (response.get ("failure reason"));

		BDictionary files = (BDictionary) response.get ("files");

		assertEquals (2, files.size());
		BDictionary hashInfo2 = (BDictionary) files.get (infoHash2.getBytes());
		assertEquals (new BInteger (0), hashInfo2.get ("complete"));
		assertEquals (new BInteger (0), hashInfo2.get ("downloaded"));
		assertEquals (new BInteger (0), hashInfo2.get ("incomplete"));
		BDictionary hashInfo3 = (BDictionary) files.get (infoHash3.getBytes());
		assertEquals (new BInteger (0), hashInfo3.get ("complete"));
		assertEquals (new BInteger (0), hashInfo3.get ("downloaded"));
		assertEquals (new BInteger (0), hashInfo3.get ("incomplete"));

		manager.close();

	}


	/**
	 * Tests response to a scrape request for an unknown hash
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testScrapeUnknownHash() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Request scrape from tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/scrape?info_hash=qwertyuiop0987654321");
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertEquals ("Unknown info hash", ((BBinary)response.get ("failure reason")).stringValue());

		manager.close();

	}


	/**
	 * Tests response to a scrape request for an unknown hash
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testScrapeInvalidHash() throws Exception {

		InfoHash infoHash = new InfoHash ("abcdefghij0123456789".getBytes ("US-ASCII"));

		// Set up tracker
		ConnectionManager manager = new ConnectionManager();
		HTTPTracker httpTracker = new HTTPTracker (manager, null, 0);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (infoHash);
		tracker.unlock();

		// Request scrape from tracker and get response
		HttpURLConnection connection = trackerConnection (httpTracker, "/scrape?info_hash=qwertyuiop098765432");
		int responseCode = connection.getResponseCode();
		String responseMessage = connection.getResponseMessage();
		BDecoder decoder = new BDecoder (connection.getInputStream());
		BDictionary response = decoder.decodeDictionary();

		// Test response
		assertEquals (200, responseCode);
		assertEquals ("OK", responseMessage);
		assertEquals ("Invalid info hash", ((BBinary)response.get ("failure reason")).stringValue());

		manager.close();

	}


}
