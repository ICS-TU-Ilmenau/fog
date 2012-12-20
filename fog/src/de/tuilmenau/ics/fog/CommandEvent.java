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
package de.tuilmenau.ics.fog;

import de.tuilmenau.ics.fog.topology.Simulation;

/**
 * Event, which is executing a command for a simulation.
 * 
 * The command execution is done in the event thread and MUST NOT block.
 * It is useful to do this in an event in order to stop the clock from
 * running during the command execution.
 */
public class CommandEvent implements IEvent
{
	public CommandEvent(Simulation pSim, String pCmd)
	{
		mSim = pSim;
		mCmd = pCmd;
		
		if(mSim == null) {
			throw new RuntimeException(this +" - Invalid parameters: Simulation not given.");
		}
	}

	@Override
	public void fire()
	{
		if(mSim != null) {
			mSim.executeCommand(mCmd);
		}
	}

	private Simulation mSim;
	private String mCmd;
}
