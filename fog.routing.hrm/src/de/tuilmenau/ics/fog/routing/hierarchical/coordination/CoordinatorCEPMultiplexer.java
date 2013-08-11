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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.ClusterDiscovery;
import de.tuilmenau.ics.fog.packets.hierarchical.DiscoveryEntry;
import de.tuilmenau.ics.fog.packets.hierarchical.MultiplexedPackage;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.ClusterDiscovery.NestedDiscovery;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ClusterName;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.HierarchyLevel;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ICluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.NeighborCluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.RoutableClusterGraphLink;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ClusterParticipationProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ClusterParticipationProperty.NestedParticipation;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Tuple;

public class CoordinatorCEPMultiplexer
{
	private HashMap<CoordinatorCEPChannel, CoordinatorSession> mCEPToSessionMapping;
	private HashMap<CoordinatorSession, LinkedList<CoordinatorCEPChannel>> mSessionToCEPsMapping;
	private HashMap<Tuple<Long, Long>, CoordinatorCEPChannel> mClusterToCEPMapping;
	private HRMController mHRMController = null;
	private LinkedList<Name> mConnectedEntities = new LinkedList<Name>();
	private ICluster mCluster;
	
	public CoordinatorCEPMultiplexer(HRMController pHRMController)
	{
		mHRMController = pHRMController;
		mCEPToSessionMapping = new HashMap<CoordinatorCEPChannel, CoordinatorSession>();
		mSessionToCEPsMapping = new HashMap<CoordinatorSession, LinkedList<CoordinatorCEPChannel>>();
		mClusterToCEPMapping = new HashMap<Tuple<Long, Long>, CoordinatorCEPChannel>();
		Logging.log(this, "CREATED for " + pHRMController);
	}
	
	public void addConnection(ICluster pTargetCluster, ICluster pSourceCluster)
	{
		HierarchyLevel tSourceClusterHierLvl = new HierarchyLevel(this, pSourceCluster.getHierarchyLevel().getValue() + 1);
		HierarchyLevel tTargetClusterHierLvl = new HierarchyLevel(this, pTargetCluster.getHierarchyLevel().getValue() + 1);

		Name tName = pTargetCluster.getCoordinatorName();
		CoordinatorCEPChannel tCEPDemultiplexed = null;
//		long tAddress=0;

		if(!mConnectedEntities.contains(pTargetCluster.getCoordinatorName())) {
			mConnectedEntities.add(pTargetCluster.getCoordinatorName());
			ClusterParticipationProperty tParticipationProperty = new ClusterParticipationProperty(pTargetCluster.getCoordinatorsAddress().getComplexAddress().longValue(), tTargetClusterHierLvl, pTargetCluster.getToken());
			CoordinatorSession tCEP = new CoordinatorSession(mHRMController, false, tSourceClusterHierLvl, mHRMController.getMultiplexerOnLevel(tSourceClusterHierLvl.getValue()));
			ClusterDiscovery tBigDiscovery = new ClusterDiscovery(mHRMController.getNodeName());
			
			for(Coordinator tManager : mHRMController.getCoordinator(new HierarchyLevel(this, tSourceClusterHierLvl.getValue() - 1))) {
				tCEPDemultiplexed = new CoordinatorCEPChannel(mHRMController, tManager);
				tCEPDemultiplexed.setPeerPriority(pTargetCluster.getBullyPriority());
				tCEP.getMultiplexer().mapCEPToSession(tCEPDemultiplexed, tCEP);
				tCEP.getMultiplexer().addDemultiplex(tCEP, tCEPDemultiplexed);
				synchronized(mClusterToCEPMapping) {
					Logging.log(this, "Registering multiplex" + tManager.getClusterID() + " to " + pTargetCluster.getClusterID() + " with connection endpoint " + tCEPDemultiplexed);
					mClusterToCEPMapping.put(new Tuple<Long, Long>(tManager.getClusterID(), pTargetCluster.getClusterID()), tCEPDemultiplexed);
				}
				tCEPDemultiplexed.setRemoteClusterName(new ClusterName(pTargetCluster.getToken(), pTargetCluster.getClusterID(), pTargetCluster.getHierarchyLevel()));
			}
			
			for(Coordinator tManager : mHRMController.getCoordinator(new HierarchyLevel(this, tSourceClusterHierLvl.getValue() - 1))) {
				if(pTargetCluster.getCoordinatorsAddress() == null) {
					Logging.err(this, "Error on trying to contact other clusters, as name is set please check its address");
				} else {
					NestedParticipation tParticipate = tParticipationProperty.new NestedParticipation(pTargetCluster.getClusterID(), pTargetCluster.getToken());
					tParticipationProperty.addNestedparticipation(tParticipate);
					tParticipate.setSenderPriority(tManager.getManagedCluster().getBullyPriority());
//					tAddress = pTargetCluster.getCoordinatorsAddress().getAddress().longValue();
					
					tParticipate.setSourceClusterID(tManager.getManagedCluster().getClusterID());
					tParticipate.setSourceToken(tManager.getManagedCluster().getToken());
					tParticipate.setSourceName(mHRMController.getNode().getCentralFN().getName());
					tParticipate.setSourceRoutingServiceAddress(tCEP.getSourceRoutingServiceAddress());
					
					List<RoutableClusterGraphLink> tClusterListToRemote = mHRMController.getRoutableClusterGraph().getRoute(tManager.getManagedCluster(), pTargetCluster);
					if(!tClusterListToRemote.isEmpty()) {
						/*
						 * we need the last hop in direct to the neighbor
						 */
						ICluster tPredecessorToRemote = (ICluster) mHRMController.getRoutableClusterGraph().getLinkEndNode(pTargetCluster, tClusterListToRemote.get(tClusterListToRemote.size()-1));
						tParticipate.setPredecessor(new ClusterName(tPredecessorToRemote.getToken(), tPredecessorToRemote.getClusterID(), tPredecessorToRemote.getHierarchyLevel()));
						Logging.log(this, "Successfully set predecessor for " + pTargetCluster + ":" + tPredecessorToRemote);
					} else {
						Logging.log(this, "Unable to set predecessor for " + pTargetCluster + ":");
					}
					
					for(ICluster tNeighbor: tManager.getManagedCluster().getNeighbors()) {
						DiscoveryEntry tEntry = new DiscoveryEntry(tNeighbor.getToken(), tNeighbor.getCoordinatorName(), tNeighbor.getClusterID(), tNeighbor.getCoordinatorsAddress(), tNeighbor.getHierarchyLevel());
						tEntry.setPriority(tNeighbor.getBullyPriority());
						if(tNeighbor.isInterASCluster()) {
							tEntry.setInterASCluster();
						}
						List<RoutableClusterGraphLink> tClusterList = mHRMController.getRoutableClusterGraph().getRoute(tManager.getManagedCluster(), tNeighbor);
						/*
						 * the predecessor has to be the next hop
						 */
						if(!tClusterList.isEmpty()) {
							ICluster tPredecessor = (ICluster) mHRMController.getRoutableClusterGraph().getLinkEndNode(tNeighbor, tClusterList.get(tClusterList.size()-1));
							tEntry.setPredecessor(new ClusterName(tPredecessor.getToken(), tPredecessor.getClusterID(), tPredecessor.getHierarchyLevel()));
							Logging.log(this, "Successfully set predecessor for " + tNeighbor + ":" + tPredecessor);
						} else {
							Logging.log(this, "Unable to set predecessor for " + tNeighbor);
						}
						if(!tManager.getManagedCluster().isInterASCluster() && ! tNeighbor.isInterASCluster() && tManager.getPathToCoordinator(tManager.getManagedCluster(), tNeighbor) != null) {
							for(RoutingServiceLinkVector tVector : tManager.getPathToCoordinator(tManager.getManagedCluster(), tNeighbor)) {
								tEntry.addRoutingVectors(tVector);
							}
						}
						if(tNeighbor.isInterASCluster()) {
							tEntry.setInterASCluster();
						}
						tParticipate.addDiscoveryEntry(tEntry);
					}
				}
			}
			
			Identity tIdentity = mHRMController.getNode().getIdentity();
			Description tConnectDescription = mHRMController.getConnectDescription(tParticipationProperty);
			Logging.log(this, "Connecting to " + pTargetCluster);
			Connection tConn = null;;
			try {
				Logging.log(this, "CREATING CONNECTION to " + tName);
				tConn = pSourceCluster.getHRMController().getHost().connectBlock(tName, tConnectDescription, tIdentity);
				tCEP.start(tConn);
				tCEP.write(tCEP.getSourceRoutingServiceAddress());
			} catch (NetworkException tExc) {
				Logging.err(this, "Unable to connect to " + tName, tExc);
			}

			for(Coordinator tManager : mHRMController.getCoordinator(new HierarchyLevel(this, tSourceClusterHierLvl.getValue() - 1))) {
				LinkedList<Integer> tTokens = new LinkedList<Integer>();
				for(ICluster tClusterForToken : tManager.getManagedCluster().getNeighbors()) {
					if(tClusterForToken.getHierarchyLevel().getValue() == tManager.getHierarchyLevel().getValue() - 1) {
						tTokens.add((tClusterForToken.getToken()));
					}
				}
				tTokens.add(tManager.getManagedCluster().getToken());
				tManager.getClusterMembers().add(tCEPDemultiplexed);
				if(!pTargetCluster.getCoordinatorName().equals(mHRMController.getNode().getCentralFN().getName())) {
					int tDistance = (pTargetCluster instanceof NeighborCluster ? ((NeighborCluster)pTargetCluster).getClusterDistanceToTarget() : 0); 
					NestedDiscovery tDiscovery = tBigDiscovery.new NestedDiscovery(tTokens, pTargetCluster.getClusterID(), pTargetCluster.getToken(), pTargetCluster.getHierarchyLevel(), tDistance);
					if(pTargetCluster instanceof NeighborCluster && ((NeighborCluster)pTargetCluster).getClusterDistanceToTarget() == 0) {
						Logging.warn(this, "Set 0 as hop count to target " + pTargetCluster);
					}
					Logging.log(this, "Created " + tDiscovery + " for " + pTargetCluster);
					tDiscovery.setOrigin(tManager.getClusterID());
					tDiscovery.setTargetClusterID(pTargetCluster.getCoordinatorsAddress().getComplexAddress().longValue());
					tBigDiscovery.addNestedDiscovery(tDiscovery);
				}
			}
			boolean tAbleToWrite = tCEP.write(tBigDiscovery);
			if(tAbleToWrite) {
				try {
					synchronized(tBigDiscovery) {
						Logging.log(this, "Waiting for come back of " + tBigDiscovery);
						if(!tBigDiscovery.isDealtWith()) {
							Logging.log(this, "ACTIVE WAITING");
							tBigDiscovery.wait(10000);
						}
						Logging.log(this, "come back of: " + tBigDiscovery);
					}
					for(NestedDiscovery tDiscovery : tBigDiscovery.getDiscoveries()) {
						String tClusters = new String();
						for(Cluster tCluster : mHRMController.getRoutingTargetClusters()) {
							tClusters += tCluster + ", ";
						}
						String tDiscoveries = new String();
						for(DiscoveryEntry tEntry : tDiscovery.getDiscoveryEntries()) {
							tDiscoveries += ", " + tEntry;
						}
						if(tDiscovery.getNeighborRelations() != null) {
							for(Tuple<ClusterName, ClusterName> tTuple : tDiscovery.getNeighborRelations()) {
								if(!mHRMController.getRoutableClusterGraph().isLinked(tTuple.getFirst(), tTuple.getSecond())) {
									Cluster tFirstCluster = mHRMController.getCluster(tTuple.getFirst());
									Cluster tSecondCluster = mHRMController.getCluster(tTuple.getSecond());
									if(tFirstCluster != null && tSecondCluster != null ) {
										tFirstCluster.addNeighborCluster(tSecondCluster);
										Logging.log(this, "Connecting " + tFirstCluster + " with " + tSecondCluster);
									} else {
										Logging.warn(this, "Unable to find cluster " + tTuple.getFirst() + ":" + tFirstCluster + " or " + tTuple.getSecond() + ":" + tSecondCluster + " out of \"" + tClusters + "\", cluster discovery contained " + tDiscoveries + " and CEP is " + tCEP);
									}
								}
							}
						} else {
							Logging.warn(this, tDiscovery + "does not contain any neighbor relations");
						}
					}
				} catch (InterruptedException tExc ) {
					Logging.err(this, "Error when waiting for come back of " + tBigDiscovery, tExc);
				}
			} else {
				if(!tAbleToWrite) {
					Logging.err(this, "Unable to discover " + tName + " because " + (tAbleToWrite ? " CEP is not connected " : ""));
				}
				if(tBigDiscovery.getDiscoveries().isEmpty()) {
					Logging.err(this, "Unable to discover " + tName + " because " + (tBigDiscovery.getDiscoveries().isEmpty() ? " no discovery entries were inserted." : ""));
				}
			}
			synchronized(mClusterToCEPMapping) {
				mClusterToCEPMapping.notifyAll();
			}
			
		}
		
		if(tCEPDemultiplexed == null) {
			tCEPDemultiplexed = mClusterToCEPMapping.get(new Tuple<Long, Long>(pSourceCluster.getClusterID(), pTargetCluster.getClusterID()));
			if(tCEPDemultiplexed == null) {
				synchronized(mClusterToCEPMapping) {
					try {
						Logging.log(this, "ACTIVE WAITING - " + pSourceCluster + " is waiting because establishment of connection to " + pTargetCluster + " did not yet take place");
						mClusterToCEPMapping.wait(10000);
						tCEPDemultiplexed = mClusterToCEPMapping.get(new Tuple<Long, Long>(pSourceCluster.getClusterID(), pTargetCluster.getClusterID()));
						//mClusterToCEPMapping.remove(pSourceCluster.getClusterID());
					} catch (IllegalMonitorStateException tExc) {
						Logging.err(this, "Error when establishing connection", tExc);
					} catch (InterruptedException tExc) {
						Logging.err(this, "Error when establishing connection", tExc);

					}
				}
			}
		}
		
		Logging.log(this, "Returning " + tCEPDemultiplexed + " for " + pSourceCluster + " and target " + pTargetCluster);

	}
	
	public boolean write(Serializable pData, CoordinatorCEPChannel pDemux, ClusterName pTargetCluster)
	{	
		Logging.log(this, "Sending " + pData + " from " + pDemux.getPeer() + " to target cluster " + pTargetCluster);

		ClusterName tSource = new ClusterName(pDemux.getPeer().getToken(), pDemux.getPeer().getClusterID(), pDemux.getPeer().getHierarchyLevel());
	
		MultiplexedPackage tMuxPackage = new MultiplexedPackage(tSource, pTargetCluster, pData);
		CoordinatorSession tCEP = mCEPToSessionMapping.get(pDemux);
		Logging.log(this, "Sending " + tMuxPackage);
		
		// send packet
		return tCEP.write(tMuxPackage);
	}
	
	public HRMName getSourceRoutingServiceAddress(CoordinatorCEPChannel pCEP)
	{
		if(mCEPToSessionMapping.containsKey(pCEP)) {
			return mCEPToSessionMapping.get(pCEP).getSourceRoutingServiceAddress();
		}
		return null;
	}
	
	public HRMName getPeerRoutingServiceAddress(CoordinatorCEPChannel pCEP)
	{
		if(mCEPToSessionMapping.containsKey(pCEP)) {
			return mCEPToSessionMapping.get(pCEP).getPeerRoutingServiceAddress();
		}
		return null;
	}
	
	public Route getRouteToPeer(CoordinatorCEPChannel pCEP)
	{
		if(mCEPToSessionMapping.containsKey(pCEP)) {
			return mCEPToSessionMapping.get(pCEP).getRouteToPeer();
		}
		return null;
	}
	
	public synchronized void mapCEPToSession(CoordinatorCEPChannel pCEP, CoordinatorSession pSession)
	{
		Logging.log(this, "Registering multiplexed connection from " + pCEP + " to " + pSession);
		
		// store the mapping
		mCEPToSessionMapping.put(pCEP, pSession);
		
		for(CoordinatorCEPChannel tCEP : mCEPToSessionMapping.keySet()) {
			Logging.log(this, tCEP + "->" + mCEPToSessionMapping.get(tCEP));
		}
		addDemultiplex(pSession, pCEP);
	}
	
	private void addDemultiplex(CoordinatorSession pCEP, CoordinatorCEPChannel pDemux)
	{
		Logging.log(this, "Registering demultiplexing from " + pCEP + " to " + pDemux);
		if(mSessionToCEPsMapping.get(pCEP) == null) {
			mSessionToCEPsMapping.put(pCEP, new LinkedList<CoordinatorCEPChannel>());
		}
		mSessionToCEPsMapping.get(pCEP).add(pDemux);
	}
	
	public LinkedList<CoordinatorCEPChannel> getDemuxCEPs(CoordinatorSession pCEP)
	{
		return mSessionToCEPsMapping.get(pCEP);
	}
	
	public CoordinatorCEPChannel findCEPChannel(CoordinatorSession pCEP, ClusterName pSource, ClusterName pCluster) throws NetworkException
	{
		if(mSessionToCEPsMapping.containsKey(pCEP)) {
			for(CoordinatorCEPChannel tCEP : mSessionToCEPsMapping.get(pCEP)) {
				if(tCEP.getPeer().getClusterID().equals(pCluster.getClusterID())) {
					Tuple<Long, Long> tTuple = new Tuple<Long, Long>(pSource.getClusterID(), pCluster.getClusterID());
					boolean tSourceIsContained = isClusterMultiplexed(tTuple);
					Logging.log(this, "Comparing \"" + tCEP + "\" and \"" + (tSourceIsContained ? getDemultiplex(tTuple) : "") + "\" " + tCEP.getRemoteClusterName() + ", " + (tSourceIsContained ? getDemultiplex(tTuple).getRemoteClusterName() : "" ));
					if(tSourceIsContained && getDemultiplex(tTuple) == tCEP) {
						Logging.log(this, "Returning " + tCEP + " for request on cluster " + pCluster);
						return tCEP;
					} else {
						Logging.log(this, "Source is \"" + pSource + "\", target is \"" + pCluster+ "\", DEMUXER of source is \"" + getDemultiplex(tTuple) + "\", currently evaluated CEP is \"" + tCEP + "\"");
					}
					if(!isClusterMultiplexed(tTuple) && tCEP.getPeer().getClusterID().equals(pCluster.getClusterID())) {
						Logging.log(this, "Returning " + tCEP + " for request on cluster " + pCluster);
						return tCEP;
					}
				}
			}
		}
		
		Logging.log(this, "Unable to find demultiplexed coonection endpoint for " + pCEP + " and target cluster " + pCluster.getClusterID());
		for(CoordinatorSession tCEP : mSessionToCEPsMapping.keySet()) {
			Logging.log(this, tCEP + " to " + mSessionToCEPsMapping.get(tCEP));
		}

		throw new NetworkException("No demultiplexed CEP found for " + pCEP + " and target cluster " + pCluster);
	}
	
	public String toString()
	{
		return "CoordinatorCEPMultiplexer" + "@" + mHRMController.getNode() + (mCluster != null ? "@" + mCluster.getHierarchyLevel().getValue() + " (Cluster=" + mCluster + ")" : "");
	}
	
	public void setCluster(ICluster pCluster)
	{
		mCluster = pCluster;
	}
	
	public void registerDemultiplex(Long pSourceClusterID, Long pTargetClusterID, CoordinatorCEPChannel pCEP)
	{
		Logging.log(this, "Registering demultiplex for Cluster ID" + pSourceClusterID + " to " + pTargetClusterID + " via " + pCEP);
		mClusterToCEPMapping.put(new Tuple<Long, Long>(pSourceClusterID, pTargetClusterID), pCEP);
	}
	
	private CoordinatorCEPChannel getDemultiplex(Tuple<Long, Long> pPair)
	{
		return mClusterToCEPMapping.get(pPair);
	}
	
	private boolean isClusterMultiplexed(Tuple<Long, Long> pPair)
	{
		if(pPair == null) return false;
		for(Tuple<Long, Long> tTuple : mClusterToCEPMapping.keySet()) {
			if(tTuple.equals(pPair)) return true;
		}
		return false;
	}
}
