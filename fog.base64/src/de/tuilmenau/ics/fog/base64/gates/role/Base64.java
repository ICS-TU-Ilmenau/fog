/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - Base64 Gates
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.base64.gates.role;

import de.tuilmenau.ics.fog.transfer.gates.roles.GateClass;
import de.tuilmenau.ics.fog.transfer.gates.roles.IFunctionDescriptor;

/**
 * Descriptor for the functional role of encoding and decoding BASE64 code.
 */
public class Base64 extends GateClass {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6948462912190524436L;
	
	/** Encoding to BASE64. */
	public static final IFunctionDescriptor ENCODER = new Base64("Base64Encoder");
	/** Decoding from BASE64. */
	public static final IFunctionDescriptor DECODER = new Base64("Base64Decoder");
	
	public Base64(String pGateType) {
		super(pGateType);
	}
	
	@Override
	public String getDescriptionString()
	{
		return "Base64 encoded data stream between two peers.";
	}
}
