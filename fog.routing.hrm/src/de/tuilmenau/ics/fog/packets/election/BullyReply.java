/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.packets.election;

import de.tuilmenau.ics.fog.facade.Name;

/**
 * PACKET: It is used when an answer to BullyElect is signaled.
 */
public class BullyReply  extends BullyMessage
{
	private static final long serialVersionUID = -4666721123778977947L;
	
	/**
	 * 
	 * @param pBullyPriority is the priority of the node that generated the reply message
	 * @param pInitiator is the initiator of this message
	 */
	public BullyReply(Name pSenderName, float pBullyPriority)
	{
		super(pSenderName);
		mBullyPriority = pBullyPriority;
	}
	
	/**
	 * find out the priority of the node that created the message
	 * @return
	 */
	public float getBullyPriority()
	{
		return mBullyPriority;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + ":" + getSenderName() + "(" + mBullyPriority + ")";
	}
	
	private float mBullyPriority = 0;
}
