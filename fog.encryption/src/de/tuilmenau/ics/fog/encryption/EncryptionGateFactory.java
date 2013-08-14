/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Encryption Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.encryption;

import java.io.Serializable;
import java.util.HashMap;

import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.encryption.gates.EncryptionDecoderGate;
import de.tuilmenau.ics.fog.encryption.gates.EncryptionEncoderGate;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.transfer.ForwardingElement;
import de.tuilmenau.ics.fog.transfer.gates.AbstractGate;
import de.tuilmenau.ics.fog.transfer.gates.GateFactory;


public class EncryptionGateFactory implements GateFactory
{
	@Override
	public AbstractGate createGate(String gateType, FoGEntity pEntity, ForwardingElement pNext, HashMap<String, Serializable> pConfigParams, Identity pOwner)
	{
		if(gateType.equals("EncryptionEncoderGate")) {
			return new EncryptionEncoderGate(pEntity, pNext, pConfigParams, pOwner);
		}
		else if (gateType.equals("EncryptionDecoderGate")) {
			return new EncryptionDecoderGate(pEntity, pNext, pConfigParams, pOwner);
		}
		else {
			return null;
		}
	}
}
