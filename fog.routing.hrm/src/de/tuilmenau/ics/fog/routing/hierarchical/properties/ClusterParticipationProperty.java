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
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.routing.hierarchical.management.HierarchyLevel;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used for meta information that is used for the establishment of connections. The connections in that
 * case lead from one (potential) cluster member to a potential coordinator. 
 * 
 */
public class ClusterParticipationProperty extends AbstractProperty
{
	private HierarchyLevel mHierarchyLevel = null;
	private Long mTargetClusterID;
	private int mTargetToken;
	private Name mSourceName;
	private HRMName mSourceAddress;
	private LinkedList<NestedParticipation> mNestedParticipations = new LinkedList<NestedParticipation>();
	private static final long serialVersionUID = 7561293731302599090L;
	
	/**
	 * @param pClusterID This is the identifier of the cluster that is supposed to be created.
	 * @param pLevel This is the hierarchical level of the entity from which this entity comes.
	 * @param pClusterHops amount of hops to the target
	 * @param pTargetToken token of the cluster this request comes from
	 */
	public ClusterParticipationProperty(Long pTargetClusterID, HierarchyLevel pLevel, int pTargetToken)
	{
		Logging.log(this, "Setting target cluster ID " + pTargetClusterID);
		mTargetClusterID = pTargetClusterID;
		mHierarchyLevel = pLevel;
		mTargetToken = pTargetToken;
	}
	
	/**
	 * 
	 * @return Returned is the token of the target cluster in order to identify that.
	 */
	public int getTargetToken()
	{
		return mTargetToken;
	}
	
	/**
	 * 
	 * @param pToken Specify the token of the target cluster here.
	 */
	public void setTargetToken(int pToken)
	{
		mTargetToken = pToken;
	}
	
	
	/**
	 * 
	 * @return The cluster ID of the target cluster is returned. 
	 */
	public Long getTargetClusterID()
	{
		return mTargetClusterID;
	}
	
	/**
	 * 
	 * @return Return the level the source cluster is associated to.
	 */
	public HierarchyLevel getHierarchyLevel()
	{
		return mHierarchyLevel;
	}
	
	/**
	 * As one physical node might be associated to more than one cluster at hierarchy level 0, every cluster manager of
	 * clusters the coordinator is located at that node would create independent connections. Therefore the cluster managers
	 * are bundled via the nested participations.  
	 * 
	 * @return The list of clusters that should be multiplexed by that connection is returned here.
	 */
	public LinkedList<NestedParticipation> getNestedParticipations()
	{
		return mNestedParticipations;
	}
	
	/**
	 * 
	 * @param pParticipation Add one more nested participation via this method.
	 */
	public void addNestedparticipation(NestedParticipation pParticipation)
	{
		Logging.log(this, "Adding nested participation: " + pParticipation);
		
		if(mNestedParticipations == null) {
			mNestedParticipations = new LinkedList<NestedParticipation>();
		}
		mNestedParticipations.add(pParticipation);
	}
	
	/**
	 * Generates a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		String tResult = getClass().getSimpleName();
		
		for (NestedParticipation tEntry : mNestedParticipations){
			tResult += "\n    .." + tEntry.toString();
		}
		
		return tResult;
	}
	
	/**
	 * As one physical node might be associated to more than one cluster at hierarchy level 0, every cluster manager of
	 * clusters the coordinator is located at that node would create independent connections. Therefore the cluster managers
	 * are bundled via the nested participations.  
	 * 
	 * The list of the nested participations can be retrieved by the ClusterParticipationProperty.getNestedParticipations() method.
	 */
	public class NestedParticipation implements Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -6712697028015706544L;
		private Long mSourceClusterID;
		private int mSourceToken;
		private ClusterName mPredecessor;
		private LinkedList<DiscoveryEntry> mDiscoveries;
		private HierarchyLevel mLevel = null;
		private BullyPriority mSenderPriority = null;
		
		/**
		 * 
		 * @param pSourceClusterID Specify the cluster identity via this number.
		 * @param pSourceToken Provide the token of the source cluster here.
		 */
		public NestedParticipation(Long pSourceClusterID, int pSourceToken)
		{
			mSourceClusterID = pSourceClusterID;
			mSourceToken = pSourceToken;
		}
		
		/**
		 * 
		 * @param pPriority This is the priority of the cluster member. It is transmitted already here to
		 * decrease communication complexity.
		 */
		public void setSenderPriority(BullyPriority pPriority)
		{
			Logging.log(this, "Setting sender priority to " + pPriority);
			mSenderPriority = pPriority;
		}
		
		/**
		 * 
		 * @return This is the priority of the cluster member. It is already here transmitted to
		 * decrease communication complexity.
		 */
		public BullyPriority getSenderPriority()
		{
			return mSenderPriority;
		}
		
		/**
		 * 
		 * @param pLevel Set the level of the source cluster here. In general this is one level below the level of
		 * the cluster that should be joined. So the level of this nested exception is below the level of the
		 * ClusterParticipationProperty this nested participation is part of.
		 */
		public void setLevel(HierarchyLevel pLevel)
		{
			mLevel = pLevel;
		}
		
		/**
		 * Get the level of the source cluster here. In general this is one level below the level of
		 * the cluster that should be joined. So the level of this nested exception is below the level of the
		 * ClusterParticipationProperty this nested participation is part of.
		 * 
		 * @return The level of the cluster that should participate is returned.
		 */
		public HierarchyLevel getLevel()
		{
			return mLevel;
		}
		
		/**
		 * 
		 * @param pAddress This is the address of the node that is about to join the cluster.
		 */
		public void setSourceRoutingServiceAddress(HRMName pAddress)
		{
			mSourceAddress = pAddress;
		}
		
		/**
		 * 
		 * @param pPredecessor This has to be the second last cluster of the path to the target cluster. Once the target interprets
		 * that cluster it knows which "outgoing" cluster should be used. in order to reach the node that generated the participation
		 * property.
		 */
		public void setPredecessor(ClusterName pPredecessor)
		{
			mPredecessor = pPredecessor;
		}
		
		/**
		 * 
		 * @return This is be the second last cluster of the path to the target cluster. Once the target interprets
		 * this cluster it knows which "outgoing" cluster should be used. in order to reach the node that generated the participation
		 * property.
		 * 
		 */
		public ClusterName getPredecessor()
		{
			return mPredecessor;
		}
		
		/**
		 * 
		 * @param pSource This is the name of the host or node that is about to join the target cluster.
		 */
		public void setSourceName(Name pSource)
		{
			mSourceName = pSource;
		}
		
		/**
		 * 
		 * @return This is the name of the host or node that is about to join the target cluster.
		 */
		public Name getSourceName()
		{
			return mSourceName;
		}
		
		/**
		 * 
		 * @return The address of the entity that wishes to become member of the cluster is returned. 
		 */
		public HRMName getSourceAddress()
		{
			return mSourceAddress;
		}
		
		/**
		 * 
		 * @param pToken This is the token of the cluster that is about to become member.
		 */
		public void setSourceToken(int pToken)
		{
			mSourceToken = pToken;
		}
		
		/**
		 * 
		 * @return The token of the cluster the coordinator is responsible for is returned here.
		 */
		public int getSourceToken()
		{
			return mSourceToken;
		}
		
		/**
		 * As the target cluster/coordinator has to be informed about the topology and especially has to receive
		 * the knowledge as to how the source node of the participation request can be reached.
		 * 
		 * @param pEntry
		 */
		public void addDiscoveryEntry(DiscoveryEntry pEntry)
		{
			if(mDiscoveries == null) {
				mDiscoveries = new LinkedList<DiscoveryEntry>();
				mDiscoveries.add(pEntry);
			} else {
				mDiscoveries.add(pEntry);
			}
		}
		
		/**
		 * 
		 * @return The neighbors of the source node are returned by this method.
		 */
		public LinkedList<DiscoveryEntry> getNeighbors()
		{
			return mDiscoveries;
		}
		
		public String toString()
		{
			return getClass().getSimpleName() + ":TARGET(" + mTargetClusterID + ")SOURCE(" + mSourceClusterID + ")STOKEN(" + mSourceToken + ")DTOKEN(" + mTargetToken + ")";
		}
		
		/**
		 * 
		 * @return The cluster identity the coordinator represents is returned.
		 */
		public Long getSourceClusterID()
		{
			return mSourceClusterID;
		}
		
		/**
		 * 
		 * @param pSourceClusterID The cluster identity the coordinator represents can be set by this function.
		 */
		public void setSourceClusterID(Long pSourceClusterID)
		{
			mSourceClusterID = pSourceClusterID;
		}
	}
}
