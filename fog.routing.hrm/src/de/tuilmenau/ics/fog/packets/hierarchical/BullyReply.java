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
 * Create an instance of this class if you would like to create an answer to a BullyElect message
 */
public class BullyReply implements Serializable
{
	private static final long serialVersionUID = -4666721123778977947L;
	
	/**
	 * 
	 * @param pBullyPriority is the priority of the node that generated the reply message
	 * @param pInitiator is the initiator of this message
	 */
	public BullyReply (float pBullyPriority, Name pInitiator)
	{
		mBullyPriority = pBullyPriority;
		mInitiator = pInitiator;
	}
	
	/**
	 * find out the priority of the node that created the message
	 * @return
	 */
	public float getBullyPriority()
	{
		return mBullyPriority;
	}
	
	/**
	 * 
	 * @return initiator of this message
	 */
	public Name getInitiator()
	{
		return mInitiator;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + ":" + mInitiator + "(" + mBullyPriority + ")";
	}
	
	private float mBullyPriority = 0;
	private Name mInitiator = null;
}
