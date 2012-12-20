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

/**
 * Sends a IP Packet to destIP by an interval
 */
public class IPv4HostLoop extends IPv4Host {
	
	protected IPv4Address destIP;
	protected long loopTimer;

	public void setDestIP(IPv4Address address) {
		this.destIP = address;
	}

	public IPv4HostLoop() {
		super();
		this.loopTimer = 5000;
	}

	public IPv4HostLoop(long loopTimer) {
		super();
		this.loopTimer = loopTimer;
	}

	@Override
	public void run() {

		while (true) {
			try {
				this.send(destIP, "A Message to " + destIP.toString());
				this.receive();
				Thread.sleep(loopTimer);
			} catch (InterruptedException e) {
				e.printStackTrace();
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
