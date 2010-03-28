/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BEncoder;
import org.itadaki.bobbin.bencode.InvalidEncodingException;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.TorrentManager;
import org.itadaki.bobbin.peer.TorrentManagerListener;
import org.itadaki.bobbin.peer.TorrentSetController;
import org.itadaki.bobbin.peer.TorrentSetControllerListener;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.MetaInfo;
import org.itadaki.bobbin.torrentdb.Metadata;
import org.itadaki.bobbin.torrentdb.MetadataProvider;
import org.junit.Test;

import test.Util;


/**
 * Tests TorrentSetController
 */
public class TestTorrentSetController {

	/**
	 * @return A test MetaInfo
	 * @throws InvalidEncodingException
	 */
	private MetaInfo createTestMetaInfo() throws InvalidEncodingException {

		BDictionary info = new BDictionary();
		info.put ("length", 1024);
		info.put ("name", "TestTorrent.txt");
		info.put ("piece length", 262144);
		info.put ("pieces", "01234567890123456789");
		BDictionary torrent = new BDictionary();
		torrent.put ("announce", "http://te.st.zz:6666/announce");
		torrent.put ("info", info);
		MetaInfo metaInfo = new MetaInfo (torrent);
		return metaInfo;
	}


	/**
	 * Tests starting and stopping a TorrentSetController with one TorrentManager
	 * @throws Exception
	 */
	@Test
	public void testStartStopOneManager() throws Exception {

		TorrentSetController controller = new TorrentSetController();

		MetaInfo metaInfo = createTestMetaInfo();

		TorrentManager manager = controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());

		final CountDownLatch startedLatch = new CountDownLatch (1);
		final CountDownLatch stoppedLatch = new CountDownLatch (1);
		manager.addListener (new TorrentManagerListener() {
			public void torrentManagerError (TorrentManager torrentManager) { }

			public void torrentManagerRunning (TorrentManager torrentManager) {
				startedLatch.countDown();
			}

			public void torrentManagerStopped (TorrentManager torrentManager) {
				stoppedLatch.countDown();
			}

			public void torrentManagerTerminated (TorrentManager torrentManager) { }
			
		});

		assertTrue (controller.isRunning());
		assertFalse (controller.isTerminated());
		assertFalse (manager.isEnabled());

		controller.start();
		startedLatch.await (2, TimeUnit.SECONDS);

		assertTrue (manager.isEnabled());

		controller.stop();
		stoppedLatch.await (2, TimeUnit.SECONDS);

		assertFalse (manager.isEnabled());

	}


	/**
	 * Tests terminating a TorrentSetController with one TorrentManager
	 * @throws Exception
	 */
	@Test
	public void testTerminateSynchronousOneManager() throws Exception {

		TorrentSetController controller = new TorrentSetController();

		MetaInfo metaInfo = createTestMetaInfo();

		TorrentManager manager = controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());

		assertTrue (controller.isRunning());
		assertFalse (controller.isTerminated());
		assertFalse (manager.isEnabled());

		controller.terminate (true);

		assertFalse (controller.isRunning());
		assertTrue (controller.isTerminated());
		assertFalse (manager.isEnabled());

	}


	/**
	 * Tests terminating a TorrentSetController with one TorrentManager asynchronously
	 * @throws Exception
	 */
	@Test
	public void testTerminateAsynchronousOneManager() throws Exception {

		TorrentSetController controller = new TorrentSetController();

		final CountDownLatch terminatedLatch = new CountDownLatch (1);
		controller.addListener (new TorrentSetControllerListener() {
			public void torrentSetControllerTerminated (TorrentSetController controller) {
				terminatedLatch.countDown();
			}
		});

		MetaInfo metaInfo = createTestMetaInfo();
		TorrentManager manager = controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());


		assertTrue (controller.isRunning());
		assertFalse (controller.isTerminated());
		assertFalse (manager.isEnabled());

		controller.terminate (false);
		terminatedLatch.await (2, TimeUnit.SECONDS);

		assertTrue (controller.isTerminated());

	}


	/**
	 * Tests routing of an incoming connection to its PeerCoordinator
	 * @throws Exception
	 */
	@Test
	public void testIncomingConnection() throws Exception {

		TorrentSetController controller = new TorrentSetController();
		int localPort = controller.getLocalPort();

		MetaInfo metaInfo = createTestMetaInfo();
		TorrentManager manager = controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());
		manager.start (true);

		assertEquals (0, manager.getPeers().size());

		// Handshake through the port and wait to hear that the coordinator has replied
		final SocketChannel socketChannel = SocketChannel.open (new InetSocketAddress (InetAddress.getLocalHost(), localPort));
		socketChannel.write (PeerProtocolBuilder.handshake (false, false, metaInfo.getInfo().getHash(), new PeerID()));

		while (socketChannel.read (ByteBuffer.allocate (1)) == 0);

		Thread.sleep (250);

		assertEquals (1, manager.getPeers().size());

		socketChannel.close();
		controller.terminate (true);

	}


	/**
	 * Tests creating a TorrentManager from a MetaInfo when terminated
	 * @throws Exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testTorrentMetaInfoTerminated() throws Exception {

		TorrentSetController controller = new TorrentSetController();
		controller.terminate (true);

		MetaInfo metaInfo = createTestMetaInfo();
		controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());

	}


	/**
	 * Tests creating a TorrentManager from a file
	 * @throws Exception
	 */
	@Test
	public void testTorrentFile() throws Exception {

		TorrentSetController controller = new TorrentSetController();

		MetaInfo metaInfo = createTestMetaInfo();
		File torrentFile = File.createTempFile ("bbt", "tmp");
		RandomAccessFile randomAccessFile = new RandomAccessFile (torrentFile, "rw");
		randomAccessFile.write (BEncoder.encode (metaInfo.getDictionary()));
		randomAccessFile.close();

		controller.addTorrentManager (torrentFile, Util.createTemporaryDirectory());

	}


	/**
	 * Tests creating a TorrentManager from a file when terminated
	 * @throws Exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testTorrentFileTerminated() throws Exception {

		TorrentSetController controller = new TorrentSetController();
		controller.terminate (true);

		MetaInfo metaInfo = createTestMetaInfo();
		File torrentFile = File.createTempFile ("bbt", "tmp");
		RandomAccessFile randomAccessFile = new RandomAccessFile (torrentFile, "rw");
		randomAccessFile.write (BEncoder.encode (metaInfo.getDictionary()));
		randomAccessFile.close();

		controller.addTorrentManager (torrentFile, Util.createTemporaryDirectory());

	}


	/**
	 * Tests adding a listener
	 * @throws Exception
	 */
	@Test
	public void testAddListener() throws Exception {

		TorrentSetController controller = new TorrentSetController();

		MetaInfo metaInfo = createTestMetaInfo();
		controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());

		final boolean[] terminatedCalled = new boolean[1];
		TorrentSetControllerListener listener = new TorrentSetControllerListener() {
			public void torrentSetControllerTerminated (TorrentSetController controller) {
				terminatedCalled[0] = true;
			}
		};
		controller.addListener (listener);

		controller.terminate (true);
		assertTrue (terminatedCalled[0]);

	}


	/**
	 * Tests adding and removing a listener
	 * @throws Exception
	 */
	@Test
	public void testAddRemoveListener() throws Exception {

		TorrentSetController controller = new TorrentSetController();

		MetaInfo metaInfo = createTestMetaInfo();
		controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());

		final boolean[] terminatedCalled = new boolean[1];
		TorrentSetControllerListener listener = new TorrentSetControllerListener() {
			public void torrentSetControllerTerminated (TorrentSetController controller) {
				terminatedCalled[0] = true;
			}
		};
		controller.addListener (listener);
		controller.removeListener (listener);

		controller.terminate (true);
		assertFalse (terminatedCalled[0]);

	}


	/**
	 * Tests getTorrentManager
	 * @throws Exception
	 */
	@Test
	public void testGetTorrentManager() throws Exception {

		TorrentSetController controller = new TorrentSetController();

		MetaInfo metaInfo = createTestMetaInfo();
		controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());

		assertNotNull (controller.getTorrentManager (metaInfo.getInfo().getHash()));

	}


	/**
	 * Tests getAllTorrentManagers
	 * @throws Exception
	 */
	@Test
	public void testGetAllTorrentManagers() throws Exception {

		TorrentSetController controller = new TorrentSetController();

		MetaInfo metaInfo = createTestMetaInfo();
		controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());

		List<TorrentManager> managers = controller.getAllTorrentManagers();
		assertEquals (1, managers.size());
		assertEquals (metaInfo.getInfo().getHash(), managers.get(0).getMetaInfo().getInfo().getHash());

	}


	/**
	 * Tests forgetting a torrent
	 * @throws Exception
	 */
	@Test
	public void testForgetTorrent() throws Exception {

		final boolean[] forgetCalled = new boolean[1];
		TorrentSetController controller = new TorrentSetController (new MetadataProvider() {

			public Metadata metadataFor (InfoHash infoHash) throws IOException {
				return null;
			}

			public void forget (InfoHash infoHash) throws IOException {
				forgetCalled[0] = true;
			}

		});

		MetaInfo metaInfo = createTestMetaInfo();
		TorrentManager manager = controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());
		manager.terminate (true);
		controller.forgetTorrent (manager.getMetaInfo().getInfo().getHash());

		assertTrue (forgetCalled[0]);

	}


	/**
	 * Tests forgetting a currently registered torrent
	 * @throws Exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testForgetRegisteredTorrent() throws Exception {

		final boolean[] forgetCalled = new boolean[1];
		TorrentSetController controller = new TorrentSetController (new MetadataProvider() {

			public Metadata metadataFor (InfoHash infoHash) throws IOException {
				return null;
			}

			public void forget (InfoHash infoHash) throws IOException {
				forgetCalled[0] = true;
			}

		});

		MetaInfo metaInfo = createTestMetaInfo();
		TorrentManager manager = controller.addTorrentManager (metaInfo, Util.createTemporaryDirectory());
		controller.forgetTorrent (manager.getMetaInfo().getInfo().getHash());

	}

}
