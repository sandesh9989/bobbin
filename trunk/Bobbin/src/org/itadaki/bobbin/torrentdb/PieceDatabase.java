/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.torrentdb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDecoder;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BEncoder;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.bencode.InvalidEncodingException;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.CharsetUtil;
import org.itadaki.bobbin.util.DSAUtil;
import org.itadaki.bobbin.util.WorkQueue;
import org.itadaki.bobbin.util.elastictree.ElasticTree;
import org.itadaki.bobbin.util.elastictree.ElasticTreeView;
import org.itadaki.bobbin.util.elastictree.HashChain;
import org.itadaki.bobbin.util.statemachine.Ordinal;
import org.itadaki.bobbin.util.statemachine.StateMachine;
import org.itadaki.bobbin.util.statemachine.StateMachineUtil;
import org.itadaki.bobbin.util.statemachine.TargetedAction;
import org.itadaki.bobbin.util.statemachine.TransitionTable;


/**
 * Manages the relationship between an {@link Info} describing a torrent's data and a
 * {@link Storage} that contains the data
 */
public class PieceDatabase {

	/**
	 * The transition table for a PieceDatabase's state machine
	 */
	private static final TransitionTable<State,Input,Action> TRANSITION_TABLE;

	static {

		final TransitionTable<State,Input,Action> transitions = new TransitionTable<State,Input,Action> (State.values().length, Input.values().length);

		transitions.add (State.STOPPED,     Input.START,                  State.CHECKING,    Action.VERIFY);
		transitions.add (State.ERROR,       Input.START,                  State.CHECKING,    Action.VERIFY);
		transitions.add (State.CHECKING,    Input.STOP,                   State.STOPPING,    Action.CANCEL_VERIFY);
		transitions.add (State.AVAILABLE,   Input.STOP,                   State.STOPPED,     Action.STOPPED);
		transitions.add (State.STOPPED,     Input.VERIFICATION_COMPLETE,  null,              Action.INTERNAL_ERROR);
		transitions.add (State.ERROR,       Input.VERIFICATION_COMPLETE,  null,              Action.INTERNAL_ERROR);
		transitions.add (State.CHECKING,    Input.VERIFICATION_COMPLETE,  State.AVAILABLE,   Action.AVAILABLE);
		transitions.add (State.AVAILABLE,   Input.VERIFICATION_COMPLETE,  null,              Action.INTERNAL_ERROR);
		transitions.add (State.STOPPING,    Input.VERIFICATION_COMPLETE,  State.STOPPED,     Action.STOPPED);
		transitions.add (State.TERMINATING, Input.VERIFICATION_COMPLETE,  State.TERMINATED,  Action.TERMINATED);
		transitions.add (State.TERMINATED,  Input.VERIFICATION_COMPLETE,  null,              Action.INTERNAL_ERROR);
		transitions.add (State.STOPPED,     Input.VERIFICATION_CANCELLED, null,              Action.INTERNAL_ERROR);
		transitions.add (State.ERROR,       Input.VERIFICATION_CANCELLED, null,              Action.INTERNAL_ERROR);
		transitions.add (State.CHECKING,    Input.VERIFICATION_CANCELLED, null,              Action.INTERNAL_ERROR);
		transitions.add (State.AVAILABLE,   Input.VERIFICATION_CANCELLED, null,              Action.INTERNAL_ERROR);
		transitions.add (State.STOPPING,    Input.VERIFICATION_CANCELLED, State.STOPPED,     Action.STOPPED);
		transitions.add (State.TERMINATING, Input.VERIFICATION_CANCELLED, State.TERMINATED,  Action.TERMINATED);
		transitions.add (State.TERMINATED,  Input.VERIFICATION_CANCELLED, null,              Action.INTERNAL_ERROR);
		transitions.add (State.CHECKING,    Input.ERROR,                  State.ERROR,       Action.ERROR);
		transitions.add (State.AVAILABLE,   Input.ERROR,                  State.ERROR,       Action.ERROR);
		transitions.add (State.STOPPING,    Input.ERROR,                  State.ERROR,       Action.ERROR);
		transitions.add (State.STOPPED,     Input.ERROR,                  null,              Action.INTERNAL_ERROR);
		transitions.add (State.TERMINATING, Input.ERROR,                  State.TERMINATED,  Action.TERMINATED);
		transitions.add (State.TERMINATED,  Input.ERROR,                  null,              Action.INTERNAL_ERROR);
		transitions.add (State.CHECKING,    Input.TERMINATE,              State.TERMINATING, Action.CANCEL_VERIFY);
		transitions.add (State.AVAILABLE,   Input.TERMINATE,              State.TERMINATED,  Action.TERMINATED);
		transitions.add (State.STOPPING,    Input.TERMINATE,              State.TERMINATING, null);
		transitions.add (State.STOPPED,     Input.TERMINATE,              State.TERMINATED,  Action.TERMINATED);
		transitions.add (State.ERROR,       Input.TERMINATE,              State.TERMINATED,  Action.TERMINATED);

		TRANSITION_TABLE = transitions;

	}

	/**
	 * The set of states that can eventually result from Input.START
	 */
	private static final Set<State> START_FINAL_STATES = Collections.unmodifiableSet (new HashSet<State> (Arrays.asList (new State[] {
			State.AVAILABLE,
			State.STOPPED,
			State.ERROR,
			State.TERMINATED
	})));

	/**
	 * The set of states that can eventually result from Input.STOP
	 */
	private static final Set<State> STOP_FINAL_STATES = Collections.unmodifiableSet (new HashSet<State> (Arrays.asList (new State[] {
			State.STOPPED,
			State.ERROR,
			State.TERMINATED
	})));

	/**
	 * The set of states that can eventually result from Input.TERMINATE
	 */
	private static final Set<State> TERMINATE_FINAL_STATES = Collections.unmodifiableSet (new HashSet<State> (Arrays.asList (new State[] {
			State.TERMINATED
	})));

	/**
	 * The state machine used to control the database
	 */
	private final StateMachine<PieceDatabase,State,Input,Action> stateMachine =
		new StateMachine<PieceDatabase,State,Input,Action> (this, TRANSITION_TABLE, State.STOPPED);

	/**
	 * A work queue used to make inputs to the database's state machine asynchronously
	 */
	private final WorkQueue workQueue;

	/**
	 * An SHA1 message digester
	 */
	private final MessageDigest digest;

	/**
	 * The listeners to inform of state changes to the PieceDatabase
	 */
	private final Set<PieceDatabaseListener> listeners = new HashSet<PieceDatabaseListener>();

	/**
	 * The {@code Info} describing the database's content
	 */
	private final Info info;

	/**
	 * The DSA public key used to sign the {@code Info}, or {@code null}
	 */
	private final PublicKey publicKey;

	/**
	 * The {@code Storage} used to access files on disk
	 */
	private final Storage storage;

	/**
	 * The {@code Metadata} used to store persistent metadata about the database
	 */
	private final Metadata metadata;

	/**
	 * The set of view signatures indexed by view length
	 */
	private final Map<Long,ViewSignature> viewSignatures = new HashMap<Long,ViewSignature>();

	/**
	 * The set of pieces that are present
	 */
	private BitField presentPieces;

	/**
	 * The set of pieces that have been verified as being either present or absent
	 */
	private BitField verifiedPieces;

	/**
	 * The concatenated piece hashes (cached from Info)
	 */
	private byte[] pieceHashes;

	/**
	 * The expandable Merkle hash tree
	 */
	private ElasticTree elasticTree;

	/**
	 * A thread that verifies the database asynchronously
	 */
	private Verifier verifier;

	/**
	 * The number of verified pieces (cached separately so it can be safely accessed unlocked)
	 */
	private volatile int verifiedPieceCount;


	/**
	 * The state of a PieceDatabase
	 */
	public enum State implements Ordinal {
		/** The PieceDatabase is checking its database */
		CHECKING,
		/** The PieceDatabase is available */
		AVAILABLE,
		/** The PieceDatabase is stopping */
		STOPPING,
		/** The PieceDatabase is stopped */
		STOPPED,
		/** The PieceDatabase is stopped due to an I/O error */
		ERROR,
		/** The PieceDatabase is terminating */
		TERMINATING,
		/** The PieceDatabase has terminated */
		TERMINATED
	}


	/**
	 * The possible inputs to the PieceDatabase's state machine
	 */
	private enum Input implements Ordinal {
		/** Starts the database. If the database is starting or started, this has no effect */
		START,
		/** Stops the database. If the database is stopping or stopped, this has no effect */
		STOP,
		/** Terminates the database. If the database is already terminating or has terminated, this has no effect */
		TERMINATE,
		/** Signals that verification of the file data has completed */
		VERIFICATION_COMPLETE,
		/** Indicates that verification of the file data hase been cancelled */
		VERIFICATION_CANCELLED,
		/** Signals that the database encountered an I/O error */
		ERROR
	}


	/**
	 * The possible actions of the PieceDatabase's state machine
	 */
	private enum Action implements TargetedAction<PieceDatabase> {
		/** Initiates verification of the database */
		VERIFY,
		/** Attempts to cancel verification (completion or an error may still result) */
		CANCEL_VERIFY,
		/** Signals listeners that the database is available */
		AVAILABLE,
		/** Signals listeners that the database has stopped */
		STOPPED,
		/** Signals listeners that the database has stopped due to an error */
		ERROR,
		/** Saves metadata and signals listeners that the database has terminated */
		TERMINATED,
		/** Signals listeners that the database has terminated without saving metadata */
		TERMINATED_ERROR,
		/** Indicates that an internal state error has occurred */
		INTERNAL_ERROR;

		/* (non-Javadoc)
		 * @see org.itadaki.bobbin.util.statemachine.TargetedAction#execute(java.lang.Object)
		 */
		public void execute (PieceDatabase target) {

			switch (this) {
				case VERIFY:            target.actionVerify();           break;
				case CANCEL_VERIFY:     target.actionCancelVerify();     break;
				case AVAILABLE:         target.actionAvailable();        break;
				case STOPPED:           target.actionStopped();          break;
				case ERROR:             target.actionError();            break;
				case TERMINATED:        target.actionTerminated();       break;
				case TERMINATED_ERROR:  target.actionTerminatedError();  break;
				case INTERNAL_ERROR:    target.actionInternalError();    break;
			}

		}
	}


	/**
	 * Initiates verification of the database
	 */
	private void actionVerify() {

		this.verifier = new Verifier();
		this.verifier.setName ("PieceDatabase Verifier - " + CharsetUtil.hexencode (this.info.getHash().getBytes()));
		this.verifier.setDaemon (true);
		this.verifier.start();

	}


	/**
	 * Attempts to cancel verification of the database (completion or an error may still result)
	 */
	private void actionCancelVerify() {

		this.verifier.interrupt();

	}


	/**
	 * Signals listeners that the database is available
	 */
	private void actionAvailable() {

		synchronized (this.listeners) {
			for (PieceDatabaseListener listener : this.listeners) {
				listener.pieceDatabaseAvailable();
			}
		}

	}


	/**
	 * Signals listeners that the database has stopped
	 */
	private void actionStopped() {

		synchronized (this.listeners) {
			for (PieceDatabaseListener listener : this.listeners) {
				listener.pieceDatabaseStopped();
			}
		}

	}


	/**
	 * Signals listeners that the database has stopped due to an error
	 */
	private void actionError() {

		this.verifiedPieces.clear();
		this.verifiedPieceCount = 0;

		synchronized (this.listeners) {
			for (PieceDatabaseListener listener : this.listeners) {
				listener.pieceDatabaseError();
			}
		}

	}


	/**
	 * Saves metadata and signals listeners that the database has terminated
	 */
	private void actionTerminated() {

		ByteBuffer storageCookie = null;
		try {
			storageCookie = this.storage.close();
		} catch (IOException e) {
			// Do nothing
		}
		this.workQueue.shutdown();
		synchronized (this.listeners) {
			for (PieceDatabaseListener listener : this.listeners) {
				listener.pieceDatabaseTerminated();
			}
		}

		// Save state if we have a {@code Metadata} instance, the database is fully verified and the
		// {@code Storage} closed normally
		if (this.metadata != null) {

			if ((storageCookie != null) && (this.verifiedPieceCount == this.storage.getDescriptor().getNumberOfPieces())) {
				try {
					if (this.elasticTree != null) {
						ByteBuffer elasticImmutableHashes = this.elasticTree.getImmutableHashes();
						this.metadata.put ("elasticImmutable", elasticImmutableHashes.array());
						if (this.info.getPieceStyle() != PieceStyle.ELASTIC) {
							ByteBuffer mutableHashes = this.elasticTree.getCeilingView(0).getMutableHashes();
							this.metadata.put ("elasticView", (mutableHashes == null) ? new byte[0] :  mutableHashes.array());
						} else {
							// elasticViews
							Set<ElasticTreeView> views = this.elasticTree.getAllViews();
							BDictionary viewsDictionary = new BDictionary();
							for (ElasticTreeView view : views) {
								ByteBuffer mutableHashes = view.getMutableHashes();
								viewsDictionary.put ("" + view.getViewLength(), mutableHashes == null ? new byte[0] : mutableHashes.array());
							}
							this.metadata.put ("elasticViews", BEncoder.encode (viewsDictionary));
							// elasticViewSignatures
							BDictionary viewSignaturesDictionary = new BDictionary();
							for (ViewSignature viewSignature : this.viewSignatures.values()) {
								byte[] viewRootHashBytes = new byte[20];
								byte[] signatureBytes = new byte[40];
								viewSignature.getViewRootHash().get (viewRootHashBytes);
								viewSignature.getSignature().get (signatureBytes);
								viewSignaturesDictionary.put (
										"" + viewSignature.getViewLength(),
										new BList (new BBinary (viewRootHashBytes), new BBinary (signatureBytes))
								);
							}

							this.metadata.put ("elasticViewSignatures", BEncoder.encode (viewSignaturesDictionary));
						}
					}
					BDictionary resumeDictionary = new BDictionary();
					resumeDictionary.put ("storageCookie", storageCookie.array());
					byte[] presentPiecesBytes = new byte [this.presentPieces.byteLength()];
					this.presentPieces.copyTo (presentPiecesBytes, 0);
					resumeDictionary.put ("presentPieces", presentPiecesBytes);
					this.metadata.put ("resume", BEncoder.encode (resumeDictionary));
				} catch (IOException e) {
					// Nothing to do. If we failed to fully write the resume data at this stage, it
					// should remain invalid
				}

			}

			this.metadata.close();

		}

	}


	/**
	 * Signals listeners that the database has terminated without saving metadata
	 */
	private void actionTerminatedError() {

		this.workQueue.shutdown();
		synchronized (this.listeners) {
			for (PieceDatabaseListener listener : this.listeners) {
				listener.pieceDatabaseTerminated();
			}
		}

	}


	/**
	 * Indicates that an internal state error has occurred
	 */
	private void actionInternalError() {

		throw new IllegalStateException();

	}


	/**
	 * A Thread that asynchronously verifies the content of the database
	 */
	private class Verifier extends Thread {

		/**
		 * The digester used to verify piece hashes
		 */
		MessageDigest digest = null;

		/* Runnable interface */

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {

			// Create message digester
			try {
				this.digest = MessageDigest.getInstance ("SHA");
			} catch (NoSuchAlgorithmException e) {
				// Shouldn't happen
				throw new InternalError (e.getMessage());
			}

			try {
				if (verifyDataInterruptibly()) {
					PieceDatabase.this.stateMachine.input (Input.VERIFICATION_COMPLETE);
				} else {
					PieceDatabase.this.stateMachine.input (Input.VERIFICATION_CANCELLED);
				}
			} catch (IOException e) {
				PieceDatabase.this.stateMachine.input (Input.ERROR);
			}

		}

		/**
		 * Verifies the pieces of the database, setting or clearing bits in the presentPieces set
		 * for each piece, and setting bits in the verifiedPieces set as each piece is checked
		 *
		 * @return If {@code true}, the database was verified completely. If {@code false}, the
		 *         thread was interrupted.
		 * @throws IOException if an error occurs reading data from disk
		 */
		private boolean verifyDataInterruptibly() throws IOException {

			int numPieces = PieceDatabase.this.storage.getDescriptor().getNumberOfPieces();
			byte[] storedPieceHash = new byte[20];

			BitField fileBackedPieces = PieceDatabase.this.storage.getStorageBackedPieces();

			synchronized (PieceDatabase.this.presentPieces) {
				for (int i = 0; i < numPieces; i++) {
					if (!PieceDatabase.this.verifiedPieces.get (i) && !fileBackedPieces.get (i)) {
						PieceDatabase.this.presentPieces.clear (i);
						PieceDatabase.this.verifiedPieces.set (i);
					}
				}
			}

			PieceDatabase.this.verifiedPieceCount = PieceDatabase.this.verifiedPieces.cardinality();

			// If we have no verified pieces, an empty hash tree and all data is file backed, build
			// a tree to see if all pieces are present
			if (
					   (PieceDatabase.this.pieceHashes == null)
					&& (PieceDatabase.this.elasticTree.getAllViews().size() == 1)
					&& (PieceDatabase.this.verifiedPieces.cardinality() == 0)
					&& (fileBackedPieces.cardinality() == numPieces)
			   )
			{
				byte[] leafHashes = new byte [20 * numPieces];

				for (int i = 0, offset = 0; i < numPieces; i++, offset += 20) {
					ByteBuffer storedPiece = PieceDatabase.this.storage.read (i);

					this.digest.reset();
					this.digest.update (storedPiece);
					try {
						this.digest.digest (leafHashes, offset, 20);
					} catch (DigestException e) {
						// Shouldn't happen
						throw new InternalError (e.getMessage());
					}

					if (interrupted()) {
						return false;
					}

				}

				ElasticTree verificationTree = ElasticTree.buildFromLeaves (
						PieceDatabase.this.storage.getDescriptor().getPieceSize(),
						PieceDatabase.this.storage.getDescriptor().getLength(),
						leafHashes
				);

				ElasticTreeView verificationView = verificationTree.getView (PieceDatabase.this.storage.getDescriptor().getLength());
				ElasticTreeView databaseView = PieceDatabase.this.elasticTree.getView (PieceDatabase.this.storage.getDescriptor().getLength());
				if (ByteBuffer.wrap(verificationView.getRootHash()).equals (ByteBuffer.wrap (databaseView.getRootHash()))) {
					// TODO Inefficient
					for (int i = 0; i < numPieces; i++) {
						if (databaseView.verifyHashChain (i, ByteBuffer.wrap (verificationView.getHashChain (i)))) {
							synchronized (PieceDatabase.this.presentPieces) {
								PieceDatabase.this.presentPieces.set (i);
							}
						}
						PieceDatabase.this.verifiedPieces.set (i);
						PieceDatabase.this.verifiedPieceCount++;
					}
					return true;
				}
			}

			// Verify pieces against the existing hash array or hash tree
			for (int i = 0; i < numPieces; i++) {

				if (!PieceDatabase.this.verifiedPieces.get (i)) {

					if (!fileBackedPieces.get (i)) {

						synchronized (PieceDatabase.this.presentPieces) {
							PieceDatabase.this.presentPieces.clear (i);
						}

					} else {

						ByteBuffer storedPiece = PieceDatabase.this.storage.read (i);

						this.digest.reset();
						this.digest.update (storedPiece);
						try {
							this.digest.digest (storedPieceHash, 0, 20);
						} catch (DigestException e) {
							// Shouldn't happen
							throw new InternalError (e.getMessage());
						}

						boolean storedPieceOK = false;
						if (PieceDatabase.this.pieceHashes != null) {
							// Hash array verification
							storedPieceOK = ByteBuffer.wrap(PieceDatabase.this.pieceHashes, 20 * i, 20).equals (ByteBuffer.wrap (storedPieceHash));
						} else {
							// Hash tree verification
							ElasticTreeView view = PieceDatabase.this.elasticTree.getCeilingView (
									(i * PieceDatabase.this.storage.getDescriptor().getPieceSize()) + PieceDatabase.this.storage.getDescriptor().getPieceLength(i)
							);
							storedPieceOK = view.verifyLeafHash (i, storedPieceHash);
						}

						synchronized (PieceDatabase.this.presentPieces) {
							PieceDatabase.this.presentPieces.set (i, storedPieceOK);
						}
						PieceDatabase.this.verifiedPieces.set (i);

					}

					PieceDatabase.this.verifiedPieceCount++;

				}

				if (interrupted()) {
					return false;
				}

			}

			return true;

		}

	};


	/**
	 * Resumes state from metadata if possible
	 *
	 * @throws IOException if an I/O error was encountered while resuming
	 */
	private void resume() throws IOException {

		BitField presentPieces;
		byte[] elasticImmutableHashes;
		byte[] elasticViewHashes;

		switch (this.info.getPieceStyle()) {

			case MERKLE:
				elasticImmutableHashes = this.metadata.get ("elasticImmutable");
				elasticViewHashes = this.metadata.get ("elasticView");
				if ((elasticImmutableHashes != null) && (elasticViewHashes != null)) {
					this.elasticTree = ElasticTree.withNodeHashes (
							this.storage.getDescriptor().getPieceSize(),
							this.storage.getDescriptor().getLength(),
							elasticImmutableHashes
					);
					this.elasticTree.addView (ElasticTreeView.withMutableHashes (this.elasticTree, this.storage.getDescriptor().getLength(), elasticViewHashes));
				} else {
					this.elasticTree = ElasticTree.emptyTree (
							this.storage.getDescriptor().getPieceSize(),
							this.storage.getDescriptor().getLength(),
							ByteBuffer.wrap (this.info.getRootHash())
					);
				}
				break;

			case ELASTIC:
				elasticImmutableHashes = this.metadata.get ("elasticImmutable");
				byte[] viewsBytes = this.metadata.get ("elasticViews");
				byte[] viewSignaturesBytes = this.metadata.get ("elasticViewSignatures");
				if ((elasticImmutableHashes != null) && (viewsBytes != null) && (viewSignaturesBytes != null)) {
					this.elasticTree = ElasticTree.withNodeHashes (
							this.storage.getDescriptor().getPieceSize(),
							this.storage.getDescriptor().getLength(),
							elasticImmutableHashes
					);
				} else {
					this.elasticTree = ElasticTree.emptyTree (
							this.storage.getDescriptor().getPieceSize(),
							this.storage.getDescriptor().getLength(),
							ByteBuffer.wrap (this.info.getRootHash())
					);
				}
				try {
					if ((viewsBytes != null) && (viewSignaturesBytes != null)) {
						BDictionary viewsDictionary = (BDictionary)BDecoder.decode (viewsBytes);
						BDictionary viewSignaturesDictionary = (BDictionary)BDecoder.decode (viewSignaturesBytes);
						// elasticViews
						long length = this.info.getStorageDescriptor().getLength();
						for (BBinary viewLengthBinary : viewsDictionary.keySet()) {
							byte[] viewMutableHashes = viewsDictionary.getBytes (viewLengthBinary.stringValue());
							this.elasticTree.addView (ElasticTreeView.withMutableHashes (this.elasticTree, new Long (viewLengthBinary.stringValue()), viewMutableHashes));
							length = new Long (viewLengthBinary.stringValue());
						}
						// elasticViewSignatures
						for (BBinary viewLengthBinary : viewSignaturesDictionary.keySet()) {
							BList signatureList = (BList)viewSignaturesDictionary.get (viewLengthBinary);
							Long viewLength = new Long (viewLengthBinary.stringValue());
							ViewSignature viewSignature = new ViewSignature (
									viewLength,
									ByteBuffer.wrap (((BBinary)signatureList.get(0)).value()),
									ByteBuffer.wrap (((BBinary)signatureList.get(1)).value())
							);
							this.viewSignatures.put (viewLength, viewSignature);
						}

						this.storage.extend (length);

					}
				} catch (InvalidEncodingException e) {
					// TODO Test - test behaviour on failure
				}
				break;

		}

		presentPieces = new BitField (this.storage.getDescriptor().getNumberOfPieces());
		this.verifiedPieces = new BitField (this.storage.getDescriptor().getNumberOfPieces());
		this.verifiedPieceCount = 0;

		try {
			byte[] resumeBytes = this.metadata.get ("resume");
			if (resumeBytes != null) {
				BDictionary resumeDictionary = new BDecoder (resumeBytes).decodeDictionary();
				byte[] storageCookie = resumeDictionary.getBytes ("storageCookie");
				byte[] presentPiecesBytes = resumeDictionary.getBytes ("presentPieces");
				if (this.storage.validate (ByteBuffer.wrap (storageCookie))) {
					if ((presentPiecesBytes != null) && (presentPiecesBytes.length == presentPieces.byteLength())) {
						presentPieces = new BitField (presentPiecesBytes, this.storage.getDescriptor().getNumberOfPieces());
						this.verifiedPieces.not();
						this.verifiedPieceCount = this.storage.getDescriptor().getNumberOfPieces();
					}
				}
			}
		} catch (InvalidEncodingException e) {
			// Resume metadata is corrupt. Leave the database unverified
		}

		this.presentPieces = presentPieces;

		this.metadata.put ("resume", null);

	}


	/**
	 * Gets an Info describing the database's content
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return An Info describing the database's content
	 */
	public Info getInfo() {

		return this.info;

	}


	/**
	 * Gets a copy of the database's bitfield of present pieces
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return A copy of the database's bitfield of present pieces
	 */
	public BitField getPresentPieces() {

		synchronized (this.presentPieces) {
			return this.presentPieces.clone();
		}

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The number of pieces within the PieceDatabase that have been verified as being either
	 * present or absent. While the state of the PieceDatabase is CHECKING, this figure will rise
	 * towards the total number of pieces of the database.
	 */
	public int getVerifiedPieceCount() {

		return this.verifiedPieceCount;

	}


	/**
	 * Checks if a given piece is present in the database
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param pieceNumber The piece number to check
	 * @return True if the piece is present, otherwise false
	 */
	public boolean havePiece (int pieceNumber) {

		synchronized (this.presentPieces) {
			return this.presentPieces.get (pieceNumber);
		}

	}


	/**
	 * @return The descriptor for the underlying {@link Storage}
	 */
	public StorageDescriptor getStorageDescriptor() {

		synchronized (this.stateMachine) {

			return this.storage.getDescriptor();

		}

	}


	/**
	 * Gets the view signature for a given view length
	 *
	 * @param viewLength The view length
	 * @return The view signature
	 */
	public ViewSignature getViewSignature (long viewLength) {

		synchronized (this.stateMachine) {

			return this.viewSignatures.get (viewLength);

		}

	}


	/**
	 * Verifies a view signature against the torrent's DSA public key
	 *
	 * @param viewSignature The view signature
	 * @return {@code true} if the signature verified correctly, otherwise {@code false}
	 */
	public boolean verifyViewSignature (ViewSignature viewSignature) {

		synchronized (this.stateMachine) {

			if (viewSignature.getViewLength() == this.info.getStorageDescriptor().getLength()) {
				return true;
			}

			byte[] token = new byte[48];
			System.arraycopy (this.info.getHash().getBytes(), 0, token, 0, 20);
			ByteBuffer viewLengthBuffer = ByteBuffer.allocate (8);
			viewLengthBuffer.asLongBuffer().put (viewSignature.getViewLength());
			viewLengthBuffer.get (token, 20, 8);
			viewSignature.getViewRootHash().get (token, 28, 20);

			try {
				Signature verify = Signature.getInstance ("SHAwithDSA", "SUN");
				verify.initVerify (this.publicKey);
				verify.update (token);
				ByteBuffer derSignature = DSAUtil.p1363SignatureToDerSignature (viewSignature.getSignature());
				if (derSignature == null) {
					return false;
				}
				return verify.verify (derSignature.array());
			} catch (GeneralSecurityException e) {
				return false;
			}

		}

	}


	/**
	 * Extend the database with a new unpopulated view
	 *
	 * @param viewSignature The signature of the new view
	 * @throws IOException on any I/O error
	 */
	public void extend (ViewSignature viewSignature) throws IOException {

		if (this.info.getPieceStyle() != PieceStyle.ELASTIC) {
			throw new IllegalStateException ("Cannot extend non-elastic database");
		}

		synchronized (this.stateMachine) {

			if (this.stateMachine.getState() != State.AVAILABLE) {
				throw new IllegalStateException();
			}

			StorageDescriptor originalDescriptor = this.storage.getDescriptor();

			// Extend the database
			try {
				this.storage.extend (viewSignature.getViewLength());
				this.presentPieces.extend (this.storage.getDescriptor().getNumberOfPieces());
				this.verifiedPieces.extend (this.storage.getDescriptor().getNumberOfPieces());
			} catch (IOException e) {
				this.workQueue.execute (new Runnable() {
					public void run() {
						PieceDatabase.this.stateMachine.input (Input.ERROR);
					}
				});
				throw e;
			}

			// Create a new view
			int additionalPieces = this.storage.getDescriptor().getNumberOfPieces()- originalDescriptor.getNumberOfPieces();
			int additionalHashes = additionalPieces + (originalDescriptor.isRegular() ? 0 : 1);
			for (int i = 0; i < additionalHashes; i++) {
				int pieceNumber = this.storage.getDescriptor().getNumberOfPieces() - additionalHashes + i;
				this.presentPieces.clear (pieceNumber);
				this.verifiedPieces.set (pieceNumber);
			}
			this.verifiedPieceCount += additionalPieces;
			this.elasticTree.addView (viewSignature.getViewLength(), viewSignature.getViewRootHash());

			this.viewSignatures.put (viewSignature.getViewLength(), viewSignature);

		}

	}


	/**
	 * Extend the database with additional data
	 *
	 * @param privateKey The private key to sign the new root hash
	 * @param length The new total length of the database
	 * @return The signature of the new view
	 * @throws IOException on any I/O error
	 */
	public ViewSignature extendDataInPlace (PrivateKey privateKey, long length) throws IOException {

		// TODO Refactor - partial merge with extendData

		if (this.info.getPieceStyle() != PieceStyle.ELASTIC) {
			throw new IllegalStateException ("Cannot extend non-elastic database");
		}

		synchronized (this.stateMachine) {

			if (this.stateMachine.getState() != State.AVAILABLE) {
				throw new IllegalStateException();
			}

			StorageDescriptor originalDescriptor = this.storage.getDescriptor();

			// Extend the database and write the additional data
			try {
				this.storage.extend (length);
				this.presentPieces.extend (this.storage.getDescriptor().getNumberOfPieces());
				this.verifiedPieces.extend (this.storage.getDescriptor().getNumberOfPieces());
			} catch (IOException e) {
				this.workQueue.execute (new Runnable() {
					public void run() {
						PieceDatabase.this.stateMachine.input (Input.ERROR);
					}
				});
				throw e;
			}

			// Create a new view
			int additionalPieces = this.storage.getDescriptor().getNumberOfPieces() - originalDescriptor.getNumberOfPieces();
			int additionalHashes = additionalPieces + (originalDescriptor.isRegular() ? 0 : 1);
			byte[][] leafHashes = new byte[additionalHashes][];
			for (int i = 0; i < additionalHashes; i++) {
				int pieceNumber = this.storage.getDescriptor().getNumberOfPieces() - additionalHashes + i;
				ByteBuffer storedPiece = PieceDatabase.this.storage.read (pieceNumber);
				leafHashes[i] = new byte[20];
				this.digest.reset();
				this.digest.update (storedPiece);
				try {
					this.digest.digest (leafHashes[i], 0, 20);
				} catch (DigestException e) {
					// Shouldn't happen
					throw new InternalError (e.getMessage());
				}
				this.presentPieces.set (pieceNumber);
				this.verifiedPieces.set (pieceNumber);
			}
			this.verifiedPieceCount += additionalPieces;

			this.elasticTree.addView (length, leafHashes);
			garbageCollectViews();

			// Generate the signature for the new view
			byte[] viewRootHash = this.elasticTree.getView(length).getRootHash();
			byte[] token = new byte[48];
			System.arraycopy (this.info.getHash().getBytes(), 0, token, 0, 20);
			ByteBuffer viewLengthBuffer = ByteBuffer.allocate (8);
			viewLengthBuffer.asLongBuffer().put (length);
			viewLengthBuffer.get (token, 20, 8);
			System.arraycopy (viewRootHash, 0, token, 28, 20);

			byte[] derSignature = null;
			try {
				Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
				dsa.initSign (privateKey);
				dsa.update (token);
				derSignature = dsa.sign();
			} catch (GeneralSecurityException e) {
				throw new InternalError (e.getMessage());
			}

			ViewSignature signature = new ViewSignature (length, ByteBuffer.wrap (viewRootHash), ByteBuffer.wrap (DSAUtil.derSignatureToP1363Signature (derSignature)));
			this.viewSignatures.put (length, signature);

			return signature;

		}

	}


	/**
	 * Extend the database with additional data
	 *
	 * @param privateKey The private key to sign the new root hash
	 * @param additionalData The additional data to append to the database
	 * @return The signature of the new view
	 * @throws IOException on any I/O error
	 */
	public ViewSignature extendData (PrivateKey privateKey, ByteBuffer additionalData) throws IOException {

		if (this.info.getPieceStyle() != PieceStyle.ELASTIC) {
			throw new IllegalStateException ("Cannot extend non-elastic database");
		}

		synchronized (this.stateMachine) {

			if (this.stateMachine.getState() != State.AVAILABLE) {
				throw new IllegalStateException();
			}

			StorageDescriptor originalDescriptor = this.storage.getDescriptor();
			int originalLastPiece = (originalDescriptor.getLength() == 0) ? 0 : originalDescriptor.getNumberOfPieces () - 1;
			long length = originalDescriptor.getLength() + additionalData.remaining();

			// Extend the database and write the additional data
			try {
				this.storage.extend (length);
				this.presentPieces.extend (this.storage.getDescriptor().getNumberOfPieces());
				this.verifiedPieces.extend (this.storage.getDescriptor().getNumberOfPieces());
				WritableByteChannel channel = this.storage.openOutputChannel (originalLastPiece, originalDescriptor.getLastPieceLength());
				channel.write (additionalData);
			} catch (IOException e) {
				this.workQueue.execute (new Runnable() {
					public void run() {
						PieceDatabase.this.stateMachine.input (Input.ERROR);
					}
				});
				throw e;
			}

			// Create a new view
			int additionalPieces = this.storage.getDescriptor().getNumberOfPieces() - originalDescriptor.getNumberOfPieces();
			int additionalHashes = additionalPieces + (originalDescriptor.isRegular() ? 0 : 1);
			byte[][] leafHashes = new byte[additionalHashes][];
			for (int i = 0; i < additionalHashes; i++) {
				int pieceNumber = this.storage.getDescriptor().getNumberOfPieces() - additionalHashes + i;
				ByteBuffer storedPiece = PieceDatabase.this.storage.read (pieceNumber);
				leafHashes[i] = new byte[20];
				this.digest.reset();
				this.digest.update (storedPiece);
				try {
					this.digest.digest (leafHashes[i], 0, 20);
				} catch (DigestException e) {
					// Shouldn't happen
					throw new InternalError (e.getMessage());
				}
				this.presentPieces.set (pieceNumber);
				this.verifiedPieces.set (pieceNumber);
			}
			this.verifiedPieceCount += additionalPieces;

			this.elasticTree.addView (length, leafHashes);

			// Generate the signature for the new view
			byte[] viewRootHash = this.elasticTree.getView(length).getRootHash();
			byte[] token = new byte[48];
			System.arraycopy (this.info.getHash().getBytes(), 0, token, 0, 20);
			ByteBuffer viewLengthBuffer = ByteBuffer.allocate (8);
			viewLengthBuffer.asLongBuffer().put (length);
			viewLengthBuffer.get (token, 20, 8);
			System.arraycopy (viewRootHash, 0, token, 28, 20);

			byte[] derSignature = null;
			try {
				Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
				dsa.initSign (privateKey);
				dsa.update (token);
				derSignature = dsa.sign();
			} catch (GeneralSecurityException e) {
				throw new InternalError (e.getMessage());
			}

			ViewSignature signature = new ViewSignature (length, ByteBuffer.wrap (viewRootHash), ByteBuffer.wrap (DSAUtil.derSignatureToP1363Signature (derSignature)));
			this.viewSignatures.put (length, signature);

			garbageCollectViews();

			return signature;

		}

	}


	/**
	 * Reads a piece from the database
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param pieceNumber The piece number to read
	 * @return The piece
	 * @throws IOException If the piece requested is not present, or on any other I/O error
	 */
	public Piece readPiece (int pieceNumber) throws IOException {

		synchronized (this.stateMachine) {

			if (this.stateMachine.getState() != State.AVAILABLE) {
				throw new IllegalStateException();
			}

			if (!havePiece (pieceNumber)) {
				throw new IOException ("Piece " + pieceNumber + " not present");
			}

			try {
				ByteBuffer content = this.storage.read (pieceNumber);
				HashChain hashChain = null;
				if (this.elasticTree != null) {
					hashChain = this.elasticTree.getHashChain (pieceNumber, this.storage.getDescriptor().getPieceLength (pieceNumber));
				}
				return new Piece (pieceNumber, content, hashChain);
			} catch (IOException e) {
				this.workQueue.execute (new Runnable() {
					public void run() {
						PieceDatabase.this.stateMachine.input (Input.ERROR);
					}
				});
				throw e;
			}

		}

	}


	/**
	 * Verifies a piece's hash and stores it in the database if it is correct
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param piece The piece
	 * @return {@code true} if the piece verified correctly and was stored, otherwise {@code false}
	 * @throws IllegalStateException if the state of the database is not currently AVAILABLE
	 * @throws IOException on any I/O error
	 */
	public boolean writePiece (Piece piece) throws IOException {

		synchronized (this.stateMachine) {

			if (this.stateMachine.getState() != State.AVAILABLE) {
				throw new IllegalStateException();
			}

			// Build hash of the supplied piece
			byte[] checkPieceHash = new byte[20];
			this.digest.reset();
			this.digest.update (piece.getContent());
			try {
				this.digest.digest (checkPieceHash, 0, 20);
			} catch (GeneralSecurityException e) {
				// Shouldn't happen
				throw new InternalError (e.getMessage());
			}

			if (this.elasticTree != null) {

				HashChain hashChain = piece.getHashChain();
				ViewSignature viewSignature = piece.getViewSignature();

				// Create the view and store the signature if necessary
				if ((viewSignature != null) && (this.elasticTree.getView (viewSignature.getViewLength()) == null)) {
					this.elasticTree.addView (viewSignature.getViewLength(), viewSignature.getViewRootHash());
					this.viewSignatures.put (viewSignature.getViewLength(), viewSignature);
				}

				// Verify the hash chain against the tree
				if (!this.elasticTree.verifyHashChain (piece.getPieceNumber(), hashChain)) {
					return false;
				}

				// Compare hash of supplied piece to known valid hash
				if (!this.elasticTree.getView(hashChain.getViewLength()).verifyLeafHash (piece.getPieceNumber(), checkPieceHash)) {
					return false;
				}

			} else {

				// Compare hash of supplied piece to known valid hash
				if (!ByteBuffer.wrap(this.pieceHashes, 20 * piece.getPieceNumber(), 20).equals (ByteBuffer.wrap (checkPieceHash))) {
					return false;
				}

			}

			try {
				this.storage.write (piece.getPieceNumber(), piece.getContent());
			} catch (IOException e) {
				this.workQueue.execute (new Runnable() {
					public void run() {
						PieceDatabase.this.stateMachine.input (Input.ERROR);
					}
				});
				throw e;
			}

			synchronized (this.presentPieces) {
				this.presentPieces.set (piece.getPieceNumber());
			}

			return true;

		}

	}


	/**
	 * Dispose of tree views and associated signatures that are not necessary for the serving of
	 * present pieces
	 */
	public void garbageCollectViews() {

		List<Long> evictedViews = this.elasticTree.garbageCollectViews();
		for (Long viewLength : evictedViews) {
			this.viewSignatures.remove (viewLength);
		}

	}


	/**
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @return The current state of the PieceDatabase
	 */
	public State getState() {

		return this.stateMachine.getState();

	}


	/**
	 * Attempts to start the PieceDatabase, checking the content first as required. If the database
	 * is already started, is in the process of starting, stopping, or has terminated, this method
	 * will have no effect
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 * 
	 * @param synchronous If {@code true}, the database is this method will block until the database
	 *        reaches any of the states STOPPED, AVAILABLE, ERROR or TERMINATED. If {@code false},
	 *        the database is started asynchronously and this method will return immediately.
	 */
	public synchronized void start (boolean synchronous) {

		if (synchronous) {
			StateMachineUtil.transitionAndWait (this.stateMachine, this.workQueue, Input.START, START_FINAL_STATES);
		} else {
			this.workQueue.execute (new Runnable() {
				public void run() {
					PieceDatabase.this.stateMachine.input (Input.START);
				}
			});
		}

	}


	/**
	 * Stops the PieceDatabase. If the database is already stopped, is in the process of stopping, or
	 * has terminated, this method will have no effect
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param synchronous If {@code true}, this method will block until the database reaches any of
	 *        the states STOPPED, ERROR, or TERMINATED. If {@code false}, the database is stopped
	 *        asynchronously and this method will return immediately.
	 */
	public synchronized void stop (boolean synchronous) {

		if (synchronous) {
			StateMachineUtil.transitionAndWait (this.stateMachine, this.workQueue, Input.STOP, STOP_FINAL_STATES);
		} else {
			this.workQueue.execute (new Runnable() {
				public void run() {
					PieceDatabase.this.stateMachine.input (Input.STOP);
				}
			});
		}

	}


	/**
	 * Terminates the PieceDatabase. If the database is already terminated, or is in the process of
	 * terminating, this will have no effect
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * If the data is fully checked and the database terminates without error, it will save its
	 * current state through a {@code Metadata} instance if one was registered on construction.
	 *
	 * @param synchronous If {@code true}, this method will block until the database reaches the
	 *        state TERMINATED. If {@code false}, the database is terminated asynchronously and
	 *        this method will return immediately.
	 */
	public synchronized void terminate (boolean synchronous) {

		if (synchronous) {
			StateMachineUtil.transitionAndWait (this.stateMachine, this.workQueue, Input.TERMINATE, TERMINATE_FINAL_STATES);
		} else {
			this.workQueue.execute (new Runnable() {
				public void run() {
					PieceDatabase.this.stateMachine.input (Input.TERMINATE);
				}
			});
		}

	}


	/**
	 * Adds a listener for state changes to the PieceDatabase
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param listener The listener to add
	 */
	public void addListener (PieceDatabaseListener listener) {

		synchronized (this.listeners) {
			this.listeners.add (listener);
		}

	}


	/**
	 * Removes a listener for state changes to the PieceDatabase
	 *
	 * <p><b>Thread safety:</b> This method is thread safe
	 *
	 * @param listener The listener to remove
	 */
	public void removeListener (PieceDatabaseListener listener) {

		synchronized (this.listeners) {
			this.listeners.remove (listener);
		}

	}


	/**
	 * If a {@code Metadata} is supplied, a single attempt will be made to restore the previous
	 * present piece state during construction.
	 *
	 * @param info The {@code Info} describing the represented torrent
	 * @param publicKey The DSA public key used to sign the {@code Info}, or {@code null}
	 * @param storage The {@code Storage} for piece data
	 * @param metadata A {@code Metadata} to store persistent metadata about the database, or
	 *        {@code null} if no metadata should be stored.
	 * @throws IOException On any I/O error reading from the {@code Metadata}
	 */
	public PieceDatabase (Info info, PublicKey publicKey, Storage storage, Metadata metadata) throws IOException {

		this.info = info;
		this.publicKey = publicKey;
		this.storage = storage;
		this.metadata = metadata;
		this.pieceHashes = this.info.getPieces();
		this.elasticTree = null;

		// Resume previous state from metadata if possible
		if (this.metadata != null ) {

			resume();

		} else {

			this.verifiedPieces = new BitField (this.storage.getDescriptor().getNumberOfPieces());
			this.verifiedPieceCount = 0;
			this.presentPieces = new BitField (this.storage.getDescriptor().getNumberOfPieces());

			if ((this.info.getPieceStyle() == PieceStyle.MERKLE) || (this.info.getPieceStyle() == PieceStyle.ELASTIC)) {
				this.elasticTree = ElasticTree.emptyTree (
						this.storage.getDescriptor().getPieceSize(),
						this.storage.getDescriptor().getLength(),
						ByteBuffer.wrap (this.info.getRootHash())
				);
			}

		}

		try {
			this.digest = MessageDigest.getInstance ("SHA");
		} catch (NoSuchAlgorithmException e) {
			// Shouldn't happen
			throw new InternalError (e.getMessage());
		}

		this.workQueue = new WorkQueue ("PieceDatabase WorkQueue - " + CharsetUtil.hexencode (info.getHash().getBytes()));

	}


}
