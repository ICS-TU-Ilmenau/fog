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

import de.tuilmenau.ics.fog.eclipse.ui.commands.EclipseCommand;
import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;


public class CreateNode extends EclipseCommand
{
	/**
	 * Stores the next number for the next created node.
	 */
	private static int sNextNumber = 0;

	/**
	 * Constructor
	 */
	public CreateNode()
	{
	}

	/**
	 * Executes the command
	 *
	 * @param pObject the object parameter
	 */
	@Override
	public void execute(Object pObject)
	{
		Logging.log(this, "INIT - object parameter is " + pObject);

		if(pObject instanceof Node) {
			mAs = ((Node) pObject).getAS();
		}
		else if(pObject instanceof IAutonomousSystem) {
			mAs = (IAutonomousSystem) pObject;
		} else {
			throw new RuntimeException(this +" requires an AutonomousSystem object instead of " + pObject +" to proceed.");
		}

		if(mAs != null) {
			try {
				mAs.executeCommand("create node N" + sNextNumber++);
			} catch (RemoteException tExc) {
				Logging.err(this, "Cannot create node", tExc);
			}
		} else {
			Logging.err(this, "Missing reference to an autonomous system. Can not run 'create node' command.");
		}
	}

	private IAutonomousSystem mAs;
}
