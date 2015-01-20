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
import de.tuilmenau.ics.fog.topology.NetworkInterface;
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
	 * Stores the counter of created packets from this type
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static Long sCreatedPackets = new Long(0);

	/**
	 * Stores the inter-node link reference.
	 * This value is not part of the HRM concept. It only eases the implementation. It could also be determined by the receiver based on an additional tracking of the network interface for each new incoming connection.
	 * However, this is not supported by the FoG implementation at the moment. TODO: implement this part in FoG
	 */	
	private NetworkInterface mInterNodeLink = null;
	
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
	 */
	public RequestClusterMembership(HRMName pSenderName, HRMName pReceiverName)
	{
		super(pSenderName, pReceiverName);
		
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
		
		//Logging.log(this, "CREATED");
	}
	
	/**
	 * Sets the inter-node link
	 * 
	 * @pram pInterNodeLink the inter-node link
	 * 
	 */
	public void setInterNodeLink(NetworkInterface pInterNodeLink)
	{
		mInterNodeLink = pInterNodeLink;
	}

	/**
	 * Returns the inter-node link
	 * 
	 * @return the inter-node-link
	 */
	public NetworkInterface getInterNodeLink()
	{
		return mInterNodeLink;
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
		 * 		[MultiplexHeader]
		 * 		[SignalingMessageHrm]
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
		 * 		[MultiplexHeader]
		 * 		[SignalingMessageHrm]
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
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", Requester="+ getSenderClusterName() + ", Destination=" + getReceiverClusterName() + ")";
	}
}
