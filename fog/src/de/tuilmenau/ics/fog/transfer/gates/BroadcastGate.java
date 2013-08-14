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
package de.tuilmenau.ics.fog.transfer.gates;

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.topology.ILowerLayer;
import de.tuilmenau.ics.fog.topology.NetworkInterface;


/**
 * Gate which forwards messages to all other nodes reachable
 * by the lower layer technology.
 */
public class BroadcastGate extends DirectDownGate
{
	public BroadcastGate(FoGEntity entity, NetworkInterface networkInterface)
	{
		super(-1, entity, networkInterface, ILowerLayer.BROADCAST, null, entity.getIdentity());
	}	
}
