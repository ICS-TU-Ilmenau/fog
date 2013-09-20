/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.topology;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;

/**
 * PACKET: This packet is used within the HRM infrastructure in order to tell other clusters the existence of a remote cluster. 
 */
public class AnnounceCluster extends SignalingMessageHrm
{
	/**
	 * Stores the ClusterName of the sender
	 */
	private ClusterName mSenderClusterName = null;
	
	/**
	 * Stores the name of the node where the coordinator of the announced cluster is located
	 */
	private Name mCoordinatorNodeName = null;
	
	/**
	 * Stores the current TTL value. If it reaches 0, the packet will be dropped
	 */
	private int mTTL = HRMConfig.Hierarchy.EXPANSION_RADIUS;
	
	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pSenderClusterName the ClusterName of the sender
	 * @param pCoordinatorNodeName the name of the node where the coordinator of the announced cluster is located
	 */
	public AnnounceCluster(Name pSenderName, ClusterName pSenderClusterName, Name pCoordinatorNodeName)
	{
		super(pSenderName, HRMID.createBroadcast());
		
		mSenderClusterName = pSenderClusterName;
		mCoordinatorNodeName = pCoordinatorNodeName;
	}
	
	/**
	 * Returns the ClusterName of the sender
	 * 
	 * @return
	 */
	public ClusterName getSenderClusterName()
	{
		return mSenderClusterName;
	}
	
	/**
	 * Returns the name of the node where the coordinator of the announced cluster is located
	 * 
	 * @return the node name
	 */
	public Name getSenderClusterCoordinatorNodeName()
	{
		return mCoordinatorNodeName;
	}
	
	/**
	 * Decreases the TTL value by one 
	 */
	public void decreaseTTL()
	{
		mTTL--;
	}
	
	/**
	 * Returns the current TTL value
	 * 
	 * @return the TTL value
	 */
	public int getTTL()
	{
		return mTTL;
	}
}
