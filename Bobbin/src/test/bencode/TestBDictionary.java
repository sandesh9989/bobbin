package test.bencode;

import static org.junit.Assert.*;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.itadaki.bobbin.bencode.BBinary;
import org.itadaki.bobbin.bencode.BDictionary;
import org.itadaki.bobbin.bencode.BInteger;
import org.itadaki.bobbin.bencode.BValue;
import org.itadaki.bobbin.util.CharsetUtil;
import org.junit.Test;



/**
 * Tests BDictionary
 */
public class TestBDictionary {


	/**
	 * Create a blank dictionary
	 */
	@Test
	public void testDictionaryBlank() {

		BDictionary dictionary = new BDictionary();

		assertEquals (dictionary.value().size(), 0);

	}


	/**
	 * Store a blank map in a dictionary
	 */
	@Test
	public void testDictionaryBlankMap() {

		BDictionary dictionary = new BDictionary (new TreeMap<BBinary,BValue>());

		assertEquals (dictionary.value().size(), 0);

	}


	/**
	 * Test getString
	 */
	@Test
	public void testDictionaryGetString() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		BDictionary dictionary = new BDictionary (map);

		assertEquals ("Wanda", dictionary.getString ("fish"));

	}


	/**
	 * Test getString on an absent key
	 */
	@Test
	public void testDictionaryGetStringAbsent() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		BDictionary dictionary = new BDictionary (map);

		assertNull (dictionary.getString ("cat"));

	}


	/**
	 * Test getBytes
	 * @throws Exception
	 */
	@Test
	public void testDictionaryGetBytes() throws Exception{

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		BDictionary dictionary = new BDictionary (map);

		assertArrayEquals ("Wanda".getBytes ("UTF-8"), dictionary.getBytes ("fish"));

	}


	/**
	 * Test getBytes on an absent key
	 */
	@Test
	public void testDictionaryGetBytesAbsent() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		BDictionary dictionary = new BDictionary (map);

		assertNull (dictionary.getBytes ("cat"));

	}


	/**
	 * Test get
	 */
	@Test
	public void testDictionaryGet() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		BDictionary dictionary = new BDictionary (map);

		assertEquals (new BBinary ("Wanda"), dictionary.get ("fish"));

	}


	/**
	 * Test get on an absent key
	 */
	@Test
	public void testDictionaryGetAbsent() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		BDictionary dictionary = new BDictionary (map);

		assertNull (dictionary.get (new BBinary ("cat")));

	}


	/**
	 * Test get (byte[])
	 */
	@Test
	public void testDictionaryGetByteKey() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		BDictionary dictionary = new BDictionary (map);

		assertEquals (new BBinary ("Wanda"), dictionary.get ("fish".getBytes (CharsetUtil.ASCII)));

	}


	/**
	 * Test get (byte[]) on an absent key
	 */
	@Test
	public void testDictionaryGetByteKeyAbsent() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		BDictionary dictionary = new BDictionary (map);

		assertNull (dictionary.get ("cat".getBytes (CharsetUtil.ASCII)));

	}


	/**
	 * Test put (BBinary, BValue)
	 */
	@Test
	public void testDictionaryPutBBinaryBValue() {

		BDictionary dictionary = new BDictionary ();

		assertEquals (0, dictionary.size());

		dictionary.put (new BBinary ("key"), new BInteger (1234));

		assertEquals (1, dictionary.size());
		assertEquals (new BInteger (1234), dictionary.get ("key"));

	}


	/**
	 * Test put (String, BValue)
	 */
	@Test
	public void testDictionaryPutStringBValue() {

		BDictionary dictionary = new BDictionary ();

		assertEquals (0, dictionary.size());

		dictionary.put ("key", new BInteger (1234));

		assertEquals (1, dictionary.size());
		assertEquals (new BInteger (1234), dictionary.get ("key"));

	}


	/**
	 * Test put (String, String)
	 */
	@Test
	public void testDictionaryPutStringString() {

		BDictionary dictionary = new BDictionary ();

		assertEquals (0, dictionary.size());

		dictionary.put ("key", "value");

		assertEquals (1, dictionary.size());
		assertEquals (new BBinary ("value"), dictionary.get ("key"));

	}


	/**
	 * Test put (String, byte[])
	 */
	@Test
	public void testDictionaryPutStringBytes() {

		BDictionary dictionary = new BDictionary ();

		assertEquals (0, dictionary.size());

		dictionary.put ("key", "value".getBytes (CharsetUtil.ASCII));

		assertEquals (1, dictionary.size());
		assertEquals (new BBinary ("value"), dictionary.get ("key"));

	}


	/**
	 * Test put (String, Number)
	 */
	@Test
	public void testDictionaryPutStringNumber() {

		BDictionary dictionary = new BDictionary ();

		assertEquals (0, dictionary.size());

		dictionary.put ("key", 1234);

		assertEquals (1, dictionary.size());
		assertEquals (new BInteger (1234), dictionary.get ("key"));

	}


	/**
	 * Test keySet
	 */
	@Test
	public void testDictionaryKeySet() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		BDictionary dictionary = new BDictionary (map);

		Set<BBinary> keys = dictionary.keySet();
		assertEquals (1, keys.size());
		assertEquals (new BBinary ("fish"), keys.iterator().next());

	}


	/**
	 * Test remove (BBinary)
	 */
	@Test
	public void testDictionaryRemoveBBinary() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		map.put (new BBinary ("cow"), new BBinary ("Daisy"));
		BDictionary dictionary = new BDictionary (map);

		dictionary.remove (new BBinary ("fish"));

		assertEquals (1, dictionary.size());
		assertEquals ("Daisy", dictionary.getString ("cow"));

	}


	/**
	 * Test remove (String)
	 */
	@Test
	public void testDictionaryRemoveString() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		map.put (new BBinary ("cow"), new BBinary ("Daisy"));
		BDictionary dictionary = new BDictionary (map);

		dictionary.remove ("fish");

		assertEquals (1, dictionary.size());
		assertEquals ("Daisy", dictionary.getString ("cow"));

	}


	/**
	 * Test clear
	 */
	@Test
	public void testDictionaryClear() {

		SortedMap<BBinary,BValue> map = new TreeMap<BBinary,BValue>();
		map.put (new BBinary ("fish"), new BBinary ("Wanda"));
		map.put (new BBinary ("cow"), new BBinary ("Daisy"));
		BDictionary dictionary = new BDictionary (map);

		dictionary.clear();

		assertEquals (0, dictionary.size());

	}


	/**
	 * Check equality of two dictionary BValues
	 */
	@Test
	public void testDictionaryEquals() {

		SortedMap<BBinary,BValue> map1 = new TreeMap<BBinary,BValue>();
		map1.put (new BBinary ("fish"), new BBinary ("Wanda"));

		SortedMap<BBinary,BValue> map2 = new TreeMap<BBinary,BValue>();
		map2.put (new BBinary ("fish"), new BBinary ("Wanda"));

		BDictionary dictionary1 = new BDictionary (map1);
		BDictionary dictionary2 = new BDictionary (map2);

		assertEquals (dictionary1, dictionary2);

	}


	/**
	 * Check inequality of two dictionary BValues
	 */
	@Test
	public void testDictionaryNotEquals() {

		SortedMap<BBinary,BValue> map1 = new TreeMap<BBinary,BValue>();
		map1.put (new BBinary ("fish"), new BBinary ("Wanda"));

		SortedMap<BBinary,BValue> map2 = new TreeMap<BBinary,BValue>();
		map2.put (new BBinary ("cow"), new BBinary ("Daisy"));

		BDictionary dictionary1 = new BDictionary (map1);
		BDictionary dictionary2 = new BDictionary (map2);

		assertFalse (dictionary1.equals (dictionary2));

	}


	/**
	 * Test cloning a BDictionary
	 */
	@Test
	public void testDictionaryClone() {

		SortedMap<BBinary, BValue> map = new TreeMap<BBinary, BValue>();
		map.put (new BBinary ("Hello"), new BBinary ("World"));
		BDictionary dictionary = new BDictionary (map);
		BDictionary clonedDictionary = dictionary.clone();

		assertEquals (dictionary, clonedDictionary);
		assertFalse (System.identityHashCode (dictionary) == System.identityHashCode (clonedDictionary));
		assertFalse (System.identityHashCode (dictionary.value()) == System.identityHashCode (clonedDictionary.value()));
		assertFalse (System.identityHashCode (dictionary.get ("Hello")) == System.identityHashCode (clonedDictionary.get ("Hello")));

	}

}
