/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.coordination;

import java.io.Serializable;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.exceptions.AuthenticationException;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.packets.hierarchical.DiscoveryEntry;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborClusterAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyData;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyData.FIBEntry;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyPriorityUpdate;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.hierarchical.*;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ClusterName;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.HierarchyLevel;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ICluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.IRoutableClusterGraphTargetName;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.NeighborCluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.RoutableClusterGraphLink;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.graph.RoutableGraph;
import edu.uci.ics.jung.algorithms.shortestpath.BFSDistanceLabeler;

/**
 * This class is used for a coordinator instance and can be used on all hierarchy levels.
 * A cluster's elector instance is responsible for creating instances of this class.
 */
public class Coordinator implements ICluster, HRMEntity
{
	/**
	 * This is the GUI specific cluster counter, which allows for globally unique cluster IDs.
	 * It's only used within the GUI. 	
	 */
	private static int sGUICoordinatorID = 0;
	
	/**
	 * The hierarchy level on which this coordinator is located.
	 */
	private HierarchyLevel mHierarchyLevel; //TODO: remove and use the level from the cluster instance

	/**
	 * List for identification of entities this cluster manager is connected to
	 */
	private LinkedList<Name> mConnectedEntities = new LinkedList<Name>();
	
	/**
	 * The following value is used in order to create each turn a monotonously growing address.
	 */
	private int mLastCreatedAddress;

	private HRMSignature mSignature = null;
	
	private HRMID mHRMID = null;
	private Cluster mManagedCluster;
	private HashMap<CoordinatorCEPChannel, TopologyData> mAddressMapping = null;
	private LinkedList<CoordinatorCEPChannel> mCEPs = null;
	private CoordinatorCEPChannel mCoordinatorCEP = null;
	private HRMSignature mCoordinatorSignature = null;
	private Name mCoordinatorName = null;
	private HRMName mCoordinatorAddress = null;
	private int mToken;
	private BullyPriority mHighestPriority = null;
	private List<IRoutableClusterGraphTargetName> mClustersToNotify;
	private LinkedList<Long> mBouncedAnnounces = new LinkedList<Long>();
	private int mReceivedAnnounces = 0;
//	private LinkedList<Name> mIgnoreOnAddressDistribution = null;
	private Long mClusterID;
	private LinkedList<HRMID> mHigherHRMIDs = null;
	private TopologyData mTopologyData = null;
	private HashMap<HRMID, IRoutableClusterGraphTargetName> mAddressToClusterMapping = new HashMap<HRMID, IRoutableClusterGraphTargetName>();
	private HashMap<HRMID, FIBEntry> mIDToFIBMapping = new HashMap<HRMID, FIBEntry>();
	private LinkedList<NeighborClusterAnnounce> mReceivedAnnouncements;
	private LinkedList<HRMSignature> mSignatures = new LinkedList<HRMSignature>();
//	private HashMap<Long, CoordinatorCEPChannel> mRouteRequestDispatcher;
	private HashMap<HRMID, LinkedList<RoutingServiceLinkVector>> mAddressToPathMapping;
	
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
		mManagedCluster = pCluster;
		mHRMID =  mManagedCluster.getHrmID();
		mHierarchyLevel = mManagedCluster.getHierarchyLevel();
		mClusterID = pCluster.getClusterID();
		mLastCreatedAddress = 0;
		mAddressMapping = new HashMap<CoordinatorCEPChannel, TopologyData>();
		mCEPs = new LinkedList<CoordinatorCEPChannel>();
		
		// creates the coordinator signature
		mSignature = getHRMController().createCoordinatorSignature(this);

		// register itself as coordinator for the managed cluster
		mManagedCluster.setCoordinator(this);

		// register at HRMController's internal database
		getHRMController().registerCoordinator(this);

		Logging.log(this, "CREATED");
	}
	
	/**
	 * Returns the coordinator HRMSignature
	 * 
	 * @return the signature
	 */
	public HRMSignature getSignature()
	{
		return mSignature;
	}

	public void storeAnnouncement(NeighborClusterAnnounce pAnnounce)
	{
		Logging.log(this, "Storing " + pAnnounce);
		if(mReceivedAnnouncements == null) {
			mReceivedAnnouncements = new LinkedList<NeighborClusterAnnounce>();
		}
		pAnnounce.setNegotiatorIdentification(new ClusterName(mManagedCluster.getToken(), mManagedCluster.getClusterID(), mManagedCluster.getHierarchyLevel()));
		mReceivedAnnouncements.add(pAnnounce);
	}
	
	public LinkedList<Long> getBounces()
	{
		return mBouncedAnnounces;
	}
	
	private HRMID createOwnAddress()
	{
		HRMID tID = mHRMID.clone();
		BigInteger tAddress = BigInteger.valueOf(++mLastCreatedAddress);
		tID.setLevelAddress(mHierarchyLevel, tAddress);
//		if(mHierarchyLevel.isHigherLevel()) {
//			HRMIPMapper.registerHRMID(tID);
//		}
		Logging.log(this, "NEW HRMID=" + tID);
		
		return tID;
	}
	
	public boolean clusterCoordinators()
	{
		Logging.log(this, "CLUSTERING STARTED, will connect to " + mManagedCluster.getNeighbors());
		
		int tRadius = HRMConfig.Routing.EXPANSION_RADIUS;

		Logging.log(this, "Radius is " + tRadius);

		BFSDistanceLabeler<IRoutableClusterGraphTargetName, RoutableClusterGraphLink> tBreadthFirstSearch = new BFSDistanceLabeler<IRoutableClusterGraphTargetName, RoutableClusterGraphLink>();

		for(int i = 1; i <= tRadius; i++) {
			
			String tString = new String(">>> Expanding to radius (" + i + "/" + tRadius + ", possible clusters:");
			for(Cluster tCluster : getHRMController().getRoutingTargetClusters()) {
				if(tCluster.getHierarchyLevel().getValue() == getHierarchyLevel().getValue() - 1) {
					tString += "\n" + tCluster.toString();
				}
			}
			Logging.log(this, tString);
			
			// compute the distances of all the node from the managed cluster
			tBreadthFirstSearch.labelDistances(getHRMController().getRoutableClusterGraph().getGraphForGUI(), mManagedCluster);
			
			mClustersToNotify = tBreadthFirstSearch.getVerticesInOrderVisited();
			List<IRoutableClusterGraphTargetName> tClustersToNotify = new LinkedList<IRoutableClusterGraphTargetName>(); 
			Logging.log(this, "Clusters remembered for notification: " + mClustersToNotify);
			for(IRoutableClusterGraphTargetName tNode : mClustersToNotify) {
				if(!((ICluster)tNode).isInterASCluster()) {
					if(tNode instanceof Cluster && i == 1) {
						tClustersToNotify.add(tNode);
					} else if (tNode instanceof NeighborCluster && ((NeighborCluster)tNode).getClusterDistanceToTarget() <= i && ((NeighborCluster)tNode).getClusterDistanceToTarget() != 0 && !mConnectedEntities.contains(((NeighborCluster)tNode).getCoordinatorName())) {
						tClustersToNotify.add(tNode);					
					}
				}
			}
			mClustersToNotify = tClustersToNotify;
			Logging.log(this, "clusters that are remaining for this round: " + mClustersToNotify);
			connectToNeighbors(i);
		}
		/*
		for(CoordinatorCEP tCEP : mCEPs) {
			tCEP.write(new BullyElect(mManagedCluster.getPriority(), pLevel, getCoordinator().getReferenceNode().getCentralFN().getName(), null));
		}
		*/
		Logging.log(this, "has a total of the following connections to higher candidates" + mCEPs);
		return true;
	}
	
	public LinkedList<RoutingServiceLinkVector> getPathToCoordinator(ICluster pSourceCluster, ICluster pDestinationCluster)
	{
		if(pDestinationCluster instanceof Cluster && ((Cluster)pDestinationCluster).isInterASCluster()) {
			Logging.info(this, "Omitting " + pDestinationCluster + " because it is an inter AS cluster");
		} else {
			List<Route> tCoordinatorPath = getHRMController().getHRS().getCoordinatorRoutingMap().getRoute(pSourceCluster.getCoordinatorsAddress(), pDestinationCluster.getCoordinatorsAddress());
			LinkedList<RoutingServiceLinkVector> tVectorList = new LinkedList<RoutingServiceLinkVector>();
			if(tCoordinatorPath != null) {
				for(Route tPath : tCoordinatorPath) {
					tVectorList.add(new RoutingServiceLinkVector(tPath, getHRMController().getHRS().getCoordinatorRoutingMap().getSource(tPath), getHRMController().getHRS().getCoordinatorRoutingMap().getDest(tPath)));
				}
			}
			return tVectorList;
		}
		return null;
	}
	
	private IRoutableClusterGraphTargetName getFarthestVirtualNodeInDirection(IRoutableClusterGraphTargetName pSource, IRoutableClusterGraphTargetName pTarget)
	{
		List<RoutableClusterGraphLink> tList = getHRMController().getRoutableClusterGraph().getRoute(pSource, pTarget);

		//ICluster tFarthestCluster = null;
		IRoutableClusterGraphTargetName tResult = pSource;
		try {
			int tDistance = 0;
			if(tList.size() > HRMConfig.Routing.EXPANSION_RADIUS) {
				while(tDistance != HRMConfig.Routing.EXPANSION_RADIUS) {
					tResult = getHRMController().getRoutableClusterGraph().getDest(tResult, tList.get(0));
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
	
	/**
	 * This is the function the highest coordinator calls in order to distribute all addresses to its clients
	 * @throws RemoteException 
	 * @throws RequirementsException 
	 * @throws RoutingException 
	 */
	public void signalAddresses() throws RoutingException, RequirementsException, RemoteException
	{
		// reseting HRM ID, TODO: needed here?
		setHRMID(this, new HRMID(0));
		
		/**
		 * the name of the cluster, which is managed by this coordinator
		 */
		ClusterName tLocalManagedClusterName = new ClusterName(mManagedCluster.getToken(), mManagedCluster.getClusterID(), mManagedCluster.getHierarchyLevel());

		/**
		 * Stored the routing DB of the local HRM controller
		 */
		RoutableGraph<HRMName, Route> tLocalRoutingDB = getHRMController().getHRS().getCoordinatorRoutingMap();
		
		TopologyData tManagedClusterTopologyData = new TopologyData();
		Logging.log(this, "Will now distribute addresses to entities on level 0");
		if(mHierarchyLevel.isBaseLevel()) {
			HRMID tOwnAddress = createOwnAddress();
			tManagedClusterTopologyData.assignHRMID(tOwnAddress);
			mManagedCluster.setHRMID(this, tOwnAddress);
		}
		/*
		 * in this part the addresses are mapped either to CEPs or clusters
		 * 
		 * level one : map addresses to connection end points, later retrieveAddress() is used in order to distribute the next hop entry
		 */
		Logging.log(this, "available clients for address distribution: " + mManagedCluster.getClusterMembers());
		for(CoordinatorCEPChannel tReceivingCEP : mManagedCluster.getClusterMembers()) {
			HRMID tID = null;
			TopologyData tTopologyData = new TopologyData();
			try {
					/*
					 * generate next address and map it to a CEP in case we are on level one, or to a cluster in case we are in a level higher than 1
					 */
					tID = createOwnAddress();
					
//TODO: TV			if(mHierarchyLevel.isBaseLevel()) {
//						map(tID, tReceivingCEP);
//					} else {
						map(tID, tReceivingCEP.getRemoteClusterName());
//					}
					tTopologyData.assignHRMID(tID);
				/*
				 * Collect all forwarding entries for connection end point tReceivingCEP, afterwards routes to supernodes are calculated
				 */
				mAddressMapping.put(tReceivingCEP, tTopologyData);
				
				/*
				 * for identification, the cluster gets its generated HRMID
				 */
				
				if ((tReceivingCEP.getRemoteClusterName() != null) && (mHierarchyLevel.isHigherLevel())) {
					tReceivingCEP.getRemoteClusterName().setHRMID(this, tID);
				} else {
					Logging.log(this, "Unable to find remote cluster for " + tReceivingCEP);
				}
			} catch (NullPointerException tExc) {
				Logging.err(this, "Connection endpoint " + tReceivingCEP + " is gone ");
			}
		}
		/*
		 * Now the calculation of the next hops begins
		 * That takes \mathcal{O}(n^2*m*n \log n)
		 */
		
		/*
		 * outer loop: for every source the target is calculated
		 */
		
		try {
			ClusterName tNameFarthestClusterInDirection = null;

			for(CoordinatorCEPChannel tSourceCEP : mAddressMapping.keySet()) {
				
				/*
				 * inner loop: these are the target we mapped an address to, earlier
				 */
				for(CoordinatorCEPChannel tDestinationCEP: mAddressMapping.keySet()) {
					if(tSourceCEP == tDestinationCEP) {
						continue;
					}
					
					tNameFarthestClusterInDirection = null;
					
					/*
					 * if cluster managers level is above one, HRMIDs are mapped to clusters
					 */
					
					// are we on a higher hierarchy level?
					if(mHierarchyLevel.isHigherLevel()) {
						/*
						 * calculate entire cluster route from source to target
						 * 
						 * then: tell the cluster the next neighbor cluster that brings the packet to its target
						 */
						List<RoutableClusterGraphLink> tList = getHRMController().getRoutableClusterGraph().getRoute(tSourceCEP.getRemoteClusterName(), tDestinationCEP.getRemoteClusterName());
						HRMID tFrom = tSourceCEP.getRemoteClusterName().getHrmID();
						HRMID tTo   = tDestinationCEP.getRemoteClusterName().getHrmID();
						
						if(tFrom.equals(tTo)) {
							continue;
						}
						
						/*
						 * Probably that cluster only knows the route to its neighbors, so the route to the next cluster has to be provided
						 * 
						 * As  two clusters can have the same coordinator, we provide the cluster that differs in the address of the coordinator
						 */
						
						ICluster tNextCluster = null;
						try {
							tNextCluster = (ICluster)getHRMController().getRoutableClusterGraph().getDest(tSourceCEP.getRemoteClusterName(), tList.get(0));
						} catch (IndexOutOfBoundsException tExc) {
							Logging.err(this, "Unable to calculate nex hop for " + tFrom + " to " + tTo);
							
							/*
							 * If no cluster neighbor was calculated, the source cluster is provided
							 */
							
							tNextCluster = tSourceCEP.getRemoteClusterName();
						}
						/*
						 * if the coordinator address of the forwarding cluster equals the address of the source coordinator address, take the next cluster
						 */
						if(tNextCluster.getCoordinatorsAddress().equals(tSourceCEP.getRemoteClusterName().getCoordinatorsAddress()) && tList.size() > 1) {
							tNextCluster = (ICluster)getHRMController().getRoutableClusterGraph().getDest(tNextCluster, tList.get(1));
						}
						
						/*
						 * The address of the next hop has to be found out.
						 * Cluster managers above cluster one do not forward by Logical Link Layer addresses, but by HRMIDs that are later on mapped to the next physical router
						 */
						HRMName tNextHop  = null;
						if(!tSourceCEP.getRemoteClusterName().getCoordinatorsAddress().equals(tDestinationCEP.getRemoteClusterName().getCoordinatorsAddress())) {
							tNextHop = getHRMController().getRoutableClusterGraph().getDest(tSourceCEP.getRemoteClusterName(), tList.get(0)).getHrmID();
						}
						
						ClusterName tFibEntryClusterName = new ClusterName(tNextCluster.getToken(), tNextCluster.getClusterID(), tNextCluster.getHierarchyLevel());
						
						FIBEntry tEntry = mAddressMapping.get(tSourceCEP).new FIBEntry(tTo, tNextHop, tFibEntryClusterName, getSignature());
						
						IRoutableClusterGraphTargetName tTargetNode = getFarthestVirtualNodeInDirection(tSourceCEP.getRemoteClusterName(), tDestinationCEP.getRemoteClusterName());
						if(tTargetNode instanceof ICluster) {
							ICluster tCluster = (ICluster)tTargetNode;
							tNameFarthestClusterInDirection = new ClusterName(tCluster.getToken(), tCluster.getClusterID(), tCluster.getHierarchyLevel());
						}
						tEntry.setFarthestClusterInDirection(tNameFarthestClusterInDirection);
						/*
						 * In this case the routes are calculated directly and not mapped to the responding nodes
						 */
						LinkedList<RoutingServiceLinkVector> tPolygon = null;
						tPolygon = getPathToCoordinator(tSourceCEP.getRemoteClusterName(), tDestinationCEP.getRemoteClusterName()); 
						if(tPolygon != null && !tPolygon.isEmpty()) {
							tEntry.setRoutingVectors(tPolygon);
						}
						mAddressMapping.get(tSourceCEP).addForwardingentry(tEntry);
					} else {
						/*
						 * At level one:
						 * In this case the HRMID is mapped to a connection end point, not to a cluster like before
						 * In that case the address is a direct neighbor. So we use the peer routing service address what is supposed to be the logical link layer address
						 * 
						 */
						
						FIBEntry tEntry = mAddressMapping.get(tSourceCEP).new FIBEntry(mAddressMapping.get(tDestinationCEP).getHRMID(), tDestinationCEP.getPeerName(), tLocalManagedClusterName, getSignature());
						
						mAddressMapping.get(tSourceCEP).addForwardingentry(tEntry);
						LinkedList<RoutingServiceLinkVector> tPolygon = getPathToCoordinator(tSourceCEP.getRemoteClusterName(), tDestinationCEP.getRemoteClusterName()); 
						if(!tPolygon.isEmpty()) {
							/*
							 * Here was a strange error, twist while and setting of the routing vector if fowarding is broken
							 */
							while(!tPolygon.getFirst().equals(tSourceCEP.getPeerName())) {
								tPolygon.removeFirst();
								if(tPolygon.isEmpty()) {
									break;
								}
							}
							tEntry.setRoutingVectors(tPolygon);
						}
						IRoutableClusterGraphTargetName tTargetNode = getFarthestVirtualNodeInDirection(tSourceCEP.getRemoteClusterName(), tDestinationCEP.getRemoteClusterName());
						if(tTargetNode instanceof ICluster) {
							ICluster tCluster = (ICluster)tTargetNode;
							tNameFarthestClusterInDirection = new ClusterName(tCluster.getToken(), tCluster.getClusterID(), tCluster.getHierarchyLevel());
						}
						tEntry.setFarthestClusterInDirection(tNameFarthestClusterInDirection);
					}
				}
				
				// are we on basic level?
				if(mHierarchyLevel.isBaseLevel()) {
					/*
					 * The host itself has to tell its client how to reach it: get the address providers address: retrieveAddress() and then give the clients the address of the address provider
					 */
					FIBEntry tEntry = mAddressMapping.get(tSourceCEP).new FIBEntry(mManagedCluster.getHrmID(), tSourceCEP.getSourceName(), tLocalManagedClusterName, getSignature());
					
					mAddressMapping.get(tSourceCEP).addForwardingentry(tEntry);
					IRoutableClusterGraphTargetName tTargetNode = getFarthestVirtualNodeInDirection(tSourceCEP.getRemoteClusterName(), mManagedCluster);
					tNameFarthestClusterInDirection = null;
					if(tTargetNode instanceof ICluster) {
						ICluster tCluster = ((ICluster)tTargetNode);
						tNameFarthestClusterInDirection = new ClusterName(tCluster.getToken(), tCluster.getClusterID(), tCluster.getHierarchyLevel());
					}
					tEntry.setFarthestClusterInDirection(tNameFarthestClusterInDirection);
					/*
					 * Now the managed cluster needs the information on how to reach the next hop
					 */
					FIBEntry tManagedEntry = tManagedClusterTopologyData.new FIBEntry(mAddressMapping.get(tSourceCEP).getHRMID(), tSourceCEP.getPeerName(),	tLocalManagedClusterName, getSignature());
					tManagedClusterTopologyData.addForwardingentry(tManagedEntry);
					IRoutableClusterGraphTargetName tPeerNode = getFarthestVirtualNodeInDirection(mManagedCluster, tSourceCEP.getRemoteClusterName());

					tNameFarthestClusterInDirection = null;
					if(tPeerNode instanceof ICluster) {
						ICluster tCluster = (ICluster)tPeerNode;
						tNameFarthestClusterInDirection = new ClusterName(tCluster.getToken(), tCluster.getClusterID(), tCluster.getHierarchyLevel());
					}
					tEntry.setFarthestClusterInDirection(tNameFarthestClusterInDirection);
				}
				
				/*
				 * Here starts distribution of the HRMIDs provided by higher coordinators
				 */
				
				if(mHigherHRMIDs != null) {
					// are we on a higher hierarchy level?
					if(mHierarchyLevel.isHigherLevel()) {
						for(HRMID tHRMID : mHigherHRMIDs) {
							/*
							 * tNegotiator is the source cluster
							 * tRelevant is the super node that provides the possibility to route the packet to its destination
							 * tHRMIDMapping gets the cluster of the connection end point that announced that cluster.
							 * 
							 * special treatment is used if the negotiator equals the HRMID mapping: we have to tell that cluster how to reach the destination via a routing service link
							 */
							ICluster tNegotiator = tSourceCEP.getRemoteClusterName();
							ICluster tRelevant = ((ICluster)getVirtualNodeFromHRMID(tHRMID));
							ICluster tHRMIDMapping = null;
							if(tRelevant instanceof NeighborCluster) {
								NeighborCluster tNewRelevant = (NeighborCluster)tRelevant;
								CoordinatorCEPChannel tNewRelevantCEP = tNewRelevant.getAnnouncedCEP(tSourceCEP.getRemoteClusterName());
								tHRMIDMapping = tNewRelevantCEP.getNegotiator(tRelevant);
								
							} else {
								//tHRMIDMapping = ((IntermediateCluster)tRelevant).getAnnouncedCEP().getNegotiator(tRelevant);
								tHRMIDMapping = tRelevant;
							}
							
							if(tHRMIDMapping == null) {
								throw new RoutingException("Unable to find forwarding cluster for HRMID " + tHRMID + " while forwarding cluster was " + tRelevant);
							}
							
							LinkedList<RoutingServiceLinkVector> tListToTarget = null;
							
							if(getPathFromHRMID(tHRMID) != null && !getPathFromHRMID(tHRMID).isEmpty() && getPathFromHRMID(tHRMID).contains(tNegotiator.getCoordinatorsAddress())) {
								/*
								 * If a path was provided, we get it and trim it until the entry for the chosen node is found:
								 * For this to work, HRMID and RoutingServiceLinkVector can be compared and are true if the source of the
								 * routing service link equals the routing service address
								 */
								LinkedList<RoutingServiceLinkVector> tPath = getPathFromHRMID(tHRMID);
								if(!tPath.isEmpty()) {
									LinkedList<RoutingServiceLinkVector> tSavedPath = (LinkedList<RoutingServiceLinkVector>) tPath.clone();
									while(!tPath.isEmpty() && !tPath.getFirst().equals(tNegotiator.getCoordinatorsAddress())) {
										tPath.removeFirst();
									}
									Logging.log(this, "Started with initial path " + tSavedPath + " and path is now " + tPath);
								}
								/*
								 * that list is given to the chose node so he can continue in doing what this node currently does: push the route forward through the network
								 */
								tListToTarget = tPath;
							} else {
								/*
								 * If no list is found, we calculate a new route again
								 * getPathToCoordinator calculates the path from a source clusters coordinator to a target coordinator
								 */
								LinkedList<RoutingServiceLinkVector> tRouteToCoordinator = getPathToCoordinator(tNegotiator, tRelevant);
								/*
								 * If we can give that node a route to the target from this point we do so.
								 */
								if(!tRouteToCoordinator.isEmpty()) {
									tListToTarget = tRouteToCoordinator;
								} else {
									tListToTarget = null;
								}
							}
							
							if(tNegotiator.getHierarchyLevel() != tHRMIDMapping.getHierarchyLevel()) {
								Logging.err(this, "Searching for a route between clusters withing different levels");
							}
							
							/*
							 * Calculate forwarding supernodes
							 * 
							 * Get the cluster connection list from the source to the cluster that is able to forward the specified HRMID
							 */
							
							List<RoutableClusterGraphLink> tList = getHRMController().getRoutableClusterGraph().getRoute(tNegotiator, tHRMIDMapping);
							Logging.log(this, "Cluster route from " + tNegotiator + " to " + tHRMIDMapping + " is " + tList);
							
							/*
							 * forwarding is done based on HRMIDs, for a target HRMID the next hop (an HRMID) has to be calculated
							 */
							
							//HRMID tFrom =  tSourceCEP.getRemoteCluster().retrieveAddress();
							HRMID tTo   = tHRMID;
							HRMName tNextHop = null;
							FIBEntry tEntry = null;
							if(tNegotiator.equals(tHRMIDMapping)) {
								/*
								 * Well, in this case, we are just giving the "forwarding cluster" the information, that is has to forward all
								 * entries for the given higher HRMID
								 * 
								 * Furthermore it is not necessary to set the farthest cluster for that entry
								 */
								tNextHop = tHRMID;
								
								ClusterName tFibEntryClusterName = new ClusterName(tNegotiator.getToken(), tNegotiator.getClusterID(), tNegotiator.getHierarchyLevel());

								tEntry = mAddressMapping.get(tSourceCEP).new FIBEntry(tTo, tNextHop, tFibEntryClusterName, getSignature());
								
								/*
								 * As the cluster probably does not know to which node it has to forward that packet,
								 * the route to the coordinator of the target cluster is chosen.
								 */
								if(tListToTarget != null) {
									tEntry.setRoutingVectors(tListToTarget);
									if(getSignatureOfPath(tHRMID) != null) {
										tEntry.setSignature(getSignatureOfPath(tHRMID));
									}
								}
								IRoutableClusterGraphTargetName tTargetNode = getFarthestVirtualNodeInDirection(tSourceCEP.getRemoteClusterName(), tSourceCEP.getRemoteClusterName());
								tNameFarthestClusterInDirection = null;
								if(tTargetNode instanceof ICluster) {
									ICluster tCluster = (ICluster)tTargetNode;
									tNameFarthestClusterInDirection = new ClusterName(tCluster.getToken(), tCluster.getClusterID(), tCluster.getHierarchyLevel());
								}
								tEntry.setFarthestClusterInDirection(tNameFarthestClusterInDirection);
							} else {
								/*
								 * In that case the cluster has to be told to which cluster is has to forward that packet
								 * 
								 * First check whether there exists a cluster connection from the source cluster to the forwarding cluster
								 */
								if(!tList.isEmpty()) {
									tNextHop = getHRMController().getRoutableClusterGraph().getDest(tSourceCEP.getRemoteClusterName(), tList.get(0)).getHrmID();
									ICluster tNextCluster = (ICluster)tNextHop; 

									ClusterName tFibEntryClusterName = new ClusterName(tNextCluster.getToken(), tNextCluster.getClusterID(), tNextCluster.getHierarchyLevel());

									tEntry = mAddressMapping.get(tSourceCEP).new FIBEntry(tTo, tNextHop, tFibEntryClusterName, getSignature());
									if(tListToTarget != null) {
										LinkedList<RoutingServiceLinkVector> tVectors = (LinkedList<RoutingServiceLinkVector>) tListToTarget.clone();
										while(!tVectors.isEmpty() && !tVectors.getFirst().equals(tSourceCEP.getPeerName())) {
											tVectors.removeFirst();
										}
										Logging.log(this, "Started with initial path " + tListToTarget + " while list is now " + tVectors);
										// was tListToTarget before, change if something is wrong now
										tEntry.setRoutingVectors(tVectors);
										if(getSignatureOfPath(tHRMID) != null) {
											tEntry.setSignature(getSignatureOfPath(tHRMID));
										}
									}
									IRoutableClusterGraphTargetName tTargetNode = getFarthestVirtualNodeInDirection(tSourceCEP.getRemoteClusterName(), tNegotiator);
									tNameFarthestClusterInDirection = null;
									if(tTargetNode instanceof ICluster) {
										ICluster tCluster = (ICluster)tTargetNode;
										tNameFarthestClusterInDirection = new ClusterName(tCluster.getToken(), tCluster.getClusterID(), tCluster.getHierarchyLevel());
									}
									tEntry.setFarthestClusterInDirection(tNameFarthestClusterInDirection);
								} else {
									/*
									 * That should only happen accidentally. the list to the target cluster does not exist,
									 * so the source cluster stays where it is.
									 * 
									 * If however, a route between the coordinators was calculated, we provide the next hop to that cluster
									 */
									LinkedList<RoutingServiceLinkVector> tVectors = null;
									if(tListToTarget != null && !tListToTarget.isEmpty()) {
										tVectors = (LinkedList<RoutingServiceLinkVector>) tListToTarget.clone();
										while(!tVectors.getFirst().equals(tSourceCEP.getPeerName())) {
											tVectors.removeFirst();
										}
									}
									tNameFarthestClusterInDirection = tSourceCEP.getRemoteClusterName();
									tEntry = mAddressMapping.get(tSourceCEP).new FIBEntry(tTo, null, tNameFarthestClusterInDirection, getSignature());
									if(tVectors != null) {
										tEntry.setRoutingVectors(tVectors);
										if(getSignatureOfPath(tHRMID) != null) {
											tEntry.setSignature(getSignatureOfPath(tHRMID));
										}
									}
									tEntry.setFarthestClusterInDirection(tNameFarthestClusterInDirection);
								}
							}
							/*
							 * Now that forwarding entry is saved inside the clients forwarding table
							 */
							mAddressMapping.get(tSourceCEP).addForwardingentry(tEntry);
						}
					} else {
						/*
						 * We are at level one, so now the real forwarding table is calculated
						 */
						Logging.log(this, "Would now distribute addresses for higher clusters");
						for(HRMID tHRMID : mHigherHRMIDs) {
							/*
							 * Find out to which cluster the HRMID is mapped to
							 */
							Logging.log(this, tHRMID + " is mapped to " + (ICluster)getVirtualNodeFromHRMID(tHRMID));
							/*
							 * Get that cluster for route calculation
							 */
							ICluster tTargetCluster = (ICluster)getVirtualNodeFromHRMID(tHRMID);
							/*
							 * As we are on level one, we have to find out, to which node tSourceCEP has to forward the packets for the 
							 * given higher HRMID
							 * 
							 * if one of the clients has the given address, the packet has to be forwarded to that client
							 */
							List<Route> tPath=null;
							if(tTargetCluster!= null) {
								 tPath = tLocalRoutingDB.getRoute(tSourceCEP.getSourceName(), tTargetCluster.getCoordinatorsAddress());
							}
							
							HRMName tDestination = null;
							/*
							 * Find out to which routing service address the packet has to be forwarded to
							 */
							HRMName tAddress = null;
							
							/*
							 * First try: find out if a path was provided
							 */
							
							LinkedList<RoutingServiceLinkVector> tPolygon = null;
							if(getPathFromHRMID(tHRMID) != null && !getPathFromHRMID(tHRMID).isEmpty()) {
								/*
								 * Nice.
								 * Get the path the higher entity provided
								 * 
								 * Cut it until the path from the node we calculate the route for is reached
								 */
								tPolygon = (LinkedList<RoutingServiceLinkVector>) getPathFromHRMID(tHRMID).clone();
								while(!tPolygon.getFirst().equals(tSourceCEP.getPeerName())) {
									tPolygon.removeFirst();
									if(tPolygon.isEmpty()) {
										break;
									}
								}
								if(!tPolygon.isEmpty()) {
									tDestination = tPolygon.getFirst().getDestination();
								}
							}
							
							/*
							 * Second try: calculate the path ourselves
							 */
							
							if(tDestination == null) {
								tAddress = (tPath != null && tPath.size() > 0 ? tLocalRoutingDB.getDest(tPath.get(0)) :  tSourceCEP.getSourceName());
							}
							
							if(tAddress != null) {
								if(tSourceCEP.getPeerName().equals(tAddress) && tPath.size() > 1) {
									tDestination = tLocalRoutingDB.getDest(tPath.get(1));
								} else if(tPath != null && !tPath.isEmpty()) {
									for(CoordinatorCEPChannel tCEP : mManagedCluster.getClusterMembers()) {
										if(tCEP.getPeerName().equals(tAddress)) {
											tDestination = tCEP.getPeerName();
										}
									}
								}
								if(tDestination == null) {
									tDestination = tSourceCEP.getSourceName();
								}
							}
							
							FIBEntry tEntry = mAddressMapping.get(tSourceCEP).new FIBEntry(tHRMID, tDestination, (tTargetCluster != null ? tLocalManagedClusterName : null), getSignature());
							
							mAddressMapping.get(tSourceCEP).addForwardingentry(tEntry);
							if(tPolygon != null && !tPolygon.isEmpty()) {
								tEntry.setRoutingVectors(tPolygon);
								if(getSignatureOfPath(tHRMID) != null) {
									tEntry.setSignature(getSignatureOfPath(tHRMID));
								}
							}
							IRoutableClusterGraphTargetName tTargetNode = getFarthestVirtualNodeInDirection(tSourceCEP.getRemoteClusterName(), tSourceCEP.getRemoteClusterName());
							tNameFarthestClusterInDirection = null;
							if(tTargetNode instanceof ICluster) {
								ICluster tCluster = (ICluster)tTargetNode;
								tNameFarthestClusterInDirection = new ClusterName(tCluster.getToken(), tCluster.getClusterID(), tCluster.getHierarchyLevel());
							}
							tEntry.setFarthestClusterInDirection(tNameFarthestClusterInDirection);
						}
					}
				}
				if(mSignatures!= null && !mSignatures.isEmpty()) {
					for(HRMSignature tSignature : mSignatures) {
						mAddressMapping.get(tSourceCEP).addApprovedSignature(tSignature);
					}
				}
				mAddressMapping.get(tSourceCEP).addApprovedSignature(getSignature());
				tSourceCEP.sendPacket(mAddressMapping.get(tSourceCEP));
			}
			if(mHigherHRMIDs != null) {
				for(HRMID tHRMID : mHigherHRMIDs) {
	
					/*
					 * Now the forwarding entry for the cluster itself is generated:
					 * Tell the cluster how to reach the given destination
					 */
					
					ICluster tDestinationCluster = (ICluster)getVirtualNodeFromHRMID(tHRMID);
					HRMName tRouteSource = (HRMName) getHRMController().getNode().getRoutingService().getNameFor(getHRMController().getNode().getCentralFN()); 
					
					List<Route> tRoute = tLocalRoutingDB.getRoute((HRMName) tRouteSource, tDestinationCluster.getCoordinatorsAddress());
					HRMName tNextHop=null;
					LinkedList<RoutingServiceLinkVector> tPathToTarget = null;
					if(getPathFromHRMID(tHRMID) != null && !getPathFromHRMID(tHRMID).isEmpty() && getPathFromHRMID(tHRMID).contains(tRouteSource)) {
						
						LinkedList<RoutingServiceLinkVector> tPolygon = (LinkedList<RoutingServiceLinkVector>) getPathFromHRMID(tHRMID).clone();
						while(!tPolygon.getFirst().equals(tRouteSource)) {
							tPolygon.removeFirst();
							if(tPolygon.isEmpty()) {
								break;
							}
						}
						if(tPolygon.isEmpty()) {
							Logging.err(this, "Tried to calculate next hop directly, but no entry found");
							if(!tRoute.isEmpty()) {
								tNextHop = tLocalRoutingDB.getDest(tRoute.get(0));
								for(Route tPolygonPath : tRoute) {
									tPolygon.add(new RoutingServiceLinkVector(tPolygonPath, tLocalRoutingDB.getSource(tPolygonPath), tLocalRoutingDB.getDest(tPolygonPath)));
								}
							}
						}
						for(CoordinatorCEPChannel tCEP : mManagedCluster.getClusterMembers()) {
							if(tCEP.getPeerName().equals(tPolygon.getFirst().getDestination())) {
								tNextHop = tPolygon.getFirst().getDestination();
								tPathToTarget = tPolygon;
							}
						}
						if(tNextHop == null && tPolygon != null && !tPolygon.isEmpty()) {
							tNextHop = tPolygon.getFirst().getDestination();
						}
						tPathToTarget = tPolygon;
					} else if(!tRoute.isEmpty()) {
						tNextHop = tLocalRoutingDB.getDest(tRoute.get(0));
					}
					
					FIBEntry tEntry = tManagedClusterTopologyData.new FIBEntry(tHRMID, tNextHop, tLocalManagedClusterName, getSignature());
					if(tPathToTarget != null && !tPathToTarget.isEmpty()) {
						tEntry.setRoutingVectors(tPathToTarget);
						if(getSignatureOfPath(tHRMID) != null) {
							tEntry.setSignature(getSignatureOfPath(tHRMID));
						}
					}
					IRoutableClusterGraphTargetName tTargetNode = getFarthestVirtualNodeInDirection(mManagedCluster, tDestinationCluster);
					tNameFarthestClusterInDirection = null;
					if(tTargetNode instanceof ICluster) {
						ICluster tCluster = (ICluster)tTargetNode;
						tNameFarthestClusterInDirection = new ClusterName(tCluster.getToken(), tCluster.getClusterID(), tCluster.getHierarchyLevel());
					}
					tEntry.setFarthestClusterInDirection(tNameFarthestClusterInDirection);
					tManagedClusterTopologyData.addForwardingentry(tEntry);
				}
			}
			
			// are we at base level?
			if(mHierarchyLevel.isBaseLevel()) {
				
				for(HRMSignature tSignature : mSignatures) {
					tManagedClusterTopologyData.addApprovedSignature(tSignature);
				}
				tManagedClusterTopologyData.addApprovedSignature(getHRMController().getIdentity().createSignature(getHRMController().getNode().toString(), null, new HierarchyLevel(this, mHierarchyLevel.getValue() + 1)));
				mManagedCluster.handleTopologyData(tManagedClusterTopologyData);
			}
		
		} catch (AuthenticationException tExc) {
			Logging.err(this, "Unable to create signatures, maybe this entity is not allowed to?", tExc);
		}
	}
	
	private boolean connectToNeighbors(int radius)
	{
		for(IRoutableClusterGraphTargetName tNode : mClustersToNotify) {
			if(tNode instanceof ICluster && !((ICluster) tNode).isInterASCluster()) {
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
					Logging.log(this, " L" + mHierarchyLevel + "-skipping connection to " + tName + " for cluster " + tNode + " because connection already exists");
					continue;
				} else {
					/*
					 * was it really this cluster? -> reevaluate
					 */
					Logging.log(this, " L" + mHierarchyLevel + "-adding connection to " + tName + " for cluster " + tNode);
					CoordinatorCEPChannel tCEP = getMultiplexer().addConnection(tCluster, mManagedCluster);
					//new CoordinatorCEP(mManagedCluster.getCoordinator().getLogger(), mManagedCluster.getCoordinator(), this, false);
					mConnectedEntities.add(tName);
				}
			}
		}
		return true;
	}
	
	@Override
	public void setCoordinatorPriority(BullyPriority pCoordinatorPriority) {
		if(mCoordinatorCEP != null) {
			if (!mCoordinatorCEP.getPeerPriority().equals(pCoordinatorPriority)){
				Logging.info(this, "Tried to set a priority that does not correspond with the priority of the concurrent coordinator, wrong connection endpoint?");
			}
			mCoordinatorCEP.setPeerPriority(pCoordinatorPriority);
		}
	}

	@Override
	public BullyPriority getCoordinatorPriority() {
		if(mCoordinatorCEP != null) {
			return mCoordinatorCEP.getPeerPriority();
		}
		return null;
	}

	@Override
	public void setPriority(BullyPriority pPriority) 
	{
		if (!getBullyPriority().equals(pPriority)){
			Logging.err(this, "############# Trying to update Bully priority from " + getBullyPriority() + " to " + pPriority);
		}else{
			Logging.log(this, "############# Trying to set same Bully priority " + getBullyPriority());
		}

		//TODO: remove this function
	}

	@Override
	public HRMController getHRMController() {
		return mManagedCluster.getHRMController();
	}

	@Override
	public LinkedList<CoordinatorCEPChannel> getClusterMembers() {
		return mCEPs;
	}

	public LinkedList<HRMID> getHigherHRMIDs()
	{
		return mHigherHRMIDs;
	}
	
	public HashMap<CoordinatorCEPChannel, TopologyData> getAddressMapping()
	{
		return mAddressMapping;
	}
	
	public LinkedList<CoordinatorCEPChannel> getLowerCEPs()
	{
		return mManagedCluster.getClusterMembers();
	}
	
	@Override
	public void addParticipatingCEP(CoordinatorCEPChannel pParticipatingCEP) {
		mCEPs.add(pParticipatingCEP);
	}

	@Override
	public Long getClusterID() {
		return mClusterID;
	}

	@Override
	public HierarchyLevel getHierarchyLevel() {
		return new HierarchyLevel(this, mHierarchyLevel.getValue() + 1);
	}

	@Override
	public Name getCoordinatorName() {
		return mCoordinatorName;
	}

	@Override
	public BullyPriority getBullyPriority() 
	{
		// return the Bully priority of the managed cluster object
		return mManagedCluster.getBullyPriority();
	}

	@Override
	public String getClusterDescription() {
		return getClass().getSimpleName() + "(" + mManagedCluster + ")";
	}

	@Override
	public void setCoordinatorName(Name pCoordName) {
		mCoordinatorName = pCoordName;
	}

	@Override
	public HRMName getCoordinatorsAddress() {
		synchronized(this) {
			while(mCoordinatorAddress == null) {
				try {
					Logging.log(this, "ACTIVE WAITING");
					wait(1000);
				} catch (InterruptedException tExc) {
					Logging.err(this, "Error while waiting for address", tExc);
				}
			}
			return mCoordinatorAddress;
		}
	}

	@Override
	public void setToken(int pToken) {
		if(mToken != 0) {
			Logging.log(this, "Updating token");
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
	public HRMSignature getCoordinatorSignature() {
		return mCoordinatorSignature;
	}
	
	private void registerFIBEntry(FIBEntry pEntry)
	{
		mIDToFIBMapping.put(pEntry.getDestination(), pEntry);
		Cluster tTargetCluster = getHRMController().getCluster(pEntry.getNextCluster());
		map(pEntry.getDestination(), tTargetCluster);
	}
	
	@Override
	public void handleTopologyData(TopologyData pTopologyData)
	{
		/*
		 * this cluster manager only computes the FIB derived from Radius algorithm
		 */
		Node tNode = getHRMController().getNode();
		
//TODO: still needed here?
//		if(pTopologyData.getPushThrougs() != null && !pTopologyData.getPushThrougs().isEmpty()) {
//			for(FIBEntry tEntry : pTopologyData.getPushThrougs()) {
//				if((tEntry.getDestination() != null && !tEntry.getDestination().equals(new HRMID(0)) ) && tEntry.getNextHop() != null && !tEntry.getNextHop().equals(tNode.getRoutingService().getNameFor(tNode.getCentralFN()))) {
//					getHRMController().getHRS().addRoutingEntry(tEntry.getDestination(), tEntry);
//				}
//			}
//		}
		
		if(pTopologyData.getApprovedSignatures() != null) {
			for(HRMSignature tSignature : pTopologyData.getApprovedSignatures()) {
				if(!mSignatures.contains(tSignature)) {
					mSignatures.add(tSignature);
				}
			}
		}
		
		Logging.log(this, "Received topology data: " + pTopologyData);
		mTopologyData = pTopologyData;
		
		// update the node's label within GUI
		tNode.setDecorationValue(tNode.getDecorationValue() + "," + pTopologyData.getHRMID());
		
		getHRMController().addIdentification(pTopologyData.getHRMID());
		if(pTopologyData.getEntries() != null && !pTopologyData.getEntries().isEmpty()) {
			if(mHigherHRMIDs == null) mHigherHRMIDs = new LinkedList<HRMID>();
			for(FIBEntry tEntry : pTopologyData.getEntries()) {
				mHigherHRMIDs.add(tEntry.getDestination());
				registerFIBEntry(tEntry);
			}
			Logging.log(this, "Have to provide FEs for " + mHigherHRMIDs);
		}
		setHRMID(this, pTopologyData.getHRMID());
		mManagedCluster.setHRMID(this, pTopologyData.getHRMID());
		try {
			signalAddresses();
		} catch (RoutingException tExc) {
			Logging.err(this, "Error-got routing exception when trying to distribute addresses", tExc);
		} catch (RequirementsException tExc) {
			Logging.err(this, "Error-got requirements exception when trying to distribute addresses", tExc);
		} catch (RemoteException tExc) {
			Logging.err(this, "Error-got remote exception when trying to distribute addresses", tExc);
		}
	}

	@Override
	public BullyPriority getHighestPriority() {
		return mHighestPriority;
	}
	
	public void handleBullyAnnounce(BullyAnnounce pAnnounce, CoordinatorCEPChannel pCEP)
	{
		/**
		 * Stores the local cluster, which corresponds to the correct hierarchy level
		 */
		ICluster tLocalCluster = getHRMController().getClusterWithCoordinatorOnLevel(getHierarchyLevel().getValue());		
		if (mManagedCluster != tLocalCluster){
			Logging.err(this,  "################## WE SHOULD NEVER REACH THIS HERE: " + mManagedCluster + "::" + tLocalCluster);
		}

		/**
		 * the name of the cluster, which is managed by this coordinator
		 */
		ClusterName tLocalManagedClusterName = new ClusterName(mManagedCluster.getToken(), mManagedCluster.getClusterID(), mManagedCluster.getHierarchyLevel());

		/*
		 * check whether old priority was lower than new priority
		 */
		if(pAnnounce.getSenderPriority().isHigher(this, getCoordinatorPriority())) {
			/*
			 * check whether a coordinator is already set
			 */
			if(getCoordinatorCEP() != null) {
				if(getCoordinatorName() != null && !pAnnounce.getSenderName().equals(getCoordinatorName())) {
					/*
					 * a coordinator was set earlier -> therefore inter-cluster communicatino is necessary
					 * 
					 * find the route from the managed cluster to the cluster this entity got to know the higher cluster
					 */
					List<RoutableClusterGraphLink> tClusterList = getHRMController().getRoutableClusterGraph().getRoute(mManagedCluster, pCEP.getRemoteClusterName());
					if(tClusterList.size() > 0) {
						if(getHRMController().getRoutableClusterGraph().getDest(pCEP.getRemoteClusterName(), tClusterList.get(tClusterList.size() - 1)) instanceof Cluster) {
							Logging.warn(this, "Not sending neighbor zone announce because another intermediate cluster has a shorter route to target");
							if(tClusterList != null) {
								String tClusterRoute = new String();
								IRoutableClusterGraphTargetName tTransitiveElement = mManagedCluster;
								for(RoutableClusterGraphLink tConnection : tClusterList) {
									tClusterRoute += tTransitiveElement + "\n";
									tTransitiveElement = getHRMController().getRoutableClusterGraph().getDest(tTransitiveElement, tConnection);
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
							
							NeighborClusterAnnounce tOldCovered = new NeighborClusterAnnounce(getCoordinatorName(), getHierarchyLevel(), getCoordinatorSignature(), getCoordinatorsAddress(),getToken(), getCoordinatorsAddress().getAddress().longValue());
							tOldCovered.setCoordinatorsPriority(getCoordinatorCEP().getPeerPriority());
							tOldCovered.setNegotiatorIdentification(tLocalManagedClusterName);
							
							DiscoveryEntry tOldCoveredEntry = new DiscoveryEntry(tLocalCluster.getToken(), tLocalCluster.getCoordinatorName(), tLocalCluster.getCoordinatorsAddress().getAddress().longValue(), tLocalCluster.getCoordinatorsAddress(), tLocalCluster.getHierarchyLevel());
							/*
							 * the forwarding cluster to the newly discovered cluster has to be one level lower so it is forwarded on the correct cluster
							 * 
							 * calculation of the predecessor is because the cluster identification on the remote site is multiplexed
							 */
							tClusterList = getHRMController().getRoutableClusterGraph().getRoute(mManagedCluster, getCoordinatorCEP().getRemoteClusterName());
							if(!tClusterList.isEmpty()) {
								ICluster tPredecessor = (ICluster) getHRMController().getRoutableClusterGraph().getDest(mManagedCluster, tClusterList.get(0));
								tOldCoveredEntry.setPredecessor(new ClusterName(tPredecessor.getToken(), tPredecessor.getClusterID(), tPredecessor.getHierarchyLevel()));
							}
							tOldCoveredEntry.setPriority(getCoordinatorPriority());
							tOldCoveredEntry.setRoutingVectors(pCEP.getPath(tLocalCluster.getCoordinatorsAddress()));
							tOldCovered.setCoveringClusterEntry(tOldCoveredEntry);
//							List<Route> tPathToCoordinator = getCoordinator().getHRS().getCoordinatorRoutingMap().getRoute((HRMName)pCEP.getSourceName(), getCoordinatorsAddress());
							//tOldCovered.setAnnouncer(getCoordinator().getHRS().getCoordinatorRoutingMap().getDest(tPathToCoordinator.get(0)));
							pCEP.sendPacket(tOldCovered);
							
							/*
							 * now the old cluster is notified about the new cluster
							 */
							
							NeighborClusterAnnounce tNewCovered = new NeighborClusterAnnounce(pAnnounce.getSenderName(), getHierarchyLevel(), pAnnounce.getCoordSignature(), pCEP.getPeerName(), pAnnounce.getToken(), pCEP.getPeerName().getAddress().longValue());
							tNewCovered.setCoordinatorsPriority(pAnnounce.getSenderPriority());
							tNewCovered.setNegotiatorIdentification(tLocalManagedClusterName);
							DiscoveryEntry tCoveredEntry = new DiscoveryEntry(pAnnounce.getToken(),	pAnnounce.getSenderName(), (pCEP.getPeerName()).getAddress().longValue(), pCEP.getPeerName(),	getHierarchyLevel());
							tCoveredEntry.setRoutingVectors(pCEP.getPath(pCEP.getPeerName()));
							tNewCovered.setCoveringClusterEntry(tCoveredEntry);
							tCoveredEntry.setPriority(pAnnounce.getSenderPriority());
							
							List<RoutableClusterGraphLink> tClusters = getHRMController().getRoutableClusterGraph().getRoute(mManagedCluster, pCEP.getRemoteClusterName());
							if(!tClusters.isEmpty()) {
								ICluster tNewPredecessor = (ICluster) getHRMController().getRoutableClusterGraph().getDest(mManagedCluster, tClusters.get(0));
								tCoveredEntry.setPredecessor(new ClusterName(tNewPredecessor.getToken(), tNewPredecessor.getClusterID(), tNewPredecessor.getHierarchyLevel()));
							}
							Logging.warn(this, "Rejecting " + (getCoordinatorCEP().getPeerName()).getDescr() + " in favor of " + pAnnounce.getSenderName());
							tNewCovered.setRejection();
							getCoordinatorCEP().sendPacket(tNewCovered);
							for(CoordinatorCEPChannel tCEP : getClusterMembers()) {
								if(pAnnounce.getCoveredNodes().contains(tCEP.getPeerName())) {
									tCEP.setAsParticipantOfMyCluster(true);
								} else {
									tCEP.setAsParticipantOfMyCluster(false);
									
								}
							}
							setToken(pAnnounce.getToken());
							setCoordinatorCEP(pCEP, pAnnounce.getCoordSignature(), pAnnounce.getSenderName(),pAnnounce.getToken(),  pCEP.getPeerName());
							getHRMController().setClusterWithCoordinator(getHierarchyLevel(), this);
							getHRMController().addApprovedSignature(pAnnounce.getCoordSignature());
							getCoordinatorCEP().sendPacket(tNewCovered);
						}
					}
				}
				
			} else {
				for(CoordinatorCEPChannel tCEP : getClusterMembers()) {
					if(pAnnounce.getCoveredNodes().contains(tCEP.getPeerName())) {
						tCEP.setAsParticipantOfMyCluster(true);
					} else {
						tCEP.setAsParticipantOfMyCluster(false);
					}
				}
				setToken(pAnnounce.getToken());
				getHRMController().setClusterWithCoordinator(getHierarchyLevel(), this);
				setCoordinatorCEP(pCEP, pAnnounce.getCoordSignature(), pAnnounce.getSenderName(), pAnnounce.getToken(), pCEP.getPeerName());
			}
		} else {
			/*
			 * this part is for the coordinator that intended to announce itself -> send rejection and send acting coordinator along with
			 * the announcement that is just gained a neighbor zone
			 */
			
			NeighborClusterAnnounce tUncoveredAnnounce = new NeighborClusterAnnounce(getCoordinatorName(), getHierarchyLevel(), getCoordinatorSignature(), getCoordinatorsAddress(), getToken(), getCoordinatorsAddress().getAddress().longValue());
			tUncoveredAnnounce.setCoordinatorsPriority(getCoordinatorCEP().getPeerPriority());
			/*
			 * the routing service address of the announcer is set once the neighbor zone announce arrives at the rejected coordinator because this
			 * entity is already covered
			 */
			
			tUncoveredAnnounce.setNegotiatorIdentification(tLocalManagedClusterName);
			
			DiscoveryEntry tUncoveredEntry = new DiscoveryEntry(tLocalCluster.getToken(), tLocalCluster.getCoordinatorName(), tLocalCluster.getCoordinatorsAddress().getAddress().longValue(), tLocalCluster.getCoordinatorsAddress(), tLocalCluster.getHierarchyLevel());
			List<RoutableClusterGraphLink> tClusterList = getHRMController().getRoutableClusterGraph().getRoute(mManagedCluster, pCEP.getRemoteClusterName());
			if(!tClusterList.isEmpty()) {
				ICluster tPredecessor = (ICluster) getHRMController().getRoutableClusterGraph().getDest(mManagedCluster, tClusterList.get(0));
				tUncoveredEntry.setPredecessor(new ClusterName(tPredecessor.getToken(), tPredecessor.getClusterID(), tPredecessor.getHierarchyLevel()));
			}
			tUncoveredEntry.setPriority(getCoordinatorPriority());
			tUncoveredEntry.setRoutingVectors(pCEP.getPath(tLocalCluster.getCoordinatorsAddress()));
			tUncoveredAnnounce.setCoveringClusterEntry(tUncoveredEntry);
			Logging.warn(this, "Rejecting " + (getCoordinatorCEP().getPeerName()).getDescr() + " in favour of " + pAnnounce.getSenderName());
			tUncoveredAnnounce.setRejection();
			pCEP.sendPacket(tUncoveredAnnounce);
			
			/*
			 * this part is for the acting coordinator, so NeighborZoneAnnounce is sent in order to announce the cluster that was just rejected
			 */
			
			NeighborClusterAnnounce tCoveredAnnounce = new NeighborClusterAnnounce(pAnnounce.getSenderName(), getHierarchyLevel(), pAnnounce.getCoordSignature(), pCEP.getPeerName(), pAnnounce.getToken(), (pCEP.getPeerName()).getAddress().longValue());
			tCoveredAnnounce.setCoordinatorsPriority(pAnnounce.getSenderPriority());
			
//			List<Route> tPathToCoordinator = getCoordinator().getHRS().getCoordinatorRoutingMap().getRoute(pCEP.getSourceName(), pCEP.getPeerName());
			
			//tCoveredAnnounce.setAnnouncer(getCoordinator().getHRS().getCoordinatorRoutingMap().getDest(tPathToCoordinator.get(0)));
			tCoveredAnnounce.setNegotiatorIdentification(tLocalManagedClusterName);
			DiscoveryEntry tCoveredEntry = new DiscoveryEntry(pAnnounce.getToken(), pAnnounce.getSenderName(), (pCEP.getPeerName()).getAddress().longValue(), pCEP.getPeerName(), getHierarchyLevel());
			tCoveredEntry.setRoutingVectors(pCEP.getPath(pCEP.getPeerName()));
			tCoveredAnnounce.setCoveringClusterEntry(tCoveredEntry);
			tCoveredEntry.setPriority(pAnnounce.getSenderPriority());
			tCoveredAnnounce.setCoordinatorsPriority(pAnnounce.getSenderPriority());
			
			List<RoutableClusterGraphLink> tClusters = getHRMController().getRoutableClusterGraph().getRoute(mManagedCluster, getCoordinatorCEP().getRemoteClusterName());
			if(!tClusters.isEmpty()) {
				ICluster tNewPredecessor = (ICluster) getHRMController().getRoutableClusterGraph().getDest(mManagedCluster, tClusters.get(0));
				tUncoveredEntry.setPredecessor(new ClusterName(tNewPredecessor.getToken(), tNewPredecessor.getClusterID(), tNewPredecessor.getHierarchyLevel()));
			}
			Logging.log(this, "Coordinator CEP is " + getCoordinatorCEP());
			getCoordinatorCEP().sendPacket(tCoveredAnnounce);
		}
	}
	
	private ICluster addAnnouncedCluster(NeighborClusterAnnounce pAnnounce, CoordinatorCEPChannel pCEP)
	{
		if(pAnnounce.getRoutingVectors() != null) {
			for(RoutingServiceLinkVector tVector : pAnnounce.getRoutingVectors()) {
				getHRMController().getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
			}
		}
		NeighborCluster tCluster = null;
		if(pAnnounce.isAnnouncementFromForeign())
		{
			tCluster = new NeighborCluster(pAnnounce.getCoordAddress().getAddress().longValue(), pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress(), pAnnounce.getToken(), new HierarchyLevel(this, mHierarchyLevel.getValue() + 2),	mManagedCluster.getHRMController());
			getHRMController().setSourceIntermediateCluster(tCluster, getHRMController().getSourceIntermediate(this));
			((NeighborCluster)tCluster).addAnnouncedCEP(pCEP);
			tCluster.setToken(pAnnounce.getToken());
			tCluster.setPriority(pAnnounce.getCoordinatorsPriority());
			//mManagedCluster.addNeighborCluster(tCluster);
		} else {
			Logging.log(this, "Cluster announced by " + pAnnounce + " is an intermediate neighbor ");
		}
		if(pAnnounce.getCoordinatorName() != null) {
			RoutingService tRS = (RoutingService)getHRMController().getNode().getRoutingService();
			if(! tRS.isKnown(pAnnounce.getCoordinatorName())) {
				try {
					getHRMController().getHRS().registerNode(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
				} catch (RemoteException tExc) {
					Logging.err(this, "Unable to register " + pAnnounce.getCoordinatorName() + " at higher entity", tExc);
				}
				
			}
		}
		return tCluster;
	}
	
	@Override
	public void handleNeighborAnnouncement(NeighborClusterAnnounce	pAnnounce, CoordinatorCEPChannel pCEP)
	{		
		if(pAnnounce.getCoveringClusterEntry() != null) {
//			Cluster tForwardingCluster = null;
			
			if(pAnnounce.isRejected()) {
//				Cluster tMultiplex = mManagedCluster;
//				tForwardingCluster = (Cluster) ((Cluster) getCoordinator().getLastUncovered(tMultiplex, pCEP.getRemoteCluster()) == null ? pCEP.getRemoteCluster() : getCoordinator().getLastUncovered(tMultiplex, pCEP.getRemoteCluster())) ;
				//pAnnounce.setAnnouncer( (tForwardingCluster.getCoordinatorsAddress() != null ? tForwardingCluster.getCoordinatorsAddress() : null ));
				Logging.log(this, "Removing " + this + " as participating CEP from " + this);
				getClusterMembers().remove(this);
			}
			if(pAnnounce.getCoordinatorName() != null) {
				RoutingService tRS = (RoutingService)getHRMController().getNode().getRoutingService();
				if(! tRS.isKnown(pAnnounce.getCoordinatorName())) {
					
					try {
						getHRMController().getHRS().registerNode(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
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
			pCEP.addAnnouncedCluster(addAnnouncedCluster(pAnnounce, pCEP), getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
		} else {
			Logging.log(this, "new negotiating cluster will be " + getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
			pCEP.addAnnouncedCluster(addAnnouncedCluster(pAnnounce, pCEP), getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
		}
	}

	@Override
	public CoordinatorCEPChannel getCoordinatorCEP() {
		return mCoordinatorCEP;
	}

	@Override
	public void setCoordinatorCEP(CoordinatorCEPChannel pCoord, HRMSignature pCoordSignature, Name pCoordName, int pCoordToken, HRMName pAddress) {
		/**
		 * the name of the cluster, which is managed by this coordinator
		 */
		ClusterName tLocalManagedClusterName = new ClusterName(mManagedCluster.getToken(), mManagedCluster.getClusterID(), mManagedCluster.getHierarchyLevel());
		setToken(pCoordToken);
		
		Logging.log(this, "announcement number " + (++mReceivedAnnounces) + ": Setting Coordinator " + pCoord + " with name " + pCoordName + " with routing address " + pAddress);
		Logging.log(this, "previous coordinator was " + mCoordinatorCEP + " with name " + mCoordinatorName);
		mCoordinatorCEP = pCoord;
		mCoordinatorSignature = pCoordSignature;
		mCoordinatorName = pCoordName;
		synchronized(this) {
			mCoordinatorAddress = pAddress;
			notifyAll();
		}
		getHRMController().getNode().setDecorationValue("(" + pCoordSignature + ")");
//		LinkedList<CoordinatorCEP> tEntitiesToNotify = new LinkedList<CoordinatorCEP> ();
		if(pCoordSignature != null) {
			for(IRoutableClusterGraphTargetName tNode: getHRMController().getRoutableClusterGraph().getNeighbors(mManagedCluster)) {
				if(tNode instanceof ICluster && !((ICluster) tNode).isInterASCluster()) {
					for(CoordinatorCEPChannel tCEP : mCEPs) {
						if(((ICluster)tNode).getCoordinatorsAddress().equals(tCEP.getPeerName()) && !tCEP.isPartOfMyCluster()) {
							Logging.info(this, "Informing " + tCEP + " about existence of neighbor zone ");
							NeighborClusterAnnounce tAnnounce = new NeighborClusterAnnounce(pCoordName, getHierarchyLevel(), pCoordSignature, pAddress, getToken(), pAddress.getAddress().longValue());
							tAnnounce.setCoordinatorsPriority(getCoordinatorCEP().getPeerPriority());
							LinkedList<RoutingServiceLinkVector> tVectorList = tCEP.getPath(pAddress);
							tAnnounce.setRoutingVectors(tVectorList);
							tAnnounce.setNegotiatorIdentification(tLocalManagedClusterName);
							tCEP.sendPacket(tAnnounce);
						}
						Logging.log(this, "Informed " + tCEP + " about new neighbor zone");
					}
				}
			}
		}
		getHRMController().addApprovedSignature(pCoordSignature);
		if(mReceivedAnnouncements != null) {
			for(NeighborClusterAnnounce tAnnounce : mReceivedAnnouncements) {
				pCoord.sendPacket(tAnnounce);
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
	public void setHRMID(Object pCaller, HRMID pHRMID) {
		Logging.log(this, "Setting HRM ID: \"" + pHRMID + "\", triggered from " + pCaller);
		mHRMID = pHRMID;
	}

	@Override
	public HRMID getHrmID() {
		return mHRMID;
	}
	
	@Override
	public void setHighestPriority(BullyPriority pHighestPriority) {
		mHighestPriority = pHighestPriority;
	}

	public void sendClusterBroadcast(Serializable pData, LinkedList<CoordinatorCEPChannel> pAlreadyInformed)
	{
		if(pData instanceof BullyPriorityUpdate)
		{
			Logging.log(this, "Will send priority update to" + mCEPs);
		}
		LinkedList<CoordinatorCEPChannel> tInformedCEPs = null;
		if(pAlreadyInformed != null) {
			tInformedCEPs= pAlreadyInformed;
		} else {
			tInformedCEPs = new LinkedList<CoordinatorCEPChannel>(); 
		}
		try {
			for(CoordinatorCEPChannel tCEP : mCEPs)
			{
				if(!tInformedCEPs.contains(tCEP))
				{
					tCEP.sendPacket(pData);
					tInformedCEPs.add(tCEP);
				}
			}
		} catch (ConcurrentModificationException tExc) {
			Logging.warn(this, "change in cluster CEP number occured, sending message to new peers", tExc);
			sendClusterBroadcast(pData, tInformedCEPs);
		}
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
	
	@Override
	public boolean isInterASCluster()
	{
		return false;
	}

	@Override
	public void setInterASCluster()
	{
		
	}

	public Cluster getManagedCluster()
	{
		return mManagedCluster;
	}
	
	@Override
	public TopologyData getTopologyData()
	{
		return mTopologyData;
	}
	
	private HRMSignature getSignatureOfPath(HRMID tHRMID)
	{
		if(mIDToFIBMapping.containsKey(tHRMID) && mIDToFIBMapping.get(tHRMID).getSignature() != null) {
			return mIDToFIBMapping.get(tHRMID).getSignature();
		} else {
			return null;
		}
		
	}
	
	public LinkedList<RoutingServiceLinkVector> getPathFromHRMID(HRMID pID)
	{
		if(mIDToFIBMapping.containsKey(pID) && mIDToFIBMapping.get(pID).getRouteToTarget() != null) {
			return mIDToFIBMapping.get(pID).getRouteToTarget();
		} else if(mAddressToPathMapping != null && mAddressToPathMapping.containsKey(pID)) {
				return (LinkedList<RoutingServiceLinkVector>) mAddressToPathMapping.get(pID).clone();
			} else {
				return null;
			}
	}
	
	private void map(HRMID pHRMID, IRoutableClusterGraphTargetName pToVirtualNode)
	{
		Logging.log(this, "Mapping HRMID " + pHRMID + " to " + pToVirtualNode);
		// Check if this is safe
		if(mAddressToClusterMapping.containsKey(pHRMID)) {
			mAddressToClusterMapping.remove(pHRMID);
		}
		if(pToVirtualNode instanceof ICluster) {
			mAddressToClusterMapping.put(pHRMID, getHRMController().getCluster((ICluster)pToVirtualNode));
		} else {
			mAddressToClusterMapping.put(pHRMID, pToVirtualNode);
		}
	}
	
	public HashMap<HRMID, IRoutableClusterGraphTargetName> getMappings()
	{
		return mAddressToClusterMapping;
	}
	
	public IRoutableClusterGraphTargetName getVirtualNodeFromHRMID(HRMID pHRMID)
	{
		IRoutableClusterGraphTargetName tNode = mAddressToClusterMapping.get(pHRMID);
		if(tNode != null) {
			/*
			 * OK: node was found
			 */
		} else {
			tNode = mIDToFIBMapping.get(pHRMID).getNextCluster();
		}
		return tNode;
	}
	
//	public void handleRouteRequest(RouteRequest pRequest, IRoutableClusterGraphTargetName pSourceCluster)
//	{
//		/**
//		 * Stored the routing DB of the local HRM controller
//		 */
//		final RoutableGraph<HRMName, Route> tLocalRoutingDB = getHRMController().getHRS().getCoordinatorRoutingMap();
//
//		final RouteRequest tParameterRouteRequest = pRequest;
//		final IRoutableClusterGraphTargetName tSourceCluster = pSourceCluster;
//		final Coordinator tManager = this;
//		
//		if(pRequest.getResult() != null && pRequest.getResult().equals(ResultType.UNFEASIBLE)) {
//			CoordinatorCEPChannel tCEP = mRouteRequestDispatcher.get(tParameterRouteRequest.getSession());
//			tParameterRouteRequest.setAnswer();
//			tCEP.sendPacket(tParameterRouteRequest);
//			return;
//		}
//		
//		new Thread () {
//			public void run() {
//				int tDescendingDifference = 0;
//				if(tParameterRouteRequest.getTarget() instanceof HRMID) {
//					tDescendingDifference = (((HRMID)tParameterRouteRequest.getTarget())).getDescendingDifference(tSourceCluster.getHrmID());
//				}
//				
//				/*
//				 * Beginning of the recursion
//				 */
//				if(tDescendingDifference > mHierarchyLevel.getValue()) {
//					RouteRequest tRequest = tParameterRouteRequest.clone();
//					getCoordinatorCEP().sendPacket(tRequest);
//					synchronized(tRequest) {
//						try {
//							tRequest.wait();
//						} catch (InterruptedException tExc) {
//							Logging.err(this, "Error when waiting for " + tRequest, tExc);
//						}
//					}
//					Logging.log(tManager, "Come back of " + tRequest);
//					try {
//						if(tRequest.getRoutingVectors() != null) {
//							for(RoutingServiceLinkVector tVector : tRequest.getRoutingVectors()) {
//								tParameterRouteRequest.addRoutingVector(tVector);
//							}
//						}
//					} catch (ConcurrentModificationException tExc) {
//						if(tRequest.getRoutingVectors() != null) {
//							for(RoutingServiceLinkVector tVector : tRequest.getRoutingVectors()) {
//								if(tParameterRouteRequest.getRoutingVectors().contains(tVector)) {
//									tParameterRouteRequest.addRoutingVector(tVector);
//								}
//							}
//						}
//					}
//					
//					tParameterRouteRequest.setAnswer();
//					tParameterRouteRequest.setResult(tRequest.getResult());
//					CoordinatorCEPChannel tCEP = mRouteRequestDispatcher.get(tParameterRouteRequest.getSession());
//					tCEP.sendPacket(tParameterRouteRequest);
//				} else {
//					/*
//					 * end of the recursion
//					 */
//					Logging.log(tManager, "Reached highest cluster");
//					final HRMID tLocalTarget = ((HRMID) (tParameterRouteRequest.getTarget())).clone();
//					for(int i = 0; i < mHierarchyLevel.getValue(); i++) {
//						tLocalTarget.setLevelAddress(new HierarchyLevel(this, i), BigInteger.valueOf(0));
//					}
//					LinkedList<IRoutableClusterGraphTargetName> tNodesToIgnore = new LinkedList<IRoutableClusterGraphTargetName>();
//					
//					List<RoutableClusterGraphLink> tClusterConnection = null;; 
//					try {
//						Logging.log(tManager, "Invalidating nodes " + tNodesToIgnore);
//						tClusterConnection = getHRMController().getRoutableClusterGraph().getRouteWithInvalidatedNodes(tSourceCluster, getVirtualNodeFromHRMID(tLocalTarget), tNodesToIgnore);
//						LinkedList<ICluster> tClusters = new LinkedList<ICluster>();
//						ICluster tLastCluster = (ICluster) tSourceCluster;
//						if(tClusterConnection != null && !tClusterConnection.isEmpty()) {
//							for(RoutableClusterGraphLink tConnection : tClusterConnection) {
//								tClusters.add(tLastCluster);
//								tLastCluster = (ICluster) getHRMController().getRoutableClusterGraph().getDest(tLastCluster, tConnection);
//							}
//						} else {
//							tParameterRouteRequest.setResult(ResultType.UNFEASIBLE);
//							CoordinatorCEPChannel tCEP = mRouteRequestDispatcher.get(tParameterRouteRequest.getSession());
//							tCEP.sendPacket(tParameterRouteRequest);
//							return;
//						}
//						
//						tClusters.add(tLastCluster);
//						
//						/*
//						 * find descending difference from left to right
//						 */
//						Logging.log(tManager, "Got cluster connection " + tClusterConnection);
//						Logging.log(tManager, "Got cluster connection over " + tClusters);
//						
//						Route tRoute = new Route();
//						tLastCluster = (ICluster) tSourceCluster;
//						for(ICluster tCluster : tClusters) {
//							List<Route> tPath = tLocalRoutingDB.getRoute(tLastCluster.getCoordinatorsAddress(), tCluster.getCoordinatorsAddress());
//							if(!tPath.isEmpty() && tPath.size() == 1 && !tLastCluster.getCoordinatorsAddress().equals(tCluster.getCoordinatorsAddress())) {
//								for(Route tRoutePart : tPath) {
//									tRoute.addAll(tRoutePart);
//									RoutingServiceLinkVector tVector = new RoutingServiceLinkVector(tRoutePart, tLocalRoutingDB.getSource(tRoutePart), tLocalRoutingDB.getDest(tRoutePart));
//									tParameterRouteRequest.addRoutingVector(tVector);
//								}
//							} else if(tPath.isEmpty() && !tLastCluster.getCoordinatorsAddress().equals(tCluster.getCoordinatorsAddress()) || !tPath.isEmpty() && tPath.size() > 1 && !tLastCluster.getCoordinatorsAddress().equals(tCluster.getCoordinatorsAddress())) {
//								Logging.err(tManager, "Unable to calculate a route segment path from " + tLastCluster.getCoordinatorsAddress() + " to " + tCluster.getCoordinatorsAddress());
//								for(CoordinatorCEPChannel tCEP : mManagedCluster.getParticipatingCEPs()) {
//									if(tCEP.getRemoteClusterName().equals(tLastCluster)) {
//										Logging.log(tManager, "About to ask route from " + tLastCluster + " to " + tCluster);
//										RouteRequest tRequest = new RouteRequest(tLastCluster.getCoordinatorsAddress(), tCluster.getCoordinatorsAddress(), null, 0);
//										tRequest.addRequiredCluster(new ClusterName(tLastCluster.getToken(), tLastCluster.getClusterID(), tLastCluster.getHierarchyLevel()));
//
//										tCEP.sendPacket(tRequest);
//										synchronized(tRequest) {
//											if(!tRequest.isAnswer()) {
//												try {
//													tRequest.wait();
//												} catch (InterruptedException tExc) {
//													Logging.err(this, "Error when waiting for come back of route request " + tRequest, tExc);
//												}
//											}
//										}
//										if(tRequest.getRoutingVectors() != null) {
//											for(RoutingServiceLinkVector tVector : tRequest.getRoutingVectors()) {
//												tParameterRouteRequest.addRoutingVector(tVector);
//											}
//										}	
//									}
//								}
//							}
//							Logging.log(tManager, "Calculated route from " + tLastCluster.getCoordinatorsAddress() + " to " + tCluster.getCoordinatorsAddress() + ":" + tPath);
//							tLastCluster = tCluster;
//						}
//						Logging.log(tManager, "Concurrent route request is " + tParameterRouteRequest);
//						if(((HRMID)tParameterRouteRequest.getTarget()).getLevelAddress(mHierarchyLevel.getValue()) != BigInteger.valueOf(0)) {
//							CoordinatorCEPChannel tCEP = mManagedCluster.getCEPOfCluster((ICluster) getVirtualNodeFromHRMID(tLocalTarget));
//							RouteRequest tRequest = new RouteRequest(tCEP.getPeerName(), tParameterRouteRequest.getTarget(), tParameterRouteRequest.getDescription(), tParameterRouteRequest.getSession());
//							tCEP.sendPacket(tRequest);
//							synchronized(tRequest) {
//								try {
//									tRequest.wait();
//								} catch (InterruptedException tExc) {
//									Logging.err(tManager, "Error while waiting for", tExc);
//								}
//							}
//							Logging.log(tManager, "Come back of " + tRequest);
//							if(tRequest.getRoutingVectors() != null) {
//								for(RoutingServiceLinkVector tVector : tRequest.getRoutingVectors()) {
//									tParameterRouteRequest.addRoutingVector(tVector);
//								}
//							}
//							tParameterRouteRequest.setResult(tRequest.getResult());
//							Logging.log(tManager, "Route request is now " + tParameterRouteRequest);
//						}
//						long tRequestSession = tParameterRouteRequest.getSession();
//						Logging.log(tManager, "registered requests for " + mRouteRequestDispatcher + ": ");
//						for(Long tLong : mRouteRequestDispatcher.keySet()) {
//							Logging.log(this, tLong + " is pointing on " + mRouteRequestDispatcher.get(tLong));
//						}
//						CoordinatorCEPChannel tCEP = mRouteRequestDispatcher.get(tRequestSession);
//						tParameterRouteRequest.setAnswer();
//						tCEP.sendPacket(tParameterRouteRequest);
//					} catch (NullPointerException tExc) {
//						Logging.err(tManager, "Error when trying to calculate route with invalidated node", tExc);
//					}
//				}
//			}
//		}.start();
//	}

	@Override
	public int getSerialisedSize()
	{
		return 0;
	}

	@Override
	public CoordinatorCEPMultiplexer getMultiplexer() {
		return getHRMController().getMultiplexerOnLevel(mHierarchyLevel.getValue() + 1);
	}

//	public void registerRouteRequest(Long pSession, CoordinatorCEPChannel pCEP)
//	{
//		if( mRouteRequestDispatcher == null ) {
//			mRouteRequestDispatcher = new HashMap<Long, CoordinatorCEPChannel>();
//		}
//		Logging.log(this, "registered " + pSession + " with " + pCEP);
//		mRouteRequestDispatcher.put(pSession, pCEP);
//	}
	
	public String toString()
	{
		//return getClass().getSimpleName() + (mManagedCluster != null ? "(" + mManagedCluster.toString() + ")" : "" ) + "TK(" +mToken + ")COORD(" + mCoordinatorSignature + ")@" + mLevel;
		return toLocation() + " " + (mManagedCluster != null ? "(ManagedCluster=" + mManagedCluster.getGUIClusterID() + ", ": "(" ) + "Tok=" +mToken + ", CoordSign=" + mCoordinatorSignature + ")";
	}

	@Override
	public String toLocation()
	{
		String tResult = getClass().getSimpleName() + mGUICoordinatorID + "@" + getHRMController().getNodeGUIName() + "@" + getHierarchyLevel();
		
		return tResult;
	}
}
