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
package de.tuilmenau.ics.fog.routing;

import java.util.Collection;
import java.util.HashMap;

import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.routing.simulated.DelegationPartialRoutingService;
import de.tuilmenau.ics.fog.routing.simulated.PartialRoutingService;
import de.tuilmenau.ics.fog.routing.simulated.RemoteRoutingService;
import de.tuilmenau.ics.fog.routing.simulated.RootRoutingService;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.graph.GraphProvider;
import de.tuilmenau.ics.graph.RoutableGraph;
import de.tuilmenau.ics.middleware.JiniHelper;


public class RoutingServiceInstanceRegister implements GraphProvider
{
	/**
	 * @return Global register for simulation (!= null)
	 */
	public static RoutingServiceInstanceRegister getInstance(Simulation sim)
	{
		RoutingServiceInstanceRegister global = (RoutingServiceInstanceRegister) sim.getGlobalObject(RoutingServiceInstanceRegister.class);
		if(global == null) {
			global = new RoutingServiceInstanceRegister();
			
			sim.setGlobalObject(RoutingServiceInstanceRegister.class, global);
		}
		
		return global;
	}
	
	/**
	 * Switches between PartialRS and DelegationPRS
	 */
	public void setRoutingServiceType(boolean delegation)
	{
		useDelegationType = delegation;
	}
	
	/**
	 * Registers a RS instance under a name
	 * 
	 * @param name Name of the routing service instance
	 * @param rs Reference to instance
	 */
	public void put(String name, RemoteRoutingService rs)
	{
		routingServiceInstances.put(name, rs);
		
		routingServiceEntityGraph.add(rs);
	}
	
	/**
	 * Getting named instances of routing service
	 * 
	 * @param name Name of the routing service instance
	 * @return Instance or null, if name not defined
	 */
	public RemoteRoutingService get(String name)
	{
		return routingServiceInstances.get(name);
	}
	
	public Collection<RemoteRoutingService> getAll()
	{
		return routingServiceInstances.values();
	}
	
	public void link(RemoteRoutingService rs, RemoteRoutingService parent)
	{
		if((rs != null) && (parent != null)) {
			routingServiceEntityGraph.link(parent, rs, "routing for " +rs);
		}
	}
	
	public RemoteRoutingService create(Simulation sim, EventHandler timeBase, Logger parentLogger, String name, RemoteRoutingService parentRS)
	{
		if(useDelegationType) {
			return new DelegationPartialRoutingService(sim, timeBase, parentLogger, name, parentRS);
		} else {
			return new PartialRoutingService(sim, timeBase, parentLogger, name, parentRS);
		}
	}
	
	@Override
	public RoutableGraph getGraph()
	{
		return routingServiceEntityGraph;
	}
	
	public RemoteRoutingService getGlobalRoutingService(Simulation pSim)
	{
		Logger tLogger = Logging.getInstance();
		RemoteRoutingService tRS = singletonRoutingService;

		// first try: local RS
		if(tRS == null) {
			tRS = (RemoteRoutingService) JiniHelper.getService(RemoteRoutingService.class, RootRoutingService.ROOT_ROUTING_SERVICE_NAME);
			
			// no Jini available or no RS registered?
			if(tRS == null) {
				tLogger.log("No IRoutingServices available from JINI: Creating local one.");
	
				// create new one and try to register it
				if(singletonRoutingService == null) {
					singletonRoutingService = new RootRoutingService(pSim);
					
					JiniHelper.registerService(RemoteRoutingService.class, singletonRoutingService, RootRoutingService.ROOT_ROUTING_SERVICE_NAME);
				}
				tRS = singletonRoutingService;
			} else {
				tLogger.log("Using RoutingService provided via Jini");
			}
		}
		
		return tRS;
	}

	private PartialRoutingService singletonRoutingService = null;	
	private boolean useDelegationType = false;
	private HashMap<String, RemoteRoutingService> routingServiceInstances = new HashMap<String, RemoteRoutingService>();
	private RoutableGraph<RemoteRoutingService, Object> routingServiceEntityGraph = new RoutableGraph<RemoteRoutingService, Object>();	
}
