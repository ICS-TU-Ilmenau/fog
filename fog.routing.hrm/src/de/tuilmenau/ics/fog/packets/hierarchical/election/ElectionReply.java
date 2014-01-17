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
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMName;

/**
 * PACKET: It is used when an answer to ElectionElect should be signaled.
 */
public class ElectionReply  extends SignalingMessageElection
{
	private static final long serialVersionUID = -4666721123778977947L;
	
	/**
	 * Stores the counter of created packets from this type
	 * This value is only used for debugging. It is not part of the HRM concept. 
	 */
	public static Long sCreatedPackets = new Long(0);

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pSenderPriority the priority of the message sender
	 */
	public ElectionReply(HRMName pSenderName, HRMName pReceiverName, ElectionPriority pSenderPriority)
	{
		super(pSenderName, pReceiverName, pSenderPriority);
		synchronized (sCreatedPackets) {
			sCreatedPackets++;
		}
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
}
