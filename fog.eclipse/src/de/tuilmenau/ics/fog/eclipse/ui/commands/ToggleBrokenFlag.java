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

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.topology.Breakable;
import de.tuilmenau.ics.fog.topology.Breakable.Status;
import de.tuilmenau.ics.fog.ui.commands.Command;


public class ToggleBrokenFlag implements Command
{
	@Override
	public void execute(Object object) throws RemoteException
	{
		if(object instanceof Breakable) {
			((Breakable) object).setBroken(((Breakable) object).isBroken() == Status.OK, Config.Routing.ERROR_TYPE_VISIBLE);
		} else {
			throw new RuntimeException(this +" requires a " +Breakable.class +" object to proceed. Instead of " +object +".");
		}
	}
}
