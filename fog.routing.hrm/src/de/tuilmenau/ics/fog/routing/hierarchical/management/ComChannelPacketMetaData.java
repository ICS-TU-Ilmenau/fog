/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Hierarchical Routing Management
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.routing.hierarchical.management;

import java.io.Serializable;

/**
 * This class stores the meta data about a packet.
 * It is used for packet storage within comm. channels
 */
public class ComChannelPacketMetaData
{
	private boolean mSent = false;
	private Serializable mPacket = null;
	
	/**
	 * Constructor
	 * 
	 * @param pPacket the packet
	 * @param pWasSent the I/O direction
	 */
	ComChannelPacketMetaData(Serializable pPacket, boolean pWasSent)
	{
		mPacket = pPacket;
		mSent = pWasSent;
	}
	
	/**
	 * Returns if the packet was sent
	 * 
	 * @return true or false
	 */
	public boolean wasSent()
	{
		return mSent;
	}
	
	/**
	 * Returns if the packet was received
	 * 
	 * @return true or false
	 */
	public boolean wasReceived()
	{
		return !mSent;
	}
	
	/**
	 * Returns the packet
	 * 
	 * @return the packet
	 */
	public Serializable getPacket()
	{
		return mPacket;
	}
}
