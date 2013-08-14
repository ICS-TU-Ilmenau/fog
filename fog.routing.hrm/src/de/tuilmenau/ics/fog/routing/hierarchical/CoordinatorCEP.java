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

import de.tuilmenau.ics.fog.application.util.Session;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.NetworkException;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.packets.hierarchical.ClusterDiscovery;
import de.tuilmenau.ics.fog.packets.hierarchical.ClusterDiscovery.NestedDiscovery;
import de.tuilmenau.ics.fog.packets.hierarchical.MultiplexedPackage;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyEnvelope;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RouteSegmentAddress;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.ClusterDummy;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.fog.util.Tuple;

public class CoordinatorCEP extends Session
{
	private Coordinator mReferenceCoordinator=null;
	private boolean mServerSide = false;
	private L2Address mSourceIdentification = null;
	private L2Address mPeerIdentification = null;
	private int mLevel = 0;
	private CoordinatorCEPMultiplexer mMux;
	private Route mRouteToPeer;
	
	/**
	 * 
	 * @param pLogger
	 * @param pCoord is the coordinator this connection endpoint is associated to
	 * @param pServerSide indicates whether this connection endpoint is the origin of a server or not
	 * @param pLevel is the level this connection end point is located at
	 * @param pMux is the multiplexer to use
	 * 
	 */
	public CoordinatorCEP(Logger pLogger, Coordinator pCoord, boolean pServerSide, int pLevel, CoordinatorCEPMultiplexer pMux)
	{
		super(false, Logging.getInstance(), null);
		mReferenceCoordinator = pCoord;		
		mSourceIdentification = (L2Address) getCoordinator().getRSName();
		
		getLogger().log(this, "Created");

		mServerSide = pServerSide;
		mLevel = pLevel;
		mMux = pMux;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean receiveData(Object pData)
	{
		if(pData instanceof Tuple && !(pData instanceof HRMID)) {
			mPeerIdentification = ((Tuple<L2Address, L2Address>)pData).getFirst();
			if(mServerSide) {
				Route tRouteToPeer = null;
				try {
					tRouteToPeer = getCoordinator().getHRS().getRoute(getCoordinator().getCentralFN(), ((Tuple<HRMID, HRMID>)pData).getSecond(), new Description(), getCoordinator().getNodeIdentity());
					mRouteToPeer = tRouteToPeer;
				} catch (RoutingException tExc) {
					getLogger().err(this, "Unable to find route to ", tExc);
				} catch (RequirementsException tExc) {
					getLogger().err(this, "Unable to fulfill requirements ", tExc);
				}
				mRouteToPeer.add(new RouteSegmentAddress(mPeerIdentification));
				if(mLevel == 0) {
					getCoordinator().getHRS().registerRoute(mSourceIdentification, mPeerIdentification, mRouteToPeer);
				}
				write(mSourceIdentification);
			}

		} else if (pData instanceof L2Address) {
			mPeerIdentification = (L2Address) pData;
			if(mLevel == 0) {
				mRouteToPeer.add(new RouteSegmentAddress(mPeerIdentification));
				getCoordinator().getHRS().registerRoute(mSourceIdentification, mPeerIdentification, mRouteToPeer);
			} else {
				if(mServerSide) {
					write(mSourceIdentification);
				}
			}
		} else if (pData instanceof MultiplexedPackage) {
			MultiplexedPackage tPackage = (MultiplexedPackage) pData;
			for(ClusterDummy tTarget : tPackage.getDestinationCluster()) {
				try {
					CoordinatorCEPDemultiplexed tCEP = mMux.getDemuxedCEP(this, (ClusterDummy)tPackage.getSourceCluster(), tTarget);
					if(tCEP != null) {
						getLogger().log(this, "Forwarding " + tPackage.getData() + " from " + tPackage.getSourceCluster() + " to " + tPackage.getDestinationCluster() + " with " + tCEP);
						tCEP.receive(tPackage.getData());
					} else {
						getLogger().warn(this, "No demultiplexed connection available ");
					}
				} catch (NetworkException tExc) {
					getLogger().err(this, "Unable to forward data", tExc);
				}
			}
		} else if(pData instanceof ClusterDiscovery) {
			getLogger().log(this, "Received " + pData);
			if(((ClusterDiscovery)pData).isRequest()) {
				for(NestedDiscovery tNestedDiscovery : ((ClusterDiscovery)pData).getDiscoveries()) {
					boolean tWasDelivered = false;

					String tAnalyzedClusters = new String("");
					for(CoordinatorCEPDemultiplexed tCEP: mMux.getDemuxCEPs(this)) {
						tAnalyzedClusters += tCEP.getCluster() + "\n";
						if(tCEP.getCluster().getClusterID().equals(tNestedDiscovery.getTargetClusterID())) {
							try {
								tCEP.handleClusterDiscovery(tNestedDiscovery, true);
								tWasDelivered = true;
							} catch (NetworkException tExc) {
								getLogger().err(this, "Error when forwarding nested discovery to clusters ",  tExc);
							}
						}
					}
					if(!tWasDelivered) {
						getLogger().log(this, "Unable to deliver\n" + tNestedDiscovery + "\nto clusters\n" + tAnalyzedClusters + "\nand CEPs\n" + mMux.getDemuxCEPs(this));
					}
				}
				((ClusterDiscovery)pData).isAnswer();
				Logging.log(this, "Sending back discovery " + pData);
				write((ClusterDiscovery)pData);
			} else {
				for(NestedDiscovery tNestedDiscovery : ((ClusterDiscovery)pData).getDiscoveries()) {
					boolean tWasDelivered = false;
					String tAnalyzedClusters = new String("");
					for(CoordinatorCEPDemultiplexed tCEP: mMux.getDemuxCEPs(this)) {
						tAnalyzedClusters += tCEP.getCluster() + "\n";

						if(tCEP.getCluster().getClusterID().equals(tNestedDiscovery.getOrigin())) {
							try {
								tCEP.handleClusterDiscovery(tNestedDiscovery, false);
								tWasDelivered = true;
							} catch (NetworkException tExc) {
								getLogger().err(this, "Error when forwarding nested discovery",  tExc);
							}
						}
					}
					if(!tWasDelivered) {
						getLogger().log(this, "Unable to deliver\n" + tNestedDiscovery + "\nto clusters\n" + tAnalyzedClusters + "\nand CEPs\n" + mMux.getDemuxCEPs(this));
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
		if(pData instanceof MultiplexedPackage && ((MultiplexedPackage)pData).getData() instanceof TopologyEnvelope) {
			Logging.log(this, "Sending topology envelope to " + ((MultiplexedPackage)pData).getDestinationCluster().getFirst().getClusterID());
		}
		if(getConnection() != null && getConnection().isConnected()) {
			try	{
				getConnection().write(pData);
			} catch (NetworkException e) {
				getLogger().err(this, "Error while writing to socket", e);
				return false;
			}
		} else {
			getLogger().err(this, "Unable to send " + pData + " because socket is not connected");
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
		mRouteToPeer = pRoute;
	}
	
	/**
	 * 
	 * @return The physical name of the central forwarding node at sender side is returned.
	 */
	public HRMName getSourceRoutingServiceAddress()
	{
		return mSourceIdentification;
	}
	
	/**
	 * 
	 * @return The phsyical name of central forwarding node at the other side is returned.
	 */
	public HRMName getPeerRoutingServiceAddress()
	{
		return mPeerIdentification;
	}
	
	/**
	 * 
	 * @return coordinator this connection endpoint is attached to
	 */
	public Coordinator getCoordinator()
	{
		return mReferenceCoordinator;
	}
	
	public String toString()
	{
		if(mPeerIdentification != null ) {
			return this.getClass().getSimpleName() + "@" + mSourceIdentification + "->" + mPeerIdentification;
		} else {
			return this.getClass().getSimpleName() + "@" + mSourceIdentification;
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

	public Logger getLogger()
	{
		return getCoordinator().getLogger();
	}
}
