package test.peer.chokingmanager;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.chokingmanager.ChokingManager;
import org.itadaki.bobbin.peer.chokingmanager.DefaultChokingManager;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.counter.Period;
import org.itadaki.bobbin.util.counter.StatisticCounter;
import org.junit.Test;



/**
 * Tests DefaultChokingManager
 */
public class TestDefaultChokingManager {

	/**
	 * A mock StatisticCounter object for use in testing DefaultChokingManager
	 */
	private static class MockStatisticCounter extends StatisticCounter {

		/**
		 * The period total to return
		 */
		private long periodTotal;

		@Override
		public synchronized void add (long value) {
			fail();
		}

		@Override
		public synchronized void addCountedPeriod (Period period) {
			// Do nothing
		}

		@Override
		public synchronized long getPeriodTotal(Period period) {
			return this.periodTotal;
		}

		@Override
		public synchronized long getTotal() {
			fail();
			return 0;
		}

		/**
		 * @param periodTotal The period total to return
		 */
		public MockStatisticCounter (int periodTotal) {
			this.periodTotal = periodTotal;
		}

	}

	/**
	 * A mock ManageablePeer object for use in testing DefaultChokingManager
	 */
	private static class ChokingMockManageablePeer implements ManageablePeer {

		/**
		 * The counter for received block bytes
		 */
		private StatisticCounter blockBytesReceivedCounter;

		/**
		 * The counter for sent block bytes
		 */
		private StatisticCounter blockBytesSentCounter;

		/**
		 * True if we are choking the remote peer
		 */
		private boolean weAreChoking;

		/**
		 * True if the remote peer is interested in us
		 */
		private boolean theyAreInterested;

		/**
		 * Trus if the remote peer has outstanding requests to us
		 */
		private boolean theyHaveOutstandingRequests;

		public void cancelRequests (List<BlockDescriptor> requests) { }

		public boolean getTheyHaveOutstandingRequests() {
			return this.theyHaveOutstandingRequests;
		}

		public void rejectPiece (int pieceNumber) { }

		public void sendHavePiece (int pieceNumber) { }

		public void sendKeepaliveOrClose() { }

		public void sendExtensionHandshake (Set<String> extensionsAdded, Set<String> extensionsRemoved, BDictionary extra) { }

		public void sendExtensionMessage (String identifier, ByteBuffer data) { }

		public void sendViewSignature (ViewSignature viewSignature) { }

		public boolean setWeAreChoking (boolean weAreChokingThem) {
			if (this.weAreChoking != weAreChokingThem) {
				this.weAreChoking = weAreChokingThem;
				return true;
			}
			return false;
		}

		public void setWeAreInterested (boolean weAreInterested) { }

		public PeerID getRemotePeerID() {
			return null;
		}

		public InetSocketAddress getRemoteSocketAddress() {
			return null;
		}

		public boolean isFastExtensionEnabled() {
			return false;
		}

		public boolean isExtensionProtocolEnabled() {
			return false;
		}

		public boolean getTheyAreChoking() {
			return true;
		}

		public boolean getTheyAreInterested() {
			return this.theyAreInterested;
		}

		public boolean getWeAreChoking() {
			return this.weAreChoking;
		}

		public boolean getWeAreInterested() {
			return false;
		}

		public long getBlockBytesReceived() {
			return 0;
		}

		public long getBlockBytesSent() {
			return 0;
		}

		public StatisticCounter getBlockBytesReceivedCounter() {
			return this.blockBytesReceivedCounter;
		}

		public StatisticCounter getBlockBytesSentCounter() {
			return this.blockBytesSentCounter;
		}

		public long getProtocolBytesReceived() {
			return 0;
		}

		public long getProtocolBytesSent() {
			return 0;
		}

		public int getProtocolBytesReceivedPerSecond() {
			return 0;
		}

		public int getProtocolBytesSentPerSecond() {
			return 0;
		}

		public BitField getRemoteBitField() {
			return null;
		}

		public long getRemoteViewLength() {
			return 0;
		}

		public void close() {
		}

		/**
		 * @param weAreChoking If true, we are choking the remote peer
		 * @param theyAreInterested If true, the remote peer are interested in us
		 * @param twentySecondReceivedBlocks The number of s received in the last 20 seconds
		 * @param twentySecondSentBlocks The number of s sent in the last 20 seconds
		 * @param theyHaveOutstandingRequests If true, the remote peer has outstanding requests to us
		 */
		public ChokingMockManageablePeer (boolean weAreChoking,
				boolean theyAreInterested,
				int twentySecondReceivedBlocks, int twentySecondSentBlocks,
				boolean theyHaveOutstandingRequests)
		{
			this.blockBytesReceivedCounter = new MockStatisticCounter (twentySecondReceivedBlocks);
			this.blockBytesSentCounter = new MockStatisticCounter (twentySecondSentBlocks);
			this.weAreChoking = weAreChoking;
			this.theyAreInterested = theyAreInterested;
			this.theyHaveOutstandingRequests = theyHaveOutstandingRequests;
		}

	}


	// Seeding mode

	/**
	 * Mode : Seeding
	 * Input: 0 peers
	 * Expected output : Successful completion
	 */
	@Test
	public void testSeeding0Peers() {

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.chokePeers (true);

	}


	/**
	 * Mode : Seeding
	 * Input: 1 uninterested, choked peer
	 * Expected output : No change
	 */
	@Test
	public void testSeeding1UninterestedChokedPeer() {

		ChokingMockManageablePeer peer = new ChokingMockManageablePeer (true, false, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer);
		chokingAlgorithm.chokePeers (true);

		assertEquals (true, peer.weAreChoking);

	}


	/**
	 * Mode : Seeding
	 * Input: 1 interested, choked peer
	 * Expected output : Peer is unchoked
	 */
	@Test
	public void testSeeding1InterestedChokedPeer() {

		ChokingMockManageablePeer peer = new ChokingMockManageablePeer (true, true, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer);
		chokingAlgorithm.chokePeers (true);

		assertEquals (false, peer.weAreChoking);

	}


	/**
	 * Mode : Seeding
	 * Input: 1 uninterested, unchoked peer
	 * Expected output : Peer is choked
	 */
	@Test
	public void testSeeding1UninterestedUnchokedPeer() {

		ChokingMockManageablePeer peer = new ChokingMockManageablePeer (false, false, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer);
		chokingAlgorithm.chokePeers (true);

		assertEquals (true, peer.weAreChoking);

	}


	/**
	 * Mode : Seeding
	 * Input: 1 interested, unchoked peer
	 * Expected output : No change
	 */
	@Test
	public void testSeeding1InterestedUnchokedPeer() {

		ChokingMockManageablePeer peer = new ChokingMockManageablePeer (false, true, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer);
		chokingAlgorithm.chokePeers (true);

		assertEquals (false, peer.weAreChoking);

	}


	/**
	 * Mode : Seeding
	 * Input: 4 eligible peers (unchoked less than 20 seconds)
	 * Expected output : No change
	 */
	@Test
	public void testSeeding4EligiblePeers() {

		ChokingMockManageablePeer peer1 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer2 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer3 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer4 = new ChokingMockManageablePeer (false, true, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer1);
		chokingAlgorithm.peerRegistered (peer2);
		chokingAlgorithm.peerRegistered (peer3);
		chokingAlgorithm.peerRegistered (peer4);
		chokingAlgorithm.chokePeers (true);

		assertEquals (false, peer1.weAreChoking);
		assertEquals (false, peer2.weAreChoking);
		assertEquals (false, peer3.weAreChoking);
		assertEquals (false, peer4.weAreChoking);

	}


	/**
	 * Mode : Seeding
	 * Input: 4 ineligible peers (unchoked more than 20 seconds, no outstanding requests)
	 * Expected output : No change
	 */
	@Test
	public void testSeeding4IneligiblePeers() {

		ChokingMockManageablePeer peer1 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer2 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer3 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer4 = new ChokingMockManageablePeer (false, true, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer1);
		chokingAlgorithm.peerRegistered (peer2);
		chokingAlgorithm.peerRegistered (peer3);
		chokingAlgorithm.peerRegistered (peer4);

		chokingAlgorithm.chokePeers (true);

		assertEquals (false, peer1.weAreChoking);
		assertEquals (false, peer2.weAreChoking);
		assertEquals (false, peer3.weAreChoking);
		assertEquals (false, peer4.weAreChoking);

	}


	/**
	 * Mode : Seeding
	 * Input: 4 choked, interested peers
	 * Expected output : All 4 peers unchoked
	 */
	@Test
	public void testSeeding4ChokedInterestedPeers() {

		ChokingMockManageablePeer peer1 = new ChokingMockManageablePeer (true, true, 0, 0, false);
		ChokingMockManageablePeer peer2 = new ChokingMockManageablePeer (true, true, 0, 0, false);
		ChokingMockManageablePeer peer3 = new ChokingMockManageablePeer (true, true, 0, 0, false);
		ChokingMockManageablePeer peer4 = new ChokingMockManageablePeer (true, true, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer1);
		chokingAlgorithm.peerRegistered (peer2);
		chokingAlgorithm.peerRegistered (peer3);
		chokingAlgorithm.peerRegistered (peer4);
		chokingAlgorithm.chokePeers (true);

		assertEquals (false, peer1.weAreChoking);
		assertEquals (false, peer2.weAreChoking);
		assertEquals (false, peer3.weAreChoking);
		assertEquals (false, peer4.weAreChoking);

	}


	/**
	 * Mode : Seeding
	 * Input: 4 choked, uninterested peers
	 * Expected output : No change
	 */
	@Test
	public void testSeeding4ChokedUninterestedPeers() {

		ChokingMockManageablePeer peer1 = new ChokingMockManageablePeer (true, false, 0, 0, false);
		ChokingMockManageablePeer peer2 = new ChokingMockManageablePeer (true, false, 0, 0, false);
		ChokingMockManageablePeer peer3 = new ChokingMockManageablePeer (true, false, 0, 0, false);
		ChokingMockManageablePeer peer4 = new ChokingMockManageablePeer (true, false, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer1);
		chokingAlgorithm.peerRegistered (peer2);
		chokingAlgorithm.peerRegistered (peer3);
		chokingAlgorithm.peerRegistered (peer4);
		chokingAlgorithm.chokePeers (true);

		assertEquals (true, peer1.weAreChoking);
		assertEquals (true, peer2.weAreChoking);
		assertEquals (true, peer3.weAreChoking);
		assertEquals (true, peer4.weAreChoking);

	}



	/**
	 * Mode : Seeding
	 * Input: 4 eligible peers (unchoked less than 20 seconds), one choked and
	 *        interested peer
	 * Expected output : One eligible peer is choked, choked and interested peer
	 *                   is unchoked
	 */
	@Test
	public void testSeeding4EligiblePeers1ChokedInterestedPeer() {

		ChokingMockManageablePeer peer1 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer2 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer3 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer4 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer5 = new ChokingMockManageablePeer (true, true, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer1);
		chokingAlgorithm.peerRegistered (peer2);
		chokingAlgorithm.peerRegistered (peer3);
		chokingAlgorithm.peerRegistered (peer4);
		chokingAlgorithm.peerRegistered (peer5);
		chokingAlgorithm.chokePeers (true);

		int unchokedPeers = 0;
		for (ChokingMockManageablePeer peer : new ChokingMockManageablePeer[] { peer1, peer2, peer3, peer4, peer5}) {
			unchokedPeers += peer.weAreChoking ? 0 : 1;
		}
		assertEquals (4, unchokedPeers);
		assertEquals (false, peer5.weAreChoking);

	}


	/**
	 * Mode : Seeding
	 * Input: 3 eligible peers (unchoked less than 20 seconds), one ineligible
	 *        peer (unchoked more than 20 seconds, no outstanding requests),
	 *        one choked and interested peer
	 * Expected output : Ineligible peer is choked, choked and interested peer
	 *                   is unchoked
	 */
	@Test
	public void testSeeding3EligiblePeers1IneligiblePeer1ChokedInterestedPeer() {

		ChokingMockManageablePeer peer1 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer2 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer3 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer4 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer5 = new ChokingMockManageablePeer (true, true, 0, 0, false);

		DefaultChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer1);
		chokingAlgorithm.peerRegistered (peer2);
		chokingAlgorithm.peerRegistered (peer3);
		chokingAlgorithm.peerRegistered (peer4);
		chokingAlgorithm.peerRegistered (peer5);
		chokingAlgorithm.setPeerLastChokeTime (peer4, System.currentTimeMillis() - 30000);
		chokingAlgorithm.chokePeers (true);

		assertEquals (false, peer1.weAreChoking);
		assertEquals (false, peer2.weAreChoking);
		assertEquals (false, peer3.weAreChoking);
		assertEquals (true, peer4.weAreChoking);
		assertEquals (false, peer5.weAreChoking);

	}


	/**
	 * Mode : Seeding
	 * Input: 4 ineligible peers (unchoked more than 20 seconds, no outstanding
	 *        requests), one choked and interested peer
	 * Expected output : One ineligible peer is choked, choked and interested 
	 *                   peer is unchoked
	 */
	@Test
	public void testSeeding4IneligiblePeers1ChokedInterestedPeer() {

		ChokingMockManageablePeer peer1 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer2 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer3 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer4 = new ChokingMockManageablePeer (false, true, 0, 0, false);
		ChokingMockManageablePeer peer5 = new ChokingMockManageablePeer (true, true, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer1);
		chokingAlgorithm.peerRegistered (peer2);
		chokingAlgorithm.peerRegistered (peer3);
		chokingAlgorithm.peerRegistered (peer4);
		chokingAlgorithm.peerRegistered (peer5);
		chokingAlgorithm.chokePeers (true);

		int unchokedPeers = 0;
		for (ChokingMockManageablePeer peer : new ChokingMockManageablePeer[] { peer1, peer2, peer3, peer4, peer5}) {
			unchokedPeers += peer.weAreChoking ? 0 : 1;
		}
		assertEquals (4, unchokedPeers);
		assertEquals (false, peer5.weAreChoking);

	}


	// Downloading mode

	/**
	 * Mode : Downloading
	 * Input: 0 peers
	 * Expected output : Successful completion
	 */
	@Test
	public void testDownloading0Peers() {

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.chokePeers (false);

	}


	/**
	 * Mode : Downloading
	 * Input: 1 uninterested, choked peer
	 * Expected output : Peer is unchoked
	 *   The uninterested peer will be unchoked optimistically while trying to
	 *   find an interested peer
	 */
	@Test
	public void testDownloading1UninterestedChokedPeer() {

		ChokingMockManageablePeer peer = new ChokingMockManageablePeer (true, false, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer);
		chokingAlgorithm.chokePeers (false);

		assertEquals (false, peer.weAreChoking);

	}


	/**
	 * Mode : Downloading
	 * Input: 1 interested, choked peer
	 * Expected output : Peer is unchoked
	 */
	@Test
	public void testDownloading1InterestedChokedPeer() {

		ChokingMockManageablePeer peer = new ChokingMockManageablePeer (true, true, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer);
		chokingAlgorithm.chokePeers (false);

		assertEquals (false, peer.weAreChoking);

	}


	/**
	 * Mode : Downloading
	 * Input: 1 uninterested, unchoked peer
	 * Expected output : No change
	 *   The uninterested peer will be unchoked optimistically while trying to
	 *   find an interested peer
	 */
	@Test
	public void testDownloading1UninterestedUnchokedPeer() {

		ChokingMockManageablePeer peer = new ChokingMockManageablePeer (false, false, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer);
		chokingAlgorithm.chokePeers (false);

		assertEquals (false, peer.weAreChoking);

	}


	/**
	 * Mode : Downloading
	 * Input: 1 interested, unchoked peer
	 * Expected output : No change
	 */
	@Test
	public void testDownloading1InterestedUnchokedPeer() {

		ChokingMockManageablePeer peer = new ChokingMockManageablePeer (false, true, 0, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer);
		chokingAlgorithm.chokePeers (false);

		assertEquals (false, peer.weAreChoking);

	}


	/**
	 * Mode : Downloading
	 * Input: 5 choked, eligible peers (sent us a  in the last 20 seconds)
	 * Expected output : 4 peers are unchoked
	 */
	@Test
	public void testDownloading5ChokedEligiblePeers() {

		ChokingMockManageablePeer peer1 = new ChokingMockManageablePeer (true, true, 1, 0, false);
		ChokingMockManageablePeer peer2 = new ChokingMockManageablePeer (true, true, 1, 0, false);
		ChokingMockManageablePeer peer3 = new ChokingMockManageablePeer (true, true, 1, 0, false);
		ChokingMockManageablePeer peer4 = new ChokingMockManageablePeer (true, true, 1, 0, false);
		ChokingMockManageablePeer peer5 = new ChokingMockManageablePeer (true, true, 1, 0, false);

		ChokingManager chokingAlgorithm = new DefaultChokingManager (0);
		chokingAlgorithm.peerRegistered (peer1);
		chokingAlgorithm.peerRegistered (peer2);
		chokingAlgorithm.peerRegistered (peer3);
		chokingAlgorithm.peerRegistered (peer4);
		chokingAlgorithm.peerRegistered (peer5);
		chokingAlgorithm.chokePeers (false);

		int unchokedPeers = 0;
		for (ChokingMockManageablePeer peer : new ChokingMockManageablePeer[] { peer1, peer2, peer3, peer4, peer5}) {
			unchokedPeers += peer.weAreChoking ? 0 : 1;
		}
		assertEquals (4, unchokedPeers);

	}


}
