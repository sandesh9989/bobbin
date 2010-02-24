package org.itadaki.bobbin.torrentdb;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BList;
import org.itadaki.bobbin.bencode.BValue;
import org.itadaki.bobbin.bencode.InvalidEncodingException;
import org.itadaki.bobbin.util.DSAUtil;



/**
 * Immutably represents the content of a torrent file<br>
 * <br>
 * <b>Thread safety:</b> All public methods of this class are thread safe
 */
public final class MetaInfo {

	/**
	 * The torrent file's dictionary
	 */
	private final BDictionary dictionary;

	/**
	 * Info dictionary 
	 */
	private final Info info;

	/**
	 * The DSA public key used to sign the Info dictionary
	 */
	private PublicKey publicKey;


	/**
	 * Helper method for dictionary validation. Throws {@link InvalidEncodingException}
	 * with a supplied message if the supplied boolean value is false
	 *
	 * @param statement if false, {@link InvalidEncodingException} is thrown
	 * @param message a message to include in the thrown exception
	 * @throws InvalidEncodingException
	 */
	private static void assertTrue (boolean statement, String message) throws InvalidEncodingException {

		if (!statement) {
			throw new InvalidEncodingException ("Invalid torrent dictionary: " + message);
		}

	}


	/**
	 * Returns a copy of the dictionary representation of the MetaInfo
	 *
	 * @return A copy of the dictionary representation of the MetaInfo
	 */
	public BDictionary getDictionary() {

		return this.dictionary.clone();

	}


	/**
	 * Gets the announce URL string of this torrent's tracker
	 *
	 * @return the announce URL string of this torrent's tracker
	 */
	public String getAnnounceURL() {

		return this.dictionary.getString ("announce");

	}


	/**
	 * Returns the torrent's multitracker announce URLs as a list of tiers, where each tier is a
	 * list of strings. If no multitracker URLs are present, a single tier containing only the
	 * single non-multitracker URL is synthesised 
	 *
	 * @return The torrent's mulititracker announce URLs
	 */
	public List<List<String>> getAnnounceURLs() {

		List<List<String>> tiers = new ArrayList<List<String>>();

		BList announceList = (BList) this.dictionary.get ("announce-list");
		if (announceList != null) {
			for (BValue tierValue : announceList) {
				List<String> trackers = new ArrayList<String>();
				BList tier = (BList) tierValue;
				for (BValue trackerValue : tier) {
					BBinary tracker = (BBinary) trackerValue;
					trackers.add (tracker.stringValue());
				}
				tiers.add (trackers);
			}
		} else {
			List<String> tier = new ArrayList<String>();
			tier.add (getAnnounceURL());
			tiers.add (tier);
		}

		return tiers;

	}


	/**
	 * Gets the torrent's Info
	 *
	 * @return the torrent's Info
	 */
	public Info getInfo() {

		return this.info;

	}


	/**
	 * Gets the torrent's creation date in seconds since the epoch, if known
	 *
	 * @return the torrent's creation date, if known, or null
	 */
	public Long getCreationDate() {

		BValue creationDateValue = this.dictionary.get ("creation date");
		if (creationDateValue instanceof BInteger) {
			return ((BInteger)creationDateValue).value().longValue();
		}		

		return null;

	}


	/**
	 * @return The DSA public key used to sign the Info dictionary, or {@code null}
	 */
	public PublicKey getPublicKey() {

		return this.publicKey;

	}


	/**
	 * Create a MetaInfo based on the given dictionary
	 * 
	 * @param dictionary The dictionary to base the MetaInfo upon
	 * @throws InvalidEncodingException if the dictionary does not describe a valid torrent 
	 */
	public MetaInfo (BDictionary dictionary) throws InvalidEncodingException {

		this.dictionary = dictionary.clone();

		// Check: '/announce' must be present and a binary string
		assertTrue (this.dictionary.get ("announce") instanceof BBinary, "'announce' missing or of incorrect type");

		// Check: if '/announce-list' is present, it must be a list of lists of binary strings
		BValue announceListValue = this.dictionary.get ("announce-list");
		if (announceListValue != null) {
			assertTrue (announceListValue instanceof BList, "'announce-list' of incorrect type");
			BList announceList = (BList) announceListValue;
			for (BValue tierValue : announceList) {
				assertTrue (tierValue instanceof BList, "'announce-list' element of incorrect type");
				BList tier = (BList) tierValue;
				assertTrue (tier.size() > 0, "'announce-list' element empty");
				for (BValue tracker : tier) {
					assertTrue (tracker instanceof BBinary, "'announce-list' element of incorrect type");
				}
			}
		}

		// Check: '/info' must be present and a dictionary
		BValue infoValue = this.dictionary.get ("info");
		assertTrue (infoValue instanceof BDictionary, "'info' missing or of incorrect type");

		this.info = new Info ((BDictionary) infoValue);

		// Check: '/info signature', if present,  must be a 40 byte binary string
		BValue infoSignatureValue = this.dictionary.get ("info signature");
		if (infoSignatureValue != null) {
			assertTrue (infoSignatureValue instanceof BBinary, "'info signature' of incorrect type");
			BBinary infoSignature = (BBinary) infoSignatureValue;
			assertTrue (infoSignature.value ().length == 40, "'info signature' of incorrect length");

			// Check : '/public key' must be present and a valid x509 certificate that verifies both
			// the info and root hash signatures
			BValue publicKeyValue = this.dictionary.get ("public key");
			BBinary publicKeyBinary = (BBinary)publicKeyValue;
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec (publicKeyBinary.value());
			try {
				KeyFactory keyFactory = KeyFactory.getInstance ("DSA", "SUN");
				PublicKey publicKey = keyFactory.generatePublic (publicKeySpec);
				Signature verify = Signature.getInstance ("NONEwithDSA", "SUN");
				verify.initVerify (publicKey);
				verify.update (this.info.getHash().getBytes());
				if (!verify.verify (DSAUtil.p1363SignatureToDerSignature (ByteBuffer.wrap (infoSignature.value())).array())) {
					throw new InvalidEncodingException ("Info signature verification failed");
				}
				verify.update (this.info.getRootHash());
				if (!verify.verify (DSAUtil.p1363SignatureToDerSignature (ByteBuffer.wrap (this.info.getRootSignature())).array())) {
					throw new InvalidEncodingException ("Root signature verification failed");
				}
				this.publicKey = publicKey;
			} catch (GeneralSecurityException e) {
				throw new InvalidEncodingException ("Signature verification error", e);
			}

		}


	}


	/**
	 * Create a MetaInfo from the given parameters
	 * 
	 * @param announceURLs The torrent's announce URLs as a list of tiers, where each tier is a list
	 *        of strings. The first tracker in the first tier is taken as the non-multitracker
	 *        tracker
	 * @param info The Info
	 * @param keyPair A DSA key pair to sign the root hash and info hash, or {@code null}
	 * @throws InvalidEncodingException if any of the passed arguments fail validation 
	 */
	public MetaInfo (List<List<String>> announceURLs, Info info, KeyPair keyPair) throws InvalidEncodingException {

		BDictionary dictionary = new BDictionary();
		String firstAnnounceURL = null;

		BList announceList = new BList();
		for (List<String> tierStrings : announceURLs) {
			BList tierList = new BList();
			for (String announceURL : tierStrings) {
				if (firstAnnounceURL == null) {
					firstAnnounceURL = announceURL;
				}
				tierList.add (announceURL);
			}
			announceList.add (tierList);
		}

		dictionary.put ("announce", firstAnnounceURL);
		dictionary.put ("announce-list", announceList);
		dictionary.put ("creation date", System.currentTimeMillis() / 1000);
		dictionary.put ("info", info.getDictionary());

		if (keyPair != null) {
			Signature dsa;
			try {
				dsa = Signature.getInstance ("NONEwithDSA", "SUN");
				dsa.initSign (keyPair.getPrivate());
				dsa.update (info.getHash().getBytes());
				byte[] derInfoSignature = dsa.sign();
				dictionary.put ("info signature", DSAUtil.derSignatureToP1363Signature (derInfoSignature));
				dictionary.put ("public key", new X509EncodedKeySpec(keyPair.getPublic().getEncoded()).getEncoded());
			} catch (Exception e) {
				throw new InvalidEncodingException (e);
			}

		}

		this.dictionary = dictionary;
		this.info = info;

	}


}
