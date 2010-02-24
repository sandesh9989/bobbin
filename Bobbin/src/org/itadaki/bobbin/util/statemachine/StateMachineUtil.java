package org.itadaki.bobbin.util.statemachine;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.itadaki.bobbin.util.WorkQueue;



/**
 * Miscellaneous state machine utilities
 */
public class StateMachineUtil {

	/**
	 * A helper to asynchronously initiate a transition on a state machine, then synchronously wait
	 * for the state machine to arrive at one of a set of possible final states
	 * 
	 * @param <T> Target - see {@link StateMachine} for details
	 * @param <S> State - see {@link StateMachine} for details
	 * @param <I> Input - see {@link StateMachine} for details
	 * @param <A> Action - see {@link StateMachine} for details
	 * @param stateMachine The state machine
	 * @param workQueue A work queue to execute the state transition upon
	 * @param input The input to apply to the state machine
	 * @param awaitedStates The set of states upon reaching any of which this method will return
	 */
	public static <T,S extends Ordinal,I extends Ordinal,A extends TargetedAction<T>> void transitionAndWait (
			final StateMachine<T,S,I,A> stateMachine, WorkQueue workQueue, final I input, final Set<S> awaitedStates)
	{

		final CountDownLatch latch = new CountDownLatch (1);

		workQueue.execute (new Runnable() {
			public void run() {
				synchronized (stateMachine) {
					stateMachine.addListener (new TransitionListener<S,I>() {
						public void stateTransitioned (S stateBefore, I input, S stateAfter) {
							if (awaitedStates.contains (stateAfter)) {
								latch.countDown();
								stateMachine.removeListener (this);
							}
						}
					});
					stateMachine.input (input);
				}
			}
		});

		boolean interrupted = false;
		boolean done = false;
		while (!done) {
			try {
				latch.await();
				done = true;
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		if (interrupted) {
			Thread.currentThread().interrupt();
		}

	}

}
