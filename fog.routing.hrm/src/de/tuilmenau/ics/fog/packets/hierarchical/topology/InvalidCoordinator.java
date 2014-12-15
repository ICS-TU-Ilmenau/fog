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

import java.util.LinkedList;

import de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * PACKET: This packet is used within the HRM infrastructure in order to tell other clusters about the invalidation of a remote cluster.
 *         Such information is needed for coordinators, which can use this information in order to update their higher clusters. 
 *
 * ****************************************************************************************************************************
 * ************************ Explanation how such a packet is forwarded within the HRM infrastructure  *************************
 * ****************************************************************************************************************************
 * 
 *                      "1. towards the bottom of the hierarchy" 
 *
 *                                      +-------+
 *                    +---------------- |Coord.2| ---------------+
 *                    |                 +-------+                |
 *                    |                                          |
 *                    |                                          |
 *                   \|/                                        \|/
 *                +-------+                                 +-------+
 *           +--- |Coord.1| ---+                       +--- |Coord.1| ---+
 *           |    +-------+    |                       |    +-------+    |
 *           |                 |                       |                 |
 *          \|/               \|/                     \|/               \|/
 *       +-------+         +-------+               +-------+         +-------+
 *       |Coord.0|         |Coord.0|               |Coord.0|         |Coord.0|
 *       +-------+         +-------+               +-------+         +-------+
 *           |                 |                       |                 |
 *           |                 |                       |                 |
 *          \|/               \|/                     \|/               \|/
 *     /==========\       /==========\           /==========\       /==========\
 *     |L0 cluster|       |L0 cluster|           |L0 cluster|       |L0 cluster|
 *     \==========/       \==========/           \==========/       \==========/  
 *
 * 
 * 
 *                              "2. towards the side"
 *     /==========\       /==========\           /==========\       /==========\
 *     |L0 cluster| <---> |L0 cluster| <-------> |L0 cluster| <---> |L0 cluster|
 *     \==========/       \==========/           \==========/       \==========/
 *       
 *                               
 *                                                               
 * ****************************************************************************************************************************
 * ****************************************************************************************************************************
*/
public class InvalidCoordinator extends SignalingMessageHierarchyUpdate implements ISignalingMessageHrmBroadcastable
{
	private static final long serialVersionUID = -1548886959657058300L;

	/**
	 * Stores if the packet is still forward top-downward or sidewards
	 */
	private boolean mEnteredSidewardForwarding = false;

	/**
	 * Stores the passed node
	 */
	private LinkedList<L2Address> mPassedNodes = new LinkedList<L2Address>();

	/**
	 * Stores the counter of created packets from this type
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static Long sCreatedPackets = new Long(0);

	/**
	 * Stores the counter of sent broadcasts from this type
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static Long sSentBroadcasts = new Long(0);

	/**
	 * Constructor
	 * 
	 * @param pHRMController the HRMController instance
	 * @param pSenderName the name of the message sender
	 * @param pSenderClusterName the ClusterName of the sender
	 * @param pCoordinatorNodeL2Address the L2 address of the node where the coordinator is located
	 */
	public InvalidCoordinator(HRMController pHRMController, HRMName pSenderName, ClusterName pSenderClusterName, L2Address pCoordinatorNodeL2Address)
	{
		super(pSenderName, HRMID.createBroadcast());
		
		setSenderEntityName(pSenderClusterName);
		
		setSenderEntityNodeL2Address(pCoordinatorNodeL2Address);
		
		/**
		 * Record the sender node
		 */
		if(pHRMController != null){
			addPassedNode(pHRMController.getNodeL2Address());
		}
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
	}
	
	/**
	 * Record the passed nodes
	 * 
	 * @param pNode the unique ID of the passed node
	 */
	public void addPassedNode(L2Address pNode)
	{
		synchronized (mPassedNodes) {
			mPassedNodes.add(pNode);
		}
	}

	/**
	 * Checks if a cluster was already passed
	 * 
	 * @param pNode the unique ID of the passed node
	 */
	public boolean hasPassedNode(L2Address pNode)
	{
		boolean tResult = false;
		
		synchronized (mPassedNodes) {
			tResult = mPassedNodes.contains(pNode);
		}
		
		return tResult;
	}

	/**
	 * Returns a list of passed nodes
	 * 
	 * @return the list of passed nodes
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<L2Address> getPassedNodes()
	{
		LinkedList<L2Address> tResult = null;
		
		synchronized (mPassedNodes) {
			tResult = (LinkedList<L2Address>) mPassedNodes.clone();
		}
		
		return tResult; 
	}
	
	/**
	 * Returns a list of passed nodes
	 * 
	 * @return the list of passed nodes
	 */
	public String getPassedNodesStr()
	{
		String tResult = "";
		
		synchronized (mPassedNodes) {
			for(L2Address tPassedNode : mPassedNodes){
				tResult += " " + tPassedNode;
			}
		}

		return tResult;
	}

	/**
	 * Returns if the sideward forwarding was already started
	 * 
	 * @return true or false
	 */
	public boolean enteredSidewardForwarding()
	{
		return mEnteredSidewardForwarding;
	}
	
	/**
	 * Marks this packet as currently in sideward forwarding
	 */
	public void setSidewardForwarding()
	{
		mEnteredSidewardForwarding = true;	
	}
	
	/**
	 * Returns a duplicate of this packet
	 * 
	 * @return the duplicate packet
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SignalingMessageHrm duplicate()
	{
		InvalidCoordinator tResult = new InvalidCoordinator(null, getSenderName(), getSenderEntityName(), getSenderEntityNodeL2Address());
		
		super.duplicate(tResult);

		// update "hop counter" (counted depending on the hierarchy level)
		tResult.mHopCounter = mHopCounter;
		
		// update "sideward forwarding" marker
		tResult.mEnteredSidewardForwarding = enteredSidewardForwarding();

		// update the recorded nodes
		tResult.mPassedNodes = (LinkedList<L2Address>) mPassedNodes.clone();
				
		//Logging.log(this, "Created duplicate packet: " + tResult);
		
		return tResult;
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
		 * 		[SignalingMessageHrmTopologyUpdate]
		 *		RouteHopCount 			 	= 1 (PassedNodes.length)
		 * 		PassedNodes				 	= dynamic
		 * 
		 *************************************************************/

		int tResult = 0;
		
		tResult += getDefaultSize();
		tResult += (mPassedNodes.size() * L2Address.getDefaultSize());
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		
		return tResult;
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
		 * 		[SignalingMessageHrmTopologyUpdate]
		 *		RouteHopCount 			 	= 1 (PassedNodes.length)
		 * 
		 *************************************************************/

		int tResult = 0;
		
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("Size of InvalidCoordinator");
		}
		tResult += SignalingMessageHierarchyUpdate.getDefaultSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += 1; // RouteHopCount: use only 1 byte here
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		
		return tResult;
	}
	
	/**
	 * Returns if this packet type has a dynamic size
	 * 
	 * @return true or false
	 */
	public static boolean hasDynamicSize()
	{
		return true;
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
	 * Accounts a broadcast of this packet type
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable#accountBroadcast()
	 */
	@Override
	public void accountBroadcast()
	{
		super.accountBroadcast();
		synchronized (sCreatedPackets) {
			sCreatedPackets--;
			sSentBroadcasts++;
		}
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "/" + getOriginalMessageNumber() + "](Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", TTL=" + mHopCounter + ", SenderCluster="+ getSenderEntityName() + ")";
	}
}
