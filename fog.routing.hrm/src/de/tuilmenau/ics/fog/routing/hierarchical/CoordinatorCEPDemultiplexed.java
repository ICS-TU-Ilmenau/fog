/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.packets.hierarchical.BullyAlive;
import de.tuilmenau.ics.fog.packets.hierarchical.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.BullyElect;
import de.tuilmenau.ics.fog.packets.hierarchical.BullyReply;
import de.tuilmenau.ics.fog.packets.hierarchical.ClusterDiscovery.NestedDiscovery;
import de.tuilmenau.ics.fog.packets.hierarchical.DiscoveryEntry;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborZoneAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.PriorityUpdate;
import de.tuilmenau.ics.fog.packets.hierarchical.RequestCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.RequestZoneMembership;
import de.tuilmenau.ics.fog.packets.hierarchical.RouteRequest;
import de.tuilmenau.ics.fog.packets.hierarchical.RouteRequest.ResultType;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyEnvelope;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyEnvelope.FIBEntry;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentPath;
import de.tuilmenau.ics.fog.routing.RoutingServiceMultiplexer;
import de.tuilmenau.ics.fog.routing.hierarchical.ElectionProcess.ElectionManager;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.AttachedCluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.ClusterDummy;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.ClusterManager;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.IntermediateCluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.NodeConnection;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.VirtualNode;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMIPMapper;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;
import edu.uci.ics.jung.algorithms.shortestpath.BFSDistanceLabeler;

public class CoordinatorCEPDemultiplexed implements VirtualNode
{
	private static final long serialVersionUID = -8290946480171751216L;
	private Cluster mRemoteCluster;
	private Cluster mCluster;
	private float mPeerPriority;
	private boolean mReceivedBorderNodeAnnouncement = false;
	private boolean mPeerIsCoordinatorForNeighborZone = false;
	private boolean mIsEdgeRouter = false;
	private boolean mKnowsCoordinator = false;
	private HashMap<Cluster, Cluster> mAnnouncerMapping;
	private boolean mRequestedCoordinator = false;
	private boolean mPartOfCluster = false;
	private Coordinator mReferenceCoordinator = null;
	private Logger mLogger = Logging.getInstance();
	private BFSDistanceLabeler<VirtualNode, NodeConnection> mBreadthFirstSearch;
	private boolean mCrossLevelCEP = false;
	
	/**
	 * 
	 * @param pLogger Logger that should be used
	 * @param pCoord is the coordinator of a node
	 * @param pCluster is the cluster this connection end point serves
	 */
	public CoordinatorCEPDemultiplexed(Logger pLogger, Coordinator pCoord, Cluster pCluster)
	{
		mReferenceCoordinator = pCoord;
		mCluster = pCluster;
		mLogger = pLogger;
		mLogger.log(this, "Created");
		getCoordinator().getLogger().log(this, "Created for " + mCluster);
	}
	
	/**
	 * 
	 * @param pData is the data that should be sent to the receiver side of this connection end point
	 * @return true if the packet left the central multiplexer and the forwarding node that is attached to a direct down gate
	 * @throws NetworkException
	 */
	public boolean receive(Serializable pData) throws NetworkException
	{
		if(pData == null) {
			throw new NetworkException("Received " + null + " data");
		}
		try {
			if(pData instanceof RequestZoneMembership) {
				if(getCluster().getCoordinatorCEP() != null) {
					Name tMyName = getCoordinator().getPhysicalNode().getRoutingService().getNameFor(getCoordinator().getPhysicalNode().getCentralFN());
					if(getCluster().getCoordinatorName().equals(tMyName)) {
						
					}
				}
			} else if(pData instanceof BullyElect)	{
				if(getCluster().getCoordinatorCEP() != null && ((BullyElect)pData).getSenderPriority() < getCluster().getHighestPriority()) {
					mPeerPriority = ((BullyElect)pData).getSenderPriority();
					if(getCluster().getCoordinator().equals(getCoordinator().getPhysicalNode().getCentralFN().getName())) {
						BullyAnnounce tAnnounce = new BullyAnnounce(getCoordinator().getPhysicalNode().getCentralFN().getName(), getCluster().getPriority(), getCoordinator().getIdentity().createSignature(getCoordinator().getPhysicalNode().toString(), null, getCluster().getLevel()), getCluster().getToken());
						mLogger.log(this, " Sending bullyannounce because I have a coordinator: " + tAnnounce);
						for(CoordinatorCEPDemultiplexed tCEP : getCluster().getParticipatingCEPs()) {
							tAnnounce.addCoveredNode(tCEP.getPeerName());
						}
						if(tAnnounce.getCoveredNodes() == null || (tAnnounce.getCoveredNodes() != null && tAnnounce.getCoveredNodes().isEmpty())) {
							getCoordinator().getLogger().log(this, "Sending announce that does not cover anyhting");
						}
						write(tAnnounce);
					} else {
						write(new BullyAlive(getCoordinator().getPhysicalNode().getCentralFN().getName(), getCluster().getCoordinatorName()));
					}
				} else {
					mPeerPriority = ((BullyElect)pData).getSenderPriority();
					BullyReply tAnswer = new BullyReply(getCluster().getPriority(), getCoordinator().getPhysicalNode().getCentralFN().getName());
					write(tAnswer);
				}
			} else if(pData instanceof BullyReply) {
				mPeerPriority = ((BullyReply)pData).getBullyPriority();
			} else if(pData instanceof BullyAnnounce)  {
				/*
				 * Only an intermediate cluster on level 0 is able to store an announcement an forward it once a coordinator is set
				 */
				getCluster().interpretAnnouncement((BullyAnnounce)pData, this);
			} else if(pData instanceof NeighborZoneAnnounce) {
				NeighborZoneAnnounce tAnnounce = (NeighborZoneAnnounce)pData;

				getCoordinator().getLogger().log(this, "\n\n\nReceived " + tAnnounce + "\n\n\n");
				
				if(tAnnounce.isInterASAnnouncement()) {
					Logging.log(getCoordinator().getPhysicalNode().getAS().getName() + " received an announcement from " + tAnnounce.getASIdentification());
					if(getCoordinator().getPhysicalNode().getAS().getName().equals(tAnnounce.getASIdentification())) {
						if(!getSourceName().equals(getPeerName())) {
							for(Route tPath : getCoordinator().getHRS().getCoordinatorRoutingMap().getRoute((HRMName)getSourceName(), (HRMName)getPeerName())) {
								tAnnounce.addRoutingVector(new RoutingServiceLinkVector(tPath, getCoordinator().getHRS().getCoordinatorRoutingMap().getSource(tPath), getCoordinator().getHRS().getCoordinatorRoutingMap().getDest(tPath)));
							}
						}
						for(CoordinatorCEPDemultiplexed tCEP : getCluster().getParticipatingCEPs()) {
							boolean tWroteAnnouncement = false;
							if(tCEP.isEdgeCEP()) {
								tCEP.write(tAnnounce);
								tWroteAnnouncement = true;
							}
							Logging.log(this, "Testing " + tCEP + " whether it is an inter as link:" + tWroteAnnouncement);
						}
					} else {
						if(getCluster() instanceof IntermediateCluster) {
							if(!getSourceName().equals(getPeerName())) {
								RoutingServiceLinkVector tVector = new RoutingServiceLinkVector(this.getRouteToPeer(), (HRMName)getSourceName(), (HRMName)getPeerName());
								tAnnounce.addRoutingVector(tVector);
							}
							for(CoordinatorCEPDemultiplexed tCEP : getCluster().getParticipatingCEPs()) {
								boolean tWroteAnnouncement = false;
								if(tCEP.getRemoteCluster().getLevel() -1 == tAnnounce.getLevel()) {
									tCEP.write(tAnnounce);
									tWroteAnnouncement = true;
								}
								Logging.log(this, "Testing " + tCEP + " whether it leads to the clusters coordinator: " + tWroteAnnouncement);
							}
						} else if(getCluster() instanceof ClusterManager) {
							Logging.log(this, "Inter AS announcement " + tAnnounce + " is handled by " + getCluster() + " whether it leads to the clusters coordinator");
							((ClusterManager)getCluster()).getManagedCluster().handleAnnouncement(tAnnounce, this);
						}
					}
				} else {
					getCluster().handleAnnouncement(tAnnounce, this);
				}
				Logging.log(this, "Received " + tAnnounce + " from remote cluster " + mRemoteCluster);
			} else if(pData instanceof PriorityUpdate) {
				mPeerPriority = ((PriorityUpdate)pData).getPriority();
			} else if(pData instanceof TopologyEnvelope) {
				getCluster().handleTopologyEnvelope((TopologyEnvelope)pData);
			}/* else if (pData instanceof NestedDiscovery) {
				NestedDiscovery tDiscovery = (NestedDiscovery) pData;
				this.handleClusterDiscovery(tDiscovery);
			}*/
			else if(pData instanceof RouteRequest) {
				RouteRequest tRequest = (RouteRequest) pData;
				if(tRequest.getTarget() instanceof HRMID) {
					HRMName tRequestAddress = tRequest.getSource();
					HRMName tDestinationAddress = getSourceName();
					if(!tRequest.isAnswer() && getCoordinator().getHRS().getFIBEntry( (HRMID) tRequest.getTarget()) != null && tRequestAddress != null && tRequestAddress.equals(tDestinationAddress)) {
						/*
						 * Find out if route request can be solved by this entity without querying a higher coordinator
						 */
						for(VirtualNode tCluster : getCoordinator().getClusters(0)) {
							FIBEntry tEntry = getCoordinator().getHRS().getFIBEntry( (HRMID) tRequest.getTarget());
							if(tCluster instanceof IntermediateCluster && tEntry != null && (tEntry.getFarthestClusterInDirection() == null || tEntry.getFarthestClusterInDirection().equals(tCluster))) {
								Route tRoute = getCoordinator().getHRS().getRoutePath( getSourceName(), tRequest.getTarget(), new Description(), getCoordinator().getPhysicalNode().getIdentity());
								RouteSegmentPath tPath = (RouteSegmentPath) tRoute.getFirst();
								HRMName tSource = null;
								HRMName tTarget = null;
								for(Route tCandidatePath : getCoordinator().getHRS().getCoordinatorRoutingMap().getEdges()) {
									if(tCandidatePath.equals(tPath)) {
										 tSource = getCoordinator().getHRS().getCoordinatorRoutingMap().getSource(tCandidatePath);
										 tTarget = getCoordinator().getHRS().getCoordinatorRoutingMap().getDest(tCandidatePath);
										 break;
									}
								}
								tRequest.addRoutingVector(new RoutingServiceLinkVector(tRoute, tSource, tTarget));
								tRequest.setAnswer();
								tRequest.setResult(ResultType.SUCCESS);
								write(tRequest);
								return true;
							}
						}
					}
					
					if(!((RouteRequest)pData).isAnswer() && ((RouteRequest)pData).isRouteAccumulation()) {
						if(getRemoteCluster().getLevel() != getCluster().getLevel() && getCluster().isInterASCluster()) {
							HRMID tAddress =  (HRMID) tRequest.getTarget();
							LinkedList<Name> tIPAddresses = HRMIPMapper.getHRMIPMapper().getIPFromHRMID(tAddress);
							Route tRoute = null;
							if(tIPAddresses != null) {
								for(Name tTargetAddress : tIPAddresses) {
									try {
										tRoute = ((RoutingServiceMultiplexer)getCoordinator().getPhysicalNode().getRoutingService()).getRoute(getCoordinator().getPhysicalNode().getCentralFN(), tTargetAddress, ((RouteRequest)pData).getDescription(), null);
									} catch (NetworkException tExc) {
										Logging.info(this, "BGP routing service did not find a route to " + tTargetAddress);
									}
									Logging.log(this, "Interop: Route to "+ tAddress + " with IP address " + tTargetAddress + " is " + tRoute);
								}
							} else {
								getCoordinator().getLogger().err(this, "Unable to distribute addresses because no IP address is available");
							}
							if(tRoute != null) {
								((RouteRequest)pData).setAnswer();
								((RouteRequest)pData).setRoute(tRoute);
								write(pData);
							}
						} 
						return true;
					} else if(tRequest.isAnswer()) {
						/*
						 * In this case normally someone is waiting for this packet to arrive, therefore is not handled by any cluster and you only get a notification.
						 */
						synchronized(tRequest) {
							tRequest.notifyAll();
						}
						return true;
					}
					if(getCluster() instanceof IntermediateCluster) {
						ClusterManager tManager = ((IntermediateCluster)getCluster()).getClusterManager();
						tManager.handleRouteRequest((RouteRequest) pData, getRemoteCluster());
						tManager.registerRouteRequest(tRequest.getSession(), this);
					} else if (getCluster() instanceof ClusterManager) {
						/*
						 * Normally that case should not appear ...
						 */
						((ClusterManager)getCluster()).handleRouteRequest((RouteRequest) pData, this);
					}
					/*
					 * This comment relates to the following else if statement: use routing service address as last instance because it is the default and all
					 * other addresses are derived from the HRMID
					 */
				} else if(tRequest.getTarget() instanceof HRMID && !tRequest.isAnswer()) {
					List<Route> tFinalPath = getCoordinator().getHRS().getCoordinatorRoutingMap().getRoute(tRequest.getSource(), tRequest.getTarget());
					if(tRequest.getRequiredClusters() != null) {

						for(Cluster tDummy : tRequest.getRequiredClusters()) {
							tFinalPath = null;
							List<Route> tPath = getCoordinator().getHRS().getCoordinatorRoutingMap().getRoute(tRequest.getSource(), tRequest.getTarget());
							
							Cluster tCluster = getCoordinator().getCluster(tDummy);
							LinkedList<HRMName> tAddressesOfCluster = new LinkedList<HRMName>();
							
							if( tCluster instanceof IntermediateCluster ) {
								for(CoordinatorCEPDemultiplexed tCEP : ((IntermediateCluster)tCluster).getParticipatingCEPs()) {
									tAddressesOfCluster.add(tCEP.getPeerName());
								}
							}
							if( tAddressesOfCluster.contains(getCoordinator().getHRS().getCoordinatorRoutingMap().getDest(tPath.get(0))) ) {
								tFinalPath = tPath;
							} else {
								for(HRMName tCandidate : tAddressesOfCluster) {
									List<Route> tOldPath = tPath;
									tPath = getCoordinator().getHRS().getCoordinatorRoutingMap().getRoute(tCandidate, tRequest.getTarget());
									
									if(tPath.size() < tOldPath.size()) {
										List<Route> tFirstPart = getCoordinator().getHRS().getCoordinatorRoutingMap().getRoute(tRequest.getSource(), tCandidate); 
										Route tSegment = (tFirstPart.size() > 0 ? tFirstPart.get(0) : null);
										if(tSegment != null) {
											tPath.add(0, tSegment);
										}
										tFinalPath = tPath;
									}
								}
							}
						}
					}
					if(tFinalPath != null && !tFinalPath.isEmpty()) {
						for(Route tSegment : tFinalPath) {
							tRequest.addRoutingVector(new RoutingServiceLinkVector(tSegment, getCoordinator().getHRS().getCoordinatorRoutingMap().getSource(tSegment), getCoordinator().getHRS().getCoordinatorRoutingMap().getDest(tSegment)));
						}
					}
					tRequest.setAnswer();
					write(tRequest);
				} else if(tRequest.getTarget() instanceof HRMID && tRequest.isAnswer()) {
					synchronized (tRequest) {
						tRequest.notifyAll();
					}
				}
			} else if (pData instanceof RequestCoordinator) {
				getCoordinator().getLogger().log(this, "Received " + pData);
				RequestCoordinator tRequest = (RequestCoordinator) pData;
				if(!tRequest.isAnswer()) {
					if(getCluster().getCoordinatorCEP() != null) {
						Logging.log(this, "Name of coordinator is " + getCluster().getCoordinator().getClusterWithCoordinatorOnLevel(getCluster().getLevel()).getCoordinatorName());
						
						int tToken = getCluster().getCoordinator().getClusterWithCoordinatorOnLevel(getCluster().getLevel()).getToken();
						Name tCoordinatorName = getCluster().getCoordinator().getClusterWithCoordinatorOnLevel(getCluster().getLevel()).getCoordinatorName();
						long tCoordinatorAddress = getCluster().getCoordinator().getClusterWithCoordinatorOnLevel(getCluster().getLevel()).getCoordinatorsAddress().getAddress().longValue();
						HRMName tL2Address = getCluster().getCoordinator().getClusterWithCoordinatorOnLevel(getCluster().getLevel()).getCoordinatorsAddress();
						int tLevel = getCluster().getCoordinator().getClusterWithCoordinatorOnLevel(getCluster().getLevel()).getLevel();
						
						DiscoveryEntry tEntry = new DiscoveryEntry(tToken, tCoordinatorName, tCoordinatorAddress, tL2Address, tLevel);
						tEntry.setPriority(getCluster().getCoordinatorPriority());
						tEntry.setRoutingVectors(getPath(getCluster().getCoordinator().getClusterWithCoordinatorOnLevel(getCluster().getLevel()).getCoordinatorsAddress()));
						tRequest.addDiscoveryEntry(tEntry);
						tRequest.setCoordinatorKnown(true);
						tRequest.setAnswer();
					} else {
						tRequest.setCoordinatorKnown(false);
						tRequest.setAnswer();
					}
					write(tRequest);
				} else {
					if(tRequest.isCoordinatorKnown()) {
						mKnowsCoordinator = true;
					} else {
						mKnowsCoordinator = false;
					}
					if(tRequest.getDiscoveryEntries() != null) {
						for(DiscoveryEntry tEntry : tRequest.getDiscoveryEntries()) {
							ClusterDummy tDummy = handleDiscoveryEntry(tEntry);
							getCluster().getCoordinator().getCluster(ClusterDummy.compare((((HRMName)this.getSourceName()).getAddress().longValue()), getCluster().getToken(), getCluster().getLevel())).addNeighborCluster(getCluster().getCoordinator().getCluster(tDummy));
							this.addAnnouncedCluster(getCoordinator().getCluster(tDummy), getRemoteCluster());
						}
					}
					synchronized(tRequest) {
						Logging.log(this, "Received answer to " + tRequest + ", notifying");
						tRequest.mWasNotified = true;
						tRequest.notifyAll();
					}
				}
			}
		} catch (PropertyException tExc) {
			Logging.err(this, "Unable to fulfill requirements", tExc);
		}
		return true;
	}
	
	/**
	 * @deprecated
	 * If this CEP is used for communication between different hierarchical levels: for use of HRM+BGP
	 */
	public void setCrossLayerCEP()
	{
		mCrossLevelCEP = true;
	}
	
	/**
	 * 
	 * @param pTarget is the target to which routing service link vectors should be generated
	 * @return vectors that provide a patch between forwarding nodes along with the gate numbers
	 */
	public LinkedList<RoutingServiceLinkVector> getPath(HRMName pTarget)
	{
		LinkedList<RoutingServiceLinkVector> tVectors = new LinkedList<RoutingServiceLinkVector>();
		List<Route> tRoute = getCoordinator().getHRS().getCoordinatorRoutingMap().getRoute(getMultiplexer().getSourceRoutingServiceAddress(this), pTarget);
		HRMName tSource = getMultiplexer().getSourceRoutingServiceAddress(this);
		HRMName tDestination;
		if(tRoute == null) {
			return null;
		} else {
			for(int i = 0 ; i < tRoute.size() ; i++) {
				if(tRoute.get(i) instanceof Route) {
					tDestination = getCoordinator().getHRS().getCoordinatorRoutingMap().getDest(tRoute.get(i));
					RoutingServiceLinkVector tVector = new RoutingServiceLinkVector(tRoute.get(i), tSource, tDestination);
					tVectors.add(tVector);
					tSource = tDestination;
				}
			}
		}
		return tVectors;
	}

	/**
	 * The nested discovery entry you provide in the first argument is the message that has to be filled with
	 * information on how to get to the cluster(s coordinator) provided as second argument
	 * 
	 * @param pDiscovery entry of the entity that has to be informed about the cluster provided as second argument
	 * @param pCluster as cluster (along with the coordinator) to which a path has to be filled into the discovery entry
	 * @throws NetworkException 
	 * @throws PropertyException in case the requirements to the target coordinator can not be fulfilled
	 */
	public void getPathTo(NestedDiscovery pDiscovery, Cluster pCluster) throws NetworkException, PropertyException
	{
		if(pCluster.getCoordinatorName() != null) {
			DiscoveryEntry tEntry = new DiscoveryEntry(pCluster.getToken(), pCluster.getCoordinatorName(), pCluster.getClusterID(), pCluster.getCoordinatorsAddress(), pCluster.getLevel());
			tEntry.setClusterHops(getCluster().getCoordinator().getClusterDistance(pCluster));
			tEntry.setPriority(pCluster.getPriority());
			tEntry.setRoutingVectors(getPath(pCluster.getCoordinatorsAddress()));
			if(pCluster.isInterASCluster()) {
				tEntry.setInterASCluster();
			}
			
			List<NodeConnection> tClusterList = getCoordinator().getClusterMap().getRoute(getCluster(), pCluster);
			if(!tClusterList.isEmpty()) {
				Cluster tPredecessor = (Cluster) getCoordinator().getClusterMap().getDest(pCluster, tClusterList.get(tClusterList.size()-1));
				tEntry.setPredecessor(ClusterDummy.compare(tPredecessor.getClusterID(), tPredecessor.getToken(), tPredecessor.getLevel()));
			}
			
			pDiscovery.addDiscoveryEntry(tEntry);
		}
	}
	
	/**
	 * Connection end points control the clusters they are associated to.
	 * 
	 * @return As one node may be associated to more than one cluster you can use this method to find out
	 * which cluster is controlled by this connection end point.
	 */
	public Cluster getCluster()
	{
		return mCluster;
	}
	
	/**
	 * 
	 * @return
	 */
	public Cluster getRemoteCluster()
	{
		Cluster tCluster = null;
		if(mRemoteCluster instanceof ClusterDummy) {
			tCluster = getCoordinator().getCluster(mRemoteCluster);
		}
		if(getCluster().getLevel() == 0) {
			return getCluster();
		}
		return (tCluster == null ? mRemoteCluster : tCluster);
	}

	public Coordinator getCoordinator()
	{
		return mReferenceCoordinator;
	}
	
	public void setAsParticipantOfMyCluster(boolean pPartOfMyCluster)
	{
		mPartOfCluster = pPartOfMyCluster;
	}
	
	public boolean receivedBorderNodeAnnouncement()
	{
		return mReceivedBorderNodeAnnouncement;
	}
	
	public Cluster getNegotiator(Cluster pCluster)
	{
		if(mAnnouncerMapping == null) {
			mAnnouncerMapping = new HashMap<Cluster, Cluster>();
		}
		Cluster tCluster = mAnnouncerMapping.get(pCluster);
		return tCluster;
	}
	
	public boolean knowsCoordinator()
	{
		return mKnowsCoordinator;
	}
	
	public void setPeerPriority(float pPeerPriority)
	{
		mPeerPriority = pPeerPriority;
	}
	
	public void addAnnouncedCluster(Cluster pAnnounced, Cluster pNegotiate)
	{
		mLogger.log(this, "Cluster " + pAnnounced + " as announced by " + pNegotiate);
		if(pNegotiate == null) {
			return;
		}
		if(this.mAnnouncerMapping == null) {
			mAnnouncerMapping = new HashMap<Cluster, Cluster>();
		}
		if(!mAnnouncerMapping.containsKey(pAnnounced)) {
			mAnnouncerMapping.put(pAnnounced, pNegotiate);
		} else {
			getCoordinator().getLogger().log(this, "comparing " + pNegotiate + " to " + mAnnouncerMapping.get(pAnnounced));
			if(pNegotiate.getLevel() < mAnnouncerMapping.get(pAnnounced).getLevel()) {
				mAnnouncerMapping.remove(pAnnounced);
				mAnnouncerMapping.put(pAnnounced, pNegotiate);
			} else if (pNegotiate instanceof AttachedCluster && mAnnouncerMapping.get(pAnnounced) instanceof AttachedCluster && ((AttachedCluster)pNegotiate).getClustersToTarget() < ((AttachedCluster)mAnnouncerMapping.get(pAnnounced)).getClustersToTarget()) {
				getCluster().getCoordinator().getLogger().log(this, "replacing negotiating cluster of " + pAnnounced + ": " + mAnnouncerMapping.get(pAnnounced) + " with " + pNegotiate);
				mAnnouncerMapping.remove(pAnnounced);
				mAnnouncerMapping.put(pAnnounced, pNegotiate);
			}
		}
	}
	
	public float getPeerPriority()
	{
		return mPeerPriority;
	}
	
	public void setRemoteCluster(Cluster pNegotiator)
	{
		if(pNegotiator == null) {
			mLogger.warn(this, "Setting " + pNegotiator + " as negotiating cluster");
		}
		mRemoteCluster = pNegotiator;
	}

	public CoordinatorCEPMultiplexer getCEPMultiplexer()
	{
		return getMultiplexer();
	}
	
	public boolean write(Serializable pData)
	{
		if(pData instanceof RequestCoordinator) {
			mRequestedCoordinator = true;
			getCoordinator().getLogger().log(this, "Sending " + pData);
		}
		if(getCluster() instanceof ClusterManager && !mCrossLevelCEP) {
			getCEPMultiplexer().write(pData, this, ClusterDummy.compare(((L2Address)getPeerName()).getAddress().longValue(), getCluster().getToken(), getCluster().getLevel()));
		} else {
			getCEPMultiplexer().write(pData, this, getRemoteCluster());
		}
		return true;
	}
	
	public HRMName getPeerName()
	{
		return getMultiplexer().getPeerRoutingServiceAddress(this);
	}
	
	public HRMName getSourceName()
	{
		return getMultiplexer().getSourceRoutingServiceAddress(this);
	}
	
	public boolean isPeerCoordinatorForNeighborZone()
	{
		return mPeerIsCoordinatorForNeighborZone;
	}
	
	public boolean isPartOfMyCluster()
	{
		return mPartOfCluster;
	}
	
	public boolean isEdgeCEP()
	{
		return mIsEdgeRouter;
	}
	
	public void setEdgeCEP()
	{
		ElectionManager.getElectionManager().removeElection(getCluster().getLevel(), getCluster().getClusterID());
		mIsEdgeRouter = true;
	}
	
	public void handleClusterDiscovery(NestedDiscovery pDiscovery, boolean pRequest) throws PropertyException, NetworkException
	{
		if(pRequest){
			Cluster tSourceCluster=null;
			tSourceCluster = getCoordinator().getCluster(ClusterDummy.compare(pDiscovery.getSourceClusterID(), pDiscovery.getToken(), pDiscovery.getLevel()));
			if(tSourceCluster == null) {
				Logging.err(this, "Unable to find appropriate cluster for" + pDiscovery.getSourceClusterID() + " and token" + pDiscovery.getToken() + " on level " + pDiscovery.getLevel() + " remote cluster is " + getRemoteCluster());
			}
			if(mBreadthFirstSearch == null ) {
				this.mBreadthFirstSearch = new BFSDistanceLabeler<VirtualNode, NodeConnection>();
			}
			mBreadthFirstSearch.labelDistances(getCoordinator().getClusterMap().getGraphForGUI(), tSourceCluster);
			List<VirtualNode> tDiscoveryCandidates = mBreadthFirstSearch.getVerticesInOrderVisited();
			if(tSourceCluster != null) {
				for(VirtualNode tVirtualNode : tDiscoveryCandidates) {
					if(tVirtualNode instanceof Cluster) {
						Cluster tCluster = (Cluster) tVirtualNode;
						int tRadius;

						tRadius = HRMConfig.Routing.PAN_CLUSTER_ELECTION_NUMBER;
						Logging.log(this, "radius is " + tRadius);
						if(tCluster instanceof AttachedCluster && ((AttachedCluster)tCluster).getClustersToTarget() + pDiscovery.getDistance() > tRadius) continue;
						boolean tBreak=false;
						for(CoordinatorCEPDemultiplexed tCEP : tCluster.getParticipatingCEPs()) {
							if(tCEP.isEdgeCEP()) tBreak = true;
						}
						if(tBreak) {
							continue;
						}
						int tToken = tCluster.getToken();
						if(!pDiscovery.getTokens().contains(Integer.valueOf(tToken))) {
							if(tCluster instanceof AttachedCluster) {
								getCoordinator().getLogger().log(this, "Reporting " + tCluster + " to " + ((HRMName)getPeerName()).getDescr() + " because " + pDiscovery.getDistance() + " + " + ((AttachedCluster)tCluster).getClustersToTarget() + "=" + (pDiscovery.getDistance() + ((AttachedCluster)tCluster).getClustersToTarget()));
								Logging.log(this, "token list was " + pDiscovery.getTokens());
							}
							getPathTo(pDiscovery, tCluster);
							for(Cluster tNeighbor : tCluster.getNeighbors()) {
								pDiscovery.addNeighborRelation(ClusterDummy.compare(tCluster.getClusterID(), tCluster.getToken(), tCluster.getLevel()), ClusterDummy.compare(tNeighbor.getClusterID(), tNeighbor.getToken(), tNeighbor.getLevel()));
							}
						} else {
							/*
							 * Maybe print out additional stuff if required
							 */
						}
					}
				}
			}
		} else {
			if(pDiscovery.getDiscoveryEntries() != null) {
				HashMap<ClusterDummy, ClusterDummy> tToSetNegotiator = new HashMap<ClusterDummy, ClusterDummy>();
				for(DiscoveryEntry tEntry : pDiscovery.getDiscoveryEntries()) {
					tToSetNegotiator.put(handleDiscoveryEntry(tEntry), tEntry.getPredecessor());
				}
				for(ClusterDummy tDummy : tToSetNegotiator.keySet()) {
					this.addAnnouncedCluster(getCoordinator().getCluster(tDummy), getCoordinator().getCluster(tToSetNegotiator.get(tDummy)));
				}
			}
		}
	}
	
	public ClusterDummy handleDiscoveryEntry(DiscoveryEntry pEntry) throws PropertyException
	{
		getCoordinator().getLogger().trace(this, "Handling " + pEntry);
		Cluster tNewCluster = getCoordinator().getCluster(ClusterDummy.compare(pEntry.getClusterID(), pEntry.getToken(), pEntry.getLevel()));
		if(tNewCluster == null) {
			for(Cluster tCluster : getCoordinator().getClusters()) {
				if(tCluster.equals(ClusterDummy.compare(pEntry.getClusterID(), pEntry.getToken(), getCluster().getLevel() - 1))) {
					tNewCluster = tCluster;
					if(tNewCluster instanceof AttachedCluster && tNewCluster.getCoordinatorsAddress() == null && tNewCluster.getCoordinatorName() == null) {
						getCoordinator().getLogger().log(this, "Filling required information into " + tNewCluster);
						tNewCluster.setCoordinatorCEP(null, null, pEntry.getCoordinatorName(), pEntry.getCoordinatorRoutingAddress());
						if(pEntry.isInterASCluster()) tNewCluster.setInterASCluster();
					}
				}
			}
			if(tNewCluster == null) {
				/*
				 * Be aware of the fact that the new attached cluster has lower level
				 */
				tNewCluster = new AttachedCluster(
						pEntry.getClusterID(),
						pEntry.getCoordinatorName(),
						pEntry.getCoordinatorRoutingAddress(),
						pEntry.getToken(),
						pEntry.getLevel(),
						getCoordinator());
				getCluster().getCoordinator().setSourceIntermediateCluster(tNewCluster, getCluster().getCoordinator().getSourceIntermediate(getCluster()));
				((AttachedCluster)tNewCluster).addAnnouncedCEP(this);
				tNewCluster.setToken(pEntry.getToken());
				tNewCluster.setPriority(pEntry.getPriority());
				getCoordinator().addCluster(tNewCluster);
				if(pEntry.isInterASCluster()) {
					tNewCluster.setInterASCluster();
				}
				try {
					getCoordinator().getHRS().registerNode(tNewCluster.getCoordinatorName(), tNewCluster.getCoordinatorsAddress());
				} catch (RemoteException tExc) {
					getCoordinator().getLogger().err(this, "Unable to register " + tNewCluster.getCoordinatorName(), tExc);
				}
				getCoordinator().getLogger().log(this, "Created " + tNewCluster);
			}
			
			((AttachedCluster)tNewCluster).addAnnouncedCEP(this);
			((AttachedCluster)tNewCluster).setClusterHopsOnOpposite(pEntry.getClusterHops(), this);
		}
		if(pEntry.getRoutingVectors() != null) {
			for(RoutingServiceLinkVector tLink : pEntry.getRoutingVectors()) {
				getCoordinator().getHRS().registerRoute(tLink.getSource(), tLink.getDestination(), tLink.getPath());
			}
		}
		return ClusterDummy.compare(tNewCluster.getClusterID(), tNewCluster.getToken(), tNewCluster.getLevel());
	}
	
	public String toString()
	{
		return this.getClass().getSimpleName()/* + "(" + mIdentification + ")"*/ + "@" + getCluster().getClusterDescription() +  (getPeerName() != null ? "->" + ((HRMName)getPeerName()).getDescr() + ":PR(" + mPeerPriority + ")" : "") + (mIsEdgeRouter ? "|INTER" : "|INTRA");
	}
	
	public boolean hasRequestedCoordinator()
	{
		return mRequestedCoordinator;
	}
	
	public Route getRouteToPeer()
	{
		return getMultiplexer().getRouteToPeer(this);
	}
	
	@Override
	public int getSerialisedSize()
	{
		return 0;
	}
	
	public HRMID retrieveAddress()
	{
		return null;
	}
	
	public Name retrieveName()
	{
		return null;
	}

	@Override
	public Namespace getNamespace()
	{
		return null;
	}
	
	protected CoordinatorCEPMultiplexer getMultiplexer()
	{
		return getCluster().getMultiplexer();
	}
}
