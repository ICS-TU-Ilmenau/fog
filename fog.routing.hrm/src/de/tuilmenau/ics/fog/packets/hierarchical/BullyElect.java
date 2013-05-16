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
 *
 * Packet that is used to initiate an election
 */
public class BullyElect implements Serializable
{
	private static final long serialVersionUID = -335936730603961378L;

	/**
	 * 
	 * @param pBullyPriority senders priority
	 * @param pElectionLevel hierarchical level the election is processed
	 * @param pInitiatorName is the identification of the initiator of this message
	 * @param pAS is the name of the autonomous system
	 */
	public BullyElect(float pBullyPriority, int pElectionLevel, Name pInitiatorName)
	{
		mBullyPriority = pBullyPriority;
		mInitiatorName = pInitiatorName;
		mElectionLevel = pElectionLevel;
	}
	
	/**
	 * 
	 * @return find out the priority of the initiator of this packet
	 */
	public float getPriority()
	{
		return mBullyPriority;
	}
	
	/**
	 * 
	 * @return level the election is carried out
	 */
	public int getElectionLevel()
	{
		return mElectionLevel;
	}
	
	private float mBullyPriority = 0;
	private Name mInitiatorName = null;
	private int mElectionLevel = 0;
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + ":" + mInitiatorName + ")" + ":Priority(" + mBullyPriority + "L" + mElectionLevel + ")";
	}
}
