/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.commands.hierarchical;

import de.tuilmenau.ics.fog.eclipse.ui.commands.EclipseCommand;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

public class DeleteNode extends EclipseCommand
{			
	public DeleteNode()
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
		Node tNode = null;
		
		if(pObject instanceof Node) {
			tNode = (Node) pObject;
		} else if(pObject instanceof HRMController) {
			tNode = ((HRMController)pObject).getNode();
		} else {
			throw new RuntimeException(this +" requires a Node object instead of " + pObject +" to proceed.");
		}
		
		if(tNode != null) {
			// delete the node
			AutonomousSystem tAS = tNode.getAS();
			
			tAS.executeCommand("remove node " + tNode.toString());
		} else {
			Logging.err(this, "Missing reference to a Node. Can not run 'remove node' command.");
		}
	}
}
