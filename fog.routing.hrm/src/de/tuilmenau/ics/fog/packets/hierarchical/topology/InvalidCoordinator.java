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
import de.tuilmenau.ics.fog.util.Size;

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
public class InvalidCoordinator extends SignalingMessageHrm implements ISignalingMessageHrmBroadcastable, ISignalingMessageASSeparator
{
	private static final long serialVersionUID = -1548886959657058300L;

	/**
	 * Stores the ClusterName of the sender
	 */
	private ClusterName mSendingCoordinator = new ClusterName(null, null, null, 0);
	
	/**
	 * Stores the L2 address of the node where the coordinator of the announced cluster is located
	 */
	private L2Address mCoordinatorNodeL2Address = new L2Address(0);
	
	/**
	 * Stores the current TTL value. If it reaches 0, the packet will be dropped
	 */
	private long mTTL = HRMConfig.Hierarchy.RADIUS;
	
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
	 * Constructor for getDefaultSize()
	 */
	private InvalidCoordinator()
	{
		super();
	}

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
		
		mSendingCoordinator = pSenderClusterName;
		mCoordinatorNodeL2Address = pCoordinatorNodeL2Address;
		
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
	public String getPassedNodes()
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
	 * Returns the ClusterName of the sender
	 * 
	 * @return
	 */
	public ClusterName getSenderClusterName()
	{
		return mSendingCoordinator;
	}
	
	/**
	 * Returns the L2 address of the node where the coordinator of the announced cluster is located
	 * 
	 * @return the L2 address
	 */
	public L2Address getSenderClusterCoordinatorNodeL2Address()
	{
		return mCoordinatorNodeL2Address;
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
	 * Decreases the TTL value by one 
	 */
	public void decreaseTTL()
	{
		mTTL--;
	}
	
	/**
	 * Returns true if the TTL is still okay
	 * 
	 * @return true or false
	 */
	public boolean isTTLOkay()
	{
		/**
		 * Return always true for the highest hierarchy level
		 */
		if(getSenderClusterName().getHierarchyLevel().isHighest()){
			return true;
		}

		/**
		 * Return always true for the second highest hierarchy level
		 */
		if(getSenderClusterName().getHierarchyLevel().getValue() == HRMConfig.Hierarchy.HEIGHT -2){
			return true;
		}
		
		/**
		 * Return true depending on the TTL value
		 */
		return (mTTL > 0);
	}
	
	/**
	 * Checks if the next AS may be entered by this packet
	 * 
	 * @param pHRMController the current HRMController instance
	 * @param the AsID of the next AS
	 * 
	 * @return true or false
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.packets.hierarchical.topology.ISignalingMessageASSeparator#isAllowedToEnterAs(de.tuilmenau.ics.fog.routing.hierarchical.HRMController, java.lang.Long)
	 */
	@Override
	public boolean isAllowedToEnterAs(HRMController pHRMController,	Long pNextAsID)
	{
		/**
		 * Return always true for the highest hierarchy level
		 */
		if(getSenderClusterName().getHierarchyLevel().getValue() >= HRMConfig.Hierarchy.HEIGHT - 2){
			return true;
		}

		/**
		 * Return true if the given AsID describes the current AS
		 */
		if(pHRMController.getAsID().equals(pNextAsID)){
			return true;
		}
		
		return false;
	}

	/**
	 * Returns the current TTL value
	 * 
	 * @return the TTL value
	 */
	public long getTTL()
	{
		return mTTL;
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
		InvalidCoordinator tResult = new InvalidCoordinator(null, getSenderName(), getSenderClusterName(), getSenderClusterCoordinatorNodeL2Address());
		sCreatedPackets--;
		
		super.duplicate(tResult);

		// update TTL
		tResult.mTTL = getTTL();
		
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
		 * 		SignalingMessageHRM	    	= 1
		 * 		SendingCoordinator        	= 9
		 * 		CoordinatorNodeL2Address  	= 16
		 * 		TTL					     	= 2
		 * 		EnteredSidewardForwarding 	= 1
		 * 		PassedNodes.length		 	= 1
		 * 		PassedNodes				 	= dynamic
		 * 
		 *************************************************************/

		int tResult = 0;
		
		tResult += getDefaultSize();
		tResult += 1; // size of the following list
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += (mPassedNodes.size() * new L2Address(0).getSerialisedSize());
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
		 * 		SignalingMessageHRM	     	= 1
		 * 		SendingCoordinator        	= 9
		 * 		CoordinatorNodeL2Address  	= 16
		 * 		TTL					     	= 2
		 * 		EnteredSidewardForwarding 	= 1
		 * 
		 *************************************************************/

		int tResult = 0;
		
		InvalidCoordinator tTest = new InvalidCoordinator();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("Size of " + tTest.getClass().getSimpleName());
		}
		tResult += SignalingMessageHrm.getDefaultSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += tTest.mSendingCoordinator.getSerialisedSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += tTest.mCoordinatorNodeL2Address.getSerialisedSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += 2; // TTL: use only 2 bytes here
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		tResult += Size.sizeOf(tTest.mEnteredSidewardForwarding);
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
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "/" + getOriginalMessageNumber() + "](Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", TTL=" + getTTL() + ", SenderCluster="+ getSenderClusterName() + ")";
	}
}
