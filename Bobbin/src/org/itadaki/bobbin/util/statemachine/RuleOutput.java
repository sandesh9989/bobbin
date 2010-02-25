/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util.statemachine;


/**
 * The output half of a state machine rule
 * @param <S> The type of the state machine's state
 * @param <A> The type of the action
 */
public class RuleOutput<S,A> {

	/**
	 * The state after a transition
	 */
	final S stateAfter;

	/**
	 * The action resulting from a transition
	 */
	final A action;

	/**
	 * @param stateAfter The state after a transition
	 * @param action The action resulting from a transition
	 */
	public RuleOutput (S stateAfter, A action) {

		this.stateAfter = stateAfter;
		this.action = action;

	}

}
