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
/**
 * 
 */
package de.tuilmenau.ics.fog.ipv4;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Is an IP-Host for echoing incoming data packets
 */
public class IPv4HostEcho extends IPv4Host {

		protected IPv4Address destIP;
		protected long loopTimer;

		/*
		 * Setter for Destination-IP 
		 */
		public void setDestIP(IPv4Address address) {
			this.destIP = address;
		}

		/*
		 * Default constructor
		 */
		public IPv4HostEcho() {
			super();
			this.loopTimer = 5000;
		}

		/*
		 * constructor
		 * @param loopTimer 
		 * 			initialize the timer for ticks
		 */
		public IPv4HostEcho(long loopTimer) {
			super();
			this.loopTimer = loopTimer;
		}

		@Override
		public void run() {

			while (true) {
				try {
					// this.send(destIP, "My loop message to " + destIP.toString());
					this.echo();
					Thread.sleep(loopTimer);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		/*
		 * Sends incoming data back
		 */
		
		public void echo() {


			
			LinkedList<IPv4Packet> buffer = ipv4Interface.getReceiveBuffer();

			synchronized (buffer) {
				
				// for all incoming packets in buffer
				for (Iterator<IPv4Packet> i = buffer.iterator(); i.hasNext();) {
					IPv4Packet packet = i.next();

					System.out.println(" S: " + packet.getSource() + " D: " + packet.getDestination()
							+ " M: " + packet.getData());

					// sends data back
					packet.setData(packet.getData().toString()+" ECHO from " +getIPv4Interface().getAddress());
					
					send(packet.getSource(),packet.getData().toString());

					i.remove();
				}

			}
		}
			
			

		/**
		 * @return the loopTimer
		 */
		public long getLoopTimer() {
			return loopTimer;
		}

		/**
		 * @param loopTimer
		 *            the loopTimer to set
		 */
		public void setLoopTimer(long loopTimer) {
			this.loopTimer = loopTimer;
		}
	}


