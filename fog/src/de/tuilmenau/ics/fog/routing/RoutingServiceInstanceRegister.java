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
import de.tuilmenau.ics.fog.routing.simulated.PartialRoutingService;
import de.tuilmenau.ics.fog.routing.simulated.RemoteRoutingService;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.middleware.JiniHelper;

public class RoutingServiceInstanceRegister
{
	private final static String GLOBAL_ROUTING_SERVICE_NAME = "FoG Routing Service";

	public static RoutingServiceInstanceRegister getInstance()
	{
		if(sInstance == null) {
			sInstance = new RoutingServiceInstanceRegister();
		}
		
		return sInstance;
	}
	
	/**
	 * Registers a RS instance under a name
	 * 
	 * @param name Name of the routing service instance
	 * @param rs Reference to instance
	 */
	public void put(String name, RemoteRoutingService rs)
	{
		sRoutingServiceInstances.put(name, rs);
	}
	
	/**
	 * Getting named instances of routing service
	 * 
	 * @param name Name of the routing service instance
	 * @return Instance or null, if name not defined
	 */
	public RemoteRoutingService get(String name)
	{
		return sRoutingServiceInstances.get(name);
	}
	
	public Collection<RemoteRoutingService> getAll()
	{
		return sRoutingServiceInstances.values();
	}
	
	public RemoteRoutingService create(EventHandler timeBase, Logger parentLogger, String name, RemoteRoutingService parentRS)
	{
		return new PartialRoutingService(timeBase, parentLogger, name, parentRS);
	}
	
	public static RemoteRoutingService getGlobalRoutingService(Simulation pSim)
	{
		Logger tLogger = Logging.getInstance();
		RemoteRoutingService tRS = sSingletonRoutingService;

		// first try: local RS
		if(tRS == null) {
			tRS = (RemoteRoutingService) JiniHelper.getService(RemoteRoutingService.class, GLOBAL_ROUTING_SERVICE_NAME);
			
			// no Jini available or no RS registered?
			if(tRS == null) {
				tLogger.log("No IRoutingServices available from JINI: Creating local one.");
	
				// create new one and try to register it
				if(sSingletonRoutingService == null) {
					sSingletonRoutingService = new PartialRoutingService(pSim.getTimeBase(), pSim.getLogger(), GLOBAL_ROUTING_SERVICE_NAME, null);
					
					JiniHelper.registerService(RemoteRoutingService.class, sSingletonRoutingService, GLOBAL_ROUTING_SERVICE_NAME);
				}
				tRS = sSingletonRoutingService;
			} else {
				tLogger.log("Using RoutingService provided via Jini");
			}
		}
		
		return tRS;
	}

	private static RoutingServiceInstanceRegister sInstance = null;
	private static PartialRoutingService sSingletonRoutingService = null;
	
	private HashMap<String, RemoteRoutingService> sRoutingServiceInstances = new HashMap<String, RemoteRoutingService>();
}
