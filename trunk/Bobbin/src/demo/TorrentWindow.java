/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package demo;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.itadaki.bobbin.peer.Peer;
import org.itadaki.bobbin.peer.PeerStatistics;
import org.itadaki.bobbin.peer.TorrentManager;
import org.itadaki.bobbin.trackerclient.TrackerClientStatus;


/**
 * A window to display the state of a torrent
 */
public class TorrentWindow extends JFrame {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The torrent's manager
	 */
	private TorrentManager manager;

	/**
	 * A TableModel for the display of peer data
	 */
	private class PeerTableModel extends AbstractTableModel {

		/**
		 * Serial version UID
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * The table's column header names
		 */
		private String[] columnNames = { "Peer Address", "Flags", "Uploaded", "Downloaded", "View length" };

		/**
		 * The most recently queried peer set
		 */
		List<Peer> cachedPeers = new ArrayList<Peer>();


		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int column) {

			return this.columnNames[column];

		}


		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {

			return this.columnNames.length;

		}


		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public synchronized int getRowCount() {

			return this.cachedPeers.size();

		}


		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public synchronized Object getValueAt (int rowIndex, int columnIndex) {

			Peer peer = this.cachedPeers.get (rowIndex);
			switch (columnIndex) {
				case 0:
					return peer.getRemoteSocketAddress();
				case 1:
					return String.format (
							"%s%s%s%s",
							peer.getWeAreChoking() ? "C" : ".",
							peer.getWeAreInterested() ? "I" : ".",
							peer.getTheyAreChoking() ? "c" : ".",
							peer.getTheyAreInterested() ? "i" : "."
					);
				case 2:
					return String.format ("%1.2f KB/s", (float)peer.getReadableStatistics().getPerSecond (PeerStatistics.Type.PROTOCOL_BYTES_SENT) / 1000);
				case 3:
					return String.format ("%1.2f KB/s", (float)peer.getReadableStatistics().getPerSecond (PeerStatistics.Type.PROTOCOL_BYTES_RECEIVED) / 1000);
				case 4:
					return peer.getRemoteViewLength();
			}

			return null;

		}


		/**
		 * Updates the model's peer data. Should be called on the Swing thread
		 *
		 * @param peers The peer data to update with
		 */
		public synchronized void updatePeerSet (final Set<Peer> peers) {

			PeerTableModel.this.cachedPeers.clear();
			PeerTableModel.this.cachedPeers.addAll (peers);
			fireTableDataChanged();

		}

	}


	/**
	 * @param name The name of the window
	 * @param manager The TorrentManager to display the state of
	 */
	public TorrentWindow (String name, TorrentManager manager) {

		super (name);
		this.manager = manager;

		setSize (640, 480);
		setLayout (new BoxLayout (getContentPane(), BoxLayout.PAGE_AXIS));

		final JButton startStopButton = new JButton ("Stop");
		startStopButton.setAlignmentX (Component.LEFT_ALIGNMENT);
		startStopButton.addActionListener (new ActionListener() {
			private boolean enabled = true;
			public void actionPerformed (ActionEvent e) {
				this.enabled = !this.enabled;
				startStopButton.setText (this.enabled ? "Stop" : "Start");
				if (this.enabled) {
					TorrentWindow.this.manager.start (false);
				} else {
					TorrentWindow.this.manager.stop (false);
				}
			}
		});
		final JLabel bytesSentPerSecondLabel = new JLabel ("Sent: ");
		bytesSentPerSecondLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		final JLabel bytesReceivedPerSecondLabel = new JLabel ("Received: ");
		bytesReceivedPerSecondLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		final JLabel verifiedLabel = new JLabel ("Verified: ");
		verifiedLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		final JLabel completionLabel = new JLabel ("Complete: ");
		completionLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		final JLabel lastAnnounceLabel = new JLabel ("Last Announce: ");
		lastAnnounceLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		final JLabel trackerResponseLabel = new JLabel ("Tracker Response: ");
		trackerResponseLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		final JLabel nextAnnounceLabel = new JLabel ("Next Announce: ");
		nextAnnounceLabel.setAlignmentX (Component.LEFT_ALIGNMENT);
		final PeerTableModel peerTableModel = new PeerTableModel();
		JTable peerTable = new JTable (peerTableModel);
		JScrollPane peerScrollPane = new JScrollPane (peerTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		add (startStopButton);
		add (bytesSentPerSecondLabel);
		add (bytesReceivedPerSecondLabel);
		add (verifiedLabel);
		add (completionLabel);
		add (lastAnnounceLabel);
		add (trackerResponseLabel);
		add (nextAnnounceLabel);
		add (peerScrollPane);

		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				final int bytesSentPerSecond = TorrentWindow.this.manager.getProtocolBytesSentPerSecond();
				final int bytesReceivedPerSecond = TorrentWindow.this.manager.getProtocolBytesReceivedPerSecond();
				final TrackerClientStatus trackerClientStatus = TorrentWindow.this.manager.getTrackerClientStatus();
				final int verifiedPieceCount = TorrentWindow.this.manager.getVerifiedPieceCount();
				final int totalPieces = TorrentWindow.this.manager.getNumberOfPieces();
				final int numPresentPieces = TorrentWindow.this.manager.getPresentPieces().cardinality();
				final Set<Peer> peers = TorrentWindow.this.manager.getPeers();

				SwingUtilities.invokeLater (new Runnable() {
					public void run() {
						bytesSentPerSecondLabel.setText (String.format ("Sent: %1.2f KB/s", (float)bytesSentPerSecond / 1000));
						bytesReceivedPerSecondLabel.setText (String.format ("Received: %1.2f KB/s", (float)bytesReceivedPerSecond / 1000));
						Long timeOfLastTrackerUpdate = trackerClientStatus.getTimeOfLastUpdate();
						String trackerFailureReason = trackerClientStatus.getFailureReason();
						Integer timeUntilNextTrackerUpdate = trackerClientStatus.getTimeUntilNextUpdate ();
						String lastAnnounceTime = (timeOfLastTrackerUpdate == null) ? "N/A" : new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss").format (new Date (timeOfLastTrackerUpdate));
						String trackerResponse = (timeOfLastTrackerUpdate == null) ? "N/A" : (trackerFailureReason == null) ? "Success" : trackerFailureReason;
						nextAnnounceLabel.setText ("Next tracker update: " + (
								trackerClientStatus.isUpdating() ? "In progress" : 
									((timeUntilNextTrackerUpdate != null) ? timeUntilNextTrackerUpdate : "N/A")
								));
						lastAnnounceLabel.setText ("Last Announce: " + lastAnnounceTime);
						trackerResponseLabel.setText ("Tracker Response: " + trackerResponse);
						verifiedLabel.setText ("Verified: " + verifiedPieceCount + " / " + totalPieces);
						float percentComplete = (totalPieces == 0) ? 0 : ((float) (100 * numPresentPieces) / totalPieces);
						completionLabel.setText (String.format ("Complete: %d of %d pieces (%1.2f%%)", numPresentPieces, totalPieces, percentComplete));
						peerTableModel.updatePeerSet (peers);
					}
				});
			}

		};
		new Timer().schedule (task, 1000, 1000);

	}

}
