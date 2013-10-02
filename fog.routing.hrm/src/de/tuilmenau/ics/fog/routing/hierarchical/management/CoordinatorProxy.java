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
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class represents a cluster member (can also be a cluster head).
 */
public class CoordinatorProxy extends ClusterMember
{
	private static final long serialVersionUID = 5456243840140110970L;

	/**
	 * Constructor
	 *  
	 * @param pHRMController the local HRMController instance
	 * @param pHierarchyLevel the hierarchy level
	 * @param pClusterID the unique ID of this cluster
	 * @param pCoordinatorID the unique coordinator ID for this cluster
	 * @param pCoordinatorNodeName the node name where the coordinator of this cluster is located
	 */
	public CoordinatorProxy(HRMController pHRMController, HierarchyLevel pHierarchyLevel, Long pClusterID, int pCoordinatorID, Name pCoordinatorNodeName)
	{	
		super(pHRMController, pHierarchyLevel, pClusterID, pCoordinatorID, pCoordinatorNodeName);

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
	public static CoordinatorProxy create(HRMController pHRMController, ClusterName pClusterName, Name pCoordinatorNodeName)
	{	
		CoordinatorProxy tResult = new CoordinatorProxy(pHRMController, pClusterName.getHierarchyLevel(), pClusterName.getClusterID(), pClusterName.getCoordinatorID(), pCoordinatorNodeName);
		
		Logging.log(tResult, "\n\n\n################ CREATED COORDINATOR PROXY at hierarchy level: " + (tResult.getHierarchyLevel().getValue()));

		// register at HRMController's internal database
		pHRMController.registerCoordinatorProxy(tResult);

		return tResult;
	}

	/**
	 * Defines the decoration text for the ARG viewer
	 * 
	 * @return text for the control entity or null if no text is available
	 */
	@Override
	public String getText()
	{
		return "RemoteCoordinator" + getGUICoordinatorID() + "@" + mHRMController.getNodeGUIName() + "@" + getHierarchyLevel().getValue() + "(Cluster" + getGUIClusterID() + ", " + idToString() + ", Coord.=" + getCoordinatorNodeName()+ ")";
	}

	/**
	 * Returns a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return toLocation() + "(Cluster" + getGUIClusterID() + ", " + idToString() + ", Coord.=" + getCoordinatorNodeName()+ ")";
	}

	/**
	 * Returns a location description about this instance
	 */
	@Override
	public String toLocation()
	{
		String tResult = getClass().getSimpleName() + getGUICoordinatorID() + "@" + mHRMController.getNodeGUIName() + "@" + (getHierarchyLevel().getValue() - 1);
		
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
			return "ID=" + getClusterID() + ", NodePrio=" + getPriority().getValue();
		}else{
			return "HRMID=" + getHRMID().toString();
		}
	}
}
