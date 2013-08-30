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
package de.tuilmenau.ics.fog.scenario;

import de.tuilmenau.ics.fog.application.ReroutingExecutor;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Node;

public class NodeConfiguratorRerouting implements NodeConfigurator
{
	@Override
	public void configure(String pName, AutonomousSystem pAS, Node pNode)
	{
		ReroutingExecutor tApp = new ReroutingExecutor(pNode, null);
		pNode.registerApp(tApp);
		tApp.start();
	}
}
