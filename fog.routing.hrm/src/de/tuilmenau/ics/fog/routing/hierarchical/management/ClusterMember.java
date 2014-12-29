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

import de.tuilmenau.ics.fog.bus.Bus;
import de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable;
import de.tuilmenau.ics.fog.packets.hierarchical.PingPeer;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.packets.hierarchical.routing.RouteReport;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.AnnounceCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.InvalidCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.SignalingMessageHierarchyUpdate;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingEntry;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingTable;
import de.tuilmenau.ics.fog.routing.hierarchical.election.ElectionPriority;
import de.tuilmenau.ics.fog.routing.hierarchical.election.Elector;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.NetworkInterface;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class represents a cluster member (can also be a cluster manager).
 */
public class ClusterMember extends ControlEntity
{
	private static final long serialVersionUID = -8746079632866375924L;

	/**
	 * Stores the L2 address of the node where the coordinator of the addressed cluster is located
	 */
	private L2Address mCoordinatorNodeL2Address = null;
	
	/**
	 * Stores the elector which is responsible for coordinator elections for this cluster.
	 */
	protected Elector mElector = null;

	/**
	 * Returns the cluster activation
	 */
	private boolean mClusterHasValidCoordinator = false;
	
	/**
	 * Stores the network interface for base hierarchy level
	 */
	private NetworkInterface mBaseHierarchyLevelNetworkInterface = null;

	/**
	 * Stores the HRMID which is assigned to this node.
	 * This variable is only used for L0.
	 */
	private HRMID mAssignedL0HRMID = null;

	/**
	 * Stores the currently reported routing table based on setL0HRMID calls
	 */
	private RoutingTable mReportedRoutingTableL0HRMID = new RoutingTable();
	
	/**
	 * Stores if the warning about invalid L0 neighborhood detection was already sent to debug output
	 */
	private boolean mWarningDetectLocalSiblingsLocalL0HRMIDInvalid = false;
	
	/**
	 * Stores for an L0 cluster if the network should be split towards the peers
	 */
	private boolean mEnforceL0AsSplit = false;
			
	/**
	 * only for simple L0 ClusterMember: stores the time when the last full routing table was reported to the superior L0 coordinator
	 */
	private double mL0TimeLastCompleteReportedRoutingTable = 0;
	
	/**
	 * only for simple L0 ClusterMember: stores always the last reported routing table, which was sent towards the superior L0 coordinator
	 */
	private RoutingTable mL0LastSentReportedRoutingTable = new RoutingTable();

	/**
	 * only for simple L0 ClusterMember: stores if the last reported routing table was sent during an unstable hierarchy
	 */
	private boolean mL0LastReportedRoutingTableWasDuringUnstableHierarchy = true;

	/**
	 * Constructor
	 *  
	 * @param pHRMController the local HRMController instance
	 * @param pClusterID the unique ID of this cluster
	 * @param pHierarchyLevel the hierarchy level
	 * @param pCoordinatorID the unique coordinator ID for this cluster
	 * @param pCoordinatorNodeL2Address the L2 address of the node where the coordinator of this cluster is located
	 */
	public ClusterMember(HRMController pHRMController, Long pClusterID, HierarchyLevel pHierarchyLevel, long pCoordinatorID, L2Address pCoordinatorNodeL2Address)
	{	
		super(pHRMController, pClusterID, pHierarchyLevel, pCoordinatorID);

		// store the L2 address of the node where the coordinator is located
		mCoordinatorNodeL2Address = pCoordinatorNodeL2Address;
	}

	/**
	 * Factory function
	 *  
	 * @param pHRMController the local HRMController instance
	 * @param pClusterName a ClusterName which includes the hierarchy level, the unique ID of this cluster, and the unique coordinator ID
	 * @param pClusterID the unique ID of this cluster
	 * @param pClusterHeadNodeL2Address the L2 address of the node where the cluster manager is located
	 */
	public static ClusterMember create(HRMController pHRMController, ClusterName pClusterName, L2Address pClusterHeadNodeL2Address)
	{	
		ClusterMember tResult = new ClusterMember(pHRMController, pClusterName.getClusterID(), pClusterName.getHierarchyLevel(), pClusterName.getCoordinatorID(), pClusterHeadNodeL2Address);
		
		Logging.log(tResult, "\n\n\n################ CREATED CLUSTER MEMBER at hierarchy level: " + (tResult.getHierarchyLevel().getValue()));

		// creates new elector object, which is responsible for election processes
		tResult.mElector = new Elector(pHRMController, tResult);

		// register at HRMController's internal database
		pHRMController.registerClusterMember(tResult);

		return tResult;
	}

	/**
	 * Detect the network interface of the L0 ClusterMember
	 */
	public void detectNetworkInterface()
	{
		if(getHierarchyLevel().isBaseLevel())  
		{
			if(!(this instanceof Cluster)){
				if(getBaseHierarchyLevelNetworkInterface() == null){

					/**
					 * The following is FoGSiEm specific and allows for an easy detection of the network interface for a ClusterMember
					 */
					ComChannel tThisClusterChannelToManager = getComChannelToClusterManager();
//					Logging.err(this, "      ..channel to cluster manager: " + tThisClusterChannelToHead);
					if(tThisClusterChannelToManager != null){
						L2Address tThisClusterChannelToManagerL2Address = tThisClusterChannelToManager.getPeerL2Address();
//						Logging.err(this, "        ..peer L2Address: " + tThisClusterChannelToHeadL2Address);
						if(tThisClusterChannelToManagerL2Address != null){
							LinkedList<Cluster> tClusters = mHRMController.getAllClusters(0);
							for(ClusterMember tCluster : tClusters){
//								Logging.err(this, "   ..found other L0 Cluster: " + tCluster);
								LinkedList<ComChannel> tThisClusterChannels = tCluster.getComChannels();
								for(ComChannel tThisClusterChannel : tThisClusterChannels){
//									Logging.err(this, "      ..channel of this cluster: " + tThisClusterChannel);
									L2Address tThisClusterChannelPeerL2Address = tThisClusterChannel.getPeerL2Address();
//									Logging.err(this, "        ..peer L2Address: " + tThisClusterChannelPeerL2Address);
									if(tThisClusterChannelPeerL2Address != null){
										if(tThisClusterChannelPeerL2Address.equals(tThisClusterChannelToManagerL2Address)){
											/**
											 * We found a Cluster, which has a comm. channel to the same peer node which is also the peer node of this ClusterMember
											 */
											if(tCluster.getBaseHierarchyLevelNetworkInterface() != null){
												// update the network interface
												setBaseHierarchyLevelNetworkInterface(tCluster.getBaseHierarchyLevelNetworkInterface());
//												Logging.err(this, "        ..SET: " + tCluster.getBaseHierarchyLevelNetworkInterface());
												return;
											}										
										}
									}
								}								
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Detects the local neighborhood and reports reservation to L0 cluster manager
	 * 		  IMPORTANT: This is the main function for determining capacities and link usage
	 */
	public void detectNeighborhood()
	{
		double tStartTime = mHRMController.getSimulationTime();
		String tTimeStr = "";
		
		if(hasClusterValidCoordinator()){
			if(getHierarchyLevel().isBaseLevel()){
				LinkedList<ComChannel> tChannels = getComChannels();
				for(ComChannel tComChannel : tChannels){
					tComChannel.detectNeighborhood();
				}
				
				tTimeStr += "\n   -> detectNeighborhood: " + (mHRMController.getSimulationTime() - tStartTime);

				detectLocalSiblings(this + "::detectNeighborhood()");
				tTimeStr += "\n   -> detect local siblings: " + (mHRMController.getSimulationTime() - tStartTime);
			}else{
				Logging.err(this, "detectNeighborhood() expects base hierarchy level");
			}

			/**
			 * tell the L0 cluster manager about the reservation update (if there was a change)
			 */
			if(!(this instanceof Cluster)){
				reportPhaseL0ClusterMember();
				tTimeStr += "\n   -> L0 reportPhase(): " + (mHRMController.getSimulationTime() - tStartTime);
			}
		}
		
		double tDuration = mHRMController.getSimulationTime() - tStartTime;
		
		if(tDuration > 0.5){
			Logging.err(this, "detectNeighborhood took " + tDuration + " sec." + tTimeStr);
		}
	}

	/**
	 * Sets the network interface of this cluster (only for base hierarchy level)
	 * 
	 * @param pInterfaceToNeighbor the network interface
	 */
	public void setBaseHierarchyLevelNetworkInterface(NetworkInterface pInterfaceToNeighbor)
	{
		Logging.log(this, "Setting network interface (base hierarchy level) to: " + pInterfaceToNeighbor);
//		for (StackTraceElement tStep : Thread.currentThread().getStackTrace()){
//			Logging.err(this, "    .." + tStep);
//		}
		mBaseHierarchyLevelNetworkInterface = pInterfaceToNeighbor;
		
		/**
		 * The following is FoGSiEm specific and allows for an easy detection of the network interface for a ClusterMember
		 */
//		LinkedList<ClusterMember> tMembers = mHRMController.getAllClusterMembers(0);
////		Logging.err(this, "   ..found other L0 ClusterMember: " + tMembers);
//		for(ClusterMember tMember : tMembers){
//			// avoid recursive access
//			if(!tMember.equals(this)){
////				Logging.err(this, "    ..ClusterMember: " + tMember);
//				// we only want to have ClusterMember instances
//				if(!(tMember instanceof Cluster)){
//					ComChannel tMemberChannel = tMember.getComChannelToClusterHead();
////					Logging.err(this, "      ..channel to cluster manager: " + tMemberChannel);
//					if(tMemberChannel != null){
//						L2Address tMemberChannelPeerL2Address = tMemberChannel.getPeerL2Address();
////						Logging.err(this, "        ..peer L2Address: " + tMemberChannelPeerL2Address);
//						LinkedList<ComChannel> tThisClusterChannels = getComChannels();
//						for(ComChannel tThisClusterChannel : tThisClusterChannels){
////							Logging.err(this, "      ..channel of this cluster: " + tThisClusterChannel);
//							L2Address tThisClusterChannelPeerL2Address = tThisClusterChannel.getPeerL2Address();
////							Logging.err(this, "        ..peer L2Address: " + tThisClusterChannelPeerL2Address);
//							if(tThisClusterChannelPeerL2Address != null){
//								if(tThisClusterChannelPeerL2Address.equals(tMemberChannelPeerL2Address))
//								{
//									/**
//									 * We found another ClusterMember, which has a comm channel to the same peer node which is also a peer node of this cluster
//									 */
//									if(tMember.getBaseHierarchyLevelNetworkInterface() == null){
//										// update the network interface
//										tMember.setBaseHierarchyLevelNetworkInterface(pInterfaceToNeighbor);
//									}
//								}
//							}
//						}
//					}					
//				}
//			}
//		}
	}
	
	/**
	 * Returns the network interface of this cluster (only for base hierarchy level)
	 * 
	 * @return the network interface
	 */
	public NetworkInterface getBaseHierarchyLevelNetworkInterface()
	{
		return mBaseHierarchyLevelNetworkInterface;
	}

	/**
	 * Assign new HRMID for being addressable.
	 *  
	 * @param pCaller the caller who assigns the new HRMID
	 * @param pHRMID the new HRMID
	 */
	@Override
	public void setHRMID(Object pCaller, HRMID pHRMID)
	{
		super.setHRMID(pCaller, pHRMID);

		if(getHierarchyLevel().isBaseLevel()){
			// is this a new HRMID?
			if((pHRMID != null) && (!pHRMID.equals(mAssignedL0HRMID)) && (!pHRMID.isClusterAddress()) && (!pHRMID.isZero())){
				Logging.log(this, "ASSINGED L0 HRMID=" + pHRMID + " (old=" + (mAssignedL0HRMID != null ? mAssignedL0HRMID.toString() : "null") + ", assigner=" + pCaller + ")");
		
				// update the HRMID
				setL0HRMID(pHRMID.clone());

				//HINT: addresses gets automatically announced via HRMController::autoDistributeLocalL0HRMIDsInL0Clusters()
			}
		}
	}

	/**
	 * Detects local siblings: 
	 * This function updates the HRG based on locally detected ClusterMember siblings.
	 * 
	 * @param pCause the cause for the call
	 */
	private void detectLocalSiblings(String pCause)
	{
		if(getHierarchyLevel().isBaseLevel()){
			boolean tDebug = false;
			double tTimeoffset = 2 * mHRMController.getPeriodReportPhase(getHierarchyLevel());
			//Logging.log(this, "Detecting local siblings..");
			
			if((mAssignedL0HRMID != null) && (!mAssignedL0HRMID.isZero())){
				if(mWarningDetectLocalSiblingsLocalL0HRMIDInvalid){
					if(HRMConfig.DebugOutput.SHOW_REPORT_PHASE){
						Logging.warn(this, "Successfully recovered from an invalid L0 HRMID, new one is: " + getL0HRMID());
					}
					mWarningDetectLocalSiblingsLocalL0HRMIDInvalid = false;
				}
				
				// backup old reported routing table
				RoutingTable tDeprecatedReportedRoutingTable = null;
				synchronized (mReportedRoutingTableL0HRMID) {
					tDeprecatedReportedRoutingTable = (RoutingTable) mReportedRoutingTableL0HRMID.clone();
					if(tDebug){
						Logging.err(this, "   ..previously reported routing table: " + tDeprecatedReportedRoutingTable);
					}
				}
		
				/**
				 * Find other active ClusterMember instances and create a local loopback route to them
				 */
				// iterate over all siblings
				RoutingTable tNewReportedRoutingTable = new RoutingTable();
				LinkedList<ClusterMember> tSiblings = mHRMController.getAllClusterMembers(getHierarchyLevel());
				for(ClusterMember tSibling : tSiblings){
					if(tSibling.hasClusterValidCoordinator()){
						HRMID tSiblingL0Address = tSibling.getL0HRMID();
						// has the sibling a valid L0 address?
						if((tSiblingL0Address != null) && (!tSiblingL0Address.isZero())){
							// avoid recursion
							if(!tSibling.equals(this)){
								if(tDebug){
									Logging.err(this, "  ..found active sibling: " + tSibling);
								}
								
								HRMID tGeneralizedSiblingL0Address = mAssignedL0HRMID.getForeignCluster(tSiblingL0Address);
										
								// create the new reported routing table entry
								RoutingEntry tRoutingEntryToSibling = RoutingEntry.create(mAssignedL0HRMID /* this cluster */, tGeneralizedSiblingL0Address /* the sibling */, tSiblingL0Address, 0 /* loopback route */, RoutingEntry.NO_UTILIZATION, RoutingEntry.NO_DELAY, RoutingEntry.INFINITE_DATARATE, pCause);
								tRoutingEntryToSibling.extendCause(this + "::detectLocalSiblings()_1");
								// even more details about the route to make it distinguishable from others
								tRoutingEntryToSibling.setNextHopL2Address(mHRMController.getNodeL2Address());
								// set the timeout for the found route to a sibling
								tRoutingEntryToSibling.setTimeout(mHRMController.getSimulationTime() + tTimeoffset);
	
								// add the entry to the reported routing table
								if(tDebug){							
									Logging.err(this, "   ..found (" + pCause + ") local route: " + tRoutingEntryToSibling);
								}
								tNewReportedRoutingTable.addEntry(tRoutingEntryToSibling);
							}
						}
					}
				}
				
				/**
				 * Step 1: learn new routes:
				 * 			- set the new value for reported routes based on setL0HRMID
				 * 			- and inform the HRMController
				 */ 
				synchronized (mReportedRoutingTableL0HRMID) {
					mReportedRoutingTableL0HRMID = tNewReportedRoutingTable;
					for(RoutingEntry tEntry : mReportedRoutingTableL0HRMID){
						if(tDebug){
							Logging.err(this, "   ..adding (" + pCause + ") to the HRG the route: " + tEntry);
						}
						
						/**
						 * Update HRG: register L0 link
						 */
						RoutingEntry tRoutingEntryToSibling2 = tEntry.clone();
						tRoutingEntryToSibling2.extendCause(pCause);
						tRoutingEntryToSibling2.extendCause(this + "::detectLocalSiblings()_2");
						mHRMController.registerLinkHRG(tEntry.getSource(), tEntry.getNextHop(), tRoutingEntryToSibling2);
						
						/**
						 * Update HRG: register cluster-2-cluster links
						 */ 
						RoutingEntry tRoutingEntryToSibling3 = tEntry.clone();
						tRoutingEntryToSibling3.extendCause(pCause);
						tRoutingEntryToSibling3.extendCause(this + "::detectLocalSiblings()_3");
						mHRMController.registerAutoHRG(tRoutingEntryToSibling3);
					}
				}
				
				/**
				 * Step 2: forget deprecated routes:
				 * 			- derive the deprecated routes
				 * 			- inform the HRS about the deprecated routing table entries
				 */ 
				tDeprecatedReportedRoutingTable.delEntries(tNewReportedRoutingTable);
				synchronized (tDeprecatedReportedRoutingTable) {
					for(RoutingEntry tEntry : tDeprecatedReportedRoutingTable){
						if(tDebug){
							Logging.err(this, "   ..removing (" + pCause + ") from the HRG the route: " + tEntry);
						}
		
						/**
						 * Update HRG: unregister L0 link
						 */
						RoutingEntry tRoutingEntryToSibling2 = tEntry.clone();
						tRoutingEntryToSibling2.extendCause(pCause);
						tRoutingEntryToSibling2.extendCause(this + "::detectLocalSiblings()_4");
						mHRMController.unregisterLinkHRG(tEntry.getSource(),  tEntry.getNextHop(), tRoutingEntryToSibling2);
						
						/**
						 * Update HRG: register cluster-2-cluster links
						 */ 
						RoutingEntry tRoutingEntryToSibling3 = tEntry.clone();
						tRoutingEntryToSibling3.extendCause(pCause);
						tRoutingEntryToSibling3.extendCause(this + "::detectLocalSiblings()_5");
						mHRMController.unregisterAutoHRG(tRoutingEntryToSibling3);
					}
				}
			}else{
				if(!mWarningDetectLocalSiblingsLocalL0HRMIDInvalid){
					if(HRMConfig.DebugOutput.SHOW_REPORT_PHASE){
						Logging.warn(this, "detectLocalSiblings() skipped because local L0 HRMID is: " + mAssignedL0HRMID);
					}
					
					// avoid endless warnings
					mWarningDetectLocalSiblingsLocalL0HRMIDInvalid = true;
				}
			}
		}
	}
	
	/**
	 * Sets the new L0 address for this physical node
	 * 
	 * @param pNewL0HRMID the new L0 HRMID
	 */
	private int mCallsSetL0HRMID = 0;
	protected void setL0HRMID(HRMID pNewL0HRMID)
	{
		mCallsSetL0HRMID++;
		
		// is this a new HRMID?
		if((pNewL0HRMID != null) && (!pNewL0HRMID.isClusterAddress()) && (!pNewL0HRMID.isZero())){
			Logging.log(this, "ASSIGNED new (" + mCallsSetL0HRMID + ") L0 node HRMID: " + pNewL0HRMID);

//			Logging.err(this, "Setting L0 HRMID: " + pNewL0HRMID);
//			for (StackTraceElement tStep : Thread.currentThread().getStackTrace()){
//			    Logging.err(this, "    .." + tStep);
//			}

			/**
			 * Unregister old HRMID
			 */
			if(this instanceof Cluster){
				if((mAssignedL0HRMID != null) && (!mAssignedL0HRMID.isZero())){
					mHRMController.unregisterHRMID(this, mAssignedL0HRMID, this + "::setL0HRMID()(" + mCallsSetL0HRMID + ")");
				}
			}
			
			/**
			 * Set the new L0 address for this physical node
			 */ 
			mAssignedL0HRMID = (pNewL0HRMID != null ? pNewL0HRMID.clone() : null);
			
			/**
			 * Register new HRMID
			 */
			if(this instanceof Cluster){
				mHRMController.registerHRMID(this, mAssignedL0HRMID, this + "::setL0HRMID()(" + mCallsSetL0HRMID + ")");
			}

			/**
			 * Trigger: update HRG
			 */
			LinkedList<ClusterMember> tClusterMembers = mHRMController.getAllClusterMembers(getHierarchyLevel());
			for(ClusterMember tClusterMember : tClusterMembers){
				if(tClusterMember.hasClusterValidCoordinator()){
					tClusterMember.detectLocalSiblings(this + "::setL0HRMID()(" + mCallsSetL0HRMID + ")");
				}
			}
		}
	}
	
	/**
	 * Returns the L0 address which was assigned by this L0 (if it is so) cluster for this physical node
	 *  
	 * @return the assigned HRMID
	 */
	public HRMID getL0HRMID()
	{
		if(mAssignedL0HRMID != null){
			return mAssignedL0HRMID.clone();
		}else{
			return null;
		}
	}

	/**
	 * Returns the comm. channel to the cluster manager
	 * 
	 * @return the comm. channel
	 */
	public ComChannel getComChannelToClusterManager()
	{
		ComChannel tResult = null;
		
		LinkedList<ComChannel> tChannels = getComChannels();
		if(tChannels.size() == 1){
			tResult = tChannels.getFirst();
		}else{
			if(tChannels.size() > 1){
				throw new RuntimeException(this + "::getComChannelToClusterManager() found an invalid amount of comm. channels: " + tChannels);
			}else{
				// no comm. channel -> can occur if the entity was already invalidated and the object reference is still included in some lists
//TODO				Logging.warn(this, "getComChannelToClusterManager() has not found any remaining comm. channel, entity valid?: " + isThisEntityValid() + ", cluster activation: " + hasClusterValidCoordinator());
			}
		}
			
		return tResult;
	}
	
	/**
	 * SEND: AnnounceHRMIDs to all known cluster members
	 */
	public void distributeAnnounceHRMIDs()
	{
		boolean DEBUG = HRMConfig.DebugOutput.GUI_SHOW_ADDRESS_DISTRIBUTION;
		
		if(getHierarchyLevel().isBaseLevel()){
			// only announce in active clusters, avoid unnecessary packets here
			if(hasClusterValidCoordinator()){
				if(DEBUG){
					Logging.log(this, "Distributing AnnounceHRMIDs...");
				}
	
				LinkedList<HRMID >tLocalHRMIDs = mHRMController.getHRMIDs();
				if(DEBUG){
					Logging.log(this, "    ..found local HRMIDs: " + tLocalHRMIDs);
				}
				
				/**
				 * Filter local HRMIDs for L0 node HRMIDs
				 */
				LinkedList<HRMID >tLocalL0HRMIDs = new LinkedList<HRMID>();
				for(HRMID tLocalHRMID : tLocalHRMIDs){
					// is the HRMID a cluster address?
					if(!tLocalHRMID.isClusterAddress()){
						// ignore this ClusterMember's node specific L0 HRMID, which is already known to the peer
						if(DEBUG){
							if(!tLocalHRMID.equals(mAssignedL0HRMID)){
								Logging.log(this, "    ..found L0 node HRMID: " + tLocalHRMID.toString());
							}else{
								Logging.log(this, "    ..ignoring L0 node HRMID: " + tLocalHRMID.toString());
							}
						}
						tLocalL0HRMIDs.add(tLocalHRMID);
					}else{
						if(DEBUG){
							Logging.log(this, "    ..ignoring cluster HRMID: " + tLocalHRMID.toString());
						}
					}
				}
				
				if (tLocalL0HRMIDs.size() > 0){
					if(DEBUG){
						Logging.log(this, "Distributing AnnounceHRMIDs packets..");
					}
		
					/**
					 * Announce the HRMIDs to the peers
					 */
					for(ComChannel tChannel : getComChannels()){
						tChannel.distributeAnnounceHRMIDs(tLocalL0HRMIDs);
					}
				}
			}
		}
	}
	
	/**
	 * EVENT: RouteShare
	 * 
	 * @param pSourceComChannel the source comm. channel
	 * @param pSharedRoutingTable the shared routing table
	 * @param pDeprecatedSharedRoutingTable the deprecated shared routing table
	 */
	public void eventReceivedRouteShare(ComChannel pSourceComChannel, RoutingTable pSharedRoutingTable, RoutingTable pDeprecatedSharedRoutingTable)
	{
		boolean DEBUG = HRMConfig.DebugOutput.SHOW_SHARE_PHASE; 

		if(DEBUG){
			Logging.log(this, "EVENT: ReceivedRouteShare via: " + pSourceComChannel);
			for(RoutingEntry tSharedEntry : pSharedRoutingTable){
				if(tSharedEntry.isRouteAcrossNetwork()){
					Logging.warn(this, "  ..received shared LOOP ROUTE (TO: " + tSharedEntry.getTimeout() + "): " + tSharedEntry);
				}else{
					Logging.warn(this, "  ..entry (TO: " + tSharedEntry.getTimeout() + "): " + tSharedEntry);
				}
			}
		
			if((pDeprecatedSharedRoutingTable != null) && (pDeprecatedSharedRoutingTable.size() > 0)){
				Logging.log(this, "Found deprecated shared routing table: " + pDeprecatedSharedRoutingTable);
				for(RoutingEntry tDeprecatedEntry : pDeprecatedSharedRoutingTable){
					Logging.log(this, "   ..found deprecated reported routing entry: " + tDeprecatedEntry);
				}
			}
		}

		mHRMController.addHRMRoutesFromRouteShare(pSharedRoutingTable, getHierarchyLevel(), getHRMID(), pSourceComChannel.getPeerHRMID(), this + "::eventReceivedRouteShare()");			
	}

	/**
	 * EVENT: PingPeer
	 * 
	 * @param pSourceComChannel the source comm. channel
	 * @param pPingPeerPacket the original packet
	 */
	public void eventReceivedPing(ComChannel pSourceComChannel, PingPeer pPingPeerPacket)
	{
		Logging.log(this, "EVENT: ReceivedPingPeer via: " + pSourceComChannel);
		Logging.log(this, "   ..sending ALIVE via: " + pSourceComChannel);
		getElector().distributeALIVE(pSourceComChannel);
	}

	/**
	 * Calculates last hop on a given hierarchy level for a received topology update packet
	 * 
	 * @param pTopologyUpdate the received topology update packet
	 * 
	 * @return the calculated last hop
	 */
	private ClusterName lastHopFor(SignalingMessageHierarchyUpdate pTopologyUpdate)
	{
		ClusterName tResult = null;
		HierarchyLevel tHierarchLevelOfSender = pTopologyUpdate.getSenderEntityName().getHierarchyLevel();
	
		if(tHierarchLevelOfSender.isBaseLevel())
		{
			/**
			 * use the name of this entity
			 */
			tResult = this;
		}else{
			/**
			 * use the name of the superior entity on the corresponding hierarchy level
			 */
			LinkedList<ClusterName> tSuperiorCoordinators = mHRMController.getAllSuperiorCoordinators(tHierarchLevelOfSender);
			if(tSuperiorCoordinators.size() > 0)
			{
				tResult = tSuperiorCoordinators.getFirst();
			}else{
				if(HRMConfig.Hierarchy.DEPTH > 3)
					Logging.warn(this, "No superior coordinators found, known superior coordinators: " + mHRMController.getAllSuperiorCoordinators() + ", " + mHRMController.getAllCoordinators());
				//tResult = null;
			}
		}
		
		return tResult;	
	}
	
	/**
	 * EVENT: coordinator announcement, we react on this by:
	 *       1.) store the topology information locally
	 *       2.) forward the announcement within the same hierarchy level ("to the side")
	 * 
	 * (gets called by the ComChannel, which received the AnnounceCoordinator packet)
	 * (responsible for sideward forwarding of an AnnounceCoordinator packet, also responsible for L0 downward forwarding of an AnnounceCoordinator pacet)
	 * 
	 * @param pComChannel the source comm. channel
	 * @param pPacket the received announcement packet
	 */
	@Override
	public void eventCoordinatorAnnouncement(ComChannel pComChannel, AnnounceCoordinator pPacket)
	{
		if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
			Logging.log(this, "EVENT: coordinator announcement (from side): " + pPacket);
		}
		
		/**
		 * Storing that the announced coordinator is a superior one of this node
		 */
		// is the packet still on its way from the top to the bottom AND does it not belong to an L0 coordinator?
		if((!pPacket.enteredSidewardForwarding()) && (!pPacket.getSenderEntityName().getHierarchyLevel().isBaseLevel())){
			mHRMController.registerRemoteSuperiorCoordinator(pPacket.getSenderEntityName()); //TODO: use timeouts here
		}

//		if(pAnnounceCoordinator.getSenderClusterName().getGUICoordinatorID() == 16){
//			Logging.log(this, "EVENT: coordinator announcement (from side): " + pAnnounceCoordinator);
//			Logging.log(this, "   ..route: " + pAnnounceCoordinator.getRoute());			
//			Logging.log(this, "   ..sender coordinator L2Address: " + pAnnounceCoordinator.getSenderClusterCoordinatorNodeL2Address());
//			Logging.log(this, "   ..peer L2Address: " + pComChannel.getPeerL2Address());
//		}

		/**
		 * Check if we should forward this announcement "to the side"
		 */
		// is this the 2+ passed ClusterMember OR (in case it is the first passed ClusterMember) the peer is the origin of the announce -> forward the announcement 
		Route tRoute = pPacket.getRoute();
		if(((tRoute != null) && (!tRoute.isEmpty()) && (tRoute.getFirst() != null)) || (pPacket.getSenderEntityNodeL2Address().equals(pComChannel.getPeerL2Address()))){
			/**
			 * Duplicate the packet and write to the duplicate
			 */
			AnnounceCoordinator tForwardPacket = (AnnounceCoordinator)pPacket.duplicate();

			int tCorrectionForPacketCounter = -1;
			
			/**
			 * Record the passed clusters
			 */
			tForwardPacket.addGUIPassedCluster(new Long(getGUIClusterID()));
	
			/**
			 * Enlarge the stored route towards the announcer
			 */
			if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
				Logging.log(this, "      ..adding route: " + pComChannel.getRouteToPeer());
			}
			tForwardPacket.addRouteHop(pComChannel.getRouteToPeer());
			
//			if(pAnnounceCoordinator.getSenderClusterName().getGUICoordinatorID() == 16){
//				Logging.log(this, "EVENT: duplicated coordinator announcement (from side): " + tForwardPacket);
//				Logging.log(this, "   ..dup route: " + tForwardPacket.getRoute());			
//				Logging.log(this, "   ..dup sender coordinator L2Address: " + tForwardPacket.getSenderClusterCoordinatorNodeL2Address());
//				Logging.log(this, "   ..dup peer L2Address: " + pComChannel.getPeerL2Address());
//			}

			/**
			 * Store the announced remote coordinator in the ARG 
			 */
			registerAnnouncedCoordinatorARG(this, tForwardPacket);

			CoordinatorProxy tLocalCoordinatorProxy = mHRMController.getCoordinatorProxyByName(tForwardPacket.getSenderEntityName());
			if(tLocalCoordinatorProxy == null){
				Logging.err(this, "eventCoordinatorAnnouncement() hasn't found the local coordinator proxy for announcement: " + tForwardPacket + ", knowing these proxies: ");
				LinkedList<CoordinatorProxy> tProxies = mHRMController.getAllCoordinatorProxies();
				for(CoordinatorProxy tProxy : tProxies){
					Logging.err(this, "   ..: " + tProxy + " => " + tProxy.equals(tForwardPacket.getSenderEntityName(), true));
				}
			}
			
			if(pPacket.isPacketTracking()){
				Logging.log(this, "\n##### Detected tracked AnnounceCoordinator packet: " + tForwardPacket + "\n");
				Logging.log(this, "  ..packet reverse route: " + tForwardPacket.getRoute());
			}
			
			if((tLocalCoordinatorProxy == null) || (tForwardPacket.getRouteLength() <= tLocalCoordinatorProxy.getPhysicalHopDistance()) /* avoid too long routes and routing loops */){
				/**
				 * decrease TTL: if there is a transition from one cluster to the next one
				 */
				ClusterName tCurrentLastHop = lastHopFor(tForwardPacket);
				if((tCurrentLastHop != null) && (!tCurrentLastHop.equals(tForwardPacket.getLastHopEntityName()))){
					tForwardPacket.incHierarchyHopCount();
				}

				/**
				 * TTL is still okay?
				 */
				if(tForwardPacket.shouldBeForwarded()){
					/**
					 * do we have a loop?
					 */ 
					if(!tForwardPacket.hasPassedNode(mHRMController.getNodeL2Address())){
						/**
						 * STEP 1: record the passed nodes
						 */
						tForwardPacket.addPassedNode(mHRMController.getNodeL2Address());
						
						/**
						 * STEP 2: update the stored entity name of the last hop
						 */
						tForwardPacket.setLastHopEntityName(lastHopFor(tForwardPacket));
						
						/**
						 * STEP 3: check if this announcement is already on its way sidewards, otherwise, mark it as sideward
						 */
						if(!tForwardPacket.enteredSidewardForwarding()){
							// are we a cluster member of a cluster, which is located on the same node from where this announcement comes from? -> forward the packet to the side
							if (pComChannel.getPeerL2Address().equals(tForwardPacket.getSenderEntityNodeL2Address())){
								/**
								 * mark packet as "sideward forwarded"
								 */
								tForwardPacket.setSidewardForwarding();
							}else{
								// we are a cluster member of any cluster located at a node where this announcement was received from a superior coordinator
								
								/**
								 * drop the packet and return immediately
								 */ 
								return;
							}
						}
			
						/**
						 * STEP 4: forward the announcement within the same hierarchy level ("to the side")
						 */
						// get locally known neighbors for this cluster and hierarchy level
						LinkedList<Cluster> tLocalClusters = mHRMController.getAllClusters(getHierarchyLevel());
						if(tLocalClusters.size() > 0){
							if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
								Logging.log(this, "     ..found " + tLocalClusters.size() + " neighbor clusters");
							}
				
							for(Cluster tLocalCluster: tLocalClusters){
								/**
								 * Do NOT forward the announcement to L0 clusters of the same network interface, they got already informed by the original sender
								 */
								if((getBaseHierarchyLevelNetworkInterface() == null) || (!getBaseHierarchyLevelNetworkInterface().equals(tLocalCluster.getBaseHierarchyLevelNetworkInterface()))){
									/**
									 * Forward the announcement
									 * HINT: we avoid loops by excluding the sender from the forwarding process
									 */
									if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
										Logging.log(this, "     ..fowarding this event to locally known neighbor cluster: " + tLocalCluster);
									}else if(tForwardPacket.isPacketTracking()){
										Logging.log(this, "#### Fowarding tracked AnnounceCoordinator to neighbor cluster: " + tLocalCluster);
									}

									// create list of prohibited nodes
									@SuppressWarnings("unchecked")
									LinkedList<L2Address> tProhibitedNodes = (LinkedList<L2Address>) tForwardPacket.getPassedNodes().clone();
									tProhibitedNodes.add(tForwardPacket.getSenderEntityNodeL2Address());
	
									// forward this announcement to all cluster members
									tLocalCluster.sendClusterBroadcast(tForwardPacket, true, tProhibitedNodes /* pComChannel.getPeerL2Address() <- exclude this and all other already passed nodes from the forwarding process */);
									
									tCorrectionForPacketCounter++;
								}else{
									// L0 cluster for the same network interface -> skip this
								}
							}
						}else{
							if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
								Logging.log(this, "No neighbors found, ending forwarding of: " + tForwardPacket);
							}
						}
					}else{
						if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
							Logging.warn(this, "eventCoordinatorAnnouncement() found a forwarding loop for: " + tForwardPacket + "\n   ..passed clusters: " + tForwardPacket.getGUIPassedClusters()+ "\n   ..passed nodes: " + tForwardPacket.getPassedNodesStr());
						}
					}
				}else{
					if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
						Logging.log(this, "TTL exceeded for coordinator announcement: " + tForwardPacket);
					}
				}
				
				/**
				 * HACK: correction of packet counter for AnnounceCoordinator packets
				 */
				synchronized (AnnounceCoordinator.sCreatedPackets) {
					AnnounceCoordinator.sCreatedPackets += tCorrectionForPacketCounter; 
				}
				synchronized (SignalingMessageHrm.sCreatedPackets) {
					SignalingMessageHrm.sCreatedPackets += tCorrectionForPacketCounter; 
				}
			}else{
				// the announcement took already a longer path than possible (we have already received this announcement via another route)
				//Logging.warn(this, "Dropping (" + tForwardPacket.getDistance() + " > " + tLocalCoordinatorProxy.getDistance() + ") announcement: " + tForwardPacket);
				//Logging.warn(this, "   ..passed nodes: " + tForwardPacket.getPassedNodesStr());
			}
		}else{
			if(pPacket.isPacketTracking()){
				Logging.log(this, "#### Forwarding to the side stopped for: " + pPacket);
				Logging.log(this, "  ..packet reverse route: " + pPacket.getRoute());
			}	
		}
			
	}

	/**
	 * EVENT: coordinator announcement, we react on this by:
	 *       1.) store the topology information locally
	 *       2.) forward the announcement within the same hierarchy level ("to the side")
	 * 
	 * @param pComChannel the source comm. channel
	 * @param pAnnounceCoordinator the received announcement
	 */
	@Override
	public void eventCoordinatorInvalidation(ComChannel pComChannel, InvalidCoordinator pInvalidCoordinator)
	{
//		if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_INVALIDATION_PACKETS){
			Logging.log(this, "EVENT: coordinator invalidation (from side): " + pInvalidCoordinator);
//		}
		
		/**
		 * Duplicate the packet and write to the duplicate
		 */
		InvalidCoordinator tForwardPacket = (InvalidCoordinator)pInvalidCoordinator.duplicate();

		int tCorrectionForPacketCounter = -1;
		
		/**
		 * Store the announced remote coordinator in the ARG 
		 */
		unregisterAnnouncedCoordinatorARG(this, tForwardPacket);
		
		/**
		 * transition from one cluster to the next one => decrease TTL value
		 */
		if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_INVALIDATION_PACKETS){
			Logging.log(this, "Deacreasing TTL of: " + tForwardPacket);
		}
		tForwardPacket.incHierarchyHopCount(); //TODO: decreasen in abhaengigkeit der hier. ebene -> dafuer muss jeder L0 cluster wissen welche hoeheren cluster darueber liegen
	
		/**
		 * forward the announcement if the TTL is still okay
		 */
		if(tForwardPacket.shouldBeForwarded()){
			// do we have a loop?
			if(!tForwardPacket.hasPassedNode(mHRMController.getNodeL2Address())){
				/**
				 * STEP 1: record the passed nodes
				 */
				tForwardPacket.addPassedNode(mHRMController.getNodeL2Address());

				/**
				 * STEP 2: Check if this announcement is already on its way sidewards, otherwise, mark it as sidewarde
				 */
				if(!tForwardPacket.enteredSidewardForwarding()){
					// are we a cluster member of a cluster, which is located on the same node from where this announcement comes from? -> forward the packet to the side
					if (pComChannel.getPeerL2Address().equals(tForwardPacket.getSenderEntityNodeL2Address())){
						/**
						 * mark packet as "sideward forwarded"
						 */
						tForwardPacket.setSidewardForwarding();
					}else{
						// we are a cluster member of any cluster located at a node where this announcement was received from a superior coordinator
						
						/**
						 * drop the packet and return immediately
						 */ 
						return;
					}
				}
	
				/**
				 * STEP 3: forward the announcement within the same hierarchy level ("to the side")
				 */
				// get locally known neighbors for this cluster and hierarchy level
				LinkedList<Cluster> tLocalClusters = mHRMController.getAllClusters(getHierarchyLevel());
				if(tLocalClusters.size() > 0){
					if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_INVALIDATION_PACKETS){
						Logging.log(this, "     ..found " + tLocalClusters.size() + " neighbor clusters");
					}
		
					for(Cluster tLocalCluster: tLocalClusters){
						/**
						 * Do NOT forward the announcement to L0 clusters of the same network interface, they got already informed by the original sender
						 */
						if((getBaseHierarchyLevelNetworkInterface() == null) || (!getBaseHierarchyLevelNetworkInterface().equals(tLocalCluster.getBaseHierarchyLevelNetworkInterface()))){
							/**
							 * Forward the announcement
							 * HINT: we avoid loops by excluding the sender from the forwarding process
							 */
							if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_INVALIDATION_PACKETS){
								Logging.log(this, "     ..fowarding this event to locally known neighbor cluster: " + tLocalCluster);
							}
							
							// create list of prohibited nodes
							@SuppressWarnings("unchecked")
							LinkedList<L2Address> tProhibitedNodes = (LinkedList<L2Address>) tForwardPacket.getPassedNodes().clone();
							tProhibitedNodes.add(tForwardPacket.getSenderEntityNodeL2Address());

							// forward this announcement to all cluster members
							tLocalCluster.sendClusterBroadcast(tForwardPacket, true, tProhibitedNodes /* pComChannel.getPeerL2Address() <- exclude this and all other already passed nodes from the forwarding process */);
							
							tCorrectionForPacketCounter++;
						}else{
							// L0 cluster for the same network interface -> skip this
						}
					}
				}else{
					if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_INVALIDATION_PACKETS){
						Logging.log(this, "No neighbors found, ending forwarding of: " + tForwardPacket);
					}
				}
			}else{
				if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_ANNOUNCEMENT_PACKETS){
					Logging.warn(this, "eventCoordinatorAnnouncement() found a forwarding loop for: " + tForwardPacket + "\n   ..passed nodes: " + tForwardPacket.getPassedNodesStr());
				}
			}
		}else{
			if(HRMConfig.DebugOutput.SHOW_DEBUG_COORDINATOR_INVALIDATION_PACKETS){
				Logging.log(this, "TTL exceeded for coordinator invalidation: " + tForwardPacket);
			}
		}
		
		/**
		 * HACK: correction of packet counter for InvalidCoordinator packets
		 */
		synchronized (InvalidCoordinator.sCreatedPackets) {
			InvalidCoordinator.sCreatedPackets += tCorrectionForPacketCounter; 
		}
		synchronized (SignalingMessageHrm.sCreatedPackets) {
			SignalingMessageHrm.sCreatedPackets += tCorrectionForPacketCounter; 
		}

	}

	/**
	 * EVENT: notifies that a communication channel is now available
	 * 
	 * @param pComChannel the communication channel which is now available
	 */
	public void eventComChannelEstablished(ComChannel pComChannel)
	{
		Logging.log(this, "EVENT: ComChannel established for " + pComChannel);
		
		/**
		 * Trigger: established for the comm. channel
		 */
		pComChannel.eventEstablished();

		/**
		 * Trigger: network interface detection
		 */
		if(getHierarchyLevel().isBaseLevel())
		{
			if(!(this instanceof Cluster)){
				detectNetworkInterface();
			}
		}
		
		/**
		 * Trigger: tell elector that a new participant has joined
		 */
		mElector.eventElectionAvailable(pComChannel);
	}

	/**
	 * EVENT: cluster membership request, a cluster requests of a coordinator to acknowledge cluster membership, triggered by the comm. session
	 * 
	 * @param pRemoteClusterName the description of the possible new cluster member
	 * @param pSourceComSession the comm. session where the packet was received
	 * @param pNetworkInterface the network interface via which this request was received
	 */
	public void eventL0ClusterMembershipRequest(ClusterName pRemoteClusterName, ComSession pSourceComSession, NetworkInterface pNetworkInterface)
	{
		Logging.log(this, "EVENT: got cluster membership request from: " + pRemoteClusterName);
		
		/**
		 * Create the communication channel for the described cluster member
		 */
		Logging.log(this, "     ..creating communication channel");
		ComChannel tComChannel = new ComChannel(mHRMController, ComChannel.Direction.IN, this, pSourceComSession);

		/**
		 * Set the remote ClusterName of the communication channel
		 */
		tComChannel.setRemoteClusterName(pRemoteClusterName);

		if(pNetworkInterface != null){
			setBaseHierarchyLevelNetworkInterface(pNetworkInterface);
		}
		
		/**
		 * SEND: acknowledgment -> will be answered by a ElectionPriorityUpdate
		 */
		tComChannel.signalRequestClusterMembershipAck(null, null);

		/**
		 * Trigger: comm. channel established 
		 */
		eventComChannelEstablished(tComChannel);
	}

	/**
	 * Sends a packet as broadcast to all cluster members
	 * 
	 * @param pPacket the packet which has to be broadcasted
	 * @param pIncludeLoopback should loopback communication be included?
	 * @param pExcludeL2Addresses describe nodes which shouldn't receive this broadcast
	 * @param pCheckLinkActivation define if the state of each link should be checked for activation before a packet is allowed to pass this link
	 */
	private void sendClusterBroadcast(ISignalingMessageHrmBroadcastable pPacket, boolean pIncludeLoopback, LinkedList<L2Address> pExcludeL2Addresses, boolean pCheckLinkActivation)
	{
		boolean DEBUG = false;
		
		// get all communication channels
		LinkedList<ComChannel> tComChannels = getComChannels();

		// get the L2Addres of the local host
		L2Address tLocalL2Address = mHRMController.getHRS().getCentralFNL2Address();
		
		if (DEBUG){
			Logging.log(this, "Sending BROADCASTS from " + tLocalL2Address + " the packet " + pPacket + " to " + tComChannels.size() + " communication channels, local base prio: " + mHRMController.getNodePriority(getHierarchyLevel()));
		}
		
		/**
		 * Account the broadcast
		 */
		pPacket.accountBroadcast();

		for(ComChannel tComChannel : tComChannels) {
			boolean tIsLoopback = tComChannel.toLocalNode();
			
			/**
			 * check if this packet should enter the next AS behind this comm. channel
			 */
			boolean tIsAllowedToEnterNextAS = true;
			if(pPacket instanceof SignalingMessageHierarchyUpdate){
				SignalingMessageHierarchyUpdate tSignalingMessageASSeparator = (SignalingMessageHierarchyUpdate)pPacket;
				tIsAllowedToEnterNextAS = tSignalingMessageASSeparator.isAllowedToEnterAs(mHRMController, (enforcesASSplit() ? new Long(-1 /* some invalid value which differs from the local one */) : tComChannel.getPeerAsID()));				
			}

			/**
			 * check if this packet is allowed to travel along this comm. channel
			 */
			boolean tIsAllowedToUseThisChannel = true;
			if(pCheckLinkActivation){
				tIsAllowedToUseThisChannel = tComChannel.isLinkActiveForElection();
			}
			
			/**
			 * check for AnnounceCoordinator if the peer is located behind a network one-way
			 */
			boolean tAvoidAnnounceCoordinatorToOneWay = false;
			if(pPacket instanceof AnnounceCoordinator){
				if ((tComChannel.getPeerConnectivity() == 1) || (tComChannel.getPeerDomains() == 1)){
					tAvoidAnnounceCoordinatorToOneWay = true;
				}
						
			}
			
			/**
			 * is this packet allowed to enter the next AS behind this comm. channel?
			 */
			if(tIsAllowedToEnterNextAS){
				/**
				 * is this packet allowed to use this comm. channel?
				 */
				if(tIsAllowedToUseThisChannel){
					/**
					 * avoid to send AnnounceCoordinator broadcasts into network one-ways
					 */
					if(!tAvoidAnnounceCoordinatorToOneWay){
						/**
						 * should we deliver this packet to the destination node behind this comm. channel?
						 */
						if((pExcludeL2Addresses == null /* excluded peer address is null => we send everywhere */) || (!pExcludeL2Addresses.contains(tComChannel.getPeerL2Address()) /* should the peer be excluded? */)){
							if (DEBUG){
								if (!tIsLoopback){
									Logging.log(this, "  ..to " + tComChannel + ", excluded: " + pExcludeL2Addresses);
								}else{
									Logging.log(this, "  ..to LOOPBACK " + tComChannel);
								}
							}
				
							/**
							 * should we deliver this packet to the loopback destination behind this comm. channel?
							 */
							if ((pIncludeLoopback) || (!tIsLoopback)){
								if(tComChannel.isOpen()){
									SignalingMessageHrm tNewPacket = pPacket.duplicate();
									if (DEBUG){
										if(pPacket instanceof AnnounceCoordinator){
											Logging.warn(this, "Sending AC (loc. conn.: " + mHRMController.getConnectivity() + ", peer domains: " + tComChannel.getPeerDomains() + ") along: " + tComChannel);
										}else{
											Logging.log(this, "      ..sending duplicate packet: " + tNewPacket);
										}
									}
									// send the packet to one of the possible cluster members
									tComChannel.sendPacket(tNewPacket);
								}else{
									if (DEBUG){
										Logging.log(this, "        ..sending skipped because we are still waiting for establishment of channel: " + tComChannel);
									}
								}
							}else{
								if (DEBUG){
									Logging.log(this, "         ..skipping " + (tIsLoopback ? "LOOPBACK CHANNEL" : ""));
								}
							}
						}else{
							if (DEBUG){
								Logging.log(this, "         ..skipping EXCLUDED DESTINATION: " + pExcludeL2Addresses);
							}
						}
					}else{
						if (DEBUG){
							Logging.warn(this, "        ..sending skipped because of one-way character of: " + tComChannel);
						}
					}
				}else{
					if (DEBUG){
						Logging.log(this, "         ..skipping comm. channel (not allwoed because of its inactive state)");
						Logging.log(this, "           ..skipping packet: " + pPacket);
					}
				}
			}else{
				if (DEBUG){
					Logging.log(this, "         ..skipping NEXT AS");
					Logging.log(this, "           ..AS" + mHRMController.getGUIAsID() + " => AS" + tComChannel.getGUIPeerAsID());
					Logging.log(this, "           ..skipping packet: " + pPacket);
				}
			}
		}
	}
	public void sendClusterBroadcast(ISignalingMessageHrmBroadcastable pPacket, boolean pIncludeLoopback, boolean pCheckLinkState)
	{
		sendClusterBroadcast(pPacket, pIncludeLoopback, null, pCheckLinkState);
	}
	public void sendClusterBroadcast(ISignalingMessageHrmBroadcastable pPacket, boolean pIncludeLoopback)
	{
		sendClusterBroadcast(pPacket, pIncludeLoopback, null, false);
	}
	protected void sendClusterBroadcast(ISignalingMessageHrmBroadcastable pPacket, boolean pIncludeLoopback, LinkedList<L2Address> pExcludeL2Addresses)
	{
		sendClusterBroadcast(pPacket, pIncludeLoopback, pExcludeL2Addresses, false);
	}

	/**
	 * This implements the report phase for a simple L0 cluster member.
	 * In general, this function can be skipped if the managed broadcast domain can have collisions (e.g., a WLAN or an half-duplex link) 
	 */
	public void reportPhaseL0ClusterMember()
	{
		boolean DEBUG = HRMConfig.DebugOutput.SHOW_REPORT_PHASE;
		
		if(DEBUG){
			Logging.warn(this, "REPORT PHASE");
		}
		
		/**
		 * Auto. delete deprecated routes
		 */
		if(getHierarchyLevel().isBaseLevel()){
			if(!(this instanceof Cluster)){
				mHRMController.autoRemoveObsoleteHRGLinks();
			
				if(hasClusterValidCoordinator()){
					if(getBaseHierarchyLevelNetworkInterface() != null){
						HRMID tLocalHRMID = getL0HRMID();
						if((tLocalHRMID != null) && (!tLocalHRMID.isZero())){
							ComChannel tChannelToHead = getComChannelToClusterManager();
							if(tChannelToHead != null){
								HRMID tClusterHeadHRMID = tChannelToHead.getPeerHRMID();
								if((tClusterHeadHRMID != null) && (!tClusterHeadHRMID.isZero())){
									RoutingTable tReportRoutingTable = new RoutingTable();
									Bus tPhysicalBus = (Bus)getBaseHierarchyLevelNetworkInterface().getBus();
									double tTimeoffset = HRMConfig.Routing.ROUTE_TIMEOUT  + HRMConfig.Hierarchy.MAX_E2E_DELAY;//2 * mHRMController.getPeriodReportPhase(mParent.getHierarchyLevel());
				
									RoutingEntry tReportedRoutingEntryForward = null;
				
									tReportedRoutingEntryForward = RoutingEntry.createRouteToDirectNeighbor(tLocalHRMID, tClusterHeadHRMID, tClusterHeadHRMID, tPhysicalBus.getUtilization(), tPhysicalBus.getDelayMSec(), tPhysicalBus.getAvailableDataRate(), this + "::reportPhaseL0ClusterMember()");
									tReportedRoutingEntryForward.addOwner(tLocalHRMID);
									tReportedRoutingEntryForward.setOrigin(tLocalHRMID);
									tReportedRoutingEntryForward.extendCause(this + "::reportPhaseL0ClusterMember() for cluster manager HRMID " + tClusterHeadHRMID + " as " + tReportedRoutingEntryForward);
									// define the L2 address of the next hop in order to let "addHRMRoute" trigger the HRS instance the creation of new HRMID-to-L2ADDRESS mapping entry
									tReportedRoutingEntryForward.setNextHopL2Address(tChannelToHead.getPeerL2Address());
									// set the timeout for the found route to neighborhood
									tReportedRoutingEntryForward.setTimeout(mHRMController.getSimulationTime() + tTimeoffset);
									
									// add the routing table entry to the final routing table which gets reported to the cluster manager
									tReportRoutingTable.addEntry(tReportedRoutingEntryForward);
									
									/**
									 * Send the created report routing table to the superior coordinator
									 */
									if(tReportRoutingTable.size() > 0){
										if (DEBUG){
											Logging.log(this, "   ..reporting via " + tChannelToHead + " the routing table:");
											int i = 0;
											for(RoutingEntry tEntry : tReportRoutingTable){
												Logging.log(this, "     ..[" + i +"]: " + tEntry);
												i++;
											}
										}
										
										/**
										 * Set the timeout for each entry depending on the state of the known HRM hierarchy
										 */
										boolean tReportOnlyADiff = false;
										
										if(mHRMController.hasLongTermStableHierarchy()){
											/**
											 * should we report only a diff.? 
											 */
											if((HRMConfig.Routing.REPORT_ROUTE_RATE_REDUCTION_FOR_STABLE_HIERARCHY) && (mL0TimeLastCompleteReportedRoutingTable > 0) && (mHRMController.getSimulationTime() < mL0TimeLastCompleteReportedRoutingTable + HRMConfig.Routing.ROUTE_TIMEOUT_STABLE_HIERARCHY) && (!mL0LastReportedRoutingTableWasDuringUnstableHierarchy)){
												/**
												 * we actually provide only a diff to the last diff/complete reported routing table
												 */
												tReportOnlyADiff = true;
												
												RoutingTable tDiffReportRoutingTable = new RoutingTable();
												for(RoutingEntry tNewEntry : tReportRoutingTable){
													boolean tEntryHasChanges = true;
													for(RoutingEntry tOldEntry : mL0LastSentReportedRoutingTable){
														/**
														 * is the new entry rather an old one?
														 */
														if ((tOldEntry.equals(tNewEntry)) && (tOldEntry.equalsQoS(tNewEntry))){
															tEntryHasChanges = false;
															break;
														}
													}
													
													/**
													 * add the entry with changes to the "diff" table
													 */
													if(tEntryHasChanges){
														tDiffReportRoutingTable.add(tNewEntry);
													}
												}
												
												// store the complete routing table as last report but send only the diff
												mL0LastSentReportedRoutingTable = (RoutingTable) tReportRoutingTable.clone();
												// the "diff" table
												tReportRoutingTable = tDiffReportRoutingTable;
												tReportRoutingTable.markAsDiff();
														
												if (DEBUG){
													Logging.log(this, "   ..reporting the DIFF TABLE via " + tChannelToHead + ":");
													int j = 0;
													for(RoutingEntry tEntry : tReportRoutingTable){
														Logging.log(this, "     ..[" + j +"]: " + tEntry);
														j++;
													}
												}
											}
											
											mL0LastReportedRoutingTableWasDuringUnstableHierarchy = false;
										}else{
											mL0LastReportedRoutingTableWasDuringUnstableHierarchy = true;
										}
										
										/**
										 * Remember the time of the last reported complete routing table -> report every x seconds a complete table
										 */
										if(!tReportOnlyADiff){
											// report a complete routing table
											mL0LastSentReportedRoutingTable = (RoutingTable) tReportRoutingTable.clone();
											// store the time
											mL0TimeLastCompleteReportedRoutingTable = mHRMController.getSimulationTime();
										}
										
										/**
										 * SEND REPORT
										 */
										if((tChannelToHead != null) && (tReportRoutingTable.size() > 0)){
											// create new RouteReport packet for the superior coordinator, constructor also sets the timeout for each routing table entry
											RouteReport tRouteReportPacket = new RouteReport(tLocalHRMID, tClusterHeadHRMID, mHRMController, tReportRoutingTable);
											// send the packet to the superior coordinator
											tChannelToHead.sendPacket(tRouteReportPacket);
										}
									}else{
										if (DEBUG){
											Logging.log(this, "reportPhase() aborted because no report for " + tChannelToHead + " available");
										}
									}
								}else{
									Logging.warn(this, "reportPhaseL0ClusterMember() aborted due to invalid cluster manager HRMID");
								}
							}else{
								Logging.err(this, "Com. channel to manager is invalid, aborting report phase here");
							}
						}else{
							if(DEBUG){
								Logging.warn(this, "reportPhaseL0ClusterMember() aborted due to invalid local L0 HRMID \"" + tLocalHRMID + "\"");
							}
						}
					}
				}else{
					Logging.warn(this, "reportPhaseL0ClusterMember() aborted due to missing coordinator");
				}
			}else{
				throw new RuntimeException(this + "reportPhaseL0ClusterMember() detected an invalid com. channel to cluster manager");
			}
		}else{
			throw new RuntimeException(this + "reportPhaseL0() is not possible for this hierarchy level");
		}
	}
	
	/**
	 * Returns all active links
	 * 
	 * @return the active links
	 */
	public LinkedList<ComChannel> getActiveLinks()
	{
		LinkedList<ComChannel> tResult = new LinkedList<ComChannel>();
		
		// get all communication channels
		LinkedList<ComChannel> tComChannels = getComChannels();
		
		for(ComChannel tComChannel : tComChannels) {
			if(tComChannel.isLinkActiveForElection()){
				tResult.add(tComChannel);
			}
		}

		return tResult;
	}

	/**
	 * Determines the coordinator of this cluster. It is "null" if the election was lost or hasn't finished yet. 
	 * 
	 * @return the cluster's coordinator
	 */
	public Coordinator getCoordinator()
	{
		Logging.err(this, "!!!!! >>> ClusterMember::getCoordinator() should never be called, otherwise, an error in higher clustering code exists <<< !!!!!");
		return null;
	}

	/**
	 * Returns how many connected cluster members are known
	 * 
	 * @return the count
	 */
	public int countConnectedClusterMembers()
	{
		int tResult = 0;

		// count all communication channels
		tResult = getComChannels().size();

		return tResult;
	}

	/**
	 * Returns how many connected external cluster members are known
	 * 
	 * @return the count
	 */
	public int countConnectedRemoteClusterMembers()
	{
		int tResult = 0;

		// get all communication channels
		LinkedList<ComChannel> tComChannels = getComChannels();

		for(ComChannel tComChannel : tComChannels) {
			// filter loopback channels
			if (tComChannel.toRemoteNode()){
				tResult++;
			}
		}

		return tResult;
	}

	/**
	 * Returns if this ClusterMember belongs to a remote cluster
	 * 
	 * @return true or false
	 */
	public boolean isRemoteCluster()
	{
		if(this instanceof Cluster){
			return false;
		}
		
		return (countConnectedRemoteClusterMembers() > 0);
	}
	
	/**
	 * EVENT: the local node has a new connectivity node priority (for L0 elections)
	 * 
	 * @param pNewConnectivityNodePriority the new connectivity node priority
	 */
	public void eventConnectivityNodePriorityUpdate(long pNewConnectivityNodePriority)
	{
		Logging.log(this, "EVENT: base node priority update to:  " + pNewConnectivityNodePriority);
		
		if(getHierarchyLevel().isBaseLevel()){
			/**
			 * Set the new priority if it differs from the old one
			 */
			if((getPriority() == null) || (getPriority().getValue() != pNewConnectivityNodePriority) || (this instanceof Cluster /* a Cluster always reports the current priority! */)){
				Logging.log(this, "Got new connectivity node priority, updating own priority from " + (getPriority() != null ? getPriority().getValue() : "null") + " to " + pNewConnectivityNodePriority);
				setPriority(ElectionPriority.create(this, pNewConnectivityNodePriority));
			}else{
				//Logging.log(this, "   ..skipping priority update, current priority: " + getPriority());
			}
		}else{
			throw new RuntimeException("Got a call to ClusterMemeber::eventConnectivityNodePriorityUpdate at higher hierarchy level " + getHierarchyLevel().getValue());
		}
	}

	/**
	 * EVENT: new hierarchy node priority
	 * 
	 * @param pNewHierarchyNodePriority the new hierarchy node priority
	 */
	public void eventHierarchyNodePriorityUpdate(long pNewHierarchyNodePriority)
	{
		Logging.log(this, "EVENT: hierarchy node priority update to: " + pNewHierarchyNodePriority);
		
		/**
		 * Set the new priority if it differs from the old one
		 */
		if((getPriority() == null) || (getPriority().getValue() != pNewHierarchyNodePriority)){
			Logging.log(this, "eventHierarchyNodePriorityUpdate() got new hierarchy node priority, updating own priority from " + getPriority().getValue() + " to " + pNewHierarchyNodePriority);
			setPriority(ElectionPriority.create(this, pNewHierarchyNodePriority));
		}else{
			Logging.log(this, "eventHierarchyNodePriorityUpdate() ignores new hierarchy node priority: " + pNewHierarchyNodePriority + ", old value: " + getPriority());
		}
	}
	
	/**
	 * EVENT: cluster member role invalid
	 * 
	 * @param: pComChannel the comm. channel towards the cluster manager
	 */
	public synchronized void eventClusterMemberRoleInvalid(ComChannel pComChannel)
	{
		Logging.log(this, "============ EVENT: cluster member role invalid, channel: " + pComChannel);
		
		if(isThisEntityValid()){
			/**
			 * Trigger: role invalid
			 */
			eventInvalidation();

			/**
			 * Trigger: Elector invalid
			 */
			getElector().eventInvalidation(this + "::eventClusterMemberRoleInvalid() for: " + pComChannel);

			unregisterComChannel(pComChannel, this + "::eventClusterMemberRoleInvalid()");
	
			Logging.log(this, "============ Destroying this CoordinatorAsClusterMember now...");
	
			/**
			 * Unregister from the HRMController's internal database
			 */ 
			mHRMController.unregisterClusterMember(this);
		}else{
			Logging.warn(this, "This ClusterMember is already invalid");
		}
	}
	
	/**
	 * EVENT: new HRMID assigned
     * The function is called when an address update was received.
	 * 
	 * @param pSourceComChannel the source comm. channel
	 * @param pHRMID the new HRMID
	 * @param pIsFirmAddress is this address firm? 
	 * 
	 * @return true if the signaled address was accepted, other (a former address is requested from the peer) false
	 */
	@Override
	public boolean eventAssignedHRMID(ComChannel pSourceComChannel, HRMID pHRMID, boolean pIsFirmAddress)
	{
		boolean tResult = super.eventAssignedHRMID(pSourceComChannel, pHRMID, pIsFirmAddress);
		
		/**
		 * Try to keep the assigned HRMID even if the hierarchy is restructured
		 */
		if(tResult){
			applyAddressToAlternativeClusters(pHRMID);
		}
		
		return tResult;
	}

	/**
	 * Applies an assigned address to alternative clusters
	 *   
	 * @param pHRMID the assigned HRMID 
	 */
	protected void applyAddressToAlternativeClusters(HRMID pHRMID)
	{
		if(getHierarchyLevel().isBaseLevel()){
			if(!(this instanceof CoordinatorAsClusterMember)){
				// only proceed if we have an associated network interface
				if(getBaseHierarchyLevelNetworkInterface() != null){
					// iterate over all known L0 ClusterMember instances
					LinkedList<ClusterMember> tL0ClusterMembers = mHRMController.getAllClusterMembers(0);
					for(ClusterMember tClusterMember : tL0ClusterMembers){
						// ignore ourself
						if(!getClusterID().equals(tClusterMember.getClusterID())){
							// search for ClusterMembers for the same network interface
							if(getBaseHierarchyLevelNetworkInterface().equals(tClusterMember.getBaseHierarchyLevelNetworkInterface())){
								/**
								 * are we the manager of the current cluster?
								 * 	yes -> inform all parallel ClusterMember instances
								 *  no  -> inform all parallel Cluster instances about a new L0 HRMID and a new cluster HRMID
								 */
								
								if(this instanceof Cluster){
									Cluster tCluster = (Cluster)tClusterMember;
									HRMID tClusterHRMID = pHRMID.getClusterAddress(0);
									tCluster.mHRMID = tClusterHRMID;
									tCluster.setL0HRMID(pHRMID.clone());
								}else{
									tClusterMember.mHRMID = mAssignedL0HRMID.clone();
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Sets a new Election priority
	 * 		HINT: This function might be called from within a separate thread (HRMControllerProcessor).
	 * 
	 * @param pPriority the new Election priority
	 */
	@Override
	public void setPriority(ElectionPriority pPriority)
	{
		ElectionPriority tOldPriority = getPriority();
		
		if((pPriority != null) && (!pPriority.isUndefined())){
			/**
			 * Set the new priority
			 */
			super.setPriority(pPriority);
	
			/**
			 * Send priority update if necessary 
			 */
			if ((tOldPriority != null) && (!tOldPriority.isUndefined()) && (!tOldPriority.equals(pPriority))){
				if(mElector != null){
					mElector.updatePriority(this + "::setPriority() from " + tOldPriority.getValue() + " to " + pPriority.getValue());
				}else{
					Logging.warn(this, "Elector is still invalid");
				}
			}else{
				Logging.log(this, "First priority was set: " + pPriority.getValue());
			}
		}else{
			Logging.err(this, "REQUEST FOR SETTING UNDEFINED PRIORITY");
		}
	}

	/**
	 * Returns a hash code for this object.
	 * This function is used within the ARG for identifying objects.
	 * 
	 * @return the hash code
	 */
	@Override
	public int hashCode()
	{
		return getClusterID().intValue();
	}

	/**
	 * Returns the elector of this cluster
	 * 
	 * @return the elector
	 */
	public Elector getElector()
	{
		return mElector;
	}

	/**
	 * Returns the L2 address of the node where the coordinator of the described cluster is located
	 * 
	 * @return the L2 address
	 */
	public L2Address getCoordinatorNodeL2Address()
	{
		return mCoordinatorNodeL2Address;
	}
	
	/**
	 * Sets the cluster activation (has active coordinator), triggered by the Elector or the Cluster which got a new local Coordinator
	 * 
	 * @param pState the new state
	 */
	public void setClusterWithValidCoordinator(boolean pState)
	{
		boolean tOldState = mClusterHasValidCoordinator;
		
		/**
		 * Update the cluster state 
		 */
		if(mClusterHasValidCoordinator != pState){
			Logging.log(this, "Setting cluster activation (has valid coordinator) to: " + pState);
			
			mClusterHasValidCoordinator = pState;
		}

		/**
		 * If it is a transition from "false" to " true", then distribute AnnounceHRMIDs
		 */
		if((!tOldState) && (pState)){
			distributeAnnounceHRMIDs();
		}
	}
	
	/**
	 * Returns the cluster activation (has valid coordinator)
	 *  
	 * @return true or false
	 */
	public boolean hasClusterValidCoordinator()
	{
		return mClusterHasValidCoordinator;
	}

	/**
	 * Updates the state of the AS-Split
	 * 
	 * @param pState the new state
	 * @param pFindReverseCluster should we search for the remote cluster?
	 */
	public void setASSplit(boolean pState, boolean pFindReverseCluster)
	{
		if(getHierarchyLevel().isHigherLevel()){
			Logging.err(this, "AS-split has to be defined at hierarchy level 0");
			return;
		}
		
		Logging.warn(this, "Setting AS-split to: " + pState);
		mEnforceL0AsSplit = pState;
		
		if(pFindReverseCluster){
			LinkedList<ClusterMember> tAllClusterMembers = mHRMController.getAllL0ClusterMembers();
			for(ClusterMember tClusterMember : tAllClusterMembers){
				if((tClusterMember.getBaseHierarchyLevelNetworkInterface() != null) && (tClusterMember.getBaseHierarchyLevelNetworkInterface().getBus().equals(getBaseHierarchyLevelNetworkInterface().getBus()))){
					tClusterMember.setASSplit(pState, false);
				}
			}
		}
	}
	
	/**
	 * Returns the state of the AS-Split
	 * 
	 * @return true or false
	 */
	public boolean enforcesASSplit()
	{
		return mEnforceL0AsSplit;
	}
	
	/**
	 * Defines the decoration text for the ARG viewer
	 * 
	 * @return text for the control entity or null if no text is available
	 */
	@Override
	public String getText()
	{
		return "RemoteCluster" + getGUIClusterID() + "@" + mHRMController.getNodeGUIName() + "@" + getHierarchyLevel().getValue() + "(" + idToString() + ")";
	}

	/**
	 * Returns a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		return toLocation() + "(" + idToString() + ")";
	}

	/**
	 * Returns a location description about this instance
	 */
	@Override
	public String toLocation()
	{
		String tResult = getClass().getSimpleName() + getGUIClusterID() + "@" + mHRMController.getNodeGUIName() + "@" + getHierarchyLevel().getValue();
		
		return tResult;
	}
	
	/**
	 * Returns a string including the ClusterID, the token, and the node priority
	 * 
	 * @return the complex string
	 */
	private String idToString()
	{	
		String tResult = (getGUICoordinatorID() > 0 ? "Coordinator" + getGUICoordinatorID() : "") + (getCoordinatorNodeL2Address() != null ? ", CoordNode.=" + getCoordinatorNodeL2Address() : ""); 
				
		if ((getHRMID() != null) && (!getHRMID().isRelativeAddress())){
			tResult += ", HRMID=" + getHRMID().toString();
		}
		
		if(mEnforceL0AsSplit){
			tResult += ", AS-SPLIT";
		}
		
		return tResult;
	}
}
