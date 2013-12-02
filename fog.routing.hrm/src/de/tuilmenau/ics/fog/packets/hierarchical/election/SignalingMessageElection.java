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
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.election.ElectionPriority;

public class SignalingMessageElection extends SignalingMessageHrm
{
	private static final long serialVersionUID = -7721094891385820251L;

	/**
	 * This is the Election priority of the message sender.
	 */
	private ElectionPriority mSenderPriority = null;

	public static long sCreatedPackets = 0;

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pSenderPriority the priority of the message sender
	 * @param pReceiverName the name of the message receiver
	 */
	public SignalingMessageElection(Name pSenderName, Name pReceiverName, ElectionPriority pSenderPriority)
	{
		super(pSenderName, pReceiverName);
		mSenderPriority = pSenderPriority;
		sCreatedPackets++;
	}
	
	/**
	 * Determine the sender's priority.
	 * 
	 * @return the priority of the message sender
	 */
	public ElectionPriority getSenderPriority()
	{
		if(mSenderPriority != null){
			return mSenderPriority.clone();
		}else{
			return ElectionPriority.create(this);
		}
	}

	/**
	 * Duplicates all member variables for another packet
	 * 
	 * @param pOtherPacket the other packet
	 */
	public void duplicate(SignalingMessageElection pOtherPacket)
	{
		super.duplicate(pOtherPacket);
		
		// update the recorded source route
		pOtherPacket.mSenderPriority = getSenderPriority();
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "/" + getOriginalMessageNumber() + "](Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + (getSenderPriority() != null ? ", SenderPrio=" + getSenderPriority().getValue() : "") + ")";
	}
}
