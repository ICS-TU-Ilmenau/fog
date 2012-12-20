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

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.IEventRef;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * Client tries to send a message to a server each 5 seconds.
 * It expects an answer message, which will be logged.
 */
public class MsgClient extends ApplicationClient implements IEvent
{
	private final double DELAY_BETWEEN_TWO_CONNECTIONS_SEC = 5.0d;
	private final int MAX_NUMBER_CONNECTIONS = -1;
	
	
	public MsgClient(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
	}
	
	@Override
	protected void started()
	{
		fire();
	}
	
	@Override
	protected Session createSession()
	{
		if(mRemainingConnections > 0) mRemainingConnections--;
		
		return new MsgClientSession(getLogger());
	}
	
	class MsgClientSession extends Session
	{
		public MsgClientSession(Logger pLogger)
		{
			super(false, pLogger, null);
		}
		
		public void connected()
		{
			try {
				getConnection().write("Hello from " +this);
			}
			catch(NetworkException tExc) {
				mLogger.warn(this, "Can not write message.", tExc);
				stop();
			}
		}
		
		public boolean receiveData(Object pData)
		{
			mLogger.log(this, "Received answer: " +pData);
			
			if(pData != null) {
				if(pData.equals("exit")) {
					exit();
				}
			}
			
			synchronized (this) {
				notifyAll();
			}
			
			// terminate session
			stop();

			return true;
		}
	}
	
	@Override
	public void exit()
	{
		mHost.getTimeBase().cancelEvent(mTimer);
		super.exit();
	}

	@Override
	public void fire()
	{
		if(mRemainingConnections != 0) {
			mTimer = getHost().getTimeBase().scheduleIn(DELAY_BETWEEN_TWO_CONNECTIONS_SEC, this);
					
			super.started();
		} else {
			exit();
		}
	}
	
	private IEventRef mTimer = null;
	private int mRemainingConnections = MAX_NUMBER_CONNECTIONS;
}
