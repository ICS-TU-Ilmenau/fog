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

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.packets.ExperimentAgent;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.roles.Transparent;


/**
 * Gate for connecting two forwarding nodes directly.
 * Besides forwarding messages, the gate do not perform
 * any addition action. 
 */
public class TransparentGate extends FunctionalGate
{
	
	public TransparentGate(Node pNode, ForwardingElement pNextNode)
	{
		super(pNode, pNextNode, Transparent.PURE_FORWARDING, pNode.getIdentity());
	}
	
	@Override
	protected void init()
	{
		if(getNextNode() != null) {
			setState(GateState.OPERATE);
		} else {
			setState(GateState.ERROR);
		}
	}
	
	@Override
	protected boolean checkAvailability(HashMap<String, Serializable> pConfigData)
	{
		// Every process is allowed to use this transparent gate.
		return true;
	}
	
	public void handlePacket(Packet pPacket, ForwardingElement pLastHop)
	{
		if(!pPacket.isInvisible()) incMessageCounter();
		getNextNode().handlePacket(pPacket, this);
	}
	
	@Override
	protected void delete()
	{
		super.delete();
	}
}
