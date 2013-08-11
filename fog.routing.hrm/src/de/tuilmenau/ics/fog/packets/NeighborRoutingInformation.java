/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets;

import java.io.Serializable;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.LoggableElement;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * PACKET: This packet is used to inform the neighbor about the association between the central FN and the FN between the central FN and the bus.
 * 		   It is necessary to inform the neighbor about the FN which it should use to route to the central FN of a neighbor node.
 */
public class NeighborRoutingInformation extends LoggableElement implements Serializable
{

	/**
	 * Stores the L2Address of the central FN.
	 */
	private L2Address mCentralFN = null;
	
	/**
	 * Stores the L2Address of the FN which should be used as routing target 
	 */
	private L2Address mRoutingTargetFN = null;

	/**
	 * For using the class within (de-)serialization. 
	 */
	private static final long serialVersionUID = 7253912074438961613L;
	
	public NeighborRoutingInformation(L2Address pCentralFN, L2Address pRoutingTargetFN)
	{
		mCentralFN = pCentralFN;
		mRoutingTargetFN = pRoutingTargetFN;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING){
			Logging.log(getClass().getSimpleName() + "(CentralFN=" + getCentralFNL2Address()  + ", RoutingTargetFN=" + getRoutingTargetFNL2Address() + "): CREATED");
		}
	}
	
	/**
	 * Determine the name of the central FN
	 * 
	 * @return name of the central FN
	 */
	public L2Address getCentralFNL2Address()
	{
		return mCentralFN;
	}

	/**
	 * Determine the name of the FN, which should be used as routing target in case the central FN should be reached
	 * 
	 * @return name of the FN
	 */
	public L2Address getRoutingTargetFNL2Address()
	{
		return mRoutingTargetFN;
	}
	
	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(CentralFN=" + getCentralFNL2Address()  + ", RoutingTargetFN=" + getRoutingTargetFNL2Address() + ")";
	}
}
