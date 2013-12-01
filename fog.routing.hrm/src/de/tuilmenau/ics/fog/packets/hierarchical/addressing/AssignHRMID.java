/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.addressing;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;

/**
 * PACKET: This packet is used to assign a new address. Such packets are sent from a coordinator to all cluster members.
 */
public class AssignHRMID  extends SignalingMessageHrm
{
	/**
	 * Stores the HRMID which was assign to the target of this packet.
	 */
	private HRMID mHRMID = null;

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
	 * @param pAssignedHRMID the assigned new HRMID for the receiver
	 */
	public AssignHRMID(Name pSenderName, Name pReceiverName, HRMID pAssignedHRMID)
	{
		super(pSenderName, pReceiverName);
		mHRMID = pAssignedHRMID;
		sCreatedPackets++;
	}
	
	/**
	 * Returns the assigned new HRMID
	 *  
	 * @return the new assigned HRMID
	 */
	public HRMID getHRMID()
	{
		return mHRMID;
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + ", newHRMID=" + getHRMID() + ")";
	}
}
