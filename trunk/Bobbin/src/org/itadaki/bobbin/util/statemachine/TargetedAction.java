package org.itadaki.bobbin.util.statemachine;


/**
 * An action that is invoked upon a target
 * 
 * @param <T> The type of the target
 */
public interface TargetedAction<T> {

	/**
	 * @param target The target to invoke the action upon
	 */
	public void execute (T target);

}
