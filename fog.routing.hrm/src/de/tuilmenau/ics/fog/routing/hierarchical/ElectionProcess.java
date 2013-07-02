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

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
//import java.util.List;
import java.util.Random;

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.exceptions.AuthenticationException;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.packets.hierarchical.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.BullyElect;
import de.tuilmenau.ics.fog.packets.hierarchical.RequestCoordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.NeighborCluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.ICluster;
//import de.tuilmenau.ics.fog.routing.hierarchical.clusters.VirtualNode;
import de.tuilmenau.ics.fog.routing.hierarchical.clusters.IntermediateCluster;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

public class ElectionProcess extends Thread
{
	private static final int FIRST_ELECTING_CLUSTER = 0; //TV

	private Boolean mPleaseInterrupt = false;
	//private long mTimeStamp = System.currentTimeMillis();
	private long TIMEOUT_FOR_PEERS = 5000;
	private long WAIT_BEFORE_ADDRESS_DISTRIBUTION = 5000;
	private long TIMEOUT_FOR_ANNOUNCEMENT=5000;
	
	private Coordinator mClusterManager=null;
	private LinkedList<IntermediateCluster> mElectingClusters = new LinkedList<IntermediateCluster>();
	private boolean mInProgress = false;
	private HRMController mHRMController = null;
	private int mLevel = 0;	
	private boolean mWillInitiateManager = false;
	private boolean mLostElection = false;
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() /*+ "(TS:" + mTimeStamp + ")"*/ + (mElectingClusters.isEmpty() ? "" : "@" + mElectingClusters.get(FIRST_ELECTING_CLUSTER).getClusterID()) + "@" + mLevel;
	}
	
	public void interruptElection()
	{
		Logging.log(this, "Interruped: will notify in case an election was running");
		synchronized(mPleaseInterrupt) {
			mPleaseInterrupt = true;
			mPleaseInterrupt.notifyAll();
		}
	}
	
	public boolean isElecting(ICluster pCluster)
	{
		Logging.log(this, "does " + (mElectingClusters.contains(pCluster) ? "contain " : "not contain ") + pCluster);
		return (mElectingClusters.contains(pCluster));
	}
	
	//TODO: really needed? otherwise delete it!
	public HRMController getCoordinator()
	{
		return mHRMController;
	}
	
	public ElectionProcess(int pLevel)
	{
		mLevel = pLevel;
	}
	
	//TV: checked
	public void addElectingCluster(IntermediateCluster pCluster)
	{
		boolean tClusterIsAlreadyKnown = false;
		
		// avoid duplicates: iterate over all already known clusters and check if pCluster is already contained
		for(ICluster tCluster : mElectingClusters) {
			if(tCluster.getCoordinator().getPhysicalNode().getName().equals(pCluster.getCoordinator().getPhysicalNode().getName())) {
				tClusterIsAlreadyKnown = true;
			}
		}
		
		// add cluster to the list of known clusters which needs an election
		if(!tClusterIsAlreadyKnown) {
			mElectingClusters.add(pCluster);
		}

		// update the thread name
		updateThreadName();
	}
	
	/**
	 * 
	 */
	//TV: checked
	private void updateThreadName()
	{
		if(!getName().equals(toString())) {
			setName(toString());
		}
	}

	public void sendElections()
	{
		try {
			for(ICluster tCluster : mElectingClusters)
			{
				Logging.log(this, "Sending elections from " + tCluster);
				for(CoordinatorCEPDemultiplexed tCEP : tCluster.getParticipatingCEPs()) {
					if(tCEP.getPeerPriority() == 0 && ! tCEP.isEdgeCEP()/* || tCEP.getPeerPriority() > tCluster.getPriority()*/) {
						tCEP.write(new BullyElect(tCluster.getCoordinator().getPhysicalNode().getCentralFN().getName(), tCluster.getPriority(), tCluster.getLevel()));
					}
				}
			}
		} catch (ConcurrentModificationException tExc) {
			Logging.log(this, "Resending elections");
			sendElections();
		}
	}
	
	public synchronized boolean isStarted()
	{
		return mInProgress;
	}
	
	public void initiateCoordinatorFunctions(IntermediateCluster pCluster)
	{
		Random tRandom = new Random(System.currentTimeMillis());
		HRMController tCoordinator = pCluster.getCoordinator();
		Node tReferenceNode = tCoordinator.getPhysicalNode();
		int tToken = tRandom.nextInt();
		
		pCluster.setToken(tToken);
		pCluster.getCoordinator().getLogger().log(pCluster, "generated token " + tToken);

		if(pCluster.getCoordinator().getIdentity() == null) {
			String tName = tReferenceNode.getName();
			HierarchicalIdentity tIdentity= new HierarchicalIdentity(tName, pCluster.getLevel());
			pCluster.getCoordinator().setIdentity(tIdentity);
		}
		
		pCluster.getCoordinator().getIdentity().setLevel(pCluster.getLevel());
		try {
			BullyAnnounce tAnnounce = new BullyAnnounce(tReferenceNode.getCentralFN().getName(), pCluster.getPriority(), pCluster.getCoordinator().getIdentity().createSignature(tReferenceNode.toString(), null, pCluster.getLevel()), pCluster.getToken());
			for(CoordinatorCEPDemultiplexed tCEP : pCluster.getParticipatingCEPs()) {
				tAnnounce.addCoveredNode(tCEP.getPeerName());
			}
			if(tAnnounce.getCoveredNodes() == null || (tAnnounce.getCoveredNodes() != null && tAnnounce.getCoveredNodes().isEmpty())) {
				pCluster.getCoordinator().getLogger().log(this, "Sending announce that does not cover anyhting");
			}
			pCluster.sendClusterBroadcast(tAnnounce, null);
			
			Name tAddress = tReferenceNode.getRoutingService().getNameFor(tReferenceNode.getCentralFN());; 
			
			pCluster.setCoordinatorCEP(null, pCluster.getCoordinator().getIdentity().createSignature(tReferenceNode.toString(), null, pCluster.getLevel()), tReferenceNode.getCentralFN().getName(), (L2Address)tAddress);
			if(pCluster.getCoordinator().getIdentity() == null) {
				pCluster.getCoordinator().setIdentity(new HierarchicalIdentity(getCoordinator().getPhysicalNode().getName(), pCluster.getLevel()));
			}
			LinkedList<HierarchicalSignature> tSignatures = tCoordinator.getApprovedSignatures();
			tSignatures.add(tCoordinator.getIdentity().createSignature(tReferenceNode.toString(), null, pCluster.getLevel()));
			
			if(mLevel > 0) {
				pCluster.getCoordinator().getLogger().log(pCluster, "has the coordinator and will now announce itself");
				for(ICluster tToAnnounce : pCluster.getNeighbors()) {
//					List<VirtualNode> tNodesBetween = pCluster.getCoordinator().getClusterMap().getIntermediateNodes(pCluster, tToAnnounce);
					/*
					 * OK: Because of the formerly sent 
					 */
					if(tToAnnounce instanceof NeighborCluster) {
						BullyAnnounce tBullyAnnounce = new BullyAnnounce(tReferenceNode.getCentralFN().getName(), pCluster.getPriority(), pCluster.getCoordinator().getIdentity().createSignature(tReferenceNode.toString(), null, pCluster.getLevel()), pCluster.getToken());
						for(CoordinatorCEPDemultiplexed tCEP: pCluster.getParticipatingCEPs()) {
							tBullyAnnounce.addCoveredNode(tCEP.getPeerName());
						}
						for(CoordinatorCEPDemultiplexed tCEP : ((NeighborCluster)tToAnnounce).getAnnouncedCEPs()) {
							tCEP.write(tBullyAnnounce);
						}
					}
				}
			}
		} catch (AuthenticationException tExc) {
			pCluster.getCoordinator().getLogger().err(this, "Unable to create signature for coordinator", tExc);
		}
		
		
		if(!mPleaseInterrupt) {
			/*
			 * synchronized(mPleaseInterrupt) {
			 *
				try {
					mPleaseInterrupt.wait(WAIT_BEFORE_ADDRESS_DISTRIBUTION);
				} catch (InterruptedException tExc) {
					Logging.trace(this, "interrupted before address distribution");
				}
			}
			 */
			mClusterManager = new Coordinator(pCluster, pCluster.getLevel()+1, pCluster.retrieveAddress());
			pCluster.setClusterManager(mClusterManager);
			pCluster.getCoordinator().setSourceIntermediateCluster(mClusterManager, pCluster);
			mClusterManager.setPriority(pCluster.getPriority());
			pCluster.getCoordinator().addCluster(mClusterManager);
			if(pCluster.getLevel() +1 != HRMConfig.Routing.HIERARCHY_LEVEL_AMOUNT) {
				// stepwise hierarchy creation
				Logging.log(this, "Will now wait because hierarchy build up is done stepwise");
				mWillInitiateManager = true;
				if(mLevel == 1) {
					Logging.log(this, "Trigger");
				}
				Logging.log(this, "Reevaluating whether other processes settled");
				ElectionManager.getElectionManager().reevaluate(pCluster.getLevel());
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException tExc) {
						Logging.err(this, "Unable to fulfill stepwise hierarchy preparation", tExc);
					}
				}
				mClusterManager.prepareAboveCluster(pCluster.getLevel() +1);
			} else {
				Logging.log(this, "Beginning address distribution");
				try {
					mClusterManager.setHRMID(new HRMID(0));
					synchronized(mPleaseInterrupt) {
						mPleaseInterrupt.wait(WAIT_BEFORE_ADDRESS_DISTRIBUTION);
					}
					mClusterManager.distributeAddresses();
				} catch (RemoteException tExc) {
					Logging.err(this, "Error when trying to distribute addresses", tExc);
				} catch (RoutingException tExc) {
					Logging.err(this, "Error when trying to distribute addresses", tExc);
				} catch (RequirementsException tExc) {
					Logging.err(this, "Error when trying to distribute addresses", tExc);
				} catch (InterruptedException tExc) {
					Logging.err(this, "Error when trying to distribute addresses", tExc);
				}
			}
		}	
	}
	
	public void checkClustersForHighestPriority(boolean pVerbose)
	{
		float tPriority = 0;
		String tOutput = new String();
		for(ICluster tCluster : mElectingClusters) {
			for(CoordinatorCEPDemultiplexed tCEP : tCluster.getParticipatingCEPs()) {
				tPriority = tCEP.getPeerPriority(); 
				tOutput +=  (tOutput.equals("") ? "" : ", ") +  tPriority;
				if(tPriority >= tCluster.getHighestPriority()) {
					tCluster.setHighestPriority(tPriority);
				} else {
					if(pVerbose) tCluster.getCoordinator().getLogger().log(tCluster, "has lower priority than " + tCEP + " while mine is " + tCluster.getPriority());
				}
			}
		}
		IntermediateCluster tNodessClusterForCoordinator = null;
		for(IntermediateCluster tCluster : mElectingClusters) {
			Logging.log(this, "Checking cluster " + tCluster);
			if(tCluster.getHighestPriority() <= tCluster.getPriority())	{
				tNodessClusterForCoordinator = tCluster;
			}
		}
		if(tNodessClusterForCoordinator != null) {
			if(!mPleaseInterrupt) {
				initiateCoordinatorFunctions(tNodessClusterForCoordinator);
			} else {
				Logging.err(this, "I had the highest priority, but election was cancelled");
				restart();
			}
		} else {
			mLostElection = true;
		}
	}
	
	/**
	 * 
	 * @param pSourceCluster is the cluster from which you want the path
	 * @param pTargetCluster is the cluster
	 * @return 
	 */
	
	public void run()
	{
		try {
			mInProgress = true;
			long tTimeWaitUntil=0;
			sendElections();
			tTimeWaitUntil=System.currentTimeMillis()+TIMEOUT_FOR_PEERS;
			checkWait(System.currentTimeMillis(), tTimeWaitUntil);
			Logging.log(this, "Sent elections");
			if(!mPleaseInterrupt) {
				checkClustersForHighestPriority(false);
				/*
				 * initiate new election in case other clusters had higher priority
				 */
				tTimeWaitUntil = System.currentTimeMillis() + TIMEOUT_FOR_ANNOUNCEMENT;
				checkWait(System.currentTimeMillis(), tTimeWaitUntil);
				if(mLevel > 0) {
					for(ICluster tCluster : mElectingClusters) { 
						/*
						 * For loop can be ignored as this can only happen in case we are above level one
						 */
						while((tCluster.getCoordinator().getClusterWithCoordinatorOnLevel(tCluster.getLevel()) == null)) {
							tCluster.setHighestPriority(tCluster.getPriority());
							Logging.log(tCluster, " did not yet receive an announcement");
							for(CoordinatorCEPDemultiplexed tCEP : tCluster.getParticipatingCEPs()) {
								RequestCoordinator tRequest = new RequestCoordinator(false);
								tCEP.write(tRequest);
								synchronized(tRequest) {
									if(!tRequest.mWasNotified)
									tRequest.wait(10000);
									if(!tRequest.mWasNotified) {
										Logging.log(this, "Was still waiting for " + tRequest);
										tRequest.wait();
									}
								}
							}
							/*
							tTimeWaitUntil = System.currentTimeMillis()+TIMEOUT_FOR_LAGGARDS;
							checkWait(System.currentTimeMillis(), tTimeWaitUntil);
							*/
							try {
								LinkedList<CoordinatorCEPDemultiplexed> tCEPs = new LinkedList<CoordinatorCEPDemultiplexed>();
								tCEPs.addAll(tCluster.getParticipatingCEPs());
								if(((IntermediateCluster)tCluster).getOldParticipatingCEPs() != null) {
									tCEPs.addAll(((IntermediateCluster)tCluster).getOldParticipatingCEPs());
								}
								for(CoordinatorCEPDemultiplexed tCEP: tCluster.getParticipatingCEPs()) {
									if(! tCEP.knowsCoordinator()) {
										if(!tCluster.getCoordinator().checkPathToTargetContainsCovered(tCluster.getCoordinator().getSourceIntermediate(tCEP.getRemoteCluster()), tCEP.getRemoteCluster(), tCEPs)) {
											tCluster.getCoordinator().getLogger().log(tCluster, "adding laggard " + tCEP + " while clusters between are " + tCluster.getCoordinator().getClusterMap().getIntermediateNodes(tCluster.getCoordinator().getSourceIntermediate(tCEP.getRemoteCluster()), tCEP.getRemoteCluster()));
											tCluster.addLaggard(tCEP);
										} else {
											tCluster.getCoordinator().getLogger().info(tCluster, "not adding laggard " + tCEP);
										}
									} 
								}
							} catch (ConcurrentModificationException tExc) {
								Logging.err(this, "Error when looking for uncovered clusters", tExc);
							}
							if(tCluster.getLaggards() != null) {
								((IntermediateCluster)tCluster).setParticipatingCEPs((LinkedList<CoordinatorCEPDemultiplexed>) tCluster.getLaggards().clone());
								tCluster.getLaggards().clear();
							}
							if(tCluster.getCoordinator().getClusterWithCoordinatorOnLevel(tCluster.getLevel()) == null) {
								checkClustersForHighestPriority(true);
							} else {
								break;
							}
						}
					}
				}
			} else {
				restart();
			}
		} catch (Exception tExc) {
			Logging.warn(this, "Election interrupted", tExc);
			run();
		}
		mInProgress = false;
		ElectionManager.getElectionManager().removeElection(mElectingClusters.get(0).getLevel(), mElectingClusters.get(0).getClusterID());
	}
	
	private void restart()
	{
		mPleaseInterrupt=false;
		run();
	}
	
	public void checkWait(long pReference, long pCompare)
	{
		synchronized(mPleaseInterrupt) {
			if(pReference >= pCompare || mPleaseInterrupt) {
				return;
			} else {
				long tWaitTime = pCompare-pReference;
				if(mPleaseInterrupt) {
					Logging.log(this, "Election was interrupted, not waiting for settlement of peer responses");
				}
				if(tWaitTime ==0 || mPleaseInterrupt) return;
				try	{
					mPleaseInterrupt.wait(tWaitTime);
				} catch (InterruptedException tExc) {
					Logging.trace(this, "was interrupted");
				}
				checkWait(System.currentTimeMillis(), pCompare);
			}
		}
	}
	
	public LinkedList<IntermediateCluster> getParticipatingClusters()
	{
		return mElectingClusters;
	}
	
	public static class ElectionManager
	{
		private HashMap<Integer, HashMap<Long, ElectionProcess>>mElections = null;
		private static ElectionManager mManager = null;
		private ElectionNotification mNotification;
		
		public ElectionManager()
		{
			mElections = new HashMap<Integer, HashMap<Long, ElectionProcess>>();
		}
		
		public static ElectionManager getElectionManager()
		{
			if(mManager == null) {
				mManager = new ElectionManager();
			}
			return mManager;
		}
		
		/**
		 * Returns a processes of a defined hierarchy level and cluster ID.
		 * This functions is used within the GUI.
		 * 
		 * @param pLevel
		 * @param pClusterID
		 * @return
		 */
		public synchronized ElectionProcess getProcess(int pLevel, Long pClusterID)
		{
			if(mElections.containsKey(pLevel)) {
				if(mElections.containsKey(pLevel) && mElections.get(pLevel).containsKey(pClusterID)) {
					return mElections.get(pLevel).get(pClusterID); 
				}
			}
			return null;
		}
		
		/**
		 * Returns all processes on a defined hierarchy level.
		 * This functions is used within the GUI.
		 * 
		 * @param pLevel
		 * @return
		 */
		public Collection<ElectionProcess> getProcesses(int pLevel)
		{
			try {
				return mElections.get(pLevel).values();
			} catch (NullPointerException tExc) {
				return new LinkedList<ElectionProcess>();
			}
		}
		
		public ElectionProcess addElection(int pLevel, Long pClusterID, ElectionProcess pElection)
		{
			if(!mElections.containsKey(pLevel)) {
				mElections.put(pLevel, new HashMap<Long, ElectionProcess>());
				mElections.get(pLevel).put(pClusterID, pElection);
				return pElection;
			} else {
				if(mElections.get(pLevel).containsKey(pClusterID)) {
					return mElections.get(pLevel).get(pClusterID);
				} else {
					mElections.get(pLevel).put(pClusterID, pElection);
					return pElection;
				}
			}
		}
		
		public void removeElection(Integer pLevel, Long pClusterID)
		{
			if(HRMConfig.Routing.BUILD_UP_HIERARCHY_AUTOMATICALLY) {
				mElections.get(pLevel).remove(pClusterID);
				if(mElections.get(pLevel).isEmpty()) {
					if(mNotification != null) {
						mNotification = null;
					}
					Logging.log(this, "No more elections available, preparing next cluster");
					if(mElections.containsKey(pLevel + 1)) {
						for(ElectionProcess tProcess : mElections.get(Integer.valueOf(pLevel + 1)).values()) {
							tProcess.start();
						}
					}
				}
			} else {
				return;
			}
		}
		
		public LinkedList<ElectionProcess> getAllElections()
		{
			LinkedList<ElectionProcess> tElections = new LinkedList<ElectionProcess>();
			for(Integer tLevel: mElections.keySet()) {
				if(mElections.get(tLevel) != null) {
					for(Long tID : mElections.get(tLevel).keySet()) {
						if(mElections.get(tLevel).get(tID) != null) {
							tElections.add(mElections.get(tLevel).get(tID));
						}
					}
				}
			}
			return tElections;
		}
		
		public void reevaluate(int pLevel)
		{
			if(HRMConfig.Routing.BUILD_UP_HIERARCHY_AUTOMATICALLY) {
				boolean tWontBeginDistribution = false;
				ElectionProcess tWaitingFor = null;
				for(ElectionProcess tProcess : mElections.get(pLevel).values()) {
					Logging.log(tProcess + " is " + (tProcess.aboutToContinue() ? " about to " : "not about to ") + "initialize its Cluster Manager");
					if(!tProcess.aboutToContinue()) {
						tWontBeginDistribution = true;
						tWaitingFor = tProcess;
					}
				}
				if(tWontBeginDistribution) {
					Logging.log(this, "Not notifying other election processes because of " + tWaitingFor + " (reporting only last process)");
				} else {
					if(mNotification == null) {
						mNotification = new ElectionNotification(mElections.get(pLevel).values());
						for(ElectionProcess tProcess : mElections.get(pLevel).values()) {
							tProcess.mElectingClusters.getFirst().getCoordinator().getPhysicalNode().getAS().getSimulation().getTimeBase().scheduleIn(5, mNotification);
							break;
						}
					} else {
						return;
					}
				}
			} else {
				return;
			}
		}
		
		private class ElectionNotification implements IEvent
		{
			private Collection<ElectionProcess> mElectionsToNotify = null;
			
			public ElectionNotification(Collection<ElectionProcess> pElections)
			{
				mElectionsToNotify = pElections;
			}
			
			@Override
			public void fire()
			{
				for(ElectionProcess tProcess : mElectionsToNotify) {
					synchronized(tProcess) {
						tProcess.notifyAll();
					}
				}
			}
			
		}
	}
	
	public boolean aboutToContinue()
	{
		return mWillInitiateManager || mLostElection;
	}
}
