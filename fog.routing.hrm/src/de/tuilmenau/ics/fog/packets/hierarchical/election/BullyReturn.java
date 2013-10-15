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
 * PACKET: This packet is used to inform that a cluster member wants to return as active candidate to an election.
 */
public class BullyReturn extends SignalingMessageBully implements ISignalingMessageHrmBroadcastable
{

	private static final long serialVersionUID = 7774205916502000178L;

	/**
	 * Constructor
	 * 
	 * @param pSenderName
	 * @param pReceiverName
	 */
	public BullyReturn(Name pSenderName, BullyPriority pSenderPriority)
	{
		super(pSenderName, HRMID.createBroadcast(), pSenderPriority);
	}

	/**
	 * Returns a duplicate of this packet
	 * 
	 * @return the duplicate packet
	 */
	@Override
	public SignalingMessageHrm duplicate()
	{
		BullyReturn tResult = new BullyReturn(getSenderName(), getSenderPriority());
		
		super.duplicate(tResult);

		//Logging.log(this, "Created duplicate packet: " + tResult);
		
		return tResult;
	}
}
