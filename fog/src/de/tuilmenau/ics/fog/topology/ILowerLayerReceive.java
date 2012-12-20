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
package de.tuilmenau.ics.fog.topology;

import java.rmi.Remote;
import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.packets.Packet;



public interface ILowerLayerReceive extends Remote
{
	public enum Status { OK, UNKNOWN_ERROR, BROKEN }

	/**
	 * Enables the lower layer to check, weather the receiving node
	 * is marked as broken.
	 * 
	 * @return If the receiving node is broken or not
	 * @throws RemoteException on error
	 */
	public Status isBroken() throws RemoteException;
	
	/**
	 * Called by the lower layer in order to send a packet to the receiving
	 * node.
	 * 
	 * @param packet Packet to be transmitted. It is a copy of the original one and the node may change it without restrictions.
	 * @param from Lower layer information about the sender. Information enables the sending of an answer.
	 * @throws RemoteException on error
	 */
	public void handlePacket(Packet packet, NeighborInformation from) throws RemoteException;
	
	/**
	 * Called by the lower layer in order to inform the higher layer about
	 * the closing of the lower layer. After this call the lower layer is
	 * not available in the simulation any more.
	 * 
	 * @throws RemoteException on error
	 */
	public void closed() throws RemoteException;
}
