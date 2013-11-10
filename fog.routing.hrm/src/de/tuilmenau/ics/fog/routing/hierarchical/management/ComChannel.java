/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.management;

import java.util.LinkedList;

import de.tuilmenau.ics.fog.packets.hierarchical.addressing.AnnounceHRMIDs;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.AssignHRMID;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.RevokeHRMIDs;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.InformClusterLeft;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.InformClusterMembershipCanceled;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.RequestClusterMembershipAck;
import de.tuilmenau.ics.fog.packets.hierarchical.MultiplexHeader;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.packets.hierarchical.election.*;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.AnnounceCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.InvalidCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.RoutingInformation;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.TopologyReport;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingEntry;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingTable;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * A communication channel can exist between level (0) cluster instances, or a level (n) coordinator and a level (n + 1) cluster instance.
 * Each ComChannel instance has only one possible parental ComSession associated. However, a ComSession object can be responsible
 * for multiple ComChannel instances.
 *
 * 
 * ****************************************************************************************************************************
 * ***************************** Explanation for two hierarchy levels (0 and 1) and three nodes *******************************
 * ****************************************************************************************************************************
 * From an abstract view, the communication is as follows: (note: node 2 is depicted two times here!)
 *
 *                                                        /===========================\
 *                                                        |         +---------+       |
 *                                                        |         |Coord.3@1|       |
 *                                                        |         +---------+       |
 *                                                        \== Cluster4@2 =|===========/
 *                                                                       /|\
 *                                                                        |
 *                                                                        |internal comm.
 *                                                                        |
 *                                                                       \|/
 *                                /=======================================|======\
 *                                |  +---------+                    +---------+  |
 *                                |  |Coord.1@0| <-- int. comm. --> |Coord.2@0|  |
 *                                |  +---------+                    +---------+  |
 *                                \======|======= Cluster3@1 =============|======/
 *                                      /|\                              /|\
 *                                       |                                |
 *                                       |internal comm.                  |internal comm.
 *                                       |                                |
 *                                      \|/                              \|/
 *   /===================================|=====\                     /====|====================================\
 *   |   +------+                   +------+   |                     |   +------+                   +------+   |			
 *   |   |Node 1| <-- ext. comm.--> |Node 2|   |                     |   |Node 2| <-- ext. comm.--> |Node 3|   |
 *   |   +------+                   +------+   |                     |   +------+                   +------+   |
 *   \============= Cluster1@0 ================/                     \============= Cluster2@0 ================/
 *   
 *   
 *   
 * ****************************************************************************************************************************
 * ****************************************************************************************************************************
 *
 * From a detailed implementation view, the used communication channels between the objects are as follows (arrows indicate the connection direction):
 *
 *                                                                       /====================\
 *                                                                       |                    |
 *                                                                       | instance on node 2 |
 *                                                                       |                    |
 *                                                                       \== Cluster4@2 ======/
 *                                                                                 |
 *   A CONNECTION/Session is needed as parent --->                                 |CHANNEL
 *   for each channel                                                             \|/
 *                                                                             +-------+        
 *                                                                             |Coord.3|        
 *                                                                             +-------+        
 *                                                                                 |
 *                                                                                 |LOCAL OBJ. REF.
 *                                                                                 |
 *                                                                                 | 
 *                                                                       /====================\
 *                                  /--                                  |                    |
 *                                  |                  +---- CHANNEL --- | instance on node 2 |
 *                                  |                  |                 |                    |
 *   both channels are summarized --+                  |                 \== Cluster3@1 ======/
 *   in ONE CONNECTION/Session to   |                  |                           |                 
 *   reduce connection complexity   |                  |                           |   
 *                                  |                  |                           |CHANNEL   
 *                                  \--               \|/                         \|/   
 *                                                 +-------+                   +-------+  
 *                                                 |Coord.1|                   |Coord.2|  
 *                                                 +-------+                   +-------+  
 *                                                     |                           |
 *                                                     |LOCAL OBJ. REF.            |LOCAL OBJ. REF.
 *                                                     |                           |
 *                                                     |                           |
 *   /====================\                 /====================\      /====================\                 /====================\
 *   |                    |                 |                    |      |                    |                 |                    |			
 *   | instance on node 1 | <-- CHANNEL --> | instance on node 2 |      | instance on node 2 | <-- CHANNEL --> | instance on node 3 | 
 *   |      (MEMBER)      |                 |                    |      |                    |                 |       (MEMBER)     | 
 *   \===== Cluster1@0 ===/                 \===== Cluster1@0 ===/      \===== Cluster2@0 ===/                 \===== Cluster2@0 ===/
 *   
 *   
 *   HINT (distributed cluster representation): a cluster can be represented by multiple instances (see "Cluster1@0") 
 *         because it can span over multiple physical nodes
 *   
 *   HINT (base hierarchy level): on level 0, the connection direction depends on the detection order, 
 *         either node 1 detects first node 2 or the other way round
 *   
 *   HINT (cluster broadcasts): if a coordinator wants to send a packet to all cluster members, it 
 *         calls a function of the cluster object ("sendClusterBroadcast()")
 *         
 *   HINT (addressing cluster member): if a coordinator wants to send data (e.g., during "share phase") to a selected
 *        cluster member, it uses the communication channels, which are stored within the cluster instance
 *        
 *  GENERAL: Because of the selected distribution of communication channels, a coordinator can be destroyed without 
 *           losing all communication channels for an existing network cluster.
 *           Under normal circumstances*, each coordinator should have only ONE communication channel, which leads to 
 *           its superior cluster. But each cluster can have MANY communication channels, each leading to one cluster member.
 *
 *   HINT: A comm. session can summarize multiple local comm. channels. However, these channels can belong to different local coordinators.
 *                    
 *   HINT (distributed clustering): The clustering code is based on redundant clusters. Hence, a broadcast domain with 3 nodes leads to 3
 *   	  L0 cluster associations (Cluster/ClusterMember instances) per node. Additionally, at higher hierarchy levels, the clustering code
 *        sees every node as possible cluster head and instantiates a Cluster object at every node. Each of those Cluster object can have
 *        communication channels to multiple ClusterMember objects. 
 *                        
 *           *otherwise, bugs exist within the clustering
 *                    
 * ****************************************************************************************************************************
 * ****************************************************************************************************************************
 * ****************************************************************************************************************************/
public class ComChannel
{
	public enum Direction{IN, OUT};

	private ClusterName mRemoteClusterName = null;

	/**
	 * Stores the parent control entity (cluster or coordinator) to which this communication channel belongs to
	 */
	private ControlEntity mParent;
	
	/**
	 * Stores the parent communication session
	 */
	private ComSession mParentComSession = null;
	
	/**
	 * Stores the Bully priority of the peer
	 */
	private BullyPriority mPeerPriority = null;
	
	/**
	 * Stores the freshness of the Bully priority of the peer
	 */
	private double mPeerPriorityTimestampLastUpdate = 0; 
	
	/**
	 * Stores the direction of the communication channel (either "out" or "in")
	 */
	private Direction mDirection;
	
	/**
	 * Stores a list of known peer HRMIDs
	 */
	private LinkedList<HRMID> mPeerHRMIDs = new LinkedList<HRMID>();
	
	/**
	 * Stores a list of assigned peer HRMIDs
	 */
	private LinkedList<HRMID> mAssignedPeerHRMIDs = new LinkedList<HRMID>();

	/**
	 * Stores a list of used cluster address for this comm. channel.
	 * We could also use "mPeerHRMIDs" for having an overview about the used cluster addresses. But this way, the life is easier.
	 */
	private LinkedList<Integer> mUsedClusterAddresses = new LinkedList<Integer>();
	
	/**
	 * Stores the comm. channel state
	 */
	public enum ChannelState{CLOSED, HALF_OPEN, OPEN};
	private ChannelState mChannelState = ChannelState.HALF_OPEN;
	
	/**
	 * Stores the peer entity
	 */
	private ControlEntity mPeer = null;
	
	/**
	 * Stores the counter for received packets
	 */
	private int mReceivedPackets = 0;
	
	/**
	 * Stores the counter for sent packets
	 */
	private int mSentPackets = 0;
	
	/**
	 * Stores if this comm. channel is end-point of an active HRM link between the parent and the peer
	 */
	private boolean mLinkActivation = true;
	
	/**
	 * Stores a description about all link activation changes
	 */
	private String mDesccriptionLinkActivation = "";
	
	/**
	 * Stores the HRMController reference
	 */
	private HRMController mHRMController = null;
	
	/**
	 * Stores the HRMID of the peer
	 */
	private HRMID mPeerHRMID = null;
	
	/**
	 * Stores the send/received packets
	 */
	private LinkedList<ComChannelPacketMetaData> mPackets = new LinkedList<ComChannelPacketMetaData>();
	
	/**
	 * Stores the packet queue
	 */
	private LinkedList<SignalingMessageHrm> mPacketQueue = new LinkedList<SignalingMessageHrm>();
	
	/**
	 * Stores the routing data which is shared with the peer (only used for ClusterMember parent)
	 */
	private RoutingTable mSharedRoutingData = new RoutingTable();
	
	/**
	 * Stores the routing table, which is reported based on peer HRMIDs data
	 */
	private RoutingTable mReportedRoutingTablePeerHRMIDs = new RoutingTable();

	/**
	 * Stores the routing table, which is stored locally based on peer HRMIDs data
	 */
	private RoutingTable mLocalRoutingTablePeerHRMIDs = new RoutingTable();

	/**
	 * Stores the routing table, which is reported to the coordinator
	 */
	private RoutingTable mReportedRoutingTable = new RoutingTable();

	/**
	 * Constructor
	 * 
	 * @param pHRMController is the HRMController instance of this node
	 * @param pDirection the direction of the communication channel (either upward or downward)
	 * @param pParent the parent control entity
	 * @param pParentComSession is the parental comm. session
	 */
	public ComChannel(HRMController pHRMController, Direction pDirection, ControlEntity pParent, ComSession pParentComSession)
	{
		// store the HRMController application reference
		mHRMController = pHRMController;
		
		// store the direction
		mDirection = pDirection;
		
		// store the peer entity
		mPeer = null;

		// the peer priority gets initialized by a default value ("undefined")
		mPeerPriority = BullyPriority.create(this);

		// store the parent (owner) of this communication channel
		mParent = pParent;
		if (mParent == null){
			Logging.err(this, "Parent invalid");
		}
		
		mParentComSession = pParentComSession;
		if (mParentComSession == null){
			Logging.err(this, "Parent communication session is invalid");
		}
		
		// register at the parental communication session
		mParentComSession.registerComChannel(this);
		
		// register at the parent (owner)
		mParent.registerComChannel(this);
		
		Logging.log(this, "CREATED");
	}
	
	/**
	 * Constructor
	 * 
	 * @param pHRMController is the HRMController instance of this node
	 * @param pDirection the direction of the communication channel (either upward or downward)
	 * @param pParent the parent control entity
	 * @param pParentComSession is the parental comm. session
	 */
	public ComChannel(HRMController pHRMController, Direction pDirection, ControlEntity pParent, ComSession pParentComSession, ControlEntity pPeer)
	{
		this(pHRMController, pDirection, pParent, pParentComSession);
		
		// store the peer entity
		mPeer = pPeer;
	}
	
	/**
	 * Defines the HRMID of the peer which is a cluster member.
	 * 
	 * @param pHRMID the new HRMID under which the peer is addressable
	 */
	public void setPeerHRMID(HRMID pHRMID)
	{
		Logging.log(this, "Setting new peer HRMID: " + pHRMID);
		
		if((pHRMID != null) && (!pHRMID.equals(mPeerHRMID))){
			mPeerHRMID = pHRMID.clone();
			
			synchronized(mAssignedPeerHRMIDs){
				if(!mAssignedPeerHRMIDs.contains(pHRMID)){
					mAssignedPeerHRMIDs.add(pHRMID);

					int tUsedClusterAddress = pHRMID.getLevelAddress(mParent.getHierarchyLevel());
					Logging.log(this, "storePeerHRMID() stores for " + pHRMID + " the used cluster address: " + tUsedClusterAddress);
					if(tUsedClusterAddress > 0){
						synchronized (mUsedClusterAddresses) {
							if(!mUsedClusterAddresses.contains(tUsedClusterAddress)){
								mUsedClusterAddresses.add(tUsedClusterAddress);
							}
						}
					}
				}else{
					Logging.warn(this, "storePeerHRMID() skips storing the already known HRMID: " + pHRMID); 
				}
			}

			/**
			 * Add peerHRMID to peerHRMIDs
			 */
			synchronized (mPeerHRMIDs) {
				if(!mPeerHRMIDs.contains(pHRMID)){
					if(HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
						Logging.err(this, "    ..adding to stored peerHRMIDs the peerHRMID: " + getPeerHRMID());
					}
					mPeerHRMIDs.add(pHRMID);
				}
			}
			
			/**
			 * Inform the parent ClusterMember about the new peer HRMIDs
			 */
			if(mParent instanceof ClusterMember){
				eventNewPeerHRMIDs();
			}else{
				Logging.err(this, "Expected a ClusterMember as parent, parent is: " + mParent);
			}
		}else{
			//Logging.warn(this, "Ignoring set-request of peer HRMID: " + pHRMID);
		}
	}
			
	/**
	 * EVENT: neighbor HRMIDs update
	 */
	private void eventNewPeerHRMIDs()
	{
		Logging.log(this, "EVENT: new peer HRMID");
		if(mParent instanceof ClusterMember){
			ClusterMember tParentClusterMember = (ClusterMember)mParent;
			
			// get the list of neighbor HRMIDs
			LinkedList<HRMID> tNeighborHRMIDs = getPeerHRMIDs();

			Logging.log(this, "EVENT: neighbor HRMIDs for: " + this);
			Logging.log(this, "    ..neighbor HRMIDs: " + tNeighborHRMIDs);
			
			/**
			 * Continue only for base hierarchy level
			 */
			if(tParentClusterMember.getHierarchyLevel().isBaseLevel()){
				// determine the HRMID of this node for this L0 cluster
				HRMID tThisNodeClusterMemberHRMID = tParentClusterMember.getL0HRMID();

				/********************************************************************
				 * Determine the HRMID which is used as source for reported routes 
				 ********************************************************************/
				HRMID tSourceForReportedRoutes = tParentClusterMember.getHRMID(); // the HRMID of this cluster
				if((tParentClusterMember.getHierarchyLevel().isBaseLevel()) && (tThisNodeClusterMemberHRMID != null)){
					// use the L0 cluster member address instead of the cluster address 
					tSourceForReportedRoutes = tThisNodeClusterMemberHRMID;
				}

				/********************************************************************
				 * Update routing table
				 ********************************************************************/
				if((tSourceForReportedRoutes != null) && (!tSourceForReportedRoutes.isZero())){
					// backup old reported routing table
					RoutingTable tDeprecatedReportedRoutingTable = null;
					synchronized (mReportedRoutingTablePeerHRMIDs) {
						tDeprecatedReportedRoutingTable = (RoutingTable) mReportedRoutingTablePeerHRMIDs.clone();
					}
					
					// backup old locally stored routing table
					RoutingTable tDeprecatedLocalRoutingTable = null;
					synchronized (mLocalRoutingTablePeerHRMIDs) {
						tDeprecatedLocalRoutingTable = (RoutingTable) mLocalRoutingTablePeerHRMIDs.clone();
					}

					// iterate over all neighbor HRMIDs
					RoutingTable tNewReportedRoutingTable = new RoutingTable();
					RoutingTable tNewLocalRoutingTable = new RoutingTable();
					for(HRMID tNeighborHRMID : tNeighborHRMIDs){
						RoutingEntry tLocalRoutingEntry = null;
						RoutingEntry tReportedRoutingEntry = null;
						
						/**
						 * Generalize neighbor HRMID to its cluster address, depending on the locally assigned HRMID.
						 * If the local node owns 1.1.1 and 1.2.1, and the neighbor HRMID is 1.3.1, the result will be 1.3.0 because this cluster is a foreign one for the local node 
						 */ 
						HRMID tGeneralizedHRMID = mHRMController.aggregateForeignHRMID(tNeighborHRMID); 
						if(tGeneralizedHRMID.isClusterAddress()){
							// create the new routing table entry
							tLocalRoutingEntry = RoutingEntry.create(tSourceForReportedRoutes, tGeneralizedHRMID, getPeerHRMID(), 1 /* TODO */, 0 /* TODO */, 1 /* TODO */, RoutingEntry.INFINITE_DATARATE /* TODO */);
							// define the L2 address of the next hop in order to let "addHRMRoute" trigger the HRS instance the creation of new HRMID-to-L2ADDRESS mapping entry
							tLocalRoutingEntry.setNextHopL2Address(getPeerL2Address());

							// create the new reported routing table entry
							tReportedRoutingEntry = RoutingEntry.create(getPeerHRMID() /* the peer belongs to the foreign cluster */, tGeneralizedHRMID /* the foreign cluster address */, tGeneralizedHRMID, 0 /* it's a local loopback routing there */, RoutingEntry.NO_UTILIZATION, RoutingEntry.NO_DELAY, RoutingEntry.INFINITE_DATARATE);
						}else{
							// create the new routing table entry
							tLocalRoutingEntry = RoutingEntry.createRouteToDirectNeighbor(tSourceForReportedRoutes, tGeneralizedHRMID, getPeerHRMID(), 0 /* TODO */, 1 /* TODO */, RoutingEntry.INFINITE_DATARATE /* TODO */);
							// define the L2 address of the next hop in order to let "addHRMRoute" trigger the HRS instance the creation of new HRMID-to-L2ADDRESS mapping entry
							tLocalRoutingEntry.setNextHopL2Address(getPeerL2Address());

							// create the new reported routing table entry
							tReportedRoutingEntry = tLocalRoutingEntry.clone();
						}
		
						// add the entry to the reported routing table
						Logging.log(this, "   ..adding reported route: " + tReportedRoutingEntry);
						tNewReportedRoutingTable.addEntry(tReportedRoutingEntry);

						// add the entry to the reported routing table
						Logging.log(this, "   ..adding local route: " + tReportedRoutingEntry);
						tNewLocalRoutingTable.addEntry(tLocalRoutingEntry);
					}

					/**
					 * Step 1: learn new routes:
					 * 			- set the new value for reported routes based on peer HRMIDs
					 * 			- and inform the HRMController
					 * 			- update mReportedRoutingTable
					 */ 
					mReportedRoutingTablePeerHRMIDs = tNewReportedRoutingTable;
					mLocalRoutingTablePeerHRMIDs = tNewLocalRoutingTable;
					mHRMController.addHRMRoutes(mLocalRoutingTablePeerHRMIDs);
					synchronized (mReportedRoutingTable) {
						mReportedRoutingTable.addEntries(mReportedRoutingTablePeerHRMIDs);
						if(HRMConfig.DebugOutput.SHOW_REPORT_PHASE){
							Logging.log(this, "Added to reported routing table: " + mReportedRoutingTablePeerHRMIDs);
							Logging.log(this, "   ..new reported routing table: " + mReportedRoutingTable);
						}
					}
					
					/**
					 * Step 2: forget deprecated routes:
					 * 			- derive the deprecated routes
					 * 			- inform the HRS about the deprecated routing table entries
					 * 			- update mReportedRoutingTable
					 */ 
					tDeprecatedReportedRoutingTable.delEntries(tNewReportedRoutingTable);
					tDeprecatedLocalRoutingTable.delEntries(tNewLocalRoutingTable);
					mHRMController.delHRMRoutes(tDeprecatedLocalRoutingTable);
					synchronized (mReportedRoutingTable) {
						mReportedRoutingTable.delEntries(tDeprecatedReportedRoutingTable);
						if(HRMConfig.DebugOutput.SHOW_REPORT_PHASE){
							Logging.log(this, "Removed from reported routing table: " + tDeprecatedReportedRoutingTable);
							Logging.log(this, "       ..new reported routing table: " + mReportedRoutingTable);
						}
					}
				}else{
					Logging.warn(this, "eventNeighborHRMIDs() skipped because own source HRMID is zero, ignoring neighbor HRMIDs: " + tNeighborHRMIDs);
				}
			}else{
				// we are at higher hierarchy level
			}
		}else{
			Logging.err(this, "eventNewPeerHRMID() expected a ClusterMember as parent, parent is: " + mParent);
		}
	}

	/**
	 * Returns the reported routing table
	 * 
	 * @return the reported routing table
	 */
	public RoutingTable getReportedRoutingTable()
	{
		RoutingTable tResult = null;
		
		synchronized (mReportedRoutingTable) {
			if(HRMConfig.DebugOutput.SHOW_REPORT_PHASE){
				//Logging.err(this, "Reporting routing table: " + mReportedRoutingTable);
			}
			tResult = (RoutingTable) mReportedRoutingTable.clone();
		}
		
		return tResult;
	}
	
	/**
	 * Determines the address of the peer (e.g., a cluster member).
	 * 
	 * @return the HRMID of the peer or "null"
	 */
	public HRMID getPeerHRMID()
	{
		return mPeerHRMID;
	}
	
	/**
	 * Returns the direction of the communication  channel
	 * 
	 * @return the direction
	 */
	public Direction getDirection()
	{
		return mDirection;
	}
	
	/**
	 * Returns the peer entity
	 * 
	 * @return peer entity
	 */
	public ControlEntity getPeer()
	{
		return mPeer;
	}
	
	/**
	 * EVENT: established
	 */
	public synchronized void eventEstablished()
	{
		Logging.log(this, "EVENT: established");
		
		mChannelState = ChannelState.OPEN;
	}
	
	/**
	 * Returns if the comm. channel is open
	 * 
	 * @return true or false
	 */
	public boolean isOpen()
	{
		return (mChannelState == ChannelState.OPEN);
	}
	
	/**
	 * Returns the state of the channel
	 * 
	 * @return the channel state
	 */
	public ChannelState getState()
	{
		return mChannelState;
	}
	
	/**
	 * Handles a SignalingMessageHrm packet.
	 * 
	 * @param pSignalingMessageHrmPacket the packet
	 */
	private void getPeerHRMIDFromHRMSignalingMessage(SignalingMessageHrm pSignalingMessageHrmPacket)
	{
		// can we learn the peer's HRMID from the packet?
		if (pSignalingMessageHrmPacket.getSenderName() instanceof HRMID){
			// get the HRMID of the peer
			HRMID tPeerHRMID = (HRMID)pSignalingMessageHrmPacket.getSenderName();
			
			// update peer's HRMID
			setPeerHRMID(tPeerHRMID);
		}		
	}

	/**
	 * @param pTopologyReportPacket
	 */
	private void eventReceivedTopologyReport(TopologyReport pTopologyReportPacket)
	{
		if (HRMConfig.DebugOutput.SHOW_REPORT_PHASE){
			Logging.log(this, "REPORT PHASE DATA received from \"" + getPeerHRMID() + "\", DATA: " + pTopologyReportPacket);
		}
	
		if(mParent instanceof Cluster){
			Cluster tParentCluster = (Cluster)mParent;
			
			/**
			 * Record the routing report
			 */
			if(HRMConfig.DebugOutput.SHOW_REPORT_PHASE){
				Logging.err(this, "   ..got routing report: " + pTopologyReportPacket.getRoutes());
			}
			synchronized (mReportedRoutingTable) {
				mReportedRoutingTable = pTopologyReportPacket.getRoutes();
			}

			/**
			 * Trigger: inform the cluster about the new routing report
			 */
			tParentCluster.eventTopologyReport(pTopologyReportPacket);
		}else{
			Logging.err(this, "eventReceivedTopologyReport() expected a Cluster as parent, parent is: " + mParent);
		}
	}

	/**
	 * Handles a RoutingInformation packet.
	 * 
	 * @param pRoutingInformationPacket the packet
	 */
	private void eventReceivedRoutingInformation(RoutingInformation pRoutingInformationPacket)
	{
		if (HRMConfig.DebugOutput.SHOW_SHARE_PHASE){
			Logging.log(this, "SHARE PHASE DATA received from \"" + getPeerHRMID() + "\", DATA: " + pRoutingInformationPacket);
		}
		
		//TODO: event in coord./cluster aufrufen
		
		for (RoutingEntry tEntry : pRoutingInformationPacket.getRoutes()){
			if (HRMConfig.DebugOutput.SHOW_SHARE_PHASE)
				Logging.log(this, "      ..found route: " + tEntry);
			
			mHRMController.addHRMRoute(tEntry);
		}
	}

	/**
	 * Handles a AnnounceHRMIDs packet.
	 * 
	 * @param pAnnounceHRMIDsPacket the packet
	 */
	@SuppressWarnings("unchecked")
	private void eventReceivedAnnounceHRMIDs(AnnounceHRMIDs pAnnounceHRMIDsPacket)
	{
		if(HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
			Logging.log(this, "Received announced peer HRMIDs: " + pAnnounceHRMIDsPacket.getHRMIDs());
		}
				
		/**
		 * Reset peerHRMIDs
		 */
		synchronized (mPeerHRMIDs) {
			mPeerHRMIDs = (LinkedList<HRMID>) pAnnounceHRMIDsPacket.getHRMIDs().clone();
		}
		
		/**
		 * Add peerHRMID to peerHRMIDs
		 */
		synchronized (mPeerHRMIDs) {
			if(!mPeerHRMIDs.contains(getPeerHRMID())){
				if(HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
					Logging.err(this, "    ..adding to stored peerHRMIDs the peerHRMID: " + getPeerHRMID());
				}
				mPeerHRMIDs.add(getPeerHRMID());
			}
		}

		/**
		 * Inform the parent ClusterMember about the new peer HRMIDs
		 */
		if(mParent instanceof ClusterMember){
			eventNewPeerHRMIDs();
		}else{
			Logging.err(this, "eventReceivedAnnounceHRMIDs() expected a ClusterMember as parent, parent is: " + mParent);
		}
	}

	/**
	 * Updates the Bully priority of the peer.
	 * 
	 * @param pPeerPriority the Bully priority
	 */
	public boolean setPeerPriority(BullyPriority pPeerPriority)
	{
		boolean tResult = false;
		
		if (pPeerPriority == null){
			Logging.warn(this, "Trying to set a NULL POINTER as peer priority, ignoring this request, current priority: " + getPeerPriority());
			return false;
		}

		if(pPeerPriority.getValue() < 0){
			throw new RuntimeException("Invalid priority update from " + mPeerPriority.getValue() + " to " + pPeerPriority.getValue());
		}
		
		// is the new value equal to the old one?
		if(!pPeerPriority.equals(mPeerPriority)){
			// get the current simulation time
			double tNow = mHRMController.getSimulationTime();
			
			Logging.log(this, "Updating peer priority from " + mPeerPriority.getValue() + " to " + pPeerPriority.getValue() + ", last update was " + (tNow - mPeerPriorityTimestampLastUpdate) + " seconds before");
			
			// update the freshness of the peer priority
			mPeerPriorityTimestampLastUpdate = tNow;
	
			// update the peer Bully priority itself
			mPeerPriority = pPeerPriority;
			
			// we have a new priority
			tResult = true;
		}
		
		return tResult;
	}

	/**
	 * Returns the Bully priority of the communication peer
	 * 
	 * @return the Bully priority
	 */
	public BullyPriority getPeerPriority()
	{
		if (mPeerPriority == null){
			mPeerPriority = BullyPriority.create(this);
		}
			
		//TODO: getPeerPriorityFreshness() integrieren und einen Timeout bauen, so das danach nur null geliefert wird
		
		return mPeerPriority;
	}

	/**
	 * Returns the L2Address of the peer
	 * 
	 * @return the L2Address
	 */
	public L2Address getPeerL2Address()
	{
		return mParentComSession.getPeerL2Address();
	}
	
	/**
	 * Returns true if this comm. channel leads to a remote node
	 *  
	 * @return true or false
	 */
	public boolean toRemoteNode()
	{
		return (!mHRMController.getNodeL2Address().equals(getPeerL2Address()));
	}

	/**
	 * Returns true if this comm. channel leads to the local node
	 *  
	 * @return true or false
	 */
	public boolean toLocalNode()
	{
		return (mHRMController.getNodeL2Address().equals(getPeerL2Address()));
	}
	
	/**
	 * Returns the parental control entity
	 * 
	 * @return the parental control entity
	 */
	public ControlEntity getParent()
	{
		return mParent;
	}
	
	/**
	 * Returns the parental communication session
	 * 
	 * @return the parental communication session
	 */
	public ComSession getParentComSession()
	{
		return mParentComSession;
	}
	
	/**
	 * Returns the route to the peer
	 * 
	 * @return the route to the peer
	 */
	public Route getRouteToPeer()
	{
		return mParentComSession.getRouteToPeer();
	}

	/**
	 * Count the amount of sent packets
	 * 
	 * @return the counter
	 */
	public int countSentPackets()
	{
		return mSentPackets;
	}
	
	/**
	 * Count the amount of received packets
	 * 
	 * @return the counter
	 */
	public int countReceivedPackets()
	{
		return mReceivedPackets;
	}

	/**
	 * Returns a storage with all sent/received packets
	 * 
	 * @return the packet I/O storage
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<ComChannelPacketMetaData> getSeenPackets()
	{
		LinkedList<ComChannelPacketMetaData> tResult = null;
		
		synchronized (mPackets) {
			tResult = (LinkedList<ComChannelPacketMetaData>) mPackets.clone();
		}

		return tResult;
	}
	
	/**
	 * Stores a packet for delayed debugging
	 * 
	 * @param pPacket the packet
	 */
	private void storePacket(SignalingMessageHrm pPacket, boolean pWasSent)
	{
		synchronized (mPackets) {
			if (pWasSent){
				/**
				 * count the packets
				 */
				mSentPackets++;
			}else{
				/**
				 * count the packets
				 */
				mReceivedPackets++;
			}
			// limit the storage size
			while(mPackets.size() > HRMConfig.DebugOutput.COM_CHANNELS_MAX_PACKET_STORAGE_SIZE){
				mPackets.removeFirst();
			}
			
			// add the packet to the storage
			mPackets.add(new ComChannelPacketMetaData(pPacket, pWasSent, mHRMController.getSimulationTime()));			
		}
	}
	/**
	 * Sends a packet to the peer
	 * 
	 * @param pPacket the packet
	 * 
	 * @return true if successful, otherwise false
	 */
	public boolean sendPacket(SignalingMessageHrm pPacket)
	{
		if(mChannelState != ChannelState.CLOSED /* at least, "half_open" is needed */){
			// create destination description
			ClusterName tDestinationClusterName = getRemoteClusterName();
			
			if (tDestinationClusterName != null){
				if (HRMConfig.DebugOutput.SHOW_SENT_CHANNEL_PACKETS){
					Logging.log(this, "SENDING DATA " + pPacket + " to destination " + tDestinationClusterName);
				}
		
				// create the source description
				ClusterName tSourceClusterName = new ClusterName(mHRMController, getParent().getHierarchyLevel(), getParent().getClusterID(), getParent().getCoordinatorID());
				
				// add source route entry
				pPacket.addSourceRoute("[S]: " + this.toString());
				
				// create the Multiplex-Header
				MultiplexHeader tMultiplexHeader = new MultiplexHeader(tSourceClusterName, tDestinationClusterName, pPacket);
					
				/**
				 * Store the packet 
				 */
				storePacket(pPacket, true);

				// send the final packet (including multiplex-header)
				return getParentComSession().write(tMultiplexHeader);
			}else{
				Logging.warn(this, "Destination is still undefined, skipping packet payload " + pPacket);
				return false;
			}
		}else{
			Logging.err(this, "sendPacket() found closed channel, dropping packet: " + pPacket);
			return false;
		}
	}

	/**
	 * Sets a new name for the remote cluster
	 * 
	 * @param pClusterName the new name for the remote cluster
	 */
	public void setRemoteClusterName(ClusterName pClusterName)
	{
		Logging.log(this, "Setting remote/peer cluster name: " + pClusterName);
		
		mRemoteClusterName = pClusterName;
	}

	/**
	 * Returns the name of the remote cluster
	 *  
	 * @return the name of the remote cluster
	 */
	public ClusterName getRemoteClusterName()
	{
		return mRemoteClusterName;
	}
	
	/**
	 * Sends "AssignHRMID"
	 * 
	 * @param pHRMID the HRMID which is to be assigned
	 */
	public void signalAssignHRMID(HRMID pHRMID)
	{
		HRMID tSenderHRMID = mParent.getHRMID();
		if(mParent instanceof ClusterMember){
			ClusterMember tParentClusterMember = (ClusterMember)mParent;
			if(tParentClusterMember.getL0HRMID() != null){
				tSenderHRMID = tParentClusterMember.getL0HRMID();
			}
		}
		// create new AssignHRMID packet for the cluster member
		AssignHRMID tAssignHRMIDPacket = new AssignHRMID(tSenderHRMID, getPeerHRMID(), pHRMID);
		// send the packet
		sendPacket(tAssignHRMIDPacket);
	}

	/**
	 * Revokes all formerly assigned HRMIDs
	 */	
	public void signalRevokeAssignedHRMIDs()
	{
		// debug output
		LinkedList<HRMID >tPeerHRMIDs = getPeerHRMIDs();
		if (tPeerHRMIDs.size() > 0){
			Logging.log(this, "Revoking assigned HRMIDs...");
			int i = 0;
			for(HRMID tHRMID : tPeerHRMIDs){				
				synchronized (mAssignedPeerHRMIDs) {
					mAssignedPeerHRMIDs.remove(tHRMID);
				}
				synchronized (mPeerHRMIDs) {
					mPeerHRMIDs.remove(tHRMID);
				}

				Logging.log(this, "    ..[" + i + "]: " + tHRMID);
				i++;
			}

			/**
			 * Revoke the HRMIDs from the peer
			 */
			// create the packet
			RevokeHRMIDs tRevokeHRMIDsPacket = new RevokeHRMIDs(mHRMController.getNodeName(), getPeerHRMID(), tPeerHRMIDs);
			// send the packet
			sendPacket(tRevokeHRMIDsPacket);
		}
	}

	/**
	 * Returns a list of used cluster addresses
	 * 
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<Integer> getUsedClusterAddresses()
	{
		LinkedList<Integer> tResult = null;
		
		synchronized (mUsedClusterAddresses) {
			tResult = (LinkedList<Integer>) mUsedClusterAddresses.clone();
		}
		
		return tResult;
	}
	
	/**
	 * Returns the list of known assigned peer HRMIDs
	 * 
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<HRMID> getAssignedPeerHRMIDs()
	{
		LinkedList<HRMID> tResult = null;
		
		synchronized (mAssignedPeerHRMIDs) {
			tResult = (LinkedList<HRMID>) mAssignedPeerHRMIDs.clone();
		}
		
		return tResult; 
	}

	/**
	 * Returns the list of known peer HRMIDs
	 * 
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<HRMID> getPeerHRMIDs()
	{
		LinkedList<HRMID> tResult = null;
		
		synchronized (mPeerHRMIDs) {
			tResult = (LinkedList<HRMID>) mPeerHRMIDs.clone();
		}
		
		return tResult; 
	}
	
	/**
	 * Acknowledges a RequestClusterMembership packet
	 * 
	 * @param pSource the source of the acknowledgment (e.g., a coordinator description)
	 */
	public void signalRequestClusterMembershipAck(ClusterName pSource)
	{
		// create the packet
		RequestClusterMembershipAck tRequestClusterMembershipAckPacket = new RequestClusterMembershipAck(mHRMController.getNodeName(), getPeerHRMID(), pSource);
		// send the packet
		Logging.log(this, "Acknowledging cluster membership: " + tRequestClusterMembershipAckPacket + " for " + getRemoteClusterName());
		sendPacket(tRequestClusterMembershipAckPacket);
	}

	/**
	 * Closes the comm. channel
	 */
	public synchronized void closeChannel()
	{
		Logging.log(this, "Closing this channel");
		if(isOpen()){
			/**
			 * Inform the peer
			 */
			if(mParent instanceof Cluster){
				Cluster tParentCluster = (Cluster)mParent;
				
				/**
				 * Send "InformClusterMembershipCanceled" along the comm. channel
				 */
				InformClusterMembershipCanceled tInformClusterMembershipCanceled = new InformClusterMembershipCanceled(mHRMController.getNodeName(), mHRMController.getNodeName(), tParentCluster.createClusterName(), getRemoteClusterName());
			    Logging.log(this, "       ..sending membership canceled: " + tInformClusterMembershipCanceled);
			    sendPacket(tInformClusterMembershipCanceled);
			}else if(mParent instanceof ClusterMember){
				/**
				 * Send: "Leave" to all superior clusters
				 */
				InformClusterLeft tInformClusterLeft = new InformClusterLeft(mHRMController.getNodeName(), getPeerHRMID(), null, null);
			    Logging.log(this, "       ..sending cluster left: " + tInformClusterLeft);
				sendPacket(tInformClusterLeft);
			}

			/**
			 * Change the channel state
			 */
			mChannelState = ChannelState.CLOSED;
		}else{
		    Logging.log(this, "       ..channel wasn't established");
		}
		
		// unregister from the parent comm. session
		mParentComSession.unregisterComChannel(this);
	}
	
	/**
	 * Main packet receive function. It is used by the parent ComSession.
	 *  
	 * @param pPacket the packet
	 * 
	 * @return true
	 */
	public boolean receivePacket(SignalingMessageHrm pPacket)
	{
		/**
		 * Store the packet in queue
		 */
		synchronized (mPacketQueue) {
			mPacketQueue.add(pPacket);
			
			mHRMController.notifyPacketProcessor(this);
		}
		
		return true;
	}

	/**
	 * Processes one packet, triggered by packet processor (HRMController)
	 */
	public void processOnePacket()
	{
		SignalingMessageHrm tNextPacket = null;
		
		synchronized (mPacketQueue) {
			if(mPacketQueue.size() > 0){
				tNextPacket = mPacketQueue.removeFirst();
			}
		}
		
		if(tNextPacket != null){
			if(getParent().isThisEntityValid()){
				handlePacket(tNextPacket);
			}else{
				Logging.warn(this, "Parent control entity is already invalidated, dropping received packet: " + tNextPacket);
			}
		}else{
			Logging.err(this, "Cannot process an invalid packet");
		}
	}
	
	/**
	 * Processes one packet 
	 * 
	 * @param pPacket the packet
	 * 
	 * @return true if everything worked fine
	 */
	@SuppressWarnings("unused")
	private boolean handlePacket(SignalingMessageHrm pPacket)
	{
		/**
		 * Store the packet 
		 */
		storePacket(pPacket, false);
		
		if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS){
			Logging.log(this, "RECEIVED DATA (" + pPacket + ") from \"" + getPeerL2Address() + "/" + getPeerHRMID() + "\": " + pPacket);
		}
			
		/*
		 * Invalid data
		 */
		if(pPacket == null) {
			Logging.err(this, "Received invalid null pointer as data");
			return false;
		}

		/**
		 * HRM signaling message
		 */
		if (pPacket instanceof SignalingMessageHrm){
			// cast to a SignalingMessageHrm signaling message
			SignalingMessageHrm tSignalingMessageHrmPacket = (SignalingMessageHrm)pPacket;
		
			// process SignalingMessageHrm message
			getPeerHRMIDFromHRMSignalingMessage(tSignalingMessageHrmPacket);
			
			// add source route entry
			tSignalingMessageHrmPacket.addSourceRoute("[R]: " + this.toString());

			//HINT: don't return here because we are still interested in the more detailed packet data from derived packet types!
		}
		
		/**
		 * Bully signaling message:
		 * 			Cluster ==> ClusterMember
		 * 			ClusterMember ==> Cluster
		 */
		if (pPacket instanceof SignalingMessageBully) {
			// cast to a Bully signaling message
			SignalingMessageBully tBullyMessage = (SignalingMessageBully)pPacket;

			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
				Logging.log(this, "RECEIVED BULLY MESSAGE " + tBullyMessage.getClass().getSimpleName());

			// the packet is received by a cluster
			//HINT: this is only possible at base hierarchy level
			if (mParent instanceof ClusterMember){
				ClusterMember tParentClusterProxy = (ClusterMember)mParent;
				
				if (tParentClusterProxy.getElector() != null){
					tParentClusterProxy.getElector().handleElectionMessage(tBullyMessage, this);
				}else{
					Logging.warn(this, "Elector is still invalid");
				}
				return true;
			}		

			// the packet is received by a coordinator
			if (mParent instanceof Coordinator){
				Coordinator tCoordinator = (Coordinator)mParent;
				
				tCoordinator.getCluster().getElector().handleElectionMessage(tBullyMessage, this);
				
				return true;
			}

			Logging.warn(this, "IGNORING THIS MESSAGE: " + tBullyMessage);

			return true;
		}

		/**
		 * TopologyReport
		 */
		if (pPacket instanceof TopologyReport){
			// cast to a TopologyReport signaling message
			TopologyReport tTopologyReportPacket = (TopologyReport)pPacket;

			if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
				Logging.log(this, "TOPOLOGY_REPORT-received from \"" + getPeerHRMID() + "\": " + tTopologyReportPacket);

			// process Bully message
			eventReceivedTopologyReport(tTopologyReportPacket);
			
			return true;
		}

		/**
		 * RoutingInformation:
		 */
		if (pPacket instanceof RoutingInformation){
			// cast to a RoutingInformation signaling message
			RoutingInformation tRoutingInformationPacket = (RoutingInformation)pPacket;

			if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
				Logging.log(this, "ROUTING_INFORMATION-received from \"" + getPeerHRMID() + "\": " + tRoutingInformationPacket);

			// process Bully message
			eventReceivedRoutingInformation(tRoutingInformationPacket);
			
			return true;
		}
		
		/**
		 * AssignHRMID:
		 * 			Coordinator (via Cluster) ==> all inferior local/remote ClusterMember
		 */
		if(pPacket instanceof AssignHRMID) {
			AssignHRMID tAssignHRMIDPacket = (AssignHRMID)pPacket;

			if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
				Logging.log(this, "ASSIGN_HRMID-received from \"" + getPeerHRMID() + "\" assigned HRMID: " + tAssignHRMIDPacket.getHRMID().toString());

			HRMID tAssignedHRMID = tAssignHRMIDPacket.getHRMID();
			
			// let the coordinator process the HRMID assignment
			getParent().eventAssignedHRMID(tAssignedHRMID);
			
			if(getParent() instanceof CoordinatorAsClusterMember){
				CoordinatorAsClusterMember tCoordinatorAsClusterMember = (CoordinatorAsClusterMember)getParent();
				
				/**
				 * Automatic address distribution via the coordinator
				 */
				// we should automatically continue the address distribution?
				if (HRMConfig.Addressing.ASSIGN_AUTOMATICALLY){
					// does the CoordinatorAsClusterMember belong to an active cluster?
					if(isLinkActive()){
						if(tAssignedHRMID != null){
							Coordinator tCoordinator = tCoordinatorAsClusterMember.getCoordinator();
							if (HRMConfig.DebugOutput.SHOW_DEBUG_ADDRESS_DISTRIBUTION){
								Logging.log(this, "     ..got new HRMID for CoordinatorAsClusterMember: " + tCoordinatorAsClusterMember);
								Logging.log(this, "     ..continuing the address distribution process via the coordinator: " + tCoordinator);
							}
							tCoordinator.eventAssignedHRMID(tAssignedHRMID);
						}			
					}

				}
				
			}
			
			return true;
		}

		/**
		 * AnnounceHRMIDs
		 */
		if (pPacket instanceof AnnounceHRMIDs){
			// cast to a RoutingInformation signaling message
			AnnounceHRMIDs tAnnounceHRMIDsPacket = (AnnounceHRMIDs)pPacket;

			if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
				Logging.log(this, "ANNOUNCE_HRMIDS-received from \"" + getPeerHRMID() + "\" revoked HRMIDs: " + tAnnounceHRMIDsPacket.getHRMIDs().toString());

			// process Bully message
			eventReceivedAnnounceHRMIDs(tAnnounceHRMIDsPacket);
			
			return true;
		}
		
		/**
		 * RevokeHRMIDs:
		 * 			Coordinator (via Cluster) ==> all inferior local/remote ClusterMember
		 */
		if(pPacket instanceof RevokeHRMIDs){
			RevokeHRMIDs tRevokeHRMIDsPacket = (RevokeHRMIDs)pPacket;

			if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
				Logging.log(this, "REVOKE_HRMIDS-received from \"" + getPeerHRMID() + "\" revoked HRMIDs: " + tRevokeHRMIDsPacket.getHRMIDs().toString());

			// revoke the HRMIDs step-by-step
			for(HRMID tHRMID: tRevokeHRMIDsPacket.getHRMIDs()){
				getParent().eventRevokedHRMID(this, tHRMID);
			}
			
			return true;
		}
		
		/**
		 * RequestClusterMembershipAck:
		 * 			ClusterMember(CoordinatorAsClusterMember) ==> Cluster
		 *  
		 */
		if(pPacket instanceof RequestClusterMembershipAck) {
			RequestClusterMembershipAck tRequestClusterMembershipAckPacket = (RequestClusterMembershipAck)pPacket;

			if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
				Logging.log(this, "REQUEST_CLUSTER_MEMBERSHIP_ACK-received from \"" + getPeerHRMID() + "\"");

			// is the parent a coordinator or a cluster?
			if (getParent() instanceof Cluster){
				Cluster tCluster = (Cluster)getParent();
				
				// trigger event "cluster member joined"
				tCluster.eventClusterMemberJoined(this);		
			}else{
				Logging.err(this, "Expected a Cluster object as parent for processing RequestClusterMembershipAck data but parent is " + getParent());
			}
			
			return true;
		}
		
		/**
		 * InformClusterLeft:
		 * 			ClusterMember ==> Cluster
		 */
		if(pPacket instanceof InformClusterLeft) {
			InformClusterLeft tInformClusterLeftPacket = (InformClusterLeft)pPacket;

			if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
				Logging.log(this, "INFORM_CLUSTER_LEFT-received from \"" + getPeerHRMID() + "\"");

			if(!isOpen()){
				Logging.warn(this, "Received InformClusterLeft in state " + mChannelState.toString() + ": " + tInformClusterLeftPacket);
			}
			
			// no further transmissions
			mChannelState = ChannelState.CLOSED;

			// is the parent a coordinator or a cluster?
			if (getParent() instanceof Cluster){
				Cluster tCluster = (Cluster)getParent();
				
				// trigger event "cluster member joined"
				tCluster.eventClusterMemberLost(this);		
			}else{
				Logging.err(this, "Expected a Cluster object as parent for processing LeaveCluster data but parent is " + getParent());
			}

			return true;
		}
		
		/**
		 * InformClusterMembershipCanceled:
		 * 			Cluster ==> ClusterMember
		 */
		if(pPacket instanceof InformClusterMembershipCanceled) {
			InformClusterMembershipCanceled tInformClusterMembershipCanceledPacket = (InformClusterMembershipCanceled)pPacket;

			if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
				Logging.log(this, "INFORM_CLUSTER_MEMBERSHIP_CANCELED-received from \"" + getPeerHRMID() + "\"");

			// no further transmissions
			mChannelState = ChannelState.CLOSED;
			
			// is the parent a coordinator or a cluster?
			if (getParent() instanceof ClusterMember){
				ClusterMember tClusterMember = (ClusterMember)getParent();
				
				// trigger event "cluster member joined"
				tClusterMember.eventClusterMemberRoleInvalid(this);		
			}else{
				Logging.err(this, "Expected a ClusterMember object as parent for processing LeaveCluster data but parent is " + getParent());
			}
			
			return true;
		}
		
		/**
		 * AnnounceCluster
		 * 			Coordinator (via Cluster) ==> all inferior local/remote ClusterMember
		 */
		if(pPacket instanceof AnnounceCoordinator) {
			AnnounceCoordinator tAnnounceClusterPacket = (AnnounceCoordinator)pPacket;

			if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
				Logging.log(this, "ANNOUNCE_COORDINATOR-received from \"" + getPeerHRMID() + "\", announcement is: " + tAnnounceClusterPacket);
		
			getParent().eventCoordinatorAnnouncement(this, tAnnounceClusterPacket);
			
			return true;
		}
		
		/**
		 * InvalidCoordinator
		 * 			Coordinator (via Cluster) ==> all inferior local/remote ClusterMember
		 */
		if(pPacket instanceof InvalidCoordinator) {
			InvalidCoordinator tInvalidCoordinatorPacket = (InvalidCoordinator)pPacket;

			if (HRMConfig.DebugOutput.SHOW_RECEIVED_CHANNEL_PACKETS)
				Logging.log(this, "INVALID_COORDINATOR-received from \"" + getPeerHRMID() + "\", invalidation is: " + tInvalidCoordinatorPacket);
		
			getParent().eventCoordinatorInvalidation(this, tInvalidCoordinatorPacket);
			
			return true;
		}

		Logging.warn(this, ">>>>>>>>>>>>> Found unsupported packet: " + pPacket);
		return true;
	}
	
	/**
	 * @param pRoutingEntryForMember
	 */
	public boolean storeRouteForPeer(RoutingEntry pRoutingEntryForMember)
	{
		/**
		 * Store the routing entry in the routing table
		 */
		boolean tResult = mSharedRoutingData.addEntry(pRoutingEntryForMember);
		
		return tResult;
	}

	/**
	 * SEND: RoutingInformation to a cluster member
	 */
	public void distributeRoutingInformation()
	{
		// create new RoutingInformation packet for the cluster member
		RoutingInformation tRoutingInformationPacket = new RoutingInformation(mHRMController.getNodeName(), getPeerHRMID());
		
		// fill the RoutingInformation packet with routing entries
		synchronized (mSharedRoutingData) {
			for(RoutingEntry tEntry : mSharedRoutingData){
				tRoutingInformationPacket.addRoute(tEntry.clone());
			}
		}
		
		// send the packet
		sendPacket(tRoutingInformationPacket);
	}

	/**
	 * (De-)activates the HRM link.
	 * 
	 * @param pState the new state
	 * @param pCause describes the cause for this change
	 */
	public void setLinkActivation(boolean pState, String pCause)
	{
		Logging.log(this, "Updating link activation from: " + mLinkActivation + " to: " + pState);

		if(getParent() instanceof CoordinatorAsClusterMember){
			CoordinatorAsClusterMember tCoordinatorAsClusterMember = (CoordinatorAsClusterMember)getParent();
			
			HRMID tHRMID = tCoordinatorAsClusterMember.getHRMID();
			
			/**
			 * State transition from "true" to "false"? -> do the following
			 *   	1.) unregister the local HRMID
			 */
			if ((isLinkActive()) && (!pState)){
				/**
				 * Unregister the local HRMID
				 */
				if((tHRMID != null) && (!tHRMID.isZero())){
					mHRMController.unregisterHRMID(tCoordinatorAsClusterMember, tHRMID);
				}
			}

			/**
			 * State transition from "false" to "true"? -> do the following
			 *   	1.) update coordinator address by triggering "assigned HRMID"
			 */
			if ((!isLinkActive()) && (pState)){
				/**
				 * Unregister the local HRMID
				 */
				if(tHRMID != null){
					/**
					 * Trigger: "assigned HRMID"
					 */
					tCoordinatorAsClusterMember.getCoordinator().eventAssignedHRMID(tHRMID);
				}
			}
		}

		mLinkActivation = pState;
		
		mDesccriptionLinkActivation += "\n - [" +pState +"] <== " + pCause;
	}
	
	/**
	 * Returns a description about all link activation changes
	 * 
	 * @return the description about all changes
	 */
	public String getDescriptionLinkActivation()
	{
		return mDesccriptionLinkActivation;
	}
	
	/**
	 * Returns true if the parent and the peer use actively this link for HRM topology distribution (e.g., a cluster member is link to a cluster)
	 * 
	 * @return true or false
	 */
	public boolean isLinkActive()
	{
		return mLinkActivation;
	}	
	
	/**
	 * Returns a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return getClass().getSimpleName() + "@" + mParent.toString() + "(Peer="+ (getPeerL2Address() != null ? (getPeer() != null ? getPeer() : getPeerL2Address()) + " <#> " + getPeerHRMID() : "") + ")";
	}
}
