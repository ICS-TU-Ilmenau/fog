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

import java.util.LinkedList;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;

/**
 * PACKET: This packet is used to revoke addresses. Such packets are sent from a coordinator to all cluster members if its role got invalid.
 */
public class RevokeHRMIDs  extends SignalingMessageHrm
{
	/**
	 * Stores the HRMIDs which have to be revoked by this packet.
	 */
	private LinkedList<HRMID> mHRMIDs = null;

	/**
	 * For using the class within (de-)serialization processes.  
	 */
	private static final long serialVersionUID = -1674381264586284319L;

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pAssignedHRMIDs the assigned HRMIDs which should be revoked
	 */
	public RevokeHRMIDs(Name pSenderName, Name pReceiverName, LinkedList<HRMID> pAssignedHRMIDs)
	{
		super(pSenderName, pReceiverName);
		mHRMIDs = pAssignedHRMIDs;
	}
	
	/**
	 * Returns the revoked HRMIDs
	 *  
	 * @return the revoked HRMIDs
	 */
	public LinkedList<HRMID> getHRMIDs()
	{
		return mHRMIDs;
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + ", revokedHRMIDs=" + getHRMIDs() + ")";
	}
}
