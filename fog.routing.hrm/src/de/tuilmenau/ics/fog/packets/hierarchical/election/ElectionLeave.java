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

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.election.ElectionPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;

/**
 * PACKET: This packet is used to inform that a cluster member wants to be a passive candidate of an election.
 */
public class ElectionLeave extends SignalingMessageElection implements ISignalingMessageHrmBroadcastable
{

	private static final long serialVersionUID = 7774205916502000178L;

	public static long sCreatedPackets = 0;

	/**
	 * Constructor
	 * 
	 * @param pSenderName
	 * @param pReceiverName
	 */
	public ElectionLeave(Name pSenderName, ElectionPriority pSenderPriority)
	{
		super(pSenderName, HRMID.createBroadcast(), pSenderPriority);
		sCreatedPackets++;
	}

	/**
	 * Returns a duplicate of this packet
	 * 
	 * @return the duplicate packet
	 */
	@Override
	public SignalingMessageHrm duplicate()
	{
		ElectionLeave tResult = new ElectionLeave(getSenderName(), getSenderPriority());
		sCreatedPackets--;

		super.duplicate(tResult);

		//Logging.log(this, "Created duplicate packet: " + tResult);
		
		return tResult;
	}
}
