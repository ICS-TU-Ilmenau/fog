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

import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalRoutingService;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.scenario.NodeConfigurator;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used to configure nodes that are newly created.
 *
 */
public class NodeConfiguratorHRM implements NodeConfigurator
{

//	public static final String NAME = "hierarchical";
	
	public NodeConfiguratorHRM()
	{
		
	}

	@Override
	public void configure(String pName, AutonomousSystem pAS, Node pNode)
	{
		mNode = pNode;
		
		Logging.log(this, "###### CONFIGURING NODE " + pName + " -START ###### ");
		
		// create a new HRM instance for this node
		HierarchicalRoutingService tHRS = new HierarchicalRoutingService(pNode);
		
		// register HRM instance as routing service for the current node
		pNode.getHost().registerRoutingService(tHRS);

		Logging.log(this, "###### CONFIGURING NODE " + pName + " -END ###### ");
	}

	public String toString()
	{
		String tResult = getClass().getSimpleName() + (mNode != null ? "@" + mNode.toString() : "");
		
		return tResult;
	}
	
	private Node mNode = null;
}
