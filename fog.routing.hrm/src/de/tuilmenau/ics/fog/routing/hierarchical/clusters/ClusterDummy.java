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
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.packets.hierarchical.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborZoneAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyEnvelope;
//import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.CoordinatorCEPDemultiplexed;
import de.tuilmenau.ics.fog.routing.hierarchical.CoordinatorCEPMultiplexer;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalSignature;
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
	public HRMID retrieveAddress() {
		return null;
	}

	@Override
	public Name retrieveName() {
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
	public void setCoordinatorPriority(float pCoordinatorPriority) {		
	}

	@Override
	public float getCoordinatorPriority() {
		return 0;
	}

	@Override
	public void setPriority(float pPriority) {
		
	}

	@Override
	public Coordinator getCoordinator() {
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
	public int getLevel() {
		return mLevel;
	}

	@Override
	public Name getCoordinatorName() {
		return null;
	}

	@Override
	public float getPriority() {
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
	public HierarchicalSignature getCoordinatorSignature() {
		return null;
	}

	@Override
	public float getHighestPriority()
	{
		return 0;
	}

	@Override
	public void initiateElection()
	{
		
	}

	@Override
	public void interruptElection()
	{
		
	}

	@Override
	public void handleAnnouncement(NeighborZoneAnnounce pAnnounce, CoordinatorCEPDemultiplexed pCEP)
	{
	}

	@Override
	public CoordinatorCEPDemultiplexed getCoordinatorCEP()
	{
		return null;
	}

	@Override
	public void setCoordinatorCEP(CoordinatorCEPDemultiplexed pCoord, HierarchicalSignature pCoordSignature, Name pCoordName, HRMName pAddress)
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
	public void setHighestPriority(float pHighestPriority) {
		
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
					tCluster.getLevel() == getLevel()) {
				return true;
			} else if(tCluster.getClusterID().equals(getClusterID()) && tCluster.getLevel() == getLevel()) {
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
	public void handleTopologyEnvelope(TopologyEnvelope pEnvelope)
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
}
