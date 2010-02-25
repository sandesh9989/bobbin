/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package demo;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.itadaki.bobbin.bencode.BDecoder;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BEncoder;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.peer.TorrentManager;
import org.itadaki.bobbin.peer.TorrentSetController;
import org.itadaki.bobbin.torrentdb.InfoBuilder;
import org.itadaki.bobbin.torrentdb.MetaInfo;
import org.itadaki.bobbin.tracker.HTTPTracker;
import org.itadaki.bobbin.tracker.Tracker;


/**
 * A demonstration of Elastic seeding
 *
 * When invoked with no arguments, creates a zero length file and periodically appends test data,
 * seeding the additional data as it is added
 * When invoked with a filename as first argument, watches that file for additional data and seeds
 * it as it is detected (note: this file must only be appended).
 */
public class DemoElasticSeed {

	/**
	 * Creates an Elastic torrent file appropriate to the demonstration
	 *
	 * @param keyPair The key pair
	 * @param sharedFile The file to share
	 * @return The torrent file
	 * @throws Exception
	 */
	private static File createTorrentFile (KeyPair keyPair, File sharedFile) throws Exception {

		String announceURL = "http://localhost:9090/announce";

		File torrentFile = File.createTempFile ("bbt", ".torrent");

		List<String> tier = Arrays.asList (new String[] { announceURL });
		List<List<String>> announceURLs = new ArrayList<List<String>>();
		announceURLs.add (tier);

		MetaInfo metaInfo = new MetaInfo (announceURLs, InfoBuilder.createElastic (sharedFile, 16384, keyPair.getPrivate()).build(), keyPair);
		BDictionary dictionary = metaInfo.getDictionary();

		FileOutputStream output = new FileOutputStream (torrentFile);
		BEncoder.encode (output, dictionary);
		output.close();

		return torrentFile.getAbsoluteFile();

	}


	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main (String[] args) throws Exception {

		final Semaphore extensionSemaphore = new Semaphore (1);

		boolean syntheticFile = false;
		File sharedFile = null;
		long sharedFileLength = 0;

		if (args.length == 0) {
			syntheticFile = true;
			sharedFile = File.createTempFile("bbt", ".txt");
		} else if (args.length == 1) {
			sharedFile = new File(args[0]).getAbsoluteFile();
			sharedFileLength = sharedFile.length();
		} else {
			System.err.println ("Syntax: \"DemoElasticSeed\" or \"DemoElasticSeed <filename>\"");
			System.exit (0);
			return;
		}
		System.out.println ("Sharing file: " + sharedFile);


		// Create key pair to sign torrent
		System.out.println ("Generating key pair...");
		KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance ("DSA", "SUN");
		SecureRandom random = SecureRandom.getInstance ("SHA1PRNG", "SUN");
		keyGenerator.initialize (1024, random);
		KeyPair keyPair = keyGenerator.generateKeyPair();
		System.out.println ("...Done");


		// Build and write torrent for test file
		File torrentFile = createTorrentFile (keyPair, sharedFile);
		System.out.println ("Torrent file: " + torrentFile);


		// Create tracker
		FileInputStream input = new FileInputStream (torrentFile);
		BDictionary dictionary = new BDecoder(input).decodeDictionary();
		input.close();
		ConnectionManager trackerConnectionManager = new ConnectionManager();
		MetaInfo metaInfo = new MetaInfo (dictionary);
		HTTPTracker httpTracker = new HTTPTracker (trackerConnectionManager, null, 9090);
		Tracker tracker = httpTracker.getTracker();
		tracker.lock();
		tracker.register (metaInfo.getInfo().getHash());
		tracker.unlock();


		// Create seed peer
		final TorrentSetController seedController = new TorrentSetController();
		File destinationDirectory = File.createTempFile ("bbt", null);
		destinationDirectory.delete();
		destinationDirectory.mkdir();
		TorrentManager seedTorrentManager = seedController.addTorrentManager (torrentFile, destinationDirectory);
		seedTorrentManager.start (true);
		System.out.println ("Destination directory : " + destinationDirectory);


		// Create ordinary peer
		final TorrentSetController ordinaryController = new TorrentSetController();
		File ordinaryDestinationDirectory = File.createTempFile ("bbt", null);
		ordinaryDestinationDirectory.delete();
		ordinaryDestinationDirectory.mkdir();
		TorrentManager ordinaryTorrentManager = ordinaryController.addTorrentManager (torrentFile, ordinaryDestinationDirectory);
		ordinaryTorrentManager.start (true);


		// Create ordinary peer window
		TorrentWindow torrentWindow = new TorrentWindow (torrentFile.getName(), ordinaryTorrentManager);
		torrentWindow.addWindowListener (new WindowAdapter() {
			@Override
			public void windowClosing (WindowEvent e) {
				extensionSemaphore.acquireUninterruptibly();
				seedController.terminate (true);
				ordinaryController.terminate (true);
				System.exit (0);
			}
		});
		torrentWindow.setVisible (true);


		// Periodically extend the data
		for (int i = 0; ; i++) {

			Thread.sleep (1000);

			extensionSemaphore.acquire();

			int delta = 0;

			if (syntheticFile) {

				// Append a test block to the file
				delta = 1000 + new Random().nextInt(1048576);
				sharedFileLength += delta;
				byte[] testData = new byte[delta];
				Arrays.fill (testData, (byte)0x0a);
				ByteBuffer buffer = ByteBuffer.wrap (testData);
				buffer.put (new String ("Block " + i).getBytes());
				buffer.rewind();
				seedTorrentManager.extendData (keyPair.getPrivate(), buffer);

			} else {

				// Extend the torrent to cover any new data in the test file
				long currentFileLength = sharedFile.length();
				delta = (int) Math.min (8 * 1048576, (currentFileLength - sharedFileLength));
				if (delta > 0) {
					sharedFileLength += delta;
					seedTorrentManager.extendDataInPlace (keyPair.getPrivate(), sharedFileLength);
				}

			}

			if (delta > 0){
				System.out.println ("Shared data length : " + sharedFileLength);
			}
			extensionSemaphore.release();

		}

	}

}
