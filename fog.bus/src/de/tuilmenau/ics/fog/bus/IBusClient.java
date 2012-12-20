/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Bus
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
package de.tuilmenau.ics.fog.bus;

import java.rmi.Remote;
import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.packets.Packet;


/**
 * Interface for the remote call part of the BusClient class.
 * Needed for the automatic proxy generation of JERI.
 */
public interface IBusClient extends Remote
{
	public void handlePacketFromServer(Packet pPacket) throws RemoteException;
}
