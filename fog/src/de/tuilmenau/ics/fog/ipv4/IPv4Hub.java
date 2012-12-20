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

@Deprecated
public class IPv4Hub extends Thread {
	// list of all connected interfaces and hosts...
	protected LinkedList<IPv4Interface> ipv4Interfaces;

	/**
	 * default constructor
	 */
	public IPv4Hub() {
		super();

		ipv4Interfaces = new LinkedList<IPv4Interface>();
	}

	
	/**
	 * Connects an ip interface with the hub
	 * @param ipv4Interface interface to connect
	 */
	public void connect(IPv4Interface ipv4Interface) {
		// never connect twice

		synchronized (ipv4Interfaces) {
			if (!this.ipv4Interfaces.contains(ipv4Interface)) {

				this.ipv4Interfaces.offer(ipv4Interface);

			}
		}
	}

	
	/**
	 * Disconnects an ip interface
	 * @param ipv4Interface interface to disconnect
	 * @return successful disconnected
	 */
	public boolean disconnect(IPv4Interface ipv4Interface) {

		synchronized (ipv4Interfaces) {

			
			return this.ipv4Interfaces.remove(ipv4Interface);

			

		}

	}

	/**
	 * Checks if an interface is connected
	 * @param ipv4Interface the interface to check
	 * @return if it is connected
	 */
	public boolean isConnected(IPv4Interface ipv4Interface) {
		synchronized (ipv4Interfaces) {
			return this.ipv4Interfaces.contains(ipv4Interface);
		}
	}

	/**
	 * Transfer of all data in any direction
	 * <p>
	 * Clears send buffer, fills receive buffer of all connected ipv4 interfaces
	 * <p>
	 * Must be called periodically
	 */
	public synchronized void transfer() {

		LinkedList<IPv4Packet> buffer = new LinkedList<IPv4Packet>();
		synchronized (buffer) {

			// all connected interfaces
			for (IPv4Interface iface : ipv4Interfaces) {
				// all packets of the interface which waiting for send

				LinkedList<IPv4Packet> sendBuffer = iface.getSendBuffer();

				synchronized (iface) {

					for (Iterator<IPv4Packet> i = sendBuffer.iterator(); i.hasNext();) {
						IPv4Packet packet = i.next();

						buffer.offer(packet);

						// i.remove();
					}
					
					sendBuffer.clear();

				}

			}
			// all connected interfaces
			for (IPv4Interface iface : ipv4Interfaces) {
				synchronized (iface) {
					for (IPv4Packet packet : buffer) {
						iface.receive(packet);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		super.run();

		while (true) {

			try {
				this.transfer();
				Thread.sleep(1000);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
