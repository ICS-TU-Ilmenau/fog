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

import java.util.LinkedList;

import org.eclipse.ui.IWorkbenchPartSite;

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.eclipse.ui.commands.Command;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.EnterStringDialog;
import de.tuilmenau.ics.fog.eclipse.ui.dialogs.SelectFromListDialog;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.RoutingServiceInstanceRegister;
import de.tuilmenau.ics.fog.routing.hierarchical.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.AddressLimitationProperty;
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
import de.tuilmenau.ics.fog.ui.eclipse.dialogs.hierarchical.RegionLimitationDialog;

/**
 * @author ossy
 *
 * @deprecated Test should use official way to establish route via an application and "connect". 
 */
public class SendPacket extends Command
{
	private Node mNode;
	private IWorkbenchPartSite mSite;

	/**
	 * 
	 */
	public SendPacket()
	{
		
	}

	@Override
	public void init(IWorkbenchPartSite site, Object object)
	{
		mSite = site;
		if(object instanceof Node) {
			mNode = (Node) object;
		} else if(object instanceof Coordinator) {
			mNode = ((Coordinator)object).getReferenceNode();
		}
	}

	@Override
	public void main() throws Exception
	{
		if(mNode != null) {
			LinkedList<String> tTargets = new LinkedList<String>();
			
			String tDestination = EnterStringDialog.open(mSite.getShell(), "Target of packet", "Please enter a target:", mNode.getName(), null);
			
			Description tDescription = new Description();
			if(HierarchicalConfig.Routing.ENABLE_REGION_LIMITATION) {
				AddressLimitationProperty tProperty = RegionLimitationDialog.open(mSite.getShell());
				if(tProperty != null) {
					tDescription.add(tProperty);
				}
			} else {
				tDescription = null;
			}
			LinkedList<String> tPossibilities = new LinkedList<String>();
			tPossibilities.add("YES");
			tPossibilities.add("NO");
			int tCompareToRoutingService = SelectFromListDialog.open(mSite.getShell(), "Comparisson run?", "Do you want to send a reference packet?", 1, tPossibilities);
			Logging.log(this, "You chose option " + tCompareToRoutingService);
			LinkedList<String> tPosibilities = new LinkedList<String>();
			for(String tTarget : mNode.getAS().getNodelist().keySet()) {
				tPosibilities.add(tTarget);
			}
			//int tResult = SelectFromListDialog.open(mSite.getShell(), "Region Limitation Dialog", "Which region do you not want?", 0, tPosibilities);
			boolean tCompleteSimulation = false;
			if(!tDestination.equals("ALL") && !tDestination.equals("") ) {
				tTargets.add(tDestination);
			} else if(tDestination.equals("") && !tDestination.equals("ALL")) {
				for(String tTarget : mNode.getAS().getNodelist().keySet()) {
					tTargets.add(tTarget);
				}
			} else {
				tCompleteSimulation = true;
			}
			
			NameMappingService tNMS = null;
			try {
				tNMS = HierarchicalNameMappingService.getGlobalNameMappingService();
			} catch (RuntimeException tExc) {
				HierarchicalNameMappingService.createGlobalNameMappingService(mNode.getAS().getSimulation());
			}
			
			if(!tCompleteSimulation) {	
				for(String tTarget : tTargets) {
					HRMID tTargetAddress = null;
					Node tTargetNode = mNode.getAS().getNodeByName(tTarget);
					if(tTargetNode == null) {
						for(IAutonomousSystem tAS : mNode.getAS().getSimulation().getAS()) {
							if(tAS instanceof AutonomousSystem) {
								tTargetNode = ((AutonomousSystem)tAS).getNodeByName(tTarget);
								if(tTargetNode != null) {
									break;
								}
							}
						}
					}
					RoutingServiceAddress tGlobalTargetIdentification = null;
					FoGEntity entity = (FoGEntity) tTargetNode.getHost().getLayer(FoGEntity.class);
					
					tGlobalTargetIdentification = (RoutingServiceAddress)entity.getRoutingService().getNameFor(entity.getCentralFN());
					if(tTargetNode != null) {
						Name tTargetName = entity.getCentralFN().getName();
						for(NameMappingEntry tEntry : tNMS.getAddresses(tTargetName)) {
							if(tEntry.getAddress() instanceof HRMID) {
								tTargetAddress =  (HRMID) tEntry.getAddress();
								Route tRoute = new Route();
								try {
									if(tDescription == null) {
										tRoute.add(new RouteSegmentAddress(tTargetAddress));
									} else {
										for(Property tProperty : tDescription) {
											if(tProperty instanceof AddressLimitationProperty) {
												tRoute = mNode.getRoutingService().getRoute(mNode.getCentralFN(), tTargetAddress, tDescription, null);
											}
										}
									}
									
									Logging.log("Sending packet from " + mNode + " to " + tTarget);
									sendHello(mNode, tRoute, tTargetNode);
									break;
								} catch (RoutingException tExc) {
									Logging.err(this, "Unable to send from " + mNode + " to " + tTargetAddress + " with requirements " + tDescription, tExc);
									continue;
								}
								

							}
						}
					}
					if(tCompareToRoutingService == 0) {
						RoutingServiceAddress tSource = (RoutingServiceAddress)mNode.getRoutingService().getNameFor(mNode.getCentralFN());
						RemoteRoutingService tGRS = RoutingServiceInstanceRegister.getGlobalRoutingService(mNode.getAS().getSimulation());
						Route tRoute = tGRS.getRoute(tSource, tGlobalTargetIdentification, null, null);
						
						mNode.getLogger().log(mNode, "Route to " + tGlobalTargetIdentification + " is " + tRoute);
						sendHello(mNode, tRoute, tTargetNode);
					}
				}
			} else {
				/*
				 * Declare all targets at first
				 */
				LinkedList<Node> tNodeList = new LinkedList<Node>();
				for(AutonomousSystem tAS : mNode.getAS().getSimulation().getLocalAS()) {
					for(Node tNode : tAS.getNodelist().values()) {
						tNodeList.add(tNode);
					}
				}
				
				for(Node tSourceNode : tNodeList) {
					for(Node tTargetNode : tNodeList) {
						if(tTargetNode != tSourceNode) {
							HRMID tTargetAddress = null;
							RoutingServiceAddress tGlobalTargetIdentification = null;
							FoGEntity tEntity = (FoGEntity) tTargetNode.getHost().getLayer(FoGEntity.class);
							tGlobalTargetIdentification = (RoutingServiceAddress)tEntity.getRoutingService().getNameFor(tEntity.getCentralFN());
							if(tTargetNode != null) {
								Name tTargetName = tEntity.getCentralFN().getName();
								for(NameMappingEntry tEntry : tNMS.getAddresses(tTargetName)) {
									if(tEntry.getAddress() instanceof HRMID) {
										tTargetAddress =  (HRMID) tEntry.getAddress();
										Route tRoute = new Route();
										tRoute.add(new RouteSegmentAddress(tTargetAddress));
										
										Logging.log("Sending packet from " + tSourceNode + " to " + tTargetNode);
										sendHello(tSourceNode, tRoute, tTargetNode);
										break;
									}
								}
							}
							if(tCompareToRoutingService == 0) {
								RoutingServiceAddress tSource = (RoutingServiceAddress)tSourceNode.getRoutingService().getNameFor(tSourceNode.getCentralFN());
								RemoteRoutingService tGRS = RoutingServiceInstanceRegister.getGlobalRoutingService(tSourceNode.getAS().getSimulation());
								Route tRoute = tGRS.getRoute(tSource, tGlobalTargetIdentification, null, null);
								
								tSourceNode.getLogger().log(tSourceNode, "Route to " + tGlobalTargetIdentification + " is " + tRoute);
								sendHello(tSourceNode, tRoute, tTargetNode);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Helper function for constructing and send a hello-packet
	 */
	private void sendHello(Node pSourceNode, Route pRoute, Node pTargetNode)
	{
		Packet tPacket = new Packet(pRoute, "hallo");
		
		mNode.getAuthenticationService().sign(tPacket, mNode.getCentralFN().getOwner());
		tPacket.setSourceNode(pSourceNode.getName());
		tPacket.setTargetNode(pTargetNode.getName());
		
		pSourceNode.getCentralFN().handlePacket(tPacket, pSourceNode.getCentralFN());
	}
}
