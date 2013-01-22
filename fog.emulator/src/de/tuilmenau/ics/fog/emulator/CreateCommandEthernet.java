/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.emulator;

import de.tuilmenau.ics.fog.commands.CreateCommand;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;

/**
 * Command extension for the "create" command of the simulator.
 * Enables the creation of Ethernet via command:
 * "create ethernet <name of lower layer> <physical interface name>" 
 * 
 * For testing purposes, the Ethernet can be connected to a local
 * test implementation using named buffers.
 * "create ethernet <name of lower layer> <buffer name for receiving> <buffer name for sending>" 
 */
public class CreateCommandEthernet implements CreateCommand
{
	@Override
	public boolean create(AutonomousSystem pAS, String[] pParameters)
	{
		try {
			if(pParameters.length >= 4) {
				String optionalOutName = pParameters[3];
				if(pParameters.length >= 5) {
					optionalOutName = pParameters[4];
				}
				
				pAS.addBus(new Ethernet(pAS, pParameters[2], pParameters[3], optionalOutName));
				return true;
			} else {
				return false;
			}
		}
		catch(Exception exc) {
			pAS.getLogger().err(this, "Can not create Ethernet.", exc);
			return false;
		}
	}

}
