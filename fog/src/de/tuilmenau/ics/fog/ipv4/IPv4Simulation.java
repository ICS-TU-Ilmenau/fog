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
 * IP-Only simulation for testing purposes
 */
public class IPv4Simulation
{
	public IPv4Simulation() {
		
	}

	public static void main(String[] args) {
		IPv4Host hostA = new IPv4Host();
		IPv4Host hostB = new IPv4Host();
		IPv4Host hostC = new IPv4Host();
		IPv4Host hostD = new IPv4Host();
		IPv4HostLoop hostE = new IPv4HostLoop();
		IPv4HostLoop hostF = new IPv4HostLoop();
		IPv4HostLoop hostG = new IPv4HostLoop();
		IPv4HostLoop hostH = new IPv4HostLoop();
		
		IPv4Hub hubA = new IPv4Hub();
		
		hubA.connect(hostA.getIPv4Interface());
		hubA.connect(hostB.getIPv4Interface());
		hubA.connect(hostC.getIPv4Interface());
		hubA.connect(hostD.getIPv4Interface());
		hubA.connect(hostE.getIPv4Interface());
		hubA.connect(hostF.getIPv4Interface());
		hubA.connect(hostG.getIPv4Interface());
		hubA.connect(hostH.getIPv4Interface());
		
		hostA.getIPv4Interface().setAddress(new IPv4Address("123.122.11.20"));
		hostB.getIPv4Interface().setAddress(new IPv4Address("123.234.11.21"));
		hostC.getIPv4Interface().setAddress(new IPv4Address("123.234.21.21"));
		hostD.getIPv4Interface().setAddress(new IPv4Address("123.234.31.21"));
		hostE.getIPv4Interface().setAddress(new IPv4Address("123.234.41.21"));
		hostF.getIPv4Interface().setAddress(new IPv4Address("123.234.41.29"));
		hostG.getIPv4Interface().setAddress(new IPv4Address("123.234.41.30"));
		hostH.getIPv4Interface().setAddress(new IPv4Address("123.234.41.31"));
		
		
		hostA.getIPv4Interface().setNetmask(new IPv4Netmask("255.255.255.255"));
		hostB.getIPv4Interface().setNetmask(new IPv4Netmask("255.255.255.255"));
		hostC.getIPv4Interface().setNetmask(new IPv4Netmask("255.255.255.255"));
		hostD.getIPv4Interface().setNetmask(new IPv4Netmask("255.255.255.255"));
		hostE.getIPv4Interface().setNetmask(new IPv4Netmask("255.255.255.255"));
		hostF.getIPv4Interface().setNetmask(new IPv4Netmask("255.255.255.255"));
		hostG.getIPv4Interface().setNetmask(new IPv4Netmask("255.255.255.255"));
		hostH.getIPv4Interface().setNetmask(new IPv4Netmask("255.255.255.255"));
		
		hostE.setDestIP(new IPv4Address("123.234.41.29"));
		hostF.setDestIP(new IPv4Address("123.234.41.21"));
		hostG.setDestIP(new IPv4Address("123.234.41.31"));
		hostH.setDestIP(new IPv4Address("123.234.41.30"));
		
		hostE.setLoopTimer(500);
		hostF.setLoopTimer(1000);
		hostG.setLoopTimer(1234);
		hostH.setLoopTimer(666);
		
		//hostD.getIPv4Interface().setNetmask(new IPv4Netmask("255.255.255.255/0"));
		
		
		hubA.start();
		hostA.start();
		hostB.start();
		hostC.start();
		hostD.start();
		hostE.start();
		hostF.start();
		hostG.start();
		hostH.start();
		
		hostA.send(new IPv4Address("123.234.11.21"), "Hallo host B");
		hostA.send(new IPv4Address("123.122.11.20"), "Hallo host A");
		
		hostB.send(new IPv4Address("123.122.11.20"), "Hallo host A");
		hostB.send(new IPv4Address("123.234.11.21"), "Hallo host B");
		hostC.send(new IPv4Address("123.122.11.20"), "Hallo host A");
		

		
		hostD.send(new IPv4Address("123.234.21.21"), "Hallo host C");
		hostE.send(new IPv4Address("123.234.31.21"), "Hallo host D");

	}
}
