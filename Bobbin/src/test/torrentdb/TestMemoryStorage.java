package test.torrentdb;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

import org.itadaki.bobbin.torrentdb.MemoryStorage;
import org.itadaki.bobbin.torrentdb.StorageDescriptor;
import org.junit.Test;

import test.Util;


/**
 * Tests MemoryStorage
 */
public class TestMemoryStorage {

	/**
	 * Tests creating a MemoryStorage that is too large
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testTooLarge() {

		new MemoryStorage (new StorageDescriptor (262144, (long)Integer.MAX_VALUE + 1));

	}


	/**
	 * Tests reading a short final piece
	 * @throws Exception 
	 */
	@Test
	public void testShortFinalPiece1() throws Exception {

		ByteBuffer expectedPiece = ByteBuffer.wrap (new byte[] { 1 });

		MemoryStorage storage = new MemoryStorage (new StorageDescriptor (1024, 1025));

		byte[] writtenPiece = new byte[1024];
		Arrays.fill (writtenPiece, (byte)1);
		storage.write (1, ByteBuffer.wrap (writtenPiece));

		ByteBuffer readPiece = storage.read (1);

		assertEquals (expectedPiece, readPiece);

	}


	/**
	 * Tests reading a short final piece
	 * @throws Exception 
	 */
	@Test
	public void testShortFinalPiece2() throws Exception {

		ByteBuffer expectedPiece = ByteBuffer.wrap (new byte[] { 1 });

		MemoryStorage storage = new MemoryStorage (new StorageDescriptor (1024, 1025));

		ByteBuffer writtenPiece = ByteBuffer.wrap (new byte[] { 1 });
		storage.write (1, writtenPiece);

		ByteBuffer readPiece = storage.read (1);

		assertEquals (expectedPiece, readPiece);

	}


	/**
	 * Tests an output channel writing to 1 block
	 * @throws Exception
	 */
	@Test
	public void testOutputChannel1() throws Exception {

		MemoryStorage storage = new MemoryStorage (new StorageDescriptor (1024, 1024));
		WritableByteChannel outputChannel = storage.openOutputChannel (0, 0);

		ByteBuffer data = ByteBuffer.wrap (Util.pseudoRandomBlock (0, 1024, 1024));
		outputChannel.write (data);

		data.rewind();
		assertEquals (storage.read (0), data);

	}


	/**
	 * Tests an output channel writing to 1.5 blocks
	 * @throws Exception
	 */
	@Test
	public void testOutputChannel1p5() throws Exception {

		MemoryStorage storage = new MemoryStorage (new StorageDescriptor (1024, 1524));
		WritableByteChannel outputChannel = storage.openOutputChannel (0, 0);

		ByteBuffer data = ByteBuffer.wrap (Util.pseudoRandomBlock (0, 1524, 1524));
		outputChannel.write (data);

		data.rewind();
		assertEquals (storage.read (0), data.asReadOnlyBuffer().limit(1024));
		assertEquals (storage.read (1), data.asReadOnlyBuffer().position(1024).limit(1524));

	}


	/**
	 * Tests an output channel writing from blocks 0.5 to 1.5
	 * @throws Exception
	 */
	@Test
	public void testOutputChannel0p5x1p5() throws Exception {

		MemoryStorage storage = new MemoryStorage (new StorageDescriptor (1024, 1524));
		WritableByteChannel outputChannel = storage.openOutputChannel (0, 500);

		ByteBuffer data = ByteBuffer.allocate (1524);
		data.position (500);
		data.put (Util.pseudoRandomBlock (0, 1024, 1024));
		data.position (500);
		outputChannel.write (data);

		data.rewind();
		assertEquals (storage.read (0), data.asReadOnlyBuffer().limit (1024));
		assertEquals (storage.read (1), data.asReadOnlyBuffer().position(1024).limit (1524));

	}


	/**
	 * Tests extending
	 *
	 * @throws Exception
	 */
	@Test
	public void testExtend() throws Exception {

		StorageDescriptor expectedDescriptor1 = new StorageDescriptor (1024, 1024);
		StorageDescriptor expectedDescriptor2 = new StorageDescriptor (1024, 2048);
		MemoryStorage storage = new MemoryStorage (new StorageDescriptor (1024, 1024));

		assertEquals (expectedDescriptor1, storage.getDescriptor());

		storage.extend (2048);

		ByteBuffer piece = ByteBuffer.wrap (Util.pseudoRandomBlock (1, 1024, 1024));
		storage.write (1, piece);

		assertEquals (expectedDescriptor2, storage.getDescriptor());

	}


}
