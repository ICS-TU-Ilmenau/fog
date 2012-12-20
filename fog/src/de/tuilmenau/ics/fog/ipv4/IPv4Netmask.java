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

/**
 * Holds a netmask, calculates a network-address
 */
public class IPv4Netmask extends IPv4Address
{
	private static final long serialVersionUID = 1764857790634564580L;

	public IPv4Netmask()
	{
		super();
		//this.address = new short[4];
	}
	
	public IPv4Netmask(short[] netmask)
	{
		super(netmask);
		//this.address = new short[4];
	}
	
	public IPv4Netmask(String netmask)
	{
		
		//this.address = new short[4];
		setNetmask(netmask);
	}
	
	public IPv4Netmask(IPv4Address netmask)
	{
		super(netmask);
		//this.address = new short[4];
	}
	
	/**
	 * Setter and parser for netmask as a string
	 * @param netmask netmask as a string
	 */
	public void setNetmask(String netmask)
	{
		String[] splitted = netmask.split("\\.");
		
		//TODO wrong string -> exceptions
		
		for(int i = 0; i < 3; i++)
		{
			this.address[i] =  Short.parseShort(splitted[i]);
		}
		
		// CIDR Notation
		String[] mask = splitted[3].split("\\/");
		
		
		
		
		if(mask.length > 1)
		{
			this.address[3] = Short.parseShort(mask[0]);
			// second part its a subnetmask length
			if(mask[1].split("\\.").length <= 1)
			{
				short routingPrefix =   Short.parseShort(mask[1]);
				
				for(int i = 0; i < 4; i++)	{
					if(routingPrefix >= 0){
						if(routingPrefix - 8 >= 0)
						{
							address[i] &= 0xff;
						} else	{
							address[i] &= 0xff << (8-routingPrefix);
						}
						
						routingPrefix -= 8;
						}else{
							address[i] = 0;
						}
				
					
				}				
			} else{ // second part is a subnetmask
				mask = mask[1].split("\\.");
				for(int i = 0; i < 4; i++)
				{
					this.address[i] &= Short.parseShort(mask[i]);
				}
			}
		} else {
			// first part was just a subnet address
			this.address[3] =  Short.parseShort(splitted[3]);
		}
		
	}
	
	/**
	 * calculates and saves the network-address
	 * @param address ip-address for calcualting network-address
	 * @return the calculated network-address
	 */
	public IPv4Address getNetwork(IPv4Address address)
	{
		IPv4Address network = new IPv4Address();
		
		for(int i = 0; i < 4; i++)
		{
			network.address[i] = (short) (this.address[i] & address.address[i]);
		}
		
		return network;
	}
	
	
}
