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

import java.io.Serializable;

import de.tuilmenau.ics.fog.facade.Name;

/**
 * PACKET: It is used when an election start is signaled.
 */
public class BullyElect implements Serializable //TV
{
	private static final long serialVersionUID = -335936730603961378L;

	/**
	 * 
	 * @param pSenderName senders identification
	 * @param pSenderPriority senders priority for the election
	 * @param pHierarchyLevel hierarchy level for this election
	 */
	public BullyElect(Name pSenderName, float pSenderPriority, int pHierarchyLevel)
	{
		mSenderPriority = pSenderPriority;
		mSenderName = pSenderName;
		mHierarchyLevel = pHierarchyLevel;
	}
	
	/**
	 * Determine the sender's priority.
	 * 
	 * @return find out the priority of the initiator of this packet
	 */
	public float getSenderPriority()
	{
		return mSenderPriority;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + ": Sender=" + mSenderName + ", Priority=" + mSenderPriority + ", Level=" + mHierarchyLevel;
	}

	// ########################################################################################################
	/**
	 * The priority of the sender for the BULLY election process.
	 */
	private float mSenderPriority = 0;
	
	/**
	 * The name of the sender of this message. This is always a name of a physical node.
	 */
	private Name mSenderName = null;
	
	/**
	 * The hierarchy level for this election.
	 */
	private int mHierarchyLevel = 0;
}
