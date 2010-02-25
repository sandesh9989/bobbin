/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;


/**
 * A single threaded work queue that decorates {@link ScheduledThreadPoolExecutor} to give a name to
 * the created worker thread
 */
public class WorkQueue extends ScheduledThreadPoolExecutor {

	/**
	 * @param name The name to give the queue's worker thread
	 */
	public WorkQueue (final String name) {
		super (1, new ThreadFactory() {
			public Thread newThread (Runnable r) {
				Thread thread = new Thread (r);
				thread.setName (name);
				thread.setDaemon (true);
				return thread;
			}
		});
	}

}
