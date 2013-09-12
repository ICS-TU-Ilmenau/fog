/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.clustering;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.management.HierarchyLevel;

/**
 * PACKET: This packet is used when a coordinator wants to join an existing superior cluster
 */
public class RequestClusterMembershipAck extends SignalingMessageHrm
{
	private static final long serialVersionUID = 445881657397476245L;

	/**
	 * Store the clusterId of the sender
	 */
	private Long mSenderClusterID = null;
	
	/**
	 * Stores the coordinatorID of the sender 
	 */
	private int mSenderCoordinatorID;
	
	/**
	 * Stores the hierarchy level of the sender
	 */
	private HierarchyLevel mSenderHierarchyLevel = null;
	
	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pSenderClusterID the clusterID of the sender
	 * @param pSenderCoordinatorID the coordinatorID of the sender
	 * @param pSenderHierarchyLevel the hierarchy level of the sender
	 */
	public RequestClusterMembershipAck(Name pSenderName, Name pReceiverName, Long pSenderClusterID, int pSenderCoordinatorID, HierarchyLevel pSenderHierarchyLevel)
	{
		super(pSenderName, pReceiverName);
		
		mSenderClusterID = pSenderClusterID;
		mSenderCoordinatorID = pSenderCoordinatorID;
		mSenderHierarchyLevel = pSenderHierarchyLevel;		
	}

	/**
	 * Returns the clusterID of the sender
	 * 
	 * @return the clusterID of the sender
	 */
	public Long getSenderClusterID()
	{
		return mSenderClusterID;
	}
	
	/**
	 * Returns the CoordinatorID of the sender
	 * 
	 * @return the CoordinatorID of the sender
	 */
	public int getSenderCoordinatorID()
	{
		return mSenderCoordinatorID;
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
}
