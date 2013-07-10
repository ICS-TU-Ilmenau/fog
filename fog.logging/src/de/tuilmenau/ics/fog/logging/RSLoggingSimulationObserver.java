/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Logging
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
package de.tuilmenau.ics.fog.logging;

import java.io.IOException;
import java.util.Collection;

import de.tuilmenau.ics.CommonSim.datastream.numeric.DoubleNode;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleWriter;
import de.tuilmenau.ics.fog.EventHandler;
import de.tuilmenau.ics.fog.launcher.SimulationObserver;
import de.tuilmenau.ics.fog.routing.RoutingServiceInstanceRegister;
import de.tuilmenau.ics.fog.routing.simulated.RemoteRoutingService;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Simulation observer, which collects all information about routing services instances
 * and pipes them to the data stream. The information are collected after a simulation
 * finished. 
 */
public class RSLoggingSimulationObserver extends FileLogObserver implements SimulationObserver
{
	public RSLoggingSimulationObserver()
	{
	}

	@Override
	public void created(Simulation sim)
	{
		this.sim = sim;
		this.timeBase = sim.getTimeBase();
		
		try {
			open(sim.getBaseDirectory(), null);
			
			// start recording log to file
			sim.getLogger().addLogObserver(this);
		}
		catch(IOException tExc) {
			sim.getLogger().err(this, "Can not open log file.", tExc);
		}
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
		Collection<RemoteRoutingService> allRS = RoutingServiceInstanceRegister.getInstance(sim).getAll();
		int sumVertices = 0;
		int sumEdges = 0;
		int sumSize = 0;
		IDoubleWriter out;
		int number = 0;

		try {
			for(RemoteRoutingService rs : allRS) {
				String baseName = rs.getClass().getName() +".";
				
				number = rs.getNumberVertices();
				out = DoubleNode.openAsWriter(baseName +rs.getName() +".vertices");
				out.write(number, timeBase.nowStream());
				sumVertices += number;
				
				number = rs.getNumberEdges();
				out = DoubleNode.openAsWriter(baseName +rs.getName() +".edges");
				out.write(number, timeBase.nowStream());
				sumEdges += number;
				
				number = rs.getSize();
				out = DoubleNode.openAsWriter(baseName +rs.getName() +".size");
				out.write(number, timeBase.nowStream());
				sumSize += number;
			}
	
			out = DoubleNode.openAsWriter("RoutingService.sumVertices");
			out.write(sumVertices, timeBase.nowStream());
			
			out = DoubleNode.openAsWriter("RoutingService.sumEdges");
			out.write(sumEdges, timeBase.nowStream());
			
			out = DoubleNode.openAsWriter("RoutingService.sumSizes");
			out.write(sumSize, timeBase.nowStream());
		}
		catch(Exception exc) {
			Logging.err(this, "Exception during collecting statistical data.", exc);
		}
		
		// stop recording log to file
		close();
	}

	@Override
	public void finished()
	{
	}
	
	private Simulation sim;
	private EventHandler timeBase;
}
