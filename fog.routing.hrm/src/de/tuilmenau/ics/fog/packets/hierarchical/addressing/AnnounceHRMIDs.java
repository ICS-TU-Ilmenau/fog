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
import de.tuilmenau.ics.fog.packets.hierarchical.ISignalingMessageHrmBroadcastable;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.packets.hierarchical.topology.AnnounceCoordinator;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.HRMID;
import de.tuilmenau.ics.fog.routing.naming.hierarchical.L2Address;

/**
 * PACKET: This packet is used to announce local HRMID addresses. Such packets are sent from a ClusterMember/Cluster to other members of the cluster.
 */
public class AnnounceHRMIDs extends SignalingMessageHrm implements ISignalingMessageHrmBroadcastable
{
	/**
	 * Stores the HRMIDs which have to be revoked by this packet.
	 */
	private LinkedList<HRMID> mHRMIDs = null;

	/**
	 * For using the class within (de-)serialization processes.  
	 */
	private static final long serialVersionUID = -8757081636576993095L;

	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pAssignedHRMIDs the assigned HRMIDs which should be revoked
	 */
	public AnnounceHRMIDs(Name pSenderName, Name pReceiverName, LinkedList<HRMID> pAssignedHRMIDs)
	{
		super(pSenderName, pReceiverName);
		mHRMIDs = pAssignedHRMIDs;
	}
	
	/**
	 * Returns the revoked HRMIDs
	 *  
	 * @return the revoked HRMIDs
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<HRMID> getHRMIDs()
	{
		return (LinkedList<HRMID>) (mHRMIDs != null ? mHRMIDs.clone() : null);
	}

	/**
	 * Returns a duplicate of this packet
	 * 
	 * @return the duplicate packet
	 */
	@Override
	public SignalingMessageHrm duplicate()
	{
		AnnounceHRMIDs tResult = new AnnounceHRMIDs(getSenderName(), getReceiverName(), getHRMIDs());
		
		super.duplicate(tResult);

		//Logging.log(this, "Created duplicate packet: " + tResult);
		
		return tResult;
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName()  + ", Receiver=" + getReceiverName() + ", announcedHRMIDs=" + getHRMIDs() + ")";
	}
}
