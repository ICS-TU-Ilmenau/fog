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

import org.eclipse.ui.IWorkbenchPartSite;

import de.tuilmenau.ics.fog.eclipse.ui.commands.Command;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

public class DeleteNode extends Command
{
	private Node mNode = null;
			
	public DeleteNode()
	{
	}

	/**
	 * Initializes this Command.
	 * 
	 * @param pObject the object parameter
	 */
	@Override
	public void init(IWorkbenchPartSite pSite, Object pObject)
	{
		Logging.log(this, "INIT - object parameter is " + pObject);

		if(pObject instanceof Node) {
			mNode = (Node) pObject;
		} else if(pObject instanceof HRMController) {
			mNode = ((HRMController)pObject).getNode();
		} else {
			throw new RuntimeException(this +" requires a Node object instead of " + pObject +" to proceed.");
		}
	}

	@Override
	public void main() throws RemoteException
	{
		if(mNode != null) {
			showAckDialog();
			
			deleteNode();
		} else {
			Logging.err(this, "Missing reference to a Node. Can not run 'remove node' command.");
		}
	}

	/**
	 * Triggers the actual deletion process.
	 */
	private void deleteNode()
	{
		if (mNode != null){
			// delete the node
			mNode.getAS().executeCommand("remove node " + mNode.toString());
		}else{
			Logging.warn(this,  "Invalid bus selection found");
		}
	}
	
	/**
	 * Shows a dialog to allow the user to acknowledge/cancel the deletion process.
	 */
	private void showAckDialog()
	{
		
	}
}
