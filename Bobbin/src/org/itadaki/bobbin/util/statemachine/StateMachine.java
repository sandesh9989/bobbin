/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util.statemachine;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;


/**
 * A type safe, thread safe safe state machine
 * 
 * @param <T> Target - The target upon which actions will be invoked
 * @param <S> State - The type of the state machine's state
 * @param <I> Input - The type of the state machine's input
 * @param <A> Action - The type of the action
 */
public class StateMachine<T,S extends Ordinal,I extends Ordinal,A extends TargetedAction<T>> {

	/**
	 * The transition table that connects (State,Input) input rules with (State,Action) output rules
	 */
	private final TransitionTable<S,I,A> transitions;

	/**
	 * The target upon which the machine's actions will be invoked
	 */
	private final T target;

	/**
	 * A set of listeners for state transitions
	 */
	private Set<TransitionListener<S,I>> listeners = new HashSet<TransitionListener<S,I>>();

	/**
	 * The current state
	 */
	private AtomicReference<S> state = new AtomicReference<S>();


	/**
	 * Supplies an input to the state machine. If there is an appropriate transition in the
	 * transition table, a transition is made and an action invoked based on the rule
	 * retrieved from the table
	 *
	 * <b>Thread Safety :</b> This method is thread safe. State transitions, including their
	 * resulting actions, are fully serialised against one another.
	 *
	 * @param input The input to supply to the state machine
	 */
	public synchronized void input (I input) {

		S stateBefore = this.state.get();
		RuleOutput<S,A> ruleOutput = this.transitions.getRuleOutput (stateBefore, input);
		S stateAfter = stateBefore;
		if (ruleOutput != null) {
			if (ruleOutput.stateAfter != null) {
				stateAfter = ruleOutput.stateAfter;
			}
			this.state.set (stateAfter);
			if (ruleOutput.action != null) {
				ruleOutput.action.execute (this.target);
			}
		}

		for (TransitionListener<S,I> listener : this.listeners) {
			listener.stateTransitioned (stateBefore, input, stateAfter);
		}

	}


	/**
	 * Adds a listener for state transitions
	 *
	 * @param listener The listener to add
	 */
	public synchronized void addListener (TransitionListener<S,I> listener) {

		this.listeners.add (listener);

	}


	/**
	 * Removes a listener for state transitions
	 *
	 * @param listener The listener to remove
	 */
	public synchronized void removeListener (TransitionListener<S,I> listener) {

		this.listeners.remove (listener);

	}


	/**
	 * @return The current state of the state machine
	 * 
	 *  <p><b>Thread Safety :</b> This method is thread safe. In contrast with
	 *  {@link #input(Ordinal)}, this method is only atomic with respect to the state change itself,
	 *  and not serialised against the associated action.
	 */
	public S getState() {

		return this.state.get();

	}


	/**
	 * @param target The target upon which the machine's actions will be invoked
	 * @param transitions The table of transitions
	 * @param initialState The initial state of the machine
	 */
	public StateMachine (T target, TransitionTable<S,I,A> transitions, S initialState) {

		this.target = target;
		this.transitions = transitions;
		this.state.set (initialState);

	}

}
