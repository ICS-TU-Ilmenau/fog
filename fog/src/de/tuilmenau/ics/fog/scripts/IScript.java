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
package de.tuilmenau.ics.fog.scripts;

import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.Simulation;


public interface IScript
{
	/**
	 * First call, after a script was constructed. Method is
	 * called before {@link #execute}.
	 * 
	 * @param simulation Simulation the script belongs to.
	 */
	public void setSimulation(Simulation simulation);

	/**
	 * Executes the script.
	 * 
	 * @param commandParts
	 * @param as TODO remove it from signature
	 * @return if the script was executed successfully
	 * @throws Exception On error
	 */
	public boolean execute(String[] commandParts, AutonomousSystem as) throws Exception;	
}
