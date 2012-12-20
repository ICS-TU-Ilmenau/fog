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
package de.tuilmenau.ics.fog.packets;

import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.transfer.gates.GateID;
import de.tuilmenau.ics.fog.ui.Viewable;


public abstract class PleaseOpenGate extends SignallingRequest
{
	private static final long serialVersionUID = 895155324051284978L;

	public PleaseOpenGate(int localProcessNumber, GateID localOutgoingGateNumber, Description description)
	{
		super(localProcessNumber);
		
		this.peerOutgoingGateNumber = localOutgoingGateNumber;
		this.description = description;
	}
	
	protected GateID getGateNumber()
	{
		return peerOutgoingGateNumber;
	}
	
	protected Description getDescription()
	{
		return description;
	}
	
	@Viewable("Peer gate number")
	private GateID peerOutgoingGateNumber;
	
	@Viewable("Description")
	private Description description;
}
