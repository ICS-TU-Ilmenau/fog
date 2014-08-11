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
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used to identify a cluster (independent from its physical location)
 */
public class ClusterName implements Serializable
{
	private static final long serialVersionUID = 3027076881853652810L;
	
	/**
	 * Stores the hierarchy level of this cluster.
	 * This value is not part of the concept. It is only used to ease the debugging. An unique clusterID would be enough for a real world implementation.
	 */
	private HierarchyLevel mHierarchyLevel = HierarchyLevel.createBaseLevel();

	/**
	 * Stores the unique cluster manager ID. This ID needs to be node locally unique.
	 */
	private Long mClusterID = new Long(-1);

	/**
	 * Stores the unique coordinator ID. This ID needs to be node locally unique for debugging purposes.
	 * This value is not part of the concept. It is only used to ease the debugging. An unique clusterID would be enough for a real world implementation.
	 */
	private long mCoordinatorID = -1;
	
	/**
	 * Stores the physical simulation machine specific multiplier, which is used to create unique IDs even if multiple physical simulation machines are connected by FoGSiEm instances
	 * The value "-1" is important for initialization!
	 */
	private static long sIDMachineMultiplier = -1;

	/**
	 * Constructor
	 * 
	 * @param pClusterID the unique ID of the cluster
	 * @param pHierarchyLevel the hierarchy level
	 * @param pCoordinatorID the unique ID of the coordinator
	 */
	public ClusterName(Long pClusterID, HierarchyLevel pHierarchyLevel, long pCoordinatorID)
	{
		//Logging.log(this, "Creating ClusterName for cluster: " + pClusterID + " and coordinator: " + pCoordinatorID);

		setClusterID(pClusterID);
		mHierarchyLevel = pHierarchyLevel;
		//setSuperiorCoordinatorID(pCoordinatorID);
		setCoordinatorID(pCoordinatorID);
	}
	
	/**
	 * Determines the physical simulation machine specific ID multiplier
	 * 
	 * @return the generated multiplier
	 */
	static protected long idMachineMultiplier()
	{
		if (sIDMachineMultiplier < 0){
			String tHostName = HRMController.getHostName();
			if (tHostName != null){
				sIDMachineMultiplier = Math.abs((tHostName.hashCode() % 10000) * 10000);
			}else{
				Logging.err(null, "Unable to determine the machine-specific ClusterID multiplier because host name couldn't be indentified");
			}
		}

		return sIDMachineMultiplier;
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
	 * Sets the cluster ID
	 * 
	 * @param pNewClusterID the new cluster ID
	 */
	protected void setClusterID(Long pNewClusterID)
	{
		mClusterID = pNewClusterID;
	}
	
	/**
	 * Returns the full CoordinatorID (including the machine specific multiplier)
	 * 
	 *  @return the full CoordinatorID
	 */
	public long getCoordinatorID()
	{
		return mCoordinatorID;
	}
	
	/**
	 * Sets the cluster ID
	 * 
	 * @param pNewCoordinatorID the new cluster ID
	 */
	protected void setCoordinatorID(long pNewCoordinatorID)
	{
		//Logging.log(this, "Setting coordinator ID: " + pNewCoordinatorID);
		mCoordinatorID = pNewCoordinatorID;
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
	 * Returns the machine-local CoordinatorID (excluding the machine specific multiplier)
	 * 
	 * @return the machine-local CoordinatorID
	 */
	public long getGUICoordinatorID()
	{
		if (getClusterID() != null)
			return getCoordinatorID() / idMachineMultiplier();
		else
			return -1;
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
	 * Returns the size of a serialized representation of this object
	 *  
	 * @return the size of the serialized representation
	 */
	public int getSerialisedSize()
	{
		return getDefaultSize();
	}

	/**
	 * Returns the default size of this packet
	 * 
	 * @return the default size
	 */
	public static int getDefaultSize()
	{
		/*************************************************************
		 * Size of serialized elements in [bytes]:
		 * 
		 * 		ClusterID             		= 4
		 * 
		 *************************************************************/
		
		int tResult = 0;
		
		tResult += 4; // clusterID as longint
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
//			Logging.log("   ..resulting size: " + tResult);
		}

		return tResult;
	}

	
	/**
	 * Returns if both objects address the same cluster/coordinator
	 * 
	 * @return true or false
	 */
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj instanceof ClusterName){
			ClusterName tComparedObj = (ClusterName) pObj;
			
			if ((tComparedObj.getClusterID().longValue() == getClusterID().longValue()) && (tComparedObj.getHierarchyLevel().equals(getHierarchyLevel())) && (tComparedObj.getCoordinatorID() == getCoordinatorID())) {
				return true;
			}
		}
		
		return false;
	}	
				
	/**
	 * Clones this object
	 * 
	 * @return the object clone
	 */
	public ClusterName clone()
	{
		return new ClusterName(getClusterID(), getHierarchyLevel(), getCoordinatorID());
	}

	/**
	 * Returns a location description about this instance
	 */
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
		return "Coordinator" + getGUICoordinatorID();
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
}
