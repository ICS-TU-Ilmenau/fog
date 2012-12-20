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

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Signature;


public class SimpleCredential implements Credential
{
	public SimpleCredential(String name)
	{
		this.identity = new SimpleIdentity(name);
	}
	
	@Override
	public Identity getIdentity()
	{
		return identity;
	}
	
	@Override
	public Signature createSignature(Serializable data)
	{
		return new SimpleSignature(identity);
	}

	private SimpleIdentity identity; 
}
