/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - App
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.app.streamClient;

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.IEventRef;
import de.tuilmenau.ics.fog.application.ApplicationClient;
import de.tuilmenau.ics.fog.application.Session;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.ui.Viewable;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * Client sending messages to communication partner with a
 * specified delay. It is used to produce a constant message
 * rate.
 */
public class StreamClient extends ApplicationClient
{
	public StreamClient(Host pHost, Identity pIdentity, Name pConnectTo, Description pRequirements)
	{
		super(pHost, pIdentity, pConnectTo, pRequirements);

		setPacketSizeByte(mPacketSize);
	}
	
	class StreamClientSession extends Session implements IEvent
	{
		public StreamClientSession(Logger pLogger)
		{
			super(false, pLogger, null);
		}
		
		@Override
		public void connected()
		{
			fire();
		}
		
		/**
		 * Just a dummy; we are more or less ignoring the incoming data
		 */
		@Override
		public boolean receiveData(Object pData)
		{
			if(pData != null) {
				if(pData.equals("exit")) {
					exit();
				}
			}
		
			return true;
		}
		
		@Override
		public void stop()
		{
			if(mTimer != null) {
				getHost().getTimeBase().cancelEvent(mTimer);
				mTimer = null;
			}
			
			super.stop();
			
			// inform application
			exit();
		}
		
		@Override
		public void fire()
		{
			mTimer = null;
			
			if(getConnection().isConnected()) {
				try {
					sendMessage();
					
					mTimer = getHost().getTimeBase().scheduleIn((double)mDelay / 1000.0d, this);
				}
				catch(NetworkException tExc) {
					getLogger().warn(this, "Send message returned error. Exiting.", tExc);
					stop();
				}
			} else {
				stop();
			}
		}
		
		private void sendMessage() throws NetworkException
		{
			if(mPacketSize < 0) {
				getConnection().write("Content " +mMsgCounter);
			} else {
				getConnection().write(mPacketContent);
			}
			
			mMsgCounter++;
		}

		private int mMsgCounter = 0;
		private IEventRef mTimer = null;
	}

	public int getDelayMSec()
	{
		return mDelay;
	}
	
	public void setDelayMSec(int pDelay)
	{
		mDelay = Math.max(0, pDelay);
	}
	
	public int getPacketSizeByte()
	{
		return mPacketSize;
	}
	
	/**
	 * @param pSize -1: Send text message with counter; >= 0: Send message with array of defined size
	 */
	public void setPacketSizeByte(int pSize)
	{
		if(pSize >= 0) {
			mPacketContent = new byte[pSize];
			
			// init payload with increasing values in a small range
			for(int i = 0; i < pSize; i++) {
				mPacketContent[i] = (byte) (i & 127);
			}
		} else {
			mPacketContent = null;
		}
		mPacketSize = Math.max(-1, pSize);
	}
	
	@Override
	protected Session createSession()
	{
		return new StreamClientSession(getLogger());
	}
	
	@Viewable("Delay between packets [msec]")
	private int mDelay = 1000;
	
	@Viewable("Packet size [bytes]")
	private int mPacketSize = -1;
	private byte[] mPacketContent = null;
}
