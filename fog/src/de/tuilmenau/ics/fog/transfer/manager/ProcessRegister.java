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
package de.tuilmenau.ics.fog.transfer.manager;

import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.ui.Logging.Level;


/**
 * Container for all processes on a node. The process instances are internally
 * mapped to there corresponding forwarding nodes. This mapping is needed
 * because the process IDs are not globally valid for a host but for a FN only.
 */
public class ProcessRegister
{
	public void registerProcess(ForwardingNode fn, Process process)
	{
		ProcessList list = getList(fn, true);
		
		list.registerProcess(process);
	}
	
	public Process getProcess(ForwardingNode fn, Identity owner, int processID)
	{
		ProcessList list = getList(fn, false);
		
		if(list != null) {
			return list.getProcess(owner, processID);
		}
		
		return null;
	}
	
	public ProcessList getProcesses(ForwardingNode fn)
	{
		ProcessList list = getList(fn, false);
		
		return list;
	}

	public boolean unregisterProcess(ForwardingNode fn, Process process)
	{
		boolean res = false;
		ProcessList list = getList(fn, false);
		
		if(list != null) {
			res = list.unregisterProcess(process);
			
			// No processes for the fn? -> Delete whole list.
			if(list.size() <= 0) {
				mProcesses.remove(fn);
			}
		}
		
		return res;
	}
	
	private ProcessList getList(ForwardingNode fn, boolean create)
	{
		ProcessList list = mProcesses.get(fn);
		
		if((list == null) && create) {
			list = new ProcessList();
			mProcesses.put(fn, list);
		}
		
		return list;
	}
	
	@Override
	public String toString()
	{
		Level tLogLevel = null;
		
		String tString = new String();
		tString += this.getSimpleToString() + " contains the following process lists:";
		for(ForwardingNode tFN : mProcesses.keySet()) {
			tString += "\n" + tFN + ":" + mProcesses.get(tFN);
			if(tLogLevel == null) {
				tLogLevel = tFN.getNode().getAS().getSimulation().getLogLevel();
				if(tLogLevel.ordinal() <= Level.LOG.ordinal()) {
					break;
				}
			}
		}
		
		if(tLogLevel.ordinal() > Level.LOG.ordinal()) {
			return tString;
		} else {
			return getSimpleToString();
		}
	}
	
	private String getSimpleToString()
	{
		return this.getClass().getSimpleName() + "(registered FNs: " + mProcesses.size() + ")";		
	}
	
	private HashMap<ForwardingNode, ProcessList> mProcesses = new HashMap<ForwardingNode, ProcessList>();
}
