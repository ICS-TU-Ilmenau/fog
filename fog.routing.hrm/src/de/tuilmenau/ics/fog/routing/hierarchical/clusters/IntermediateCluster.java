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
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.packets.hierarchical.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborZoneAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.PriorityUpdate;
import de.tuilmenau.ics.fog.packets.hierarchical.RouteRequest;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyEnvelope;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyEnvelope.FIBEntry;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.CoordinatorCEPDemultiplexed;
import de.tuilmenau.ics.fog.routing.hierarchical.CoordinatorCEPMultiplexer;
import de.tuilmenau.ics.fog.routing.hierarchical.ElectionProcess;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HierarchicalSignature;
import de.tuilmenau.ics.fog.routing.hierarchical.RoutingServiceLinkVector;
import de.tuilmenau.ics.fog.routing.hierarchical.ElectionProcess.ElectionManager;
import de.tuilmenau.ics.fog.routing.naming.HierarchicalNameMappingService;
import de.tuilmenau.ics.fog.routing.naming.NameMappingEntry;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.IElementDecorator;
import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;

/**
 * This class represents clusters that are directly attached to a physical node. In contrast to this class
 * there also exists the attached cluster. Attached clusters are representatives of clusters that are not
 * directly connected to a physical node. There is always one intermediate cluster between an attached cluster
 * and a physical node. Only on an intermediate cluster may be managed by a ClusterManager.
 * 
 */
public class IntermediateCluster implements Cluster, IElementDecorator
{
	private CoordinatorCEPDemultiplexed mCoordinator;
	private Long mClusterID;
	private float mHighestPriority;
	private HRMID mHRMID = null;
	protected int mLevel;
	protected float mPriority;
	protected float mCoordinatorPriority;
	protected Name mCoordName;
	protected Name mCoordAddress;
	protected Coordinator mCoordinatorInstance;
	protected LinkedList<CoordinatorCEPDemultiplexed> mCEPs;
	protected LinkedList<NeighborZoneAnnounce> mReceivedAnnounces = null;
	protected LinkedList<NeighborZoneAnnounce> mSentAnnounces = null;
	protected HierarchicalSignature mCoordSignature;
	protected Route mRouteToCoordinator;
	protected boolean mInterASCluster = false;
	private int mToken;
	private int mAnnoucementCounter = 0;
	private LinkedList<CoordinatorCEPDemultiplexed> mLaggards;
	private static final long serialVersionUID = -2087553402508167474L;
	protected CoordinatorCEPDemultiplexed mNegotiator = null;
	private LinkedList<CoordinatorCEPDemultiplexed> mNegotiators= new LinkedList<CoordinatorCEPDemultiplexed>();
	private TopologyEnvelope mEnvelope = null;
	private CoordinatorCEPDemultiplexed mAnnouncer = null;
	private LinkedList<CoordinatorCEPDemultiplexed> mOldParticipatingCEPs;
	private ClusterManager mClusterManager = null;
	private CoordinatorCEPMultiplexer mMux = null;
	
	/**
	 * This is the constructor of an intermediate cluster. At first such a cluster is identified by its cluster
	 * ID and the hierarchical level. Later on - once a coordinator is found, it is additionally identified
	 * by a token the coordinator sends to all participants. In contrast to the cluster token the identity is used
	 * to filter potential participants that may be used for the election of a coordinator.
	 * 
	 * @param pClusterID
	 * @param pLevel
	 * @param pCoordinatorInstance
	 * @param pLogger
	 */
	public IntermediateCluster(Long pClusterID, int pLevel, Coordinator pCoordinatorInstance, Logger pLogger)
	{
		mClusterID = pClusterID;
		mLevel = pLevel;
		mCEPs = new LinkedList<CoordinatorCEPDemultiplexed>();
		mReceivedAnnounces = new LinkedList<NeighborZoneAnnounce>();
		mSentAnnounces = new LinkedList<NeighborZoneAnnounce>();
		mCoordinatorInstance = pCoordinatorInstance;
		mPriority = (float) getCoordinator().getPhysicalNode().getParameter().get("BULLY_PRIORITY_LEVEL_" + getLevel(), 3.14159);
		getCoordinator().getLogger().log(this, "Created Cluster " + mClusterID + " on level " + mLevel + " with priority " + mPriority);
		mLevel = pLevel;
		for(Cluster tCluster : getCoordinator().getClusters())
		{
			if(tCluster.getLevel() == pLevel && !(tCluster == this))
			{
				tCluster.addNeighborCluster(this);
				mPriority *=10;
			}
		}
		ElectionProcess tProcess = ElectionManager.getElectionManager().addElection(mLevel, mClusterID, new ElectionProcess(mLevel));
		tProcess.addElectingCluster(this);
		mMux = new CoordinatorCEPMultiplexer(mCoordinatorInstance);
		mMux.setCluster(this);
	}
	
	public void setAnnouncedCEP(CoordinatorCEPDemultiplexed pCEP)
	{
		mAnnouncer = pCEP;
	}
	
	public void interpretAnnouncement(BullyAnnounce pAnnounce, CoordinatorCEPDemultiplexed pCEP)
	{
		setToken(pAnnounce.getToken());
		setCoordinatorCEP(pCEP, pAnnounce.getCoordSignature(), pAnnounce.getCoord(), pCEP.getPeerName());
		getCoordinator().addApprovedSignature(pAnnounce.getCoordSignature());
		getCoordinator().setClusterWithCoordinator(getLevel(), this);
	}
	
	public void setCoordinatorCEP(CoordinatorCEPDemultiplexed pCoord, HierarchicalSignature pCoordSignature, Name pCoordName, HRMName pAddress)
	{
		getCoordinator().getLogger().log(this, "announcement number " + (++this.mAnnoucementCounter) + ": Setting Coordinator " + pCoord + " with signature " + pCoordSignature + " with routing address " + pAddress + " and priority ");
		getCoordinator().getLogger().log(this, "previous coordinator was " + mCoordinator + " with name " + mCoordName);
		mCoordinator = pCoord;
		mCoordSignature = pCoordSignature;
		mCoordName = pCoordName;
		if(mCoordinator == null) {
			synchronized(this) {
				mCoordAddress = getCoordinator().getPhysicalNode().getRoutingService().getNameFor(getCoordinator().getPhysicalNode().getCentralFN());
				notifyAll();
			}
			setCoordinatorPriority(getPriority());
			getCoordinator().getPhysicalNode().setDecorationParameter("L"+ (mLevel+1));
			getCoordinator().getPhysicalNode().setDecorationValue("(" + pCoordSignature + ")");
		} else {
			synchronized(this) {
				mCoordAddress = pAddress;
				notifyAll();
			}
			getCoordinator().getPhysicalNode().setDecorationValue("(" + pCoordSignature + ")");
			setCoordinatorPriority(pCoord.getPeerPriority());
			try {
				getCoordinator().getHRS().registerNode(pCoordName, pAddress);
			} catch (RemoteException tExc) {
				getCoordinator().getLogger().err(this, "Unable to register " + pCoordName, tExc);
			}
			
			if(pCoord.getRouteToPeer() != null && !pCoord.getRouteToPeer().isEmpty()) {
				if(pAddress instanceof L2Address) {
					getCoordinator().getHRS().registerNode((L2Address) pAddress, false);
				}
				
				getCoordinator().getHRS().registerRoute(pCoord.getSourceName(), pCoord.getPeerName(), pCoord.getRouteToPeer());
			}
			
			/*getCoordinator().getReferenceNode().setDecorationParameter(null);*/
		}
		getCoordinator().getLogger().log(this, "This cluster has the following neighbors: " + getNeighbors());
		for(Cluster tCluster : getNeighbors()) {
			if(tCluster instanceof IntermediateCluster) {
				getCoordinator().getLogger().log(this, "Preparing neighbor zone announcement");
				NeighborZoneAnnounce tAnnounce = new NeighborZoneAnnounce(pCoordName, mLevel, pCoordSignature, pAddress, getToken(), mClusterID);
				tAnnounce.setCoordinatorsPriority(mPriority);
				if(pCoord != null) {
					tAnnounce.addRoutingVector(new RoutingServiceLinkVector(pCoord.getRouteToPeer(), pCoord.getSourceName(), pCoord.getPeerName()));
				}
				mSentAnnounces.add(tAnnounce);
				((IntermediateCluster)tCluster).announceNeighborCoord(tAnnounce, pCoord);
			}
		}
		if(mReceivedAnnounces.isEmpty()) {
			getCoordinator().getLogger().log(this, "No announces came in while no coordinator was set");
		} else {
			getCoordinator().getLogger().log(this, "sending old announces");
			while(!mReceivedAnnounces.isEmpty()) {
				if(mCoordinator != null)
				{
					// OK, we have to notify the other node via socket communication, so this cluster has to be at least one hop away
					mCoordinator.write(mReceivedAnnounces.removeFirst());
				} else {
					/*
					 * in this case this announcement came from a neighbor intermediate cluster
					 */
					handleAnnouncement(mReceivedAnnounces.removeFirst(), pCoord);
				}
			}
		}
		if(pCoord == null) {
//			boolean tIsEdgeRouter = false;
			LinkedList<ClusterDummy> tInterASClusterIdentifications = new LinkedList<ClusterDummy>();

			for(VirtualNode tNode : getCoordinator().getClusterMap().getNeighbors(this)) {
				if(tNode instanceof Cluster && ((Cluster) tNode).isInterASCluster()) {
//					tIsEdgeRouter = true;
					tInterASClusterIdentifications.add(ClusterDummy.compare(((Cluster)tNode).getClusterID(), ((Cluster)tNode).getToken(), ((Cluster)tNode).getLevel()));
				}
			}
		}
	}
	
	public Cluster addAnnouncedCluster(NeighborZoneAnnounce pAnnounce, CoordinatorCEPDemultiplexed pCEP)
	{
		if(pAnnounce.getRoutingVectors() != null) {
			for(RoutingServiceLinkVector tVector : pAnnounce.getRoutingVectors()) {
				getCoordinator().getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
			}
		}
		Cluster tCluster = getCoordinator().getCluster(ClusterDummy.compare(pAnnounce.getClusterID(), pAnnounce.getToken(), pAnnounce.getLevel()));
		if(tCluster == null) {
			tCluster = new NeighborCluster(
					pAnnounce.getClusterID(),
					pAnnounce.getCoordinatorName(),
					pAnnounce.getCoordAddress(),
					pAnnounce.getToken(),
					mLevel,
					getCoordinator());
			getCoordinator().setSourceIntermediateCluster(tCluster, this);
			((NeighborCluster)tCluster).addAnnouncedCEP(pCEP);
			((NeighborCluster)tCluster).setSourceIntermediate(this);
			tCluster.setPriority(pAnnounce.getCoordinatorsPriority());
			tCluster.setToken(pAnnounce.getToken());
			
			if(pAnnounce.isInterASCluster()) {
				tCluster.setInterASCluster();
			}
			
			try {
				getCoordinator().getHRS().registerNode(tCluster.getCoordinatorName(), tCluster.getCoordinatorsAddress());
			} catch (RemoteException tExc) {
				Logging.err(this, "Unable to fulfill requirements", tExc);
			}
			
		} else {
			getCoordinator().getLogger().log(this, "Cluster announced by " + pAnnounce + " is an intermediate neighbor ");
		}
		/*if(tCluster instanceof AttachedCluster) {
			((AttachedCluster)tCluster).setNegotiatingHost(pAnnounce.getAnnouncersAddress());
		}*/
		
		/*
		 * function checks whether neighbor relation was established earlier
		 */
		addNeighborCluster(tCluster);

		if(pAnnounce.getCoordinatorName() != null) {
//			Description tDescription = new Description();
			try {
				getCoordinator().getHRS().registerNode(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
			} catch (RemoteException tExc) {
				getCoordinator().getLogger().warn(this, "Unable to register " + pAnnounce.getCoordinatorName(), tExc);
			}
		}
		return tCluster;
	}
	
	public void handleAnnouncement(NeighborZoneAnnounce	pAnnounce, CoordinatorCEPDemultiplexed pCEP)
	{
		if(!pAnnounce.getCoordinatorName().equals(getCoordinator().getPhysicalNode().getCentralFN().getName())) {
			Logging.log(this, "Received announcement of foreign cluster");
		}
		
		if(getLevel() < 1) {
			if(pCEP != null) {
				if(!pCEP.getSourceName().equals(pCEP.getPeerName()) && pCEP.getRouteToPeer() != null) {
					RoutingServiceLinkVector tLink = new RoutingServiceLinkVector(pCEP.getRouteToPeer().clone(),  pCEP.getSourceName(), pCEP.getPeerName());
					pAnnounce.addRoutingVector(tLink);
					getCoordinator().getLogger().log(this, "Added routing vector " + tLink);
				}
				pAnnounce.isForeignAnnouncement();
			}
			if(pCEP != null) {
				pCEP.addAnnouncedCluster(addAnnouncedCluster(pAnnounce, pCEP), getCoordinator().getCluster(pAnnounce.getNegotiatorIdentification()));
			}
		} else {
			if(getCoordinator().getClusterWithCoordinatorOnLevel(mLevel) == null) {
				/*
				 * no coordinator set -> find cluster that is neighbor of the predecessor, so routes are correct
				 */
				for(ClusterManager tManager : getCoordinator().getClusterManagers(mLevel)) {
					if(tManager.getNeighbors().contains(pAnnounce.getNegotiatorIdentification())) {
						tManager.storeAnnouncement(pAnnounce);
					}
				}
			} else {
				/*
				 * coordinator set -> find cluster that is neighbor of the predecessor, so routes are correct
				 */
				for(ClusterManager tManager : getCoordinator().getClusterManagers(mLevel)) {
					if(tManager.getNeighbors().contains(pAnnounce.getNegotiatorIdentification())) {
						if(tManager.getCoordinatorCEP() != null) {
							tManager.getCoordinatorCEP().write(pAnnounce);
						}
					}
				}
			}
			
			
			if(pAnnounce.getCoveringClusterEntry() != null) {
//				Cluster tForwardingCluster = null;
				
				if(pAnnounce.isRejected()) {
//					Cluster tMultiplex = this;
//					tForwardingCluster = (Cluster) ((Cluster) getCoordinator().getLastUncovered(tMultiplex, pCEP.getRemoteCluster()) == null ? pCEP.getRemoteCluster() : getCoordinator().getLastUncovered(tMultiplex, pCEP.getRemoteCluster())) ;
					//pAnnounce.setAnnouncer( (tForwardingCluster.getCoordinatorsAddress() != null ? tForwardingCluster.getCoordinatorsAddress() : null ));
					getCoordinator().getLogger().log(this, "Removing " + this + " as participating CEP from " + this);
					getParticipatingCEPs().remove(this);
				}
				try {
					this.addNeighborCluster(getCoordinator().getCluster(pCEP.handleDiscoveryEntry(pAnnounce.getCoveringClusterEntry())));
				} catch (PropertyException tExc) {
					getCoordinator().getLogger().log(this, "Unable to fulfill requirements");
				}
				getCoordinator().getLogger().log(this, "new negotiating cluster will be " + getCoordinator().getCluster(pAnnounce.getNegotiatorIdentification()));
				pCEP.addAnnouncedCluster(addAnnouncedCluster(pAnnounce, pCEP), getCoordinator().getCluster(pAnnounce.getNegotiatorIdentification()));
			} else if(pCEP != null) {
				getCoordinator().getLogger().log(this, "new negotiating cluster will be " + getCoordinator().getCluster(pAnnounce.getNegotiatorIdentification()));
				pCEP.addAnnouncedCluster(addAnnouncedCluster(pAnnounce, pCEP), getCoordinator().getCluster(pAnnounce.getNegotiatorIdentification()));
			}
		}
	}
	
	public CoordinatorCEPDemultiplexed getCoordinatorCEP()
	{
		return mCoordinator;
	}
	
	public synchronized void interruptElection()
	{
		/*if(getCoordinator().getReferenceNode().getAS().getSimulation().getElectionProcess(mClusterID) != null)
		{
			getCoordinator().getLogger().log(this, "interrupting election " + getCoordinator().getReferenceNode().getAS().getSimulation().getElectionProcess(mClusterID));
			mClusterBuffer = getCoordinator().getReferenceNode().getAS().getSimulation().getElectionProcess(mClusterID).getParticipatingClusters();
			getCoordinator().getReferenceNode().getAS().getSimulation().getElectionProcess(mClusterID).interrupt();
			getCoordinator().getReferenceNode().getAS().getSimulation().removeElection(mClusterID);
		}*/
	}
	
	public void initiateElection()
	{
		try {
			if(!ElectionManager.getElectionManager().getElectionProcess(mLevel, mClusterID).isStarted() && ! ElectionManager.getElectionManager().getElectionProcess(mLevel, mClusterID).getState().equals(Thread.State.RUNNABLE)) {
				getCoordinator().getLogger().log(this, "Election " + ElectionManager.getElectionManager().getElectionProcess(mLevel, mClusterID) + " is running? " + (ElectionManager.getElectionManager().getElectionProcess(mLevel, mClusterID).isAlive()));
				ElectionManager.getElectionManager().getElectionProcess(mLevel, mClusterID).start();
			} else {
				ElectionManager.getElectionManager().getElectionProcess(mLevel, mClusterID).interrupt();
			}
		} catch (IllegalStateException tExc) {
			getCoordinator().getLogger().err(this, "Error while trying to start or restart: " + ElectionManager.getElectionManager().getElectionProcess(mLevel, mClusterID).getState(), tExc);
		} catch (IllegalMonitorStateException tExc) {
			getCoordinator().getLogger().err(this, "Error while trying to start or restart: " + ElectionManager.getElectionManager().getElectionProcess(mLevel, mClusterID).getState(), tExc);
		} catch (IllegalThreadStateException tExc) {
			getCoordinator().getLogger().err(this, "Error while trying to start or restart: " + ElectionManager.getElectionManager().getElectionProcess(mLevel, mClusterID).getState(), tExc);
		}
		
	}
	
	public void addNeighborCluster(Cluster pNeighbor)
	{
		LinkedList<Cluster> tNeighbors = getNeighbors(); 
		if(!tNeighbors.contains(pNeighbor))
		{
			if(pNeighbor instanceof IntermediateCluster) {
				NodeConnection tLink = new NodeConnection(NodeConnection.ConnectionType.LOCAL);
				getCoordinator().getClusterMap().link(pNeighbor, this, tLink);
			} else {
				NodeConnection tLink = new NodeConnection(NodeConnection.ConnectionType.REMOTE);
				getCoordinator().getClusterMap().link(pNeighbor, this, tLink);
			}
			if(pNeighbor instanceof IntermediateCluster && !pNeighbor.isInterASCluster()) {
				mPriority *= 10;
				if(!this.mInterASCluster) {
					getCoordinator().getLogger().log(this, "Informing " + getParticipatingCEPs() + " about change in priority and initiating new election");
					sendClusterBroadcast(new PriorityUpdate(mPriority), (LinkedList<CoordinatorCEPDemultiplexed>)null);
					getCoordinator().getLogger().log(this, "Informed other clients about change of priority - it is now " + mPriority);
				}
			}
		}
	}
	
	public void setRouteToCoordinator(Route pPath)
	{
		getCoordinator().getLogger().log(this, "Route to coordinator is now " + pPath);
		mRouteToCoordinator = pPath;
	}
	
	public void announceNeighborCoord(NeighborZoneAnnounce pAnnouncement, CoordinatorCEPDemultiplexed pCEP)
	{
		getCoordinator().getLogger().log(this, "Handling " + pAnnouncement);
		if(mCoordName != null)
		{
			if(getCoordinator().getPhysicalNode().getCentralFN().getName().equals(mCoordName))
			{
				handleAnnouncement(pAnnouncement, pCEP);
			} else {
				mCoordinator.write(pAnnouncement);
			}
		} else {
			mReceivedAnnounces.add(pAnnouncement);
		}
	}
	
	/**
	 * @param pHRMID identification of this cluster
	 */
	public void setHRMID(HRMID pHRMID)
	{
		mHRMID = pHRMID;
	}
	
	public float getHighestPriority()
	{
		return mHighestPriority;
	}
	
	public LinkedList<CoordinatorCEPDemultiplexed> getOldParticipatingCEPs()
	{
		return mOldParticipatingCEPs;
	}
	
	public void setParticipatingCEPs(LinkedList<CoordinatorCEPDemultiplexed> pCEPs)
	{
		mOldParticipatingCEPs = mCEPs;
		getCoordinator().getLogger().log(this, "Setting participating CEPs to " + pCEPs);
		mCEPs = pCEPs;
	}
	
	public void addParticipatingCEP(CoordinatorCEPDemultiplexed pParticipatingCEP)
	{
		if(!mCEPs.contains(pParticipatingCEP)) {
			mCEPs.add(pParticipatingCEP);
			getCoordinator().getLogger().info(this, "Added " + pParticipatingCEP + " to participating CEPs");
			if(mCEPs.size() > 1) {
				getCoordinator().getLogger().info(this, "Adding second participating CEP " + pParticipatingCEP);
//				StackTraceElement[] tStackTrace = Thread.currentThread().getStackTrace();
//				for (StackTraceElement tElement : tStackTrace) {
//					getCoordinator().getLogger().log(tElement.toString());
//				}
			}
		}
	}

	public LinkedList<CoordinatorCEPDemultiplexed> getParticipatingCEPs()
	{
		return mCEPs;
	}
	
	public Coordinator getCoordinator()
	{
		return mCoordinatorInstance;
	}
	
	public void setPriority(float pPriority)
	{
		mPriority = pPriority;
	}
	
	public float getCoordinatorPriority()
	{
		return mCoordinatorPriority;
	}
	
	public void setCoordinatorPriority(float pCoordinatorPriority)
	{
		mCoordinatorPriority = pCoordinatorPriority;
	}
	
	public Long getClusterID()
	{
		return mClusterID;
	}
	
	public HierarchicalSignature getCoordinatorSignature()
	{
		return mCoordSignature;
	}
	
	public LinkedList<Cluster> getNeighbors()
	{
		LinkedList<Cluster> tList = new LinkedList<Cluster>();
		for(VirtualNode tNode : getCoordinator().getClusterMap().getNeighbors(this)) {
			if(tNode instanceof Cluster) {
				tList.add((Cluster)tNode);
			}
		}
		return tList;
	}
	
	public int getToken()
	{
		return mToken;
	}
	
	public L2Address getCoordinatorsAddress()
	{
		return (L2Address) mCoordAddress;
	}
	
	public void setCoordinatorName(Name pCoordName)
	{
		mCoordName = pCoordName;
	}
	
	public void sendClusterBroadcast(Serializable pData, LinkedList<CoordinatorCEPDemultiplexed> pAlreadyInformed)
	{
		if(pData instanceof PriorityUpdate)
		{
			getCoordinator().getLogger().log(this, "Will send priority update to" + mCEPs);
		}
		LinkedList<CoordinatorCEPDemultiplexed> tInformedCEPs = null;
		if(pAlreadyInformed != null) {
			tInformedCEPs= pAlreadyInformed;
		} else {
			tInformedCEPs = new LinkedList<CoordinatorCEPDemultiplexed>(); 
		}
		try {
			for(CoordinatorCEPDemultiplexed tCEP : mCEPs)
			{
				if(!tInformedCEPs.contains(tCEP))
				{
					tCEP.write(pData);
					tInformedCEPs.add(tCEP);
				}
			}
		} catch (ConcurrentModificationException tExc) {
			getCoordinator().getLogger().warn(this, "change in cluster CEP number occured, sending message to new peers", tExc);
			sendClusterBroadcast(pData, tInformedCEPs);
		}
	}
	
	public String getClusterDescription()
	{
		return getCoordinator().getPhysicalNode() + ":" + mClusterID + "@" + mLevel + "(" + mCoordSignature + ")";
	}
	
	public float getPriority()
	{
		return mPriority;
	}
	
	public Name getCoordinatorName()
	{
		return mCoordName;
	}
	
	public int getLevel()
	{
		return mLevel;
	}
	
	public String toString()
	{
		if(mHRMID != null && HRMConfig.Routing.ADDR_DISTRIBUTOR_PRINTS_HRMID) {
			return mHRMID.toString();
		} else {
			return this.getClass().getSimpleName() + "@" + getCoordinator().getPhysicalNode() + ":ID(" + getClusterID() + ")TK(" + mToken +  "):PR(" + getPriority() + ")COORD(" +  (getCoordinatorSignature() != null ? "(" + getCoordinatorSignature() + ")" : "") + ")" + ")@" + getLevel() + (mInterASCluster ? ":InterAS" : "");

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
	public HRMID retrieveAddress() {
		return mHRMID;
	}
	
	@Override
	public Name retrieveName() {
		return getCoordinator().getPhysicalNode().getCentralFN().getName();
	}
	
	@Override
	public void setHighestPriority(float pHighestPriority) {
		mHighestPriority = pHighestPriority;
	}
	
	@Override
	public Namespace getNamespace() {
		return new Namespace("cluster");
	}

	@Override
	public int getSerialisedSize() {
		return 0;
	}
	

	@Override
	public boolean equals(Object pObj)
	{
		if(pObj instanceof ClusterManager) {
			return false;
		}
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
		return this.mLaggards;
	}

	@Override
	public void addLaggard(CoordinatorCEPDemultiplexed pCEP)
	{
		if(mLaggards == null) {
			mLaggards = new LinkedList<CoordinatorCEPDemultiplexed>();
			mLaggards.add(pCEP);
		} else {
			mLaggards.add(pCEP);
		}
	}

	@Override
	public CoordinatorCEPDemultiplexed getNegotiatorCEP()
	{
		return mNegotiator;
	}

	@Override
	public void setNegotiatorCEP(CoordinatorCEPDemultiplexed pCEP)
	{
		if(!mNegotiators.contains(pCEP)) mNegotiators.add(pCEP);
		mNegotiator = pCEP;	
	}
	
	public CoordinatorCEPDemultiplexed getAnnouncedCEP()
	{
		return mAnnouncer;
	}

	@Override
	public void handleTopologyEnvelope(TopologyEnvelope pEnvelope)
	{
		if(pEnvelope.getApprovedSignatures() != null) {
			for(HierarchicalSignature tSignature : pEnvelope.getApprovedSignatures()) {
				getCoordinator().addApprovedSignature(tSignature);
			}
		}
		mEnvelope = pEnvelope;
		HierarchicalNameMappingService<HRMID> tNMS = null;
		try {
			tNMS = (HierarchicalNameMappingService) HierarchicalNameMappingService.getGlobalNameMappingService();
		} catch (RuntimeException tExc) {
			HierarchicalNameMappingService.createGlobalNameMappingService(getCoordinator().getPhysicalNode().getAS().getSimulation());
		}
		tNMS.registerName(getCoordinator().getPhysicalNode().getCentralFN().getName(), pEnvelope.getHRMID(), NamingLevel.NAMES);
		String tString = new String();
		for(NameMappingEntry<HRMID> tEntry : tNMS.getAddresses(getCoordinator().getPhysicalNode().getCentralFN().getName())) {
			tString += tEntry + " ";
		}
		getCoordinator().getLogger().log(this, "Currently registered names: " + tString);

		setHRMID(pEnvelope.getHRMID());
		
		getCoordinator().getPhysicalNode().setDecorationValue(getCoordinator().getPhysicalNode().getDecorationValue() + " " + pEnvelope.getHRMID().toString() + ",");
		getCoordinator().addIdentification(pEnvelope.getHRMID());
		if(mEnvelope.getEntries() != null) {
			for(FIBEntry tEntry : mEnvelope.getEntries()) {
				if((tEntry.getDestination() != null && !tEntry.getDestination().equals(new HRMID(0)) ) && tEntry.getNextHop() != null) {
					/*if(!getCoordinator().getHRS().getRoutingTable().containsKey(tEntry.getDestination())) {
						getCoordinator().getHRS().addRoutingEntry(tEntry.getDestination(), tEntry);
					} else {
						if(getCoordinator().getHRS().getFIBEntry(tEntry.getDestination()).isWriteProtected()) {
							getCoordinator().getLogger().log(this, "Not replacing " + getCoordinator().getHRS().getFIBEntry(tEntry.getDestination()) + " with " + tEntry);
						} else {
							getCoordinator().getHRS().getRoutingTable().remove(tEntry.getDestination());
							getCoordinator().getHRS().addRoutingEntry(tEntry.getDestination(), tEntry);
						}
					}*/
					getCoordinator().getHRS().addRoutingEntry(tEntry.getDestination(), tEntry);
				}
			}
		}
	}
	
	public TopologyEnvelope getTopologyEnvelope()
	{
		return mEnvelope;
	}
	
	/**
	 * 
	 * @return Return the cluster manager that is associated to this intermediate cluster. However it is only initialized if this
	 * node really had the highest priority.
	 */
	public ClusterManager getClusterManager()
	{
		return mClusterManager;
	}
	
	public void setClusterManager(ClusterManager pManager)
	{
		mClusterManager = pManager;
	}
	
	public void handleRouteRequest(RouteRequest pRequest)
	{
		/*
		 * moved entirely to cluster manager: switch is implemented in CoordinatorCEPDemultiplexed
		 */
	}

	@Override
	public synchronized CoordinatorCEPMultiplexer getMultiplexer()
	{
		return mMux;
	}

	/**
	 * This method is specific for the handling of RouteRequests.
	 * 
	 * @param pCluster
	 * @return
	 */
	public CoordinatorCEPDemultiplexed getCEPOfCluster(Cluster pCluster)
	{
		for(CoordinatorCEPDemultiplexed tCEP : getParticipatingCEPs()) {
			if(tCEP.getRemoteCluster().equals(pCluster)) {
				return tCEP;
			}
		}
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
		return Float.valueOf(0.8f);
	}

	public int hashCode()
	{
		return mClusterID.intValue() * 1;
	}
	
	@Override
	public void setDecorationValue(Object tLabal)
	{
		
	}
}
