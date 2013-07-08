/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.clustering;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.facade.properties.PropertyException;
import de.tuilmenau.ics.fog.packets.election.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.election.BullyPriorityUpdate;
import de.tuilmenau.ics.fog.packets.hierarchical.NeighborClusterAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.RouteRequest;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyData;
import de.tuilmenau.ics.fog.packets.hierarchical.TopologyData.FIBEntry;
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.CoordinatorCEPDemultiplexed;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.CoordinatorCEPMultiplexer;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.ElectionProcess;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMSignature;
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
 * there also exists the "neighbor cluster". Such clusters are representatives of clusters that are not
 * directly connected to a physical node. There is always one intermediate cluster between an attached cluster
 * and a physical node. Only on an intermediate cluster may be managed by a ClusterManager.
 * 
 */
public class Cluster implements ICluster, IElementDecorator
{
	/**
	 * This is the GUI specific cluster counter, which allows for globally unique cluster IDs.
	 * It's only used within the GUI. 	
	 */
	private static int sGUIClusterID = 0;

	private CoordinatorCEPDemultiplexed mCoordinator;
	private Long mClusterID;
	private float mHighestPriority;
	private HRMID mHRMID = null;
	protected int mLevel;
	protected float mPriority;
	protected float mCoordinatorPriority;
	protected Name mCoordName;
	protected Name mCoordAddress;
	protected HRMController mCoordinatorInstance;
	protected LinkedList<CoordinatorCEPDemultiplexed> mCEPs;
	protected LinkedList<NeighborClusterAnnounce> mReceivedAnnounces = null;
	protected LinkedList<NeighborClusterAnnounce> mSentAnnounces = null;
	protected HRMSignature mCoordSignature;
	protected Route mRouteToCoordinator;
	protected boolean mInterASCluster = false;
	private int mToken;
	private int mAnnoucementCounter = 0;
	private LinkedList<CoordinatorCEPDemultiplexed> mLaggards;
	private static final long serialVersionUID = -2087553402508167474L;
	protected CoordinatorCEPDemultiplexed mNegotiator = null;
	private LinkedList<CoordinatorCEPDemultiplexed> mNegotiators= new LinkedList<CoordinatorCEPDemultiplexed>();
	private TopologyData mEnvelope = null;
	private CoordinatorCEPDemultiplexed mAnnouncer = null;
	private LinkedList<CoordinatorCEPDemultiplexed> mOldParticipatingCEPs;
	private Coordinator mClusterManager = null;
	private CoordinatorCEPMultiplexer mMux = null;
	
	/**
	 * This is the GUI specific cluster ID. It is used to allow for an easier debugging.
	 */
	private int mGUIClusterID = sGUIClusterID++;
	
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
	public Cluster(Long pClusterID, int pLevel, HRMController pCoordinatorInstance, Logger pLogger)
	{
		mClusterID = pClusterID;
		mLevel = pLevel;
		mCEPs = new LinkedList<CoordinatorCEPDemultiplexed>();
		mReceivedAnnounces = new LinkedList<NeighborClusterAnnounce>();
		mSentAnnounces = new LinkedList<NeighborClusterAnnounce>();
		mCoordinatorInstance = pCoordinatorInstance;
		mPriority = (float) getHRMController().getPhysicalNode().getParameter().get("BULLY_PRIORITY_LEVEL_" + getLevel(), HRMConfig.Election.DEFAULT_BULLY_PRIORITY);
		getHRMController().getLogger().log(this, "Created Cluster " + mClusterID + " on level " + mLevel + " with priority " + mPriority);
		mLevel = pLevel;
		for(ICluster tCluster : getHRMController().getClusters())
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
		setCoordinatorCEP(pCEP, pAnnounce.getCoordSignature(), pAnnounce.getSenderName(), pCEP.getPeerName());
		getHRMController().addApprovedSignature(pAnnounce.getCoordSignature());
		getHRMController().setClusterWithCoordinator(getLevel(), this);
	}
	
	public void setCoordinatorCEP(CoordinatorCEPDemultiplexed pCoord, HRMSignature pCoordSignature, Name pCoordName, HRMName pAddress)
	{
		getHRMController().getLogger().log(this, "announcement number " + (++mAnnoucementCounter) + ": Setting Coordinator " + pCoord + " with signature " + pCoordSignature + " with routing address " + pAddress + " and priority ");
		getHRMController().getLogger().log(this, "previous coordinator was " + mCoordinator + " with name " + mCoordName);
		mCoordinator = pCoord;
		mCoordSignature = pCoordSignature;
		mCoordName = pCoordName;
		if(mCoordinator == null) {
			synchronized(this) {
				mCoordAddress = getHRMController().getPhysicalNode().getRoutingService().getNameFor(getHRMController().getPhysicalNode().getCentralFN());
				notifyAll();
			}
			setCoordinatorPriority(getPriority());
			getHRMController().getPhysicalNode().setDecorationParameter("L"+ (mLevel+1));
			getHRMController().getPhysicalNode().setDecorationValue("(" + pCoordSignature + ")");
		} else {
			synchronized(this) {
				mCoordAddress = pAddress;
				notifyAll();
			}
			getHRMController().getPhysicalNode().setDecorationValue("(" + pCoordSignature + ")");
			setCoordinatorPriority(pCoord.getPeerPriority());
			try {
				getHRMController().getHRS().registerNode(pCoordName, pAddress);
			} catch (RemoteException tExc) {
				getHRMController().getLogger().err(this, "Unable to register " + pCoordName, tExc);
			}
			
			if(pCoord.getRouteToPeer() != null && !pCoord.getRouteToPeer().isEmpty()) {
				if(pAddress instanceof L2Address) {
					getHRMController().getHRS().registerNode((L2Address) pAddress, false);
				}
				
				getHRMController().getHRS().registerRoute(pCoord.getSourceName(), pCoord.getPeerName(), pCoord.getRouteToPeer());
			}
			
			/*getCoordinator().getReferenceNode().setDecorationParameter(null);*/
		}
		getHRMController().getLogger().log(this, "This cluster has the following neighbors: " + getNeighbors());
		for(ICluster tCluster : getNeighbors()) {
			if(tCluster instanceof Cluster) {
				getHRMController().getLogger().log(this, "Preparing neighbor zone announcement");
				NeighborClusterAnnounce tAnnounce = new NeighborClusterAnnounce(pCoordName, mLevel, pCoordSignature, pAddress, getToken(), mClusterID);
				tAnnounce.setCoordinatorsPriority(mPriority);
				if(pCoord != null) {
					tAnnounce.addRoutingVector(new RoutingServiceLinkVector(pCoord.getRouteToPeer(), pCoord.getSourceName(), pCoord.getPeerName()));
				}
				mSentAnnounces.add(tAnnounce);
				((Cluster)tCluster).announceNeighborCoord(tAnnounce, pCoord);
			}
		}
		if(mReceivedAnnounces.isEmpty()) {
			getHRMController().getLogger().log(this, "No announces came in while no coordinator was set");
		} else {
			getHRMController().getLogger().log(this, "sending old announces");
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

			for(IVirtualNode tNode : getHRMController().getClusterMap().getNeighbors(this)) {
				if(tNode instanceof ICluster && ((ICluster) tNode).isInterASCluster()) {
//					tIsEdgeRouter = true;
					tInterASClusterIdentifications.add(ClusterDummy.compare(((ICluster)tNode).getClusterID(), ((ICluster)tNode).getToken(), ((ICluster)tNode).getLevel()));
				}
			}
		}
	}
	
	public ICluster addAnnouncedCluster(NeighborClusterAnnounce pAnnounce, CoordinatorCEPDemultiplexed pCEP)
	{
		if(pAnnounce.getRoutingVectors() != null) {
			for(RoutingServiceLinkVector tVector : pAnnounce.getRoutingVectors()) {
				getHRMController().getHRS().registerRoute(tVector.getSource(), tVector.getDestination(), tVector.getPath());
			}
		}
		ICluster tCluster = getHRMController().getCluster(ClusterDummy.compare(pAnnounce.getClusterID(), pAnnounce.getToken(), pAnnounce.getLevel()));
		if(tCluster == null) {
			tCluster = new NeighborCluster(
					pAnnounce.getClusterID(),
					pAnnounce.getCoordinatorName(),
					pAnnounce.getCoordAddress(),
					pAnnounce.getToken(),
					mLevel,
					getHRMController());
			getHRMController().setSourceIntermediateCluster(tCluster, this);
			((NeighborCluster)tCluster).addAnnouncedCEP(pCEP);
			((NeighborCluster)tCluster).setSourceIntermediate(this);
			tCluster.setPriority(pAnnounce.getCoordinatorsPriority());
			tCluster.setToken(pAnnounce.getToken());
			
			if(pAnnounce.isInterASCluster()) {
				tCluster.setInterASCluster();
			}
			
			try {
				getHRMController().getHRS().registerNode(tCluster.getCoordinatorName(), tCluster.getCoordinatorsAddress());
			} catch (RemoteException tExc) {
				Logging.err(this, "Unable to fulfill requirements", tExc);
			}
			
		} else {
			getHRMController().getLogger().log(this, "Cluster announced by " + pAnnounce + " is an intermediate neighbor ");
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
				getHRMController().getHRS().registerNode(pAnnounce.getCoordinatorName(), pAnnounce.getCoordAddress());
			} catch (RemoteException tExc) {
				getHRMController().getLogger().warn(this, "Unable to register " + pAnnounce.getCoordinatorName(), tExc);
			}
		}
		return tCluster;
	}
	
	public void handleAnnouncement(NeighborClusterAnnounce	pAnnounce, CoordinatorCEPDemultiplexed pCEP)
	{
		if(!pAnnounce.getCoordinatorName().equals(getHRMController().getPhysicalNode().getCentralFN().getName())) {
			Logging.log(this, "Received announcement of foreign cluster");
		}
		
		if(getLevel() < 1) {
			if(pCEP != null) {
				if(!pCEP.getSourceName().equals(pCEP.getPeerName()) && pCEP.getRouteToPeer() != null) {
					RoutingServiceLinkVector tLink = new RoutingServiceLinkVector(pCEP.getRouteToPeer().clone(),  pCEP.getSourceName(), pCEP.getPeerName());
					pAnnounce.addRoutingVector(tLink);
					getHRMController().getLogger().log(this, "Added routing vector " + tLink);
				}
				pAnnounce.isForeignAnnouncement();
			}
			if(pCEP != null) {
				pCEP.addAnnouncedCluster(addAnnouncedCluster(pAnnounce, pCEP), getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
			}
		} else {
			if(getHRMController().getClusterWithCoordinatorOnLevel(mLevel) == null) {
				/*
				 * no coordinator set -> find cluster that is neighbor of the predecessor, so routes are correct
				 */
				for(Coordinator tManager : getHRMController().getClusterManagers(mLevel)) {
					if(tManager.getNeighbors().contains(pAnnounce.getNegotiatorIdentification())) {
						tManager.storeAnnouncement(pAnnounce);
					}
				}
			} else {
				/*
				 * coordinator set -> find cluster that is neighbor of the predecessor, so routes are correct
				 */
				for(Coordinator tManager : getHRMController().getClusterManagers(mLevel)) {
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
					getHRMController().getLogger().log(this, "Removing " + this + " as participating CEP from " + this);
					getParticipatingCEPs().remove(this);
				}
				try {
					addNeighborCluster(getHRMController().getCluster(pCEP.handleDiscoveryEntry(pAnnounce.getCoveringClusterEntry())));
				} catch (PropertyException tExc) {
					getHRMController().getLogger().log(this, "Unable to fulfill requirements");
				}
				getHRMController().getLogger().log(this, "new negotiating cluster will be " + getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
				pCEP.addAnnouncedCluster(addAnnouncedCluster(pAnnounce, pCEP), getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
			} else if(pCEP != null) {
				getHRMController().getLogger().log(this, "new negotiating cluster will be " + getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
				pCEP.addAnnouncedCluster(addAnnouncedCluster(pAnnounce, pCEP), getHRMController().getCluster(pAnnounce.getNegotiatorIdentification()));
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
			if(!ElectionManager.getElectionManager().getProcess(mLevel, mClusterID).isStarted() && ! ElectionManager.getElectionManager().getProcess(mLevel, mClusterID).getState().equals(Thread.State.RUNNABLE)) {
				getHRMController().getLogger().log(this, "Election " + ElectionManager.getElectionManager().getProcess(mLevel, mClusterID) + " is running? " + (ElectionManager.getElectionManager().getProcess(mLevel, mClusterID).isAlive()));
				ElectionManager.getElectionManager().getProcess(mLevel, mClusterID).start();
			} else {
				ElectionManager.getElectionManager().getProcess(mLevel, mClusterID).interrupt();
			}
		} catch (IllegalStateException tExc) {
			getHRMController().getLogger().err(this, "Error while trying to start or restart: " + ElectionManager.getElectionManager().getProcess(mLevel, mClusterID).getState(), tExc);
		} catch (IllegalMonitorStateException tExc) {
			getHRMController().getLogger().err(this, "Error while trying to start or restart: " + ElectionManager.getElectionManager().getProcess(mLevel, mClusterID).getState(), tExc);
		} catch (IllegalThreadStateException tExc) {
			getHRMController().getLogger().err(this, "Error while trying to start or restart: " + ElectionManager.getElectionManager().getProcess(mLevel, mClusterID).getState(), tExc);
		}
		
	}
	
	public void addNeighborCluster(ICluster pNeighbor)
	{
		LinkedList<ICluster> tNeighbors = getNeighbors(); 
		if(!tNeighbors.contains(pNeighbor))
		{
			if(pNeighbor instanceof Cluster) {
				NodeConnection tLink = new NodeConnection(NodeConnection.ConnectionType.LOCAL);
				getHRMController().getClusterMap().link(pNeighbor, this, tLink);
			} else {
				NodeConnection tLink = new NodeConnection(NodeConnection.ConnectionType.REMOTE);
				getHRMController().getClusterMap().link(pNeighbor, this, tLink);
			}
			if(pNeighbor instanceof Cluster && !pNeighbor.isInterASCluster()) {
				//TODO
				mPriority *= 10;
				if(!mInterASCluster) {
					getHRMController().getLogger().log(this, "Informing " + getParticipatingCEPs() + " about change in priority and initiating new election");
					sendClusterBroadcast(new BullyPriorityUpdate(getHRMController().getPhysicalNode().getCentralFN().getName(), mPriority), (LinkedList<CoordinatorCEPDemultiplexed>)null);
					getHRMController().getLogger().log(this, "Informed other clients about change of priority - it is now " + mPriority);
				}
			}
		}
	}
	
	public void setRouteToCoordinator(Route pPath)
	{
		getHRMController().getLogger().log(this, "Route to coordinator is now " + pPath);
		mRouteToCoordinator = pPath;
	}
	
	public void announceNeighborCoord(NeighborClusterAnnounce pAnnouncement, CoordinatorCEPDemultiplexed pCEP)
	{
		getHRMController().getLogger().log(this, "Handling " + pAnnouncement);
		if(mCoordName != null)
		{
			if(getHRMController().getPhysicalNode().getCentralFN().getName().equals(mCoordName))
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
		getHRMController().getLogger().log(this, "Setting participating CEPs to " + pCEPs);
		mCEPs = pCEPs;
	}
	
	public void addParticipatingCEP(CoordinatorCEPDemultiplexed pParticipatingCEP)
	{
		if(!mCEPs.contains(pParticipatingCEP)) {
			mCEPs.add(pParticipatingCEP);
			getHRMController().getLogger().info(this, "Added " + pParticipatingCEP + " to participating CEPs");
			if(mCEPs.size() > 1) {
				getHRMController().getLogger().info(this, "Adding second participating CEP " + pParticipatingCEP);
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
	
	public HRMController getHRMController()
	{
		return mCoordinatorInstance;
	}
	
	public void setPriority(float pPriority)
	{
		mPriority = pPriority;
	}
	
	public float getNodePriority()
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
	
	public HRMSignature getCoordinatorSignature()
	{
		return mCoordSignature;
	}
	
	public LinkedList<ICluster> getNeighbors()
	{
		LinkedList<ICluster> tList = new LinkedList<ICluster>();
		for(IVirtualNode tNode : getHRMController().getClusterMap().getNeighbors(this)) {
			if(tNode instanceof ICluster) {
				tList.add((ICluster)tNode);
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
		if(pData instanceof BullyPriorityUpdate)
		{
			getHRMController().getLogger().log(this, "Will send priority update to" + mCEPs);
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
			getHRMController().getLogger().warn(this, "change in cluster CEP number occured, sending message to new peers", tExc);
			sendClusterBroadcast(pData, tInformedCEPs);
		}
	}
	
	public String getClusterDescription()
	{
		return getHRMController().getPhysicalNode() + ":" + mClusterID + "@" + mLevel + "(" + mCoordSignature + ")";
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
	
	public int getGUIClusterID()
	{
		return mGUIClusterID;
	}
	
	public String toString()
	{
		if(mHRMID != null && HRMConfig.Debugging.PRINT_HRMIDS_AS_CLUSTER_IDS) {
			return mHRMID.toString();
		} else {
			return "Cluster " + mGUIClusterID + "@L" + mLevel + " (ID=" + getClusterID() + ", Tok=" + mToken +  ", NodePrio=" + getPriority() + ", Coord.=" +  (getCoordinatorSignature() != null ? getCoordinatorSignature() : "-") + (mInterASCluster ? ":transit" : "") + ")";

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
		if(pObj instanceof Coordinator) {
			return false;
		}
		if(pObj instanceof ICluster) {
			ICluster tCluster = (ICluster) pObj;
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
		return mLaggards;
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
	public void handleTopologyEnvelope(TopologyData pEnvelope)
	{
		if(pEnvelope.getApprovedSignatures() != null) {
			for(HRMSignature tSignature : pEnvelope.getApprovedSignatures()) {
				getHRMController().addApprovedSignature(tSignature);
			}
		}
		mEnvelope = pEnvelope;
		HierarchicalNameMappingService<HRMID> tNMS = null;
		try {
			tNMS = (HierarchicalNameMappingService) HierarchicalNameMappingService.getGlobalNameMappingService();
		} catch (RuntimeException tExc) {
			HierarchicalNameMappingService.createGlobalNameMappingService(getHRMController().getPhysicalNode().getAS().getSimulation());
		}
		tNMS.registerName(getHRMController().getPhysicalNode().getCentralFN().getName(), pEnvelope.getHRMID(), NamingLevel.NAMES);
		String tString = new String();
		for(NameMappingEntry<HRMID> tEntry : tNMS.getAddresses(getHRMController().getPhysicalNode().getCentralFN().getName())) {
			tString += tEntry + " ";
		}
		getHRMController().getLogger().log(this, "Currently registered names: " + tString);

		setHRMID(pEnvelope.getHRMID());
		
		getHRMController().getPhysicalNode().setDecorationValue(getHRMController().getPhysicalNode().getDecorationValue() + " " + pEnvelope.getHRMID().toString() + ",");
		getHRMController().addIdentification(pEnvelope.getHRMID());
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
					getHRMController().getHRS().addRoutingEntry(tEntry.getDestination(), tEntry);
				}
			}
		}
	}
	
	@Override
	public TopologyData getTopologyData()
	{
		return mEnvelope;
	}
	
	/**
	 * 
	 * @return Return the cluster manager that is associated to this intermediate cluster. However it is only initialized if this
	 * node really had the highest priority.
	 */
	public Coordinator getClusterManager()
	{
		return mClusterManager;
	}
	
	public void setClusterManager(Coordinator pManager)
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
	public CoordinatorCEPDemultiplexed getCEPOfCluster(ICluster pCluster)
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
