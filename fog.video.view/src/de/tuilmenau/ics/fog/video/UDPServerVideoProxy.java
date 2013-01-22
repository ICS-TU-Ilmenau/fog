/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.video;

import de.tuilmenau.ics.fog.application.UDPServerProxy;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * UDP server socket mapped to FoG for video streaming. 
 * (See Config.Transfer.DEBUG_INTEROP_PACKETS to enable packet debugging)
 * 
 */
public class UDPServerVideoProxy extends UDPServerProxy
{
	private static boolean sRunningVideoServers[] = { false, false, false, false };
	public static int sMaxRunningServers = sRunningVideoServers.length;
	private int mVideoServerNumber = -1;
	
	public static final Namespace NAMESPACE_VIDEO = new Namespace("video");
	
	/**
	 * Constructor for running UDPProxy for video streaming via command line.
	 * The parameters have to be set via setParameters.
	 * 
	 * @param pHost Host the application is running on
	 * @param pIdentity Identity of the caller
	 */
	public UDPServerVideoProxy(Host pHost, Identity pIdentity)
	{
		super(pHost, pIdentity);
		getLogger().log(this, "Created UDPServerVideoProxy");
	}
	
	private String findServerSlot()
	{
		String tResult = "";
		boolean tFound = false;
		
		for (mVideoServerNumber = 0; mVideoServerNumber < sMaxRunningServers; mVideoServerNumber++) {
			if (!sRunningVideoServers[mVideoServerNumber]) {
				tFound = true;
				sRunningVideoServers[mVideoServerNumber] = true;
				break;				
			}
		}
		
		if (tFound) {
			tResult = "VideoServer" + mVideoServerNumber;
		}
		else{
			mVideoServerNumber = -1;
		}
		
		return tResult;		
	}
	
	public UDPServerVideoProxy(Host pHost, Identity pIdentity, String pDestName, int pDestPort, int pRecPort)
	{
		super(pHost, pIdentity);
		getLogger().log(this, "Created UDPServerVideoProxy with destination and receive port");
		
		mDestName = findServerSlot();
		if (!"".equals(mDestName)) {
			mCEP = createCEP(mDestName, pDestPort, pRecPort);
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
		return new SimpleName(NAMESPACE_VIDEO, pDestName);
	}
	
	protected void finalize() throws Throwable
	{
		super.finalize();
		if (mVideoServerNumber != -1) {
			sRunningVideoServers[mVideoServerNumber] = false;
		}
		getLogger().log(this, "Destroyed UDPServerVideoProxy");
	}
}
