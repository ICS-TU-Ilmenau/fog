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

import de.tuilmenau.ics.fog.launcher.SimpleSimulationObserver;


public class SimulationViewSimulationObserver extends SimpleSimulationObserver
{
	@Override
	public void init()
	{
		SimulationView.addSimulation(getSimulation());
	}

	@Override
	public void started()
	{
		SimulationView.openSimulation(getSimulation());
	}

	@Override
	public void ended()
	{
		SimulationView.removeSimulation(getSimulation());
	}
}
