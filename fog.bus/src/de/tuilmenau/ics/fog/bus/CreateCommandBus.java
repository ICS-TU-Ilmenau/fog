/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Bus
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
package de.tuilmenau.ics.fog.bus;

import de.tuilmenau.ics.fog.commands.CreateCommand;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.properties.DatarateProperty;
import de.tuilmenau.ics.fog.facade.properties.DelayProperty;
import de.tuilmenau.ics.fog.facade.properties.LossRateProperty;
import de.tuilmenau.ics.fog.facade.properties.MinMaxProperty.Limit;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;


/**
 * Command extension for the "create" command of the simulator.
 * Enables the creation of Bus via command:
 * "create bus <name of bus> [<max bandwidth> [<min delay> [<max loss probability>]]]" 
 */
public class CreateCommandBus implements CreateCommand
{
	@Override
	public boolean create(AutonomousSystem pAS, String[] tParts)
	{
		boolean tRes = false;
		
		if(tParts[1].equals("bus")) {
			Description tDescr = null;
			
			// at least one QoS parameter present?
			if(tParts.length > 3) {
				//
				// <max bandwidth> <min delay> <max loss probability>
				//
				tDescr = new Description();
				tDescr.set(new DatarateProperty(Integer.parseInt(tParts[3]), Limit.MAX));
				
				if(tParts.length > 4) {
					tDescr.set(new DelayProperty(Integer.parseInt(tParts[4]), Limit.MIN));
					
					if(tParts.length > 5) {
						tDescr.set(new LossRateProperty(Integer.parseInt(tParts[5]), Limit.MAX));
					}
				}
			}
			
			tRes = pAS.addBus(new Bus(pAS, tParts[2], tDescr));
		}
		
		return tRes;
	}

}
