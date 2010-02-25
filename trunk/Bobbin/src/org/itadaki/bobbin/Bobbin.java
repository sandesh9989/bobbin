/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDecoder;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BEncoder;
import org.itadaki.bobbin.bencode.InvalidEncodingException;
import org.itadaki.bobbin.connectionmanager.ConnectionManager;
import org.itadaki.bobbin.peer.Peer;
import org.itadaki.bobbin.peer.TorrentManager;
import org.itadaki.bobbin.peer.TorrentSetController;
import org.itadaki.bobbin.torrentdb.IncompatibleLocationException;
import org.itadaki.bobbin.torrentdb.InfoBuilder;
import org.itadaki.bobbin.torrentdb.MetaInfo;
import org.itadaki.bobbin.tracker.HTTPTracker;
import org.itadaki.bobbin.tracker.Tracker;
import org.itadaki.bobbin.util.BitField;



/**
 * A Bittorrent peer and tracker application
 */
public class Bobbin {

	/**
	 * The default piece length
	 */
	private static final int PIECE_LENGTH = 262144;

	/**
	 * Prints a program usage message
	 */
	private static void usage() {

		System.out.println ("Usage: bobbin <torrent file...>");
		System.out.println ("          to download one or more torrents");
		System.out.println ("   or  bobbin [command] [options] ...");
		System.out.println ("");
		System.out.println ("where commands include:");
		System.out.println ("  --download [port=<listen port>] <torrent file...>");
		System.out.println ("          peers on one or more torrents, listening on the specified port");
		System.out.println ("  --createtorrent <torrent file> <file|directory> <tracker url>");
		System.out.println ("          creates a torrent file for the specified file or directory");
		System.out.println ("  --tracker [host=<hostname|ip>[:<port>]] <torrent file...>");
		System.out.println ("          starts a tracker for the specified torrent file(s)");

	}


	/**
	 * Exits forcefully, printing a message to stderr
	 *
	 * @param message The message to display
	 */
	private static void exitWithError (String message) {

		System.err.println (message);
		System.exit (0);

	}


	/**
	 * Main method
	 *
	 * @param arguments ignores
	 */
	public static void main (String[] arguments) {

		if (arguments.length == 0) {

			usage();

		} else {

			if (arguments[0].equals ("--download")) {

				if (arguments.length != 2) {
					System.err.println ("Error: Missing argument");
					usage();
					System.exit (0);
				}

				String torrentFilename = arguments[1];
				File torrentFile = new File (torrentFilename);

				TorrentSetController controller = null;
				try {
					controller = new TorrentSetController();
				} catch (IOException e) {
					exitWithError ("Could not open server socket");
				}

				// Can't actually happen. Satisfies compiler stupidity
				if (controller == null) return;

				TorrentManager manager = null;
				try {
					manager = controller.addTorrentManager (torrentFile, new File("."));
					manager.start (false);
				} catch (InvalidEncodingException e) {
					exitWithError ("Invalid torrent file");
				} catch (IncompatibleLocationException e) {
					exitWithError ("Incompatible location or filename");
				} catch (IOException e) {
					exitWithError ("Could not read torrent file");
				}

				// Can't actually happen. Satisfies compiler stupidity
				if (manager == null) return;

				while (true) {
					try {
						Thread.sleep (1000);
					} catch (InterruptedException e) {
						// Do nothing
					}

					BitField presentPieces = manager.getPresentPieces();
					int numPiecesPresent = presentPieces.cardinality();
					System.out.format (
							"%d of %d (%1.2f%%) pieces present%n",
							numPiecesPresent,
							presentPieces.length(),
							(float) (100 * numPiecesPresent) / presentPieces.length()
					);
					Set<Peer> peers = manager.getPeers();
					for (Peer peer : peers) {
						System.out.format (
								"  %s%s%s%s %s%n",
								peer.getWeAreChoking() ? "C" : ".",
								peer.getWeAreInterested() ? "I" : ".",
								peer.getTheyAreChoking() ? "c" : ".",
								peer.getTheyAreInterested() ? "i" : ".",
								peer.getRemoteSocketAddress()
						);
					}
					System.out.println();

					if (numPiecesPresent == presentPieces.length()) {
						// break;
					}

				}

			} else if (arguments[0].equals ("--createtorrent")) {

				if (arguments.length != 4) {
					System.err.println ("Error: Missing argument");
					usage();
					System.exit (0);
				}

				String torrentFilename = arguments[1];
				String sharedFilename = arguments[2];
				String announceURL = arguments[3];

				File torrentFile = new File (torrentFilename);
				if (torrentFile.exists()) {
					exitWithError ("Cowardly refusing to overwrite existing file " + torrentFile);
				}
				File sharedFile = new File (sharedFilename);

				try {

					List<String> tier = Arrays.asList (new String[] { announceURL });
					List<List<String>> announceURLs = new ArrayList<List<String>>();
					announceURLs.add (tier);

					MetaInfo metaInfo = new MetaInfo (announceURLs, InfoBuilder.createPlain (sharedFile, PIECE_LENGTH).build(), null);
					BDictionary dictionary = metaInfo.getDictionary();

					FileOutputStream output = new FileOutputStream (torrentFile);
					BEncoder.encode (output, dictionary);
					output.close();

					System.out.println ("Wrote torrent file: " + torrentFile);

				} catch (IOException e) {
					exitWithError ("Failed to create torrent file: " + e.getMessage());
				}

			} else if (arguments[0].equals ("--tracker")) {

				try {

					if (arguments.length < 2) {
						System.err.println ("Error: Missing argument");
						usage();
						System.exit (0);
					}

					ConnectionManager manager = new ConnectionManager();
					HTTPTracker httpTracker = new HTTPTracker (manager, null, 9090);
					Tracker tracker = httpTracker.getTracker();

					tracker.lock();
					for (int i = 1; i < arguments.length; i++) {
						String torrentFilename = arguments[i];

						File torrentFile = new File (torrentFilename);
						FileInputStream input = new FileInputStream (torrentFile);
						BDictionary dictionary = new BDecoder(input).decodeDictionary();
						input.close();
						MetaInfo metaInfo = new MetaInfo (dictionary);

						tracker.register (metaInfo.getInfo().getHash());
					}
					tracker.unlock();

					while (true) {
						try {
							Thread.sleep (Integer.MAX_VALUE);
						} catch (InterruptedException e) {
							// Do nothing
						}
					}

				} catch (IOException e) {
					exitWithError ("Failed to start tracker: " + e.getMessage());
				}

			}

		}

	}

}
