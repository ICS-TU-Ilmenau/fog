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
import de.tuilmenau.ics.fog.routing.hierarchical.management.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.management.CoordinatorAsClusterMember;
import de.tuilmenau.ics.fog.routing.hierarchical.management.HierarchyLevel;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * This class is responsible for coordinator elections. 
 * It contains all processing steps of an election member. 
 * This can either be a cluster manager or a coordinator instances.
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
	 * Stores the causes for changes in the election result
	 */
	private LinkedList<String> mResultChangeCauses = new LinkedList<String>();

	private static final boolean SEND_ALL_ELECTION_PARTICIPANTS = false;
	private static final boolean IGNORE_LINK_STATE = true;
	private static final boolean CHECK_LINK_STATE = false;
	private static final ComChannel BROADCAST = null;
	private static final Long ACCEPT_ALL_SURROUNDING_COORDINATORS = null;
	
	/**
	 * Constructor
	 *  
	 * @param pHRMController the HRMController instance
	 * @param pClusterMember the parent cluster member
	 */
	public Elector(HRMController pHRMController, ClusterMember pClusterMember)
	{
		mParent = pClusterMember;
		mElectionWon = false;
		mHRMController = pHRMController;
	}
	
	/**
	 * Check if we are the elector belongs to a cluster manager or not
	 * 
	 * @return true or false
	 */
	private boolean isManager()
	{
		return (mParent instanceof Cluster);
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
	 * EVENT: participant joined
	 * 
	 * @param pComChannel the comm. channel towards the new participant
	 */
	public void eventElectionAvailable(ComChannel pComChannel)
	{
		Logging.log(this, "EVENT: election available for: " + pComChannel);
		
		startElection(pComChannel, this + "::eventElectionAvailable() for " + pComChannel);
	}

	/**
	 * SIGNAL: This function ends the election by signaling WINNER to all cluster members.
	 * 		 It is only used by cluster managers, which won the election. 		
	 */
	private void distributeWINNER()
	{
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
			Logging.warn(this, "Election misses coordinator instance at this point for signaling an ELECTION END");
			
			// set correct elector state
			setElectorState(ElectorState.ERROR);
		}

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDWINNER()-END");
		}
	}
	
	/**
	 * SIGNAL: ends the election by signaling RESIGN to all cluster members 		
	 */
	private void distributeRESIGN()
	{
		// get the size of the cluster
		int tKnownClusterMembers = mParent.countConnectedClusterMembers();
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDRESIGN()-START, electing cluster is " + mParent);
			Logging.log(this, "SENDRESIGN(), cluster members: " + tKnownClusterMembers);
		}

		// create the packet
		ElectionResign tElectionResignPacket = new ElectionResign(mHRMController.getNodeL2Address(), mParent.getPriority());

		/**
		 * HINT: do NOT check for tComChannelToPeer.isSignaledAsWinner(), otherwise, the peer will not recognize that the cluster manager lost the election
		 */

		// send broadcast
		//Logging.err(this, "SENDING: " + tElectionResignPacket);
		mParent.sendClusterBroadcast(tElectionResignPacket, true, SEND_ALL_ELECTION_PARTICIPANTS);

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDRESIGN()-END");
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
			ElectionPriorityUpdate tElectionPriorityUpdatePacket = new ElectionPriorityUpdate(mHRMController.getNodeL2Address(), null, mParent.getPriority());
			Logging.log(this, "Distributing priority update: " + tElectionPriorityUpdatePacket);
	
			if(pComChannel == null){
				/**
				 * send priority update to ALL cluster members
				 */ 
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
							tElectionPriorityUpdatePacket = new ElectionPriorityUpdate(mHRMController.getNodeL2Address(), tComChannelToPeer.getPeerL2Address(), mParent.getPriority());
							tComChannelToPeer.sendPacket(tElectionPriorityUpdatePacket);
							tSentPackets++;
						}
					}
				}
				
				/**
				 * hack: account the broadcast if there was one
				 */
				if(tSentPackets > 0){
					tElectionPriorityUpdatePacket.accountBroadcast();
				}else{
					synchronized (ElectionPriorityUpdate.sCreatedPackets) {
						ElectionPriorityUpdate.sCreatedPackets--;
					}
				}
			}else{
				/**
				 * send priority update to one single cluster member
				 */
				// is this priority update needed?
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
	 * SIGNAL: Alive, report itself as alive by signaling ALIVE to all cluster members
	 *         This function is NOT periodically called. It is explicitly called each time a PING packet is received or as result of a trigger in the GUI.
	 * 	
	 * @param pComChannel the comm. channel along the ElectionAlive should be sent
	 */
	public void distributeALIVE(ComChannel pComChannel)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDALIVE()-START, electing cluster is " + mParent);
			Logging.log(this, "SENDALIVE(), cluster members: " + mParent.getComChannels().size());
		}

		// create the packet
		ElectionAlive tElectionAlivePacket = new ElectionAlive(mHRMController.getNodeL2Address(), mParent.getPriority());

		// send packet
		if(pComChannel == null){
			/**
			 * send priority update to ALL cluster members
			 */ 
			mParent.sendClusterBroadcast(tElectionAlivePacket, true, SEND_ALL_ELECTION_PARTICIPANTS);
		}else{
			/**
			 * send priority update to one single cluster member
			 */
			pComChannel.sendPacket(tElectionAlivePacket);
		}

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "SENDALIVE()-END");
		}
	}
	
	/**
	 * SIGNAL: Leave, report itself as a passive cluster member, should only be called for a ClusterMember
	 * 
	 * @param pCause the cause for this signaling
	 */
	private void distributeLEAVE(String pCause)
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
				updateElectionParticipationState(tComChannelToPeer, false, this + "::distributeLEAVE()\n   ^^^^" + pCause);
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
	 * SIGNAL: Return, report itself as a returning active cluster member
	 * 
	 * @param pCause the cause for this signaling
	 */
	private void distributeRETURN(String pCause)
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
					updateElectionParticipationState(tComChannelToPeer, true, this + "::distributeRETURN()\n   ^^^^" + pCause);
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
	 * This is the central function for sending LEAVE/RETURN to a cluster manager. 
	 * This (de-)activates the participation for the election of this cluster manager. 
	 * 
	 * @param pComChannel the comm. channel towards the cluster manager
	 * @param pState the new participation state
	 * @param pCauseForStateChange the cause for this state change 
	 */
	private void updateElectionParticipationState(ComChannel pComChannel, boolean pState, String pCauseForStateChange)
	{
		Logging.log(this, "### Changing election participation to " + pState + " for comm. channel: " + pComChannel);
		
		if(!isManager()){
			if(pState){
				/**
				 * create the packet
				 */
				ElectionReturn tElectionReturnPacket = new ElectionReturn(mHRMController.getNodeL2Address(), pComChannel.getParent().getPriority());
	
				/**
				 * update local link activation
				 */
				Logging.log(this, "  ..activating link(updateElectionParticipation): " + pComChannel + ", cause=" + "RETURN[" + tElectionReturnPacket.getOriginalMessageNumber() + "]\n   ^^^^" + pCauseForStateChange);
				pComChannel.setLinkActivationForElection(pState, "RETURN[" + tElectionReturnPacket.getOriginalMessageNumber() + "]\n   ^^^^" + pCauseForStateChange);
	
				/**
				 * signal to peer
				 */
				//Logging.err(this, "SENDING RETURN: " + tElectionReturnPacket + ", cause=" + pCauseForStateChange);
				pComChannel.sendPacket(tElectionReturnPacket);
			}else{
				/**
				 * create the LEAVE packet
				 */
				ElectionLeave tElectionLeavePacket = new ElectionLeave(mHRMController.getNodeL2Address(), pComChannel.getParent().getPriority());
	
				/**
				 * update local link activation
				 */
				Logging.log(this, "  ..deactivating link(updateElectionParticipation): " + pComChannel+ ", cause=" + "LEAVE[" + tElectionLeavePacket.getOriginalMessageNumber() + "]\n   ^^^^" + pCauseForStateChange);
				pComChannel.setLinkActivationForElection(pState, "LEAVE[" + tElectionLeavePacket.getOriginalMessageNumber() + "]\n   ^^^^" + pCauseForStateChange);
	
				/**
				 * signal to peer
				 */
				//Logging.err(this, "SENDING: " + tElectionLeavePacket);
				pComChannel.sendPacket(tElectionLeavePacket);
			}
		}else{
			Logging.err(this, "updateElectionParticipation() can only be called for ClusterMember instances, error in state machine, parent is: " + mParent);
		}
	}
	
	/**
	 * Determines the best (highest priority) superior coordinator on a given hierarchy level
	 * 
	 * @param pHRMController the HRMController instance
	 * @param pRefClusterID the Cluster ID of the local inferior coordinator
	 * @param pHierarchyLevel the hierarchy level of the inferior coordinator
	 * 
	 * @return the comm. channel to the found best coordinator instance
	 */
	public static ComChannel getBestSuperiorCoordinator(HRMController pHRMController, Long pRefClusterID, HierarchyLevel pHierarchyLevel)
	{
		ComChannel tResult = null;
		
		LinkedList<CoordinatorAsClusterMember> tClusterMemberships = pHRMController.getAllCoordinatorAsClusterMembers(pHierarchyLevel.getValue());
		/**
		 * iterate over all cluster memberships
		 */
		for (CoordinatorAsClusterMember tClusterMembership : tClusterMemberships){
			/**
			 * does the cluster membership belong to the given inferior coordinator?
			 */
			if((pRefClusterID == null) || (tClusterMembership.getCoordinator().getClusterID().equals(pRefClusterID))){
				/**
				 * does the cluster membership belong to a cluster manager with a valid coordinator instance?
				 */
				if(tClusterMembership.hasClusterValidCoordinator()){				
					// get the channel to the coordinator instance
					ComChannel tChannelToCoordinator = tClusterMembership.getComChannelToClusterManager();
					if(tResult != null){
						Elector tElectorClusterMembership = tClusterMembership.getElector();
						/**
						 * find the coordinator with the highest priority step-by-step
						 */
						if(!tElectorClusterMembership.hasClusterManagerLowerPriorityThan(tResult.getPeerL2Address(), tResult.getPeerPriority(), IGNORE_LINK_STATE)){
							/**
							 * we have found a better coordinator instance
							 */
							tResult = tChannelToCoordinator;
						}
	
					}else{
						/**
						 * use the first found coordinator instance as default result
						 */
						tResult = tChannelToCoordinator;
					}				
				}
			}else{
				// found a cluster membership for a sibling inferior coordinator
			}
		}
		
		return tResult;
	}
	
	/**
	 * Returns to all elections for a given hierarchy level
	 *  
	 * @param pHierarchyLevel the hierarchy level on which we return all elections
	 */
	private void returnToAllBetterElections(HierarchyLevel pHierarchyLevel)
	{
		LinkedList<CoordinatorAsClusterMember> tClusterMemberships = mHRMController.getAllCoordinatorAsClusterMembers(pHierarchyLevel.getValue());
		/**
		 * iterate over all cluster memberships
		 */
		for (CoordinatorAsClusterMember tClusterMembership : tClusterMemberships){
			// get the elector for the cluster membership
			Elector tClusterMembershipElector = tClusterMembership.getElector();

			if((tClusterMembership.getComChannelToClusterManager() == null) || (!hasHigherPrioriorityThan(tClusterMembership.getComChannelToClusterManager(), IGNORE_LINK_STATE))){
				tClusterMembershipElector.distributeRETURN(this + "::returnToAllBetterElections()");
			}else{
				// superior cluster manager has already a lower priority than we have
			}
		}
	}

	/**
	 * Leave all elections which have a cluster manager with a worse priority than the cluster manager referenced by the given comm. channel
	 * 
	 * @param pReferenceChannel the comm. channel towards the reference cluster manager
	 * @param pCause the cause for the call
	 */
	private void leaveElectionsWorseThan(ComChannel pReferenceChannel, String pCause)
	{
		L2Address tRefL2Address = pReferenceChannel.getPeerL2Address();
		ElectionPriority tRefPriority = pReferenceChannel.getPeerPriority();
		
		// get all possible elections
		LinkedList<CoordinatorAsClusterMember> tClusterMemberships = mHRMController.getAllCoordinatorAsClusterMembers(mParent.getHierarchyLevel().getValue());
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
				 * Get the values of the cluster manager of the election
				 */
				ElectionPriority tClusterMembershipPriority = ElectionPriority.create(this);
				if(tClusterMembership.getComChannelToClusterManager() != null){
					// get the priority of the cluster head of the alternative election
					tClusterMembershipPriority = tClusterMembership.getComChannelToClusterManager().getPeerPriority(); 
				}
				
				/**
				 * don't leave the reference election
				 */
				if(pReferenceChannel != tClusterMembership.getComChannelToClusterManager()){
					// get the elector
					Elector tClusterMembershipElector = tClusterMembership.getElector();

					if(tClusterMembershipElector != null){
						/**
						 * leave all worse elections, best coordinator will be left
						 */
						if((!tClusterMembershipPriority.isUndefined()) /* the priority has to be already defined */ &&
						   (tClusterMembershipElector.hasClusterManagerLowerPriorityThan(tRefL2Address, tRefPriority, IGNORE_LINK_STATE)) /* compare the two priorities */){

							/**
							 * signal "LEAVE"
							 */
							if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
								Logging.log(this, "      ..LEAVING: " + tClusterMembershipElector);
							}                                            
							tClusterMembershipElector.distributeLEAVE(this + "::leaveAllWorseAlternativeElections() for " + tMemberCount + "/" + tClusterMemberships.size() + " member [" + (tClusterMembership.getComChannelToClusterManager() != null ? tClusterMembership.getComChannelToClusterManager().getPeerL2Address() : "null") + ", ThisPrio: " + tClusterMembershipPriority.getValue() + " < ReferencePrio: " + tRefPriority.getValue() + ", " + tRefL2Address + "]\n   ^^^^" + pCause);
						}else{
							if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_DISTRIBUTED_ELECTIONS){
								Logging.log(this, "      ..NOT LEAVING: " + tClusterMembershipElector);
							}
							if(tClusterMembershipPriority.isUndefined()){
								Logging.log(this, "leaveAllWorseAlternativeElections() aborted (undef. prio.) for " + tMemberCount + "/" + tClusterMemberships.size() + " member [" + (tClusterMembership.getComChannelToClusterManager() != null ? tClusterMembership.getComChannelToClusterManager().getPeerL2Address() : "null") + ", ThisPrio: " + tClusterMembershipPriority.getValue() + " <> ReferencePrio: " + tRefPriority.getValue() + ", " + tRefL2Address + "]\n   ^^^^ " + pCause);
							}else{
								Logging.log(this, "leaveAllWorseAlternativeElections() aborted for " + tMemberCount + "/" + tClusterMemberships.size() + " member [" + (tClusterMembership.getComChannelToClusterManager() != null ? tClusterMembership.getComChannelToClusterManager().getPeerL2Address() : "null") + ", ThisPrio: " + tClusterMembershipPriority.getValue() + " <> ReferencePrio: " + tRefPriority.getValue() + ", " + tRefL2Address + "]\n   ^^^^ " + pCause);
							}
						}
						/**********************************************************************************************************************************/
					}else{
						throw new RuntimeException("Found invalid elector for: " + tClusterMembership);
					}
				}else{
					Logging.log(this, "leaveAllWorseAlternativeElections() aborted (same cluster!) for " + tMemberCount + "/" + tClusterMemberships.size() + " member [ThisPrio: " + tClusterMembershipPriority.getValue() + " <> ReferencePrio: " + tRefPriority.getValue() + ", " + tRefL2Address + "]\n   ^^^^ " + pCause);
					// we have found a local cluster member which belongs to the same cluster like we do
				}
			}// for
		}
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
	 * EVENT: sets the local node as coordinator for the parent cluster.
	 * 
	 * @param pCause the cause for this event
	 */
	private synchronized void eventElectionWon(String pCause)
	{
		if ((!isWinner()) || (!finished())){
			Logging.log(this, "ELECTION WON for cluster " + mParent +", cause=" + pCause);
			
			synchronized (mResultChangeCauses) {
				mResultChangeCauses.add("WON <== " + pCause);
			}

			// is the parent the cluster head?
			if(isManager()){
				/**
				 * create/get coordinator instance
				 */
				Coordinator tCoordinator = mParent.getCoordinator();
				if (tCoordinator == null){
					Cluster tParentCluster = (Cluster)mParent;
					
					Logging.log(this, "    ..creating new coordinator at hierarch level: " + mParent.getHierarchyLevel().getValue());
					
					// create new coordinator instance
					tCoordinator = Coordinator.create(mHRMController, tParentCluster);
					
					if(tCoordinator.getHierarchyLevel().isHigherLevel()){
						if((mHRMController.getControllerID() - 7) % 9 != 0){
//							Logging.err(mHRMController, "++++ @ " + tCoordinator.getHierarchyLevel().getValue() + ", " + pCause);
						}
					}
				}else{
					if(tCoordinator.isThisEntityValid()){
						Logging.log(this, "Cluster " + mParent + " has already a coordinator");
					}
				}
	
				Logging.log(this, "    ..coordinator is: " + tCoordinator);
				
				/**
				 * signal cluster members that coordinator instance was created
				 */
				if(tCoordinator != null){
					// send WINNER in order to signal all cluster members that we are the coordinator
					distributeWINNER();
		
					// trigger event "announced" for the coordinator
					tCoordinator.eventAnnouncedAsCoordinator();
				}
			}else{
				Logging.log(this, "We have won the election, parent isn't the cluster head: " + mParent + ", waiting for cluster head of alternative cluster");
			}
		}
		
		// mark as election winner
		mElectionWon = true;

		// set correct elector state
		setElectorState(ElectorState.ELECTED);
	}

	/**
	 * EVENT: sets the local node as simple cluster member.
	 * 
	 * @pCause the cause for this event
	 */
	private synchronized void eventElectionLost(String pCause)
	{
		// have we been the former winner of this election?
		if (isWinner()){
			Logging.log(this, "ELECTION LOST for cluster " + mParent +", cause=" + pCause);

			synchronized (mResultChangeCauses) {
				mResultChangeCauses.add("LOST <== " + pCause);
			}

			/**
			 * TRIGGER: invalidate the local coordinator because it was deselected by another coordinator
			 */
			if(isManager()){
				/**
				 * signal cluster members that the coordinator instance is (it will be destroyed immediately after this signaling) destroyed
				 */ 
				distributeRESIGN();

				/**
				 * destroy the coordinator instance
				 */
				Coordinator tCoordinator = mParent.getCoordinator();
				if (tCoordinator != null){
					if(tCoordinator.getHierarchyLevel().getValue() == 1){
						//Logging.warn(null, mHRMController.getControllerID() + "=> " + (mHRMController.getControllerID() - 7) + ", " + ((mHRMController.getControllerID() - 7) % 9));
						if((mHRMController.getControllerID() - 7) % 9 == 0){
//							Logging.err(mHRMController, "---- @ " + tCoordinator.getHierarchyLevel().getValue() + ", " + pCause);
						}
					}

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

		// mark as election loser
		mElectionWon = false;

		// set correct elector state
		setElectorState(ElectorState.ELECTED);
	}

	/**
	 * Checks for an election result
	 * 
	 * @param pCause the cause for this event
	 */
	private void updateElectionResult(String pCause)
	{
		boolean tWinnerCanBeDetermined = true;
		boolean DEBUG = false;
		
		if(DEBUG){
			Logging.log(this, "Checking for election RESULT.., cause=" + pCause);
		}
		
		LinkedList<ComChannel> tActiveClusterMembershipChannels = mParent.getActiveLinks();
		
		// OPTIMIZATION: check if we have found at least one active inferior coordinator, the same result can be derived by hasHigherPriorityThanSurroundingCoordinators()
		if(tActiveClusterMembershipChannels.size() > 0){
			// OPTIMIZATION: do we know more than 0 external cluster members?
			if (mParent.countConnectedRemoteClusterMembers() > 0){
				
				/**
				 * check if all needed priorities are known
				 */
				tWinnerCanBeDetermined = allPrioritiesKnown();
				
				/**
				 * check if election is complete
				 */
				if (tWinnerCanBeDetermined){
					/**
					 * check if we are the winner of the election
					 */
					if((hasHigherPriorityThanActiveClusterMembers(this + "::updateElectionResult()\n   ^^^^" + pCause)) && (hasHigherPriorityThanSurroundingCoordinators())) {
						if(DEBUG){
							Logging.log(this, "	 ..I AM WINNER");
						}
						eventElectionWon(this + "::updateElectionResult()_1\n   ^^^^" + pCause);
					}else{
						if(DEBUG){
							Logging.log(this, "	 ..I HAVE LOST");
						}
						eventElectionLost(this + "::updateElectionResult()_2\n   ^^^^" + pCause);
					}
				}else{
					Logging.log(this, "  ..incomplete election");
					/**
					 * election is incomplete: we are still waiting for some priority value(s)
					 */ 
				}
			}else{
				if(DEBUG){
					Logging.log(this, "  ..I AM WINNER because no external cluster member is known, known channels to cluster members are:" );
					Logging.log(this, "    ..: " + mParent.getComChannels());
				}
				eventElectionWon(this + "::updateElectionResult() - detected isolation\n   ^^^^" + pCause);
			}
		}else{
			/**
			 * no active inferior coordinator found -> coordinator instance is not needed anymore
			 */
			eventElectionLost(this + "::updateElectionResult()_3");
		}
	}

	/**
	 * Central function for updating local election results.
	 *    1.) It checks the local cluster manager if it is the winner or the loser of its cluster
	 *    2.) It checks the current elector if is the winner or the loser of its cluster
	 * 
	 * @param pCause the cause for this call
	 */
	private void updateLocalElectionResults(String pCause)
	{	
		/**
		 * update election result
		 */
		// we immediately switch to electing mode
		setElectorState(ElectorState.ELECTING);		

		if(mParent.getHierarchyLevel().isHigherLevel()){
			/**
			 * determine the best coordinator instance on the hierarchy level of the parent
			 */
			ComChannel tChannelToBestCoordinator = getBestSuperiorCoordinator(mHRMController, ACCEPT_ALL_SURROUNDING_COORDINATORS, mParent.getHierarchyLevel());
			
			/**
			 * execute the LEAVE/RETURN mechanism for higher hierarchy levels
			 */
			if(tChannelToBestCoordinator != null){
				/**
				 * RETURN: to the best coordinator if the election participation is currently deactivated
				 */
				if(tChannelToBestCoordinator.getParent() instanceof CoordinatorAsClusterMember){
					// get the cluster membership instance
					CoordinatorAsClusterMember tClusterMembership = (CoordinatorAsClusterMember)tChannelToBestCoordinator.getParent();
					// use the cluster membership instance to distribute the LEAVE message
					if (!tClusterMembership.getComChannelToClusterManager().isLinkActiveForElection()){
						if(!hasHigherPrioriorityThan(tChannelToBestCoordinator, IGNORE_LINK_STATE)){
							tClusterMembership.getElector().distributeRETURN(this + "::updateLocalElectionResults()\n   ^^^^" + pCause);
						}else{
							// best neighbor coordinator has already a lower priority than we have
						}
					}
				}
				
				/**
				 * LEAVE: all worse elections
				 */
				leaveElectionsWorseThan(tChannelToBestCoordinator, "::updateLocalElectionResults()\n   ^^^^" + pCause);
			}else{
				/**
				 * no superior coordinator available, make sure we have an active cluster membership to all superior cluster managers with an equal/ better priority
				 */
				returnToAllBetterElections(mParent.getHierarchyLevel());
			}
		}
		
		/**
		 * Check the election result of the local cluster on the hierarchy level of the current elector
		 */
		boolean tThisElectorAlreadyChecked = false;
		if(mParent.getHierarchyLevel().isHigherLevel()){
			LinkedList<Cluster> tLocalClusters = mHRMController.getAllClusters(mParent.getHierarchyLevel());
			for(Cluster tCluster : tLocalClusters){
				Elector tClusterElector = tCluster.getElector();
					
				/**
				 * make sure the election process is marked as "running"
				 */
				tClusterElector.setElectorState(ElectorState.ELECTING);

				/**
				 * Recalculate an election result	
				 */
				tClusterElector.updateElectionResult(this + "::updateLocalElectionResults()\n   ^^^^" + pCause);
				
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
			updateElectionResult(this + "::updateLocalElectionResults()\n   ^^^^" + pCause);		
		}
	}

	/**
	 * EVENT: a cluster member left the election process
	 * 
	 * @param pComChannel the communication channel to the cluster member which left the election
	 * @param pPacket the LEAVE packet
	 */
	private void eventReceivedLEAVE(ComChannel pComChannel, ElectionLeave pPacket)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "EVENT: cluster member left election by packet: " + pPacket + " via: " + pComChannel);
		}

		if(isManager()){
			// check if the link state has changed	
			if(pComChannel.isLinkActiveForElection()){
				/**
				 * deactivate the link for the remote cluster member
				 */
				if(isManager()){
					Logging.log(this, "  ..deactivating link(eventReceivedLEAVE): " + pComChannel);
					pComChannel.setLinkActivationForElection(false, "LEAVE[" + pPacket.getOriginalMessageNumber() + "] received");
	
					// checkElectionResult() will be called at the end of the central handleMessage function
				}else{
					Logging.err(this, "Received as cluster member a LEAVE from: " + pComChannel);
				}
			}
		}else{
			throw new RuntimeException("Got a LEAVE message as cluster member");
		}
	}
	
	/**
	 * EVENT: a cluster member returned to the election process
	 * 
	 * @param pComChannel the communication channel to the cluster member which returned to the election
	 * @param pPacket the RETURN packet
	 */
	private void eventReceivedRETURN(ComChannel pComChannel, ElectionReturn pPacket)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "EVENT: cluster member returned: " + pComChannel);
		}
		
		if(isManager()){
			// check if the link state has changed	
			if(!pComChannel.isLinkActiveForElection()){
				/**
				 * activate the link for the remote cluster member 
				 */
				Logging.log(this, "  ..activating link(eventReceivedRETURN): " + pComChannel);
				pComChannel.setLinkActivationForElection(true, "RETURN[" + pPacket.getOriginalMessageNumber() + "] received");

				// checkElectionResult() will be called at the end of the central handleMessage function
			}
		}else{
			throw new RuntimeException("Got a RETURN message as cluster member");
		}
	}
	
	/**
	 * EVENT: A remote coordinator was announced and we are not the coordinator. 
	 * 
	 * @param pComChannel the comm. channel from where the packet was received
	 * @param pPacket the WINNER packet
	 */
	private void eventReceivedWINNER(ComChannel pComChannel, ElectionWinner pPacket)
	{
		Logging.log(this, "EVENT: WINNER: " + pPacket + " via: " + pComChannel);
		
		if(mParent.hasClusterValidCoordinator()){
			Logging.err(this, "Redundant packet: " + pPacket);
		}

		if(!isManager()){
			Logging.log(this, "    ..we are a cluster member");
		
			// mark this cluster as active
			mParent.setClusterWithValidCoordinator(true);
	
			// trigger: superior coordinator available	
			mParent.eventClusterCoordinatorAvailable(pPacket.getSenderName(), pPacket.getCoordinatorID(), pComChannel.getPeerL2Address(), pPacket.getCoordinatorDescription());
			
			// checkElectionResult() will be called at the end of the central handleMessage function
		}else{
			throw new RuntimeException("Got a WINNER message as cluster head");
		}
	}
	
	/**
	 * EVENT: the remote (we are not the coordinator!) coordinator resigned
	 * 
	 * @param pComChannel the comm. channel from where the packet was received
	 * @param pPacket the RESIGN packet
	 */
	private void eventReceivedRESIGN(ComChannel pComChannel, ElectionResign pPacket)
	{
		Logging.log(this, "EVENT: resign: " + pPacket);

		//HINT: do NOT check for redundant packets here

		if(!isManager()){
			Logging.log(this, "    ..we are a cluster member");

			// mark this cluster as active
			mParent.setClusterWithValidCoordinator(false);

			// fake (for reset) trigger: superior coordinator available	
			mParent.eventClusterCoordinatorAvailable(pPacket.getSenderName(), -1, pComChannel.getPeerL2Address(), "N/A");
		}else{
			throw new RuntimeException("Got a RESIGN message as cluster manager");
		}
	}

	/**
	 * Returns if this elector has the highest priority in the surrounding.
	 *  
	 * @return true of false
	 */	
	public boolean hasHigherPriorityThanSurroundingCoordinators()
	{
		boolean tResult = true;
		boolean DEBUG = false;
		
		if(DEBUG){
			Logging.log(this, "Checking if candidate has highest priority in the local surrounding..");
		}
		
		if(mParent.getHierarchyLevel().isHigherLevel()){
			LinkedList<CoordinatorAsClusterMember> tAllClusterMemberships = mHRMController.getAllCoordinatorAsClusterMembers(mParent.getHierarchyLevel().getValue());
			
			if(DEBUG){
				for(CoordinatorAsClusterMember tCoordinatorAsClusterMember : tAllClusterMemberships){
					Logging.log(this, "       ..found known CoordinatorAsClusterMember instance: " + tCoordinatorAsClusterMember);
					Logging.log(this, "         ..channel to head: " + tCoordinatorAsClusterMember.getComChannelToClusterManager());
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
									Logging.log(this, "   ..comm. channel is: " + tCoordinatorAsClusterMember.getComChannelToClusterManager());
								}
	
								if(!tElectorClusterMember.hasClusterManagerLowerPriorityThan(mHRMController.getNodeL2Address(), mParent.getPriority(), IGNORE_LINK_STATE)){
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
			// it's an L0 Cluster/ClusterMember -> check not needed, an L0-cluster always contains the local surrounding
		}
		
		if(DEBUG){
			Logging.log(this, "   ..hasHighestPriorityInTheSurrounding() result: " + tResult);
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
     * Iterates over all active cluster members and searches for a higher priority
	 * 
	 * @return true or false
	 */
	private boolean hasHigherPriorityThanActiveClusterMembers(String pCause)
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
				if(!hasHigherPrioriorityThan(tComChannel, CHECK_LINK_STATE)) {
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
			
			/**
			 * There is nothing more to do here.
			 * The checkElectionResult() function will be called at the end of this function.
			 */ 
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

		/***************************
		 * NEW RECEIVED MESSAGE -> check the local election result if it has changed
		 ***************************/
		updateLocalElectionResults(this + "::handleElectionMessage() for " + pPacket);
	}

	/**
	 * This is the central function for comparing two priorities.
	 * It returns true if the priority of the source is higher than the one of the peer (from the communication channel)
	 * 
	 * @param pRefL2Address the reference L2Address
	 * @param pRefPriority the reference priority
	 * @param pComChannelToPeer the comm. channel
	 * @param pIgnoreLinkState defines if the link state should be ignored
	 * 
	 * @return true or false
	 */
	private synchronized boolean hasHigherPrioriorityThanChannel(L2Address pRefL2Address, ElectionPriority pRefPriority, ComChannel pComChannelToPeer, boolean pIgnoreLinkState)
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
			if(mHRMController.getNodeL2Address().equals(pRefL2Address)){
				Logging.log(this, "COMPARING LOCAL PRIORITY with: " + pComChannelToPeer);
			}else{
				Logging.log(this, "COMPARING REMOTE PRIORITY of: " + pRefL2Address + "(" + pRefPriority.getValue() + ") with: " + pComChannelToPeer);
			}
		}
		
		/**
		 * Compare the priorities
		 */
		if (pRefPriority.isHigher(this, pComChannelToPeer.getPeerPriority())){
			if (tDEBUG){
				Logging.log(this, "	        ..HAVING HIGHER PRIORITY (" + pRefPriority.getValue() + " > " + pComChannelToPeer.getPeerPriority().getValue() + ") than " + pComChannelToPeer.getPeerL2Address());
			}
			
			tResult = true;
		}else{
			/**
			 * Check if both priorities are equal and the L2Addresses have to be checked
			 */
			if (pRefPriority.equals(pComChannelToPeer.getPeerPriority())){
				if (tDEBUG){
					Logging.log(this, "	        ..HAVING SAME PRIORITY like " + pComChannelToPeer.getPeerL2Address());
				}

				if(pRefL2Address.isHigher(pComChannelToPeer.getPeerL2Address())) {
					if (tDEBUG){
						Logging.log(this, "	        ..HAVING HIGHER L2 address than " + pComChannelToPeer.getPeerL2Address());
					}

					tResult = true;
				}else{
					if (pRefL2Address.isLower(pComChannelToPeer.getPeerL2Address())){
						if (tDEBUG){
							Logging.log(this, "	        ..HAVING LOWER L2 address " + pRefL2Address + " than " +  pComChannelToPeer.getPeerL2Address());
						}
					}else{
						if (tDEBUG){
							Logging.log(this, "	        ..DETECTED OWN LOCAL L2 address " + pRefL2Address);
						}
						if(isManager()){
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
					Logging.log(this, "	        ..HAVING LOWER PRIORITY (" + pRefPriority.getValue() + " < " + pComChannelToPeer.getPeerPriority().getValue() + ") than " + pComChannelToPeer.getPeerL2Address());
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
	private synchronized boolean hasHigherPrioriorityThan(ComChannel pComChannelToPeer, boolean pIgnoreLinkState)
	{
		return hasHigherPrioriorityThanChannel(mHRMController.getNodeL2Address(), mParent.getPriority(), pComChannelToPeer, pIgnoreLinkState);
	}

	/**
	 * Returns true if the give cluster manager has a lower priority than the current election member
	 * 
	 * @param pRefL2Address the L2Address of the source (to which the priority should be compared to)
	 * @param pRefPriority the priority of the source (to which the priority should be compared to)
	 * 
	 * @return true or false
	 */
	private synchronized boolean hasClusterManagerLowerPriorityThan(L2Address pRefL2Address, ElectionPriority pRefPriority, boolean pIgnoreLinkState) 
	{
		if(mParent.isThisEntityValid()){
			LinkedList<ComChannel> tChannels = mParent.getComChannels();
	
			if(tChannels.size() == 1){
				ComChannel tComChannelToPeer = mParent.getComChannelToClusterManager();
					
				return hasHigherPrioriorityThanChannel(pRefL2Address, pRefPriority, tComChannelToPeer, pIgnoreLinkState);
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

		// check the local election result if it has changed
		updateLocalElectionResults(this + "::updatePriority()\n   ^^^^" + pCause);
	}

	/**
	 * EVENT: elector is invalidated, triggered by ClusterMember if it gets invalidated
	 *        (without this function and its callings, a local coordinator might remain isolated and no superior coordinator gets instantiated)
	 * 
	 * @param pCause the cause for the call
	 */
	public void eventInvalidation(String pCause)
	{
		Logging.log(this, "EVENT: invalidation, cause=" + pCause);

		updateLocalElectionResults(this + "::eventInvalidation()\n   ^^^^" + pCause);
	}

	/**
	 * Starts the election process.
	 * This function can be called by external processes.
	 * 
	 * @param pComChannel the com. channel to the peer (either the cluster manager or the joined cluster member)
	 * @param pCause the cause for this election start
	 */
	public void startElection(ComChannel pComChannel, String pCause)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_ELECTIONS){
			Logging.log(this, "#### STARTING ELECTION");
		}
		
		/**
		 * Send a priority update to all local cluster members
		 */
		distributePRIRORITY_UPDATE(pComChannel, this + "::startElection()\n   ^^^^" + pCause);

		/**
		 * NEW OUTGOING MESSAGE -> check the local election result if it has changed
		 */
		updateLocalElectionResults(this + "::startElection_1() for " + pComChannel + "\n   ^^^^" + pCause);
	}

	/**
	 * Generates a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	@Override
	public String toString()
	{
		return toLocation() + "@" + mParent.toString() +"[" + mState + ", " + mElectionWon + ", " +  mParent.getPriority().getValue() + "]";
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
