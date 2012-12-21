/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Encryption Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.encryption.gates;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.encryption.gates.headers.EncryptionHeader;
import de.tuilmenau.ics.fog.encryption.gates.role.Encryption;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.FunctionalGate;


/**
 * Functional gate to encode the payload.
 * A real encryption is not performed. It is just simulated in a way
 * enabling good simulation performance. The original payload is moved
 * into a {@link EncryptionHeader} and this header is replacing the
 * payload in a packet.
 */
public class EncryptionEncoderGate extends FunctionalGate
{
	/**
	 * @param pNode The node this gate belongs to.
	 * @param pNext The ForwardingElement the functional gate points to
	 * (in most cases a multiplexer).
	 * @param pConfigParams 
	 */
	public EncryptionEncoderGate(Node pNode, ForwardingElement pNext, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		super(pNode, pNext, Encryption.ENCODER, pOwner);
	}
	
	@Override
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop) {
		if(pPacket == null) {
			return;
		}
		
		ForwardingElement tTargetFE = getNextNode();
		if(tTargetFE != null) {
			if(!pPacket.isInvisible()) {
				incMessageCounter();
			}

			pPacket.setData(new EncryptionHeader(pPacket.getData()));
			
			tTargetFE.handlePacket(pPacket, this);
		} else {
			mLogger.log(this, "No next hop given. Packet " +pPacket +" dropped.");
			pPacket.logStats();
		}
	}
	
	@Override
	protected void init() {
		switchToState(GateState.OPERATE);
	}
	
	@Override
	protected boolean checkAvailability(HashMap<String, Serializable> pConfigData) {
		// Every process is allowed to use this gate.
		return true;
	}
}
