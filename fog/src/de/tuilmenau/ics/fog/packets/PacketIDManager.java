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
package de.tuilmenau.ics.fog.packets;

import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.middleware.JiniHelper;

/**
 * Packet IDs are for debugging purposes and to create logs
 * about the route of packets. They do not exist in real-
 * world FoG implementations. This class provides a way to
 * create such IDs with a global JINI service.
 */
public class PacketIDManager implements IPacketIDManager
{
	private static final long serialVersionUID = 755013264316237950L;
	
	private static long ID = 0;
	private static PacketIDManager sSingletonIDManager = null;
	private final static String PACKET_ID_MANGER_NAME = "Packet ID Manager";
	
	public synchronized long getID() throws RemoteException
	{
		return ID++;
	}
	
	public static IPacketIDManager getSimulationPacketIDManager()
	{
		IPacketIDManager pIDManager = sSingletonIDManager;

		// first try: local RS
		if(pIDManager == null) {
			if(Config.Transfer.ENABLE_GLOBAL_PACKET_NUMBERS) {
				pIDManager = (IPacketIDManager) JiniHelper.getService(IPacketIDManager.class, PACKET_ID_MANGER_NAME);
			}
			
			// no Jini available or no RS registered?
			if(pIDManager == null) {
				Logging.log("No IPacketIDManagager available from JINI: Creating local one.");
	
				// create new one and try to register it
				if(sSingletonIDManager == null) {
					sSingletonIDManager = new PacketIDManager();
					
					JiniHelper.registerService(IPacketIDManager.class, sSingletonIDManager, PACKET_ID_MANGER_NAME);
				}
				pIDManager = sSingletonIDManager;
			}
		}
		
		return pIDManager;
	}
}
