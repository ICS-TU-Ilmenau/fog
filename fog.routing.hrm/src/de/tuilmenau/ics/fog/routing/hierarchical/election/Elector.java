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
import java.util.Random;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.RequirementsException;
import de.tuilmenau.ics.fog.facade.RoutingException;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAlive;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyElect;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMEntity;
import de.tuilmenau.ics.fog.routing.hierarchical.clustering.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.coordination.CoordinatorCEPChannel;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is responsible for coordinator elections. It is instantiated per cluster. 
 *
 */
public class Elector implements HRMEntity
{
	public enum ElectorState {
		START,    // Constructor
		IDLE,     // no coordinator known, no election running
		ELECTING, // election process is currently running
		ELECTED   // election process has established common consensus about the coordinator of the cluster
	}

	//TODO: rückkehr von ELECTED zu ELECTING, wenn BullyAlive von koordinator ausbleibt
	
	/** 
	 * Stores the internal state of the elector
	 */
	private ElectorState mState;
	
	/**
	 * Pointer to the parent cluster, which owns this elector
	 */
	private Cluster mParentCluster = null;

	/**
	 * The timeout for an awaited BullyAlive message (in ms).
	 */
	private long TIMEOUT_FOR_ALIVE = 8000;

	/**
	 * The time period between two BullyAlive messages (in ms).
	 */
	private long PERIOD_FOR_ALIVE = 8000;

	/**
	 * Stores if election was won.
	 */
	private boolean mElectionWon = false;
	
	public Elector(Cluster pCluster)
	{
		mState = ElectorState.START;
		mParentCluster = pCluster;
		mElectionWon = false;
		
		if (mParentCluster != null){
			// register at ElectionManager
			ElectionManager.getElectionManager().addElection(mParentCluster.getHierarchyLevel().getValue(), mParentCluster.getClusterID(), this);
		}

		// set IDLE state
		setElectorState(ElectorState.IDLE);
		
		// start coordinator election for the created HRM instance if desired
		if(HRMConfig.Hierarchy.BUILD_AUTOMATICALLY) {
			signalElectionStart();
		}
	}
	
	/**
	 * Starts the election process. This function is usually called by the GUI.
	 */
	public void startElection()
	{
		signalElectionStart();
		
		//TODO: remove the following and trigger it in a correct way 
		eventAllPrioritiesReceived();
	}
	
	/**
	 * Starts the clustering process. This function is usually called by the GUI.
	 */
	public void startClustering()
	{
		if (mParentCluster == null){
			Logging.err(this, "Invalid parent cluster");
			return;
		}
		
		// does the parent cluster have a coordinator, so we can start clustering of it and its siblings? 
		if (mParentCluster.getCoordinator() == null){
			Logging.warn(this, "Parent cluster doesn't have a coordinator yet, skipping clustering request");
			return;
		}

		// start the clustering of this cluster's coordinator and its neighbors
		mParentCluster.getCoordinator().clusterCoordinators();
	}

	/**
	 * Sets the current elector state
	 * 
	 * @param pNewState the new state
	 */
	private void setElectorState(ElectorState pNewState)
	{
		// check if state transition is valid
		if(( ((mState == ElectorState.START) && (pNewState == ElectorState.IDLE)) ||
			((mState == ElectorState.IDLE) && (pNewState == ElectorState.ELECTING)) ||
			((mState == ElectorState.ELECTING) && (pNewState == ElectorState.ELECTED)) ||
			((mState == ElectorState.ELECTED) && (pNewState == ElectorState.ELECTING)) ||
			((mState == ElectorState.ELECTED) && (pNewState == ElectorState.IDLE))
		   ) && (mState != pNewState)) 
		{
			Logging.log(this, "State transition from " + getElectorState() + " to " + pNewState);

			// set new state
			mState = pNewState;
		} else {
			throw new RuntimeException(toLocation() + " cannot change its state from " + mState +" to " + pNewState);
		}
	}
	
	/**
	 * Determines the current elector state.
	 * 
	 * @return current state of the elector
	 */
	public ElectorState getElectorState()
	{
		return mState;
	}	

	/**
	 * Determines of this elector has won the election.
	 * 
	 * @return true if won, otherwise false
	 */
	public boolean isWinner()
	{
		return mElectionWon;
	}
	
	/**
	 * SIGNAL: start the election by signaling BULLY ELECT to all cluster members
	 */
	private void signalElectionStart()
	{
		if (getElectorState() != ElectorState.ELECTED){
			Logging.log(this, "SENDELECTIONS()-START, electing cluster is " + mParentCluster);
			Logging.log(this, "SENDELECTIONS(), cluster members: " + mParentCluster.getClusterMembers().size());
	
			// set correct elector state
			setElectorState(ElectorState.ELECTING);
			
			// get reference to the node
			Node tNode = mParentCluster.getHRMController().getNode();
	
			// create the packet
			BullyElect tPacketBullyElect = new BullyElect(tNode.getCentralFN().getName(), mParentCluster.getBullyPriority());
			
			// send broadcast
			mParentCluster.sendPacketBroadcast(tPacketBullyElect);
			
			Logging.log(this, "SENDELECTIONS()-END");
		}else{
			// elector state is ELECTED
			Logging.warn(this, "Election state is still ELECTED, we still have a valid coordinator elected, skipping election request");
		}			
	}

	/**
	 * SIGNAL: ends the election by signaling BULLY ANNOUNCE to all cluster members
	 */
	private void signalElectionEnd()
	{
		if (getElectorState() == ElectorState.ELECTING){
			Logging.log(this, "SENDANNOUNCE()-START, electing cluster is " + mParentCluster);
			Logging.log(this, "SENDANNOUNCE(), cluster members: " + mParentCluster.getClusterMembers().size());
	
			// get reference to the node
			Node tNode = mParentCluster.getHRMController().getNode();
	
			// create the packet
			BullyAnnounce tPacketBullyAnnounce = new BullyAnnounce(tNode.getCentralFN().getName(), mParentCluster.getBullyPriority(), mParentCluster.getSignature(), mParentCluster.getToken());
	
			// send broadcast
			mParentCluster.sendPacketBroadcast(tPacketBullyAnnounce);
	
			// set correct elector state
			setElectorState(ElectorState.ELECTED);
	
			Logging.log(this, "SENDANNOUNCE()-END");
		}else{
			// elector state is ELECTED
			Logging.warn(this, "Election state isn't ELECTING, we cannot finishe an election which wasn't started yet, error in state machine");
		}			
	}
	
	/**
	 * SIGNAL: report itself as alive by signaling BULLY ALIVE to all cluster members
	 */
	private void signalAlive()
	{
		if (HRMConfig.Election.SEND_BULLY_ALIVES){
			Logging.log(this, "SENDALIVE()-START, electing cluster is " + mParentCluster);
			Logging.log(this, "SENDALIVE(), cluster members: " + mParentCluster.getClusterMembers().size());
	
			// get reference to the node
			Node tNode = mParentCluster.getHRMController().getNode();
	
			// create the packet
			BullyAlive tPacketBullyAlive = new BullyAlive(tNode.getCentralFN().getName());
	
			// send broadcast
			mParentCluster.sendPacketBroadcast(tPacketBullyAlive);
	
			Logging.log(this, "SENDALIVE()-END");
		}else{
			// BullyAlive messages are currently deactivated
		}			
	}

	/**
	 * EVENT: sets the local node as coordinator for the parent cluster. 
	 */
	private void eventElectionWon()
	{
		Logging.log(this, "ELECTION WON for cluster " + mParentCluster);
		
		// mark as election winner
		mElectionWon = true;
		
		HRMController tHRMController = mParentCluster.getHRMController();
		Node tNode = tHRMController.getNode();		
		int tHierarchyLevelValue = mParentCluster.getHierarchyLevel().getValue();

		// send BULLY ANNOUNCE in order to signal all cluster members that we are the coordinator
		signalElectionEnd();
		
		Name tNodeName = tNode.getRoutingService().getNameFor(tNode.getCentralFN());
		mParentCluster.setCoordinatorCEP(null, mParentCluster.getSignature(), tNode.getCentralFN().getName(), /* token */ new Random(System.currentTimeMillis()).nextInt(), (L2Address)tNodeName);

		// create new coordinator instance
		Coordinator tElectedCoordinator = new Coordinator(mParentCluster);
	
		//TODO: ??
		mParentCluster.getHRMController().setSourceIntermediateCluster(tElectedCoordinator, mParentCluster);
			
		if(tHierarchyLevelValue < HRMConfig.Hierarchy.HEIGHT - 1) {
			if (HRMConfig.Hierarchy.BUILD_AUTOMATICALLY){
				Logging.log(this, "ELECTION WON - triggering clustering of thus cluster's coordinator and its neighbors");

				// start the clustering of this cluster's coordinator and its neighbors
				startClustering();
			}
		} else {
			Logging.log(this, "ELECTION WON - beginning address distribution");
			try {
				tElectedCoordinator.signalAddresses();
			} catch (RemoteException tExc) {
				Logging.err(this, "Remote problem - error when trying to distribute addresses", tExc);
			} catch (RoutingException tExc) {
				Logging.err(this, "Routing problem - error when trying to distribute addresses", tExc);
			} catch (RequirementsException tExc) {
				Logging.err(this, "Requirements problem - error when trying to distribute addresses", tExc);
			}
		}
	}
	
	private void eventAllPrioritiesReceived()
	{
		// collect the Bully priority from all cluster members
		Logging.log(this, "Searching for highest priority...");
		for(CoordinatorCEPChannel tClusterMember : mParentCluster.getClusterMembers()) {
			BullyPriority tPriority = tClusterMember.getPeerPriority(); 
			
			Logging.log(this, "		..cluster member " + tClusterMember + " has priority " + tClusterMember.getPeerPriority().getValue()); 
			
			// does the cluster member have a higher Bully priority?
			if(tPriority.isHigher(this, mParentCluster.getHighestPriority())) {
				mParentCluster.setHighestPriority(tPriority);
				Logging.log(this, "	         ..selecting " + tClusterMember + " as coordinator candidate");
			}
		}

		// check own priority - are we the winner of the election?
		Logging.log(this, "Checking if we are the winner of the election...");		
		boolean tIsWinner = false;
		if(mParentCluster.getBullyPriority().isHigher(this, mParentCluster.getHighestPriority())){
			tIsWinner = true;
		}else{
			if (mParentCluster.getBullyPriority().equals(mParentCluster.getHighestPriority())){
				Logging.warn(this, "Peers have SAME BULLY PRIORITY");

				//TODO: rework signaling and calculate random number which has to be communicated to peers!
				
				// mark as winner
				tIsWinner = true;				
			}
		}
	
		// have we won the election?
		if(tIsWinner) {
			eventElectionWon();
		}else{
			eventElectionLost();
		}
	}
	
	/**
	 * EVENT: sets the local node as simple cluster member.
	 */
	private void eventElectionLost()
	{
		Logging.log(this, "ELECTION LOST for cluster " + mParentCluster);
	
		// mark as election loser
		mElectionWon = false;
	}
	
	/**
	 * 
	 * @param pSourceCluster is the cluster from which you want the path
	 * @param pTargetCluster is the cluster
	 * @return 
	 */
	
//	public void run()
//	{
//		signalElectionStart();
//	
//		deriveElectionResult();
//		
//		if(getHierarchLevel().isHigherLevel()) {
//			/*
//			 * For loop can be ignored as this can only happen in case we are above level one
//			 */
//			while((mParentCluster.getHRMController().getClusterWithCoordinatorOnLevel(mParentCluster.getHierarchyLevel().getValue()) == null)) {
//				mParentCluster.setHighestPriority(mParentCluster.getBullyPriority());
//				Logging.log(this, mParentCluster + " didn't yet receive an announcement");
//				for(CoordinatorCEPChannel tCEP : mParentCluster.getClusterMembers()) {
//					RequestCoordinator tRequest = new RequestCoordinator(/* false */);
//					tCEP.sendPacket(tRequest);
////					synchronized(tRequest) {
////						if(!tRequest.mWasNotified){
////							Logging.log(this, "ACTIVE WAITING (run)");
////							tRequest.wait(10000);
////						}
////						if(!tRequest.mWasNotified) {
////							Logging.log(this, "Was still waiting for " + tRequest);
////							tRequest.wait();
////						}
////					}
//				}
//				/*
//				tTimeWaitUntil = System.currentTimeMillis()+TIMEOUT_FOR_LAGGARDS;
//				checkWait(System.currentTimeMillis(), tTimeWaitUntil);
//				*/
//				try {
//					LinkedList<CoordinatorCEPChannel> tCEPs = new LinkedList<CoordinatorCEPChannel>();
//					tCEPs.addAll(mParentCluster.getClusterMembers());
//					if(mParentCluster.getOldParticipatingCEPs() != null) {
//						tCEPs.addAll(mParentCluster.getOldParticipatingCEPs());
//					}
//					for(CoordinatorCEPChannel tCEP: mParentCluster.getClusterMembers()) {
//						if(! tCEP.knowsCoordinator()) {
//							if(!mParentCluster.getHRMController().checkPathToTargetContainsCovered(mParentCluster.getHRMController().getSourceIntermediate(tCEP.getRemoteClusterName()), tCEP.getRemoteClusterName(), tCEPs)) {
//								Logging.log(this, mParentCluster + " is adding laggard " + tCEP + " while clusters between are " + mParentCluster.getHRMController().getRoutableClusterGraph().getIntermediateNodes(mParentCluster.getHRMController().getSourceIntermediate(tCEP.getRemoteClusterName()), tCEP.getRemoteClusterName()));
//								mParentCluster.addLaggard(tCEP);
//							} else {
//								Logging.info(this, mParentCluster + " doesn't add laggard " + tCEP);
//							}
//						} 
//					}
//				} catch (ConcurrentModificationException tExc) {
//					Logging.err(this, "Error when looking for uncovered clusters", tExc);
//				}
//				if(mParentCluster.getLaggards() != null) {
//					((Cluster)mParentCluster).setParticipatingCEPs((LinkedList<CoordinatorCEPChannel>) mParentCluster.getLaggards().clone());
//					mParentCluster.getLaggards().clear();
//				}
//				if(mParentCluster.getHRMController().getClusterWithCoordinatorOnLevel(mParentCluster.getHierarchyLevel().getValue()) == null) {
//					deriveElectionResult();
//				} else {
//					break;
//				}
//			}
//		}
//
//		ElectionManager.getElectionManager().removeElection(mParentCluster.getHierarchyLevel().getValue(), mParentCluster.getClusterID());
//	}

	/**
	 * Determine the parent cluster, which owns this elector. 
	 * @return the parent cluster
	 */
	public Cluster getCluster()
	{
		return mParentCluster;
	}
	
	@Override
	public String toString()
	{
		return toLocation() + "(Cluster=" + (mParentCluster != null ? "(Cluster=" + mParentCluster.getClusterID() + ")" : "");
	}

	@Override
	public String toLocation()
	{
		String tResult = null;
		
		if (getCluster() != null){
			tResult = getClass().getSimpleName() + "@" + getCluster().getHRMController().getNodeGUIName() + "@" + getCluster().getHierarchyLevel().getValue();
		}else{
			tResult = getClass().getSimpleName();
		}			
		
		return tResult;
	}

//	private Boolean mPleaseInterrupt = false;
//	private long TIMEOUT_FOR_PEERS = 1000;
//	private long TIMEOUT_FOR_ANNOUNCEMENT=5000;
}
