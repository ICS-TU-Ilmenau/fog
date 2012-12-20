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
package de.tuilmenau.ics.fog.facade.properties;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import de.tuilmenau.ics.fog.application.InterOpIP;


public class IpDestinationProperty extends AbstractProperty
{
	private static final long serialVersionUID = -6577720701729904617L;

	public IpDestinationProperty(String pDestIp, int pDestPort, InterOpIP.Transport pDestTransport)
	{
		try {
			mDestIp = InetAddress.getByName(pDestIp).getAddress();
		} catch (UnknownHostException e) {			
			e.printStackTrace();
			mDestIp = new byte[4];
			mDestIp[0] = 127;
			mDestIp[1] = 0;
			mDestIp[2] = 0;
			mDestIp[3] = 1;
		}	
		mDestPort = pDestPort;
		mDestTransport = pDestTransport;
	}
	
	public IpDestinationProperty(byte[] pDestIp, int pDestPort, InterOpIP.Transport pDestTransport)
	{
		mDestIp = Arrays.copyOf(pDestIp, 4 /* hard coded for IPv4 */);
		mDestPort = pDestPort;
		mDestTransport = pDestTransport;
	}
	
	@Override
	public String getPropertyValues()
	{
		return getDestinationIpStr() + ":" + mDestPort + "[" + mDestTransport + "]";
	}
	
	public byte[] getDestinationIp(){
		return mDestIp;
	}
	
	public String getDestinationIpStr(){
		try {
//			System.out.println("Length " + mDestIp.length);
//			for (int i = 0; i < mDestIp.length;i++)
//				System.out.println("Data " + i + " = " + mDestIp[i]);
			String tRes = InetAddress.getByAddress(mDestIp).toString();
			// filter this "/" from the start of the IP address
			tRes = tRes.substring(1); 
			return tRes;
		} catch (Exception e) {
			//Logging.err(this, "Unable to parse IP address");			
			e.printStackTrace();
			return "127.0.0.1";
		}	
	}
	
	public int getDestinationPort(){
		return mDestPort;
	}
	
	public InterOpIP.Transport getDestinationTransport(){
		return mDestTransport;
	}
		
	private byte[] mDestIp;
	private int mDestPort;
	private InterOpIP.Transport mDestTransport;
}
