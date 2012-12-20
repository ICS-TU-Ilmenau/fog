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
import java.util.HashMap;
import java.util.LinkedList;

import de.tuilmenau.ics.fog.exceptions.AuthenticationException;
import de.tuilmenau.ics.fog.facade.Host;
import de.tuilmenau.ics.fog.facade.Identity;
import de.tuilmenau.ics.fog.facade.Signature;
import de.tuilmenau.ics.fog.packets.Packet;
import de.tuilmenau.ics.fog.topology.AutonomousSystem;
import de.tuilmenau.ics.fog.util.Logger;


/**
 * Authentication service for FoG.
 * Stores credentials of entities. It allows the creation and verification of signatures.
 * Different types of signatures (from non-signatures for minimal performance overhead
 * to public-private-key) are available. 
 */
public class IdentityManagement
{
	private static final String AUTHENTICATION_ENTITY_TYPE_PROPERTY = "authentication.entity";
	
	/**
	 * Allows an authentication service entity to accept identities not known
	 * by it. If that is not allowed, the network management must set the
	 * accepted entities manually per entity. 
	 */
	private static final boolean ACCEPT_UNKNOWN_IDENTITIES = true;
	
	/**
	 * Defines if each node uses its own authentication service entity or
	 * if all nodes within the VM uses a single instance. While the latter
	 * is better for the performance for large simulations, the former is
	 * more realistic.
	 */
	private static final boolean AUTHENTICATION_SERVICE_ENTITY_PER_NODE = false;
	
	public enum IdentityType {
		Off,     // no entities (more exact: one entity for everything)
		Simple,  // no signatures; just matching entity names
		MD5,     // MD5 check sum and entity names (not encrypted; detects only bit errors)
		PPK      // encrypted MD5 check sum and entity names (performance intense)
	};
	
	/**
	 * Type of signature used in the simulation.
	 */
	private static IdentityType entityType = IdentityType.Simple;
	
	private static IdentityManagement sSingleton;
	
	
	public static IdentityManagement getInstance(AutonomousSystem as, Host host)
	{
		if((sSingleton == null) || AUTHENTICATION_SERVICE_ENTITY_PER_NODE) {
			
			// first time we create an instance?
			if(sSingleton == null) {
				String entityTypeName = System.getProperty(AUTHENTICATION_ENTITY_TYPE_PROPERTY, null);
				
				if(entityTypeName != null) {
					entityType = IdentityType.valueOf(entityTypeName);
				}
				// else: use default value
				
				as.getSimulation().getLogger().info(IdentityManagement.class, "Using authentication entity type " +entityType);
			}

			sSingleton = new IdentityManagement(as.getSimulation().getLogger(), entityType);
		}
		
		return sSingleton;
	}
	
	private IdentityManagement(Logger logger, IdentityType type)
	{
		this.type = type;
		this.logger = logger;
		
		logger.log(this, "Creating authentication service for identity type " +type);
	}
	
	/**
	 * Creates a new identity object for a name.
	 * 
	 * @param name Name of the identity
	 * @return Reference for the identity object (!= null)
	 */
	public Identity createIdentity(String name)
	{
		if(name == null) throw new RuntimeException(this +" - Can not create an identity since name is not defined.");
		
		Credential cred = getCredential(name);
		
		// is entity already known?
		if(cred == null) {
			switch(type) {
				case Off: cred = offCredential; break;// Identities deactivated
				case MD5: cred = new MD5Credential(name); break;
				case PPK: cred = new PPKCredential(name); break;
				default:  cred = new SimpleCredential(name);
			}
		}
		
		Identity iden = cred.getIdentity();
		mIdentities.put(iden, cred);
		
		return iden;
	}
	
	/**
	 * @return true, if the authentication service can and is allowed to sign data for the given entity; false otherwise
	 */
	public boolean canSignFor(Identity entity)
	{
		return mIdentities.get(entity) != null;
	}
	
	/**
	 * @return A signature for the data for the given entity (!= null)
	 * @throws AuthenticationException On error; e.g. if the entity is not known
	 */
	public Signature createSignature(Serializable data, Identity identity) throws AuthenticationException
	{
		if((identity != null) && (data != null)) {
			Credential cred = mIdentities.get(identity);
			
			if(cred != null) {
				return cred.createSignature(data);
			} else {
				throw new AuthenticationException(this, "Entity " +identity +" not known.");
			}
		} else {
			throw new AuthenticationException(this, "Can not generate a signature, since data or entity are not specified.");
		}		
	}
	
	/**
	 * @return true, if the signature fits to the data and the entity of the signature is known; false otherwise
	 */
	public boolean checkSignature(Signature signature, Serializable data)
	{
		if((signature != null) && (data != null)) {
			Credential cred = mIdentities.get(signature.getIdentity());
			
			// is the entity known?
			if((cred != null) || ACCEPT_UNKNOWN_IDENTITIES) {
				if(cred == null) {
					logger.debug(this, "Accept unknown identity " +signature.getIdentity());
				}
				
				return signature.check(data);
			}
		}
		
		return false;
	}
	
	/**
	 * Adds an authentication information to a packet.
	 */
	public boolean sign(Packet packet, Identity identity)
	{
		if((identity != null) && (packet != null)) {
			try {
				packet.addAuthentication(createSignature(packet.getData(), identity));
				return true;
			}
			catch (AuthenticationException exc) {
				logger.err(this, "Can not sign packet " +packet +" for identity " +identity +".", exc);
			}
		}

		return false;
	}
	
	/**
	 * Checks the authentication information in a packet.
	 *  
	 * @return true, if all signatures are valid for the data; false otherwise
	 */
	public boolean check(Packet packet)
	{
		if(packet != null) {
			LinkedList<Signature> signatures = packet.getAuthentications();
			
			if(signatures != null) {
				for(Signature sign : signatures) {
					if(!checkSignature(sign, packet.getData())) {
						return false;
					}
				}
			}
		}
		
		return true;
	}

	/**
	 * @return The credential for the entity name or null, if no entity found
	 */
	private Credential getCredential(String name)
	{
		for(Identity ident : mIdentities.keySet()) {
			if(ident.getName().equals(name)) {
				return mIdentities.get(ident);
			}
		}
		
		return null;
	}
	
	private Logger logger;
	private IdentityType type;
	private HashMap<Identity, Credential> mIdentities = new HashMap<Identity, Credential>();
	
	private static final SimpleCredential offCredential = new SimpleCredential("off");
}
