package test.peer;

import static org.junit.Assert.*;

import java.util.NavigableMap;
import java.util.Set;

import org.itadaki.bobbin.peer.PeerID;
import org.itadaki.bobbin.peer.PeerState;
import org.itadaki.bobbin.torrentdb.StorageDescriptor;
import org.itadaki.bobbin.torrentdb.ViewSignature;
import org.itadaki.bobbin.util.BitField;


/**
 * A mock PeerState
 */
public class MockPeerState extends PeerState {

	@Override
	public long getLastDataReceivedTime() {
		fail();
		return 0;
	}

	@Override
	public BitField getRemoteBitField() {
		fail();
		return null;
	}

	@Override
	public Set<String> getRemoteExtensions() {
		fail();
		return null;
	}

	@Override
	public PeerID getRemotePeerID() {
		fail();
		return null;
	}

	@Override
	public NavigableMap<Long, ViewSignature> getRemotePeerSignatures() {
		fail();
		return null;
	}

	@Override
	public StorageDescriptor getRemoteView() {
		fail();
		return null;
	}

	@Override
	public boolean getTheyAreChoking() {
		fail();
		return false;
	}

	@Override
	public boolean getTheyAreInterested() {
		fail();
		return false;
	}

	@Override
	public boolean getWeAreChoking() {
		fail();
		return false;
	}

	@Override
	public boolean getWeAreInterested() {
		fail();
		return false;
	}

	@Override
	public boolean isExtensionProtocolEnabled() {
		fail();
		return false;
	}

	@Override
	public boolean isFastExtensionEnabled() {
		fail();
		return false;
	}


}
