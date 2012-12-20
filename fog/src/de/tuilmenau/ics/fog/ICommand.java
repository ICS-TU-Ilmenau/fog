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

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ICommand extends Remote
{
	/**
	 * Methods executes a command in a context
	 *  
	 * @param pCmd command to be executed.
	 * @return true=successfully executed; false=on error
	 * @throws RemoteException on error
	 */
	public boolean executeCommand(String pCmd) throws RemoteException;

}
