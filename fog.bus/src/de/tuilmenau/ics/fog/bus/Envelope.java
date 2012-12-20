/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Bus
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.bus;

import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.NeighborInformation;


public class Envelope
{
	public Envelope(Packet packet, NeighborInformation from, double timeToDeliver, double deliverDuration)
	{
		mPacket = packet;
		mFrom = from;
		mTimeToDeliver = timeToDeliver;
		mDeliverDuration = deliverDuration;
		
		// Debug check
		if(mDeliverDuration < 0) {
			mDeliverDuration = 0;
		}
	}
	
	public Packet mPacket;
	public NeighborInformation mFrom;
	public double mTimeToDeliver;
	public double mDeliverDuration;
}
