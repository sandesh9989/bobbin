package demo;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import org.itadaki.bobbin.bencode.InvalidEncodingException;
import org.itadaki.bobbin.peer.TorrentManager;
import org.itadaki.bobbin.peer.TorrentSetController;
import org.itadaki.bobbin.torrentdb.IncompatibleLocationException;



/**
 * A demonstration bittorrent client
 */
public class DemoClient {

	/**
	 * The controller for the torrents being downloaded
	 */
	private static TorrentSetController controller;

	/**
	 * Prints a program usage message
	 */
	private static void usage() {

		System.out.println ("Usage: democlient <torrent file>");

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

			if (arguments.length != 1) {
				System.err.println ("Error: Missing argument");
				usage();
				System.exit (0);
			}

			String torrentFilename = arguments[0];
			File torrentFile = new File (torrentFilename);

			File metadataDirectory = new File (new File (System.getProperty ("user.home")), ".democlient" + File.separator + "metadata");
			try {
				controller = new TorrentSetController (metadataDirectory);
			} catch (IOException e) {
				e.printStackTrace ();
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

			TorrentWindow torrentWindow = new TorrentWindow (torrentFile.getName(), manager);
			torrentWindow.addWindowListener (new WindowAdapter() {
				@Override
				public void windowClosing (WindowEvent e) {
					controller.terminate (true);
					System.exit (0);
				}
			});
			torrentWindow.setVisible (true);

			manager.start (false);

		}

	}


}
