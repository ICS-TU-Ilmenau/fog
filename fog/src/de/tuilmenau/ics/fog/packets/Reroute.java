/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets;

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.packets.statistics.ReroutingTestAgent;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ClientFN;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ConnectionEndPoint;
import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.transfer.manager.ProcessGateCollectionConstruction;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * This informs a node that it shall determine a new route to its target node.
 */
public class Reroute extends SignallingRequest
{
	private static final long serialVersionUID = -6190970661708707047L;
	
	public Reroute()
	{
		super();
	}
	
	@Override
	public boolean execute(ForwardingNode fn, Packet packet, Identity requester)
	{
		Logger log = fn.getNode().getLogger();

		// are we executed on an end point?
		if(fn instanceof ClientFN) {
			Process process = ((ClientFN) fn).getRelatedProcess();
			ForwardingNode source = process.getBase();
			ConnectionEndPoint CEP = ((ClientFN) fn).getConnectionEndPoint();
			Name tTarget = ((ClientFN) fn).getPeerRoutingName();
			
			// do we know the name of the destination binding?
			if(tTarget != null) {
				RoutingService rs = fn.getNode().getRoutingService();
				Route route = null;
				
				// calculate new route
				// TODO for routes with requirements, we have to start the signaling again!
				try {
					route = rs.getRoute(source, tTarget, CEP.getRequirements(), null); // TODO identity?
				}
				catch(NetworkException tExc) {
					log.warn(this, "No alternative route found.", tExc);
					
					if (packet.getData() instanceof ReroutingTestAgent) {
			            ((ReroutingTestAgent)packet.getData()).finish(fn, packet);
					}
				}
				
				// set new route for subsequent packets
				if(process instanceof ProcessGateCollectionConstruction) {
					log.trace(this, "Setting new route for " +fn +" to " +route);
					((ProcessGateCollectionConstruction) process).updateRoute(route, requester);
					
					// inform app, that QoS was/is degraded
					CEP.informAboutNetworkEvent();
				} else {
					log.warn(this, "Process " +((ClientFN) fn).getRelatedProcess() +" is not a GateCollectionConstruction one.");
					((ClientFN) fn).closed();
				}
				
			} else {
				log.warn(this, "Destination binding for " +CEP +" not known; closing.");
				((ClientFN) fn).closed();
			}
		} else {
			log.err(this, "Can not be executed on " +fn);
		}
		
		return true;
	}

}
