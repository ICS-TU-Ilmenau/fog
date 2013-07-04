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

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.GateFactory;

public class VideoOSDGateFactory implements GateFactory 
{
	@Override
	public AbstractGate createGate(String gateType, FoGEntity pEntity, ForwardingElement pNext, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		pEntity.getLogger().debug(this, "Have to create gate of type " + gateType);
		
		if (VideoOSDGate.class.getSimpleName().equals(gateType)) {				
			return new VideoOSDGate(pEntity, pNext, pConfigParams, pOwner);
		}
		else {
			return null;
		}
	}
}
