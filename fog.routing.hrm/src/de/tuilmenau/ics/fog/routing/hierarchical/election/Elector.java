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

import java.util.LinkedList;

import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionAlive;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionWinner;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionLeave;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionPriorityUpdate;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionResign;
import de.tuilmenau.ics.fog.packets.hierarchical.election.ElectionReturn;
import de.tuilmenau.ics.fog.packets.hierarchical.election.SignalingMessageElection;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.Localization;
import de.tuilmenau.ics.fog.routing.hierarchical.management.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterMember;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ComChannel;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ControlEntity;
import de.tuilmenau.ics.fog.routing.hierarchical.management.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.management.CoordinatorAsClusterMember;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is responsible for coordinator elections. It is instantiated per Cluster and ClusterProxy object.
 *  For a Cluster object, this class plays the role of the cluster head.
 *  For a ClusterProxy, this class acts in the role of a cluster member.
 *
 */
public class Elector implements Localization
{
	private enum ElectorState {
		START,    // no coordinator known, no election running
		ELECTING, // election process is currently running
		ELECTED,   // election process has established common consensus about the coordinator of the cluster
		ERROR // election process run into an error state
	}
	
	/** 
	 * Stores the internal state of the elector
	 */
	private ElectorState mState = ElectorState.START;
	
	/**
	 * Pointer to the parent cluster, which owns this elector
	 */
	private ClusterMember mParent = null;

	/**
	 * Stores if election was won.
	 */
	private boolean mElectionWon = false;
	
	/**
	 * Stores a reference to the HRMController instance
	 */
	private HRMController mHRMController = null;
	
	/**
	 * Stores if the parent is an active or passive election member
	 */
	private boolean mParentIsActiveMember = false;
	
	/**
	 * Stores a counter for processed "re-elects"
	 */
	private long mCounterReelects = 0;

	/**
	 * Stores the causes for re-elections
	 */
	private LinkedList<String> mReelectCauses = new LinkedList<String>();
	
	/**
	 * Stores the causes for changes in the election result
	 */
	private LinkedList<String> mResultChangeCauses = new LinkedList<String>();

	/**
	 * Stores the number of the current election round
	 */
	private int mElectionRounds = 0;
	
	/**
	 * Stores the node-global election state: the active best ClusterMember instances per hierarchy level.
	 * All entries per higher (!) hierarchy level have to be part of the same superior cluster. This fact is enforced by leaveAllWorseAlternativeElections().
	 * For example, two or more ClusterAsClusterMember instances can be registered, which are part of the same local superior cluster.
	 * In general, this list stores the best choices for the distributed election process for each higher hierarchy level.
	 */
	private LinkedList<ClusterMember>[] mNodeActiveClusterMemberships = null;

	private static final boolean SEND_ALL_ELECTION_PARTICIPANTS = false;
	private static final boolean IGNORE_LINK_STATE = true;
	private static final boolean CHECK_LINK_STATE = false;
	private static final ComChannel BROADCAST = null;
	
	/**
	 * Constructor
	 *  
	 * @param pHRMController the HRMController instance
	 * @param pClusterMember the parent cluster member
	 */
	@SuppressWarnings("unchecked")
	public Elector(HRMController pHRMController, ClusterMember pClusterMember)
	{
		mParent = pClusterMember;
		mElectionWon = false;
		mHRMController = pHRMController;
		mNodeActiveClusterMemberships = (LinkedList<ClusterMember>[]) mHRMController.getNodeElectionState();
	}
	
	/**
	 * Creates the node-global election state object.
	 * It is created once a node is initialized and its HRMController instance is created.
	 * 
	 * @return the node-global election state
	 */
	@SuppressWarnings("unchecked")
	public static Object createNodeElectionState()
	{
		LinkedList<ClusterMember>[] tResult = null;

		tResult = (LinkedList<ClusterMember>[])new LinkedList[HRMConfig.Hierarchy.DEPTH];
		for(int i = 0; i < HRMConfig.Hierarchy.DEPTH; i++){
			tResult[i] = new LinkedList<ClusterMember>();
		}
		
		return tResult;	
	}

	/**
	 * Adds a ClusterMember as active entity to the database
	 * 
	 * @param pCause the cause for this call
	 */
	private void addActiveClusterMember(String pCause)
	{
		if(mNodeActiveClusterMemberships == null){
			throw new RuntimeException("Invalid node-global election state");
		}

		if (mParent instanceof Cluster){
			throw new RuntimeException("Invalid active ClusterMember: " + mParent);
		}
	
		if(mParent instanceof CoordinatorAsClusterMember){
			CoordinatorAsClusterMember tCoordinatorAsClusterMember = (CoordinatorAsClusterMember)mParent;
			
			tCoordinatorAsClusterMember.eventClusterMembershipToSuperiorCoordinator();
		}
		
		Logging.log(this, "Adding active ClusterMember: " + mParent);
		synchronized (mNodeActiveClusterMemberships) {
			LinkedList<ClusterMember> tLevelList = mNodeActiveClusterMemberships[mParent.getHierarchyLevel().getValue()];
			
			if(!tLevelList.contains(mParent)){
				tLevelList.add(mParent);
				Logging.log(this, "    ..added");
				mHRMController.addGUIDescriptionNodeElectionStateChange("\n + " + mParent + "\n   ^^^^" + pCause);
				
				mParentIsActiveMember = true;
			}else{
				Logging.log(this, "    ..NOT added because it was added in a previous turn");
			}
		}
	}
	
	/**
	 * Removes a ClusterMember as active entity from the database
	 * 
	 * @param pClusterMember the formerly active ClusterMember
	 * @param pCause the cause for this call
	 * 
	 * @return true if the given ClusterMember was actually an active entity
	 */
	private boolean removeActiveClusterMember(ClusterMember pClusterMember, String pCause)
	{
		boolean tResult = false;
		
		if(mNodeActiveClusterMemberships == null){
			throw new RuntimeException("Invalid node-global election state");
		}

		synchronized (mNodeActiveClusterMemberships) {
			LinkedList<ClusterMember> tLevelList = mNodeActiveClusterMemberships[pClusterMember.getHierarchyLevel().getValue()];
			
			if(tLevelList.contains(pClusterMember)){
				Logging.log(this, "Removing active ClusterMember: " + pClusterMember + ", cause=" + pCause);
				tLevelList.remove(pClusterMember);
				Logging.log(this, "    ..removed as ACTIVE ClusterMember");
				mHRMController.addGUIDescriptionNodeElectionStateChange("\n - " + pClusterMember + "\n   ^^^^" + pCause);
				pClusterMember.getElector().mParentIsActiveMember = false;
				tResult = true;
			}else{
				Logging.log(this, "    ..NOT removed as ACTIVE ClusterMember");
			}
		}
		
		return tResult;
	}

	/**
	 * Returns the active cluster memberships of the parent coordinator.
	 * This function actually returns only the active memberships of one single coordinator/L0-ClusterMember. This allows for the support of multiple L0 coordinators per node
	 *  
	 * @return a list of active cluster memberships
	 */
	private LinkedList<ClusterMember> getParentCoordinatorActiveClusterMemberships()
	{
		LinkedList<ClusterMember> tResult = new LinkedList<ClusterMember>();
		
		synchronized (mNodeActiveClusterMemberships) {
			LinkedList<ClusterMember> tAllPerLevel = mNodeActiveClusterMemberships[mParent.getHierarchyLevel().getValue()];
			for(ClusterMember tClusterMembership : tAllPerLevel){
				/**
				 * We filter the list for the cluster of the parent ClusterMember
				 */				
				if(tClusterMembership.getClusterID().equals(mParent.getClusterID())){
					// add it to the result
					tResult.add(tClusterMembership);
				}
			}			
		}
		
		return tResult;
	}

	/**
	 * Returns all cluster memberships of the parent coordinator.
	 * This function returns only the memberships of one single coordinator/L0-ClusterMember. This allows for the support of multiple L0 coordinators per node
	 *  
	 * @return a list of cluster memberships
	 */
	private LinkedList<CoordinatorAsClusterMember> getParentCoordinatorClusterMemberships()
	{
		LinkedList<CoordinatorAsClusterMember> tResult = new LinkedList<CoordinatorAsClusterMember>();
		
		LinkedList<CoordinatorAsClusterMember> tLevelClusterMemberships = mHRMController.getAllCoordinatorAsClusterMembers(mParent.getHierarchyLevel().getValue());
		for(CoordinatorAsClusterMember tClusterMembership : tLevelClusterMemberships){
			/**
			 * Be aware of multiple local coordinators at the same hierarchy level -> search only for alternative CoordinatorAsClusterMember instances belong to the parent instance!
			 * For example:
			 *  	- a node can have two L0 coordinators, each for a separate physical link (FoG bus)
			 *  	=> leads to two coordinators with own CoordinatorAsClusterMember instances, the amount depends on the clustering radius   			 
			 */
			if(mParent.getClusterID().equals(tClusterMembership.getClusterID())){
				// add it to the result
				tResult.add(tClusterMembership);
			}
		}			
		
		return tResult;
	}

	/**
	 * Elects the coordinator for this cluster.
	 * 
	 * @param pCause the cause for this call
	 */
	private void elect(String pCause)
	{
		// increase the counter for election rounds
		mElectionRounds++;
		
		// set correct elector state
		setElectorState(ElectorState.ELECTING);

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "ELECTING now...");
		}
		
		// OPTIMIZATION: do we know more than 0 external cluster members?
		if (mParent.countConnectedRemoteClusterMembers() > 0){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "elect()-trying to ask " + mParent.countConnectedRemoteClusterMembers() + " external cluster members for their Election priority: " + mParent.getComChannels());
			}

			/**
			 * Level 1+: use priorities
			 */
			Logging.log(this, "ELECTION round " + mElectionRounds);
			// make sure all others know our priority
			distributePRIRORITY_UPDATE(BROADCAST, this + "::elect()");
		}else{
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "elect()-don't have external cluster members");
			}
			
			// we don't have external members - but do we have local members?
			if(mParent.countConnectedClusterMembers() > 0){					
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
					Logging.log(this, "elect()-having " + mParent.countConnectedClusterMembers() + " local cluster members");
				}
				
				/**
				 * Send a priority update to all local cluster members
				 */
				distributePRIRORITY_UPDATE(BROADCAST, this + "::elect()\n   ^^^^" + pCause);
			}
		}
	}
	
	/**
	 * EVENT: elector is invalidated, triggered by ClusterMember if it gets invalidated
	 * 
	 * @param pCause the cause for the call
	 */
	public void eventInvalidation(String pCause)
	{
		Logging.log(this, "EVENT: invalidation, cause=" + pCause);

		if(mParentIsActiveMember){
			// return to all other election processes because we have lost this ClusterMember at this hierarchy level
			returnToAlternativeElections(this + "::eventReset()\n   ^^^^" + pCause);
			
			// mark/store as inactive ClusterMember
			boolean tRemovedActiveClusterMember = removeActiveClusterMember(mParent, this + "::eventReset()\n   ^^^^" + pCause);

			if(!tRemovedActiveClusterMember){
				Logging.err(this, "Haven't found parent in the list of active ClusterMembers (but it should be there), error in state machine, parent is: " + mParent);
			}
		}

		checkLocalElection(this + "::eventReset()\n   ^^^^" + pCause);
	}

	/**
	 * EVENT: participant joined
	 * 
	 * @param pComChannel the comm. channel towards the new participant
	 */
	public void eventElectionAvailable(ComChannel pComChannel)
	{
		Logging.log(this, "EVENT: election available for: " + pComChannel);
		
		/**
		 * Check if there exist already a better choice (an active ClusterMembership) for a superior cluster/coordinator
		 * 		-> leave this election immediately
		 */
		leaveWorseElection(pComChannel, this + "::eventElectionAvailable() for " + pComChannel);
		
		/**
		 * JOIN ELECTION:
		 * 		-> we either are a simple cluster member or we are a cluster head and a new member has joined
		 */
		Logging.log(this, "      ..eventElectionAvailable(), joining ELECTION, cause=" + pComChannel);
		joinElection(pComChannel, this + "::eventElectionAvailable() for " + pComChannel);
	}

	/**
	 * EVENT: lost candidate
	 * 
	 * @param pComChannelToCandidate the comm. channel to the lost candidate (the channel is already removed from all database and this is the last change to get some data from it)
	 */
	public void eventLostCandidate(ComChannel pComChannelToCandidate)
	{
		Logging.log(this, "Lost election candidate: " + pComChannelToCandidate);
	}

	/**
	 * Counts the re-elects
	 * 
	 * @return the number of processed re-elects
	 */
	public long countReelects()
	{
		return mCounterReelects;
	}
	
	/**
	 * Returns the causes for re-elections
	 * 
	 * @return the list of causes
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<String> getReelectCauses()
	{
		LinkedList<String> tResult = null;
		
		synchronized (mReelectCauses) {
			tResult = (LinkedList<String>) mReelectCauses.clone();
		}
		return tResult; 
	}

	/**
	 * Returns the causes for changes in the election result
	 * 
	 * @return the list of causes
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<String> getResultChangeCauses()
	{
		LinkedList<String> tResult = null;
		
		synchronized (mResultChangeCauses) {
			tResult = (LinkedList<String>) mResultChangeCauses.clone();
		}
		return tResult; 
	}	
	
	/**
	 * Restarts the election process for this cluster
	 * 
	 * @param pCause the causes for this re-election
	 */
	private void reelect(String pCause)
	{
		if (head()){
			mCounterReelects++;

			synchronized (mReelectCauses) {
				mReelectCauses.add("[" + mState.toString() + (isWinner() ? " WINNER, prio=" + mParent.getPriority().getValue() : "") + "]\n   ^^^^" + pCause);
			}
			
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "REELECTION");
			}
			elect(this + "::reelect()\n   ^^^^" + pCause);
		}else{
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "REELECTION needed but we aren't the cluster head, we hope that the other local Cluster object will trigger a REELECTION" );
			}
		}
	}
	
	/**
	 * Starts the election process.
	 * This function is be called by external processes.
	 * 
	 * @param pCause the cause for this election start
	 */
	public void startElection(String pCause)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "#### STARTING ELECTION");
		}
		
		/**
		 * Broadcast "ELECT"
		 */
		if(mParent instanceof ClusterMember){
			switch(mState){
				case START:
					elect(this + "::startElection_1()\n   ^^^^" + pCause);
					break;
				case ELECTED:
					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
						Logging.log(this, "RESTARTING ELECTION, old coordinator was valid: " + finished());
					}
					reelect(this + "::startElection_2()\n   ^^^^" + pCause);
					break;
				case ELECTING:
					Logging.log(this, "Election is already running");
					break;
				case ERROR:
					Logging.err(this, "Election is in ERROR state");
					break;
				default:
					break;
			}
		}else{
			throw new RuntimeException("We skipped election start because parent isn't a cluster head/member: " + mParent);
		}
		
		/**
		 * NEW OUTGOING MESSAGE -> check the local election result if it has changed
		 */
		checkLocalElection(this + "::startElection_1()\n   ^^^^" + pCause);
	}
	
	/**
	 * A cluster member joins an election
	 * 
	 * @param pComChannel the com. channel to the peer (either the cluster head or the joined cluster member)
	 * @param pCause the cause for the call
	 */
	private void joinElection(ComChannel pComChannel, String pCause)
	{
		/**
		 * make sure the election process is marked as "running"
		 */
		setElectorState(ElectorState.ELECTING);
		
		/**
		 * SEND PRIORITY UPDATE:
		 *    a.) we are a cluster head
		 * 		-> we are already electing, we should at least inform the peer about our priority
		 * 		-> if we don't send our priority, the peer might never get informed about our priority because another cluster participant might have won the election
		 *    b.) we are a cluster member
		 *      -> we are a fresh cluster member and send the head our priority 
		 */
		distributePRIRORITY_UPDATE(pComChannel, this + "::eventParticipantJoined() for " + pComChannel);
		
		/**
		 * Check the local election result if it has changed
		 */
		checkLocalElection(this + "::joinElection() for " + pComChannel);
	}

	
	/**
	 * Sets the current elector state
	 * 
	 * @param pNewState the new state
	 */
	private void setElectorState(ElectorState pNewState)
	{
		// check if state transition is valid
		if((pNewState == ElectorState.ERROR) ||	(mState == pNewState) || 
			( 
			((mState == ElectorState.START) && (pNewState == ElectorState.ELECTING)) ||
			((mState == ElectorState.ELECTING) && (pNewState == ElectorState.ELECTED)) ||
			((mState == ElectorState.ELECTED) && (pNewState == ElectorState.ELECTING))
		    )
		){
			if (mState != pNewState){
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
					Logging.log(this, "STATE TRANSITION from " + mState + " to " + pNewState);
				}
	
				// set new state
				mState = pNewState;
			}
		} else {
			throw new RuntimeException(this + "::setElectorState() can't change the state from " + mState +" to " + pNewState);
		}
	}
	
	/**
	 * Determines the current election state and returns a descriptive string.
	 * 
	 * @return current state of the election as string
	 */
	public String getElectionStateStr()
	{
		return mState.toString();
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
	 * Determines if the election process was already finished
	 * 
	 * @return true or false
	 */
	public boolean finished()
	{
		return (mState == ElectorState.ELECTED);
	}

	/**
	 * SIGNAL: This function ends the election by signaling WINNER to all cluster members.
	 * 		 It is only used by cluster managers, which won the election. 		
	 */
	private void distributeWINNER()
	{
		if(mParent.getHierarchyLevel().isHigherLevel()){
			if (mState == ElectorState.ELECTED){
				// get the size of the cluster
				int tKnownClusterMembers = mParent.countConnectedClusterMembers();
				
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
					Logging.log(this, "SENDWINNER()-START, electing cluster is " + mParent);
					Logging.log(this, "SENDWINNER(), cluster members: " + tKnownClusterMembers);
				}
		
				// HINT: the coordinator has to be already created here
	
				if (mParent.getCoordinator() != null){
					// create the packet
					ElectionWinner tElectionWinnerPacket = new ElectionWinner(mHRMController.getNodeL2Address(), mParent.getPriority(), mParent.getCoordinator().getCoordinatorID(), mParent.getCoordinator().toLocation() + "@" + HRMController.getHostName());
					
					// send broadcast
					//do the following but avoid unneeded updates: mParent.sendClusterBroadcast(tElectionWinnerPacket, true, SEND_ALL_ELECTION_PARTICIPANTS);
					
					int tSentPackets = 0;
					LinkedList<ComChannel> tChannels = mParent.getComChannels();
					for(ComChannel tComChannelToPeer : tChannels){
						/**
						 * is this announcement needed?
						 */
						if(!tComChannelToPeer.isSignaledAsWinner()){
							/**
							 * only send via established channels
							 */
							if(tComChannelToPeer.isOpen()){
								//Logging.err(this, "SENDING: " + tElectionWinnerPacket);
								tComChannelToPeer.sendPacket(tElectionWinnerPacket.duplicate());
								tSentPackets++;
							}
						}
					}
					
					/**
					 * account the broadcast if there was one
					 */
					if(tSentPackets > 0){
						tElectionWinnerPacket.accountBroadcast();
					}
	
				}else{
					Logging.warn(this, "Election has wrong state " + mState + " for signaling an ELECTION END, ELECTED expected");
					
					// set correct elector state
					setElectorState(ElectorState.ERROR);
				}
		
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
					Logging.log(this, "SENDWINNER()-END");
				}
			}else{
				// elector state is ELECTED
				Logging.warn(this, "Election state isn't ELECTING, we cannot finishe an election which wasn't started yet, error in state machine");
			}
		}else{
			// base hierarchy level: each candidate can conclude the winner of the broadcast domain by its own
		}
	}
	
	/**
	 * SIGNAL: ends the election by signaling RESIGN to all cluster members 		
	 */
	private void distributeRESIGN()
	{
		if(mParent.getHierarchyLevel().isHigherLevel()){
			if (mState == ElectorState.ELECTED){
				// get the size of the cluster
				int tKnownClusterMembers = mParent.countConnectedClusterMembers();
				
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
					Logging.log(this, "SENDRESIGN()-START, electing cluster is " + mParent);
					Logging.log(this, "SENDRESIGN(), cluster members: " + tKnownClusterMembers);
				}
		
				// create the packet
				ElectionResign tElectionResignPacket = new ElectionResign(mHRMController.getNodeL2Address(), mParent.getPriority(), mParent.toLocation() + "@" + HRMController.getHostName());
	
				// send broadcast
				//Logging.err(this, "SENDING: " + tElectionResignPacket);
				mParent.sendClusterBroadcast(tElectionResignPacket, true, SEND_ALL_ELECTION_PARTICIPANTS);
		
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
					Logging.log(this, "SENDRESIGN()-END");
				}
			}else{
				// elector state is ELECTED
				Logging.warn(this, "Election state isn't ELECTING, we cannot finishe an election which wasn't started yet, error in state machine");
			}
		}else{
			// base hierarchy level: each candidate can conclude the winner of the broadcast domain by its own
		}
	}

	/**
	 * SIGNAL: PriorityUpdate, but send it only if this really needed
	 * 
	 * @param pComChannel the comm. channel to which we want to send this update packet, "null" to send it to all cluster members
	 * @param pCause the cause for the call
	 */
	private void distributePRIRORITY_UPDATE(ComChannel pComChannel, String pCause)
	{
		if(mParent.isThisEntityValid()){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "SENDPRIOUPDATE()-START, cause=" + pCause);
				Logging.log(this, "SENDPRIOUPDATE(), cluster members: " + mParent.getComChannels().size());
			}
	
			int tSentPackets = 0;
			ElectionPriorityUpdate tElectionPriorityUpdatePacket = new ElectionPriorityUpdate(mHRMController.getNodeL2Address(), mParent.getPriority());
	
			if(pComChannel == null){
				// send broadcast
				Logging.log(this, "Distributing priority update: " + tElectionPriorityUpdatePacket);
				//do the following but avoid unneeded updates: mParent.sendClusterBroadcast(tElectionPriorityUpdatePacket, true, SEND_ALL_ELECTION_PARTICIPANTS);
				
				LinkedList<ComChannel> tChannels = mParent.getComChannels();
				for(ComChannel tComChannelToPeer : tChannels){
					/**
					 * is this priority update needed?
					 */
					if((tComChannelToPeer.getSignaledPriority().isUndefined()) || (!tComChannelToPeer.getSignaledPriority().equals(mParent.getPriority()))){
						/**
						 * only send via established channels
						 */
						if(tComChannelToPeer.isOpen()){
							tComChannelToPeer.sendPacket(tElectionPriorityUpdatePacket.duplicate());
							tSentPackets++;
						}
					}
				}
				
				/**
				 * hack: correct packet accounting here
				 * account the broadcast if there was one
				 */
				if(tSentPackets > 0){
					tElectionPriorityUpdatePacket.accountBroadcast();
				}else{
					synchronized (tElectionPriorityUpdatePacket.sCreatedPackets) {
						tElectionPriorityUpdatePacket.sCreatedPackets--;
					}
				}
			}else{
				/**
				 * is this priority update needed?
				 */
				if((pComChannel.getSignaledPriority().isUndefined()) || (!pComChannel.getSignaledPriority().equals(mParent.getPriority()))){
					// send explicit update
					Logging.log(this, "Distributing explicit priority update: " + tElectionPriorityUpdatePacket);
					pComChannel.sendPacket(tElectionPriorityUpdatePacket);
				}
			}
	
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "SENDPRIOUPDATE()-END");
			}
		}else{
			Logging.warn(this, "distributePRIRORITY_UPDATE() skipped because parent entity is already invalidated");
		}
	}

	/**
	 * SIGNAL: ElectionAlive, report itself as alive by signaling ALIVE to all cluster members
	 * 	
	 * @param pComChannel the comm. channel along the ElectionAlive should be sent
	 */
	public void sendALIVE(ComChannel pComChannel)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDALIVE()-START, electing cluster is " + mParent);
			Logging.log(this, "SENDALIVE(), cluster members: " + mParent.getComChannels().size());
		}

		// create the packet
		ElectionAlive tElectionAlivePacket = new ElectionAlive(mHRMController.getNodeL2Address(), mParent.getPriority());

		// send packet
		pComChannel.sendPacket(tElectionAlivePacket);

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDALIVE()-END");
		}
	}
	
	/**
	 * SIGNAL: ElectionAlive, report itself as alive by signaling ALIVE to all cluster members
	 */
	public void distributeALIVE()
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDALIVE()-START, electing cluster is " + mParent);
			Logging.log(this, "SENDALIVE(), cluster members: " + mParent.getComChannels().size());
		}

		// create the packet
		ElectionAlive tElectionAlivePacket = new ElectionAlive(mHRMController.getNodeL2Address(), mParent.getPriority());

		// send broadcast
		mParent.sendClusterBroadcast(tElectionAlivePacket, true, SEND_ALL_ELECTION_PARTICIPANTS);

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDALIVE()-END");
		}
	}

	/**
	 * This is the central function for updating election participations. It (de-)activates the participation for an election of a cluster manager. 
	 * 
	 * @param pComChannel the comm. channel towards the cluster head
	 * @param pState the new participation state
	 * @param pCauseForStateChange the cause for this state change 
	 */
	private void updateElectionParticipation(ComChannel pComChannel, boolean pState, String pCauseForStateChange)
	{
		Logging.log(this, "### Changing election participation to " + pState + " for comm. channel: " + pComChannel);
		
		if(!head()){
			if(pState){
				/**
				 * create the packet
				 */
				ElectionReturn tElectionReturnPacket = new ElectionReturn(mHRMController.getNodeL2Address(), pComChannel.getParent().getPriority());
	
				/**
				 * Update local link activation
				 */
				Logging.log(this, "  ..activating link(updateElectionParticipation): " + pComChannel + ", cause=" + "RETURN[" + tElectionReturnPacket.getOriginalMessageNumber() + "]\n   ^^^^" + pCauseForStateChange);
				pComChannel.setLinkActivationForElection(pState, "RETURN[" + tElectionReturnPacket.getOriginalMessageNumber() + "]\n   ^^^^" + pCauseForStateChange);
	
				/**
				 * Signal to peer
				 */
				//Logging.err(this, "SENDING: " + tElectionReturnPacket);
				pComChannel.sendPacket(tElectionReturnPacket);
			}else{
				/**
				 * create the LEAVE packet
				 */
				ElectionLeave tElectionLeavePacket = new ElectionLeave(mHRMController.getNodeL2Address(), pComChannel.getParent().getPriority());
	
				/**
				 * Update the list of active ClusterMembers:  (it represents the best choices per hier. level)
				 * 		=> remove this ClusterMember from the list because it's - in every case - not active anymore 
				 */ 
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
					Logging.log(this, "      ..losing active ClusterMember: " + mParent);
				}
				boolean tRemovedActiveClusterMember = removeActiveClusterMember(mParent, "LEAVE[" + tElectionLeavePacket.getOriginalMessageNumber() + "]\n   ^^^^" + pCauseForStateChange);
				if(tRemovedActiveClusterMember){
					Logging.log(this, "Having removed myself as active ClusterMember");
				}
				
				/**
				 * Update local link activation
				 */
				Logging.log(this, "  ..deactivating link(updateElectionParticipation): " + pComChannel+ ", cause=" + "LEAVE[" + tElectionLeavePacket.getOriginalMessageNumber() + "]\n   ^^^^" + pCauseForStateChange);
				pComChannel.setLinkActivationForElection(pState, "LEAVE[" + tElectionLeavePacket.getOriginalMessageNumber() + "]\n   ^^^^" + pCauseForStateChange);
	
				/**
				 * Signal to peer
				 */
				//Logging.err(this, "SENDING: " + tElectionLeavePacket);
				pComChannel.sendPacket(tElectionLeavePacket);
				
				/**
				 * Auto-join all elections if a coordinator left all elections
				 */
				if(mParent instanceof CoordinatorAsClusterMember){
					CoordinatorAsClusterMember tCoordinatorAsClusterMember = (CoordinatorAsClusterMember) mParent;
					if(!hasSiblingWithActiveElectionParticipation(tCoordinatorAsClusterMember)){
						eventCoordinatorLeftAllPossibleElections();
					}
				}
			}
		}else{
			Logging.err(this, "updateElectionParticipation() can only be called for ClusterMember instances, error in state machine, parent is: " + mParent);
		}
	}

	/**
	 * EVENT: a coordinator left all possible elections
	 */
	private void eventCoordinatorLeftAllPossibleElections()
	{
		if(mParent instanceof CoordinatorAsClusterMember){
			Logging.warn(this, "EVENT: coordinator left all possible elections");
		}else{
			Logging.err(this, "Expected a CoordinatorAsClustermember as parent, error in state machine, parent is: " + mParent);
		}
	}

	/**
	 * Checks if a CoordinatorAsClusterMember has still a sibling with active election participation
	 *  
	 * @param pRefCoordinatorAsClusterMember the reference CoordinatorAsClusterMember
	 * 
	 * @return true if siblings exist
	 */
	private boolean hasSiblingWithActiveElectionParticipation(CoordinatorAsClusterMember pRefCoordinatorAsClusterMember)
	{
		boolean tResult = false;
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
			Logging.log(this, "Checking for an active sibling of: " + pRefCoordinatorAsClusterMember);
		}

		LinkedList<CoordinatorAsClusterMember> tLevelClusterMembers = mHRMController.getAllCoordinatorAsClusterMembers(mParent.getHierarchyLevel().getValue());
		for(CoordinatorAsClusterMember tLevelClusterMember: tLevelClusterMembers){
			/**
			 * Be aware of multiple local coordinators at the same hierarchy level -> search only for alternative CoordinatorAsClusterMember instances belong to the parent instance!
			 * For example:
			 *  	- a node can have two coordinators, each for a separate link
			 *  	=> leads to two coordinators with own CoordinatorAsClusterMember instances, the amount depends on the clustering radius   			 
			 */
			if(pRefCoordinatorAsClusterMember.getClusterID().equals(tLevelClusterMember.getClusterID())){
				/**
				 * don't use the reference CoordinatorAsClusterMember
				 */ 
				if(!pRefCoordinatorAsClusterMember.equals(tLevelClusterMember)){
					// avoid null pointer deref.
					if(tLevelClusterMember.getComChannelToClusterHead() != null){
						/**
						 * Have we found a sibling with still active election participation?
						 */
						if(tLevelClusterMember.getComChannelToClusterHead().isLinkActiveForElection()){
							// update result
							tResult = true;
							
							// everything was done
							break;
						}
					}
				}else{
					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
						Logging.log(this, "      ..skipping reference to ourself");
					}
				}
			}else{
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
					Logging.log(this, "      ..skipping CoordinatorAsClusterMember instance of sibling coordinator found: " + tLevelClusterMember);
				}
			}
		}
		
		return tResult;
	}

	/**
	 * SIGNAL: ElectionLeave, report itself as a passive cluster member, should only be called for a ClusterMember
	 * 
	 * @param pCause the cause for this signaling
	 */
	private void leaveElection(String pCause)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDLEAVE()-START, electing cluster is " + mParent);
			Logging.log(this, "SENDLEAVE(), cluster members: " + mParent.getComChannels().size());
		}

		Logging.log(this, "Leaving election, cause=" + pCause);
		
		LinkedList<ComChannel> tChannels = mParent.getComChannels();
		if(tChannels.size() == 1){
			ComChannel tComChannelToPeer = mParent.getComChannels().getFirst();

			if(tComChannelToPeer.isLinkActiveForElection()){
				updateElectionParticipation(tComChannelToPeer, false, this + "::distributeLEAVE()\n   ^^^^" + pCause);
			}else{
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
					Logging.log(this, "    ..skipped LEAVE");
				}
			}
		}else{
			throw new RuntimeException("Found an invalid comm. channel list: " + tChannels);
		}
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDLEAVE()-END");
		}
	}

	/**
	 * SIGNAL: ElectionReturn, report itself as a returning active cluster member
	 * 
	 * @param pCause the cause for this signaling
	 */
	private void returnToElection(String pCause)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDRETURN()-START, electing cluster is " + mParent);
			Logging.log(this, "SENDRETURN(), cluster members: " + mParent.getComChannels().size());
		}

		if(mParent.isThisEntityValid()){
			LinkedList<ComChannel> tChannels = mParent.getComChannels();

			if(tChannels.size() == 1){
				ComChannel tComChannelToPeer = mParent.getComChannels().getFirst();
				
				if(!tComChannelToPeer.isLinkActiveForElection()){
					Logging.log(this, "   ..distributeRETURN() - enforcing a REACTIVATION of this link, cause=" + pCause);
					updateElectionParticipation(tComChannelToPeer, true, this + "::distributeRETURN()\n   ^^^^" + pCause);
				}else{
					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
						Logging.log(this, "    ..skipped RETURN");
					}
				}
			}else{
				if(tChannels.size() > 1){
					throw new RuntimeException("Found an invalid comm. channel list: " + tChannels);
				}else{
					Logging.warn(this, "Found empty channel list for: " + mParent + ", event cause=" + pCause);
				}
			}
		}else{
			Logging.log(this, "distributeRETURN() aborted because parent is invalid, cause=" + pCause);
		}

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDRETURN()-END");
		}
	}

	/**
	 * Leaves alternative elections with a lower priority than the ClusterMember behind the given comm. channel 
	 * This function is triggered if WINNER is received.
	 * 
	 * @param pSourceL2Address the L2Address of the source
	 * @param pSourcePriority the priority of the source
	 * @param pCause the cause for this call
	 */
	private void leaveAllWorseAlternativeElections(ComChannel pReferenceChannel, String pCause)
	{
		L2Address tRefL2Address = pReferenceChannel.getPeerL2Address();
		ElectionPriority tRefPriority = pReferenceChannel.getPeerPriority();
		Long tRefClusterID = pReferenceChannel.getRemoteClusterName().getClusterID();
		ControlEntity tRefParent = pReferenceChannel.getParent();
		
		// do this only for higher hierarchy levels!
		// at base hierarchy level, we have local redundant clusters covering the same bus (network interface)
		if(mParent.getHierarchyLevel().isHigherLevel()){
			/**
			 * AVOID multiple LEAVES
			 */
			synchronized (mNodeActiveClusterMemberships){
				LinkedList<ClusterMember> tActiveClusterMemberships = getParentCoordinatorActiveClusterMemberships();
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
					Logging.log(this, "      ..knowing these ACTIVE ClusterMember instances: " + tActiveClusterMemberships);
				}

				// get all possible elections
				LinkedList<CoordinatorAsClusterMember> tClusterMemberships = getParentCoordinatorClusterMemberships();
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
					Logging.log(this, "Distributing LEAVE, found ClusterMembers: " + tClusterMemberships);
				}
				
				// have we found elections?
				if(tClusterMemberships.size() > 0){					
					/**
					 * Iterate over all alternatives
					 */
					int tMemberCount = 0;
					for (CoordinatorAsClusterMember tClusterMembership : tClusterMemberships){
						if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
							Logging.log(this, "      ..### leaveAllWorseAlternativeElections() checks member: " + tClusterMembership);
						}
						
						tMemberCount++;

						/**
						 * Get the values of the cluster head of the alternative election, which are used during priority comparison
						 */
						ElectionPriority tAlternativeElectionClusterHeadPriority = ElectionPriority.create(this);
						if(tClusterMembership.getComChannelToClusterHead() != null){
							// get the priority of the cluster head of the alternative election
							tAlternativeElectionClusterHeadPriority = tClusterMembership.getComChannelToClusterHead().getPeerPriority(); 
						}
						
						/**
						 * don't leave this election: is the parent the alternative?
						 */ 
						if(!mParent.equals(tClusterMembership)){
							/**
							 * don't leave this election: is the source the coordinator of this alternative election process?
							 * HINT: this is only used to make sure we don't leave this election, under normal conditions this case should never happen (more than one local cluster member for the same cluster/coordinator can only happen for base hierarchy levels!)
							 */
							if(!tRefL2Address.equals(tClusterMembership.getCoordinatorNodeL2Address())){
								// get the elector
								Elector tAlternativeElection = tClusterMembership.getElector();

								// get the clusterID of the remote cluster to which this ClusterMember belongs
								Long tAlternativeElectionRemoteClusterID = null;
								if(tClusterMembership.getComChannelToClusterHead() != null){
									if(tClusterMembership.getComChannelToClusterHead().getRemoteClusterName() != null){
										tAlternativeElectionRemoteClusterID = tClusterMembership.getComChannelToClusterHead().getRemoteClusterName().getClusterID(); 
									}
								}										
								
								if(tAlternativeElection != null){
									/**********************************************************************************************************************************
									 * DO ONLY LEAVE elections with a lower priority -> incrementally leave all bad possible elections and find the best election
									 **********************************************************************************************************************************/
									if((!tClusterMembership.equals(tRefParent)) /* avoid that we compare a control entity with itself and decide by mistake that the priority is lower and we should leave this election */ && 
									   ((tAlternativeElectionRemoteClusterID == null) || (!tAlternativeElectionRemoteClusterID.equals(tRefClusterID))) /* avoid that we compare two local CoordinatorAsCluster instances, which belong to the same remote cluster*/ &&
									   (!tAlternativeElectionClusterHeadPriority.isUndefined()) /* the priority has to be already defined */ &&
									   (tAlternativeElection.hasClusterLowerPriorityThan(tRefL2Address, tRefPriority, IGNORE_LINK_STATE)) /* compare the two priorities */){

										/**
										 * Distribute "LEAVE" for the alternative election process
										 */
										if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
											Logging.log(this, "      ..LEAVING: " + tAlternativeElection);
										}                                            
										tAlternativeElection.leaveElection(this + "::leaveAllWorseAlternativeElections() for " + tMemberCount + "/" + tClusterMemberships.size() + " member [" + (tClusterMembership.getComChannelToClusterHead() != null ? tClusterMembership.getComChannelToClusterHead().getPeerL2Address() : "null") + ", ThisPrio: " + tAlternativeElectionClusterHeadPriority.getValue() + " < ReferencePrio: " + tRefPriority.getValue() + ", " + tRefL2Address + "]\n   ^^^^" + pCause);
									}else{
										if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
											Logging.log(this, "      ..NOT LEAVING: " + tAlternativeElection);
										}
										if(tAlternativeElectionClusterHeadPriority.isUndefined()){
											Logging.log(this, "leaveAllWorseAlternativeElections() aborted (undef. prio.) for " + tMemberCount + "/" + tClusterMemberships.size() + " member [" + (tClusterMembership.getComChannelToClusterHead() != null ? tClusterMembership.getComChannelToClusterHead().getPeerL2Address() : "null") + ", ThisPrio: " + tAlternativeElectionClusterHeadPriority.getValue() + " <> ReferencePrio: " + tRefPriority.getValue() + ", " + tRefL2Address + "]\n   ^^^^ " + pCause);
										}else{
											Logging.log(this, "leaveAllWorseAlternativeElections() aborted for " + tMemberCount + "/" + tClusterMemberships.size() + " member [" + (tClusterMembership.getComChannelToClusterHead() != null ? tClusterMembership.getComChannelToClusterHead().getPeerL2Address() : "null") + ", ThisPrio: " + tAlternativeElectionClusterHeadPriority.getValue() + " <> ReferencePrio: " + tRefPriority.getValue() + ", " + tRefL2Address + "]\n   ^^^^ " + pCause);
										}
									}
									/**********************************************************************************************************************************/
								}else{
									throw new RuntimeException("Found invalid elector for: " + tClusterMembership);
								}
							}else{
								Logging.log(this, "leaveAllWorseAlternativeElections() aborted (same cluster!) for " + tMemberCount + "/" + tClusterMemberships.size() + " member [ThisPrio: " + tAlternativeElectionClusterHeadPriority.getValue() + " <> ReferencePrio: " + tRefPriority.getValue() + ", " + tRefL2Address + "]\n   ^^^^ " + pCause);
								// we have found a local cluster member which belongs to the same cluster like we do
							}
						}else{
							Logging.log(this, "leaveAllWorseAlternativeElections() aborted (same entity!) for " + tMemberCount + "/" + tClusterMemberships.size() + " member [ThisPrio: " + tAlternativeElectionClusterHeadPriority.getValue() + " <> ReferencePrio: " + tRefPriority.getValue() + ", " + tRefL2Address + "]\n   ^^^^ " + pCause);
							// we have found this election process
						}
					}// for
				}else{
					// we haven't even found our parent as ClusterMember at this hierarchy level
				}
			}
		}
	}
	
	/**
	 * Leaves alternative elections with a lower priority than this ClusterMember.
	 * 
	 * @param pCause the cause for the call
	 */
	private void leaveAllWorseAlternativeElections(String pCause)
	{
		if(mParent.isThisEntityValid()){
			LinkedList<ComChannel> tChannels = mParent.getComChannels();
	
			if(tChannels.size() == 1){
				ComChannel tComChannelToCoordinator = tChannels.getFirst();
				
				leaveAllWorseAlternativeElections(tComChannelToCoordinator, pCause);
			}else{
				Logging.err(this, "leaveAllWorseAlternativeElections() found an unplausible amount of comm. channels: " + tChannels + ", call cause=" + pCause);
			}
		}else{
			Logging.warn(this, "leaveAllWorseAlternativeElections() because entity is already invalidated, cause=" + pCause);
		}
	}

	/**
	 * Checks for an election result
	 * 
	 * @param pCause the cause for this event
	 */
	private void checkElectionResult(String pCause)
	{
		boolean tWinnerCanBeDetermined = true;
		boolean DEBUG = false;
		
		if(DEBUG){
			Logging.log(this, "Checking for election winner.., cause=" + pCause);
		}
		
		LinkedList<ComChannel> tActiveClusterMembershipChannels = mParent.getActiveLinks();
		
		// OPTIMIZATION: check if we have found at least one active inferior coordinator
		if(tActiveClusterMembershipChannels.size() > 0){
			// OPTIMIZATION: do we know more than 0 external cluster members?
			if (mParent.countConnectedRemoteClusterMembers() > 0){
				
				/**
				 * Check if all needed priorities are known
				 */
				tWinnerCanBeDetermined = allPrioritiesKnown();
				
				/**
				 * Check if election is complete
				 */
				if (tWinnerCanBeDetermined){
					/**
					 * Check if we are the winner of the election
					 */
					if((hasHighestPriorityInTheSurrounding()) && (hasHighestPriorityInCluster("checkElectionResult()\n   ^^^^" + pCause))) {
						if(DEBUG){
							Logging.log(this, "	 ..I AM WINNER");
						}
						eventElectionWon("checkElectionResult()\n   ^^^^" + pCause);
					}else{
						if(DEBUG){
							Logging.log(this, "	 ..I HAVE LOST");
						}
						eventElectionLost("checkElectionResult()\n   ^^^^" + pCause);
					}
				}else{
					Logging.log(this, "  ..incomplete election");
					// election is incomplete: we are still waiting for some priority value(s)
				}
			}else{
				if(DEBUG){
					Logging.log(this, "  ..I AM WINNER because no external cluster member is known, known channels to cluster members are:" );
					Logging.log(this, "    ..: " + mParent.getComChannels());
				}
				eventElectionWon("checkElectionResult() - detected isolation\n   ^^^^" + pCause);
			}
		}else{
			/**
			 * no active inferior coordinator found -> coordinator instance is not needed anymore
			 */
			eventElectionLost("eventAllLinksInactive()");
		}
	}
	
	/**
	 * Central function for checking the local election result.
	 *    1.) It checks the local cluster manager if it is the winner or the loser of its cluster
	 *    2.) It checks the current elector if is the winner or the loser of its cluster
	 * 
	 * @param pCause the cause for this call
	 */
	private void checkLocalElection(String pCause)
	{		
		/**
		 * Check the election result of the local cluster on the hierarchy level of the current elector
		 */
		boolean tThisElectorAlreadyChecked = false;
		if(mParent.getHierarchyLevel().isHigherLevel()){
			LinkedList<Cluster> tLocalClusters = mHRMController.getAllClusters(mParent.getHierarchyLevel());
			for(Cluster tCluster : tLocalClusters){
				Elector tClusterElector = tCluster.getElector();
						
				/**
				 * Recalculate an election result	
				 */
				tClusterElector.checkElectionResult(this + "::recheckLocalClusterIsAllowedToWin()\n   ^^^^" + pCause);
				
				if(tCluster.equals(mParent)){
					tThisElectorAlreadyChecked = true;
				}
			}
		}else{
			// base hierarchy level, which doesn't have to process this
		}

		/**
		 * Check the election result of the elector
		 */
		// OPTIMIZATION: do not do the work twice 
		if(!tThisElectorAlreadyChecked){
			checkElectionResult(this + "::checkLocalElection()\n   ^^^^" + pCause);		
		}
	}
	
	/**
	 * Return to alternative elections if the current parent is an active ClusterMember for this node.
	 * This function is triggered if a RESIGN is received from a remote coordinator or the local ClusterMember is reset
	 * 
	 * @param pCause the cause for this call
	 */
	private void returnToAlternativeElections(String pCause)
	{
		// only do this for a higher hierarchy level! at base hierarchy level we have local redundant cluster covering the same bus (network interface)
		if(mParent.getHierarchyLevel().isHigherLevel()){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "Returning to elections..");
			}
			/**
			 * AVOID multiple RETURNS
			 */
			synchronized (mNodeActiveClusterMemberships){
				LinkedList<ClusterMember> tActiveClusterMemberships = getParentCoordinatorActiveClusterMemberships();
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
					Logging.log(this, "      ..knowing these ACTIVE ClusterMember instances: " + tActiveClusterMemberships);
				}
				
				/**
				 * ONLY PROCEED IF THE PARENT IS AN ACTIVE ClusterMember!
				 */
				if(tActiveClusterMemberships.contains(mParent)){
					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
						Logging.log(this, "      ..is ACTIVE ClusterMember");
					}
					
					/**
					 * Mark/remove this ClusterMember (best choice election) because it's not active anymore
					 */ 
					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
						Logging.log(this, "      ..lost active (best choice) ClusterMember: " + mParent);
					}
					
					// get all possible elections on this hierarchy level
					LinkedList<CoordinatorAsClusterMember> tClusterMemberships = getParentCoordinatorClusterMemberships();
					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
						Logging.log(this, "Distributing RETURN, found CoordinatorAsClusterMembers: " + tClusterMemberships);
					}
					
					// have we found elections?
					if(tClusterMemberships.size() > 0){
						boolean tStillAnAlternativeElectionWithValidCoordinatorExists = false;
						
						/**************************************************************************************************************************************
						 * Search for an alternative election in which this node is still an active participant and the election has found a valid coordinator 
						 **************************************************************************************************************************************/
						for (CoordinatorAsClusterMember tClusterMembership : tClusterMemberships){
							/**
							 * don't check the same election!
							 */ 
							if(!mParent.equals(tClusterMembership)){
								// check if this election has a valid coordinator
								if(tClusterMembership.hasClusterValidCoordinator()){
									if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
										Logging.log(this, "      ..lost active (best choice) ClusterMember: " + mParent);
										Logging.log(this, "        ..alternative (best choice) ClusterMember is: " + tClusterMembership);
									}
									/**
									 * check if we are already active member of the election
									 */
									if(!tClusterMembership.getComChannelToClusterHead().isLinkActiveForElection()){
										if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
											Logging.log(this, "      ..RETURN to: " + tClusterMembership);
										}
										tClusterMembership.getElector().returnToElection(this + "::returnToAlternativeElections()_1\n   ^^^^" + pCause);
										if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
											Logging.log(this, "      ..leaving worse alternative elections in relation to: " + tClusterMembership);
										}
										tClusterMembership.getElector().leaveAllWorseAlternativeElections(this + "::returnToAlternativeElections()_1\n   ^^^^" + pCause);
									}
									if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
										Logging.log(this, "        ..adding as new superior coordinator, cause=" + this + "::returnToAlternativeElections()\n   ^^^^" + pCause);
									}
									tStillAnAlternativeElectionWithValidCoordinatorExists = true;
									tClusterMembership.getElector().addActiveClusterMember(this + "::returnToAlternativeElections()\n   ^^^^" + pCause);
									break;
								}
							}
						}// for
						if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
							Logging.log(this, "      ..still an alternative election with valid coordinator exists: " + tStillAnAlternativeElectionWithValidCoordinatorExists);
						}

						/**********************************************************
						 * Return to all alternative elections 
						 **********************************************************/
						if(!tStillAnAlternativeElectionWithValidCoordinatorExists){
							/**
							 * Iterate over all alternatives
							 */
							for (CoordinatorAsClusterMember tClusterMembership : tClusterMemberships){
								/**
								 * don't return to the same election!
								 */ 
								if(!mParent.equals(tClusterMembership)){
									// are we the coordinator (so, the coordinator is on this node!)?
									//if(!mHRMController.getNodeL2Address().equals(tLevelClusterMember.getCoordinatorNodeL2Address())){
										// get the elector
										Elector tAlternativeElection = tClusterMembership.getElector();
										if(tAlternativeElection != null){
											/**
											 * Distribute "RETURN" for the alternative election process
											 */
												if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
													Logging.log(this, "      ..RETURN to: " + tAlternativeElection);
												}
											tAlternativeElection.returnToElection(this + "::returnToAlternativeElections()_2\n   ^^^^" + pCause);
										}else{
											throw new RuntimeException("Found invalid elector for: " + tClusterMembership);
										}
//									}else{
//										Logging.log(this, "      ..skipping reference to local cluster member which belongs to the same cluster like we do: " + tLevelClusterMember + ", coordinator=" + tLevelClusterMember.getCoordinatorNodeL2Address());
//									}
								}else{
									if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
										Logging.log(this, "      ..skipping reference to ourself");
									}
								}
							}// for
						}// if
					}else{
						Logging.err(this, "We haven't even found our parent as ClusterMember at hierarchy level: " + mParent.getHierarchyLevel().getValue());
					}
				}else{
					Logging.log(this, "returnToAlternativeElections() stops here because parent is not an ACTIVE ClusterMember, parent is: " + mParent);
				}
			}
		}
	}
	
	/**
	 * Leave/return alternative elections as reaction on new peer priority in order to correct the distributed election.
	 * This function reacts on delayed priority updates and is only used for ClusterMember instances.
	 * 
	 * @param pComChannel the comm. channel for which a new priority (with influence on the current election result) was received
	 * @param pPacket the packet which caused this call
	 */
	private void leaveReturnOnNewPeerPriority(ComChannel pComChannel, SignalingMessageElection pCausingPacket)
	{
		// only do this for a higher hierarchy level! at base hierarchy level we have local redundant cluster covering the same bus (network interface)
		if(mParent.getHierarchyLevel().isHigherLevel()){
			//if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
				Logging.log(this, "leaveReturnOnNewPeerPriority() by " + pCausingPacket + " for: " + pComChannel);
			//}
			
			if((mParent instanceof Cluster) || (mParent.getComChannelToClusterHead().isLinkActiveForElection())){
				/**
				 * AVOID multiple LEAVES/RETURNS
				 */
				synchronized (mNodeActiveClusterMemberships){
					/***********************************
					 * AUTO_LEAVE: if we are a simple cluster member: should we deactivate this election participation?
					 ***********************************/
					leaveWorseElection(pComChannel, this + "::leaveReturnOnNewPeerPriority()_0 for " + pCausingPacket);
					
					if(finished()){
						/***********************************
						 ** ELECTED: React similar to a received WINNER/RESIGN if the election is already finished
						 ***********************************/
						
						/**
						 * Do we belong to an active cluster with an existing (remote) coordinator?
						 */
						if(mParent.hasClusterValidCoordinator()){
							/**
							 * We behave like we would do if we receive a WINNER packet
							 */
							if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
								Logging.log(this, "      ..leave all alternative election processes with a lower priority than the peer");
							}
							leaveAllWorseAlternativeElections(pComChannel, this + "::leaveReturnOnNewPeerPriority()_1 for " + pCausingPacket);
						}else{
							/**
							 * We behave like we would do if we receive a RESIGN packet
							 */
							//we skip returnToAlternativeElections(pComChannel.getPeerL2Address(), pComChannel.getPeerPriority()) here because this step was already processed based on the already received RESIGN, a priority update doesn't change anything
						}
					}else{
						if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
							Logging.log(this, "    ..leaveReturnOnNewPeerPriority() for unfinished election");
						}
						
						/***********************************
						 * NOT ELECTED:
						 ***********************************/
						LinkedList<ClusterMember> tActiveClusterMemberships = getParentCoordinatorActiveClusterMemberships();

						/**
						 * ONLY PROCEED IF AN ACTIVE ClusterMember is already known
						 */
						if(tActiveClusterMemberships.size() > 0){
							/**
							 * Iterate over all known active ClusterMember entries
							 */ 
							for(ClusterMember tActiveClusterMembership : tActiveClusterMemberships){
								Elector tElectorClusterMember = tActiveClusterMembership.getElector();
								
								/**
								 * don't leave this election: is the parent the alternative?
								 */ 
								if(!mParent.equals(tActiveClusterMembership)){
									if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
										Logging.log(this, "    ..leaveReturnOnNewPeerPriority() triggers leaveAllWorseAlternativeElections for all alternative election processes in relation to foreign election: " + tElectorClusterMember);
									}
									tElectorClusterMember.leaveAllWorseAlternativeElections(this + "::leaveReturnOnNewPeerPriority()_2 for " + pCausingPacket);
								}
							}								
						}else{
							// no active ClusterMember is known and the priority update affects only the current ClusterMember
						}
					}					
				}
			}else{
				// link is not active
			}
		}
	}

	/**
	 * Leave for an active better ClusterMember the given election.
	 * If we are a simple cluster member, should we deactivate this election participation for an already selected better choice?
	 * 
	 * @param pComChannel the comm. channel towards the reference cluster
	 * @param pCause the cause for this call
	 */
	private void leaveWorseElection(ComChannel pComChannel, String pCause)
	{
		if(!head()){
			Logging.log(this, "leaveForActiveBetterClusterMembership() for: " + pComChannel + ",cause=" + pCause);
			
			LinkedList<ClusterMember> tActiveClusterMemberships = getParentCoordinatorActiveClusterMemberships();

			if(!tActiveClusterMemberships.isEmpty()){
				if(mParent instanceof CoordinatorAsClusterMember){
					CoordinatorAsClusterMember tThisCoordinatorAsClusterMember = (CoordinatorAsClusterMember)mParent;
					
					// get the first active ClusterMember
					CoordinatorAsClusterMember tActiveClusterMembership = (CoordinatorAsClusterMember)tActiveClusterMemberships.getFirst();
					// get its elector
					Elector tActiveClusterMemberShipElector = tActiveClusterMembership.getElector();
					
					/**
					 * abort if the active ClusterMember and this ClusterMember belong to the same remote cluster 
					 * HINT: we cannot not simply compare the local instances and have to use the remote cluster ID in order to support multiple local L0 coordinators which belong to the same remote cluster
					 */ 
					if(!tThisCoordinatorAsClusterMember.getRemoteClusterName().getClusterID().equals(tActiveClusterMembership.getRemoteClusterName().getClusterID())){
						/**
						 * Plausibility check: does it really have a valid coordinator?
						 */
						if(tActiveClusterMembership.hasClusterValidCoordinator()){
							/**
							 * is the currently active ClusterMembership (with a valid coordinator) not worse than this ClusterMembership? -> so, we have already found a better superior cluster/coordinator!
							 *     -> we have to deactivate the participation to this election
							 */
							if(!tActiveClusterMemberShipElector.hasClusterLowerPriorityThan(pComChannel.getPeerL2Address(), pComChannel.getPeerPriority(), IGNORE_LINK_STATE)){
								Logging.log(this, "leaveForActiveBetterClusterMembership() triggers the LEAVING of election");
								// deactivate this ClusterMembership
								updateElectionParticipation(pComChannel,  false, this + "::leaveForActiveBetterClusterMembership() for better active cluster membership: " + tActiveClusterMemberShipElector + "\n   ^^^^" + pCause);
							}else{
								Logging.log(this, "leaveForActiveBetterClusterMembership() DOES NOT trigger the LEAVING of election");
							}
						}else{
							Logging.err(this, "Active ClusterMember does not have a valid coordinator, error in state machine, parent is: " + mParent);
						}
					}else{
						Logging.log(this, "leaveForActiveBetterClusterMembership() aborted because active ClusterMember belongs to the same remote cluster than this ClusterMember");
					}
				}else{
					Logging.log(this, "leaveForActiveBetterClusterMembership() aborted because parent is a simple ClusterMember on base hierarchy level");
				}
			}
		}
	}

	/**
	 * EVENT: sets the local node as coordinator for the parent cluster.
	 * 
	 * @param pCause the cause for this event
	 */
	private synchronized void eventElectionWon(String pCause)
	{
		if ((!isWinner()) || (!finished())){
			Logging.log(this, "ELECTION WON for cluster " + mParent +", cause=" + pCause);
			
			// mark as election winner
			mElectionWon = true;
			
			synchronized (mResultChangeCauses) {
				mResultChangeCauses.add("WON <== " + pCause);
			}

			// set correct elector state
			setElectorState(ElectorState.ELECTED);

			// is the parent the cluster head?
			if(head()){
				// get the coordinator from the parental cluster
				Coordinator tCoordinator = mParent.getCoordinator();
				if (tCoordinator == null){
					Cluster tParentCluster = (Cluster)mParent;
					
					Logging.log(this, "    ..creating new coordinator at hierarch level: " + mParent.getHierarchyLevel().getValue());
					
					// create new coordinator instance
					tCoordinator = new Coordinator(mHRMController, tParentCluster);
				}else{
					if(tCoordinator.isThisEntityValid()){
						Logging.log(this, "Cluster " + mParent + " has already a coordinator");
					}
				}
	
				Logging.log(this, "    ..coordinator is: " + tCoordinator);
				
				if(tCoordinator != null){
					// send WINNER in order to signal all cluster members that we are the coordinator
					distributeWINNER();
		
					// trigger event "announced" for the coordinator
					tCoordinator.eventAnnouncedAsCoordinator();
				}
			}else{
				Logging.log(this, "We have won the election, parent isn't the cluster head: " + mParent + ", waiting for cluster head of alternative cluster");
			}
		}else{
			/**
			 * we have re-won the election but since the last election turn a new cluster member joined the election and doen't know yet our own priority!?
			 */
			distributePRIRORITY_UPDATE(BROADCAST, this + "::eventElectionWon()\n   ^^^^cause=" + pCause);
			
			Logging.warn(this, "Cluster " + mParent + " has still a valid and known coordinator");
		}
	}

	/**
	 * EVENT: sets the local node as simple cluster member.
	 * 
	 * @pCause the cause for this event
	 */
	private synchronized void eventElectionLost(String pCause)
	{
		Logging.log(this, "ELECTION LOST for cluster " + mParent +", cause=" + pCause);
	
		// store the old election result
		boolean tWasFormerWinner = mElectionWon;
		
		// mark as election loser
		mElectionWon = false;
		
		synchronized (mResultChangeCauses) {
			mResultChangeCauses.add("LOST <== " + pCause);
		}

		// set correct elector state
		setElectorState(ElectorState.ELECTED);
		
		// have we been the former winner of this election?
		if (tWasFormerWinner){
			Logging.log(this, "ELECTION LOST BUT WE WERE THE FORMER WINNER");

			/**
			 * TRIGGER: invalidate the local coordinator because it was deselected by another coordinator
			 */
			if(head()){
				// send RESIGN in order to signal all cluster members that we are no longer the coordinator
				distributeRESIGN();

				Coordinator tCoordinator = mParent.getCoordinator();
				if (tCoordinator != null){
					/**
					 * Invalidate the coordinator
					 * HINT: this call triggers also a call to Coordinator::Cluster::Elector::eventInvalidation()
					 */
					Logging.log(this, "     ..invalidating the coordinator role of: " + tCoordinator);
					tCoordinator.eventCoordinatorRoleInvalid(this + "::eventElectionLost()" + "\n   ^^^^" + pCause);
				}else{
					Logging.log(this, "We were the former winner of the election and the coordinator is already invalid");
				}
			}else{
				// we are not the cluster header, so we can't be the coordinator
			}
		}
	}

	/**
	 * EVENT: a cluster member left the election process
	 * 
	 * @param pComChannel the communication channel to the cluster member which left the election
	 * @param pLeavePacket the received packet
	 */
	private void eventReceivedLEAVE(ComChannel pComChannel, ElectionLeave pLeavePacket)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "EVENT: cluster member left election by packet: " + pLeavePacket + " via: " + pComChannel);
		}

		// check if the link state has changed	
		if(pComChannel.isLinkActiveForElection()){
			/**
			 * deactivate the link for the remote cluster member
			 */
			if(head()){
				Logging.log(this, "  ..deactivating link(eventReceivedLEAVE): " + pComChannel);
				pComChannel.setLinkActivationForElection(false, "LEAVE[" + pLeavePacket.getOriginalMessageNumber() + "] received");

				// checkElectionResult() will be called at the end of the central handleMessage function
			}else{
				Logging.err(this, "Received as cluster member a LEAVE from: " + pComChannel);
			}
		}
	}
	
	/**
	 * EVENT: a cluster member returned to the election process
	 * 
	 * @param pComChannel the communication channel to the cluster member which returned to the election
	 * @param pReturnPacket the received packet
	 */
	private void eventReceivedRETURN(ComChannel pComChannel, ElectionReturn pReturnPacket)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "EVENT: cluster member returned: " + pComChannel);
		}
		
		// check if the link state has changed	
		if(!pComChannel.isLinkActiveForElection()){
			/**
			 * activate the link for the remote cluster member 
			 */
			if(head()){
				Logging.log(this, "  ..activating link(eventReceivedRETURN): " + pComChannel);
				pComChannel.setLinkActivationForElection(true, "RETURN[" + pReturnPacket.getOriginalMessageNumber() + "] received");

				// are we the winner?
//				if(isWinner()){
					/**
					 * Trigger : reelect
					 */
					reelect("eventReceivedRETURN() from [peerPrio=" + pComChannel.getPeerPriority().getValue() + "]" + pComChannel);
//				}else{
//					// we weren't the previous winner and the joined member won't change this
//					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
//						Logging.log(this, "   ..we weren't the previous winner and the joined member won't change this, ignoring this RETURN: " + pReturnPacket);
//					}
//				}
			}else{
				Logging.err(this, "Received as cluster member a RETURN from: " + pComChannel);
			}
		}
	}
	
	/**
	 * EVENT: A remote coordinator was announced and we are not the coordinator. 
	 * 
	 * @param pComChannel the comm. channel from where the packet was received
	 * @param pAnnouncePacket the packet itself
	 */
	private void eventReceivedWINNER(ComChannel pComChannel, ElectionWinner pAnnouncePacket)
	{
		Logging.log(this, "EVENT: WINNER: " + pAnnouncePacket + " via: " + pComChannel);
		
		if(!head()){
			Logging.log(this, "    ..we are a cluster member");
			
			if(mParent.hasClusterValidCoordinator()){
				Logging.warn(this, "Redundant packet: " + pAnnouncePacket);
			}
			
			LinkedList<ClusterMember> tActiveClusterMemberships = getParentCoordinatorActiveClusterMemberships();

			/**
			 * Continue processing if:
			 * 		a.) the link is active
			 * 		b.) we don't have a valid active ClusterMember for this hierarchy level at the moment 
			 */
			if((pComChannel.isLinkActiveForElection()) || (tActiveClusterMemberships == null) || (tActiveClusterMemberships.isEmpty())){
				Logging.log(this, "    ..we received the ANNOUNCE-WINNER via an active link, packet=" + pAnnouncePacket);

				if (!pComChannel.isLinkActiveForElection()){
					Logging.log(this, "    ..found a possible superior coordinator, enforcing REACTIVATION of this link, packet=" + pAnnouncePacket);
					updateElectionParticipation(pComChannel, true, this + "::eventReceivedWINNER()\n   ^^^^announce packet=" + pAnnouncePacket);					
				}
				
				// does the previous active ClusterMember for this hier. level has a lower priority than the new candidate?
				if((tActiveClusterMemberships == null) || (tActiveClusterMemberships.isEmpty()) || 
				   (tActiveClusterMemberships.getFirst().getElector().hasClusterLowerPriorityThan(pComChannel.getPeerL2Address(), pComChannel.getPeerPriority(), IGNORE_LINK_STATE)) || // the new ClusterMember is the better choice?
				   ((tActiveClusterMemberships.getFirst().getComChannelToClusterHead().getPeerL2Address().equals(pComChannel.getPeerL2Address()) /* both have the coordinator at the same node? */) && (mParent.getHierarchyLevel().getValue() == 1 /* this exception is only possible for hierarchy level 1 because two L0 coordinator are allowed to e active ClusterMember simultaneously */))){
					addActiveClusterMember(this + "::eventReceivedWINNER() for " + pAnnouncePacket);
				}else{
					Logging.log(this, "### Avoid to set this entity as active ClusterMember, the list of active Clustermembers is: ");
					for(ClusterMember tClusterMember :  tActiveClusterMemberships){
						Logging.log(this, "   .." + tClusterMember);
					}
				}
				
				// leave all alternative election processes with a lower priority than the peer
				leaveAllWorseAlternativeElections(pComChannel, this + "::eventReceivedWINNER() for " + pAnnouncePacket);

			}else{
				Logging.log(this, "    ..we received the ANNOUNCE via an inactive link");
			}

			// mark this cluster as active
			mParent.setClusterWithValidCoordinator(true);
	
			// trigger: superior coordinator available	
			mParent.eventClusterCoordinatorAvailable(pAnnouncePacket.getSenderName(), pAnnouncePacket.getCoordinatorID(), pComChannel.getPeerL2Address(), pAnnouncePacket.getCoordinatorDescription());
			
			// checkElectionResult() will be called at the end of the central handleMessage function
		}else{
			throw new RuntimeException("Got an ANNOUNCE as cluster head");
		}
	}
	
	/**
	 * EVENT: the remote (we are not the coordinator!) coordinator resigned
	 * 
	 * @param pComChannel the comm. channel from where the packet was received
	 * @param pResignPacket the packet itself
	 */
	private void eventReceivedRESIGN(ComChannel pComChannel, ElectionResign pResignPacket)
	{
		Logging.log(this, "EVENT: resign: " + pResignPacket);

		if(!head()){
			Logging.log(this, "    ..we are a cluster member");

			/**
			 * Have we lost the coordinator of an election for which we are an active member? 
			 */
			if(pComChannel.isLinkActiveForElection()){
				Logging.log(this, "    ..we received the RESIGN via an ACTIVE LINK");

				// return to best alternative election process because we have lost the active superior coordinator on this hierarchy level
				returnToAlternativeElections(this + "::eventReceivedRESIGN() for " + pResignPacket);

				// mark/store as inactive ClusterMember
				removeActiveClusterMember(mParent, this + "::eventReceivedRESIGN() for " + pResignPacket);
			}	

			// mark this cluster as active
			mParent.setClusterWithValidCoordinator(false);

			// fake (for reset) trigger: superior coordinator available	
			mParent.eventClusterCoordinatorAvailable(pResignPacket.getSenderName(), -1, pComChannel.getPeerL2Address(), "N/A");
		}else{
			throw new RuntimeException("Got a RESIGN as cluster head");
		}
	}

	/**
	 * EVENT: priority update
	 * 
	 * @param pComChannel the comm. channel from where the packet was received
	 * @param pElectionPriorityUpdatePacket the priority update packet
	 * @param pOldPeerPriority the old peer priority
	 * 
	 * @return true if the new priority could have influence on the election result
	 */
	private boolean eventReceivedPRIORITY_UPDATE(ComChannel pComChannel, ElectionPriorityUpdate pElectionPriorityUpdatePacket, ElectionPriority pOldPeerPriority)
	{
		boolean tNewPriorityCouldInfluenceElectionResult = false; 
		
		// get the priority of the sender
		ElectionPriority tSenderPriority = pComChannel.getPeerPriority();
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "EVENT: priority update by " + pElectionPriorityUpdatePacket + " via: " + pComChannel);
		}

		/**
		 * Are we a ClusterMember and received an update from the local Cluster or are we the Cluster and received an update from the local ClusterMember? 
		 * 		-> we always have the same priority as the sending Cluster/ClusterMember -> no influence on the election result -> ignore this
		 */
		if (pComChannel.toRemoteNode()){
			/**
			 * React only if the link is active (for L1+)
			 */
			if(pComChannel.isLinkActiveForElection()){
				if(!pOldPeerPriority.isUndefined()){
					/**
					 * Have we already won the election and the new priority still lower than ours?
					 */
					if(isWinner()){
						// do we have the higher priority?
						if (havingHigherPrioriorityThan(pComChannel, CHECK_LINK_STATE)){
							if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
								Logging.log(this, "eventReceivedPRIORITY_UPDATE(): remote priority " + tSenderPriority.getValue() + " is lower than local " + mParent.getPriority().getValue() + " and we are already the election winner");
							}
						
							/**
							 * We (still) have the highest priority -> nothing to change here
							 */
							// ..
						}else{
							/**
							 * New received peer priority could influence the election result
							 */
							tNewPriorityCouldInfluenceElectionResult = true;
						}
					}else{
						// do we have the higher priority?
						if (havingHigherPrioriorityThan(pComChannel, CHECK_LINK_STATE)){
							if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
								Logging.log(this, "eventReceivedPRIORITY_UPDATE(): remote priority " + tSenderPriority.getValue() + " is lower than local " + mParent.getPriority().getValue() + " and we lost the last election, triggered by: " + pElectionPriorityUpdatePacket);
							}
							/**
							 * New received peer priority could influence the election result
							 */
							tNewPriorityCouldInfluenceElectionResult = true;
						}
					}
				}else{
					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
						Logging.log(this, "eventReceivedPRIORITY_UPDATE(): remote priority " + tSenderPriority.getValue() + " is the FIRST VALID priority");
					}
					/**
					 * New received peer priority could influence the election result
					 */
					tNewPriorityCouldInfluenceElectionResult = true;
				}
			}else{
				// link is inactive -> no influence on the election result
			}			
		}else{
			// prio. update was received from a local entity -> no influence on the election result
		}
		
		if((tNewPriorityCouldInfluenceElectionResult) && (mParent.getHierarchyLevel().isHigherLevel())){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "eventReceivedPRIORITY_UPDATE() triggers a re-election");
			}

			/**
			 * Trigger: election
			 */
			if (head()){
				reelect("eventReceivedPRIORITY_UPDATE(): received new priority " + pComChannel.getPeerPriority().getValue() + " from " + pComChannel);
			}
		}else{
			/**
			 * If the election wasn't finished yet, maybe all needed priorities are available now and the election could be finished.
			 */
			if((tNewPriorityCouldInfluenceElectionResult) || (!finished())){
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
					Logging.log(this, "   ..checking for election winner, triggered by: " + pElectionPriorityUpdatePacket);
				}
			}			
		}
		
		// checkElectionResult() will be called at the end of the central handleMessage function

		return tNewPriorityCouldInfluenceElectionResult;
	}

	/**
	 * Check if we are the elector of a cluster head
	 * 
	 * @return true or false
	 */
	private boolean head()
	{
		return (mParent instanceof Cluster);
	}
	
	/**
	 * Returns if this elector has the highest priority in the surrounding.
	 *  
	 * @return true of false
	 */	
	public boolean hasHighestPriorityInTheSurrounding()
	{
		boolean tResult = true;
		boolean DEBUG = false;
		
		if(DEBUG){
			Logging.log(this, "Checking if election win is allowed..");
		}
		
		if(mParent.getHierarchyLevel().isHigherLevel()){
			LinkedList<CoordinatorAsClusterMember> tAllClusterMemberships = mHRMController.getAllCoordinatorAsClusterMembers(mParent.getHierarchyLevel().getValue());
			
			if(DEBUG){
				for(CoordinatorAsClusterMember tCoordinatorAsClusterMember : tAllClusterMemberships){
					Logging.log(this, "       ..found known CoordinatorAsClusterMember instance: " + tCoordinatorAsClusterMember);
					Logging.log(this, "         ..channel to head: " + tCoordinatorAsClusterMember.getComChannelToClusterHead());
					Logging.log(this, "         ..valid coordinator: " + tCoordinatorAsClusterMember.hasClusterValidCoordinator());
					Logging.log(this, "         ..elector: " + tCoordinatorAsClusterMember.getElector());
				}
			}
					
			/**
			 * only proceed if at least one cluster membership exists
			 */
			if(tAllClusterMemberships.size() > 0){
				/**
				 * Iterate over all known active ClusterMember entries
				 */ 
				for(CoordinatorAsClusterMember tCoordinatorAsClusterMember : tAllClusterMemberships){
					/**
					 * Only proceed if the membership is still valid
					 */
					if(tCoordinatorAsClusterMember.isThisEntityValid()){
						/**
						 * Only proceed for memberships of clusters with valid coordinator
						 */
						if(tCoordinatorAsClusterMember.hasClusterValidCoordinator()){
							/**
							 * Only proceed for memberships of foreign clusters
							 */
							if(tCoordinatorAsClusterMember.isRemoteCluster()){
								Elector tElectorClusterMember = tCoordinatorAsClusterMember.getElector();
								ElectionPriority tCoordinatorAsClusterMemberPriority = tCoordinatorAsClusterMember.getPriority();
								
								/**
								 * Only proceed if the remote cluster has a higher priority
								 */
								if(DEBUG){
									Logging.log(this, "   ..checking if Cluster of ClusterMember " + tCoordinatorAsClusterMember + " has lower priority than local priority: " + tCoordinatorAsClusterMemberPriority.getValue() + " < " + mParent.getPriority().getValue() + "?");
									Logging.log(this, "   ..comm. channel is: " + tCoordinatorAsClusterMember.getComChannelToClusterHead());
								}
	
								if(!tElectorClusterMember.hasClusterLowerPriorityThan(mHRMController.getNodeL2Address(), mParent.getPriority(), IGNORE_LINK_STATE)){
									if(DEBUG){
										Logging.log(this, "      ..NOT ALLOWED TO WIN because alternative better coordinator in the surrounding exists: " + tElectorClusterMember);
									}
									tResult = false;
									break;
								}
							}
						}else{
							// cluster has no valid coordinator
						}
					}else{
						// entity is already invalidated
					}
				}								
			}else{
				if(DEBUG){
					// no active ClusterMember is known and the Cluster/ClusterMember is allowed to win
					Logging.log(this, "       ..no active ClusterMember is known and the Cluster/ClusterMember is allowed to win");
				}
			}
		}else{
			// it's an L0 Cluster/ClusterMember -> all of them are allowed to win
		}
		
		if(DEBUG){
			Logging.log(this, "   ..isAllowedToWin() result: " + tResult);
		}
				
		return tResult;
	}
	
	/**
	 * Checks if all needed priority values are known
	 * 
	 * @return true or false
	 */
	private boolean allPrioritiesKnown()
	{
		boolean tResult = true;
		boolean DEBUG = false;
		
		LinkedList<ComChannel> tActiveClusterMembershipChannels = mParent.getActiveLinks();
		for(ComChannel tComChannel : tActiveClusterMembershipChannels) {
			ElectionPriority tPriority = tComChannel.getPeerPriority();
			
			/**
			 * Only external cluster members are interesting.
			 * The priority of Local cluster members is always known.
			 */
			if(tComChannel.toRemoteNode()){								
				/**
				 * are we still waiting for the Election priority of some cluster member?
				 */
				if ((tPriority == null) || (tPriority.isUndefined())){
					if(DEBUG){
						Logging.log(this, "		   ..missing peer priority for: " + tComChannel);
					}
					
					// election is incomplete
					tResult = false;
				
					// leave the loop because we already know that the election is incomplete
					break;
				}
			}
		}
		
		return tResult;
	}
	
	/**
	 * Checks if this elector has the highest priority in this cluster.
     * Iterates over all cluster members and searches for a higher priority
	 * 
	 * @return true or false
	 */
	private boolean hasHighestPriorityInCluster(String pCause)
	{
		boolean tResult = true;
		boolean DEBUG = false;
		ComChannel tBetterCandidate = null;
		long tBetterCandidatePriority = -2;
		
		/**
		 */
		if(DEBUG){
			Logging.log(this, "hasHighestPriority() searches for a higher priority...");
		}
		LinkedList<ComChannel> tActiveClusterMembershipChannels = mParent.getActiveLinks();
		for(ComChannel tComChannel : tActiveClusterMembershipChannels) {
			ElectionPriority tPriority = tComChannel.getPeerPriority();
			
			/**
			 * Only external cluster members can prevent us from winning this election!
			 * A local cluster member has always the same priority as we have.
			 */
			if(tComChannel.toRemoteNode()){								
				if(DEBUG){
					Logging.log(this, "  ..external cluster member " + tComChannel + " with priority " + tPriority.getValue());
				}
				
				/**
				 * compare our priority with each priority of a cluster member 
				 */
				if(!havingHigherPrioriorityThan(tComChannel, CHECK_LINK_STATE)) {
					if(DEBUG){
						Logging.log(this, "		   ..found better candidate: " + tComChannel);
					}
					tBetterCandidate = tComChannel;
					tBetterCandidatePriority = tComChannel.getPeerPriority().getValue();
					tResult = false;
					
					break;
				}
			}else{
				if(DEBUG){
					Logging.log(this, "  ..local cluster member " + tComChannel + " with priority " + tPriority.getValue());
				}
			}
		}

		/**
		 * debug output
		 */
		if (DEBUG){
			if (tBetterCandidate != null){
				Logging.log(this, "  ..seeing " + tBetterCandidate.getPeerL2Address() + " as better coordinator candidate");
				Logging.log(this, "  .." + tActiveClusterMembershipChannels.size() + " external cluster members, better candidate[prio: " + tBetterCandidatePriority + "]: " + tBetterCandidate + "]\n   ^^^^" + pCause);
			}else{
				if(tResult){
					Logging.log(this, "I have the highest priority out there");
				}else{
					Logging.err(this, "External winner is unknown but I am also not the winner");
				}
			}
		}

		return tResult;
	}
	
	/**
	 * Handles an election signaling packet
	 * 
	 * @param pPacket the packet
	 * @param pComChannel the communication channel from where the message was received
	 */
	public synchronized void handleElectionMessage(SignalingMessageElection pPacket, ComChannel pComChannel)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS)
			Logging.log(this, "RECEIVED ELECTION MESSAGE " + pPacket.getClass().getSimpleName() + " FROM " + pComChannel);

		if (pComChannel == null){
			Logging.err(this, "Communication channel is invalid.");
		}
		
		/***************************
		 * UPDATE PEER PRIORITY
		 ***************************/ 
		ElectionPriority tOldPeerPriority = pComChannel.getPeerPriority();	
		boolean tReceivedNewPriority = pComChannel.setPeerPriority(pPacket.getSenderPriority());
		if(tReceivedNewPriority){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "      ..updating peer priority from " + tOldPeerPriority.getValue() + " to: " + pPacket.getSenderPriority().getValue());
			}		
		}
		
		/***************************
		 * REACT ON THE MESSAGE
		 ***************************/

		/**
		 * WINNER
		 */
		if(pPacket instanceof ElectionWinner)  {
			// cast to an ElectionWinner packet
			ElectionWinner tAnnouncePacket = (ElectionWinner)pPacket;

			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "ELECTION-received via \"" + pComChannel + "\" an ANNOUNCE: " + tAnnouncePacket);
			}

			eventReceivedWINNER(pComChannel, tAnnouncePacket);
		}

		/**
		 * RESIGN
		 */
		if(pPacket instanceof ElectionResign)  {
			// cast to an ElectionResign packet
			ElectionResign tResignPacket = (ElectionResign)pPacket;

			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "ELECTION-received via \"" + pComChannel + "\" an RESIGN: " + tResignPacket);
			}

			eventReceivedRESIGN(pComChannel, tResignPacket);
		}

		/**
		 * PRIORITY UPDATE
		 */
		if(pPacket instanceof ElectionPriorityUpdate) {
			// cast to an ElectionPriorityUpdate packet
			ElectionPriorityUpdate tElectionPriorityUpdatePacket = (ElectionPriorityUpdate)pPacket;

			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "ELECTION-received via \"" + pComChannel + "\" a PRIORITY UPDATE: " + tElectionPriorityUpdatePacket);
			}
			
			tReceivedNewPriority = eventReceivedPRIORITY_UPDATE(pComChannel, tElectionPriorityUpdatePacket, tOldPeerPriority);
		}
		
		/**
		 * LEAVE
		 */
		if(pPacket instanceof ElectionLeave) {
			// cast to an ElectionLeave packet
			ElectionLeave tLeavePacket = (ElectionLeave)pPacket;

			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "ELECTION-received via \"" + pComChannel + "\" a LEAVE: " + tLeavePacket);
			}

			eventReceivedLEAVE(pComChannel, tLeavePacket);
		}
		
		/**
		 * RETURN
		 */
		if(pPacket instanceof ElectionReturn) {
			// cast to an ElectionReturn packet
			ElectionReturn tReturnPacket = (ElectionReturn)pPacket;

			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
				Logging.log(this, "ELECTION-received via \"" + pComChannel + "\" a RETURN: " + tReturnPacket);
			}

			eventReceivedRETURN(pComChannel, tReturnPacket);
		}

		/*****************************
		 * REACT ON THE NEW PEER PRIORITY
		 *****************************/
		if(tReceivedNewPriority){
			leaveReturnOnNewPeerPriority(pComChannel, pPacket);
		}

		/**
		 * NEW RECEIVED MESSAGE -> check the local election result if it has changed
		 */
		checkLocalElection(this + "::handleElectionMessage() by " + pPacket);
	}

	/**
	 * This is the central function for comparing two priorities.
	 * It returns true if the priority of the source is higher than the one of the peer (from the communication channel)
	 * 
	 * @param pSourceL2Address the L2Address of the source (to which the priority should be compared to)
	 * @param pSourcePriority the priority of the source (to which the priority should be compared to)
	 * @param pComChannelToPeer the communication channel to the peer
	 * @param pIgnoreLinkState defines if the link state should be ignored
	 * 
	 * @return true or false
	 */
	private synchronized boolean hasSourceHigherPrioriorityThan(L2Address pSourceL2Address, ElectionPriority pSourcePriority, ComChannel pComChannelToPeer, boolean pIgnoreLinkState)
	{
		boolean tResult = false;
		boolean tDEBUG = false;
		
		/**
		 * Return true if the comm. channel has a deactivated link
		 */
		if (!pIgnoreLinkState){
			if (!pComChannelToPeer.isLinkActiveForElection()){
				return true;
			}
		}
		
		if (tDEBUG){
			if(mHRMController.getNodeL2Address().equals(pSourceL2Address)){
				Logging.log(this, "COMPARING LOCAL PRIORITY with: " + pComChannelToPeer);
			}else{
				Logging.log(this, "COMPARING REMOTE PRIORITY of: " + pSourceL2Address + "(" + pSourcePriority.getValue() + ") with: " + pComChannelToPeer);
			}
		}
		
		/**
		 * Compare the priorities
		 */
		if (pSourcePriority.isHigher(this, pComChannelToPeer.getPeerPriority())){
			if (tDEBUG){
				Logging.log(this, "	        ..HAVING HIGHER PRIORITY (" + pSourcePriority.getValue() + " > " + pComChannelToPeer.getPeerPriority().getValue() + ") than " + pComChannelToPeer.getPeerL2Address());
			}
			
			tResult = true;
		}else{
			/**
			 * Check if both priorities are equal and the L2Addresses have to be checked
			 */
			if (pSourcePriority.equals(pComChannelToPeer.getPeerPriority())){
				if (tDEBUG){
					Logging.log(this, "	        ..HAVING SAME PRIORITY like " + pComChannelToPeer.getPeerL2Address());
				}

				if(pSourceL2Address.isHigher(pComChannelToPeer.getPeerL2Address())) {
					if (tDEBUG){
						Logging.log(this, "	        ..HAVING HIGHER L2 address than " + pComChannelToPeer.getPeerL2Address());
					}

					tResult = true;
				}else{
					if (pSourceL2Address.isLower(pComChannelToPeer.getPeerL2Address())){
						if (tDEBUG){
							Logging.log(this, "	        ..HAVING LOWER L2 address " + pSourceL2Address + " than " +  pComChannelToPeer.getPeerL2Address());
						}
					}else{
						if (tDEBUG){
							Logging.log(this, "	        ..DETECTED OWN LOCAL L2 address " + pSourceL2Address);
						}
						if(head()){
							// we are the cluster head and have won the election
							tResult = true;
						}else{
							// we are a ClusterMember and have lost the game
							tResult = false;
						}
					}
				}
			}else{
				if (tDEBUG){
					Logging.log(this, "	        ..HAVING LOWER PRIORITY (" + pSourcePriority.getValue() + " < " + pComChannelToPeer.getPeerPriority().getValue() + ") than " + pComChannelToPeer.getPeerL2Address());
				}
			}
		}
		
		return tResult;
	}
	
	/**
	 * Returns true if the local priority is higher than the one of the peer (from the communication channel)
	 * 
	 * @param pComChannelToPeer the comm. channel to the peer
	 * @param pIgnoreLinkState define if the link state should be ignored
	 * 
	 * @return true or false
	 */
	private synchronized boolean havingHigherPrioriorityThan(ComChannel pComChannelToPeer, boolean pIgnoreLinkState)
	{
		return hasSourceHigherPrioriorityThan(mHRMController.getNodeL2Address(), mParent.getPriority(), pComChannelToPeer, pIgnoreLinkState);
	}

	/**
	 * Returns true if the source priority is higher than the one of the peer
	 * 
	 * @param pRefL2Address the L2Address of the source (to which the priority should be compared to)
	 * @param pRefPriority the priority of the source (to which the priority should be compared to)
	 * 
	 * @return true or false
	 */
	private synchronized boolean hasClusterLowerPriorityThan(L2Address pRefL2Address, ElectionPriority pRefPriority, boolean pIgnoreLinkState) 
	{
		if(mParent.isThisEntityValid()){
			LinkedList<ComChannel> tChannels = mParent.getComChannels();
	
			if(tChannels.size() == 1){
				ComChannel tComChannelToPeer = mParent.getComChannelToClusterHead();
					
				return hasSourceHigherPrioriorityThan(pRefL2Address, pRefPriority, tComChannelToPeer, pIgnoreLinkState);
			}else{
				if(mState != ElectorState.START){
					Logging.err(this, "hasClusterLowerPriorityThan() found an unplausible amount of comm. channels: " + tChannels);
				}else{
					// Elector is in IDLE state and the election is neither running nor finished yet
				}
			}
		}else{
			Logging.warn(this, "hasClusterLowerPriorityThan() skipped for \"" + pRefL2Address + "\" with prio " + pRefPriority.getValue() + " because entity is already invalidated");
		}
		
		return false;
	}

	/**
	 * SIGNAL: priority update, triggered by ClusterMember when the priority is changed (e.g., if the base node priority was changed)
	 * 		   HINT: This function has to be synchronized because it can be called from within a separate thread (HRMControllerProcessor)
	 * 
	 * @param pCause the cause for this update
	 */
	public synchronized void updatePriority(String pCause)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "Updating local priority");
		}
		
		/**
		 * trigger signaling of "priority update"
		 */
		distributePRIRORITY_UPDATE(BROADCAST, "updatePriority(), cause=" + pCause);

		/**
		 * update election result
		 */
		// we immediately switch to electing mode
		setElectorState(ElectorState.ELECTING);		

		// check the local election result if it has changed
		checkLocalElection(this + "::updatePriority()\n   ^^^^" + pCause);
	}

	/**
	 * Generates a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	@Override
	public String toString()
	{
		return toLocation() + "@" + mParent.toString() +"[" + mState + ", " + mElectionWon + (!(mParent instanceof Cluster) ? ", " + (mParentIsActiveMember  ? "ACTIVE_MEMB" : "INACTIVE_MEMB") : "") + ", " +  mParent.getPriority().getValue() + "]";
	}

	/**
	 * Generates a description of the location of this object instance
	 * 
	 * @return the location description
	 */
	@Override
	public String toLocation()
	{
		String tResult = null;
		
		tResult = getClass().getSimpleName();
		
		return tResult;
	}
}
