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
import de.tuilmenau.ics.fog.routing.hierarchical.election.BullyPriority;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.ui.Logging;

/**
 * PACKET: It is used when an election start is signaled.
 *         The packet has to be send as broadcast.
 */
public class BullyElect extends SignalingMessageBully implements ISignalingMessageHrmBroadcastable
{
	private static final long serialVersionUID = -335936730603961378L;

	public static long sCreatedPackets = 0;

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pSenderPriority the Bully priority of the message sender
	 */
	public BullyElect(Name pSenderName, BullyPriority pSenderPriority)
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
		BullyElect tResult = new BullyElect(getSenderName(), getSenderPriority());
		
		super.duplicate(tResult);
		
		//Logging.log(this, "Created duplicate packet: " + tResult);
		
		return tResult;
	}
}
