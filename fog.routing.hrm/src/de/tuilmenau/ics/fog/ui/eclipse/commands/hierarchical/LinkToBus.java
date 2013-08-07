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

public class LinkToBus extends Command
{
	private Node mSourceNode = null;
	private AutonomousSystem mAs = null;
	private IWorkbenchPartSite mSite = null;
	private static String sLastSelectedBusName = null;
	private ILowerLayer mSelectedBus = null;
			
	public LinkToBus()
	{
	}

	@Override
	public void init(IWorkbenchPartSite pSite, Object pObject)
	{
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
			showBusDialog();
			
			createLinkToBus();
		} else {
			Logging.err(this, "Missing reference to a Source Node. Can not run 'create link' command.");
		}
	}

	private void createLinkToBus()
	{
		if (mSelectedBus != null){
			// attach the node to the bus
			try {
				mSourceNode.getAS().executeCommand("connect " + mSourceNode.toString() + " " + mSelectedBus.getName());
			} catch (RemoteException e) {
				Logging.err(this, "Got a remote exception when attaching node " + mSourceNode.toString() + " to bus " + mSelectedBus.toString());
			}
		}else{
			Logging.warn(this,  "Invalid bus selection found");
		}
	}
	
	private void showBusDialog()
	{
		// determine how many busses exist in the network
		int tAsBusCount = mSourceNode.getAS().getBuslist().keySet().size();
		
		// allocate structure for storing names of possible destination busses
		LinkedList<String> tPossibleBusNames = new LinkedList<String>();

		// determine names of possible destination busses
		int i = 0;
		int tPreSelectedBusNr = 0;
		Logging.log(this, "Found " + tAsBusCount + " busses in the current AS \"" + mAs.toString() + "\"");
		for(String tBusName : mSourceNode.getAS().getBuslist().keySet()) {
			// check the string array boundaries	
			if ((sLastSelectedBusName != null) && (sLastSelectedBusName == tBusName)){
				Logging.log(this, "    ..possible bus " + i + ": \"" + tBusName + "\" [used last time]");
				tPreSelectedBusNr = i;
			}else{
				Logging.log(this, "    ..possible bus " + i + ": \"" + tBusName + "\"");
			}
			tPossibleBusNames.add(tBusName);
			i++;
		}

		// ask the user to which bus should the node be attached to
		int tSelectedBusNr = SelectFromListDialog.open(mSite.getShell(), "Select bus", "At which bus should node " + mSourceNode.toString() + " be attached to?", tPreSelectedBusNr, tPossibleBusNames);

		Logging.log(this, "Source node: " + mSourceNode);

		String tBusName = tPossibleBusNames.get(tSelectedBusNr);
		mSelectedBus = mAs.getBusByName(tBusName);
		Logging.log(this, "Selected bus: " + mSelectedBus + "(" + tBusName + ")");

		if (mSelectedBus != null){
			// store the selected bus name for the next time
			sLastSelectedBusName = tBusName;
		}
	}
}
