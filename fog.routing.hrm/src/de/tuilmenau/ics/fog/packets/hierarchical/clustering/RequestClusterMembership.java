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

import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Size;

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
	private ClusterName mRequestingCluster = new ClusterName(null, null, null, 0);
	
	/**
	 * Store the ClusterName of the request destination
	 */
	private ClusterName mRequestDestination = new ClusterName(null, null, null, 0);

	/**
	 * Stores the counter of created packets from this type
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static Long sCreatedPackets = new Long(0);

	/**
	 * Constructor for getDefaultSize()
	 */
	private RequestClusterMembership()
	{
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pRequestingCluster the ClusterName of the sender (a Cluster object)
	 * @param pDestination the ClusterName of the target (a Coordinator or a new ClusterMember object)
	 */
	public RequestClusterMembership(HRMName pSenderName, HRMName pReceiverName, ClusterName pRequestingCluster, ClusterName pDestination)
	{
		super(pSenderName, pReceiverName);
		
		mRequestingCluster = pRequestingCluster;
		mRequestDestination = pDestination;
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
		
		Logging.log(this, "CREATED");
	}

	/**
	 * Returns the ClusterName of the source cluster
	 * 
	 * @return the ClusterName of the source
	 */
	public ClusterName getRequestingCluster()
	{
		return mRequestingCluster;
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
	 * Returns the size of a serialized representation of this packet 
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.transfer.gates.headers.ProtocolHeader#getSerialisedSize()
	 */
	@Override
	public int getSerialisedSize()
	{
		/*************************************************************
		 * Size of serialized elements in [bytes]:
		 * 
		 * 		SignalingMessageHRM	   = 1
		 * 		RequestingCluster  	   = 9
		 * 		RequestDestination     = 9
		 * 
		 *************************************************************/

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
		 * 		SignalingMessageHRM	   = 1
		 * 		RequestingCluster  	   = 9
		 * 		RequestDestination     = 9
		 * 
		 *************************************************************/

		int tResult = 0;
		
		RequestClusterMembership tTest = new RequestClusterMembership();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("Size of " + tTest.getClass().getSimpleName());
		}
		tResult += SignalingMessageHrm.getDefaultSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += tTest.mRequestingCluster.getSerialisedSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += tTest.mRequestDestination.getSerialisedSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}

		return tResult;
	}

	/**
	 * Returns the counter of created packets from this type
	 *  
	 * @return the packet counter
	 */
	public static long getCreatedPackets()
	{
		long tResult = 0;
		
		synchronized (sCreatedPackets) {
			tResult = sCreatedPackets;
		}
		
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
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", Requester="+ getRequestingCluster() + ", Destination=" + getDestination() + ")";
	}
}
