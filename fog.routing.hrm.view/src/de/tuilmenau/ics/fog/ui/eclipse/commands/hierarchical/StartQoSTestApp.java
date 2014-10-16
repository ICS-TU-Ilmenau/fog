/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - QoS test APP
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.commands.hierarchical;

import java.awt.event.ActionListener;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.app.routing.QoSTestApp;
import de.tuilmenau.ics.fog.eclipse.ui.commands.EclipseCommand;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.SelectFromListDialog;
import de.tuilmenau.ics.fog.eclipse.ui.menu.MenuCreator;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Command for starting up a QoSTestApp at a host.
 */
public class StartQoSTestApp extends EclipseCommand
{
	/**
	 * Stores the selection of the last call in order to ease the GUI handling.
	 */
	private static int sLastSelection = -1;
	
	/**
	 * Executes the command
	 *
	 * @param pObject the object parameter
	 */
	@Override
	public void execute(Object pObject)
	{
		Logging.log(this, "INIT - object parameter is " + pObject);
		Node tLocalNode = null;
		
		if(pObject instanceof Node) {
			tLocalNode = (Node) pObject;
		} else if(pObject instanceof HRMController) {
			tLocalNode = ((HRMController)pObject).getNode();
		} else {
			throw new RuntimeException(this +" requires a Node object instead of " + pObject +" to proceed.");
		}
		
		if(tLocalNode != null) {
			// delete the node
			AutonomousSystem tLocalNodeAS = tLocalNode.getAS();

			/**
			 * determine all nodes from the simulation
			 */
			LinkedList<Node> tNodeList = new LinkedList<Node>();
			for(AutonomousSystem tAS : tLocalNodeAS.getSimulation().getAllAS()) {
				for(Node tNode : tAS.getNodelist().values()) {
					tNodeList.add(tNode);
				}
			}

			/**
			 * Let the user select the target(s) for the probe packet
			 */
			// list possible targets
			LinkedList<String> tPossibilities = new LinkedList<String>();
			tPossibilities.add(ProbeRouting.SEND_TO_ALL_ADDRESSES_OF_TARGET_NODE);
			for (Node tNode : tNodeList){
				tPossibilities.add(tNode.getName());
			}
			// show dialog
			int tSelection = SelectFromListDialog.open(getSite().getShell(), "Send probe packets from " + tLocalNode.toString(), "From " + tLocalNode.toString() + " to ..?", (sLastSelection >= 0 ? sLastSelection : 1), tPossibilities);
			if (tSelection >= 0){
				sLastSelection = tSelection;			
				String tDestinationNodeName = tPossibilities.get(tSelection);
				Logging.log(this, "Selected destination node: " + tDestinationNodeName + "(" + tSelection + ")");
	
				QoSTestApp tQoSTestApp = new QoSTestApp(tLocalNode, tDestinationNodeName);
				
				Logging.log(this, "Starting the \"onCreation\" action");
				MenuCreator tMenuCreator = new MenuCreator(getSite());
				ActionListener action = tMenuCreator.getCreationAction(tQoSTestApp);
				if(action != null) {
					action.actionPerformed(null);
				}

				tQoSTestApp.start();					
			} else {
				Logging.err(this, "Cannot run QoS test app. The user canceled the destination select dialog.");
			}
		} else {
			Logging.err(this, "Missing reference to a Node. Can not run 'remove node' command.");
		}
	}
}
