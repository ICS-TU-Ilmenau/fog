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
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.election.Elector;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class represents a cluster member (can also be a cluster head).
 */
public class ClusterMember extends ClusterName
{
	private static final long serialVersionUID = -8746079632866375924L;

	/**
	 * Stores if the neighborhood is already initialized
	 */
	private boolean mNeighborhoodInitialized = false;

	/**
	 * Stores the name of the node where the coordinator of the addressed cluster is located
	 */
	protected Name mCoordinatorNodeName = null;
	
	/**
	 * Stores the elector which is responsible for coordinator elections for this cluster.
	 */
	protected Elector mElector = null;

	/**
	 * Constructor
	 *  
	 * @param pHRMController the local HRMController instance
	 * @param pHierarchyLevel the hierarchy level
	 * @param pClusterID the unique ID of this cluster
	 * @param pCoordinatorID the unique coordinator ID for this cluster
	 * @param pCoordinatorNodeName the node name where the coordinator of this cluster is located
	 */
	public ClusterMember(HRMController pHRMController, HierarchyLevel pHierarchyLevel, Long pClusterID, int pCoordinatorID, Name pCoordinatorNodeName)
	{	
		super(pHRMController, pHierarchyLevel, pClusterID, pCoordinatorID);

		// store the name of the node where the coordinator is located
		mCoordinatorNodeName = pCoordinatorNodeName;

		Logging.log(this, "CREATED");
	}

	/**
	 * Factory function
	 *  
	 * @param pHRMController the local HRMController instance
	 * @param pClusterName a ClusterName which includes the hierarchy level, the unique ID of this cluster, and the unique coordinator ID
	 * @param pClusterID the unique ID of this cluster
	 * @param pCoordinatorNodeName the node name where the coordinator of this cluster is located
	 */
	public static ClusterMember create(HRMController pHRMController, ClusterName pClusterName, Name pCoordinatorNodeName)
	{	
		ClusterMember tResult = new ClusterMember(pHRMController, pClusterName.getHierarchyLevel(), pClusterName.getClusterID(), pClusterName.getCoordinatorID(), pCoordinatorNodeName);
		
		// detect neighbor clusters (members), increase the Bully priority based on the local connectivity
		tResult.initializeNeighborhood();

		Logging.log(tResult, "\n\n\n################ CREATED CLUSTER MEMBER on hierarchy level: " + (tResult.getHierarchyLevel().getValue()));

		// register at HRMController's internal database
		pHRMController.registerClusterMember(tResult);

		// creates new elector object, which is responsible for Bully based election processes
		tResult.mElector = new Elector(pHRMController, tResult);
		
		return tResult;
	}

	/**
	 * Detects neighbor clusters and increases the cluster's Bully priority based on the local connectivity. 
	 */
	protected void initializeNeighborhood()
	{
		Logging.log(this, "Checking local connectivity for increasing priority " + getPriority().getValue());

		/**
		 * Store neighborhood in ARG for every locally known cluster at this hierarchy level 
		 */
		for(ClusterMember tClusterMember : mHRMController.getAllClusterMembers(getHierarchyLevel()))
		{
			if (tClusterMember != this)
			{
				Logging.log(this, "      ..found known neighbor cluster (member): " + tClusterMember);
				
				// add this cluster as neighbor to the already known one
				tClusterMember.registerLocalNeighborARG(this);
			}
		}
		
		mNeighborhoodInitialized = true;
	}

	/**
	 * Returns true if the neighborhood is already initialized - otherwise false
	 * This function is used by the elector to make sure that the local neighborhood is already probed and initialized.
	 *  
	 * @return true of false
	 */
	public boolean isNeighborHoodInitialized()
	{
		return mNeighborhoodInitialized;
	}

	
	
	
	

//	public void handleNeighborAnnouncement(AnnounceRemoteCluster pAnnounce, ComChannel pCEP)
//	{
//		if(pAnnounce.getRoutingVectors() != null) {
//			for(RoutingServiceLinkVector tVector : pAnnounce.getRoutingVectors()) {
//				mHRMController.getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
//			}
//		}
//		Cluster tCluster = mHRMController.getClusterByID(new ClusterName(mHRMController, pAnnounce.getLevel(), pAnnounce.getToken(), pAnnounce.getClusterID()));
//		if(tCluster == null)
//		{
//			Logging.log(this, "     ..creating cluster proxy");
//			ClusterProxy tClusterProxy = new ClusterProxy(mHRMController, pAnnounce.getClusterID(), getHierarchyLevel(), pAnnounce.getCoordinatorName(), pAnnounce.getToken());
//			mHRMController.setSourceIntermediateCluster(tClusterProxy, mHRMController.getSourceIntermediateCluster(this));
//			tClusterProxy.setPriority(pAnnounce.getCoordinatorsPriority());
//			tClusterProxy.setSuperiorCoordinatorID(pAnnounce.getToken());
//			registerNeighborARG(tClusterProxy);
//		} else {
//			Logging.log(this, "Cluster announced by " + pAnnounce + " is an intermediate neighbor ");
//			registerNeighborARG(tCluster);
//		}
//		//((AttachedCluster)tCluster).setNegotiatingHost(pAnnounce.getAnnouncersAddress());
//
//		/*
//		 * function checks whether neighbor relation was established earlier
//		 */
//
//		if(pAnnounce.getCoordinatorName() != null) {
//			mHRMController.getHRS().mapFoGNameToL2Address(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
//		}
//	}

	/**
	 * EVENT: "lost cluster member", triggered by Elector in case a member left the election 

	 * @param pComChannel the comm. channel of the lost cluster member
	 */
	public void eventClusterMemberLost(ComChannel pComChannel)
	{
		Logging.log(this, "EVENT: lost cluster member, comm. channel: " + pComChannel);
	}

	/**
	 * Sends a packet to the coordinator
	 * 
	 * @param pPacket the packet
	 */
	public void sendCoordinator(Serializable pPacket)
	{
		Logging.log(this, "Sending to superior coordinator: " + pPacket);
		
		if(superiorCoordinatorComChannel() != null){
			superiorCoordinatorComChannel().sendPacket(pPacket);
		}else{
			Logging.warn(this, "Channel to superior coordinator is invalid");
		}
	}

	/**
	 * Sends a packet as broadcast to all cluster members
	 * 
	 * @param pPacket the packet which has to be broadcasted
	 */
	public void sendClusterBroadcast(Serializable pPacket, boolean pIncludeLoopback)
	{
		// get all communication channels
		LinkedList<ComChannel> tComChannels = getComChannels();

		// get the L2Addres of the local host
		L2Address tLocalL2Address = mHRMController.getHRS().getCentralFNL2Address();
		
		Logging.log(this, "Sending BROADCASTS from " + tLocalL2Address + " the packet " + pPacket + " to " + tComChannels.size() + " communication channels");
		
		for(ComChannel tComChannel : tComChannels) {
			boolean tIsLoopback = tLocalL2Address.equals(tComChannel.getPeerL2Address());
			
			if (!tIsLoopback){
				Logging.log(this, "       ..to " + tComChannel);
			}else{
				Logging.log(this, "       ..to LOOPBACK " + tComChannel);
			}

			if ((HRMConfig.Hierarchy.SIGNALING_INCLUDES_LOCALHOST) || (pIncludeLoopback) || (!tIsLoopback)){
				// send the packet to one of the possible cluster members
				tComChannel.sendPacket(pPacket);
			}else{
				Logging.log(this, "              ..skipping " + (tIsLoopback ? "LOOPBACK CHANNEL" : ""));
			}
		}
	}
	public void sendClusterBroadcast(Serializable pPacket)
	{
		sendClusterBroadcast(pPacket, false);
	}

	/**
	 * Determines the coordinator of this cluster. It is "null" if the election was lost or hasn't finished yet. 
	 * 
	 * @return the cluster's coordinator
	 */
	public Coordinator getCoordinator()
	{
		Logging.err(this, "!!!!! >>> ClusterProxy::getCoordinator() should never be called, otherwise, an error in higher clustering code exists <<< !!!!!");
		return null;
	}

	/**
	 * Returns how many connected cluster members are known
	 * 
	 * @return the count
	 */
	public int countConnectedClusterMembers()
	{
		int tResult = 0;

		// count all communication channels
		tResult = getComChannels().size();

		return tResult;
	}

	/**
	 * Returns how many connected external cluster members are known
	 * 
	 * @return the count
	 */
	public int countConnectedRemoteClusterMembers()
	{
		int tResult = 0;

		// if the local host is also treated as cluster member, we return an additional cluster member 
		if (HRMConfig.Hierarchy.SIGNALING_INCLUDES_LOCALHOST){
			tResult++;
		}
		
		// get all communication channels
		LinkedList<ComChannel> tComChannels = getComChannels();

		// get the L2Addres of the local host
		L2Address tLocalL2Address = mHRMController.getHRS().getCentralFNL2Address();
		
		for(ComChannel tComChannel : tComChannels) {
			boolean tIsLoopback = tLocalL2Address.equals(tComChannel.getPeerL2Address());
			
			// filter loopback channels
			if (!tIsLoopback){
				tResult++;
			}
		}

		return tResult;
	}

	/**
	 * Returns a hash code for this object.
	 * This function is used within the ARG for identifying objects.
	 * 
	 * @return the hash code
	 */
	@Override
	public int hashCode()
	{
		return getClusterID().intValue();
	}

	/**
	 * Returns the elector of this cluster
	 * 
	 * @return the elector
	 */
	public Elector getElector()
	{
		return mElector;
	}

	/**
	 * Returns the name of the node where the coordinator of the described cluster is located
	 * 
	 * @return the node name
	 */
	public Name getCoordinatorNodeName()
	{
		return mCoordinatorNodeName;
	}
	
	/**
	 * Defines the decoration text for the ARG viewer
	 * 
	 * @return text for the control entity or null if no text is available
	 */
	@Override
	public String getText()
	{
		return "Cluster" + getGUIClusterID() + "@" + mHRMController.getNodeGUIName() + "@" + getHierarchyLevel().getValue() + "(" + idToString() + ", Coord.=" + getCoordinatorNodeName()+ ")";
	}

	/**
	 * Returns a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return toLocation() + "(" + idToString() + ", Coord.=" + getCoordinatorNodeName()+ ")";
	}

	/**
	 * Returns a location description about this instance
	 */
	@Override
	public String toLocation()
	{
		String tResult = getClass().getSimpleName() + getGUIClusterID() + "@" + mHRMController.getNodeGUIName() + "@" + getHierarchyLevel().getValue();
		
		return tResult;
	}
	
	/**
	 * Returns a string including the ClusterID, the token, and the node priority
	 * 
	 * @return the complex string
	 */
	private String idToString()
	{
		if (getHRMID() == null){
			return "ID=" + getClusterID() + ", CoordID=" + superiorCoordinatorID() +  ", Prio=" + getPriority().getValue();
		}else{
			return "HRMID=" + getHRMID().toString();
		}
	}
}
