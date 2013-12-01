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
 * PACKET: This packet is used to request the remote side to leave the local cluster. It is send from a cluster head to all cluster members
 * 
 * ****************************************************************************************************************************
 * ****************************************** Explanation of the packet usage *************************************************
 * ****************************************************************************************************************************
 * 
 *                                 "1. cancel packet " 
 *
 *                                      
 *               /==========\
 *               |L2 cluster| -------- CANCEL PACKET -----------+
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
public class InformClusterMembershipCanceled extends SignalingMessageHrm
{
	private static final long serialVersionUID = -5548938370118859121L;

	/**
	 * Store the ClusterName of the sending cluster head
	 */
	private ClusterName mRequestingClusterName = null;
	
	/**
	 * Store the ClusterName of the request destination
	 */
	private ClusterName mRequestDestination = null;

	public static long sCreatedPackets = 0;

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pRequestingCluster the ClusterName of the sender (a Cluster object)
	 * @param pDestination the ClusterName of the target (a Coordinator or a new ClusterMember object)
	 */
	public InformClusterMembershipCanceled(Name pSenderName, Name pReceiverName, ClusterName pRequestingCluster, ClusterName pDestination)
	{
		super(pSenderName, pReceiverName);
		
		mRequestingClusterName = pRequestingCluster;
		mRequestDestination = pDestination;
		sCreatedPackets++;
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
	 * Returns the ClusterName of the request destination
	 * 
	 * @return the ClusterName of the destination
	 */
	public ClusterName getDestination()
	{
		return mRequestDestination;
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", Requester="+ getRequestingCluster() + ", Destination=" + getDestination() + ")";
	}
}
