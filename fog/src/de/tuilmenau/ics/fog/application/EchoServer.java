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

import java.io.Serializable;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.properties.CommunicationTypeProperty;
import de.tuilmenau.ics.fog.facade.properties.OrderedProperty;
import de.tuilmenau.ics.fog.facade.properties.TransportProperty;
import de.tuilmenau.ics.fog.util.SimpleName;
import de.tuilmenau.ics.fog.util.RateLimitedAction;


/**
 * Server application offering a echo service. All connections will be
 * accepted. The data messages received by a connection will be reflected
 * back to the sender as string.
 * 
 * Command line:
 * EchoServer <name of server> [<reflect messages == true>]
 */
public class EchoServer extends Application
{
	private static final String CMD_TERMINATE = "terminate";
	private static final double MAX_LOG_OUTPUT_RATE_MSG_PER_SEC = 2;
	
	/**
	 * Constructor for running EchoServer via command line.
	 * The parameters have to be set via setParameters.
	 * 
	 * @param pHost Host the application is running on
	 * @param pIdentity Identity of the caller
	 */
	public EchoServer(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
		
		mName = null;
	}
	
	protected void started()
	{
		try {
			// enable single datagram connections to echo server
			Description requ = getDescription();
			requ.set(CommunicationTypeProperty.DATAGRAM);
			
			// register at FoG
			Binding tBinding = getHost().bind(null, mName, requ, getIdentity());
			
			// create object, which adds EchoService objects
			// to each incoming connection
			mServerSocket = new Service(false, null)
			{
				public void newConnection(Connection pConnection)
				{
					Session tSession = new EchoSession();
					
					// start event processing
					tSession.start(pConnection);
					
					// add it to list of ongoing connections
					if(mSessions == null) {
						mSessions = new LinkedList<Session>();
					}
					mSessions.add(tSession);
				}
			};
			mServerSocket.start(tBinding);
		}
		catch (NetworkException tExc) {
			terminated(tExc);
		}
	}
	
	public boolean isRunning()
	{
		return(mServerSocket != null);
	}
	
	public synchronized void exit()
	{
		if(isRunning()) {
			if(mSessions != null) {
				while(!mSessions.isEmpty()) {
					mSessions.getFirst().closed();
				}
				mSessions.clear();
			}
			
			mServerSocket.stop();
			mServerSocket = null;
			terminated(null);
		}
	}
	
	@Override
	public void setParameters(String[] pParameters) throws InvalidParameterException
	{
		if(pParameters.length >= 2) {
			mName = SimpleName.parse(pParameters[1]);
			
			// optional: reflect or not?
			if(pParameters.length > 2) {
				mReflect = Boolean.parseBoolean(pParameters[2]);
			}
		} else {
			throw new InvalidParameterException("Required parameter: EchoServer <name>");
		}
	}
	
	/**
	 * Session object handling the events for a connection.
	 */
	class EchoSession extends Session
	{
		public EchoSession()
		{
			super(false, mLogger, null);
		}
		
		@Override
		public boolean receiveData(Object pData)
		{
			boolean tRes = false;
			getLogger().trace(this, "Got data : " + pData);
			// log received text in a rate limited way in order to
			// ensure reduced output by high rate senders
			mLogReceivedMsgAction.trigger(pData.toString());
			
			// do we send answers back?
			if(mReflect) {
				// send same string as answer back to sender
				Connection tConnection = getConnection();
				if(tConnection != null) {
					try {
						if(pData instanceof Serializable) {
							tConnection.write((Serializable) pData);
						} else {
							tConnection.write(pData.toString());
						}
						tRes = true;
					}
					catch(NetworkException tExc) {
						getLogger().warn(this, "echoing failed for: " +pData, tExc);
					}
				} else {
					getLogger().err(this, "no socket for answering available");
				}
			}
			
			// check, if we received an exit command
			if(CMD_TERMINATE.equals(pData)) {
				exit();
			}
			
			return tRes;
		}
		
		public void closed()
		{
			super.closed();
			if(mSessions != null) {
				mSessions.remove(this);
			}
		}
		
		private void logReceivedMsg(String pText)
		{
			mLogger.log(this, pText);
		}
		
		private RateLimitedAction<String> mLogReceivedMsgAction = new RateLimitedAction<String>(getHost().getTimeBase(), MAX_LOG_OUTPUT_RATE_MSG_PER_SEC) {
			@Override
			protected void doAction(String pParameter)
			{
				logReceivedMsg("received: " +pParameter);
			}
			
			@Override
			protected void firstIgnoreEvent()
			{
				logReceivedMsg("Logging output will be reduced to " +MAX_LOG_OUTPUT_RATE_MSG_PER_SEC +" msg/sec due to high rate.");
			}
		};
	};


	private Service mServerSocket;
	private SimpleName mName;
	private LinkedList<Session> mSessions;
	private boolean mReflect = true;

}
