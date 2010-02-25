/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.torrentdb;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.itadaki.bobbin.torrentdb.FileMetadata;
import org.itadaki.bobbin.torrentdb.FileStorage;
import org.itadaki.bobbin.torrentdb.Info;
import org.itadaki.bobbin.torrentdb.MemoryStorage;
import org.itadaki.bobbin.torrentdb.Piece;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.PieceDatabaseListener;
import org.itadaki.bobbin.torrentdb.Storage;
import org.itadaki.bobbin.torrentdb.StorageDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.DSAUtil;
import org.itadaki.bobbin.util.elastictree.ElasticTree;
import org.itadaki.bobbin.util.elastictree.HashChain;
import org.junit.Test;

import test.Util;


/**
 * Tests PieceDatabase
 */
public class TestPieceDatabase {

	/**
	 * Tests that the initial state is STOPPED
	 * @throws Exception 
	 */
	@Test
	public void testInitialState() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);

		assertEquals (PieceDatabase.State.STOPPED, pieceDatabase.getState());


	}


	/**
	 * Tests that start() transititions asynchronously from STOPPED to AVAILABLE
	 * @throws Exception 
	 */
	@Test
	public void testAsynchronousTransitionStoppedAvailable() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);

		final CountDownLatch latch = new CountDownLatch (1);
		pieceDatabase.addListener (new PieceDatabaseListener() {

			public void pieceDatabaseAvailable() {
				latch.countDown();
			}

			public void pieceDatabaseStopped() { }
			public void pieceDatabaseError() { }
			public void pieceDatabaseTerminated() { }

		});
		pieceDatabase.start (false);
		
		assertTrue (latch.await (2, TimeUnit.SECONDS));
		assertEquals (PieceDatabase.State.AVAILABLE, pieceDatabase.getState());

	}


	/**
	 * Test that stop() transitions asynchronously from AVAILABLE to STOPPED
	 * @throws Exception 
	 */
	@Test
	public void testAsynchronousTransitionAvailableStopped() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);

		final CyclicBarrier barrier = new CyclicBarrier (2);
		pieceDatabase.addListener (new PieceDatabaseListener() {

			public void pieceDatabaseAvailable() {
				try {
					barrier.await();
				} catch (Exception e) {
					// Do nothing
				}
			}

			public void pieceDatabaseStopped() {
				try {
					barrier.await();
				} catch (Exception e) {
					// Do nothing
				}
			}

			public void pieceDatabaseError() { }
			public void pieceDatabaseTerminated() { }

		});
		pieceDatabase.start (false);
		barrier.await (2, TimeUnit.SECONDS);

		assertEquals (PieceDatabase.State.AVAILABLE, pieceDatabase.getState());

		pieceDatabase.stop (false);
		barrier.await (2, TimeUnit.SECONDS);

		assertEquals (PieceDatabase.State.STOPPED, pieceDatabase.getState());

	}


	/**
	 * Test that stop() transitions synchronously from AVAILABLE to STOPPED
	 * @throws Exception 
	 */
	@Test
	public void testSynchronousTransitionAvailableStopped() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);

		pieceDatabase.start (true);

		assertEquals (PieceDatabase.State.AVAILABLE, pieceDatabase.getState());

		pieceDatabase.stop (true);

		assertEquals (PieceDatabase.State.STOPPED, pieceDatabase.getState());

	}


	/**
	 * Test removeListener()
	 * @throws Exception 
	 */
	@Test
	public void testRemoveListener() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);

		final boolean availableCalled[] = new boolean[1];
		final boolean stoppedCalled[] = new boolean[1];
		PieceDatabaseListener listener = new PieceDatabaseListener() {

			public void pieceDatabaseAvailable() {
				availableCalled[0] = true;
			}

			public void pieceDatabaseStopped() {
				stoppedCalled[0] = true;
			}

			public void pieceDatabaseError() { }
			public void pieceDatabaseTerminated() { }

		};
		pieceDatabase.addListener (listener);
		pieceDatabase.start (true);

		pieceDatabase.removeListener (listener);

		pieceDatabase.stop (true);

		assertTrue (availableCalled[0]);
		assertFalse (stoppedCalled[0]);

	}


	/**
	 * Tests that call to readPiece() fails while STOPPED
	 * @throws Exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testReadPieceWhileStopped() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1", 16384);

		pieceDatabase.readPiece (0);

	}


	/**
	 * Tests that call to checkAndWritePiece() fails while STOPPED
	 * @throws Exception
	 */
	@Test(expected=IllegalStateException.class)
	public void testCheckAndWritePieceWhileStopped() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0", 16384);

		pieceDatabase.writePiece (new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)), null));

	}


	/**
	 * Tests that getVerifiedPieceCount() is initially 0
	 * @throws Exception
	 */
	@Test
	public void testGetVerifiedPieceCountInitially() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);

		assertEquals (0, pieceDatabase.getVerifiedPieceCount());

	}


	/**
	 * Tests that getVerifiedPieceCount() equals the total piece count when AVAILABLE
	 * @throws Exception
	 */
	@Test
	public void testGetVerifiedPieceCountAvailable() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);
		pieceDatabase.start (true);

		assertEquals (4, pieceDatabase.getVerifiedPieceCount());

	}


	/**
	 * Construct from blank existing data and check bitfield
	 * @throws Exception 
	 */
	@Test
	public void testExistingBlankFileBitfield() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);
		pieceDatabase.start (true);

		BitField bitField = pieceDatabase.getPresentPieces();

		assertEquals (4, bitField.length());
		for (int i = 0; i < bitField.length (); i++) {
			assertFalse (bitField.get (i));
			assertFalse (pieceDatabase.havePiece (i));
		}

	}


	/**
	 * Construct from partial existing data and check bitfield
	 * @throws Exception 
	 */
	@Test
	public void testExistingPartialFileBitfield() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1001", 16384);
		pieceDatabase.start (true);

		BitField bitField = pieceDatabase.getPresentPieces();

		assertEquals (4, bitField.length());
		assertTrue (bitField.get (0));
		assertFalse (bitField.get (1));
		assertFalse (bitField.get (2));
		assertTrue (bitField.get (3));
		assertTrue (pieceDatabase.havePiece (0));
		assertFalse (pieceDatabase.havePiece (1));
		assertFalse (pieceDatabase.havePiece (2));
		assertTrue (pieceDatabase.havePiece (3));

	}


	/**
	 * Construct from fully existing data and check bitfield
	 * @throws Exception 
	 */
	@Test
	public void testExistingFullFileBitfield() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("1111", 16384);
		pieceDatabase.start (true);

		BitField bitField = pieceDatabase.getPresentPieces();

		assertEquals (4, bitField.length());
		assertTrue (bitField.get (0));
		assertTrue (bitField.get (1));
		assertTrue (bitField.get (2));
		assertTrue (bitField.get (3));
		assertTrue (pieceDatabase.havePiece (0));
		assertTrue (pieceDatabase.havePiece (1));
		assertTrue (pieceDatabase.havePiece (2));
		assertTrue (pieceDatabase.havePiece (3));

	}


	/**
	 * Check readPiece() - < 0
	 * @throws Exception 
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void testReadPieceNegative() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);
		pieceDatabase.start (true);

		pieceDatabase.readPiece (-1);

	}


	/**
	 * Check readPiece() - in range
	 * @throws Exception 
	 */
	@Test
	public void testReadPieceOK() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0010", 16384);
		pieceDatabase.start (true);

		Piece piece = pieceDatabase.readPiece (2);

		assertEquals (ByteBuffer.wrap (Util.pseudoRandomBlock (2, 16384, 16384)), piece.getContent());

	}


	/**
	 * Check readPiece() - > range
	 *
	 * @throws Exception 
	 */
	@Test(expected=IndexOutOfBoundsException.class)
	public void testReadPieceTooLarge() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);
		pieceDatabase.start (true);

		pieceDatabase.readPiece (4);

	}


	/**
	 * Check verifyAndWritePiece - bad
	 * @throws Exception 
	 *
	 */
	@Test
	public void testVerifyAndWritePieceBad() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);
		pieceDatabase.start (true);
		boolean result = pieceDatabase.writePiece (new Piece (2, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)), null));
		assertFalse (result);
		assertFalse (pieceDatabase.havePiece (2));

		boolean exceptionCaught = false;
		try {
			pieceDatabase.readPiece (2);
		} catch (IOException e) {
			exceptionCaught = true;
		}
		assertTrue (exceptionCaught);

	}


	/**
	 * Check verifyAndWritePiece - good
	 * @throws Exception 
	 *
	 */
	@Test
	public void testVerifyAndWritePieceGood() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);
		pieceDatabase.start (true);
		boolean result = pieceDatabase.writePiece (new Piece (2, ByteBuffer.wrap (Util.pseudoRandomBlock (2, 16384, 16384)), null));
		assertTrue (result);
		assertTrue (pieceDatabase.havePiece (2));

		Piece piece = pieceDatabase.readPiece (2);

		assertEquals (ByteBuffer.wrap (Util.pseudoRandomBlock (2, 16384, 16384)), piece.getContent());

	}


	/**
	 * Check verifyAndWritePiece - good (small final piece)
	 * @throws Exception 
	 *
	 */
	@Test
	public void testVerifyAndWritePieceSmallGood() throws Exception {

		File testFile = Util.createNonExistentTemporaryFile();

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (16384, 65537);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.createSingleFile (testFile.getName(), 65537, 16384, pieceHashes);

		PieceDatabase pieceDatabase = new PieceDatabase (info, null, FileStorage.create (testFile.getParentFile(), info), null);
		pieceDatabase.start (true);
		boolean result = pieceDatabase.writePiece (new Piece (4, ByteBuffer.wrap (Util.pseudoRandomBlock (4, 1, 1)), null));
		assertTrue (result);
		assertTrue (pieceDatabase.havePiece (4));

		Piece piece = pieceDatabase.readPiece (4);

		assertEquals (ByteBuffer.wrap (Util.pseudoRandomBlock (4, 1, 1)), piece.getContent());

	}


	/**
	 * Check read after terminate
	 *
	 * @throws Exception 
	 */
	@Test(expected=IllegalStateException.class)
	public void testReadAfterTerminate() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);
		pieceDatabase.start (true);
		pieceDatabase.terminate (true);

		pieceDatabase.readPiece (0);

	}


	/**
	 * Check write after terminate
	 * @throws Exception 
	 */
	@Test(expected=IllegalStateException.class)
	public void testCheckAndWriteAfterTerminate() throws Exception {

		PieceDatabase pieceDatabase = MockPieceDatabase.create ("0000", 16384);
		pieceDatabase.start (true);
		pieceDatabase.writePiece (new Piece (2, ByteBuffer.wrap (Util.pseudoRandomBlock (2, 16384, 16384)), null));
		pieceDatabase.terminate (true);

		pieceDatabase.readPiece (2);

	}


	/**
	 * Tests a storage error during verification
	 * @throws Exception
	 */
	@Test
	public void testErrorDuringVerification() throws Exception {

		Info info = Info.createSingleFile ("test", 1024, 1024, new byte[20]);
		Storage storage = new MemoryStorage (new StorageDescriptor (1024, 1024)) {
			@Override
			public ByteBuffer read (int pieceNumber) throws IOException {
				throw new IOException();
			}
		};
		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, null);
		final boolean[] errorCalled = new boolean[1];
		pieceDatabase.addListener (new PieceDatabaseListener() {
			public void pieceDatabaseAvailable() { }
			public void pieceDatabaseError() {
				errorCalled[0] = true;
			}
			public void pieceDatabaseStopped() { }
			public void pieceDatabaseTerminated() { }
		});

		pieceDatabase.start (true);

		assertEquals (PieceDatabase.State.ERROR, pieceDatabase.getState());
		assertTrue (errorCalled[0]);

	}


	/**
	 * Tests a stop during verification
	 * @throws Exception
	 */
	@Test
	public void testStopDuringVerification() throws Exception {

		Info info = Info.createSingleFile ("test", 2048, 1024, new byte[40]);
		final CountDownLatch latch1 = new CountDownLatch (1);
		final CountDownLatch latch2 = new CountDownLatch (1);
		Storage storage = new MemoryStorage (new StorageDescriptor (1024, 2048)) {
			@Override
			public ByteBuffer read (int pieceNumber) throws IOException {
				if (pieceNumber == 0) {
					try {
						latch1.countDown();
						latch2.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				return ByteBuffer.allocate (1024);
			}
		};
		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, null);

		pieceDatabase.start (false);

		latch1.await ();
		pieceDatabase.stop (false);
		Thread.sleep (500);
		latch2.countDown();
		Thread.sleep (500);

		assertEquals (PieceDatabase.State.STOPPED, pieceDatabase.getState());


	}


	/**
	 * Tests a storage error during readPiece()
	 * @throws Exception
	 */
	@Test
	public void testErrorDuringRead() throws Exception {

		Info info = Info.createSingleFile ("test", 1024, 1024, Util.flatten2DArray (Util.pseudoRandomBlockHashes (1024, 1024)));
		Storage storage = new MemoryStorage (new StorageDescriptor (1024, 1024)) {
			private int count = 0;
			@Override
			public ByteBuffer read (int pieceNumber) throws IOException {
				if (this.count++ == 1) {
					throw new IOException();
				}
				return ByteBuffer.wrap (Util.pseudoRandomBlock (0, 1024, 1024));
			}
		};
		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, null);
		final boolean[] errorCalled = new boolean[1];
		pieceDatabase.addListener (new PieceDatabaseListener() {
			public void pieceDatabaseAvailable() { }
			public void pieceDatabaseError() {
				errorCalled[0] = true;
			}
			public void pieceDatabaseStopped() { }
			public void pieceDatabaseTerminated() { }
		});

		pieceDatabase.start (true);
		boolean caught = false;
		try {
			pieceDatabase.readPiece (0);
		} catch (IOException e) {
			caught = true;
		}
		Thread.sleep (500);

		assertTrue (caught);
		assertEquals (PieceDatabase.State.ERROR, pieceDatabase.getState());
		assertTrue (errorCalled[0]);

	}


	/**
	 * Tests a storage error during checkAndWritePiece()
	 * @throws Exception
	 */
	@Test
	public void testErrorDuringWrite() throws Exception {

		Info info = Info.createSingleFile ("test", 1024, 1024, Util.flatten2DArray (Util.pseudoRandomBlockHashes (1024, 1024)));
		Storage storage = new MemoryStorage (new StorageDescriptor (1024, 1024)) {
			@Override
			public ByteBuffer read (int pieceNumber) throws IOException {
				return ByteBuffer.wrap (Util.pseudoRandomBlock (0, 1024, 1024));
			}
			@Override
			public void write (int pieceNumber, ByteBuffer buffer) throws IOException {
				throw new IOException();
			}
		};
		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, null);
		final boolean[] errorCalled = new boolean[1];
		pieceDatabase.addListener (new PieceDatabaseListener() {
			public void pieceDatabaseAvailable() { }
			public void pieceDatabaseError() {
				errorCalled[0] = true;
			}
			public void pieceDatabaseStopped() { }
			public void pieceDatabaseTerminated() { }
		});

		pieceDatabase.start (true);
		boolean caught = false;
		try {
			pieceDatabase.writePiece (new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 1024, 1024)), null));
		} catch (IOException e) {
			caught = true;
		}
		Thread.sleep (500);

		assertTrue (caught);
		assertEquals (PieceDatabase.State.ERROR, pieceDatabase.getState());
		assertTrue (errorCalled[0]);

	}


	/**
	 * Tests a storage error forces a full verification
	 * @throws Exception
	 */
	@Test
	public void testErrorForcesVerification() throws Exception {

		Info info = Info.createSingleFile ("test", 4096, 1024, Util.flatten2DArray (Util.pseudoRandomBlockHashes (1024, 4096)));
		Storage storage = new MemoryStorage (new StorageDescriptor (1024, 4096)) {
			@Override
			public ByteBuffer read (int pieceNumber) throws IOException {
				if (pieceNumber == 3) {
					throw new IOException();
				}
				return ByteBuffer.wrap (Util.pseudoRandomBlock (0, 1024, 1024));
			}
		};
		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, null);
		final boolean[] errorCalled = new boolean[1];
		pieceDatabase.addListener (new PieceDatabaseListener() {
			public void pieceDatabaseAvailable() { }
			public void pieceDatabaseError() {
				errorCalled[0] = true;
			}
			public void pieceDatabaseStopped() { }
			public void pieceDatabaseTerminated() { }
		});

		pieceDatabase.start (true);

		assertEquals (PieceDatabase.State.ERROR, pieceDatabase.getState());
		assertTrue (errorCalled[0]);
		assertEquals (0, pieceDatabase.getVerifiedPieceCount());

	}


	/**
	 * Check database not initially verified when no resume data is provided
	 * @throws Exception
	 */
	@Test
	public void testNoResumeNotVerified() throws Exception {

		File testFile = Util.createNonExistentTemporaryFile();

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (16384, 16384);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.createSingleFile (testFile.getName(), 16384, 16384, pieceHashes);
		Storage storage = FileStorage.create (testFile.getParentFile(), info);
		storage.write (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)));

		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, null);
		assertEquals (0, pieceDatabase.getVerifiedPieceCount());

	}


	/**
	 * Check database initially verified when resume data is provided
	 * @throws Exception
	 */
	@Test
	public void testResumeVerified() throws Exception {

		File testFile = Util.createNonExistentTemporaryFile();
		File metadataDirectory = Util.createTemporaryDirectory();

		byte[][] blockHashes = Util.pseudoRandomBlockHashes (16384, 16384);
		byte[] pieceHashes = Util.flatten2DArray (blockHashes);
		Info info = Info.createSingleFile (testFile.getName(), 16384, 16384, pieceHashes);
		Storage storage = FileStorage.create (testFile.getParentFile(), info);
		storage.write (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)));

		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, new FileMetadata (metadataDirectory));

		assertEquals (0, pieceDatabase.getVerifiedPieceCount());
		assertFalse (pieceDatabase.getPresentPieces().get (0));

		pieceDatabase.start (true);

		assertEquals (1, pieceDatabase.getVerifiedPieceCount());
		assertTrue (pieceDatabase.getPresentPieces().get (0));

		pieceDatabase.terminate (true);
		Storage storage2 = FileStorage.create (testFile.getParentFile(), info);
		PieceDatabase pieceDatabase2 = new PieceDatabase (info, null, storage2, new FileMetadata (metadataDirectory));

		assertEquals (1, pieceDatabase2.getVerifiedPieceCount());
		assertTrue (pieceDatabase2.getPresentPieces().get (0));

	}


	/**
	 * Tests writing to a Merkle database with 1 partial piece
	 * @throws Exception
	 */
	@Test
	public void testMerkle1PartialWriteOK() throws Exception {

		int pieceSize = 16384;
		int totalLength = 1000;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createEmptyMerkle (pieceSize, totalLength, tree.getView(totalLength).getRootHash());

		pieceDatabase.start (true);

		HashChain hashChain = tree.getHashChain (0, totalLength);
		Piece piece = new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 1000, 1000)), hashChain);
		assertTrue (pieceDatabase.writePiece (piece));

	}


	/**
	 * Tests writing to a Merkle database with 1 partial piece
	 * @throws Exception
	 */
	@Test
	public void testMerkle1PartialWriteFailPiece() throws Exception {

		int pieceSize = 16384;
		int totalLength = 1000;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createEmptyMerkle (pieceSize, totalLength, tree.getView(totalLength).getRootHash());

		pieceDatabase.start (true);

		HashChain hashChain = tree.getHashChain (0, totalLength);
		Piece piece = new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (1, 1000, 1000)), hashChain);
		assertFalse (pieceDatabase.writePiece (piece));

	}


	/**
	 * Tests writing to a Merkle database with 1 partial piece
	 * @throws Exception
	 */
	@Test
	public void testMerkle1PartialWriteFailChain() throws Exception {

		int pieceSize = 16384;
		int totalLength = 1000;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createEmptyMerkle (pieceSize, totalLength, tree.getView(totalLength).getRootHash());

		pieceDatabase.start (true);

		ByteBuffer hashChainBytes = ByteBuffer.wrap (tree.getView (totalLength).getHashChain (0));
		hashChainBytes.put (0, (byte)0);
		HashChain hashChain = new HashChain (totalLength, hashChainBytes);

		Piece piece = new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 1000, 1000)), hashChain);
		assertFalse (pieceDatabase.writePiece (piece));

	}


	/**
	 * Tests writing to a Merkle database with 1 piece
	 * @throws Exception
	 */
	@Test
	public void testMerkle1WriteOK() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createEmptyMerkle (pieceSize, totalLength, tree.getView(totalLength).getRootHash());

		pieceDatabase.start (true);

		HashChain hashChain = tree.getHashChain (0, pieceSize);
		Piece piece = new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, pieceSize, pieceSize)), hashChain);
		assertTrue (pieceDatabase.writePiece (piece));

	}


	/**
	 * Tests writing to a Merkle database with 1 piece
	 * @throws Exception
	 */
	@Test
	public void testMerkle1WriteFail() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createEmptyMerkle (pieceSize, totalLength, tree.getView(totalLength).getRootHash());

		pieceDatabase.start (true);

		HashChain hashChain = tree.getHashChain (0, pieceSize);
		Piece piece = new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (1, pieceSize, pieceSize)), hashChain);
		assertFalse (pieceDatabase.writePiece (piece));

	}


	/**
	 * Tests writing to a Merkle database with 16 pieces
	 * @throws Exception
	 */
	@Test
	public void testMerkle16WriteOK() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384 * 16;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createEmptyMerkle (pieceSize, totalLength, tree.getView(totalLength).getRootHash());

		pieceDatabase.start (true);

		for (int i = 0; i < 16; i++) {
			HashChain hashChain = tree.getHashChain (i, pieceSize);
			Piece piece = new Piece (i, ByteBuffer.wrap (Util.pseudoRandomBlock (i, pieceSize, pieceSize)), hashChain);
			assertTrue (pieceDatabase.writePiece (piece));
		}

	}


	/**
	 * Tests writing to a Merkle database with 16 pieces
	 * @throws Exception
	 */
	@Test
	public void testMerkle16WriteFail() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384 * 16;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createEmptyMerkle (pieceSize, totalLength, tree.getView(totalLength).getRootHash());

		pieceDatabase.start (true);

		for (int i = 0; i < 16; i++) {
			HashChain hashChain = tree.getHashChain (i, pieceSize);
			Piece piece = new Piece (i, ByteBuffer.wrap (Util.pseudoRandomBlock (i+1, pieceSize, pieceSize)), hashChain);
			assertFalse (pieceDatabase.writePiece (piece));
		}

	}


	/**
	 * Check Merkle database not initially verified when no resume data is provided
	 * @throws Exception
	 */
	@Test
	public void testMerkleNoResumeNotVerified() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384;
		File testFile = Util.createNonExistentTemporaryFile();

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		Info info = Info.createSingleFileMerkle (testFile.getName(), totalLength, pieceSize, tree.getView(totalLength).getRootHash());
		Storage storage = FileStorage.create (testFile.getParentFile(), info);
		storage.write (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, pieceSize, pieceSize)));

		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, null);
		assertEquals (0, pieceDatabase.getVerifiedPieceCount());

	}


	/**
	 * Check Merkle database initially verified and tree populated when resume data is provided
	 * @throws Exception
	 */
	@Test
	public void testMerkleResumeVerified() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384;
		File testFile = Util.createNonExistentTemporaryFile();
		File metadataDirectory = Util.createTemporaryDirectory();

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		Info info = Info.createSingleFileMerkle (testFile.getName(), totalLength, pieceSize, tree.getView(totalLength).getRootHash());
		Storage storage = FileStorage.create (testFile.getParentFile(), info);
		storage.write (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)));

		PieceDatabase pieceDatabase = new PieceDatabase (info, null, storage, new FileMetadata (metadataDirectory));

		assertEquals (0, pieceDatabase.getVerifiedPieceCount());
		assertFalse (pieceDatabase.getPresentPieces().get (0));

		pieceDatabase.start (true);

		assertEquals (1, pieceDatabase.getVerifiedPieceCount());
		assertTrue (pieceDatabase.getPresentPieces().get (0));

		pieceDatabase.terminate (true);
		Storage storage2 = FileStorage.create (testFile.getParentFile(), info);
		PieceDatabase pieceDatabase2 = new PieceDatabase (info, null, storage2, new FileMetadata (metadataDirectory));

		assertEquals (1, pieceDatabase2.getVerifiedPieceCount());
		assertTrue (pieceDatabase2.getPresentPieces().get (0));


		pieceDatabase2.terminate (true);

	}


	/**
	 * Tests extend() on an Elastic database from 0 to 1 pieces
	 * @throws Exception
	 */
	@Test
	public void testElasticExtend0x1() throws Exception {

		int pieceSize = 16384;
		int totalLength = 0;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		byte[] derSignature = null;
		try {
			Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
			dsa.initSign (MockPieceDatabase.mockPrivateKey);
			dsa.update (tree.getView(totalLength).getRootHash());
			derSignature = dsa.sign();
		} catch (GeneralSecurityException e) {
			throw new InternalError (e.getMessage());
		}


		Info info = Info.createSingleFileElastic ("blah", totalLength, pieceSize, tree.getView(totalLength).getRootHash(),
				DSAUtil.derSignatureToP1363Signature (derSignature));
		Storage storage = new MemoryStorage (new StorageDescriptor (pieceSize, totalLength));

		PieceDatabase pieceDatabase = new PieceDatabase (info, MockPieceDatabase.mockPublicKey, storage, null);
		pieceDatabase.start (true);

		assertEquals (0, pieceDatabase.getVerifiedPieceCount());

		totalLength += pieceSize;
		tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		try {
			Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
			dsa.initSign (MockPieceDatabase.mockPrivateKey);
			dsa.update (tree.getView(totalLength).getRootHash());
			derSignature = dsa.sign();
		} catch (GeneralSecurityException e) {
			throw new InternalError (e.getMessage());
		}


		pieceDatabase.extend (
				new ViewSignature (
						totalLength,
						ByteBuffer.wrap (tree.getView(totalLength).getRootHash()),
						ByteBuffer.wrap (DSAUtil.derSignatureToP1363Signature (derSignature))
				)
		);

		assertEquals (totalLength, pieceDatabase.getStorageDescriptor().getLength());
		assertEquals (1, pieceDatabase.getVerifiedPieceCount());
		assertFalse (pieceDatabase.getPresentPieces().get (0));

		pieceDatabase.terminate (true);

	}


	/**
	 * Tests extend() on an Elastic database from 1 to 2 pieces
	 * @throws Exception
	 */
	@Test
	public void testElasticExtend1x2() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		byte[] derSignature = null;
		try {
			Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
			dsa.initSign (MockPieceDatabase.mockPrivateKey);
			dsa.update (tree.getView(totalLength).getRootHash());
			derSignature = dsa.sign();
		} catch (GeneralSecurityException e) {
			throw new InternalError (e.getMessage());
		}


		Info info = Info.createSingleFileElastic ("blah", totalLength, pieceSize, tree.getView(totalLength).getRootHash(),
				DSAUtil.derSignatureToP1363Signature (derSignature));
		Storage storage = new MemoryStorage (new StorageDescriptor (pieceSize, totalLength));
		storage.write (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)));

		PieceDatabase pieceDatabase = new PieceDatabase (info, MockPieceDatabase.mockPublicKey, storage, null);
		pieceDatabase.start (true);

		assertEquals (1, pieceDatabase.getVerifiedPieceCount());

		totalLength += pieceSize;
		tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		try {
			Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
			dsa.initSign (MockPieceDatabase.mockPrivateKey);
			dsa.update (tree.getView(totalLength).getRootHash());
			derSignature = dsa.sign();
		} catch (GeneralSecurityException e) {
			throw new InternalError (e.getMessage());
		}


		pieceDatabase.extend (
				new ViewSignature (
						totalLength,
						ByteBuffer.wrap (tree.getView(totalLength).getRootHash()),
						ByteBuffer.wrap (DSAUtil.derSignatureToP1363Signature (derSignature))
				)
		);

		assertEquals (totalLength, pieceDatabase.getStorageDescriptor().getLength());
		assertEquals (2, pieceDatabase.getVerifiedPieceCount());
		assertTrue (pieceDatabase.getPresentPieces().get (0));
		assertFalse (pieceDatabase.getPresentPieces().get (1));

		pieceDatabase.terminate (true);

	}


	/**
	 * Tests extend() on an Elastic database from 1 to 2 pieces
	 * @throws Exception
	 */
	@Test
	public void testElasticExtend1p5x2p5() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384 + 8192;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		byte[] originalSignature = Util.dsaSign (MockPieceDatabase.mockPrivateKey, tree.getView(totalLength).getRootHash());
		Info info = Info.createSingleFileElastic ("blah", totalLength, pieceSize, tree.getView(totalLength).getRootHash(), originalSignature);
		Storage storage = new MemoryStorage (new StorageDescriptor (pieceSize, totalLength));
		PieceDatabase pieceDatabase = new PieceDatabase (info, MockPieceDatabase.mockPublicKey, storage, null);
		pieceDatabase.start (true);
		pieceDatabase.writePiece (new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)), tree.getHashChain (0, 16384)));
		pieceDatabase.writePiece (new Piece (1, ByteBuffer.wrap (Util.pseudoRandomBlock (1, 8192, 8192)), tree.getHashChain (0, 8192)));

		assertEquals (2, pieceDatabase.getVerifiedPieceCount());
		assertEquals (2, pieceDatabase.getPresentPieces().cardinality());


		totalLength += pieceSize;
		tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		byte[] extendedSignature = Util.dsaSign (MockPieceDatabase.mockPrivateKey, tree.getView(totalLength).getRootHash());

		pieceDatabase.extend (
				new ViewSignature (
						totalLength,
						ByteBuffer.wrap (tree.getView(totalLength).getRootHash()),
						ByteBuffer.wrap (extendedSignature)
				)
		);

		assertEquals (totalLength, pieceDatabase.getStorageDescriptor().getLength());
		assertEquals (3, pieceDatabase.getVerifiedPieceCount());
		assertTrue (pieceDatabase.getPresentPieces().get (0));
		assertFalse (pieceDatabase.getPresentPieces().get (1));
		assertFalse (pieceDatabase.getPresentPieces().get (2));

		pieceDatabase.terminate (true);

	}


	/**
	 * Tests extendData() on an Elastic database from 0 to 1 pieces
	 * @throws Exception
	 */
	@Test
	public void testElasticExtendData0x1() throws Exception {

		int pieceSize = 16384;
		int totalLength = 0;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		byte[] derSignature = null;
		try {
			Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
			dsa.initSign (MockPieceDatabase.mockPrivateKey);
			dsa.update (tree.getView(totalLength).getRootHash());
			derSignature = dsa.sign();
		} catch (GeneralSecurityException e) {
			throw new InternalError (e.getMessage());
		}


		Info info = Info.createSingleFileElastic ("blah", totalLength, pieceSize, tree.getView(totalLength).getRootHash(),
				DSAUtil.derSignatureToP1363Signature (derSignature));
		Storage storage = new MemoryStorage (new StorageDescriptor (pieceSize, totalLength));

		PieceDatabase pieceDatabase = new PieceDatabase (info, MockPieceDatabase.mockPublicKey, storage, null);
		pieceDatabase.start (true);

		assertEquals (0, pieceDatabase.getVerifiedPieceCount());

		totalLength += pieceSize;
		pieceDatabase.extendData (MockPieceDatabase.mockPrivateKey, ByteBuffer.wrap (Util.pseudoRandomBlock (0, pieceSize, pieceSize)));

		assertEquals (totalLength, pieceDatabase.getStorageDescriptor().getLength());
		assertEquals (ByteBuffer.wrap (Util.pseudoRandomBlock (0, pieceSize, pieceSize)), storage.read (0));
		assertEquals (1, pieceDatabase.getVerifiedPieceCount());
		assertTrue (pieceDatabase.getPresentPieces().get (0));

		pieceDatabase.terminate (true);

	}


	/**
	 * Tests extendData() on an Elastic database from 1 to 2 pieces
	 * @throws Exception
	 */
	@Test
	public void testElasticExtendData1x2() throws Exception {

		int pieceSize = 16384;
		int totalLength = 16384;

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));

		byte[] derSignature = null;
		try {
			Signature dsa = Signature.getInstance ("SHAwithDSA", "SUN");
			dsa.initSign (MockPieceDatabase.mockPrivateKey);
			dsa.update (tree.getView(totalLength).getRootHash());
			derSignature = dsa.sign();
		} catch (GeneralSecurityException e) {
			throw new InternalError (e.getMessage());
		}

		Info info = Info.createSingleFileElastic ("blah", totalLength, pieceSize, tree.getView(totalLength).getRootHash(),
				DSAUtil.derSignatureToP1363Signature (derSignature));
		Storage storage = new MemoryStorage (new StorageDescriptor (pieceSize, totalLength));
		storage.write (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, 16384, 16384)));

		PieceDatabase pieceDatabase = new PieceDatabase (info, MockPieceDatabase.mockPublicKey, storage, null);
		pieceDatabase.start (true);

		assertEquals (1, pieceDatabase.getVerifiedPieceCount());
		assertTrue (pieceDatabase.getPresentPieces().get (0));

		totalLength += pieceSize;
		pieceDatabase.extendData (MockPieceDatabase.mockPrivateKey, ByteBuffer.wrap (Util.pseudoRandomBlock (1, pieceSize, pieceSize)));

		assertEquals (totalLength, pieceDatabase.getStorageDescriptor().getLength());
		assertEquals (ByteBuffer.wrap (Util.pseudoRandomBlock (1, pieceSize, pieceSize)), storage.read (1));
		assertEquals (2, pieceDatabase.getVerifiedPieceCount());
		assertTrue (pieceDatabase.getPresentPieces().get (0));
		assertTrue (pieceDatabase.getPresentPieces().get (1));

		pieceDatabase.terminate (true);

	}

}
