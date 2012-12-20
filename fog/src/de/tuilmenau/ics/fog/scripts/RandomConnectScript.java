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

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.application.ConnectTest;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.topology.IAutonomousSystem;
import de.tuilmenau.ics.fog.topology.Simulation;


/**
 * 'start RandomConnect <integer number of connections> [<close connection after test=true> [<terminate simulation in the end=false>]]'
 */
public class RandomConnectScript extends Script implements IEvent
{
	@Override
	public boolean execute(String[] commandParts, AutonomousSystem as) throws Exception
	{
		// how many connects?
		if(commandParts.length > 2) {
			try {
				mCounter = Integer.parseInt(commandParts[2]);
			} catch (NumberFormatException tExc) {
				getLogger().warn(this, "Can not parse number of iterations. Doing it just one time.", tExc);
				mCounter = 1;
			}
		}
		
		// close connection after test?
		if(commandParts.length > 3) {
			mCloseAfterTest = Boolean.parseBoolean(commandParts[3]);
		}

		// terminate simulation after connection test?
		if(commandParts.length > 4) {
			mExitSimulationAfterConnection = Boolean.parseBoolean(commandParts[4]);
		}

		// register for end event
		ConnectTest.setOnExitEvent(this);

		// start first connect
		fire();
		return true;
	}

	@Override
	public void fire()
	{
		if(mCounter > 0) {
			mCounter--;
			
			try {
				startConnect();
			}
			catch(Exception tExc) {
				getLogger().err(this, "Error during starting a test.", tExc);
			}
		} else {
			//
			// Connects are all done. Terminate simulation?
			//
			if(mExitSimulationAfterConnection) {
				getSimulation().exit();
			}
		}
	}
	
	private void startConnect() throws Exception
	{
		Simulation tSim = getSimulation();
		
		IAutonomousSystem tAS1 = tSim.getRandomAS(false, true);
		if(tAS1 != null) {
			String tNode1 = tAS1.getRandomNodeString();
			
			IAutonomousSystem tAS2;
			String tNode2;
			int tTries = 100;
			
			//
			// find random destination != source
			//
			do {
				tAS2 = tSim.getRandomAS(false, true);
				tNode2 = tAS2.getRandomNodeString();
				
				tTries--;
				if(tTries <= 0) {
					throw new NetworkException(this, "Can not find a random node different from '" +tNode1 +"'. Stopping at iteration " +mCounter +".");
				}
			}
			while(tNode1.equals(tNode2));
			
			//
			// execute connect
			//
			if(tSim.switchToAS(tAS1.getName())) {
				tSim.executeCommand("start App ConnectTest " +tNode1 +" " +tNode2 +" " +mCloseAfterTest);
			} else {
				throw new NetworkException(this, "Can not switch to AS '" +tAS1.getName() +"'");
			}
		} else {
			throw new NetworkException(this, "No autonomous system found.");
		}
	}

	private boolean mCloseAfterTest = true;
	private boolean mExitSimulationAfterConnection = false;
	private int mCounter = 1;
}

