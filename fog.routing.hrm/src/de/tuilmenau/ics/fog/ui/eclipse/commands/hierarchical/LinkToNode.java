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
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.eclipse.dialogs.hierarchical.ConfigureLinkDialog;
import de.tuilmenau.ics.fog.ui.eclipse.dialogs.hierarchical.ConfigureLinkDialogResults;

public class LinkToNode extends Command
{
	private Node mSourceNode = null;
	private Node mSelectedDestinationNode = null;
	private AutonomousSystem mAs = null;
	private IWorkbenchPartSite mSite = null;
	private static String sLastSelectedNodeName = null;
	private Description mCreatedBusAttributes = null;
	
	public LinkToNode()
	{
	}

	@Override
	public void init(IWorkbenchPartSite pSite, Object pObject)
	{
		Logging.log(this, "INIT - object parameter is " + pObject);

		mSite = pSite;
		if(pObject instanceof Node) {
			mSourceNode = (Node) pObject;
		} else if(pObject instanceof HRMController) {
			mSourceNode = ((HRMController)pObject).getNode();
		} else {
			throw new RuntimeException(this +" requires a Node object instead of " + pObject +" to proceed.");
		}
		mAs = mSourceNode.getAS();
	}

	@Override
	public void main() throws RemoteException
	{
		if(mSourceNode != null) {
			showBusDialogs();
			
			showNodeDialog();
			
			createBusAndLinkToNodes();
		} else {
			Logging.err(this, "Missing reference to a Source Node. Can not run 'create link' command.");
		}
	}

	private void createBusAndLinkToNodes()
	{
		// create name for bus
		String tBusName = "bus_" + mSourceNode.toString() + "_" + mSelectedDestinationNode.toString();			
		
		// create the bus, allow multiple bus between same nodes here
		String tBusNameTmp = tBusName;
		int i = 0;
		while(mSourceNode.getAS().containsBus(tBusNameTmp)){
			tBusNameTmp = tBusName + "_" + Integer.toString(i);
			i++;
		}
		tBusName = tBusNameTmp;
		mSourceNode.getAS().executeCommand("create bus " + tBusName /* TODO: QoS parameter */);
		
		// connect the nodes at both end points of the link
		mSourceNode.getAS().executeCommand("connect " + mSourceNode.toString() + " " + tBusName);
		mSourceNode.getAS().executeCommand("connect " + mSelectedDestinationNode.toString() + " " + tBusName);
	}
	
	/**
	 * Shows the dialog which allows the user to select the destination node.
	 */
	@SuppressWarnings("unused")
	private void showNodeDialog()
	{
		// determine how many nodes exist in the network
		int tAsNodeCount = mSourceNode.getAS().getNodelist().keySet().size();
		
		// allocate structure for storing names of possible destination nodes
		LinkedList<String> tPossibleNodeNames = new LinkedList<String>();

		// determine names of possible destination nodes
		int i = 0;
		int tPreSelectedNodeNr = 0;
		Logging.log(this, "Found " + tAsNodeCount + " destination nodes in the current AS \"" + mAs.toString() + "\"");
		for(String tNodeName : mSourceNode.getAS().getNodelist().keySet()) {
			// check the string array boundaries	
			if ((sLastSelectedNodeName != null) && (sLastSelectedNodeName == tNodeName)){
				Logging.log(this, "    ..possible node " + i + ": \"" + tNodeName + "\" [used last time]");
				tPreSelectedNodeNr = i;
			}else{
				Logging.log(this, "    ..possible node " + i + ": \"" + tNodeName + "\"");
			}
			tPossibleNodeNames.add(tNodeName);
			i++;
		}

		// ask the user to which bus should the node be attached to
		int tSelectedNodeNr = SelectFromListDialog.open(mSite.getShell(), "Select destination node", "To which node should node " + mSourceNode.toString() + " have a new link?", tPreSelectedNodeNr, tPossibleNodeNames);

		Logging.log(this, "Source node: " + mSourceNode);

		String tNodeName = tPossibleNodeNames.get(tSelectedNodeNr);
		mSelectedDestinationNode = mAs.getNodeByName(tNodeName);
		Logging.log(this, "Selected destination node: " + mSelectedDestinationNode.toString() + "(" + tNodeName + ")");

		if (mSelectedDestinationNode != null){
			// store the selected bus name for the next time
			sLastSelectedNodeName = tNodeName;
		}else{
			Logging.warn(this,  "Invalid destination node found");
		}
	}
	
	/**
	 * Shows the dialog which allows the user to select the attributes for the bus.
	 */
	private void showBusDialogs()
	{
		mCreatedBusAttributes = new Description();
		//TODO: show dialog to allow user to select link attributes
	}
}
