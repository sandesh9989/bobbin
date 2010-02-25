/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.tracker;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


/**
 * A peer event
 */
public enum TrackerEvent {

	/**
	 * A peer started downloading
	 */
	STARTED ("started"),

	/**
	 * A peer stopped downloading
	 */
	STOPPED ("stopped"),

	/**
	 * A peer finished downloading
	 */
	COMPLETED ("completed"),

	/**
	 * A peer sent us an update 
	 */
	UPDATE ("");


	/**
	 * A map connecting event string names to events 
	 */
	private static final Map<String,TrackerEvent> nameMap = new HashMap<String, TrackerEvent>();

	static {
		for (TrackerEvent event : EnumSet.allOf (TrackerEvent.class)) {
			nameMap.put (event.getName(), event);
		}
	}


	/**
	 * The event's string name
	 */
	private String name;


	/**
	 * Returns the event's string name
	 *
	 * @return The event's string name
	 */
	public String getName() {
		return this.name;
	}


	/**
	 * Returns the event corresponding to a string name
	 *
	 * @param name The event's string name
	 * @return The event, or null if non-existant
	 */
	public static TrackerEvent forName (String name) {

		return nameMap.get (name);

	}


	/**
	 * @param name The event's string name
	 */
	private TrackerEvent (String name) {
		this.name = name;
	}

}