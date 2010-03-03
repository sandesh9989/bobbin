/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.peer.chokingmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.PeerStatistics;
import org.itadaki.bobbin.util.counter.Period;



/**
 * The choking algorithm selects peers to be choked and unchoked, and initiates
 * their choking and unchoking.<br>
 * <br>
 * If {@link #chokePeers} is invoked with {@code seeding = false}, an algorithm
 * suitable for the management of the torrent while downloading (i.e. while
 * there are still pieces that we are interested in and have not received) is
 * applied.<br>
 * If {@link #chokePeers} is invoked with {@code seeding = true}, an algorithm
 * suitable for the management of the torrent while seeding (i.e. when there
 * are no more pieces that we are interested in and have not received) is
 * applied.<br>
 */
public class DefaultChokingManager implements ChokingManager {

	/**
	 * A 20 second period used for peer block statistics
	 */
	private static Period TWENTY_SECOND_PERIOD = new Period (500, 40);

	/**
	 * The set of peers to choke
	 */
	private Map<ManageablePeer,PeerState> peerStates = new HashMap<ManageablePeer,PeerState>();

	/**
	 * A comparator to prioritise peers to unchoke when seeding
	 */
	private static Comparator<PeerState> seedingComparator = new Comparator<PeerState>() {

		public int compare (PeerState peerState1, PeerState peerState2) {

			// Unchoked time - most recently unchoked peers first
			// Although the time is in milliseconds, each peer is given the same unchoke time within
			// the same round
			long t1 = peerState1.lastChokeTime;
			long t2 = peerState2.lastChokeTime;
			if (t1 != t2) {
				return (t1 < t2) ? -1 : 1;
			}

			// Send rate - highest receiving peers first
			int r1 = peerState1.twentySecondSentBlocks;
			int r2 = peerState2.twentySecondSentBlocks;
			if (r1 != r2) {
				return (r1 < r2) ? -1 : 1;
			}

			// Peers are equal
			return 0;

		}

	};

	/**
	 * A comparator to prioritise peers to unchoke when downloading
	 */
	private static Comparator<PeerState> downloadingComparator = new Comparator<PeerState>() {

		public int compare (PeerState peerState1, PeerState peerState2) {

			// Receive rate - highest sending peers first
			int r1 = peerState1.twentySecondReceivedBlocks;
			int r2 = peerState2.twentySecondReceivedBlocks;
			if (r1 != r2) {
				return (r1 < r2) ? -1 : 1;
			}

			// Peers are equal
			return 0;

		}

	};

	/**
	 * The number of the present choking algorithm round. There are three rounds
	 * (0,1,2), and the seeding and downloading algorithms make different
	 * choices depending on the current round number.
	 */
	private int roundNumber = 0;

	/**
	 * A random number generator that will be kept for reuse
	 */
	private Random random = new Random();

	/**
	 * A peer chosen to be optimistically unchoked
	 */
	private ManageablePeer optimisticUnchokePeer;

	/**
	 * The choking algorithm state for a given peer
	 */
	private static class PeerState {

		/**
		 * The peer
		 */
		public ManageablePeer peer;

		/**
		 * The last time the peer's choke state changed
		 */
		public long lastChokeTime = System.currentTimeMillis();

		/**
		 * The number of blocks received from the peer in the last 20 seconds. This value is cached
		 * here in order to guarantee a stable sort given that the figures from StatisticCounter may
		 * change at any time
		 */
		public int twentySecondReceivedBlocks = 0;

		/**
		 * The number of blocks sent to the peer in the last 20 seconds. This value is cached here
		 * in order to guarantee a stable sort given that the figures from StatisticCounter may
		 * change at any time
		 */
		public int twentySecondSentBlocks = 0;

		/**
		 * @param peer The peer
		 */
		public PeerState (ManageablePeer peer) {
			this.peer = peer;
		}

	}


	/* ChokingManager interface */

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerCoordinatorListener#peerRegistered(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public void peerRegistered (ManageablePeer peer) {

		if (this.peerStates.containsKey (peer)) {
			throw new IllegalArgumentException();
		}

		this.peerStates.put (peer, new PeerState (peer));
		peer.getStatistics().getCounter(PeerStatistics.Type.BLOCK_BYTES_RECEIVED_RAW).addCountedPeriod (TWENTY_SECOND_PERIOD);
		peer.getStatistics().getCounter(PeerStatistics.Type.BLOCK_BYTES_SENT).addCountedPeriod (TWENTY_SECOND_PERIOD);

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerCoordinatorListener#peerDeregistered(org.itadaki.bobbin.peer.ManageablePeer)
	 */
	public void peerDeregistered (ManageablePeer peer) {

		this.peerStates.remove (peer);
		if (this.optimisticUnchokePeer == peer) {
			this.optimisticUnchokePeer = null;
		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.PeerCoordinatorListener#peerCoordinatorCompleted()
	 */
	public void peerCoordinatorCompleted() {

		// Do nothing

	}


	/**
	 * Applies the seeding choke algorithm to the given peer set 
	 */
	private void seedingChoke() {

		List<PeerState> eligibleUnchokedPeers = new ArrayList<PeerState>();
		List<PeerState> ineligibleUnchokedPeers = new ArrayList<PeerState>();
		List<PeerState> interestedChokedPeers = new ArrayList<PeerState>();

		Set<PeerState> previouslyUnchokedPeers = new HashSet<PeerState>();
		List<PeerState> newUnchokedPeers = new ArrayList<PeerState>(4);

		long currentTime =  System.currentTimeMillis();

		// Classify and order the peers
		// - eligibleChokedPeers : Unchoked peers that have been choked for less than 20 seconds or
		// that have outstanding requests to us
		// - ineligibleUnchokedPeers : All unchoked, interested peers not in eligibleChokedPeers
		// - interestedChokedPeers : All choked, interested peers
		// - previouslyUnchokedPeers : All peers that were unchoked at the start of this new round
		// The sort criteria for the eligible peers are :
		//   - 1. More recently unchoked peers before peers that have been unchoked for longer
		//   - 2. (Within peers that were unchoked in the same round, ) peers that have received
		//        more piece blocks before peers that have received fewer
		for (PeerState peerState : this.peerStates.values()) {

			if (peerState.peer.getPeerState().getTheyAreInterested()) {
				if (peerState.peer.getPeerState().getWeAreChoking() == false) {
					if (
							((currentTime - peerState.lastChokeTime) < 20000)
							|| peerState.peer.getTheyHaveOutstandingRequests()
					   )
					{
						eligibleUnchokedPeers.add (peerState);
					} else {
						ineligibleUnchokedPeers.add (peerState);
					}
				} else {
					interestedChokedPeers.add (peerState);
				}
			}

			if (peerState.peer.getPeerState().getWeAreChoking() == false) {
				previouslyUnchokedPeers.add (peerState);
			}

		}
		for (PeerState peerState : eligibleUnchokedPeers) {
			peerState.twentySecondSentBlocks = (int) peerState.peer.getStatistics().getCounter(PeerStatistics.Type.BLOCK_BYTES_SENT).getPeriodTotal (TWENTY_SECOND_PERIOD);
		}
		Collections.sort (eligibleUnchokedPeers, seedingComparator);


		// Select the peers to unchoke

		// In rounds 0 and 1, try to choose one peer to unchoke at random from the interested, choked peer set
		int wantedEligiblePeers = (this.roundNumber == 2) ? 4 : 3;
		int wantedRandomPeers = (this.roundNumber == 2) ? 0 : 1;
		int availableEligiblePeers = eligibleUnchokedPeers.size();
		int availableRandomPeers = interestedChokedPeers.size();

		// Choose the actual numbers of eligible and random peers to unchoke
		// If there are too few random peers, try to top up with eligible peers
		// If there are too few eligible peers, try to top up with random peers
		// Finally, if there are no eligible or random peers left to use, add any remaining ineligible peers back in
		int actualEligiblePeersToUnchoke = Math.min (availableEligiblePeers, wantedEligiblePeers + Math.max (0, wantedRandomPeers - availableRandomPeers));
		int actualRandomPeersToUnchoke = Math.min (4 - actualEligiblePeersToUnchoke, availableRandomPeers);
		int ineligiblePeersToUnchoke = Math.min (4 - actualEligiblePeersToUnchoke - actualRandomPeersToUnchoke, ineligibleUnchokedPeers.size());

		// Select peers to keep unchoked from the eligible unchoked peer set
		for (int i = 0; i < actualEligiblePeersToUnchoke; i++) {
			newUnchokedPeers.add (eligibleUnchokedPeers.get(i));
			previouslyUnchokedPeers.remove (eligibleUnchokedPeers.get(i));
		}

		// Fill any available slots with random interested, choked peers
		if (actualRandomPeersToUnchoke > 0) {
			int randomPeerIndex = this.random.nextInt (interestedChokedPeers.size());
			for (int i = 0; i < actualRandomPeersToUnchoke; i++) { 
				newUnchokedPeers.add (interestedChokedPeers.get ((randomPeerIndex + i) % interestedChokedPeers.size()));
			}
		}

		// If we still don't have enough peers, add peers from the ineligible unchoked peer list
		for (int i = 0; i < ineligiblePeersToUnchoke; i++) {
			newUnchokedPeers.add (ineligibleUnchokedPeers.get(i));
			previouslyUnchokedPeers.remove (ineligibleUnchokedPeers.get(i));
		}


		// Unchoke the selected peers
		for (PeerState peerState : newUnchokedPeers) {
			if (peerState.peer.setWeAreChoking (false)) {
				peerState.lastChokeTime = currentTime;
			}
		}

		// Choke any other peers that were unchoked previously
		for (PeerState peerState : previouslyUnchokedPeers) {
			if (peerState.peer.setWeAreChoking (true)) {
				peerState.lastChokeTime = currentTime;
			}
		}

	}


	/**
	 * Applies the downloading choke algorithm to the given peer set 
	 */
	private void downloadingChoke () {

		List<PeerState> eligiblePeers = new ArrayList<PeerState>();

		List<PeerState> chokedInterestedPeers = new ArrayList<PeerState> (this.peerStates.size());
		Set<PeerState> previouslyUnchokedPeers = new HashSet<PeerState>();
		Set<PeerState> newUnchokedPeers = new HashSet<PeerState>();

		long currentTime =  System.currentTimeMillis();

		// Classify and order the peers
		// - eligiblePeers : Interested peers that have sent a complete block in the last 20 seconds
		// - chokedInterestedPeers : All peers that are choked and interested
		// - previouslyUnchokedPeers : All peers that were unchoked at the start of this new round
		// The sort criteria for the eligible peers are :
		//   - 1. Peers that have sent more piece blocks before peers that have sent fewer
		for (PeerState peerState : this.peerStates.values()) {
			peerState.twentySecondReceivedBlocks =
				(int) peerState.peer.getStatistics().getCounter(PeerStatistics.Type.BLOCK_BYTES_RECEIVED_RAW).getPeriodTotal (TWENTY_SECOND_PERIOD);
		}
		for (PeerState peerState : this.peerStates.values()) {
			if (peerState.peer.getPeerState().getTheyAreInterested()) {
				if (peerState.twentySecondReceivedBlocks > 0) {
					eligiblePeers.add (peerState);
				}
				if (peerState.peer.getPeerState().getWeAreChoking()) {
					chokedInterestedPeers.add (peerState);
				}
			}
			if (!peerState.peer.getPeerState().getWeAreChoking()) {
				previouslyUnchokedPeers.add (peerState);
			}
		}
		Collections.sort (eligiblePeers, downloadingComparator);


		// Select the peers to unchoke

		// Select peers to unchoke or keep unchoked from the eligible peer set
		int peersToUnchoke = Math.min (3, eligiblePeers.size());
		for (int i = 0; i < peersToUnchoke; i++) {
			PeerState peer = eligiblePeers.get(i);
			newUnchokedPeers.add (peer);
			previouslyUnchokedPeers.remove (peer);
		}

		// Retrieve the existing optimistically unchoked peer if any, or try to select a new one in the third round
		PeerState optimisticUnchokePeerState = null;
		if (this.roundNumber == 2) {
			// Select a random choked, interested peer
			if (chokedInterestedPeers.size() > 0) {
				int optimisticPeerIndex = this.random.nextInt (chokedInterestedPeers.size());
				optimisticUnchokePeerState = chokedInterestedPeers.get (optimisticPeerIndex);
				this.optimisticUnchokePeer = optimisticUnchokePeerState.peer;
			}
		} else {
			if (this.optimisticUnchokePeer != null) {
				// May be null if deregistered
				optimisticUnchokePeerState = this.peerStates.get (this.optimisticUnchokePeer);
			}
		}

		// Add the optimistic unchoke peer if it is not already present
		if (optimisticUnchokePeerState != null) {
			newUnchokedPeers.add (optimisticUnchokePeerState);
			previouslyUnchokedPeers.remove (optimisticUnchokePeerState);
		}

		// If we do not have four unchoked peers, select new potential optimistically unchoked
		// peers for unchoking at random until enough interested peers are unchoked
		if (newUnchokedPeers.size() < 4) {
			ArrayList<PeerState> randomPeers = new ArrayList<PeerState>(this.peerStates.values());
			Collections.shuffle (randomPeers, this.random);
			for (PeerState peerState : randomPeers) {
				if (!newUnchokedPeers.contains (peerState)) {
					newUnchokedPeers.add (peerState);
					previouslyUnchokedPeers.remove (peerState);
					if (peerState.peer.getPeerState().getTheyAreInterested()) {
						this.optimisticUnchokePeer = peerState.peer;
						if (newUnchokedPeers.size() == 4)
							break;
					}
				}
			}
		}


		// Unchoke the selected peers
		for (PeerState peerState : newUnchokedPeers) {
			if (peerState.peer.setWeAreChoking (false)) {
				peerState.lastChokeTime = currentTime;
			}
		}

		// Choke any other peers that were unchoked previously
		for (PeerState peerState : previouslyUnchokedPeers) {
			if (peerState.peer.setWeAreChoking (true)) {
				peerState.lastChokeTime = currentTime;
			}
		}

	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.peer.chokingmanager.ChokingManager#chokePeers(boolean)
	 */
	public void chokePeers (boolean seeding) {

		if (seeding) {

			seedingChoke();

		} else {

			downloadingChoke();

		}

		this.roundNumber = (this.roundNumber + 1) % 3;

	}


	/**
	 * Sets the recorded last choke time for a given peer.
	 * This is primarily useful in testing, and not used in normal operation.
	 *
	 * @param peer The peer to set the last choke time for
	 * @param lastChokeTime The last choke time to set
	 */
	public void setPeerLastChokeTime (ManageablePeer peer, long lastChokeTime) {

		this.peerStates.get (peer).lastChokeTime = lastChokeTime;

	}


	/**
	 * Special purpose constructor that sets the seed of the instance's internal
	 * random number generator. With the same seed and the same inputs to the
	 * chokePeers() method, the same sets of peers will be choked and unchoked.
	 * This is primarily useful in testing, and not used in normal operation.
	 * 
	 * @param seed The random seed to use
	 */
	public DefaultChokingManager (long seed) {

		this.random.setSeed (seed);

	}


	/**
	 * Default constructor 
	 */
	public DefaultChokingManager() {
		
	}


}
