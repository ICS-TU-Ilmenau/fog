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

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used to identify a remote cluster (including the location information). It includes all needed data about the remote cluster.
 */
public class ClusterProxy extends ClusterName
{
	private static final long serialVersionUID = -8746079632866375924L;

	/**
	 * Stores the name of the node where the coordinator of the addressed cluster is located
	 */
	protected Name mCoordinatorNodeName = null;
	
	/**
	 * Constructor
	 *  
	 * @param pHRMController the local HRMController instance
	 * @param pHierarchyLevel the hierarchy level
	 * @param pClusterID the unique ID of this cluster
	 * @param pCoordinatorNodeName
	 * @param pCoordinatorID
	 */
	public ClusterProxy(HRMController pHRMController, HierarchyLevel pHierarchyLevel, Long pClusterID, Name pCoordinatorNodeName, int pCoordinatorID)
	{	
		super(pHRMController, pHierarchyLevel, pClusterID, pCoordinatorID);

		// store the name of the node where the coordinator is located
		mCoordinatorNodeName = pCoordinatorNodeName;

		// do we already have a valid cluster ID? -> then we also have a valid hash code for this object and may use this as reference within the ARG
		if (pClusterID != null){
			// register the ClusterProxy at the local ARG
			mHRMController.registerNodeARG(this);
		}

		Logging.log(this, "CREATED");
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
	 * Returns the name of the node where the coordinator of the described cluster is located
	 * 
	 * @return the node name
	 */
	public Name getCoordinatorNodeName()
	{
		return mCoordinatorNodeName;
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
