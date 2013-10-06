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
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;

/**
 * PACKET: This packet is used to request the remote side to join a local cluster. It is send from a coordinator to another coordinator.
 * 
 * ****************************************************************************************************************************
 * ****************************************** Explanation of the packet usage *************************************************
 * ****************************************************************************************************************************
 * 
 *                                 "1. request packet " 
 *
 *                                      
 *               /==========\
 *               |L2 cluster| ------- REQUEST PACKET -----------+
 *               \==========/                                   |
 *                    |                                         |
 *                   \|/                                       \|/
 *                +-------+                                 +-------+
 *           +... |Coord.1| ...+                       +... |Coord.1| ...+
 *           :    +-------+    :                       :    +-------+    :
 *           :                 :                       :                 :
 *           :                 :                       :                 : 
 *       +-------+         +-------+               +-------+         +-------+
 *       |Coord.0|         |Coord.0|               |Coord.0|         |Coord.0|
 *       +-------+         +-------+               +-------+         +-------+
 *       
 *       
 * ****************************************************************************************************************************
 * ****************************************************************************************************************************
 *       
 */
public class RequestClusterMembership extends SignalingMessageHrm
{
	private static final long serialVersionUID = 445881657397476245L;

	/**
	 * Store the ClusterName of the sending coordinator
	 */
	private ClusterName mRequestingClusterName = null;
	
	/**
	 * Store the ClusterName of the target coordinator
	 */
	private ClusterName mTargetCoordinator = null;

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pSourceCoordinator the ClusterName of the sender (a cluster)
	 * @param pTargetClusterName the ClusterName of the target (another coordinator)
	 */
	public RequestClusterMembership(Name pSenderName, Name pReceiverName, ClusterName pRequestingCluster, ClusterName pTargetCoordinator)
	{
		super(pSenderName, pReceiverName);
		
		mRequestingClusterName = pRequestingCluster;
		mTargetCoordinator = pTargetCoordinator;
	}

	/**
	 * Returns the ClusterName of the source cluster
	 * 
	 * @return the ClusterName of the source
	 */
	public ClusterName getRequestingCluster()
	{
		return mRequestingClusterName;
	}
	
	/**
	 * Returns the ClusterName of the target coordinator
	 * 
	 * @return the ClusterName of the target
	 */
	public ClusterName getTargetCoordinator()
	{
		return mTargetCoordinator;
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", Requester="+ getRequestingCluster() + ", TargetCoord.=" + getTargetCoordinator() + ")";
	}
}
