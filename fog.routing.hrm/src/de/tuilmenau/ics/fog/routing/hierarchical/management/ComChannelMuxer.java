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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.facade.Connection;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.ClusterDiscovery;
import de.tuilmenau.ics.fog.packets.hierarchical.DiscoveryEntry;
import de.tuilmenau.ics.fog.packets.hierarchical.MultiplexedPackage;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.ClusterDiscovery.NestedDiscovery;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ClusterParticipationProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ClusterParticipationProperty.NestedParticipation;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Tuple;

public class ComChannelMuxer
{
	private HashMap<ComChannel, ComSession> mChannelToSessionMappings;
	private HashMap<ComSession, LinkedList<ComChannel>> mSessionToChannelsMapping;
	private HashMap<Tuple<Long, Long>, ComChannel> mClusterToCEPMapping;
	private HRMController mHRMController = null;
	private LinkedList<Name> mConnectedEntities = new LinkedList<Name>();
	private ICluster mParent = null;
	
	public ComChannelMuxer(ICluster pParent, HRMController pHRMController)
	{
		mParent = pParent;
		mHRMController = pHRMController;
		mChannelToSessionMappings = new HashMap<ComChannel, ComSession>();
		mSessionToChannelsMapping = new HashMap<ComSession, LinkedList<ComChannel>>();
		mClusterToCEPMapping = new HashMap<Tuple<Long, Long>, ComChannel>();
		Logging.log(this, "CREATED for " + pHRMController);
	}
	
	public void connectToNeighborCoordinator(ICluster pTargetCluster, Coordinator pSourceCoordinator)
	{
		Logging.log(this, "############## Connecting to neighbor coordinator: " + pTargetCluster + ", source=" + pSourceCoordinator);
		
		HierarchyLevel tSourceClusterHierLvl = new HierarchyLevel(this, pSourceCoordinator.getHierarchyLevel().getValue());
		HierarchyLevel tTargetClusterHierLvl = new HierarchyLevel(this, pTargetCluster.getHierarchyLevel().getValue() + 1);

		Name tName = pTargetCluster.getCoordinatorName();
		ComChannel tCEPDemultiplexed = null;
//		long tAddress=0;

		Logging.log(this, "Ping0");
		
		ControlEntity tTargetControlEntity = (ControlEntity)pTargetCluster;
		
		if(!mConnectedEntities.contains(pTargetCluster.getCoordinatorName())) {
			mConnectedEntities.add(pTargetCluster.getCoordinatorName());
			ClusterParticipationProperty tParticipationProperty = new ClusterParticipationProperty(tTargetControlEntity.superiorCoordinatorL2Address().getComplexAddress().longValue(), tTargetClusterHierLvl, pTargetCluster.getToken());
			ComSession tSession = new ComSession(mHRMController, false, tSourceClusterHierLvl, mHRMController.getCoordinatorMultiplexerOnLevel(pSourceCoordinator));
			ClusterDiscovery tBigDiscovery = new ClusterDiscovery(mHRMController.getNodeName());
			
			for(Coordinator tCoordinator : mHRMController.getCoordinator(new HierarchyLevel(this, tSourceClusterHierLvl.getValue() - 1))) {
				Logging.log(this, "Ping1: " + tCoordinator );

				tCEPDemultiplexed = new ComChannel(mHRMController, tCoordinator);
				tCEPDemultiplexed.setPeerPriority(pTargetCluster.getPriority());
				tSession.getMultiplexer().mapChannelToSession(tCEPDemultiplexed, tSession);
				synchronized(mClusterToCEPMapping) {
					Logging.log(this, "Registering multiplex" + tCoordinator.getClusterID() + " to " + pTargetCluster.getClusterID() + " with connection endpoint " + tCEPDemultiplexed);
					mClusterToCEPMapping.put(new Tuple<Long, Long>(tCoordinator.getClusterID(), pTargetCluster.getClusterID()), tCEPDemultiplexed);
				}
				tCEPDemultiplexed.setRemoteClusterName(new ClusterName(pTargetCluster.getToken(), pTargetCluster.getClusterID(), pTargetCluster.getHierarchyLevel()));
			}
			
			for(Coordinator tManager : mHRMController.getCoordinator(new HierarchyLevel(this, tSourceClusterHierLvl.getValue() - 1))) {
				Logging.log(this, "Ping2: " + tManager);

				if(tTargetControlEntity.superiorCoordinatorL2Address() == null) {
					//TODO: fall unmoeglich, wuerde sonst oben crashen
					Logging.err(this, "Error on trying to contact other clusters, as name is set please check its address");
				} else {
					NestedParticipation tParticipate = tParticipationProperty.new NestedParticipation(pTargetCluster.getClusterID(), pTargetCluster.getToken());
					tParticipationProperty.addNestedparticipation(tParticipate);
					tParticipate.setSenderPriority(tManager.getCluster().getPriority());
//					tAddress = pTargetCluster.getCoordinatorsAddress().getAddress().longValue();
					
					tParticipate.setSourceClusterID(tManager.getCluster().getClusterID());
					tParticipate.setSourceToken(tManager.getCluster().getToken());
					tParticipate.setSourceName(mHRMController.getNode().getCentralFN().getName());
					tParticipate.setSourceRoutingServiceAddress(tSession.getSourceRoutingServiceAddress());
					
					List<AbstractRoutingGraphLink> tClusterListToRemote = mHRMController.getRouteARG(tManager.getCluster(), pTargetCluster);
					if(!tClusterListToRemote.isEmpty()) {
						/*
						 * we need the last hop in direct to the neighbor
						 */
						ICluster tPredecessorToRemote = (ICluster) mHRMController.getOtherEndOfLinkARG(pTargetCluster, tClusterListToRemote.get(tClusterListToRemote.size()-1));
						tParticipate.setPredecessor(new ClusterName(tPredecessorToRemote.getToken(), tPredecessorToRemote.getClusterID(), tPredecessorToRemote.getHierarchyLevel()));
						Logging.log(this, "Successfully set predecessor for " + pTargetCluster + ":" + tPredecessorToRemote);
					} else {
						Logging.log(this, "Unable to set predecessor for " + pTargetCluster + ":");
					}
					
					for(ICluster tNeighbor: tManager.getCluster().getNeighbors()) {
						ControlEntity tNeighborControlEntity = (ControlEntity)tNeighbor;

						
						DiscoveryEntry tEntry = new DiscoveryEntry(tNeighbor.getToken(), tNeighbor.getCoordinatorName(), tNeighbor.getClusterID(), tNeighborControlEntity.superiorCoordinatorL2Address(), tNeighbor.getHierarchyLevel());
						tEntry.setPriority(tNeighbor.getPriority());
						List<AbstractRoutingGraphLink> tClusterList = mHRMController.getRouteARG(tManager.getCluster(), tNeighbor);
						/*
						 * the predecessor has to be the next hop
						 */
						if(!tClusterList.isEmpty()) {
							ICluster tPredecessor = (ICluster) mHRMController.getOtherEndOfLinkARG(tNeighbor, tClusterList.get(tClusterList.size()-1));
							tEntry.setPredecessor(new ClusterName(tPredecessor.getToken(), tPredecessor.getClusterID(), tPredecessor.getHierarchyLevel()));
							Logging.log(this, "Successfully set predecessor for " + tNeighbor + ":" + tPredecessor);
						} else {
							Logging.log(this, "Unable to set predecessor for " + tNeighbor);
						}
						if(tManager.getPathToCoordinator(tManager.getCluster(), tNeighbor) != null) {
							for(RoutingServiceLinkVector tVector : tManager.getPathToCoordinator(tManager.getCluster(), tNeighbor)) {
								tEntry.addRoutingVectors(tVector);
							}
						}
						tParticipate.addDiscoveryEntry(tEntry);
					}
				}
			}
			
			Identity tIdentity = mHRMController.getNode().getIdentity();
			Description tConnectDescription = mHRMController.createHRMControllerDestinationDescription();
			tConnectDescription.set(tParticipationProperty);
			
			Logging.log(this, "Connecting to " + pTargetCluster);
			Connection tConn = null;;
			try {
				Logging.log(this, "CREATING CONNECTION to " + tName);
				tConn = pSourceCoordinator.getHRMController().getHost().connectBlock(tName, tConnectDescription, tIdentity);
				tSession.start(tConn);
				tSession.write(tSession.getSourceRoutingServiceAddress());
			} catch (NetworkException tExc) {
				Logging.err(this, "Unable to connect to " + tName, tExc);
			}

			for(Coordinator tManager : mHRMController.getCoordinator(new HierarchyLevel(this, tSourceClusterHierLvl.getValue() - 1))) {
				LinkedList<Integer> tTokens = new LinkedList<Integer>();
				for(ICluster tClusterForToken : tManager.getCluster().getNeighbors()) {
					if(tClusterForToken.getHierarchyLevel().getValue() == tManager.getHierarchyLevel().getValue() - 1) {
						tTokens.add((tClusterForToken.getToken()));
					}
				}
				tTokens.add(tManager.getCluster().getToken());
				tManager.getComChannels().add(tCEPDemultiplexed);
				if(!pTargetCluster.getCoordinatorName().equals(mHRMController.getNode().getCentralFN().getName())) {
					int tDistance = 0;
					if (pTargetCluster instanceof ClusterProxy){
						ClusterProxy tClusterProxy = (ClusterProxy) pTargetCluster;
					
						tDistance = mHRMController.getClusterDistance(tClusterProxy); 
					}
					
					NestedDiscovery tDiscovery = tBigDiscovery.new NestedDiscovery(tTokens, pTargetCluster.getClusterID(), pTargetCluster.getToken(), pTargetCluster.getHierarchyLevel(), tDistance);
					Logging.log(this, "Created " + tDiscovery + " for " + pTargetCluster);
					tDiscovery.setOrigin(tManager.getClusterID());
					tDiscovery.setTargetClusterID(tTargetControlEntity.superiorCoordinatorL2Address().getComplexAddress().longValue());
					tBigDiscovery.addNestedDiscovery(tDiscovery);
				}
			}
			boolean tAbleToWrite = tSession.write(tBigDiscovery);
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
						for(Cluster tCluster : mHRMController.getAllClusters()) {
							tClusters += tCluster + ", ";
						}
						String tDiscoveries = new String();
						for(DiscoveryEntry tEntry : tDiscovery.getDiscoveryEntries()) {
							tDiscoveries += ", " + tEntry;
						}
						if(tDiscovery.getNeighborRelations() != null) {
							for(Tuple<ClusterName, ClusterName> tTuple : tDiscovery.getNeighborRelations()) {
								if(!mHRMController.isLinkedARG(tTuple.getFirst(), tTuple.getSecond())) {
									Cluster tFirstCluster = mHRMController.getCluster(tTuple.getFirst());
									Cluster tSecondCluster = mHRMController.getCluster(tTuple.getSecond());
									if(tFirstCluster != null && tSecondCluster != null ) {
										tFirstCluster.registerNeighbor(tSecondCluster);
										Logging.log(this, "Connecting " + tFirstCluster + " with " + tSecondCluster);
									} else {
										Logging.warn(this, "Unable to find cluster " + tTuple.getFirst() + ":" + tFirstCluster + " or " + tTuple.getSecond() + ":" + tSecondCluster + " out of \"" + tClusters + "\", cluster discovery contained " + tDiscoveries + " and CEP is " + tSession);
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
			tCEPDemultiplexed = mClusterToCEPMapping.get(new Tuple<Long, Long>(pSourceCoordinator.getClusterID(), pTargetCluster.getClusterID()));
			if(tCEPDemultiplexed == null) {
				synchronized(mClusterToCEPMapping) {
					try {
						Logging.log(this, "ACTIVE WAITING - " + pSourceCoordinator + " is waiting because establishment of connection to " + pTargetCluster + " did not yet take place");
						mClusterToCEPMapping.wait(10000);
						tCEPDemultiplexed = mClusterToCEPMapping.get(new Tuple<Long, Long>(pSourceCoordinator.getClusterID(), pTargetCluster.getClusterID()));
						//mClusterToCEPMapping.remove(pSourceCluster.getClusterID());
					} catch (IllegalMonitorStateException tExc) {
						Logging.err(this, "Error when establishing connection", tExc);
					} catch (InterruptedException tExc) {
						Logging.err(this, "Error when establishing connection", tExc);

					}
				}
			}
		}
		
		Logging.log(this, "Returning " + tCEPDemultiplexed + " for " + pSourceCoordinator + " and target " + pTargetCluster);

	}
	
	public boolean write(Serializable pData, ComChannel pDemux, ClusterName pTargetCluster)
	{	
		Logging.log(this, "Sending " + pData + " from " + pDemux.getParent() + " to target cluster " + pTargetCluster);

		ClusterName tSource = new ClusterName(((ICluster)pDemux.getParent()).getToken(), ((ICluster)pDemux.getParent()).getClusterID(), pDemux.getParent().getHierarchyLevel());
	
		MultiplexedPackage tMuxPackage = new MultiplexedPackage(tSource, pTargetCluster, pData);
		ComSession tCEP = mChannelToSessionMappings.get(pDemux);
		Logging.log(this, "Sending " + tMuxPackage);
		
		// send packet
		return tCEP.write(tMuxPackage);
	}
	
	public HRMName getSourceRoutingServiceAddress(ComChannel pCEP)
	{
		if(mChannelToSessionMappings.containsKey(pCEP)) {
			return mChannelToSessionMappings.get(pCEP).getSourceRoutingServiceAddress();
		}
		return null;
	}
	
	public L2Address getPeerL2Address(ComChannel pCEP)
	{
		if(mChannelToSessionMappings.containsKey(pCEP)) {
			return mChannelToSessionMappings.get(pCEP).getPeerL2Address();
		}
		return null;
	}
	
	public Route getRouteToPeer(ComChannel pCEP)
	{
		if(mChannelToSessionMappings.containsKey(pCEP)) {
			return mChannelToSessionMappings.get(pCEP).getRouteToPeer();
		}
		return null;
	}
	
	public synchronized void mapChannelToSession(ComChannel pCEP, ComSession pSession)
	{
		Logging.log(this, "Registering ComChannel-to-ComSession mapping: " + pCEP + " to " + pSession + ", already know the following mappings");
		
		for(ComChannel tCEP : mChannelToSessionMappings.keySet()) {
			Logging.log(this, "      .." + tCEP + " to " + mChannelToSessionMappings.get(tCEP));
		}
		
		// store the mapping
		mChannelToSessionMappings.put(pCEP, pSession);

		mapSessionToChannel(pSession, pCEP);
	}
	
	private void mapSessionToChannel(ComSession pCEP, ComChannel pDemux)
	{
		Logging.log(this, "Registering ComSession-to-ComChannel mapping: " + pCEP + " to " + pDemux + ", already know the following mappins");
		
		if(mSessionToChannelsMapping.get(pCEP) == null) {
			mSessionToChannelsMapping.put(pCEP, new LinkedList<ComChannel>());
		}
		
		mSessionToChannelsMapping.get(pCEP).add(pDemux);
	}
	
	public LinkedList<ComChannel> getComChannels(ComSession pCEP)
	{
		return mSessionToChannelsMapping.get(pCEP);
	}
	
	public ComChannel getComChannel(ComSession pComSession, ClusterName pSource, ClusterName pCluster) throws NetworkException
	{
		ComChannel tResult = null;
		
		//Logging.log(this, "Search for the ")
		if(mSessionToChannelsMapping.containsKey(pComSession)) {
			for(ComChannel tComChannel : mSessionToChannelsMapping.get(pComSession)) {
				if(((ICluster)tComChannel.getParent()).getClusterID().equals(pCluster.getClusterID())) {
					Tuple<Long, Long> tTuple = new Tuple<Long, Long>(pSource.getClusterID(), pCluster.getClusterID());
					boolean tSourceIsContained = isClusterMultiplexed(tTuple);
					Logging.log(this, "Comparing \"" + tComChannel + "\" and \"" + (tSourceIsContained ? getDemultiplex(tTuple) : "") + "\" " + tComChannel.getRemoteClusterName() + ", " + (tSourceIsContained ? getDemultiplex(tTuple).getRemoteClusterName() : "" ));
					if(tSourceIsContained && getDemultiplex(tTuple) == tComChannel) {
						Logging.log(this, "Returning " + tComChannel + " for request on cluster " + pCluster);
						tResult = tComChannel;
					} else {
						Logging.log(this, "Source is \"" + pSource + "\", target is \"" + pCluster+ "\", DEMUXER of source is \"" + getDemultiplex(tTuple) + "\", currently evaluated CEP is \"" + tComChannel + "\"");
					}
					if(!isClusterMultiplexed(tTuple) && ((ICluster)tComChannel.getParent()).getClusterID().equals(pCluster.getClusterID())) {
						Logging.log(this, "Returning " + tComChannel + " for request on cluster " + pCluster);
						tResult = tComChannel;
					}
				}
			}
		}
		
		if (tResult == null){
			Logging.err(this, "Unable to find communication channel for ComSesseion " + pComSession + " and target cluster " + pCluster + ", known mappings are:");
			for(ComSession tCEP : mSessionToChannelsMapping.keySet()) {
				Logging.log(this, "       .." + tCEP + " to " + mSessionToChannelsMapping.get(tCEP));
			}
		}

		return tResult;
	}
	
	public String toString()
	{
		return getClass().getSimpleName() + "@" + mHRMController.getNode() + (mParent != null ? "@" + mParent.getHierarchyLevel().getValue() + "(Parent=" + mParent + ")" : "");
	}
	
	public void registerDemultiplex(Long pSourceClusterID, Long pTargetClusterID, ComChannel pCEP)
	{
		Logging.log(this, "Registering demultiplex for Cluster ID" + pSourceClusterID + " to " + pTargetClusterID + " via " + pCEP);
		mClusterToCEPMapping.put(new Tuple<Long, Long>(pSourceClusterID, pTargetClusterID), pCEP);
	}
	
	private ComChannel getDemultiplex(Tuple<Long, Long> pPair)
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
