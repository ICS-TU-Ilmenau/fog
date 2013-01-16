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
package de.tuilmenau.ics.fog.authentication;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Arrays;


public class PPKSignature extends SimpleSignature
{
	private static final long serialVersionUID = -8367522442173253573L;
	

	public PPKSignature(PPKIdentity identity, byte[] signature)
	{
		super(identity);
		
		this.signature = signature;
	}
	
	@Override
	public boolean check(Serializable data)
	{
		try {
			// 1. generate data checksum
			MessageDigest tChecksum = MessageDigest.getInstance("MD5");
			tChecksum.update(data.toString().getBytes());
			
			// 2. decrypt original checksum 
			byte[] tData = ((PPKIdentity)getIdentity()).getPublicKey().doFinal(signature);
			
			// 3. compare original checksum stored in the packet with generated one
			return Arrays.equals(tData, tChecksum.digest());
		}
		catch(Exception tExc) {
			throw new RuntimeException(this.toString(), tExc);
		}
	}
	
	public String toString()
	{
		int size = 0;
		if(signature != null) size = signature.length;
		
		return getIdentity() +"={PPK(" +size +")}";
	}
	
	private byte[] signature;
}

