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
import de.tuilmenau.ics.fog.util.Size;

/**
 * This class is used to identify a cluster (independent from its physical location)
 */
public class ClusterName extends ControlEntity implements Serializable, AbstractRoutingGraphNode
{
	private static final long serialVersionUID = 3027076881853652810L;
	
	private ClusterName()
	{
		super();
	}
	
	/**
	 * Constructor
	 * 
	 * @param pHRMController the local HRMController instance (for accessing topology data)
	 * @param pHierarchyLevel the hierarchy level
	 * @param pClusterID the unique ID of the cluster
	 * @param pCoordinatorID the unique ID of the coordinator
	 */
	public ClusterName(HRMController pHRMController, HierarchyLevel pHierarchyLevel, Long pClusterID, long pCoordinatorID)
	{
		super(pHRMController, pHierarchyLevel);
		
		//Logging.log(this, "Creating ClusterName for cluster: " + pClusterID + " and coordinator: " + pCoordinatorID);

		setClusterID(pClusterID);
		setSuperiorCoordinatorID(pCoordinatorID);
		setCoordinatorID(pCoordinatorID);
	}
	
	/**
	 * Returns the size of a serialized representation of this packet 
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.transfer.gates.headers.ProtocolHeader#getSerialisedSize()
	 */
	@Override
	public int getSerialisedSize()
	{
		/*
		 * Serialized size in byte:
		 * Hierarchy level       = 1
		 * ClusterID             = 4
		 * CoordinatorID		 = 4
		 */

		return getDefaultSize();
	}

	/**
	 * Returns the default size of this packet
	 * 
	 * @return the default size
	 */
	public static int getDefaultSize()
	{
		/*
		 * Serialized size in byte:
		 * Hierarchy level       = 1
		 * ClusterID             = 4
		 * CoordinatorID		 = 4
		 */

		int tResult = 0;
		
		ClusterName tTest = new ClusterName();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
//			Logging.log("Size of " + tTest.getClass().getSimpleName());
		}
		tResult += tTest.getHierarchyLevel().getSerialisedSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
//			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += Size.sizeOf(tTest.getClusterID());
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
//			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += Size.sizeOf(tTest.getCoordinatorID());
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
//			Logging.log("   ..resulting size: " + tResult);
		}

		return tResult;
	}

	/**
	 * Clones this object
	 * 
	 * @return the object clone
	 */
	public ClusterName clone()
	{
		return new ClusterName(mHRMController, getHierarchyLevel(), getClusterID(), getCoordinatorID());
	}
	
	/**
	 * Returns a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return toLocation() + "(" + idToString() + ")";
	}

	/**
	 * Returns a location description about this instance
	 */
	@Override
	public String toLocation()
	{
		String tResult = "Cluster" + getGUIClusterID() + "@" + (getHierarchyLevel() != null ? getHierarchyLevel().getValue() : "-");
		
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
			return "Coordinator" + getGUICoordinatorID();
		}else{
			return "Coordinator" + getGUICoordinatorID() + ", HRMID=" + getHRMID().toString();
		}
	}
}
