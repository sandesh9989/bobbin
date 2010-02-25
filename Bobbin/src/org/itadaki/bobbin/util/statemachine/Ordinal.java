/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util.statemachine;


/**
 * An interface to allow any class to provide a unique ordinal() in the same fashion as Enum
 */
public interface Ordinal {

	/**
	 * @return An ordinal that uniquely represents the content of the implementing object
	 */
	public int ordinal();

}
