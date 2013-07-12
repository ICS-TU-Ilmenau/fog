/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.election;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import de.tuilmenau.ics.fog.IEvent;
import de.tuilmenau.ics.fog.exceptions.AuthenticationException;
import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.packets.hierarchical.RequestCoordinator;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyElect;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMIdentity;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMSignature;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig.Hierarchy;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.ICluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.NeighborCluster;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.CoordinatorCEPDemultiplexed;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

public class ElectionProcess extends Thread
{
	private Boolean mPleaseInterrupt = false;
	private long TIMEOUT_FOR_PEERS = 1000;
	private long WAIT_BEFORE_ADDRESS_DISTRIBUTION = 5000;
	private long TIMEOUT_FOR_ANNOUNCEMENT=5000;
	
	private Coordinator mClusterManager=null;
	private Cluster mElectingCluster = null;
	private boolean mInProgress = false;
	private int mHierarchyLevel = 0;	
	private boolean mWillInitiateManager = false;
	private boolean mLostElection = false;
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(Cluster=" + (mElectingCluster != null ? "" : mElectingCluster.getClusterID()) + ", HierLevel=" + mHierarchyLevel + ")";
	}
	
	public ElectionProcess(Cluster pCluster)
	{
		mHierarchyLevel = pCluster.getHierarchyLevel();
		mElectingCluster = pCluster;

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

	private void sendElections()
	{
		Logging.log("SENDELECTIONS()-START, electing cluster is " + mElectingCluster);
		Logging.log("SENDELECTIONS()-CEPs: " + mElectingCluster.getParticipatingCEPs().size());

		for(CoordinatorCEPDemultiplexed tCEP : mElectingCluster.getParticipatingCEPs()) {
			if(tCEP.getPeerPriority() == BullyPriority.UNDEFINED_PRIORITY && ! tCEP.isEdgeCEP()/* || tCEP.getPeerPriority() > tCluster.getPriority()*/) {
				Node tNode = mElectingCluster.getHRMController().getPhysicalNode();
				
				Logging.log("Node " + tNode + ": Sending elections from " + mElectingCluster);

				tCEP.sendPacket(new BullyElect(tNode.getCentralFN().getName(), new BullyPriority(mElectingCluster.getBullyPriority()), mElectingCluster.getHierarchyLevel()));
			}
		}
		Logging.log("SENDELECTIONS()-END");
	}
	
	public synchronized boolean isStarted()
	{
		return mInProgress;
	}
	
	private void initiateCoordinatorFunctions(Cluster pCluster)
	{
		Random tRandom = new Random(System.currentTimeMillis());
		HRMController tHRMController = pCluster.getHRMController();
		Node tNode = tHRMController.getPhysicalNode();
		int tToken = tRandom.nextInt();
		
		pCluster.setToken(tToken);
		pCluster.getHRMController().getLogger().log(pCluster, "Initiating coordinator functions...");

		if(pCluster.getHRMController().getIdentity() == null) {
			String tName = tNode.getName();
			HRMIdentity tIdentity= new HRMIdentity(tName);
			pCluster.getHRMController().setIdentity(tIdentity);
		}
		
		try {
			BullyAnnounce tAnnounce = new BullyAnnounce(tNode.getCentralFN().getName(), new BullyPriority(pCluster.getBullyPriority()), pCluster.getHRMController().getIdentity().createSignature(tNode.toString(), null, pCluster.getHierarchyLevel()), pCluster.getToken());
			for(CoordinatorCEPDemultiplexed tCEP : pCluster.getParticipatingCEPs()) {
				tAnnounce.addCoveredNode(tCEP.getPeerName());
			}
			if(tAnnounce.getCoveredNodes() == null || (tAnnounce.getCoveredNodes() != null && tAnnounce.getCoveredNodes().isEmpty())) {
				pCluster.getHRMController().getLogger().log(this, "Sending announce that does not cover anyhting");
			}
			pCluster.sendClusterBroadcast(tAnnounce, null);
			
			Name tAddress = tNode.getRoutingService().getNameFor(tNode.getCentralFN());; 
			
			pCluster.setCoordinatorCEP(null, pCluster.getHRMController().getIdentity().createSignature(tNode.toString(), null, pCluster.getHierarchyLevel()), tNode.getCentralFN().getName(), (L2Address)tAddress);
			LinkedList<HRMSignature> tSignatures = tHRMController.getApprovedSignatures();
			tSignatures.add(tHRMController.getIdentity().createSignature(tNode.toString(), null, pCluster.getHierarchyLevel()));
			
			if(mHierarchyLevel > 0) {
				pCluster.getHRMController().getLogger().log(pCluster, "has the coordinator and will now announce itself");
				for(ICluster tToAnnounce : pCluster.getNeighbors()) {
//					List<VirtualNode> tNodesBetween = pCluster.getCoordinator().getClusterMap().getIntermediateNodes(pCluster, tToAnnounce);
					/*
					 * OK: Because of the formerly sent 
					 */
					if(tToAnnounce instanceof NeighborCluster) {
						BullyAnnounce tBullyAnnounce = new BullyAnnounce(tNode.getCentralFN().getName(), new BullyPriority(pCluster.getBullyPriority()), pCluster.getHRMController().getIdentity().createSignature(tNode.toString(), null, pCluster.getHierarchyLevel()), pCluster.getToken());
						for(CoordinatorCEPDemultiplexed tCEP: pCluster.getParticipatingCEPs()) {
							tBullyAnnounce.addCoveredNode(tCEP.getPeerName());
						}
						for(CoordinatorCEPDemultiplexed tCEP : ((NeighborCluster)tToAnnounce).getAnnouncedCEPs()) {
							tCEP.sendPacket(tBullyAnnounce);
						}
					}
				}
			}
		} catch (AuthenticationException tExc) {
			pCluster.getHRMController().getLogger().err(this, "Unable to create signature for coordinator", tExc);
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
			mClusterManager = new Coordinator(pCluster, pCluster.getHierarchyLevel()+1, pCluster.getHrmID());
			pCluster.setClusterManager(mClusterManager);
			pCluster.getHRMController().setSourceIntermediateCluster(mClusterManager, pCluster);
			mClusterManager.setPriority(pCluster.getBullyPriority());
			pCluster.getHRMController().addCluster(mClusterManager);
			if(pCluster.getHierarchyLevel() +1 != HRMConfig.Hierarchy.HEIGHT) {
				// stepwise hierarchy creation
				Logging.log(this, "Will now wait because hierarchy build up is done stepwise");
				mWillInitiateManager = true;
				if(mHierarchyLevel == 1) {
					Logging.log(this, "Trigger");
				}
				Logging.log(this, "Reevaluating whether other processes settled");
				ElectionManager.getElectionManager().reevaluate(pCluster.getHierarchyLevel());
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException tExc) {
						Logging.err(this, "Unable to fulfill stepwise hierarchy preparation", tExc);
					}
				}
				mClusterManager.prepareAboveCluster(pCluster.getHierarchyLevel() +1);
			} else {
				Logging.log(this, "Beginning address distribution");
				try {
					mClusterManager.setHRMID(new HRMID(0));
					synchronized(mPleaseInterrupt) {
						Logging.log(this, "ACTIVE WAITING (init) - " + WAIT_BEFORE_ADDRESS_DISTRIBUTION);
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
	
	private void checkClustersForHighestPriority(boolean pVerbose)
	{
		long tPriority = 0;
		String tOutput = new String();
		for(CoordinatorCEPDemultiplexed tCEP : mElectingCluster.getParticipatingCEPs()) {
			tPriority = tCEP.getPeerPriority(); 
			tOutput +=  (tOutput.equals("") ? "" : ", ") +  tPriority;
			if(tPriority >= mElectingCluster.getHighestPriority()) {
				mElectingCluster.setHighestPriority(tPriority);
			} else {
				if(pVerbose) mElectingCluster.getHRMController().getLogger().log(mElectingCluster, "has lower priority than " + tCEP + " while mine is " + mElectingCluster.getBullyPriority());
			}
		}

		Cluster tNodessClusterForCoordinator = null;
		Logging.log(this, "Checking cluster " + mElectingCluster);
		if(mElectingCluster.getHighestPriority() <= mElectingCluster.getBullyPriority())	{
			tNodessClusterForCoordinator = mElectingCluster;
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
			Logging.log(this, "Sending elections..");
			if(!mPleaseInterrupt) {
				checkClustersForHighestPriority(false);
				/*
				 * initiate new election in case other clusters had higher priority
				 */
				tTimeWaitUntil = System.currentTimeMillis() + TIMEOUT_FOR_ANNOUNCEMENT;
				checkWait(System.currentTimeMillis(), tTimeWaitUntil);
				if(mHierarchyLevel > 0) {
					/*
					 * For loop can be ignored as this can only happen in case we are above level one
					 */
					while((mElectingCluster.getHRMController().getClusterWithCoordinatorOnLevel(mElectingCluster.getHierarchyLevel()) == null)) {
						mElectingCluster.setHighestPriority(mElectingCluster.getBullyPriority());
						Logging.log(mElectingCluster, " did not yet receive an announcement");
						for(CoordinatorCEPDemultiplexed tCEP : mElectingCluster.getParticipatingCEPs()) {
							RequestCoordinator tRequest = new RequestCoordinator(/* false */);
							tCEP.sendPacket(tRequest);
							synchronized(tRequest) {
								if(!tRequest.mWasNotified){
									Logging.log(this, "ACTIVE WAITING (run)");
									tRequest.wait(10000);
								}
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
							tCEPs.addAll(mElectingCluster.getParticipatingCEPs());
							if(mElectingCluster.getOldParticipatingCEPs() != null) {
								tCEPs.addAll(mElectingCluster.getOldParticipatingCEPs());
							}
							for(CoordinatorCEPDemultiplexed tCEP: mElectingCluster.getParticipatingCEPs()) {
								if(! tCEP.knowsCoordinator()) {
									if(!mElectingCluster.getHRMController().checkPathToTargetContainsCovered(mElectingCluster.getHRMController().getSourceIntermediate(tCEP.getRemoteCluster()), tCEP.getRemoteCluster(), tCEPs)) {
										mElectingCluster.getHRMController().getLogger().log(mElectingCluster, "adding laggard " + tCEP + " while clusters between are " + mElectingCluster.getHRMController().getRoutableClusterGraph().getIntermediateNodes(mElectingCluster.getHRMController().getSourceIntermediate(tCEP.getRemoteCluster()), tCEP.getRemoteCluster()));
										mElectingCluster.addLaggard(tCEP);
									} else {
										mElectingCluster.getHRMController().getLogger().info(mElectingCluster, "not adding laggard " + tCEP);
									}
								} 
							}
						} catch (ConcurrentModificationException tExc) {
							Logging.err(this, "Error when looking for uncovered clusters", tExc);
						}
						if(mElectingCluster.getLaggards() != null) {
							((Cluster)mElectingCluster).setParticipatingCEPs((LinkedList<CoordinatorCEPDemultiplexed>) mElectingCluster.getLaggards().clone());
							mElectingCluster.getLaggards().clear();
						}
						if(mElectingCluster.getHRMController().getClusterWithCoordinatorOnLevel(mElectingCluster.getHierarchyLevel()) == null) {
							checkClustersForHighestPriority(true);
						} else {
							break;
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
		ElectionManager.getElectionManager().removeElection(mElectingCluster.getHierarchyLevel(), mElectingCluster.getClusterID());
	}
	
	private void restart()
	{
		mPleaseInterrupt=false;
		run();
	}
	
	private void checkWait(long pReference, long pCompare)
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
					Logging.log(this, "ACTIVE WAITING (checkWait) - " + tWaitTime);
					mPleaseInterrupt.wait(tWaitTime);
				} catch (InterruptedException tExc) {
					Logging.trace(this, "was interrupted");
				}
				checkWait(System.currentTimeMillis(), pCompare);
			}
		}
	}
	
	public Cluster getCluster()
	{
		return mElectingCluster;
	}
	
	public static class ElectionManager
	{
		private HashMap<Integer, HashMap<Long, ElectionProcess>>mElections = null;
		private static ElectionManager mManager = null;
		private ElectionEventNotification mNotification;
		
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
			if(HRMConfig.Hierarchy.BUILD_AUTOMATICALLY) {
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
		
		private void reevaluate(int pLevel)
		{
			if(HRMConfig.Hierarchy.BUILD_AUTOMATICALLY) {
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
						mNotification = new ElectionEventNotification(mElections.get(pLevel).values());
						for(ElectionProcess tProcess : mElections.get(pLevel).values()) {
							tProcess.mElectingCluster.getHRMController().getPhysicalNode().getAS().getSimulation().getTimeBase().scheduleIn(5, mNotification);
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
		
		private class ElectionEventNotification implements IEvent
		{
			private Collection<ElectionProcess> mElectionsToNotify = null;
			
			public ElectionEventNotification(Collection<ElectionProcess> pElections)
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
	
	private boolean aboutToContinue()
	{
		return mWillInitiateManager || mLostElection;
	}
}
