/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.commands.hierarchical;

import java.rmi.RemoteException;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.app.routing.QoSTestApp;
import de.tuilmenau.ics.fog.eclipse.ui.commands.EclipseCommand;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.SelectFromListDialog;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.SimpleName;

/**
 * This GUI command is used to create a test routing from one FoG node to another based on the HRM routing service.  
 */
public class HRMProbeRouting extends EclipseCommand
{
	/**
	 * Defines the node name which is used to send a packet to all nodes. 
	 */
	public static final String SEND_TO_ALL_ADDRESSES_OF_TARGET_NODE = "all nodes";
	
	/**
	 * Stores a reference to the NMS instance.
	 */
	private NameMappingService mNMS = null;

	/**
	 * Stores the selection of the last call in order to ease the GUI handling.
	 */
	private static int sLastTargetSelection = -1;

	/**
	 * Stores the current node where the probe routing should start
	 */
	private Node mNode = null;
	
	/**
	 * Constructor
	 */
	public HRMProbeRouting()
	{		
	}

	/**
	 * Initializes this Command.
	 *
	 * @param pSite the SWT site for this Command
	 * @param pObject the object parameter
	 */
	@Override
	public void execute(Object object)
	{
		if(object instanceof Node) {
			mNode = (Node) object;
		} else if(object instanceof HRMController) {
			mNode = ((HRMController)object).getNode();
		}
		
		if(mNode != null) {
			/**
			 * determine all nodes from the simulation
			 */
			LinkedList<Node> tNodeList = new LinkedList<Node>();
			for(AutonomousSystem tAS : mNode.getAS().getSimulation().getAllAS()) {
				for(Node tNode : tAS.getNodelist().values()) {
					tNodeList.add(tNode);
				}
			}
			
			/**
			 * Let the user select the target(s) for the probe packet
			 */
			// list possible targets
			LinkedList<String> tPossibilities = new LinkedList<String>();
			tPossibilities.add(SEND_TO_ALL_ADDRESSES_OF_TARGET_NODE);
			for (Node tNode : tNodeList){
				tPossibilities.add(tNode.getName());
			}
			// show dialog
			int tTargetSelection = SelectFromListDialog.open(getSite().getShell(), "Send probe packet from " + mNode.toString(), "From " + mNode.toString() + " to ..?", (sLastTargetSelection >= 0 ? sLastTargetSelection : 1), tPossibilities);
			if (tTargetSelection >= 0){
				sLastTargetSelection = tTargetSelection;			
				String tTargetName = tPossibilities.get(tTargetSelection);
				Logging.log(this, "Selected target: " + tTargetName + "(" + tTargetSelection + ")");
				// store target selection as string
				
				/**
				 * Has user selected "all nodes"?
				 */
				// send to all nodes of the simulation?
				boolean tSendToAllNodes = false;
				if (tTargetName.equals(SEND_TO_ALL_ADDRESSES_OF_TARGET_NODE)){
					tSendToAllNodes = true;
				}
	
				/**
				 * Get a reference to the naming-service
				 */
				try {
					mNMS = HierarchicalNameMappingService.getGlobalNameMappingService(mNode.getAS().getSimulation());
				} catch (RuntimeException tExc) {
					mNMS = HierarchicalNameMappingService.createGlobalNameMappingService(mNode.getAS().getSimulation());
				}
	
				/**
				 * We determine detailed data about the target node here.
				 * This is the same as if the user would have directly selected this string. Thus, the two following operations are allowed - and eases the handling of the FoG GUI.
				 */

				// send to all nodes?
				if(!tSendToAllNodes) {	
					/**
					 * Send a probe-packet to each HRMID, which is found in the NMS instance. 
					 */
					sendProbeConnectionRequest(tTargetName, 53, 1 * 1000);
				} else {
					for(Node tTargetNode : tNodeList) {
						/**
						 * Send a probe-packet to each HRMID, which is found in the NMS instance. 
						 */
						sendProbeConnectionRequest(tTargetNode.getName(), 53, 1 * 1000);
					}
				}
			}else{
				Logging.log(this, "User canceled the target selection dialog");
			}
		}
	}

	/**
	 * Send a HRM probe-packet to the defined target.
	 * 
	 * @param pDestinationNodeNameStr the name (as string) of the destination node
	 * @param pDesiredDelay the desired delay
	 * @param pDataRate the desired data rate
	 */
	private void sendProbeConnectionRequest(String pDestinationNodeNameStr, int pDesiredDelay, int pDataRate)
	{
		Logging.log(this, "\n\n\n############## Sending probe packet to " + pDestinationNodeNameStr);
	
		// check if we have a valid NMS
		if (mNMS == null){
			Logging.err(this, "Reference to NMS is invalid, cannot send a packet to " + pDestinationNodeNameStr);
			return;
		}
		
		// get the name of the central FN
		Name tDestinationNodeName = new SimpleName(Node.NAMESPACE_HOST, pDestinationNodeNameStr);

		// send a HRM probe-packet to each registered address for the given target name
		try {
			
			for(NameMappingEntry<?> tNMSEntryForTarget : mNMS.getAddresses(tDestinationNodeName)) {
				if(tNMSEntryForTarget.getAddress() instanceof HRMID) {
					// get the HRMID of the target node
					HRMID tTargetNodeHRMID = (HRMID)tNMSEntryForTarget.getAddress();
					
					Logging.log(this, "Found in the NMS the HRMID " + tTargetNodeHRMID.toString() + " for node " + tDestinationNodeName);
				}
			}
			
			for(NameMappingEntry<?> tNMSEntryForTarget : mNMS.getAddresses(tDestinationNodeName)) {
				if(tNMSEntryForTarget.getAddress() instanceof HRMID) {
					// get the HRMID of the target node
					HRMID tTargetNodeHRMID = (HRMID)tNMSEntryForTarget.getAddress();
					
					Logging.log(this, "Probing the HRMID " + tTargetNodeHRMID.toString() + " for node " + tDestinationNodeName);  
					
					if ((HRMConfig.DebugOutput.GUI_SHOW_RELATIVE_ADDRESSES) || (!tTargetNodeHRMID.isRelativeAddress())){
						if(!tTargetNodeHRMID.isClusterAddress()){
							/**
							 * Connect to the destination node
							 */
							Connection tConnection = QoSTestApp.createProbeRoutingConnection(this, mNode, tTargetNodeHRMID, pDesiredDelay, pDataRate, false);
							
							/**
							 * Check if connect request was successful
							 */
							if(tConnection != null){
								Logging.log(this, "        ..found valid connection to " + tTargetNodeHRMID + "(" + tDestinationNodeName + ")");
								
								/**
								 * Send some test data
								 */
								for(int i = 0; i < 5; i++){
									try {
										Logging.log(this, "      ..sending test data " + i);
										tConnection.write("TEST DATA " + Integer.toString(i));
									} catch (NetworkException tExc) {
										Logging.err(this, "Couldn't send test data", tExc);
									}
								}

								/**
								 * Disconnect by closing the connection
								 */
								tConnection.close();
							}
						}else{
							// cluster address
						}

						Logging.log(this, "############## PROBING END ###########"); 
					}else{
						Logging.log(this, "     ..address " + tTargetNodeHRMID + " is ignored because it is a relative one");
					}
				}else{
					Logging.log(this, "Found in the NMS the unsupported address " + tNMSEntryForTarget.getAddress() + " for node " + tDestinationNodeName);
				}
			}
		} catch (RemoteException tExc) {
			Logging.err(this, "Unable to determine addresses for node " + tDestinationNodeName, tExc);
		}
	}

	/**
	 * Returns a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return getClass().getSimpleName() + "@" + mNode.toString();
	}
}
