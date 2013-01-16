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


/**
 * This signature class is optimized for simulation performance. It contains
 * a string only. Thus, it is not a "real signature".
 */
public class SimpleSignature implements Signature
{
	private static final long serialVersionUID = -2055333911690373441L;
	

	public SimpleSignature(Identity identity)
	{
		this.identity = identity;
	}

	@Override
	public Identity getIdentity()
	{
		return identity;
	}
	
	@Override
	public boolean check(Serializable data)
	{
		return true;
	}
	
	@Override
	public String toString()
	{
		return identity +"={Simple}";
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == this) return true;
		
		if(obj instanceof SimpleSignature) {
			return identity.equals(((SimpleSignature) obj).getIdentity());
		} else {
			return false;
		}
	}
	
	private Identity identity;
}
