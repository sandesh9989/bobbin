/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer;



import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.PeerOutboundQueue;
import org.itadaki.bobbin.peer.protocol.PeerProtocolBuilder;
import org.itadaki.bobbin.peer.protocol.PeerProtocolConstants;
import org.itadaki.bobbin.torrentdb.BlockDescriptor;
import org.itadaki.bobbin.torrentdb.InfoHash;
import org.itadaki.bobbin.torrentdb.Piece;
import org.itadaki.bobbin.torrentdb.PieceDatabase;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;
import org.itadaki.bobbin.util.CharsetUtil;
import org.itadaki.bobbin.util.counter.StatisticCounter;
import org.itadaki.bobbin.util.elastictree.ElasticTree;
import org.junit.Test;

import test.Util;
import test.torrentdb.MockPieceDatabase;


/**
 * Tests PeerOutboundQueue
 */
public class TestPeerOutboundQueue {

	/**
	 * Tests that a handshake is written through the PeerOutboundQueue's Connection
	 * @throws IOException
	 */
	@Test
	public void testHandshake() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a handshake is written through the PeerOutboundQueue's Connection in 1 byte chunks
	 * @throws IOException
	 */
	@Test
	public void testHandshakeSplit() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);

		for (int i = 0; i < 68; i++) {
			connection.mockSetPermittedWriteBytes (1);
			assertEquals (1, peerOutboundQueue.sendData());
		}

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a bitfield message is written through the PeerOutboundQueue's Connection
	 * @throws IOException
	 */
	@Test
	public void testBitfield() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BitField bitField = new BitField (10);
		bitField.set (9);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendBitfieldMessage (bitField);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.bitfieldMessage (bitField));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a bitfield message is written through the PeerOutboundQueue's Connection in 1 byte
	 * chunks
	 * @throws IOException
	 */
	@Test
	public void testBitfieldSplit() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BitField bitField = new BitField (10);
		bitField.set (9);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendBitfieldMessage (bitField);

		for (int i = 0; i < 75; i++) {
			connection.mockSetPermittedWriteBytes (1);
			assertEquals (1, peerOutboundQueue.sendData());
		}

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.bitfieldMessage (bitField));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a choke message is written through the PeerOutboundQueue's Connection
	 * @throws IOException
	 */
	@Test
	public void testChoke() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendChokeMessage (true);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.chokeMessage());
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a choke message is written through the PeerOutboundQueue's Connection in 1 byte
	 * chunks
	 * @throws IOException
	 */
	@Test
	public void testChokeSplit() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendChokeMessage (true);

		for (int i = 0; i < 73; i++) {
			connection.mockSetPermittedWriteBytes (1);
			assertEquals (1, peerOutboundQueue.sendData());
		}

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.chokeMessage());
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that an interested message is written through the PeerOutboundQueue's Connection
	 * @throws IOException
	 */
	@Test
	public void testInterested() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendInterestedMessage (true);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that an interested message is written through the PeerOutboundQueue's Connection in 1
	 * byte chunks
	 * @throws IOException
	 */
	@Test
	public void testInterestedSplit() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendInterestedMessage (true);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		for (int i = 0; i < 73; i++) {
			connection.mockSetPermittedWriteBytes (1);
			assertEquals (1, peerOutboundQueue.sendData());
		}

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.interestedMessage());
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a piece message is written through the PeerOutboundQueue's Connection
	 * @throws Exception 
	 */
	@Test
	public void testPiece() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("11", 65536);
		pieceDatabase.start (true);
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);
		byte[] expectedBlockData = new byte[16384];
		System.arraycopy (Util.pseudoRandomBlock (1, 65536, 65536), 32768, expectedBlockData, 0, 16384);


		MockConnection connection = new MockConnection();

		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendPieceMessage (descriptor);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.pieceMessage (descriptor, ByteBuffer.wrap (expectedBlockData)));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a piece message is written through the PeerOutboundQueue's Connection in 1 byte
	 * chunks
	 * @throws Exception 
	 */
	@Test
	public void testPieceSplit() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("11", 65536);
		pieceDatabase.start (true);
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);
		byte[] expectedBlockData = new byte[16384];
		System.arraycopy (Util.pseudoRandomBlock (1, 65536, 65536), 32768, expectedBlockData, 0, 16384);


		MockConnection connection = new MockConnection();

		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendPieceMessage (descriptor);

		for (int i = 0; i < 68 + 16397; i++) {
			connection.mockSetPermittedWriteBytes (1);
			assertEquals (1, peerOutboundQueue.sendData());
		}

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.pieceMessage (descriptor, ByteBuffer.wrap (expectedBlockData)));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a request message is written through the PeerOutboundQueue's Connection
	 * @throws IOException
	 */
	@Test
	public void testRequest() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendRequestMessage (descriptor);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a request message is written through the PeerOutboundQueue's Connection in 1 byte
	 * chunks
	 * @throws IOException
	 */
	@Test
	public void testRequestSplit() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendRequestMessage (descriptor);

		for (int i = 0; i < 68 + 17; i++) {
			connection.mockSetPermittedWriteBytes (1);
			assertEquals (1, peerOutboundQueue.sendData());
		}

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a cancel message is written through the PeerOutboundQueue's Connection
	 * @throws IOException
	 */
	@Test
	public void testCancel() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendRequestMessage (descriptor);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		peerOutboundQueue.sendCancelMessage (descriptor, false);

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectOutput (PeerProtocolBuilder.cancelMessage (descriptor));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a cancel message is written through the PeerOutboundQueue's Connection in 1 byte
	 * chunks
	 * @throws IOException
	 */
	@Test
	public void testCancelSplit() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendRequestMessage (descriptor);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		peerOutboundQueue.sendCancelMessage (descriptor, false);

		for (int i = 0; i < 17; i++) {
			connection.mockSetPermittedWriteBytes (1);
			assertEquals (1, peerOutboundQueue.sendData());
		}

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectOutput (PeerProtocolBuilder.cancelMessage (descriptor));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a have message is written through the PeerOutboundQueue's Connection
	 * @throws IOException
	 */
	@Test
	public void testHave() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendHaveMessage (1234);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveMessage (1234));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a have message is written through the PeerOutboundQueue's Connection in 1 byte chunks
	 * @throws IOException
	 */
	@Test
	public void testHaveSplit() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendHaveMessage (1234);

		for (int i = 0; i < 68 + 9; i++) {
			connection.mockSetPermittedWriteBytes (1);
			assertEquals (1, peerOutboundQueue.sendData());
		}

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveMessage (1234));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a Have All message is written through the PeerOutboundQueue's Connection
	 * @throws IOException
	 */
	@Test
	public void testHaveAll() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, false, infoHash, peerID);
		peerOutboundQueue.sendHaveAllMessage();

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveAllMessage());
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a Have All message is written through the PeerOutboundQueue's Connection
	 * @throws IOException
	 */
	@Test
	public void testHaveNone() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, false, infoHash, peerID);
		peerOutboundQueue.sendHaveNoneMessage();

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a Reject Request message is written through the PeerOutboundQueue's Connection
	 * @throws IOException
	 */
	@Test
	public void testRejectRequest() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);
		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, false, infoHash, peerID);
		peerOutboundQueue.sendHaveNoneMessage();
		peerOutboundQueue.sendRejectRequestMessage (descriptor);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		connection.mockExpectOutput (PeerProtocolBuilder.rejectRequestMessage (descriptor));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a piece message is cancelled unsent by a choke message
	 * @throws Exception 
	 */
	@Test
	public void testChokeDiscardsPiece() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("11", 65536);
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);
		byte[] expectedBlockData = new byte[16384];
		System.arraycopy (Util.pseudoRandomBlock (1, 65536, 65536), 32768, expectedBlockData, 0, 16384);


		MockConnection connection = new MockConnection();

		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendPieceMessage (descriptor);
		peerOutboundQueue.sendChokeMessage (true);

		assertTrue (connection.mockIsWriteEnabled());

		// Will explode if pieceMessage() is called
		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.chokeMessage());
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a not interested message is cancelled unsent by an interested message
	 * @throws IOException
	 */
	@Test
	public void testNotInterestedCancelsInterested() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendInterestedMessage (true);
		peerOutboundQueue.sendInterestedMessage (false);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a not interested message is cancelled unsent by an interested message
	 * @throws IOException
	 */
	@Test
	public void testInterestedCancelsNotInterested() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendInterestedMessage (false);
		peerOutboundQueue.sendInterestedMessage (true);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a queue limit is applied to inbound requests
	 * @throws Exception 
	 */
	@Test
	public void testPieceQueueLimit() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("11", 65536);
		pieceDatabase.start (true);
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		for (int i = 0; i < PeerProtocolConstants.MAXIMUM_INBOUND_REQUESTS + 1; i++) {
			peerOutboundQueue.sendPieceMessage (descriptor);
		}

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		for (int i = 0; i < PeerProtocolConstants.MAXIMUM_INBOUND_REQUESTS; i++) {
			connection.mockExpectOutput (PeerProtocolBuilder.pieceMessage (
					descriptor,
					pieceDatabase.readPiece (descriptor.getPieceNumber()).getBlock (descriptor)
			));
		}
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a request is tracked once only
	 * @throws IOException
	 */
	@Test
	public void testRequestTracked() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		connection.mockExpectNoMoreOutput();
		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, false, infoHash, peerID);
		peerOutboundQueue.sendHaveNoneMessage();
		peerOutboundQueue.sendRequestMessage (descriptor);

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectNoMoreOutput();
		assertTrue (peerOutboundQueue.rejectReceived (descriptor));
		assertFalse (peerOutboundQueue.rejectReceived (descriptor));

	}


	/**
	 * Tests that a request message is cancelled unsent by a cancel message
	 * @throws IOException
	 */
	@Test
	public void testCancelCancelsRequest() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendRequestMessage (descriptor);
		peerOutboundQueue.sendCancelMessage (descriptor, false);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a cancelled request is tracked once only
	 * @throws IOException
	 */
	@Test
	public void testCancelTracked() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, false, infoHash, peerID);
		peerOutboundQueue.sendHaveNoneMessage();
		peerOutboundQueue.sendRequestMessage (descriptor);

		peerOutboundQueue.sendData();

		peerOutboundQueue.sendCancelMessage (descriptor, true);

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectOutput (PeerProtocolBuilder.cancelMessage (descriptor));
		connection.mockExpectNoMoreOutput();
		assertTrue (peerOutboundQueue.rejectReceived (descriptor));
		assertFalse (peerOutboundQueue.rejectReceived (descriptor));

	}


	/**
	 * Tests that a cancelled request completed by the receipt of a block is tracked once only
	 * @throws IOException
	 */
	@Test
	public void testCancelBlockReceived() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, false, infoHash, peerID);
		peerOutboundQueue.sendHaveNoneMessage();
		peerOutboundQueue.sendRequestMessage (descriptor);

		peerOutboundQueue.sendData();

		peerOutboundQueue.sendCancelMessage (descriptor, true);

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectOutput (PeerProtocolBuilder.cancelMessage (descriptor));
		connection.mockExpectNoMoreOutput();
		assertTrue (peerOutboundQueue.requestReceived (descriptor));
		assertFalse (peerOutboundQueue.requestReceived (descriptor));

	}


	/**
	 * Tests that a request is requeued by requeueAllRequestMessages()
	 *
	 * @throws IOException
	 */
	@Test
	public void testRequeueAllRequestMessages() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendRequestMessage (descriptor);

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectNoMoreOutput();

		peerOutboundQueue.requeueAllRequestMessages();

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests that a piece message is cancelled by a call to discardPieceMessage()
	 * @throws Exception
	 */
	@Test
	public void testDiscardPieceMessage() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("11", 65536);
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendPieceMessage (descriptor);
		peerOutboundQueue.discardPieceMessage (descriptor);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests hasOutstandingRequests() with no queued or sent requests
	 *
	 * @throws IOException
	 */
	@Test
	public void testhasOutstandingRequestsNone() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectNoMoreOutput();
		assertFalse (peerOutboundQueue.hasOutstandingRequests());

	}


	/**
	 * Tests hasOutstandingRequests() with a queued request
	 *
	 * @throws IOException
	 */
	@Test
	public void testhasOutstandingRequestsQueued() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);

		peerOutboundQueue.sendData();

		peerOutboundQueue.sendRequestMessage (descriptor);

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectNoMoreOutput();
		assertTrue (peerOutboundQueue.hasOutstandingRequests());

	}


	/**
	 * Tests hasOutstandingRequests() with a sent request
	 *
	 * @throws IOException
	 */
	@Test
	public void testhasOutstandingRequestsSent() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);
		peerOutboundQueue.sendRequestMessage (descriptor);

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectNoMoreOutput();
		assertTrue (peerOutboundQueue.hasOutstandingRequests());

	}


	/**
	 * Tests getUnsentPieceCount()
	 * @throws Exception
	 */
	@Test
	public void testUnsentPieceCount() throws Exception {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		PieceDatabase pieceDatabase = MockPieceDatabase.create ("11", 65536);
		pieceDatabase.start (true);
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);

		assertEquals (0, peerOutboundQueue.getUnsentPieceCount());

		peerOutboundQueue.sendPieceMessage (descriptor);

		connection.mockExpectNoMoreOutput();
		assertEquals (1, peerOutboundQueue.getUnsentPieceCount());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.pieceMessage (
				descriptor,
				pieceDatabase.readPiece (descriptor.getPieceNumber()).getBlock (descriptor)
		));
		connection.mockExpectNoMoreOutput();
		assertEquals (0, peerOutboundQueue.getUnsentPieceCount());

	}


	/**
	 * Tests getRequestsNeeded()
	 * @throws IOException
	 */
	@Test
	public void testRequestsNeeded() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);

		assertEquals (PeerProtocolConstants.MAXIMUM_OUTBOUND_REQUESTS, peerOutboundQueue.getRequestsNeeded());

		peerOutboundQueue.sendRequestMessage (descriptor);

		connection.mockExpectNoMoreOutput();
		assertEquals (PeerProtocolConstants.MAXIMUM_OUTBOUND_REQUESTS - 1, peerOutboundQueue.getRequestsNeeded());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectNoMoreOutput();
		assertEquals (PeerProtocolConstants.MAXIMUM_OUTBOUND_REQUESTS - 1, peerOutboundQueue.getRequestsNeeded());

	}


	/**
	 * Tests requestReceived()
	 * @throws IOException
	 */
	@Test
	public void testRequestReceived() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		BlockDescriptor descriptor = new BlockDescriptor (1, 32768, 16384);


		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);
		peerOutboundQueue.setRequestsPlugged (false);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (false, false, infoHash, peerID);

		assertEquals (PeerProtocolConstants.MAXIMUM_OUTBOUND_REQUESTS, peerOutboundQueue.getRequestsNeeded());

		peerOutboundQueue.sendRequestMessage (descriptor);

		connection.mockExpectNoMoreOutput();
		assertEquals (PeerProtocolConstants.MAXIMUM_OUTBOUND_REQUESTS - 1, peerOutboundQueue.getRequestsNeeded());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (false, false, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.requestMessage (descriptor));
		connection.mockExpectNoMoreOutput();
		assertEquals (PeerProtocolConstants.MAXIMUM_OUTBOUND_REQUESTS - 1, peerOutboundQueue.getRequestsNeeded());

		peerOutboundQueue.requestReceived (descriptor);

		assertEquals (PeerProtocolConstants.MAXIMUM_OUTBOUND_REQUESTS, peerOutboundQueue.getRequestsNeeded());

	}


	/**
	 * Tests sendExtensionHandshake() adding an extension
	 * @throws IOException
	 */
	@Test
	public void testExtensionHandshakeAdd() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		Set<String> extensionsAdded = new TreeSet<String>();
		extensionsAdded.add ("bl_ah");

		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, true, infoHash, peerID);
		peerOutboundQueue.sendExtensionHandshake (extensionsAdded, null, null);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, peerID));
		Map<String,Integer> expectedExtensions = new TreeMap<String,Integer>();
		expectedExtensions.put ("bl_ah", 3);
		connection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (expectedExtensions, null));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests sendExtensionHandshake() removing an extension
	 * @throws IOException
	 */
	@Test
	public void testExtensionHandshakeRemove() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		Set<String> extensionsAdded = new TreeSet<String>();
		extensionsAdded.add ("bl_ah");

		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, true, infoHash, peerID);
		peerOutboundQueue.sendExtensionHandshake (extensionsAdded, null, null);
		peerOutboundQueue.sendExtensionHandshake (null, extensionsAdded, null);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, peerID));
		Map<String,Integer> expectedExtensions = new TreeMap<String,Integer>();
		expectedExtensions.put ("bl_ah", 3);
		connection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (expectedExtensions, null));
		Map<String,Integer> expectedExtensions2 = new TreeMap<String,Integer>();
		expectedExtensions2.put ("bl_ah", 0);
		connection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (expectedExtensions2, null));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests sendExtensionMessage()
	 * @throws IOException
	 */
	@Test
	public void testExtensionMessage() throws IOException {

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		Set<String> extensionsAdded = new TreeSet<String>();
		extensionsAdded.add ("bl_ah");

		MockConnection connection = new MockConnection();

		PieceDatabase pieceDatabase = null;
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, true, infoHash, peerID);
		peerOutboundQueue.sendExtensionHandshake (extensionsAdded, null, null);
		peerOutboundQueue.sendExtensionMessage ("bl_ah", ByteBuffer.wrap (new byte[] { 1, 2, 3, 4 }));

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, peerID));
		Map<String,Integer> expectedExtensions = new TreeMap<String,Integer>();
		expectedExtensions.put ("bl_ah", 3);
		connection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (expectedExtensions, null));
		connection.mockExpectOutput (PeerProtocolBuilder.extensionMessage (3, ByteBuffer.wrap (new byte[] { 1, 2, 3, 4})));
		connection.mockExpectNoMoreOutput();

	}


	/**
	 * Tests sendPieceMessage() with the Merkle extension enabled
	 * @throws Exception
	 */
	@Test
	public void testMerklePieceMessage() throws Exception {

		int pieceSize = 1024;
		int totalLength = 1024;
		BlockDescriptor blockDescriptor = new BlockDescriptor (0, 0, 1024);

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		Set<String> extensionsAdded = new TreeSet<String>();
		extensionsAdded.add (PeerProtocolConstants.EXTENSION_MERKLE);

		MockConnection connection = new MockConnection();

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createEmptyMerkle (pieceSize, totalLength, tree.getView(totalLength).getRootHash());
		pieceDatabase.start (true);
		pieceDatabase.writePiece (new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, pieceSize, pieceSize)), tree.getHashChain (0, pieceSize)));
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, true, infoHash, peerID);
		peerOutboundQueue.sendHaveNoneMessage();
		peerOutboundQueue.sendExtensionHandshake (extensionsAdded, null, null);
		peerOutboundQueue.sendPieceMessage (blockDescriptor);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();

		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		Map<String,Integer> expectedExtensions = new TreeMap<String,Integer>();
		expectedExtensions.put (PeerProtocolConstants.EXTENSION_MERKLE, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_MERKLE);
		connection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (expectedExtensions, null));
		connection.mockExpectOutput (PeerProtocolBuilder.merklePieceMessage (blockDescriptor, tree.getHashChain(0, pieceSize).getHashes(), ByteBuffer.wrap (Util.pseudoRandomBlock (0, pieceSize, pieceSize))));
		connection.mockExpectNoMoreOutput();

		pieceDatabase.terminate (true);

	}


	/**
	 * Tests sendELasticSignatureMessage() with the Elastic extension enabled
	 * @throws Exception
	 */
	@Test
	public void testElasticSignatureMessage() throws Exception {

		int pieceSize = 1024;
		int totalLength = 1024;
		long viewLength = 1024;

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		Set<String> extensionsAdded = new TreeSet<String>();
		extensionsAdded.add (PeerProtocolConstants.EXTENSION_ELASTIC);

		MockConnection connection = new MockConnection();

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createEmptyMerkle (pieceSize, totalLength, tree.getView(totalLength).getRootHash());
		pieceDatabase.start (true);
		pieceDatabase.writePiece (new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, pieceSize, pieceSize)), tree.getHashChain (0, pieceSize)));
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		ViewSignature viewSignature = new ViewSignature (viewLength, ByteBuffer.wrap (tree.getView (viewLength).getRootHash()), ByteBuffer.allocate (40));

		peerOutboundQueue.sendHandshake (true, true, infoHash, peerID);
		peerOutboundQueue.sendHaveNoneMessage();
		peerOutboundQueue.sendExtensionHandshake (extensionsAdded, null, null);
		peerOutboundQueue.sendElasticSignatureMessage (viewSignature);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();
		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		Map<String,Integer> expectedExtensions = new TreeMap<String,Integer>();
		expectedExtensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC);
		connection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (expectedExtensions, null));
		connection.mockExpectOutput (PeerProtocolBuilder.elasticSignatureMessage (viewSignature));
		connection.mockExpectNoMoreOutput();

		pieceDatabase.terminate (true);

	}


	/**
	 * Tests sendPieceMessage() with the Elastic extension enabled
	 * @throws Exception
	 */
	@Test
	public void testElasticPieceMessage() throws Exception {

		int pieceSize = 1024;
		int totalLength = 1024;
		BlockDescriptor blockDescriptor = new BlockDescriptor (0, 0, 1024);
		long viewLength = 1024;

		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		Set<String> extensionsAdded = new TreeSet<String>();
		extensionsAdded.add (PeerProtocolConstants.EXTENSION_ELASTIC);

		MockConnection connection = new MockConnection();

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createElastic ("0", totalLength);
		pieceDatabase.start (true);
		pieceDatabase.writePiece (new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, pieceSize, pieceSize)), tree.getHashChain (0, pieceSize)));
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, true, infoHash, peerID);
		peerOutboundQueue.sendHaveNoneMessage();
		peerOutboundQueue.sendExtensionHandshake (extensionsAdded, null, null);
		peerOutboundQueue.sendPieceMessage (blockDescriptor);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();
		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		Map<String,Integer> expectedExtensions = new TreeMap<String,Integer>();
		expectedExtensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC);
		connection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (expectedExtensions, null));
		connection.mockExpectOutput (PeerProtocolBuilder.elasticPieceMessage (blockDescriptor, viewLength, tree.getHashChain(0, pieceSize).getHashes(), ByteBuffer.wrap (Util.pseudoRandomBlock (0, pieceSize, pieceSize))));
		connection.mockExpectNoMoreOutput();

		pieceDatabase.terminate (true);

	}


	/**
	 * Tests sendELasticBitfieldMessage() with the Elastic extension enabled
	 * @throws Exception
	 */
	@Test
	public void testElasticBitfieldMessage() throws Exception {

		int pieceSize = 1024;
		int totalLength = 1024;
		BitField bitfield = new BitField (new byte[] { (byte)0xff, 0x00, (byte)0xee, (byte)0xf0 }, 28);


		InfoHash infoHash = new InfoHash ("qwertyuiop1234567890".getBytes (CharsetUtil.ASCII));
		PeerID peerID = new PeerID ("0987654321asdfghjkl;".getBytes (CharsetUtil.ASCII));
		Set<String> extensionsAdded = new TreeSet<String>();
		extensionsAdded.add (PeerProtocolConstants.EXTENSION_ELASTIC);

		MockConnection connection = new MockConnection();

		ElasticTree tree = ElasticTree.buildFromLeaves (pieceSize, totalLength, Util.pseudoRandomBlockHashes (pieceSize, totalLength));
		PieceDatabase pieceDatabase = MockPieceDatabase.createEmptyMerkle (pieceSize, totalLength, tree.getView(totalLength).getRootHash());
		pieceDatabase.start (true);
		pieceDatabase.writePiece (new Piece (0, ByteBuffer.wrap (Util.pseudoRandomBlock (0, pieceSize, pieceSize)), tree.getHashChain (0, pieceSize)));
		StatisticCounter sentBlockCounter = new StatisticCounter();
		PeerOutboundQueue peerOutboundQueue = new PeerOutboundQueue (connection, pieceDatabase, sentBlockCounter);

		assertFalse (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendHandshake (true, true, infoHash, peerID);
		peerOutboundQueue.sendHaveNoneMessage();
		peerOutboundQueue.sendExtensionHandshake (extensionsAdded, null, null);
		peerOutboundQueue.sendElasticBitfieldMessage (bitfield);

		connection.mockExpectNoMoreOutput();
		assertTrue (connection.mockIsWriteEnabled());

		peerOutboundQueue.sendData();
		connection.mockExpectOutput (PeerProtocolBuilder.handshake (true, true, infoHash, peerID));
		connection.mockExpectOutput (PeerProtocolBuilder.haveNoneMessage());
		Map<String,Integer> expectedExtensions = new TreeMap<String,Integer>();
		expectedExtensions.put (PeerProtocolConstants.EXTENSION_ELASTIC, (int)PeerProtocolConstants.EXTENDED_MESSAGE_TYPE_ELASTIC);
		connection.mockExpectOutput (PeerProtocolBuilder.extensionHandshakeMessage (expectedExtensions, null));
		connection.mockExpectOutput (PeerProtocolBuilder.elasticBitfieldMessage (bitfield));
		connection.mockExpectNoMoreOutput();

		pieceDatabase.terminate (true);

	}

}
