/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.management;

import java.io.Serializable;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborClusterAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;

public class ClusterName implements Serializable, ICluster
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3027076881853652810L;
	private int mToken;
	private Long mClusterID;
	private HierarchyLevel mHierarchyLevel = null;
	
	/**
	 * 
	 * @param pToken token of the cluster
	 * @param pClusterID ID of the cluster
	 * @param pLevel level of the cluster
	 */
	public ClusterName(int pToken, Long pClusterID, HierarchyLevel pLevel)
	{
		mClusterID = pClusterID;
		mToken = pToken;
		mHierarchyLevel = pLevel;
	}
	
	@Override
	public String toString()
	{
		return "\"(ID=" + mClusterID + ", Tok=" + mToken + (mHierarchyLevel != null ? ", HierLvl.=" + mHierarchyLevel.getValue() : "") + ")\""; 
	}

	@Override
	public HRMID getHRMID() {
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
	public void setCoordinatorPriority(BullyPriority pCoordinatorPriority) {		
	}

	@Override
	public BullyPriority getCoordinatorPriority() {
		return null;
	}

	@Override
	public void setPriority(BullyPriority pPriority) {
		
	}

	@Override
	public HRMController getHRMController() {
		return null;
	}

	@Override
	public LinkedList<ComChannel> getClusterMembers() {
		return null;
	}

	@Override
	public void registerComChannel(ComChannel pParticipatingCEP) {
		
	}

	@Override
	public Long getClusterID() {
		return mClusterID;
	}

	@Override
	public HierarchyLevel getHierarchyLevel() {
		return mHierarchyLevel;
	}

	@Override
	public Name getCoordinatorName() {
		return null;
	}

	@Override
	public BullyPriority getPriority() {
		return null;
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
	public BullyPriority getHighestPriority()
	{
		return null;
	}

	@Override
	public void handleNeighborAnnouncement(NeighborClusterAnnounce pAnnounce, ComChannel pCEP)
	{
	}

	@Override
	public ComChannel getSuperiorCoordinatorCEP()
	{
		return null;
	}

	@Override
	public void setSuperiorCoordinatorCEP(ComChannel pCoord, Name pCoordName, int pCoordToken, HRMName pAddress)
	{
		
	}

	@Override
	public void addNeighborCluster(ICluster pNeighbor)
	{
		
	}

	@Override
	public void setHRMID(Object pCaller, HRMID pHRMID)
	{
		
	}

	@Override
	public void setHighestPriority(BullyPriority pHighestPriority) {
		
	}

	@Override
	public void sendClusterBroadcast(Serializable pData, LinkedList<ComChannel> pAlreadyInformed)
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

	@Override
	public void handleBullyAnnounce(BullyAnnounce pAnnounce, ComChannel pCEP)
	{
		
	}

	@Override
	public ComChannelMuxer getMultiplexer()
	{
		return null;
	}
}
