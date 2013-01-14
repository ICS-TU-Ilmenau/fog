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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import de.tuilmenau.ics.fog.facade.Identity;


/**
 * Signature created by a plain MD5 checksum over some data.
 */
public class MD5Signature extends SimpleSignature
{
	private static final long serialVersionUID = 6546990138319999153L;
	

	public MD5Signature(Identity identity, Serializable data)
	{
		super(identity);
		
		try {
			// 1. generate checksum
			MessageDigest checksum = MessageDigest.getInstance("MD5");
			checksum.update(data.toString().getBytes());

			// 2. store result
			signature = checksum.digest();
		}
		catch(NoSuchAlgorithmException exc) {
			throw new RuntimeException(this.toString(), exc);
		}
	}

	@Override
	public boolean check(Serializable data)
	{
		try {
			// 1. generate data checksum
			MessageDigest checksum = MessageDigest.getInstance("MD5");
			checksum.update(data.toString().getBytes());
			
			// 2. compare original checksum with generated one
			return Arrays.equals(signature, checksum.digest());
		}
		catch(Exception exc) {
			throw new RuntimeException(this.toString(), exc);
		}
	}
	
	@Override
	public String toString()
	{
		return getIdentity() +"={MD5}";
	}
	
	private byte[] signature;
}
