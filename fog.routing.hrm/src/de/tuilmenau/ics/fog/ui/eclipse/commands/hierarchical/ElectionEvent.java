/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.ui.eclipse.commands.hierarchical;

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.routing.hierarchical.CoordinatorCEPDemultiplexed;
import de.tuilmenau.ics.fog.routing.hierarchical.ElectionProcess;
import de.tuilmenau.ics.fog.routing.hierarchical.ElectionProcess.ElectionManager;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.Cluster;

public class ElectionEvent implements IEvent 
{
	public ElectionEvent()
	{
		
	}
	
	@Override
	public void fire()
	{
		for(ElectionProcess tElection : ElectionManager.getElectionManager().getAllElections()) {
			boolean tStartProcess=true;
			for(Cluster tCluster : tElection.getParticipatingClusters()) {
				for(CoordinatorCEPDemultiplexed tCEP : tCluster.getParticipatingCEPs()) {
					if(tCEP.isEdgeCEP()) {
						tStartProcess = false;
					}
				}
			}
			if(tStartProcess) {
				tElection.start();
			}
		}
	}

	@Override
	public int hashCode()
	{
		return 0;
	}
	
}
