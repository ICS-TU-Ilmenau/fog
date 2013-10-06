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

import de.tuilmenau.ics.fog.packets.hierarchical.topology.AnnounceCoordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.election.Elector;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class represents a cluster member (can also be a cluster head).
 */
public class CoordinatorAsClusterMember extends ClusterMember
{
	private static final long serialVersionUID = -8746079632866375924L;

	/**
	 * Stores the coordinator for which this cluster membership was created 
	 */
	private Coordinator mCoordinatorAsClusterMember = null;

	/**
	 * Constructor
	 *  
	 * @param pHRMController the local HRMController instance
	 * @param pHierarchyLevel the hierarchy level
	 * @param pClusterID the unique ID of this cluster
	 * @param pCoordinatorID the unique coordinator ID for this cluster
	 * @param pCoordinatorNodeL2Address the L2 address of the node where the coordinator of this cluster is located
	 */
	public CoordinatorAsClusterMember(HRMController pHRMController, HierarchyLevel pHierarchyLevel, Long pClusterID, int pCoordinatorID, L2Address pCoordinatorNodeL2Address)
	{	
		super(pHRMController, pHierarchyLevel, pClusterID, pCoordinatorID, pCoordinatorNodeL2Address);

		Logging.log(this, "CREATED");
	}

	/**
	 * Factory function: for coordinators which join a cluster
	 * 
	 * @param pHRMController the local HRMController instance
	 * @param pCoordinator the coordinator which joins a cluster
	 * @param pClusterName a ClusterName which this describes the cluster of the coordinator for which this cluster membership was created
	 * @param pClusterID the unique ID of this cluster
	 * @param pClusterHeadNodeL2Address the L2 address of the node where the cluster head is located
	 * @return
	 */
	public static CoordinatorAsClusterMember create(HRMController pHRMController, Coordinator pCoordinator, ClusterName pClusterName, L2Address pClusterHeadNodeL2Address)
	{
		CoordinatorAsClusterMember tResult = new CoordinatorAsClusterMember(pHRMController, pClusterName.getHierarchyLevel(), pClusterName.getClusterID(), pClusterName.getCoordinatorID(), pClusterHeadNodeL2Address);
		
		Logging.log(tResult, "\n\n\n################ CREATED COORDINATOR AS CLUSTER MEMBER at hierarchy level: " + (tResult.getHierarchyLevel().getValue()));

		// register at HRMController's internal database
		//pHRMController.registerClusterMember(tResult);

		// creates new elector object, which is responsible for Bully based election processes
		tResult.mElector = new Elector(pHRMController, tResult);

		// update the coordinator for which this membership was created
		tResult.mCoordinatorAsClusterMember = pCoordinator;
		
		// register the coordinator in the local ARG
		//if (HRMConfig.DebugOutput.GUI_SHOW_COORDINATORS_IN_ARG){
		//	pHRMController.registerLinkARG(tResult, pCoordinator, new AbstractRoutingGraphLink(AbstractRoutingGraphLink.LinkType.OBJECT_REF));
		//}
		
		return tResult;
	}

	/**
	 * EVENT: coordinator announcement, we react on this by:
	 *       1.) forward this packet to the coordinator for which this cluster membership was created
	 * 
	 * @param pComChannel the source comm. channel
	 * @param pAnnounceCoordinator the received announcement
	 */
	@Override
	public void eventCoordinatorAnnouncement(ComChannel pComChannel, AnnounceCoordinator pAnnounceCoordinator)
	{
		Logging.log(this, "EVENT: coordinator announcement (from above): " + pAnnounceCoordinator);
		Logging.log(this, "       ..fowarding announcement to coordinator object: " + mCoordinatorAsClusterMember);
		
		mCoordinatorAsClusterMember.eventCoordinatorAnnouncement(pComChannel, pAnnounceCoordinator);
	}

	/**
	 * Defines the decoration text for the ARG viewer
	 * 
	 * @return text for the control entity or null if no text is available
	 */
	@Override
	public String getText()
	{
		return "CoordAsClusterMember" + getGUIClusterID() + "@" + mHRMController.getNodeGUIName() + "@" + getHierarchyLevel().getValue() + "(" + idToString() + ", Coord.=" + getCoordinatorNodeL2Address()+ ")";
	}

	/**
	 * Returns a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return toLocation() + "(" + idToString() + ", Coord.=" + getCoordinatorNodeL2Address()+ ")";
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
		if ((getHRMID() == null) || (getHRMID().isRelativeAddress())){
			return "ID=" + getClusterID() + ", CoordID=" + superiorCoordinatorID() +  ", Prio=" + getPriority().getValue();
		}else{
			return "HRMID=" + getHRMID().toString();
		}
	}
}
