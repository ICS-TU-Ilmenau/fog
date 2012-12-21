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
 * This class is supposed to signalise the fact that the peer is alive
 *
 */
public class BullyAlive implements Serializable
{
	private static final long serialVersionUID = 4870662765189881992L;
	
	/**
	 * @param pHLName: name of the bus 
	 * @param pPeerName name of the peer that sent the alive message
	 */
	public BullyAlive(Name pPeerName, Name pCoordinatorName)
	{
		mPeerName = pPeerName;
		mCoord=pCoordinatorName;
	}
	
	/**
	 * 
	 * @return name of the coordinator that is active for the domain the election was initiated
	 */
	public Name getCoord()
	{
		return mCoord;
	}
	
	/**
	 * 
	 * @return name of the initiator of this message
	 */
	public Name getPeerName()
	{
		return mPeerName;
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + ":" + mPeerName;
	}
	
	private Name mCoord = null;
	private Name mPeerName=null;
}
