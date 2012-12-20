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
package de.tuilmenau.ics.fog.application.observer;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.application.Application;


public class ApplicationObservable
{
	public ApplicationObservable(Application pApplication)
	{
		mApplication = pApplication;
		mNotificatingObservers = false;
	}
	
	public synchronized void addObserver(IApplicationEventObserver pObserver)
	{
		if (mNotificatingObservers)
		{
			mApplication.getLogger().trace(this, "Got recursive call to addObserver while notifying already registered observers");
			LinkedList<IApplicationEventObserver> tRegisteredObservers = new LinkedList<IApplicationEventObserver>(mRegisteredObservers);
			tRegisteredObservers.add(pObserver);
			mRegisteredObservers = tRegisteredObservers;
		} else {
			if (!mRegisteredObservers.contains(pObserver))
			{
				mRegisteredObservers.add(pObserver);
			}	
		}
	}
	
	public synchronized boolean deleteObserver(IApplicationEventObserver pObserver)
	{
		boolean tRes = false;
		
		if (mNotificatingObservers)
		{
			mApplication.getLogger().trace(this, "Got recursive call to deleteObserver while notifying already registered observers");
			LinkedList<IApplicationEventObserver> tRegisteredObservers = new LinkedList<IApplicationEventObserver>(mRegisteredObservers);
			tRes = tRegisteredObservers.remove(pObserver);
			mRegisteredObservers = tRegisteredObservers;
		}else
			tRes = mRegisteredObservers.remove(pObserver);
		
		return tRes;
	}
	
	public synchronized void clear()
	{
		mRegisteredObservers.clear();
	}

	public synchronized void notifyObservers(ApplicationEvent pEvent)
	{
		mNotificatingObservers = true;
		
		for (IApplicationEventObserver tObserver : mRegisteredObservers)
		{
			try
			{
				tObserver.handleEvent(mApplication, pEvent);
			}
			catch(Exception tExc)
			{
				mApplication.getLogger().err(this, "App observer " +tObserver +" throws exception. Ignoring it.", tExc);
			}
		}
		
		mNotificatingObservers = false;
	}
	
	public synchronized int countObservers()
	{
		return mRegisteredObservers.size();
	}
	
	private LinkedList<IApplicationEventObserver> mRegisteredObservers = new LinkedList<IApplicationEventObserver>();
	private Application mApplication;
	
	/*
	 * A flag indicating if observers are notified at the moment.
	 * It is used without locking because it is only needed in case of recursive calls to this observerable object by one thread. 
	 */
	private boolean mNotificatingObservers;
}
