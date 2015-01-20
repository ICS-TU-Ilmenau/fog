/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.election;

import de.tuilmenau.ics.fog.routing.hierarchical.election.ElectionPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;
import de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;

/**
 * PACKET: It is used when a new coordinator is signaled to all cluster members.
 * 		   The packet has to be send as broadcast.
 */
public class ElectionWinner extends SignalingMessageElection implements ISignalingMessageHrmBroadcastable
{
	private static final long serialVersionUID = 794175467972815277L;

	/**
	 * Stores the unique coordinator ID of the announcing coordinator.
	 * This value can be derived from the multiplex header. Hence, it is not an additional overhead during packet transmission.
	 */
	private long mCoordinatorID = -1;
	
	/**
	 * Stores some GUI description about the announcing coordinator
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	private String mCoordinatorDescription = null;
	
	/**
	 * Stores the counter of created packets from this type
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static Long sCreatedPackets = new Long(0);

	/**
	 * Stores the counter of sent broadcasts from this type
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static Long sSentBroadcasts = new Long(0);

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender (coordinator)
	 * @param pSenderPriority the priority of the message sender (coordinator)
	 * @param pCoordinatorID the unique ID of the message sender (coordinator)
	 * @param pCoordinatorDescription a description text of the coordinator
	 */
	public ElectionWinner(HRMName pSenderName, ElectionPriority pSenderPriority, long pCoordinatorID, String pCoordinatorDescription)
	{
		super(pSenderName, HRMID.createBroadcast(), pSenderPriority);
		mCoordinatorDescription = pCoordinatorDescription;
		mCoordinatorID = pCoordinatorID;
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
	}
	
	/**
	 * Returns the descriptive string about the coordinator which announces it coordination
	 * 
	 * @return the descriptive string
	 */
	public String getCoordinatorDescription()
	{
		return new String(mCoordinatorDescription);
	}
	
	/**
	 * Returns the unique coordinator ID
	 * 
	 * @return the unique coordinator ID
	 */
	public long getCoordinatorID()
	{
		return mCoordinatorID;
	}

	/**
	 * Returns the counter of created packets from this type
	 *  
	 * @return the packet counter
	 */
	public static long getCreatedPackets()
	{
		long tResult = 0;
		
		synchronized (sCreatedPackets) {
			tResult = sCreatedPackets;
		}
		
		return tResult;
	}

	/**
	 * Returns a duplicate of this packet
	 * 
	 * @return the duplicate packet
	 */
	@Override
	public SignalingMessageHrm duplicate()
	{
		ElectionWinner tResult = new ElectionWinner(getSenderName(), getSenderPriority(), getCoordinatorID(), getCoordinatorDescription());

		super.duplicate(tResult);

		//Logging.log(this, "Created duplicate packet: " + tResult);
		
		return tResult;
	}

	/**
	 * Accounts a broadcast of this packet type
	 */
	/* (non-Javadoc)
	 * @see de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable#accountBroadcast()
	 */
	@Override
	public void accountBroadcast()
	{
		super.accountBroadcast();
		synchronized (sCreatedPackets) {
			sCreatedPackets--;
			sSentBroadcasts++;
		}
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "/" + getOriginalMessageNumber() + "](Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + ", SenderPrio=" + getSenderPriority().getValue() + ", Coordinator=" + mCoordinatorDescription + ")";
	}
}
