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
import de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * PACKET: This packet is used to leave a remote cluster. It is send from a cluster member to the cluster head.
 * 
 * ****************************************************************************************************************************
 * ****************************************** Explanation of the packet usage *************************************************
 * ****************************************************************************************************************************
 * 
 *                                 "1. leave packet " 
 *
 *                                      
 *               /==========\
 *               |L2 cluster| <-------- LEAVE PACKET -----------+
 *               \==========/                                   |
 *                   /|\                                        |
 *                    |                                         | 
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
public class LeaveCluster extends SignalingMessageHrm implements ISignalingMessageHrmBroadcastable
{
	private static final long serialVersionUID = 445881657397476245L;

	/**
	 * Store the ClusterName of the sending ClusterMember
	 */
	private ClusterName mLeavingClusterMember = null;
	
	/**
	 * Store the ClusterName of the destination Cluster
	 */
	private ClusterName mRequestDestination = null;

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pLeavingMember the ClusterName of the sender (a ClusterMember object)
	 * @param pDestination the ClusterName of the target (a Cluster object)
	 */
	public LeaveCluster(Name pSenderName, Name pReceiverName, ClusterName pLeavingMember, ClusterName pDestination)
	{
		super(pSenderName, pReceiverName);
		
		mLeavingClusterMember = pLeavingMember;
		mRequestDestination = pDestination;
	}

	/**
	 * Returns the ClusterName of the leaving ClusterMember
	 * 
	 * @return the ClusterName of the ClusterMember
	 */
	public ClusterName getLeavingClusterMember()
	{
		return mLeavingClusterMember;
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
	 * Returns a duplicate of this packet
	 * 
	 * @return the duplicate packet
	 */
	@Override
	public SignalingMessageHrm duplicate()
	{
		LeaveCluster tResult = new LeaveCluster(getSenderName(), getReceiverName(), getLeavingClusterMember(), getDestination());
		
		super.duplicate(tResult);

		Logging.log(this, "Created duplicate packet: " + tResult);
		
		return tResult;
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", Leaver="+ getLeavingClusterMember() + ", Destination=" + getDestination() + ")";
	}
}
