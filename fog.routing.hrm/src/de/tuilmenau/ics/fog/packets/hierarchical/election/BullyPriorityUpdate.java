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
 * PACKET: If the priority of a node changes this object has to be used to inform other cluster members about the change.
 * 		   The packet has to be send as broadcast.
 */
public class BullyPriorityUpdate extends SignalingMessageBully implements ISignalingMessageHrmBroadcastable
{
	private static final long serialVersionUID = -8819106581802846812L;
	
	public static long sCreatedPackets = 0;

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pSenderPriroity the Bully priority of the message sender
	 */
	public BullyPriorityUpdate(Name pSenderName, BullyPriority pSenderPriroity)
	{
		super(pSenderName, HRMID.createBroadcast(), pSenderPriroity);
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
		BullyPriorityUpdate tResult = new BullyPriorityUpdate(getSenderName(), getSenderPriority());
		
		super.duplicate(tResult);

		//Logging.log(this, "Created duplicate packet: " + tResult);
		
		return tResult;
	}
}
