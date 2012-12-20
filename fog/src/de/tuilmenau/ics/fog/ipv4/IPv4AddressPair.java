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
 * Is a class for a couple of IPv4Address
 */
public class IPv4AddressPair {
	
	// saves source and destination
	public IPv4Address source,dest;
	
	/**
	 *	default constructor
	 *	initializes source and destination 
	 */
	public IPv4AddressPair() {
		source = new IPv4Address();
		dest = new IPv4Address();
	}
	
	/**
	 * constructor
	 * @param source ip-address of the source
	 * @param dest ip-address of the destination
	 */
	public IPv4AddressPair(IPv4Address source, IPv4Address dest) {
		this.source = source;
		this.dest = dest;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		IPv4AddressPair couple = (IPv4AddressPair) obj;
		/*
		return (couple.source.equals(source) && couple.dest.equals(dest)) || 
				(couple.source.equals(dest) && couple.dest.equals(source));
		*/
		
		return (couple.source.equals(source) && couple.dest.equals(dest));
	}
}
