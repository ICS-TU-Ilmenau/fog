/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
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
package de.tuilmenau.ics.fog.facade.events;

import de.tuilmenau.ics.fog.facade.EventSource;
import de.tuilmenau.ics.fog.topology.NeighborInformation;

public class PeerInformationEvent extends Event
{
	public PeerInformationEvent(EventSource source, NeighborInformation peer, boolean appeared)
	{
		super(source);
		
		this.peer = peer;
		this.appeared = appeared;
	}
	
	public NeighborInformation getPeer()
	{
		return peer;
	}
	
	public boolean isAppeared()
	{
		return appeared;
	}
	
	private NeighborInformation peer;
	private boolean appeared;
}
