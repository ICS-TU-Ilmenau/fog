/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.properties;

import java.io.Serializable;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.properties.AbstractProperty;
import de.tuilmenau.ics.fog.packets.hierarchical.DiscoveryEntry;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.ClusterDummy;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;

public class ClusterParticipationProperty extends AbstractProperty
{
	private int mLevel;
	private Long mTargetClusterID;
	private int mTargetToken;
	private Name mSourceName;
	private HRMName mSourceAddress;
	private LinkedList<Name> mAddressesToTarget;
	private LinkedList<NestedParticipation> mNestParticipations = new LinkedList<NestedParticipation>();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7561293731302599090L;
	
	/**
	 * @param pClusterID identificator of the cluster that is supposed to be created
	 * @param pLevel hierarchical level of this participation request
	 * @param pClusterHops amount of hops to the target
	 * @param pTargetToken token of the cluster this request comes from
	 */
	public ClusterParticipationProperty(Long pTargetClusterID, int pLevel, int pTargetToken)
	{
		mTargetClusterID = pTargetClusterID;
		mLevel = pLevel;
		mTargetToken = pTargetToken;
	}
	
	public void addAddressToTarget(Name pAddress)
	{
		if(mAddressesToTarget == null) {
			mAddressesToTarget = new LinkedList<Name>();
		}
		mAddressesToTarget.add(pAddress);
	}
	
	public LinkedList<Name> getAddressesToTarget()
	{
		return mAddressesToTarget;
	}
	
	public int getTargetToken()
	{
		return mTargetToken;
	}
	
	public void setTargetToken(int pToken)
	{
		mTargetToken = pToken;
	}
	
	public void setTargetClusterID(Long pTargetClusterID)
	{
		mTargetClusterID = pTargetClusterID;
	}
		
	public Long getTargetClusterID()
	{
		return mTargetClusterID;
	}
	
	public void setClusterID(Long pClusterID)
	{
		mTargetClusterID = pClusterID;
	}
	
	public int getLevel()
	{
		return mLevel;
	}
		
	public LinkedList<NestedParticipation> getNestedParticipations()
	{
		return mNestParticipations;
	}
	
	public void addNestedparticipation(NestedParticipation pParticipation)
	{
		if(mNestParticipations == null) {
			mNestParticipations = new LinkedList<NestedParticipation>();
		}
		mNestParticipations.add(pParticipation);
	}
	
	public class NestedParticipation implements Serializable
	{
		private Long mSourceClusterID;
		private int mSourceToken;
		private ClusterDummy mPredecessor;
		private LinkedList<DiscoveryEntry> mDiscoveries;
		private boolean mInterASCluster = false;
		private int mLevel;
		private float mPriority;
		
		public NestedParticipation(Long pSourceClusterID, int pSourceToken)
		{
			mSourceClusterID = pSourceClusterID;
			mSourceToken = pSourceToken;
		}
		
		public void setSourcePriority(float pPriority)
		{
			mPriority = pPriority;
		}
		
		public float getSourcePriority()
		{
			return mPriority;
		}
		
		public void setLevel(int pLevel)
		{
			mLevel = pLevel;
		}
		
		public int getLevel()
		{
			return mLevel;
		}
		
		public void setSourceRoutingServiceAddress(HRMName pAddress)
		{
			mSourceAddress = pAddress;
		}
		
		public void setPredecessor(ClusterDummy pPredecessor)
		{
			mPredecessor = pPredecessor;
		}
		
		public ClusterDummy getPredecessor()
		{
			return mPredecessor;
		}
		
		public void setSourceName(Name pSource)
		{
			mSourceName = pSource;
		}
		
		public Name getSourceName()
		{
			return mSourceName;
		}
		
		public HRMName getSourceAddress()
		{
			return mSourceAddress;
		}
		
		public void setSourceToken(int pToken)
		{
			mSourceToken = pToken;
		}
		
		public void setInterASCluster()
		{
			mInterASCluster = true;
		}
		
		public int getSourceToken()
		
		{
			return mSourceToken;
		}
		
		public void addDiscoveryEntry(DiscoveryEntry pEntry)
		{
			if(mDiscoveries == null) {
				mDiscoveries = new LinkedList<DiscoveryEntry>();
				mDiscoveries.add(pEntry);
			} else {
				mDiscoveries.add(pEntry);
			}
		}
		
		public LinkedList<DiscoveryEntry> getNeighbors()
		{
			return mDiscoveries;
		}
		
		public boolean isInterASCluster()
		{
			return mInterASCluster;
		}
		
		public String toString()
		{
			return this.getClass().getSimpleName() + ":TARGET(" + mTargetClusterID + ")SOURCE(" + mSourceClusterID + ")STOKEN(" + mSourceToken + ")DTOKEN(" + mTargetToken + ")";
		}
		
		public Long getSourceClusterID()
		{
			return this.mSourceClusterID;
		}
		
		public void setSourceClusterID(Long pSourceClusterID)
		{
			mSourceClusterID = pSourceClusterID;
		}
	}
}
