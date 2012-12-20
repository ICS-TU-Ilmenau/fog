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

import java.util.LinkedList;

public class IPv4Interface
{
	// configuration parameters
	protected IPv4Address address;
	protected IPv4Address broadcast;
	protected IPv4Address gateway;
	protected IPv4Netmask netmask;

	/**
	 * holds a buffer of anything to send, collected by hub
	 */
	protected LinkedList<IPv4Packet> sendBuffer;
	/**
	 * holds a buffer of anything to receive, collected by host
	 */
	protected LinkedList<IPv4Packet> receiveBuffer;

	
	/**
	 * default constructor 
	 */
	public IPv4Interface() {
		super();

		this.address = new IPv4Address();
		this.broadcast = new IPv4Address();
		this.gateway = new IPv4Address();
		this.netmask = new IPv4Netmask();

		this.sendBuffer = new LinkedList<IPv4Packet>();
		this.receiveBuffer = new LinkedList<IPv4Packet>();

	}
	
	
	/**
	 * @param address ipadress to initialize
	 */
	public IPv4Interface(IPv4Address address) {
		super();

		this.address = address;
		this.broadcast = new IPv4Address();
		this.gateway = new IPv4Address();
		this.netmask = new IPv4Netmask();

		this.sendBuffer = new LinkedList<IPv4Packet>();
		this.receiveBuffer = new LinkedList<IPv4Packet>();
	}

	/**
	 * @param address ip-address to initialize
	 * @param netmask ip-netmask to initialize
	 */
	public IPv4Interface(IPv4Address address, IPv4Netmask netmask) {
		super();

		this.address = address;
		this.broadcast = new IPv4Address();
		this.gateway = new IPv4Address();
		this.netmask = netmask;

		this.sendBuffer = new LinkedList<IPv4Packet>();
		this.receiveBuffer = new LinkedList<IPv4Packet>();
	}

	/**
	 * @return the mIPv4Address
	 */
	public IPv4Address getAddress() {
		return address;
	}

	/**
	 * @param address
	 *            the mIPv4Address to set
	 */
	public void setAddress(IPv4Address address) {
		this.address = address;
	}

	/**
	 * @return the mIPv4Broadcast
	 */
	public IPv4Address getBroadcast() {
		return broadcast;
	}

	/**
	 * @param broadcast
	 *            the mIPv4Broadcast to set
	 */
	public void setBroadcast(IPv4Address broadcast) {
		this.broadcast = broadcast;
	}

	/**
	 * @return the mIPv4Gateway
	 */
	public IPv4Address getGateway() {
		return gateway;
	}

	/**
	 * @param gateway
	 *            the mIPv4Gateway to set
	 */
	public void setGateway(IPv4Address gateway) {
		this.gateway = gateway;
	}

	/**
	 * @return the mIPv4Netmask
	 */
	public IPv4Netmask getNetmask() {
		return netmask;
	}

	/**
	 * @param netmask
	 *            the mIPv4Netmask to set
	 */
	public void setNetmask(IPv4Netmask netmask) {
		this.netmask = netmask;
	}

	/**
	 * Method for sending IPv4 Pakets
	 * 
	 * @param packet
	 *            what should be send Source address is automatically set
	 * @param raw set to true if packet should be delivered without any manipulation to the interface
	 */
	public void send(IPv4Packet packet, boolean raw) {
		if(!raw)
		{
			packet.setSource(this.address);
		}
		synchronized (sendBuffer) {
			this.sendBuffer.offer(packet);
		}

	}
	


	/**
	 * 
	 * Method for sending IPv4 packets
	 * 
	 * @param packet
	 *            what should be send Source address is automatically set
	 * 
	 * @param destination
	 *            where the packet should be send to, overrides header
	 */
	public void send(IPv4Packet packet, IPv4Address destination) {
		packet.setDestination(destination);
		send(packet,false);
	}

	/**
	 * Interface can receive IPv4 packets with that method
	 * 
	 * @param packet for receiving this packet
	 */
	public void receive(IPv4Packet packet) {
		// if packet is for this interface
		/*
		 * for ip address only if(packet.getDestination().equals(this.address))
		 * { // add it to receive buffer this.receiveBuffer.add(packet); }
		 */

		IPv4Address network = this.netmask.getNetwork(this.address);

		if (this.netmask.getNetwork(packet.getDestination()).equals(network)) {
			synchronized (receiveBuffer) {

				this.receiveBuffer.offer(packet);
			}
		}
	}

	/**
	 * @return the mSendBuffer
	 */
	public LinkedList<IPv4Packet> getSendBuffer() {

		return this.sendBuffer;

	}

	/**
	 * @return the mReceiveBuffer
	 */
	public LinkedList<IPv4Packet> getReceiveBuffer() {

		return this.receiveBuffer;

	}

}
