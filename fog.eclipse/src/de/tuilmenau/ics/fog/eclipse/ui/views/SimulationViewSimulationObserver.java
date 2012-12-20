/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Eclipse
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.eclipse.ui.views;

import de.tuilmenau.ics.fog.launcher.SimulationObserver;
import de.tuilmenau.ics.fog.topology.Simulation;


public class SimulationViewSimulationObserver implements SimulationObserver
{
	@Override
	public void init(Simulation sim)
	{
		this.sim = sim;
		
		SimulationView.addSimulation(sim);
	}

	@Override
	public void started()
	{
		SimulationView.openSimulation(sim);
	}

	@Override
	public void ended()
	{
		SimulationView.removeSimulation(sim);
	}
	
	@Override
	public void finished()
	{
	}

	private Simulation sim = null;
}
