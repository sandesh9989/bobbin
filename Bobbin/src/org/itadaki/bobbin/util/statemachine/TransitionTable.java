/*
 * Copyright (c) 2010 Matthew J. Francis and Contributors of the Bobbin Project
 * This file is distributed under the MIT licence. See the LICENCE file for further information.
 */
package org.itadaki.bobbin.util.statemachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * The transition table for a state machine.
 * <p>The table embodies a set of relationships of the form (State,Input) -&gt;
 * (State,Action)
 * 
 * @param <S> State - The type of the state machine's state
 * @param <I> Input - The type of the state machine's input
 * @param <A> Action - The type of the action
 */
public class TransitionTable<S extends Ordinal,I extends Ordinal,A> {

	/**
	 * The transition table
	 */
	public final int[][] table;

	/**
	 * The rule outputs from each transition
	 */
	public final List<RuleOutput<S,A>> ruleOutputs = new ArrayList<RuleOutput<S,A>>();


	/**
	 * Adds a single rule to the table
	 *
	 * @param stateBefore The initial state
	 * @param input The input
	 * @param stateAfter The resulting state. If {@code null}, the state is not changed
	 * @param action The resulting action. If {@code null}, no action is performed
	 */
	public void add (S stateBefore, I input, S stateAfter, A action) {

		RuleOutput<S,A> ruleOutput = new RuleOutput<S,A> (stateAfter, action);
		this.ruleOutputs.add (ruleOutput);
		this.table[stateBefore.ordinal()][input.ordinal()] = this.ruleOutputs.size() - 1;

	}


	/**
	 * Adds a rule for multiple input states
	 *
	 * @param statesBefore The set of initial states
	 * @param input The input
	 * @param stateAfter The resulting state. If {@code null}, the state is not changed
	 * @param action The resulting action. If {@code null}, no action is performed
	 */
	public void add (Set<S> statesBefore, I input, S stateAfter, A action) {
		for (S stateBefore : statesBefore) {
			add (stateBefore, input, stateAfter, action);
		}
	}


	/**
	 * Gets the rule output for a given rule input
	 *
	 * @param stateBefore The initial state
	 * @param input The input
	 * @return The rule output
	 */
	public RuleOutput<S,A> getRuleOutput (S stateBefore, I input) {

		int ruleIndex = this.table[stateBefore.ordinal()][input.ordinal()];
		if (ruleIndex != 0) {
			return this.ruleOutputs.get (ruleIndex);
		}
		return null;

	}


	/**
	 * @param stateSize The cardinality of the state set
	 * @param inputSize The cardinality of the input set
	 */
	public TransitionTable (int stateSize, int inputSize) {

		this.table = new int[stateSize][inputSize];
		this.ruleOutputs.add (null);

	}

}
