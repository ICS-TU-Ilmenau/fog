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

import java.util.Random;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAlive;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyAnnounce;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyElect;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyPriorityUpdate;
import de.tuilmenau.ics.fog.packets.hierarchical.election.BullyReply;
import de.tuilmenau.ics.fog.packets.hierarchical.election.SignalingMessageBully;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMConfig;
import de.tuilmenau.ics.fog.routing.hierarchical.HRMController;
import de.tuilmenau.ics.fog.routing.hierarchical.Localization;
import de.tuilmenau.ics.fog.routing.hierarchical.management.Cluster;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ComChannel;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ControlEntity;
import de.tuilmenau.ics.fog.routing.hierarchical.management.Coordinator;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ICluster;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.ui.Logging;

//TODO: invalidateElection() missing, if error during communication with coordinator occurred

/**
 * This class is responsible for coordinator elections. It is instantiated per cluster. 
 *
 */
public class Elector implements Localization
{
	public enum ElectorState {
		START,    // Constructor
		IDLE,     // no coordinator known, no election running
		ELECTING, // election process is currently running
		ELECTED,   // election process has established common consensus about the coordinator of the cluster
		ERROR // election process run into an error state
	}

	//TODO: r�ckkehr von ELECTED zu ELECTING, wenn BullyAlive von koordinator ausbleibt
	
	/** 
	 * Stores the internal state of the elector
	 */
	private ElectorState mState;
	
	/**
	 * Pointer to the parent cluster, which owns this elector
	 */
	private Cluster mParentCluster = null;

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
	 * Stores the timestamp of the last ElectBroadcast signaling
	 */
	private Double mTimestampLastElectBroadcast =  new Double(0);
	
	public Elector(Cluster pCluster)
	{
		mState = ElectorState.START;
		mParentCluster = pCluster;
		mElectionWon = false;
		
		// set IDLE state
		setElectorState(ElectorState.IDLE);
		
		// start coordinator election for the created HRM instance if desired
		if(HRMConfig.Hierarchy.BUILD_AUTOMATICALLY) {
			signalElectBroadcast();
		}
	}
	
	/**
	 * Starts the election process. This function is usually called by the GUI.
	 */
	public void startElection()
	{
		switch(getElectorState()){
			case IDLE:
				// set correct elector state
				setElectorState(ElectorState.ELECTING);

				// do we know more than 0 external cluster members?
				if (mParentCluster.countClusterMembers() > 0){
					Logging.log(this, "Trying to ask all cluster members for their Bully priority");
					signalElectBroadcast();
				}else{
					Logging.log(this, "I am automatically the election WINNER because no alternative cluster member is known");
					eventElectionWon();
				}
				break;
			case ELECTED:
				Logging.log(this, "Election has already finished, coordinator is valid: " + isCoordinatorValid());
				break;
			case ELECTING:
				Logging.log(this, "Election is already running");
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
					Logging.log(this, "STATE TRANSITION from " + getElectorState() + " to " + pNewState);
				}
	
				// set new state
				mState = pNewState;
			}
		} else {
			throw new RuntimeException(toLocation() + "-cannot change its state from " + mState +" to " + pNewState);
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
	 * Determines if the coordinator for this election domain (=cluster) is (still) valid-
	 * 
	 * @return true if valid, otherwise false
	 */
	public boolean isCoordinatorValid()
	{
		return (getElectorState() == ElectorState.ELECTED);
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
		double tNow = mParentCluster.getHRMController().getSimulationTime();
		double tTimeout = mTimestampLastElectBroadcast.longValue() + TIMEOUT_FOR_REPLY;
				
		Logging.log(this, "Checking timing of ELECT BROADCAST: last=" + mTimestampLastElectBroadcast.longValue() + ", MinPeriod=" + TIMEOUT_FOR_REPLY + ", now=" + tNow + ", MinTime=" + tTimeout);
		
		// is timing okay?
		if ((mTimestampLastElectBroadcast.doubleValue() == 0) || (tNow > mTimestampLastElectBroadcast.doubleValue() + TIMEOUT_FOR_REPLY)){
			tResult = true;
			mTimestampLastElectBroadcast = new Double(tNow);
		}
		
		return tResult;
	}
	
	/**
	 * SIGNAL: start the election by signaling BULLY ELECT to all cluster members
	 */
	private void signalElectBroadcast()
	{
		if (getElectorState() == ElectorState.ELECTING){
			if (isTimingOkayOfElectBroadcast()){
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "SENDELECTIONS()-START, electing cluster is " + mParentCluster);
					Logging.log(this, "SENDELECTIONS(), cluster members: " + mParentCluster.countClusterMembers());
				}
		
				// create the packet
				BullyElect tPacketBullyElect = new BullyElect(mParentCluster.getHRMController().getNodeName(), mParentCluster.getPriority());
				
				// send broadcast
				mParentCluster.sendClusterBroadcast(tPacketBullyElect);
				
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "SENDELECTIONS()-END");
				}
			}else{
				Logging.log(this, "signalElectBroadcast() was triggered too frequently, timeout isn't reached yet, skipping this action");
			}

		}else{
			Logging.warn(this, "Election has wrong state " + getElectorState() + " for signaling an ELECTION START, ELECTING expected");

			// set correct elector state
			setElectorState(ElectorState.ERROR);
		}			
	}

	/**
	 * SIGNAL: ends the election by signaling BULLY ANNOUNCE to all cluster members 		
	 */
	private void signalAnnounceBroadcast()
	{
		if (getElectorState() == ElectorState.ELECTED){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDANNOUNCE()-START, electing cluster is " + mParentCluster);
				Logging.log(this, "SENDANNOUNCE(), cluster members: " + mParentCluster.getComChannels().size());
			}
	
			// HINT: the coordinator has to be already created here

			if (mParentCluster.getCoordinator() != null){
				// create the packet
				BullyAnnounce tPacketBullyAnnounce = new BullyAnnounce(mParentCluster.getHRMController().getNodeName(), mParentCluster.getPriority(), mParentCluster.getCoordinator().toLocation() + "@" + HRMController.getHostName(), mParentCluster.getToken());
		
				// send broadcast
				mParentCluster.sendClusterBroadcast(tPacketBullyAnnounce);
			}else{
				Logging.warn(this, "Election has wrong state " + getElectorState() + " for signaling an ELECTION END, ELECTED expected");
				
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
	 * SIGNAL: report itself as alive by signaling BULLY ALIVE to all cluster members
	 */
	private void signalAliveBroadcast()
	{
		if (HRMConfig.Election.SEND_BULLY_ALIVES){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDALIVE()-START, electing cluster is " + mParentCluster);
				Logging.log(this, "SENDALIVE(), cluster members: " + mParentCluster.getComChannels().size());
			}
	
			// create the packet
			BullyAlive tPacketBullyAlive = new BullyAlive(mParentCluster.getHRMController().getNodeName());
	
			// send broadcast
			mParentCluster.sendClusterBroadcast(tPacketBullyAlive);
	
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDALIVE()-END");
			}
		}else{
			// BullyAlive messages are currently deactivated
		}			
	}

	/**
	 * SIGNAL: report itself as alive by signaling BULLY ALIVE to all cluster members
	 * 
	 * @param pComChannel the communication channel along which the RESPONSE should be send
	 */
	private void signalResponse(ComChannel pComChannel)
	{
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "SENDRESPONSE()-START, electing cluster is " + mParentCluster);
				Logging.log(this, "SENDRESPONSE(), cluster members: " + mParentCluster.getComChannels().size());
			}
		}

		// create REPLY packet
		BullyReply tReplyPacket = new BullyReply(mParentCluster.getHRMController().getNodeName(), pComChannel.getPeerHRMID(), mParentCluster.getPriority());
			
		// send the answer packet
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
			Logging.log(this, "BULLY-sending to \"" + pComChannel + "\" a REPLY: " + tReplyPacket);

		// send message
		pComChannel.sendPacket(tReplyPacket);

		Logging.log(this, "SENDRESPONSE()-END");
	}

	
	/**
	 * EVENT: sets the local node as coordinator for the parent cluster. 
	 */
	private void eventElectionWon()
	{
		if ((!isWinner()) || (!isCoordinatorValid())){
			Logging.log(this, "ELECTION WON for cluster " + mParentCluster);
			
			// mark as election winner
			mElectionWon = true;
			
			// set correct elector state
			setElectorState(ElectorState.ELECTED);
	
			// get the node
			Node tNode = mParentCluster.getHRMController().getNode();
			
			// create new coordinator instance
			Coordinator tNewCoordinator = new Coordinator(mParentCluster);
	
			Name tNodeName = tNode.getRoutingService().getNameFor(tNode.getCentralFN());
			
			mParentCluster.setSuperiorCoordinator(null, tNode.getCentralFN().getName(), /* token */ new Random(System.currentTimeMillis()).nextInt(), (L2Address)tNodeName);
	
			// send BULLY ANNOUNCE in order to signal all cluster members that we are the coordinator
			signalAnnounceBroadcast();

			// trigger event "announced" for the coordinator
			tNewCoordinator.eventAnnounced();
			
		}else{
			Logging.warn(this, "Cluster " + mParentCluster + " has still a valid and known coordinator, skipping eventElectionWon() here");
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
		
		// set correct elector state
		setElectorState(ElectorState.ELECTED);
	}

	/**
	 * EVENT: the election process was triggered by another cluster member
	 */
	private void eventReceivedElect()
	{
		// set correct elector state
		setElectorState(ElectorState.ELECTING);
	}
	
	/**
	 * EVENT: another cluster member has sent its Bully priority 
	 */
	private void eventReceivedReply()
	{
		BullyPriority tHighestPrio = null;
		ComChannel tExternalWinner = null;
		boolean tIsWinner = false;
		boolean tElectionComplete = true;
		
		/**
		 * Find the highest priority of all external cluster members
		 */
		Logging.log(this, "Searching for highest priority...");
		for(ComChannel tComChannel : mParentCluster.getComChannels()) {
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
			 * find the highest priority in the cluster
			 */
			if((tHighestPrio == null) || (tPriority.isHigher(this, tHighestPrio))) {
				tHighestPrio = tPriority;
				tExternalWinner = tComChannel;
			}
		}
		
		/**
		 * Check if election is complete
		 */
		if (tElectionComplete){
			/**
			 * Is the local priority higher?
			 */
			if (mParentCluster.getPriority().isHigher(this, tHighestPrio)){
				Logging.log(this, "	        ..HAVING HIGHER PRIORITY than " + tExternalWinner.getPeerL2Address());
	
				// we are the absolute winner
				tIsWinner = true;
			}else{
				if (mParentCluster.getPriority().equals(tHighestPrio)){
					Logging.log(this, "	        ..HAVING SAME PRIORITY like " + tExternalWinner.getPeerL2Address());
	
					if (mParentCluster.getPriority().getUniqueID() > tHighestPrio.getUniqueID()){
						Logging.log(this, "	        ..HAVING HIGHER priority ID than " + tExternalWinner.getPeerL2Address());
	
						// mark as winner
						tIsWinner = true;
					}else{
						if (mParentCluster.getPriority().getUniqueID() < tHighestPrio.getUniqueID()){
							Logging.log(this, "	        ..HAVING LOWER priority ID " + mParentCluster.getPriority().getUniqueID() + " than " +  tHighestPrio.getUniqueID() + " of " + tExternalWinner.getPeerL2Address());
						}else{
							Logging.err(this, "	        ..HAVING SAME priority ID " + mParentCluster.getPriority().getUniqueID() + " like " +  tHighestPrio.getUniqueID() + " of " + tExternalWinner.getPeerL2Address());
						}
					}
				}
			}
			
			/**
			 * React on the result
			 */
			if(tIsWinner) {
				Logging.log(this, "	        ..I AM WINNER");
				eventElectionWon();
			}else{
				if (tExternalWinner != null){
					Logging.log(this, "	        ..seeing " + tExternalWinner.getPeerL2Address() + " as election winner");
				}else{
					Logging.err(this, "External winner is unknown but also I am not the winner");
				}
				eventElectionLost();
			}
		}else{
			// election is incomplete: we are still waiting for some priority value(s)
		}
	}
	
	/**
	 * Handles a Bully signaling packet
	 * 
	 * @param pPacketBully the packet
	 * @param pComChannel the communication channel from where the message was received
	 */
	@SuppressWarnings("unused")
	public void handleSignalingMessageBully(SignalingMessageBully pPacketBully, ComChannel pComChannel)
	{
		Node tNode = mParentCluster.getHRMController().getNode();
		Name tLocalNodeName = mParentCluster.getHRMController().getNodeName(); 
		ControlEntity tControlEntity = pComChannel.getParent();
		
		if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
			Logging.log(this, "RECEIVED BULLY MESSAGE FROM " + pComChannel);

		if (pComChannel == null){
			Logging.err(this, "Communication channel is invalid.");
		}
		
		if (tControlEntity == null){
			Logging.err(this, "Control entity reference is invalid");
		}
		
		// update the stored Bully priority of the cluster member
		pComChannel.setPeerPriority(pPacketBully.getSenderPriority());		

		/**
		 * ELECT
		 */
		if(pPacketBully instanceof BullyElect)	{
			
			// cast to Bully elect packet
			BullyElect tPacketBullyElect = (BullyElect)pPacketBully;
			
			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
				Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" an ELECT: " + tPacketBullyElect);
			}

//			if ((tSource.getSuperiorCoordinatorCEP() != null) && (tSource.getHighestPriority().isHigher(this, tPacketBullyElect.getSenderPriority()))) {
//				
//				if (tSource.getHRMController().equals(tLocalNodeName)) {
//					BullyAnnounce tAnnouncePacket = new BullyAnnounce(tLocalNodeName, tSource.getBullyPriority(), "CEP-to?", tSource.getToken());
//					
//					for(CoordinatorCEPChannel tCEP : tSource.getClusterMembers()) {
//						tAnnouncePacket.addCoveredNode(tCEP.getPeerL2Address());
//					}
//					if(tAnnouncePacket.getCoveredNodes() == null || (tAnnouncePacket.getCoveredNodes() != null && tAnnouncePacket.getCoveredNodes().isEmpty())) {
//						Logging.log(this, "Sending announce that does not cover anyhting");
//					}
//
//					// send packet
//					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
//						Logging.log(this, "BULLY-sending to \"" + tSourceDescription + "\" an ANNOUNCE: " + tAnnouncePacket);
//					pSourceClusterMember.sendPacket(tAnnouncePacket);
//					
//				} else {
//					// create ALIVE packet
//					BullyAlive tAlivePacket = new BullyAlive(tLocalNodeName);
//					
//					// send packet
//					if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
//						Logging.log(this, "BULLY-sending to \"" + tSourceDescription + "\" an ALIVE: " + tAlivePacket);
//					pSourceClusterMember.sendPacket(tAlivePacket);
//					//TODO: packet is sent but never parsed or a timeout timer reset!!
//				}
//			} else {

			// update the state
			eventReceivedElect();
		
			// answer the "elect" message
			signalResponse(pComChannel);
				
			/**
			 * do we have a higher priority than the peer?
			 */
			boolean tHavingHigherPrio = false;
			if (mParentCluster.getPriority().isHigher(this, pComChannel.getPeerPriority())){
				if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY){
					Logging.log(this, "	        ..the local priority is HIGHER than the remote, starting ELECTION by ourself");
				}
				
				tHavingHigherPrio = true;
			}else{
				if (mParentCluster.getPriority().equals(pComChannel.getPeerPriority())){
					Logging.log(this, "	        ..HAVING SAME PRIORITY like " + pComChannel.getPeerL2Address());
	
					if (mParentCluster.getPriority().getUniqueID() > pComChannel.getPeerPriority().getUniqueID()){
						Logging.log(this, "	        ..HAVING HIGHER priority ID than " + pComChannel.getPeerL2Address());
	
						tHavingHigherPrio = true;
					}else{
						Logging.log(this, "	        ..HAVING LOWER/EQUAL priority ID " + mParentCluster.getPriority().getUniqueID() + " than " +  pComChannel.getPeerPriority().getUniqueID() + " of " + pComChannel.getPeerL2Address());
					}
				}
			}

			/**
			 * send ELECT broadcasts
			 */
			if (tHavingHigherPrio){
				// broadcast "ELECT" ourself
				signalElectBroadcast();
			}
			
//			}
		}
		
		/**
		 * REPLY
		 */
		if(pPacketBully instanceof BullyReply) {
			
			// cast to Bully replay packet
			BullyReply tReplyPacket = (BullyReply)pPacketBully;

			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
				Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" a REPLY: " + tReplyPacket);

			eventReceivedReply();
		}
		
		/**
		 * ANNOUNCE
		 */
		if(pPacketBully instanceof BullyAnnounce)  {
			// cast to Bully replay packet
			BullyAnnounce tAnnouncePacket = (BullyAnnounce)pPacketBully;

			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
				Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" an ANNOUNCE: " + tAnnouncePacket);

			eventElectionLost();

//			//TODO: only an intermediate cluster on level 0 is able to store an announcement and forward it once a coordinator is set
			tControlEntity.handleBullyAnnounce(tAnnouncePacket, pComChannel);
		}

		/**
		 * PRIORITY UPDATE
		 */
		if(pPacketBully instanceof BullyPriorityUpdate) {
			// cast to Bully replay packet
			BullyPriorityUpdate tPacketBullyPriorityUpdate = (BullyPriorityUpdate)pPacketBully;

			if (HRMConfig.DebugOutput.GUI_SHOW_SIGNALING_BULLY)
				Logging.log(this, "BULLY-received from \"" + tControlEntity + "\" a PRIORITY UPDATE: " + tPacketBullyPriorityUpdate);
		}
	}

	/**
	 * Determine the parent cluster, which owns this elector. 
	 * @return the parent cluster
	 */
	public Cluster getCluster()
	{
		return mParentCluster;
	}
	
	/**
	 * Generates a descriptive string about the object
	 * 
	 * @return the descriptive string
	 */
	@Override
	public String toString()
	{
		return toLocation() + (mParentCluster != null ? "(Cluster=" + mParentCluster.toString() + ")" : "");
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
		
		if (getCluster() != null){
			tResult = getClass().getSimpleName() + "@" + getCluster().getHRMController().getNodeGUIName() + "@" + getCluster().getHierarchyLevel().getValue();
		}else{
			tResult = getClass().getSimpleName();
		}			
		
		return tResult;
	}
}
