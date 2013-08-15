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

import java.util.Random;

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.routing.hierarchical.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalRoutingService;
import de.tuilmenau.ics.fog.scenario.NodeConfigurator;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Decoration;
import de.tuilmenau.ics.fog.ui.Logging;


/**
 * This class is used to configure nodes that are newly created.
 *
 */
public class NodeConfiguratorHierarchicalRadiusOnly implements NodeConfigurator
{

	public static final String NAME = "hierarchical";
	
	public NodeConfiguratorHierarchicalRadiusOnly()
	{
		
	}

	@Override
	public void configure(String pName, AutonomousSystem pAS, Node pNode)
	{
		HierarchicalRoutingService hRS = new HierarchicalRoutingService(pNode);
		
		FoGEntity.registerRoutingService(pNode, hRS);
		Decoration.getInstance(Node.class).setDecorator(pNode, hRS.initiateCoordinator());
		
		Logging.getInstance().log(this, "Available decoration types are by now: " + Decoration.getClassTypes());
		
		if(HierarchicalConfig.Routing.ELECTION_BEGINS_IMMEDIATLY_AFTER_SETUP) {
			if(pAS.getSimulation().getEvents() == null || !pAS.getSimulation().getEvents().contains(new ElectionEvent())) {
				pAS.getSimulation().addEvent(new ElectionEvent());
			} else if (pAS.getSimulation().getEvents().contains(new ElectionEvent())) {
				/*
				 * hashCode of ElectionEvent always returns 0 because it is only needed once
				 * So do nothing now!
				 */
			}
		}

		Random tRandomGenerator = new Random(System.currentTimeMillis());
		float tCurrentRandomNumber = tRandomGenerator.nextFloat();
		
		for(int i = 0; i < HierarchicalConfig.Routing.HIERARCHY_LEVEL_AMOUNT; i++) {
			pNode.getParameter().put("BULLY_PRIORITY_LEVEL_" + i, ( HierarchicalConfig.INHERIT_PRIORITY_TO_UPPER_LEVELS ? tCurrentRandomNumber : tRandomGenerator.nextFloat()));
		}
	}
}
