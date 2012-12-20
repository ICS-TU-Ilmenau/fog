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

import de.tuilmenau.ics.CommonSim.datastream.numeric.CounterNode;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.IEventRef;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.util.Logger;


public class ConnectTest extends ApplicationClient
{
	private static final double CONNECT_TEST_TIMEOUT_SEC = 40;
	
	public ConnectTest(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
		
		count("Start");
	}
//?? parameters nicht in started parsen?
	@Override
	public void setParameters(String[] pParameters) throws InvalidParameterException
	{
		super.setParameters(pParameters);
		
		if(pParameters != null) {
			if(pParameters.length > 2) {
				mCloseAfterTest = Boolean.parseBoolean(pParameters[2]);
			}
		}
	}

	@Override
	protected void started()
	{
		try {
			count("Connect");
			super.started();
		}
		catch(Exception tExc) {
			count("Exception");
			exit();
		}
	}
	
	@Override
	protected Session createSession()
	{
		return new ConnectTestSession(getLogger());
	}
	
	@Override
	public void exit()
	{
		if(!isExiting()) {
			count("Exit");
			
			if(!mCloseAfterTest) {
				// remove session in order to prevent closing on exit
				setSession(null);
			}
			
			if(sOnExit != null) {
				getHost().getTimeBase().scheduleIn(0, sOnExit);
			}
			
			super.exit();
		}
	}
	
	class ConnectTestSession extends Session implements IEvent
	{
		public ConnectTestSession(Logger pLogger)
		{
			super(false, pLogger, null);
		}
		
		public void connected()
		{
			// start timeout for detecting errors
			// due to packet loss or other problems
			mTimer = getHost().getTimeBase().scheduleIn(CONNECT_TEST_TIMEOUT_SEC, this);
			
			count("Send");
			try {
				getConnection().write("Hello " +this);
			}
			catch(NetworkException tExc) {
				count("Exception");
				getLogger().err(this, "Exception during sending data from " +getHost() +" to " +getDestination(), tExc);
				exit();
			}
		}
		
		@Override
		public boolean receiveData(Object pData)
		{
			if(pData.equals("Hello " +this)) {
				count("Recv");
			} else {
				count("Wrong");
			}
			
			exit();
			return true;
		}

		@Override
		public void closed()
		{
			exit();
		}
		
		@Override
		public void error(Exception pExc)
		{
			count("Exception");
			getLogger().err(this, "Exception during connection from " +getHost() +" to " +getDestination(), pExc);
			exit();
		}

		/**
		 * Called if timeout occurs.
		 */
		@Override
		public void fire()
		{
			mTimer = null;
			
			if(isRunning()) {
				count("Timeout");
				exit();
			}
		}
	}
	
	public static void setOnExitEvent(IEvent pOnExit)
	{
		sOnExit = pOnExit;
	}

	@Override
	public Description getDescription()
	{
		if(mRequirements == null) {
			mRequirements = new Description();
			//mRequirements.set(new LossRateProperty(1));
			//mRequirements.set(new DatarateProperty(1, Limit.MIN));
			//mRequirements.set(DatarateProperty.createSoftRequirement(1000, 0.7d));
		}
		return mRequirements;
	}
	
	private void count(String pEvent)
	{
		IDoubleWriter counter = CounterNode.openAsWriter(getClass().getCanonicalName() +"." +pEvent);
		counter.write(+1.0, getHost().getTimeBase().nowStream());
	}
	
	private IEventRef mTimer = null;
	private boolean mCloseAfterTest = true;
	private Description mRequirements = null;
	
	private static IEvent sOnExit = null;
}
