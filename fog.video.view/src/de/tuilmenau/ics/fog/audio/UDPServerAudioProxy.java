/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.audio;

import de.tuilmenau.ics.fog.application.Session;
import de.tuilmenau.ics.fog.application.UDPServerProxy;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * UDP server socket mapped to FoG for audio streaming.
 */
public class UDPServerAudioProxy extends UDPServerProxy
{
	static boolean sRunningAudioServers[] = { false, false, false, false };
	public static int sMaxRunningServers = 4;
	private int mAudioServerNumber = -1;
	
	public static final Namespace NAMESPACE_AUDIO = new Namespace("audio");
	
	/**
	 * Constructor for running UDPProxy for audio streaming via command line.
	 * The parameters have to be set via setParameters.
	 * 
	 * @param pHost Host the application is running on
	 * @param pIdentity Identity of the caller
	 */
	public UDPServerAudioProxy(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
		getLogger().log(this, "Created UDPServerAudioProxy");
	}
	
	private String findServerSlot()
	{
		String tResult = "";
		boolean tFound = false;
		
		for (mAudioServerNumber = 0; mAudioServerNumber < sMaxRunningServers; mAudioServerNumber++){
			if (!sRunningAudioServers[mAudioServerNumber]) {
				tFound = true;
				sRunningAudioServers[mAudioServerNumber] = true;
				break;				
			}
		}
		
		if (tFound) {
			tResult = "AudioServer" + mAudioServerNumber;
		}
		else{
			mAudioServerNumber = -1;
		}
		
		return tResult;		
	}
	
	public UDPServerAudioProxy(Host pHost, Identity pIdentity, String pDestName, int pDestPort, int pRecPort)
	{
		super(pHost, pIdentity);
		getLogger().log(this, "Created UDPServerAudioProxy with destination and receive port");
		
		mDestName = findServerSlot();
		if (!"".equals(mDestName)) {
			mCEP = createCEP(mDestName, pDestPort, pRecPort);
		}
	}

	@Override
	protected Session createCEP(String pDestName, int pDestPort, int pRecPort)
	{
		try {
			return new ConnectionEndPointUDPAudioProxy(getLogger(), pDestName, pDestPort, pRecPort);
		}		       
		catch(Exception tExc) {
			getLogger().log(this, "UDP port " +pRecPort +" couldn't be opened because of " +tExc);
			return null;
		}
	}
	
	@Override
	protected String determineDestinationName(String[] pParameters)
	{
		return findServerSlot();
	}
	
	@Override
	protected Name createServerName(String pDestName) throws InvalidParameterException
	{
		return new SimpleName(NAMESPACE_AUDIO, pDestName);
	}
	
	protected void finalize() throws Throwable
	{
		super.finalize();
		if (mAudioServerNumber != -1) {
			sRunningAudioServers[mAudioServerNumber] = false;
		}
		getLogger().log(this, "Destroyed UDPServerAudioProxy");
	}
}
