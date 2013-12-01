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
import de.tuilmenau.ics.fog.ui.Logging;

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
	public RequestClusterMembership(Name pSenderName, Name pReceiverName, ClusterName pRequestingCluster, ClusterName pDestination)
	{
		super(pSenderName, pReceiverName);
		
		mRequestingClusterName = pRequestingCluster;
		mRequestDestination = pDestination;
		sCreatedPackets++;
		
		Logging.log(this, "CREATED");
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
