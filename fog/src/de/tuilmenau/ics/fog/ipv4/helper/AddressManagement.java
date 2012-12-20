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
package de.tuilmenau.ics.fog.ipv4.helper;

import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.middleware.JiniHelper;


/**
 * Helper class for getting IP addresses.
 */
public class AddressManagement implements IAddressManagement
{
	private byte[] sIPofRouter = { 10, 0, 0, 0 };
	private byte[] sIPofRouterPrefix = { 11, 0, 0, 0 };
	private int sNextASNumber = 0;
	
	private static final String HELPER_NAME = "GLOBAL_IP_ADDRESS_MANAGEMENT";
	
	private static AddressManagement sHelperSingleton = null;
	
	
	public static IAddressManagement getInstance()
	{
		IAddressManagement tRS = sHelperSingleton;

		// first try: local RS
		if(tRS == null) {
			tRS = (IAddressManagement) JiniHelper.getService(IAddressManagement.class, HELPER_NAME);
			
			// no Jini available or no RS registered?
			if(tRS == null) {
				Logging.getInstance().log("No Helper available from JINI: Creating local one.");
	
				// create new one and try to register it
				if(sHelperSingleton == null) {
					sHelperSingleton = new AddressManagement();
					
					JiniHelper.registerService(IAddressManagement.class, sHelperSingleton, HELPER_NAME);
				}
				tRS = sHelperSingleton;
			} else {
				Logging.getInstance().log("Using Helper provided via Jini");
			}
		} else {
			Logging.getInstance().log("Using local Helper");
		}
		
		return tRS;
	}
	
	
	private static int getPos(byte b)
	{
		if(b < 0) return b +256;
		else return b;
	}
	
	public static String sGetNextIP()
	{
		try {
			return getInstance().getNextIP();
		} catch (RemoteException tExc) {
			throw new RuntimeException("Can not get next IP.", tExc);
		}
	}
	
	public String getNextIP() throws RemoteException
	{
		sIPofRouter[3]++;
		if(sIPofRouter[3] == 255) {
			sIPofRouter[3] = 1;
			sIPofRouter[2]++;
			if(sIPofRouter[2] == 255) {
				sIPofRouter[2] = 0;
				sIPofRouter[1]++;
				
				// do not change [0] since the "10" prefix is reserved for router addresses
				if(sIPofRouterPrefix[1] == 0) {
					throw new RuntimeException(this +" - Not enough IP addresses for routers available.");
				}
			}
		}
		
		return getPos(sIPofRouter[0]) +"." +getPos(sIPofRouter[1]) +"." +getPos(sIPofRouter[2]) +"." +getPos(sIPofRouter[3]);
	}

	public static String sGetNextIPPrefix()
	{
		try {
			return getInstance().getNextIPPrefix();
		} catch (RemoteException tExc) {
			throw new RuntimeException("Can not get next IP prefix.", tExc);
		}
	}
	
	public String getNextIPPrefix() throws RemoteException
	{
		sIPofRouterPrefix[1]++;
		if(sIPofRouterPrefix[1] == 255) {
			sIPofRouterPrefix[1] = 0;
			sIPofRouterPrefix[0]++;
			
			if(sIPofRouterPrefix[0] == 0) {
				throw new RuntimeException(this +" - Not enough IP prefix available.");
			}
		}
		
		return getPos(sIPofRouterPrefix[0]) +"." +getPos(sIPofRouterPrefix[1]) +"." +getPos(sIPofRouterPrefix[2]) +"." +getPos(sIPofRouterPrefix[3]) +"/16";
	}


	public static int sGetNextASNumber()
	{
		try {
			return getInstance().getNextASNumber();
		} catch (RemoteException tExc) {
			throw new RuntimeException("Can not get next AS number.", tExc);
		}
	}

	public int getNextASNumber() throws RemoteException
	{
		return sNextASNumber++;
	}


}
