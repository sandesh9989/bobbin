/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.trackerclient;

/**
 * The current status of a tracker client
 */
public interface TrackerClientStatus {

	/**
	 * @return {@code true} if the tracker client is currently updating the tracker
	 */
	public boolean isUpdating();

	/**
	 * @return The number of seconds until the next tracker update, if any, or {@code null}
	 */
	public Integer getTimeUntilNextUpdate();

	/**
	 * @return The system time in milliseconds of the last successful tracker update, or
	 *         {@code null} if there has not been an update
	 */
	public Long getTimeOfLastUpdate();

	/**
	 * @return The most recent failure or warning message returned by the tracker, or {@code null}
	 *         if there has not been a failure or warning
	 */
	public String getFailureReason();

	/**
	 * @return The tracker's most recently reported count of complete peers (seeds)
	 */
	public int getCompleteCount();

	/**
	 * @return The tracker's most recently reported count of incomplete peers (downloaders)
	 */
	public int getIncompleteCount();

}