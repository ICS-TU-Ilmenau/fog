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
 * Interface for the remote call part of the BusServer class.
 * Needed for the automatic proxy generation of JERI.
 */
public interface IBusServer extends Remote
{
	public void attach(IBusClient pNewnode) throws RemoteException;
	public void detach(IBusClient pNode) throws RemoteException;
	public int getNewID() throws RemoteException;
	public void handlePacketFromClient(Packet pPacket) throws RemoteException;
}
