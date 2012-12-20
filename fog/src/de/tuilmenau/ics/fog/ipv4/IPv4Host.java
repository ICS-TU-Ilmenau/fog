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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Is a class of an ip-host
 */
public class IPv4Host extends Thread {
	// refence to ipv4interface
	protected IPv4Interface ipv4Interface;

	/**
	 *	default constructor
	 *	initializes default constructor 
	 */
	public IPv4Host() {
		super();
		ipv4Interface = new IPv4Interface();
	}

	/**
	 * @return the mIPv4Interface
	 */
	public IPv4Interface getIPv4Interface() {
		return ipv4Interface;
	}

	/**
	 * @param destination ist the destination ip-address for sending message to
	 * @param message message to send
	 */
	public void send(IPv4Address destination, String message) {
		if(destination != null)
		{
			IPv4Packet packet = new IPv4Packet();
			packet.setDestination(destination);
			packet.setProtocol(IPv4Protocol.IP);
			packet.setData(message);
	
			synchronized (ipv4Interface) {
				ipv4Interface.send(packet,false);
			}
		} else {
			System.err.println("IPv4Host.send(): destination == null");
		}
	}

	/**
	 * receives message and prints all payload out to the console
	 */
	public void receive() {
		if(ipv4Interface != null){
				
			int packetnr = 0;
	
			// synchronized (ipv4Interface.receiveBuffer) {
	
			System.out.println("Host: " + ipv4Interface.getAddress()+ " recieved packets");
	
	
			LinkedList<IPv4Packet> buffer = ipv4Interface.getReceiveBuffer();
	
			synchronized (buffer) {
	
				for (Iterator<IPv4Packet> i = buffer.iterator(); i.hasNext();) {
					IPv4Packet packet = i.next();
	
					System.out.println("P: " + packetnr + " S: "
							+ packet.getSource() + " D: " + packet.getDestination()
							+ " M: " + packet.getData());
	
					packetnr++;
	
					i.remove();
				}
	
			}
		} else {
			System.err.println("IPv4Host.receive(): ipv4Interface == null");
		}
	}

	@Override
	public void run() {

		while (true) {
			try {
				this.receive();
				Thread.sleep(1000 * 5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
