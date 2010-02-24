package test.peer;

import static org.junit.Assert.*;

import org.itadaki.bobbin.peer.PeerID;
import org.junit.Test;



/**
 * Tests PeerID
 */
public class TestPeerID {

	/**
	 * Tests PeerID
	 */
	@Test
	public void testPeerID() {

		PeerID peerID = new PeerID();

		assertEquals (20, peerID.getBytes().length);

	}


	/**
	 * Tests PeerID (byte[])
	 */
	@Test
	public void testPeerIDBytes() {

		PeerID peerID = new PeerID (new byte[20]);

		assertEquals (20, peerID.getBytes().length);

	}


	/**
	 * Tests PeerID (byte[]) with an invalid length
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testPeerIDBytesInvalid() {

		new PeerID (new byte[19]);

	}

}
