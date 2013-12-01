/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;

/**
 * PACKET: This packet is used to probe a comm. channel.
 * 		   The packet is not part of the concept. It is only useful for debugging purposes. 
 */
public class ProbePacket  extends SignalingMessageHrm
{
	/**
	 * For using the class within (de-)serialization processes.  
	 */
	private static final long serialVersionUID = -1674381264586284319L;

	public static long sCreatedPackets = 0;

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 */
	public ProbePacket(Name pSenderName, Name pReceiverName)
	{
		super(pSenderName, pReceiverName);
		sCreatedPackets++;
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + ")";
	}
}
