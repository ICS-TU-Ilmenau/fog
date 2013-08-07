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
import java.util.LinkedList;

import org.eclipse.ui.IWorkbenchPartSite;

import de.tuilmenau.ics.fog.eclipse.ui.commands.Command;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.SelectFromListDialog;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

public class DeleteNode extends Command
{
	private Node mNode = null;
	private AutonomousSystem mAs = null;
	private IWorkbenchPartSite mSite = null;
			
	public DeleteNode()
	{
	}

	@Override
	public void init(IWorkbenchPartSite pSite, Object pObject)
	{
		Logging.log(this, "INIT - object parameter is " + pObject);

		mSite = pSite;
		if(pObject instanceof Node) {
			mNode = (Node) pObject;
		} else if(pObject instanceof HRMController) {
			mNode = ((HRMController)pObject).getNode();
		} else {
			throw new RuntimeException(this +" requires a Node object instead of " + pObject +" to proceed.");
		}
		mAs = mNode.getAS();
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

	private void deleteNode()
	{
		if (mNode != null){
			// delete the node
			mNode.getAS().executeCommand("remove node " + mNode.toString());
		}else{
			Logging.warn(this,  "Invalid bus selection found");
		}
	}
	
	private void showAckDialog()
	{
		
	}
}
