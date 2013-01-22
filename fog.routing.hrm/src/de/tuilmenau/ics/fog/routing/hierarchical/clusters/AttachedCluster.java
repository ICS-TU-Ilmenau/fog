/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.clusters;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.packets.hierarchical.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborZoneAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyEnvelope;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.hierarchical.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.CoordinatorCEP;
import de.tuilmenau.ics.fog.routing.hierarchical.CoordinatorCEPDemultiplexed;
import de.tuilmenau.ics.fog.routing.hierarchical.CoordinatorCEPMultiplexer;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalSignature;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.IElementDecorator;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;

public class AttachedCluster implements Cluster, IElementDecorator
{
	private static final long serialVersionUID = -8746079632866375924L;
	private LinkedList<RoutingServiceLinkVector> mVectors;
	private int mToken;
	protected int mLevel;
	protected float mPriority;
	protected float mHighestPriority;
	protected float mCoordinatorPriority;
	protected Name mCoordName;
	protected HRMName mCoordAddress;
	protected L2Address mNegotiatingHost;
	protected Long mClusterID;
	protected Coordinator mCoordinatorInstance;
	protected CoordinatorCEP mCEP;
	protected HierarchicalSignature mCoordSignature;
	protected Route mRouteToCoordinator;
	protected HRMID mHRMID;
	protected Name mAnnouncer;
	protected LinkedList<CoordinatorCEPDemultiplexed> mAnnouncedCEPs;
	protected CoordinatorCEPDemultiplexed mNegotiator=null;
	private int mClusterHopsOnOpposite;
	private IntermediateCluster mSourceIntermediateCluster = null;
	private LinkedList<CoordinatorCEPDemultiplexed> mNegotiators= new LinkedList<CoordinatorCEPDemultiplexed>();
	private boolean mInterASCluster = false;
	private HashMap<CoordinatorCEPDemultiplexed, Integer> mClustersOnOppositeCounter = new HashMap<CoordinatorCEPDemultiplexed, Integer>();
	private StackTraceElement[] mStackTrace = null;
	
	/**
	 * 
	 * @param pClusterID identificator of the cluster
	 * @param pCoordName name of the coordinator
	 * @param pAddress routing service address
	 * @param pToken as the token this cluster got
	 * @param pLevel is the level this cluster is related to
	 * @param pResponsibleCoordinator as the coordinator this cluster works on (local)
	 */
	public AttachedCluster(Long pClusterID, Name pCoordName, HRMName pAddress, int pToken, int pLevel, Coordinator pResponsibleCoordinator)
	{	
		mAnnouncer = pResponsibleCoordinator.getReferenceNode().getCentralFN().getName();
		mCoordAddress = pAddress;
		setCoordinatorName(pCoordName);
		mClusterID = pClusterID;
		mCoordinatorInstance = pResponsibleCoordinator;
		mCoordName = pCoordName;
		mToken = pToken;
		mLevel = pLevel;
		mStackTrace = Thread.currentThread().getStackTrace();
	}
	
	public void addAnnouncedCEP(CoordinatorCEPDemultiplexed pCEP)
	{
		if(mAnnouncedCEPs == null) {
			mAnnouncedCEPs = new LinkedList<CoordinatorCEPDemultiplexed>();
		}
		Logging.log(this.getClusterDescription(), "Adding announcer " + pCEP);
		mAnnouncedCEPs.add(pCEP);
	}
	
	public int getClustersToTarget()
	{
		return getCoordinator().getClusterDistance(this);
	}

	public float getHighestPriority()
	{
		return getPriority();
	}

	public void initiateElection()
	{
		/*
		 * not needed, this is just a dummy for topology
		 */
	}

	public void interruptElection()
	{
		/*
		 * not needed, this is just a dummy for topology
		 */
	}

	public void handleAnnouncement(NeighborZoneAnnounce pAnnounce, CoordinatorCEPDemultiplexed pCEP)
	{
		if(pAnnounce.getRoutingVectors() != null) {
			for(RoutingServiceLinkVector tVector : pAnnounce.getRoutingVectors()) {
				getCoordinator().getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
			}
		}
		Cluster tCluster = getCoordinator().getCluster(ClusterDummy.compare(pAnnounce.getClusterID(), pAnnounce.getToken(), pAnnounce.getLevel()));
		if(tCluster == null)
		{
			tCluster = new AttachedCluster(
					pAnnounce.getClusterID(),
					pAnnounce.getCoordinatorName(),
					pAnnounce.getCoordAddress(),
					pAnnounce.getToken(),
					mLevel,
					getCoordinator());
			getCoordinator().setSourceIntermediateCluster(tCluster, getCoordinator().getSourceIntermediate(this));
			((AttachedCluster)tCluster).addAnnouncedCEP(pCEP);
			tCluster.setPriority(pAnnounce.getCoordinatorsPriority());
			tCluster.setToken(pAnnounce.getToken());
		} else {
			getLogger().log(this, "Cluster announced by " + pAnnounce + " is an intermediate neighbor ");
		}
		//((AttachedCluster)tCluster).setNegotiatingHost(pAnnounce.getAnnouncersAddress());

		/*
		 * function checks whether neighbor relation was established earlier
		 */
		addNeighborCluster(tCluster);

		if(pAnnounce.getCoordinatorName() != null) {
			RoutingService tRS = (RoutingService)getCoordinator().getReferenceNode().getRoutingService();
			if(! tRS.isKnown(pAnnounce.getCoordinatorName())) {
				try {
					getCoordinator().getHRS().registerNode(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
				} catch (RemoteException tExc) {
					getLogger().err(this, "Unable to register " + pAnnounce.getCoordinatorName() + " at name mapping service", tExc);
				}
			}
		}
		pCEP.addAnnouncedCluster(tCluster, this);
	}

	public CoordinatorCEPDemultiplexed getCoordinatorCEP()
	{
		return null;
	}

	public synchronized void setCoordinatorCEP(CoordinatorCEPDemultiplexed pCoord, HierarchicalSignature pCoordSignature, Name pCoordName, HRMName pAddress)
	{
		this.mCoordAddress = pAddress;
		this.mCoordName = pCoordName;
	}

	public void addNeighborCluster(Cluster pNeighbor)
	{
		getCoordinator().getClusterMap().link(this,	pNeighbor, new NodeConnection(NodeConnection.ConnectionType.REMOTE));
	}

	public void setRouteToCoordinator(Route pPath)
	{
		mRouteToCoordinator = pPath;
	}

	public void setHRMID(HRMID pHRMID)
	{
		mHRMID = pHRMID;
	}

	@Override
	public void setCoordinatorPriority(float pCoordinatorPriority)
	{
		/*
		 * not needed, this is just a dummy for topology
		 */
	}

	@Override
	public float getCoordinatorPriority()
	{
		return mCoordinatorPriority;
	}

	@Override
	public void setPriority(float pPriority)
	{
		mPriority = pPriority;
	}

	@Override
	public Coordinator getCoordinator() {
		return this.mCoordinatorInstance;
	}

	@Override
	public LinkedList<CoordinatorCEPDemultiplexed> getParticipatingCEPs()
	{
		LinkedList<CoordinatorCEPDemultiplexed> tCEPs = new LinkedList<CoordinatorCEPDemultiplexed>();
		//tCEPs.add(mCEP);
		return tCEPs;
	}

	@Override
	public void addParticipatingCEP(CoordinatorCEPDemultiplexed pParticipatingCEP)
	{
		/*
		 * not needed, this is just a dummy for topology
		 */
	}

	@Override
	public Long getClusterID()
	{
		return mClusterID;
	}

	@Override
	public int getLevel() {
		return mLevel;
	}

	@Override
	public Name getCoordinatorName()
	{
		return this.mCoordName;
	}

	@Override
	public float getPriority()
	{
		return mPriority;
	}

	@Override
	public String getClusterDescription()
	{
		return this.getClass().getSimpleName() + "(" + this.mAnnouncer + "->" + this.mCoordName + ")"+"PR" + "(" + mPriority + ")" + "ID(" + this.mClusterID + ")TK(" + mToken + ")@" + mLevel;
	}

	@Override
	public void setCoordinatorName(Name pCoordName)
	{
		mCoordName = pCoordName;
	}

	@Override
	public HRMName getCoordinatorsAddress()
	{
		return mCoordAddress;
	}

	@Override
	public void setToken(int pToken) {
		mToken = pToken;
	}

	@Override
	public int getToken() {
		return mToken;
	}

	@Override
	public LinkedList<Cluster> getNeighbors() {
		LinkedList<Cluster> tCluster = new LinkedList<Cluster>();
		for(VirtualNode tNode : getCoordinator().getClusterMap().getNeighbors(this)) {
			if(tNode instanceof Cluster) {
				tCluster.add((Cluster) tNode);
			}
		}
		return tCluster;
	}

	@Override
	public HierarchicalSignature getCoordinatorSignature() {
		return mCoordSignature;
	}

	@Override
	public HRMID retrieveAddress() {
		return mHRMID;
	}

	/**
	 * Return the announcer of this cluster, so the next hop is determined
	 * (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.routing.hierarchical.clusters.VirtualNode#retrieveName()
	 */
	@Override
	public Name retrieveName() {
		return mAnnouncer;
	}

	@Override
	public Cluster getClusterWithHighestPriority() {
		/*
		 * not needed, this is just a dummy for topology
		 */
		return null;
	}

	@Override
	public void setHighestPriority(float pHighestPriority) {
		/*
		 * not needed, this is just a dummy for topology
		 */
	}

	@Override
	public void sendClusterBroadcast(Serializable pData, LinkedList<CoordinatorCEPDemultiplexed> pAlreadyInformed) {
		/*
		 * not needed, this is just a dummy for topology
		 */
	}
	
	public String toString()
	{
		if(mHRMID != null && HierarchicalConfig.Routing.ADDR_DISTRIBUTOR_PRINTS_HRMID) {
			return mHRMID.toString();
		} else {
			return getClusterDescription() + ":HOPS(" + getCoordinator().getClusterDistance(this) + ")" + (mInterASCluster ? "InterAS" : "");
		}
	}

	@Override
	public Namespace getNamespace() {
		return new Namespace("attachedcluster");
	}

	@Override
	public int getSerialisedSize() {
		return 0;
	}

	@Override
	public boolean equals(Object pObj)
	{
		if(pObj instanceof Cluster) {
			Cluster tCluster = (Cluster) pObj;
			if(tCluster.getClusterID().equals(getClusterID()) &&
					tCluster.getToken() == getToken() &&
					tCluster.getLevel() == getLevel()) {
				return true;
			} else if(tCluster.getClusterID().equals(getClusterID()) && tCluster.getLevel() == getLevel()) {
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
		return mInterASCluster;
	}

	@Override
	public void setInterASCluster()
	{
		mInterASCluster = true;
	}

	@Override
	public LinkedList<CoordinatorCEPDemultiplexed> getLaggards()
	{
		return null;
	}

	@Override
	public void addLaggard(CoordinatorCEPDemultiplexed pCEP)
	{
		
	}

	@Override
	public CoordinatorCEPDemultiplexed getNegotiatorCEP()
	{
		return mNegotiator;
	}

	@Override
	public void setNegotiatorCEP(CoordinatorCEPDemultiplexed pCEP)
	{
		getLogger().log(this.getClusterDescription(), "Setting " + pCEP + " as  negotiating CEP");
		if(!mNegotiators.contains(pCEP)) mNegotiators.add(pCEP);
		mNegotiator = pCEP;
	}

	public CoordinatorCEPDemultiplexed getAnnouncedCEP(Cluster pCluster)
	{
		int tClosestCluster = Integer.MAX_VALUE;
		CoordinatorCEPDemultiplexed tClosest = null;
		for(CoordinatorCEPDemultiplexed tCEP : mAnnouncedCEPs) {
			Cluster tRemoteCluster = tCEP.getRemoteCluster();
			tRemoteCluster = getCoordinator().getCluster(tRemoteCluster) != null ? getCoordinator().getCluster(tRemoteCluster) : tRemoteCluster;
			if(pCluster.getLevel() == tRemoteCluster.getLevel()) {
				List<NodeConnection> tConnection = getCoordinator().getClusterMap().getRoute(pCluster, tRemoteCluster);
				int tDistance = 0;
				if(tConnection != null) {
					tDistance = tConnection.size();
				}
				if(tDistance < tClosestCluster) {
					tClosestCluster = tDistance;
					tClosest = tCEP;
				}
			}
		}
		if(tClosest == null) {
			getLogger().err(this, "Would return no announced CEP");
			tClosest = mAnnouncedCEPs.getFirst();
		}
		return tClosest;
	}
	
	public LinkedList<CoordinatorCEPDemultiplexed> getAnnouncedCEPs()
	{
		return mAnnouncedCEPs;
	}
	
	public void setAnnouncer(Name pAnnouncer)
	{
		Logging.log(this, "Announcer is " + pAnnouncer);
		mAnnouncer = pAnnouncer;
	}
	
	public int getClusterHopsOnAnnouncer(CoordinatorCEP pCEP)
	{
		return mClustersOnOppositeCounter.get(pCEP);
	}
	
	public void setClusterHopsOnOpposite(int pClustersOnOpposite, CoordinatorCEPDemultiplexed pCEP)
	{
		mClustersOnOppositeCounter.put(pCEP, pClustersOnOpposite);
		mClusterHopsOnOpposite = pClustersOnOpposite;
	}
	
	public void setSourceIntermediate(IntermediateCluster pIntermediate)
	{
		mSourceIntermediateCluster = pIntermediate;
	}
	
	public IntermediateCluster getSourceIntermediateCluster()
	{
		return mSourceIntermediateCluster;
	}
/*
	public void setNegotiatingHost(RoutingServiceAddress pAddress)
	{
		getLogger().log("Negotiating host is" + pAddress);
		mNegotiatingHost = pAddress;
	}
	
	public RoutingServiceAddress getNegotiatingHost()
	{
		return mNegotiatingHost;
	}
*/
	@Override
	public void handleTopologyEnvelope(TopologyEnvelope pEnvelope)
	{
		
	}

	@Override
	public void interpretAnnouncement(BullyAnnounce pAnnounce, CoordinatorCEPDemultiplexed pCEP)
	{
		
	}

	@Override
	public CoordinatorCEPMultiplexer getMultiplexer()
	{
		return null;
	}

	
	@Override
	public Object getDecorationParameter()
	{
		return IElementDecorator.Color.GREEN;
	}

	@Override
	public void setDecorationParameter(Object pDecoration)
	{
		
	}

	@Override
	public Object getDecorationValue()
	{
		return Float.valueOf(0.6f);
	}

	@Override
	public void setDecorationValue(Object pValue)
	{
		
	}

	public Logger getLogger()
	{
		return getCoordinator().getLogger();
	}
	
	public int hashCode()
	{
		return mClusterID.intValue() * 1;
	}	
}
