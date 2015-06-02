/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2015, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical;

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.scenario.NodeConfigurator;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.eclipse.SimulationCreatedEvent;

/**
 * This class is used to configure FoG nodes that are newly created with the HRM specific routing service.
 * The HRM routing service starts automatically the HRM controll instance for the node.
 */
public class HRMNodeConfigurator implements NodeConfigurator
{
	
	/**
	 * Stores the node which has to be configured
	 */
	private Node mNode = null;
	
	/**
	 * This function is not part of the concept.
	 * It is only used to provide a correct simulation environment.
	 */	
	private Simulation sLastSimulation = null;

	/**
	 * Constructor
	 */
	public HRMNodeConfigurator()
	{		
	}

	/**
	 * Configures the HRM specific parameters of the already create node.
	 * 
	 * @param pName the name of the node
	 * @param pAS the autonomous system to which the node belongs to
	 * @param pNode the node which has to be configured
	 */
	@Override
	public void configure(String pName, AutonomousSystem pAS, Node pNode)
	{
		Simulation tCurSimulation = pAS.getSimulation();
		
		if(sLastSimulation == null){
			sLastSimulation = tCurSimulation;
		}else{
			if(!tCurSimulation.equals(sLastSimulation)){
				Logging.warn(this, "####################### Detected simulation restart ####################### ");
				Logging.warn(this, "####################### Detected simulation restart ####################### ");
				Logging.warn(this, "####################### Detected simulation restart ####################### ");
				
				// trigger event: simulation restart
				HRMController.eventSimulationRestarted();
			}
			sLastSimulation = tCurSimulation;
		}
		
		mNode = pNode;
		
		Logging.log(this, "###### CONFIGURING NODE " + pName + " -START ###### ");
		
		// create a new HRM instance for this node
		HRMRoutingService tHRS = new HRMRoutingService(pNode);
		
		// register HRM instance as routing service for the current node
		FoGEntity.registerRoutingService(pNode, tHRS);

		Logging.log(this, "###### CONFIGURING NODE " + pName + " -END ###### ");

		if(HRMConfig.DebugOutput.BLOCK_HIERARCHY_UNTIL_END_OF_SIMULATION_CREATION){
			if(pAS.getSimulation().getPendingEvents() == null) {
				//TODO: what about if some other part of FoGSiEm uses such events and has registered another event before?
				pAS.getSimulation().addEvent(new SimulationCreatedEvent());
			} else {
				// there are already pending events
			}
		}
	}

	/**
	 * Generates a descriptive string about this object
	 * 
	 * @return the description string
	 */
	public String toString()
	{
		String tResult = getClass().getSimpleName() + (mNode != null ? "@" + mNode.toString() : "");
		
		return tResult;
	}
}
