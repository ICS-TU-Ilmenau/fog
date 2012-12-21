/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.clusters;

import java.io.Serializable;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborZoneAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyEnvelope;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.CoordinatorCEPDemultiplexed;
import de.tuilmenau.ics.fog.routing.hierarchical.CoordinatorCEPMultiplexer;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalSignature;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;


public interface Cluster extends Serializable, VirtualNode
{
	/**
	 * Sets the priority of the currently acting coordinator of this cluster.
	 * 
	 * @param pCoordinatorPriority is the priority of the entity
	 */
	public void setCoordinatorPriority(float pCoordinatorPriority);
	
	/**
	 * Get the priority of the currently acting coordinator
	 * 
	 * @return priority of the acting coordinator
	 */
	public float getCoordinatorPriority();
	
	/**
	 * Set the priority the reference node has in this cluster
	 * 
	 * @param pPriority
	 */
	public void setPriority(float pPriority);
	
	/**
	 * Get the object that provides functions required for coordinators
	 * 
	 * @return
	 */
	public Coordinator getCoordinator();
	
	public LinkedList<CoordinatorCEPDemultiplexed> getParticipatingCEPs();
	
	public void addParticipatingCEP(CoordinatorCEPDemultiplexed pParticipatingCEP);
		
	public String toString();
	
	public Long getClusterID();
	
	public int getLevel();
		
	public Name getCoordinatorName();
	
	public float getPriority();
	
	public String getClusterDescription();
	
	public void setCoordinatorName(Name pCoordName);
	
	public HRMName getCoordinatorsAddress();
	
	public void setToken(int pToken);
	
	public int getToken();
	
	public LinkedList<Cluster> getNeighbors();
	
	public HierarchicalSignature getCoordinatorSignature();
	
	public Cluster getClusterWithHighestPriority();
	
	public float getHighestPriority();
	
	public void initiateElection();
	
	public void interruptElection();
	
	public void interpretAnnouncement(BullyAnnounce pAnnounce, CoordinatorCEPDemultiplexed pCEP);
	
	public void handleAnnouncement(NeighborZoneAnnounce pAnnounce, CoordinatorCEPDemultiplexed pCEP);
	
	public CoordinatorCEPDemultiplexed getCoordinatorCEP();
	
	
	public void setCoordinatorCEP(CoordinatorCEPDemultiplexed pCoord, HierarchicalSignature pCoordSignature, Name pCoordName, HRMName pAddress);
	
	public void addNeighborCluster(Cluster pNeighbor);
	
	public void setRouteToCoordinator(Route pPath);
	
	public void setHRMID(HRMID pHRMID);
	
	public void setHighestPriority(float pHighestPriority);
	
	public void sendClusterBroadcast(Serializable pData, LinkedList<CoordinatorCEPDemultiplexed> pAlreadyInformed);
	
	public boolean equals(Object pObj);
	
	public boolean isInterASCluster();
	
	public void setInterASCluster();
	
	public LinkedList<CoordinatorCEPDemultiplexed> getLaggards();
	
	public void addLaggard(CoordinatorCEPDemultiplexed pCEP);
	
	public CoordinatorCEPDemultiplexed getNegotiatorCEP();
	
	public void setNegotiatorCEP(CoordinatorCEPDemultiplexed pCEP);
	
	public void handleTopologyEnvelope(TopologyEnvelope pEnvelope);
	
	public CoordinatorCEPMultiplexer getMultiplexer();
}
