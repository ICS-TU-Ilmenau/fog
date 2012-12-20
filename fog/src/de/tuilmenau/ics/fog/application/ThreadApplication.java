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
package de.tuilmenau.ics.fog.application;

import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Logger;


public abstract class ThreadApplication extends Application implements Runnable
{
	protected ThreadApplication(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
	}
	
	protected ThreadApplication(Host pHost, Logger pParentLogger, Identity pIdentity)
	{
		super(pHost, pParentLogger, pIdentity);
	}
	
	public void started()
	{
		mExit = false;
		mAppThread = new Thread(this);
		mAppThread.start();
	}
	
	public void exit()
	{
		mExit = true;
	}
	
	public boolean isRunning()
	{
		return (mAppThread != null);
	}
	
	public final void run()
	{
		Exception tError = null;
		
		try {
			execute();
		}
		catch(Exception tExc) {
			tError = tExc;
		}
		
		mAppThread = null;
		terminated(tError);
	}
	
	/**
	 * Implements task of the application.
	 * <code>run</code> is not used directly because it is doing some
	 * additional error handling.
	 */
	protected abstract void execute() throws Exception;

	/**
	 * Indicates if the application should terminate.
	 */
	@Viewable("Exit")
	protected volatile boolean mExit = false;
	
	private Thread mAppThread;
}
