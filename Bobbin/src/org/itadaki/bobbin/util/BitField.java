/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An extendable bit field<br>
 * <br>
 * In contrast with java.util.BitSet, this class allows converting to / from a byte array
 */
public class BitField implements Cloneable, Iterable<Integer> {

	/**
	 * The length in bits of the bit field
	 */
	private int length;

	/**
	 * The represented bits
	 */
	private byte[] bits;

	/**
	 * The cardinality of the bit field
	 */
	private int cardinality;


	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public BitField clone() {

		BitField copy = null;
		try {
			copy = (BitField) super.clone();
			copy.bits = Arrays.copyOf (this.bits, this.bits.length);
		} catch (CloneNotSupportedException e) {
			// Can't happen
		}

		return copy;

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (Object object) {

		if (this == object) return true;
		if (!(object instanceof BitField)) return false;

		BitField other = (BitField) object;
		if (this.length != other.length) return false;
		for (int i = 0; i < this.bits.length; i++) {
			if (this.bits[i] != other.bits[i]) return false;
		}

		return true;

	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();
		builder.append ("BitField[ " + this.length + ": ");
		for (int i = 0; i < this.length; i++) {
			if ((i > 0) && ((i % 4) == 0)) {
				builder.append (" ");
			}
			builder.append (this.get(i) ? "1" : "0");
		}
		builder.append (" ]");

		return builder.toString();

	}


	/* Iterator<Integer> interface */

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Integer> iterator() {

		Iterator<Integer> iterator = new Iterator<Integer>() {

			private int position = -1;
			private int nextPosition = -1;

			private void findNext() {

				for (int i = this.position + 1; i < BitField.this.length; i++) {
					if (get (i)) {
						this.nextPosition = i;
						return;
					}
				}

				this.nextPosition = -1;

			}

			public boolean hasNext() {

				if (this.nextPosition <= this.position) {
					findNext();
				}

				return (this.nextPosition != -1);

			}

			public Integer next() {

				if (this.nextPosition <= this.position) {
					findNext();
				}
				if (this.nextPosition == -1) {
					throw new NoSuchElementException();
				}
				this.position = this.nextPosition;
				return this.position;

			}

			public void remove() {

				throw new UnsupportedOperationException();

			}

		};

		return iterator;

	}


	/**
	 * Calculate and store the bit field's cardinality
	 */
	private void calculateCardinality() {

		// The number of bits in the byte array may be higher than the number of
		// bits in the bit field, but the extra bits are always 0
		int cardinality = 0;
		for (int b : this.bits) {
			b = (b & 0x55) + (b >>> 1 & 0x55);
			b = (b & 0x33) + (b >>> 2 & 0x33);
			b = (b & 0x07) + (b >>> 4);
			cardinality += b;
		}

		this.cardinality = cardinality;

	}


	/**
	 * Returns the length in bits of the represented bit field
	 *
	 * @return The length in bits of the represented bit field
	 */
	public int length() {

		return this.length;

	}


	/**
	 * Returns the length in bytes of the underlying byte array
	 *
	 * @return The length in bytes of the underlying byte array
	 */
	public int byteLength() {

		return this.bits.length;

	}


	/**
	 * Returns the number of set bits in the bit field
	 *
	 * @return The number of set bits in the bit field
	 */
	public int cardinality() {

		return this.cardinality;

	}


	/**
	 * Performs a logical AND between the contents of the bitfield and another bitfield. The
	 * bitfields must be of the same length. After this call, only bits that are set in both
	 * bitfields will be set in this bitfield. The bitfield that is passed in will not be changed.
	 *
	 * @param other The bitfield to AND with this bitfield
	 * @return This bitfield
	 */
	public BitField and (BitField other) {

		if (this.length != other.length) {
			throw new IllegalArgumentException ("Bitfields are of different length");
		}

		for (int i = 0; i < this.bits.length; i++) {
			this.bits[i] &= other.bits[i];
		}

		calculateCardinality();

		return this;

	}


	/**
	 * Performs a logical NOT on the contents of the bitfield
	 * @return This bitfield
	 */
	public BitField not() {

		for (int i = 0; i < this.bits.length; i++) {
			this.bits[i] = (byte) ~this.bits[i];
		}

		int remainder = this.length % 8;
		if (remainder > 0) {
			byte mask = (byte) (0xff << (8 - remainder));
			this.bits[this.bits.length - 1] &= mask; 
		}

		this.cardinality = this.length - this.cardinality;

		return this;

	}


	/**
	 * Tests if this BitField and another intersect (share any set bits)
	 *
	 * @param other The other BitField to compare with
	 * @return {@code true} if this BitField and the other share any set bits, otherwise
	 *         {@code false}
	 */
	public boolean intersects (BitField other) {

		int compareLength = Math.min (this.bits.length, other.bits.length);

		for (int i = 0; i < compareLength; i++) {
			if ((this.bits[i] & other.bits[i]) != 0) {
				return true;
			}
		}

		return false;

	}


	/**
	 * Gets one bit from the bit field
	 *
	 * @param index The index of the bit to get
	 * @return The value of the bit at the given index
	 */
	public boolean get (int index) {

		if (index < 0 || index >= this.length) {
			throw new IndexOutOfBoundsException();
		}

		int byteIndex = index / 8;
		int bitIndex = index % 8;

		return (this.bits[byteIndex] & (128 >> bitIndex)) != 0;

	}


	/**
	 * Sets one bit within the given bit field to true
	 *
	 * @param index The index of the bit to set to true
	 */
	public void set (int index) {
		
		if (index < 0 || index >= this.length) {
			throw new IndexOutOfBoundsException();
		}

		int byteIndex = index / 8;
		int bitIndex = index % 8;

		this.cardinality += ((this.bits[byteIndex] & (128 >> bitIndex)) == 0) ? 1 : 0;
		this.bits[byteIndex] |= (128 >> bitIndex);

	}


	/**
	 * Sets one bit within the given bit field to the given value
	 *
	 * @param index The index of the bit to set
	 * @param value The value to set the bit to
	 */
	public void set (int index, boolean value) {

		if (value) {
			set (index);
		} else {
			clear (index);
		}

	}


	/**
	 * Sets all bits of the bit field to false
	 */
	public void clear() {

		Arrays.fill (this.bits, (byte) 0);

		this.cardinality = 0;

	}


	/**
	 * Sets one bit within the bit field to false
	 *
	 * @param index The index of the bit to set to false
	 */
	public void clear (int index) {
		
		if (index < 0 || index >= this.length) {
			throw new IndexOutOfBoundsException();
		}

		int byteIndex = index / 8;
		int bitIndex = index % 8;

		this.cardinality -= ((this.bits[byteIndex] & (128 >> bitIndex)) == 0) ? 0 : 1;
		this.bits[byteIndex] &= ~(128 >> bitIndex);

	}


	/**
	 * Extend the bit field to a new total length
	 *
	 * @param length The new length of the bit field in bits
	 * @throws IllegalArgumentException if the length of the data is not at least as long as the
	 *         existing length
	 */
	public void extend (int length) {

		if (length < this.length) {
			throw new IllegalArgumentException ("New length must be at least as great as old length");
		}

		int byteLength = (length + 7) / 8;

		if (byteLength > this.bits.length) {
			this.bits = Arrays.copyOf (this.bits, byteLength);
		}

		this.length = length;

	}


	/**
	 * @return A copy of the bytes of the bit field
	 */
	public byte[] content() {

		return Arrays.copyOf (this.bits, this.bits.length);

	}


	/**
	 * Copies the represented bit field into the given array
	 *
	 * @param destination The array into which to copy the bit field
	 * @param offset The offset in bytes in the destination array from which to start
	 */
	public void copyTo (byte[] destination, int offset) {

		if (this.length > 0) {
			System.arraycopy (this.bits, 0, destination, offset, this.bits.length);
		}

	}


	/**
	 * Creates a blank bit field of the given number of bits
	 * 
	 * @param length The size of the bit field in bits
	 */
	public BitField (int length) {

		if (length < 0) {
			throw new IllegalArgumentException ("Negative size : " + length);
		}

		this.length = length;
		this.bits = new byte[(length + 7) / 8];
		this.cardinality = 0;

	}


	/**
	 * Creates a bit field from the given byte array and length in bits<br>
	 * 
	 * @param data The bytes with which to initialise the bit field
	 * @param length The length of the bit field in bits
	 * @throws IllegalArgumentException if the length of the data does not match the number of bits,
	 *           or if a bit is set in the data higher than the number of bits
	 */
	public BitField (byte[] data, int length) {

		this (length);

		if (data.length != this.bits.length) {
			throw new IllegalArgumentException ("Invalid data size");
		}

		System.arraycopy (data, 0, this.bits, 0, data.length);

		if ((length % 8) > 0) {
			for (int i = length % 8; i < 8; i++) {
				if ((this.bits[this.bits.length - 1] & (128 >> i)) != 0) {
					throw new IllegalArgumentException ("Bit " + i + " set greater than size");
				}
			}
		}

		calculateCardinality();

	}


}
