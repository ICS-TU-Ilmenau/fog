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
import de.tuilmenau.ics.fog.routing.Route;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.gates.GateID;


public class PleaseOpenUnicast extends PleaseOpenGate
{
	public PleaseOpenUnicast(int localProcessNumber, GateID localOutgoingGateNumber, Description description)
	{
		super(localProcessNumber, localOutgoingGateNumber, description);
	}

	@Override
	public boolean execute(ForwardingNode fn, Packet packet, Identity requester)
	{
		Route route = new Route(packet.getReturnRoute());
		Packet answer = new Packet(route, new OpenGateResponse(this, new GateID(-1), null));
		
		signAndSend(fn, answer);
		return true;
	}
}
