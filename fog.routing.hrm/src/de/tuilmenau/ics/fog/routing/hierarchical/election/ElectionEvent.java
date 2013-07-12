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

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ICluster;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.CoordinatorCEPChannel;
import de.tuilmenau.ics.fog.routing.hierarchical.election.ElectionProcess.ElectionManager;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * The implementation expects the network to be synchronized. This class causes the start of all elections.
 *
 */
public class ElectionEvent implements IEvent 
{
	public ElectionEvent()
	{
		
	}
	
	@Override
	public void fire()
	{
		Logging.log(this,  "Firing election event " + toString());
		for(ElectionProcess tElection : ElectionManager.getElectionManager().getAllElections()) {
			boolean tStartProcess=true;
			Cluster tCluster = tElection.getCluster();
			for(CoordinatorCEPChannel tCEP : tCluster.getParticipatingCEPs()) {
				if(tCEP.isEdgeCEP()) {
					tStartProcess = false;
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
