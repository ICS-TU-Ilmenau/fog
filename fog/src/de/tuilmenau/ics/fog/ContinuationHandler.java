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
package de.tuilmenau.ics.fog;

import java.util.LinkedList;


/**
 * Helper class for storing and executing continuations.
 *
 * @param <CallingObject> Object using this helper class
 */
public class ContinuationHandler<CallingObject> implements IContinuation<CallingObject>, IEvent
{
	/**
	 * Constructor without a timeout.
	 */
	public ContinuationHandler(CallingObject pCaller)
	{
		this(null, -1, pCaller);
	}
	
	/**
	 * Constructor staring a timeout after which the failure method is called.
	 * 
	 * @param pTimeoutSec Timeout in seconds
	 * @param pCaller Object doing the operation.
	 */
	public ContinuationHandler(EventHandler pTimeBase, double pTimeoutSec, CallingObject pCaller)
	{
		mCaller = pCaller;
		
		if(pTimeoutSec >= 0) {
			if(pTimeBase != null) {
				pTimeBase.scheduleIn(pTimeoutSec, this);
			} else {
				throw new RuntimeException(this +" - Can not schedule timeout because no time base given.");
			}
		}
	}
	
	public void add(IContinuation<CallingObject> pContinuation)
	{
		if(mContinuation == null) {
			mContinuation = pContinuation;
		} else {
			// ok, more than one continuation -> create list
			if(mContinuations == null) {
				mContinuations = new LinkedList<IContinuation<CallingObject>>();
			}
			
			mContinuations.add(pContinuation);
		}
	}

	@Override
	public void fire()
	{
		failure(mCaller, null);
	}
	
	/**
	 * Executes all continuations and deletes them from list.
	 */
	@Override
	public void success(CallingObject pCaller)
	{
		if(mContinuation != null) {
			mContinuation.success(pCaller);
			mContinuation = null;
			
			if(mContinuations != null) {
				for(IContinuation<CallingObject> tCont : mContinuations) {
					tCont.success(pCaller);
				}

				mContinuations = null;
			}
		}
	}

	/**
	 * Executes all continuations and deletes them from list.
	 */
	@Override
	public void failure(CallingObject pCaller, Exception pException)
	{
		if(mContinuation != null) {
			mContinuation.failure(pCaller, pException);
			mContinuation = null;
			
			if(mContinuations != null) {
				for(IContinuation<CallingObject> tCont : mContinuations) {
					tCont.failure(pCaller, pException);
				}
				
				mContinuations.clear();
			}
		}
	}

	private CallingObject mCaller = null;
	private IContinuation<CallingObject> mContinuation = null;
	private LinkedList<IContinuation<CallingObject>> mContinuations = null;
}
