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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Signature;


/**
 * Private key for public-private key encryption.
 */
public class PPKCredential implements Credential
{
	private static KeyPairGenerator sKPG = null;
	
	public PPKCredential(String name)
	{
		try {
			if(sKPG == null) {
				sKPG = KeyPairGenerator.getInstance("RSA");
			}

			mKeys = sKPG.generateKeyPair();
			
			mEncCipher = Cipher.getInstance("RSA");
			mEncCipher.init(Cipher.ENCRYPT_MODE, mKeys.getPrivate());
			
			Cipher mDecCipher = Cipher.getInstance("RSA");
			mDecCipher.init(Cipher.DECRYPT_MODE, mKeys.getPublic());
			
			identity = new PPKIdentity(name, mDecCipher);
		}
		catch (Exception tExc) {
			throw new RuntimeException(this.toString(), tExc);
		}
	}
	
	@Override
	public Identity getIdentity()
	{
		return identity;
	}
	
	@Override
	public Signature createSignature(Serializable data)
	{
		try {
			// 1. generate checksum
			MessageDigest tChecksum = MessageDigest.getInstance("MD5");
			tChecksum.update(data.toString().getBytes());
			
			// 2. encript checksum
			byte[] tSig = mEncCipher.doFinal(tChecksum.digest());

			// 3. store result
			if(tSig != null)
				return new PPKSignature(identity, tSig);
			else
				return null;
		}
		catch(NoSuchAlgorithmException tExc) {
			throw new RuntimeException(this.toString(), tExc);
		}
		catch(BadPaddingException  tExc) {
			throw new RuntimeException(this.toString(), tExc);
		}
		catch (IllegalBlockSizeException tExc) {
			throw new RuntimeException(this.toString(), tExc);
		}
	}
	
	private KeyPair mKeys;
	private Cipher mEncCipher;
	private PPKIdentity identity;
}

