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

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAlive;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyElect;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyLeave;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyPriorityUpdate;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyReply;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyResign;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyReturn;
import de.tuilmenau.ics.fog.packets.hierarchical.election.SignalingMessageBully;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.Localization;
import de.tuilmenau.ics.fog.routing.hierarchical.management.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterMember;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ComChannel;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ControlEntity;
import de.tuilmenau.ics.fog.routing.hierarchical.management.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.management.CoordinatorAsClusterMember;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

//TODO: invalidateElection() missing, if error during communication with coordinator occurred

/**
 * This class is responsible for coordinator elections. It is instantiated per Cluster and ClusterProxy object.
 *  For a Cluster object, this class plays the role of the cluster head.
 *  For a ClusterProxy, this class acts in the role of a cluster member.
 *
 */
public class Elector implements Localization
{
	private enum ElectorState {
		START,    // Constructor
		IDLE,     // no coordinator known, no election running
		ELECTING, // election process is currently running
		ELECTED,   // election process has established common consensus about the coordinator of the cluster
		ERROR // election process run into an error state
	}

	//TODO: rückkehr von ELECTED zu ELECTING, wenn BullyAlive von koordinator ausbleibt
	
	/** 
	 * Stores the internal state of the elector
	 */
	private ElectorState mState;
	
	/**
	 * Pointer to the parent cluster, which owns this elector
	 */
	private ClusterMember mParent = null;

	/**
	 * The timeout for an awaited BullyAlive message (in s).
	 */
	private long TIMEOUT_FOR_REPLY = 25;
	/**
	 * The timeout for an awaited BullyAlive message (in s).
	 */
	private long TIMEOUT_FOR_ALIVE = 25;

	/**
	 * The time period between two BullyAlive messages (in s).
	 */
	private long PERIOD_FOR_ALIVE = 10;

	/**
	 * Stores if election was won.
	 */
	private boolean mElectionWon = false;
	
	/**
	 * Stores a reference to the HRMController instance
	 */
	private HRMController mHRMController = null;
	
	/**
	 * Stores the timestamp of the last ElectBroadcast signaling
	 */
	private Double mTimestampLastElectBroadcast =  new Double(0);
	
	public Elector(HRMController pHRMController, ClusterMember pCluster)
	{
		mState = ElectorState.START;
		mParent = pCluster;
		mElectionWon = false;
		mHRMController = pHRMController;
		
		// set IDLE state
		setElectorState(ElectorState.IDLE);
	}
	
	/**
	 * Elects the coordinator for this cluster.
	 */
	private void elect()
	{
		// set correct elector state
		setElectorState(ElectorState.ELECTING);

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "ELECTING now...");
		}
		
		if(head()){
			// do we know more than 0 external cluster members?
			if (mParent.countConnectedRemoteClusterMembers() > 0){
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "Trying to ask " + mParent.countConnectedRemoteClusterMembers() + " external cluster members for their Bully priority: " + mParent.getComChannels());
				}
				distributeELECT();
			}else{
				// we don'T have external members - but do we have local members?
				if(mParent.countConnectedClusterMembers() > 0){					
					/**
					 * Send a priority update to all local cluster members
					 */
					distributePRIRORITY_UPDATE();
				}
				/**
				 * trigger "detected isolation"
				 */
				eventDetectedIsolation();
			}
		}else{
			Logging.log(this, "elect() stops here because parent is not the cluster head: " + mParent);
		}
	}
	
	private void eventAllLinksInactive()
	{
		Logging.log(this, "EVENT: all links inactive");
		
		/**
		 * trigger: "election lost"
		 */
		// do we still try to find a winner/loser?
		if(mState == ElectorState.ELECTING){
			eventElectionLost();
		}
	}
	
	/**
	 * EVENT: detected isolation 
	 */
	private void eventDetectedIsolation()
	{
		Logging.log(this, "EVENT: isolation");
		
		Logging.log(this, "I AM WINNER because no alternative cluster member is known, known cluster channels:" );
		Logging.log(this, "    ..: " + mParent.getComChannels());
		eventElectionWon();
	}
	
	/**
	 * EVENT: we have joined a remote cluster, triggered by ClusterMember
	 * 
	 * @param pComChannelToRemoteCluster the comm. channel to the cluster
	 */
	public void eventJoinedRemoteCluster(ComChannel pComChannelToRemoteCluster)
	{
		Logging.log(this, "HAVE JOINED remote cluster");

		/**
		 * Send: priority update
		 */
		distributePRIRORITY_UPDATE();
	}

	/**
	 * Restarts the election process for this cluster
	 */
	private void reelect()
	{
		if (head()){
			//reset ELECT BROADCAST timer
			mTimestampLastElectBroadcast = new Double(0);
			
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "REELECTION");
			}
			elect();
		}else{
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "Reelection needed but we aren't the cluster head, we hope that the other local Cluster object will trigger a reelection" );
			}
		}
	}
	
	/**
	 * Starts the election process. This function is usually called by the GUI.
	 */
	public synchronized void startElection()
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "#### STARTING ELECTION");
		}
		
		if(mParent instanceof ClusterMember){
			switch(mState){
				case IDLE:
					elect();
					break;
				case ELECTED:
					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
						Logging.log(this, "RESTARTING ELECTION, old coordinator was valid: " + finished());
					}
					reelect();
					break;
				case ELECTING:
					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
						Logging.log(this, "Election is already running");
					}
					break;
				case ERROR:
					Logging.err(this, "Election is in ERROR state");
					break;
				case START:
					Logging.err(this, "Election is stuck");
					break;
				default:
					break;
			}
		}else{
			throw new RuntimeException("We skipped election start because parent isn't a cluster head/member: " + mParent);
		}
	}
	
	/**
	 * Sets the current elector state
	 * 
	 * @param pNewState the new state
	 */
	private void setElectorState(ElectorState pNewState)
	{
		// check if state transition is valid
		if((pNewState == ElectorState.ERROR) ||
			(mState == pNewState) || 
			( ((mState == ElectorState.START) && (pNewState == ElectorState.IDLE)) ||
			((mState == ElectorState.IDLE) && (pNewState == ElectorState.ELECTING)) ||
			((mState == ElectorState.ELECTING) && (pNewState == ElectorState.ELECTED)) ||
			((mState == ElectorState.ELECTED) && (pNewState == ElectorState.ELECTING)) ||
			((mState == ElectorState.ELECTED) && (pNewState == ElectorState.IDLE))
		   )) 
		{
			if (mState != pNewState){
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "STATE TRANSITION from " + mState + " to " + pNewState);
				}
	
				// set new state
				mState = pNewState;
			}
		} else {
			throw new RuntimeException(toLocation() + "-cannot change its state from " + mState +" to " + pNewState);
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
	 * Determines if the timing of an action is okay because the minimum time period between two of such actions is maintained.
	 * 
	 * @param pTimestampLastSignaling the timestamp of the last action
	 * @param pMinPeriod
	 * @return
	 */
	private boolean isTimingOkayOfElectBroadcast()
	{
		boolean tResult = false;
		double tNow = mHRMController.getSimulationTime();
		double tTimeout = mTimestampLastElectBroadcast.longValue() + TIMEOUT_FOR_REPLY;
				
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "Checking timing of ELECT BROADCAST: last=" + mTimestampLastElectBroadcast.longValue() + ", MinPeriod=" + TIMEOUT_FOR_REPLY + ", now=" + tNow + ", MinTime=" + tTimeout);
		}
		
		// is timing okay?
		if ((mTimestampLastElectBroadcast.doubleValue() == 0) || (tNow > mTimestampLastElectBroadcast.doubleValue() + TIMEOUT_FOR_REPLY)){
			tResult = true;
			mTimestampLastElectBroadcast = new Double(tNow);
			
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "     ..ELECT BROADCAST is okay");
			}
		}else{
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "     ..ELECT BROADCAST is skipped due to timer");
			}
		}
		
		return tResult;
	}
	
	/**
	 * SEND: start the election by signaling BULLY ELECT to all cluster members
	 */
	private void distributeELECT()
	{
		if (mState == ElectorState.ELECTING){
			if (isTimingOkayOfElectBroadcast()){
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "SENDELECTIONS()-START, electing cluster is " + mParent);
					Logging.log(this, "SENDELECTIONS(), external cluster members: " + mParent.countConnectedRemoteClusterMembers());
				}
		
				// create the packet
				BullyElect tPacketBullyElect = new BullyElect(mHRMController.getNodeName(), mParent.getPriority());
				
				// HINT: we send a broadcast to all cluster members, the common Bully algorithm sends this message only to alternative candidates which have a higher priority				
				mParent.sendClusterBroadcast(tPacketBullyElect, true);
				
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "SENDELECTIONS()-END");
				}
			}else{
				Logging.warn(this, "signalElectBroadcast() was triggered too frequently, timeout isn't reached yet, skipping this action");
			}

		}else{
			Logging.warn(this, "Election has wrong state " + mState + " for signaling an ELECTION START, ELECTING expected");

			// set correct elector state
			setElectorState(ElectorState.ERROR);
		}			
	}

	/**
	 * SEND: ends the election by signaling BULLY ANNOUNCE to all cluster members 		
	 */
	private void distributeANNOUNCE()
	{
		if (mState == ElectorState.ELECTED){
			// get the size of the cluster
			int tKnownClusterMembers = mParent.countConnectedClusterMembers();
			
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDANNOUNCE()-START, electing cluster is " + mParent);
				Logging.log(this, "SENDANNOUNCE(), cluster members: " + tKnownClusterMembers);
			}
	
			// HINT: the coordinator has to be already created here

			if (mParent.getCoordinator() != null){
				// create the packet
				BullyAnnounce tPacketBullyAnnounce = new BullyAnnounce(mHRMController.getNodeName(), mParent.getPriority(), mParent.getCoordinator().getCoordinatorID(), mParent.getCoordinator().toLocation() + "@" + HRMController.getHostName());
		
				// send broadcast
				mParent.sendClusterBroadcast(tPacketBullyAnnounce, true);
			}else{
				Logging.warn(this, "Election has wrong state " + mState + " for signaling an ELECTION END, ELECTED expected");
				
				// set correct elector state
				setElectorState(ElectorState.ERROR);
			}
	
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDANNOUNCE()-END");
			}
		}else{
			// elector state is ELECTED
			Logging.warn(this, "Election state isn't ELECTING, we cannot finishe an election which wasn't started yet, error in state machine");
		}			
	}
	
	/**
	 * SEND: ends the election by signaling BULLY RESIGN to all cluster members 		
	 */
	private void distributeRESIGN()
	{
		if (mState == ElectorState.ELECTED){
			// get the size of the cluster
			int tKnownClusterMembers = mParent.countConnectedClusterMembers();
			
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDRESIGN()-START, electing cluster is " + mParent);
				Logging.log(this, "SENDRESIGN(), cluster members: " + tKnownClusterMembers);
			}
	
			// HINT: the coordinator has to be already created here

			if (mParent.getCoordinator() != null){
				// create the packet
				BullyResign tPacketBullyResign = new BullyResign(mHRMController.getNodeName(), mParent.getPriority(), mParent.getCoordinator().getCoordinatorID(), mParent.getCoordinator().toLocation() + "@" + HRMController.getHostName());
		
				// send broadcast
				mParent.sendClusterBroadcast(tPacketBullyResign, true);
			}else{
				Logging.warn(this, "Election has wrong state " + mState + " for signaling an ELECTION END, ELECTED expected");
				
				// set correct elector state
				setElectorState(ElectorState.ERROR);
			}
	
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDRESIGN()-END");
			}
		}else{
			// elector state is ELECTED
			Logging.warn(this, "Election state isn't ELECTING, we cannot finishe an election which wasn't started yet, error in state machine");
		}			
	}

	/**
	 * SEND: BullyPriorityUpdate
	 */
	private void distributePRIRORITY_UPDATE()
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "SENDPRIOUPDATE()-START, electing cluster is " + mParent);
			Logging.log(this, "SENDPRIOUPDATE(), cluster members: " + mParent.getComChannels().size());
		}

		BullyPriorityUpdate tBullyPriorityUpdatePacket = new BullyPriorityUpdate(mHRMController.getNodeName(), mParent.getPriority());

		// send broadcast
		mParent.sendClusterBroadcast(tBullyPriorityUpdatePacket, true);

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "SENDPRIOUPDATE()-END");
		}
	}

	/**
	 * SEND: BullyAlive, report itself as alive by signaling BULLY ALIVE to all cluster members
	 */
	private void distributeALIVE()
	{
		if (HRMConfig.Election.SEND_BULLY_ALIVES){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDALIVE()-START, electing cluster is " + mParent);
				Logging.log(this, "SENDALIVE(), cluster members: " + mParent.getComChannels().size());
			}
	
			// create the packet
			BullyAlive tPacketBullyAlive = new BullyAlive(mHRMController.getNodeName(), mParent.getPriority());
	
			// send broadcast
			mParent.sendClusterBroadcast(tPacketBullyAlive, true);
	
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDALIVE()-END");
			}
		}else{
			// BullyAlive messages are currently deactivated
		}			
	}

	/**
	 * SEND: BullyLeave, report itself as a passive cluster member
	 * 
	 * @param pCause the cause for this signaling
	 */
	private void distributeLEAVE(String pCause)
	{
		//if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "SENDLEAVE()-START, electing cluster is " + mParent);
			Logging.log(this, "SENDLEAVE(), cluster members: " + mParent.getComChannels().size());
		//}

		// create the packet
		BullyLeave tPacketBullyLeave = new BullyLeave(mHRMController.getNodeName(), mParent.getPriority());

		// deactivate all links
		mParent.setLAllinksActivation(false, "LEAVE[" + tPacketBullyLeave.getOriginalMessageNumber() + "] broadcast - " + pCause);

		// send broadcast
		mParent.sendClusterBroadcast(tPacketBullyLeave, true);

		//if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "SENDLEAVE()-END");
		//}
	}

	/**
	 * SEND: BullyReturn, report itself as a returning active cluster member
	 * 
	 * @param pCause the cause for this signaling
	 */
	private void distributeRETURN(String pCause)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "SENDRETURN()-START, electing cluster is " + mParent);
			Logging.log(this, "SENDRETURN(), cluster members: " + mParent.getComChannels().size());
		}

		// create the packet
		BullyReturn tPacketBullyReturn = new BullyReturn(mHRMController.getNodeName(), mParent.getPriority());

		// deactivate all links
		mParent.setLAllinksActivation(true, "RETURN[" + tPacketBullyReturn.getOriginalMessageNumber() + "] broadcast - " + pCause);

		// send broadcast
		mParent.sendClusterBroadcast(tPacketBullyReturn, true);

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "SENDRETURN()-END");
		}
	}

	/**
	 * SIGNAL: BullyReply
	 * 
	 * @param pComChannel the communication channel along which the RESPONSE should be send
	 */
	private void sendREPLY(ComChannel pComChannel)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDRESPONSE()-START, electing cluster is " + mParent);
				Logging.log(this, "SENDRESPONSE(), cluster members: " + mParent.getComChannels().size());
			}
		}

		// create REPLY packet
		BullyReply tReplyPacket = new BullyReply(mHRMController.getNodeName(), pComChannel.getPeerHRMID(), mParent.getPriority());
			
		// send the answer packet
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "BULLY-sending to \"" + pComChannel + "\" a REPLY: " + tReplyPacket);
		}

		// send message
		pComChannel.sendPacket(tReplyPacket);

		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "SENDRESPONSE()-END");
		}
	}
	
	/**
	 * EVENT: sets the local node as coordinator for the parent cluster. 
	 */
	private void eventElectionWon()
	{
		if ((!isWinner()) || (!finished())){
			Logging.log(this, "ELECTION WON for cluster " + mParent);
			
			// mark as election winner
			mElectionWon = true;
			
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
					tCoordinator = new Coordinator(tParentCluster);
				}else{
					Logging.warn(this, "Cluster " + mParent + " has already a coordinator");
				}
	
				Logging.log(this, "    ..coordinator is: " + tCoordinator);
				
				if(tCoordinator != null){
					// leave all other election processes because we have our winner at this hierarchy level
					//leaveAlternativeHigherElections();
					
					// send BULLY ANNOUNCE in order to signal all cluster members that we are the coordinator
					distributeANNOUNCE();
		
					// trigger event "announced" for the coordinator
					tCoordinator.eventAnnouncedAsCoordinator();
				}
			}else{
				Logging.log(this, "We have won the election, parent isn't the cluster head: " + mParent + ", waiting for cluster head of alternative cluster");
			}
		}else{
			Logging.warn(this, "Cluster " + mParent + " has still a valid and known coordinator, skipping eventElectionWon() here");
		}
	}
	
	/**
	 * Leave all alternative elections if:
	 *   	- we have won an election
	 *    	- we are a ClusterMember and have received an ANNOUNCE via an active link
	 */
	private void leaveAlternativeHigherElections()
	{
		if(HRMConfig.Election.USE_LINK_STATES){
			// only do this for a higher hierarchy level! at base hierarchy level we have local redundant cluster covering the same bus (network interface)
			if(mParent.getHierarchyLevel().isHigherLevel()){
				// get all possibilities
				LinkedList<CoordinatorAsClusterMember> tAlternatives = mHRMController.getAllCoordinatorAsClusterMembers(mParent.getHierarchyLevel().getValue());
				Logging.log(this, "Distributing LEAVE for alternatives now...: " + tAlternatives);
				
				if(tAlternatives.size() > 0){					
					/**
					 * Iterate over all alternatives
					 */
					for (CoordinatorAsClusterMember tAlternative : tAlternatives){
						// is it this instance?
						if(!mParent.equals(tAlternative)){
							// are we the coordinator (so, the coordinator is on this node!)?
							if(!mHRMController.getNodeL2Address().equals(tAlternative.getCoordinatorNodeL2Address())){
								// get the elector
								Elector tAlternativeElector = tAlternative.getElector();
								if(tAlternativeElector != null){
									/**
									 * Distribute "LEAVE" for the alternative election process
									 */
									Logging.log(this, "      ..LEAVE in: " + tAlternativeElector);
									tAlternativeElector.distributeLEAVE("leaveAlternativeHigherElections() called by election winner: " + this);
								}else{
									throw new RuntimeException("Found invalid elector for: " + tAlternative);
								}
							}
						}
					}
				}
			}
		}	
	}
	
	/**
	 * Return to all alternative elections if:
	 * 		- TODO
	 */
	private void returnAlternativeHigherElections()
	{
		if(HRMConfig.Election.USE_LINK_STATES){
			// only do this for a higher hierarchy level! at base hierarchy level we have local redundant cluster covering the same bus (network interface)
			if(mParent.getHierarchyLevel().isHigherLevel()){
				// get all possibilities
				LinkedList<CoordinatorAsClusterMember> tAlternatives = mHRMController.getAllCoordinatorAsClusterMembers(mParent.getHierarchyLevel().getValue());
				Logging.log(this, "Distributing RETURN for alternatives now...: " + tAlternatives);
				
				if(tAlternatives.size() > 0){					
					/**
					 * Iterate over all alternatives
					 */
					for (CoordinatorAsClusterMember tAlternative : tAlternatives){
						// is it this instance?
						if(!mParent.equals(tAlternative)){
							// are we the coordinator (so, the coordinator is on this node!)?
							if(!mHRMController.getNodeL2Address().equals(tAlternative.getCoordinatorNodeL2Address())){
								// get the elector
								Elector tAlternativeElector = tAlternative.getElector();
								if(tAlternativeElector != null){
									/**
									 * Distribute "RETURN" for the alternative election process
									 */
									Logging.log(this, "      ..RETURN in: " + tAlternativeElector);
									tAlternativeElector.distributeRETURN("returnAlternativeHigherElections() called by election loser: " + this);
								}else{
									throw new RuntimeException("Found invalid elector for: " + tAlternative);
								}
							}
						}
					}
				}
			}
		}	
	}
	
	/**
	 * EVENT: sets the local node as simple cluster member.
	 */
	private void eventElectionLost()
	{
		Logging.log(this, "ELECTION LOST for cluster " + mParent);
	
		// store the old election result
		boolean tWasFormerWinner = mElectionWon;
		
		// mark as election loser
		mElectionWon = false;
		
		// set correct elector state
		setElectorState(ElectorState.ELECTED);
		
		// have we been the former winner of this election?
		if (tWasFormerWinner){
			Logging.log(this, "ELECTION LOST BUT WE WERE THE FORMER WINNER");

			/**
			 * TRIGGER: invalidate the local coordinator because it was deselected by another coordinator
			 */
			if(head()){
				Coordinator tCoordinator = mParent.getCoordinator();
				if (tCoordinator != null){
					// send BULLY ANNOUNCE in order to signal all cluster members that we are the coordinator
					distributeRESIGN();

					/**
					 * Invalidate the coordinator
					 */
					Logging.log(this, "     ..invalidating the coordinator role of: " + tCoordinator);
					tCoordinator.eventCoordinatorRoleInvalid();
					
					// return to all other election processes because we have lost this coordinator at this hierarchy level
					//returnAlternativeHigherElections();
				}else{
					Logging.err(this, "We were the former winner of the election but the coordinator is invalid");
				}
			}else{
				// we are not the cluster header, so we can't be the coordinator
			}
		}
	}

	/**
	 * EVENT: a candidate left the election process
	 * 
	 * @param pComChannel the communication channel to the cluster member which left the election
	 * @param pLeavePacket the received packet
	 */
	private void eventLEAVE(ComChannel pComChannel, BullyLeave pLeavePacket)
	{
		//if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "EVENT: cluster member left election: " + pComChannel);
		//}

		// check if the link state has changed	
		if(pComChannel.getLinkActivation()){
			/**
			 * deactivate the link for the remote cluster member
			 */
			if(head()){
				pComChannel.setLinkActivation(false, "LEAVE[" + pLeavePacket.getOriginalMessageNumber() + "] received");
			}else{
				Logging.warn(this, "Ignoring LEAVE for: " + mParent);
			}
			
			if(HRMConfig.Election.USE_LINK_STATES){
				if (head()){
					//if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
						Logging.log(this, "     ..REELECTING, maybe a change should happen");
					//}
					// maybe we can now win a formerly lost election? or we are the winner but a new election would result in a "lost"? -> start a new election
					startElection();
				}			
			}
		}
	}
	
	/**
	 * EVENT: the election process was triggered by another cluster member
	 * 
	 * @param pComChannel the source comm. channel
	 * @param pReturnPacket the received packet
	 */
	private void eventRETURN(ComChannel pComChannel, BullyReturn pReturnPacket)
	{
		//if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "EVENT: cluster member returned: " + pComChannel);
		//}
		
		/**
		 * activate the link for the remote cluster member 
		 */
		if(head()){
			pComChannel.setLinkActivation(true, "RETURN[" + pReturnPacket.getOriginalMessageNumber() + "] received");
		}else{
			Logging.warn(this, "Ignoring RETURN for: " + mParent);
		}

		/**
		 * Trigger : reelect
		 */
		// start re-election
		reelect();
	}

	/**
	 * EVENT: the election process was triggered by another cluster member
	 * 
	 * @param pComChannel the source comm. channel
	 */
	private void eventELECT(ComChannel pComChannel)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "EVENT: received ELECT");
		}
		
		// answer the "elect" message
		sendREPLY(pComChannel);
			
		/**
		 * do we have a higher priority than the peer?
		 */
		if (havingHigherPrioriorityThan(pComChannel)){
			// start re-election
			reelect();
		}
	}
	
	/**
	 * EVENT: another cluster member has sent its Bully priority
	 * 
	 * @param pSourceComChannel the source comm. channel 
	 */
	private void eventREPLY(ComChannel pSourceComChannel)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "EVENT: received REPLY");
		}

		/**
		 * check for a winner
		 */
		if(mState == ElectorState.ELECTING){
			checkForWinner();
		}else{
			/**
			 *  we received a delayed reply, this can happen if:
			 *      0.) we send an ELECT to all peers
			 *      1.) we receive BullyPriorityUpdates from all peers
			 *        ==> we know the priority of all peers
			 *        ==> we have the highest priority
			 *        ==> we decide to be the winner
			 *      2.) a peer answers a former ELECT
			 */
			Logging.warn(this, "Received delayed REPLY via: " + pSourceComChannel);
		}
	}
	
	/**
	 * EVENT: the remote (we are not the coordinator!) coordinator was announced
	 * 
	 * @param pComChannel the comm. channel from where the packet was received
	 * @param pAnnouncePacket the packet itself
	 */
	private void eventANNOUNCE(ComChannel pComChannel, BullyAnnounce pAnnouncePacket)
	{
		ControlEntity tControlEntity = pComChannel.getParent();

		// we have a winner -> leave all other election processes
//		if(!head()){
//			if(pComChannel.getLinkActivation()){
//				leaveAlternativeHigherElections();
//			}
//		}

		// trigger "election lost"
		eventElectionLost();

		// trigger: superior coordinator available	
		tControlEntity.eventClusterCoordinatorAvailable(pComChannel, pAnnouncePacket.getSenderName(), pAnnouncePacket.getCoordinatorID(), pComChannel.getPeerL2Address(), pAnnouncePacket.getCoordinatorDescription());
	}
	
	/**
	 * EVENT: the remote (we are not the coordinator!) coordinator resigned
	 * 
	 * @param pComChannel the comm. channel from where the packet was received
	 * @param pResignPacket the packet itself
	 */
	private void eventRESIGN(ComChannel pComChannel, BullyResign pResignPacket)
	{
		ControlEntity tControlEntity = pComChannel.getParent();

		// fake (for reset) trigger: superior coordinator available	
		tControlEntity.eventClusterCoordinatorAvailable(null, pResignPacket.getSenderName(), -1, pComChannel.getPeerL2Address(), "N/A");
	}

	/**
	 * EVENT: priority update
	 * 
	 * @param pComChannel the comm. channel from where the packet was received
	 */
	private void eventPRIORITY_UPDATE(ComChannel pComChannel)
	{
		// get the priority of the sender
		BullyPriority tSenderPriority = pComChannel.getPeerPriority();
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "EVENT: priority " + tSenderPriority.getValue() + " via comm. channel: " + pComChannel);
		}

		// do we have the higher priority?
		if (havingHigherPrioriorityThan(pComChannel)){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "Received remote priority " + tSenderPriority.getValue() + " is lower than local " + mParent.getPriority().getValue());
			}
		}else{
			/**
			 * Trigger: new election round if we are the current winner
			 */
			if(isWinner()){
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "Received remote priority " + tSenderPriority.getValue() + " is higher than local " + mParent.getPriority().getValue() + ", triggering re-election");
				}
				startElection();
			}
		}
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
	 * Checks for a winner
	 */
	private void checkForWinner()
	{
		boolean tIsWinner = true;
		boolean tElectionComplete = true;
		ComChannel tExternalWinner = null;
		
		if(mState == ElectorState.ELECTING){
			Logging.log(this, "Checking for election winner..");
			LinkedList<ComChannel> tActiveChannels = mParent.getActiveLinks();
			
			// check if we have found at least one active link
			if(tActiveChannels.size() > 0){
				// do we know more than 0 external cluster members?
				if (mParent.countConnectedRemoteClusterMembers() > 0){
					/**
					 * Iterate over all cluster members and check if their priority is available, check every cluster member if it has a higher priority
					 */
					Logging.log(this, "   ..searching for highest priority...");
					for(ComChannel tComChannel : tActiveChannels) {
						BullyPriority tPriority = tComChannel.getPeerPriority(); 
						
						/**
						 * are we still waiting for the Bully priority of some cluster member?
						 */
						if ((tPriority == null) || (tPriority.isUndefined())){
							// election is incomplete
							tElectionComplete = false;
						
							// leave the loop because we already known that the election is incomplete
							break;
						}
						
						Logging.log(this, "		..cluster member " + tComChannel + " has priority " + tPriority.getValue());
						
						/**
						 * compare our priority with each priority of a cluster member 
						 */
						if(!havingHigherPrioriorityThan(tComChannel)) {
							Logging.log(this, "		   ..found better candidate: " + tComChannel);
							tExternalWinner = tComChannel;
							tIsWinner = false;
						}
					}
					
					/**
					 * Check if election is complete
					 */
					if (tElectionComplete){
						/**
						 * React on the result
						 */
						if(tIsWinner) {
							Logging.log(this, "	        ..I AM WINNER");
							eventElectionWon();
						}else{
							if (tExternalWinner != null){
								Logging.log(this, "	        ..seeing " + tExternalWinner.getPeerL2Address() + " as better coordinator candidate");
							}else{
								Logging.err(this, "External winner is unknown but also I am not the winner");
							}
							eventElectionLost();
						}
					}else{
						// election is incomplete: we are still waiting for some priority value(s)
					}
				}else{
					/**
					 * trigger "detected isolation"
					 */
					eventDetectedIsolation();
				}
			}else{
				/**
				 * trigger "all links inactive"
				 */
				eventAllLinksInactive();
			}
		}else{
			Logging.err(this, "checkForWinner() EXPECTED STATE \"ELECTING\" here but got state: " + mState.toString());
		}
	}
	
	/**
	 * Handles a Bully signaling packet
	 * 
	 * @param pPacketBully the packet
	 * @param pComChannel the communication channel from where the message was received
	 */
	@SuppressWarnings("unused")
	public synchronized void handleSignalingMessageBully(SignalingMessageBully pPacketBully, ComChannel pComChannel)
	{
		Node tNode = mHRMController.getNode();
		Name tLocalNodeName = mHRMController.getNodeName(); 
		ControlEntity tControlEntity = pComChannel.getParent();
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
			Logging.log(this, "RECEIVED BULLY MESSAGE " + pPacketBully.getClass().getSimpleName() + " FROM " + pComChannel);

		if (pComChannel == null){
			Logging.err(this, "Communication channel is invalid.");
		}
		
		if (tControlEntity == null){
			Logging.err(this, "Control entity reference is invalid");
		}
		
		/***************************
		 * UPDATE PEER PRIORITY
		 ***************************/ 
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "      ..updating peer priority to: " + pPacketBully.getSenderPriority().getValue());
		}
		boolean tGotNewPeerPriority = pComChannel.setPeerPriority(pPacketBully.getSenderPriority());		

		/***************************
		 * REACT ON THE MESSAGE
		 ***************************/
		if (!tControlEntity.getHierarchyLevel().isHigher(this, mParent.getHierarchyLevel())){
			/**
			 * ELECT
			 */
			if(pPacketBully instanceof BullyElect)	{
				
				// cast to Bully elect packet
				BullyElect tPacketBullyElect = (BullyElect)pPacketBully;
				
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" an ELECT: " + tPacketBullyElect);
				}
	
				// update the state
				eventELECT(pComChannel);
			}
			
			/**
			 * REPLY
			 */
			if(pPacketBully instanceof BullyReply) {
				
				// cast to Bully replay packet
				BullyReply tReplyPacket = (BullyReply)pPacketBully;
	
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" a REPLY: " + tReplyPacket);
				}
	
				eventREPLY(pComChannel);
			}
			
			/**
			 * ANNOUNCE
			 */
			if(pPacketBully instanceof BullyAnnounce)  {
				// cast to Bully replay packet
				BullyAnnounce tAnnouncePacket = (BullyAnnounce)pPacketBully;
	
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" an ANNOUNCE: " + tAnnouncePacket);
				}
	
				if(!head()){
					eventANNOUNCE(pComChannel, tAnnouncePacket);
				}else{
					throw new RuntimeException("Got an ANNOUNCE as cluster head");
				}
			}
	
			/**
			 * RESIGN
			 */
			if(pPacketBully instanceof BullyResign)  {
				// cast to Bully replay packet
				BullyResign tResignPacket = (BullyResign)pPacketBully;
	
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" an RESIGN: " + tResignPacket);
				}
	
				if(!head()){
					eventRESIGN(pComChannel, tResignPacket);
				}else{
					throw new RuntimeException("Got a RESIGN as cluster head");
				}
			}

			/**
			 * PRIORITY UPDATE
			 */
			if(pPacketBully instanceof BullyPriorityUpdate) { //TODO: paket muss auch gesendet werden
				// cast to Bully replay packet
				BullyPriorityUpdate tPacketBullyPriorityUpdate = (BullyPriorityUpdate)pPacketBully;
	
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" a PRIORITY UPDATE: " + tPacketBullyPriorityUpdate);
				}
				
				eventPRIORITY_UPDATE(pComChannel);
			}
			
			/**
			 * LEAVE
			 */
			if(pPacketBully instanceof BullyLeave) {
				// cast to Bully leave packet
				BullyLeave tLeavePacket = (BullyLeave)pPacketBully;
	
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" a LEAVE: " + tLeavePacket);
				}
	
				eventLEAVE(pComChannel, tLeavePacket);
			}
			
			/**
			 * RETURN
			 */
			if(pPacketBully instanceof BullyReturn) {
				// cast to Bully leave packet
				BullyReturn tReturnPacket = (BullyReturn)pPacketBully;
	
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" a RETURN: " + tReturnPacket);
				}
	
				eventRETURN(pComChannel, tReturnPacket);
			}
		}else{
			Logging.log(this, "HIGHER LEVEL SENT BULLY MESSAGE " + pPacketBully.getClass().getSimpleName() + " FROM " + pComChannel);

			/**
			 * ANNOUNCE: a superior coordinator was elected and sends its announce towards its inferior coordinators 
			 */
			if(pPacketBully instanceof BullyAnnounce)  {
				// cast to Bully replay packet
				BullyAnnounce tAnnouncePacket = (BullyAnnounce)pPacketBully;
	
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" an ANNOUNCE: " + tAnnouncePacket);
				}
	
				if(tControlEntity instanceof Coordinator){
					Coordinator tCoordinator = (Coordinator)tControlEntity;
					
					// trigger: superior coordinator available	
					tCoordinator.eventClusterCoordinatorAvailable(pComChannel, tAnnouncePacket.getSenderName(), tAnnouncePacket.getCoordinatorID(), pComChannel.getPeerL2Address(), tAnnouncePacket.getCoordinatorDescription());
				}else{
					// HINT: this case shouldn't occur since the concept includes such messages only from a higher cluster towards its members (which are coordinators again)
					Logging.err(this, "EXPECTED COORDINATOR as parent control entity for comm. channel: " + pComChannel);
				}
			}else{
				Logging.log(this, "      ..ignoring Bully message: " + pPacketBully);
			}
		}

		/*****************************
		 * REACT ON THE NEW PEER PRIORITY
		 *****************************/
		if(tGotNewPeerPriority){
			if(mParent.getHierarchyLevel().isHigherLevel()){
				//checkNewPeerPriority(pComChannel);
			}
		}
	}

	/**
	 * Checks the distributed election
	 * 
	 * @param pComChannel the comm. channel for which the new priority was received
	 */
	private void checkNewPeerPriority(ComChannel pComChannel)
	{
		Logging.log(this, "Checking new priority from: " + pComChannel);

		Coordinator tCoordinator = mHRMController.getCoordinator(mParent.getHierarchyLevel());
		Logging.log(this, "    ..coordinator query result: " + tCoordinator); 

		/**
		 * Distributed Election
		 */
		if(!head()){
			// does this comm. channel lead to a remote node? -> so, a remote node could be a better election candidate than this node!
			if(pComChannel.toRemoteNode()){
				Logging.log(this, "    ..is channel to remote node: " + pComChannel.getPeerL2Address());
				
				/**
				 * Update the link activation for the comm. channel from where we received a new priority by:
				 * 		- send a LEAVE, if this node has a higher prio than the peer of the channel
				 * 		- send a RETURN, if this node has a lower prio than the peer of the channel
				 */
				// does there already exist a local coordinator for this hierarchy level?
				if(tCoordinator != null){
					Logging.log(this, "    ..local coordinator is known, has priority: " + tCoordinator.getPriority());
					Logging.log(this, "    ..remote priority: " + pComChannel.getPeerPriority());
					if(havingHigherPrioriorityThan(pComChannel)){
						if(!pComChannel.getLinkActivation()){
							Logging.err(this, "Inconsistent (should be true) link activation handling");
						}
						/**
						 * Send: LEAVE
						 */
						Logging.log(this, "    ..HAVING HIGHER PRIORITY than: " + pComChannel);
						distributeLEAVE("checkNewPeerPriority()");
					}else{
						if(pComChannel.getLinkActivation()){
							Logging.err(this, "Inconsistent (should be false) link activation handling");
						}
						/**
						 * Send: RETURN
						 */
						Logging.log(this, "    ..HAVING LOWER PRIORITY than: " + pComChannel);
						distributeRETURN("checkNewPeerPriority()");
					}
				}else{
					// coordinator at this hierarchy level is unknown
				}
			}else{
				// comm. channel leads to local node
			}
		}else{
			// only cluster member
		}
	}

	/**
	 * This is the central function for comparing two priorities.
	 * It returns true if the local priority is higher than the one of the peer (from the communication channel)
	 * 
	 * @param pComChannel the communication channel
	 * 
	 * @return true or false
	 */
	private boolean havingHigherPrioriorityThan(ComChannel pComChannel)
	{
		boolean tResult = false;
		boolean tDEBUG = true;
		
		/**
		 * Return true if the comm. channel has a deactivated link
		 */
		if(HRMConfig.Election.USE_LINK_STATES){
			if (!pComChannel.getLinkActivation()){
				return true;
			}
		}
		
		/**
		 * Compare the priorities
		 */
		if (mParent.getPriority().isHigher(this, pComChannel.getPeerPriority())){
			if (tDEBUG){
				Logging.log(this, "	        ..HAVING HIGHER PRIORITY than " + pComChannel.getPeerL2Address());
			}
			
			tResult = true;
		}else{
			if (mParent.getPriority().equals(pComChannel.getPeerPriority())){
				if (tDEBUG){
					Logging.log(this, "	        ..HAVING SAME PRIORITY like " + pComChannel.getPeerL2Address());
				}

				if(mHRMController.getNodeL2Address().isHigher(pComChannel.getPeerL2Address())) {
					if (tDEBUG){
						Logging.log(this, "	        ..HAVING HIGHER L2 address than " + pComChannel.getPeerL2Address());
					}

					tResult = true;
				}else{
					if (mHRMController.getNodeL2Address().isLower(pComChannel.getPeerL2Address())){
						if (tDEBUG){
							Logging.log(this, "	        ..HAVING LOWER L2 address " + mHRMController.getNodeL2Address() + " than " +  pComChannel.getPeerL2Address());
						}
					}else{
						if (tDEBUG){
							Logging.log(this, "	        ..DETECTED OWN LOCAL L2 address " + mHRMController.getNodeL2Address());
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
			}
		}
		
		return tResult;
	}

	/**
	 * SEND: priority update, triggered by ClusterMember when the priority is changed (e.g., if the base node priority was changed)
	 */
	public synchronized void updatePriority()
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			Logging.log(this, "SEND: priority updates");
		}
		
		/**
		 * trigger signaling of "priority update"
		 */
		distributePRIRORITY_UPDATE();
		
		/**
		 * check for winner
		 */
		if(mState == ElectorState.ELECTING){
			checkForWinner();
		}
	}

	/**
	 * Generates a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	@Override
	public String toString()
	{
		return toLocation() + "@" + mParent.toString() +"[" + mState + "]";
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
