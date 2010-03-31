/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import test.bencode.TestBDecoder;
import test.bencode.TestBEncoder;
import test.bencode.TestBBinary;
import test.bencode.TestBInteger;
import test.bencode.TestBList;
import test.bencode.TestBDictionary;
import test.bencode.TestBValue;
import test.connectionmanager.TestConnectionManager;
import test.peer.TestPeerCoordinator;
import test.peer.TestPeerHandler;
import test.peer.TestPeerID;
import test.peer.TestPeerOutboundQueue;
import test.peer.TestTorrentManager;
import test.peer.TestTorrentSetController;
import test.peer.chokingmanager.TestDefaultChokingManager;
import test.peer.extensionmanager.TestExtensionManager;
import test.peer.protocol.TestPeerProtocolBuilder;
import test.peer.protocol.TestPeerProtocolNegotiator;
import test.peer.protocol.TestPeerProtocolParser;
import test.peer.requestmanager.TestDefaultRequestManager;
import test.statemachine.TestStateMachine;
import test.torrentdb.TestBlockDescriptor;
import test.torrentdb.TestFileMetadata;
import test.torrentdb.TestFileMetadataProvider;
import test.torrentdb.TestFileStorage;
import test.torrentdb.TestInfoBuilder;
import test.torrentdb.TestMemoryStorage;
import test.torrentdb.TestPiece;
import test.torrentdb.TestPieceDatabase;
import test.torrentdb.TestInfoHash;
import test.torrentdb.TestMetaInfo;
import test.torrentdb.TestStorageDescriptor;
import test.tracker.TestHTTPTracker;
import test.tracker.TestHTTPRequestParser;
import test.tracker.TestTracker;
import test.trackerclient.TestHTTPRequestHandler;
import test.trackerclient.TestHTTPResponseParser;
import test.trackerclient.TestTrackerClient;
import test.util.TestBitField;
import test.util.TestCharsetUtil;
import test.util.TestDSAUtil;
import test.util.counter.TestPeriod;
import test.util.counter.TestPeriodicCounter;
import test.util.counter.TestStatisticCounter;
import test.util.counter.TestTemporalCounter;
import test.util.elastictree.TestElasticTree;


/**
 * Test suite that runs all tests
 */
@RunWith(Suite.class)
@SuiteClasses({
	TestBDecoder.class,
	TestBEncoder.class,
	TestBValue.class,
	TestBBinary.class,
	TestBInteger.class,
	TestBList.class,
	TestBDictionary.class,
	TestHTTPTracker.class,
	TestHTTPRequestParser.class,
	TestTracker.class,
	TestPieceDatabase.class,
	TestMetaInfo.class,
	TestFileStorage.class,
	TestBitField.class,
	TestPeerProtocolBuilder.class,
	TestPeerProtocolParser.class,
	TestPeerHandler.class,
	TestTorrentManager.class,
	TestDefaultChokingManager.class,
	TestConnectionManager.class,
	TestPiece.class,
	TestDefaultRequestManager.class,
	TestPeerOutboundQueue.class,
	TestHTTPResponseParser.class,
	TestHTTPRequestHandler.class,
	TestTrackerClient.class,
	TestPeriodicCounter.class,
	TestTemporalCounter.class,
	TestStatisticCounter.class,
	TestStateMachine.class,
	TestPeerCoordinator.class,
	TestTorrentSetController.class,
	TestBlockDescriptor.class,
	TestPeerID.class,
	TestInfoHash.class,
	TestCharsetUtil.class,
	TestPeriod.class,
	TestInfoBuilder.class,
	TestMemoryStorage.class,
	TestFileMetadata.class,
	TestFileMetadataProvider.class,
	TestExtensionManager.class,
	TestElasticTree.class,
	TestDSAUtil.class,
	TestStorageDescriptor.class,
	TestPeerProtocolNegotiator.class
})
public class AllTests {
	// This space left blank
}
