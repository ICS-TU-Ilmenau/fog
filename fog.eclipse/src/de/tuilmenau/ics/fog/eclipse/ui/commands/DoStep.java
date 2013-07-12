/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.commands;

import java.rmi.RemoteException;

import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.commands.Command;


public class DoStep implements Command
{
	@Override
	public void execute(Object object) throws RemoteException
	{
		IAutonomousSystem as = null;
		
		if(object instanceof Node) {
			as = ((Node) object).getAS();
		}
		else if(object instanceof IAutonomousSystem) {
			as = (IAutonomousSystem) object;
		}
		
		if(as != null) {
			as.executeCommand("time");
		} else {
			Logging.err(this, "No link to an autonomous system. Can not run 'time' command.");
		}
	}
}
