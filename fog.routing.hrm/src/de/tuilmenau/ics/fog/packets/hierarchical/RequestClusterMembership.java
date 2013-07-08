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

import de.tuilmenau.ics.fog.packets.election.BullyElect;

public class RequestClusterMembership implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4759949996098949362L;
	
	private BullyElect mElectionMessage;
	
	public RequestClusterMembership(BullyElect pElectionMessage)
	{
		mElectionMessage = pElectionMessage;
	}
	
	public BullyElect getNestedElectionMessage()
	{
		return mElectionMessage;
	}
}
