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
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

import de.tuilmenau.ics.CommonSim.datastream.DatastreamManager;
import de.tuilmenau.ics.CommonSim.datastream.numeric.IDoubleReader;
import de.tuilmenau.ics.fog.launcher.SimulationObserver;
import de.tuilmenau.ics.fog.topology.Simulation;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.CSVWriter;


public class EndStatisticWriter implements SimulationObserver
{
	private static final String DATASTREAM_IGNORE_PREFIX = "__";
	
	public EndStatisticWriter()
	{
	}

	@Override
	public void init(Simulation sim)
	{
		this.sim = sim;
	}

	@Override
	public void started()
	{
	}

	@Override
	public void ended()
	{
		DatastreamManager dm = DatastreamManager.getInstance();
		Set<String> streamNames = dm.getRegisteredNames();
		
		String filename = sim.getBaseDirectory() +this.getClass().getCanonicalName() +"_" +System.currentTimeMillis() +".csv";
		CSVWriter out = null;
		
		try {
			out = new CSVWriter(filename);
			
			// sort the key entries
			LinkedList<String> streamNamesList = new LinkedList<String>(streamNames);
			Collections.sort(streamNamesList);
	
			LinkedList<Double> valueList = new LinkedList<Double>();
			
			// put entries in GUI list
			for(String name : streamNamesList) {
				if(!name.startsWith(DATASTREAM_IGNORE_PREFIX)) {
					IDoubleReader node = DatastreamManager.open(IDoubleReader.class, name);
	
					out.write(name);
					valueList.addLast(node.read());
				}
			}
			
			out.finishEntry();
			for(Double value : valueList) {
				out.write(value);
			}
		}
		catch (IOException exc) {
			Logging.getInstance().err(this, "Can not log data stream to file '" +filename +"'.", exc);
		}
		finally {
			try {
				if(out != null) out.close();
			}
			catch(IOException exc) {
				Logging.getInstance().err(this, "Can not close log file '" +filename +"'.", exc);
			}
		}
		
		// after saving, remove all elements of data stream
		DatastreamManager.clear();
	}
	
	@Override
	public void finished()
	{
	}

	private Simulation sim;
}
