/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.properties;

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.properties.AbstractProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.management.HierarchyLevel;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used for describing a cluster, which both the connection source and the destination should 
 * have in common (the connection destination should join the described cluster). 
 * 
 * Additionally, the sender describes local cluster members, which have already joined this new cluster.
 * 
 */
public class RequestClusterParticipationProperty extends AbstractProperty
{
	/**
	 * Stores the hierarchy level of the cluster
	 */
	private HierarchyLevel mHierarchyLevel = new HierarchyLevel(this, -1);
	
	/**
	 * Stores the hierarchy level of the sender
	 */
	private HierarchyLevel mSenderHierarchyLevel = new HierarchyLevel(this,  -1);
	
	/**
	 * Stores the unique clusterID
	 */
	private Long mClusterID = null;
			
	/**
	 * Stores the L2Address of the node where the sender is located
	 */
	private L2Address mSenderL2Address = null;

	private static final long serialVersionUID = 7561293731302599090L;
	
	/**
	 * Factory function
	 * 
	 * @param pHRMController the HRMController of the current node
	 * @param pSenderHierarchyLevel the hierarchy level of the sender
	 * @param pClusterID the already created unique ID for the cluster the sender and the receiver should be part of
	 * @param pHierarchyLevel the hierarchy level of the cluster
	 */
	public static RequestClusterParticipationProperty create(HRMController pHRMController, HierarchyLevel pSenderHierarchyLevel, Long pClusterID, HierarchyLevel pHierarchyLevel)
	{
		// get the recursive FoG layer
		FoGEntity tFoGLayer = (FoGEntity) pHRMController.getNode().getLayer(FoGEntity.class);

		// get the central FN of this node
		L2Address tThisHostL2Address = pHRMController.getHRS().getL2AddressFor(tFoGLayer.getCentralFN());
	
		RequestClusterParticipationProperty tResult = new RequestClusterParticipationProperty(tThisHostL2Address, pSenderHierarchyLevel, pClusterID, pHierarchyLevel);
		
		return tResult;
	}
	
	/**
	 * Constructor
	 * 
	 * @param pSenderNodeName the FoG name of the node where the sender is located
	 * @param pSenderL2Address the L2Adress of the node where the sender is located
	 * @param pSenderHierarchyLevel the hierarchy level of the sender
	 * @param pClusterID the already created unique ID for the cluster the sender and the receiver should be part of
	 * @param pHierarchyLevel the hierarchy level of the new cluster
	 */
	private RequestClusterParticipationProperty(L2Address pSenderL2Address, HierarchyLevel pSenderHierarchyLevel, Long pClusterID, HierarchyLevel pHierarchyLevel)
	{
		Logging.log(this, "Setting sender L2Address: " + pSenderL2Address);
		Logging.log(this, "Setting sender hierarchy level: " + pSenderHierarchyLevel.getValue());
		Logging.log(this, "Setting target cluster ID: " + pClusterID);
		Logging.log(this, "Setting cluster hierarchy level: " + pHierarchyLevel.getValue());
		mSenderL2Address = pSenderL2Address;
		mSenderHierarchyLevel = pSenderHierarchyLevel;
		mClusterID = pClusterID;
		mHierarchyLevel = pHierarchyLevel;
	}
	
	/**
	 * Returns the unique cluster ID
	 * 
	 * @return the unique cluster ID 
	 */
	public Long getClusterID()
	{
		return mClusterID;
	}
	
	/**
	 * Returns the hierarchy level
	 * 
	 * @return the hierarchy level
	 */
	public HierarchyLevel getHierarchyLevel()
	{
		return mHierarchyLevel;
	}
	
	/**
	 * Returns the hierarchy level of the sender
	 * 
	 * @return the hierarchy level of the sender
	 */
	public HierarchyLevel getSenderHierarchyLevel()
	{
		return mSenderHierarchyLevel;
	}

	/**
	 * 
	 * Returns the L2Address of the node where the sender is located
	 * 
	 * @return the L2Address of the node where the sender is located 
	 */
	public L2Address getSenderL2Address()
	{
		return mSenderL2Address;
	}

	/**
	 * Generates a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return getClass().getSimpleName() + "(ClusterID=" + mClusterID + ", HierLvl.=" + getHierarchyLevel().getValue() + ", ";
	}
}
