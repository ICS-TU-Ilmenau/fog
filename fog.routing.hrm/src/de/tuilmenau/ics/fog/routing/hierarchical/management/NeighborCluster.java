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
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborClusterAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.routing.RoutingService;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.IElementDecorator;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is used when a layer 0 neighbor cluster is detected. 
 * It includes all needed data about the neighbor cluster. //TV
 */
public class NeighborCluster implements ICluster, IElementDecorator
{
	private static final long serialVersionUID = -8746079632866375924L;
	private int mToken;
	private HierarchyLevel mHierarchyLevel = null;
	private BullyPriority mPriority;
	private BullyPriority mCoordinatorPriority;
	private Name mCoordName;
	private HRMName mCoordAddress;
	private Long mClusterID;
	private HRMController mHRMController;
	private HRMID mHRMID;
	private Name mAnnouncer;
	private LinkedList<ComChannel> mAnnouncedCEPs;
	private Cluster mSourceIntermediateCluster = null;
	
	/**
	 * 
	 * @param pClusterID identifier of the cluster
	 * @param pCoordName name of the coordinator
	 * @param pAddress routing service address
	 * @param pToken as the token this cluster got
	 * @param pLevel is the level this cluster is related to
	 * @param pResponsibleCoordinator as the coordinator this cluster works on (local)
	 */
	public NeighborCluster(Long pClusterID, Name pCoordName, HRMName pAddress, int pToken, HierarchyLevel pLevel, HRMController pHRMController)
	{	
		mCoordAddress = pAddress;
		mClusterID = pClusterID;
		mHRMController = pHRMController;
		mCoordName = pCoordName;
		mToken = pToken;
		mHierarchyLevel = pLevel;
		mAnnouncer = mHRMController.getNodeName();
		setCoordinatorName(pCoordName);
	}
	
	public void addAnnouncedCEP(ComChannel pCEP)
	{
		if(mAnnouncedCEPs == null) {
			mAnnouncedCEPs = new LinkedList<ComChannel>();
		}
		Logging.log(getClusterDescription(), "Adding announcer " + pCEP);
		mAnnouncedCEPs.add(pCEP);
	}
	
	public int getClusterDistanceToTarget()
	{
		return mHRMController.getClusterDistance(this);
	}

	public BullyPriority getHighestPriority()
	{
		return getPriority();
	}

	public void handleNeighborAnnouncement(NeighborClusterAnnounce pAnnounce, ComChannel pCEP)
	{
		if(pAnnounce.getRoutingVectors() != null) {
			for(RoutingServiceLinkVector tVector : pAnnounce.getRoutingVectors()) {
				mHRMController.getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
			}
		}
		ICluster tCluster = mHRMController.getCluster(new ClusterName(pAnnounce.getToken(), pAnnounce.getClusterID(), pAnnounce.getLevel()));
		if(tCluster == null)
		{
			tCluster = new NeighborCluster(pAnnounce.getClusterID(), pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress(), pAnnounce.getToken(), mHierarchyLevel,	mHRMController);
			mHRMController.setSourceIntermediateCluster(tCluster, mHRMController.getSourceIntermediate(this));
			((NeighborCluster)tCluster).addAnnouncedCEP(pCEP);
			tCluster.setPriority(pAnnounce.getCoordinatorsPriority());
			tCluster.setToken(pAnnounce.getToken());
		} else {
			Logging.log(this, "Cluster announced by " + pAnnounce + " is an intermediate neighbor ");
		}
		//((AttachedCluster)tCluster).setNegotiatingHost(pAnnounce.getAnnouncersAddress());

		/*
		 * function checks whether neighbor relation was established earlier
		 */
		registerNeighbor(tCluster);

		if(pAnnounce.getCoordinatorName() != null) {
			RoutingService tRS = (RoutingService)mHRMController.getNode().getRoutingService();
			if(! tRS.isKnown(pAnnounce.getCoordinatorName())) {
				try {
					mHRMController.getHRS().mapFoGNameToL2Address(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
				} catch (RemoteException tExc) {
					Logging.err(this, "Unable to register " + pAnnounce.getCoordinatorName() + " at name mapping service", tExc);
				}
			}
		}
	}

	public ComChannel getSuperiorCoordinatorCEP()
	{
		return null;
	}

	public synchronized void setSuperiorCoordinator(ComChannel pCoordinatorComChannel, Name pCoordinatorName, int pCoordToken, L2Address pCoordinatorL2Address)
	{
		mCoordAddress = pCoordinatorL2Address;
		mCoordName = pCoordinatorName;
	}

	public void registerNeighbor(ICluster pNeighbor)
	{
		mHRMController.registerLinkARG(this, pNeighbor, new AbstractRoutingGraphLink(AbstractRoutingGraphLink.LinkType.LOGICAL_LINK));
	}

	public void setHRMID(Object pCaller, HRMID pHRMID)
	{
		Logging.log(this, "Setting HRM ID: \"" + pHRMID + "\", triggered from " + pCaller);
		mHRMID = pHRMID;
	}

	@Override
	public void setPriority(BullyPriority pPriority)
	{
		mPriority = pPriority;
	}

	@Override
	public Long getClusterID()
	{
		return mClusterID;
	}

	@Override
	public HierarchyLevel getHierarchyLevel() {
		return mHierarchyLevel;
	}

	@Override
	public Name getCoordinatorName()
	{
		return mCoordName;
	}

	@Override
	public BullyPriority getPriority()
	{
		return mPriority;
	}

	@Override
	public String getClusterDescription()
	{
		return getClass().getSimpleName() + "(" + mAnnouncer + "->" + mCoordName + ")"+"PR" + "(" + mPriority + ")" + "ID(" + mClusterID + ")TK(" + mToken + ")@" + mHierarchyLevel;
	}

	@Override
	public void setCoordinatorName(Name pCoordName)
	{
		mCoordName = pCoordName;
	}

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
	public LinkedList<ICluster> getNeighbors() {
		LinkedList<ICluster> tCluster = new LinkedList<ICluster>();
		for(AbstractRoutingGraphNode tNode : mHRMController.getNeighborsARG(this)) {
			if(tNode instanceof ICluster) {
				tCluster.add((ICluster) tNode);
			}
		}
		return tCluster;
	}

	@Override
	public HRMID getHRMID() {
		return mHRMID;
	}

	@Override
	public void setHighestPriority(BullyPriority pHighestPriority) {
		/*
		 * not needed, this is just a dummy for topology
		 */
	}

	@SuppressWarnings("unused")
	public String toString()
	{
		if(mHRMID != null && HRMConfig.Debugging.PRINT_HRMIDS_AS_CLUSTER_IDS) {
			return mHRMID.toString();
		} else {
			return getClusterDescription() + ":HOPS(" + mHRMController.getClusterDistance(this) + ")";
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
		if(pObj instanceof ICluster) {
			ICluster tCluster = (ICluster) pObj;
			if(tCluster.getClusterID().equals(getClusterID()) &&
					tCluster.getToken() == getToken() &&
					tCluster.getHierarchyLevel() == getHierarchyLevel()) {
				return true;
			} else if(tCluster.getClusterID().equals(getClusterID()) && tCluster.getHierarchyLevel() == getHierarchyLevel()) {
				return false;
			} else if (tCluster.getClusterID().equals(getClusterID())) {
				return false;
			}
		}
		return false;
	}	

	public ComChannel getAnnouncedCEP(ICluster pCluster)
	{
		int tClosestCluster = Integer.MAX_VALUE;
		ComChannel tClosest = null;
		for(ComChannel tCEP : mAnnouncedCEPs) {
			ClusterName tClusterName = tCEP.getRemoteClusterName();
			ICluster tRemoteCluster = mHRMController.getCluster(tClusterName) != null ? mHRMController.getCluster(tClusterName) : tClusterName;
			if(pCluster.getHierarchyLevel() == tRemoteCluster.getHierarchyLevel()) {
				List<AbstractRoutingGraphLink> tConnection = mHRMController.getRouteARG(pCluster, tRemoteCluster);
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
			Logging.err(this, "Would return no announced CEP");
			tClosest = mAnnouncedCEPs.getFirst();
		}
		return tClosest;
	}
	
	public LinkedList<ComChannel> getAnnouncedCEPs()
	{
		return mAnnouncedCEPs;
	}
	
	public void setAnnouncer(Name pAnnouncer)
	{
		Logging.log(this, "Announcer is " + pAnnouncer);
		mAnnouncer = pAnnouncer;
	}
	
	public void setSourceIntermediate(Cluster pIntermediate)
	{
		mSourceIntermediateCluster = pIntermediate;
	}
	
	public Cluster getSourceIntermediateCluster()
	{
		return mSourceIntermediateCluster;
	}

	@Override
	public ComChannelMuxer getMultiplexer()
	{
		return null;
	}

	
	@Override
	public Object getDecorationParameter()
	{
		return null;
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

	public int hashCode()
	{
		return mClusterID.intValue() * 1;
	}
}
