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
package de.tuilmenau.ics.fog.application.util;

import java.rmi.Remote;
import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.topology.NeighborInformation;

/**
 * Inferface for observer of the neighbor list
 */
public interface LayerObserverCallback extends Remote
{
	/**
	 * Called if a new neighbor appears at this lower layer.
	 * Callback is only called if lower layer is not broken.
	 * 
	 * @param newNeighbor New neighbor attached to the bus.
	 * @throws RemoteException On error.
	 */
	public void neighborDiscovered(NeighborInformation newNeighbor) throws RemoteException;
	
	/**
	 * Called if a neighbor disappears from the lower layer.
	 * Callback is only called if lower layer is not broken.
	 * 
	 * @param oldNeighbor Neighbor disconnected from bus.
	 * @throws RemoteException On error.
	 */
	public void neighborDisappeared(NeighborInformation oldNeighbor) throws RemoteException;
	
	/**
	 * Called after lower layer was broken. The attached objects
	 * should check, if all there gates using this lower layer are
	 * ok.
	 * 
	 * @throws RemoteException On error.
	 */
	public void neighborCheck() throws RemoteException;
}