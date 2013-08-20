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
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborClusterAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.AssignHRMID;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.election.SignalingMessageBully;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.Localization;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.IElementDecorator;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * A control entity can be either a cluster or a coordinator instance.
 * This class is used to concentrate common function of clusters and coordinators
 */
public abstract class ControlEntity implements AbstractRoutingGraphNode, Localization, IElementDecorator
{
	private static final long serialVersionUID = 6770007191316056223L;

	/**
	 * Stores the hierarchy level of this cluster.
	 */
	private HierarchyLevel mHierarchyLevel = null;
	
	/**
	 * Stores the Bully priority of this node for this cluster.
	 * The value is also used inside the Elector of this cluster.
	 */
	private BullyPriority mBullyPriority = null;

	/**
	 * The HRM ID of this cluster.
	 */
	private HRMID mHRMID = null;

	/**
	 * Stores a reference to the parent HRMController.
	 */
	private HRMController mHRMController = null;

	/**
	 * Stores the registered communication channels
	 */
	private LinkedList<ComChannel> mComChannels = new LinkedList<ComChannel>();

	/**
	 * Stores the communication channel to the superior coordinator.
	 * For a base hierarchy level cluster, this is a level 0 coordinator.
	 * For a level n coordinator, this is a level n+1 coordinator. 
	 */
	private ComChannel mSuperiorCoordinatorComChannel = null;

	/**
	 * Stores the L2Address of the superior coordinator.
	 */
	private L2Address mSuperiorCoordinatorL2Address = null;
	
	/**
	 * Counter about how many times a superior coordinator was defined
	 */
	private int mSuperiorCoordinatorUpdateCounter = 0;

	/**
	 * Constructor
	 */
	public ControlEntity(HRMController pHRMController, HierarchyLevel pHierarchyLevel)
	{
		// initialize the HRMID of the cluster to ".0.0.0"
		mHRMID = new HRMID(0);

		// the hierarchy level is defined from outside
		mHierarchyLevel = pHierarchyLevel;
		
		// update the reference to the HRMController application for internal use
		mHRMController = pHRMController;

		// create a new standard Bully priority
		mBullyPriority = BullyPriority.createForControlEntity(this);
	}

	/** 
	 * Returns the reference to the node local HRMController instance 
	 */
	public HRMController getHRMController()
	{
		return mHRMController;
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
	 * Sets a new Bully priority
	 * 
	 * @param pPriority the new Bully priority
	 */
	public void setPriority(BullyPriority pPriority)
	{
		// store the old one
		BullyPriority tBullyPriority = mBullyPriority;
		
		// update to the new one
		mBullyPriority = pPriority;
		
		Logging.log(this, "ASSIGNED BULLY PRIORITY for cluster " + toString() + " updated from " + tBullyPriority.getValue() + " to " + mBullyPriority.getValue());
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
	 * Assign new HRMID for being addressable.
	 *  
	 * @param pCaller the caller who assigns the new HRMID
	 * @param pHRMID the new HRMID
	 */
	public void setHRMID(Object pCaller, HRMID pHRMID)
	{
		Logging.log(this, "ASSINGED HRMID=" + pHRMID + " (caller=" + pCaller + ")");

		// update the HRMID
		if (pHRMID != null){
			mHRMID = pHRMID.clone();
		}else{
			mHRMID = null;
		}
		
		if (this instanceof Cluster){
			Cluster tCluster = (Cluster)this;

			// inform HRM controller about the address change
			getHRMController().updateClusterAddress(tCluster);

			return;
		}
		if (this instanceof ClusterProxy){
			ClusterProxy tClusterProxy = (ClusterProxy)this;

			// inform HRM controller about the address change
			//TODO: getHRMController().updateClusterAddress(tClusterProxy);
			
			return;
		}
		if (this instanceof Coordinator){
			Coordinator tCoordinator = (Coordinator)this;

			// inform HRM controller about the address change
			getHRMController().updateCoordinatorAddress(tCoordinator);

			return;
		}
	}

	/**
	 * Returns the HRMID under which this node is addressable for this cluster
	 * 
	 * @return the HRMID
	 */
	public HRMID getHRMID() {
		return mHRMID;
	}
	
	/**
	 * Returns all register communication channels
	 * 
	 * @return the communication channels
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<ComChannel> getComChannels()
	{
		LinkedList<ComChannel> tResult = null;
			
		synchronized (mComChannels) {
			tResult = (LinkedList<ComChannel>) mComChannels.clone();
		}
		
		return tResult;
	}

	/**
	 * Registers a communication channel to the internal database
	 * 
	 * @param pComChan the communication channel
	 */
	public void registerComChannel(ComChannel pComChan)
	{
		synchronized (mComChannels) {
			if(!mComChannels.contains(pComChan)) {
				Logging.log(this, "Registering communication channel " + pComChan + ", " + mComChannels.size() + " communication channels already registered");

				// add the channel to the database
				mComChannels.add(pComChan);
			}else{
				Logging.err(this, "Communication channel " + pComChan + " is alredy known");
			}			
		}
	}

	/**
	 * Registers a neighbor of this control entity within the ARG of the HRMController instance
	 *  
	 * @param pNeighbor the neighbor, which should be registered
	 */
	public void registerNeighbor(ControlEntity pNeighbor)
	{
		Logging.log(this, "Registering neighbor: " + pNeighbor);

		AbstractRoutingGraphLink.LinkType tLinkType = AbstractRoutingGraphLink.LinkType.REMOTE_LINK;

		boolean tRegisterNeighbor = false;
		if (this instanceof Cluster){
			// increase Bully priority because of changed connectivity (topology depending) 
			getPriority().increaseConnectivity();
			
			// inform all cluster members about the Bully priority change
			//TODO: sendClusterBroadcast(new BullyPriorityUpdate(getHRMController().getNodeName(), getPriority()));
	
			tLinkType = AbstractRoutingGraphLink.LinkType.LOCAL_LINK;
			
			tRegisterNeighbor = true;
		}

		if (this instanceof ClusterProxy){
			tRegisterNeighbor = true;
		}

		/**
		 * Register a link to the neighbor and tell the neighbor about it 
		 */
		if (tRegisterNeighbor){
			LinkedList<ControlEntity> tNeighbors = getNeighborsARG(); 
			if(!tNeighbors.contains(pNeighbor))
			{
				AbstractRoutingGraphLink tLink = new AbstractRoutingGraphLink(tLinkType);
				getHRMController().registerLinkARG(pNeighbor, this, tLink);
	
				// backward call
				pNeighbor.registerNeighbor(this);
			}else{
				Logging.log(this, "Neighbor " + pNeighbor + " is already known");
			}
		}else{
			Logging.warn(this, "registerNeighbor() ignores registration request for neighbor: " + pNeighbor);
		}
	}

	/**
	 * Determines all registered neighbors for this control entity, which can be found withing the ARG of the HRMController instance
	 * 
	 * @return the found neighbors of the ARG
	 */
	public LinkedList<ControlEntity> getNeighborsARG()
	{
		LinkedList<ControlEntity> tResult = new LinkedList<ControlEntity>();
		
		for(AbstractRoutingGraphNode tNode : getHRMController().getNeighborsARG(this)) {
			if (tNode instanceof ControlEntity){
				tResult.add((ControlEntity)tNode);
			}else{
				Logging.warn(this, "getNeighborsARG() ignores ARG neighbor: " + tNode);
			}
		}
		
		return tResult;
	}

	public void setSuperiorCoordinator(ComChannel pCoordinatorComChannel, Name pCoordinatorName, int pCoordToken, L2Address pCoordinatorL2Address)
	{
		Logging.log(this, "Setting new superior coordinator (update " + (++mSuperiorCoordinatorUpdateCounter) + "): " + pCoordinatorName + "/" + pCoordinatorComChannel + " with L2Address " + pCoordinatorL2Address);

		// store the communication channel to the superior coordinator
		setSuperiorCoordinatorComChannel(pCoordinatorComChannel);
		
		// store the L2Address of the superior coordinator
		setSuperiorCoordinatorL2Address(pCoordinatorL2Address);
	}

	/**
	 * Sets the communication channel to the superior coordinator.
	 * For a base hierarchy level cluster, this is a level 0 coordinator.
	 * For a level n coordinator, this is a level n+1 coordinator.
	 *  
	 * @param pComChannel the new communication channel
	 */
	public void setSuperiorCoordinatorComChannel(ComChannel pComChannel)
	{
		mSuperiorCoordinatorComChannel = pComChannel;
	}
	
	/**
	 * Returns a reference to the communication channel towards the superior coordinator.
	 * 
	 * @return the communication channel
	 */
	public ComChannel superiorCoordinatorComChannel()
	{
		return mSuperiorCoordinatorComChannel;
	}
	
	/**
	 * Sets the L2Address of the superior coordinator.
	 *  
	 * @param pAddr the new L2Address
	 */
	public void setSuperiorCoordinatorL2Address(L2Address pAddr)
	{
		mSuperiorCoordinatorL2Address = pAddr;
	}
	
	/**
	 * Returns the L2Address of the superior coordinator.
	 * 
	 * @return the L2Address
	 */
	public L2Address superiorCoordinatorL2Address()
	{
		return mSuperiorCoordinatorL2Address;
	}

	public void handleBullyAnnounce(BullyAnnounce pBullyAnnounce, ComChannel pComChannel)
	{
		//TODO: remove this
	}
	
	public void handleNeighborAnnouncement(NeighborClusterAnnounce pNeighborClusterAnnounce, ComChannel pComChannel)
	{
		//TODO: remove this
	}

	/**
	 * Handles a Bully related signaling message from an external cluster member
	 * 
	 * @param pBullyMessage the Bully message
	 * @param pSourceClusterMember the channel to the message source
	 */
	private void handleSignalingMessageBully(SignalingMessageBully pBullyMessage, ComChannel pSourceClusterMember)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
			Logging.log(this, "RECEIVED BULLY MESSAGE FROM " + pSourceClusterMember);

		if (this instanceof Cluster){
			Cluster tCluster = (Cluster)this;
			
			tCluster.getElector().handleSignalingMessageBully(pBullyMessage, pSourceClusterMember);
			
			return;
		}
		if (this instanceof ClusterProxy){
			//nothing
		}
		if (this instanceof Coordinator){
			Coordinator tCoordinator = (Coordinator)this;
			
			tCoordinator.getCluster().getElector().handleSignalingMessageBully(pBullyMessage, pSourceClusterMember);
			
			return;
		}
	}
	
	/**
	 * Handles packet type "AssignHRMID".
     * The function is called when an address update for the physical node (hierarchy level 0) was received.
	 * 
	 * @param pAssignHRMIDPacket the received packet with the new hierarchy level 0 address
	 */
	public void handleAssignHRMID(AssignHRMID pAssignHRMIDPacket)
	{
		// extract the HRMID from the packet 
		HRMID tHRMID = pAssignHRMIDPacket.getHRMID();
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ADDRESSING)
			Logging.log(this, "Handling AssignHRMID with assigned HRMID " + tHRMID.toString());

		/**
		 * Store the new HRMID
		 */
		// we process such packets for cluster only on base hierarchy level and on all hierarchy level for coordinators
		if ((getHierarchyLevel().isBaseLevel()) || (this instanceof Coordinator)){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ADDRESSING)
				Logging.log(this, "     ..setting assigned HRMID " + tHRMID.toString());
			
			// update the local HRMID
			setHRMID(this, tHRMID);
		}else{
			Logging.warn(this, "     ..ignoring AssignHRMID packet " + pAssignHRMIDPacket + " at hierachy level " + getHierarchyLevel().getValue());
		}

		/**
		 * Get the coordinator
		 */
		Coordinator tCoordinator = null;
		if (this instanceof Cluster){
			Cluster tCluster = (Cluster)this;
			if (tCluster.hasLocalCoordinator()){
				tCoordinator = tCluster.getCoordinator();
			}
		}
		if (this instanceof Coordinator){
			tCoordinator = (Coordinator)this;
		}
		
		/**
		 * Automatic address distribution via the coordinator
		 */
		// the local router has also the coordinator instance for this cluster?
		if (tCoordinator != null){
			// we should automatically continue the address distribution?
			if (HRMConfig.Addressing.ASSIGN_AUTOMATICALLY){
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ADDRESSING)
					Logging.log(this, "     ..continuing the address distribution process via the coordinator " + tCoordinator);
				tCoordinator.signalAddressDistribution();				
			}			
		}else{
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ADDRESSING)
				Logging.log(this, "     ..stopping address propagation here because node " + getHRMController().getNodeGUIName() + " is only a cluster member");
		}
	}

	/**
	 * Handles a general signaling message from an external cluster member
	 * 
	 * @param pMessage the signaling message
	 * @param pCoordinatorCEPChannel the channel to the message source
	 */
	public void handlePacket(Serializable pMessage, ComChannel pComChannel)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING)
			Logging.log(this, "RECEIVED SIGNALING MESSAGE FROM " + pComChannel);

		/**
		 * Bully signaling message
		 */
		if (pMessage instanceof SignalingMessageBully) {
			// cast to a Bully signaling message
			SignalingMessageBully tBullyMessage = (SignalingMessageBully)pMessage;
		
			// process Bully message
			handleSignalingMessageBully(tBullyMessage, pComChannel);
		}
	}

	/**
	 * Returns if both objects address the same cluster/coordinator
	 * 
	 * @return true or false
	 */
	@Override
	public boolean equals(Object pObj)
	{
		if (((this instanceof Cluster) && (pObj instanceof Coordinator)) ||
			((this instanceof Coordinator) && (pObj instanceof Cluster))){
			return false;
		}
		
		if (this instanceof ICluster)
		{
			ICluster tThisICluster = (ICluster)this;
			
			if(pObj instanceof ICluster) {
				ICluster tICluster = (ICluster) pObj;
				
				//Logging.log(this, "EQUALS COMPARING with " + pObj + ": " + tICluster.getClusterID() + "<=>" + tThisICluster.getClusterID() + ", " + tICluster.getToken() + "<=>" + tThisICluster.getToken() + ", " + tICluster.getHierarchyLevel().getValue() + "<=>" + getHierarchyLevel().getValue());

				if (tICluster.getClusterID().equals(tThisICluster.getClusterID()) && (tICluster.getToken() == tThisICluster.getToken()) && (tICluster.getHierarchyLevel().equals(getHierarchyLevel()))) {
					return true;
				}
			}
			
			if (pObj instanceof Long){
				Long tOtherClusterID = (Long)pObj;

				Logging.log(this, "EQUALS CLUSTER_ID COMPARING with " + pObj + ": " + tOtherClusterID + "<=>" + tThisICluster.getClusterID());

				if (tOtherClusterID.equals(tThisICluster.getClusterID())) {
					return true;
				}
			}
		}
		return false;
	}	

	/**
	 * Defines the decoration color for the ARG viewer
	 */
	@Override
	public Object getDecorationParameter()
	{
		if (this instanceof Coordinator){
			return IElementDecorator.Color.GREEN;
		}
		if (this instanceof Cluster){
			return IElementDecorator.Color.RED;
		}
		if (this instanceof ClusterProxy){
			return IElementDecorator.Color.BLUE;
		}
		
		return null;
	}

	@Override
	public void setDecorationParameter(Object pDecoration)
	{
		// not used, but have to be implemented for implementing interface IElementDecorator
	}

	/**
	 * Defines the decoration color intensity for the ARG viewer
	 */
	@Override
	public Object getDecorationValue()
	{
		Float tResult = Float.valueOf(1.0f - 0.5f * (getHierarchyLevel().getValue() + 1)/ HRMConfig.Hierarchy.HEIGHT);
		
		//Logging.log(this, "Returning decoration value: " + tResult);
		
		return tResult;
	}

	@Override
	public void setDecorationValue(Object tLabal)
	{
		// not used, but have to be implemented for implementing interface IElementDecorator
	}

	/**
	 * Returns a location description about this instance
	 */
	@Override
	public String toLocation()
	{
		String tResult = getClass().getSimpleName() + "@" + getHRMController().getNodeGUIName() + "@" + getHierarchyLevel().getValue();
		
		return tResult;
	}

}
