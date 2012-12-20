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
package de.tuilmenau.ics.fog;

import java.rmi.RemoteException;


/**
 * Interface represents a remote object, which can run commands.
 * These worker objects are used in order to share the load of
 * a simulation between different hosts.
 */
public interface IWorker extends ICommand
{
	/**
	 * @return Name of worker client.
	 * @throws RemoteException on error
	 */
	public String getName() throws RemoteException;
	
	/**
	 * @return Number of autonomous systems running on this worker client.
	 * @throws RemoteException on error
	 */
	public int getNumberAS() throws RemoteException;
	
}
