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

import de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.election.ElectionPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;

/**
 * PACKET: This packet is used to inform that a cluster member wants to return as active candidate to an election.
 */
public class ElectionReturn extends SignalingMessageElection implements ISignalingMessageHrmBroadcastable
{

	private static final long serialVersionUID = 7774205916502000178L;

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
	 * @param pSenderName
	 * @param pReceiverName
	 */
	public ElectionReturn(HRMName pSenderName, ElectionPriority pSenderPriority)
	{
		super(pSenderName, HRMID.createBroadcast(), pSenderPriority);
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
	}

	/**
	 * Returns a duplicate of this packet
	 * 
	 * @return the duplicate packet
	 */
	@Override
	public SignalingMessageHrm duplicate()
	{
		ElectionReturn tResult = new ElectionReturn(getSenderName(), getSenderPriority());

		super.duplicate(tResult);

		//Logging.log(this, "Created duplicate packet: " + tResult);
		
		return tResult;
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
}
