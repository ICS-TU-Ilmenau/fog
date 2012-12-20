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

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.transfer.forwardingNodes.ClientFN;


/**
 * Signals that the peer had closed the corresponding socket.
 */
public class PleaseCloseConnection extends SignallingRequest
{
	private static final long serialVersionUID = -5909151809075802012L;
	
	@Override
	public boolean execute(ForwardingNode fn, Packet packet, Identity requester)
	{
		if(fn instanceof ClientFN) {
			fn.getNode().getLogger().log(this, "execute close socket request on " +fn + " @ " + fn.getNode());
			((ClientFN) fn).closed();
			return true;
		}
		
		return false;
	}
}
