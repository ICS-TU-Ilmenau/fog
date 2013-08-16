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
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborClusterAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.AssignHRMID;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyPriorityUpdate;
import de.tuilmenau.ics.fog.packets.hierarchical.election.SignalingMessageBully;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.hierarchical.election.Elector;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMEntity;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.IElementDecorator;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class represents a clusters on a defined hierarchy level.
 * 
 */
public class Cluster implements ICluster, IElementDecorator, HRMEntity
{
	/**
	 * For using this class within (de-)serialization.
	 */
	private static final long serialVersionUID = -7486131336574637721L;

	/**
	 * This is the cluster counter, which allows for globally (related to a physical simulation machine) unique cluster IDs.
	 */
	private static long sNextClusterFreeID = 0;

	/**
	 * Stores the physical simulation machine specific multiplier, which is used to create unique cluster IDs even if multiple physical simulation machines are connected by FoGSiEm instances
	 * The value "-1" is important for initialization!
	 */
	private static long sClusterIDMachineMultiplier = -1;

	/**
	 * Stores the unique cluster ID
	 */
	private Long mClusterID;

	/**
	 * Stores the Bully priority of this node for this cluster.
	 * The value is also used inside the Elector of this cluster.
	 */
	private BullyPriority mBullyPriority = null;

	/**
	 * Stores the hierarchy level of this cluster.
	 */
	private HierarchyLevel mHierarchyLevel;

	/**
	 * Stores the elector which is responsible for coordinator elections for this cluster.
	 */
	private Elector mElector = null;
	
	/**
	 * Counter about how many times a coordinator was defined
	 */
	private int mCoordinatorUpdateCounter = 0;

	/**
	 * The HRM ID of this cluster.
	 */
	private HRMID mHRMID = null;

	/**
	 * Stores a descriptive string about the elected coordinator
	 */
	private String mCoordinatorDescription = null;
	
	private ComChannel mChannelToCoordinator = null;
	private BullyPriority mHighestPriority = null;
	private BullyPriority mCoordinatorPriority;
	private Name mCoordName;
	private Name mCoordAddress;
	private HRMController mHRMController;
	private LinkedList<ComChannel> mClusterMemberChannels;
	private LinkedList<NeighborClusterAnnounce> mReceivedAnnounces = null;
	private int mToken;

	/**
	 * Stores a reference to the local coordinator instance if the local router is also the coordinator for this cluster
	 */
	private Coordinator mCoordinator = null;
	
	private ComChannelMuxer mMux = null;

	
	/**
	 * This is the constructor of a cluster object. At first such a cluster is identified by its cluster
	 * ID and the hierarchical level. Later on - once a coordinator is found, it is additionally identified
	 * by a token the coordinator sends to all participants. In contrast to the cluster token the identity is used
	 * to filter potential participants that may be used for the election of a coordinator.
	 * 
	 * @param ptHRMController a reference to the HRMController instance
	 * @param pClusterID the cluster ID
	 * @param pHierarchyLevel the hierarchy level
	 */
	public Cluster(HRMController pHRMController, Long pClusterID, HierarchyLevel pHierarchyLevel)
	{
		// initialize the HRMID of the cluster to ".0.0.0"
		mHRMID = new HRMID(0);

		// the hierarchy level is defined from outside
		mHierarchyLevel = pHierarchyLevel;
		
		// update the reference to the HRMController application for internal use
		mHRMController = pHRMController;
		
		// set the ClusterID
		if (pClusterID < 0){
			// create an ID for the cluster
			mClusterID = createClusterID();
		}else{
			// use the ClusterID from outside
			mClusterID = pClusterID;
		}

		// create a new standard Bully priority
		mBullyPriority = BullyPriority.createForCluster(this);

		// creates new elector object, which is responsible for Bully based election processes
		mElector = new Elector(this);

		mClusterMemberChannels = new LinkedList<ComChannel>();
		mReceivedAnnounces = new LinkedList<NeighborClusterAnnounce>();

		for(ICluster tCluster : getHRMController().getRoutingTargets())
		{
			Logging.log(this, "Found already known neighbor: " + tCluster);
			if ((tCluster.getHierarchyLevel().equals(mHierarchyLevel)) && (tCluster != this))
			{
				if (!(tCluster instanceof Cluster)){
					Logging.err(this, "Routing target should be a cluster, but it is " + tCluster);
				}
				tCluster.addNeighborCluster(this);

				// increase Bully priority because of changed connectivity (topology depending)
				mBullyPriority.increaseConnectivity();
			}
		}

		
		mMux = new ComChannelMuxer(this, mHRMController);
		
		// register at HRMController's internal database
		getHRMController().registerCluster(this);

		Logging.log(this, "CREATED");
	}
	
	/**
	 * Determines the physical simulation machine specific ClusterID multiplier
	 * 
	 * @return the generated multiplier
	 */
	private long clusterIDMachineMultiplier()
	{
		if (sClusterIDMachineMultiplier < 0){
			String tHostName = getHRMController().getHostName();
			if (tHostName != null){
				sClusterIDMachineMultiplier = (tHostName.hashCode() % 10000) * 10000;
			}else{
				Logging.err(this, "Unable to determine the machine-specific ClusterID multiplier because host name couldn't be indentified");
			}
		}

		return sClusterIDMachineMultiplier;
	}

	/**
	 * Generates a new ClusterID
	 * 
	 * @return the ClusterID
	 */
	private long createClusterID()
	{
		// get the current unique ID counter
		long tResult = sNextClusterFreeID * clusterIDMachineMultiplier();

		// make sure the next ID isn't equal
		sNextClusterFreeID++;
		
		return tResult;
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
	 * Returns the reference to the node local HRMController instance 
	 */
	public HRMController getHRMController()
	{
		return mHRMController;
	}

	/**
	 * Returns a descriptive string about the elected coordinator 
	 * 
	 * @return the descriptive string
	 */
	public String getCoordinatorDescription()
	{
		return mCoordinatorDescription;
	}
	
	/**
	 * Returns a descriptive string about the cluster
	 * 
	 * @return the descriptive string
	 */
	public String getClusterDescription()
	{
		return toLocation();
		//getHRMController().getPhysicalNode() + ":" + mClusterID + "@" + mHierarchyLevel + "(" + mCoordSignature + ")";
	}

	/**
	 * Determines the coordinator of this cluster. It is "null" if the election was lost or hasn't finished yet. 
	 * 
	 * @return the cluster's coordinator
	 */
	public Coordinator getCoordinator()
	{
		return mCoordinator;
	}
	
	/**
	 * Determines if a coordinator is known.
	 * 
	 * @return true if the coordinator is elected and known, otherwise false
	 */
	private boolean hasLocalCoordinator()
	{
		return (mCoordinator != null);
	}
	
	/**
	 * Set the new coordinator, which was elected by the Elector instance.
	 * 
	 * @param pCoordinator the new coordinator
	 */
	public void setCoordinator(Coordinator pCoordinator)
	{
		Logging.log(this, "Updating the coordinator from " + mCoordinator + " to " + pCoordinator);
		
		// set the coordinator
		mCoordinator = pCoordinator;
		
		// update the descriptive string about the coordinator
		mCoordinatorDescription = mCoordinator.toLocation();
	}
	
	/**
	 * Handles packet type "AssignHRMID".
     * The function is called when an address update for the physical node (hierarchy level 0) was received.
	 * 
	 * @param pAssignHRMIDPacket the received packet with the new hierarchy level 0 address
	 */
	public void handleAssignHRMIDForPhysicalNode(AssignHRMID pAssignHRMIDPacket)
	{
		// extract the HRMID from the packet 
		HRMID tHRMID = pAssignHRMIDPacket.getHRMID();
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ADDRESSING)
			Logging.log(this, "Handling AssignHRMID with assigned HRMID " + tHRMID.toString());

		// we process such packets only on base hierarchy level, on higher hierarchy levels coordinators should be the only target for such packets
		if (getHierarchyLevel().isBaseLevel()){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ADDRESSING)
				Logging.log(this, "     ..setting assigned HRMID " + tHRMID.toString());
			
			// update the local HRMID
			setHRMID(this, tHRMID);
		}else{
			Logging.warn(this, "     ..ignoring AssignHRMID packet because we are at the higher hierachy level " + getHierarchyLevel().getValue());
		}

		// the local router has also the coordinator instance for this cluster?
		if (hasLocalCoordinator()){
			// we should automatically continue the address distribution?
			if (HRMConfig.Addressing.ASSIGN_AUTOMATICALLY){
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ADDRESSING)
					Logging.log(this, "     ..continuing the address distribution process via the coordinator " + getCoordinator());
				getCoordinator().signalAddressDistribution();				
			}			
		}else{
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ADDRESSING)
				Logging.log(this, "     ..stopping address propagation here because node " + getHRMController().getNodeGUIName() + " is only a cluster member");
		}
	}

	/**
	 * Returns the Bully priority of this node for this cluster
	 * 
	 * @return the Bully priority
	 */
	public BullyPriority getPriority()
	{
		if (mBullyPriority == null){
			mBullyPriority = new BullyPriority(this);
		}
			
		return mBullyPriority;
	}

	/**
	 * Sends a packet as broadcast to all cluster members
	 * 
	 * @param pPacket the packet which has to be broadcasted
	 */
	public void sendBroadcast(Serializable pPacket)
	{
		Logging.log(this, "Sending CLUSTER BROADCAST " + pPacket);
		
		for(ComChannel tClusterMember : getClusterMembers()) {			
			Logging.log(this, "       ..to " + tClusterMember);
			
			// send the packet to one of the possible cluster members
			tClusterMember.sendPacket(pPacket);
		}
	}

	/**
	 * Assign new HRMID for being addressable as cluster member.
	 *  
	 * @param pCaller the caller who assigns the new HRMID
	 * @param pHRMID the new HRMID
	 */
	public void setHRMID(Object pCaller, HRMID pHRMID)
	{
		Logging.log(this, "ASSINGED HRMID=" + pHRMID + " (caller=" + pCaller + ")");

		// update the HRMID
		mHRMID = pHRMID.clone();
		
		// inform HRM controller about the address change
		getHRMController().updateClusterAddress(this);
	}

	/**
	 * Returns the HRMID under which this node is addressable for this cluster
	 * 
	 * @return the HRMID
	 */
	@Override
	public HRMID getHRMID() {
		return mHRMID;
	}
	

	/**
	 * Returns the full ClusterID (including the machine specific multiplier)
	 * 
	 *  @return the full ClusterID
	 */
	public Long getClusterID()
	{
		return mClusterID;
	}
	
	/**
	 * Returns the hierarchy level of this cluster
	 * 
	 * @return the hierarchy level
	 */
	public HierarchyLevel getHierarchyLevel()
	{
		return mHierarchyLevel;
	}
	
	/**
	 * Returns the machine-local ClusterID (excluding the machine specific multiplier)
	 * 
	 * @return the machine-local ClusterID
	 */
	public long getGUIClusterID()
	{
		return mClusterID / clusterIDMachineMultiplier();
	}
	
	/**
	 * Handles a Bully related signaling message from an external cluster member
	 * 
	 * @param pBullyMessage the Bully message
	 * @param pSourceClusterMember the channel to the message source
	 */
	public void handleSignalingMessageBully(SignalingMessageBully pBullyMessage, ComChannel pSourceClusterMember)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
			Logging.log(this, "RECEIVED BULLY MESSAGE FROM " + pSourceClusterMember);

		getElector().handleMessageFromClusterMember(pBullyMessage, pSourceClusterMember);
	}
	
	/**
	 * Handles a general signaling message from an external cluster member
	 * 
	 * @param pMessage the signaling message
	 * @param pCoordinatorCEPChannel the channel to the message source
	 */
	public void handleMessageFromClusterMember(Serializable pMessage, ComChannel pSourceClusterMember)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING)
			Logging.log(this, "RECEIVED SIGNALING MESSAGE FROM " + pSourceClusterMember);

		/**
		 * Bully signaling message
		 */
		if (pMessage instanceof SignalingMessageBully) {
			// cast to a Bully signaling message
			SignalingMessageBully tBullyMessage = (SignalingMessageBully)pMessage;
		
			// process Bully message
			handleSignalingMessageBully(tBullyMessage, pSourceClusterMember);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	public void handleBullyAnnounce(BullyAnnounce pBullyAnnounce, ComChannel pCEP)
	{
		// update the description about the elected coordinator
		mCoordinatorDescription = pBullyAnnounce.getCoordinatorDescription();
				
		setSuperiorCoordinatorCEP(pCEP, pBullyAnnounce.getSenderName(), pBullyAnnounce.getToken(), pCEP.getPeerL2Address());
		getHRMController().setClusterWithCoordinator(getHierarchyLevel(), this);
	}
	
	public void setSuperiorCoordinatorCEP(ComChannel pCoordinatorChannel, Name pCoordName, int pCoordToken, HRMName pAddress)
	{
		setToken(pCoordToken);
		
		Logging.log(this, "Setting " + (++mCoordinatorUpdateCounter) + " time a new coordinator: " + pCoordName + "/" + pCoordinatorChannel + " with routing address " + pAddress);
		mChannelToCoordinator = pCoordinatorChannel;
		mCoordName = pCoordName;
		if(mChannelToCoordinator == null) {
			synchronized(this) {
				mCoordAddress = getHRMController().getNode().getRoutingService().getNameFor(getHRMController().getNode().getCentralFN());
				notifyAll();
			}
			setCoordinatorPriority(getPriority());
		} else {
			synchronized(this) {
				mCoordAddress = pAddress;
				notifyAll();
			}
			setCoordinatorPriority(pCoordinatorChannel.getPeerPriority());
			try {
				getHRMController().getHRS().mapFoGNameToL2Address(pCoordName, pAddress);
			} catch (RemoteException tExc) {
				Logging.err(this, "Unable to register " + pCoordName, tExc);
			}
			
			if(pCoordinatorChannel.getRouteToPeer() != null && !pCoordinatorChannel.getRouteToPeer().isEmpty()) {
				if(pAddress instanceof L2Address) {
					getHRMController().getHRS().registerNode((L2Address) pAddress, false);
				}
				
				getHRMController().getHRS().registerRoute(pCoordinatorChannel.getSourceName(), pCoordinatorChannel.getPeerL2Address(), pCoordinatorChannel.getRouteToPeer());
			}
		}
		Logging.log(this, "This cluster has the following neighbors: " + getNeighbors());
		for(ICluster tCluster : getNeighbors()) {
			if(tCluster instanceof Cluster) {
				Logging.log(this, "CLUSTER-CEP - found already known neighbor cluster: " + tCluster);

				Logging.log(this, "Preparing neighbor zone announcement");
				NeighborClusterAnnounce tAnnounce = new NeighborClusterAnnounce(pCoordName, mHierarchyLevel, pAddress, getToken(), mClusterID);
				tAnnounce.setCoordinatorsPriority(mBullyPriority); //TODO : ???
				if(pCoordinatorChannel != null) {
					tAnnounce.addRoutingVector(new RoutingServiceLinkVector(pCoordinatorChannel.getRouteToPeer(), pCoordinatorChannel.getSourceName(), pCoordinatorChannel.getPeerL2Address()));
				}
				((Cluster)tCluster).announceNeighborCoord(tAnnounce, pCoordinatorChannel);
			}
		}
		if(mReceivedAnnounces.isEmpty()) {
			Logging.log(this, "No announces came in while no coordinator was set");
		} else {
			Logging.log(this, "sending old announces");
			while(!mReceivedAnnounces.isEmpty()) {
				if(mChannelToCoordinator != null)
				{
					// OK, we have to notify the other node via socket communication, so this cluster has to be at least one hop away
					mChannelToCoordinator.sendPacket(mReceivedAnnounces.removeFirst());
				} else {
					/*
					 * in this case this announcement came from a neighbor intermediate cluster
					 */
					handleNeighborAnnouncement(mReceivedAnnounces.removeFirst(), pCoordinatorChannel);
				}
			}
		}
	}
	
	private ICluster addAnnouncedCluster(NeighborClusterAnnounce pAnnounce, ComChannel pCEP)
	{
		if(pAnnounce.getRoutingVectors() != null) {
			for(RoutingServiceLinkVector tVector : pAnnounce.getRoutingVectors()) {
				getHRMController().getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
			}
		}
		ICluster tCluster = getHRMController().getCluster(new ClusterName(pAnnounce.getToken(), pAnnounce.getClusterID(), pAnnounce.getLevel()));
		if(tCluster == null) {
			tCluster = new NeighborCluster(pAnnounce.getClusterID(), pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress(), pAnnounce.getToken(), mHierarchyLevel, getHRMController());
			getHRMController().setSourceIntermediateCluster(tCluster, this);
			((NeighborCluster)tCluster).addAnnouncedCEP(pCEP);
			((NeighborCluster)tCluster).setSourceIntermediate(this);
			tCluster.setPriority(pAnnounce.getCoordinatorsPriority());
			tCluster.setToken(pAnnounce.getToken());
			
			try {
				getHRMController().getHRS().mapFoGNameToL2Address(tCluster.getCoordinatorName(), tCluster.getCoordinatorsAddress());
			} catch (RemoteException tExc) {
				Logging.err(this, "Unable to fulfill requirements", tExc);
			}
			
		} else {
			Logging.log(this, "Cluster announced by " + pAnnounce + " is an intermediate neighbor ");
		}
		/*if(tCluster instanceof AttachedCluster) {
			((AttachedCluster)tCluster).setNegotiatingHost(pAnnounce.getAnnouncersAddress());
		}*/
		
		/*
		 * function checks whether neighbor relation was established earlier
		 */
		addNeighborCluster(tCluster);

		if(pAnnounce.getCoordinatorName() != null) {
//			Description tDescription = new Description();
			try {
				getHRMController().getHRS().mapFoGNameToL2Address(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
			} catch (RemoteException tExc) {
				Logging.warn(this, "Unable to register " + pAnnounce.getCoordinatorName(), tExc);
			}
		}
		return tCluster;
	}
	
	public void handleNeighborAnnouncement(NeighborClusterAnnounce	pAnnounce, ComChannel pCEP)
	{
		if(!pAnnounce.getCoordinatorName().equals(getHRMController().getNodeName())) {
			Logging.log(this, "Received announcement of foreign cluster");
		}
		
		if(getHierarchyLevel().isBaseLevel()) {
			if(pCEP != null) {
				if(!pCEP.getSourceName().equals(pCEP.getPeerL2Address()) && pCEP.getRouteToPeer() != null) {
					RoutingServiceLinkVector tLink = new RoutingServiceLinkVector(pCEP.getRouteToPeer().clone(),  pCEP.getSourceName(), pCEP.getPeerL2Address());
					pAnnounce.addRoutingVector(tLink);
					Logging.log(this, "Added routing vector " + tLink);
				}
				pAnnounce.isForeignAnnouncement();
			}
		} else {
			if(getHRMController().getClusterWithCoordinatorOnLevel(mHierarchyLevel.getValue()) == null) {
				/*
				 * no coordinator set -> find cluster that is neighbor of the predecessor, so routes are correct
				 */
				for(Coordinator tManager : getHRMController().getCoordinator(mHierarchyLevel)) {
					if(tManager.getNeighbors().contains(pAnnounce.getNegotiatorIdentification())) {
						tManager.storeAnnouncement(pAnnounce);
					}
				}
			} else {
				/*
				 * coordinator set -> find cluster that is neighbor of the predecessor, so routes are correct
				 */
				for(Coordinator tManager : getHRMController().getCoordinator(mHierarchyLevel)) {
					if(tManager.getNeighbors().contains(pAnnounce.getNegotiatorIdentification())) {
						if(tManager.getSuperiorCoordinatorCEP() != null) {
							tManager.getSuperiorCoordinatorCEP().sendPacket(pAnnounce);
						}
					}
				}
			}
			
			
			if(pAnnounce.getCoveringClusterEntry() != null) {
//				Cluster tForwardingCluster = null;
				
				if(pAnnounce.isRejected()) {
//					Cluster tMultiplex = this;
//					tForwardingCluster = (Cluster) ((Cluster) getCoordinator().getLastUncovered(tMultiplex, pCEP.getRemoteCluster()) == null ? pCEP.getRemoteCluster() : getCoordinator().getLastUncovered(tMultiplex, pCEP.getRemoteCluster())) ;
					//pAnnounce.setAnnouncer( (tForwardingCluster.getCoordinatorsAddress() != null ? tForwardingCluster.getCoordinatorsAddress() : null ));
					Logging.log(this, "Removing " + this + " as participating CEP from " + this);
					getClusterMembers().remove(this);
				}
				try {
					addNeighborCluster(getHRMController().getCluster(pCEP.handleDiscoveryEntry(pAnnounce.getCoveringClusterEntry())));
				} catch (PropertyException tExc) {
					Logging.log(this, "Unable to fulfill requirements");
				}
				Logging.log(this, "new negotiating cluster will be " + getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
			} else if(pCEP != null) {
				Logging.log(this, "new negotiating cluster will be " + getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
			}
		}
	}
	
	public ComChannel getSuperiorCoordinatorCEP()
	{
		return mChannelToCoordinator;
	}
	
	public void addNeighborCluster(ICluster pNeighbor)
	{
		LinkedList<ICluster> tNeighbors = getNeighbors(); 
		if(!tNeighbors.contains(pNeighbor))
		{
			if(pNeighbor instanceof Cluster) {
				RoutableClusterGraphLink tLink = new RoutableClusterGraphLink(RoutableClusterGraphLink.LinkType.PHYSICAL_LINK);
				getHRMController().getRoutableClusterGraph().storeLink(pNeighbor, this, tLink);
			} else {
				RoutableClusterGraphLink tLink = new RoutableClusterGraphLink(RoutableClusterGraphLink.LinkType.LOGICAL_LINK);
				getHRMController().getRoutableClusterGraph().storeLink(pNeighbor, this, tLink);
			}
			if(pNeighbor instanceof Cluster) {
				
				Logging.log(this, "CLUSTER - adding neighbor cluster: " + pNeighbor);

				// increase Bully priority because of changed connectivity (topology depending) 
				mBullyPriority.increaseConnectivity();
				
				Logging.log(this, "Informing " + getClusterMembers() + " about change in priority and initiating new election");
				
				sendClusterBroadcast(new BullyPriorityUpdate(getHRMController().getNodeName(), mBullyPriority), null);
				
				Logging.log(this, "Informed other clients about change of priority - it is now " + mBullyPriority.getValue());
			}
		}
	}
	
	private void announceNeighborCoord(NeighborClusterAnnounce pAnnouncement, ComChannel pCEP)
	{
		Logging.log(this, "Handling " + pAnnouncement);
		if(mCoordName != null)
		{
			if(getHRMController().getNodeName().equals(mCoordName))
			{
				handleNeighborAnnouncement(pAnnouncement, pCEP);
			} else {
				mChannelToCoordinator.sendPacket(pAnnouncement);
			}
		} else {
			mReceivedAnnounces.add(pAnnouncement);
		}
	}
	
	public BullyPriority getHighestPriority()
	{
		if (mHighestPriority == null){
			mHighestPriority = new BullyPriority(this);
		}
		
		return mHighestPriority;
	}
	
	public void setParticipatingCEPs(LinkedList<ComChannel> pCEPs)
	{
		Logging.log(this, "Setting participating CEPs to " + pCEPs);
		mClusterMemberChannels = pCEPs;
	}
	
	public void registerComChannel(ComChannel pParticipatingCEP)
	{
		if(!mClusterMemberChannels.contains(pParticipatingCEP)) {
			mClusterMemberChannels.add(pParticipatingCEP);
			Logging.info(this, "Added " + pParticipatingCEP + " to participating CEPs");
			if(mClusterMemberChannels.size() > 1) {
				Logging.info(this, "Adding second participating CEP " + pParticipatingCEP);
//				StackTraceElement[] tStackTrace = Thread.currentThread().getStackTrace();
//				for (StackTraceElement tElement : tStackTrace) {
//					getCoordinator().getLogger().log(tElement.toString());
//				}
			}
		}
	}

	public LinkedList<ComChannel> getClusterMembers()
	{
		return mClusterMemberChannels;
	}
	

	public void setPriority(BullyPriority pPriority)
	{
		BullyPriority tBullyPriority = mBullyPriority;
		mBullyPriority = pPriority;
		Logging.log(this, "######## Setting Bully priority for cluster " + toString() + " from " + tBullyPriority.getValue() + " to " + mBullyPriority.getValue());
	}
	
	public BullyPriority getCoordinatorPriority()
	{
		return mCoordinatorPriority;
	}
	
	public void setCoordinatorPriority(BullyPriority pCoordinatorPriority)
	{
		mCoordinatorPriority = pCoordinatorPriority;
	}
	
	public LinkedList<ICluster> getNeighbors()
	{
		LinkedList<ICluster> tList = new LinkedList<ICluster>();
		for(HRMGraphNodeName tNode : getHRMController().getRoutableClusterGraph().getNeighbors(this)) {
			if(tNode instanceof ICluster) {
				tList.add((ICluster)tNode);
			}
		}
		return tList;
	}
	
	public int getToken()
	{
		return mToken;
	}
	
	public L2Address getCoordinatorsAddress()
	{
		return (L2Address) mCoordAddress;
	}
	
	public void setCoordinatorName(Name pCoordName)
	{
		mCoordName = pCoordName;
	}

	public void sendClusterBroadcast(Serializable pData, LinkedList<ComChannel> pAlreadyInformed)
	{
		Logging.log(this, "Sending CLUSTER BROADCAST " + pData);
		
		if(pData instanceof BullyPriorityUpdate)
		{
			Logging.log(this, "Will send priority update to" + mClusterMemberChannels);
		}
		LinkedList<ComChannel> tInformedCEPs = null;
		if(pAlreadyInformed != null) {
			tInformedCEPs= pAlreadyInformed;
		} else {
			tInformedCEPs = new LinkedList<ComChannel>(); 
		}
		try {
			for(ComChannel tClusterMember : mClusterMemberChannels)
			{
				if(!tInformedCEPs.contains(tClusterMember))
				{
					Logging.log(this, "   BROADCAST TO " + tClusterMember);
					tClusterMember.sendPacket(pData);
					tInformedCEPs.add(tClusterMember);
				}
			}
		} catch (ConcurrentModificationException tExc) {
			Logging.warn(this, "change in cluster CEP number occured, sending message to new peers", tExc);
			sendClusterBroadcast(pData, tInformedCEPs);
		}
	}
	
	public Name getCoordinatorName()
	{
		return mCoordName;
	}
	
	@Override
	public void setToken(int pToken) {
		Logging.log(this, "Updating token from " + mToken + " to " + pToken);
		mToken = pToken;
	}
	
	@Override
	public void setHighestPriority(BullyPriority pHighestPriority) {
		mHighestPriority = pHighestPriority;
	}
	
	@Override
	public Namespace getNamespace() {
		return new Namespace("cluster");
	}

	@Override
	public int getSerialisedSize() {
		return 0;
	}
	

	@Override
	public boolean equals(Object pObj)
	{
		if(pObj instanceof Coordinator) {
			return false;
		}
		if(pObj instanceof Cluster) {
			ICluster tCluster = (ICluster) pObj;
			if (tCluster.getClusterID().equals(getClusterID()) && (tCluster.getToken() == getToken()) && (tCluster.getHierarchyLevel() == getHierarchyLevel())) {
				return true;
			} else if(tCluster.getClusterID().equals(getClusterID()) && tCluster.getHierarchyLevel() == getHierarchyLevel()) {
				return false;
			} else if (tCluster.getClusterID().equals(getClusterID())) {
				return false;
			}
		}
		if(pObj instanceof ClusterName) {
			ClusterName tClusterName = (ClusterName) pObj;
			if (tClusterName.getClusterID().equals(getClusterID()) && (tClusterName.getToken() == getToken()) && (tClusterName.getHierarchyLevel() == getHierarchyLevel())) {
				return true;
			} else if(tClusterName.getClusterID().equals(getClusterID()) && tClusterName.getHierarchyLevel() == getHierarchyLevel()) {
				return false;
			} else if (tClusterName.getClusterID().equals(getClusterID())) {
				return false;
			}
		}
		return false;
	}	

	@Override
	public synchronized ComChannelMuxer getMultiplexer()
	{
		return mMux;
	}

	/**
	 * This method is specific for the handling of RouteRequests.
	 * 
	 * @param pCluster
	 * @return
	 */
//	public CoordinatorCEPChannel getCEPOfCluster(ICluster pCluster)
//	{
//		for(CoordinatorCEPChannel tCEP : getParticipatingCEPs()) {
//			if(tCEP.getRemoteClusterName().equals(pCluster)) {
//				return tCEP;
//			}
//		}
//		return null;
//	}
	
	@Override
	public Object getDecorationParameter()
	{
		return IElementDecorator.Color.GREEN;
	}

	@Override
	public void setDecorationParameter(Object pDecoration)
	{
		// not used, but have to be implemented for implementing interface IElementDecorator
	}

	@Override
	public Object getDecorationValue()
	{
		return Float.valueOf(0.8f);
	}

	@Override
	public void setDecorationValue(Object tLabal)
	{
		// not used, but have to be implemented for implementing interface IElementDecorator
	}

	public int hashCode()
	{
		return mClusterID.intValue();
	}

	
	
	
	
	
	/**
	 * Returns a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	@SuppressWarnings("unused")
	public String toString()
	{
		if(mHRMID != null && HRMConfig.Debugging.PRINT_HRMIDS_AS_CLUSTER_IDS) {
			return mHRMID.toString();
		} else {
			return toLocation() + "(" + idToString() + ")";

		}
	}

	/**
	 * Returns a location description about this instance
	 */
	@Override
	public String toLocation()
	{
		String tResult = getClass().getSimpleName() + getGUIClusterID() + "@" + getHRMController().getNodeGUIName() + "@" + getHierarchyLevel().getValue();
		
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
			return "ID=" + getClusterID() + ", Tok=" + mToken +  ", NodePrio=" + getPriority().getValue();
		}else{
			return "HRMID=" + getHRMID().toString();
		}
	}
}
