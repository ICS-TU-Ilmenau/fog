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
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.routing.hierarchical.management.Coordinator;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * PACKET: This packet is used within the HRM infrastructure in order to tell other clusters about the existence of a remote cluster.
 *         Such information is needed for coordinators, which can use this information in order to create a new higher cluster with the coordinators of announce neighbor clusters. 
 *
 * ****************************************************************************************************************************
 * ************************ Explanation how such a packet is forwarded within the HRM infrastructure  *************************
 * ****************************************************************************************************************************
 * 
 *                      "1. towards the bottom of the hierarchy" (only for hierarchy height > 3)
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
 * HOW TO announce for a Hierarchy height of 3:
 *  - L0 coordinators are broadcasted till the radius is reached
 *  - L1 coordinators are broadcasted everywhere
 *  - L2 coordinators are never broadcasted because no superior cluster exists which could use such L2 coordinators
 *  * never send announcements to nodes, which represent the end of a dead-end route
 *    
 *                               
 * HINT: Assumption: each L0 coordinator knows to which L1+ clusters it belongs.
 * 
 * HINT (TTL): TTL handling: Based on the previous assumption, each L0 cluster is able to decide if a new logical hop is passed 
 *             when forwarding such packets within level 0. As a result of this, the TTL value can be automatically decreased if
 *             a new logical hop is entered 
 *                                
 * HINT (max. hierarchy level): Level 0 cluster don't have to distribute announces from the coordinator at the maximum hierarchy 
 *                              level beyond the abstract borders of the cluster at maximum hierarchy. Each node gets this information 
 *                              from its superior coordinators. There is not additional node which still needs this information 
 *                              forwarded from the side. Otherwise, we would have an isolated node which doesn't belong to the
 *                              HRM infrastructure. 
 *                                                               
 * ****************************************************************************************************************************
 * ****************************************************************************************************************************
*/
public class AnnounceCoordinator extends SignalingMessageHierarchyUpdate implements ISignalingMessageHrmBroadcastable
{
	private static final long serialVersionUID = -1548886959657058300L;

	/**
	 * Stores the logical hop count for the stored route 
	 * This value is used to simplify the implementation. We use route objects from the FoG implementation (otherwise, we would have to introduce a new route description). 
	 */
	private int mPhysHopCount = 0;
	
	/**
	 * Defines the life span of this announcement in [s]. Allowed values are between 0 and 255.
	 */
	private double mLifeSpan = 0;

	/**
	 * Stores the route to the announced cluster
	 * This value is FoG-specific and eases the implementation. The recorded L2Address values of the passed nodes (variable "mPassedNodes") are enough to determine a valid route to the sending coordinator. 
	 */
	private Route mFoGRoute = new Route();
	
	/**
	 * Stores the passed clusters for the GUI
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	private LinkedList<Long> mGUIPassedClusters = new LinkedList<Long>();

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
	 * Defines if packet tracking is active
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	private boolean mPacketTracking = false;
	
	/**
	 * Constructor
	 * 
	 * @param pHRMController the HRMController instance
	 * @param pSenderName the name of the message sender
	 * @param pSenderClusterName the ClusterName of the sender
	 * @param pCoordinatorNodeL2Address the L2 address of the node where the coordinator is located
	 * @param pCoordinator the coordinator for which the announcement should be sent
	 */
	public AnnounceCoordinator(HRMController pHRMController, HRMName pSenderName, ClusterName pSenderClusterName, L2Address pCoordinatorNodeL2Address, Coordinator pCoordinator)
	{
		super(pSenderName, HRMID.createBroadcast());
		
		mLifeSpan = calcLifetime(pCoordinator);
		
		if(pCoordinator != null){
			setLastHopEntityName(pCoordinator);
		}
		
		setSenderEntityName(pSenderClusterName);

		setSenderEntityNodeL2Address(pCoordinatorNodeL2Address);
		
		if(pHRMController != null){
			/**
			 * Record the sender node
			 */
			addPassedNode(pHRMController.getNodeL2Address());
		
			/**
			 * Record the passed clusters
			 */
			addGUIPassedCluster(new Long(pSenderClusterName.getGUIClusterID()));
		}
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
	}
	
	private double calcLifetime(Coordinator pCoordinator)
	{
		double tResult = 2 * HRMConfig.Hierarchy.COORDINATOR_ANNOUNCEMENTS_INTERVAL + HRMConfig.Hierarchy.MAX_E2E_DELAY; 
		
		if((pCoordinator != null) && (pCoordinator.hasLongTermExistence())){
			//Logging.err(this, "Using higher lifetime here");
			tResult = HRMConfig.Hierarchy.COORDINATOR_ANNOUNCEMENTS_INTERVAL_LT_EXISTENCE + HRMConfig.Hierarchy.MAX_E2E_DELAY;
		}

		return tResult;
	}
	
	/**
	 * Record the passed clusters
	 * 
	 * @param pClusterID the unique ID of the passed cluster
	 */
	public void addGUIPassedCluster(Long pClusterID)
	{
		synchronized (mGUIPassedClusters) {
			mGUIPassedClusters.add(pClusterID);
		}
	}

	/**
	 * Returns a list of passed clusters
	 * 
	 * @return the list of passed clusters (their IDs)
	 */
	public String getGUIPassedClusters()
	{
		String tResult = "";
		
		synchronized (mGUIPassedClusters) {
			for(Long tPassedCluster : mGUIPassedClusters){
				tResult += " " + Long.toString(tPassedCluster);
			}
		}

		return tResult;
	}
	
	/**
	 * Returns the lifetime of this announcement
	 * 
	 * @return the lifetime
	 */
	public double getLifetime()
	{
		return mLifeSpan;
	}
	
	/**
	 * Adds an entry to the recorded route towards the announced cluster
	 * 
	 * @param pRoute the partial route which should be added to the route
	 */
	public void addRouteHop(Route pRoute)
	{
		if(pRoute != null){
			increasePhysHopCount();
			
			if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
				Logging.log(this, "Adding route head");
				Logging.log(this, "      ..old route to sender: " + mFoGRoute);
			}
			Route tNewRoute = pRoute.clone();
			tNewRoute.add(mFoGRoute);
			mFoGRoute = tNewRoute;
			if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
				Logging.log(this, "      ..new route to sender: " + mFoGRoute);
			}
		}else{
			Logging.warn(this, "Cannot add an invalid route head");
		}
	}
	
	/**
	 * Returns the hop count for the physical route in the network.
	 * 
	 * @return the route costs
	 */
	public int getPhysHopCount()
	{
		return mPhysHopCount;
	}
	
	/**
	 * Increases the hop count of the physical route.
	 */
	private void increasePhysHopCount()
	{
		mPhysHopCount++;
	}
	
	/**
	 * Returns the route to the announced cluster.
	 * 
	 * @return the route
	 */
	public Route getRoute()
	{
		return mFoGRoute.clone();
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
		AnnounceCoordinator tResult = new AnnounceCoordinator(null, getSenderName(), getSenderEntityName(), getSenderEntityNodeL2Address(), null);
		
		super.duplicate(tResult);

		// packet tracking
		tResult.mPacketTracking = mPacketTracking;
		
		// update the route to the announced cluster
		tResult.mFoGRoute = getRoute();
		
		// update the route hop costs 
		tResult.mPhysHopCount = getPhysHopCount();
		
		// update "sideward forwarding" marker
		tResult.mEnteredSidewardForwarding = enteredSidewardForwarding();
		
		// add an entry to the recorded source route
		tResult.addSourceRoute("[route]: (" + mFoGRoute + ") -> (" + tResult.mFoGRoute + ")");

		// last hop's entity name
		tResult.mLastHopEntityName = mLastHopEntityName;
		
		// update "hop counter" (counted depending on the hierarchy level)
		tResult.mHopCounter = mHopCounter;

		// lifetime value
		tResult.mLifeSpan = mLifeSpan;

		// update the recorded nodes
		tResult.mRouteToSender = (LinkedList<L2Address>) mRouteToSender.clone();

		// update the recorded cluster ID
		tResult.mGUIPassedClusters = (LinkedList<Long>) mGUIPassedClusters.clone();
		
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
		 * 		[SignalingMessageHiearchyUpdate]
		 * 		Life span					= 1
		 * 		Route to sender 		 	= dynamic
		 * 
		 *************************************************************/

		int tResult = getDefaultSize();
		
		tResult += (mRouteToSender.size() * L2Address.getDefaultSize());
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
		 * 		[SignalingMessageHiearchyUpdate]
		 * 		Life span					= 1
		 *
		 *************************************************************/

		int tResult = 0;
		
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("Size of AnnounceCoordinator");
		}
		tResult += SignalingMessageHierarchyUpdate.getDefaultSize();
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		
		/**
		 * life duration: use only 1 byte here
		 */
		tResult += 1;
		if(HRMConfig.DebugOutput.GUI_SHOW_PACKET_SIZE_CALCULATIONS){
			Logging.log("   ..resulting size: " + tResult);
		}
		
		/**
		 * length of the route to sender
		 */
		//---
		/**
		 * Remark: Within the measurements, only hierarchies with a depth of 3 are used. Hence, the length of the route to the sender always corresponds to the hop counter and does not need to be transmitted as separate value.
		 */

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
	 * Activates packet tracking
	 */
	public void activateTracking()
	{
		mPacketTracking = true;		
	}

	/**
	 * Returns if packet tracking is active
	 */
	public boolean isPacketTracking()
	{
		return mPacketTracking;
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
