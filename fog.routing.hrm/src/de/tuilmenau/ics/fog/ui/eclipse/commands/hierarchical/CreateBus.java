/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.commands.hierarchical;

import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.eclipse.ui.commands.SilentCommand;
import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;


public class CreateBus extends SilentCommand
{
	/**
	 * Stores the next number for the next created node.
	 */
	private static int sNextNumber = 0;
	
	public CreateBus()
	{
	}

	@Override
	public void init(Object pObject)
	{
		if(pObject instanceof Node) {
			mAs = ((Node) pObject).getAS();
		}
		else if(pObject instanceof IAutonomousSystem) {
			mAs = (IAutonomousSystem) pObject;
		} else {
			throw new RuntimeException(this +" requires an AutonomousSystem object instead of " + pObject +" to proceed.");
		}
	}

	@Override
	public void main() throws RemoteException
	{
		if(mAs != null) {
			mAs.executeCommand("create bus bus" + sNextNumber++);
		} else {
			Logging.err(this, "Missing reference to an autonomous system. Can not run 'create bus' command.");
		}
	}

	private IAutonomousSystem mAs;
}
