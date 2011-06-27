/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.peer.extensionmanager;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.peer.ManageablePeer;
import org.itadaki.bobbin.peer.extensionmanager.ExtensionHandler;
import org.itadaki.bobbin.peer.extensionmanager.ExtensionManager;
import org.junit.Test;
import org.mockito.InOrder;


/**
 * Tests ExtensionManager
 */
public class TestExtensionManager {

	/**
	 * Tests the basic operation of an extension
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSimpleExtension() {

		// Given
		ExtensionManager manager = new ExtensionManager();
		ExtensionHandler handler = mock (ExtensionHandler.class);
		when (handler.getIdentifier()).thenReturn ("bl_ah");
		ManageablePeer peer = mock (ManageablePeer.class);

		// When
		manager.registerExtension (handler);
		manager.peerRegistered (peer);
		manager.offerExtensionsToPeer (peer);
		manager.enableDisablePeerExtensions (peer, new HashSet<String> (Arrays.asList ("bl_ah")), null, new BDictionary());
		manager.processExtensionMessage (peer, "bl_ah", new byte[] { 1, 2,3, 4 } );
		manager.peerDeregistered (peer);

		// Then
		InOrder sequence = inOrder (handler, peer);
		sequence.verify(handler).peerRegistered (peer);
		sequence.verify(peer).sendExtensionHandshake (any (Map.class), any (Set.class), any (BDictionary.class));
		sequence.verify(handler).peerEnabledExtension (eq (peer), any (BDictionary.class));
		sequence.verify(handler).extensionMessage (eq (peer), eq (new byte[]{ 1, 2, 3, 4 }));
		sequence.verify(handler).peerDeregistered (peer);

	}

	// TODO Test - disable / enable an extension

}
