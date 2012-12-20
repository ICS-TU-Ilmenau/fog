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
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.ui.Logging;


abstract public class SignallingRequest extends Signalling
{
	private static final long serialVersionUID = -5066252039243644354L;

	public SignallingRequest()
	{
		this(-1);
	}

	public SignallingRequest(int localProcessNumber)
	{
		super(localProcessNumber);
	}

	@Override
	public final boolean execute(ForwardingElement element, Packet packet)
	{
		if(element instanceof ForwardingNode) {
			ForwardingNode fn = (ForwardingNode) element;
			Identity sender = getSenderIdentity(fn.getNode().getAuthenticationService(), packet);
			
			if(sender != null) {
				synchronized(element) {
					return execute(fn, packet, sender);
				}
			} else {
				fn.getNode().getLogger().err(this, "Can not execute signaling message " +packet +" at " +element +" due to invalid authentication.");
			}
		} else {
			Logging.log(this, "Element '" +element +"' has wrong type for signalling msg");
		}
		
		return false;
	}

	abstract public boolean execute(ForwardingNode fn, Packet packet, Identity requester);
}
