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

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.packets.hierarchical.DiscoveryEntry;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborClusterAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.addressing.AssignHRMID;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.RoutingInformation;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.hierarchical.*;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;
import edu.uci.ics.jung.algorithms.shortestpath.BFSDistanceLabeler;

/**
 * This class is used for a coordinator instance and can be used on all hierarchy levels.
 * A cluster's elector instance is responsible for creating instances of this class.
 */
public class Coordinator extends ControlEntity implements ICluster, Localization
{
	/**
	 * This is the GUI specific cluster counter, which allows for globally unique cluster IDs.
	 * It's only used within the GUI. 	
	 */
	private static int sGUICoordinatorID = 0;
	
	/**
	 * The hierarchy level on which this coordinator is located.
	 */
//	private HierarchyLevel mHierarchyLevel; //TODO: remove and use the level from the cluster instance

	/**
	 * List for identification of entities this cluster manager is connected to
	 */
	private LinkedList<Name> mConnectedEntities = new LinkedList<Name>();
	
	/**
	 * Stores the simulation timestamp of the last "share phase"
	 */
	private double mTimeOfLastSharePhase = 0; 
	
	/**
	 * Stores the routes which should be shared with cluster members.
	 */
	private LinkedList<RoutingEntry> mSharedRoutes = new LinkedList<RoutingEntry>();
	
	/**
	 * Stores whether the data of the "shared phase" has changed or not.
	 */
	private boolean mSharedRoutesHaveChanged = false;
	
	/**
	 * Stores the parent cluster, which is managed by this coordinator instance.
	 */
	private Cluster mParentCluster = null;;

	//	private HRMID mHRMID = null;
//	private LinkedList<ComChannel> mCEPs = new LinkedList<ComChannel>();

	private Name mCoordinatorName = null;
	private int mToken;
	private BullyPriority mHighestPriority = null;
	private List<HRMGraphNodeName> mClustersToNotify;
	private LinkedList<Long> mBouncedAnnounces = new LinkedList<Long>();
	private Long mClusterID;
	private LinkedList<NeighborClusterAnnounce> mReceivedAnnouncements;
	
	/**
	 * This is the GUI specific coordinator ID. It is used to allow for an easier debugging.
	 */
	private int mGUICoordinatorID = sGUICoordinatorID++;

	/**
	 * 
	 */
	private static final long serialVersionUID = 6824959379284820010L;
	
	/**
	 * The constructor for a cluster object. Usually, it is called by a cluster's elector instance
	 * 
	 * @param pCluster the parent cluster instance
	 */
	public Coordinator(Cluster pCluster)
	{
		// use the HRMController reference and the hierarchy level from the cluster
		super(pCluster.getHRMController(), pCluster.getHierarchyLevel());
		
		mParentCluster = pCluster;
		
		// clone the HRMID of the managed cluster because it can already contain the needed HRMID prefix address
		setHRMID(this,  mParentCluster.getHRMID().clone());
		
		mClusterID = pCluster.getClusterID();
		
		// register itself as coordinator for the managed cluster
		mParentCluster.setCoordinator(this);

		// register at HRMController's internal database
		getHRMController().registerCoordinator(this);

		Logging.log(this, "CREATED");
	}
	
	/**
	 * Creates a new HRMID for a cluster member depending on the given member number.
	 * 
	 * @param pMemberNumber the member number
	 * @return the new HRMID for the cluster member
	 */
	private HRMID createClusterMemberAddress(int pMemberNumber)
	{
		HRMID tHRMID = getHRMID().clone();
		
		// transform the member number to a BigInteger
		BigInteger tAddress = BigInteger.valueOf(pMemberNumber);

		// set the member number for the given hierarchy level
		tHRMID.setLevelAddress(super.getHierarchyLevel(), tAddress);

		// some debug outputs
		if (HRMConfig.DebugOutput.GUI_HRMID_UPDATES){
			Logging.log(this, "Set " + tAddress + " on hierarchy level " + super.getHierarchyLevel().getValue() + " for HRMID " + tHRMID.toString());
			Logging.log(this, "Created for a cluster member the NEW HRMID=" + tHRMID.toString());
		}
		
		return tHRMID;
	}

	/**
	 * This function is called for distributing HRMIDs among the cluster members.
	 */
	public void signalAddressDistribution()
	{
		/**
		 * The following value is used to assign monotonously growing addresses to all cluster members.
		 * The addressing has to start with "1".
		 */
		int tNextClusterMemberAddress = 1;

		Logging.log(this, "DISTRIBUTING ADDRESSES to entities on level " + (getHierarchyLevel().getValue() - 1) + "/" + (HRMConfig.Hierarchy.HEIGHT - 1));
		
		/**
		 * Assign ourself an HRMID address
		 */
		// are we at the base level?
		if(super.getHierarchyLevel().isBaseLevel()) {
			
			// create new HRMID for ourself
			HRMID tOwnAddress = createClusterMemberAddress(tNextClusterMemberAddress++);

			Logging.log(this, "    ..setting local HRMID " + tOwnAddress.toString());

			//HINT: don't update the HRMID of the coordinator here!
			
			// update the HRMID of the managed cluster by direct call and avoid additional communication overhead
			mParentCluster.setHRMID(this, tOwnAddress);
		}

		/**
		 * Distribute AssignHRMID packets among the cluster members 
		 */
		LinkedList<ComChannel> tComChannels = mParentCluster.getComChannels();
		
		Logging.log(this, "    ..distributing HRMIDs among cluster members: " + tComChannels);
		for(ComChannel tComChannel : tComChannels) {

			//TODO: don't send this update in a loop to ourself!
			//TODO: check if cluster members already have an address and distribute only free addresses here
			
			// create new HRMID for cluster member
			HRMID tHRMID = createClusterMemberAddress(tNextClusterMemberAddress++);

			// store the HRMID under which the peer will be addressable from now 
			tComChannel.setPeerHRMID(tHRMID);
			
			if ((tComChannel.getPeerHRMID() != null) && (!tComChannel.getPeerHRMID().equals(tHRMID))){
				Logging.log(this, "    ..replacing HRMID " + tComChannel.getPeerHRMID().toString() + " and assign new HRMID " + tHRMID.toString() + " to " + tComChannel.getPeerL2Address());
			}else
				Logging.log(this, "    ..assigning new HRMID " + tHRMID.toString() + " to " + tComChannel.getPeerL2Address());

			// create new AssignHRMID packet for the cluster member
			AssignHRMID tAssignHRMID = new AssignHRMID(getHRMController().getNodeName(), tComChannel.getPeerHRMID(), tHRMID);
			
			// register this new HRMID in the local HRS and create a mapping to the right L2Address
			Logging.log(this, "    ..creating MAPPING " + tHRMID.toString() + " to " + tComChannel.getPeerL2Address());
			getHRMController().getHRS().mapHRMIDToL2Address(tHRMID, tComChannel.getPeerL2Address());
			
			// share the route to this cluster member with all other cluster members
			shareRouteToClusterMember(tComChannel);
			
			// send the packet
			tComChannel.sendPacket(tAssignHRMID);
		}
	}
	
	/**
	 * Shares a route to a cluster cluster member with other cluster members
	 * 
	 * @param pClusterMemberChannel the cluster member to whom we have a sharable route
	 */
	private void shareRouteToClusterMember(ComChannel pClusterMemberChannel)
	{
		// determine the HRMID of the cluster member
		HRMID tMemberHRMID = pClusterMemberChannel.getPeerHRMID();
		
		// are we on base hierarchy level?
		if (getHierarchyLevel().getValue() == 1){ // TODO: isBaseLevel()){
			// create the new routing table entry
			RoutingEntry tRoutingEntry = RoutingEntry.createRouteToDirectNeighbor(tMemberHRMID, 0 /* TODO */, 1 /* TODO */, RoutingEntry.INFINITE_DATARATE /* TODO */);
			// define the L2 address of the next hop in order to let "addHRMRoute" trigger the HRS instance the creation of new HRMID-to-L2ADDRESS mapping entry
			tRoutingEntry.setNextHopL2Address(pClusterMemberChannel.getPeerL2Address());
			
			Logging.log(this, "SHARING ROUTE: " + tRoutingEntry);
			
			// add the entry to the local routing table
			getHRMController().addHRMRoute(tRoutingEntry);
			
			// store the entry for route sharing with cluster members
			synchronized (mSharedRoutes){
				/**
				 * Check for duplicates
				 */
				if (HRMConfig.Routing.AVOID_DUPLICATES_IN_ROUTING_TABLES){
					boolean tRestartNeeded;
					do{		
						tRestartNeeded = false;
						for (RoutingEntry tEntry: mSharedRoutes){
							// have we found a route to the same destination which uses the same next hop?
							//TODO: what about multiple links to the same next hop?
							if ((tEntry.getDest().equals(tRoutingEntry.getDest())) /* same destination? */ &&
								(tEntry.getNextHop().equals(tRoutingEntry.getNextHop())) /* same next hop? */){
		
								Logging.log(this, "REMOVING DUPLICATE: " + tEntry);
								
								// remove the route
								mSharedRoutes.remove(tEntry);
								
								// mark "shared phase" data as changed
								mSharedRoutesHaveChanged = true;
								
								// force a restart at the beginning of the routing table
								tRestartNeeded = true;
								//TODO: use a better(scalable) method here for removing duplicates
								break;						
								
							}
						}
					}while(tRestartNeeded);
				}
				
				/**
				 * Add the entry to the shared routing table
				 */
				mSharedRoutes.add(tRoutingEntry);//TODO: use a database per cluster member here
				// mark "shared phase" data as changed
				mSharedRoutesHaveChanged = true;
			}
		}else{
			//TODO
			Logging.log(this, "IMPLEMENT ME - SHARING ROUTE TO: " + pClusterMemberChannel);
		}
	}

	/**
	 * Checks if the "share phase" should be started or not
	 * 
	 * @return true if the "share phase" should be started, otherwise false
	 */
	private boolean sharePhaseHasTimeout()
	{
		// determine the time between two "share phases"
		double tDesiredTimePeriod = getHRMController().getPeriodSharePhase(getHierarchyLevel().getValue() - 1);
		
		// determine the time when a "share phase" has to be started 
		double tTimeNextSharePhase = mTimeOfLastSharePhase + tDesiredTimePeriod;
	
		// determine the current simulation time from the HRMCotnroller instance
		double tCurrentSimulationTime = getHRMController().getSimulationTime();
		
		if (HRMConfig.DebugOutput.GUI_SHOW_TIMING_ROUTE_DISTRIBUTION){
			Logging.log(this, "Checking for timeout of \"share phase\": desired time period is " + tDesiredTimePeriod + ", " + tCurrentSimulationTime + " > " + tTimeNextSharePhase + "? -> " + (tCurrentSimulationTime >= tTimeNextSharePhase));
		}
		
		return (tCurrentSimulationTime >= tTimeNextSharePhase);
	}
	
	/**
	 * Determines if new "share phase" data is available
	 * 
	 * @return true if new data is available, otherwise false
	 */
	private boolean hasNewSharePhaseData()
	{
		boolean tResult = false;
		
		synchronized (mSharedRoutes){
			tResult = mSharedRoutesHaveChanged;
		}
		
		return tResult;
	}
	
	/**
	 * This function implements the "share phase".
	 * It distributes locally stored sharable routing data among the known cluster members
	 */
	public void sharePhase()
	{
		// should we start the "share phase"?
		if (sharePhaseHasTimeout()){
			if (HRMConfig.DebugOutput.SHOW_SHARE_PHASE){
				Logging.log(this, "SHARE PHASE with cluster members on level " + (getHierarchyLevel().getValue() - 1) + "/" + (HRMConfig.Hierarchy.HEIGHT - 1));
			}

			// store the time of this "share phase"
			mTimeOfLastSharePhase = getHRMController().getSimulationTime();

			if ((!HRMConfig.Routing.PERIODIC_SHARE_PHASES) && (!hasNewSharePhaseData())){
				if (HRMConfig.DebugOutput.SHOW_SHARE_PHASE){
					Logging.log(this, "SHARE PHASE skipped because routing data hasn't changed since last signaling round");
				}
				return;
			}
			
			/**
			 * SHARE PHASE 
			 */
			// determine own local cluster address
			HRMID tOwnClusterAddress = mParentCluster.getHRMID();
	
			if (HRMConfig.DebugOutput.SHOW_SHARE_PHASE){
				Logging.log(this, "    ..distributing as " + tOwnClusterAddress.toString() + " aggregated ROUTES among cluster members: " + mParentCluster.getComChannels());
			}
			
			synchronized (mSharedRoutes){
				// send the routing information to cluster members
				for(ComChannel tClusterMember : mParentCluster.getComChannels()) {
					RoutingInformation tRoutingInformationPacket = new RoutingInformation(tOwnClusterAddress, tClusterMember.getPeerHRMID());
				
					// are we on base hierarchy level?
					if (getHierarchyLevel().getValue() == 1){ // TODO: isBaseLevel()){
	
						/**
						 * ADD ROUTES: routes from the cluster member to this node for every registered local HRMID.
						 */
						// determine the L2 address of this physical node
						L2Address tPhysNodeL2Address = getHRMController().getHRS().getCentralFNL2Address();
						// iterate over all HRMIDs which are registered for this physical node
						for (HRMID tHRMID : getHRMController().getOwnHRMIDs()){
							// create entry for cluster internal routing towards us
							RoutingEntry tRouteFromClusterMemberToHere = RoutingEntry.createRouteToDirectNeighbor(tHRMID, 0 /* TODO */, 1 /* TODO */, RoutingEntry.INFINITE_DATARATE /* TODO */);
							// define the L2 address of the next hop in order to let the receiver store it in its HRMID-to-L2ADDRESS mapping
							tRouteFromClusterMemberToHere.setNextHopL2Address(tPhysNodeL2Address);
							// add the route in the "share phase" signaling
							tRoutingInformationPacket.addRoute(tRouteFromClusterMemberToHere);
						}
						
						//TODO: need routing graph here!
						
						//TODO: routes to other cluster members
					}else{
						//TODO: implement me
					}	
					
					/**
					 * Send the routing data to the cluster member
					 */
					// do we have interesting routing information?
					if (tRoutingInformationPacket.getRoutes().size() > 0){
						tClusterMember.sendPacket(tRoutingInformationPacket);
					}else{
						// no routing information -> no packet is sent
					}
				}

				/**
				 * mark "share phase" data as known
				 */
				mSharedRoutesHaveChanged = false;
			}
		}else{
			// share phase shouldn't be started, we have to wait until next trigger
		}
	}
	
	/**
	 * This function implements the "report phase".
	 * It sends locally stored sharable routing data to the superior coordinator
	 */
	public void reportPhase()
	{
		if (!getHierarchyLevel().isHighest()){
			
		}else{
			// we are the highest hierarchy level, no one to send topology reports to
		}
	}
	
	/**
	 * EVENT: "announced", triggered by Elector if the election was won and this coordinator was announced to all cluster members 	 
	 */
	public void eventAnnounced()
	{
		/**
		 * AUTO ADDRESS DISTRIBUTION
		 */
		if (HRMConfig.Addressing.ASSIGN_AUTOMATICALLY){
			Logging.log(this, "EVENT ANNOUNCED - triggering address assignment for " + getComChannels().size() + " cluster members");

			signalAddressDistribution();
		}

		
		//TODO: ??
		getCluster().getHRMController().setSourceIntermediateCluster(this, getCluster());

		/**
		 * AUTO CLUSTERING
		 */
		if(getHierarchyLevel().getValue() < HRMConfig.Hierarchy.HEIGHT) {
			if (HRMConfig.Hierarchy.BUILD_AUTOMATICALLY){
				Logging.log(this, "EVENT ANNOUNCED - triggering clustering of this cluster's coordinator and its neighbors");

				// start the clustering of this cluster's coordinator and its neighbors
				clusterCoordinators();
			}
		}
	}

	//TODO: fix this +1 stuff
	@Override
	public HierarchyLevel getHierarchyLevel() {
		return new HierarchyLevel(this, super.getHierarchyLevel().getValue() + 1);
	}

	/**
	 * Returns the Bully priority of the parent cluster
	 * 
	 * @return the Bully priority
	 */
	@Override
	public BullyPriority getPriority() 
	{
		// return the Bully priority of the managed cluster object
		return mParentCluster.getPriority();
	}
	
	/**
	 * Sets a new Bully priority
	 * 
	 * @param pPriority the new Bully priority
	 */
	@Override
	public void setPriority(BullyPriority pPriority) 
	{
		if (!getPriority().equals(pPriority)){
			Logging.err(this, "Updating Bully priority from " + getPriority() + " to " + pPriority);
		}else{
			Logging.log(this, "Trying to set same Bully priority " + getPriority());
		}
		
		// update the Bully priority of the parent cluster, which is managed by this coordinator
		mParentCluster.setPriority(pPriority);
	}

	
	

	
	
	public void storeAnnouncement(NeighborClusterAnnounce pAnnounce)
	{
		Logging.log(this, "Storing " + pAnnounce);
		if(mReceivedAnnouncements == null) {
			mReceivedAnnouncements = new LinkedList<NeighborClusterAnnounce>();
		}
		pAnnounce.setNegotiatorIdentification(new ClusterName(mParentCluster.getToken(), mParentCluster.getClusterID(), mParentCluster.getHierarchyLevel()));
		mReceivedAnnouncements.add(pAnnounce);
	}
	
	public LinkedList<Long> getBounces()
	{
		return mBouncedAnnounces;
	}
	
	public void clusterCoordinators()
	{
		Logging.log(this, "CLUSTERING STARTED on hierarchy level " + getHierarchyLevel().getValue() + ", will connect to " + mParentCluster.getNeighbors());
		
		// are we already at the highest hierarchy level?
		if (getHierarchyLevel().isHighest()){
			Logging.warn(this,  "CLUSTERING SKIPPED, no clustering on highest hierarchy level " + getHierarchyLevel().getValue() + " needed");
			return;
		}
			
		int tRadius = HRMConfig.Routing.EXPANSION_RADIUS;

		Logging.log(this, "Radius is " + tRadius);

		BFSDistanceLabeler<HRMGraphNodeName, RoutableClusterGraphLink> tBreadthFirstSearch = new BFSDistanceLabeler<HRMGraphNodeName, RoutableClusterGraphLink>();

		for(int i = 1; i <= tRadius; i++) {
			
			String tString = new String(">>> Expanding to radius (" + i + "/" + tRadius + ", possible clusters:");
			for(Cluster tCluster : getHRMController().getRoutingTargetClusters()) {
				if(tCluster.getHierarchyLevel().getValue() == getHierarchyLevel().getValue() - 1) {
					tString += "\n" + tCluster.toString();
				}
			}
			Logging.log(this, tString);
			
			// compute the distances of all the node from the managed cluster
			tBreadthFirstSearch.labelDistances(getHRMController().getRoutableClusterGraph().getGraphForGUI(), mParentCluster);
			
			mClustersToNotify = tBreadthFirstSearch.getVerticesInOrderVisited();
			List<HRMGraphNodeName> tClustersToNotify = new LinkedList<HRMGraphNodeName>(); 
			Logging.log(this, "Clusters remembered for notification: " + mClustersToNotify);
			for(HRMGraphNodeName tNode : mClustersToNotify) {
				if(tNode instanceof Cluster && i == 1) {
					tClustersToNotify.add(tNode);
				} else if (tNode instanceof NeighborCluster && ((NeighborCluster)tNode).getClusterDistanceToTarget() <= i && ((NeighborCluster)tNode).getClusterDistanceToTarget() != 0 && !mConnectedEntities.contains(((NeighborCluster)tNode).getCoordinatorName())) {
					tClustersToNotify.add(tNode);					
				}
			}
			mClustersToNotify = tClustersToNotify;
			Logging.log(this, "clusters that are remaining for this round: " + mClustersToNotify);
			connectToNeighborCoordinators(i);
		}
		/*
		for(CoordinatorCEP tCEP : mCEPs) {
			tCEP.write(new BullyElect(mParentCluster.getPriority(), pLevel, getCoordinator().getReferenceNode().getCentralFN().getName(), null));
		}
		*/
		Logging.log(this, "has a total of the following connections to higher candidates" + getComChannels().size());
	}
	
	public LinkedList<RoutingServiceLinkVector> getPathToCoordinator(ICluster pSourceCluster, ICluster pDestinationCluster)
	{
		List<Route> tCoordinatorPath = getHRMController().getHRS().getCoordinatorRoutingMap().getRoute(((ControlEntity)pSourceCluster).superiorCoordinatorL2Address(), ((ControlEntity)pDestinationCluster).superiorCoordinatorL2Address());
		LinkedList<RoutingServiceLinkVector> tVectorList = new LinkedList<RoutingServiceLinkVector>();
		if(tCoordinatorPath != null) {
			for(Route tPath : tCoordinatorPath) {
				tVectorList.add(new RoutingServiceLinkVector(tPath, getHRMController().getHRS().getCoordinatorRoutingMap().getSource(tPath), getHRMController().getHRS().getCoordinatorRoutingMap().getDest(tPath)));
			}
		}
		return tVectorList;
	}
	
	private HRMGraphNodeName getFarthestVirtualNodeInDirection(HRMGraphNodeName pSource, HRMGraphNodeName pTarget)
	{
		List<RoutableClusterGraphLink> tList = getHRMController().getRoutableClusterGraph().getRoute(pSource, pTarget);

		//ICluster tFarthestCluster = null;
		HRMGraphNodeName tResult = pSource;
		try {
			int tDistance = 0;
			if(tList.size() > HRMConfig.Routing.EXPANSION_RADIUS) {
				while(tDistance != HRMConfig.Routing.EXPANSION_RADIUS) {
					tResult = getHRMController().getRoutableClusterGraph().getLinkEndNode(tResult, tList.get(0));
					tList.remove(0);
					tDistance++;
				}
				return tResult;
			} else {
				return pTarget;
			}
		} catch (Exception tExc) {
			Logging.err(this, "Unable to determine cluster that is farthest in direction from " + pSource + " to target " + pTarget);
			return null;
		}
	}
	
	private boolean connectToNeighborCoordinators(int radius)
	{
		for(HRMGraphNodeName tNode : mClustersToNotify) {
			if(tNode instanceof ICluster) {
				ICluster tCluster = (ICluster)tNode;
				Name tName = tCluster.getCoordinatorName();
				synchronized(tCluster) {
					if(tName == null) {
						try {
							tCluster.wait();
						} catch (InterruptedException tExc) {
							Logging.err(this, tCluster + " is skipped on cluster discovery", tExc);
						}
					}
				}
				
				if(mConnectedEntities.contains(tName)){
					Logging.log(this, "L" + super.getHierarchyLevel().getValue() + "-skipping connection to " + tName + " for cluster " + tNode + " because connection already exists");
					continue;
				} else {
					/*
					 * was it really this cluster? -> reevaluate
					 */
					Logging.log(this, "L" + super.getHierarchyLevel().getValue() + "-adding connection to " + tName + " for cluster " + tNode);
					getMultiplexer().connectToNeighborCoordinator(tCluster, this);
					//new CoordinatorCEP(mParentCluster.getCoordinator().getLogger(), mParentCluster.getCoordinator(), this, false);
					mConnectedEntities.add(tName);
				}
			}
		}
		return true;
	}
	
	public void setCoordinatorPriority(BullyPriority pCoordinatorPriority) {
		if(superiorCoordinatorComChannel() != null) {
			if (!superiorCoordinatorComChannel().getPeerPriority().equals(pCoordinatorPriority)){
				Logging.info(this, "Tried to set a priority that does not correspond with the priority of the concurrent coordinator, wrong connection endpoint?");
			}
			superiorCoordinatorComChannel().setPeerPriority(pCoordinatorPriority);
		}
	}

	/**
	 * Determines the Bully priority of the superior coordinator.
	 * 
	 * @return the Bully priority of the superior coordinator
	 */
	public BullyPriority getCoordinatorPriority() {
		if(superiorCoordinatorComChannel() != null) {
			return superiorCoordinatorComChannel().getPeerPriority();
		}
		return null;
	}

	@Override
	public Long getClusterID() {
		return mClusterID;
	}

	@Override
	public Name getCoordinatorName() {
		return mCoordinatorName;
	}

	@Override
	public String getClusterDescription() {
		return getClass().getSimpleName() + "(" + mParentCluster + ")";
	}

	@Override
	public void setCoordinatorName(Name pCoordName) {
		mCoordinatorName = pCoordName;
	}

	@Override
	public void setToken(int pToken) {
		if(mToken != 0) {
			Logging.log(this, "######################### Updating token to " + pToken);
		}
		mToken = pToken;
	}

	@Override
	public int getToken() {
		return mToken;
	}

	@Override
	public LinkedList<ICluster> getNeighbors() {
		return new LinkedList<ICluster>();
	}

	@Override
	public BullyPriority getHighestPriority() {
		return mHighestPriority;
	}
	
	public void handleBullyAnnounce(BullyAnnounce pAnnounce, ComChannel pCEP)
	{
		/**
		 * the name of the cluster, which is managed by this coordinator
		 */
		ClusterName tLocalManagedClusterName = new ClusterName(mParentCluster.getToken(), mParentCluster.getClusterID(), mParentCluster.getHierarchyLevel());

		/*
		 * check whether old priority was lower than new priority
		 */
		if(pAnnounce.getSenderPriority().isHigher(this, getCoordinatorPriority())) {
			/*
			 * check whether a coordinator is already set
			 */
			if(superiorCoordinatorComChannel() != null) {
				if(getCoordinatorName() != null && !pAnnounce.getSenderName().equals(getCoordinatorName())) {
					/*
					 * a coordinator was set earlier -> therefore inter-cluster communicatino is necessary
					 * 
					 * find the route from the managed cluster to the cluster this entity got to know the higher cluster
					 */
					List<RoutableClusterGraphLink> tClusterList = getHRMController().getRoutableClusterGraph().getRoute(mParentCluster, pCEP.getRemoteClusterName());
					if(tClusterList.size() > 0) {
						if(getHRMController().getRoutableClusterGraph().getLinkEndNode(pCEP.getRemoteClusterName(), tClusterList.get(tClusterList.size() - 1)) instanceof Cluster) {
							Logging.warn(this, "Not sending neighbor zone announce because another intermediate cluster has a shorter route to target");
							if(tClusterList != null) {
								String tClusterRoute = new String();
								HRMGraphNodeName tTransitiveElement = mParentCluster;
								for(RoutableClusterGraphLink tConnection : tClusterList) {
									tClusterRoute += tTransitiveElement + "\n";
									tTransitiveElement = getHRMController().getRoutableClusterGraph().getLinkEndNode(tTransitiveElement, tConnection);
								}
								Logging.log(this, "cluster route to other entity:\n" + tClusterRoute);
							}
						} else {
							
							/*
							 * This is for the new coordinator - he is being notified about the fact that this cluster belongs to another coordinator
							 * 
							 * If there are intermediate clusters between the managed cluster of this cluster manager we do not send the announcement because in that case
							 * the forwarding would get inconsistent
							 * 
							 * If this is a rejection the forwarding cluster as to be calculated by the receiver of this neighbor zone announcement
							 */
							
							NeighborClusterAnnounce tOldCovered = new NeighborClusterAnnounce(getCoordinatorName(), getHierarchyLevel(), superiorCoordinatorL2Address(),getToken(), superiorCoordinatorL2Address().getComplexAddress().longValue());
							tOldCovered.setCoordinatorsPriority(superiorCoordinatorComChannel().getPeerPriority());
							tOldCovered.setNegotiatorIdentification(tLocalManagedClusterName);
							
							DiscoveryEntry tOldCoveredEntry = new DiscoveryEntry(mParentCluster.getToken(), mParentCluster.getCoordinatorName(), mParentCluster.superiorCoordinatorL2Address().getComplexAddress().longValue(), mParentCluster.superiorCoordinatorL2Address(), mParentCluster.getHierarchyLevel());
							/*
							 * the forwarding cluster to the newly discovered cluster has to be one level lower so it is forwarded on the correct cluster
							 * 
							 * calculation of the predecessor is because the cluster identification on the remote site is multiplexed
							 */
							tClusterList = getHRMController().getRoutableClusterGraph().getRoute(mParentCluster, superiorCoordinatorComChannel().getRemoteClusterName());
							if(!tClusterList.isEmpty()) {
								ICluster tPredecessor = (ICluster) getHRMController().getRoutableClusterGraph().getLinkEndNode(mParentCluster, tClusterList.get(0));
								tOldCoveredEntry.setPredecessor(new ClusterName(tPredecessor.getToken(), tPredecessor.getClusterID(), tPredecessor.getHierarchyLevel()));
							}
							tOldCoveredEntry.setPriority(getCoordinatorPriority());
							tOldCoveredEntry.setRoutingVectors(pCEP.getPath(mParentCluster.superiorCoordinatorL2Address()));
							tOldCovered.setCoveringClusterEntry(tOldCoveredEntry);
//							List<Route> tPathToCoordinator = getCoordinator().getHRS().getCoordinatorRoutingMap().getRoute((HRMName)pCEP.getSourceName(), getCoordinatorsAddress());
							//tOldCovered.setAnnouncer(getCoordinator().getHRS().getCoordinatorRoutingMap().getDest(tPathToCoordinator.get(0)));
							pCEP.sendPacket(tOldCovered);
							
							/*
							 * now the old cluster is notified about the new cluster
							 */
							
							NeighborClusterAnnounce tNewCovered = new NeighborClusterAnnounce(pAnnounce.getSenderName(), getHierarchyLevel(), pCEP.getPeerL2Address(), pAnnounce.getToken(), pCEP.getPeerL2Address().getComplexAddress().longValue());
							tNewCovered.setCoordinatorsPriority(pAnnounce.getSenderPriority());
							tNewCovered.setNegotiatorIdentification(tLocalManagedClusterName);
							DiscoveryEntry tCoveredEntry = new DiscoveryEntry(pAnnounce.getToken(),	pAnnounce.getSenderName(), (pCEP.getPeerL2Address()).getComplexAddress().longValue(), pCEP.getPeerL2Address(),	getHierarchyLevel());
							tCoveredEntry.setRoutingVectors(pCEP.getPath(pCEP.getPeerL2Address()));
							tNewCovered.setCoveringClusterEntry(tCoveredEntry);
							tCoveredEntry.setPriority(pAnnounce.getSenderPriority());
							
							List<RoutableClusterGraphLink> tClusters = getHRMController().getRoutableClusterGraph().getRoute(mParentCluster, pCEP.getRemoteClusterName());
							if(!tClusters.isEmpty()) {
								ICluster tNewPredecessor = (ICluster) getHRMController().getRoutableClusterGraph().getLinkEndNode(mParentCluster, tClusters.get(0));
								tCoveredEntry.setPredecessor(new ClusterName(tNewPredecessor.getToken(), tNewPredecessor.getClusterID(), tNewPredecessor.getHierarchyLevel()));
							}
							Logging.warn(this, "Rejecting " + (superiorCoordinatorComChannel().getPeerL2Address()).getDescr() + " in favor of " + pAnnounce.getSenderName());
							tNewCovered.setRejection();
							superiorCoordinatorComChannel().sendPacket(tNewCovered);
							for(ComChannel tCEP : getComChannels()) {
								if(pAnnounce.getCoveredNodes().contains(tCEP.getPeerL2Address())) {
									tCEP.setAsParticipantOfMyCluster(true);
								} else {
									tCEP.setAsParticipantOfMyCluster(false);
									
								}
							}
							setToken(pAnnounce.getToken());
							setSuperiorCoordinator(pCEP, pAnnounce.getSenderName(),pAnnounce.getToken(),  pCEP.getPeerL2Address());
							getHRMController().setClusterWithCoordinator(getHierarchyLevel(), this);
							superiorCoordinatorComChannel().sendPacket(tNewCovered);
						}
					}
				}
				
			} else {
				if (pAnnounce.getCoveredNodes() != null){
					for(ComChannel tCEP : getComChannels()) {
						if(pAnnounce.getCoveredNodes().contains(tCEP.getPeerL2Address())) {
							tCEP.setAsParticipantOfMyCluster(true);
						} else {
							tCEP.setAsParticipantOfMyCluster(false);
						}
					}
				}
				setToken(pAnnounce.getToken());
				getHRMController().setClusterWithCoordinator(getHierarchyLevel(), this);
				setSuperiorCoordinator(pCEP, pAnnounce.getSenderName(), pAnnounce.getToken(), pCEP.getPeerL2Address());
			}
		} else {
			/*
			 * this part is for the coordinator that intended to announce itself -> send rejection and send acting coordinator along with
			 * the announcement that is just gained a neighbor zone
			 */
			
			NeighborClusterAnnounce tUncoveredAnnounce = new NeighborClusterAnnounce(getCoordinatorName(), getHierarchyLevel(), superiorCoordinatorL2Address(), getToken(), superiorCoordinatorL2Address().getComplexAddress().longValue());
			tUncoveredAnnounce.setCoordinatorsPriority(superiorCoordinatorComChannel().getPeerPriority());
			/*
			 * the routing service address of the announcer is set once the neighbor zone announce arrives at the rejected coordinator because this
			 * entity is already covered
			 */
			
			tUncoveredAnnounce.setNegotiatorIdentification(tLocalManagedClusterName);
			
			DiscoveryEntry tUncoveredEntry = new DiscoveryEntry(mParentCluster.getToken(), mParentCluster.getCoordinatorName(), mParentCluster.superiorCoordinatorL2Address().getComplexAddress().longValue(), mParentCluster.superiorCoordinatorL2Address(), mParentCluster.getHierarchyLevel());
			List<RoutableClusterGraphLink> tClusterList = getHRMController().getRoutableClusterGraph().getRoute(mParentCluster, pCEP.getRemoteClusterName());
			if(!tClusterList.isEmpty()) {
				ICluster tPredecessor = (ICluster) getHRMController().getRoutableClusterGraph().getLinkEndNode(mParentCluster, tClusterList.get(0));
				tUncoveredEntry.setPredecessor(new ClusterName(tPredecessor.getToken(), tPredecessor.getClusterID(), tPredecessor.getHierarchyLevel()));
			}
			tUncoveredEntry.setPriority(getCoordinatorPriority());
			tUncoveredEntry.setRoutingVectors(pCEP.getPath(mParentCluster.superiorCoordinatorL2Address()));
			tUncoveredAnnounce.setCoveringClusterEntry(tUncoveredEntry);
			Logging.warn(this, "Rejecting " + (superiorCoordinatorComChannel().getPeerL2Address()).getDescr() + " in favour of " + pAnnounce.getSenderName());
			tUncoveredAnnounce.setRejection();
			pCEP.sendPacket(tUncoveredAnnounce);
			
			/*
			 * this part is for the acting coordinator, so NeighborZoneAnnounce is sent in order to announce the cluster that was just rejected
			 */
			
			NeighborClusterAnnounce tCoveredAnnounce = new NeighborClusterAnnounce(pAnnounce.getSenderName(), getHierarchyLevel(), pCEP.getPeerL2Address(), pAnnounce.getToken(), (pCEP.getPeerL2Address()).getComplexAddress().longValue());
			tCoveredAnnounce.setCoordinatorsPriority(pAnnounce.getSenderPriority());
			
//			List<Route> tPathToCoordinator = getCoordinator().getHRS().getCoordinatorRoutingMap().getRoute(pCEP.getSourceName(), pCEP.getPeerName());
			
			//tCoveredAnnounce.setAnnouncer(getCoordinator().getHRS().getCoordinatorRoutingMap().getDest(tPathToCoordinator.get(0)));
			tCoveredAnnounce.setNegotiatorIdentification(tLocalManagedClusterName);
			DiscoveryEntry tCoveredEntry = new DiscoveryEntry(pAnnounce.getToken(), pAnnounce.getSenderName(), (pCEP.getPeerL2Address()).getComplexAddress().longValue(), pCEP.getPeerL2Address(), getHierarchyLevel());
			tCoveredEntry.setRoutingVectors(pCEP.getPath(pCEP.getPeerL2Address()));
			tCoveredAnnounce.setCoveringClusterEntry(tCoveredEntry);
			tCoveredEntry.setPriority(pAnnounce.getSenderPriority());
			tCoveredAnnounce.setCoordinatorsPriority(pAnnounce.getSenderPriority());
			
			List<RoutableClusterGraphLink> tClusters = getHRMController().getRoutableClusterGraph().getRoute(mParentCluster, superiorCoordinatorComChannel().getRemoteClusterName());
			if(!tClusters.isEmpty()) {
				ICluster tNewPredecessor = (ICluster) getHRMController().getRoutableClusterGraph().getLinkEndNode(mParentCluster, tClusters.get(0));
				tUncoveredEntry.setPredecessor(new ClusterName(tNewPredecessor.getToken(), tNewPredecessor.getClusterID(), tNewPredecessor.getHierarchyLevel()));
			}
			Logging.log(this, "Coordinator CEP is " + superiorCoordinatorComChannel());
			superiorCoordinatorComChannel().sendPacket(tCoveredAnnounce);
		}
	}
	
	private ICluster addAnnouncedCluster(NeighborClusterAnnounce pAnnounce, ComChannel pCEP)
	{
		if(pAnnounce.getRoutingVectors() != null) {
			for(RoutingServiceLinkVector tVector : pAnnounce.getRoutingVectors()) {
				getHRMController().getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
			}
		}
		NeighborCluster tCluster = null;
		if(pAnnounce.isAnnouncementFromForeign())
		{
			tCluster = new NeighborCluster(pAnnounce.getCoordAddress().getComplexAddress().longValue(), pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress(), pAnnounce.getToken(), new HierarchyLevel(this, super.getHierarchyLevel().getValue() + 2),	mParentCluster.getHRMController());
			getHRMController().setSourceIntermediateCluster(tCluster, getHRMController().getSourceIntermediate(this));
			((NeighborCluster)tCluster).addAnnouncedCEP(pCEP);
			tCluster.setToken(pAnnounce.getToken());
			tCluster.setPriority(pAnnounce.getCoordinatorsPriority());
			//mParentCluster.addNeighborCluster(tCluster);
		} else {
			Logging.log(this, "Cluster announced by " + pAnnounce + " is an intermediate neighbor ");
		}
		if(pAnnounce.getCoordinatorName() != null) {
			RoutingService tRS = (RoutingService)getHRMController().getNode().getRoutingService();
			if(! tRS.isKnown(pAnnounce.getCoordinatorName())) {
				try {
					getHRMController().getHRS().mapFoGNameToL2Address(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
				} catch (RemoteException tExc) {
					Logging.err(this, "Unable to register " + pAnnounce.getCoordinatorName() + " at higher entity", tExc);
				}
				
			}
		}
		return tCluster;
	}
	
	@Override
	public void handleNeighborAnnouncement(NeighborClusterAnnounce	pAnnounce, ComChannel pCEP)
	{		
		if(pAnnounce.getCoveringClusterEntry() != null) {
//			Cluster tForwardingCluster = null;
			
			if(pAnnounce.isRejected()) {
//				Cluster tMultiplex = mParentCluster;
//				tForwardingCluster = (Cluster) ((Cluster) getCoordinator().getLastUncovered(tMultiplex, pCEP.getRemoteCluster()) == null ? pCEP.getRemoteCluster() : getCoordinator().getLastUncovered(tMultiplex, pCEP.getRemoteCluster())) ;
				//pAnnounce.setAnnouncer( (tForwardingCluster.getCoordinatorsAddress() != null ? tForwardingCluster.getCoordinatorsAddress() : null ));
				Logging.log(this, "Removing " + this + " as participating CEP from " + this);
				getComChannels().remove(this);
			}
			if(pAnnounce.getCoordinatorName() != null) {
				RoutingService tRS = (RoutingService)getHRMController().getNode().getRoutingService();
				if(! tRS.isKnown(pAnnounce.getCoordinatorName())) {
					
					try {
						getHRMController().getHRS().mapFoGNameToL2Address(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
					} catch (RemoteException tExc) {
						Logging.err(this, "Unable to register " + pAnnounce.getCoordinatorName() + " at name mapping service", tExc);
					}
				}
			}
			try {
				pCEP.handleDiscoveryEntry(pAnnounce.getCoveringClusterEntry());
			} catch (PropertyException tExc) {
				Logging.log(this, "Unable to fulfill requirements");
			}
			Logging.log(this, "new negotiating cluster will be " + getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
		} else {
			Logging.log(this, "new negotiating cluster will be " + getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
		}
	}

	@Override
	public void setSuperiorCoordinator(ComChannel pCoordinatorComChannel, Name pCoordinatorName, int pCoordToken, L2Address pCoordinatorL2Address) 
	{
		/**
		 * the name of the cluster, which is managed by this coordinator
		 */
		ClusterName tLocalManagedClusterName = new ClusterName(mParentCluster.getToken(), mParentCluster.getClusterID(), mParentCluster.getHierarchyLevel());
		setToken(pCoordToken);
		
		Logging.log(this, "Setting channel to superior coordinator to " + pCoordinatorComChannel + " for coordinator " + pCoordinatorName + " with routing address " + pCoordinatorL2Address);
		Logging.log(this, "Previous channel to superior coordinator was " + superiorCoordinatorComChannel() + " with name " + mCoordinatorName);
		setSuperiorCoordinatorComChannel(pCoordinatorComChannel);
		mCoordinatorName = pCoordinatorName;
		synchronized(this) {
			setSuperiorCoordinatorL2Address(pCoordinatorL2Address);
			notifyAll();
		}

		//		LinkedList<CoordinatorCEP> tEntitiesToNotify = new LinkedList<CoordinatorCEP> ();
		for(HRMGraphNodeName tNeighbor: getHRMController().getRoutableClusterGraph().getNeighbors(mParentCluster)) {
			if(tNeighbor instanceof ICluster) {
				for(ComChannel tComChannel : getComChannels()) {
					if(((ControlEntity)tNeighbor).superiorCoordinatorL2Address().equals(tComChannel.getPeerL2Address()) && !tComChannel.isPartOfMyCluster()) {
						Logging.info(this, "Informing " + tComChannel + " about existence of neighbor zone ");
						NeighborClusterAnnounce tAnnounce = new NeighborClusterAnnounce(pCoordinatorName, getHierarchyLevel(), pCoordinatorL2Address, getToken(), pCoordinatorL2Address.getComplexAddress().longValue());
						tAnnounce.setCoordinatorsPriority(superiorCoordinatorComChannel().getPeerPriority());
						LinkedList<RoutingServiceLinkVector> tVectorList = tComChannel.getPath(pCoordinatorL2Address);
						tAnnounce.setRoutingVectors(tVectorList);
						tAnnounce.setNegotiatorIdentification(tLocalManagedClusterName);
						tComChannel.sendPacket(tAnnounce);
					}
					Logging.log(this, "Informed " + tComChannel + " about new neighbor zone");
				}
			}
		}
		if(mReceivedAnnouncements != null) {
			for(NeighborClusterAnnounce tAnnounce : mReceivedAnnouncements) {
				superiorCoordinatorComChannel().sendPacket(tAnnounce);
			}
		}
		
	}

	@Override
	public void addNeighborCluster(ICluster pNeighbor) {
		/*
		 * cluster manager does not need neighbors
		 */
		//TODO: remove this
	}
	
	@Override
	public void setHighestPriority(BullyPriority pHighestPriority) {
		mHighestPriority = pHighestPriority;
	}

	@Override
	public Namespace getNamespace() {
		return new Namespace("clustermanager");
	}
	
	@Override
	public boolean equals(Object pObj)
	{
		if(pObj instanceof Cluster) {
			return false;
		}
		if(pObj instanceof ICluster) {
			ICluster tCluster = (ICluster) pObj;
			if(tCluster.getClusterID().equals(getClusterID()) &&
					tCluster.getToken() == getToken() &&
					tCluster.getHierarchyLevel() == getHierarchyLevel()) {
				return true;
			} else if(tCluster.getClusterID().equals(getClusterID()) && tCluster.getHierarchyLevel() == getHierarchyLevel()) {
				Logging.log(this, "compared to " + pObj + "is false");
				return false;
			} else if (tCluster.getClusterID().equals(getClusterID())) {
				return false;
			}
		}
		return false;
	}	
	
	public Cluster getCluster()
	{
		return mParentCluster;
	}
	
	@Override
	public int getSerialisedSize()
	{
		return 0;
	}

	@Override
	public ComChannelMuxer getMultiplexer() {
		return getHRMController().getCoordinatorMultiplexerOnLevel(this);
	}

	
	
	
	/**
	 * Generates a descriptive string about this object
	 * 
	 * @return the descriptive string
	 */
	public String toString()
	{
		//return getClass().getSimpleName() + (mParentCluster != null ? "(" + mParentCluster.toString() + ")" : "" ) + "TK(" +mToken + ")COORD(" + mCoordinatorSignature + ")@" + mLevel;
		return toLocation() + "(" + (mParentCluster != null ? "Cluster" + mParentCluster.getGUIClusterID() + ", " : "") + idToString() + ")";
	}

	@Override
	public String toLocation()
	{
		String tResult = getClass().getSimpleName() + mGUICoordinatorID + "@" + getHRMController().getNodeGUIName() + "@" + (getHierarchyLevel().getValue() - 1);
		
		return tResult;
	}

	private String idToString()
	{
		if (getHRMID() == null){
			return "ID=" + getClusterID() + ", Tok=" + mToken +  ", NodePrio=" + getPriority().getValue();
		}else{
			return "HRMID=" + getHRMID().toString();
		}
	}
}
