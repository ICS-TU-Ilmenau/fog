/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.clustering;

import java.io.Serializable;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.packets.election.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborClusterAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyData;
//import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMSignature;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.CoordinatorCEPDemultiplexed;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.CoordinatorCEPMultiplexer;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;

public class ClusterDummy implements Serializable, ICluster
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3027076881853652810L;
	private int mToken;
	private Long mClusterID;
	private int mLevel;
	
	/**
	 * 
	 * @param pToken token of the cluster
	 * @param pClusterID ID of the cluster
	 * @param pLevel level of the cluster
	 */
	public ClusterDummy(int pToken, Long pClusterID, int pLevel)
	{
		mClusterID = pClusterID;
		mToken = pToken;
		mLevel = pLevel;
	}
	
	@Override
	public String toString()
	{
		return "ID(" + mClusterID + ")" + "TK(" + mToken + ")LEVEL(" + mLevel + ")"; 
	}

	@Override
	public HRMID getHrmID() {
		return null;
	}

	@Override
	public Namespace getNamespace() {
		return null;
	}

	@Override
	public int getSerialisedSize() {
		return 0;
	}

	@Override
	public void setCoordinatorPriority(long pCoordinatorPriority) {		
	}

	@Override
	public long getNodePriority() {
		return 0;
	}

	@Override
	public void setPriority(long pPriority) {
		
	}

	@Override
	public HRMController getHRMController() {
		return null;
	}

	@Override
	public LinkedList<CoordinatorCEPDemultiplexed> getParticipatingCEPs() {
		return null;
	}

	@Override
	public void addParticipatingCEP(CoordinatorCEPDemultiplexed pParticipatingCEP) {
		
	}

	@Override
	public Long getClusterID() {
		return mClusterID;
	}

	@Override
	public int getHierarchyLevel() {
		return mLevel;
	}

	@Override
	public Name getCoordinatorName() {
		return null;
	}

	@Override
	public long getPriority() {
		return 0;
	}

	@Override
	public String getClusterDescription() {
		return null;
	}

	@Override
	public void setCoordinatorName(Name pCoordName) {
		
	}

	@Override
	public HRMID getCoordinatorsAddress() {
		return null;
	}

	@Override
	public void setToken(int pToken) {
		
	}

	@Override
	public int getToken() {
		return mToken;
	}

	@Override
	public LinkedList<ICluster> getNeighbors() {
		return null;
	}

	@Override
	public HRMSignature getCoordinatorSignature() {
		return null;
	}

	@Override
	public long getHighestPriority()
	{
		return 0;
	}

	@Override
	public void handleAnnouncement(NeighborClusterAnnounce pAnnounce, CoordinatorCEPDemultiplexed pCEP)
	{
	}

	@Override
	public CoordinatorCEPDemultiplexed getCoordinatorCEP()
	{
		return null;
	}

	@Override
	public void setCoordinatorCEP(CoordinatorCEPDemultiplexed pCoord, HRMSignature pCoordSignature, Name pCoordName, HRMName pAddress)
	{
		
	}

	@Override
	public void addNeighborCluster(ICluster pNeighbor)
	{
		
	}

	@Override
	public void setHRMID(HRMID pHRMID)
	{
		
	}

	@Override
	public void setHighestPriority(long pHighestPriority) {
		
	}

	@Override
	public void sendClusterBroadcast(Serializable pData, LinkedList<CoordinatorCEPDemultiplexed> pAlreadyInformed)
	{
		
	}

	@Override
	public boolean isInterASCluster()
	{
		return false;
	}

	@Override
	public void setInterASCluster()
	{
		
	}
	
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj instanceof ICluster) {
			ICluster tCluster = (ICluster) pObj;
			if(tCluster.getClusterID().equals(getClusterID()) &&
					tCluster.getToken() == getToken() &&
					tCluster.getHierarchyLevel() == getHierarchyLevel()) {
				return true;
			} else if(tCluster.getClusterID().equals(getClusterID()) && tCluster.getHierarchyLevel() == getHierarchyLevel()) {
				return false;
			} else if (tCluster.getClusterID().equals(getClusterID())) {
				return false;
			}
		}
		return false;
	}
	
	public static ClusterDummy compare(Long pClusterID, int pToken, int pLevel)
	{
		return new ClusterDummy(pToken, pClusterID, pLevel);
	}

	@Override
	public LinkedList<CoordinatorCEPDemultiplexed> getLaggards()
	{
		return null;
	}

	@Override
	public void addLaggard(CoordinatorCEPDemultiplexed pCEP)
	{
		
	}

	@Override
	public CoordinatorCEPDemultiplexed getNegotiatorCEP()
	{
		return null;
	}

	@Override
	public void setNegotiatorCEP(CoordinatorCEPDemultiplexed pCEP)
	{
		
	}

	@Override
	public void handleTopologyData(TopologyData pEnvelope)
	{
		
	}
	
	@Override
	public void interpretAnnouncement(BullyAnnounce pAnnounce, CoordinatorCEPDemultiplexed pCEP)
	{
		
	}

	@Override
	public CoordinatorCEPMultiplexer getMultiplexer()
	{
		return null;
	}

	@Override
	public TopologyData getTopologyData()
	{
		return null;
	}
}
