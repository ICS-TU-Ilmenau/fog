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
package de.tuilmenau.ics.fog.packets.statistics;


import java.rmi.Remote;
import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;

public interface IRerouteMaster extends Remote
{
	/**
	 * This method is a special kind of execute. It gives the object running the rerouting experiment special treatment
	 * for remote executions.
	 *  
	 * @param pElement has to be a valid forwarding element
	 * @param pPacket has to be some kind of serializable data put into a packet
	 * @throws NetworkException if in most cases the execute method of the remote site is unable to handle the answer
	 */
	//public boolean tell(ForwardingElement pElement, Packet pPacket) throws RemoteException;
	
	public boolean tell(Packet pPacket) throws RemoteException;
	
	public String getTarget();
	
	public String getSource();
	
	public int getCurrentBrokenType();
}
