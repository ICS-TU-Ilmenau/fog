/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
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
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.EnterStringDialog;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.SelectFromListDialog;
import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.RouteSegmentDescription;
import de.tuilmenau.ics.fog.routing.RoutingServiceInstanceRegister;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.DestinationApplicationProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ProbeRoutingProperty;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.NameMappingService;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.simulated.RemoteRoutingService;
import de.tuilmenau.ics.fog.routing.simulated.RoutingServiceAddress;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.SimpleName;

/**
 * In order to create simulations this class sends packets from one node to another randomly chosen node. Or from one 
 * packets are sent to all other nodes within the network. The last case is to iteratively walk through the nodes of a network
 * and send packets to all other nodes. In that case you can determine the stretch of your system.
 *  
 *
 */
public class ProbeRouting extends Command
{
	/**
	 * Defines the node name which is used to send a packet to all nodes. 
	 */
	private static final String ALL_NODES = "all nodes";
	
	/**
	 * Stores a reference to the NMS instance.
	 */
	private NameMappingService mNMS = null;

	/**
	 * Stores the selection of the last call in order to ease the GUI handling.
	 */
	private static int sLastTargetSelection = -1;

	private Node mNode = null;
	private IWorkbenchPartSite mSite;
	
	public ProbeRouting()
	{
		
	}

	/**
	 * Initializes this Command.
	 *
	 * @param pSite the SWT site for this Command
	 * @param pObject the object parameter
	 */
	@Override
	public void init(IWorkbenchPartSite site, Object object)
	{
		mSite = site;
		if(object instanceof Node) {
			mNode = (Node) object;
		} else if(object instanceof HRMController) {
			mNode = ((HRMController)object).getNode();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void main() throws Exception
	{
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
			tPossibilities.add(ALL_NODES);
			for (Node tNode : tNodeList){
				tPossibilities.add(tNode.getName());
			}
			// show dialog
			int tTargetSelection = SelectFromListDialog.open(mSite.getShell(), "Target(s) selection", "To which target(s) should the probe packet be send?", (sLastTargetSelection >= 0 ? sLastTargetSelection : 1), tPossibilities);
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
				if (tTargetName.equals(ALL_NODES)){
					tSendToAllNodes = true;
				}
	
				/**
				 * Get a reference to the naming-service
				 */
				try {
					mNMS = HierarchicalNameMappingService.getGlobalNameMappingService();
				} catch (RuntimeException tExc) {
					mNMS = HierarchicalNameMappingService.createGlobalNameMappingService(mNode.getAS().getSimulation());
				}
	
				// send to all nodes?
				if(!tSendToAllNodes) {	
					/**
					 * We determine detailed data about the target node here.
					 * This is the same as if the user would have directly selected this string. Thus, the two following operations are allowed - and eases the handling of our FoG GUI.
					 */
					// get a reference to the target node: this is possible for the GUI only
					Node tTargetNode = tNodeList.get(tTargetSelection - 1 /* be aware of "all nodes" entry */);				
					// get the name of the target node
					Name tTargetNodeName = tTargetNode.getCentralFN().getName();
	
					/**
					 * Send a probe-packet to each HRMID, which is found in the NMS instance. 
					 */
					sendProbeConnectionRequest(tTargetNodeName, tTargetNode);
				} else {
					for(Node tTargetNode : tNodeList) {
						/**
						 * We determine detailed data about the target node here.
						 * This is the same as if the user would have directly selected this string. Thus, the following operation is allowed - and eases the handling of our FoG GUI.
						 */
						// get the name of the target node
						Name tTargetNodeName = tTargetNode.getCentralFN().getName();
	
						/**
						 * Send a probe-packet to each HRMID, which is found in the NMS instance. 
						 */
						sendProbeConnectionRequest(tTargetNodeName, tTargetNode);
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
	 * @param pTargetNodeName the name (e.g., HRMID) of the target
	 * @param pTargetNode the target node (reference is only used for debugging purposes)
	 */
	@SuppressWarnings("unused")
	private void sendProbeConnectionRequest(Name pTargetNodeName, Node pTargetNode)
	{
		Logging.log(this, "Sending probe packet to " + pTargetNodeName + ", which belongs to node " + pTargetNode);
		
		// check if we have a valid NMS
		if (mNMS == null){
			Logging.err(this, "Reference to NMS is invalid, cannot send a packet to " + pTargetNodeName);
			return;
		}
		
		// send a HRM probe-packet to each registered address for the given target name
		try {
			for(NameMappingEntry<?> tNMSEntryForTarget : mNMS.getAddresses(pTargetNodeName)) {
				if(tNMSEntryForTarget.getAddress() instanceof HRMID) {
					// get the HRMID of the target node
					HRMID tTargetNodeHRMID = (HRMID)tNMSEntryForTarget.getAddress();
					
					Logging.log(this, "Found in the NMS the HRMID " + tTargetNodeHRMID.toString() + " for node " + pTargetNode);  
					
					if ((HRMConfig.DebugOutput.GUI_SHOW_RELATIVE_ADDRESSES) || (!tTargetNodeHRMID.isRelativeAddress())){
						/**
						 * Connect to the destination node
						 */
						// create requirements with probe-routing property and DestinationApplication property
						Description tConnectionReqs = new Description();
						tConnectionReqs.set(new ProbeRoutingProperty(mNode.getCentralFN().getName().toString()));
						tConnectionReqs.set(new DestinationApplicationProperty(HRMController.ROUTING_NAMESPACE));
						// probe connection
						Connection tConnection = null;
						Logging.log(this, "\n\n\nProbing a connection to " + tTargetNodeHRMID + " with requirements " + tConnectionReqs);
						try {
							tConnection = mNode.getHost().connectBlock(tTargetNodeHRMID, tConnectionReqs, mNode.getIdentity());
						} catch (NetworkException tExc) {
							Logging.err(this, "Unable to connecto to " + tTargetNodeHRMID, tExc);
							tConnection = null;
						}
						
						/**
						 * Disconnect by closing the connection
						 */
						if(tConnection != null) {
							Logging.log(this, "        ..found valid connection to " + tTargetNodeHRMID + "(" + pTargetNodeName + ")");
							tConnection.close();
						}
					}else{
						Logging.log(this, "     ..address " + tTargetNodeHRMID + " is ignored because it is a relative one");
					}
				}else{
					Logging.log(this, "Found in the NMS the unsupported address " + tNMSEntryForTarget.getAddress() + " for node " + pTargetNode);
				}
			}
		} catch (RemoteException tExc) {
			Logging.err(this, "Unable to determine addresses for node " + pTargetNode, tExc);
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
