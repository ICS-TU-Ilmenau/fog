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
package de.tuilmenau.ics.fog.ipv4;


import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;

import de.tuilmenau.ics.fog.facade.Name;
import de.tuilmenau.ics.fog.facade.Namespace;
import de.tuilmenau.ics.fog.util.Size;

/**
 * Is a class of an ip-address
 */
public class IPv4Address implements Serializable, Name {
	
	private static final long serialVersionUID = -8906174202537073436L;
	// holds the address
	protected short[] address;
	
	public static final Namespace NAMESPACE_IP = new Namespace("ip");
	
	/**
	 * Default constructor inits object with invalid IP address
	 */
	public IPv4Address()
	{
		address = new short[]{255,255,255,255};
	}
	
	/**
	 * @param address ip-address to set as array of shorts
	 */
	public IPv4Address(short[] address)
	{
		this.address = address;
	}
	
	/**
	 * @param address ip-address to set as string
	 */
	public IPv4Address(String address)
	{
		super();
		this.address = new short[]{255,255,255,255};
		setAddress(address);
	}
	
	/**
	 * @param addr ip-address to set as Java std. IP address
	 */
	public IPv4Address(InetAddress addr)
	{
		byte[] addrBytes = addr.getAddress();
		address = new short[addrBytes.length];
		
		for(int i=0; i<addrBytes.length; i++) {
			address[i] = addrBytes[i];
			
			// correct byte to short transition
			if(address[i] < 0) address[i] += 256;
		}
	}
	
	/**
	 * Copy constructor
	 */
	public IPv4Address(IPv4Address address)
	{
		this.address = address.address.clone();
	}
	
	/**
	 * @param address as a string
	 * @return ip-address was successfully extracted from address string
	 */
	public boolean setAddress(String address)
	{
		String[] splitted = address.split("\\.");
		
		// invalid ip address format
		if(splitted.length != 4) return false;
		
		
		try
		{
			for(int i = 0; i < 4; i++)
			{
				this.address[i] = Short.parseShort(splitted[i]);
			}
		}
		catch (Exception e) {
			// invalid ip address format
			this.address =  new short[]{255,255,255,255};
			return false;
		}
		return true;
	}
	
	@Override
	public String toString()
	{
		String addr = "";
				
		// mAddress is initialized
		if(this.address.length == 4)
		{
		
			for(int i=0;i<4;i++)
			{
				addr += this.address[i];
				
				if(i < 3) addr += ".";
			}
		}
		else // no initialization yet
		{
			addr = "0.0.0.0";
		}
		
		return addr;
	}
	
	
	
	/**
	 * @return the ip-address
	 */
	public short[] getAddress()
	{
		return this.address;	
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == null) return false;
		
		IPv4Address addr = (IPv4Address)obj;
			
		return Arrays.equals(this.address,addr.address);	
	}
	
	@Override
	public int hashCode()
	{
		return Arrays.hashCode(address);
	}

	@Override
	public Namespace getNamespace()
	{
		return NAMESPACE_IP;
	}
	
	public int getSerialisedSize()
	{
		return Size.sizeOf(address);
	}
	
	//TODO ipv4 address clone method?!
}
