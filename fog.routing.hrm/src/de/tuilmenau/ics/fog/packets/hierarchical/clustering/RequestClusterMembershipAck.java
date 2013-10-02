/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.hierarchical.clustering;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.packets.hierarchical.SignalingMessageHrm;
import de.tuilmenau.ics.fog.routing.hierarchical.management.ClusterName;

/**
 * PACKET: This packet is used when a coordinator wants to join an existing superior cluster
 */
public class RequestClusterMembershipAck extends SignalingMessageHrm
{
	private static final long serialVersionUID = 445881657397476245L;

	/**
	 * Store the ClusterName of the sender
	 */
	private ClusterName mSenderClusterName = null;
	
	/**
	 * Constructor
	 * 
	 * @param pSenderName the name of the message sender
	 * @param pReceiverName the name of the message receiver
	 * @param pSenderClusterName the ClusterName of the sender
	 */
	public RequestClusterMembershipAck(Name pSenderName, Name pReceiverName, ClusterName pSenderClusterName)
	{
		super(pSenderName, pReceiverName);
		
		mSenderClusterName = pSenderClusterName;
	}

	/**
	 * Returns the ClusterName which describes the desired cluster
	 * 
	 * @return the ClusterName description
	 */
	public ClusterName getSenderClusterName()
	{
		return mSenderClusterName;
	}

	/**
	 * Returns an object describing string
	 * 
	 *  @return the describing string
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getMessageNumber() + "](Sender=" + getSenderName() + ", Receiver=" + getReceiverName() + ", SenderClusterName="+ getSenderClusterName() + ")";
	}
}
