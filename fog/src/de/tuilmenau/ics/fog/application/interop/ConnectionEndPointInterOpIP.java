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
package de.tuilmenau.ics.fog.application.interop;

import de.tuilmenau.ics.fog.application.InterOpIP;
import de.tuilmenau.ics.fog.application.Session;
import de.tuilmenau.ics.fog.application.InterOpIP.Transport;
import de.tuilmenau.ics.fog.exceptions.InvalidParameterException;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.util.SimpleName;


/**
 * CEP for managing the interoperation with IP world.
 * 
 * The CEP is usually used by the RoutingServiceIP to manage the interoperation proxies
 * for bridging packet streams from the IP world to FoG. For the opposite direction the FoG applications
 * call directly the InterOpIP class to initiate new bridges from FoG to IP.
 */
public class ConnectionEndPointInterOpIP extends Session
{
	public final static String INTEROP_PROTOCOL_ID = "InterOpIP";
	public final static String INTEROP_PROTOCOL_ADD = "add";
	public final static String INTEROP_PROTOCOL_REMOVE = "remove";

	public ConnectionEndPointInterOpIP(InterOpIP pInterOpApp)
	{
		super(false, pInterOpApp.getLogger(), null);
		mInterOpApp = pInterOpApp;
	}
	
	@Override
	public boolean receiveData(Object pData)
	{
		getLogger().trace(this, "Got packet at CEP of InterOpIP application with payload: " + pData);

		if(pData instanceof String[]) {
			String[] tTokenList = (String[])pData;
			getLogger().trace(this, "Got packet at CEP of InterOpIP application with string list of size " + tTokenList.length);
			for (int i = 0; i < tTokenList.length; i++) {
				getLogger().trace(this, "Token " + i + ": " + tTokenList[i]);
			}
			
			if ((tTokenList.length > 1) && INTEROP_PROTOCOL_ID.equals(tTokenList[0])) {
				getLogger().trace(this, "Got packet at CEP of InterOpIP application with InterOpIP token list");
				
				/*-----	action "new" ------------------------ 
				 		parameter 1 = server name
				 		parameter 2 = transport type
				 		parameter 3 = listener port 
				 		parameter 4 = QoS bandwidth
				 		parameter 5 = QoS delay
				 		parameter 6 = ordering activation
				 -------------------------------------------*/
				if (INTEROP_PROTOCOL_ADD.equals(tTokenList[1])){
					getLogger().trace(this, "InterOpIP token list triggers process \"add\"");
					// needed parameters for call to InterOpIPv4
					SimpleName tServerName = null;
					Description tDescription = null;
					Transport tTransport = Transport.TCP;
					int tServerPort = 0;
					
					// parse FoG server name
					try {
						tServerName = SimpleName.parse(tTokenList[2]);
					} catch (InvalidParameterException tExc) {
						getLogger().err(this, "Failed to parse given server name", tExc);
						return false;
					}

					// parse transport type
					if ("tcp".equals(tTokenList[3]))
						tTransport = Transport.TCP;
					else if ("udp".equals(tTokenList[3]))
						tTransport = Transport.UDP;
					else
						getLogger().err(this, "Unknown transport protocol " +tTokenList[3] +". Using TCP.");
					
					
					// parse QoS value
					tDescription = Description.createQoS(Boolean.parseBoolean(tTokenList[7]), Integer.parseInt(tTokenList[6]), Integer.parseInt(tTokenList[5]));
						
					tServerPort = Integer.parseInt(tTokenList[4]);
					mInterOpApp.addIpListener(tServerName, tDescription, tTransport, tServerPort);					
				}else
				{
					/*-----	action "remove" -------------------- 
					 		parameter 1 = server name
			 		-------------------------------------------*/
					if (INTEROP_PROTOCOL_REMOVE.equals(tTokenList[1])){
						getLogger().trace(this, "InterOpIP token list triggers process \"remove\"");
						// needed parameters for call to InterOpIPv4
						SimpleName tServerName = null;
						
						try {
							tServerName = SimpleName.parse(tTokenList[2]);
						} catch (InvalidParameterException tExc) {
							getLogger().err(this, "Failed to parse given server name '" +tTokenList[2] +"'", tExc);
							return false;
						}
	
						mInterOpApp.removeIpListener(tServerName);					
					}else
						getLogger().err(this, "Unknown token list received");
				}
			}
		}
		
		return true;
	}

	private InterOpIP mInterOpApp;
}
