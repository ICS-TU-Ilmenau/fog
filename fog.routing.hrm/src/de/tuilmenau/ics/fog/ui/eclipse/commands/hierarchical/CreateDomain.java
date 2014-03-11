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

import java.util.LinkedList;

import de.tuilmenau.ics.fog.eclipse.ui.commands.EclipseCommand;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.SelectFromListDialog;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

public class CreateDomain extends EclipseCommand
{
	private Node mSourceNode = null;
	private int mDomainSize = 0;
	private AutonomousSystem mAs = null;
	private static int sLastDomainSize = 4;
	private Description mBusAttributes = null;
	
	public CreateDomain()
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
			mSourceNode = (Node) pObject;
		} else if(pObject instanceof HRMController) {
			mSourceNode = ((HRMController)pObject).getNode();
		} else {
			throw new RuntimeException(this +" requires a Node object instead of " + pObject +" to proceed.");
		}
		mAs = mSourceNode.getAS();

		if(mSourceNode != null) {
			showBusDialogs();
			
			showNodeDialog();
			
			createDomain();
		} else {
			Logging.err(this, "Missing reference to a Source Node. Can not run 'create link' command.");
		}
	}

	/**
	 * Triggers the actual link creation process.
	 */
	private void createDomain()
	{
		if(mDomainSize > 1){
			// create name for bus
			String tDomainName = "domain_" + mSourceNode.toString() + "_" + Integer.toString(mDomainSize) + "nodes";			
			
			mSourceNode.getAS().executeCommand("create bus " + tDomainName /* TODO: QoS parameter */);
			
			// connect the nodes at both end points of the link
			mSourceNode.getAS().executeCommand("connect " + mSourceNode.toString() + " " + tDomainName);
			
			for(int i = 1; i < mDomainSize; i++){
				String tNodeName = "d_" + mSourceNode.toString() + "#" + Integer.toString(i);
				mSourceNode.getAS().executeCommand("create node " + tNodeName);
				mSourceNode.getAS().executeCommand("connect " + tNodeName + " " + tDomainName);
			}
		}
	}
	
	/**
	 * Shows the dialog which allows the user to select the destination node.
	 */
	private void showNodeDialog()
	{
		mDomainSize = 0;
		
		// allocate structure for storing names of possible destination nodes
		LinkedList<String> tPossibleNodeNames = new LinkedList<String>();

		// determine names of possible destination nodes
		int tPreSelectedNodeNr = 0;
		for(int i = 1; i < 11; i++) {
			if ((sLastDomainSize > 1) && (sLastDomainSize - 2 == i)){
				Logging.log(this, "    ..possible nodes: " + i + " [used last time]");
				tPreSelectedNodeNr = i;
			}else{
				Logging.log(this, "    ..possible nodes " + i);
			}
			tPossibleNodeNames.add(Integer.toString(i));
		}

		// ask the user to which bus should the node be attached to
		int tSelectedNodeCounter = SelectFromListDialog.open(getSite().getShell(), "Creating a domain of nodes arround " + mSourceNode + ": ", "How many nodes should be created for the the domain with " + mSourceNode + "?", tPreSelectedNodeNr, tPossibleNodeNames);

		Logging.log(this, "Source node: " + mSourceNode);

		if(tSelectedNodeCounter > -1){
			mDomainSize = tSelectedNodeCounter + 2;
			Logging.err(this, "Creating a domain with " + mDomainSize + " nodes.");
	
			if (mDomainSize > 1){
				// store the selected bus name for the next time
				sLastDomainSize = mDomainSize;
			}else{
				Logging.warn(this,  "Invalid destination node found");
			}
		}else{
			Logging.log(this, "User canceled the dialog");
		}
	}
	
	/**
	 * Shows the dialog which allows the user to select the attributes for the bus which has to be created for the desired link.
	 */
	private void showBusDialogs()
	{
		mBusAttributes = new Description();
		//TODO: show dialog to allow user to select link attributes
	}
}
