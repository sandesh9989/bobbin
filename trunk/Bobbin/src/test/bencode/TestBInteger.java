/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package test.bencode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.itadaki.bobbin.bencode.BInteger;
import org.junit.Test;



/**
 * 
 */
public class TestBInteger {

	/**
     * Store an integer value and retrieve it
     */
    @Test
    public void testInteger() {
    
    	BInteger integer = new BInteger (new Integer (42));
    
    	assertEquals (integer.value(), new Integer (42));
    
    }

	/**
     * Check inequality of two integer BValues
     */
    @Test
    public void testIntegerNotEquals() {
    
    	BInteger integer1 = new BInteger (new Integer (42));
    	BInteger integer2 = new BInteger (new Integer (-1));
    
    	assertFalse (integer1.equals (integer2));
    
    }

	/**
     * Test cloning a BInteger
     */
    @Test
    public void testIntegerClone() {
    
    	BInteger integer = new BInteger (1234);
    	BInteger clonedInteger = integer.clone();
    
    	assertEquals (integer, clonedInteger);
    	assertFalse (System.identityHashCode (integer) == System.identityHashCode (clonedInteger));
    
    }

	/**
     * Check equality of two integer BValues
     */
    @Test
    public void testIntegerEquals() {
    
    	BInteger integer1 = new BInteger (new Integer (42));
    	BInteger integer2 = new BInteger (new Integer (42));
    
    	assertEquals (integer1, integer2);
    
    }

}
