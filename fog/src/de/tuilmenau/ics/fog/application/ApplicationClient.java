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

import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.SimpleName;


public abstract class ApplicationClient extends Application
{
	public ApplicationClient(Host pHost, Identity pIdentity)
	{
		this(pHost, pIdentity, null, null);
	}
	
	public ApplicationClient(Host pHost, Identity pIdentity, Name pConnectTo, Description pRequirements)
	{
		super(pHost, pIdentity);
		
		mConnectTo = pConnectTo;
		mRequirements = pRequirements;
	}
	
	@Override
	public Description getDescription()
	{
		return mRequirements;
	}
	
	@Override
	public void setParameters(String[] pParameters) throws InvalidParameterException
	{
		super.setParameters(pParameters);
		
		pParameters = getParameters(); // avoid null pointer
		if(pParameters.length >= 2) {
			mConnectTo = SimpleName.parse(pParameters[1]);
		} else {
			throw new InvalidParameterException("Required parameter: <name to connect to>");
		}
	}
	
	@Override
	protected void started()
	{
		mIsRunning = true;
		mIsExiting = false;
		
		Connection tConn = getHost().connect(mConnectTo, getDescription(), getIdentity());
		mSession = createSession();
		mSession.start(tConn);
	}
	
	protected abstract Session createSession();
	
	protected Session setSession(Session newSession)
	{
		Session oldSession = mSession;
		
		mSession = newSession;
		return oldSession;
	}
	
	@Override
	public boolean isRunning()
	{
		return mIsRunning;
	}
	
	protected boolean isExiting()
	{
		return mIsExiting;
	}
	
	@Override
	public void exit()
	{
		// avoid recursive calls (e.g. during Session.stop)
		if(!mIsExiting) {
			mIsExiting = true;
			
			if(mSession != null) {
				mSession.stop();
				mSession = null;
			}
			
			mIsRunning = false;
			terminated(null);
		}
	}
	
	public Name getDestination()
	{
		return mConnectTo;
	}
	
	@Viewable("Requirements")
	private Description mRequirements = null;
	
	private Session mSession   = null;
	private Name mConnectTo   = null;
	private boolean mIsRunning = false;
	private boolean mIsExiting = false;
}
