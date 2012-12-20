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

import java.io.Serializable;

import de.tuilmenau.ics.fog.authentication.IdentityManagement;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.ForwardingNode;
import de.tuilmenau.ics.fog.ui.Viewable;


/**
 * Base class for all FoG signaling messages. It acts as marker class for
 * FoG signaling messages and represents a flag in the FoG message header
 * marking signaling messages. Common base class allows an easy
 * identification of signaling payload.
 */
public abstract class Signalling extends LoggableElement implements Serializable
{
	private static final long serialVersionUID = -7009313966175211624L;
	
	public Signalling()
	{
		referenceProcessNumber = -1;
	}
	
	public Signalling(int localProcessNumber)
	{
		referenceProcessNumber = localProcessNumber;
	}
	
	/**
	 * @return Reference number for process the signaling belongs to
	 */
	public int getProcessNumber()
	{
		return referenceProcessNumber;
	}
	
	/**
	 * Method for executing actions in the context of a network entity.
	 * 
	 * @param pElement forwarding node or gate the packet is executed at
	 * @param pPacket whole packet
	 * @return true=successful; false=failed
	 */
	abstract public boolean execute(ForwardingElement pElement, Packet pPacket);
	
	/**
	 * @return Identity of first signature stored in packet; null if no signature available or entity not known
	 */
	protected static Identity getSenderIdentity(IdentityManagement management, Packet packet)
	{
		Signature senderSign = packet.getSenderAuthentication();
		
		if(senderSign != null) {
			if(management.checkSignature(senderSign, packet.getData())) {
				return senderSign.getIdentity();
			}
		}
		
		return null;
	}
	
	/**
	 * Shorthand for signing a packet by the FoG node of the forwarding node and sending it via the forwarding node.
	 */
	protected static void signAndSend(ForwardingNode fn, Packet packet)
	{
		fn.getNode().getAuthenticationService().sign(packet, fn.getOwner());
		
		fn.handlePacket(packet, null);
	}

	public String toString()
	{
		return this.getClass().getSimpleName();
	}

	@Viewable("Reference process number")
	private int referenceProcessNumber;	
}
