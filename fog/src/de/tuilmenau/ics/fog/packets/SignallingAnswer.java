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
 * Base class for answering a SignallingRequest message.
 */
abstract public class SignallingAnswer extends Signalling
{
	private static final long serialVersionUID = -2462368668165423798L;

	public SignallingAnswer(SignallingRequest pRequest)
	{
		super(pRequest.getProcessNumber());
	}
	
	protected SignallingAnswer(int pReceiversProcessNumber)
	{
		super(pReceiversProcessNumber);
	}
	
	public final boolean execute(ForwardingElement pElement, Packet pPacket)
	{
		if(pElement instanceof ForwardingNode) {
			ForwardingNode tFN = (ForwardingNode) pElement;
			Identity tSender = getSenderIdentity(tFN.getEntity().getAuthenticationService(), pPacket);
			
			if(tSender != null) {
				synchronized(tFN) {
					Process tProcess = tFN.getEntity().getProcessRegister().getProcess(tFN, tSender, getProcessNumber());
					if(tProcess != null) {
						return execute(tProcess, pPacket, tSender);
					} else {
						tFN.getEntity().getLogger().warn(this, "No process available for owner " +tSender + " and ID " +getProcessNumber() +" in " +tFN + " while authentications of packets were " + pPacket.getAuthentications());
					}
				}
			} else {
				tFN.getEntity().getLogger().err(this, "Can not execute signaling message " +pPacket +" at " +pElement +" due to invalid authentication.");
			}
		} else {
			Logging.warn(this, "Element '" +pElement +"' has wrong type for signalling msg.");
		}
		
		return false;
	}

	abstract public boolean execute(Process pProcess, Packet pPacket, Identity pResponder);
}
