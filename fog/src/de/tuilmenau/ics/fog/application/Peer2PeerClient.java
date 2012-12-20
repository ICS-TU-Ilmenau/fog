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

import java.util.LinkedList;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.IServerCallback;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.facade.properties.IgnoreDestinationProperty;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.fog.util.Timer;


public class Peer2PeerClient extends ThreadApplication implements IServerCallback
{
	public Peer2PeerClient(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
		
		mName = null;
	}
	
	@Override
	public void setParameters(String[] pParameters) throws InvalidParameterException
	{
		if(pParameters.length >= 2) {
			mName = SimpleName.parse(pParameters[1]);
		} else {
			throw new InvalidParameterException("Required parameter: Peer2PeerClient <name>");
		}
	}
	
	private class PeerSession extends Session
	{
		public PeerSession()
		{
			super(false, mHost.getLogger(), null);
			
			mTimer = new Timer(getHost().getTimeBase(), new IEvent() {
				@Override
				public void fire()
				{
					close();
				}
			}, Config.PROCESS_STD_TIMEOUT_SEC);
			
			mTimer.start();
		}
		
		public void send() throws NetworkException
		{
			getConnection().write("Hello from " +this);
		}
		
		@Override
		public boolean receiveData(Object pData)
		{
			mLogger.log(this, "received: " +pData.toString());
			
			return true;
		}
		
		public void close()
		{
			getConnection().close();
		}
		
		private Timer mTimer;
	};
	
	public boolean openAck(LinkedList<Signature> pAuths, Description pDescription, Name pTargetName)
	{
		return true;
	}
	
	public void newConnection(Connection pConnection)
	{
		new PeerSession().start(pConnection);
	}
	
	protected void execute() throws NetworkException
	{
		int i = 0;
		mExit = false;
		
		// register own service provider
		mServerSocket = getHost().bind(null, mName, getDescription(), getIdentity());
		Description tDescription = new Description();
		
		// ignore myself at the connect request
		tDescription.add(new IgnoreDestinationProperty(mServerSocket));
		
		// connecting to some peers
		try {
			for(i=0; i<5; i++) {
				Connection socket = getHost().connectBlock(mName, tDescription, null);
				PeerSession connToPeer = new PeerSession();

				connToPeer.send();
				
				// add the previous connection to the ignore list for the next connect request
				tDescription.add(new IgnoreDestinationProperty(socket));
			}
		}
		catch(NetworkException exc) {
			mLogger.log(this, "Can not connect to more than " +i +" clients.");
		}
		
		
		waitForExit();
		
		mServerSocket.close();
	}
	
	private Binding mServerSocket;
	private SimpleName mName;
}
