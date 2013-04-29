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

import de.tuilmenau.ics.fog.application.interop.ConnectionEndPointUDPProxy;
import de.tuilmenau.ics.fog.application.util.ServerCallback;
import de.tuilmenau.ics.fog.application.util.Service;
import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Binding;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * UDP server socket mapped to FoG.
 */
public class UDPServerProxy extends Application implements ServerCallback
{
	/**
	 * Constructor for running TCPProxy via command line.
	 * The parameters have to be set via setParameters.
	 * 
	 * @param pHost Host the application is running on
	 * @param pIdentity Identity of the caller
	 */
	public UDPServerProxy(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
	}
	
	/**
	 * Method for creating the connection endpoint, which represents the
	 * native socket.
	 * In order to enable child classes to override this function, it is
	 * encapsulated in a single method.
	 * 
	 * @param pDestName Destination name of the socket
	 * @param pDestPort Destination port of the socket
	 * @param pRecPort Receiving port of the server socket
	 * @return Reference to the created CEP or null on error
	 */
	protected Session createCEP(String pDestName, int pDestPort, int pRecPort)
	{
		try {
			return new ConnectionEndPointUDPProxy(mLogger, pDestName, pDestPort, pRecPort);
		}
		catch(Exception tExc) {
			mLogger.warn(this, "open UDP port " +pRecPort +" failes.", tExc);
			return null;
		}
	}
	
	protected Name createServerName(String pDestName) throws InvalidParameterException
	{
		return SimpleName.parse(pDestName);
	}
	
	protected String determineDestinationName(String[] pParameters)
	{
		if(pParameters != null) {
			if(pParameters.length > 1) {
				return pParameters[1];
			}
		}
		
		return "";
	}
	
	@Override
	public void setParameters(String[] pParameters) throws InvalidParameterException
	{
		if(pParameters.length >= 4) {
			mDestName = determineDestinationName(pParameters);
			try {
				mCEP = createCEP(mDestName, Integer.parseInt(pParameters[2]), Integer.parseInt(pParameters[3]));
			}
			catch(Exception tExc) {
				throw new InvalidParameterException("Invalid parameters for UDPServerProxy.", tExc);
			}
		} else {
			throw new InvalidParameterException("Required parameter: UDPServerProxy <destName> <destPort> <recPort>");
		}
	}
	
	@Override
	public boolean openAck(LinkedList<Signature> pAuths, Description pDescription, Name pTargetName)
	{
		mLogger.trace(this, "Got an openAck call with auths " + pAuths + ", description " + pDescription + ", target " + pTargetName);
		if(mCEP == null) {
			mLogger.log(this, "openAck failes because of invalid UDP socket");
			return false;
		}
		
		return true;
	}
	
	@Override
	public void newConnection(Connection pConnection)
	{
		// TODO do we have to create a new CEP for each connection?
		//      in the old code, a single CEP was used.
		// TODO (TV) A new CEP per FoG connection is needed here, 
		//		because FoG2IP-CEPs are designed like Berkeley client sockets and 
		// 		support only one assigned FoG connection
		mCEP.start(pConnection);
	}

	@Override
	protected void started()
	{
		try {
			Binding tBinding = getHost().bind(null, createServerName(mDestName), getDescription(), getIdentity());
			mServerBinding = new Service(false, this);
			mServerBinding.start(tBinding);
		}
		catch(NetworkException tExc) {
			terminated(tExc);
		} catch (InvalidParameterException tExc) {
			terminated(tExc);
		}
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
	
	protected Service mServerBinding;
	protected Session mCEP;
	protected Name mServerName;
	protected String mDestName;
}
