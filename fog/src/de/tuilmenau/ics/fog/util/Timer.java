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
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.IEventRef;


/**
 * Class implements timer based on event handling.
 */
public class Timer
{
	/**
	 * Creates timer with an event.
	 * The default timeout time is 0 seconds.
	 * 
	 * @param timeoutEvent Event fired at timeout
	 */
	public Timer(EventHandler timeBase, IEvent timeoutEvent, double pTimeoutSec)
	{
		mEvent = timeoutEvent;
		mEventHandler = timeBase;
		
		setTimeout(pTimeoutSec);
	}

	/**
	 * Constructors for events extending Timer class.
	 * This construtor will try to cast this class to
	 * an IEvent object.
	 * 
	 * @param pTimeoutSec
	 */
	public Timer(EventHandler timeBase, double pTimeoutSec)
	{
		mEvent = (IEvent) this;
		mEventHandler = timeBase;
		
		setTimeout(pTimeoutSec);
	}

	/**
	 * Starts timer if it is not already started.
	 */
	public void start()
	{
		if((mLifetimeSec >= 0) && (mEvent != null)) {
			// do not start again, if the event is already scheduled
			if(mScheduledEvent == null) {
				mScheduledEvent = mEventHandler.scheduleIn(mLifetimeSec, mEvent);
			}
		}
	}
	
	/**
	 * Sets time for timeout. If the timer is already started,
	 * the running timer is not changed. The timeout time is used
	 * only if the timer is (re-)started.
	 * 
	 * @param pTimeoutSec Timeout for the timer in seconds >= 0
	 */
	public void setTimeout(double pTimeoutSec)
	{
		if(pTimeoutSec >= 0) {
			mLifetimeSec = pTimeoutSec;
		}
	}
	
	public double getTimeout()
	{
		return mLifetimeSec;
	}
	
	public boolean isCancelled()
	{
		return (mScheduledEvent == null);
	}
	
	public boolean isRunning()
	{
		return (mScheduledEvent != null);
	}
	
	/**
	 * Re-starts timer. If it is already started, it is canceled.
	 */
	public void restart()
	{
		cancel();
		start();
	}
	
	/**
	 * Stops timer if it is already started.
	 */
	public void cancel()
	{
		if(mScheduledEvent != null) {
			mEventHandler.cancelEvent(mScheduledEvent);
			mScheduledEvent = null;
		}
	}

	private EventHandler mEventHandler;
	private double mLifetimeSec = 0;
	private IEvent mEvent = null;
	private IEventRef mScheduledEvent = null;
}
