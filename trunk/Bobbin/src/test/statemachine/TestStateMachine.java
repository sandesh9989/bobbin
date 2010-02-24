package test.statemachine;

import static org.junit.Assert.*;

import org.itadaki.bobbin.util.statemachine.Ordinal;
import org.itadaki.bobbin.util.statemachine.StateMachine;
import org.itadaki.bobbin.util.statemachine.TargetedAction;
import org.itadaki.bobbin.util.statemachine.TransitionTable;
import org.junit.Test;



/**
 * Tests StateMachine
 */
public class TestStateMachine {

	/**
	 * Tests a simple two state machine
	 */
	@Test
	public void testSimpleStateMachine() {

		MockTarget target = new MockTarget();

		TransitionTable<SimpleState,Input,Action> transitions = new TransitionTable<SimpleState,Input,Action> (
				SimpleState.values().length,
				Action.values().length
		);
		transitions.add (SimpleState.STOPPED, Input.RUN, SimpleState.RUNNING, Action.START);
		transitions.add (SimpleState.RUNNING, Input.STOP, SimpleState.STOPPED, Action.STOP);

		StateMachine<MockTarget,SimpleState,Input,Action> machine = new StateMachine<MockTarget,SimpleState,Input,Action> (
				target,
				transitions,
				SimpleState.STOPPED
		);

		assertEquals (SimpleState.STOPPED, machine.getState());

		machine.input (Input.RUN);

		assertEquals (SimpleState.RUNNING, machine.getState());
		assertEquals (1, target.startedCount);
		assertEquals (0, target.stoppedCount);

		machine.input (Input.RUN);
		assertEquals (SimpleState.RUNNING, machine.getState());
		assertEquals (1, target.startedCount);
		assertEquals (0, target.stoppedCount);

		machine.input (Input.STOP);
		assertEquals (SimpleState.STOPPED, machine.getState());
		assertEquals (1, target.startedCount);
		assertEquals (1, target.stoppedCount);

		machine.input (Input.STOP);
		assertEquals (SimpleState.STOPPED, machine.getState());
		assertEquals (1, target.startedCount);
		assertEquals (1, target.stoppedCount);

	}

	/**
	 * Tests a compound state machine
	 */
	@Test
	public void testCompoundStateMachine() {

		MockTarget target = new MockTarget();

		TransitionTable<CompoundState,Input,Action> transitions = new TransitionTable<CompoundState,Input,Action> (
				CompoundState.cardinality(),
				Action.values().length
		);
		transitions.add (new CompoundState (SimpleState.STOPPED, false), Input.RUN,  new CompoundState (SimpleState.STOPPED, true),  null);
		transitions.add (new CompoundState (SimpleState.STOPPED, true),  Input.RUN,  new CompoundState (SimpleState.RUNNING, false), Action.START);
		transitions.add (new CompoundState (SimpleState.RUNNING, false), Input.STOP, new CompoundState (SimpleState.RUNNING, true),  null);
		transitions.add (new CompoundState (SimpleState.RUNNING, true),  Input.STOP, new CompoundState (SimpleState.STOPPED, false), Action.STOP);

		StateMachine<MockTarget,CompoundState,Input,Action> machine = new StateMachine<MockTarget,CompoundState,Input,Action> (
				target,
				transitions,
				new CompoundState (SimpleState.STOPPED, false)
		);

		assertEquals (new CompoundState (SimpleState.STOPPED, false), machine.getState());

		machine.input (Input.RUN);
		assertEquals (new CompoundState (SimpleState.STOPPED, true), machine.getState());
		assertEquals (0, target.startedCount);
		assertEquals (0, target.stoppedCount);

		machine.input (Input.RUN);
		assertEquals (new CompoundState (SimpleState.RUNNING, false), machine.getState());
		assertEquals (1, target.startedCount);
		assertEquals (0, target.stoppedCount);

		machine.input (Input.STOP);
		assertEquals (new CompoundState (SimpleState.RUNNING, true), machine.getState());
		assertEquals (1, target.startedCount);
		assertEquals (0, target.stoppedCount);

		machine.input (Input.STOP);
		assertEquals (new CompoundState (SimpleState.STOPPED, false), machine.getState());
		assertEquals (1, target.startedCount);
		assertEquals (1, target.stoppedCount);

	}

}


/**
 * A mock target
 */
class MockTarget {

	/**
	 * The number of times actionStart() has been called
	 */
	public int startedCount = 0;

	/**
	 * 
	 */
	public int stoppedCount = 0;


	/**
	 * Increases the number of times actionStarted() has been called
	 */
	void actionStarted() {
		this.startedCount++;
	}


	/**
	 * Increases the number of times actionStopped() has been called
	 */
	void actionStopped() {
		this.stoppedCount++;
	}

}


/**
 * A test input
 */
enum Input implements Ordinal {
	/** */
	RUN,
	/** */
	STOP;
}


/**
 * A test state
 */
enum SimpleState implements Ordinal {
	/** */
	RUNNING,
	/** */
	STOPPED;
}


/**
 * A compound state comprising a SimpleState and a boolean
 */
class CompoundState implements Ordinal {

	/**
	 * The SimpleState
	 */
	final SimpleState simpleState;

	/**
	 * The boolean sub state
	 */
	final boolean subState;

	/**
	 * @return The cardinality of the set represented by the compound state
	 */
	public static int cardinality() {
		return SimpleState.values ().length * 2;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return ordinal();
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals (Object other) {

		if (this == other)
			return true;
		if (other == null)
			return false;
		if (getClass () != other.getClass ())
			return false;
		CompoundState otherState = (CompoundState) other;
		if (this.simpleState == null) {
			if (otherState.simpleState != null)
				return false;
		} else if (!this.simpleState.equals (otherState.simpleState))
			return false;
		if (this.subState != otherState.subState)
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.util.statemachine.Ordinal#ordinal()
	 */
	public int ordinal() {

		return (this.simpleState.ordinal() * 2) + (this.subState ? 1 : 0);

	}


	/**
	 * @param simpleState The SimpleState
	 * @param subState The boolean sub state
	 */
	public CompoundState (SimpleState simpleState, boolean subState) {

		this.simpleState = simpleState;
		this.subState = subState;

	}

}


/**
 * A test action
 */
enum Action implements TargetedAction<MockTarget> {
	/** */
	START,
	/** */
	STOP;

	/* (non-Javadoc)
	 * @see org.itadaki.bobbin.util.statemachine.TargetedAction#execute(java.lang.Object)
	 */
	public void execute (MockTarget target) {

		switch (this) {
			case START:
				target.actionStarted();
				break;
			case STOP:
				target.actionStopped ();
				break;
		}

	}

}
