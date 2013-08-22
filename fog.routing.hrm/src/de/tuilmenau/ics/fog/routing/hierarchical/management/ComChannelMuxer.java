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
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ClusterDescriptionProperty;
import de.tuilmenau.ics.fog.routing.hierarchical.properties.ClusterDescriptionProperty.ClusterMemberDescription;
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
	
	public void connectToNeighborCoordinators(ICluster pTargetCluster, Coordinator pSourceCoordinator)
	{
		Logging.log(this, "############## Connecting to possible cluster member: " + pTargetCluster + ", source=" + pSourceCoordinator);
		
		/**
		 * get the name of the central FN
		 */
		L2Address tLocalCentralFNL2Address = mHRMController.getHRS().getCentralFNL2Address();

		ComChannel tComChannel = null;
		int tFoundNeighbors = 0;
		
		if(!mConnectedEntities.contains(pTargetCluster.getCoordinatorName())) {
			mConnectedEntities.add(pTargetCluster.getCoordinatorName());

			ControlEntity tControlEntityTargetCluster = (ControlEntity)pTargetCluster;

			Name tCoordinatorName = pTargetCluster.getCoordinatorName();

			HierarchyLevel tSourceClusterHierLvl = new HierarchyLevel(this, pSourceCoordinator.getHierarchyLevel().getValue());
			HierarchyLevel tTargetClusterHierLvl = new HierarchyLevel(this, pTargetCluster.getHierarchyLevel().getValue() + 1);

			ControlEntity tTargetControlEntity = (ControlEntity)pTargetCluster;

			Logging.log(this, "    ..creating cluster description");
			ClusterDescriptionProperty tPropClusterDescription = new ClusterDescriptionProperty(tTargetControlEntity.superiorCoordinatorL2Address().getComplexAddress().longValue(), tTargetClusterHierLvl, pTargetCluster.getToken());
			
			ComSession tSession = new ComSession(mHRMController, false, tSourceClusterHierLvl, mHRMController.getCoordinatorMultiplexerOnLevel(pSourceCoordinator));
			ClusterDiscovery tBigDiscovery = new ClusterDiscovery(mHRMController.getNodeName());
			
			Logging.log(this, "    ..searching for neighbor coordinators on hierarchy level: " + (tSourceClusterHierLvl.getValue() - 1));
			
			for(Coordinator tCoordinator : mHRMController.getAllCoordinators(new HierarchyLevel(this, tSourceClusterHierLvl.getValue()))) {
				Logging.log(this, "         ..found [" + tFoundNeighbors + "] : " + tCoordinator);

				tComChannel = new ComChannel(mHRMController, tCoordinator);
				tComChannel.setPeerPriority(pTargetCluster.getPriority());
				tSession.getMultiplexer().mapChannelToSession(tComChannel, tSession);
				synchronized(mClusterToCEPMapping) {
					Logging.log(this, "Registering multiplex" + tCoordinator.getClusterID() + " to " + pTargetCluster.getClusterID() + " with connection endpoint " + tComChannel);
					mClusterToCEPMapping.put(new Tuple<Long, Long>(tCoordinator.getClusterID(), pTargetCluster.getClusterID()), tComChannel);
				}
				tComChannel.setRemoteClusterName(new ClusterName(pTargetCluster.getToken(), pTargetCluster.getClusterID(), pTargetCluster.getHierarchyLevel()));
				tFoundNeighbors++;
			}
			
			if (tFoundNeighbors == 0){
				Logging.log(this, "    ..haven't found a neighbor coordinator, known coordinators are: " + mHRMController.getAllCoordinators().toString());
			}else{
				Logging.log(this, "    ..found " + tFoundNeighbors + " neighbor coordinators");
			}

			for(Coordinator tCoordinator : mHRMController.getAllCoordinators(new HierarchyLevel(this, tSourceClusterHierLvl.getValue()))) {
				Logging.log(this, "Ping2: " + tCoordinator);

				if(tTargetControlEntity.superiorCoordinatorL2Address() == null) {
					//TODO: fall unmoeglich, wuerde sonst oben crashen
					Logging.err(this, "Error on trying to contact other clusters, as name is set please check its address");
				} else {
					Cluster tCoordinatorCluster = tCoordinator.getCluster();
					
					/**
					 * Describe the new created cluster
					 */
				    Logging.log(this, "    ..creating cluster member description for the found cluster " + tCoordinatorCluster);
					ClusterMemberDescription tClusterMemberDescription = tPropClusterDescription.addClusterMember(tCoordinatorCluster.getClusterID(), tCoordinatorCluster.getToken(), tCoordinatorCluster.getPriority());
					
					tClusterMemberDescription.setSourceName(mHRMController.getNode().getCentralFN().getName());
					tClusterMemberDescription.setSourceL2Address(tLocalCentralFNL2Address);
					
					List<AbstractRoutingGraphLink> tClusterListToRemote = mHRMController.getRouteARG(tCoordinator.getCluster(), tControlEntityTargetCluster);
					if(!tClusterListToRemote.isEmpty()) {
						/*
						 * we need the last hop in direct to the neighbor
						 */
						ICluster tPredecessorToRemote = (ICluster) mHRMController.getOtherEndOfLinkARG(tControlEntityTargetCluster, tClusterListToRemote.get(tClusterListToRemote.size()-1));
						tClusterMemberDescription.setPredecessor(new ClusterName(tPredecessorToRemote.getToken(), tPredecessorToRemote.getClusterID(), tPredecessorToRemote.getHierarchyLevel()));
						Logging.log(this, "Successfully set predecessor for " + pTargetCluster + ":" + tPredecessorToRemote);
					} else {
						Logging.log(this, "Unable to set predecessor for " + pTargetCluster + ":");
					}
					
					for(ControlEntity tNeighbor: tCoordinator.getCluster().getNeighborsARG()) {
						ICluster tIClusterNeighbor = (ICluster)tNeighbor;
						
						DiscoveryEntry tEntry = new DiscoveryEntry(tIClusterNeighbor.getToken(), tIClusterNeighbor.getCoordinatorName(), tIClusterNeighbor.getClusterID(), tNeighbor.superiorCoordinatorL2Address(), tNeighbor.getHierarchyLevel());
						tEntry.setPriority(tNeighbor.getPriority());
						List<AbstractRoutingGraphLink> tClusterList = mHRMController.getRouteARG(tCoordinator.getCluster(), tNeighbor);
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
						if(tCoordinator.getPathToCoordinator(tCoordinator.getCluster(), tIClusterNeighbor) != null) {
							for(RoutingServiceLinkVector tVector : tCoordinator.getPathToCoordinator(tCoordinator.getCluster(), tIClusterNeighbor)) {
								tEntry.addRoutingVectors(tVector);
							}
						}
						tClusterMemberDescription.addDiscoveryEntry(tEntry);
					}
				}
			}
			
			Identity tIdentity = mHRMController.getNode().getIdentity();
			Description tConnectDescription = mHRMController.createHRMControllerDestinationDescription();
			tConnectDescription.set(tPropClusterDescription);
			
			Logging.log(this, "Connecting to " + pTargetCluster);
			Connection tConn = null;;
			try {
				Logging.log(this, "CREATING CONNECTION to " + tCoordinatorName);
				tConn = pSourceCoordinator.getHRMController().getHost().connectBlock(tCoordinatorName, tConnectDescription, tIdentity);
				tSession.start(tConn);
				tSession.write(tLocalCentralFNL2Address);
			} catch (NetworkException tExc) {
				Logging.err(this, "Unable to connect to " + tCoordinatorName, tExc);
			}

			for(Coordinator tCoordinator : mHRMController.getAllCoordinators(new HierarchyLevel(this, tSourceClusterHierLvl.getValue() - 1))) {
				LinkedList<Integer> tTokens = new LinkedList<Integer>();
				for(ControlEntity tNeighbor : tCoordinator.getCluster().getNeighborsARG()) {
					if(tNeighbor.getHierarchyLevel().getValue() == tCoordinator.getHierarchyLevel().getValue() - 1) {
						tTokens.add(((ICluster) tNeighbor).getToken());
					}
				}
				tTokens.add(tCoordinator.getCluster().getToken());
				tCoordinator.getComChannels().add(tComChannel);
				if(!pTargetCluster.getCoordinatorName().equals(mHRMController.getNode().getCentralFN().getName())) {
					int tDistance = 0;
					if (pTargetCluster instanceof ClusterProxy){
						ClusterProxy tClusterProxy = (ClusterProxy) pTargetCluster;
					
						tDistance = mHRMController.getClusterDistance(tClusterProxy); 
					}
					
					NestedDiscovery tDiscovery = tBigDiscovery.new NestedDiscovery(tTokens, pTargetCluster.getClusterID(), pTargetCluster.getToken(), pTargetCluster.getHierarchyLevel(), tDistance);
					Logging.log(this, "Created " + tDiscovery + " for " + pTargetCluster);
					tDiscovery.setOrigin(tCoordinator.getClusterID());
					tDiscovery.setTargetClusterID(tTargetControlEntity.superiorCoordinatorL2Address().getComplexAddress().longValue());
					tBigDiscovery.addNestedDiscovery(tDiscovery);
				}
			}
			
			tSession.write(tBigDiscovery);
			
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
							Cluster tFirstCluster = mHRMController.getClusterByID(tTuple.getFirst());
							Cluster tSecondCluster = mHRMController.getClusterByID(tTuple.getSecond());
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
		}
		
		if (tComChannel == null){
			Logging.warn(this, "Haven't created a communication channel, found " + tFoundNeighbors + " neighbor coordinators for source " + pSourceCoordinator + " and target " + pTargetCluster);
		}else{
			Logging.log(this, "Found " + tFoundNeighbors + " neighbor coordinators for source " + pSourceCoordinator + " and target " + pTargetCluster);
		}
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
					Logging.log(this, "Comparing \"" + tComChannel + "\" and \"" + (tSourceIsContained ? getComChannel(tTuple) : "") + "\" " + tComChannel.getRemoteClusterName() + ", " + (tSourceIsContained ? getComChannel(tTuple).getRemoteClusterName() : "" ));
					if(tSourceIsContained && getComChannel(tTuple) == tComChannel) {
						Logging.log(this, "Returning " + tComChannel + " for request on cluster " + pCluster);
						tResult = tComChannel;
					} else {
						Logging.log(this, "Source is \"" + pSource + "\", target is \"" + pCluster+ "\", DEMUXER of source is \"" + getComChannel(tTuple) + "\", currently evaluated CEP is \"" + tComChannel + "\"");
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
	
	public void mapClusterToComChannel(Long pSourceClusterID, Long pTargetClusterID, ComChannel pComChannel)
	{
		Logging.log(this, "Adding CLUSTER-to-COMCHANNEL mapping: cluster " + "" + " is mapped to channel \"" + pComChannel + "\"");

		synchronized (mClusterToCEPMapping) {
			mClusterToCEPMapping.put(new Tuple<Long, Long>(pSourceClusterID, pTargetClusterID), pComChannel);
		}
	}
	
	private ComChannel getComChannel(Tuple<Long, Long> pPair)
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
