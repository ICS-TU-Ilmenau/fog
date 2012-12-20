/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Importer
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
package de.tuilmenau.ics.fog.tools;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDromedar extends Remote
{
	public int cardinalityWorkers() throws RemoteException;
	public int cardinalityTotalNodes() throws RemoteException;
	
	public float avgNodesPerWorker() throws RemoteException;
	public float avgNodesPerAS() throws RemoteException;
	public float avgASPerWorker() throws RemoteException;
	
	public boolean updAvgNodesPerWorker() throws RemoteException;
	public boolean updAvgNodesPerAS() throws RemoteException;
	public boolean updAvgASPerWorker() throws RemoteException;
}
