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
import de.tuilmenau.ics.fog.transfer.manager.Process;
import de.tuilmenau.ics.fog.ui.Logging;


/**
 * Base class for notifications/error/info messages.
 */
abstract public class SignallingNotification extends Signalling
{
	public SignallingNotification()
	{
		super();
	}
	
	public SignallingNotification(Signalling basedOn)
	{
		super(basedOn != null ? basedOn.getProcessNumber() : -1);
	}
	
	@Override
	public final boolean execute(ForwardingElement element, Packet packet)
	{
		if(element instanceof ForwardingNode) {
			ForwardingNode fn = (ForwardingNode) element;
			Identity sender = getSenderIdentity(fn.getNode().getAuthenticationService(), packet);
			
			if(sender != null) {
				synchronized(fn) {
					Process tProcess = fn.getNode().getProcessRegister().getProcess(fn, sender, getProcessNumber());
					if(tProcess != null) {
						return executeProcess(tProcess, packet, sender);
					} else {
						return executeFN(fn, packet, sender);
					}
				}
			} else {
				fn.getNode().getLogger().err(this, "Can not execute signaling message " +packet +" due to invalid authentication.");
			}
		} else {
			Logging.warn(this, "Element '" +element +"' has wrong type for signalling msg.");
		}
		
		return false;
	}

	abstract public boolean executeProcess(Process process, Packet packet, Identity notifier);
	abstract public boolean executeFN(ForwardingNode fn, Packet packet, Identity notifier);
}
