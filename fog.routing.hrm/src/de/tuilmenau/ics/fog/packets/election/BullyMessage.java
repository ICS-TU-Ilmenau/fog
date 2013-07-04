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

import java.io.Serializable;

import de.tuilmenau.ics.fog.facade.Name;

public class BullyMessage implements Serializable
{
	public BullyMessage(Name pSenderName)
	{
		mSenderName = pSenderName;
	}
	
	/**
	 * Determine the name of the sender
	 * 
	 * @return name of the sender
	 */
	//TODO: not used until now, however, the function is maybe used in the future when BullyAlive will be parsed by the main packet processing CoordinatorCEPDemultiplexed
	public Name getSenderName()
	{
		return mSenderName;
	}

	/**
	 * The name of the sender of this message. This is always a name of a physical node.
	 */
	private Name mSenderName = null;
}
