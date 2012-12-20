/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.util;

import de.tuilmenau.ics.fog.EventHandler;


/**
 * Class implementing a token bucket algorithm in order to limit
 * the rate with which an action is performed.
 * 
 * Class is using the EventHandler as time base.
 *
 * @param <ActionParameter> Type of input parameter used for the action (ignored by RateLimitedAction itself)
 */
public abstract class RateLimitedAction<ActionParameter>
{
	private static final double MAX_TOKEN_INCR_TIME_SEC = 5;
	
	/**
	 * Constructor for an action, which invocation rate is limited.
	 *  
	 * @param actionsPerSecond Average rate of action invocation (e.g. 2.0 means 2 action invocations in average per second)
	 * @param timeBase Time base for rate limitation
	 */
	public RateLimitedAction(EventHandler timeBase, double actionsPerSecond)
	{
		this.timeBase = timeBase;
		timeLastAction = this.timeBase.now();
		this.actionsPerSecond = actionsPerSecond;
		
		// allows the first action to be performed immediately
		tokenBucket = 1.0d;
	}
	
	/**
	 * Is called in order to invoke the action defined by doAction. The
	 * method tracks the rate of invocations and might ignore some
	 * invocations in order to reduce the rate.
	 * 
	 * @param parameter Parameter for the action (ignored by RateLimitedAction class)
	 */
	public final void trigger(ActionParameter parameter)
	{
		double now = timeBase.now();
		double timeSinceLastAction = Math.max(0, now -timeLastAction);
		
		// add incrementation of token since last action was triggered
		double newTokenBucket = tokenBucket +(timeSinceLastAction *actionsPerSecond);

		// limit token value to maximum value, which is either defined by the rate
		// or for small rates by one
		final double maxTokenSice = Math.max(1.0d, actionsPerSecond * MAX_TOKEN_INCR_TIME_SEC);
		newTokenBucket = Math.min(newTokenBucket, maxTokenSice);

		// is the current token value high enough that we are allowed to trigger the action?
		if(newTokenBucket >= 1.0d) {
			tokenBucket = newTokenBucket -1.0d;
			timeLastAction = now;
			actionPerformedCounter++;
			
			// Trigger action after all own operations had
			// been performed in order to be able to ignore
			// exceptions from an action.
			doAction(parameter);
		} else {
			// is it the first time that we ignore an event?
			if(actionIgnoredCounter == 0) {
				firstIgnoreEvent();
			}
			
			actionIgnoredCounter++;
		}
	}
	
	public int getNumberPerformedActions()
	{
		return actionPerformedCounter;
	}
	
	public int getNumberIgnoredActions()
	{
		return actionIgnoredCounter;
	}
	
	public EventHandler getEventHandler()
	{
		return timeBase;
	}
	
	/**
	 * Action itself
	 */
	protected abstract void doAction(ActionParameter parameter);

	/**
	 * Called first time a trigger is ignored due to a full
	 * bucket. This method is intended to be overwritten by
	 * child class. The base implementation ignores this
	 * event.
	 */
	protected void firstIgnoreEvent()
	{
		// ignore it in base class
	}
	
	private EventHandler timeBase;
	
	private double timeLastAction;
	private double actionsPerSecond;
	private double tokenBucket;
	
	private int actionPerformedCounter = 0;
	private int actionIgnoredCounter = 0;
}
