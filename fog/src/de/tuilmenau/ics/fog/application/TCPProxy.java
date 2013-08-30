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

import de.tuilmenau.ics.fog.application.interop.ConnectionEndPointTCPProxy;
import de.tuilmenau.ics.fog.application.util.ServerCallback;
import de.tuilmenau.ics.fog.application.util.Service;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.facade.events.ErrorEvent;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * Proxy maps incomming FoG connections to a specific TCP service.
 * Destination server and port for the TCP connections are set via the constructor.
 */
public class TCPProxy extends Application implements ServerCallback
{
	/**
	 * Constructor for running TCPProxy via command line.
	 * The parameters have to be set via setParameters.
	 * 
	 * @param pHost Host the application is running on
	 * @param pIdentity Identity of the caller
	 */
	public TCPProxy(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
		
		mName = null;
		mPort = -1;
	}
	
	/**
	 * Constructor for running TCPProxy without a call of setParemters.
	 * 
	 * @param pHost Host the application is running on
	 * @param pIdentity Identity of the caller
	 * @param pName Destination name for TCP connection
	 * @param pPort Destination port number for TCP connection
	 */
	public TCPProxy(Host pHost, Identity pIdentity, String pName, int pPort)
	{
		super(pHost, pIdentity);
		
		mName = pName;
		mPort = pPort;
	}
	
	@Override
	public void setParameters(String[] pParameters) throws InvalidParameterException
	{
		if(pParameters.length >= 3) {
			mName = pParameters[1];
			try {
				mPort = Integer.parseInt(pParameters[2]);
			}
			catch(NumberFormatException tExc) {
				throw new InvalidParameterException("Invalid port number '" +pParameters[2] +"'", tExc);
			}
		} else {
			throw new InvalidParameterException("Required parameter: TCPProxy <destName> <destPort>");
		}
	}

	@Override
	public boolean openAck(LinkedList<Signature> pAuths, Description pDescription, Name pTargetName)
	{
		// accept all connections
		return true;
	}
	
	@Override
	public void newConnection(Connection pConnection)
	{
		try {
			ConnectionEndPointTCPProxy tSession = new ConnectionEndPointTCPProxy(mLogger, mName, mPort);
			tSession.start(pConnection);
		}
		catch(Exception tExc) {
			mLogger.warn(this, "openAck failes", tExc);
		}
	}
	
	@Override
	public void error(ErrorEvent cause)
	{
		terminated(cause.getException());
	}

	protected void started()
	{
		Binding tBinding = getLayer().bind(null, new SimpleName(HttpServer.NAMESPACE_HTTP, mName), getDescription(), getIdentity());
		
		mServerBinding = new Service(false, this);
		mServerBinding.start(tBinding);
	}
	
	@Override
	public void exit()
	{
		if(mServerBinding != null) {
			mServerBinding.stop();
			mServerBinding = null;
		}
	}
	
	@Override
	public boolean isRunning()
	{
		return (mServerBinding != null);
	}
	
	private Service mServerBinding;
	private String mName;
	private int mPort;
}
