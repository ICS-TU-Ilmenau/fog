/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator - emulator interface
 * Copyright (c) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 ******************************************************************************/
package de.tuilmenau.ics.fog.emulator.ethernet;

import java.io.Serializable;


public class MACAddress implements Serializable
{
	private static final long serialVersionUID = -8615832067720049926L;
	
	public static int ETHERNET_ADDRESS_LENGTH = 6;

	public MACAddress(String pAddress)
	{
		mAddress = pAddress;
	}
	
	/**
	 * Creates MAC address object from byte array.
	 * 
	 * @param pInput Byte array with at least 6 bytes.
	 * @return MAC address object (!= null)
	 */
	public static MACAddress fromByteArray(byte[] pInput)
	{
		StringBuilder tSb = new StringBuilder();
		
		int tFoundParts = 0;
		for (int i = 0; i < pInput.length; i++) {
			tSb.append(String.format("%02X%s", pInput[i], (i < ETHERNET_ADDRESS_LENGTH - 1) ? ":" : ""));
			
			tFoundParts++;			
			if (tFoundParts == 6) {
				break;
			}
		}

		String tMacAddrStr = tSb.toString();

		return new MACAddress(tMacAddrStr);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == null) return false;
		if(this == obj) return true;
		
		if(obj instanceof MACAddress) {
			return mAddress.equals(((MACAddress) obj).mAddress);
		}
		
		return false;
	}
	
	public String toString()
	{
		return mAddress;
	}
	
	@Override
	public int hashCode()
	{
		return mAddress.hashCode();
	}
	
	private String mAddress;
}
