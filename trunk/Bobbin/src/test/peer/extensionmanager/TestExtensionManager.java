package test.peer.extensionmanager;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.ExtensiblePeer;
import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.extensionmanager.ExtensionHandler;
import org.itadaki.bobbin.peer.extensionmanager.ExtensionManager;
import org.junit.Test;

import test.peer.MockManageablePeer;


/**
 * Tests ExtensionManager
 */
public class TestExtensionManager {

	/**
	 * Tests the basic operation of an extension
	 */
	@Test
	public void testSimpleExtension() {

		ExtensionManager manager = new ExtensionManager();
		final int[] sequence = new int[1];
		ExtensionHandler handler = new ExtensionHandler() {
			public void peerCoordinatorCompleted() {
				fail();
			}
			public void peerDeregistered (ManageablePeer peer) {
				assertEquals (4, sequence[0]++);
			}
			public void peerRegistered (ManageablePeer peer) {
				assertEquals (0, sequence[0]++);
			}
			public String getIdentifier() {
				return "bl_ah";
			}
			public void peerDisabledExtension (ExtensiblePeer peer) {
				fail();
			}
			public void peerEnabledExtension (ExtensiblePeer peer, BDictionary extra) {
				assertEquals (2, sequence[0]++);
			}
			public void extensionMessage (ExtensiblePeer peer, byte[] messageData) {
				assertEquals (3, sequence[0]++);
				assertArrayEquals (new byte[]{ 1, 2, 3, 4 }, messageData);
			}
		};
		manager.registerExtension (handler);

		ManageablePeer peer = new MockManageablePeer() {
			@Override
			public void sendExtensionHandshake (Set<String> extensionsAdded, Set<String> extensionsRemoved, BDictionary extra) {
				assertEquals (1, sequence[0]++);
			}
		};

		manager.peerRegistered (peer);
		manager.offerExtensionsToPeer (peer);
		manager.enableDisablePeerExtensions (peer, new HashSet<String> (Arrays.asList ("bl_ah")), null, new BDictionary());
		manager.processExtensionMessage (peer, "bl_ah", new byte[] { 1, 2,3, 4 } );
		manager.peerDeregistered (peer);

		assertEquals (5, sequence[0]);

	}

	// TODO Test - disable / enable an extension

}
