/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Random;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ClusterDummy;
import de.tuilmenau.ics.fog.util.Tuple;

/**
 * PACKET: This class carries information of other clusters that have to be discovered by other nodes
 */
public class ClusterDiscovery implements Serializable
{	
	private static final long serialVersionUID = -5917475359148543777L;
	private LinkedList<NestedDiscovery> mDiscoveries = null;
	private boolean mRequest = false;
	private boolean mDealtWith = false;
	private int mIdentification = 0;
	private Name mSource;
	
	/**
	 * 
	 * @param pSource is the name of the source that wishes to discover nearby not yet known clusters
	 */
	public ClusterDiscovery(Name pSource)
	{
		mDiscoveries = new LinkedList<NestedDiscovery>();
		mRequest = true;
		Random tRandom = new Random(System.currentTimeMillis());
		mIdentification = tRandom.nextInt();
		mSource = pSource;
	}
	
	/**
	 * Because clusters are multiplexed in order to decrease communication one physical node may contain multiple discovery requests that are
	 * saved inside one ClusterDiscovery
	 * 
	 * @param pDiscovery
	 */
	public void addNestedDiscovery(NestedDiscovery pDiscovery)
	{
		mDiscoveries.add(pDiscovery);
	}
	
	/**
	 * Because clusters are multiplexed in order to decrease communication one physical node may contain multiple discovery requests that are
	 * saved inside one ClusterDiscovery
	 * 
	 * @return all nested discoveries
	 */
	public LinkedList<NestedDiscovery> getDiscoveries()
	{
		return mDiscoveries;
	}

	public String toString()
	{
		return getClass().getSimpleName() + ":"  + mSource + "(" + mIdentification + "):" /*+ tResult*/ ;
	}
	
	/**
	 * Set flag that processing of this message was completed
	 */
	public void completed()
	{
		mDealtWith = true;
	}
	
	/**
	 * 
	 * @return true if processing of this discovery is completed
	 */
	public boolean isDealtWith()
	{
		return mDealtWith;
	}
	
	/**
	 * Say that this discovery is not a request anymore
	 */
	public void isAnswer()
	{
		mRequest = false;
	}
	
	/**
	 * Find out whether this message is a request or not.
	 */
	public boolean isRequest()
	{
		return mRequest;
	}

	/**
	 * 
	 * Because clusters are multiplexed in order to decrease communication one physical node may contain multiple discovery requests that are
	 * saved inside one ClusterDiscovery.
	 * 
	 * This is one object that is used by a coordinator to discover the nearby neighborhood of another coordinator
	 */
	public class NestedDiscovery implements Serializable
	{
		private static final long serialVersionUID = -781019033813113905L;
		private LinkedList<DiscoveryEntry> mDiscoveryEntries = null;
		private LinkedList<Integer> mTokens = null;
		private LinkedList<Tuple<ClusterDummy, ClusterDummy>> mNeighbors = null;
		private Long mSourceClusterID;
		private int mDistance = 0;
		private int mToken;
		private int mLevel;
		//private boolean mRequest = false;
		private Long mTargetClusterID;
		private Long mOriginClusterID;
		
		/**
		 * 
		 * @param pTokens inclues all tokens that are ALREADY KNOWN to the coordinator that emits this discovery request
		 * @param pSourceClusterID is the Cluster ID of the initiator of this message
		 * @param pToken is the token that is used to identify the cluster this discovery request comes from
		 * @param pLevel is the level at which other clusters should be discovered
		 * @param pDistance is the maximum distance at which other clusters are wished to be known by the initiator of this message
		 */
		public NestedDiscovery(LinkedList<Integer> pTokens, Long pSourceClusterID, int pToken, int pLevel, int pDistance)
		{
			setTokens(pTokens);
			mSourceClusterID = pSourceClusterID;
			mDistance = pDistance;
			mLevel = pLevel;
			mToken = pToken;			
		}
		
		/**
		 * 
		 * @return ClusterID here
		 */
		public Long getOrigin()
		{
			return mOriginClusterID;
		}
		
		/**
		 * 
		 * @param pClusterID used for identification of the sender
		 */
		public void setOrigin(Long pClusterID)
		{
			mOriginClusterID = pClusterID;
		}
		
		/**
		 * 
		 * @param pTargetClusterID is the cluster identity from which the target coordinator is supposed to perform the search on the neighborhood 
		 */
		public void setTargetClusterID(Long pTargetClusterID)
		{
			mTargetClusterID = pTargetClusterID;
		}
		
		/**
		 * 
		 * @return the cluster identity from which the target coordinator is supposed to perform the search on the neighborhood
		 */
		public Long getTargetClusterID()
		{
			return mTargetClusterID;
		}
		
		/**
		 * 
		 * @return list of tokens that identify clusters that are already known to the sender of this discovery request
		 */
		public LinkedList<Integer> getTokens()
		{
			return mTokens;
		}

		/**
		 * 
		 * @param pTokens is a list of tokens that identify clusters that are already known to the sender of this discovery request
		 */
		public void setTokens(LinkedList<Integer> pTokens)
		{
			mTokens = pTokens;
		}

		/**
		 * 
		 * @return list of entries that represent the clusters discovered along with meta information about those (coordinator address, cluster ID etc)
		 */
		public LinkedList<DiscoveryEntry> getDiscoveryEntries()
		{
			if(mDiscoveryEntries == null) {
				mDiscoveryEntries = new LinkedList<DiscoveryEntry>();
			}
			return mDiscoveryEntries;
		}

		/**
		 * 
		 * @param pVector add one entry that represents a cluster discovered along with meta information about those (coordinator address, cluster ID etc)
		 */
		public void addDiscoveryEntry(DiscoveryEntry pVector)
		{
			if(mDiscoveryEntries == null) {
				mDiscoveryEntries = new LinkedList<DiscoveryEntry>();
			}
			mDiscoveryEntries.add(pVector);
		}
		
		public String toString()
		{
			String tResult = new String();
			tResult = "\n" + getClass().getSimpleName() + "\nFROM " + mSourceClusterID + "\nTO " + mTargetClusterID + "\n{";
			if(mDiscoveryEntries != null) {
				for(DiscoveryEntry tDiscovery : mDiscoveryEntries) {
					tResult += tDiscovery + "\n";
				}
			}
			tResult += "}TOKENS(" + mTokens + ")" + ( isRequest() ? "REQUEST" : "REPLY" ); 
			return  tResult;
		}

		/**
		 * 
		 * @param pFirst is the beginning of the undirected edge that represents a connection between the two clusters
		 * @param pSecond is the end of the undirected edge that represents a connection between the two clusters
		 */
		public void addNeighborRelation(ClusterDummy pFirst, ClusterDummy pSecond)
		{
			if(mNeighbors == null) {
				mNeighbors = new LinkedList<Tuple<ClusterDummy, ClusterDummy>>();
				mNeighbors.add(new Tuple<ClusterDummy, ClusterDummy>(pFirst, pSecond));
			} else {
				if(mNeighbors.contains(new Tuple<ClusterDummy, ClusterDummy>(pFirst, pSecond)) || mNeighbors.contains(new Tuple<ClusterDummy, ClusterDummy>(pSecond, pFirst))) {
					//don't add
				} else {
					mNeighbors.add(new Tuple<ClusterDummy, ClusterDummy>(pFirst, pSecond));
				}
				
			}
		}
		
		/**
		 * 
		 * @return a list of neighbor relations: connection between clusters
		 */
		public LinkedList<Tuple<ClusterDummy, ClusterDummy>> getNeighborRelations()
		{
			return mNeighbors;
		}
		
		/**
		 * 
		 * @return token of the the cluster for which a coordinator initated a request on neighbors
		 */
		public int getToken()
		{
			return mToken;
		}
		
		/**
		 * 
		 * @return level at which cluster is located
		 */
		public int getLevel()
		{
			return mLevel;
		}
		
		/**
		 * 
		 * @return distance to cluster - seen from entity that sends 
		 */
		public int getDistance()
		{
			return mDistance;
		}
		
		/**
		 * 
		 * @return find out the cluster identity of the entity that sent the request
		 */
		public Long getSourceClusterID()
		{
			return mSourceClusterID;
		}
		
	}
}
