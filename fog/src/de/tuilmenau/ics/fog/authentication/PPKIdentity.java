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

import javax.crypto.Cipher;


/**
 * Public key of an entity in a public-private key system.
 */
public class PPKIdentity extends SimpleIdentity
{
	public PPKIdentity(String name, Cipher publicKey)
	{
		super(name);
	}
	
	public Cipher getPublicKey()
	{
		return publicKey;
	}
	
	public String toString()
	{
		return getName() +"(" +publicKey +")";
	}

	private Cipher publicKey;
}

