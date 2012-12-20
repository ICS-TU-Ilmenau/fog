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
package de.tuilmenau.ics.fog.routing.naming;

import java.io.Serializable;

import de.tuilmenau.ics.fog.transfer.TransferPlaneObserver.NamingLevel;


public class NameMappingEntry<Address extends Serializable> implements Serializable
{
	public NameMappingEntry(Address address)
	{
		this.address = address;
		this.level = NamingLevel.NAMES;
	}
	
	public NameMappingEntry(Address address, NamingLevel level)
	{
		this.address = address;
		this.level = level;
	}
	
	public Address getAddress()
	{
		return address;
	}

	public NamingLevel getNamingLevel()
	{
		return level;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj != null) {
			if(obj instanceof NameMappingEntry) {
				return ((NameMappingEntry) obj).getAddress().equals(address);
			}
			
			return obj.equals(address);
		}
		
		return false;
	}

	private Address address;
	private NamingLevel level;
}
