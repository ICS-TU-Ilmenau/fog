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

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
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
public class AnnounceCoordinator extends SignalingMessageHrm implements ISignalingMessageHrmBroadcastable
{
	private static final long serialVersionUID = -1548886959657058300L;

	/**
	 * Stores the ClusterName of the sender
	 */
	private ClusterName mSenderClusterName = null;
	
	/**
	 * Stores the L2 address of the node where the coordinator of the announced cluster is located
	 */
	private L2Address mCoordinatorNodeL2Address = null;
	
	/**
	 * Stores the current TTL value. If it reaches 0, the packet will be dropped
	 */
	private int mTTL = HRMConfig.Hierarchy.EXPANSION_RADIUS; //TODO: set the max. INT value if it is the highest hierarchy level -> tell everyone about this coordinator!
	
	/**
	 * Stores the logical hop count for the stored route 
	 */
	private int mRouteHopCount = 0;
	
	/**
	 * Stores the route to the announced cluster
	 */
	private Route mRoute = new Route();
	
	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pSenderClusterName the ClusterName of the sender
	 * @param pCoordinatorNodeL2Address the L2 address of the node where the coordinator is located
	 */
	public AnnounceCoordinator(Name pSenderName, ClusterName pSenderClusterName, L2Address pCoordinatorNodeL2Address)
	{
		super(pSenderName, HRMID.createBroadcast());
		
		mSenderClusterName = pSenderClusterName;
		mCoordinatorNodeL2Address = pCoordinatorNodeL2Address;
	}
	
	/**
	 * Returns the ClusterName of the sender
	 * 
	 * @return
	 */
	public ClusterName getSenderClusterName()
	{
		return mSenderClusterName;
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
		return (mTTL > 0);
	}
	
	/**
	 * Returns the current TTL value
	 * 
	 * @return the TTL value
	 */
	public int getTTL()
	{
		return mTTL;
	}
	
	/**
	 * Adds an entry to the recorded route towards the announced cluster
	 * 
	 * @param pRoute the partial route which should be added to the route
	 */
	public void addRouteHop(Route pRoute)
	{
		if(pRoute != null){
			increaseRouteHopCount();
			
			Logging.log(this, "Adding route head");
			Logging.log(this, "      ..old route to sender: " + mRoute);
			Route tNewRoute = pRoute.clone();
			tNewRoute.add(mRoute);
			mRoute = tNewRoute;
			Logging.log(this, "      ..new route to sender: " + mRoute);
		}else{
			Logging.warn(this, "Cannot add an invalid route head");
		}
	}
	
	/**
	 * Returns the costs for the route to the announced cluster
	 * 
	 * @return the route costs
	 */
	public int getRouteHopCount()
	{
		return mRouteHopCount;
	}
	
	/**
	 * Increases the hop count for this route
	 */
	private void increaseRouteHopCount()
	{
		mRouteHopCount++;
	}
	
	/**
	 * Returns the route to the announced cluster.
	 * 
	 * @return the route
	 */
	public Route getRoute()
	{
		return mRoute.clone();
	}
	
	/**
	 * Returns a duplicate of this packet
	 * 
	 * @return the duplicate packet
	 */
	@Override
	public SignalingMessageHrm duplicate()
	{
		AnnounceCoordinator tResult = new AnnounceCoordinator(getSenderName(), getSenderClusterName(), getSenderClusterCoordinatorNodeL2Address());
		
		super.duplicate(tResult);

		// update TTL
		tResult.mTTL = getTTL();
		
		// update the route to the announced cluster
		tResult.mRoute = getRoute();
		
		// update the route hop costs 
		tResult.mRouteHopCount = getRouteHopCount();
		
		// add an entry to the recorded source route
		tResult.addSourceRoute("[route]: (" + mRoute + ") -> (" + tResult.mRoute + ")");

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
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", TTL=" + getTTL() + ", SenderCluster="+ getSenderClusterName() + ")";
	}
}
