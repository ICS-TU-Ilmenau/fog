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

import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;

/**
 * This class is used to identify a cluster (independent from its physical location)
 */
public class ClusterName extends ControlEntity implements Serializable, AbstractRoutingGraphNode
{
	private static final long serialVersionUID = 3027076881853652810L;
	
	/**
	 * Constructor
	 * 
	 * @param pHRMController the local HRMController instance (for accessing topology data)
	 * @param pHierarchyLevel the hierarchy level
	 * @param pCoordinatorID the unique ID of the coordinator
	 * @param pClusterID the unique ID of the cluster
	 */
	public ClusterName(HRMController pHRMController, HierarchyLevel pHierarchyLevel, int pCoordinatorID, Long pClusterID)
	{
		super(pHRMController, pHierarchyLevel);
		
		setClusterID(pClusterID);
		setSuperiorCoordinatorID(pCoordinatorID);
		setCoordinatorID(pCoordinatorID);
	}
	
	/**
	 * Returns the machine-local ClusterID (excluding the machine specific multiplier)
	 * 
	 * @return the machine-local ClusterID
	 */
	public long getGUIClusterID()
	{
		//TODO: bei signalisierten ClusterName-Objekten stimmt hier der Bezug zum richtigen MachineMultiplier nicht
		if (getClusterID() != null)
			return getClusterID() / idMachineMultiplier();
		else
			return -1;
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
		String tResult = "Cluster" + getGUIClusterID() + "@" + getHierarchyLevel().getValue();
		
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
