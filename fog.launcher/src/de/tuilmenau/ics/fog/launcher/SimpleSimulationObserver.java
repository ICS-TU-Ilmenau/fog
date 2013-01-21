/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Launcher
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
package de.tuilmenau.ics.fog.launcher;

import de.tuilmenau.ics.fog.topology.Simulation;


/**
 * Simple base class for observer, which do not override all
 * methods. It's methods are doing nothing (except the
 * <code>created</code> method).
 */
public class SimpleSimulationObserver implements SimulationObserver
{

	@Override
	public void created(Simulation sim)
	{
		this.sim = sim;
	}

	@Override
	public void init()
	{
	}

	@Override
	public void started()
	{
	}

	@Override
	public void ended()
	{
	}

	@Override
	public void finished()
	{
	}
	
	protected Simulation getSimulation()
	{
		return sim;
	}
	
	private Simulation sim;
}
