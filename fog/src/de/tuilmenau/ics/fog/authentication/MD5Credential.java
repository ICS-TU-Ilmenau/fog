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
package de.tuilmenau.ics.fog.authentication;

import java.io.Serializable;

import de.tuilmenau.ics.fog.facade.Signature;


public class MD5Credential extends SimpleCredential
{
	public MD5Credential(String name)
	{
		super(name);
	}
	
	@Override
	public Signature createSignature(Serializable data)
	{
		return new MD5Signature(getIdentity(), data);
	}
}
