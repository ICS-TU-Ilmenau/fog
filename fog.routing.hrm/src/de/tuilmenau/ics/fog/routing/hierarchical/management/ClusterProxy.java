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

import java.rmi.RemoteException;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborClusterAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used when a remote cluster is detected. It includes all needed data about the remote cluster.
 */
public class ClusterProxy extends ControlEntity implements ICluster
{
	private static final long serialVersionUID = -8746079632866375924L;

	/**
	 * Stores the reference to the local HRMController instance.
	 */
	private HRMController mHRMController;

	private int mToken;
	private Long mClusterID;

	private Name mCoordName;
	
	/**
	 * Constructor
	 *  
	 * @param pHRMController the local HRMController instance
	 * @param pClusterID the unique ID of this cluster
	 * @param pHierarchyLevel the hierarchy level
	 * @param pCoordName
	 * @param pAddress
	 * @param pToken
	 */
	public ClusterProxy(HRMController pHRMController, Long pClusterID, HierarchyLevel pHierarchyLevel, Name pCoordName, HRMName pAddress, int pToken)
	{	
		super(pHRMController, pHierarchyLevel);

		mClusterID = pClusterID;
		mHRMController = pHRMController;
		mCoordName = pCoordName;
		mToken = pToken;
		setCoordinatorName(pCoordName);
		
		// register the ClusterProxy at the local ARG
		getHRMController().registerNodeARG(this);

		Logging.log(this, "CREATED");
	}

	/**
	 * Determines the physical simulation machine specific ClusterID multiplier
	 * 
	 * @return the generated multiplier
	 */
	private long clusterIDMachineMultiplier()
	{
		//TODO: get this value from the signaling
		return 1;
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
	 * Returns the machine-local ClusterID (excluding the machine specific multiplier)
	 * 
	 * @return the machine-local ClusterID
	 */
	public long getGUIClusterID()
	{
		return mClusterID / clusterIDMachineMultiplier();
	}
	
	
	
	

	public void handleNeighborAnnouncement(NeighborClusterAnnounce pAnnounce, ComChannel pCEP)
	{
		if(pAnnounce.getRoutingVectors() != null) {
			for(RoutingServiceLinkVector tVector : pAnnounce.getRoutingVectors()) {
				mHRMController.getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
			}
		}
		ICluster tCluster = mHRMController.getCluster(new ClusterName(pAnnounce.getToken(), pAnnounce.getClusterID(), pAnnounce.getLevel()));
		if(tCluster == null)
		{
			Logging.log(this, "     ..creating cluster proxy");
			tCluster = new ClusterProxy(mHRMController, pAnnounce.getClusterID(), getHierarchyLevel(), pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress(), pAnnounce.getToken());
			mHRMController.setSourceIntermediateCluster(tCluster, mHRMController.getSourceIntermediateCluster(this));
			tCluster.setPriority(pAnnounce.getCoordinatorsPriority());
			tCluster.setToken(pAnnounce.getToken());
		} else {
			Logging.log(this, "Cluster announced by " + pAnnounce + " is an intermediate neighbor ");
		}
		//((AttachedCluster)tCluster).setNegotiatingHost(pAnnounce.getAnnouncersAddress());

		/*
		 * function checks whether neighbor relation was established earlier
		 */
		registerNeighbor((ControlEntity)tCluster);

		if(pAnnounce.getCoordinatorName() != null) {
			RoutingService tRS = (RoutingService)mHRMController.getNode().getRoutingService();
			if(! tRS.isKnown(pAnnounce.getCoordinatorName())) {
				try {
					mHRMController.getHRS().mapFoGNameToL2Address(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
				} catch (RemoteException tExc) {
					Logging.err(this, "Unable to register " + pAnnounce.getCoordinatorName() + " at name mapping service", tExc);
				}
			}
		}
	}

	public synchronized void setSuperiorCoordinator(ComChannel pCoordinatorComChannel, Name pCoordinatorName, int pCoordToken, L2Address pCoordinatorL2Address)
	{
		// nothing
	}

	@Override
	public void setToken(int pToken) {
		mToken = pToken;
	}

	@Override
	public int getToken() {
		return mToken;
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.routing.hierarchical.management.ICluster#getHighestPriority()
	 */
	@Override
	public BullyPriority getHighestPriority()
	{
		return getPriority();
	}

	@Override
	public void setHighestPriority(BullyPriority pHighestPriority) {
		/*
		 * not needed, this is just a dummy for topology
		 */
	}

	@Override
	public Namespace getNamespace() {
		return new Namespace("attachedcluster");
	}

	@Override
	public int getSerialisedSize() {
		return 0;
	}

	@Override
	public ComChannelMuxer getMultiplexer()
	{
		return null;
	}

	
	public int hashCode()
	{
		return mClusterID.intValue() * 1;
	}


	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.routing.hierarchical.management.ControlEntity#handleBullyAnnounce(de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce, de.tuilmenau.ics.fog.routing.hierarchical.management.ComChannel)
	 */
	@Override
	public void handleBullyAnnounce(BullyAnnounce pBullyAnnounce, ComChannel pComChannel)
	{
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.routing.hierarchical.management.ICluster#getCoordinatorName()
	 */
	@Override
	public Name getCoordinatorName()
	{
		return mCoordName;
	}

	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.routing.hierarchical.management.ICluster#setCoordinatorName(de.tuilmenau.ics.fog.facade.Name)
	 */
	@Override
	public void setCoordinatorName(Name pCoordName)
	{
		mCoordName = pCoordName;		
	}
	
	
	
	
	
	/**
	 * Returns a descriptive string about the cluster
	 * 
	 * @return the descriptive string
	 */
	public String getClusterDescription()
	{
		return toLocation();
		//getHRMController().getPhysicalNode() + ":" + mClusterID + "@" + getHierarchyLevel() + "(" + mCoordSignature + ")";
	}

	/**
	 * Returns a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	@SuppressWarnings("unused")
	public String toString()
	{
		HRMID tHRMID = getHRMID();
		
		if(tHRMID != null && HRMConfig.Debugging.PRINT_HRMIDS_AS_CLUSTER_IDS) {
			return tHRMID.toString();
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
