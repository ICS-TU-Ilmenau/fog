/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Video and audio views
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.videoOSD;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.topology.Node;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.GateFactory;

public class VideoOSDGateFactory implements GateFactory 
{
	@Override
	public AbstractGate createGate(String gateType, Node pNode, ForwardingElement pNext, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		pNode.getLogger().debug(this, "Have to create gate of type " + gateType);
		
		if (VideoOSDGate.class.getSimpleName().equals(gateType)) {				
			return new VideoOSDGate(pNode, pNext, pConfigParams, pOwner);
		}
		else {
			return null;
		}
	}
}
