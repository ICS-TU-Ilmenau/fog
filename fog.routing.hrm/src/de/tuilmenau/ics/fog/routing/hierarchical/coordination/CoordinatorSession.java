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

import de.tuilmenau.ics.fog.application.Session;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.packets.NeighborRoutingInformation;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.ClusterDiscovery;
import de.tuilmenau.ics.fog.packets.hierarchical.clustering.ClusterDiscovery.NestedDiscovery;
import de.tuilmenau.ics.fog.packets.hierarchical.MultiplexedPackage;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyData;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ClusterName;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.HierarchyLevel;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Tuple;

public class CoordinatorSession extends Session
{
	private HRMController mHRMController = null;
	private boolean mServerSide = false;
	private L2Address mSessionOriginL2Address = null;
	private L2Address mPeerIdentification = null;
	private HierarchyLevel mHierarchyLevel = null;
	private CoordinatorCEPMultiplexer mMux;
	private Route mRouteToPeer;
	
	/**
	 * 
	 * @param pHRMController is the coordinator this connection end point is associated to
	 * @param pServerSide indicates whether this connection end point is the origin of a server or not
	 * @param pLevel is the level this connection end point is located at
	 * @param pMux is the multiplexer to use
	 * 
	 */
	public CoordinatorSession(HRMController pHRMController, boolean pServerSide, HierarchyLevel pLevel, CoordinatorCEPMultiplexer pMux)
	{
		super(false, Logging.getInstance(), null);
		mHRMController = pHRMController;
		
		mSessionOriginL2Address = mHRMController.getHRS().getCentralFNL2Address(); 
		
		mServerSide = pServerSide;
		mHierarchyLevel = pLevel;
		mMux = pMux;
		if (mServerSide){
			Logging.log(this, "SERVER SESSION CREATED");
		}else{
			Logging.log(this, "CLIENT SESSION CREATED");
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean receiveData(Object pData)
	{
		Logging.log(this, "RECEIVED SESSION DATA: " + pData);
		
		if(pData instanceof NeighborRoutingInformation) {
			// get the packet
			NeighborRoutingInformation tNeighborRoutingInformationPacket = (NeighborRoutingInformation)pData;

			if (HRMConfig.DebugOutput.GUI_SHOW_TOPOLOGY_DETECTION){
				Logging.log(this, "NEIGHBOR ROUTING INFO found: " + tNeighborRoutingInformationPacket);
			}
			
			// get the L2Address of the peer
			mPeerIdentification = tNeighborRoutingInformationPacket.getCentralFNL2Address();
			if(mServerSide) {
				/**
				 * Determine the route to the peer
				 */
				Route tRouteToPeer = null;
				// search a route form the central FN to the intermediate FN between the central FN and the bus
				try {
					tRouteToPeer = getHRMController().getHRS().getRoute(tNeighborRoutingInformationPacket.getRoutingTargetFNL2Address(), new Description(), getHRMController().getNode().getIdentity());
				} catch (RoutingException tExc) {
					Logging.err(this, "Unable to find route to ", tExc);
				} catch (RequirementsException tExc) {
					Logging.err(this, "Unable to fulfill requirements ", tExc);
				}
				/**
				 * Update the local route to the peer
				 */
				mRouteToPeer = tRouteToPeer;
				mRouteToPeer.add(new RouteSegmentAddress(mPeerIdentification));
				if(mHierarchyLevel.isBaseLevel()) {
					getHRMController().getHRS().registerRoute(mSessionOriginL2Address, mPeerIdentification, mRouteToPeer);
					getHRMController().getHRS().addRouteToDirectNeighbor(mPeerIdentification, mRouteToPeer);
				}
				write(mSessionOriginL2Address);
			}

		} else if (pData instanceof L2Address) {
			mPeerIdentification = (L2Address) pData;
			if(mHierarchyLevel.isBaseLevel()) {
				mRouteToPeer.add(new RouteSegmentAddress(mPeerIdentification));
				getHRMController().getHRS().registerRoute(mSessionOriginL2Address, mPeerIdentification, mRouteToPeer);
			} else {
				if(mServerSide) {
					write(mSessionOriginL2Address);
				}
			}
		} else if (pData instanceof MultiplexedPackage) {
			MultiplexedPackage tPackage = (MultiplexedPackage) pData;
			ClusterName tTargetCluster = tPackage.getDestinationCluster();
			
			try {
				CoordinatorCEPChannel tCEP = mMux.findCEPChannel(this, (ClusterName)tPackage.getSourceCluster(), tTargetCluster);
				if(tCEP != null) {
					Logging.log(this, "Forwarding " + tPackage.getData() + " from " + tPackage.getSourceCluster() + " to " + tPackage.getDestinationCluster() + " with " + tCEP);
					tCEP.receive(tPackage.getData());
				} else {
					Logging.warn(this, "No demultiplexed connection available ");
				}
			} catch (NetworkException tExc) {
				Logging.err(this, "Unable to forward data", tExc);
			}
		} else if(pData instanceof ClusterDiscovery) {
			Logging.log(this, "Received " + pData);
			if(((ClusterDiscovery)pData).isRequest()) {
				for(NestedDiscovery tNestedDiscovery : ((ClusterDiscovery)pData).getDiscoveries()) {
					boolean tWasDelivered = false;

					String tAnalyzedClusters = new String("");
					for(CoordinatorCEPChannel tCEP: mMux.getDemuxCEPs(this)) {
						tAnalyzedClusters += tCEP.getPeer() + "\n";
						if(tCEP.getPeer().getClusterID().equals(tNestedDiscovery.getTargetClusterID())) {
							try {
								tCEP.handleClusterDiscovery(tNestedDiscovery, true);
								tWasDelivered = true;
							} catch (NetworkException tExc) {
								Logging.err(this, "Error when forwarding nested discovery to clusters ",  tExc);
							}
						}
					}
					if(!tWasDelivered) {
						Logging.log(this, "Unable to deliver\n" + tNestedDiscovery + "\nto clusters\n" + tAnalyzedClusters + "\nand CEPs\n" + mMux.getDemuxCEPs(this));
					}
				}
				((ClusterDiscovery)pData).isAnswer();
				Logging.log(this, "Sending back discovery " + pData);
				write((ClusterDiscovery)pData);
			} else {
				for(NestedDiscovery tNestedDiscovery : ((ClusterDiscovery)pData).getDiscoveries()) {
					boolean tWasDelivered = false;
					String tAnalyzedClusters = new String("");
					for(CoordinatorCEPChannel tCEP: mMux.getDemuxCEPs(this)) {
						tAnalyzedClusters += tCEP.getPeer() + "\n";

						if(tCEP.getPeer().getClusterID().equals(tNestedDiscovery.getOrigin())) {
							try {
								tCEP.handleClusterDiscovery(tNestedDiscovery, false);
								tWasDelivered = true;
							} catch (NetworkException tExc) {
								Logging.err(this, "Error when forwarding nested discovery",  tExc);
							}
						}
					}
					if(!tWasDelivered) {
						Logging.log(this, "Unable to deliver\n" + tNestedDiscovery + "\nto clusters\n" + tAnalyzedClusters + "\nand CEPs\n" + mMux.getDemuxCEPs(this));
					}
				}
				((ClusterDiscovery)pData).completed();
				synchronized(((ClusterDiscovery)pData)) {
					Logging.log(this, "Notifying about come back of " + pData);
					((ClusterDiscovery)pData).notifyAll();
				}
			}
		}
		
		return true;
	}

	/**
	 * 
	 * @param pData is the data that should be sent to the entity at the opposite.
	 * @return true if information was sent successfully
	 */
	public boolean write(Serializable pData)
	{
		if(pData instanceof MultiplexedPackage && ((MultiplexedPackage)pData).getData() instanceof TopologyData) {
			Logging.log(this, "Sending topology data to " + ((MultiplexedPackage)pData).getDestinationCluster().getClusterID());
		}
		if(getConnection() != null && getConnection().isConnected()) {
			try	{
				getConnection().write(pData);
			} catch (NetworkException e) {
				Logging.err(this, "Error while writing to socket", e);
				return false;
			}
		} else {
			Logging.err(this, "Unable to send " + pData + " because socket is not connected");
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @return The route to the central forwarding node of the peer is returned.
	 */
	public Route getRouteToPeer()
	{
		return mRouteToPeer;
	}
	
	/**
	 * 
	 * @param pRoute is the route that is used to reach the peer
	 */
	public void setRouteToPeer(Route pRoute)
	{
		Logging.log(this, "### Setting route to peer: " + pRoute);
		mRouteToPeer = pRoute;
	}
	
	/**
	 * 
	 * @return The physical name of the central forwarding node at sender side is returned.
	 */
	public HRMName getSourceRoutingServiceAddress()
	{
		return mSessionOriginL2Address;
	}
	
	/**
	 * 
	 * @return The physical name of central forwarding node at the other side is returned.
	 */
	public HRMName getPeerRoutingServiceAddress()
	{
		return mPeerIdentification;
	}
	
	/**
	 * 
	 * @return coordinator this connection end point is attached to
	 */
	public HRMController getHRMController()
	{
		return mHRMController;
	}
	
	public String toString()
	{
		if(mPeerIdentification != null ) {
			return getClass().getSimpleName() + "@" + mHRMController.getNodeGUIName() + "@" + getMultiplexer() + "(Initiator=" + mSessionOriginL2Address + ", Peer=" + mPeerIdentification + ")";
		} else {
			return getClass().getSimpleName() + "@" + mHRMController.getNodeGUIName() + "(Initiator=" + mSessionOriginL2Address + ")";
		}
		 
	}
	
	/**
	 * 
	 * @return multiplexer that is in charge of this connection - only of relevance if Multicast is implemented
	 */
	public CoordinatorCEPMultiplexer getMultiplexer()
	{
		return mMux;
	}
}
