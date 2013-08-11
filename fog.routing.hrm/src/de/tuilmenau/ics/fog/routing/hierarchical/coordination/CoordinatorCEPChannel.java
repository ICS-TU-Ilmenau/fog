/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.coordination;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.AssignHRMID;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.ClusterDiscovery.NestedDiscovery;
import de.tuilmenau.ics.fog.packets.hierarchical.DiscoveryEntry;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborClusterAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.RequestCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyData;
import de.tuilmenau.ics.fog.packets.hierarchical.election.*;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.RoutingInformation;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMSignature;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalRoutingService;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingEntry;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ClusterName;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.HierarchyLevel;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ICluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.HRMGraphNodeName;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.NeighborCluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.RoutableClusterGraphLink;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.graph.RoutableGraph;
import edu.uci.ics.jung.algorithms.shortestpath.BFSDistanceLabeler;

/**
 * The class is used for the communication between a cluster and its coordinator.
 * For this purpose, both the cluster object and the coordinator object have a reference to this object. 
 */
public class CoordinatorCEPChannel
{
	private static final long serialVersionUID = -8290946480171751216L;
	private ClusterName mRemoteCluster;
	private ICluster mPeerCluster;
	private BullyPriority mPeerPriority = null;
	private boolean mKnowsCoordinator = false;
	private boolean mPartOfCluster = false;
	private HRMController mHRMController = null;
	private BFSDistanceLabeler<HRMGraphNodeName, RoutableClusterGraphLink> mBreadthFirstSearch;
	
	/**
	 * For COORDINATORS: Stores the HRMID under which the corresponding peer cluster member is addressable.
	 */
	private HRMID mPeerHRMID = null;
	
	/**
	 * Stores the L2 address of the peer
	 */
	private L2Address mPeerL2Address = null;
	
	/**
	 * 
	 * @param pHRMController is the coordinator of a node
	 * @param pPeerCluster is the peer cluster/coordinator
	 */
	public CoordinatorCEPChannel(HRMController pHRMController, ICluster pPeerCluster)
	{
		mHRMController = pHRMController;
		mPeerCluster = pPeerCluster;
		mPeerPriority = new BullyPriority(this);
		Logging.log(this, "CREATED for " + mPeerCluster);
	}
	
	/**
	 * Defines the HRMID of the peer which is a cluster member.
	 * 
	 * @param pHRMID the new HRMID under which the peer is addressable
	 */
	public void setPeerHRMID(HRMID pHRMID)
	{
		mPeerHRMID = pHRMID.clone();		
	}
	
	/**
	 * Determines the address of the peer (e.g., a cluster member).
	 * 
	 * @return the HRMID of the peer or "null"
	 */
	public HRMID getPeerHRMID()
	{
		return mPeerHRMID;
	}
	
	/**
	 * Returns the L2 address of the peer (cluster member).
	 * 
	 * @return the desired L2 address
	 */
	public L2Address getPeerL2Address()
	{
		return mPeerL2Address;
	}

	/**
	 * Handles a SignalingMessageHrm packet.
	 * 
	 * @param pSignalingMessageHrmPacket the packet
	 */
	private void handleSignalingMessageHRM(SignalingMessageHrm pSignalingMessageHrmPacket)
	{
		// can we learn the peer's HRMID from the packet?
		if (pSignalingMessageHrmPacket.getSenderName() instanceof HRMID){
			// get the HRMID of the peer
			HRMID tPeerHRMID = (HRMID)pSignalingMessageHrmPacket.getSenderName();
			
			// update peer's HRMID
			setPeerHRMID(tPeerHRMID);
		}		
	}

	/**
	 * Handles a RoutingInformation packet.
	 * 
	 * @param pRoutingInformationPacket the packet
	 */
	private void handleSignalingMessageSharePhase(RoutingInformation pRoutingInformationPacket)
	{
		if (HRMConfig.DebugOutput.SHOW_SHARE_PHASE){
			Logging.log(this, "SHARE PHASE DATA received from \"" + getPeerHRMID() + "\", DATA: " + pRoutingInformationPacket);
		}
		
		for (RoutingEntry tEntry : pRoutingInformationPacket.getRoutes()){
			if (HRMConfig.DebugOutput.SHOW_SHARE_PHASE)
				Logging.log(this, "      ..found route: " + tEntry);
			
			getHRMController().addHRMRoute(tEntry);
		}
	}

	/**
	 * Handles a Bully signaling packet.
	 * 
	 * @param pPacketBully the packet
	 */
	private void handleSignalingMessageBully(SignalingMessageBully pPacketBully) throws NetworkException
	{
		Node tNode = getHRMController().getNode();
		Name tLocalNodeName = tNode.getCentralFN().getName(); 
				
		boolean BULLY_SIGNALING_DEBUGGING = true;

		if (getPeer() == null){
			Logging.warn(this, "Peer is still invalid!");
		}
		
		/**
		 * ELECT
		 */
		if(pPacketBully instanceof BullyElect)	{
			
			// cast to Bully elect packet
			BullyElect tPacketBullyElect = (BullyElect)pPacketBully;
			
			if (BULLY_SIGNALING_DEBUGGING)
				Logging.log(this, "BULLY-received from \"" + mPeerCluster + "\" an ELECT: " + tPacketBullyElect);

			if ((getPeer().getSuperiorCoordinatorCEP() != null) && (getPeer().getHighestPriority().isHigher(this, tPacketBullyElect.getSenderPriority()))) {
				
				mPeerPriority = tPacketBullyElect.getSenderPriority();
				
				if (getPeer().getHRMController().equals(tLocalNodeName)) {
					// create ANNOUNCE packet
					HRMSignature tSignature = getHRMController().getIdentity().createSignature(tNode.toString(), null, getPeer().getHierarchyLevel());
					
					BullyAnnounce tAnnouncePacket = new BullyAnnounce(tLocalNodeName, getPeer().getBullyPriority(), tSignature, getPeer().getToken());
					
					for(CoordinatorCEPChannel tCEP : getPeer().getClusterMembers()) {
						tAnnouncePacket.addCoveredNode(tCEP.getPeerName());
					}
					if(tAnnouncePacket.getCoveredNodes() == null || (tAnnouncePacket.getCoveredNodes() != null && tAnnouncePacket.getCoveredNodes().isEmpty())) {
						Logging.log(this, "Sending announce that does not cover anyhting");
					}

					// send packet
					if (BULLY_SIGNALING_DEBUGGING)
						Logging.log(this, "BULLY-sending to \"" + mPeerCluster + "\" an ANNOUNCE: " + tAnnouncePacket);
					sendPacket(tAnnouncePacket);
					
				} else {
					// create ALIVE packet
					BullyAlive tAlivePacket = new BullyAlive(tLocalNodeName);
					
					// send packet
					if (BULLY_SIGNALING_DEBUGGING)
						Logging.log(this, "BULLY-sending to \"" + mPeerCluster + "\" an ALIVE: " + tAlivePacket);
					sendPacket(tAlivePacket);
					//TODO: packet is sent but never parsed or a timeout timer reset!!
				}
			} else {
				if (getPeer() instanceof Cluster){
					// store peer's Bully priority
					//TODO: peer prio direkt mal abspeichern und auf größte checken!
					mPeerPriority = tPacketBullyElect.getSenderPriority();
					
					// create REPLY packet
					BullyReply tReplyPacket = new BullyReply(tLocalNodeName, getPeerHRMID(), getPeer().getBullyPriority());
					
					// send the answer packet
					if (BULLY_SIGNALING_DEBUGGING)
						Logging.log(this, "BULLY-sending to \"" + mPeerCluster + "\" a REPLY: " + tReplyPacket);
					sendPacket(tReplyPacket);
				}else{
					Logging.err(this, "Peer is not a cluster, skipping BullyReply message, peer is " + getPeer());
				}
			}
		}
		
		/**
		 * REPLY
		 */
		if(pPacketBully instanceof BullyReply) {
			
			// cast to Bully replay packet
			BullyReply tReplyPacket = (BullyReply)pPacketBully;

			if (BULLY_SIGNALING_DEBUGGING)
				Logging.log(this, "BULLY-received from \"" + mPeerCluster + "\" a REPLY: " + tReplyPacket);

			// store peer's Bully priority
			//TODO: peer prio direkt mal abspeichern und auf größte checken!
			mPeerPriority = tReplyPacket.getSenderPriority();
		}
		
		/**
		 * ANNOUNCE
		 */
		if(pPacketBully instanceof BullyAnnounce)  {
			// cast to Bully replay packet
			BullyAnnounce tAnnouncePacket = (BullyAnnounce)pPacketBully;

			if (BULLY_SIGNALING_DEBUGGING)
				Logging.log(this, "BULLY-received from \"" + mPeerCluster + "\" an ANNOUNCE: " + tAnnouncePacket);

			//TODO: only an intermediate cluster on level 0 is able to store an announcement and forward it once a coordinator is set
			getPeer().handleBullyAnnounce(tAnnouncePacket, this);
		}

		/**
		 * PRIORITY UPDATE
		 */
		if(pPacketBully instanceof BullyPriorityUpdate) {
			// cast to Bully replay packet
			BullyPriorityUpdate tPacketBullyPriorityUpdate = (BullyPriorityUpdate)pPacketBully;

			if (BULLY_SIGNALING_DEBUGGING)
				Logging.log(this, "BULLY-received from \"" + mPeerCluster + "\" a PRIORITY UPDATE: " + tPacketBullyPriorityUpdate);

			// store peer's Bully priority
			mPeerPriority = tPacketBullyPriorityUpdate.getSenderPriority();
		}
	}
	
	/**
	 * 
	 * @param pData is the data that should be sent to the receiver side of this connection end point
	 * @return true if the packet left the central multiplexer and the forwarding node that is attached to a direct down gate
	 * @throws NetworkException
	 */
	public boolean receive(Serializable pData) throws NetworkException
	{
		if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS){
			Logging.log(this, "RECEIVED DATA from \"" + getPeerName() + "/" + getPeerHRMID() + "\": " + pData);
		}
			

		Node tNode = getHRMController().getNode();
		HierarchicalRoutingService tHRS = getHRMController().getHRS();
		
		/*
		 * Invalid data
		 */
		if(pData == null) {
			throw new NetworkException("Received invalid null pointer as data");
		}

		/*
		 * main packet processing
		 */
		try {
			
			/**
			 * HRM signaling message
			 */
			if (pData instanceof SignalingMessageHrm){
				// cast to a SignalingMessageHrm signaling message
				SignalingMessageHrm tSignalingMessageHrmPacket = (SignalingMessageHrm)pData;
			
				// process SignalingMessageHrm message
				handleSignalingMessageHRM(tSignalingMessageHrmPacket);
				
				//HINT: don't return here because we are still interested in the more detailed packet data from derived packet types!
			}
			
			/**
			 * Bully signaling message
			 */
			if (pData instanceof SignalingMessageBully) {
				// cast to a Bully signaling message
				SignalingMessageBully tBullyMessage = (SignalingMessageBully)pData;
			
				// process Bully message
				handleSignalingMessageBully(tBullyMessage);
				
				return true;
			}

			/**
			 * RoutingInformation
			 */
			if (pData instanceof RoutingInformation){
				// cast to a RoutingInformation signaling message
				RoutingInformation tRoutingInformationPacket = (RoutingInformation)pData;

				// process Bully message
				handleSignalingMessageSharePhase(tRoutingInformationPacket);
				
				return true;
			}
			
			
			
			
			
			/**
			 * NeighborClusterAnnounce
			 */
			if(pData instanceof NeighborClusterAnnounce) {
				NeighborClusterAnnounce tAnnouncePacket = (NeighborClusterAnnounce)pData;

				if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
					Logging.log(this, "NEIGHBOR received from \"" + mPeerCluster + "\" a NEIGHBOR CLUSTER ANNOUNCE: " + tAnnouncePacket);

				if(tAnnouncePacket.isInterASAnnouncement()) {
					Logging.log(this, tNode.getAS().getName() + " received an announcement from " + tAnnouncePacket.getASIdentification());
					if(tNode.getAS().getName().equals(tAnnouncePacket.getASIdentification())) {
						if(!getSourceName().equals(getPeerName())) {
							for(Route tPath : tHRS.getCoordinatorRoutingMap().getRoute(getSourceName(), getPeerName())) {
								tAnnouncePacket.addRoutingVector(new RoutingServiceLinkVector(tPath, tHRS.getCoordinatorRoutingMap().getSource(tPath), tHRS.getCoordinatorRoutingMap().getDest(tPath)));
							}
						}
					} else {
						if(getPeer() instanceof Cluster) {
							if(!getSourceName().equals(getPeerName())) {
								RoutingServiceLinkVector tVector = new RoutingServiceLinkVector(getRouteToPeer(), getSourceName(), getPeerName());
								tAnnouncePacket.addRoutingVector(tVector);
							}
							for(CoordinatorCEPChannel tCEP : getPeer().getClusterMembers()) {
								boolean tWroteAnnouncement = false;
								if(tCEP.getRemoteClusterName().getHierarchyLevel().getValue() - 1 == tAnnouncePacket.getLevel().getValue()) {
									
									// send packet
									tCEP.sendPacket(tAnnouncePacket);
									
									tWroteAnnouncement = true;
								}
								Logging.log(this, "Testing " + tCEP + " whether it leads to the clusters coordinator: " + tWroteAnnouncement);
							}
						} else if(getPeer() instanceof Coordinator) {
							Logging.log(this, "Inter AS announcement " + tAnnouncePacket + " is handled by " + getPeer() + " whether it leads to the clusters coordinator");
							((Coordinator)getPeer()).getManagedCluster().handleNeighborAnnouncement(tAnnouncePacket, this);
						}
					}
				} else {
					if (!(getPeer() instanceof Cluster)){
						Logging.err(this, "Peer should be a cluster here");
					}
					getPeer().handleNeighborAnnouncement(tAnnouncePacket, this);
				}
				Logging.log(this, "Received " + tAnnouncePacket + " from remote cluster " + mRemoteCluster);
			}
			
			/**
			 * AssignHRMID
			 */
			if(pData instanceof AssignHRMID) {
				AssignHRMID tAssignHRMIDPacket = (AssignHRMID)pData;

				if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
					Logging.log(this, "ASSIGN_HRMID-received from \"" + getPeerHRMID() + "\" assigned HRMID: " + tAssignHRMIDPacket.getHRMID().toString());

				if (getPeer() instanceof Coordinator){
					Coordinator tCoordinator = (Coordinator)getPeer();
					tCoordinator.handleAssignHRMID(tAssignHRMIDPacket);
				} else if (getPeer() instanceof Cluster){
					Cluster tCluster = (Cluster)getPeer();
					tCluster.handleAssignHRMIDForPhysicalNode(tAssignHRMIDPacket);
				} 
			}
			
			/**
			 * RequestCoordinator
			 */
			if (pData instanceof RequestCoordinator) {
				RequestCoordinator tRequestCoordinatorPacket = (RequestCoordinator) pData;
				
				if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
					Logging.log(this, "CHANNEL-received from \"" + mPeerCluster + "\" COORDINATOR REQUEST: " + tRequestCoordinatorPacket);

				if(!tRequestCoordinatorPacket.isAnswer()) {
					if(getPeer().getSuperiorCoordinatorCEP() != null) {
						ICluster tCluster = getPeer().getHRMController().getClusterWithCoordinatorOnLevel(getPeer().getHierarchyLevel().getValue());
						Logging.log(this, "Name of coordinator is " + tCluster.getCoordinatorName());
						
						int tToken = tCluster.getToken();
						Name tCoordinatorName = tCluster.getCoordinatorName();
						long tCoordinatorAddress = tCluster.getCoordinatorsAddress().getComplexAddress().longValue();
						HRMName tL2Address = tCluster.getCoordinatorsAddress();
						DiscoveryEntry tEntry = new DiscoveryEntry(tToken, tCoordinatorName, tCoordinatorAddress, tL2Address, tCluster.getHierarchyLevel());
						tEntry.setPriority(getPeer().getCoordinatorPriority());
						tEntry.setRoutingVectors(getPath(tCluster.getCoordinatorsAddress()));
						tRequestCoordinatorPacket.addDiscoveryEntry(tEntry);
						tRequestCoordinatorPacket.setCoordinatorKnown(true);
						tRequestCoordinatorPacket.setAnswer();
					} else {
						tRequestCoordinatorPacket.setCoordinatorKnown(false);
						tRequestCoordinatorPacket.setAnswer();
					}
					
					// send packet
					sendPacket(tRequestCoordinatorPacket);
					
				} else {
					if(tRequestCoordinatorPacket.isCoordinatorKnown()) {
						mKnowsCoordinator = true;
					} else {
						mKnowsCoordinator = false;
					}
					if(tRequestCoordinatorPacket.getDiscoveryEntries() != null) {
						for(DiscoveryEntry tEntry : tRequestCoordinatorPacket.getDiscoveryEntries()) {
							ClusterName tDummy = handleDiscoveryEntry(tEntry);
							getPeer().getHRMController().getCluster(new ClusterName(getPeer().getToken(), ((getSourceName()).getComplexAddress().longValue()), getPeer().getHierarchyLevel())).addNeighborCluster(getPeer().getHRMController().getCluster(tDummy));
						}
					}
					synchronized(tRequestCoordinatorPacket) {
						Logging.log(this, "Received answer to " + tRequestCoordinatorPacket + ", notifying");
						tRequestCoordinatorPacket.notifyAll();
					}
				}
			}
		} catch (PropertyException tExc) {
			Logging.err(this, "Unable to fulfill requirements", tExc);
		}
		return true;
	}
	
	/**
	 * 
	 * @param pTarget is the target to which routing service link vectors should be generated
	 * @return vectors that provide a patch between forwarding nodes along with the gate numbers
	 */
	public LinkedList<RoutingServiceLinkVector> getPath(HRMName pTarget)
	{
		LinkedList<RoutingServiceLinkVector> tVectors = new LinkedList<RoutingServiceLinkVector>();
		RoutableGraph<HRMName, Route> tRoutingDatabase = getHRMController().getHRS().getCoordinatorRoutingMap();
		List<Route> tRoute = tRoutingDatabase.getRoute(getMultiplexer().getSourceRoutingServiceAddress(this), pTarget);
		HRMName tSource = getMultiplexer().getSourceRoutingServiceAddress(this);
		HRMName tDestination;
		if(tRoute == null) {
			return null;
		} else {
			for(int i = 0 ; i < tRoute.size() ; i++) {
				if(tRoute.get(i) instanceof Route) {
					tDestination = tRoutingDatabase.getDest(tRoute.get(i));
					RoutingServiceLinkVector tVector = new RoutingServiceLinkVector(tRoute.get(i), tSource, tDestination);
					tVectors.add(tVector);
					tSource = tDestination;
				}
			}
		}
		return tVectors;
	}

	/**
	 * The nested discovery entry you provide in the first argument is the message that has to be filled with
	 * information on how to get to the cluster(s coordinator) provided as second argument
	 * 
	 * @param pDiscovery entry of the entity that has to be informed about the cluster provided as second argument
	 * @param pCluster as cluster (along with the coordinator) to which a path has to be filled into the discovery entry
	 * @throws NetworkException 
	 * @throws PropertyException in case the requirements to the target coordinator can not be fulfilled
	 */
	private void getPathTo(NestedDiscovery pDiscovery, ICluster pCluster) throws NetworkException, PropertyException
	{
		if(pCluster.getCoordinatorName() != null) {
			DiscoveryEntry tEntry = new DiscoveryEntry(pCluster.getToken(), pCluster.getCoordinatorName(), pCluster.getClusterID(), pCluster.getCoordinatorsAddress(), pCluster.getHierarchyLevel());
			tEntry.setClusterHops(getPeer().getHRMController().getClusterDistance(pCluster));
			tEntry.setPriority(pCluster.getBullyPriority());
			tEntry.setRoutingVectors(getPath(pCluster.getCoordinatorsAddress()));
			if(pCluster.isInterASCluster()) {
				tEntry.setInterASCluster();
			}
			
			List<RoutableClusterGraphLink> tClusterList = getHRMController().getRoutableClusterGraph().getRoute(getPeer(), pCluster);
			if(!tClusterList.isEmpty()) {
				ICluster tPredecessorCluster = (ICluster) getHRMController().getRoutableClusterGraph().getLinkEndNode(pCluster, tClusterList.get(tClusterList.size()-1));
				ClusterName tPredecessorClusterName = new ClusterName(tPredecessorCluster.getToken(), tPredecessorCluster.getClusterID(), tPredecessorCluster.getHierarchyLevel());
				tEntry.setPredecessor(tPredecessorClusterName);
			}
			
			pDiscovery.addDiscoveryEntry(tEntry);
		}
	}
	
	/**
	 * Connection end points control the clusters they are associated to.
	 * 
	 * @return As one node may be associated to more than one cluster you can use this method to find out
	 * which cluster is controlled by this connection end point.
	 */
	public ICluster getPeer()
	{
		return mPeerCluster;
	}
	
	/**
	 * 
	 * @return
	 */
	public ClusterName getRemoteClusterName()
	{
//		ICluster tCluster = null;
//		if(mRemoteCluster instanceof ClusterName) {
//			tCluster = getHRMController().getCluster(mRemoteCluster);
//		}
//		if(getCluster().getHierarchyLevel() == HRMConfig.Hierarchy.BASE_LEVEL) {
//			return getCluster();
//		}
//		return (tCluster == null ? mRemoteCluster : tCluster);
		return mRemoteCluster;
	}

	public HRMController getHRMController()
	{
		return mHRMController;
	}
	
	public void setAsParticipantOfMyCluster(boolean pPartOfMyCluster)
	{
		mPartOfCluster = pPartOfMyCluster;
	}
	
	public boolean knowsCoordinator()
	{
		return mKnowsCoordinator;
	}
	
	public BullyPriority getPeerPriority()
	{
		if (mPeerPriority == null){
			mPeerPriority = new BullyPriority(this);
		}
			
		return mPeerPriority;
	}

	public void setPeerPriority(BullyPriority pPeerPriority)
	{
		if (pPeerPriority == null){
			Logging.warn(this, "Trying to set NULL POINTER as Bully priority, ignoring this request");
			return;
		}
			
		mPeerPriority = pPeerPriority;
	}
	
	public void setRemoteClusterName(ClusterName pClusterName)
	{
		Logging.log(this, "Setting remote/peer cluster " + pClusterName);
		mRemoteCluster = pClusterName;
	}

	public CoordinatorCEPMultiplexer getCEPMultiplexer()
	{
		return getMultiplexer();
	}
	
	public boolean sendPacket(Serializable pData)
	{
		Logging.log(this, "Sending to " + getRemoteClusterName() + " the packet " + pData);
		
		if(pData instanceof RequestCoordinator) {
//			mRequestedCoordinator = true;
			Logging.log(this, "Sending " + pData);
		}
		if(getPeer() instanceof Coordinator) {
			getCEPMultiplexer().write(pData, this, new ClusterName(getPeer().getToken(), ((L2Address)getPeerName()).getComplexAddress().longValue(), getPeer().getHierarchyLevel()));
		} else {
			getCEPMultiplexer().write(pData, this, getRemoteClusterName());
		}
		return true;
	}
	
	public HRMName getPeerName()
	{
		return getMultiplexer().getPeerRoutingServiceAddress(this);
	}
	
	public HRMName getSourceName()
	{
		return getMultiplexer().getSourceRoutingServiceAddress(this);
	}
	
	public boolean isPartOfMyCluster()
	{
		return mPartOfCluster;
	}
	
	public void handleClusterDiscovery(NestedDiscovery pDiscovery, boolean pRequest) throws PropertyException, NetworkException
	{
		if(pRequest){
			Cluster tSourceCluster = getHRMController().getCluster(new ClusterName(pDiscovery.getToken(), pDiscovery.getSourceClusterID(), pDiscovery.getLevel()));
			if(tSourceCluster == null) {
				Logging.err(this, "Unable to find appropriate cluster for" + pDiscovery.getSourceClusterID() + " and token" + pDiscovery.getToken() + " on level " + pDiscovery.getLevel() + " remote cluster is " + getRemoteClusterName());
			}
			if(mBreadthFirstSearch == null ) {
				mBreadthFirstSearch = new BFSDistanceLabeler<HRMGraphNodeName, RoutableClusterGraphLink>();
			}
			mBreadthFirstSearch.labelDistances(getHRMController().getRoutableClusterGraph().getGraphForGUI(), tSourceCluster);
			List<HRMGraphNodeName> tDiscoveryCandidates = mBreadthFirstSearch.getVerticesInOrderVisited();
			if(tSourceCluster != null) {
				for(HRMGraphNodeName tVirtualNode : tDiscoveryCandidates) {
					if(tVirtualNode instanceof ICluster) {
						ICluster tCluster = (ICluster) tVirtualNode;
						
						int tRadius = HRMConfig.Routing.EXPANSION_RADIUS;
						Logging.log(this, "Radius is " + tRadius);
						
						if(tCluster instanceof NeighborCluster && ((NeighborCluster)tCluster).getClusterDistanceToTarget() + pDiscovery.getDistance() > tRadius) continue;
						int tToken = tCluster.getToken();
						if(!pDiscovery.getTokens().contains(Integer.valueOf(tToken))) {
							if(tCluster instanceof NeighborCluster) {
								Logging.log(this, "Reporting " + tCluster + " to " + getPeerName().getDescr() + " because " + pDiscovery.getDistance() + " + " + ((NeighborCluster)tCluster).getClusterDistanceToTarget() + "=" + (pDiscovery.getDistance() + ((NeighborCluster)tCluster).getClusterDistanceToTarget()));
								Logging.log(this, "token list was " + pDiscovery.getTokens());
							}
							getPathTo(pDiscovery, tCluster);
							for(ICluster tNeighbor : tCluster.getNeighbors()) {
								ClusterName tFirstClusterName = new ClusterName(tCluster.getToken(), tCluster.getClusterID(), tCluster.getHierarchyLevel()); 
								ClusterName tSecondClusterName = new ClusterName(tNeighbor.getToken(), tNeighbor.getClusterID(), tNeighbor.getHierarchyLevel()); 
								pDiscovery.addNeighborRelation(tFirstClusterName, tSecondClusterName);
							}
						} else {
							/*
							 * Maybe print out additional stuff if required
							 */
						}
					}
				}
			}
		} else {
			if(pDiscovery.getDiscoveryEntries() != null) {
				HashMap<ClusterName, ClusterName> tToSetNegotiator = new HashMap<ClusterName, ClusterName>();
				for(DiscoveryEntry tEntry : pDiscovery.getDiscoveryEntries()) {
					tToSetNegotiator.put(handleDiscoveryEntry(tEntry), tEntry.getPredecessor());
				}
			}
		}
	}
	
	public ClusterName handleDiscoveryEntry(DiscoveryEntry pEntry) throws PropertyException
	{
		Logging.trace(this, "Handling " + pEntry);
		ICluster tNewCluster = getHRMController().getCluster(new ClusterName(pEntry.getToken(), pEntry.getClusterID(), pEntry.getLevel()));
		if(tNewCluster == null) {
			for(Cluster tCluster : getHRMController().getRoutingTargetClusters()) {
				if(tCluster.equals(new ClusterName(pEntry.getToken(), pEntry.getClusterID(), new HierarchyLevel(this, getPeer().getHierarchyLevel().getValue() - 1)))) {
					tNewCluster = tCluster;
					if(tNewCluster instanceof NeighborCluster && tNewCluster.getCoordinatorsAddress() == null && tNewCluster.getCoordinatorName() == null) {
						Logging.log(this, "Filling required information into " + tNewCluster);
						tNewCluster.setSuperiorCoordinatorCEP(null, null, pEntry.getCoordinatorName(), pEntry.getToken(), pEntry.getCoordinatorRoutingAddress());
						if(pEntry.isInterASCluster()){
							tNewCluster.setInterASCluster();
						}
					}
				}
			}
			if(tNewCluster == null) {
				/*
				 * Be aware of the fact that the new attached cluster has lower level
				 */
				tNewCluster = new NeighborCluster(pEntry.getClusterID(), pEntry.getCoordinatorName(), pEntry.getCoordinatorRoutingAddress(), pEntry.getToken(), pEntry.getLevel(), getHRMController());
				
				getPeer().getHRMController().setSourceIntermediateCluster(tNewCluster, getPeer().getHRMController().getSourceIntermediate(getPeer()));
				((NeighborCluster)tNewCluster).addAnnouncedCEP(this);
				tNewCluster.setToken(pEntry.getToken());
				tNewCluster.setPriority(pEntry.getPriority());
				getHRMController().addRoutableTarget(tNewCluster);
				if(pEntry.isInterASCluster()) {
					tNewCluster.setInterASCluster();
				}
				try {
					getHRMController().getHRS().mapFoGNameToL2Address(tNewCluster.getCoordinatorName(), tNewCluster.getCoordinatorsAddress());
				} catch (RemoteException tExc) {
					Logging.err(this, "Unable to register " + tNewCluster.getCoordinatorName(), tExc);
				}
				Logging.log(this, "Created " + tNewCluster);
			}
			
			((NeighborCluster)tNewCluster).addAnnouncedCEP(this);
//			((NeighborCluster)tNewCluster).setClusterHopsOnOpposite(pEntry.getClusterHops(), this);
		}
		if(pEntry.getRoutingVectors() != null) {
			for(RoutingServiceLinkVector tLink : pEntry.getRoutingVectors()) {
				getHRMController().getHRS().registerRoute(tLink.getSource(), tLink.getDestination(), tLink.getPath());
			}
		}
		return new ClusterName(tNewCluster.getToken(), tNewCluster.getClusterID(), tNewCluster.getHierarchyLevel());
	}
	
	public Route getRouteToPeer()
	{
		return getMultiplexer().getRouteToPeer(this);
	}
	
	private CoordinatorCEPMultiplexer getMultiplexer()
	{
		return getPeer().getMultiplexer();
	}

	/**
	 * Returns a descriptive string about this object 
	 */
	public String toString()
	{
		return getClass().getSimpleName() + "@" + getPeer().getClusterDescription() +  "(PeerPrio=" + mPeerPriority.getValue() + (getPeerName() != null ? ", Peer=" + getPeerHRMID() : "") + ")";
	}
}
