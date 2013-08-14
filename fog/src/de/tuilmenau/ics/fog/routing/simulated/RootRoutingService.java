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
package de.tuilmenau.ics.fog.routing.simulated;

import de.tuilmenau.ics.fog.topology.Simulation;


/**
 * Class is used for root object of partial routing service hierarchy.
 * Main purpose of own class is logging under a different class name.
 */
public class RootRoutingService extends PartialRoutingService
{
	public final static String ROOT_ROUTING_SERVICE_NAME = "FoG Routing Service";
	
	
	public RootRoutingService(Simulation pSim)
	{
		super(pSim, pSim.getTimeBase(), pSim.getLogger(), ROOT_ROUTING_SERVICE_NAME, null);
	}
}
