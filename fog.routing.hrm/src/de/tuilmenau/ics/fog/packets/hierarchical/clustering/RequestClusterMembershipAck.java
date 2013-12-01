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
 * PACKET: This packet is used to acknowledge a RequestComChannel packet. It is send after the comm. channel was established.
 * 
 * ****************************************************************************************************************************
 * ****************************************** Explanation of the packet usage *************************************************
 * ****************************************************************************************************************************
 * 
 *        "2. request acknowledge packet " (the figure shows the ACK packet for the figure of "RequestClusterMembership") 
 *
 *                                      
 *               /==========\
 *               |L2 cluster| <------ REQUEST ACK PACKET -------+
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
 */
public class RequestClusterMembershipAck extends SignalingMessageHrm
{
	private static final long serialVersionUID = 445881657397476245L;

	/**
	 * Store the ClusterName of the sender (a Coordinator or a new ClusterMember object)
	 */
	private ClusterName mSource = null;
	
	public static long sCreatedPackets = 0;

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pSourceCoordinator the ClusterName of the sender (a coordinator)
	 */
	public RequestClusterMembershipAck(Name pSenderName, Name pReceiverName, ClusterName pSource)
	{
		super(pSenderName, pReceiverName);
		mSource = pSource;
		sCreatedPackets++;
		
		Logging.log(this, "CREATED");
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", Source=" + mSource + ")";
	}
}
