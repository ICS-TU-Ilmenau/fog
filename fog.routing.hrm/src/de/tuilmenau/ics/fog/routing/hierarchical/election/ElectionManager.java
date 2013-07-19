/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.election;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.election.ElectionEventNotification;
import de.tuilmenau.ics.fog.ui.Logging;

public class ElectionManager
{
	private HashMap<Integer, HashMap<Long, Elector>>mElections = null;
	private static ElectionManager mManager = null;
	private ElectionEventNotification mNotification;
	
	public ElectionManager()
	{
		mElections = new HashMap<Integer, HashMap<Long, Elector>>();
	}
	
	public static ElectionManager getElectionManager()
	{
		if(mManager == null) {
			mManager = new ElectionManager();
		}
		return mManager;
	}
	
	/**
	 * Returns a processes of a defined hierarchy level and cluster ID.
	 * This functions is used within the GUI.
	 * 
	 * @param pLevel
	 * @param pClusterID
	 * @return
	 */
	public synchronized Elector getProcess(int pLevel, Long pClusterID)
	{
		if(mElections.containsKey(pLevel)) {
			if(mElections.containsKey(pLevel) && mElections.get(pLevel).containsKey(pClusterID)) {
				return mElections.get(pLevel).get(pClusterID); 
			}
		}
		return null;
	}
	
	/**
	 * Returns all processes on a defined hierarchy level.
	 * This functions is used within the GUI.
	 * 
	 * @param pLevel
	 * @return
	 */
	public Collection<Elector> getProcesses(int pLevel)
	{
		try {
			return mElections.get(pLevel).values();
		} catch (NullPointerException tExc) {
			return new LinkedList<Elector>();
		}
	}
	
	public Elector addElection(int pLevel, Long pClusterID, Elector pElection)
	{
		if(!mElections.containsKey(pLevel)) {
			mElections.put(pLevel, new HashMap<Long, Elector>());
			mElections.get(pLevel).put(pClusterID, pElection);
			return pElection;
		} else {
			if(mElections.get(pLevel).containsKey(pClusterID)) {
				return mElections.get(pLevel).get(pClusterID);
			} else {
				mElections.get(pLevel).put(pClusterID, pElection);
				return pElection;
			}
		}
	}
	
	public void removeElection(Integer pLevel, Long pClusterID)
	{
		if(HRMConfig.Hierarchy.BUILD_AUTOMATICALLY) {
			mElections.get(pLevel).remove(pClusterID);
			if(mElections.get(pLevel).isEmpty()) {
				if(mNotification != null) {
					mNotification = null;
				}
				Logging.log(this, "No more elections available, preparing next cluster");
				if(mElections.containsKey(pLevel + 1)) {
					for(Elector tProcess : mElections.get(Integer.valueOf(pLevel + 1)).values()) {
						tProcess.start();
					}
				}
			}
		} else {
			return;
		}
	}
	
	public LinkedList<Elector> getAllElections()
	{
		LinkedList<Elector> tElections = new LinkedList<Elector>();
		for(Integer tLevel: mElections.keySet()) {
			if(mElections.get(tLevel) != null) {
				for(Long tID : mElections.get(tLevel).keySet()) {
					if(mElections.get(tLevel).get(tID) != null) {
						tElections.add(mElections.get(tLevel).get(tID));
					}
				}
			}
		}
		return tElections;
	}
	
	public void reevaluate(int pLevel)
	{
		if(HRMConfig.Hierarchy.BUILD_AUTOMATICALLY) {
			boolean tWontBeginDistribution = false;
			Elector tWaitingFor = null;
			for(Elector tProcess : mElections.get(pLevel).values()) {
				Logging.log(tProcess + " is " + (tProcess.aboutToContinue() ? " about to " : "not about to ") + "initialize its Cluster Manager");
				if(!tProcess.aboutToContinue()) {
					tWontBeginDistribution = true;
					tWaitingFor = tProcess;
				}
			}
			if(tWontBeginDistribution) {
				Logging.log(this, "Not notifying other election processes because of " + tWaitingFor + " (reporting only last process)");
			} else {
				if(mNotification == null) {
					mNotification = new ElectionEventNotification(mElections.get(pLevel).values());
					for(Elector tProcess : mElections.get(pLevel).values()) {
						tProcess.getCluster().getHRMController().getNode().getAS().getSimulation().getTimeBase().scheduleIn(5, mNotification);
						break;
					}
				} else {
					return;
				}
			}
		} else {
			return;
		}
	}
}
