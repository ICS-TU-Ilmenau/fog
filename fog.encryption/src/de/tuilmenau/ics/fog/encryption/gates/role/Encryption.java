/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Encryption Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.encryption.gates.role;

import de.tuilmenau.ics.fog.transfer.gates.roles.GateClass;
import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;

/**
 * Descriptor for the functional role of encoding and decoding cipher code.
 */
public class Encryption extends GateClass
{
	private static final long serialVersionUID = 4010645749572219990L;
	
	/** Encoding to cipher. */
	public static final IFunctionDescriptor ENCODER = new Encryption("EncryptionEncoder");
	/** Decoding from cipher. */
	public static final IFunctionDescriptor DECODER = new Encryption("EncryptionDecoder");

	public Encryption(String pGateType) {
		super(pGateType);
	}
	
	@Override
	public String getDescriptionString()
	{
		return "Encrypting data stream between two peers.";
	}
}
