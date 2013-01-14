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

import de.tuilmenau.ics.fog.facade.Identity;


/**
 * An identity without any keys or other "advanced" things.
 * It is just a string and optimized for simulation performance.
 */
public class SimpleIdentity implements Identity
{
	private static final long serialVersionUID = -2527795091170379614L;
	

	public SimpleIdentity(String name)
	{
		this.name = name;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj == this) {
			return true;
		}
		
		if(obj instanceof SimpleIdentity) {
			if(name != null) {
				return name.equals(((SimpleIdentity) obj).name);
			}
		}
		
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
	
	private String name;
}

