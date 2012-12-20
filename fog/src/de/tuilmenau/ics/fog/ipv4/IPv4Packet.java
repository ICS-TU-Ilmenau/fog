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

/**
 * Represents a IPv4-Packet
 */
public class IPv4Packet implements Serializable{
	private static final long serialVersionUID = -4283738105925209811L;
	protected IPv4Address source;
	protected IPv4Address destination;
	protected IPv4Protocol protocol;
	protected short ttl;
	protected IPv4TypeOfService tos;
	protected Serializable data;
	
	
	/**
	 * @return the mSource
	 */
	public IPv4Address getSource() {
		return source;
	}


	/**
	 * @param source the source to set
	 */
	public void setSource(IPv4Address source) {
		this.source = source;
	}


	/**
	 * @return the mDestination
	 */
	public IPv4Address getDestination() {
		return destination;
	}


	/**
	 * @param destination the destination to set
	 */
	public void setDestination(IPv4Address destination) {
		this.destination = destination;
	}


	/**
	 * @return the protocol
	 */
	public IPv4Protocol getProtocol() {
		return protocol;
	}


	/**
	 * @param protocol the protocol to set
	 */
	public void setProtocol(IPv4Protocol protocol) {
		this.protocol = protocol;
	}


	/**
	 * @return the mData
	 */
	public Serializable getData() {
		return data;
	}


	/**
	 * @param data the data to set
	 */
	public void setData(Serializable data) {
		this.data = data;
	}


	public IPv4Packet() {
		this.source = new IPv4Address();
		this.destination = new IPv4Address();
		this.protocol = IPv4Protocol.IP;
		
	}


	/**
	 * @return the Time To Live
	 */
	public short getTtl() {
		return ttl;
	}


	/**
	 * @param ttl the Time To Live to set
	 */
	public void setTtl(short ttl) {
		this.ttl = ttl;
	}


	/**
	 * @return the TypeOfService
	 */
	public IPv4TypeOfService getTos() {
		return tos;
	}


	/**
	 * @param tos the TypeOfService to set
	 */
	public void setTos(IPv4TypeOfService tos) {
		this.tos = tos;
	}

}
